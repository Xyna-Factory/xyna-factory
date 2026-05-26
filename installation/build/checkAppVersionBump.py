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
Check that every application whose files were changed has its versionName bumped.

Inputs:
  --diff             File listing changed file paths (gh pr diff --name-only)
  --base-versions    JSON file mapping application.xml path to versionName (base branch)
  --new-versions     JSON file mapping application.xml path to versionName (PR branch)
  --ignore-patterns  Optional file containing regular expressions (one per line);
                     changed files matching any expression are ignored
"""

import argparse
import json
import os
import re
import sys


def find_owning_application(changed_file: str, application_dirs: set[str]) -> str | None:
    """Return the application directory that owns *changed_file*, or None."""
    file_dir = os.path.dirname(changed_file).replace("\\", "/")
    best = None
    for app_dir in application_dirs:
        normalised = app_dir.rstrip("/")
        if file_dir.startswith(normalised + "/"):
            if best is None or len(normalised) > len(best):
                best = normalised
    return best


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Verify that applications with changed files have a bumped versionName."
    )
    parser.add_argument(
        "--diff", required=True, metavar="FILE",
        help="File listing changed file paths (gh pr diff --name-only)",
    )
    parser.add_argument(
        "--base-versions", required=True, metavar="FILE",
        help="JSON mapping application.xml path -> versionName on the base branch",
    )
    parser.add_argument(
        "--new-versions", required=True, metavar="FILE",
        help="JSON mapping application.xml path -> versionName on the PR branch",
    )
    parser.add_argument(
        "--ignore-patterns", metavar="FILE",
        help="File containing regular expressions (one per line); matching changed files are ignored",
    )
    args = parser.parse_args()

    ignore_patterns: list[re.Pattern[str]] = []
    if args.ignore_patterns:
        with open(args.ignore_patterns, encoding="utf-8") as ignore_patterns_file:
            ignore_patterns = [
                re.compile(line.strip())
                for line in ignore_patterns_file
                if line.strip() and not line.startswith("#")
            ]

    with open(args.diff, encoding="utf-8") as diff_file:
        changed_files = [line.strip() for line in diff_file if line.strip()]

    with open(args.base_versions, encoding="utf-8") as base_version_file:
        base_versions: dict[str, str] = {
            k.lstrip("./") if k.startswith("./") else k: v
            for k, v in json.load(base_version_file).items()
        }

    with open(args.new_versions, encoding="utf-8") as new_version_file:
        new_versions: dict[str, str] = {
            k.lstrip("./") if k.startswith("./") else k: v
            for k, v in json.load(new_version_file).items()
        }

    application_dirs: set[str] = {
        os.path.dirname(p).replace("\\", "/") for p in new_versions
    }

    # Determine which applications are touched by the changed files,
    # skipping files that match any of the ignore patterns
    touched_apps: set[str] = set()
    for changed_file in changed_files:
        if any(p.search(changed_file) for p in ignore_patterns):
            continue
        owner = find_owning_application(changed_file, application_dirs)
        if owner is not None:
            touched_apps.add(owner)

    if not touched_apps:
        print("No changed files belong to any application — nothing to check.")
        sys.exit(0)

    errors: list[str] = []
    for app_dir in sorted(touched_apps):
        app_xml = app_dir.rstrip("/") + "/application.xml"
        old_version = base_versions.get(app_xml)
        new_version = new_versions.get(app_xml)

        if new_version is None:
            print(f"OK:    {app_xml}: Application removed (was {old_version})")
        elif old_version is None:
            print(f"OK:    {app_xml}: New Application (version {new_version})")
        elif old_version == new_version:
            errors.append(f"{app_xml}: versionName not bumped (still {old_version})")
        else:
            print(f"OK:    {app_xml}: {old_version} -> {new_version}")

    if errors:
        print("\nVersion bump check FAILED:")
        for err in errors:
            print(f"  ERROR: {err}")
        sys.exit(1)

    print("\nAll changed applications have a bumped versionName.")


if __name__ == "__main__":
    main()
