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

#  globale Optionen
JVM_OPTIONS="-Xmx32m -Dlog4j.debug=false -Dlog4j.configuration=file:log4j.properties"

#  Aufruf mit relativem/absoluten Pfad moeglich
cd "$(dirname "$0")"

#  Classpath dynamisch aufbauen
CLASSES=""
for i in lib/*.jar; do
  if [[ -f "${i}" ]]; then
    CLASSES="${i}:${CLASSES}"
  fi
done

#  Debugausgabe durch 'export DEBUG=1' auf der Shell moeglich
if [[ ${DEBUG:-0} -gt 0 ]]; then echo "java ${JVM_OPTIONS} -cp \"${CLASSES}\" com.gip.xyna.xact.Main"; fi

# alle Ausgaben ins Log
exec 2> >(logger -plocal0.debug)

#  Job control anschalten
set -m

#  Demon im Hintergrund starten damit die PID ermittelt werden kann
java ${JVM_OPTIONS} -cp "${CLASSES}" com.gip.xyna.xact.Main &

#  PID-File schreiben
echo $! > ${0%sh}pid

#  Demon wieder in den Vordergrund holen, damit das Shell-Skript nicht beendet wird
fg %1

#  PID-File loeschen, falls der Java-Prozess abgeschossen wurde
rm -f ${0%sh}pid
