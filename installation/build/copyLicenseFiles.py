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

import shutil
import sys
from os import listdir, makedirs, path


def copy_license_files(input_dir, output_dir):
    """copy license files (non .jar-files) in input dir to output dir"""

    makedirs(output_dir, exist_ok=True)

    for item in listdir(input_dir):
        item_path = path.join(input_dir, item)
        if path.isfile(item_path) and not item.lower().endswith(".jar"):
            shutil.copy(item_path, output_dir)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 copyLicenseFiles.py <input_directory> <output_directory>")
        sys.exit(1)

    input_dir = sys.argv[1]
    output_dir = sys.argv[2]
    copy_license_files(input_dir, output_dir)
