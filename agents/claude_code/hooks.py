"""Hooks for the Claude Code generator session — section 6 of the design doc.

Three hooks are implemented, matching the plan's table:

- ``scope_guard`` (PreToolUse on Write/Edit/MultiEdit): denies writes outside the current
  layer's package (or, in fix-compile mode, outside the project's ``src/`` tree), and always
  denies the skeleton/exclusion files in ``constants/exclusion_files.py``.
- ``bash_guard`` (PreToolUse on Bash): only used in fix-compile mode, where the model is
  allowed to delete a file it decides is no longer needed. Everything else is denied.
- ``javalang_check`` (PostToolUse on Write/Edit/MultiEdit): parses the written/edited ``*.java``
  file with javalang and feeds a syntax error back to the model as ``additionalContext`` so it
  can fix it immediately, instead of waiting for the next Maven compile.

All three read a single mutable `ScopeGuardState`, which the caller updates before each
`run_turn` (see session.py) — one Claude Code session is reused across every layer and the
fix-compile turns, so the hooks can't be rebuilt per turn; they just read whatever the state
says "now" mode/layer is.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Literal, Optional

from constants.exclusion_files import EXCLUSION_FILES

try:
    import javalang
except ImportError:  # pragma: no cover - dependency is in requirements.txt
    javalang = None

# Layers that may also touch these extra package subdirectories.
EXTRA_ALLOWED_SUBDIRS = {
    "entity": ["enumeration"],
    "service": ["config"],
}

# A conservative allowlist for the fix-compile turn's scoped `rm`.
_RM_PATTERN = re.compile(r"^\s*rm\s+(-f\s+)?(src/(main|test)/java/\S+\.java)\s*$")


@dataclass
class ScopeGuardState:
    """Mutable scope the hooks consult on every tool call.

    `output_dir` / `src_main_java` are fixed for the whole run. `mode` and `layer` are updated
    by the graph node right before each `run_turn` call on the shared session.
    """

    output_dir: Path
    service_name: str
    mode: Literal["layer", "fix"] = "layer"
    layer: Optional[str] = None  # CodeLayer.value, only meaningful when mode == "layer"
    denied_log: list[str] = field(default_factory=list)

    def __post_init__(self) -> None:
        # Resolved once so it matches the resolved paths checked in check_write_path (macOS
        # symlinks /tmp -> /private/tmp, so leaving this unresolved caused every path to be
        # rejected as "outside the project directory").
        self.output_dir = self.output_dir.resolve()

    @property
    def src_main_java(self) -> Path:
        return self.output_dir / "src" / "main" / "java" / "ai" / "agilery" / self.service_name

    @property
    def src_test_java(self) -> Path:
        return self.output_dir / "src" / "test" / "java" / "ai" / "agilery" / self.service_name

    def _relative_to_service_root(self, path: Path) -> Optional[Path]:
        for root in (self.src_main_java, self.src_test_java):
            try:
                return path.relative_to(root)
            except ValueError:
                continue
        return None

    def check_write_path(self, raw_path: str) -> tuple[bool, str]:
        """Returns (allowed, reason). reason is only meaningful when allowed is False."""
        path = Path(raw_path)
        if not path.is_absolute():
            path = (self.output_dir / path).resolve()
        else:
            path = path.resolve()

        try:
            path.relative_to(self.output_dir.resolve())
        except ValueError:
            return False, f"Path {path} is outside the project directory {self.output_dir}"

        rel = self._relative_to_service_root(path)
        if rel is None:
            # Non-Java source paths (application.yml, pom.xml, etc.) are left to the
            # deterministic pipeline, not the layer/fix turns.
            return False, (
                f"Path {path} is not under the generated service's package "
                f"(ai/agilery/{self.service_name}/...)"
            )

        rel_str = str(rel).replace("\\", "/")
        if rel_str in EXCLUSION_FILES:
            return False, f"{rel_str} is a skeleton file and must never be regenerated or edited"

        if self.mode == "fix":
            return True, ""

        # Layer mode: the first path segment must be the current layer (or an allowed extra).
        allowed_dirs = [self.layer] + EXTRA_ALLOWED_SUBDIRS.get(self.layer or "", [])
        top_dir = rel.parts[0] if rel.parts else ""
        if top_dir not in allowed_dirs:
            return False, (
                f"Currently generating the '{self.layer}' layer — writes are only allowed "
                f"under {allowed_dirs}, not '{top_dir}'"
            )
        return True, ""


def make_scope_guard_hook(state: ScopeGuardState):
    async def scope_guard(input_data: dict, tool_use_id: str | None, context: Any) -> dict:
        tool_input = input_data.get("tool_input", {})
        file_path = tool_input.get("file_path")
        if not file_path:
            return {}

        allowed, reason = state.check_write_path(file_path)
        if allowed:
            return {}

        state.denied_log.append(f"{input_data.get('tool_name')} {file_path}: {reason}")
        return {
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "deny",
                "permissionDecisionReason": reason,
            }
        }

    return scope_guard


def make_bash_guard_hook(state: ScopeGuardState):
    async def bash_guard(input_data: dict, tool_use_id: str | None, context: Any) -> dict:
        if state.mode != "fix":
            reason = "Bash is not available while generating a layer — use Write/Edit only"
            state.denied_log.append(f"Bash denied outside fix mode: {reason}")
            return {
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": "deny",
                    "permissionDecisionReason": reason,
                }
            }

        command = (input_data.get("tool_input", {}) or {}).get("command", "")

        if not _RM_PATTERN.match(command):
            reason = (
                "Bash is only allowed to delete a source file with a scoped "
                "`rm src/main/java/...` or `rm src/test/java/...` command"
            )
            state.denied_log.append(f"Bash {command!r}: {reason}")
            return {
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": "deny",
                    "permissionDecisionReason": reason,
                }
            }
        return {}

    return bash_guard


def make_javalang_check_hook(state: ScopeGuardState):
    async def javalang_check(input_data: dict, tool_use_id: str | None, context: Any) -> dict:
        if javalang is None:
            return {}

        tool_input = input_data.get("tool_input", {})
        file_path = tool_input.get("file_path")
        if not file_path or not str(file_path).endswith(".java"):
            return {}

        path = Path(file_path)
        if not path.is_absolute():
            path = state.output_dir / path

        try:
            source = path.read_text(encoding="utf-8")
        except OSError:
            return {}

        try:
            javalang.parse.parse(source)
        except javalang.parser.JavaSyntaxError as exc:
            return {
                "hookSpecificOutput": {
                    "hookEventName": "PostToolUse",
                    "additionalContext": (
                        f"javalang detected a syntax error in {path.name} — fix it now before "
                        f"moving on: {exc}"
                    ),
                }
            }
        except Exception:
            # javalang doesn't parse every valid modern Java construct (e.g. some pattern
            # matching / record syntax) — a parser exception here is not necessarily a real
            # syntax error, so don't block the model on it.
            return {}

        return {}

    return javalang_check
