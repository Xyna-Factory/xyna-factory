#!/bin/bash

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2022 Xyna GmbH, Germany
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

if [ "$#" -ne "1" ]; then
  echo "Parameter: Directory to search in"
  exit 1;
fi
DIR=$1;
FILE=$(mktemp)
FILE1=$(mktemp)

find $DIR -name "*.xml" | grep -v "Exceptions.xml" | grep -v "xmcp.xml" | grep -v "xdev.xml" | grep -v "xnwh.xml" | grep -v "xprc.xml" | grep -v "xfmg.xml" | grep -v "xact.xml" > $FILE1
#./ssh/server/XMOM/xact/ssh/server/enums/Newline.xml
#./ssh/server/XMOM/xact/ssh/server/enums/NewlineAuto.xml


cat $FILE1 | xargs grep "Code=" | sed 's/.* Code="\([^"]*\)".* TypeName="\([^"]*\)".* TypePath="\([^"]*\)".*/\1 \3.\2/g' | awk '{printf "%-30s %s\n", $1, $2 }' | sort | uniq > $FILE
#SSHMOCK-00000                  xact.ssh.mock.NoSessionFoundException
#SSHMOCK-00001                  xact.ssh.mock.ParseBehaviorException
#Achtung: eine Zeile kann wegen dem uniq zu mehreren Files gehören

#für alle doppelten exceptioncodes ($i)
#  für alle exceptionnamen mit code $i (=$j)
#     für alle files, die einen der doppelten codes enthalten: (punkte im exceptionname gelten dann als wildcards und matchen auf die slashes in den filenamen)

for i in $(cat $FILE | awk '{print $1}' | uniq -d); do for j in $(grep "$i " $FILE | awk '{print $2}'); do for k in $(grep $j $FILE1); do echo $i" "$j" "$k | awk '{printf "%-30s %-65s %s\n", $1, $2, $3 }'; done; done; done;

rm -f $FILE
rm -f $FILE1
