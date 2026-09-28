"""Skeleton placeholder rewriting, copied from utils/file_parser.py in the main repo.

Only `fix_project_structure` is kept here — the `parse_generated_files` /
`parse_deleted_files` regex parsers are gone on purpose: Claude Code writes files directly
with its own Write/Edit tools, so there is no `=== FILE: ... ===` text to parse (this is the
whole point of section 4 of the design doc, and it also sidesteps the pre-existing
`parse_deleted_files` bug noted in section 12 of that doc, which never matched
`=== DELETED FILE:`).
"""

import os
import re
from typing import List


def fix_project_structure(project_path: str, service_name: str) -> List[str]:
    for folder_type in ["main/java", "test/java"]:
        old_dir = os.path.join(project_path, "src", folder_type, "ai", "agilery", "example")
        new_dir = os.path.join(
            project_path, "src", folder_type, "ai", "agilery", service_name.lower()
        )
        os.rename(old_dir, new_dir)

    old_package = "ai.agilery.example"
    new_package = f"ai.agilery.{service_name}"
    old_prefix = "Example"

    # Defensively strip a "_service"/"-service" suffix if present, so the artifactId/class
    # name/pom metadata don't end up doubled (e.g. "expense-service-service").
    display_name = service_name
    for suffix in ("_service", "-service"):
        if display_name.lower().endswith(suffix):
            display_name = display_name[: -len(suffix)]
            break
    new_prefix = display_name.capitalize()

    for root_path, _, files in os.walk(project_path):
        for file in files:
            file_path = os.path.join(root_path, file)

            if file.endswith(".java"):
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()

                updated_content = content.replace(old_package, new_package)

                if file in [
                    f"{old_prefix}ServiceApplication.java",
                    f"{old_prefix}ServiceApplicationTests.java",
                ]:
                    updated_content = updated_content.replace(old_prefix, new_prefix)

                updated_content = updated_content.replace(
                    f"{old_prefix}ServiceApplication", f"{new_prefix}ServiceApplication"
                )

                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(updated_content)

                if file == f"{old_prefix}ServiceApplication.java":
                    new_file = f"{new_prefix}ServiceApplication.java"
                    new_path = os.path.join(root_path, new_file)
                    os.rename(file_path, new_path)

                elif file == f"{old_prefix}ServiceApplicationTests.java":
                    new_file = f"{new_prefix}ServiceApplicationTests.java"
                    new_path = os.path.join(root_path, new_file)
                    os.rename(file_path, new_path)

            elif file == "pom.xml":
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()

                updated_content = (
                    content.replace(
                        "example-service", f"{display_name.lower().replace('_', '-')}-service"
                    )
                    .replace("<n>ExampleService</n>", f"<n>{new_prefix}Service</n>")
                    .replace(
                        "<description>Example microservice</description>",
                        f"<description>{new_prefix} microservice</description>",
                    )
                    .replace("ExampleService", f"{new_prefix}Service")
                    .replace(old_package, new_package)
                )
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(updated_content)

            elif file == "Dockerfile":
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()
                updated_content = content.replace("example", service_name.lower())
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(updated_content)

            elif file == "init.sql":
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()
                clean = (
                    re.sub(
                        r"[-_\s]?(java[-_\s]?service|python[-_\s]?service|api[-_\s]?service|service|svc|api)$",
                        "",
                        service_name.strip(),
                        flags=re.IGNORECASE,
                    )
                    .strip()
                    .lower()
                    .replace("-", "_")
                    .replace(" ", "_")
                )
                clean = re.sub(
                    r"[-_](java[-_]?service|python[-_]?service|service|svc|api)$", "", clean
                )
                clean = re.sub(r"_(service|svc|api)$", "", clean)
                schema_name = f"{clean}_svc" if clean else "example_svc"
                updated_content = re.sub(
                    r'CREATE SCHEMA IF NOT EXISTS\s+["\']?[^"\'\s;]+["\']?',
                    f'CREATE SCHEMA IF NOT EXISTS "{schema_name}"',
                    content,
                    flags=re.IGNORECASE,
                )
                if updated_content == content and "example" in content:
                    updated_content = content.replace("example", schema_name)
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(updated_content)

            elif file == "application.yml":
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()
                updated_content = content.replace(
                    "name: Example Service", f"name: {new_prefix} Service"
                )
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(updated_content)
