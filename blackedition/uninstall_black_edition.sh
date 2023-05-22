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

#  Diese Variablen muessen gesetzt sein.
PRODUCT_NAME="black_edition"
#  Diese Property wird in der host-spezifischen Properties-Datei abgelegt
HOST_PROPERTIES="black_edition.instances"
#  Diese Properties werden in der produkt-spezifischen Properties-Datei abgelegt
PRODUCT_PROPERTIES="installation.folder"

#  Einige Funktionen sind in Bibliotheken ausgelagert.
#+ Diese Bibliotheken importieren
#
#  Eingabeparameter:
#o   Dateiname der zu importierenden Bibliothek
load_functions () {
  SOURCE_FILE="${1}"
  if [[ ! -f ${SOURCE_FILE} ]]; then
    echo "Unable to import functions from '${SOURCE_FILE}'. Abort!"; exit 99;
  else
    source ${SOURCE_FILE}
  fi
}

#  Generische Funktionen importieren.
load_functions "$(dirname "$0")/func_lib/func_lib.sh"

#  Produktspezifische Funktionen importieren.
load_functions "$(dirname "$0")/blackedition_lib.sh"

#  Read host-specific properties
PROP_FILE=$(get_properties_filename host);
for i in ${HOST_PROPERTIES} ; do
  get_property ${i} "${PROP_FILE}"
  case ${i} in
    black_edition.instances) BLACK_EDITION_INSTANCES="${CURRENT_PROPERTY}";;
  esac
done

#  Check, if instance number '-i' in the allowed range
INSTANCE_NUMBER="1"
parse_commandline_arguments "$@"
if [[ $# -lt 1 ]]; then DISPLAY_USAGE="true"; fi
if [[ "x${DISPLAY_USAGE}" == "xtrue" ]]; then display_usage; exit; fi

#  Read product-specific properties
PROP_FILE=$(get_properties_filename product "${INSTANCE_NUMBER}")
for i in ${PRODUCT_PROPERTIES} ; do
  get_property ${i} "${PROP_FILE}"
  case ${i} in
    installation.folder) INSTALL_PREFIX="${CURRENT_PROPERTY}";;
  esac
done

#  start main logic
if [[ "x${DRY_RUN}" == "xtrue" ]]; then debug_variables; exit; fi
check_component_status "${PRODUCT_NAME}"
PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})

#  Nur entfernen, was installiert wurde.
if [[ "x${COMPONENT_UPDATE}" == "xinstall" ]]; then
  err_msg "Instance '${PRODUCT_INSTANCE}' is not installed. Abort!"
  exit 99
fi

FIVE_DIGITS=$(head -c10 /dev/urandom | md5sum | tr -d [a-z] | tr -d 0 | cut -c1-5)
${VOLATILE_CAT} << A_HERE_DOCUMENT

###########################################################################
  Removing installation.                                     ${FIVE_DIGITS}
  This is only for development!! NEVER use on production systems!!
###########################################################################

Instance '${PRODUCT_INSTANCE}' is about to be removed:
  + ${INSTALL_PREFIX}/MDM
  + ${INSTALL_PREFIX}/server

Enter the code given above to proceed with removing:
A_HERE_DOCUMENT
read ANSWER
if [[ "x${FIVE_DIGITS}" == "x${ANSWER}" ]]; then
  echo "Code OK - proceeding with removing the installation."
  stop_xynafactory
  ${VOLATILE_RM} -rf "${INSTALL_PREFIX}/revisions" "${INSTALL_PREFIX}/saved" "${INSTALL_PREFIX}/server" "${INSTALL_PREFIX}/xmomrepository"
  echo "Deleted files"
  #  Update version and installation date
  clear_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}"
  echo "Cleared components file"
else
  echo "Code FAILED - exiting script, nothing happened."
fi

#  EOF
