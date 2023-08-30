#!/usr/bin/python3
# -*- coding: iso-8859-15 -*-
"""
Goes through all the files in the given directory and checks, that they match their expected encoding:

- .xml, .json and .pom: UTF-8
- .java: ISO-8859-1
"""

import sys
import argparse
from pathlib import Path

UTF_8 = "UTF-8"
ISO_8859_1 = "ISO_8859_1"

REPO_ROOT = Path(sys.argv[0]).absolute().parents[2]
VERBOSE = 0

VIOLATING_FILES: list[Path] = []

EXPECTED_UTF_8: list[str] = [
    ".xml",
    ".pom",
    ".py",
    ".json"
]

# characters the give away, that a file opened with the given encoding isn't actually encoded that way
ENCODING_WITNESSES: dict[str, list[str]] = {
    ISO_8859_1: [
        "Ã¶",
        "Ã¤",
        "Ã¼",
        "Ã–",
        "Ã",
    ],
    UTF_8: [
        "\uFFFD"
    ]
}


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
        or ".class" in file.name \
        or "checkEncoding.py" in file.name \
        or "OtherExportImportAndUtils.java" in file.name


def get_expected_encoding(file: Path) -> str:
    """
    Returns:
        The expected encoding based on the filename
    """
    if any([file.name.endswith(utf_8_extension) for utf_8_extension in EXPECTED_UTF_8]):
        return "UTF-8"

    return "ISO-8859-1"


def get_witnesses(encoding: str) -> list[str]:
    """
    Returns:
        the encoding witnisses for the given encoding. If encoding is unknown, the witnesses for ISO-8859-1 are used
    """
    if encoding not in ENCODING_WITNESSES:
        encoding = ISO_8859_1
    return ENCODING_WITNESSES[encoding]



def encoding_ok(file: Path) -> bool:
    """
    Checks if `file` has its expected encoding encoded or not

    Args:
        `file`  Path to file to be checked

    Returns:
        `True` if file has expected encoding; `False` if not
    """
    global VERBOSE

    if ignore_file(file):
        if VERBOSE > 2:
            print(f"  Ignoring file {file}")
        return True

    try:
        encoding = get_expected_encoding(file)
        witnesses = get_witnesses(encoding)
        content = file.read_text(encoding=encoding, errors="strict")
        found_witness = any(
            [witness in content for witness in witnesses])
        if VERBOSE > 0 and found_witness:
            print(f"\n\tFound witness in file {file}")
            print(f"\tencoding: {encoding}; witnesses: {witnesses}")
        return not found_witness
    except ValueError as e:
        if VERBOSE > 0:
            print(f"\n\tGot error reading file {file}: {e}")
        return False


def find_files(directory: Path, report_all: bool):
    """
    Recursively searches through the given directory and adds files in utf-8 encoding to `VIOLATING_FILES`
    """
    global VIOLATING_FILES, VERBOSE

    if not directory.is_dir() or ignore_dir(directory):
        if VERBOSE > 2:
            print(f"  Ignoring directory {directory}")
        return

    if VERBOSE > 2:
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
            f"Found {len(VIOLATING_FILES)} files(s) not in expected encoding:\n")
        sys.stderr.writelines([f" - {file}\n" for file in VIOLATING_FILES])
        sys.exit(1)

    print("All files ok")


if __name__ == "__main__":
    main()
