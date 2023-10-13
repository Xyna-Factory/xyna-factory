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


BUILD_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
INSTALL_DIR=$BUILD_DIR/..
SERVER_POM=$INSTALL_DIR/../server/pom.xml
BOM=$INSTALL_DIR/build/pom.xml
DELIVERY=$INSTALL_DIR/delivery/delivery.properties


SERVER_VERSION=$(mvn -f "${SERVER_POM}" help:evaluate -Dexpression=project.version -q -DforceStdout)
DELIVERY_VERSION=$(cat "${DELIVERY}" | grep release\\.number=v)

if [[ $DELIVERY_VERSION != "release.number=v${SERVER_VERSION}" ]] ; then
  echo "version mismatch! server version: ${SERVER_VERSION} - delivery version: ${DELIVERY_VERSION}"
  exit 1
fi

# requires version tag to immediatly follow artifactId
if (( $(grep -A1 ">xynafactory<" "${BOM}" | grep version | grep "${SERVER_VERSION}" | wc -l) == 0 )) ; then
  echo "version mismatch! server version: ${SERVER_VERSION}. Bom version is different!"
fi

echo "versions match: ${SERVER_VERSION}"
exit 0