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


#  Pruefen, dass in delivery.properties das Property
#+ svn.branch auskommentiert ist, falls es der Trunk ist
#+ und mit dem korrekten Wert befuellt ist, falls es
#+ ein Branch ist.

export LANG="C"

echo -e "\n* Getting information from SVN"

STR_SVN_ROOT="$(svn info | awk 'BEGIN { FS=":"} $1 == "Repository Root" { print $NF }')/"
STR_SVN_URL=$(svn info | awk 'BEGIN { FS=":"} $1 == "URL" { print $NF }')

STR_SVN_PATH="${STR_SVN_URL#${STR_SVN_ROOT}}"
STR_TRUNK=$(echo "${STR_SVN_PATH}" | awk 'BEGIN { FS="/"} {print $1 }')

BOOL_SVN_IS_TRUNK="false"
if [[ "${STR_TRUNK}" == "trunk" ]]; then
  BOOL_SVN_IS_TRUNK="true"
fi

#echo "     + repository root  : svn:${STR_SVN_ROOT}"
#echo "     + repository path  : ${STR_SVN_PATH}"
echo "     + sandbox is trunk : ${BOOL_SVN_IS_TRUNK}"


echo -e "\n* Checking delivery.properties"

TARGET_FILE="$(dirname $0)/delivery.properties"
if [[ ! -f "${TARGET_FILE}" ]]; then
  echo -e "\n==> FAIL - Unable to locate '${TARGET_FILE}'. Abort!"
  exit 1
else

  STR_SVN_BRANCH_PROPERTIES=$(grep "svn.branch=" "${TARGET_FILE}")

  BOOL_PROPERTIES_IS_TRUNK="true"
  if [[ "${STR_SVN_BRANCH_PROPERTIES:0:1}" == "s" ]]; then
    BOOL_PROPERTIES_IS_TRUNK="false"
  fi
  echo "     + properties trunk : ${BOOL_PROPERTIES_IS_TRUNK}"

  if [[ "${BOOL_PROPERTIES_IS_TRUNK}" != "${BOOL_SVN_IS_TRUNK}" ]]; then
    echo -e "\n==> FAIL - '${TARGET_FILE}' does not match SVN!"
    exit 2
  fi

  if [[ "${BOOL_PROPERTIES_IS_TRUNK}" == "false" ]]; then
    echo -e "\n* Checking if branch name is identical"

    STR_BRANCH_VERSION_PROPERTIES=$(echo "${STR_SVN_BRANCH_PROPERTIES}" | awk 'BEGIN { FS="=" } { print $NF }')
    STR_BRANCH_VERSION_SVN=$(echo "${STR_SVN_PATH}" | awk 'BEGIN { FS="/"} { print $2 }')
    echo "     + properties branch: ${STR_BRANCH_VERSION_PROPERTIES}"
    echo "     + sandbox branch   : ${STR_BRANCH_VERSION_SVN}"

    if [[ "${STR_BRANCH_VERSION_PROPERTIES}" != "${STR_BRANCH_VERSION_SVN}" ]]; then
      echo -e "\n==> FAIL - version differs!"
      echo -e "\nHint: Maybe the encoding of 'delivery.properties' is incorrect."
      echo -e "\nTry this\n    vim -c \":set ff=unix\" -c \":wq\" delivery.properties"
      exit 3
    fi
  fi

fi

echo -e "\n==> OK"
#  EOF
