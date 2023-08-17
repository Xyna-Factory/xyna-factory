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

if [[ $# < 1 ]]; then
  echo "provide a new version."
  exit 1
fi

set -e

VERSION=$1
FORMAT="^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$"

if [[ ! $VERSION =~ $FORMAT ]]; then
        echo "Invalid version format. Expected format: \"$FORMAT\"."
        exit 1
fi

INSTALL_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SERVER_POM=$INSTALL_DIR/../server/pom.xml
BOM=$INSTALL_DIR/build/pom.xml
DELIVERY=$INSTALL_DIR/delivery/delivery.properties

mvn -f "$SERVER_POM" versions:set -DnewVersion="$VERSION" -DartifactId=xynafactory -DgroupId=com.gip.xyna -DprocessDependencies=true -DgenerateBackupPoms=false
mvn -f "$BOM" versions:use-dep-version -DdepVersion="$VERSION" -Dincludes=com.gip.xyna:xynafactory -DforceVersion=true -DgenerateBackupPoms=false
sed -i "s#release\.number=v.*#release\.number=v$VERSION#" $DELIVERY

echo "Version updated to $VERSION"
exit 0
