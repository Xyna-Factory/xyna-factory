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

#  Bootstrap-Installer
#  unzip XBE-Delivery and load it's libs

load_functions () {
  SOURCE_FILE="${1}"
  if [[ ! -f ${SOURCE_FILE} ]]; then
    echo "Unable to import functions from '${SOURCE_FILE}'. Abort!"; exit 99;
  else
    source ${SOURCE_FILE}
  fi
}

activate_testfactory_mode () {
  local WORKING_FOLDER="${1}"
  local testfactory_activation=$(${VOLATILE_GREP} "testFactory.testFactoryMode=" "${WORKING_FOLDER}/${FRACTAL_MODELLING_FILE}" | wc -l)
  if [[ ${testfactory_activation} == 0 ]]; then
    ${VOLATILE_CP} "${WORKING_FOLDER}/${FRACTAL_MODELLING_FILE}" "${TMP_FILE}"
    echo "testFactory.testFactoryMode=true" >> "${TMP_FILE}"
    # assuming sliding is unset if testFactoryMode is not set
    echo "forms.layout.animation.sliding=true" >> "${TMP_FILE}"
    echo "forms.layout.animation.sliding.effectDuration=400" >> "${TMP_FILE}"
  else 
    ${VOLATILE_SED} \
      -e "s/testFactory.testFactoryMode=false/testFactory.testFactoryMode=true/" \
      "${WORKING_FOLDER}/${FRACTAL_MODELLING_FILE}" > "${TMP_FILE}"
  fi
  ${VOLATILE_RM} -f "${WORKING_FOLDER}/${FRACTAL_MODELLING_FILE}"
  ${VOLATILE_MV} "${TMP_FILE}" "${WORKING_FOLDER}/${FRACTAL_MODELLING_FILE}"
  ${VOLATILE_CHMOD} 664 "${WORKING_FOLDER}/${FRACTAL_MODELLING_FILE}"
}

activate_testfactory_slide () {
  local WORKING_FOLDER="${1}"
  local testfactory_slide=$(${VOLATILE_GREP} "<Slide><Application Source=\"TestFactory\"/></Slide>" "${WORKING_FOLDER}/${TRIPTYCHON_FILE}" | wc -l)
  if [[ ${testfactory_slide} == 0 ]]; then
    ${VOLATILE_SED} \
    -e "s/<\/XynaPresenter>/  <Slide><Application Source=\"TestFactory\"\/><\/Slide>\n<\/XynaPresenter>/" "${WORKING_FOLDER}/${TRIPTYCHON_FILE}" > "${TMP_FILE}"
    ${VOLATILE_RM} -f "${WORKING_FOLDER}/${TRIPTYCHON_FILE}"
    ${VOLATILE_MV} "${TMP_FILE}" "${WORKING_FOLDER}/${TRIPTYCHON_FILE}"
    ${VOLATILE_CHMOD} 664 "${WORKING_FOLDER}/${TRIPTYCHON_FILE}"
  fi 
}

parse_commandline_arguments () {
  if [[ $# -lt 1 ]]; then DISPLAY_USAGE="true"; fi
  local ALL="false"
  while getopts ":ansdpfoDPFOi:" OPTION
  do
    if [[ "x${OPTARG:0:1}" == "x-" ]]; then DISPLAY_USAGE="true"; fi
    case ${OPTION} in
      n) DRY_RUN="true";;
      a) ALL="true";;
      p) SET_XYNA_PROPERTIES="true";;
      P) SET_XYNA_PROPERTIES="false";;
      d) INSTALL_AND_DISTRIBUTE_INFRASTRUCTURE_APP="true";;
      D) INSTALL_AND_DISTRIBUTE_INFRASTRUCTURE_APP="false";;
      f) ADJUST_FRACTAL_MODELLER="true";;
      F) ADJUST_FRACTAL_MODELLER="false";;
      o) REGISTER_ORDER_INPUT_SOURCE="true";;
      O) REGISTER_ORDER_INPUT_SOURCE="false";;
      s) SHOW_DEFAULT_XBE_INSTALLATION_PARAMS="true";;
      i) parse_instance_number "${OPTARG}";;
      *) DISPLAY_USAGE="true";;
    esac
  done
  if [[ ${ALL} == "true" ]]; then
    if [[ ! ${SET_XYNA_PROPERTIES}  ]]; then
      SET_XYNA_PROPERTIES="true"
    fi
    if [[ ! ${INSTALL_AND_DISTRIBUTE_INFRASTRUCTURE_APP}  ]]; then
      INSTALL_AND_DISTRIBUTE_INFRASTRUCTURE_APP="true"
    fi
    if [[ ! ${ADJUST_FRACTAL_MODELLER} ]]; then
      ADJUST_FRACTAL_MODELLER="true"
    fi
    if [[ ! ${REGISTER_ORDER_INPUT_SOURCE} ]]; then
      REGISTER_ORDER_INPUT_SOURCE="true"
    fi
  fi
}

display_usage () {
  ${VOLATILE_CAT} << A_HERE_DOCUMENT

  usage: "$(basename "$0")" -ansdpfoDPFO -i instance

-n    dry-run, display the evaluated commandline parameters
-i    specify instancenumber; default is 1
-a    execute all steps
-s    display default xyna factory installation parameters
-pP   [don't] set xyna properties
-dD   [don't] import and distribute testfactory infrastructure application
-fF   [don't] adjust and redeploy FractalModeller
-oO   [don't] register order input source

* lower-case letters mean to include the specific component
* UPPER-CASE letters mean to exclude the specific component
A_HERE_DOCUMENT
}

debug_variables () {
  echo "== Parameter =="
  echo "xynaproperties             : ${SET_XYNA_PROPERTIES:-false}"
  echo "infrastructure application : ${INSTALL_AND_DISTRIBUTE_INFRASTRUCTURE_APP:-false}"
  echo "fractal modeller           : ${ADJUST_FRACTAL_MODELLER:-false}"
  echo "order input source         : ${REGISTER_ORDER_INPUT_SOURCE:-false}"
}


validate_setup () {
  local XYNA_FACTORY_SH="${INSTALL_PREFIX}/server/xynafactory.sh"
  IS_XMOM_STORABLE_PL_ID_SET=$(${XYNA_FACTORY_SH} get xnwh.persistence.xmom.defaultpersistencelayerid | grep -v "is not set" | wc -l)
  if [[ ${IS_XMOM_STORABLE_PL_ID_SET} == "0" ]]; then
    f_exit_with_message ${EX_TESTFACTORY} "Factory is not properly configured aborting"
  fi
}

parse_instance_number () {
  export INSTANCE_NUMBER="${1}"
  if [[ 1 -le ${INSTANCE_NUMBER} && ${INSTANCE_NUMBER} -le ${BLACK_EDITION_INSTANCES} ]]; then
    #  nichts machen, alles bestens
    :
  else
    #  Parameter nicht richtig; Hilfe ausgeben.
    echo "Error: Instance number '-i' is not in the allowed range [1..${BLACK_EDITION_INSTANCES}]"
    DISPLAY_USAGE="true"
  fi
}

#############
# Constants #
#############

EX_TESTFACTORY=78
DELIVERY_ITEM_SUBFOLDER="factory"
DELIVERY_ITEM_FIND_EXPRESSION="XynaBlackEdition*.zip"
PRE_DELIVERY_LIB_IMPORT="./lib/system_lib.sh"
MAIN_LIB_IMPORT="func_lib/func_lib.sh"

##############
# Main Logic #
##############



# used for VOLATILE_FIND & VOLATILE_UNZIP
load_functions ${PRE_DELIVERY_LIB_IMPORT}

get_volatile_settings

delivery_item=$(${VOLATILE_FIND} ${DELIVERY_ITEM_SUBFOLDER} -maxdepth 1 -name ${DELIVERY_ITEM_FIND_EXPRESSION})

if [[ ${#delivery_item} == 0 ]]; then
  item_count=0
else
  item_count=$(echo "${delivery_item}" | wc -l)
fi

case ${item_count} in
  0 ) echo "No delivery item found in" ${DELIVERY_ITEM_SUBFOLDER} >&2 
      exit -255;;
  1 ) echo "Found delivery item: " ${delivery_item};;
  * ) echo ${item_count} "delivery items found in" ${DELIVERY_ITEM_SUBFOLDER} >&2 
      exit -255;;
esac

delivery_folder=${delivery_item%.zip}
if [[ ! -d "${delivery_folder}" ]]; then
  ${VOLATILE_UNZIP} ${delivery_item} -d ${DELIVERY_ITEM_SUBFOLDER}
fi

load_functions "${delivery_folder}/${MAIN_LIB_IMPORT}"

f_read_host_properties
parse_commandline_arguments "$@"
f_read_product_properties


if [[ (-n ${DISPLAY_USAGE}) && (${DISPLAY_USAGE}=="true") ]]; then
  display_usage
  exit
fi

if [[ (-n ${DRY_RUN}) && (${DRY_RUN}=="true") ]]; then
  debug_variables
  exit
fi

if [[ (-n ${SHOW_DEFAULT_XBE_INSTALLATION_PARAMS}) && (${SHOW_DEFAULT_XBE_INSTALLATION_PARAMS}=="true") ]]; then
  echo "./install_black_edition.sh -a -D geronimo,oracle -F nsnhix5600,dhcp_v4,radius -S nsnhix5600,jmsforwarding,sipuseragent,dhcp_v4,ldapservices -T nsnhix5600,dhcp_v4,radius -W topologymodeller"
  exit
fi

validate_setup

SERVER_HOME=${INSTALL_PREFIX}"/server/"

if [[ ( -n ${REGISTER_ORDER_INPUT_SOURCE} ) && ( ${REGISTER_ORDER_INPUT_SOURCE} == "true" ) ]]; then
  echo "* registering XTFInputSource"
  if [[ ! -d  ${SERVER_HOME} ]]; then
    f_exit_with_message ${EX_TESTFACTORY} "Factory installation could not be found in ${SERVER_HOME}"
  fi
  if [[ ! -d  ${SERVER_HOME}"/orderinputsourcetypes" ]]; then
    ${VOLATILE_MKDIR} ${SERVER_HOME}"/orderinputsourcetypes"
  fi
  ${VOLATILE_CP} -rf "orderinputsourcetypes/XTFInputSource" ${SERVER_HOME}"/orderinputsourcetypes"
  REGISTRATION_RESPONSE=$(${SERVER_HOME}xynafactory.sh registerorderinputsourcetype -name XTFInputSource -fqClassName com.gip.xyna.xfmg.xods.orderinputsource.xtf.XTFInputSourceType)
  REGISTRATION_ERRORS=$(echo ${REGISTRATION_RESPONSE} | grep "Error" | wc -l)
  if [[ ${REGISTRATION_ERRORS} -gt  "0" ]]; then
    f_exit_with_message ${EX_TESTFACTORY} "Failed to register XTFInputSource: ${REGISTRATION_RESPONSE}"
  fi
fi

if [[ ( -n ${SET_XYNA_PROPERTIES} ) && ( ${SET_XYNA_PROPERTIES} == "true" ) ]]; then
  echo "* setting xyna properties"
  ${SERVER_HOME}xynafactory.sh set xyna.global.set.ordercontext true
fi

if [[ ( -n ${INSTALL_AND_DISTRIBUTE_INFRASTRUCTURE_APP} ) && ( ${INSTALL_AND_DISTRIBUTE_INFRASTRUCTURE_APP} == "true" ) ]]; then
  echo "* installing and distributing testfactory infrastructure application"
  application_item=$(${VOLATILE_FIND} . -maxdepth 1 -name "XynaTestFactoryInfrastructure*.[az][ip]p")
  if [[ ${#application_item} == 0 ]]; then
    item_count=0
  else
    item_count=$(echo "${delivery_item}" | wc -l)
  fi
  case ${item_count} in
    0 ) f_exit_with_message ${EX_TESTFACTORY} "No application item found";;
    1 ) echo "   + found application item: " ${application_item};;
    * ) f_exit_with_message ${EX_TESTFACTORY} "${item_count} applications items found";;
  esac
  ${VOLATILE_CP} -f ${application_item} ${SERVER_HOME}
  echo "   + starting application import"
  ${VOLATILE_MKDIR} components
  ${VOLATILE_MV} "${application_item}" components
  f_import_applications_internal ${application_item}
  ${VOLATILE_MV} components/"${application_item}" .
  ${VOLATILE_RM} -r components
fi


if [[ ( -n ${ADJUST_FRACTAL_MODELLER} ) && ( ${ADJUST_FRACTAL_MODELLER} == "true" ) ]]; then
  echo "* adjusting FractalModeller"
  if [[ ! -d "${TOMCAT_HOME}" ]]; then
    f_exit_with_message ${EX_OSFILE} "unable to locate Apache Tomcat installation in ${TOMCAT_HOME}"
  fi
  if [[ ! -d "${TOMCAT_HOME}/webapps" ]]; then
    f_exit_with_message ${EX_OSFILE} "unable to locate Apache Tomcat installation in ${TOMCAT_HOME}"
  fi
  DEPLOYED_WAR_FILE="${TOMCAT_HOME}/webapps/FractalModeller.war"
  if [[ ! -f ${DEPLOYED_WAR_FILE} ]]; then
    f_exit_with_message ${EX_OSFILE} "unable to locate FractalModeller installation in ${TOMCAT_HOME}/webapps"
  fi
  
  STR_TMP_UNPACK_DIR="/tmp/install_testfactory"
  ${VOLATILE_RM} -rf "${STR_TMP_UNPACK_DIR}"
  echo "    + modifying war file '${DEPLOYED_WAR_FILE}'"
  MODIFIED_WAR_FILE="${STR_TMP_UNPACK_DIR}/FractalModeller.war"
  if [[ -f ${MODIFIED_WAR_FILE} ]]; then
    ${VOLATILE_RM} "${MODIFIED_WAR_FILE}"
  fi 
  ${VOLATILE_MKDIR} -p "${STR_TMP_UNPACK_DIR}"
  ${VOLATILE_CP} "${DEPLOYED_WAR_FILE}" "${MODIFIED_WAR_FILE}"
  
  FRACTAL_MODELLING_FILE="FractalModelling.properties"
  ${VOLATILE_UNZIP} "${MODIFIED_WAR_FILE}" "${FRACTAL_MODELLING_FILE}" -d "${STR_TMP_UNPACK_DIR}"
  if [[ ! -f "${STR_TMP_UNPACK_DIR}/${FRACTAL_MODELLING_FILE}" ]]; then
    ${VOLATILE_RM} -f "${MODIFIED_WAR_FILE}"
    f_exit_with_message ${EX_OSFILE} "${FRACTAL_MODELLING_FILE} not found in ${DEPLOYED_WAR_FILE}"
  fi
  
  activate_testfactory_mode "${STR_TMP_UNPACK_DIR}"

  TRIPTYCHON_FILE="TriptychonConfig.xml"
  ${VOLATILE_UNZIP} "${MODIFIED_WAR_FILE}" "${TRIPTYCHON_FILE}" -d "${STR_TMP_UNPACK_DIR}"
  if [[ ! -f "${STR_TMP_UNPACK_DIR}/${TRIPTYCHON_FILE}" ]]; then
    ${VOLATILE_RM} -f "${MODIFIED_WAR_FILE}"
    f_exit_with_message ${EX_OSFILE} "${TRIPTYCHON_FILE} not found in ${DEPLOYED_WAR_FILE}"
  fi

  activate_testfactory_slide "${STR_TMP_UNPACK_DIR}"
  
  cd ${STR_TMP_UNPACK_DIR}
  ${VOLATILE_ZIP} "${MODIFIED_WAR_FILE}" -d "${FRACTAL_MODELLING_FILE}"
  ${VOLATILE_ZIP} "${MODIFIED_WAR_FILE}" "${FRACTAL_MODELLING_FILE}"
  ret_val=$?
  if [[ ${ret_val} -gt 0 ]]; then
    ${VOLATILE_RM} -rf ${STR_TMP_UNPACK_DIR}
    err_msg "Building of modified war-file failed"
    if [[ -f "${WAR_DELIVERY}.org" ]]; then
      ${VOLATILE_MV} "${WAR_DELIVERY}.org" "${WAR_DELIVERY}"
    fi   
  fi
  ${VOLATILE_ZIP} "${MODIFIED_WAR_FILE}" -d "${TRIPTYCHON_FILE}"
  ${VOLATILE_ZIP} "${MODIFIED_WAR_FILE}" "${TRIPTYCHON_FILE}"
  ret_val=$?
  if [[ ${ret_val} -gt 0 ]]; then
    ${VOLATILE_RM} -rf ${STR_TMP_UNPACK_DIR}
    err_msg "Building of modified war-file failed"
    if [[ -f "${WAR_DELIVERY}.org" ]]; then
      ${VOLATILE_MV} "${WAR_DELIVERY}.org" "${WAR_DELIVERY}"
    fi   
  fi
  cd -
  echo "   + deploying modified files"
  f_deploy_war_into_tomcat "${MODIFIED_WAR_FILE}" "FractalModeller"
  ${VOLATILE_RM} -rf ${STR_TMP_UNPACK_DIR}
fi
  
echo "Installation finished."
