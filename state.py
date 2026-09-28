"""State schema for the Claude-Code-powered Java generator POC.

Deliberately trimmed from agents/java/generator/state_v2.py in the main repo:
no DB/websocket/S3/ECS/HITL/credits fields, since this POC only exercises the
layer-generation + compile-fix worker nodes described in section 4 of
docs/CLAUDE_CODE_HEADLESS_AGENTS_PLAN.md.
"""

from enum import Enum
from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class CodeLayer(str, Enum):
    """Code layers to generate in order (mirrors the main repo's CodeLayer)."""

    ENTITY = "entity"
    DTO = "dto"
    REPOSITORY = "repository"
    MAPPER = "mapper"
    SERVICE = "service"
    VALIDATION = "validation"
    CONTROLLER = "controller"


LAYER_ORDER: List[CodeLayer] = [
    CodeLayer.ENTITY,
    CodeLayer.DTO,
    CodeLayer.REPOSITORY,
    CodeLayer.MAPPER,
    CodeLayer.SERVICE,
    CodeLayer.VALIDATION,
    CodeLayer.CONTROLLER,
]


class AgentStage(str, Enum):
    COPY_SKELETON = "copy_skeleton"
    GENERATE_ENTITIES = "generate_entities"
    GENERATE_DTOS = "generate_dtos"
    GENERATE_REPOSITORIES = "generate_repositories"
    GENERATE_MAPPERS = "generate_mappers"
    GENERATE_SERVICES = "generate_services"
    GENERATE_VALIDATION = "generate_validation"
    GENERATE_CONTROLLERS = "generate_controllers"
    COMPILE_CODE = "compile_code"
    FIX_COMPILE_ERROR = "fix_compile_error"
    RUN_TEST = "run_test"
    COMPLETED = "completed"
    FAILED = "failed"


class CompilationError(BaseModel):
    file: str
    line: int
    column: int
    error_type: str
    message: str


class LayerContext(BaseModel):
    """What a Claude Code turn reported/produced for one layer."""

    layer: CodeLayer
    files_touched: List[str] = Field(default_factory=list)
    summary: str = ""


class AgentState(BaseModel):
    # Input
    service_name: str
    requirements: str
    output_dir: str
    template_dir: str
    example_dir: str

    # Claude Code session — one session per generation run, resumed turn to turn
    # so every layer sees what earlier layers built without re-injecting context.
    claude_session_id: Optional[str] = None

    # Layer-by-layer generation
    current_layer: Optional[CodeLayer] = None
    layers_completed: List[CodeLayer] = Field(default_factory=list)
    layer_contexts: Dict[CodeLayer, LayerContext] = Field(default_factory=dict)

    # Compilation state
    max_compile_retries: int = 3
    compilation_attempt: int = 0
    compilation_errors: List[CompilationError] = Field(default_factory=list)
    compilation_errors_by_file: Dict[str, List[CompilationError]] = Field(default_factory=dict)
    compilation_output: str = ""
    compilation_success: bool = False

    # Test state
    test_output: str = ""
    test_success: bool = False

    # Workflow state
    current_stage: AgentStage = AgentStage.COPY_SKELETON
    error_message: Optional[str] = None

    # Usage/billing (§7 of the plan) — accumulated across all Claude Code turns.
    total_input_tokens: int = 0
    total_output_tokens: int = 0
    total_cache_read_tokens: int = 0
    total_cache_creation_tokens: int = 0

    class Config:
        arbitrary_types_allowed = True
