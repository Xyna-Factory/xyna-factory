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

#  Die Xyna Property xyna.xcs.networkavailability muss gesetzt werden.
#
#  Eingabeparameter:
#o   $1  - Wert, auf den die Property gesetzt werden soll
#o         Es sind nur die Werte "OK" oder "ERROR" zugelassen
#

f_display_usage () {
  echo
  echo "usage: $(basename "${0}") [OK|ERROR]"
  echo
  echo "    sets the property 'xyna.xcs.networkavailability'"
  echo
}

#  Kommandozeilenparameter verarbeiten ...
XCS_NETWORKAVAILABILITY="${1}"

# ... und auf Plausibilitaet pruefen
case "${XCS_NETWORKAVAILABILITY}" in
  "OK"|"ERROR") ;;  #  alles ok, nichts machen
  *) f_display_usage; exit 100;;
esac

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
${XYNA_FACTORY_SH} set xyna.xcs.networkavailability "${XCS_NETWORKAVAILABILITY}"
INT_STATUS=$?

#  Ermittelten Status ausgeben
exit ${INT_STATUS}

#  EOF
