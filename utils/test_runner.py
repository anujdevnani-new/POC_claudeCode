"""Maven test runner, trimmed from utils/test_runner.py in the main repo (see
java_compiler.py's docstring for what was simplified and why)."""

import subprocess
from typing import Tuple

from utils.java_compiler import MAVEN_REPO_LOCAL


def run_tests(project_dir: str) -> Tuple[bool, str]:
    try:
        MAVEN_REPO_LOCAL.mkdir(parents=True, exist_ok=True)

        result = subprocess.run(
            ["./mvnw", "test", f"-Dmaven.repo.local={MAVEN_REPO_LOCAL}"],
            cwd=project_dir,
            capture_output=True,
            text=True,
            timeout=600,
        )

        output = result.stdout + "\n" + result.stderr
        return result.returncode == 0, output

    except subprocess.TimeoutExpired:
        return False, "Tests timed out after 10 minutes"
    except FileNotFoundError:
        return False, "mvnw not found. Ensure the skeleton's Maven wrapper is present."
    except Exception as e:
        return False, f"Test execution error: {str(e)}"
