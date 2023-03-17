#!/usr/bin/python3
"""
Goes through all the pom.xml files and removes all <dependency> tags without a version specified
"""

import io
import os
import re
import glob

CENTRAL_POM_PATH = os.path.normpath("../../installation/build/pom.xml")


def removeInheritedDependencies(pom: io.TextIOWrapper) -> str:
    """
    Goes through the contents and removes each <dependency> tag that doesn't specify a version.

    Updates the content of the file.

    We find all <dependency> tags with a regexp, then we go through each match 
    and write from the last valid index to the start of the match. The last valid index is then set to the end of the match
    """
    content = pom.read()
    pom.seek(0)
    lastIndex = 0
    for match in re.finditer(r'<dependency>.*?</dependency>', content, re.DOTALL):
        # skip matches that have a version specified
        if "<version>" in content[match.start():match.end()]:
            continue
        pom.write(content[lastIndex:match.start()])
        lastIndex = match.end()
    pom.write(content[lastIndex:])
    pom.truncate()


def main():
    pomPaths = [os.path.normpath(path) for path in glob.glob("../../**/pom.xml", recursive=True)]
    for pomXml in pomPaths:
        # ignore central pom.xml
        if os.path.samefile(pomXml, CENTRAL_POM_PATH):
            print(f"Skip {pomXml}")
            continue

        with open(pomXml, "r+") as pomFile:
            removeInheritedDependencies(pomFile)


if __name__ == "__main__":
    main()
