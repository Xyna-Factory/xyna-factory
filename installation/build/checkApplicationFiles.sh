#!/bin/bash

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2022 GIP SmartMercial GmbH, Germany
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

# 1) filename von application.xml

# checke, dass alle im application-xml erwaehnten objekte auch als file existieren
FILES=$(grep "<FqName>" $1 | awk -F'<|>' '{ gsub(/\./, "/", $3); print $3".xml" }');

for file in $FILES ; do 
  #echo $file
  if [[ ! -e  XMOM/$file ]] ; then
    echo "-----------------WARNUNG----------------: $file existiert nicht" 
  fi;
done;

# checke, dass alle xml files auch im applicationxml existieren
NAMES=$(find XMOM -name "*.xml" | xargs grep ' TypeName="\([^"]*\)".* TypePath="\([^"]*\)"' | grep -v "<Choice" | sed 's/.* TypeName="\([^"]*\)".* TypePath="\([^"]*\)".*/\2.\1/g');
for name in $NAMES ; do
  APP=$(grep "<FqName>$name</FqName>" $1);
  if [[ "x" == "x$APP" ]] ; then
    echo "-----------------WARNUNG----------------: Eintrag fuer $name fehlt in application.xml"
  fi;
done;
