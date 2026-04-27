#!/usr/bin/python3

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2026 Xyna GmbH, Germany
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
import xml.etree.ElementTree as ET


MAVEN_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
TARGET_FILENAMES = {"pom.xml", "third_parties.pom.xml"}


@dataclass(frozen=True)
class ArtifactKey:
    group_id: str
    artifact_id: str
    def display(self) -> str:
        return f"{self.group_id}:{self.artifact_id}"


@dataclass(frozen=True)
class MavenArtifact:
    key: ArtifactKey
    version: str
    file_path: Path


@dataclass(frozen=True)
class ParsePomResult:
    managed_dependencies: list[MavenArtifact]
    direct_dependencies: list[MavenArtifact]
    project_artifact: MavenArtifact | None


def find_text(element: ET.Element, xpath: str) -> str | None:
    child = element.find(xpath, MAVEN_NAMESPACE)
    if child is None or child.text is None:
        return None
    text = child.text.strip()
    return text or None


def resolve_project_text(root: ET.Element, field_name: str) -> str | None:
    direct_value = find_text(root, f"m:{field_name}")
    if direct_value is not None:
        return direct_value
    return find_text(root, f"m:parent/m:{field_name}")


def parse_artifact_key(element: ET.Element) -> ArtifactKey | None:
    group_id = find_text(element, "m:groupId")
    artifact_id = find_text(element, "m:artifactId")
    if group_id is None or artifact_id is None:
        return None

    return ArtifactKey(group_id=group_id, artifact_id=artifact_id)


def collect_versioned_artifacts(root: ET.Element, xpath: str, file_path: Path) -> list[MavenArtifact]:
    result: list[MavenArtifact] = []
    for dependency in root.findall(xpath, MAVEN_NAMESPACE):
        artifact_key = parse_artifact_key(dependency)
        version = find_text(dependency, "m:version")
        if artifact_key is None or version is None:
            continue
        result.append(MavenArtifact(key=artifact_key, version=version, file_path=file_path))
    return result


def parse_pom(file_path: Path) -> ParsePomResult:
    tree = ET.parse(file_path)
    root = tree.getroot()

    managed_dependencies = collect_versioned_artifacts(
        root, "m:dependencyManagement/m:dependencies/m:dependency", file_path
    )
    direct_dependencies = collect_versioned_artifacts(
        root, "m:dependencies/m:dependency", file_path
    )

    project_group_id = resolve_project_text(root, "groupId")
    project_artifact_id = resolve_project_text(root, "artifactId")
    project_version = resolve_project_text(root, "version")

    project_artifact: MavenArtifact | None = None
    if project_group_id and project_artifact_id and project_version:
        project_artifact = MavenArtifact(
            key=ArtifactKey(project_group_id, project_artifact_id),
            version=project_version,
            file_path=file_path,
        )

    return ParsePomResult(
        managed_dependencies=managed_dependencies,
        direct_dependencies=direct_dependencies,
        project_artifact=project_artifact,
    )


def iter_target_files(root_dir: Path) -> Iterable[Path]:
    for file_path in root_dir.rglob("*.xml"):
        if file_path.name in TARGET_FILENAMES:
            yield file_path


def check_consistency(root_dir: Path) -> list[str]:
    """ Checks:
    - duplicate dependencyManagement entries
    - project artifact version vs dependencyManagement version
    - direct dependency version vs project artifact version"""
    inconsistencies: list[str] = []
    managed_dependencies: dict[ArtifactKey, MavenArtifact] = {}
    direct_dependencies: list[MavenArtifact] = []
    project_artifacts: dict[ArtifactKey, MavenArtifact] = {}

    for file_path in sorted(iter_target_files(root_dir)):
        try:
            parse_result = parse_pom(file_path)
        except ET.ParseError as exc:
            inconsistencies.append(f"{file_path}: invalid XML: {exc}")
            continue
        except OSError as exc:
            inconsistencies.append(f"{file_path}: unable to read file: {exc}")
            continue

        for managed_dependency in parse_result.managed_dependencies:
            existing = managed_dependencies.get(managed_dependency.key)
            if existing is None:
                managed_dependencies[managed_dependency.key] = managed_dependency
                continue
            inconsistencies.append(
                "duplicate dependencyManagement entry for "
                f"{managed_dependency.key.display()}: "
                f"{existing.file_path} declares version {existing.version}, "
                f"{managed_dependency.file_path} declares version {managed_dependency.version}"
            )

        direct_dependencies.extend(parse_result.direct_dependencies)

        if parse_result.project_artifact is not None:
            project_artifacts[parse_result.project_artifact.key] = parse_result.project_artifact

    for project_artifact in project_artifacts.values():
        managed_dependency = managed_dependencies.get(project_artifact.key)
        if managed_dependency is None:
            continue
        if managed_dependency.version == project_artifact.version:
            continue
        inconsistencies.append(
            f"version mismatch for {project_artifact.key.display()}: "
            f"{project_artifact.file_path} declares project version {project_artifact.version}, "
            f"but dependencyManagement in {managed_dependency.file_path} declares version {managed_dependency.version}"
        )

    for direct_dependency in direct_dependencies:
        matching_project = project_artifacts.get(direct_dependency.key)
        if matching_project is None:
            continue
        if matching_project.version == direct_dependency.version:
            continue
        inconsistencies.append(
            f"version mismatch for {direct_dependency.key.display()}: "
            f"{direct_dependency.file_path} declares dependency version {direct_dependency.version}, "
            f"but artifact definition in {matching_project.file_path} declares project version {matching_project.version}"
        )

    return inconsistencies


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Scan a directory for pom.xml and third_parties.pom.xml files and "
            "check dependencyManagement consistency."
        )
    )
    parser.add_argument("directory", help="Path to the directory to scan")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    root_dir = Path(args.directory)
    if not root_dir.exists():
        print(f"Directory does not exist: {root_dir}", file=sys.stderr)
        return 2
    if not root_dir.is_dir():
        print(f"Path is not a directory: {root_dir}", file=sys.stderr)
        return 2
    print(f"Scanning {root_dir.resolve()}")
    inconsistencies = check_consistency(root_dir)
    for inconsistency in inconsistencies:
        print(inconsistency)
    print("Done")
    return 1 if inconsistencies else 0


if __name__ == "__main__":
    raise SystemExit(main())
