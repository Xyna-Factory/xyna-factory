#!/usr/bin/python3
# -*- coding: iso-8859-15 -*-
"""
Goes through all the files in the given directory and checks, that their encoding is ISO-8859-1.
"""

import sys
import argparse
from pathlib import Path


REPO_ROOT = Path(sys.argv[0]).absolute().parents[2]
VERBOSE = 0

VIOLATING_FILES: list[Path] = []

ENCODING_WITNESSES: list[str] = [
    "ö",
    "ä",
    "ü",
    "Ö",
    "�",
]


def ignore_dir(directory: Path) -> bool:
    """
    Checks if the directory shall be ignored (i.e., `.git/`)

    Returns:
        `True` if this directory shall be ignored; `False` if not
    """
    return ".git" in str(directory)


def ignore_file(file: Path) -> bool:
    """
    Checks if the file shall be ignored (i.e., `*.jar`)

    Returns:
        `True` if the file shall be ignored; `False` if not
    """
    return ".jar" in file.name \
        or ".zip" in file.name \
        or ".app" in file.name \
        or ".swf" in file.name \
        or not "." in file.name \
        or (".xml" in file.name and not file.name.startswith("build")) \
        or "checkEncoding.py" in file.name \
        or "OtherExportImportAndUtils.java" in file.name


def encoding_ok(file: Path) -> bool:
    """
    Checks if `file` is ISO-8859-1 encoded or not

    Args:
        `file`  Path to file to be checked

    Returns:
        `True` if file is ISO-8859-1 encoded; `False` if not
    """
    global VERBOSE

    if ignore_file(file):
        if VERBOSE > 1:
            print(f"  Ignoring file {file}")
        return True

    try:
        content = file.read_text(encoding="ISO-8859-1", errors="strict")
        found_witness = any(
            [witness in content for witness in ENCODING_WITNESSES])
        if VERBOSE > 2 and found_witness:
            print(f"Found witness in file {file}")
        return not found_witness
    except ValueError as e:
        if VERBOSE > 2:
            print(f"Got error reading file {file}: {e}")
        return False


def find_files(directory: Path, report_all: bool):
    """
    Recursively searches through the given directory and adds files in utf-8 encoding to `VIOLATING_FILES`
    """
    global VIOLATING_FILES, VERBOSE

    if not directory.is_dir() or ignore_dir(directory):
        if VERBOSE > 1:
            print(f"  Ignoring directory {directory}")
        return

    if VERBOSE > 0:
        print(f"Search through directory {directory}")
    for entry in directory.iterdir():
        if entry.is_dir():
            find_files(entry, report_all)
        elif not encoding_ok(entry):
            VIOLATING_FILES.append(entry)
            if not report_all:
                return


def main():
    global VERBOSE

    parser = argparse.ArgumentParser()
    parser.add_argument("-d", "--directory", default=str(REPO_ROOT),
                        help="Directory to recursively search through")
    parser.add_argument("--report-all", action="store_true",
                        help="First find all violating files and reporting them before exiting")
    parser.add_argument("-v", "--verbose", action="count")
    args = parser.parse_args()

    search_directory = Path(args.directory).resolve()
    VERBOSE = args.verbose if args.verbose is not None else 0

    find_files(search_directory, args.report_all)

    print("")
    if len(VIOLATING_FILES) > 0:
        sys.stderr.write(
            f"Found {len(VIOLATING_FILES)} files(s) not in ISO-8859-1 encoding:\n")
        sys.stderr.writelines([f" - {file}\n" for file in VIOLATING_FILES])
        sys.exit(1)

    print("All files ok")


if __name__ == "__main__":
    main()
