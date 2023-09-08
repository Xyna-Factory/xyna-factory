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

parse_commandline_arguments () {
  VERBOSE="true"
  DEBUG="false"

  while getopts ":nhHfFqd:" OPTION
  do
    if [[ "x${OPTARG:0:1}" == "x-" ]]; then DISPLAY_USAGE="true"; fi
    case ${OPTION} in
      n) DRY_RUN="true";;
      f) BOOL_EXIT="true";;
      F) BOOL_KILL="true";;
      q) VERBOSE="false";;
      d) DEBUG="true"; DEBUG_SERVER_PORT="${OPTARG}";;
      h|H) DISPLAY_USAGE="true";;
    esac
  done

  if [[ "${VERBOSE}" == "false" ]]; then
    QUIET_OPTION="-q"
  fi

  if [[ "${DEBUG}" == "true"  ]]; then
    if [[ "${DEBUG_SERVER_PORT}" -le 1024 || "${DEBUG_SERVER_PORT}" -ge 65536 ]]; then
      DISPLAY_USAGE="true"
    fi
  fi

  if [[ "${BOOL_EXIT:-false}" == "true" && "${BOOL_KILL:-false}" == "true" ]]; then
    DISPLAY_USAGE="true"
  fi
}

#  print all variables for debugging, then exit
debug_variables () {
  echo "factory cli port : ${FACTORY_CLI_PORT}"
  echo "verbose          : ${VERBOSE:-true}"
  echo "debug            : ${DEBUG:-false}"
  echo "debug server port: ${DEBUG_SERVER_PORT}"
  echo "stop with exit   : ${BOOL_EXIT:-false}"
  echo "stop with kill   : ${BOOL_KILL:-false}"
  echo
  echo "cli parameters propagated to the Xyna Factory Server:"
  echo "$@"
}


f_read_properties () {
  #  bugz 11003
  OLD_VERBOSE="${VERBOSE}"
  VERBOSE="false"
    f_read_product_properties_fast
  #  bugz 11003
  VERBOSE="${OLD_VERBOSE}"
}

display_usage () {
  ${VOLATILE_CAT} << A_HERE_DOCUMENT

  usage: "$(basename "$0")" -f -F -d port -nq command [options]

-n    dry-run, display the evaluated commandline parameters
-q    suppress output
-d    open port for debugging ]1024, ... , 65536[
-f    forces stopping of the factory              (conflicts with -F)
-F    forces stopping of the factory even harder  (conflicts with -f)

Useful commands are:
  start, stop, restart, help

Create, join or leave an XSOR-based cluster:
  "$(basename "$0")" create_cluster
  "$(basename "$0")" join_cluster
  "$(basename "$0")" remove_cluster

Create, join or leave an Oracle-based cluster:
  "$(basename "$0")" create_cluster_using_oracle
  "$(basename "$0")" join_cluster_using_oracle
  "$(basename "$0")" remove_cluster_using_oracle

Examples:

(1) "$(basename "$0")" help
      show available commands including usage description
      CAUTION: Use some pager to filter the lengthy output.

(2) "$(basename "$0")" -q start
      start ${XYNA_INSTANCENAME} and hide output.

(3) "$(basename "$0")" -d 4000 -n start
      set debug port, but perform dry-run only

A_HERE_DOCUMENT
}

f_do_xyna_cluster_using_oracle () {
  local CLUSTER_COMMAND="${1:-missing}"
  local CLUSTER_SETUP_OR_JOIN
  local CLUSTER_INIT_PARAMETERS
  local CLUSTER_RMI_PARAMETERS
  local INT_ALLOWED_DB_ENTRIES

  case "${CLUSTER_COMMAND}" in
    "create")
      CLUSTER_SETUP_OR_JOIN="setupnewcluster"
      CLUSTER_INIT_PARAMETERS="-initParameters"
      CLUSTER_RMI_PARAMETERS="${CLUSTER_LOCAL_RMI_PORT} ${CLUSTER_LOCAL_IPADDRESS}"
      INT_ALLOWED_DB_ENTRIES=0
      ;;
    "join")
      CLUSTER_SETUP_OR_JOIN="joincluster"
      CLUSTER_INIT_PARAMETERS="-connectionParameters"
      CLUSTER_RMI_PARAMETERS="${CLUSTER_REMOTE_IPADDRESS} ${CLUSTER_REMOTE_RMI_PORT} ${CLUSTER_LOCAL_IPADDRESS} ${CLUSTER_LOCAL_RMI_PORT}"
      INT_ALLOWED_DB_ENTRIES=1
      ;;
    *)
      err_msg "f_do_xyna_cluster_using_oracle: Cluster command '${CLUSTER_COMMAND}' not recognized."
      return 99
      ;;
  esac

  echo -e "\n* Setting up Xyna cluster"

  local PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  if [[ "$(check_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}.cluster")" != "install" ]]; then
    err_msg "Cluster setup can only be done once."
    echo
    echo "Hint: Maybe you want to use 'remove_cluster'..."
    return 99
  fi

  echo "    + checking Xyna Factory status"
  run_xynafactory_cmd -q -p "${FACTORY_CLI_PORT}" status
  local IST_STATUS=$?
  if [[ ${IST_STATUS:-${XYNA_FACTORY_IS_STARTING}} != ${XYNA_FACTORY_RUNS} ]]; then
    err_msg "f_do_xyna_cluster_using_oracle: Xyna factory must be running."
    return 99
  fi

  if [[ -n "${CLUSTER_REMOTE_IPADDRESS}" && "${CLUSTER_COMMAND}" == "join" ]]; then
    echo "    + checking Xyna Factory status on the remote machine"
    f_check_open_port "${CLUSTER_REMOTE_IPADDRESS}" "${CLUSTER_REMOTE_RMI_PORT}"
    if [[ $? -ne 0 ]]; then
      err_msg "f_do_xyna_cluster: Xyna factory on the remote machine needs to be running."
      return 99
    fi
  fi

  echo "    + checking no. of entries in table XYNACLUSTERSETUP"
  local INT_FOUND_DB_ENTRIES="-1"
  #  Existiert die Tabelle XYNACLUSTERSETUP ueberhaupt?
  #
  echo -e "SELECT TABLE_NAME FROM USER_TABLES;\nQUIT;\n" > "${TMP_FILE}.sql"
  local INT_TABLE_COUNT=$(f_run_sqlplus_from_file "${CLUSTER_DB_USER}" "${CLUSTER_DB_PASSWORD}" "${CLUSTER_DB_JDBC_URL#*@}" "${TMP_FILE}.sql" | ${VOLATILE_GREP} -ci XYNACLUSTERSETUP)
  case ${INT_TABLE_COUNT} in
    0) #  Tabelle existiert nicht. Sie wird durch 'create cluster' angelegt.
       INT_FOUND_DB_ENTRIES=0
       ;;
    1) #  In der Tabelle XYNACLUSTERSETUP die Anzahl der Datensaetze bestimmen
       #+
       #+   0 = Kein Clustersetup durchgefuehrt
       #+   1 = Auf dem ersten Knoten wurde 'create cluster' durchgefuehrt
       #+   2 = Auf dem zweiten Knoten wurde 'join cluster' durchgefuehrt
       #
       echo -e "SELECT COUNT(*) FROM XYNACLUSTERSETUP;\nQUIT;\n" > "${TMP_FILE}.sql"
       local TXT_SQLPLUS_OUTPUT=$(f_run_sqlplus_from_file "${CLUSTER_DB_USER}" "${CLUSTER_DB_PASSWORD}" "${CLUSTER_DB_JDBC_URL#*@}" "${TMP_FILE}.sql")
       INT_FOUND_DB_ENTRIES=$(echo "${TXT_SQLPLUS_OUTPUT}" | ${VOLATILE_AWK} 'NR == 4 {print $1}')
       ;;
    *) ;;
  esac
  ${VOLATILE_RM} -f "${TMP_FILE}.sql"
  
  if [[ ${INT_ALLOWED_DB_ENTRIES} -ne ${INT_FOUND_DB_ENTRIES} ]]; then
    err_msg "f_do_xyna_cluster: Found ${INT_FOUND_DB_ENTRIES} entries in table XYNACLUSTERSETUP, but for '${CLUSTER_COMMAND}' are only ${INT_ALLOWED_DB_ENTRIES} entries allowed"
    echo
    echo "Hint: Repair the entries in table XYNACLUSTERSETUP"
    return 99
  fi

  echo "    + saving persistence layer configuration"
  TARGET_FILE="${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml"
  if [[ -f "${TARGET_FILE}" ]]; then
    ${VOLATILE_CP} -p "${TARGET_FILE}" "${TARGET_FILE}_before_cluster_setup"
  else
    err_msg "File not found: ${TARGET_FILE}"
  fi
  INDENTATION="        "

  echo "    + add connection pool"
  f_xynafactory addconnectionpool -name OracleCluster_HISTORY -user "${CLUSTER_DB_USER}" -password "${CLUSTER_DB_PASSWORD}" \
                -connectstring "${CLUSTER_DB_JDBC_URL}" -type Oracle -size "${CLUSTER_DB_CONNECTIONPOOLSIZE_HISTORY}" \
                -retries 3 -pooltypespecifics socketTimeout=${CLUSTER_SOCKET_TIMEOUT_HISTORY} \
                                              connectTimeout=${CLUSTER_CONNECT_TIMEOUT_HISTORY}

  f_xynafactory addconnectionpool -name OracleCluster_DEFAULT -user "${CLUSTER_DB_USER}" -password "${CLUSTER_DB_PASSWORD}" \
                -connectstring "${CLUSTER_DB_JDBC_URL}" -type Oracle -size "${CLUSTER_DB_CONNECTIONPOOLSIZE_DEFAULT}" \
                -retries 3 -pooltypespecifics socketTimeout=${CLUSTER_SOCKET_TIMEOUT_DEFAULT} \
                                              connectTimeout=${CLUSTER_CONNECT_TIMEOUT_DEFAULT}

  echo "    + instantiate persistencelayers"
  f_xynafactory instantiatepersistencelayer -persistenceLayerInstanceName OracleCluster_HISTORY \
                -connectionType HISTORY -department xprc -persistenceLayerName oracle \
                -persistenceLayerSpecifics OracleCluster_HISTORY "${CLUSTER_DB_TIMEOUT_HISTORY}" "nameSuffix=history" "${CLUSTER_ADDITIONAL_OPTIONS}"
  
  f_xynafactory instantiatepersistencelayer -persistenceLayerInstanceName OracleCluster_DEFAULT \
                -connectionType DEFAULT -department xprc -persistenceLayerName oracle \
                -persistenceLayerSpecifics OracleCluster_DEFAULT "${CLUSTER_DB_TIMEOUT_DEFAULT}" "nameSuffix=default" "${CLUSTER_ADDITIONAL_OPTIONS}"
  
  echo "    + register tables with HISTORY layer"
  local TABLES="orderarchive cronlikeorders synchronizationentries xmomversion revision seriesinformation sessionmanagement oidNameMapping";
  for tablename in ${TABLES} ; do
    f_xynafactory registertable -tableName "${tablename}" -persistenceLayerInstanceName OracleCluster_HISTORY -c
  done
 
  echo "    + register tables with DEFAULT layer"
  local TABLES="capacities vetos idgeneration clusteringserviceslocks orderseriesmanagement orderbackup cronlikeorders miarchive synchronizationentries xmomversion revision seriesinformation sessionmanagement"
  for tablename in ${TABLES} ; do
    f_xynafactory registertable -tableName "${tablename}" -persistenceLayerInstanceName OracleCluster_DEFAULT -c
  done
  
  f_xynafactory set -key xyna.scheduler.orderbackupwaitingforscheduling -value true
  
  if [[ -z "${CLUSTER_REMOTE_IPADDRESS}" ]]; then
    attention_msg "f_do_xyna_cluster_using_oracle: No remote ip-address given. This is a \"single cluster\" instance now."
  else
    
    echo "    + register cluster"
    f_xynafactory registerclusterprovider -name OracleRACClusterProvider

    f_xynafactory "${CLUSTER_SETUP_OR_JOIN}" -clusterType OracleRACClusterProvider "${CLUSTER_INIT_PARAMETERS}" "${CLUSTER_DB_USER}" "${CLUSTER_DB_PASSWORD}" "${CLUSTER_DB_JDBC_URL}" 15000 -instanceDescription oraclecluster
    if [[ $? != 0 ]] ; then 
      return 99;
    fi;
    local ORACLE_ID=${XYNA_FACTORY_OUTPUT##Created new cluster instance with id = }
    if f_is_integer ${ORACLE_ID} ; then
      echo "    + id for clustertype 'OracleRACClusterProvider': ${ORACLE_ID}"
    else
      attention_msg "Failed to ${CLUSTER_SETUP_OR_JOIN} OracleRACClusterProvider : ${XYNA_FACTORY_OUTPUT}"
      return 99
    fi

    f_xynafactory "${CLUSTER_SETUP_OR_JOIN}" -clusterType RMIClusterProvider "${CLUSTER_INIT_PARAMETERS}" ${CLUSTER_RMI_PARAMETERS} -instanceDescription rmicluster
    if [[ $? != 0 ]] ; then 
      return 99;
    fi;
    local RMI_ID=${XYNA_FACTORY_OUTPUT##Created new cluster instance with id = }
    if f_is_integer ${RMI_ID} ; then
      echo "    + id for clustertype 'RMIClusterProvider': ${RMI_ID}"
    else
      attention_msg "Failed to ${CLUSTER_SETUP_OR_JOIN} RMIClusterProvider : ${XYNA_FACTORY_OUTPUT}"
      return 99
    fi


    echo "    + configure cluster components"
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${ORACLE_ID}" -component OraclePersistenceLayer_history
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${ORACLE_ID}" -component OraclePersistenceLayer_default

    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component Scheduler
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component 'Capacity Management'
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component ClusteredOrderArchive
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component ClusteredConfiguration
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component ClusteredCronLikeScheduler
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component ManualInteractionManagement
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component OrderAbortionManagement
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component OrderCancellationManagement
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component ClusteredApplicationManagement
    f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component 'Order Series Management'
  fi

  save_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}.cluster"
  
  ${INSTALL_PREFIX}/server/xynafactory.sh restart
}

f_remove_cluster_using_oracle () {
  echo -e "\n* Removing Xyna cluster"

  ${INSTALL_PREFIX}/server/xynafactory.sh stop

  if [[ -d "${INSTALL_PREFIX}/server/storage/defaultHISTORY" ]]; then
    ${VOLATILE_RM} -rf ${INSTALL_PREFIX}/server/storage/defaultHISTORY/cluster* ${INSTALL_PREFIX}/server/storage/defaultHISTORY/rmi* ${INSTALL_PREFIX}/server/storage/defaultHISTORY/oracle*
  fi

  attention_msg "Cluster data has to be removed from the database."

  ${VOLATILE_CAT} << A_HERE_DOCUMENT

(1) Connect to the database. The following information might be helpful:

Username: ${CLUSTER_DB_USER}
Password: ${CLUSTER_DB_PASSWORD}
Jdbc_Url: ${CLUSTER_DB_JDBC_URL}

With SQL*Plus the commandline probably is:

  sqlplus ${CLUSTER_DB_USER}/${CLUSTER_DB_PASSWORD}@${CLUSTER_DB_JDBC_URL}

(2) Run the following SQL-statements:

DELETE FROM XYNACLUSTERSETUP;
COMMIT;

(3) Remove cluster configuration from persistencelayerinstance.xml:

If there are no other changes, simply copy the backup file:

   cp ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml_before_cluster_setup \\
      ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml

If you expect other changes, then diff the files and edit manually:

   diff ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml_before_cluster_setup \\
        ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml
   vi ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml

(4) Start Xyna Factory:

${INSTALL_PREFIX}/server/xynafactory.sh start

A_HERE_DOCUMENT

  local PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  clear_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}.cluster"
}

f_do_xyna_cluster () {
  local CLUSTER_COMMAND="${1:-missing}"
  local CLUSTER_SETUP_OR_JOIN
  local CLUSTER_INIT_PARAMETERS

  case "${CLUSTER_COMMAND}" in
    "create")
      CLUSTER_SETUP_OR_JOIN="setupnewcluster"
      CLUSTER_INIT_PARAMETERS="-initParameters"
      ;;
    "join")
      CLUSTER_SETUP_OR_JOIN="joincluster"
      CLUSTER_INIT_PARAMETERS="-connectionParameters"
      ;;
    *)
      err_msg "f_do_xyna_cluster: Cluster command '${CLUSTER_COMMAND}' not recognized."
      return 99
      ;;
  esac

  f_configure_network_availability_demon

  echo -e "\n* Setting up Xyna cluster"

  local PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  if [[ "$(check_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}.cluster")" != "install" ]]; then
    err_msg "Cluster setup can only be done once."
    echo
    echo "Hint: Maybe you want to use 'remove_cluster'..."
    return 99
  fi

  echo "    + checking Xyna Factory status"
  run_xynafactory_cmd -q -p "${FACTORY_CLI_PORT}" status
  local IST_STATUS=$?
  if [[ ${IST_STATUS:-${XYNA_FACTORY_IS_STARTING}} != ${XYNA_FACTORY_RUNS} ]]; then
    err_msg "f_do_xyna_cluster: Xyna factory must be running."
    return 99
  fi

  #  Der Check ist bei 'create' sinnfrei, da auf dem Interconnect zu diesem Zeitpunkt noch nicht gelauscht wird.
  if [[ "${CLUSTER_COMMAND}" == "join" ]]; then
    echo "    + checking Xyna Factory status on the remote machine"
    f_check_open_port "${CLUSTER_INTERCONNECT_REMOTE_IPADDRESS}" "${CLUSTER_INTERCONNECT_REMOTE_PORT}"
    if [[ $? -ne 0 ]]; then
      err_msg "f_do_xyna_cluster: Could not connect to ${CLUSTER_INTERCONNECT_REMOTE_IPADDRESS} on port ${CLUSTER_INTERCONNECT_REMOTE_PORT}. Maybe Xyna factory on the remote machine is not running or not prepared for 'join cluster' or an active firewall is blocking."
      return 99
    fi
  fi

  echo "    + saving persistence layer configuration"
  TARGET_FILE="${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml"
  if [[ -f "${TARGET_FILE}" ]]; then
    ${VOLATILE_CP} -p "${TARGET_FILE}" "${TARGET_FILE}_before_cluster_setup"
  else
    err_msg "File not found: ${TARGET_FILE}"
  fi

  echo "    + register XSOR cluster provider"
  local CLUSTER_TYPE="XSORClusterProvider"
  if [[ $(${INSTALL_PREFIX}/server/xynafactory.sh listclustertypes | ${VOLATILE_GREP} -c "${CLUSTER_TYPE}") -eq 0 ]]; then
    run_xynafactory_cmd -q -p "${FACTORY_CLI_PORT}" registerclusterprovider -name "${CLUSTER_TYPE}"
  fi
  if [[ $(${INSTALL_PREFIX}/server/xynafactory.sh listclustertypes | ${VOLATILE_GREP} -c "${CLUSTER_TYPE}") -eq 0 ]]; then
    err_msg "Unable to register XSOR cluster provider - Abort!"
    return 99
  fi

  echo "    + register cluster"
  #                                              ---- node 1 ----    ---- node 2 ----
  # 1.  Interconnect Port Remote,                1712
  # 2.  Interconenct Port Local,                 1712
  # 3.  Interconnect Correction Queue Length,    100
  # 4.  Interconnect Hostname Remote,            192.168.180.111
  # 5.  ClusterManagement Hostname Local,        192.168.180.110
  # 6.  ClusterManagement Hostname Remote,       192.168.180.111
  # 7.  ClusterManagement Port Communication,    0
  # 8.  ClusterManagement Port Registry Local,   1103
  # 9.  ClusterManagement Port Registry Remote,  1103
  # 10. Persistence Max Batch Size,              100
  # 11. Persistence Interval (ms),               5000
  # 12. Synchronous Persistence (true/false),    false
  # 13. Node Id,                                 1                   2
  # 14. Node Preference (true/false),            true                false
  # 15. Delay of Availability Daemon (ms)        30000
  #
  ret_val=$(${INSTALL_PREFIX}/server/xynafactory.sh "${CLUSTER_SETUP_OR_JOIN}" -clusterType "${CLUSTER_TYPE}" "${CLUSTER_INIT_PARAMETERS}" "${CLUSTER_INTERCONNECT_REMOTE_PORT}" "${CLUSTER_INTERCONNECT_LOCAL_PORT}" "${CLUSTER_INTERCONNECT_CORRECTION_QUEUE_LENGTH}" "${CLUSTER_INTERCONNECT_REMOTE_IPADDRESS}" "${CLUSTER_MANAGEMENT_LOCAL_IPADDRESS}" "${CLUSTER_MANAGEMENT_REMOTE_IPADDRESS}" "${CLUSTER_MANAGEMENT_COMMUNICATION_PORT}" "${CLUSTER_MANAGEMENT_LOCAL_PORT}" "${CLUSTER_MANAGEMENT_REMOTE_PORT}" "${CLUSTER_PERSISTENCE_MAX_BATCH_SIZE}" "${CLUSTER_PERSISTENCE_INTERVAL}" "${CLUSTER_PERSISTENCE_SYNCHRONOUS}" "${CLUSTER_NODE_ID}" "${CLUSTER_NODE_PREFERENCE}" "${CLUSTER_AVAILABILITY_DEMON_DELAY}" -instanceDescription "${CLUSTER_INSTANCE_DESCRIPTION}" | ${VOLATILE_GREP} -v "got: ok" | ${VOLATILE_SED} -e "s+^got: ++")
  if [[ $(echo "${ret_val}" | ${VOLATILE_AWK} 'END {print NR}') -eq 1 ]]; then
    local CLUSTER_ID=$(echo "${ret_val}" | ${VOLATILE_AWK} '{print $NF}')
    echo "    + id for clustertype '${CLUSTER_TYPE}': ${CLUSTER_ID}"
  else
    echo "${ret_val}"
    return 99
  fi

  echo "    + deploying persistence layer"
  if [[ $(${INSTALL_PREFIX}/server/xynafactory.sh listpersistencelayers | ${VOLATILE_GREP} -c "com.gip.xyna.persistence.xsor.XynaClusterPersistenceLayer") -eq 0 ]]; then
    run_xynafactory_cmd -q -p "${FACTORY_CLI_PORT}" deploypersistencelayer -fqPersistenceLayerName com.gip.xyna.persistence.xsor.XynaClusterPersistenceLayer
  fi

  save_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}.cluster"

  ${INSTALL_PREFIX}/server/xynafactory.sh restart
}

f_remove_cluster () {
  echo -e "\n* Removing Xyna cluster"

  ${INSTALL_PREFIX}/server/xynafactory.sh stop
###########################################
##  FIXME: hier "das Richtige" eintragen ##
###########################################
  if [[ -d "${INSTALL_PREFIX}/server/storage/defaultHISTORY" ]]; then
    ${VOLATILE_RM} -rf ${INSTALL_PREFIX}/server/storage/defaultHISTORY/cluster* ${INSTALL_PREFIX}/server/storage/defaultHISTORY/rmi*
  fi

  attention_msg "Cluster data has to be removed from the database."

  ${VOLATILE_CAT} << A_HERE_DOCUMENT

(1) Connect to the database. The following information might be helpful:

Username: ${CLUSTER_DB_USER}
Password: ${CLUSTER_DB_PASSWORD}
Jdbc_Url: ${CLUSTER_DB_JDBC_URL}

With SQL*Plus the commandline probably is:

  sqlplus ${CLUSTER_DB_USER}/${CLUSTER_DB_PASSWORD}@${CLUSTER_DB_JDBC_URL}

(2) Run the following SQL-statements:

DELETE FROM XYNACLUSTERSETUP;
COMMIT;

(3) Remove cluster configuration from persistencelayerinstance.xml:

If there are no other changes, simply copy the backup file:

   cp ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml_before_cluster_setup \\
      ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml

If you expect other changes, then diff the files and edit manually:

   diff ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml_before_cluster_setup \\
        ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml
   vi ${INSTALL_PREFIX}/server/storage/persistence/persistencelayerinstance.xml

(4) Start Xyna Factory:

${INSTALL_PREFIX}/server/xynafactory.sh start

A_HERE_DOCUMENT

  local PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  clear_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}.cluster"
}

#  EOF
