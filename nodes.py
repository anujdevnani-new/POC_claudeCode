"""LangGraph node functions.

Deterministic nodes (`copy_skeleton_node`, `compile_code_node`, `run_test_node`) are unchanged
in spirit from the main repo's `nodes_v2.py` — same skeleton copy + Maven compile/test, just
trimmed of the DB/S3/ECS/HITL/credits integration that only makes sense inside the production
service (see README.md "What's deliberately left out").

`generate_layer_node` / `fix_compile_error_node` are the section-4 replacement for
`nodes_v2_optimized.py`'s `generate_layer_node_optimized` / `fix_compile_error_node_optimized`:
one Claude Code turn per layer (or per fix attempt) on a single shared `ClaudeCodeSession`,
instead of one LangChain LLM call whose `=== FILE: ... ===` text is regex-parsed and written to
disk.
"""

import shutil
from pathlib import Path

from agents.claude_code.session import ClaudeCodeSession, extract_summary
from prompts import (
    FINAL_MESSAGE_INSTRUCTION,
    LAYER_PROMPT_MAP,
)
from state import LAYER_ORDER, AgentState, AgentStage, CodeLayer, CompilationError, LayerContext
from utils.file_parser import fix_project_structure
from utils.java_compiler import compile_java_project, parse_compilation_errors
from utils.test_runner import run_tests

# ----------------------------------------------------------------------------
# Deterministic nodes
# ----------------------------------------------------------------------------


def copy_skeleton_node(state: AgentState) -> AgentState:
    if state.current_stage != AgentStage.COPY_SKELETON:
        return state

    print("\n" + "=" * 60)
    print("STAGE: COPY_SKELETON")
    print("=" * 60 + "\n")

    template_dir = Path(state.template_dir)
    output_dir = Path(state.output_dir)

    if output_dir.exists():
        shutil.rmtree(str(output_dir), ignore_errors=True)
    shutil.copytree(str(template_dir), str(output_dir))
    fix_project_structure(str(output_dir), state.service_name)

    print(f"Copied skeleton -> {output_dir}")
    state.current_stage = AgentStage.GENERATE_ENTITIES
    return state


def compile_code_node(state: AgentState) -> AgentState:
    if state.current_stage != AgentStage.COMPILE_CODE:
        return state

    state.compilation_attempt += 1
    print("\n" + "=" * 60)
    print(f"STAGE: COMPILE_CODE (attempt {state.compilation_attempt})")
    print("=" * 60 + "\n")

    success, output = compile_java_project(state.output_dir)
    state.compilation_output = output
    state.compilation_success = success

    if success:
        print("Compilation SUCCESSFUL")
        state.compilation_errors = []
        state.compilation_errors_by_file = {}
        state.current_stage = AgentStage.RUN_TEST
        return state

    print("Compilation FAILED")
    print(output[-2000:])

    parsed = parse_compilation_errors(output)
    state.compilation_errors = [CompilationError(**e) for e in parsed]
    errors_by_file: dict[str, list[CompilationError]] = {}
    for err in state.compilation_errors:
        errors_by_file.setdefault(err.file, []).append(err)
    state.compilation_errors_by_file = errors_by_file

    if state.compilation_attempt >= state.max_compile_retries:
        state.error_message = (
            f"Compilation still failing after {state.compilation_attempt} attempts."
        )
        state.current_stage = AgentStage.FAILED
    else:
        state.current_stage = AgentStage.FIX_COMPILE_ERROR

    return state


def run_test_node(state: AgentState) -> AgentState:
    print("\n" + "=" * 60)
    print("STAGE: RUN_TEST")
    print("=" * 60 + "\n")

    output_dir = Path(state.output_dir)
    test_dir = output_dir / "src" / "test" / "java"
    if not test_dir.exists() or not list(test_dir.rglob("*.java")):
        print("No test files found, skipping tests")
        state.test_success = True
        state.test_output = "No tests found"
        state.current_stage = AgentStage.COMPLETED
        return state

    success, output = run_tests(str(output_dir))
    state.test_output = output
    state.test_success = success
    state.current_stage = AgentStage.COMPLETED if success else AgentStage.FAILED
    if not success:
        state.error_message = "Tests failed — see test_output."
        print(output[-2000:])
    else:
        print("Tests PASSED")
    return state


# ----------------------------------------------------------------------------
# Claude Code layer / fix nodes
# ----------------------------------------------------------------------------

# Which earlier layers' package paths a layer should be pointed at (mirrors
# nodes_v2_optimized.build_layer_context_optimized's layer_context_map).
LAYER_CONTEXT_MAP: dict[CodeLayer, list[CodeLayer]] = {
    CodeLayer.DTO: [CodeLayer.ENTITY],
    CodeLayer.REPOSITORY: [CodeLayer.ENTITY],
    CodeLayer.MAPPER: [CodeLayer.ENTITY, CodeLayer.DTO],
    CodeLayer.SERVICE: [CodeLayer.ENTITY, CodeLayer.DTO, CodeLayer.REPOSITORY, CodeLayer.MAPPER],
    CodeLayer.VALIDATION: [CodeLayer.ENTITY, CodeLayer.DTO, CodeLayer.REPOSITORY, CodeLayer.SERVICE],
    CodeLayer.CONTROLLER: [CodeLayer.DTO, CodeLayer.SERVICE, CodeLayer.VALIDATION],
}

_NEXT_STAGE_AFTER_LAYER = {
    CodeLayer.ENTITY: AgentStage.GENERATE_DTOS,
    CodeLayer.DTO: AgentStage.GENERATE_REPOSITORIES,
    CodeLayer.REPOSITORY: AgentStage.GENERATE_MAPPERS,
    CodeLayer.MAPPER: AgentStage.GENERATE_SERVICES,
    CodeLayer.SERVICE: AgentStage.GENERATE_VALIDATION,
    CodeLayer.VALIDATION: AgentStage.GENERATE_CONTROLLERS,
    CodeLayer.CONTROLLER: AgentStage.COMPILE_CODE,
}


def _build_layer_prompt(state: AgentState, layer: CodeLayer) -> str:
    service_pkg = f"ai/agilery/{state.service_name.lower()}"
    example_pkg = f"ai/agilery/order/{layer.value}"

    prior_layers = LAYER_CONTEXT_MAP.get(layer, [])
    prior_section = ""
    if prior_layers:
        pointers = "\n".join(
            f"- src/main/java/{service_pkg}/{prev.value}/ (already generated, read it before writing this layer)"
            for prev in prior_layers
        )
        prior_section = f"\n# Previously generated layers to read first\n{pointers}\n"

    guideline = LAYER_PROMPT_MAP[layer.value].format(service_name=state.service_name.lower())

    return f"""# Requirements
{state.requirements}

{guideline}
{prior_section}
# Example context
Read the {layer.value} example classes under `templates/examples/order-service/src/main/java/{example_pkg}/`
(a sibling directory made readable to you for this run) for the expected patterns, imports and
annotations before writing anything. Do not copy them verbatim — adapt them to these requirements.

{FINAL_MESSAGE_INSTRUCTION}
"""


async def generate_layer_node(
    state: AgentState, session: ClaudeCodeSession, layer: CodeLayer
) -> AgentState:
    print("\n" + "=" * 60)
    print(f"STAGE: GENERATE_{layer.value.upper()} (Claude Code)")
    print("=" * 60 + "\n")

    session.set_scope(mode="layer", layer=layer.value)
    prompt = _build_layer_prompt(state, layer)
    result = await session.run_turn(prompt)

    if result.denied:
        print(f"  scope guard denied {len(result.denied)} tool call(s):")
        for d in result.denied:
            print(f"    - {d}")

    summary = extract_summary(result.text)
    print(f"  files touched: {result.files_touched}")
    print(f"  summary: {summary}")

    state.layers_completed.append(layer)
    state.current_layer = layer
    state.layer_contexts[layer] = LayerContext(
        layer=layer, files_touched=result.files_touched, summary=summary
    )
    state.claude_session_id = result.session_id
    state.total_input_tokens += result.usage.input_tokens
    state.total_output_tokens += result.usage.output_tokens
    state.total_cache_read_tokens += result.usage.cache_read_tokens
    state.total_cache_creation_tokens += result.usage.cache_creation_tokens

    state.current_stage = _NEXT_STAGE_AFTER_LAYER[layer]
    return state


def _build_fix_prompt(state: AgentState) -> str:
    errors_summary = "\n".join(
        f"[{i + 1}] {err.file}:{err.line}:{err.column} - {err.error_type}: {err.message}"
        for i, err in enumerate(state.compilation_errors[:50])
    )
    errors_by_file = "\n".join(
        f"\n{file} ({len(errs)} errors):\n"
        + "\n".join(f"  Line {e.line}: {e.message[:150]}" for e in errs[:10])
        for file, errs in state.compilation_errors_by_file.items()
    )

    return f"""Fix the Maven compilation errors below in this Java Spring Boot microservice.

# Guidelines
- Read each affected file (and anything it depends on) before editing it.
- Fix ALL related errors together — don't fix one file in isolation if the errors are related
  across files (e.g. a mapper and the DTO/entity it maps).
- Ensure imports, method signatures and generic types stay consistent across layers.
- If a file is a genuine duplicate or is no longer needed, delete it with a scoped
  `Bash(rm src/main/java/...)` or `Bash(rm src/test/java/...)` command — nothing else.

# Service name
{state.service_name}

# Compilation errors ({len(state.compilation_errors)} total)
{errors_summary}

# Errors grouped by file
{errors_by_file}

{FINAL_MESSAGE_INSTRUCTION}
"""


async def fix_compile_error_node(state: AgentState, session: ClaudeCodeSession) -> AgentState:
    if state.current_stage != AgentStage.FIX_COMPILE_ERROR:
        return state

    print("\n" + "=" * 60)
    print(f"STAGE: FIX_COMPILE_ERROR (attempt {state.compilation_attempt}, Claude Code)")
    print("=" * 60 + "\n")

    session.set_scope(mode="fix")
    prompt = _build_fix_prompt(state)
    result = await session.run_turn(prompt)

    if result.denied:
        print(f"  scope guard denied {len(result.denied)} tool call(s):")
        for d in result.denied:
            print(f"    - {d}")

    print(f"  files touched: {result.files_touched}")
    print(f"  summary: {extract_summary(result.text)}")

    state.claude_session_id = result.session_id
    state.total_input_tokens += result.usage.input_tokens
    state.total_output_tokens += result.usage.output_tokens
    state.total_cache_read_tokens += result.usage.cache_read_tokens
    state.total_cache_creation_tokens += result.usage.cache_creation_tokens

    state.current_stage = AgentStage.COMPILE_CODE
    return state


__all__ = [
    "copy_skeleton_node",
    "compile_code_node",
    "run_test_node",
    "generate_layer_node",
    "fix_compile_error_node",
    "LAYER_ORDER",
]
