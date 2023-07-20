#!/bin/bash

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

mvn -f "$SERVER_POM" versions:set -DnewVersion="$VERSION" -DartifactId=xynafactory -DgroupId=com.gip.xyna -DprocessDependencies=true
mvn -f "$BOM" versions:use-dep-version -DdepVersion="$VERSION" -Dincludes=com.gip.xyna:xynafactory -DforceVersion=true
sed -i "s#release\.number=v.*#release\.number=v$VERSION#" $DELIVERY

echo "Version updated to $VERSION"
exit 0
