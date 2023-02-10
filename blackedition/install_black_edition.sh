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

#  Aufruf mit vollem Pfad ermoeglichen
cd "$(dirname "$0")"

f_read_host_properties
f_check_os_settings 

#  No arguments is an error, so give help to the user.
if [[ $# -lt 1 ]]; then DISPLAY_USAGE="true"; fi

#  Parse commandline parameters...
ALL_ARGS="$@"
parse_commandline_arguments "$@"
shift $((OPTIND-1))

#  Are there still any commandline parameters left?
#+ This is only possible for parameters which I do not understand.
#+ If this is the case, give help to the user.
if [[ $# -gt 0 ]]; then 
  display_usage;
  exit 1; #mit Fehler abbrechen
fi
if [[ "x${DISPLAY_USAGE}" == "xtrue" ]]; then 
  display_usage; 
  exit;
fi

INSTANCE_PROP_FILE=$(get_properties_filename "product" ${INSTANCE_NUMBER})

get_local_interfaces
f_read_product_properties
INSTANCE_PROP_FILE=$(get_properties_filename "product" ${INSTANCE_NUMBER})

#  start main logic
if [[ "x${DRY_RUN}" == "xtrue" ]]; then 
  check_usage;
  debug_variables; 
  if f_selected ${COMPONENT_ORACLECLUSTER} ; then 
    check_install_oracle_cluster
  fi
  exit;
fi

#Prüfen der commandline-Parameter-Kombination
check_usage;

if f_selected ${COMPONENT_ORACLECLUSTER} ; then 
  check_install_oracle_cluster
fi

CURRENT_USER=$(${VOLATILE_PS} -o "user=" -p $$ | ${VOLATILE_SED} -e "s+ ++g")
if [[ "x${CURRENT_USER}" != "x${XYNA_USER}" ]]; then
  echo "This instance is configured for user '${XYNA_USER}'. Abort, because '${CURRENT_USER}' is not allowed!"
  exit 1
fi







f_install () {

check_component_status "${PRODUCT_NAME}"
if [[ "x${COMPONENT_UPDATE}" != "xinstall" ]]; then
  warn_for_factory_restart
fi

create_folder_xynafactory

if f_selected ${COMPONENT_XYNAFACTORY} ; then
  if [[ "x${COMPONENT_UPDATE}" == "xinstall" ]]; then
    install_xynafactory
  else
    #  Bei einem Update die Xyna Factory stoppen
    stop_xynafactory
    update_xynafactory
  fi
  XYNAFACTORY_NEEDS_RESTART="true"
else
  if [[ "x${COMPONENT_UPDATE}" == "xinstall" ]]; then
    err_msg "Installation without Xyna Factory does not make sense. Proceeding anyway as requested..."
  fi
fi

#  Update version and installation date
# bereits weit vorne ausführen, damit Eintrag nicht fehlt, selbst wenn weitere Installation scheitert
PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
save_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}"

if [[ "x${COMPONENT_XYNACLUSTER}" == "xtrue" ]]; then 
  install_xyna_cluster
  f_configure_network_availability_demon
fi

if [[ "x${COMPONENT_XYNAFACTORY}" == "xtrue" ]]; then 
  echo -e "\n* Starting Xyna Factory."
  start_xynafactory
  if [[ $(f_xynafactory version | ${VOLATILE_GREP} -c "Last successful Update") -eq 0 ]]; then
    echo "Factory Updates to ${RELEASE_NUMBER} succeeded"
  else 
    err_msg "Some Factory Updates failed: Expected version=$(f_get_factory_version), found version=$(f_xynafactory version | ${VOLATILE_AWK} '$1=="Last"{print $4}'). For further Information see logfile. Aborting installation."
    exit 1
  fi
fi

if f_selected ${COMPONENT_ORACLECLUSTER} ; then 
  f_install_oracle_cluster
fi

if f_selected ${COMPONENT_APPLICATIONS} ; then 
  import_applications
fi

if f_selected ${COMPONENT_XYNAFACTORY} ; then
  add_basic_requirements
fi

deploy_trigger
deploy_services
deploy_filter

if f_selected ${SET_XYNA_PROPERTIES} ; then
  set_properties;
fi

if f_selected ${ETC_INITD_FILES} ; then
  etc_initd_files;
fi

if f_selected ${DEPLOY_TARGET_GERONIMO} ${DEPLOY_TARGET_TOMCAT} ; then
  if f_selected ${COMPONENT_FRACTALMODELLERH5EN} ; then
    deploy_xfracmodh5en;
  fi;
  if f_selected ${COMPONENT_FRACTALMODELLERH5DE} ; then
    deploy_xfracmodh5de;
  fi;
fi



if [[ "${BOOL_CAROUSEL_NEEDS_TO_BE_CHECKED:-false}" == "true" ]]; then
  #TODO direkt in deploy_webservice_topologymodeller?
  enable_topologymodeller_slide
fi

register_repositoryaccesses

register_datamodeltypes

create_bashcompletion

#  bugz 13300: Restart ist erforderlich, damit alle gesetzten Properties zum Tragen kommen.
if f_is_true ${XYNAFACTORY_NEEDS_RESTART} ; then
  stop_xynafactory
  start_xynafactory
fi

if [[ "${CHECK_THIRD_PARTY_LICENSES}" != "none" ]] ; then
  check_third_party_licenses;
fi;

if [[ -f "${TMP_FILE}" ]]; then ${VOLATILE_RM} -f "${TMP_FILE}"; fi

}



if [[ ! -d $HOSTNAME ]] ; then
  echo "creating folder ${HOSTNAME}"
  mkdir $HOSTNAME
fi

LOG_FILE_NAME="${HOSTNAME}/installation_${INSTALL_PREFIX##*/}_$(date +%Y%m%d_%H%M%S).log"
if [[ -e ${LOG_FILE_NAME} ]] ; then
  attention_msg "Log file ${LOG_FILE_NAME} already exists; please try again..."
  exit 1
fi

echo -e "cd ${PWD}\n./install_black_edition.sh ${ALL_ARGS}" > ${LOG_FILE_NAME}
echo "Installation started at $(date)" >> ${LOG_FILE_NAME}
echo ""

f_install 2>&1 | tee -a ${LOG_FILE_NAME}

echo "Installation ended at $(date)" >> ${LOG_FILE_NAME}


#  EOF


