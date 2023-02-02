
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



# Portal -> Oracle OC4J library

####################
# Status OC4J-Conatainer pruefen
f_check_oc4j_container_status () {
  
  local ret_val=0
  local OC4JINSTANCE="${1}" 
  VOLATILE_OPMNCTL=$(get_full_path_to_executable opmnctl)
  
  if [[ "x${OC4JINSTANCE}" == "x" ]]; then
    ${VOLATILE_OPMNCTL} status -fmt %prt50%sta8
  else
   
    #  Liste laufender OC4J-Instanzen bestimmen:
    #     opmnctl status -fmt %prt50%sta8
    # process-type                                       | status
    # ---------------------------------------------------+---------
    # OC4J:Tethys7                                       | Alive
    # OC4J:Tethys6                                       | Down
    # HTTP_Server                                        | Alive
    #
    #  Zeile mit dem Container filtern und den Status aus der letzten Spalte ausgeben
    ${VOLATILE_OPMNCTL} status -fmt %prt50%sta8 | ${VOLATILE_AWK} '/^OC4J:'"${OC4JINSTANCE}"'/ {print $NF}'
    ret_val=$?
  fi
  return ${ret_val}
}

####################
# Status OC4J-application pruefen
f_check_oc4j_application_status () {
 
  local ORACLE_HOME="${1}"
  local OC4JAPPLICATION="${2}" 
  local ret_val=0
 
  VOLATILE_OPMNCTL=$(get_full_path_to_executable opmnctl)
  
  if [[ "x" == "x${ORACLE_HOME}" ]]; then
    echo "    + \$ORACLE_HOME is not set - skipping"
    return ${EX_USAGE}
  fi
  if [[ ! -d "${ORACLE_HOME}/opmn" ]]; then
    echo "    + \$ORACLE_HOME does not point to an Application Server - skipping"
    return ${EX_USAGE}
  fi
  
  VOLATILE_OPMNCTL=$(get_full_path_to_executable opmnctl)
       
  if [[ "x${OC4JAPPLICATION}" == "x" ]]; then
    ${VOLATILE_OPMNCTL} status -app
    ret_val=$?
  else

    #  Liste laufender OC4J-Instanzen bestimmen:
    #     opmnctl status -app
    #------+-----------------------------+---------+---------+----------------+----------+------------------
    #pid   | name                        | state   | rtid    | classification | routable | parent           
    #------+-----------------------------+---------+---------+----------------+----------+------------------
    #1394  | system                      | started | g_rt_id | external       | true     |                  
    #1394  | default                     | started | g_rt_id | external       | true     | system            
    #1394  | ascontrol                   | stopped | g_rt_id | external       | false    |                  
    #1394  | datatags                    | started | g_rt_id | internal       | true     | default          
    #1394  | javasso                     | stopped | g_rt_id | internal       | true     |                  
    #28437 | imxyz                       | started | g_rt_id | external       | true     | default          
    #28437 | system                      | started | g_rt_id | external       | true     |                  
    #28437 | cnm                         | started | g_rt_id | external       | true     | default   
    #
    #  Zeile mit der Application filtern und den Status aus der 5. Spalte ausgeben
    ${VOLATILE_OPMNCTL} status -app | ${VOLATILE_GREP} -w "${OC4JAPPLICATION}" |  ${VOLATILE_AWK} '{print $5}'
    ret_val=$?
  fi
  
  return ${ret_val}
}

####################
# Application in OC4J undeployen
# 1: Pfad zum Oracle_Home
# 2: Name des OC4J-Container
# 3: Name der Applications
# 4: Benutzername des OC4J-ADMINS, default:  oc4jadmin
# 5: Passwort des OC4J-ADMINS, default: oracle10
f_undeploy_oc4j () {
  
  local ORACLE_HOME="${1}"
  local OC4JINSTANCE="${2}"
  local APPLNAME="${3}"
  local OC4J_ADMIN_USER="${4:-oc4jadmin}"
  local OC4J_ADMIN_PWD="${5:-oracle10}"
  local ret_val=0
    
  echo "* Undeploy application '${APPLNAME}' in instance '${OC4JINSTANCE}'"
  
  if [[ "x" == "x${ORACLE_HOME}" ]]; then
    echo "    + \$ORACLE_HOME is not set - skipping"
    return ${EX_USAGE}
  fi
  if [[ ! -d "${ORACLE_HOME}/opmn" ]]; then
    echo "    + \$ORACLE_HOME does not point to an Application Server - skipping"
    return ${EX_USAGE}
  fi
  
  if [[ ! -f ${ORACLE_HOME}/j2ee/home/admin_client.jar ]]; then
    err_msg "Cannot find '${ORACLE_HOME}/j2ee/home/admin_client.jar'"
    return ${EX_OSFILE}
  fi
    
  VOLATILE_JAVA=$(get_full_path_to_executable java)
  
  OC4J_STATUS=$(f_check_oc4j_container_status "${OC4JINSTANCE}")
  case ${OC4J_STATUS} in
    Alive)
       debug_msg "Oracle OC4J instance '${OC4JINSTANCE}' is running"
      ;;
     Down|Init)
        err_msg "Oracle OC4J instance '${OC4JINSTANCE}' is not running. Start instance and try again!"
        return ${EX_TEMPFAIL}
        ;;
      *)
        err_msg "Oracle OC4J instance '${OC4JINSTANCE}' does not exist!"
        return ${EX_USAGE}
        ;;
  esac
  #  Request-Port fuer bestimmen:
  #+   Zeile mit 'request=' aus $ORACLE_HOME/opmn/conf/opmn.xml entnehmen und mit sed die Portnummer filtern:
  #+ <port local="6102" remote="6202" request="6002"/>
  ORACLE_REQUEST_PORT=$(${VOLATILE_GREP} request "${ORACLE_HOME}/opmn/conf/opmn.xml" | ${VOLATILE_SED} -e "s+\(.*request=\"\)\([0-9]*\)\(\".*\)+\2+")
  if [[ ${ORACLE_REQUEST_PORT:-0} -ge 1024 && ${ORACLE_REQUEST_PORT:-0} -lt 65536 ]]; then  
    local CMD="${VOLATILE_JAVA} -jar ${ORACLE_HOME}/j2ee/home/admin_client.jar deployer:oc4j:opmn://$(hostname):${ORACLE_REQUEST_PORT}/${OC4JINSTANCE} ${OC4J_ADMIN_USER} ${OC4J_ADMIN_PWD} -undeploy ${APPLNAME} "
    debug_msg "${CMD}"              
    eval ${CMD}
    ret_val=$?
    if [[ ${ret_val} -gt 0 ]]; then
     err_msg "Failed to undeploy ${APPLNAME}"
    fi
  else
    err_msg "Unable to determine OPMN request port from '${ORACLE_HOME}/opmn/conf/opmn.xml'."
    ret_val=${EX_NOINPUT}
  fi  
  return ${ret_val}
}



####################
# Application in OC4J deployen
# 1: Pfad zum Oracle_Home
# 2: Name des OC4J-Container
# 3: Name der Applications
# 4: Benutzername des OC4J-ADMINS, default:  oc4jadmin
# 5: Passwort des OC4J-ADMINS, default: oracle10
# 6: Die einzuspielente Datei
# 7: Root-Url der Application
# 8: Web-Applicationname, default: identisch zu 3:

f_deploy_oc4j() {
  local ORACLE_HOME="${1}"
  local OC4JINSTANCE="${2}"
  local APPLNAME="${3}"
  local OC4J_ADMIN_USER="${4:-oc4jadmin}"
  local OC4J_ADMIN_PWD="${5:-oracle10}"
  local FILE="${6}"
  local APPLROOT="${7}"
  local WEBAPPNAME="${8}"
  local ret_val=0

  echo "* Deploy application '${APPLNAME}' in instance '${OC4JINSTANCE}'"
  
  if [[ "x" == "x${ORACLE_HOME}" ]]; then
    echo "    + \$ORACLE_HOME is not set - skipping"
    return ${EX_USAGE}
  fi
  if [[ ! -d "${ORACLE_HOME}/opmn" ]]; then
    echo "    + \$ORACLE_HOME does not point to an Application Server - skipping"
    return ${EX_USAGE}
  fi 
  
  if [[ ! -f "${ORACLE_HOME}/j2ee/home/admin_client.jar" ]]; then
    err_msg "Cannot find '${ORACLE_HOME}/j2ee/home/admin_client.jar'"
    return ${EX_OSFILE}
  fi
  
  if [[ ! -f ${FILE} ]]; then
    err_msg "Cannot find file '${FILE}' fo deploy"
    return ${EX_OSFILE}
  fi
  export ORACLE_HOME=${ORACLE_HOME}
  VOLATILE_JAVA=$(get_full_path_to_executable java)
  
  OC4J_STATUS=$(f_check_oc4j_container_status "${OC4JINSTANCE}")
  case ${OC4J_STATUS} in
    Alive)
      debug_msg "Oracle OC4J instance '${OC4JINSTANCE}' is running"
      ;;
     Down|Init)
        err_msg "Oracle OC4J instance '${OC4JINSTANCE}' is not running. Start instance and try again!"
        return ${EX_TEMPFAIL}
        ;;
      *)
        err_msg "Oracle OC4J instance '${OC4JINSTANCE}' does not exist!"
        return ${EX_USAGE}
        ;;
  esac
    
  #  Request-Port fuer bestimmen:
  #+   Zeile mit 'request=' aus $ORACLE_HOME/opmn/conf/opmn.xml entnehmen und mit sed die Portnummer filtern:
  #+ <port local="6102" remote="6202" request="6002"/>
  ORACLE_REQUEST_PORT=$(${VOLATILE_GREP} request "${ORACLE_HOME}/opmn/conf/opmn.xml" | ${VOLATILE_SED} -e "s+\(.*request=\"\)\([0-9]*\)\(\".*\)+\2+")
  if [[ ${ORACLE_REQUEST_PORT:-0} -ge 1024 && ${ORACLE_REQUEST_PORT:-0} -lt 65536 ]]; then  
    local CMD="${VOLATILE_JAVA} -jar ${ORACLE_HOME}/j2ee/home/admin_client.jar deployer:oc4j:opmn://$(hostname):${ORACLE_REQUEST_PORT}/${OC4JINSTANCE} ${OC4J_ADMIN_USER} ${OC4J_ADMIN_PWD} -deploy -file ${FILE} -deploymentName ${APPLNAME} -bindAllWebApps -contextRoot ${APPLROOT}"
    debug_msg "${CMD}" 
    eval ${CMD}  
    ret_val=$?

   #JPS kompilieren
   if [[ ${ret_val} -gt 0 ]]; then
     err_msg "Failed to deploy ${APPLNAME}"
     return ${ret_val}
   else
     SRC=${ORACLE_HOME}/j2ee/${OC4JINSTANCE}/applications/$APPLNAME/${WEBAPPNAME}
     DEST=${ORACLE_HOME}/j2ee/$OC4JINSTANCE/application-deployments/$APPLNAME/${WEBAPPNAME}/persistence/_pages
     cd $SRC
     echo "searching jsp files ..."
     JSPS=`find . -name *.jsp`
     if [[ -n "${JSPS}" ]]; then
      if [[ ! -d $DEST ]]; then
        echo "creating destination directory $DEST"
        mkdir -p $DEST
     fi
       echo "compiling jsp files ..."
       local STR_OPTIONS=""
       if  [[ ${DEBUG:-0} -gt 0 ]]; then
         STR_OPTIONS="-verbose"
       fi
       ${ORACLE_HOME}/bin/ojspc "${STR_OPTIONS}" -d "$DEST" $JSPS
       ret_val=$?
     fi  
   fi
  else
    err_msg "Unable to determine OPMN request port from '${ORACLE_HOME}/opmn/conf/opmn.xml'."
    ret_val=${EX_NOINPUT}
  fi

  return ${ret_val}
}


####################
# Application in OC4J stoppen
# 1: Pfad zum Oracle_Home
# 2: Name des OC4J-Container
# 3: Name der Applications
# 4: Benutzername des OC4J-ADMINS, default:  oc4jadmin
# 5: Passwort des OC4J-ADMINS, default: oracle10
f_stop_oc4j () {
  
  local ORACLE_HOME="${1}"
  local OC4JINSTANCE="${2}"
  local APPLNAME="${3}"
  local OC4J_ADMIN_USER="${4:-oc4jadmin}"
  local OC4J_ADMIN_PWD="${5:-oracle10}"
  local ret_val=0
    
  echo "* Stopping application '${APPLNAME}' in instance '${OC4JINSTANCE}'"
  
  if [[ "x" == "x${ORACLE_HOME}" ]]; then
    echo "    + \$ORACLE_HOME is not set - skipping"
    return ${EX_USAGE}
  fi
  if [[ ! -d "${ORACLE_HOME}/opmn" ]]; then
    echo "    + \$ORACLE_HOME does not point to an Application Server - skipping"
    return ${EX_USAGE}
  fi
  
  if [[ ! -f ${ORACLE_HOME}/j2ee/home/admin_client.jar ]]; then
    err_msg "Cannot find '${ORACLE_HOME}/j2ee/home/admin_client.jar'"
    return ${EX_OSFILE}
  fi
  
  VOLATILE_JAVA=$(get_full_path_to_executable java)
    
  OC4J_STATUS=$(f_check_oc4j_container_status "${OC4JINSTANCE}")
  case ${OC4J_STATUS} in
    Alive)
      debug_msg "Oracle OC4J instance '${OC4JINSTANCE}' is running"
      ;;
     Down|Init)
        err_msg "Oracle OC4J instance '${OC4JINSTANCE}' is not running. Start instance and try again!"
        return ${EX_TEMPFAIL}
        ;;
      *)
        err_msg "Oracle OC4J instance '${OC4JINSTANCE}' does not exist!"
        return ${EX_USAGE}
        ;;
  esac
  #  Request-Port fuer bestimmen:
  #+   Zeile mit 'request=' aus $ORACLE_HOME/opmn/conf/opmn.xml entnehmen und mit sed die Portnummer filtern:
  #+ <port local="6102" remote="6202" request="6002"/>
  ORACLE_REQUEST_PORT=$(${VOLATILE_GREP} request "${ORACLE_HOME}/opmn/conf/opmn.xml" | ${VOLATILE_SED} -e "s+\(.*request=\"\)\([0-9]*\)\(\".*\)+\2+")
  if [[ ${ORACLE_REQUEST_PORT:-0} -ge 1024 && ${ORACLE_REQUEST_PORT:-0} -lt 65536 ]]; then
    local CMD="${VOLATILE_JAVA} -jar ${ORACLE_HOME}/j2ee/home/admin_client.jar deployer:oc4j:opmn://$(hostname):${ORACLE_REQUEST_PORT}/${OC4JINSTANCE} ${OC4J_ADMIN_USER} ${OC4J_ADMIN_PWD} -stop ${APPLNAME}"
    debug_msg "${CMD}"           
    eval ${CMD}
    ret_val=$?
  
    if [[ ${ret_val} -gt 0 ]]; then
     err_msg "Failed to stopp ${APPLNAME}"
    fi
  else
    err_msg "Unable to determine OPMN request port from '${ORACLE_HOME}/opmn/conf/opmn.xml'."
    ret_val=${EX_NOINPUT}
  fi
    
  return ${ret_val}
}

###################
# Application in OC4J starten
# 1: Pfad zum Oracle_Home
# 2: Name des OC4J-Container
# 3: Name der Applications
# 4: Benutzername des OC4J-ADMINS, default:  oc4jadmin
# 5: Passwort des OC4J-ADMINS, default: oracle10
f_start_oc4j () {
  
  local ORACLE_HOME="${1}"
  local OC4JINSTANCE="${2}"
  local APPLNAME="${3}"
  local OC4J_ADMIN_USER="${4:-oc4jadmin}"
  local OC4J_ADMIN_PWD="${5:-oracle10}"
  local ret_val=0
    
  echo "* Starting application '${APPLNAME}' in instance '${OC4JINSTANCE}'"
  
  if [[ "x" == "x${ORACLE_HOME}" ]]; then
    echo "    + \$ORACLE_HOME is not set - skipping"
    return ${EX_USAGE}
  fi
  if [[ ! -d "${ORACLE_HOME}/opmn" ]]; then
    echo "    + \$ORACLE_HOME does not point to an Application Server - skipping"
    return ${EX_USAGE}
  fi
  
  if [[ ! -f ${ORACLE_HOME}/j2ee/home/admin_client.jar ]]; then
    err_msg "Cannot find '${ORACLE_HOME}/j2ee/home/admin_client.jar'"
    return ${EX_OSFILE}
  fi
  
  VOLATILE_JAVA=$(get_full_path_to_executable java)
  
  OC4J_STATUS=$(f_check_oc4j_container_status "${OC4JINSTANCE}")
  case ${OC4J_STATUS} in
    Alive)
      debug_msg "Oracle OC4J instance '${OC4JINSTANCE}' is running"
      ;;
     Down|Init)
        err_msg "Oracle OC4J instance '${OC4JINSTANCE}' is not running. Start instance and try again!"
        return ${EX_TEMPFAIL}
        ;;
      *)
        err_msg "Oracle OC4J instance '${OC4JINSTANCE}' does not exist!"
        return ${EX_USAGE}
        ;;
  esac
  
  #  Request-Port fuer bestimmen:
  #+   Zeile mit 'request=' aus $ORACLE_HOME/opmn/conf/opmn.xml entnehmen und mit sed die Portnummer filtern:
  #+ <port local="6102" remote="6202" request="6002"/>
  ORACLE_REQUEST_PORT=$(${VOLATILE_GREP} request "${ORACLE_HOME}/opmn/conf/opmn.xml" | ${VOLATILE_SED} -e "s+\(.*request=\"\)\([0-9]*\)\(\".*\)+\2+")
  if [[ ${ORACLE_REQUEST_PORT:-0} -ge 1024 && ${ORACLE_REQUEST_PORT:-0} -lt 65536 ]]; then
    local CMD="${VOLATILE_JAVA} -jar ${ORACLE_HOME}/j2ee/home/admin_client.jar deployer:oc4j:opmn://$(hostname):${ORACLE_REQUEST_PORT}/${OC4JINSTANCE} ${OC4J_ADMIN_USER} ${OC4J_ADMIN_PWD} -start ${APPLNAME}"
     debug_msg "${CMD}"            
     eval ${CMD}
     ret_val=$?
     if [[ ${ret_val} -gt 0 ]]; then
       err_msg "Failed to start ${APPLNAME}"
     fi
  else
     err_msg "Unable to determine OPMN request port from '${ORACLE_HOME}/opmn/conf/opmn.xml'."
     ret_val=${EX_NOINPUT}
  fi
  return ${ret_val}
}
