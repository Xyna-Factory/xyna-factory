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

"""
Scan a directory for application.xml files and write a JSON mapping of
  { "<path>/application.xml": "<versionName>", ... }
to the specified output file.

Usage:
    python createAppVersionSummary.py --path <dir> --output <file>
"""

import argparse
import json
import os
import xml.etree.ElementTree as ET


def collect_versions(root_path: str) -> dict[str, str]:
    versions: dict[str, str] = {}
    for root, _dirs, files in os.walk(root_path):
        if "application.xml" in files:
            path = os.path.join(root, "application.xml").replace("\\", "/")
            with open(path, encoding="utf-8") as file:
                tree = ET.parse(file)
            version = tree.getroot().get("versionName")
            if version is not None:
                versions[path] = version
    return versions


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Collect versionName from all application.xml files under a path."
    )
    parser.add_argument(
        "--path", required=True, metavar="DIR",
        help="Root directory to search for application.xml files",
    )
    parser.add_argument(
        "--output", required=True, metavar="FILE",
        help="Output JSON file path",
    )
    args = parser.parse_args()

    versions = collect_versions(args.path)

    with open(args.output, "w", encoding="utf-8") as file:
        json.dump(versions, file, indent=2)

    print(f"Wrote {len(versions)} application(s) to {args.output}")


if __name__ == "__main__":
    main()
