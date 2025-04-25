
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



# Processing library

XYNA_INSTANCES_MAX="999"

XYNA_FACTORY_RUNS="0"
XYNA_FACTORY_STOPPED="5"
XYNA_FACTORY_IS_STARTING="1"
XYNA_FACTORY_IS_STOPPING="6"

#  JVM-Memory options for sending CLI commands
JVM_OPTIONS_CLI_MAXHEAP_SIZE="50M"


create_folder_xynafactory () {
  if [[ ! -d ${INSTALL_PREFIX} ]]; then
    echo -e "\n* Creating installation folder '${INSTALL_PREFIX}'"
    ${VOLATILE_MKDIR} -p ${INSTALL_PREFIX} 2>/dev/null || {
      #  Anlegen des Verzeichnisses hat nicht geklappt
      err_msg "Unable to create directory '${INSTALL_PREFIX}'. Abort!"
      exit 89
    }
  fi
}

create_folder_xynaserver () {
  echo -e "\n* Creating installation folders"

  if [[ ! -d "${INSTALL_PREFIX}" ]]; then
    echo "    + creating folder '${INSTALL_PREFIX}'"
    ${VOLATILE_MKDIR} -p "${INSTALL_PREFIX}" 2>/dev/null || {
      #  Anlegen des Verzeichnisses hat nicht geklappt
      f_exit_with_message ${EX_NOPERM} "Unable to create directory '${INSTALL_PREFIX}'. Abort!"
    }
  fi

  if [[ ! -d "${INSTALL_PREFIX}/bin" ]]; then
    echo "    + creating folder '${INSTALL_PREFIX}/bin'"
    ${VOLATILE_MKDIR} -p "${INSTALL_PREFIX}/bin" 2>/dev/null || {
      #  Anlegen des Verzeichnisses hat nicht geklappt
      f_exit_with_message ${EX_NOPERM} "Unable to create directory '${INSTALL_PREFIX}/bin'. Abort!"
    }
  fi

  check_if_user_exists "${XYNA_USER}"
  if [[ $? -eq 1 ]]; then
    local TARGET_FILE="${USERHOME_PREFIX}/${XYNA_USER}/.bashrc"
    if [[ "x${XYNA_USER}" == "xroot" ]]; then
  TARGET_FILE="/root/.bashrc"
    fi
    if [[ ! -f "${TARGET_FILE}" ]]; then
      echo >> "${TARGET_FILE}"
      echo "# set environment variable 'PATH'" >> "${TARGET_FILE}"
      echo "export PATH=\"\${PATH}\"" >> "${TARGET_FILE}"
    fi

    case $(${VOLATILE_GREP} -v "^#" "${TARGET_FILE}" | ${VOLATILE_GREP} -c " PATH=") in
  0) echo >> "${TARGET_FILE}"
     echo "# set environment variable 'PATH'" >> "${TARGET_FILE}"
     echo "export PATH=\"\${PATH}:${INSTALL_PREFIX}/bin\"" >> "${TARGET_FILE}"
     ;;
  1) local STR_PATH="$(${VOLATILE_AWK} 'BEGIN { FS="=" } $1 == "export PATH" {print $2}' "${TARGET_FILE}")"
     local NEW_PATH="$(add_elements_to_string "${STR_PATH}" "${INSTALL_PREFIX}/bin" | ${VOLATILE_TR} " " ":" | ${VOLATILE_TR} -d "\"")"
     f_replace_in_file "${TARGET_FILE}" "s+export PATH=.*+export PATH=\"${NEW_PATH}\"+"
     ;;
  *) err_msg "${TARGET_FILE} contains multiple PATH statements - this needs to be fixed manually!";;
    esac

    #  bugz 15389: /opt/xyna als Link unterstuetzen
    local TARGET_DIR="${INSTALL_PREFIX}"
    if [[ -L "${INSTALL_PREFIX}" ]]; then
  #  Linkziel aufloesen, damit das 'chown' darauf ausgefuehrt wird und nicht auf den Link selbst...
  TARGET_DIR=$(cd -P "${INSTALL_PREFIX}" > /dev/null; ${VOLATILE_PWD})
    fi
    ${VOLATILE_CHOWN} -R "$(f_generate_chown_parameter)" "${TARGET_DIR}" "${TARGET_FILE}"
  fi
}

######################################################################
#  Lesen einer Xyna-Property
#
#  Eingabeparameter
#o   Property-Name
#o   Default-Value, falls Property nicht gesetzt ist
######################################################################
f_get_xyna_property() {
  #direkter Aufruf, um keine modifizierte Ausgabe zu erhalten
  local RET=$(${INSTALL_PREFIX}/server/xynafactory.sh get -key ${1} | 
               ${VOLATILE_AWK} '$1=="Property"{print "'${2}'"} $1=="Value"{print substr($0,index($0,":")+2)}'
             );
  echo ${RET};
}

######################################################################
#  Setzen einer Xyna-Property
#
#  Eingabeparameter
#o   Property-Name
#o   Value
#o   (optional) only_if_not_set (true,false), false ist Default
######################################################################

f_set_xyna_property() {
  local only_if_not_set=${3:-false}
  if f_is_true ${only_if_not_set} ; then 
    f_xynafactory set -key "$1" -value "$2" -n
  else 
    f_xynafactory set -key "$1" -value "$2"
  fi;

  local documentation=${4:-}
  f_xynafactory setpropertydocumentation -key "$1" -documentation "${documentation}"
}

######################################################################
#  Zurücksetzen einer Xyna-Property
#
#  Eingabeparameter
#o   Property-Name
#o   Value
#o   Default-Value, falls Value==Default-Value -> removeProperty
######################################################################
f_reset_xyna_property() {
  if [ "${2}" == "${3}" ]; then
    f_xynafactory removeproperty -key ${1}
  else 
    f_xynafactory set -key ${1} -value "${2}"
  fi
}









#  Setzen des monitoring levels fuer einen ordertype
f_set_xyna_monitoring_level() {

 local ret_val=0
  local BLACK_HOME="${1}"
  local STR_LEVEL="${2}"
  local STR_ORDERTYPE="${3}"
  local BLN_OVERWRITE="${4:-false}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  
  f_check_xyna_online ${BLACK_HOME}
  ret_val=$?
  
  local INT_IS_ALREADY_SET=$(${XYNA_FACTORY_SH} getmonitoringlevel -orderType ${STR_ORDERTYPE} | ${VOLATILE_GREP} -c "${STR_ORDERTYPE}")
  if [[ ${INT_IS_ALREADY_SET} -gt 0 ]]; then
    ${XYNA_FACTORY_SH} getmonitoringlevel -orderType ${STR_ORDERTYPE} | ${VOLATILE_GREP} "${STR_ORDERTYPE}" | ${VOLATILE_SED} -e "s+^got: ++" -e "s+^Monitoring code+Monitoring level already set+"
  fi
  
  if [[ ${INT_IS_ALREADY_SET} -eq 0 || "x${BLN_OVERWRITE}" == "xtrue" ]]; then
    echo "Setting monitoring level for ordertype '${STR_ORDERTYPE}': ${STR_LEVEL}"
    debug_msg "${XYNA_FACTORY_SH} -q setmonlvl -code \"${STR_LEVEL}\" -orderType \"${STR_ORDERTYPE}\""
    ${XYNA_FACTORY_SH} -q setmonlvl -code "${STR_LEVEL}" -orderType "${STR_ORDERTYPE}"
    ret_val=$?
  fi  
  
  return  ${ret_val}
}


# Xyna Rolle mit zugehoerigen Rechten anlegen
f_create_role() {
  local ret_val=0
  local BLACK_HOME="${1}"
  local STR_ROLE="${2}"
  local LIST_RIGHTS="${3}"
  local BLN_OVERWRITE="${4:-false}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
   
  f_check_xyna_online ${BLACK_HOME}
  ret_val=$?
  

  local INT_IS_ALREADY_SET=$(${XYNA_FACTORY_SH} listroles | ${VOLATILE_SED} -e "s+^got: ++" | ${VOLATILE_GREP} " XYNA" | ${VOLATILE_AWK} '{print $1}' | ${VOLATILE_GREP} -wc ${STR_ROLE})
  if [[ ${INT_IS_ALREADY_SET} -eq 1 ]]; then
     echo -n " Role ${STR_ROLE} already exist - "
     if [[ "x${BLN_OVERWRITE}" == "xtrue" ]]; then
       echo "recreating"
     else
       echo "skipping"
     fi
  fi
 
  if [[ ${INT_IS_ALREADY_SET} -eq 0 || "x${BLN_OVERWRITE}" == "xtrue" ]]; then
    echo "Creating role ${STR_ROLE}"
    debug_msg "${XYNA_FACTORY_SH} -q createrole -domainName XYNA -roleName \"${STR_ROLE}\""
    ${XYNA_FACTORY_SH} -q createrole -domainName XYNA -roleName "${STR_ROLE}"
    ret_val=$?
    if [[ ${ret_val} -eq 0 ]]; then
     for STR_RIGHT in ${LIST_RIGHTS}; do
       echo "  + Assigning right '${STR_RIGHT}' to role '${STR_ROLE}'"
       debug_msg "${XYNA_FACTORY_SH} -q grantright -roleName \"${STR_ROLE}\" -rightName \"${STR_RIGHT}\""
       ${XYNA_FACTORY_SH} -q grantright -roleName "${STR_ROLE}" -rightName "${STR_RIGHT}"
       ret_val=$((${ret_val} + $?))
     done   
    fi
  fi
  
  return  ${ret_val}
}


######################################################################
#  Setzen einer Xyna-Kapazitaet
#
#  Eingabeparameter
#o   Basispfad zur Xyna Factory                    ${black_home}
#o   Capacity-Name
#o   Cardinality
#o   Ersetzte vorhandenen Wert (optional)         'false'
######################################################################
# Capacity in XynaFactory setzen
f_set_xyna_capacity() {
  
  local ret_val=0
  local BLACK_HOME="${1}"
  local STR_NAME="${2}"
  local STR_CARDINALITY="${3}"
  local BLN_OVERWRITE="${4:-false}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  
  f_check_xyna_online ${BLACK_HOME}
  ret_val=$?
  echo "    + setting capacity '${STR_NAME}'"
  local INT_IS_ALREADY_SET=$(${XYNA_FACTORY_SH} listcapacities | ${VOLATILE_GREP} -cw "'${STR_NAME}'")
  if [[ ${INT_IS_ALREADY_SET} -gt 0 ]]; then
    #  Zweistufiges entfernen mit sed? Warum nicht alles in einem Kommando entfernen?
    #+ Fabrik kann eine Spalte mit "got: " ausgeben, oder auch nicht. Beide Faelle muessen abgedeckt werden...
    ${XYNA_FACTORY_SH} listcapacities | ${VOLATILE_GREP} -w "'${STR_NAME}'" | ${VOLATILE_SED} -e "s+^got: ++" | ${VOLATILE_SED} -e "s+^+    o already exists: +"
  fi
  
  if [[ ${INT_IS_ALREADY_SET} -eq 0 || "x${BLN_OVERWRITE}" == "xtrue" ]]; then
    echo "    + creating ${STR_NAME}: ${STR_CARDINALITY}"
    debug_msg "${XYNA_FACTORY_SH} addcapacity -name \"${STR_PROPERTY_NAME}\" -cardinality  \"${STR_PROPERTY_VALUE}\""
    ${XYNA_FACTORY_SH}  addcapacity -name "${STR_NAME}" -cardinality  "${STR_CARDINALITY}"
    ret_val=$?
  fi  
  
  return  ${ret_val}
}

######################################################################
# Workflow starten
#  Eingabeparameter
#o   Basispfad zur Xyna Factory                    ${black_home}
#o   Name des Workflows 
#o   Optionale Parameter    
######################################################################
f_start_workflow() {
  local ret_val=0
  local BLACK_HOME="${1}"
  local STR_WORKFLOW="${2}"
  local STR_EXTRA_OPTIONS="${3}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"

  f_check_xyna_online ${BLACK_HOME}
  ret_val=$?
  
  if [[ -n ${STR_WORKFLOW} ]]; then
    echo "Starting workflow '${STR_WORKFLOW}'"
    debug_msg  "${XYNA_FACTORY_SH} -q startorder -orderType \"${STR_WORKFLOW}\" ${STR_EXTRA_OPTIONS}"
    ${XYNA_FACTORY_SH} -q startorder -orderType "${STR_WORKFLOW}" ${STR_EXTRA_OPTIONS}
    ret_val=$?
  else
    err_msg "f_start_workflow: no workflow specified"
    ret_val=${EX_XYNA}
  fi

  return  ${ret_val}
}


###########################
# Ueberpruefe of xynafactory.sh existiert und Xyna Factory "online" ist
#  Eingabeparameter
#o  Basispfad zur Xyna Factory                    ${black_home}
#o  Exit wenn nicht vorhanden oder nicht online   Default:true 
f_check_xyna_online() {
  local BLACK_HOME="${1}"
  local BLN_EXIT_ON_FAILURE="${2:-true}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  local PRODUCT_LIB_SH="${BLACK_HOME}/server/product_lib.sh"
  local ret_val=${EX_XYNA};
  
  if [[ -x "${XYNA_FACTORY_SH}" ]]; then
    ${XYNA_FACTORY_SH} -q status
    ret_val=$?
    debug_msg "Current Xyna status: ${ret_val}" 2
    debug_msg "Running Xyna status: ${XYNA_FACTORY_RUNS}" 2
    if [[ ${ret_val} -ne ${XYNA_FACTORY_RUNS:-0}  && "x${BLN_EXIT_ON_FAILURE}" == "xtrue"  ]]; then
      ${XYNA_FACTORY_SH} status
      f_exit_with_message ${EX_XYNA} "Xyna Factory must be running. Abort!"
    fi
  else
    if [[ "x${BLN_EXIT_ON_FAILURE}" == "xtrue" ]]; then
      f_exit_with_message ${EX_NOPERM} "'${XYNA_FACTORY_SH}' not found."
    else
      err_msg "'${XYNA_FACTORY_SH}' not found."
      ret_val=${EX_XYNA}
    fi  
  fi
  return ${ret_val}
}

warn_for_factory_restart () {
  #  bugz 13320: Warnung ausgeben, wenn eine Komponente ausgewaehlt wurde, die einen Neustart erfordert.
  if [[ "x${COMPONENT_XYNAFACTORY}" == "xtrue" ]]; then
    attention_msg "Xyna Factory will be restarted if you continue."
    ${VOLATILE_CAT} << A_HERE_DOCUMENT

Hit [ENTER] to continue or [Ctrl-C] to stop:

A_HERE_DOCUMENT
    read
  fi
}


# wait_for_factory_status
# wartet auf den Statusübergang der Factory, derzeit nur start oder stop
# Eingabeparameter
# o start oder stop
# o Wartezeit zwischen der Ausgebe der Punkte
# o maximale Anzahl an Retries 
#
wait_for_factory_status () {
  local SLEEP=${2:-2}
  local MAX_RETRIES=${3:-1800}
  
  local SOLL_STATUS
  case "${1}" in
    start)  SOLL_STATUS="${XYNA_FACTORY_RUNS}";;
    stop)   SOLL_STATUS="${XYNA_FACTORY_STOPPED}";;
    *) echo "Which target status has to be reached? Abort!"; exit 95;;
  esac
  
  local IST_STATUS;
  local XYNA_PID;
  local START_TIME="$(${VOLATILE_DATE} '+%s')"
  
  if f_selected ${VERBOSE} ; then echo -n "Waiting for ${XYNA_INSTANCENAME} "; fi;
  
  for ((t=0 ; t< ${MAX_RETRIES}; ++ t )) ; do
    local IST_STATUS=$(f_xynafactory_status)
    
    if [[ ${IST_STATUS} == ${SOLL_STATUS} ]]; then
      break; #Warten zuende, Zielzustand erreicht
    fi;
    #Frühzeitiges Abbrechen des Wartens, falls Factory nicht gestartet werden kann
    if [[ ${SOLL_STATUS} == ${XYNA_FACTORY_RUNS} ]] ; then
      if abort_waiting_for_factory_start ${t} ${IST_STATUS} ; then
        break;
      fi;
    fi;
  
    if f_selected ${VERBOSE} ; then echo -n "."; fi;
    sleep ${SLEEP};
  done;
  
  local END_TIME="$(${VOLATILE_DATE} '+%s')"
  local TIME_DIFFERENCE=0
  #  stupid Solaris does not support '+%s' for the date command!
  if [[ "${START_TIME}" == '%s' ]]; then
      #  stupid solaris: do nothing, time cannot be calculated
      :
  else
    TIME_DIFFERENCE=$(( END_TIME - START_TIME ))
  fi
  
  if [[ ${IST_STATUS} == ${SOLL_STATUS} ]]; then
    if f_selected ${VERBOSE} ; then
      local MSG="${XYNA_INSTANCENAME}";
      case "${1}" in
        start) MSG="${MSG} up and running" ;;
        stop)  MSG="${MSG} shut down" ;;
      esac
      if [[ ${TIME_DIFFERENCE} -gt 0 ]]; then
        MSG="${MSG} after ${TIME_DIFFERENCE} seconds"
      fi;
      MSG="${MSG}.";
      message_ok ${MSG}
    fi;
  else
    local MSG="${XYNA_INSTANCENAME}";
    case "${1}" in
      start) MSG="${MSG} did not start" ;;
      stop)  MSG="${MSG} did not shut down" ;;
    esac
    if [[ ${TIME_DIFFERENCE} -gt 0 ]]; then
      MSG="${MSG} while waiting for ${TIME_DIFFERENCE} seconds"
    fi;
    MSG="${MSG}.";
    message_fail_and_exit ${MSG}
  fi
}

#
# abort_waiting_for_factory_start
# Ermöglicht ein frühzeitiges Abbrechen des Wartens auf den XynaFactory-Start.
# Es muss bald ein "xynafactory.pid" existieren, wenig später muss Factory im Status "Starting" sein
# Eingabeparameter
# o Tick
# o aktueller Status
abort_waiting_for_factory_start() {
  local TICK=${1}
  local IST_STATUS=${2}
  local TARGET_FILE="${PID_FOLDER}/xynafactory.pid"
  local MAX_TICK_FOR_PID=12; #nach soviel Ticks muss PID vorliegen
  local MAX_TICK_FOR_NOT_STARTING=10; #nach soviel Ticks muss Factory-Status STARTING sein

  if [[ -z "${PID_FOLDER}" ]]; then
    TARGET_FILE="${INSTALL_PREFIX}/server/xynafactory.pid"
  fi

  if [[ ${TICK} -ge ${MAX_TICK_FOR_PID} ]] ; then
    if [[ ! -f "${TARGET_FILE}" ]]; then
      #echo "PID does not exist in tick ${TICK}";
      return 0;
    fi;
  fi;
  if [[ ${TICK} -ge ${MAX_TICK_FOR_NOT_STARTING} ]] ; then
    if [[ ${IST_STATUS} == ${XYNA_FACTORY_STOPPED} ]] ; then
      #echo "Factory is not starting in tick ${TICK}";
      return 0;
    fi;
  fi;
  return 1;
}



start_xynafactory () {
  ${INSTALL_PREFIX}/server/xynafactory.sh start || {
    err_msg "Unable to start Xyna Factory. Abort!"
    echo
    exit 88
  }
}

stop_xynafactory () {
  if [[ -f ${INSTALL_PREFIX}/server/xynaserver.sh ]]; then
    ${INSTALL_PREFIX}/server/xynaserver.sh stop
  elif [[ -f ${INSTALL_PREFIX}/server/xynafactory.sh ]]; then
    ${INSTALL_PREFIX}/server/xynafactory.sh stop
  else
    err_msg "Unable to stop Xyna Factory using '${INSTALL_PREFIX}/server/xynafactory.sh'. Skipping..."
  fi
}

j_start_xynafactory () {
  #  bugz 10352
  export LANG="${OS_LOCALE}"
  export LC_ALL="${OS_LOCALE}"

  local STATUS_INT=$(f_xynafactory_status)

  if [[ "${STATUS_INT:-${XYNA_FACTORY_RUNS}}" == "${XYNA_FACTORY_RUNS}" ]]; then
    message_fail_and_exit "Cannot start ${XYNA_INSTANCENAME} on port ${FACTORY_CLI_PORT} because it is already running."
  elif [[ "${STATUS_INT:-${XYNA_FACTORY_STOPPED}}" == "${XYNA_FACTORY_IS_STARTING}" ]]; then
    message_fail_and_exit "Cannot start ${XYNA_INSTANCENAME} on port ${FACTORY_CLI_PORT} because it is already starting."
  elif [[ "${STATUS_INT:-${XYNA_FACTORY_STOPPED}}" == "${XYNA_FACTORY_IS_STOPPING}" ]]; then
    message_fail_and_exit "Cannot start ${XYNA_INSTANCENAME} on port ${FACTORY_CLI_PORT} because it is currently shutting down."
  fi

  if [[ "x${VERBOSE}" == "xtrue" ]]; then message_ok "${XYNA_INSTANCENAME} on port ${FACTORY_CLI_PORT} starting."; fi

  if [[ "x${DEBUG}" == "xtrue" ]]; then
    if [[ "x${VERBOSE}" == "xtrue" ]]; then message_ok "Debug port ${DEBUG_SERVER_PORT} enabled."; fi
    DEBUG_OPTIONS="-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_SERVER_PORT} -Xdebug -Xnoagent -Djava.compiler=NONE";
  fi

  # ssl parameter -Djavax.net.ssl.trustStore=cacerts -Djavax.net.ssl.keyStore=cacerts -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.debug=all

  local PID_FOLDER_OPTION="-Dpid.folder=$PID_FOLDER"

  local JEP_OPTION=$([ -n "$JEP_MODULE_PATH" ] && echo "-Djep.module.path=$JEP_MODULE_PATH" || echo "")
  
  local JAVA_OPTIONS="-DBLACK_SERVER_HOME=${PWD} -Xms${JVM_OPTIONS_MINHEAP_SIZE} -Xmx${JVM_OPTIONS_MAXHEAP_SIZE} ${LOG4J_OPTIONS} ${EXCEPTION_OPTIONS} ${GC_OPTIONS} ${PROFILING_OPTIONS} ${DEBUG_OPTIONS} ${RMI_OPTIONS} ${XML_BACKUP_OPTIONS} $PID_FOLDER_OPTION $JEP_OPTION ${ADDITIONAL_OPTIONS}";

  if [ -n "${PYTHON_VENV_PATH}" ]; then
    source "${PYTHON_VENV_PATH}/bin/activate"
  fi

  f_start_factory_internal ${JAVA_OPTIONS} com.gip.xyna.xmcp.xfcli.XynaFactoryCommandLineInterface "$@" >/dev/null 2>&1 &

  wait_for_factory_status start
  VERBOSE=false
  f_xynafactory listfurtherinformationfromstartup  
}

j_stop_xynafactory () {
  local STATUS_INT=$(f_xynafactory_status)
  
  if [[ "${STATUS_INT:-${XYNA_FACTORY_RUNS}}" == "${XYNA_FACTORY_STOPPED}" ]]; then
    message_fail_and_exit "Cannot stop ${XYNA_INSTANCENAME} on port ${FACTORY_CLI_PORT} because it is not running."
  fi

  if [[ "${STATUS_INT:-${XYNA_FACTORY_RUNS}}" == "${XYNA_FACTORY_IS_STARTING}" ]]; then
    message_fail_and_exit "Cannot stop ${XYNA_INSTANCENAME} because on port ${FACTORY_CLI_PORT} it is currently starting."
  fi

  if [[ "${STATUS_INT:-${XYNA_FACTORY_RUNS}}" == "${XYNA_FACTORY_IS_STOPPING}" ]]; then
    message_fail_and_exit "Cannot stop ${XYNA_INSTANCENAME} because on port ${FACTORY_CLI_PORT} it is currently shutting down."
  fi

  if [[ "x${VERBOSE}" == "xtrue" ]]; then
    message_ok "${XYNA_INSTANCENAME} on port ${FACTORY_CLI_PORT} stopping."
  fi

  ${FACTORY_CLI_CMD} -Xmx${JVM_OPTIONS_CLI_MAXHEAP_SIZE} ${LOG4J_OPTIONS} com.gip.xyna.xmcp.xfcli.XynaFactoryCommandLineInterface "$@"
  if [[ $? -ne 0 ]]; then
    message_fail_and_exit "Cannot stop ${XYNA_INSTANCENAME} on port ${FACTORY_CLI_PORT}."
  fi
  
  wait_for_factory_status stop
}

run_xynafactory_cmd () {
  ${FACTORY_CLI_CMD} -Xmx${JVM_OPTIONS_CLI_MAXHEAP_SIZE} ${LOG4J_OPTIONS} com.gip.xyna.xmcp.xfcli.XynaFactoryCommandLineInterface "$@"
  return $?
}


#  Try to read pid from the Xyna Factory pid-file.
#
#  Perform the following checks:
#o   File can be found
#o   PID could be read
#o   a process with the PID could be found
#o   the process is 'java'
#o   the process is owned by ${XYNA_USER}
#
#  Returns:
#+   'false', if one of the checks failed
#+   PID, if valid process could be found
f_get_xynafactory_pid () {
  local ret_val="false"
  local TARGET_FILE="${PID_FOLDER}/xynafactory.pid"
  local XYNA_PID

  if [[ -z "${PID_FOLDER}" ]]; then
    echo "pid folder not set. Expecting pid file at ${INSTALL_PREFIX}/server/xynafactory.pid"
    TARGET_FILE="${INSTALL_PREFIX}/server/xynafactory.pid"
  fi

  #  File can be found
  if [[ -f "${TARGET_FILE}" ]]; then

    #  PID could be read
    XYNA_PID=$(${VOLATILE_CAT} "${TARGET_FILE}")
    if [[ -n "${XYNA_PID}" ]]; then

      local STR_USER=$(   ${VOLATILE_PS} -o pid,user,fname -p "${XYNA_PID}"  | ${VOLATILE_AWK} '$1 == '${XYNA_PID}' {print $2}')
      local STR_COMMAND=$(${VOLATILE_PS} -o pid,user,fname -p "${XYNA_PID}"  | ${VOLATILE_AWK} '$1 == '${XYNA_PID}' {print $3}')

      #  a process with the PID could be found
      if [[ -n "${STR_USER}" ]]; then

        #  the process is 'java'
        if [[ "${STR_COMMAND}" == "java" ]]; then

          #  the process is owned by ${XYNA_USER}
          if [[ "${STR_USER}" == "${XYNA_USER}" ]]; then

            #  o.k. - pid is valid.
            ret_val="${XYNA_PID}"

          else
            err_msg "Process with pid '${XYNA_PID}' is owned by '${STR_USER}', must be '${XYNA_USER}'."
          fi

        else
          err_msg "Process with pid '${XYNA_PID}' is '${STR_COMMAND}', must be 'java'."
        fi

      else
        err_msg "Unable to locate a process with pid '${XYNA_PID}'."
      fi

    else
      err_msg "Unable to read pid from '${TARGET_FILE}'."
    fi

  else
    err_msg "Unable to locate '${TARGET_FILE}'."
  fi

  echo "${ret_val}"
}

get_class_jars () {
  CLASSES="";
  for i in lib/*.jar userlib/*.jar conpooltypes/*.jar; do
    if [[ -f "${i}" ]]; then
      CLASSES="${i}:${CLASSES}"
    fi
  done
  export CLASSES="${CLASSES}"
}


# Neustart XynaFactory
f_restart_xynafactory() {  
 echo -e "\n* Restarting Xyna Factory ${PRODUCT_INSTANCE:-001} at 'localhost'"
 str_CMD=/etc/init.d/xynafactory_${PRODUCT_INSTANCE-"001"}
 if [[ ! -x ${str_CMD} ]]; then
   str_CMD=${INSTALL_PREFIX}/etc/init.d/xynafactory
 fi
 if [[ ! -x ${str_CMD} ]]; then
   str_CMD=${BLACK_EDITION_INSTALL_PREFIX}/server/xynafactory.sh
 fi
 exit_if_not_exists  ${str_CMD} " - Xyna Factory not proper installed!"
 ${str_CMD} restart
 return $?
}  


############################################################
#  XynaFactory-Aufruf
#
#
#  Globale Parameter:
#    in  VERBOSE=<boolean>
#    in  INDENTATION=<string>
#    out XYNA_FACTORY_OUTPUT
#    out XYNA_FACTORY_RC
#
#  Eingabeparameter:
#    alle XynaFactory-Aufruf-Parameter 
#
#  Ausgabe
#    bei VERBOSE=true: XynaFactory-Aufruf
#    bei XYNA_FACTORY_RC !=(0,9,12): attention_multi_msg mit XYNA_FACTORY_RC und XYNA_FACTORY_OUTPUT
#
#  Rückgabe:
#    XYNA_FACTORY_RC
############################################################
f_xynafactory_silent () {
  XYNA_FACTORY_OUTPUT=$(${INSTALL_PREFIX}/server/xynafactory.sh "$@");
  XYNA_FACTORY_RC=$?
  if [[ ${XYNA_FACTORY_RC} == 0 || ${XYNA_FACTORY_RC} == 9 || ${XYNA_FACTORY_RC} == 12 ]] ; then 
    #                   SUCCESS,     SUCCESS_BUT_NO_CHANGE,       SUCCESS_WITH_PROBLEM
    if f_selected ${VERBOSE} ; then 
      echo "${INDENTATION}./xynafactory.sh $@";
    fi
  else 
    local MSG="./xynafactory.sh ${@}\nfailed with return code ${XYNA_FACTORY_RC}:\n\n${XYNA_FACTORY_OUTPUT}";
    attention_multi_msg "${MSG}";
  fi;
  return ${XYNA_FACTORY_RC}
}

############################################################
#  XynaFactory-Aufruf
#
#
#  Globale Parameter:
#    in  VERBOSE=<boolean>
#    in  INDENTATION=<string>
#    out XYNA_FACTORY_OUTPUT
#    out XYNA_FACTORY_RC
#
#  Eingabeparameter:
#    alle XynaFactory-Aufruf-Parameter 
#
#  Ausgabe
#    bei VERBOSE=true: XynaFactory-Aufruf
#    bei XYNA_FACTORY_RC  =(0,9,12): XYNA_FACTORY_OUTPUT
#    bei XYNA_FACTORY_RC !=(0,9,12): attention_multi_msg mit XYNA_FACTORY_RC und XYNA_FACTORY_OUTPUT
#
#  Rückgabe:
#    XYNA_FACTORY_RC
############################################################
f_xynafactory () {
  XYNA_FACTORY_OUTPUT=$(${INSTALL_PREFIX}/server/xynafactory.sh "$@");
  XYNA_FACTORY_RC=$?
  if [[ ${XYNA_FACTORY_RC} == 0 || ${XYNA_FACTORY_RC} == 9 || ${XYNA_FACTORY_RC} == 12 ]] ; then 
    #                   SUCCESS,     SUCCESS_BUT_NO_CHANGE,       SUCCESS_WITH_PROBLEM
    if f_selected ${VERBOSE} ; then 
      echo "${INDENTATION}./xynafactory.sh $@";
    fi
    echo "${XYNA_FACTORY_OUTPUT}" | sed "s+^+${INDENTATION}+" #Einrücken
  else 
    local MSG="./xynafactory.sh ${@}\nfailed with return code ${XYNA_FACTORY_RC}:\n\n${XYNA_FACTORY_OUTPUT}";
    attention_multi_msg "${MSG}";
  fi;
  return ${XYNA_FACTORY_RC}
}


f_xynafactory_status () {
  local RET;
  local RC;
  RET=$(${INSTALL_PREFIX}/server/xynafactory.sh -q status);
  RC=$?
  echo ${RC};
  return ${RC}
} 

f_xynafactory_status_to_string () {
  local STATUS=$1;
  if [[ -z $1 ]] ; then
    STATUS=$(f_xynafactory_status);
  fi;
  case ${STATUS} in
    ${XYNA_FACTORY_RUNS}) echo "running";;
    ${XYNA_FACTORY_STOPPED}) echo "stopped";;
    ${XYNA_FACTORY_IS_STARTING}) echo "starting";;
    ${XYNA_FACTORY_IS_STOPPING}) echo "stopping";;
    *) echo "unknown state ${STATUS}";;
  esac
} 


#Trick:
# Aufruf f_start_factory_internal ${JAVA_OPTIONS} com.gip.xyna.xmcp.xfcli.XynaFactoryCommandLineInterface "$@" >/dev/null 2>&1 &
#verhindert nun, dass irgendwelche Ausgaben aus dem Hintergrund-Prozess entweichen.
#Diese möglichen Ausgaben haben verhindert, dass bei Factory-Installation das tee in install_black_edition.sh beendet wurde.
f_start_factory_internal() {
  nohup ${FACTORY_CLI_CMD} "$@" 2>&1 | ${VOLATILE_LOGGER} -p "${XYNA_SYSLOG_FACILITY}.debug" &
}
