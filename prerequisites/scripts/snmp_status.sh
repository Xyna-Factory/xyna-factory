#! /bin/bash

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



ME=$(basename $0)
VERBOSE="false"

usage () {
echo "usage: ${ME} [xynafactory|apache|netstat_num] <instance nummer>"
        echo
        echo "  Returns the state of the specified service as a number for monitoring with Cacti."
  echo
}

#  bugz 8205: Falls $HOME und $HOSTNAME nicht gesetzt ist, dann einen Wert annehmen
if [[ "x" == "x${HOME}" ]]; then export HOME="/root"; fi
if [[ "x" == "x${HOSTNAME}" ]]; then export HOSTNAME=$(/bin/hostname); fi

export LANG=C

SERVICE=${1}
PRODUCT_INSTANCE=${2}
state="255"

if [[ "x${PRODUCT_INSTANCE}" = "x" ]]; then
  echo ${state}
fi

case ${SERVICE} in
  xynafactory)
    XYNAFACTORY_INIT_SCRIPT="/etc/init.d/xynafactory_${PRODUCT_INSTANCE}"
    if [[ -x ${XYNAFACTORY_INIT_SCRIPT} ]]; then
      ${XYNAFACTORY_INIT_SCRIPT} status 1>/dev/null 2>/dev/null
      state=$(echo "$?")
    fi
  ;;
  apache2)
    APACHE2_INIT_SCRIPT="/etc/init.d/apache2"
    if [[ -x ${APACHE2_INIT_SCRIPT} ]]; then
      ${APACHE2_INIT_SCRIPT} status 1>/dev/null 2>/dev/null
      state=$(echo "$?")
    fi
  ;;
  netstat_num)
    state=$(netstat -t | grep -c ESTABLISHED)
  ;;
  *) usage; exit 67;;
esac

#  Ausgabe des Status fuer Cacti
echo ${state}

#EOF

