#!/bin/bash
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2023 Xyna GmbH, Germany
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


# checkout modules, but only application.xml and XMOM-files

#svn-modules path => branch/tag
#option to also checkout build.xml files

if [[ $# -eq 0 ]]; then
  echo "$0 - check out xyna module application and XMOM-files."
  echo "    usage: pass at least one parameter"
  echo "    svn-modules path e.g. https://github.com/Xyna-Factory/xyna-factory/tags/9.0.1.0/modules"
  echo "    optional: also checkout build.xml files"
  exit 1
fi

svn checkout "$1" --depth immediates
if [[ $? != 0 ]]; then
  echo "checkout failed. Abort!"
  exit 2
fi
cd modules
if [[ $? != 0 ]]; then
  echo "checkout did not create a modules folder. Abort!"
  exit 2
fi

for path in $(find . -maxdepth 1 -type d)
do
  if [ "$path" == "./.svn" ] || [ "$path" == "." ]; then
	continue
  fi
  svn update --set-depth immediates "$path"
  for module in $(find $path -maxdepth 1 -type d)
  do
    if [ "x$module" == "x$path" ]; then
	  echo "ignore $module"
	  continue
	fi
    svn update --set-depth infinity "$module/XMOM"
	svn update --set-depth empty "$module/application.xml"
	if [[ $# == 2 ]]; then
	  svn update --set-depth empty "$module/build.xml"
	fi
  done
done
echo "work complete"
exit 0
