# Claude Code Headless Agents — Java Generator POC

Standalone proof-of-concept for section 4 ("Java generator mapping") of the design doc
`docs/CLAUDE_CODE_HEADLESS_AGENTS_PLAN.md` from the `agilery-agents` repo (copied here as
[`docs/CLAUDE_CODE_HEADLESS_AGENTS_PLAN.md`](docs/CLAUDE_CODE_HEADLESS_AGENTS_PLAN.md) for
reference). It answers one question: **can Claude Code, run headless via the Python
`claude-agent-sdk`, replace the LangChain LLM calls in the generator's layer-generation and
compile-fix nodes, while LangGraph keeps owning layer ordering and the compile-retry loop?**

This lives outside `agilery-agents` on purpose (per the request that created it) so the POC's
simplifications don't get mixed into the production codebase. Nothing here is wired back into
the main repo.

## What this is

A layer-by-layer Java Spring Boot microservice generator:

```
copy_skeleton -> generate_entities -> generate_dtos -> generate_repositories ->
generate_mappers -> generate_services -> generate_validation -> generate_controllers ->
compile_code -> (fix_compile_error -> compile_code)* -> run_test -> done
```

- **LangGraph** (`src/graph.py`) owns the node order and the compile/fix retry loop (capped by
  `--max-retries`), exactly like `agents/java/generator/graph_v2.py` in the main repo.
- **Claude Code**, via one shared `ClaudeCodeSession` (`src/agents/claude_code/session.py`)
  wrapping the Claude Agent SDK's `ClaudeSDKClient`, does the actual code generation: one turn
  per layer, plus one turn per compile-fix attempt, all on the **same session** (so prompt
  caching covers the shared prefix and later layers see what earlier layers wrote on disk
  without the graph re-injecting context — section 4 of the plan).
- The model writes files directly with its own `Write`/`Edit`/`MultiEdit` tools. There is no
  `=== FILE: ... ===` regex parser (`utils/file_parser.py` in the main repo) — that's the core
  bet this POC tests.

## What's implemented from the design doc

| Plan section | Status here |
|---|---|
| §3 `ClaudeCodeSession` (system prompt, hooks, tools, per-turn usage, session id) | Implemented — `agents/claude_code/session.py` |
| §4 Layer nodes → one Claude Code turn each, sharing a session | Implemented — `nodes.py::generate_layer_node` |
| §4 Compile-fix node → a turn in the same session | Implemented — `nodes.py::fix_compile_error_node` |
| §6 PreToolUse path/layer scope guard | Implemented — `agents/claude_code/hooks.py::make_scope_guard_hook` |
| §6 PreToolUse Bash guard (scoped `rm` only, fix mode only) | Implemented — `make_bash_guard_hook` |
| §6 PostToolUse javalang syntax check | Implemented — `make_javalang_check_hook` |
| §7 Billing usage extraction (input/output/cache tokens) | Implemented as accumulation on `AgentState`, not wired to a real credits ledger (there isn't one here) |
| §9 Guideline retention (`SYSTEM_PROMPT` / `GENERATE_*_PROMPT` verbatim minus OUTPUT FORMAT) | Implemented — `src/prompts.py` |
| §6 PostToolUse / stream progress → `AgentStage` | Simplified to console `print()`s (`nodes.py`) — no websocket/UI to map to here |

## What's deliberately left out, and why

The instruction that created this POC was: focus on the **Java generator only**, in a location
separate from the main repo. Beyond dropping the maintainer (§5 of the plan) entirely, the
generator itself is trimmed to what's needed to test the Claude-Code-as-worker bet, since the
rest requires production infrastructure this POC doesn't have and isn't testing:

- **No HITL / requirement refinement / plan mode.** `validate_requirements`, `refine_requirements`,
  `confirm_requirements`, and plan-mode all talk to a human over the production websocket
  (`utils/hitl.py`) and to an LLM for summarization. The POC takes requirements directly from
  the CLI argument.
- **No DB, project-service, S3, ECS, SBOM, or credits/billing integration.** `update_project_status_orchestrator`,
  `upload_jar_file_to_s3_node`, `create_ecs_task_node`, `run_api_preview_sanity_node`,
  `upload_sbom_node_v2`, and the credit-deduction path all depend on AWS resources and a
  Postgres DB that only exist in the deployed service. The pipeline here stops after
  `run_test`.
- **No `update_openapi_description` / `update_pom` one-shot LLM nodes.** These are unchanged,
  non-agentic LangChain calls per the plan (§4, "non-layer single-shot LLM calls stay on
  `get_llm()`") — reintroducing them would mean also copying `utils/llm.py` and provider
  config unrelated to the Claude Code question this POC is testing. `pom.xml` and
  `OpenApiConfig.java` are used as copied from the skeleton.
- **No crash-resume.** The plan's `claude_session_id` + `resume=` mechanism (§4, §7 "Resume")
  is represented by `AgentState.claude_session_id`, but a POC run is a single `asyncio.run()`
  call end to end — there's no crash/restart path to resume from.
- **No abort.** §7 "Abort" (poll `is_abort_requested`, `client.interrupt()`, kill the process
  group) needs the websocket/task-cancellation machinery this POC doesn't have.
- **Minimal env isolation.** The plan's §8 "Isolation and security" (a minimal subprocess env
  with no DB/JWT secrets) doesn't apply — there are no such secrets in this POC's environment
  to protect against. See `agents/claude_code/env.py`.

## Repo layout

```
templates/skeleton/                  copied from agilery-agents/templates/skeleton
templates/examples/order-service/    copied from agilery-agents/templates/examples/order-service
src/
  state.py            AgentState (pydantic) + CodeLayer/AgentStage enums — trimmed state_v2.py
  prompts.py           SYSTEM_PROMPT + 7 layer prompts, copied verbatim minus OUTPUT FORMAT blocks
  nodes.py             copy_skeleton / compile / test (deterministic) + generate_layer / fix_compile_error (Claude Code)
  graph.py             LangGraph wiring
  main.py              Typer CLI (generate, validate)
  constants/exclusion_files.py   copied verbatim — skeleton files no layer may touch
  utils/
    file_parser.py      only fix_project_structure kept (skeleton placeholder renaming)
    java_compiler.py    Maven compile, trimmed (no process registry / EFS)
    test_runner.py      Maven test, trimmed
  agents/claude_code/
    session.py           ClaudeCodeSession (wraps ClaudeSDKClient)
    hooks.py             scope guard, bash guard, javalang check
    env.py               subprocess env for the CLI
```

## Setup

```bash
cd claude_headless_poc
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

cp .env.example .env   # fill in ANTHROPIC_API_KEY, or the Bedrock vars
```

You need `java` on `PATH` (Java 17+; the skeleton's `mvnw` bootstraps its own Maven on first
run — no separate Maven install needed, but it does need network access the first time).

## Running

```bash
export PYTHONPATH=src
python src/main.py validate

python src/main.py generate \
  --service-name order \
  "Create an order management microservice: orders have line items, a status lifecycle ..." \
  --max-retries 3
```

Output goes to `generated/<service_name>_<timestamp>/` by default (`--output` to override).
Each layer's Claude Code turn prints the tools it called, files it touched, and its
`SUMMARY:` line; a denied tool call (wrong layer, skeleton file, disallowed Bash) is logged
inline as `scope guard denied ...`.

## Known limitations / things to check on a real run

- This was built and structurally dry-run (LangGraph wiring, the compile-retry loop, the
  scope-guard/javalang hooks) with a stubbed Claude Code session — **no real Claude Code turn
  has been executed against it yet**, since this environment has no Anthropic/Bedrock
  credentials configured. Run `python src/main.py validate` after setting up `.env`, then a
  real `generate` call, before drawing any conclusions about generation quality or compile
  success rate.
- `add_dirs=[example_dir]` is what makes the example service's package readable to the model
  outside `cwd`; if a Claude Agent SDK version changes that option's semantics, double-check
  `Read`/`Glob` on `templates/examples/order-service/...` still works from inside a run.
- The javalang check is best-effort: it swallows parser exceptions other than
  `JavaSyntaxError` (some modern Java syntax isn't in javalang's grammar), so it won't catch
  everything Maven's own compiler would.
