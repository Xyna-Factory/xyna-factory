#!/bin/bash
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

if [[ $# -eq 0 ]]; then
  echo "$0 - check out xyna module application.xml and XMOM-files."
  echo "    usage: $0 <branch>. Example: $0 main"
  exit 1
fi

git clone --no-checkout https://github.com/Xyna-Factory/xyna-factory.git --branch "$1"
cd xyna-factory
git sparse-checkout init --cone

PATHS=""
for path in $(git ls-tree --full-name --name-only -r HEAD | grep "^modules/.*/XMOM" | cut -d "/" -f 1-3 | uniq)
do
  PATHS="${PATHS} ${path}/XMOM"
  PATHS="${PATHS} ${path}/application.xml"
done

git sparse-checkout set ${PATHS}
git checkout "$1"
echo "work complete"
exit 0