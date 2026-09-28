"""`ClaudeCodeSession` — the POC's implementation of section 3's sketch.

One session (one `ClaudeSDKClient`, one underlying CLI subprocess) is created per generation
run and reused across all 7 layer turns plus every compile-fix turn, so:
- prompt caching covers the shared system-prompt/skeleton prefix across turns;
- each layer sees what earlier layers wrote on disk without the graph re-injecting context;
- token/cost usage can be accumulated across the whole run.

`set_scope()` is called by the graph node right before each `run_turn`, to tell the shared
hooks which layer (or "fix" mode) is currently active — see hooks.py.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

from claude_agent_sdk import (
    AssistantMessage,
    ClaudeAgentOptions,
    ClaudeSDKClient,
    HookMatcher,
    ResultMessage,
    TextBlock,
    ToolUseBlock,
)

from agents.claude_code.env import build_env
from agents.claude_code.hooks import (
    ScopeGuardState,
    make_bash_guard_hook,
    make_javalang_check_hook,
    make_scope_guard_hook,
)

WRITE_TOOL_NAMES = {"Write", "Edit", "MultiEdit"}


def _usage_field(obj, *keys: str, default: int = 0) -> int:
    """ModelUsage/ResultMessage.usage fields may come back as dict-like or attribute-like."""
    for key in keys:
        if isinstance(obj, dict) and key in obj:
            return obj[key] or default
        if hasattr(obj, key):
            return getattr(obj, key) or default
    return default


@dataclass
class TurnUsage:
    input_tokens: int = 0
    output_tokens: int = 0
    cache_read_tokens: int = 0
    cache_creation_tokens: int = 0


@dataclass
class TurnResult:
    text: str
    files_touched: list[str] = field(default_factory=list)
    tool_calls: list[str] = field(default_factory=list)
    usage: TurnUsage = field(default_factory=TurnUsage)
    session_id: Optional[str] = None
    is_error: bool = False
    num_turns: int = 0
    denied: list[str] = field(default_factory=list)


class ClaudeCodeSession:
    def __init__(
        self,
        *,
        output_dir: str,
        example_dir: str,
        service_name: str,
        project_id: str,
        system_append: str,
        max_turns: int = 30,
        model: Optional[str] = None,
    ):
        self.output_dir = Path(output_dir)
        self.scope = ScopeGuardState(output_dir=self.output_dir, service_name=service_name.lower())

        options = ClaudeAgentOptions(
            cwd=str(self.output_dir),
            add_dirs=[str(Path(example_dir).resolve())],
            system_prompt={
                "type": "preset",
                "preset": "claude_code",
                "append": system_append,
            },
            allowed_tools=["Read", "Glob", "Grep", "Write", "Edit", "MultiEdit", "TodoWrite", "Bash"],
            permission_mode="acceptEdits",
            setting_sources=[],  # don't let user/project/local CLAUDE.md or settings leak in
            hooks={
                "PreToolUse": [
                    HookMatcher(
                        matcher="Write|Edit|MultiEdit",
                        hooks=[make_scope_guard_hook(self.scope)],
                    ),
                    HookMatcher(matcher="Bash", hooks=[make_bash_guard_hook(self.scope)]),
                ],
                "PostToolUse": [
                    HookMatcher(
                        matcher="Write|Edit|MultiEdit",
                        hooks=[make_javalang_check_hook(self.scope)],
                    ),
                ],
            },
            env=build_env(project_id),
            max_turns=max_turns,
            model=model,
        )
        self._client = ClaudeSDKClient(options=options)
        self._connected = False
        self.session_id: Optional[str] = None
        self.total_usage = TurnUsage()

    def set_scope(self, *, mode: str, layer: Optional[str] = None) -> None:
        self.scope.mode = mode
        self.scope.layer = layer

    async def connect(self) -> None:
        if not self._connected:
            await self._client.connect()
            self._connected = True

    async def close(self) -> None:
        if self._connected:
            await self._client.disconnect()
            self._connected = False

    async def interrupt(self) -> None:
        if self._connected:
            await self._client.interrupt()

    async def run_turn(self, prompt: str) -> TurnResult:
        await self.connect()

        denied_before = len(self.scope.denied_log)
        await self._client.query(prompt)

        files_touched: list[str] = []
        tool_calls: list[str] = []
        text_parts: list[str] = []
        result_message: Optional[ResultMessage] = None

        async for message in self._client.receive_response():
            if isinstance(message, AssistantMessage):
                for block in message.content:
                    if isinstance(block, ToolUseBlock):
                        tool_calls.append(block.name)
                        file_path = block.input.get("file_path") if isinstance(block.input, dict) else None
                        if file_path and block.name in WRITE_TOOL_NAMES:
                            files_touched.append(file_path)
                        print(f"    [tool] {block.name} {file_path or ''}")
                    elif isinstance(block, TextBlock) and block.text:
                        text_parts.append(block.text)
            elif isinstance(message, ResultMessage):
                result_message = message

        usage = TurnUsage()
        if result_message is not None:
            self.session_id = result_message.session_id
            model_usage = result_message.model_usage or {}
            if model_usage:
                for mu in model_usage.values():
                    usage.input_tokens += _usage_field(mu, "inputTokens")
                    usage.output_tokens += _usage_field(mu, "outputTokens")
                    usage.cache_read_tokens += _usage_field(mu, "cacheReadInputTokens")
                    usage.cache_creation_tokens += _usage_field(mu, "cacheCreationInputTokens")
            elif result_message.usage:
                u = result_message.usage
                usage.input_tokens = _usage_field(u, "input_tokens")
                usage.output_tokens = _usage_field(u, "output_tokens")
                usage.cache_read_tokens = _usage_field(u, "cache_read_input_tokens")
                usage.cache_creation_tokens = _usage_field(u, "cache_creation_input_tokens")

            self.total_usage.input_tokens += usage.input_tokens
            self.total_usage.output_tokens += usage.output_tokens
            self.total_usage.cache_read_tokens += usage.cache_read_tokens
            self.total_usage.cache_creation_tokens += usage.cache_creation_tokens

        return TurnResult(
            text="\n".join(text_parts).strip(),
            files_touched=files_touched,
            tool_calls=tool_calls,
            usage=usage,
            session_id=self.session_id,
            is_error=bool(result_message and result_message.is_error),
            num_turns=result_message.num_turns if result_message else 0,
            denied=self.scope.denied_log[denied_before:],
        )


def extract_summary(text: str) -> str:
    """Pull the `SUMMARY: ...` line the layer/fix prompts ask for (see prompts.FINAL_MESSAGE_INSTRUCTION)."""
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.upper().startswith("SUMMARY:"):
            return stripped[len("SUMMARY:") :].strip()
    return text[:200].strip()
