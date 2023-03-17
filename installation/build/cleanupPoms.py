#!/usr/bin/python3
"""
Goes through all the pom.xml files and removes all <dependency> tags without a version specified
"""

import io
import os
import re
import glob

CENTRAL_POM_PATH = os.path.normpath("../../installation/build/pom.xml")


def remove_inherited_dependencies(pom: io.TextIOWrapper) -> str:
    """
    Goes through the contents and removes each <dependency> tag that doesn't specify a version.

    Updates the content of the file.

    We find all <dependency> tags with a regexp, then we go through each match 
    and write from the last valid index to the start of the match. 
    The last valid index is then set to the end of the match
    """
    content = pom.read()
    pom.seek(0)
    last_index = 0
    for match in re.finditer(r'<dependency>.*?</dependency>', content, re.DOTALL):
        # skip matches that have a version specified
        if "<version>" in content[match.start():match.end()]:
            continue
        pom.write(content[last_index:match.start()])
        last_index = match.end()
    pom.write(content[last_index:])
    pom.truncate()


def main():
    pom_paths = [os.path.normpath(path) for path in glob.glob("../../**/pom.xml", recursive=True)]
    for pom_xml in pom_paths:
        # ignore central pom.xml
        if os.path.samefile(pom_xml, CENTRAL_POM_PATH):
            continue

        with open(pom_xml, "r+") as pom_file:
            remove_inherited_dependencies(pom_file)


if __name__ == "__main__":
    main()
