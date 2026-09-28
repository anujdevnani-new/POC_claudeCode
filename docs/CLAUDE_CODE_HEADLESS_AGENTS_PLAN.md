# Claude Code (Headless) as the Agentic Layer: Feasibility & Design

**Status:** Design only. Not implemented yet.
**Date:** 2026-09-25
**Scope of phase 1:** Java generator and Java maintainer, behind a feature flag. The Python agents follow the same pattern afterwards.

---

## 1. Context

Today the generator and maintainer are custom LangGraph/LangChain agents. The orchestration (layer order, HITL, checkpoint/resume, abort, compile, test, package, deploy) is solid. The LLM "brain" has structural weaknesses:

- **Generator layer nodes are tool-less.** The LLM returns `=== FILE: path ===` text, which is regex-parsed and written to disk (`src/utils/file_parser.py`). Deletions never work because `parse_deleted_files` (L170) matches `=== FILE:`, not `=== DELETED FILE:`.
- **Lossy context.** Previous-layer source is passed through LLMLingua compression (`utils/prompt_optimizer.py`) before it is sent to the model.
- **Compile-fix prompts are hand-assembled** (`nodes_v2_optimized.py` L393–439) and limited to 50 errors and a curated file subset.
- **The maintainer's tools reimplement Claude Code's built-ins** (read, edit, grep, shell, todos). They resolve paths through the process-global `os.environ["PROJECT_ROOT"]` (`graph/plan_graph.py` L186), which races when concurrent projects run in one task.
- **Maintainer "RAG" is not semantic.** `rag/code_index_store.py` builds an `InMemoryStore()` with no embedding index, so `store.search` returns the first N items and ignores the query.
- **Maintainer HITL mid-task is dead code.** No tool calls `interrupt()`, so `approval_manager._collect_decision` never runs.

**Goal:** power the agentic layer with Claude Code in headless mode, using the Python **Claude Agent SDK** (`claude-agent-sdk`). The aim is better accuracy, tool usage and performance, while **retaining our prompt guidelines** and the **layer-by-layer generation flow**.

## 2. Feasibility verdict

**Feasible.** Recommended approach: **hybrid**.

- **LangGraph keeps orchestration.** It owns the node order, retry limits, HITL gates, checkpoint/resume, abort, and the deterministic build and deploy steps.
- **Claude Code replaces only the LLM "worker" nodes:**
  - the 7 generator layer nodes;
  - the generator compile-fix node;
  - the maintainer's inner CodeAct loop.

Every current contract has an SDK equivalent:

| Need | Current mechanism | Claude Agent SDK equivalent |
|---|---|---|
| Per-project working dir | `os.environ["PROJECT_ROOT"]` (global) | `ClaudeAgentOptions(cwd=output_dir)`, per session, no race |
| Our guidelines | Inline prompts in `prompts_v2.py`, `java_maintainer.py` | `system_prompt={"type":"preset","preset":"claude_code","append": ...}` plus the user-turn prompt |
| File ops | Regex parser / custom `@tool`s | Built-in `Read`, `Write`, `Edit`, `MultiEdit`, `Glob`, `Grep` |
| Planning | `write_todos` (no-op echo) | Built-in `TodoWrite` |
| Scope enforcement | None (generator), prompt only (maintainer) | `PreToolUse` hooks (allow/deny with reason) |
| Syntax check before save | javalang in `tools/file_tools.py` | `PostToolUse` hook that runs javalang and returns errors as feedback |
| Domain tools (memory, HITL, compile) | LangChain `@tool` | In-process MCP: `@tool` + `create_sdk_mcp_server` |
| Multi-agent (investigate, review) | None | `agents={name: AgentDefinition(...)}` plus the `Task` tool |
| Progress events | `emit_tool_notification` | Stream `AssistantMessage` `ToolUseBlock`s, or hooks, mapped to `AgentStage` |
| Billing | LangChain callback `PerCallCreditDeductionHandler` | `AssistantMessage` / `ResultMessage.usage` (input, output, cache read/creation) |
| Abort | Task cancel + `process_registry.kill_all` | `client.interrupt()` + kill the CLI process group |
| Resume | LangGraph Postgres checkpoint | Checkpoint (unchanged) + `resume=session_id` with the session store on EFS |
| Provider | `ChatBedrock` | `CLAUDE_CODE_USE_BEDROCK=1` + `ANTHROPIC_MODEL` |

**Unchanged:** `BaseAgent.invoke/resume`, `ChatService.process_message` / `resume_generation`, the websocket protocol, `AgentStrategy` routing, `InitialAgentState`.

---

## 3. Target architecture

```
ChatService ──► AgentStrategy ──► JavaGenerator / JavaMaintainer (unchanged entry points)
                                        │
                              LangGraph outer graph (unchanged order, HITL, checkpoints, abort)
                                        │
                  ┌─────────────────────┴──────────────────────┐
     deterministic nodes (unchanged)            LLM worker nodes ──► AGILERY_AGENT_ENGINE?
     copy_skeleton, compile_code, run_test,         │                     │
     run_quality, package, SBOM, S3, ECS,       langgraph (today)    claude_code
     sanity, refresh_api_preview                                          │
                                                               ClaudeCodeSession (new)
                                                               ├─ hooks (scope, bash, javalang, progress, credits)
                                                               ├─ in-process MCP tools (memory, ask_user, compile)
                                                               ├─ subagents (investigator, reviewer)
                                                               └─ usage → CreditsService
```

### New shared module: `src/agents/claude_code/`

| File | Responsibility |
|---|---|
| `session.py` | `ClaudeCodeSession`: wraps `ClaudeSDKClient`; builds options (`cwd`, system prompt, tools, hooks, MCP, agents, env); `run_turn(prompt) -> TurnResult` (final text, files touched, usage, session_id); abort polling; process registration |
| `hooks.py` | Path/layer guard, Bash guard, javalang syntax check, `AgentStage` progress mapping, credit gate |
| `mcp_tools.py` | `save_to_memory`, `recall_from_memory`, `ask_user`, `run_java_compile` (wrapping existing code) |
| `subagents.py` | `AgentDefinition`s: `code-investigator`, `code-reviewer` |
| `billing.py` | Maps SDK usage to the existing credit pipeline |
| `env.py` | Builds the minimal env for the CLI subprocess (Bedrock, config dir, telemetry off) |

### Feature flag

`AGILERY_AGENT_ENGINE=langgraph|claude_code` (default `langgraph`), read in `src/config/env_config.py`.

The recommendation is to branch **inside the LLM nodes**, not to add new agent classes. That way the graphs, checkpoint namespaces, routing and tests are reused unchanged, and rollback is a single env var.

### Sketch of `ClaudeCodeSession`

```python
class ClaudeCodeSession:
    def __init__(self, state, *, system_append: str, allowed_tools: list[str],
                 hooks: dict, mcp_tools: list, agents: dict | None = None,
                 session_id: str | None = None, max_turns: int = 40): ...

    async def run_turn(self, prompt: str) -> TurnResult:
        # 1. credits_service.ensure_has_credits (pre-check)
        # 2. client.query(prompt); iterate receive_response()
        #    - ToolUseBlock → notify_stage(AgentStage.X, save_to_db=False)
        #    - AssistantMessage.usage → billing.deduct(...)
        #    - poll ws_manager.is_abort_requested → client.interrupt(); kill pgid
        # 3. ResultMessage → session_id, is_error, final text, files touched
```

---

## 4. Java generator mapping (`src/agents/java/generator/`)

### What stays exactly as it is

- The `graph_v2.py` node order and conditional edges. Layer order Entity → DTO → Repository → Mapper → Service → Validation → Controller is **enforced by the graph, not by the model**.
- The validate, refine, confirm and plan/review HITL nodes (`utils/hitl._ask_user`).
- `copy_skeleton`, `compile_code`, `run_test`, `run_quality`, `package_code`, `upload_sbom`, `upload_jar_file_to_s3`, `create_ecs_task` and `run_api_preview_sanity` (`nodes_v2.py`).
- `max_compile_retries=3` and the compile → fix → compile loop.
- The post-SERVICE deterministic steps: `@Value` extraction into `application.yml` and environment variable registration.
- Checkpointing (`checkpoint.py`) and the abort runner (`agents/common/generation_runner.py`).
- Non-layer single-shot LLM calls stay on `get_llm()`: refine, legitimacy, service-name inference, OpenAPI description, POM update and final summary. They are structured one-shot calls where an agent loop adds nothing.

### What changes

**`generate_layer_node_optimized`** (`nodes_v2_optimized.py` L221) becomes one Claude Code turn per layer. All layers of one generation run share **one session**, resumed via a new `AgentState.claude_session_id` field. Each layer therefore sees what it built earlier without re-injecting context, and prompt caching covers the shared prefix.

| Aspect | Today | Claude Code |
|---|---|---|
| System prompt | `SYSTEM_PROMPT` | preset `claude_code` + append `SYSTEM_PROMPT` **with the OUTPUT FORMAT block removed**. All architectural rules are retained verbatim |
| Layer guidelines | `GENERATE_<LAYER>_PROMPT.format(service_name)` | **Same constant, unchanged**, with only the OUTPUT FORMAT block removed. Sent as the user turn |
| Example context | Filtered + comment-stripped + LLMLingua-compressed text | Pointer: "Read the `<layer>` examples under `templates/examples/order-service/src/main/java/...`" (the model reads the real files) |
| Previous-layer context | Formatted text of earlier layers | Already on disk and in the session; the prompt lists the package paths |
| Output | `=== FILE ===` text → regex → write | Model calls `Write` / `Edit` directly |
| Tools | none | `Read, Glob, Grep, Write, Edit, MultiEdit, TodoWrite` (no Bash; the graph owns compile) |
| Scope | Not enforced | `PreToolUse` hook: Write/Edit only under `ai/agilery/<svc>/<layer>/` (plus `enumeration/` for ENTITY and `config/` for SERVICE); deny any path in `constants/exclusion_files.py` and all skeleton classes |
| State update | Parsed file list | After the turn, diff the tree (mtime/hash snapshot) to fill `generated_files` and `layer_contexts[layer]` |
| Summary | Text after `=== SUMMARY ===` | The prompt asks for a final message starting with `SUMMARY:`; take that, truncated to 500 characters |
| Turn cap | n/a | `max_turns` ≈ 30 per layer |

**`fix_compile_error_node_optimized`** (L321) becomes a turn in the **same session**:
- **Prompt:** the parsed `[ERROR]` lines from `utils/java_compiler.parse_compilation_errors`, grouped by file, plus the existing fix guidance from L393–439, retained as text.
- **Tools:** Read/Grep/Glob/Edit/MultiEdit/Write; deleting a file is allowed via a scoped `Bash(rm src/...)` rule.
- **Scope:** anything under `src/`, but skeleton files stay denied.
- The graph still runs `compile_code` afterwards and still enforces the 3-attempt limit.

**Can be removed after the A/B comparison:** `utils/file_parser.py`, the OUTPUT FORMAT blocks, the LLMLingua path, and the context-injection parts of `prompt_optimizer.py` / `prompt_cache.py`.

---

## 5. Java maintainer mapping (`src/agents/java/maintainer/`)

This is where Claude Code gives the biggest win. Its native tool loop, Grep/Glob navigation and subagents directly replace hand-rolled tools.

### What stays

- The outer `graph/plan_graph.py`: validate → refine → confirm → (plan ↔ review) → run agent. Also the relevance gate against the `SERVICE_SUMMARY` memory.
- The backup zip before the run and restore on abort (`agents/common/maintainer_backup.py`).
- The post-run compile validation loop (up to 3 tries) and `refresh_api_preview` (`main.py` L140–291).
- The long-term memory table and repository (`maintainer_memories`, pgvector).

### What changes

The inner CodeAct agent (`agents/base_agent.build_codeact_agent` + `human_approval/approval_manager.run_with_hitl`) is replaced by a `ClaudeCodeSession`.

**Tools**

| Current tool | Claude Code replacement |
|---|---|
| `read_file`, `read_file_with_lines` | `Read` |
| `write_file` | `Write` + javalang `PostToolUse` hook |
| `replace_file_content`, `multi_replace_file_content` | `Edit`, `MultiEdit` + javalang hook |
| `ast_insert_field`, `ast_insert_method` | `Edit` (anchored on the last field or method) + javalang hook |
| `list_files`, `get_file_structure`, `create_directory` | `Glob`, `Bash(ls/mkdir)`; `Write` creates dirs |
| `search_codebase`, `search_code_context` (RAG) | `Grep`, `Glob`, and the `code-investigator` subagent |
| `write_todos` | `TodoWrite` |
| `run_shell` | `Bash` (guarded by a hook) |
| `run_java_compile` | MCP `run_java_compile` (wraps the existing function; registered in `process_registry` so abort kills it) |
| `save_to_memory`, `recall_from_memory` | MCP tools wrapping `repository/maintainer_memory_repository.py` + `memory/embeddings.embed_text` |
| *(none; HITL dead)* | MCP `ask_user` wrapping `utils/hitl._ask_user` (real mid-task clarification over the existing websocket `request_input`) |
| `execute_python`, `java_class_template`, `finish_task` | Dropped (the final assistant message is the summary) |

**System prompt:** the preset `claude_code`, plus an append made of:
1. The existing `java_maintainer.py` prompt. The EXECUTION WORKFLOW, JAVA STANDARDS and ABSOLUTE RULES are retained, with **only the tool names remapped** (e.g. "use `read_file_with_lines`" becomes "use `Read`", "never grep via `run_shell`" becomes "use `Grep`, not Bash grep").
2. **The generator's layer guidelines** (`SYSTEM_PROMPT` + `LAYER_PROMPT_MAP` from `prompts_v2.py`). Code the maintainer adds then follows the same rules the generator used; today the maintainer prompt lacks them.
3. Project facts: service name, base package and the path to the git-connected repo.

**Subagents** (`agents=`)

| Name | Tools | Model | Purpose |
|---|---|---|---|
| `code-investigator` | Read, Grep, Glob | Small/fast model (e.g. Haiku 4.5), if IAM allows | Locate the affected files across layers and report the call chain before any edit |
| `code-reviewer` | Read, Grep, Glob, Bash(`git diff`) | Main model | Review the diff against the layer guidelines and ABSOLUTE RULES; the main agent must address its findings before finishing |

Added to the appended prompt: "Investigate with `code-investigator` → plan with TodoWrite → edit → `run_java_compile` → review with `code-reviewer` → save learnings to memory."

The existing outer compile loop still runs afterwards as a safety net.

---

## 6. Hooks

| Hook | Matcher | Behaviour |
|---|---|---|
| PreToolUse: path guard | `Write\|Edit\|MultiEdit\|Read` | Deny any path outside `cwd` (and outside `templates/examples` for Read). In generator mode, also deny paths outside the current layer's package and skeleton/exclusion files. The deny reason is returned to the model |
| PreToolUse: Bash guard | `Bash` | Allowlist: `./mvnw ...`, `ls`, `mkdir`, `git diff/status`, scoped `rm`. Deny network tools, `git push`, `rm -rf` outside `cwd`, env dumps |
| PreToolUse: credit gate | `*` (every N calls) | `credits_service.ensure_has_credits`; on failure, deny and set a flag so the session interrupts and raises `InsufficientCreditsError` (already handled in `chat_service.py` L759) |
| PostToolUse: javalang | `Write\|Edit\|MultiEdit` on `*.java` | Parse with javalang; on a syntax error return `additionalContext` with the error so the model fixes it immediately |
| PostToolUse / stream: progress | `*` | Map to `AgentStage` and call `utils/agent_messaging.notify_stage(save_to_db=False)`: Read→READING_FILE, Write→WRITING_FILE, Edit/MultiEdit→EDITING_FILE, Grep/Glob→SEARCHING_CODEBASE, TodoWrite→PLANNING, `run_java_compile`→COMPILING_CODE, Bash→RUNNING_COMMAND, memory tools→SAVING_/RECALLING_MEMORY, `ask_user`→REQUESTING_CLARIFICATION |

---

## 7. Cross-cutting integration

### Billing (`src/service/credits_service.py`)
- **Pre-check:** before every `run_turn`, plus the PreToolUse credit gate.
- **Deduction:** for each `AssistantMessage` with usage (or once per `ResultMessage` if per-message usage isn't available), feed `input_tokens`, `output_tokens`, `cache_read_input_tokens` and `cache_creation_input_tokens` into the same `compute_credit_usage → _deduct_credits → credit_usage_logs_service.add_log` path used by `utils/per_call_credit_tracker.py`. Extract that logic into a callable helper both paths share.
- Subagent usage on a different model needs its own `model_pricing` row.
- **Known trade-off:** there is no "refuse before the HTTP call" hook, so a run can overshoot by at most one model turn.

### Abort
- `ClaudeCodeSession` polls `ws_manager.is_abort_requested(project_id)` every 0.2 s, matching `generation_runner.py`.
- On abort it calls `client.interrupt()`, then SIGKILLs the CLI process group registered in `utils/process_registry`.
- Existing cleanup then runs: generator artifact cleanup, or the maintainer backup restore.

### Resume
- The LangGraph checkpoint still decides **which node** to resume.
- Claude session transcripts go to EFS via `CLAUDE_CONFIG_DIR=$AGILERY_EFS_DIRECTORY_PATH/.claude/<project_id>`, and `claude_session_id` is stored in the checkpointed state. On resume the node passes `resume=session_id`.
- If the transcript is missing, the fallback is a fresh session. Files on disk are the source of truth, so this is safe.

### Isolation and security
- `setting_sources=[]`: no user, project or local settings or CLAUDE.md leak into runs.
- `permission_mode="acceptEdits"`; deny rules are enforced by hooks. Use `bypassPermissions` only if the hooks cover everything.
- The CLI subprocess gets a **minimal env**: Bedrock/AWS vars, the config dir and telemetry flags only. It gets **no** DB passwords, JWT secrets or Dependency-Track keys.
- `DISABLE_AUTOUPDATER=1`, `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC=1`.

### Provider env

| Var | Value |
|---|---|
| `CLAUDE_CODE_USE_BEDROCK` | `1` |
| `AWS_REGION` | existing |
| `ANTHROPIC_MODEL` | `$AGILERY_MODEL_NAME` (Sonnet 4.5 inference profile today) |
| `ANTHROPIC_SMALL_FAST_MODEL` | A Haiku 4.5 inference profile (needs IAM), or the same Sonnet profile |
| `CLAUDE_CONFIG_DIR` | `$AGILERY_EFS_DIRECTORY_PATH/.claude/<project_id>` |

---

## 8. Infra changes

- **Image (`Dockerfile`):** add `claude-agent-sdk` to `requirements.txt` (pinned). Verify that the wheel bundles a **linux-arm64** CLI. If it doesn't, install Node 18+ and a pinned `@anthropic-ai/claude-code`. Add `git` (for context and `git diff` in the reviewer) and `ripgrep` if it isn't bundled.
- **IAM (`terraform/infra/ecs_task_role.tf` L147–157):** allow Bedrock invoke on the small/fast model (Haiku 4.5 inference profile + foundation model), or point `ANTHROPIC_SMALL_FAST_MODEL` at Sonnet.
- **Task size:** prod is **512 CPU / 1 GB** (`env_prd.tfvars`), which is too small for a Node CLI plus mvnw/JVM per concurrent run. Suggest at least 2 vCPU / 4 GB, sized by the expected number of concurrent runs per task. Dev and qa (4 vCPU / 8 GB) are fine.
- **Env vars:** add `AGILERY_AGENT_ENGINE` and the provider vars above to `terraform/{blue,green}/main.tf`.

---

## 9. Guidelines retention (explicit)

| Guideline source | How it is retained |
|---|---|
| `prompts_v2.SYSTEM_PROMPT` | Appended to the Claude Code preset, verbatim minus the OUTPUT FORMAT block |
| `GENERATE_*_PROMPT` (7 layers) | Sent verbatim as the layer turn, minus the OUTPUT FORMAT block |
| `FIX_COMPILATION_ERROR_SYSTEM_PROMPT` + inline fix guidance | Sent as the fix turn text |
| `java_maintainer.py` system prompt | Appended verbatim, with tool names remapped |
| `LAYER_PROMPT_MAP` | Also appended to the maintainer (new) |
| Examples (`templates/examples/order-service`) and skeleton (`templates/skeleton`) | Read by the agent directly instead of pre-digested |
| `rules/python/*.md` (phase 3) | Passed as Read pointers or appends, unchanged |

Prompt edits are limited to removing the output-format blocks and renaming tools. Per CLAUDE.md, these prompts encode strict ordering and tool rules, so any other change should be reviewed on its own.

---

## 10. Rollout

1. **Phase 1: Java maintainer** on Claude Code behind the flag. This has the highest payoff and the smallest blast radius: the outer graph, backup, compile loop and preview refresh are unchanged.
2. **Phase 2: Java generator** layer nodes and the compile-fix node.
3. **Phase 3: Python maintainer and generator.** Same pattern, with `run_python_compile` / poetry equivalents and `rules/python/*.md`.
4. **Phase 4:** A/B comparison (compile-success rate on the first attempt, fix rounds, tokens and credits per run, wall-clock), then remove the dead code: file parser, LLMLingua, CodeAct tools, RAG.

---

## 11. Risks and trade-offs

| Risk | Mitigation |
|---|---|
| Higher token use (the agent reads files instead of getting pruned context) | Automatic prompt caching; one session across layers; `max_turns` caps; the investigator runs on a small model; measure in the A/B |
| Higher latency per layer (multi-turn loop) | Offset by fewer compile-fix rounds; parallel reads within a turn |
| The model writes outside its layer or edits skeleton files | PreToolUse path guard (deterministic deny) |
| Billing overshoot by one turn | Per-turn and per-tool credit gate |
| SDK/CLI version drift changes behaviour | Pin the SDK and CLI versions; upgrade deliberately |
| Session transcripts on EFS grow | Delete `CLAUDE_CONFIG_DIR/<project_id>` sessions on COMPLETED/CANCELLED |
| Memory per run (Node + JVM) | Task sizing (§8); existing one-run-per-project advisory lock |

---

## 12. Pre-existing bugs found during exploration (fix regardless)

- `utils/file_parser.py` L170: the `parse_deleted_files` regex never matches `=== DELETED FILE:`.
- `nodes_v2.py` ~L1053: a failed S3 upload's `FAILED` stage is overwritten with `CREATE_ECS_TASK`.
- `agents/java/maintainer/graph/plan_graph.py` L186: the global `PROJECT_ROOT` env var races under concurrency.
- `api/chat_api.py` L215–216: `parsed.pop("mode")` raises `KeyError` when `mode` is absent, and the whole raw JSON becomes the message.
- Maintainer `interrupt()`/clarify path is dead; maintainer subprocesses (`compilation_tools._run`, `execute_python`) are not registered, so abort can't kill them.
- The prompt-cache comment claims the system prompt is cache-marked on the layer path; it isn't (`utils/prompt_cache.py`).
- Python generator: `clarify_requirement` (`validate_requirement.py` L82), `fix_test_errors` and code review are all unreachable. Quality failure still packages.
- Python maintainer RAG: `rag_tools.py` reads `text` but `retriever.py` reads `content`.
- `AGILERY_MAINTAINER_BACKUP_DIR=/efs/agilery/backup` is not on the EFS mount, so backups land on ephemeral disk.

---

## 13. Verification plan (for implementation)

1. **Confirm SDK specifics** against the current Claude Agent SDK docs before coding: the bundled CLI on linux-arm64, the `AssistantMessage` / `ResultMessage.usage` fields, `interrupt()`, `resume`, `agents=`, the hook return schema and the Bedrock env vars.
2. **Unit tests** in `tests/agents/claude_code/`, mocking `ClaudeSDKClient`: hook allow/deny decisions, stage mapping, billing deduction from usage, abort → interrupt + kill, resume with `session_id`.
3. **Local maintainer run:** `AGILERY_AGENT_ENGINE=claude_code python src/main_maintainer.py` against a generated order-service. Check that:
   - edits compile through the existing post-run loop;
   - websocket stages arrive (`scripts/ws_chat_test_client.py`);
   - `credit_usage_logs` rows are written;
   - abort restores the backup.
4. **Local generator run:** `python src/main_v2.py generate -s order ...` with the flag on. Check that each layer only writes inside its package, compile passes within the 3 attempts, and tests, packaging and the preview sanity check pass.
5. **A/B** on a fixed set of requirement prompts: compare the metrics from §10.
