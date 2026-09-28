"""LangGraph wiring — section 2/4 of the plan: LangGraph keeps orchestration (node order,
compile-retry loop); Claude Code only replaces the 7 layer-generation nodes and the
compile-fix node.

The graph is a straight-line pipeline, same shape as the main repo's
`agents/java/generator/graph_v2.py` minus the HITL/plan-mode fork and the S3/ECS/SBOM/package
tail (see README.md for what's excluded and why):

    copy_skeleton -> generate_entities -> generate_dtos -> generate_repositories ->
    generate_mappers -> generate_services -> generate_validation -> generate_controllers ->
    compile_code -> (fix_compile_error -> compile_code)* -> run_test -> END
"""

from functools import partial

from langgraph.graph import END, StateGraph

from agents.claude_code.session import ClaudeCodeSession
from nodes import (
    compile_code_node,
    copy_skeleton_node,
    fix_compile_error_node,
    generate_layer_node,
    run_test_node,
)
from state import LAYER_ORDER, AgentState, AgentStage


def _should_continue_after_compile(state: AgentState) -> str:
    if state.current_stage == AgentStage.RUN_TEST:
        return "run_test"
    if state.current_stage == AgentStage.FIX_COMPILE_ERROR:
        return "fix_compile_error"
    return END


def create_agent_graph(session: ClaudeCodeSession):
    workflow = StateGraph(AgentState)

    workflow.add_node("copy_skeleton", copy_skeleton_node)
    for layer in LAYER_ORDER:
        workflow.add_node(f"generate_{layer.value}", partial(generate_layer_node, session=session, layer=layer))
    workflow.add_node("compile_code", compile_code_node)
    workflow.add_node("fix_compile_error", partial(fix_compile_error_node, session=session))
    workflow.add_node("run_test", run_test_node)

    workflow.set_entry_point("copy_skeleton")

    layer_node_names = [f"generate_{layer.value}" for layer in LAYER_ORDER]
    workflow.add_edge("copy_skeleton", layer_node_names[0])
    for a, b in zip(layer_node_names, layer_node_names[1:]):
        workflow.add_edge(a, b)
    workflow.add_edge(layer_node_names[-1], "compile_code")

    workflow.add_conditional_edges(
        "compile_code",
        _should_continue_after_compile,
        {"run_test": "run_test", "fix_compile_error": "fix_compile_error", END: END},
    )
    workflow.add_edge("fix_compile_error", "compile_code")
    workflow.add_edge("run_test", END)

    return workflow.compile()
