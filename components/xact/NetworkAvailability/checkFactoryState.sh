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

#  Der Status der Xyna Factory wird abgefragt.
#+ Ist die Fabrik hochgefahren, so wird der Exitstatus 0 zurueckgeliefert.
#+ Anderenfalls wird ein Wert != 0 zurueckgeliefert.
#
#  In der aktuellen Implementierung wird ../server/xynafactory.sh -q status aufgerufen.
#+ Dessen Verhalten erfuellt bereits die beschriebenen Kriterien.
#+ Dieses Skript ermoeglicht eine Erweiterung, falls noch zusaetzliche Schritte
#+ erforderlich werden...

#  Aufruf mit relativem/absoluten Pfad moeglich
cd "$(dirname "$0")"

#  Verzeichnisstruktur ist so, dass MDM, server und NetworkAvailability auf der gleichen
#+ Ebene liegen. D.h. das Verzeichnis der Factory 'server' ist relativ gesehen hier:
#+   ../server
XYNA_FACTORY_BASE_DIR="../server"
XYNA_FACTORY_SH="${XYNA_FACTORY_BASE_DIR}/xynafactory.sh"

#  Koennen alle Voraussetzungen gefunden werden?
if [[ ! -d "${XYNA_FACTORY_BASE_DIR}" ]]; then
  echo "Unable to locate Xyna factory base directory '${XYNA_FACTORY_BASE_DIR}' - Abort!"
  exit 99
fi

if [[ ! -x "${XYNA_FACTORY_SH}" ]]; then
  echo "Unable to locate '${XYNA_FACTORY_SH}' or executable bit is not set - Abort!"
  exit 98
fi

#  Status der Fabrik erfragen
#${XYNA_FACTORY_SH} status

#exit 1, wenn status -v entweder
#  leer, oder 
#  got: STARTUP: XynaCluster - INIT, oder 
#  got: POSTSTARTUP: XynaCluster - SHUTDOWN
STATUS_OUTPUT=$(${XYNA_FACTORY_SH} status -v | grep ": XynaCluster -")
#echo "a${STATUS_OUTPUT}b" >> fstate.txt

if [[ "x${STATUS_OUTPUT}" == "x"  ]]; then
  exit 1
elif [[ "x${STATUS_OUTPUT}" == "xSTARTUP: XynaCluster - INIT" ]]; then
  exit 1
elif [[ "x${STATUS_OUTPUT}" == "xPOSTSTARTUP: XynaCluster - SHUTDOWN" ]]; then
  exit 1
fi
exit 0


#old: factory state abfrage
#INT_STATUS=$?
#
#  Ermittelten Status ausgeben
#exit ${INT_STATUS}

#  EOF
