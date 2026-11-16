#!/usr/bin/python3

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

import argparse
from fnmatch import fnmatchcase
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


MAVEN_NAMESPACE = "http://maven.apache.org/POM/4.0.0"


def read_dependencies(file_path: Path, managed: bool) -> set[str]:
    root = ET.parse(file_path).getroot()
    if root.tag == f"{{{MAVEN_NAMESPACE}}}project":
        prefix = f"{{{MAVEN_NAMESPACE}}}"
    elif root.tag == "project":
        prefix = ""
    else:
        raise ValueError(f"{file_path}: expected a Maven project root element")

    tags = ["dependencies", "dependency"]
    if managed:
        tags.insert(0, "dependencyManagement")
    xpath = "/".join(prefix + tag for tag in tags)
    dependencies = set()
    for index, dependency in enumerate(root.findall(xpath), start=1):
        group_id = dependency.findtext(prefix + "groupId", "").strip()
        artifact_id = dependency.findtext(prefix + "artifactId", "").strip()
        if not group_id or not artifact_id:
            raise ValueError(
                f"{file_path}: dependency #{index} in {'/'.join(tags)} "
                "must contain non-empty groupId and artifactId"
            )
        dependencies.add(f"{group_id}.{artifact_id}")
    return dependencies


def check_dependencies(
    managed: set[str], declared: set[str], expected_deviations: list[str]
) -> list[str]:
    exceptions = {
        dependency
        for dependency in managed | declared
        if any(fnmatchcase(dependency, pattern) for pattern in expected_deviations)
    }
    deviations = []
    for dependency in sorted(managed - exceptions - declared):
        deviations.append(f"Missing dependency: {dependency}")
    for dependency in sorted(declared - managed - exceptions):
        deviations.append(f"Extra dependency (not managed): {dependency}")
    for dependency in sorted(declared & exceptions):
        deviations.append(f"Excluded dependency must not be declared: {dependency}")
    return deviations


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Compare dependencyManagement/dependencies in the first POM with "
            "dependencies in the second POM by exact groupId.artifactId. "
            "Versions, scopes, types, and classifiers are ignored. Expected "
            "deviations may use * wildcards."
        )
    )
    parser.add_argument("managed_pom", type=Path, help="POM defining managed dependencies")
    parser.add_argument("dependencies_pom", type=Path, help="POM declaring dependencies")
    parser.add_argument(
        "expected_deviations",
        nargs="*",
        metavar="DEVIATION",
        help="groupId.artifactId entries that must be absent; * matches any text",
    )
    args = parser.parse_args()

    try:
        managed = read_dependencies(args.managed_pom, managed=True)
        declared = read_dependencies(args.dependencies_pom, managed=False)
    except (OSError, ET.ParseError, UnicodeError, ValueError) as exc:
        print(f"Input error: {exc}", file=sys.stderr)
        return 2

    deviations = check_dependencies(managed, declared, args.expected_deviations)
    for deviation in deviations:
        print(deviation)
    if deviations:
        print(f"Found {len(deviations)} unexpected deviation(s).")
        return 1
    print("No unexpected deviations.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
