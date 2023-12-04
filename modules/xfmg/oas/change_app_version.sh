#!/bin/bash

# changes the version number of the oas base app at the necessary locations

versionregex=[0-9]\.[0-9]\.[0-9]

if [[ $1 =~ ^$versionregex$ ]]
then
    sed -i -E "s/(versionName=\")$versionregex/\1$1/" application.xml
    sed -i -E "s/(<VersionName>)$versionregex(<\/VersionName>)/\1$1\2/" sharedlib/xyna-openapi/generators/xmom-data-model/src/main/resources/xmom-data-model/application.mustache
    sed -i -E "s/(<Mapping>%0%.version=\")$versionregex(\"<\/Mapping>)/\1$1\2/" XMOM/xmcp/oas/fman/GetOASImportHistoryDefinition.xml
else
    echo "invalid version format"
fi
