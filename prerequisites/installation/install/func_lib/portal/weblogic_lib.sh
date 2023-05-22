
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



# Portal -> Oracle WebLogic library

#Extract information von ${DOMAIN_HOME}/init-info/tokenValue.properties
f_set_weblogic_domain_properties() {
  local ret_val=0
  local DOMAIN_HOME="${1}"
  local FILE_TO_PARSE="${DOMAIN_HOME}/init-info/tokenValue.properties"
  
  WLS_PASSWORD=""
  WLS_USERNAME=""
  WLS_SERVER_PORT=""
  WLS_ADMIN_SERVER_URL=""
  WLS_DOMAIN_NAME=""
  WLS_SSL_PORT=""
  WLS_SERVER_NAME=""
  
  if [[ "x" == "x${DOMAIN_HOME}" ]]; then
    err_msg "f_set_weblogic_domain_properties: \$DOMAIN_HOME is not set."
    return ${EX_USAGE}
  fi
    
  if [[ ! -d "${DOMAIN_HOME}/init-info" ]]; then
    err_msg "f_set_weblogic_domain_properties: \$DOMAIN_HOME does not point to an domain home"
    return ${EX_USAGE}
  fi
  
  local CUR_DIR="$(pwd)"
  f_load_library  setDomainEnv.sh  ${DOMAIN_HOME}/bin
  cd ${CUR_DIR}
  
  if [[ -f ${FILE_TO_PARSE} ]]; then
    WLS_SERVER_PORT=$( ${VOLATILE_SED} -ne 's/^@SERVER_PORT=\([0-9][0-9]*\)/\1/p' ${FILE_TO_PARSE})
    WLS_SSL_PORT=$( ${VOLATILE_SED} -ne 's/^@SSL_PORT=\([0-9][0-9]*\)/\1/p' ${FILE_TO_PARSE})
    WLS_SERVER_HOST=$( ${VOLATILE_SED} -ne 's/^@SERVER_HOST=\([A-Za-z0-9]*\)/\1/p' ${FILE_TO_PARSE})
    WLS_DOMAIN_NAME=$( ${VOLATILE_SED} -ne 's/^@DOMAIN_NAME=\([A-Za-z0-9]*\)/\1/p' ${FILE_TO_PARSE})
    WLS_SERVER_NAME=$( ${VOLATILE_SED} -ne 's/^@SERVER_NAME=\([A-Za-z0-9]*\)/\1/p' ${FILE_TO_PARSE})
    WLS_ADMIN_SERVER_URL="t3://localhost:${WLS_SERVER_PORT}"
  else
    err_msg "f_set_weblogic_domain_properties: Cannot find '${FILE_TO_PARSE}'"
    return ${EX_USAGE}
  fi
  
  return ${ret_val}
} 


# Manage WebLogic-Application 
f_weblogic_admin() {
  # see http://docs.oracle.com/cd/E13222_01/wls/docs81/admin_ref/admin_refTOC.html
  local DOMAIN_HOME="${1}"
  local ACTION="${2}"
  local EXTRA_OPTIONS="${3}"
  local ret_val=0
  
  f_set_weblogic_domain_properties ${DOMAIN_HOME}
  ret_val=$?
  if [[ ${ret_val} -gt 0 ]]; then
    err_msg "f_manage_weblogic_application: Error loading weblogic properties'"
    exit ${EX_USAGE}
  fi 
  
  local STR_EXE="${JAVA_HOME}/bin/java weblogic.Admin" 
  local STR_COMMON_OPTIONS="-adminurl ${WLS_ADMIN_SERVER_URL} "
  
  case ${ACTION} in
  
    CONNECT)     echo "Testing connection to' ${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} CONNECT ${EXTRA_OPTIONS:-1}
                 ret_val=$?
                 ;;
  
    PING)        echo "Sending ping to '${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} PING ${EXTRA_OPTIONS:-10}
                 ret_val=$?
                 ;;
                 
    FORCESHUTDOWN)
                 echo "Forced shuttdown of '${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} FORCESHUTDOWN
                 ret_val=$?
                 ;;
                 
    SHUTDOWN)
                 echo "Shuttdown of '${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} SHUTDOWN
                 ret_val=$?
                 ;;
                                             
    GETSTATE)
                 echo "Get state of '${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} GETSTATE
                 ret_val=$?
                 ;;
                              
    RESUME)
                 echo "Resuming '${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} RESUME
                 ret_val=$?
                 ;;                                 
    LIST)
                 echo "Listing of '${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} LIST
                 ret_val=$?
                 ;;
                                   
    SERVERLOG)
                 echo "Showing Server log of' ${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} SERVERLOG
                 ret_val=$?
                 ;;
                                   
    THREAD_DUMP)                                                                                              
                 echo "Prints a snapshot of threads  of '${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS}  THREAD_DUMP
                 ret_val=$?
                 ;;
                                                                                                                                                                                                                                       
         *)      echo "f_weblogic_admin: Unknown action '${ACTION}'" 
  esac             
  
  return ${ret_val}
}


# Manage WebLogic-Application 
f_weblogic_deployer() {
 
  local DOMAIN_HOME="${1}"
  local ACTION="${2}"
  local APPLICATION="${3}"   
  local APPLICATION_VERSION="${4}"   
  local APPLICATION_FILE="${5}" 
  local ret_val=0
  
  f_set_weblogic_domain_properties ${DOMAIN_HOME}
  ret_val=$?
  if [[ ${ret_val} -gt 0 ]]; then
    err_msg "f_manage_weblogic_application: Error loading weblogic properties'"
    exit ${EX_USAGE}
  fi 
  
  if [[ "x$(f_check_weblogic_connection ${DOMAIN_HOME})" != "x0" ]]; then
    echo  "Connecting to ${WLS_DOMAIN_NAME} (${WLS_ADMIN_SERVER_URL}) failed, possible the server is down!" >&2
    return ${EX_TEMPFAIL}
  fi 
  
  local STR_EXE="${JAVA_HOME}/bin/java weblogic.Deployer" 
  local STR_COMMON_OPTIONS="-adminurl ${WLS_ADMIN_SERVER_URL} "
  local STR_EXTRA_OPTIONS=""
  
  if [[ ${DEBUG:-0} -gt 0 ]]; then
    STR_COMMON_OPTIONS="${STR_COMMON_OPTIONS} -verbose"
  fi
  if [[ ${DEBUG:-0} -gt 1 ]]; then
    STR_COMMON_OPTIONS="${STR_COMMON_OPTIONS} -debug"
  fi
  
  if [[  -n "${APPLICATION_VERSION}" ]]; then
     STR_EXTRA_OPTIONS="${STR_EXTRA_OPTIONS} -appversion  ${APPLICATION_VERSION}"
  fi

  case ${ACTION} in
    DEPLOY)      echo "Deploying application '${APPLICATION}' in '${WLS_DOMAIN_NAME}@${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS}  -deploy -name ${APPLICATION} ${APPLICATION_FILE}
                 ret_val=$?
                 ;;
                 
    DISTRIBUTE)  echo "Distributing application '${APPLICATION}' in '${WLS_DOMAIN_NAME}@${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS}  -distribute -name ${APPLICATION} ${APPLICATION_FILE}
                 ret_val=$?
                 ;;
                 
    START)       echo "Starting application '${APPLICATION}' in '${WLS_DOMAIN_NAME}@${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} -start -name ${APPLICATION} ${STR_EXTRA_OPTIONS}
                 ret_val=$?
                 ;;
                 
    STOP)        echo "Stopping application '${APPLICATION}' in '${WLS_DOMAIN_NAME}@${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} -stop -name ${APPLICATION} ${STR_EXTRA_OPTIONS}
                 ret_val=$?
                 ;;
                  
    UNDEPLOY)    echo "Undeploying application '${APPLICATION}' in '${WLS_DOMAIN_NAME}@${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS}  -undeploy -name ${APPLICATION} ${STR_EXTRA_OPTIONS}
                 ret_val=$?
                 ;;
                          
    LISTAPPS)    echo "Listing application in '${WLS_DOMAIN_NAME}@${WLS_SERVER_NAME}'"
                 ${STR_EXE}  ${STR_COMMON_OPTIONS} -listapps
                 ret_val=$?
                 ;;                     
         *)      echo "f_weblogic_deployer: Unknown action '${ACTION}'" 
  esac             
  
  return ${ret_val}
}

#Ueberpeufe ob die Verbindung zum Weblogic-Server moeoglich ist
f_check_weblogic_connection() {
  
  local DOMAIN_HOME="${1}"
  local ret_val=0
  local SCRIPT_FILE="${TMP_FILE}_wls_status.py"
  f_set_weblogic_domain_properties ${DOMAIN_HOME}
  ret_val=$?
  if [[ ${ret_val} -gt 0 ]]; then
    err_msg "f_check_weblogic_connection: Error loading weblogic properties'"
    exit ${EX_USAGE}
  fi
  echo "import os" > ${SCRIPT_FILE}
  echo "if os.environ.has_key('wlsUserID'):" >> ${SCRIPT_FILE} 
  echo "    wlsUserID = os.environ['wlsUserID']" >> ${SCRIPT_FILE} 
  echo "if os.environ.has_key('wlsPassword'):" >> ${SCRIPT_FILE}
  echo "    wlsPassword = os.environ['wlsPassword']" >> ${SCRIPT_FILE} 
  echo "print '* ' " >> ${SCRIPT_FILE}
  echo "try:" >> ${SCRIPT_FILE}
  echo "  connect(${WLS_USERNAME} ${WLS_PASSWORD} url='${WLS_ADMIN_SERVER_URL}', adminServerName='${WLS_SERVER_NAME}')" >> ${SCRIPT_FILE} 
  echo "  disconnect()" >> ${SCRIPT_FILE}
  echo "except Exception, e:"  >> ${SCRIPT_FILE}
  echo "  dumpStack()" >> ${SCRIPT_FILE}
  echo "  exit(exitcode=1)" >> ${SCRIPT_FILE}
  echo "exit()" >> ${SCRIPT_FILE}  
  
  ${JAVA_HOME}/bin/java -classpath "${FMWCONFIG_CLASSPATH}" ${MEM_ARGS} ${JVM_D64} ${JAVA_OPTIONS} weblogic.WLST "${SCRIPT_FILE}" "${APPLICATION}" 1>/dev/null 2>/dev/null
  ret_val=$?
  
  echo $ret_val  
  
  return $ret_val
}


# Status WebLogic-Application pruefen
f_check_weblogic_application_status () {
 
  local DOMAIN_HOME="${1}"
  local APPLICATION="${2}" 
  local BLN_NICE_OUTPUT="${3:-false}"
  local ret_val=0
  local SCRIPT_FILE="${TMP_FILE}_wls_status.py"
  
  f_set_weblogic_domain_properties ${DOMAIN_HOME}
  ret_val=$?
  if [[ ${ret_val} -gt 0 ]]; then
    err_msg "f_check_weblogic_application_status: Error loading weblogic properties'"
    exit ${EX_USAGE}
  fi
  
  if [[ "x$(f_check_weblogic_connection ${DOMAIN_HOME})" != "x0" ]]; then
    echo  "Connecting to ${WLS_DOMAIN_NAME} (${WLS_ADMIN_SERVER_URL}) failed, possible the server is down!" >&2
    return ${EX_TEMPFAIL}
  fi 
  
  echo "import os" > ${SCRIPT_FILE}
  echo "if os.environ.has_key('wlsUserID'):" >> ${SCRIPT_FILE} 
  echo "    wlsUserID = os.environ['wlsUserID']" >> ${SCRIPT_FILE} 
  echo "if os.environ.has_key('wlsPassword'):" >> ${SCRIPT_FILE}
  echo "    wlsPassword = os.environ['wlsPassword']" >> ${SCRIPT_FILE} 
  if [[ "x${BLN_NICE_OUTPUT}" == "xtrue" ]]; then
    echo "print '* ' " >> ${SCRIPT_FILE}
    echo "print '* Connecting to ${WLS_ADMIN_SERVER_URL}'" >> ${SCRIPT_FILE}
  fi
  echo "try:" >> ${SCRIPT_FILE}
  echo "  connect(${WLS_USERNAME} ${WLS_PASSWORD} url='${WLS_ADMIN_SERVER_URL}', adminServerName='${WLS_SERVER_NAME}')" >> ${SCRIPT_FILE} 
  echo "  domainConfig()" >> ${SCRIPT_FILE}
  echo "  apps=cmo.getAppDeployments()" >> ${SCRIPT_FILE}
  if [[ "x${BLN_NICE_OUTPUT}" == "xtrue" ]]; then
    echo "  print '* ' " >> ${SCRIPT_FILE}
    if [[ -n "${APPLICATION}" ]]; then
      echo "  print '* Get Status for application ${APPLICATION} in ${WLS_DOMAIN_NAME}@${WLS_SERVER_NAME}'" >> ${SCRIPT_FILE} 
    else
      echo "  print '* Get Status for all applications in ${WLS_DOMAIN_NAME}@${WLS_SERVER_NAME}'" >> ${SCRIPT_FILE} 
    fi
    echo "  print '* ================================================'" >> ${SCRIPT_FILE}
  fi
  echo "  for i in apps:" >> ${SCRIPT_FILE}
  echo "    pureAppName=(i.getName()).split('#')[0]" >> ${SCRIPT_FILE}
  echo "    currAppName=i.getName()" >> ${SCRIPT_FILE}
  if [[ -n "${APPLICATION}" ]]; then
      echo "    if pureAppName == '${APPLICATION}' :" >> ${SCRIPT_FILE}
  else
      echo "    if currAppName :" >> ${SCRIPT_FILE}
  fi        
  echo "      navPath1=getMBean('domainConfig:/AppDeployments/'+currAppName)" >> ${SCRIPT_FILE}
  echo "      appID=navPath1.getApplicationIdentifier()" >> ${SCRIPT_FILE}
  echo "      navPath=getMBean('domainRuntime:/AppRuntimeStateRuntime/AppRuntimeStateRuntime')" >> ${SCRIPT_FILE}
  echo "      sts=navPath.getCurrentState(appID,'${WLS_SERVER_NAME}')" >> ${SCRIPT_FILE}
  echo "      version=i.getVersionIdentifier()" >> ${SCRIPT_FILE}
  if [[ "x${BLN_NICE_OUTPUT}" == "xtrue" ]]; then
    echo '      print "* Application: %-12s\tVersion: %-18s\tStatus: %s" % (pureAppName, version, sts)'  >> ${SCRIPT_FILE}
  else
    echo '      print "* :%s:%s:%s" % (pureAppName,version,sts)'  >> ${SCRIPT_FILE}
  fi
  if [[ "x${BLN_NICE_OUTPUT}" == "xtrue" ]]; then
    echo "  print '* ' " >> ${SCRIPT_FILE}
  fi    
  echo "  disconnect()" >> ${SCRIPT_FILE}
  echo "except Exception, e:"  >> ${SCRIPT_FILE}
  echo "  print >> sys.stderr, '* Error getting application status for ${WLS_DOMAIN_NAME}@${WLS_SERVER_NAME}'"  >> ${SCRIPT_FILE}
  echo "  dumpStack()" >> ${SCRIPT_FILE}
  echo "  exit(exitcode=1)" >> ${SCRIPT_FILE}
  echo "exit()" >> ${SCRIPT_FILE}  
  #Wenn nicht im Debug-Modus, dann unterdruecke "gespraechige" Ausgaben von WLST auf STDOUT
  if [[ ${DEBUG:-0} -eq 0 ]]; then
    ${JAVA_HOME}/bin/java -classpath "${FMWCONFIG_CLASSPATH}" ${MEM_ARGS} ${JVM_D64} ${JAVA_OPTIONS} weblogic.WLST "${SCRIPT_FILE}" "${APPLICATION}" > ${TMP_FILE} 
    ret_val=$? 
    ${VOLATILE_GREP} "* "  ${TMP_FILE}
  else
    ${JAVA_HOME}/bin/java -classpath "${FMWCONFIG_CLASSPATH}" ${MEM_ARGS} ${JVM_D64} ${JAVA_OPTIONS} weblogic.WLST "${SCRIPT_FILE}" "${APPLICATION}"
    ret_val=$?
  fi  

  ${VOLATILE_RM} -f ${SCRIPT_FILE} 

  return ${ret_val}
}

#Delete old weblogic applications
f_clean_weblogic_applications() {
  local DOMAIN_HOME="${1}"
  local STR_APPLICATIONS="${2}"
  local INT_APPLICATIONS_REMAIN=${3:-2}
  local STR_EXTRA_OPTIONS="${4}"
  local BATCH_MODE="${5:-false}"
  local ret_val=0
  
   ${VOLATILE_RM} -f "${TMP_FILE}"
   f_check_weblogic_application_status "${DOMAIN_HOME}" "" "false" > ${TMP_FILE}_all
   ret_val=$((${ret_val} + $?))
   if [[ ${ret_val} -gt 0 ]]; then
     return $ret_val
   fi
   ${VOLATILE_SED} -e "s+* ++" ${TMP_FILE}_all | ${VOLATILE_GREP} -vw ":None:"  | ${VOLATILE_GREP} "STATE_PREPARED"> ${TMP_FILE}
   ${VOLATILE_RM} -f ${TMP_FILE}_all
 
   if [[ -z "${STR_APPLICATIONS}" ]]; then
     STR_APPLICATIONS=$(${VOLATILE_AWK} '{print $1}' ${TMP_FILE} | ${VOLATILE_SORT} -u) 
   fi
  for app in ${STR_APPLICATIONS}; do
    INT_NR_TO_DELETE=0
    INT_NR_VERSIONS=$( ${VOLATILE_GREP} -cw ${app} ${TMP_FILE} )
    if [[ ${INT_NR_VERSIONS} -gt ${INT_APPLICATIONS_REMAIN} ]]; then
      INT_NR_TO_DELETE=$((${INT_NR_VERSIONS} - ${INT_APPLICATIONS_REMAIN}))
    fi
    echo -e "Cleaning Application $app\t(Removing ${INT_NR_TO_DELETE} of ${INT_NR_VERSIONS} stopped versions)"
     if [[ ${INT_NR_TO_DELETE} -gt 0 ]]; then
      local STR_VERSIONS_TO_DELETE="$( ${VOLATILE_GREP} -w ${app} ${TMP_FILE} |  ${VOLATILE_SORT} | ${VOLATILE_AWK} -F: -vende=$INT_NR_TO_DELETE 'NR<=ende {printf ("%s %s\n",$2, $3)}')"
      echo "Following application versions will be undeployed:"
      echo "${STR_VERSIONS_TO_DELETE}"
      if [[ "x${BATCH_MODE}" != "xtrue" ]]; then
        echo "Continue (yes/no) [no]?"
        read answer
        if [[ "x${answer}" != "xyes" ]]; then
         return ${EX_TEMPFAIL}
        fi
      fi  
        echo "${STR_VERSIONS_TO_DELETE}"  | while read zeile; do
        local STR_APP_NAME=$(echo ${zeile} | ${VOLATILE_AWK} '{print $1}')
        local STR_APP_VERSION=$(echo ${zeile} | ${VOLATILE_AWK} '{print $2}')
        f_weblogic_deployer "${DOMAIN_HOME}" "UNDEPLOY" "${STR_APP_NAME}" "${STR_APP_VERSION}" ""
        ret_val=$((${ret_val} + $?))
      done 
    fi
  done
  ${VOLATILE_RM} -f "${TMP_FILE}"
  return ${ret_val}
  
}