"""Env overrides for the Claude Code CLI subprocess (section 7 / section 8 of the plan).

The SDK inherits the current process environment by default (see
`subprocess_cli.py::_build_env` in the installed `claude-agent-sdk` package) and applies
these as overrides on top. For this local POC we don't attempt the production-grade minimal
env (no DB/JWT secrets exist here to protect against); we only set what controls the CLI's
own behaviour and provider selection.

Set one of:
- `ANTHROPIC_API_KEY` (direct Anthropic API), or
- `CLAUDE_CODE_USE_BEDROCK=1` + `AWS_REGION` (+ AWS credentials) and `ANTHROPIC_MODEL`,
before running the POC — see README.md.
"""

import os
from pathlib import Path

POC_ROOT = Path(__file__).resolve().parents[3]


def build_env(project_id: str) -> dict[str, str]:
    env: dict[str, str] = {
        "DISABLE_AUTOUPDATER": "1",
        "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": "1",
        "CLAUDE_CONFIG_DIR": str(POC_ROOT / ".cache" / "claude_config" / project_id),
    }

    # Pass provider selection through explicitly so the resulting env is self-documenting,
    # even though these would already be inherited from the parent process.
    for key in (
        "ANTHROPIC_API_KEY",
        "ANTHROPIC_MODEL",
        "ANTHROPIC_SMALL_FAST_MODEL",
        "CLAUDE_CODE_USE_BEDROCK",
        "AWS_REGION",
        "AWS_ACCESS_KEY_ID",
        "AWS_SECRET_ACCESS_KEY",
        "AWS_SESSION_TOKEN",
    ):
        if key in os.environ:
            env[key] = os.environ[key]

    return env
