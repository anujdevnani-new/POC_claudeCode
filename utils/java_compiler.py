"""Maven compile + error parsing, trimmed from utils/java_compiler.py in the main repo.

Simplified for the POC: no `process_registry`/abort wiring (there is no abort feature here)
and no shared EFS `.m2` — each POC run uses a local Maven repo cache under
`<repo_root>/.cache/m2` so repeated runs reuse downloaded dependencies.
"""

import re
import subprocess
from pathlib import Path
from typing import List, Tuple

POC_ROOT = Path(__file__).resolve().parents[2]
MAVEN_REPO_LOCAL = POC_ROOT / ".cache" / "m2"


def compile_java_project(project_dir: str) -> Tuple[bool, str]:
    """Compile a Java Maven project. Returns (success, combined stdout+stderr)."""
    try:
        MAVEN_REPO_LOCAL.mkdir(parents=True, exist_ok=True)

        project_path = Path(project_dir)
        mvnw_path = next(project_path.rglob("mvnw"), None)
        if mvnw_path is None:
            return False, "Could not find mvnw in project directory or any subdirectory"

        compile_dir = mvnw_path.parent

        result = subprocess.run(
            ["./mvnw", "clean", "test-compile", f"-Dmaven.repo.local={MAVEN_REPO_LOCAL}"],
            cwd=compile_dir,
            capture_output=True,
            text=True,
            timeout=600,
        )

        output = result.stdout + "\n" + result.stderr
        return result.returncode == 0, output

    except subprocess.TimeoutExpired:
        return False, "Compilation timed out after 10 minutes"
    except Exception as e:
        return False, f"Compilation error: {str(e)}"


def parse_compilation_errors(output: str) -> List[dict]:
    """Parse Maven compilation output into a list of {file, line, column, error_type, message}."""
    errors = []

    error_pattern = r"\[ERROR\]\s+([^\[]+\.java):\[(\d+),(\d+)\]\s+([^:]+):\s*(.+)"
    for match in re.finditer(error_pattern, output):
        file_path, line, column, error_type, message = match.groups()
        errors.append(
            {
                "file": Path(file_path.strip()).name,
                "line": int(line),
                "column": int(column),
                "error_type": error_type.strip(),
                "message": message.strip(),
            }
        )

    if not errors:
        alt_pattern = r"\[ERROR\]\s+([^\[]+\.java):\[(\d+)(?:,(\d+))?\]\s+(.+)"
        for match in re.finditer(alt_pattern, output):
            file_path, line, column, message = match.groups()
            errors.append(
                {
                    "file": Path(file_path.strip()).name,
                    "line": int(line),
                    "column": int(column) if column else 0,
                    "error_type": "error",
                    "message": message.strip(),
                }
            )

    if not errors and "COMPILATION ERROR" in output:
        error_lines = [line for line in output.split("\n") if "[ERROR]" in line]
        for error_line in error_lines[:10]:
            errors.append(
                {
                    "file": "unknown",
                    "line": 0,
                    "column": 0,
                    "error_type": "compilation_error",
                    "message": error_line.replace("[ERROR]", "").strip(),
                }
            )

    return errors
