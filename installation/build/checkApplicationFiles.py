#!/usr/bin/python3

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2024 Xyna GmbH, Germany
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

import sys
import xml.etree.ElementTree as ET
from os import path, walk


def main(application_path):
    xmomDir = path.join(path.dirname(application_path), "XMOM")

    # extract fqn's from the input application.xml
    appRoot = ET.parse(application_path).getroot()
    fqNames = [
        entry.find("FqName").text.replace(".", "/") + ".xml" for entry in appRoot.findall("XMOMEntries/XMOMEntry")
    ]

    # check if files exist in the XMOM directory
    for fqn in fqNames:
        if not path.exists(path.join(xmomDir, fqn)):
            print(f"-----------------WARNING----------------: {fqn} does not exist")

    # collect all referenced objects in the xml files in the XMOM directory
    objectRefs = []
    for dirpath, _, filenames in walk(xmomDir):
        for xmlFile in filenames:
            xmlRoot = ET.parse(path.join(dirpath, xmlFile)).getroot()
            if "TypePath" in xmlRoot.attrib and "TypeName" in xmlRoot.attrib:
                objectRefs.append(xmlRoot.get("TypePath") + "." + xmlRoot.get("TypeName"))
            for element in xmlRoot.findall(".//*[@TypePath]"):
                if "TypePath" in element.attrib and "TypeName" in element.attrib and not element.tag.endswith("Choice"):
                    objectRefs.append(element.get("TypePath") + "." + element.get("TypeName"))

    # check if entries for all references exist in the input application.xml
    for ref in objectRefs:
        if appRoot.find(f".//FqName[.='{ref}']") is None:
            print(f"-----------------WARNING----------------: Entry for {ref} is missing in application.xml")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python script.py <application.xml>")
        sys.exit(1)

    main(sys.argv[1])
