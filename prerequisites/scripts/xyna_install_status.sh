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


#
#  Den Installationsstatus aller Komponenten anzeigen.
#  Eigentlich eine aufgehuebschte Variante von
#     cat /etc/xyna/environment/${HOSTNAME}.component.properties

XYNA_ENVIRONMENT_DIR="/etc/xyna/environment"
PROPERTIES_FILE_NAME="${XYNA_ENVIRONMENT_DIR}/${HOSTNAME}.component.properties"
VOLATILE_AWK="/usr/bin/awk"
LENGTH_COMPONENT_NAME="40"
LENGTH_COMPONENT_VERSION="15"

if [[ ! -f "${PROPERTIES_FILE_NAME}" ]]; then
  echo "No components installed. Abort!"
  exit 1
fi

#  Liste aller installierter Komponenten bestimmen
ALL_COMPONENTS=$(${VOLATILE_AWK} 'BEGIN { FS="=" } (NF == 2)  && $1 ~ /.*.version/ { print $1 }' ${PROPERTIES_FILE_NAME} | sort)

#  Ueber alle Komponenten iterieren
for i in ${ALL_COMPONENTS}
do
  #  Status lesen
  unset COMPONENT_DATE
  COMPONENT_DATE=$(${VOLATILE_AWK} 'BEGIN { FS="=" } (NF == 2)  && ($1 == "'"${i%version}date"'") { print $2 }' ${PROPERTIES_FILE_NAME})
  COMPONENT_VERSION=$(${VOLATILE_AWK} 'BEGIN { FS="=" } (NF == 2)  && ($1 == "'"${i}"'") { print $2 }' ${PROPERTIES_FILE_NAME})
  if [[ "x${COMPONENT_VERSION}" == "x" ]]; then
    COMPONENT_VERSION="_removed_"
  fi

  #  Text fuer die Ausgabe etwas aufhuebschen
  OUT_COMPONENT_NAME=$(printf "%-${LENGTH_COMPONENT_NAME}s" "${i}")
  OUT_COMPONENT_VERSION=$(printf "%-${LENGTH_COMPONENT_VERSION}s" "${COMPONENT_VERSION}")
  echo "${OUT_COMPONENT_NAME}: ${OUT_COMPONENT_VERSION} (${COMPONENT_DATE})"
done

#  EOF
