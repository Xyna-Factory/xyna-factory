#!/bin/bash

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2024 Xyna GmbH, Germany
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
#  Diese datei wird von install_black_edition.sh geladen.
#  Alle Funktionen befinden sich hier.

#  Namen der einzelnen Komponenten muessen eindeutig sein!
#+ Es wird mit grep der CSV-String geprueft...
ALL_COMPONENTS=("filter" "trigger" "services" "xynacluster" "oraclecluster" "xynafactory", "fractalmodellerh5en", "fractalmodellerh5de")
ALL_FILTERS=("nsnhix5600" "dhcp_v4" "radius")
ALL_TRIGGERS=("nsnhix5600" "dhcp_v4" "radius")
ALL_SERVICES=("nsnhix5600" "templatemechanism" "sipuseragent" "jmsforwarding" "dhcp_v4")
ALL_DEPLOY_TARGETS=("geronimo" "tomcat" "oracle")
ALL_DATAMODELTYPES=("mib","tr069","xsd");
#ACHTUNG: Version auch bei addRequirement zu default workspace berücksichtigen
ALL_APPLICATIONS="Base Processing"; #Default-Applications, die immer installiert sein sollten
APPMGMTVERSION=1.0.10
GUIHTTPVERSION=1.1.390
SNMPSTATVERSION=1.0.3
PROCESSINGVERSION=1.0.21
ALL_REPOSITORYACCESSES=("svn");
INSTANCE_NUMBER="1" #1 ist default

parse_commandline_arguments () {
  MERGE_MODE="merge";
  BASH_COMPLETION="true";
  LOG4J2_MERGE="true";
  CHECK_THIRD_PARTY_LICENSES="none";
  COMPONENT_APPLICATIONS="true";
  while getopts ":nvhH?ablpABLP3:c:d:f:g:i:m:r:s:t:w:x:C:D:F:G:R:S:T:W:X:" OPTION
  do
    if [[ "x${OPTARG:0:1}" == "x-" ]]; then DISPLAY_USAGE="true"; echo "optarg ${OPTARG:0:1}"; fi
    case ${OPTION} in
      n) DRY_RUN="true";;
      v) VERBOSE="true";;
      b) BASH_COMPLETION="true";;
      B) BASH_COMPLETION="false";;
      l) LOG4J2_MERGE="true";;
      L) LOG4J2_MERGE="false";;
      p) SET_XYNA_PROPERTIES="true";;
      P) SET_XYNA_PROPERTIES="false";;
      3) parse_third_party_mode    "${OPTARG}";;
      i) parse_instance_number     "${OPTARG}";;
      m) parse_merge_mode          "${OPTARG}";;
      a) parse_components          "ALL"       "true";;
      A) parse_components          "ALL"       "false";;
      c) parse_components          "${OPTARG}" "true";;
      C) parse_components          "${OPTARG}" "false";;
      d) parse_deploy_targets      "${OPTARG}" "true";;
      D) parse_deploy_targets      "${OPTARG}" "false";;
      f) parse_filters             "${OPTARG}" "true";;
      F) parse_filters             "${OPTARG}" "false";;
      g) parse_datamodeltypes      "${OPTARG}" "true";;
      G) parse_datamodeltypes      "${OPTARG}" "false";;
      r) parse_repositoryaccesses  "${OPTARG}" "true";;
      R) parse_repositoryaccesses  "${OPTARG}" "false";;
      s) parse_services            "${OPTARG}" "true";;
      S) parse_services            "${OPTARG}" "false";;
      t) parse_triggers            "${OPTARG}" "true";;
      T) parse_triggers            "${OPTARG}" "false";;
      x) parse_applications        "${OPTARG}" "true";; 
      X) parse_applications        "${OPTARG}" "false";;
      h|H) DISPLAY_USAGE="true";;
      ?) if [[ ${OPTARG} == "?" ]] ; then 
           DISPLAY_USAGE="true";
         else
           #unbekannte Option, daher mit Fehler abbrechen
           display_usage;
           exit 1;
         fi
         ;;
      *) #unimplementierte Option, daher mit Fehler abbrechen
         display_usage;
         exit 1;
         ;;
    esac
  done
}

#  Parameter 1: String der geprueft werden soll, Parameter 2: Wert, den die Variable bekommen soll ("true" oder "false")
parse_components () {
  for v in $(f_split_to_array "${1}") ; do
    case $v in
      filter)            parse_filters            "ALL" "${2}";;
      services)          parse_services           "ALL" "${2}";;
      trigger)           parse_triggers           "ALL" "${2}";;
      repositories)      parse_repositoryaccesses "ALL" "${2}";;
      datamodeltypes)    parse_datamodeltypes     "ALL" "${2}";;
      applications)      parse_applications       "ALL" "${2}";;
      xynacluster)       COMPONENT_XYNACLUSTER="${2}";;
      oraclecluster)     COMPONENT_ORACLECLUSTER="${2}";;
      xynafactory)       COMPONENT_XYNAFACTORY="${2}";;
      fractalmodellerh5en)COMPONENT_FRACTALMODELLERH5EN="${2}";;
      fractalmodellerh5de)COMPONENT_FRACTALMODELLERH5DE="${2}";;
      ALL)
        parse_filters            "ALL" "${2}";
        parse_services           "ALL" "${2}";
        parse_triggers           "ALL" "${2}";
        parse_repositoryaccesses "ALL" "${2}";
        parse_datamodeltypes     "ALL" "${2}";
        parse_applications       "ALL" "${2}";
        COMPONENT_XYNAFACTORY="${2}";
        COMPONENT_FRACTALMODELLERH5EN="${2}";
        COMPONENT_FRACTALMODELLERH5DE="${2}";
        parse_deploy_targets "${1}" "${2}"
        SET_XYNA_PROPERTIES="${2}"
        ;;
      *) attention_msg "Ignoring unknown component \"$v\"";
    esac
  done;
}

#  Parameter 1: String der geprueft werden soll, Parameter 2: Wert, den die Variable bekommen soll ("true" oder "false")
parse_filters () {
  if [[ "x${1}" == "xALL" || $(echo "${1}" | ${VOLATILE_GREP} -c nsnhix5600           ) -gt 0 ]]; then FILTER_NSN_HIX5600="${2}"; fi
  if [[ "x${1}" == "xALL" || $(echo "${1}" | ${VOLATILE_GREP} -c dhcp_v4              ) -gt 0 ]]; then FILTER_DHCP_V4="${2}"; fi
  if [[ "x${1}" == "xALL" || $(echo "${1}" | ${VOLATILE_GREP} -c radius               ) -gt 0 ]]; then FILTER_RADIUS="${2}"; fi
}

#  Parameter 1: String der geprueft werden soll, Parameter 2: Wert, den die Variable bekommen soll ("true" oder "false")
parse_services () {
  for v in $(f_split_to_array "${1}") ; do
    case $v in
      nsnhix5600)           SERVICE_NSN_HIX5600="${2}";;
      templatemechanism)    SERVICE_TEMPLATEMECHANISM="${2}";;
      sipuseragent)         SERVICE_SIPUSERAGENT="${2}";;
      jmsforwarding)        SERVICE_JMSFORWARDING="${2}";;
      dhcp_v4)              SERVICE_DHCP_V4="${2}";;
      
      ALL)
        SERVICE_NSN_HIX5600="${2}";
        SERVICE_TEMPLATEMECHANISM="${2}";
        SERVICE_SIPUSERAGENT="${2}";
        SERVICE_JMSFORWARDING="${2}";
        SERVICE_DHCP_V4="${2}";
        ;;
      *) attention_msg "Ignoring unknown service \"$v\"";
    esac
  done;
}

#  Parameter 1: String der geprueft werden soll, Parameter 2: Wert, den die Variable bekommen soll ("true" oder "false")
parse_triggers () {
  if [[ "x${1}" == "xALL" || $(echo "${1}" | ${VOLATILE_GREP} -c nsnhix5600           ) -gt 0 ]]; then TRIGGER_NSN_HIX5600="${2}"; fi
  if [[ "x${1}" == "xALL" || $(echo "${1}" | ${VOLATILE_GREP} -c dhcp_v4              ) -gt 0 ]]; then TRIGGER_DHCP_V4="${2}"; fi
  if [[ "x${1}" == "xALL" || $(echo "${1}" | ${VOLATILE_GREP} -c radius              ) -gt 0 ]]; then TRIGGER_RADIUS="${2}"; fi
}

#  Parameter 1: String der geprueft werden soll, Parameter 2: Wert, den die Variable bekommen soll ("true" oder "false")
parse_deploy_targets () {
  if [[ "x${1}" == "xALL" || $(echo "${1}" | ${VOLATILE_GREP} -c geronimo             ) -gt 0 ]]; then DEPLOY_TARGET_GERONIMO="${2}"; fi
  if [[ "x${1}" == "xALL" || $(echo "${1}" | ${VOLATILE_GREP} -c oracle               ) -gt 0 ]]; then DEPLOY_TARGET_ORACLE="${2}"; fi
  if [[ "x${1}" == "xALL" || $(echo "${1}" | ${VOLATILE_GREP} -c tomcat               ) -gt 0 ]]; then DEPLOY_TARGET_TOMCAT="${2}"; fi
}


#  Parameter 1: String der geprueft werden soll, Parameter 2: Wert, den die Variable bekommen soll ("true" oder "false")
parse_repositoryaccesses () {
    for v in $(f_split_to_array "${1}") ; do
    case $v in
      svn) REPOSITORYACCESS_SVN="${2}";;
      ALL)
        REPOSITORYACCESS_SVN="${2}";
        ;;
      *) attention_msg "Ignoring unknown repositoryaccess \"$v\"";
     esac
  done;
}

#  Parameter 1: String der geprueft werden soll, Parameter 2: Wert, den die Variable bekommen soll ("true" oder "false")
parse_datamodeltypes () {
  for v in $(f_split_to_array "${1}") ; do
    case $v in
      mib)   DATAMODELTYPE_MIB="${2}";;
      tr069) DATAMODELTYPE_TR069="${2}";;
      xsd)   DATAMODELTYPE_XSD="${2}"; SERVICE_TYPEGENERATION="${2}";;
      ALL)
        DATAMODELTYPE_MIB="${2}";
        DATAMODELTYPE_TR069="${2}";
        DATAMODELTYPE_XSD="${2}"; SERVICE_TYPEGENERATION="${2}";
        ;;
      *) attention_msg "Ignoring unknown datamodeltype \"$v\"";
     esac
  done;
}

#  Parameter 1: String der geprueft werden soll, Parameter 2: Wert, den die Variable bekommen soll ("true" oder "false")
parse_applications () {
  local APP_LIST=$(echo ${1} | ${VOLATILE_TR} ',' ' ' );
  #echo "APP_LIST=${APP_LIST}";
  if f_is_in_list APP_LIST "ALL" ; then
    #ALL 
    if f_is_true $2 ; then
      ALL_APPLICATIONS=$(f_list_applications_in_components);
    else
      ALL_APPLICATIONS="";
      COMPONENT_APPLICATIONS="false";
    fi;
  else
    #Einzel-Apps
    for v in ${APP_LIST} ; do
      if ! f_application_exists_in_components $v ; then
        attention_msg "Ignoring unknown application \"$v\"";
      else
        if f_is_true $2 ; then
          ALL_APPLICATIONS=$(f_add_to_list ALL_APPLICATIONS ${v});
        else
          ALL_APPLICATIONS=$(f_remove_from_list ALL_APPLICATIONS ${v});
        fi;
      fi;
    done;
  fi;
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

parse_merge_mode () {
  export MERGE_MODE="${1}"
  case ${MERGE_MODE} in 
    merge|customized|new|abort)
      ;;
    *) 
      #  Parameter nicht richtig; Hilfe ausgeben.
      echo "Error: Merge mode not in [merge|customized|new|abort]"
      DISPLAY_USAGE="true"
      ;;
  esac
}

parse_third_party_mode () {
  CHECK_THIRD_PARTY_LICENSES="${1}"
  case ${CHECK_THIRD_PARTY_LICENSES} in 
    server|revision|delivery|none|dir=*|app=*)
      ;;
    *) 
      #  Parameter nicht richtig; Hilfe ausgeben.
      echo "Error: Check-Third-Party-License mode not in [server|revision|delivery|none|dir=*|app=*]"
      DISPLAY_USAGE="true"
      ;;
  esac
}

#  print all variables for debugging, then exit
debug_variables () {
  echo "instance_number      : ${INSTANCE_NUMBER}"
  case ${MERGE_MODE} in
    merge)      echo "merge-mode           : merge: in case of conflict installation halts, user has to resolve conflict and continue"  ;;
    customized) echo "merge-mode           : customized: in case of conflict customized version will be kept"  ;;
    new)        echo "merge-mode           : new: in case of conflict customized version will be replaced"  ;;
    abort)      echo "merge-mode           : abort: in case of conflict installation will be aborted"  ;;
    *)          ;; #kann nicht vorkommen
  esac
  echo "verbose              : ${VERBOSE:-false}"
  echo "bash-completion      : ${BASH_COMPLETION:-false}"
  echo "log4j2-merge         : ${LOG4J2_MERGE:-false}"

  echo "== Components =="
  echo "xynafactory          : ${COMPONENT_XYNAFACTORY:-false}"
  echo "fractalmodellerh5de  : ${COMPONENT_FRACTALMODELLERH5DE:-false}"
  echo "fractalmodellerh5en  : ${COMPONENT_FRACTALMODELLERH5EN:-false}"
  echo "xynacluster          : ${COMPONENT_XYNACLUSTER:-false}"
  echo "oraclecluster        : ${COMPONENT_ORACLECLUSTER:-false}"
  echo "set_xyna_properties  : ${SET_XYNA_PROPERTIES:-false}"

  echo "== Filter =="
  echo "nsnhix5600           : ${FILTER_NSN_HIX5600:-false}"
  echo "dhcp v4              : ${FILTER_DHCP_V4:-false}"
  echo "radius               : ${FILTER_RADIUS:-false}"

  echo "== Services =="
  echo "nsnhix5600           : ${SERVICE_NSN_HIX5600:-false}"
  echo "templatemechanism    : ${SERVICE_TEMPLATEMECHANISM:-false}"
  echo "sipuseragent         : ${SERVICE_SIPUSERAGENT:-false}"
  echo "jmsforwarding        : ${SERVICE_JMSFORWARDING:-false}"
  echo "dhcp v4              : ${SERVICE_DHCP_V4:-false}"
  echo "radius               : ${SERVICE_RADIUS:-false}"
  
  echo "== Trigger =="
  echo "nsnhix5600           : ${TRIGGER_NSN_HIX5600:-false}"
  echo "dhcp v4              : ${TRIGGER_DHCP_V4:-false}"
  echo "radius               : ${TRIGGER_RADIUS:-false}"

  echo "== Deploy-Targets =="
  echo "geronimo             : ${DEPLOY_TARGET_GERONIMO:-false}"
  echo "oracle               : ${DEPLOY_TARGET_ORACLE:-false}"
  echo "tomcat               : ${DEPLOY_TARGET_TOMCAT:-false}"

  echo "== RepositoryAccess =="
  echo "svn                  : ${REPOSITORYACCESS_SVN:-false}"
 
  echo "== DataModelTypes =="
  echo "mib                  : ${DATAMODELTYPE_MIB:-false}"
  echo "tr069                : ${DATAMODELTYPE_TR069:-false}"
  echo "xsd                  : ${DATAMODELTYPE_XSD:-false}"
  
  echo "== Applications =="
  echo "The following applications are selected: ${ALL_APPLICATIONS}"
}

display_usage () {
  local DONOT="[don't]";
  ${VOLATILE_CAT} << A_HERE_DOCUMENT

  usage: "$(basename "$0")" -nvabpABEP -cdfgqrstCDFGQRST [csv list] -i instance -m merge -3 check

-n    dry-run, display the evaluated commandline parameters
-v    verbose mode, display factory commands
-i    specify instancenumber; default is 1
-m    specify merge-mode for merging configuration files, e.g. server.policy, log4j2.xml
      possible values are merge, customized, new, abort; default is merge
-3    specify check-mode for checking third party licenses
      possible values are server, revision, delivery, none
-aA   include / exclude all components except 'xynacluster', 'oraclecluster'
-bB   ${DONOT} configure bash_completion
-lL   ${DONOT} merge log4j2.xml
-pP   ${DONOT} set xyna properties
-dD   ${DONOT} deploy in one or more of the following application servers:
    ALL ${ALL_DEPLOY_TARGETS[@]}
-cC   ${DONOT} install one or more of the following components:
    ALL ${ALL_COMPONENTS[@]}
-fF   ${DONOT} install one or more of the following filters:
    ALL ${ALL_FILTERS[@]}
-gG   ${DONOT} install one or more of the following datamodeltypes:
    ALL ${ALL_DATAMODELTYPES[@]}
-rR   ${DONOT} install one or more of the following repositoryaccesses:
    ALL ${ALL_REPOSITORYACCESSES[@]}
-sS   ${DONOT} install one or more of the following services:
    ALL ${ALL_SERVICES[@]}
-tT   ${DONOT} install one or more of the following triggers:
    ALL ${ALL_TRIGGERS[@]}
-xX   ${DONOT} install one or more of the following applications:
    ALL $(f_list_applications_in_components);

* lower-case letters mean to include the specific component
* UPPER-CASE letters mean to exclude the specific component
* Global component name means 'ALL' for specific componnent
* subsequent arguments override prior arguments

Examples:

(1) "$(basename "$0")" -a -C fractalmodellerh5de
      install everything, but exclude the FractalModeller

(2) "$(basename "$0")" -A -c fractalmodellerh5en,fractalmodellerh5de
      install nothing, but include the FractalModeller

(3) "$(basename "$0")" -c filter
    "$(basename "$0")" -f ALL
      both calls are identical

(4) "$(basename "$0")" -c filter,trigger -F snmp -f ALL
      install filter and trigger
      The prior exclusion of snmp filter is overridden by the subsequent
      inclusion of all filters.

A_HERE_DOCUMENT
}


f_indent () {
  local INDENT=${1};
  shift 1
  local OUTPUT="$@"
  echo "${OUTPUT}" | sed "s+^+${INDENT}+" #Einrücken
}



f_deploy_war () {
  if f_selected ${DEPLOY_TARGET_TOMCAT} ; then
    f_deploy_war_into_tomcat "$@" true true false
  fi;
  if f_selected ${DEPLOY_TARGET_GERONIMO} ; then
    f_deploy_war_into_geronimo "$@"
  fi;
  if f_selected ${DEPLOY_TARGET_ORACLE} ; then
    f_deploy_war_into_oracle "$@"
  fi;
}

f_deploy_target () {
  if f_selected ${DEPLOY_TARGET_TOMCAT} ; then
    echo "Apache Tomcat"
  fi;
  if f_selected ${DEPLOY_TARGET_GERONIMO} ; then
    echo "Apache Geronimo"
  fi;
  if f_selected ${DEPLOY_TARGET_ORACLE} ; then
    echo "Oracle AS"
  fi;
  
}


f_install_license () {
  echo -e "\n    + Install licenses"
  local WAR_DELIVERY="${1}"
  local STR_DELIVERY_NAME=$(basename ${WAR_DELIVERY} .war)
  local STR_DELIVERY_PATH=$(dirname ${WAR_DELIVERY})
  local STR_TMP_UNPACK_DIR="/tmp/${STR_DELIVERY_NAME}"
  
  local OUTPUT=$(${VOLATILE_UNZIP} -C ${WAR_DELIVERY} "*.jar" -d ${STR_TMP_UNPACK_DIR})
  
  for JAR in $(${VOLATILE_FIND} ${STR_TMP_UNPACK_DIR} -name "*.jar" | ${VOLATILE_SORT}) ; do
    copy_license ${JAR} "    |   "
  done;

  rm -rf ${STR_TMP_UNPACK_DIR}
}


deploy_xfracmodh5de () {
  FILE_TO_DEPLOY="${PWD}/server/xfracmod/modeller_de-DE.war"
  if [[ -f "${FILE_TO_DEPLOY}" ]]; then
    echo -e "\n* Deploying HTML5-Modeller (de-DE) into $(f_deploy_target)"
    if f_selected ${DEPLOY_TARGET_GERONIMO} ; then
      # not supported
      echo "ERROR: Only tomcat is supported"
      exit 4
    fi;
    if f_selected ${DEPLOY_TARGET_ORACLE} ; then
      # not supported
      echo "ERROR: Only tomcat is supported"
      exit 4
    fi;
    f_deploy_war "${FILE_TO_DEPLOY}" "modeller_de-DE"
  fi
}

deploy_xfracmodh5en () {
 FILE_TO_DEPLOY="${PWD}/server/xfracmod/modeller_en-US.war"
  if [[ -f "${FILE_TO_DEPLOY}" ]]; then
    echo -e "\n* Deploying HTML5-Modeller (en-US) into $(f_deploy_target)"
    if f_selected ${DEPLOY_TARGET_GERONIMO} ; then
      # not supported
      echo "ERROR: Only tomcat is supported"
      exit 4
    fi;
    if f_selected ${DEPLOY_TARGET_ORACLE} ; then
      # not supported
      echo "ERROR: Only tomcat is supported"
      exit 4
    fi;
    f_deploy_war "${FILE_TO_DEPLOY}" "modeller_en-US"
  fi
}


check_oc4j_container_status () {
  if [[ "x${1}" == "x" ]]; then
    #  kein Instanzname angegeben: Erste Installation oder Container geloescht.
    echo "NEW"
  else
    VOLATILE_OPMNCTL=$(get_full_path_to_executable opmnctl)
    #  Liste laufender OC4J-Instanzen bestimmen:
    #     opmnctl status -fmt %prt50%sta8
    # process-type                                       | status
    # ---------------------------------------------------+---------
    # OC4J:Project7                                      | Alive
    # OC4J:Project6                                      | Down
    # HTTP_Server                                        | Alive
    #
    #  Zeile mit dem Container filtern und den Status aus der letzten Spalte ausgeben
    echo "$(${VOLATILE_OPMNCTL} status -fmt %prt50%sta8 | ${VOLATILE_AWK} '/^OC4J:'"${1}"'/ {print $NF}')"
  fi
}

select_running_oc4j_container () {
  VOLATILE_OPMNCTL=$(get_full_path_to_executable opmnctl)
  #  Liste laufender OC4J-Instanzen bestimmen:
  #     opmnctl status -fmt %prt50%sta8
  # process-type                                       | status
  # ---------------------------------------------------+---------
  # OC4J:Project7                                      | Alive
  # OC4J:Project6                                      | Down
  # HTTP_Server                                        | Alive
  #
  # Zeile beginnt mit OC4J, Status ist 'Alive', dann erste Spalte mit Trenner ':' nochmal durchsuchen, danach ist das Ergebnis in der letzten Spalte
  ORACLE_OC4J_CONTAINER=$(${VOLATILE_OPMNCTL} status -fmt %prt50%sta8 | ${VOLATILE_AWK} '/^OC4J/ && $NF == "Alive" {print $1}' | ${VOLATILE_AWK} 'BEGIN { FS=":"} {print $2}')
  #  Benutzer muss einen der laufenden Container auswaehlen
  number_of_matches=$(echo "${ORACLE_OC4J_CONTAINER}" | ${VOLATILE_AWK} 'END {print NR}')
  if [[ "x" == "x${ORACLE_OC4J_CONTAINER}" ]]; then number_of_matches="0"; fi
  case ${number_of_matches} in
    0)  err_msg "Unable to determine running OC4J instances."
        echo
        echo "Hint: Check Application Server status using 'opmnctl status'..."
        echo
        return
        ;;
    1)  # nothing to be done, instance found only once
        ;;
    *)  # multiple entries matched - choose one
        OWN_OC4J_SELECTION="0"
        until [[ ${OWN_OC4J_SELECTION} -ge 1 && ${OWN_OC4J_SELECTION} -le ${number_of_matches} ]]; do
          echo "Following OC4J instances found:"
          let OWN_OC4J_INDEX=1
          for i in ${ORACLE_OC4J_CONTAINER}; do
            echo "  (${OWN_OC4J_INDEX}): ${i}"
            let OWN_OC4J_INDEX=OWN_OC4J_INDEX+1
          done
          echo; echo -n "Enter number to choose one: "
          read OWN_OC4J_SELECTION
          if [[ -z ${OWN_OC4J_SELECTION} ]]; then OWN_OC4J_SELECTION=0; fi
        done
        let OWN_OC4J_INDEX=1
        for i in ${ORACLE_OC4J_CONTAINER}; do
          if [[ ${OWN_OC4J_SELECTION} -eq ${OWN_OC4J_INDEX} ]]; then
            ORACLE_OC4J_CONTAINER="${i}"
          fi
          let OWN_OC4J_INDEX=OWN_OC4J_INDEX+1
        done
        ;;
  esac
}


deploy_war_into_oracle () {
  COMPONENT_WAR_FILE="${1}"
  COMPONENT_NAME="${2}"

  if [[ "x${COMPONENT_WAR_FILE}" == "x" || "x" == "x${ORACLE_HOME}" || ! -d "${ORACLE_HOME}/opmn" ]]; then
    if [[ "x" != "x${ORACLE_HOME}" || -d "${ORACLE_HOME}/opmn" ]]; then
      err_msg "ORACLE_HOME ${ORACLE_HOME} is not set or does not point to an Application Server - skipping deployment"
    else 
      err_msg "Unable to work without war-file."
    fi
  else
    echo -e "\n* Deploying ${COMPONENT_NAME} into Oracle Application Server"

    if [[ "x" == "x${ORACLE_HOME}" ]]; then
      echo "    + \$ORACLE_HOME is not set - skipping"
      return
    fi
    if [[ ! -d "${ORACLE_HOME}/opmn" ]]; then
      echo "    + \$ORACLE_HOME does not point to an Application Server - skipping"
      return
    fi
    
    choose_interface "Oracle AS" "network.interface.oracle.as"
    
    VOLATILE_JAVA=$(get_full_path_to_executable java)
    VOLATILE_OPMNCTL=$(get_full_path_to_executable opmnctl)

    #  bugz 10345: ORACLE_HOME speichern und nachfragen, ob trotzdem deplyoed werden soll, falls es sich geaendert hat
    PROPERTY_NAME="env.oracle.home"
    get_property ${PROPERTY_NAME} "${INSTANCE_PROP_FILE}"
    if [[ "x" == "x${CURRENT_PROPERTY}" ]]; then
      set_property "${PROPERTY_NAME}" "${ORACLE_HOME}" "${INSTANCE_PROP_FILE}"
    else
      if [[ "x${CURRENT_PROPERTY}" != "x${ORACLE_HOME}" ]]; then
        echo "\$ORACLE_HOME is set to: ${ORACLE_HOME}"
        echo "Last run was          : ${CURRENT_PROPERTY}"
        echo; echo -n "Continue with installation anyway (y/[n]) ? "
        read INSTALL_WITH_DIFFERENT_ORACLE_HOME
        if [[ ${INSTALL_WITH_DIFFERENT_ORACLE_HOME:0:1} == "y" || ${INSTALL_WITH_DIFFERENT_ORACLE_HOME:0:1} == "Y" ]]; then
          set_property "${PROPERTY_NAME}" "${ORACLE_HOME}" "${INSTANCE_PROP_FILE}"
        else
          #  Antwort war negativ, dann Abbruch
          return
        fi
      fi
    fi

    #  bugz 10345: OC4J-Container speichern, in den deployed werden soll
    #
    #  Fallunterscheidung fuer den gespeicherten Container:
    #    a) Container ist Alive => o.k., deployen
    #    b) Container ist Down => o.k., nicht deployen, Meldung an den User, Hint: der Container muss wieder gestartet werden.
    #    c) Container ist nicht mehr aufzufinden, kann 2 Gruende haben:
    #       1) Falsches $ORACLE_HOME => kann vom Skript nicht erkannt werden.
    #       2) Container geloescht => o.k., User darf neu auswaehlen.
    PROPERTY_NAME="oracle.oc4j.container"
    get_property ${PROPERTY_NAME} "${INSTANCE_PROP_FILE}"
    OC4J_STATUS=$(check_oc4j_container_status "${CURRENT_PROPERTY}")
    case ${OC4J_STATUS} in
      Alive)
        ORACLE_OC4J_CONTAINER="${CURRENT_PROPERTY}"
        ;;
      Down|Init)
        err_msg "Oracle OC4J instance '${CURRENT_PROPERTY}' is not running. Start instance and try again!"
        return
        ;;
      *)
        select_running_oc4j_container
        set_property "${PROPERTY_NAME}" "${ORACLE_OC4J_CONTAINER}" "${INSTANCE_PROP_FILE}"
        ;;
    esac

    #  Request-Port fuer bestimmen:
    #+   Zeile mit 'request=' aus $ORACLE_HOME/opmn/conf/opmn.xml entnehmen und mit sed die Portnummer filtern:
    #+ <port local="6102" remote="6202" request="6002"/>
    ORACLE_REQUEST_PORT=$(${VOLATILE_GREP} request "${ORACLE_HOME}/opmn/conf/opmn.xml" | ${VOLATILE_SED} -e "s+\(.*request=\"\)\([0-9]*\)\(\".*\)+\2+")
    if [[ ${ORACLE_REQUEST_PORT:-0} -ge 1024 && ${ORACLE_REQUEST_PORT:-0} -lt 65536 ]]; then
      echo "    + deploying ${COMPONENT_NAME}"
      ${VOLATILE_JAVA} -jar ${ORACLE_HOME}/j2ee/home/admin_client.jar deployer:oc4j:opmn://${CHOOSEN_INTERFACE}:${ORACLE_REQUEST_PORT}/${ORACLE_OC4J_CONTAINER} ${ORACLE_AS_USERID} ${ORACLE_AS_PASSWORD} -deploy -file ${COMPONENT_WAR_FILE} -deploymentName ${COMPONENT_NAME} -contextRoot ${COMPONENT_NAME} -bindAllWebApps
    else
      err_msg "Unable to determine OPMN request port from '${ORACLE_HOME}/opmn/conf/opmn.xml'."
    fi
  fi
}


configure_persistence_layer_for_radius_service () {
  echo -e "\n* Configuring persistence layer for XynaRadiusService"

  TOKEN_RADIUS_PERSISTENCELAYERINSTANCE="radius"

  local XML_PERSISTENCE_LAYER_NAME="${TOKEN_RADIUS_PERSISTENCELAYERINSTANCE}Xml"
  local MEMORY_PERSISTENCE_LAYER_NAME="${TOKEN_RADIUS_PERSISTENCELAYERINSTANCE}Memory"

  f_instantiate_named_persistencelayer "${INSTALL_PREFIX}" "${XML_PERSISTENCE_LAYER_NAME}" "xml" "HISTORY" "xact" "${TOKEN_RADIUS_PERSISTENCELAYERINSTANCE}"
  f_instantiate_named_persistencelayer "${INSTALL_PREFIX}" "${MEMORY_PERSISTENCE_LAYER_NAME}" "memory" "HISTORY" "xact"

  if [[ ! -d "${INSTALL_PREFIX}/server/storage/${TOKEN_RADIUS_PERSISTENCELAYERINSTANCE}" ]]; then ${VOLATILE_MKDIR} -p "${INSTALL_PREFIX}/server/storage/${TOKEN_RADIUS_PERSISTENCELAYERINSTANCE}"; fi
  install_file "components/xact/radius/storage/radiustlv.xml" "${INSTALL_PREFIX}/server/storage/${TOKEN_RADIUS_PERSISTENCELAYERINSTANCE}/."

  f_register_table_by_name "${INSTALL_PREFIX}" "${XML_PERSISTENCE_LAYER_NAME}" "radiustlv"  "HISTORY"
  f_register_table_by_name "${INSTALL_PREFIX}" "${MEMORY_PERSISTENCE_LAYER_NAME}" "radiususer" "HISTORY"
}

deploy_trigger () {
  if ! f_selected ${TRIGGER_NSN_HIX5600} ${TRIGGER_DHCP_V4} ${TRIGGER_RADIUS}; then
  	return;
  fi;
  echo -e "\n* Deploying trigger"

  if [[ "x${TRIGGER_NSN_HIX5600}" == "xtrue" ]] && [[ -d revisions/rev_workingset/saved/trigger/SNMPTrigger ]] && [[ -d revisions/rev_workingset/saved/filter/NSN_HiX5630_SNMPFilter ]]; then
    echo "  + SNMPTrigger for DSLAM NSN 5600 series"
    choose_interface "SNMP Trigger" "network.interface.nsnhix5600trigger"
    #  Trigger installieren
    trigger_copy SNMPTrigger revisions/rev_workingset/saved/trigger/SNMPTrigger/*
    f_xynafactory deploysharedlib snmplibs
    f_xynafactory addtrigger SNMP com.gip.xyna.xact.trigger.SNMPTrigger snmplibs ../revisions/rev_workingset/saved/trigger/SNMPTrigger/SNMPTrigger.jar
    f_xynafactory deploytrigger SNMP SNMP${SNMP_NSNHIX_PORT} ${CHOOSEN_INTERFACE_NAME} ${SNMP_NSNHIX_PORT} 10 30 200 600 3

    add_to_server_policy \
      '//SocketPermission for SNMPTrigger '${SNMP_NSNHIX_PORT} \
      'permission java.net.SocketPermission "*:'${SNMP_NSNHIX_PORT}'", "accept, listen, connect, resolve";'
  fi

  if [[ "x${TRIGGER_DHCP_V4}" == "xtrue" ]] && [[ -d revisions/rev_workingset/saved/trigger/DHCPTrigger ]]; then
    echo "    + DHCPTrigger v4"
    choose_interface "DHCP Trigger v4" "network.interface.dhcptrigger_v4"
    #  Trigger installieren
    trigger_copy DHCPTrigger revisions/rev_workingset/saved/trigger/DHCPTrigger/*
    f_xynafactory deploysharedlib dhcplibs
    f_xynafactory addtrigger DHCP com.gip.xyna.xact.trigger.DHCPTrigger dhcplibs ../revisions/rev_workingset/saved/trigger/DHCPTrigger/DHCPTrigger.jar
    f_xynafactory deploytrigger DHCP DHCPv4 ${CHOOSEN_INTERFACE} 67,68,67
  fi

  if [[ "x${TRIGGER_RADIUS}" == "xtrue" ]] && [[ -d revisions/rev_workingset/saved/trigger/XynaRadiusTrigger ]]; then
    echo "    + XynaRadiusTrigger"
    choose_interface "XynaRadiusTrigger" "network.interface.xynaradiustrigger"
    #  Trigger installieren
    trigger_copy XynaRadiusTrigger revisions/rev_workingset/saved/trigger/XynaRadiusTrigger/*
    configure_persistence_layer_for_radius_service
    f_xynafactory defineip -ip ${CHOOSEN_INTERFACE} -name "radius" -f
    f_xynafactory addtrigger -fqClassName com.gip.xyna.xact.trigger.XynaRadiusTrigger -sharedLibs : -triggerName XynaRadiusTrigger -jarFiles ../revisions/rev_workingset/saved/trigger/XynaRadiusTrigger/XynaRadiusTrigger.jar
    f_xynafactory deploytrigger -triggerInstanceName XynaRadiusTriggerInstance -triggerName XynaRadiusTrigger -startParameters radius
  fi
}

deploy_filter () {
  if ! f_selected ${FILTER_NSN_HIX5600} ${FILTER_DHCP_V4} ${FILTER_RADIUS} ; then
  	return;
  fi;
  echo -e "\n* Deploying filter"
  

  if [[ "x${FILTER_NSN_HIX5600}" == "xtrue" ]] && [[ -d components/xact/dslam/nsn/5600series/filterimpl/NSN_HiX5630_SNMP ]]; then
    echo "    + DSLAM NSN 5600 series"
    xmom_copy . components/xact/dslam/nsn/5600series/XMOM/*
    filter_copy NSN_HiX5630_SNMPFilter components/xact/dslam/nsn/5600series/filterimpl/NSN_HiX5630_SNMP/*
    f_xynafactory deploysharedlib NSNHiX5630
    f_xynafactory addfilter nsnhix5600 com.gip.xyna.xact.trigger.NSN_HiX5630_SNMPFilter SNMP NSNHiX5630 ../revisions/rev_workingset/saved/filter/NSN_HiX5630_SNMPFilter/NSN_HiX5630_SNMPFilter.jar
    f_xynafactory deployfilter nsnhix5600 nsnhix5600_${SNMP_NSNHIX_PORT} SNMP${SNMP_NSNHIX_PORT}
  fi

  if [[ "x${FILTER_DHCP_V4}" == "xtrue" ]] && [[ -d revisions/rev_workingset/saved/filter/DHCPFilter ]]; then
    echo "    + DHCPFilter v4"
    filter_copy DHCPFilter revisions/rev_workingset/saved/filter/DHCPFilter/*
    f_xynafactory addfilter DHCPFilterv4 com.gip.xyna.xact.trigger.DHCPFilter DHCP : ../revisions/rev_workingset/saved/filter/DHCPFilter/DHCPFilter.jar
    f_xynafactory deployfilter DHCPFilterv4 DHCPFilter DHCPv4
  fi

  if [[ "x${FILTER_RADIUS}" == "xtrue" ]] && [[ -d revisions/rev_workingset/saved/filter/XynaRadiusFilter ]]; then
    echo "    + XynaRadiusFilter"
    xmom_copy xact components/xact/radius/XMOM/*
    filter_copy XynaRadiusFilter components/xact/radius/filter/XynaRadiusFilter/*
    f_xynafactory addfilter -filterName XynaRadiusFilter -fqClassName com.gip.xyna.xact.trigger.XynaRadiusFilter -jarFiles ../revisions/rev_workingset/saved/filter/XynaRadiusFilter/XynaRadiusFilter.jar -triggerName XynaRadiusTrigger -sharedLibs :
    f_xynafactory deployfilter -filterInstanceName XynaRadiusFilterInstance -filterName XynaRadiusFilter -triggerInstanceName XynaRadiusTriggerInstance
  fi
}

deploy_services () {
  if ! f_selected ${SERVICE_TEMPLATEMECHANISM} ${SERVICE_NSN_HIX5600} ${SERVICE_SIPUSERAGENT} ${SERVICE_JMSFORWARDING} ${SERVICE_DHCP_V4} ; then
  	return;
  fi;
  echo -e "\n* Deploying services"
  local DEPLOY_DATATYPES; 
  local DEPLOY_WORKFLOWS;
  
  if [[ "x${SERVICE_TEMPLATEMECHANISM}" == "xtrue" ]] && [[ -d components/xact/configserver/mdmimpl/TemplateProvider ]] && [[ -d components/xact/configserver/mdmimpl/VelocityTemplate ]]; then
      echo "    + Template Mechanism"
      
      xmom_copy xact/templates components/xact/configserver/XMOM/xact/templates/*
      service_copy xact.templates.TemplateProvider components/xact/configserver/mdmimpl/TemplateProvider/*
      service_copy xact.templates.VelocityService components/xact/configserver/mdmimpl/VelocityTemplate/*
      sharedlib_copy templatestorables components/xact/configserver/sharedLibs/*
      
      
      f_xynafactory deploysharedlib templatestorables
      
      f_xynafactory deploydatatype xact.templates.VelocityTemplate
      f_xynafactory deployexception xact.templates.VelocityTemplateEvaluationException
      
      f_xynafactory set velocity.parser.pool.size 5
      
      f_xynafactory set xact.acs.velocity.aliases ""
      
      f_xynafactory deploydatatype xact.templates.VelocityService
      
      #fuer das Deployment des TemplateProviders muss vorher die Persistence fuer die Tabelle velocitytemplate konfiguriert werden
      #f_xynafactory deploydatatype xact.templates.TemplateProvider   
  fi
  
  if [[ "x${SERVICE_SIPUSERAGENT}" == "xtrue" ]] && [[ -d components/xact/sip/mdmimpl/SipUserAgentService ]]; then
      echo "    + SIP User Agent Service"
      
      xmom_copy xact/sip components/xact/sip/XMOM/*
      service_copy xact.sip.SipUserAgent components/xact/sip/mdmimpl/SipUserAgentService/*
      
      f_xynafactory deploydatatype xact.sip.SipUserAgent
  fi

  if [[ "x${SERVICE_NSN_HIX5600}" == "xtrue" ]] && [[ -d components/xact/dslam/nsn/5600series/mdmimpl/SNMPHelper ]] && [[ -d components/xact/dslam/nsn/5600series/mdmimpl/DSLAMHelper ]]; then
    echo "    + DSLAM NSN 5600 series"

    xmom_copy . components/xact/dslam/nsn/5600series/XMOM/*
    sharedlib_copy NSNHiX5630 components/xact/dslam/nsn/5600series/sharedLibs/*
    service_copy xact.dslam.nsn._5600series.helper.DSLAMHelper components/xact/dslam/nsn/5600series/mdmimpl/DSLAMHelper/*
    service_copy xact.dslam.nsn._5600series.storage.NSNDSLAMPortGroupStorage components/xact/dslam/nsn/5600series/mdmimpl/NSNDSLAMPortGroupStorage/*
    service_copy xact.dslam.nsn._5600series.storage.NSNDSLAMStorage components/xact/dslam/nsn/5600series/mdmimpl/NSNDSLAMStorage/*
    service_copy xact.dslam.nsn._5600series.traphandling.TrapCollectionService components/xact/dslam/nsn/5600series/mdmimpl/TrapCollectionService/*
    service_copy xact.snmp.helpers.SNMPHelper components/xact/dslam/nsn/5600series/mdmimpl/SNMPHelper/*

    f_xynafactory deploysharedlib NSNHiX5630
    
    f_xynafactory deploy xact.dslam.nsn.5600series.traphandling.OnSuccessfulGBondCreation
    f_xynafactory deploy xact.dslam.nsn.5600series.traphandling.OnTrapReceivedNoData
  fi

  if [[ "x${SERVICE_JMSFORWARDING}" == "xtrue" ]] && [[ -d components/xact/jms/filterimpl/JMSForwarding ]]; then
    echo "    + ForwardDequeuedJMSMessage"

    xmom_copy xact/jms components/xact/jms/XMOM/*
    filter_copy JMSForwardingFilter components/xact/jms/filterimpl/JMSForwarding/*

    f_xynafactory deploy xact.jms.ForwardDequeuedJMSMessage
  fi

  if [[ "x${SERVICE_DHCP_V4}" == "xtrue" ]] && [[ -d revisions/rev_workingset/saved/filter/DHCPFilter ]]; then
    echo "    + DHCPv4"
    f_xynafactory deploy xact.dhcp.LeaseQuery_v4
    f_xynafactory addcapacity -cardinality 5 -name LeaseQueryCapacity_v4 -state ACTIVE
    f_xynafactory requirecapacityforwf -capacityName LeaseQueryCapacity_v4 -cardinality 1 -workflowName xact.dhcp.LeaseQuery_v4
  fi
  
} 



set_properties () {
  echo -e "\n* Setting properties"
  if [[ -d revisions/rev_workingset/saved/services/xact.templates.TemplateProvider ]] && [[ -d revisions/rev_workingset/saved/services/xact.templates.VelocityTemplate ]]; then
    echo "    + Template Mechanism"
    f_xynafactory set velocity.parser.pool.size ${VELOCITY_PARSER_POOL_SIZE}
  fi
  if [[ -d revisions/rev_workingset/saved/services/xact.sip.SipUserAgent ]]; then
    echo "    + SipUserAgent"
    choose_interface "SipUserAgent" "network.interface.sipuseragent"
    f_xynafactory set xact.sip.localip ${CHOOSEN_INTERFACE}
    f_xynafactory set xact.sip.localportrange ${SIPADAPTER_LOW_PORT}-${SIPADAPTER_HIGH_PORT}
    f_xynafactory set xact.sip.username.black xynablack
    f_xynafactory set xact.sip.notify.responsetimeout ${SIPADAPTER_NOTIFY_RESPONSETIMEOUT}
  fi
  if [[ -d revisions/rev_workingset/saved/filter/NSN_HiX5630_SNMPFilter ]]; then
    echo "    + DSLAM NSN 5600 series"
    f_xynafactory set xact.snmp.exceptionmapping.file NSNDSLAMExceptionmappings.xml
  fi
  if [[ -d revisions/rev_workingset/saved/filter/DHCPFilter ]]; then
    echo "    + DHCPv4"
    f_xynafactory set xact.dhcp.hashv4 false
    f_xynafactory set xact.dhcp.hashv4passval 0
  fi

  echo "    + Monitoring"
  f_xynafactory set xyna.default.monitoringlevel ${DEFAULT_MONITORINGLEVEL}
  echo "    + Miscellaneous"
  f_xynafactory set xyna.scheduler.stop.timeout.offset ${SCHEDULER_STOP_TIMEOUT_OFFSET}
  f_xynafactory set xyna.scheduler.orderbackupwaitingforscheduling true
  f_xynafactory set xyna.xnwh.persistence.xmlshell.greppath ${VOLATILE_GREP}
  echo "    + Exception-Code Groups"
  f_xynafactory set xyna.exceptions.codegroup.extension.automatic true
  f_xynafactory set xyna.exceptions.codegroup.extension.defaultpadding 5
  f_xynafactory set xyna.exceptions.codegroup.extension.defaultpattern ${PROJECT_PREFIX_UPPERCASE}-[[]]

  echo "    + RMI-Hostname"
  f_xynafactory set xyna.rmi.hostname.registry "${XYNA_RMI_IPADDRESS}"
}

install_xynafactory () {
  echo -e "\n* Installing Xyna Factory into '${INSTALL_PREFIX}'."

  PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  PORT_OFFSET=$(( ($((10#${PRODUCT_INSTANCE})) - 1) * 10 ))

  exit_if_dir_not_found revisions
  exit_if_dir_not_found server
  
  echo -e "\n  + Copy delivery items to ${INSTALL_PREFIX}/{revisions,server}/."
  ${VOLATILE_MKDIR} -p ${INSTALL_PREFIX}/revisions/rev_workingset/saved/{services,sharedLibs,XMOM}
  ${VOLATILE_CP} -rp server ${INSTALL_PREFIX}/.
  ${VOLATILE_CP} -rp "./func_lib/" ${INSTALL_PREFIX}/server/.

  #Lizenzen
  install_license ${INSTALL_PREFIX}/server/lib

  #Anpassen von log4j2.xml und xynafactory.sh
  configure_log4j2_xml "${INSTALL_PREFIX}/server/log4j2.xml"
  configure_xynafactory_sh "${INSTALL_PREFIX}/server/xynafactory.sh"
  
  ${VOLATILE_MKDIR} -p ${INSTALL_PREFIX}/server/installDefaults/
  ${VOLATILE_CP} ${INSTALL_PREFIX}/server/{log4j2.xml,server.policy,xynafactory.sh} ${INSTALL_PREFIX}/server/installDefaults
  
  #Anpassen des RMI-Ports, damit Factory korrekt starten kann
  set_rmi_port
  
  #Schreibschutz für xynafactory.sh
  ${VOLATILE_CHMOD} 550 "${INSTALL_PREFIX}/server/xynafactory.sh"

  #Bash-Completion auf jeden Fall einrichten
  BASH_COMPLETION="true";

  #Erstanlage der XynaProperties
  f_install_properties
}

update_xynafactory () {
  echo -e "\n* Updating Xyna Factory in '${INSTALL_PREFIX}'."

  PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  PORT_OFFSET=$(( ($((10#${PRODUCT_INSTANCE})) - 1) * 10 ))

  exit_if_dir_not_found revisions
  exit_if_dir_not_found server

  #hier schon Berechtigung ändern, damit schreibbar im Backup
  ${VOLATILE_CHMOD} 750 ${INSTALL_PREFIX}/server/xynafactory.sh

  echo -e "\n  + Backup."
  backup_dir ${INSTALL_PREFIX} revisions
  backup_dir ${INSTALL_PREFIX} saved      optional
  backup_dir ${INSTALL_PREFIX} server
  if [ -d ${INSTALL_PREFIX}/NetworkAvailability ] ; then
     backup_dir ${INSTALL_PREFIX} NetworkAvailability
  fi
   
  echo -e "\n  + Copy delivery items to ${INSTALL_PREFIX}/{revisions,server}/."

  ${VOLATILE_RM} -rf ${INSTALL_PREFIX}/server/lib
   
  ${VOLATILE_MKDIR} -p ${INSTALL_PREFIX}/revisions/rev_workingset/saved/{services,sharedLibs,XMOM}
  ${VOLATILE_CP} -rp "./func_lib/" ${INSTALL_PREFIX}/server/.
  
  #alles im server-Verzeichnis kopieren außer log4j2.xml, server.policy, xynafactory.sh
  ${VOLATILE_CP} -rp server ${INSTALL_PREFIX}/.
  
  #log4j2.xml, server.policy, xynafactory.sh aus backup wiederherstellen
  restore_file_from_dir ${INSTALL_PREFIX} server server.policy 
  restore_file_from_dir ${INSTALL_PREFIX} server log4j2.xml
  restore_file_from_dir ${INSTALL_PREFIX} server xynafactory.sh
  #weitere Ausnahmen:
  restore_file_from_dir ${INSTALL_PREFIX} server/storage/persistence persistencelayers.xml
  
  #Lizenzen
  install_license ${INSTALL_PREFIX}/server/lib
  
  #Konfigurieren und Mergen von log4j2.xml, server.policy, xynafactory.sh
  NEW_FILE_DIR="$(basename $HOSTNAME)"
  if f_selected ${LOG4J2_MERGE} ; then
    ${VOLATILE_CP} server/log4j2.xml "$NEW_FILE_DIR/log4j2.xml"
    configure_log4j2_xml "$NEW_FILE_DIR/log4j2.xml" "$NEW_FILE_DIR/log4j2.xml_configured"
    merge_files ${INSTALL_PREFIX}/server log4j2.xml "$NEW_FILE_DIR" log4j2.xml_configured
  fi;
  merge_files ${INSTALL_PREFIX}/server server.policy server server.policy
  ${VOLATILE_CP} server/xynafactory.sh "$NEW_FILE_DIR/xynafactory.sh"
  configure_xynafactory_sh   "$NEW_FILE_DIR/xynafactory.sh"   "$NEW_FILE_DIR/xynafactory.sh_configured"
  merge_files ${INSTALL_PREFIX}/server xynafactory.sh "$NEW_FILE_DIR" xynafactory.sh_configured

  # Fall es networkAvailability.properties gibt, dann werden die jetzt konfiguriert und gemixt
  if [ -f ${INSTALL_PREFIX}/NetworkAvailability/config/networkAvailability.properties ]; then
     ${VOLATILE_CP} components/xact/NetworkAvailability/config/networkAvailability.properties "$NEW_FILE_DIR/networkAvailability.properties"
     configure_networkAvailability_properties "$NEW_FILE_DIR/networkAvailability.properties" "$NEW_FILE_DIR/networkAvailability.properties_configured"
     merge_files ${INSTALL_PREFIX}/NetworkAvailability/config networkAvailability.properties "$NEW_FILE_DIR" networkAvailability.properties_configured
     # Da spaeter der ganze NA nochmal rekursiv kopiert wird, werden die bereits konfigurierten Werte wieder zurueckkopiert
     ${VOLATILE_CP} -rp ${INSTALL_PREFIX}/NetworkAvailability/config/networkAvailability.properties "$NEW_FILE_DIR/networkAvailability.properties"
  fi

  
  #für Umstieg auf log4j2 muss die Property 'jvm.option.log4j" angepasst werden
  update_log4j_property
  
  #Schreibschutz für xynafactory.sh
  ${VOLATILE_CHMOD} 550 ${INSTALL_PREFIX}/server/xynafactory.sh

  echo -e "\n  Updating server directory finished.\n"
}

install_license () {
  echo -e "\n  + Install Licenses";
  for jar in ${1}/*.jar ; do 
    copy_license ${jar} "      "
  done;
}

configure_networkAvailability_properties () {
   echo -e "\n  + Configuring networkAvailability.properties"
   f_replace_token "${1}" "${2:-${1}}" \
      "s+^hostname.remote=.*+hostname.remote=${CLUSTER_MANAGEMENT_REMOTE_IPADDRESS}+" \
      "s+^master=.*+master=${CLUSTER_NODE_PREFERENCE}+"
}

configure_xynafactory_sh () {
  echo -e "\n  + Configuring xynafactory.sh."
  f_replace_token "${1}" "${2:-${1}}" \
    "s+TOKEN_FACTORY_CLI_PORT+$(( 4242 + ${PORT_OFFSET} ))+" \
    "s+TOKEN_INSTANCE_NUMBER+${INSTANCE_NUMBER:-1}+" \
    "s+TOKEN_NETCAT+${VOLATILE_NETCAT}+"
}

configure_log4j2_xml () {
  echo -e "\n  + Configuring log4j2.xml."
  PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  exit_if_not_exists ${1}
  f_replace_token "${1}" "${2:-${1}}" \
    "s+XYNA +XYNA_${PRODUCT_INSTANCE} +" \
    "s+ facility=\"LOCAL0\"+ facility=\"${XYNA_SYSLOG_FACILITY}\"+"
}

update_log4j_property () {
  local PROPERTY="jvm.option.log4j"
  get_property ${PROPERTY} "${INSTANCE_PROP_FILE}"
  if [[ ${CURRENT_PROPERTY} == *"-Dlog4j.configuration=file:"* ]]; then
    local NEW_VALUE=$(default_value_for_property "${PROPERTY}")
    echo -e "\n  + Replace outdated property 'jvm.option.log4j' in '${PROPERTIES_FILE_NAME}'"
    echo -e "    old value: '${CURRENT_PROPERTY}', new value '${NEW_VALUE}'"
    set_property "${PROPERTY}" "${NEW_VALUE}" "${INSTANCE_PROP_FILE}"
  fi;
}

set_rmi_port () {
  FILE_TO_EDIT="${INSTALL_PREFIX}/server/storage/Configuration/xynaproperties.xml"
  if [[ ! -d "$(dirname "${FILE_TO_EDIT}")" ]]; then
    ${VOLATILE_MKDIR} -p "$(dirname "${FILE_TO_EDIT}")"
  fi
  ${VOLATILE_CAT} > "${FILE_TO_EDIT}" << A_HERE_DOCUMENT
<?xml version="1.0" encoding="UTF-8"?>
<xynapropertiesTable>
  <xynaproperties>
    <propertykey>xyna.rmi.port.registry</propertykey>
    <propertyvalue>${XYNA_RMI_PORT}</propertyvalue>
    <factorycomponent>false</factorycomponent>
  </xynaproperties>
</xynapropertiesTable>
A_HERE_DOCUMENT
}



#  Eingabeparameter:
#o   Name der Komponente
check_component_status () {
  COMPONENT_NAME="${1}"
  PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})

  comp_state[0]=$(check_components_file "black_edition_prerequisites.${PRODUCT_INSTANCE}")
  if [[ "x${comp_state[0]}" == "xinstall" ]]; then
    err_msg "Prerequisites for Xyna Black Edition not installed. Abort!"
    echo
    echo "Hint: Run 'install_prerequisites.sh' first..."
    exit 2
  fi
  local COMP_VERSION=$(get_component_property "black_edition_prerequisites.${PRODUCT_INSTANCE}.version");
  if [[ $(f_compare_versions "${COMP_VERSION}" "0.7.0.0") -lt 0 ]]; then
    err_msg "Minimum required version for Prerequisites is '0.7.x.x'. Abort!"
    echo
    echo "Hint: Run 'install_prerequisites.sh' first..."
    exit 3
  fi

  #  Prueft, ob die Komponente installiert ('install') oder aktualisiert ('update') werden muss
  COMPONENT_UPDATE=$(check_components_file "${COMPONENT_NAME}.${PRODUCT_INSTANCE}")

  #  Prueft, ob "install" korrekt ist und fragt im Zweifelsfall nach
  if [[ "x${COMPONENT_UPDATE}" == "xinstall" ]]; then
    if [[ -e "${INSTALL_PREFIX}/server/lib/xynafactory-javadoc.jar" ]]; then
      attention_msg "It seems that the factory is already installed in ${INSTALL_PREFIX}/server, but expected entry in ${PROPERTIES_FILE_NAME} is missing.";
      echo "Please specify how to continue:";
      select CONT in "Install" "Update" "Abort"; do
        case $CONT in
          Install ) COMPONENT_UPDATE="install"; break;;
          Update )  COMPONENT_UPDATE="update"; break;;
          Abort )   exit -1;;
        esac
      done
    fi
  fi
  
  if [[ "x${COMPONENT_UPDATE}" == "xinstall" ]]; then
    echo -e "\n-- Installation --\n"
  else
    echo -e "\n-- Update --\n"
  fi

}


install_xyna_cluster () {
  echo -e "\n* Installing Xyna Cluster components"

  if [[ -d components/xnwh/xcs/xsor ]] && [[ -d components/xnwh/xcs/XynaClusterPersistenceLayer ]] && [[ -d server/clusterproviders/XSORClusterProvider ]] && [[ -d components/xact/NetworkAvailability ]]; then
    
    echo "    + Xyna Scalable Object Repository library"
    if [[ ! -d "${INSTALL_PREFIX}/server/lib" ]]; then ${VOLATILE_MKDIR} -p "${INSTALL_PREFIX}/server/lib"; fi
    install_file "components/xnwh/xcs/xsor/xsor.jar" "${INSTALL_PREFIX}/server/lib/."
    
    echo "    + Xyna Scalable Object Repository cluster provider"
    #  nothing to do here - build already put this into the right place...
    
    echo "    + Xyna Cluster persistence layer"
    if [[ ! -d "${INSTALL_PREFIX}/server/persistencelayers/XynaClusterPersistenceLayer" ]]; then ${VOLATILE_MKDIR} -p "${INSTALL_PREFIX}/server/persistencelayers/XynaClusterPersistenceLayer"; fi
    # remove old jars
    ${VOLATILE_RM} -f "${INSTALL_PREFIX}/server/persistencelayers/XynaClusterPersistenceLayer/XynaMemoryPersistenceLayer*.jar"
    # install new jars
    install_file "components/xnwh/xcs/XynaClusterPersistenceLayer/XynaClusterPersistenceLayer.jar" "${INSTALL_PREFIX}/server/persistencelayers/XynaClusterPersistenceLayer/."
    install_file "components/xnwh/xcs/XynaClusterPersistenceLayer/XynaMemoryPersistenceLayer*.jar" "${INSTALL_PREFIX}/server/persistencelayers/XynaClusterPersistenceLayer/."

    echo "    + network availability demon"
    # The folders NetworkAvailability and NetworkAvailability/lib will be created, if it does not exist. 
    ${VOLATILE_MKDIR} -p "${INSTALL_PREFIX}/NetworkAvailability/lib"
    # remove old jars, if there are some
    ${VOLATILE_RM} -f "${INSTALL_PREFIX}"/NetworkAvailability/lib/*.jar 
    # copy all stuff from delivery to the NetworkAvailability folder
    ${VOLATILE_CP} -rp components/xact/NetworkAvailability/* "${INSTALL_PREFIX}/NetworkAvailability/."
    if [[ -f "$HOSTNAME/networkAvailability.properties" ]]; then ${VOLATILE_CP} -rp "$HOSTNAME/networkAvailability.properties" "${INSTALL_PREFIX}/NetworkAvailability/config/networkAvailability.properties"; fi
    # create installDefaults
    ${VOLATILE_MKDIR} -p ${INSTALL_PREFIX}/NetworkAvailability/config/installDefaults/
    ${VOLATILE_CP} ${INSTALL_PREFIX}/NetworkAvailability/config/networkAvailability.properties ${INSTALL_PREFIX}/NetworkAvailability/config/installDefaults

    
    FILE_TO_EDIT="${INSTALL_PREFIX}/NetworkAvailability/networkAvailabilityDemonWrapper.sh"
    exit_if_not_exists "${FILE_TO_EDIT}"
    f_replace_in_file "${FILE_TO_EDIT}" "s+TOKEN_XYNA_USER+${XYNA_USER}+"
  fi
}

check_install_oracle_cluster () {
  #Suche nach Pattern, daher kein  if [ -e ojdbc*.jar ]; möglich!
  local OJDBC_JAR_FOUND="false";
  if [[ -d ${INSTALL_PREFIX}/server/userlib/ ]] ; then
    for f in ${INSTALL_PREFIX}/server/userlib/ojdbc*.jar; do 
      OJDBC_JAR_FOUND="true";
    done;
  fi
  if ! f_selected ${OJDBC_JAR_FOUND} ; then
    err_msg "Required ojdbc*.jar not found in ${INSTALL_PREFIX}/server/userlib";
    exit 1;
  fi;
  
  #Testen, ob bereits geclustert
  if f_is_clustered ; then
    err_msg "Cluster setup can only be done once."
    echo "Hint: Remove ${PRODUCT_NAME}.${PRODUCT_INSTANCE}.cluster from ${PROPERTIES_FILE_NAME}"
    exit 1
  fi
}


create_bashcompletion () {
  if ! f_selected ${BASH_COMPLETION} ; then
    return;
  fi;
  echo -e "\n* Configuring bash-completion"
  
  local STATUS=$(f_xynafactory_status)
  if [[ ${STATUS} != 0 ]] ; then
    attention_msg "Could not configure bash-completion: factory is $(f_xynafactory_status_to_string ${STATUS})"
    return;
  fi;
  ${VOLATILE_RM} -f ${TMP_FILE}
  #direkter Aufruf, um keine modifizierte Ausgabe zu erhalten #TODO auch xynafactory.sh umgehen?
  ${INSTALL_PREFIX}/server/xynafactory.sh bashcompletion > ${TMP_FILE}
  
  #Prüfung
  local FIRST=$(${VOLATILE_HEAD} -n 1 ${TMP_FILE});
  if [[ ${FIRST} != "# This configuration file is auto-generated." ]] ; then
    attention_msg "Could not configure bash-completion: Unexpected content \"${FIRST}\" in generated code"; 
    return 1;
  fi; 
 
  local TARGET_FILE="${HOME}/.xynafactory_completion"
  ${VOLATILE_MV} ${TMP_FILE} "${TARGET_FILE}"

  local FILE_TO_EDIT="${HOME}/.bashrc"
  if [[ ! -f "${FILE_TO_EDIT}" ]]; then ${VOLATILE_TOUCH} "${FILE_TO_EDIT}"; fi

  if [[ $(${VOLATILE_GREP} -c "xynafactory_completion" "${FILE_TO_EDIT}") -lt 2 ]]; then
    ${VOLATILE_CAT} << A_HERE_DOCUMENT >> "${FILE_TO_EDIT}"

#  Source completions for Xyna-Factory
if [[ -f ~/$(basename "${TARGET_FILE}") ]]; then
        . ~/$(basename "${TARGET_FILE}")
fi
A_HERE_DOCUMENT
  fi
}

register_repositoryaccesses () {
  if ! f_selected ${REPOSITORYACCESS_SVN} ; then
  	return;
  fi;
  echo -e "\n* Register repositoryaccess"

  if f_selected ${REPOSITORYACCESS_SVN} && [[ -d ${INSTALL_PREFIX}/server/repositoryaccess/SVNRepositoryAccess ]]; then
    local CALLRESULT=$(echo "listhooks" | ${VOLATILE_NETCAT} ${SVN_SERVER} ${SVN_HOOKMANAGER_PORT} 2>&1);
    if [[ "${CALLRESULT}" != *FACTORY_HOOKS* ]] ; then
      attention_msg "SVN Hook Manager not installed on SVN server."
    fi
    f_xynafactory registerrepositoryaccess -name SVNRepositoryAccess -fqClassName com.gip.xyna.xdev.xlibdev.repositoryaccess.svn.SVNRepositoryAccess
  fi
}

register_datamodeltypes () {
  if ! f_selected ${DATAMODELTYPE_MIB} ${DATAMODELTYPE_TR069} ${DATAMODELTYPE_XSD} ; then
    return;
  fi;  
  echo -e "\n* Register datamodeltypes"
  
  if f_selected ${DATAMODELTYPE_MIB} ; then 
    register_datamodeltype MIB server/datamodeltypes/MIB com.gip.xyna.xfmg.xfctrl.datamodel.mib.DataModelTypeImpl
  fi
  
  if f_selected ${DATAMODELTYPE_TR069} ; then 
    register_datamodeltype TR069 server/datamodeltypes/TR069 com.gip.xyna.xfmg.xfctrl.datamodel.tr069.DataModelTypeImpl
  fi
  
  if f_selected ${DATAMODELTYPE_XSD} ; then 
    register_datamodeltype XSD server/datamodeltypes/XSD com.gip.xyna.xfmg.xfctrl.datamodel.xsd.DataModelTypeImpl
  fi
  
}

register_datamodeltype () { #Name, Pfad, FqClassName
  local DMT_NAME=$1
  local DMT_PATH=$2
  local DMT_FQCN=$3
  if [[ -d ${DMT_PATH} ]]; then
    INDENTATION="    ";
    echo "  + ${DMT_NAME}"
    mkdir -p ${INSTALL_PREFIX}/${DMT_PATH}
    cp -r ${DMT_PATH}/* ${INSTALL_PREFIX}/${DMT_PATH}/
    
    if f_files_exist ${DMT_PATH}/*-{GROUP,LICENSE,NOTICE,LICENCE}* ; then
      echo -e "    Install Licenses";
      for jar in ${DMT_PATH}/*.jar ; do 
        copy_license ${jar} "      "
      done;
    fi;
    
    f_xynafactory registerdatamodeltype -name ${DMT_NAME} -fqClassName ${DMT_FQCN}
  fi
}

import_applications () {
  if [[ -z ${ALL_APPLICATIONS} ]] ; then
    return
  fi;
  echo -e "\n* Import applications"
  INDENTATION="    ";
  local IMPORTED_APP_LIST="";  #importierte Apps
  local FAILED_APP_LIST="";    #Apps mit Fehler: nicht gefunden; fehlgeschlagener Import 
  local SKIPPED_APP_LIST="";   #Apps die bereits vorhanden sind und deshalb übersprungen werden
  local REQUIRING_APP_LIST=""; #Apps, die andere Apps als Voraussetzung haben
  
  #Sollen Applications global installiert werden? "" oder "--global"
  local IMPORT_APPLICATIONS_GLOBALLY=""; 
  if f_is_clustered ; then
    IMPORT_APPLICATIONS_GLOBALLY="--global"
  fi
  
  #mit Versionen ergänzte lokale Liste
  local APP_LIST="";
  local APP;
  for APP in ${ALL_APPLICATIONS} ; do
    local APPFILE=$(f_find_application_in_components ${APP});
    if [[ -z ${APPFILE} ]] ; then
      attention_msg "Application ${APP} cannot be found";
      FAILED_APP_LIST=$(f_add_to_list FAILED_APP_LIST ${APP});
    else
      APP_LIST=$(f_add_to_list APP_LIST ${APPFILE});
    fi;
  done
  
  local OLD_VAL=$(f_get_xyna_property "xfmg.xfctrl.rcdependencies.change.orders.active.behavior" "NOTSET")
  f_set_xyna_property "xfmg.xfctrl.rcdependencies.change.orders.active.behavior" IGNORE
  
  #alle Applications installieren
  while [[ -n ${APP_LIST} ]] ; do 
    APP_FILE=$(f_first_in_list APP_LIST);
    #echo "    trying to import ${APP_FILE}"
    APP_LIST=$(f_remove_from_list APP_LIST ${APP_FILE})
    case ${APP_FILE} in
      */GuiHttp.*)
        f_adjust_guihttp_app
        f_import_applications_internal "../$HOSTNAME/GuiHttp.${GUIHTTPVERSION}.app"
        continue;;
      */SNMPStatistics.*)
        f_adjust_snmpstatistics_app
        add_to_server_policy \
          '//SocketPermission for SNMPTrigger '${SNMP_TRIGGER_PORT} \
          'permission java.net.SocketPermission "*:'${SNMP_TRIGGER_PORT}'", "accept, listen, connect, resolve";'
        f_import_applications_internal "../$HOSTNAME/SNMPStatistics.${SNMPSTATVERSION}.app"
        continue;;
      */Radius.*)
        configure_persistence_layer_for_radius_service;;
      */LDAP.*)
        add_to_server_policy \
          '//required for LDAP bind SSL' \
          'permission java.lang.RuntimePermission "setFactory";'
        ;;
    esac;
    f_import_applications_internal ${APP_FILE}
  done
  
  echo
  f_reset_xyna_property "xfmg.xfctrl.rcdependencies.change.orders.active.behavior" "${OLD_VAL}" "NOTSET"
  
  echo
  echo "    Imported: ${IMPORTED_APP_LIST}";
  if [[ -n ${FAILED_APP_LIST} ]] ; then
    echo "    Failed: ${FAILED_APP_LIST}";
  fi;
  if [[ -n ${SKIPPED_APP_LIST} ]] ; then
    echo "    Skipped: ${SKIPPED_APP_LIST}";
  fi;

  echo
  
  for APP in ${IMPORTED_APP_LIST} ${SKIPPED_APP_LIST}; do
    case ${APP} in
      */GlobalApplicationMgmt.*)
        echo -e "\n  - Starting application ${APP}";
        f_xynafactory startapplication --force -applicationName "GlobalApplicationMgmt" -versionName "${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.ListApplications.Destination" "xfmg.xfctrl.appmgmt.ListApplications@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.ListLocalApplications.Destination" "xfmg.xfctrl.appmgmt.ListLocalApplications@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.RemoveApplications.Destination" "xfmg.xfctrl.appmgmt.RemoveApplications@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.StartApplications.Destination" "xfmg.xfctrl.appmgmt.StartApplications@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.StopApplications.Destination" "xfmg.xfctrl.appmgmt.StopApplications@GlobalApplicationMgmt/${APPMGMTVERSION}"
        
        f_set_xyna_property "xfmg.xfctrl.appmgmt.CreateRemoteRuntimeContexts.Destination" "xfmg.xfctrl.appmgmt.CreateRemoteRuntimeContexts@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.DeleteRemoteRuntimeContexts.Destination" "xfmg.xfctrl.appmgmt.DeleteRemoteRuntimeContexts@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.ListRuntimeDependencyContextDetails.Destination" "xfmg.xfctrl.appmgmt.ListRuntimeDependencyContextDetails@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.ModifyRuntimeDependencyContext.Destination" "xfmg.xfctrl.appmgmt.ModifyRuntimeDependencyContext@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspace.Destination" "xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspace@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.ExportApplication.Destination" "xfmg.xfctrl.appmgmt.ExportApplication@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.ImportApplication.Destination" "xfmg.xfctrl.appmgmt.ImportApplication@GlobalApplicationMgmt/${APPMGMTVERSION}"
        f_set_xyna_property "xfmg.xfctrl.appmgmt.MigrateRuntimeContextDependencies.Destination" "xfmg.xfctrl.appmgmt.MigrateRuntimeContextDependencies@GlobalApplicationMgmt/${APPMGMTVERSION}"
        
      ;;
     */GuiHttp.*)
        echo -e "\n  - Starting application ${APP}";
        f_xynafactory startapplication --force -applicationName "GuiHttp" -versionName "${GUIHTTPVERSION}"
      ;;
    esac;
  done;
  
  
}


xmom_copy () {
  local XMOMDIR="${INSTALL_PREFIX}/revisions/rev_workingset/saved/XMOM/$1"
  if [[ ! -d ${XMOMDIR} ]] ; then
    ${VOLATILE_MKDIR} -p ${XMOMDIR};
  fi
  shift 1
  ${VOLATILE_CP} -r "$@" ${XMOMDIR}
}

xmom_remove () {
  local XMOMDIR="${INSTALL_PREFIX}/revisions/rev_workingset/saved/XMOM/"
  for i in "$@"; do 
    ${VOLATILE_RM} -r "${XMOMDIR}$i"
  done
}

sharedlib_copy () {
  local SHAREDLIBDIR="${INSTALL_PREFIX}/revisions/rev_workingset/saved/sharedLibs/$1"
  if [[ ! -d ${SHAREDLIBDIR} ]] ; then
    ${VOLATILE_MKDIR} -p ${SHAREDLIBDIR};
  fi
  shift 1
  ${VOLATILE_CP} -r "$@" ${SHAREDLIBDIR}
}

sharedlib_deploy () {
  local RC;
  local MSG=$(f_xynafactory deploysharedlib $1);
  RC=$?
  if [[ ${RC} != 0 ]] ; then
  	echo ${MSG}
  fi;
}
   
service_copy () {
  local SERVICEDIR="${INSTALL_PREFIX}/revisions/rev_workingset/saved/services/$1"
  if [[ ! -d ${SERVICEDIR} ]] ; then
    ${VOLATILE_MKDIR} -p ${SERVICEDIR};
  fi
  shift 1
  ${VOLATILE_CP} -r "$@" ${SERVICEDIR}
}

trigger_copy () {
  local TRIGGERDIR="${INSTALL_PREFIX}/revisions/rev_workingset/saved/trigger/$1"
  if [[ ! -d ${TRIGGERDIR} ]] ; then
    ${VOLATILE_MKDIR} -p ${TRIGGERDIR};
  fi
  shift 1
  ${VOLATILE_CP} -r "$@" ${TRIGGERDIR}
}

filter_copy () {
  local FILTERDIR="${INSTALL_PREFIX}/revisions/rev_workingset/saved/filter/$1"
  if [[ ! -d ${FILTERDIR} ]] ; then
    ${VOLATILE_MKDIR} -p ${FILTERDIR};
  fi
  shift 1
  ${VOLATILE_CP} -r "$@" ${FILTERDIR}
}

#
# Merge zweier Dateien auf Basis des gemeinsamen Vorläufers
# Parameter sind <dir> <filename> <srcdir> <newFileName>
# Zu mergende Dateien sind <dir>/<filename> und <srcdir>/<newFileName> mit
# Vorläufer <dir>/installDefaults/<filename>
#
# Falls der Merge scheitert, wir die Installation angehalten, der
# Benutzer kann den Konflikt manuell beseitigen und die Installation 
# fortsetzen.
merge_files () {

  local DIR=${1}
  local FILENAME=${2}
  local SRCDIR=${3}
  local NEW_FILE=${SRCDIR}/${4}
  local TMP_FILE_DIR="$(basename $HOSTNAME)"
  
  echo -e "\n  + Merging ${FILENAME}."
  
  local BASE_DIR="installDefaults";
  local CUSTOMIZED_FILE=${DIR}/${FILENAME}
  local COMMON_BASE_FILE=${DIR}/${BASE_DIR}/${FILENAME}
  
  local TMP_CUSTOMIZED_FILE=${TMP_FILE_DIR}/${FILENAME}_customized
  local TMP_COMMON_BASE_FILE=${TMP_FILE_DIR}/${FILENAME}_base
  local TMP_NEW_FILE=${TMP_FILE_DIR}/${FILENAME}_new
  local TMP_MERGE_FILE=${TMP_FILE_DIR}/${FILENAME}_merge
  
  if [[ ! -e ${CUSTOMIZED_FILE} ]] ; then
    attention_msg "Customized \"${CUSTOMIZED_FILE}\" does not exist, using new file from delivery"
    ${VOLATILE_CP} ${NEW_FILE} ${CUSTOMIZED_FILE}
    ${VOLATILE_CP} ${NEW_FILE} ${COMMON_BASE_FILE}
    return;
  fi;
  if [[ ! -e ${NEW_FILE} ]] ; then
    echo "New \"${NEW_FILE}\" does not exist, abort ..."
    exit 11;
  fi;
  
  #temporäre Kopien, die im Konfliktfall interessant sind 
  ${VOLATILE_CP} ${CUSTOMIZED_FILE} ${TMP_CUSTOMIZED_FILE}
  ${VOLATILE_CP} ${NEW_FILE} ${TMP_NEW_FILE}
  
  #Common Base ist nötig, evtl. versuchen anzulegen
  if [ -e ${COMMON_BASE_FILE} ] ; then
    ${VOLATILE_CP} ${COMMON_BASE_FILE} ${TMP_COMMON_BASE_FILE}
  else
    if [[ ${INSTALLATION_PLATFORM} = "solaris" ]] ; then
      #solaris kann das diff so nicht, deshalb muss man mit mehr manuellen konfliktaufloesungen leben 
      echo "Common base \"${COMMON_BASE_FILE}\" does not exist, using empty file..."
      echo "" > ${TMP_COMMON_BASE_FILE}
    else 
      echo "Common base \"${COMMON_BASE_FILE}\" does not exist, trying to generate..."
      ${VOLATILE_MKDIR} -p ${DIR}/${BASE_DIR}/
      diff --unchanged-line-format='%L' --old-line-format='' --new-line-format='' ${CUSTOMIZED_FILE} ${NEW_FILE} > ${TMP_COMMON_BASE_FILE}
    fi;
  fi;
  
  #Merge versuchen
  local RC;
  #TODO: diff3 als VOLATILE_DIFF3
  diff3 -A -m -L customized -L base -L new ${CUSTOMIZED_FILE} ${TMP_COMMON_BASE_FILE} ${NEW_FILE} > ${TMP_MERGE_FILE}
  RC=$?
  
  local MERGE_SUCCEEDED=${RC};
  if [[ ${MERGE_SUCCEEDED} != 0 ]] ; then
    #ist TMP_COMMON_BASE_FILE gleich dem unconfigured stand von NEW_FILE, dann ist das wie keine aenderung zwischen TMP_COMMON_BASE und NEW_FILE zu handhaben
    local NEW_UNCONFIGURED=${SRCDIR}/${FILENAME}
    diff ${TMP_COMMON_BASE_FILE} ${NEW_UNCONFIGURED} >/dev/null
    RC=$?
    if [[ ${RC} = 0 ]] ; then
      MERGE_SUCCEEDED=0; 
      ${VOLATILE_CP} ${CUSTOMIZED_FILE} ${TMP_MERGE_FILE}
    fi;
    
  fi;
  
  if [[ ${MERGE_SUCCEEDED} = 0 ]] ; then
    echo "    Merge \"${FILENAME}\" succeeded";
    
    ${VOLATILE_CP} ${TMP_MERGE_FILE} ${CUSTOMIZED_FILE}
    ${VOLATILE_CP} ${NEW_FILE} ${COMMON_BASE_FILE}
    ${VOLATILE_RM} ${TMP_CUSTOMIZED_FILE} ${TMP_COMMON_BASE_FILE} ${TMP_NEW_FILE} ${TMP_MERGE_FILE}
    
  else
    local MSG="Merging customized \"${FILENAME}\" with new version from delivery failed.\n"
    MSG="${MSG}Following files can be found in \"${DIR}\":\n";
    MSG="${MSG}  - conflicted merge: \"${FILENAME}_merge\"\n";
    MSG="${MSG}  - original file: \"${FILENAME}_customized\" (or \"${FILENAME}\")\n";
    MSG="${MSG}  - new file \"${FILENAME}_new\" (or \"${NEW_FILE}\")\n";
    if [ -e ${COMMON_BASE_FILE} ] ; then
      MSG="${MSG}  - common base \"${FILENAME}_base (or \"${BASE_DIR}/${FILENAME}\")\n";
    else
      MSG="${MSG}  - generated common base \"${FILENAME}_base\"\n";
    fi;
    MSG="${MSG}\n";
    case ${MERGE_MODE} in
      merge)
        MSG="${MSG}Please resolve the conflict manually.\n"; 
        MSG="${MSG}Actions:\n";
        if [ ! -e ${COMMON_BASE_FILE} ] ; then
          MSG="${MSG}  *) Provide a proper common base from last installed delivery \n";
          MSG="${MSG}       in \"${BASE_DIR}/${FILENAME}\".\n";
          MSG="${MSG}     Continue installation with [ENTER]\n";
        fi;
        MSG="${MSG}  *) Resolve conflict manually:\n";
        MSG="${MSG}     Edit \"${FILENAME}_merge\" to resolve the conflict.\n";
        MSG="${MSG}     Copy \"${FILENAME}_merge\" to \"${FILENAME}\".\n";
        MSG="${MSG}     Copy \"${FILENAME}_new\" to \"${BASE_DIR}/${FILENAME}\".\n";
        MSG="${MSG}     Continue installation with [ENTER]\n";
        MSG="${MSG}  *) Stop installation with [STRG-C]";
        ;;
      customized) MSG="${MSG}Customized version will be kept."; ;;
      new)        MSG="${MSG}Customized version will be replaced with new version."; ;;
      abort)      MSG="${MSG}Aborting"; ;;
    esac
        
    attention_multi_msg "${MSG}"
    
    case ${MERGE_MODE} in
      merge)
        echo -e "\n\n    Hit [ENTER] to continue or [STRG-C] to stop:\n\n";
        read
        echo "  Continueing...";
        merge_files $@
        ;;
      customized)
        ${VOLATILE_CP} ${TMP_CUSTOMIZED_FILE} ${CUSTOMIZED_FILE}
        ${VOLATILE_CP} ${NEW_FILE} ${COMMON_BASE_FILE}
        ;;
      new)
        ${VOLATILE_CP} ${TMP_NEW_FILE} ${CUSTOMIZED_FILE}
        ${VOLATILE_CP} ${NEW_FILE} ${COMMON_BASE_FILE}
        ;;
      abort)
        exit 1;
        ;;
    esac
  fi;
}

#
# Trägt die übergebenen Zeilen in server.policy ein, falls die erste Zeile noch nicht eingetragen ist.
# Falls die Datei server.policy geändert wurde, wird XYNAFACTORY_NEEDS_RESTART auf true gesetzt
# Aufruf : add_to_server_policy "Zeile 1" "Zeile 2" ...
add_to_server_policy () {
  FILE_TO_EDIT="${INSTALL_PREFIX}/server/server.policy"
  if [[ $# < 1 ]] ; then return; fi; 
  #Ist erste Zeile bereits in server.policy eingetragen?
  local MATCH=$(${VOLATILE_AWK} -vfirstline="$1" \
                  'BEGIN { matchStr="none"; } \
                   { if( match($0,firstline) != 0) { matchStr=NR; } } \
                   END {print matchStr;}' \
                  ${FILE_TO_EDIT});
  if [[ ${MATCH} = "none" ]] ; then
    #server.policy ergänzen
    backup_file ${FILE_TO_EDIT}
    local LINES;
    for line in "$@"; do 
      LINES="${LINES}\t${line}\n";
    done;
    ${VOLATILE_RM} -f ${TMP_FILE}
    ${VOLATILE_AWK} -vbefore="//this should always be the last permission in this file to guarantee it having no syntax errors." \
                    -vlines="${LINES}" \
                    '{ if( match($0,before) != 0) { print lines; }; print $0; }' \
                    ${FILE_TO_EDIT} > ${TMP_FILE}
    ${VOLATILE_MV} ${TMP_FILE} ${FILE_TO_EDIT}
    ${VOLATILE_RM} -f ${TMP_FILE}
    #Damit server.policy wirksam wird, ist ein Neustart erforderlich
    XYNAFACTORY_NEEDS_RESTART="true";
  fi;
}


f_adjust_guihttp_app () {
  local ret_val=0
  local STR_GUIHTTP_APP_FILE="${PWD}/components/xmcp/GuiHttp.${GUIHTTPVERSION}.app"
  local STR_APPLICATION_XML_FILE="application.xml"
  local STR_TMP_DIR="/tmp"
  local STR_TMP_UNPACK_DIR="${STR_TMP_DIR}/adjustApp"
  local TMP_FILE="application.xml.new"
  local STR_REPLACEMENT=""
  
  ${VOLATILE_CP} "${STR_GUIHTTP_APP_FILE}" "$(realpath $HOSTNAME)/GuiHttp.${GUIHTTPVERSION}.app"
  STR_GUIHTTP_APP_FILE="$(realpath $HOSTNAME)/GuiHttp.${GUIHTTPVERSION}.app"

  ${VOLATILE_RM} -rf "${STR_TMP_UNPACK_DIR}"
  ${VOLATILE_MKDIR} -p "${STR_TMP_UNPACK_DIR}"
  
  choose_interface "HTTP Trigger" "network.interface.httptrigger" true
  if [[ "${CHOOSEN_INTERFACE_NAME}" == '*' ]]; then
    STR_REPLACEMENT="<StartParameter>${HTTP_TRIGGER_PORT}:</StartParameter>"
  else
    STR_REPLACEMENT="<StartParameter>${HTTP_TRIGGER_PORT}:${CHOOSEN_INTERFACE_NAME}:</StartParameter>"
  fi

  cd "${STR_TMP_UNPACK_DIR}"
  ${VOLATILE_UNZIP} -q "${STR_GUIHTTP_APP_FILE}" "${STR_APPLICATION_XML_FILE}"
  f_replace_in_file "${STR_APPLICATION_XML_FILE}" \
    "s+<StartParameter>4245:[^<]*</StartParameter>+${STR_REPLACEMENT}+"
  ${VOLATILE_CHMOD} 664 "${STR_APPLICATION_XML_FILE}"

  ${VOLATILE_ZIP} -q "${STR_GUIHTTP_APP_FILE}" -d "${STR_APPLICATION_XML_FILE}"
  ${VOLATILE_ZIP} -q "${STR_GUIHTTP_APP_FILE}" "${STR_APPLICATION_XML_FILE}"
  cd -

  ${VOLATILE_RM} -rf "${STR_TMP_UNPACK_DIR}"
}

f_adjust_snmpstatistics_app () {
  local ret_val=0
  local STR_APP_FILE="${PWD}/components/xfmg/SNMPStatistics.${SNMPSTATVERSION}.app"
  local STR_APPLICATION_XML_FILE="application.xml"
  local STR_TMP_DIR="/tmp"
  local STR_TMP_UNPACK_DIR="${STR_TMP_DIR}/adjustApp"
  local TMP_FILE="application.xml.new"
  local STR_REPLACEMENT=""
  
  ${VOLATILE_CP} "${STR_APP_FILE}" "$(realpath $HOSTNAME)/SNMPStatistics.${SNMPSTATVERSION}.app"
  STR_APP_FILE="$(realpath $HOSTNAME)/SNMPStatistics.${SNMPSTATVERSION}.app"

  ${VOLATILE_RM} -rf "${STR_TMP_UNPACK_DIR}"
  ${VOLATILE_MKDIR} -p "${STR_TMP_UNPACK_DIR}"
  
  choose_interface "SNMP Trigger" "network.interface.snmptrigger"
  STR_REPLACEMENT="<StartParameter>${CHOOSEN_INTERFACE_NAME}:${SNMP_TRIGGER_PORT}:10:30:200:600:2c:</StartParameter>"

  cd "${STR_TMP_UNPACK_DIR}"
  ${VOLATILE_UNZIP} -q "${STR_APP_FILE}" "${STR_APPLICATION_XML_FILE}"
  f_replace_in_file "${STR_APPLICATION_XML_FILE}" \
    "s+<StartParameter>eth0:5999:10:30:200:600:2c:</StartParameter>+${STR_REPLACEMENT}+"
  ${VOLATILE_CHMOD} 664 "${STR_APPLICATION_XML_FILE}"

  ${VOLATILE_ZIP} -q "${STR_APP_FILE}" -d "${STR_APPLICATION_XML_FILE}"
  ${VOLATILE_ZIP} -q "${STR_APP_FILE}" "${STR_APPLICATION_XML_FILE}"
  cd -

  ${VOLATILE_RM} -rf "${STR_TMP_UNPACK_DIR}"

}


#  Netzwerkinterface interaktiv abfragen
#+ Antwort fuer zukuenftige Updates speichern
# setzt globale Variablen CHOOSEN_INTERFACE und CHOOSEN_INTERFACE_NAME
choose_interface () {
  local PURPOSE="${1}"
  local PROPERTY_NAME="${2}"
  local ALLOW_STAR_SELECTION="${3:-false}"
  
  get_property ${PROPERTY_NAME} "${INSTANCE_PROP_FILE}"
  local INTERFACE_NUMBER="$(get_interface_number "${CURRENT_PROPERTY}")"  #  bugz 9628
  if [[ -z "${INTERFACE_NUMBER}" ]]; then
    echo "Choosing interface for ${PURPOSE}";
    choose_ip_address ${ALLOW_STAR_SELECTION}
    CHOOSEN_INTERFACE="${local_interface[${OWN_IP_SELECTION}]}"
    CHOOSEN_INTERFACE_NAME="${local_interface_name[${OWN_IP_SELECTION}]}"
    
    set_property "${PROPERTY_NAME}" "${CHOOSEN_INTERFACE_NAME}" "${INSTANCE_PROP_FILE}"
    echo "        - storing interface '${CHOOSEN_INTERFACE_NAME} (${CHOOSEN_INTERFACE})' to property '${PROPERTY_NAME}' for later reuse"
  else
    OWN_IP_SELECTION="${INTERFACE_NUMBER}"
    CHOOSEN_INTERFACE="${local_interface[${OWN_IP_SELECTION}]}"
    CHOOSEN_INTERFACE_NAME="${local_interface_name[${OWN_IP_SELECTION}]}"
    echo "        - reusing interface '${CHOOSEN_INTERFACE_NAME} (${CHOOSEN_INTERFACE})' from property '${PROPERTY_NAME}'"
  fi
}


add_basic_requirements () {
  echo -e "\n* Add basic requirements"
  #TODO check if worspace and apps are present
  INDENTATION="  ";
  if f_is_in_list ALL_APPLICATIONS Processing ; then
    f_xynafactory addruntimecontextdependency -ownerWorkspaceName "default workspace" -requirementApplicationName Processing -requirementVersionName "${PROCESSINGVERSION}" -f
  fi
  
  echo -e "    Importing localization data"
  f_xynafactory importlocalization -filename ${INSTALL_PREFIX}/server/resources/localization.xml -o
}

copy_license () {
  local JAR=${1##*/}
  local INDENTATION=${2}
  local NAME=${JAR%.jar}
  local LICENCE_FILES="";
  local THIRD_PARTY_DIR="${INSTALL_PREFIX}/third_parties/"
 
  ${VOLATILE_MKDIR} -p ${THIRD_PARTY_DIR}

  if f_files_exist third_parties/${NAME}-{GROUP,LICENSE,NOTICE,LICENCE}* ; then
  
    echo -e "${INDENTATION}${JAR}"
    
    #Sammlung der Lizenzen
    for LICENCE_FILE in third_parties/${NAME}-{GROUP,LICENSE,NOTICE,LICENCE}* ; do 
      if [ -e ${LICENCE_FILE} ] ; then
        cp ${LICENCE_FILE} ${THIRD_PARTY_DIR}
        LICENCE_FILES="${LICENCE_FILES} ${LICENCE_FILE##*/}"
      fi;
    done;
    
    #Analyse der Lizenz-GROUPs, Sammlung der Lizenzen darin
    if f_files_exist third_parties/${NAME}-GROUP* ; then
      for GROUP_FILE in third_parties/${NAME}-GROUP* ; do
        local GROUP_LIST=$(${VOLATILE_AWK} '$NF>0 && substr($1,1,1)!="#" {print $0}' ${GROUP_FILE})
        for LICENCE_FILE in ${GROUP_LIST} ; do
          if [ -e third_parties/${LICENCE_FILE} ] ; then
            cp third_parties/${LICENCE_FILE} ${THIRD_PARTY_DIR}
            LICENCE_FILES="${LICENCE_FILES} ${LICENCE_FILE##*/}"
          fi;
        done;
      done;
    fi;
    
    #Jar soll in third_parties sichtbar sein
    cp $1 ${THIRD_PARTY_DIR};
    
    #Ausgabe der verwendeten Lizenzen
    if f_is_true ${VERBOSE} ; then
      echo "${INDENTATION}  ->${LICENCE_FILES}";
    fi;
  fi;
}

check_third_party_licenses () {
  local JARS;
  local THIRD_PARTY_DIR;
  case ${CHECK_THIRD_PARTY_LICENSES} in
    server)
      JARS=$(${VOLATILE_FIND} ${INSTALL_PREFIX}/server -name "*.jar" );
      THIRD_PARTY_DIR=${INSTALL_PREFIX}/third_parties
      ;;
    revision)
      JARS=$(${VOLATILE_FIND} ${INSTALL_PREFIX}/revision -name "*.jar" );
      THIRD_PARTY_DIR=${INSTALL_PREFIX}/third_parties
      ;;
    delivery)
      JARS=$(${VOLATILE_FIND} . -name "*.jar" );
      THIRD_PARTY_DIR=third_parties
      ;;
    dir=*)
      local DIR=${CHECK_THIRD_PARTY_LICENSES:4}
      #echo "Looking for jars in ${DIR}";
      JARS=$(${VOLATILE_FIND} ${DIR} -name "*.jar" );
      THIRD_PARTY_DIR=third_parties
      ;;
    app=*)
      local APP=${CHECK_THIRD_PARTY_LICENSES:4}
      if [[ -z $APP ]] ; then
        attention_msg "No Application matching \"\" found";
        return;
      fi;
      if [ -f "${APP}"  ] ; then
        check_third_party_licenses_in_app ${APP}
      else 
        local APPS=$(${VOLATILE_FIND} components -name "${APP}*.app" );
        if [[ -z ${APPS} ]] ; then
          attention_msg "No Application matching \"${APP}\" found";
        fi;
        for APP in ${APPS} ; do
          check_third_party_licenses_in_app ${APP}
        done;
      fi;
      ;;
    *)
      attention_msg "Unexpected mode ${CHECK_THIRD_PARTY_LICENSES}";
      return;
      ;;
  esac
  
  for JAR in ${JARS} ; do
    f_check_license ${JAR} ${THIRD_PARTY_DIR}
  done;
}

check_third_party_licenses_in_app () {
  local APP=$1;
  #echo "    Looking for jars in application ${APP}";
  APPDIR=tmp_unzip_app;
  ${VOLATILE_RM} -rf "${APPDIR}"
  ${VOLATILE_MKDIR} -p "${APPDIR}"
  ${VOLATILE_UNZIP} ${APP} -d "${APPDIR}" > /dev/null
  local JARS=$(${VOLATILE_FIND} ${APPDIR} -name "*.jar" );
  #echo "    Looking for jars in application ${APP}";
  local THIRD_PARTY_DIR=third_parties
  echo $APP
  INDENTATION="    ";
  for JAR in ${JARS} ; do
    f_check_license ${JAR} ${THIRD_PARTY_DIR}
  done;
  
  ${VOLATILE_RM} -rf "${APPDIR}"
}

f_check_license () {
  local JAR=${1}
  local THIRD_PARTY_DIR=${2}
  local JAR_FILE=${JAR##*/}
  local CHECK_LICENSE="true";
  local VENDOR;
    
  #Jar-Name prüfen
  case ${JAR_FILE} in
    serviceDefinition.jar)
      CHECK_LICENSE="false";
      ;;
    serviceDefinition-javadoc.jar)
      CHECK_LICENSE="false";
      ;;
    mdm.jar)
      CHECK_LICENSE="false";
      ;;
        *)
      CHECK_LICENSE="true";
      ;;
  esac;
    
  #Lizenz prüfen
  if f_is_true ${CHECK_LICENSE} ; then
    local NAME=${JAR_FILE%.jar}
    if f_files_exist ${THIRD_PARTY_DIR}/${NAME}-* ; then
      if f_is_true ${VERBOSE} ; then
        echo "${INDENTATION}License installed for ${JAR_FILE}"
      fi;
      return;
    fi;
  fi;
    
  #Vendor prüfen
  if f_is_true ${CHECK_LICENSE} ; then
    VENDOR=$(f_extract_vendor_from_jar ${JAR});
    #echo "Vendor ${VENDOR} for ${JAR}"
    case ${VENDOR} in 
      'Xyna GmbH') 
         CHECK_LICENSE="false";
         ;;
      '${vendor.name}')    #FIXME  das sollte raus!
         #echo "invalid vendor for ${JAR}"
         CHECK_LICENSE="false";
         ;;
      *)
         CHECK_LICENSE="true";
         ;;
    esac;
  fi;
  
  if f_is_true ${CHECK_LICENSE} ; then
    echo "${INDENTATION}License missing for ${JAR_FILE} (Vendor \"${VENDOR}\") location: ${JAR%%*/}" ;
  else
    if f_is_true ${VERBOSE} ; then
      echo "${INDENTATION}No third party license necessary for ${JAR_FILE}"
    fi;
  fi;
}

f_extract_vendor_from_jar () {
  rm -rf META-INF/
  jar -xf ${JAR} META-INF/MANIFEST.MF
  if [ -e META-INF/MANIFEST.MF ] ; then
    ${VOLATILE_AWK} '$1=="Vendor:"{ print substr($0,9,length($0)-9)}' META-INF/MANIFEST.MF
    rm -rf META-INF/
  else
    echo "no MANIFEST.MF";
  fi;
}

f_install_properties () {
  ${VOLATILE_MKDIR} -p ${INSTALL_PREFIX}/server/storage/Configuration/
  local FILE_TO_EDIT=${INSTALL_PREFIX}/server/storage/Configuration/xynaproperties.xml
  echo -e "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" > ${FILE_TO_EDIT}
  echo -e "<xynapropertiesTable>" >> ${FILE_TO_EDIT}
  echo -e "</xynapropertiesTable>" >> ${FILE_TO_EDIT}
  f_add_file_property ${FILE_TO_EDIT} "xyna.rmi.hostname.registry" ${XYNA_RMI_IPADDRESS}
  f_add_file_property ${FILE_TO_EDIT} "xyna.rmi.interlink.hostname.registry" ${XYNA_RMI_IPADDRESS}
  f_add_file_property ${FILE_TO_EDIT} "xyna.rmi.port.registry" ${XYNA_RMI_PORT}
  f_add_file_property ${FILE_TO_EDIT} "xyna.rmi.interlink.port.registry" ${XYNA_RMI_PORT}
}

f_add_file_property () {
  local FILE_TO_EDIT=$1;
  local PROPERTY_KEY=$2;
  local PROPERTY_VALUE=$3;
  local PROPERTY="  <xynaproperties>
    <propertykey>${PROPERTY_KEY}</propertykey>
    <propertyvalue>${PROPERTY_VALUE}</propertyvalue>
    <propertydocumentation/>
    <factorycomponent>false</factorycomponent>
    <binding>0</binding>
  </xynaproperties>"
  f_insert_in_file_before_line "${FILE_TO_EDIT}" "</xynapropertiesTable>"  "${PROPERTY}"
}

#  EOF
