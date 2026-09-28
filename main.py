"""Standalone CLI for the POC — the equivalent of the main repo's `src/main_v2.py`, minus the
DB/websocket wiring that only makes sense inside the production ChatService.

Usage:
    export PYTHONPATH=src
    python src/main.py generate --service-name order "Create an order management microservice..."
    python src/main.py generate -s order requirements.txt --max-retries 3
"""

import asyncio
import sys
import uuid
from datetime import datetime
from pathlib import Path
from typing import Optional

import typer
from dotenv import load_dotenv
from rich.console import Console
from rich.panel import Panel

from agents.claude_code.session import ClaudeCodeSession
from graph import create_agent_graph
from prompts import SYSTEM_PROMPT
from state import AgentState, AgentStage

load_dotenv()

app = typer.Typer(help="Claude-Code-powered Java generator POC (layer-by-layer)")
console = Console()

POC_ROOT = Path(__file__).resolve().parent.parent


@app.command()
def generate(
    requirements: str = typer.Argument(
        ..., help="Requirements for the microservice (a file path or inline text)"
    ),
    service_name: str = typer.Option(..., "--service-name", "-s"),
    output_dir: Optional[str] = typer.Option(None, "--output", "-o"),
    max_retries: int = typer.Option(3, "--max-retries", "-r"),
    max_turns_per_layer: int = typer.Option(30, "--max-turns-per-layer"),
    model: Optional[str] = typer.Option(
        None, "--model", help="Overrides ANTHROPIC_MODEL for this run (e.g. for Bedrock)"
    ),
):
    """Generate a Java Spring Boot microservice, layer by layer, using Claude Code."""
    try:
        is_requirements_file = Path(requirements).is_file()
    except OSError:
        # Long inline requirements text can exceed the OS filename length limit
        # when treated as a path (e.g. "File name too long"); treat it as text.
        is_requirements_file = False

    if is_requirements_file:
        requirements_text = Path(requirements).read_text()
        console.print(f"[green]Loaded requirements from {requirements}[/green]")
    else:
        requirements_text = requirements

    if output_dir is None:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_dir = str(POC_ROOT / "generated" / f"{service_name}_{timestamp}")

    template_dir = POC_ROOT / "templates" / "skeleton"
    example_dir = POC_ROOT / "templates" / "examples" / "order-service"
    if not template_dir.exists() or not example_dir.exists():
        console.print(f"[red]Missing templates under {POC_ROOT / 'templates'}[/red]")
        sys.exit(1)

    project_id = str(uuid.uuid4())

    console.print(
        Panel.fit(
            f"Service: [yellow]{service_name}[/yellow]\n"
            f"Output: [yellow]{output_dir}[/yellow]\n"
            f"Max compile retries: [yellow]{max_retries}[/yellow]\n"
            f"Engine: [cyan]Claude Code (claude-agent-sdk)[/cyan]",
            title="Generation POC",
        )
    )

    state = AgentState(
        service_name=service_name,
        requirements=requirements_text,
        output_dir=output_dir,
        template_dir=str(template_dir),
        example_dir=str(example_dir),
        max_compile_retries=max_retries,
        current_stage=AgentStage.COPY_SKELETON,
    )

    session = ClaudeCodeSession(
        output_dir=output_dir,
        example_dir=str(example_dir),
        service_name=service_name,
        project_id=project_id,
        system_append=SYSTEM_PROMPT,
        max_turns=max_turns_per_layer,
        model=model,
    )

    async def _run():
        graph = create_agent_graph(session)
        try:
            final_state = await graph.ainvoke(state)
        finally:
            await session.close()
        return final_state

    try:
        final_state = asyncio.run(_run())
    except Exception:
        console.print_exception()
        sys.exit(1)

    stage = final_state["current_stage"] if isinstance(final_state, dict) else final_state.current_stage
    _print_result(final_state, stage, output_dir)
    if stage != AgentStage.COMPLETED:
        sys.exit(1)


def _get(final_state, key, default=None):
    if isinstance(final_state, dict):
        return final_state.get(key, default)
    return getattr(final_state, key, default)


def _print_result(final_state, stage, output_dir: str) -> None:
    console.print("\n" + "=" * 60)
    if stage == AgentStage.COMPLETED:
        layers = _get(final_state, "layers_completed", [])
        console.print(
            Panel.fit(
                f"[bold green]Generation complete[/bold green]\n\n"
                f"Layers generated: [cyan]{len(layers)}/7[/cyan]\n"
                f"Compilation attempts: [cyan]{_get(final_state, 'compilation_attempt')}[/cyan]\n"
                f"Tests: [green]{'passed' if _get(final_state, 'test_success') else 'failed/skipped'}[/green]\n"
                f"Input tokens: [cyan]{_get(final_state, 'total_input_tokens')}[/cyan]  "
                f"Output tokens: [cyan]{_get(final_state, 'total_output_tokens')}[/cyan]  "
                f"Cache read: [cyan]{_get(final_state, 'total_cache_read_tokens')}[/cyan]  "
                f"Cache write: [cyan]{_get(final_state, 'total_cache_creation_tokens')}[/cyan]\n\n"
                f"Output: [yellow]{output_dir}[/yellow]",
                title="Result",
                border_style="green",
            )
        )
    else:
        console.print(
            Panel.fit(
                f"[bold red]Generation failed[/bold red]\n\n"
                f"Stage: [yellow]{stage}[/yellow]\n"
                f"Error: [yellow]{_get(final_state, 'error_message')}[/yellow]\n"
                f"Partial output: [yellow]{output_dir}[/yellow]",
                title="Result",
                border_style="red",
            )
        )


@app.command()
def validate():
    """Check that the local environment can plausibly run the POC."""
    import shutil as _shutil

    console.print(Panel.fit("Environment Validation", title="Validation"))
    ok = True

    if _shutil.which("java"):
        console.print("[green]OK[/green] java on PATH")
    else:
        console.print("[red]MISSING[/red] java not on PATH")
        ok = False

    template_dir = POC_ROOT / "templates" / "skeleton"
    example_dir = POC_ROOT / "templates" / "examples" / "order-service"
    for name, path in [("skeleton", template_dir), ("example", example_dir)]:
        if path.exists():
            console.print(f"[green]OK[/green] {name} templates found at {path}")
        else:
            console.print(f"[red]MISSING[/red] {name} templates not found at {path}")
            ok = False

    import os

    if os.getenv("ANTHROPIC_API_KEY") or os.getenv("CLAUDE_CODE_USE_BEDROCK"):
        console.print("[green]OK[/green] a Claude provider is configured")
    else:
        console.print(
            "[red]MISSING[/red] set ANTHROPIC_API_KEY, or CLAUDE_CODE_USE_BEDROCK=1 + AWS creds"
        )
        ok = False

    if not ok:
        sys.exit(1)
    console.print("[bold green]All checks passed[/bold green]")


if __name__ == "__main__":
    app()
