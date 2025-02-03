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



if [[ "x$(ps -o "user=" -p $$ | sed -e "s+ ++g")" != "xroot" ]]; then
  echo "This script can only be run as root. Abort!"
  exit 90
fi

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
load_functions "$(dirname "$0")/prerequisites_lib.sh"

#  Aufruf mit vollem Pfad ermoeglichen
cd "$(dirname "$0")"

# Reload Cache
if [[ -f "${XYNA_CACHE_DIR}/xyna_func_lib_cache.sh" ]]; then
  ${VOLATILE_RM} "${XYNA_CACHE_DIR}/xyna_func_lib_cache.sh"
  load_settings_from_cache
fi

check_target_platform

INSTANCE_NUMBER="1"
parse_commandline_arguments "$@"
f_set_environment_dir

if [[ $# -lt 1 ]]; then DISPLAY_USAGE="true"; fi
if [[ "x${DISPLAY_USAGE}" == "xtrue" ]]; then display_usage; exit; fi

get_local_interfaces
f_read_properties
f_set_environment
f_check_parameters 

debug_variables
if [[ "x${DRY_RUN}" == "xtrue" ]]; then exit; fi

#  save the (possibly) new value at script start - skip updating the entry if 'dry-run' is specified
f_check_or_create_property_file "host"
set_property black_edition.instances "${BLACK_EDITION_INSTANCES}" $(get_properties_filename "host")

check_component_status "black_edition_prerequisites"
get_local_interfaces
set_platform_dependent_properties

#  start main logic

if [[ "x${COMPONENT_LIMITS}"              == "xtrue" ]]; then SOMETHING_CHANGED="true"; f_configure_limits; fi
if [[ "x${COMPONENT_SSH}"                 == "xtrue" ]]; then SOMETHING_CHANGED="true"; f_configure_ssh; fi
if [[ "x${COMPONENT_TIME}"                == "xtrue" ]]; then SOMETHING_CHANGED="true"; f_configure_time; fi
if [[ "x${COMPONENT_XYNAUSER}"            == "xtrue" ]]; then SOMETHING_CHANGED="true"; install_xyna_user; fi
if [[ "x${COMPONENT_INSTALLATION_FOLDER}" == "xtrue" ]]; then SOMETHING_CHANGED="true"; create_folder_xynaserver; fi
if [[ "x${COMPONENT_SCRIPTS}"             == "xtrue" ]]; then SOMETHING_CHANGED="true"; install_scripts; fi
if [[ "x${COMPONENT_ANT}"                == "xtrue" ]]; then
  SOMETHING_CHANGED="true"
  install_ant
  if [[ -f "${XYNA_CACHE_DIR}/xyna_func_lib_cache.sh" ]]; then
    ${VOLATILE_RM} "${XYNA_CACHE_DIR}/xyna_func_lib_cache.sh"
    load_settings_from_cache
    set_platform_dependent_properties
  fi
fi
if [[ "x${COMPONENT_SSL_CERTIFICATE}"     == "xtrue" ]]; then SOMETHING_CHANGED="true"; create_certificate; fi
if [[ "x${COMPONENT_SYSLOG}"              == "xtrue" ]]; then 
    SOMETHING_CHANGED="true"
    f_add_syslog_facility "${XYNA_SYSLOG_FACILITY}"   "${XYNA_SYSLOG_FILE}"
    install_syslog_and_logrotation
fi
if [[ "x${COMPONENT_FIREWALL}"            == "xtrue" ]]; then SOMETHING_CHANGED="true"; f_configure_firewall; fi
# TODO: remove Deployer as well
# if [[ "x${COMPONENT_DEPLOYER}"            == "xtrue" ]]; then 
# fi
if [[ "x${COMPONENT_SNMPD}"               == "xtrue" ]]; then SOMETHING_CHANGED="true"; f_install_snmpd; fi

if f_selected ${COMPONENT_INITD_XYNA} ; then SOMETHING_CHANGED="true"; f_install_initd_xyna; fi
if f_selected ${COMPONENT_NETWORK_AVAILABILITY_DEMON} ; then SOMETHING_CHANGED="true"; f_install_network_availability_demon; fi



#  Update version and installation date
if [[ "x${SOMETHING_CHANGED}" == "xtrue" ]]; then
  PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  save_components_file "black_edition_prerequisites.${PRODUCT_INSTANCE}"
  
  # Berechtigungen in /etc/opt/xyna pruefen und ggf. korrigieren
  f_set_etc_opt_xyna_permission
fi

if [[ -n "${STR_HINWEIS_AM_ENDE}" ]]; then
  attention_msg "${STR_HINWEIS_AM_ENDE}"
fi  
#  EOF
