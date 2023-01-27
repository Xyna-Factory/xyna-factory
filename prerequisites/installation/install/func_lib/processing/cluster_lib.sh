
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



# Processing -> Cluster library

f_configure_network_availability_demon () {
  echo -e "\n* Configuring network availability demon"

  FILE_TO_EDIT="${INSTALL_PREFIX}/NetworkAvailability/networkAvailabilityDemonWrapper.sh"
  exit_if_not_exists "${FILE_TO_EDIT}"
  f_replace_in_file "${FILE_TO_EDIT}" "s+TOKEN_XYNA_USER+${XYNA_USER}+"

  local INITD_SCRIPT="/etc/init.d/network_availability_demon_${PRODUCT_INSTANCE}"
  if [[ -f ${INITD_SCRIPT} ]] ; then
    ${INITD_SCRIPT} stop
  else
    local TARGET_FILE="${INSTALL_PREFIX}/NetworkAvailability/networkAvailabilityDemon.pid"
    if [[ -f "${TARGET_FILE}" ]]; then
      echo "    + restarting network availability demon"
      local PID=$(${VOLATILE_CAT} "${TARGET_FILE}")
      echo "       * old pid: ${PID}"
      #${VOLATILE_PS} -f -p "${PID}"
      #echo
      kill -9 "${PID}"
    fi;
  fi
  sleep 1
  PID=$(${VOLATILE_CAT} "${INSTALL_PREFIX}/NetworkAvailability/networkAvailabilityDemon.pid")
  echo "       * new pid: ${PID}"

}

f_etc_initd_network_availability_demon_files () {
  #Anlegen des /etc/init.d/network_availability_demon_???-Skripts
  f_etc_initd_files "network_availability_demon" "application/network_availability_demon" \
      "s+TOKEN_INSTALL_PREFIX+${INSTALL_PREFIX}+" \
      "s+TOKEN_PROVIDES_NAD+network_availability_demon_${PRODUCT_INSTANCE}+" \
      "s+TOKEN_NAD_USER+${XYNA_USER}+" \
      "s+TOKEN_LOGGER+${VOLATILE_LOGGER}+" \
      "s+TOKEN_LOG_FACILITY+${XYNA_SYSLOG_FACILITY}+"
}

f_etc_respawn_network_availability_demon () {
  f_etc_respawn "network_availability_demon"
}



f_is_clustered () {
  local PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  if [[ "$(check_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}.cluster")" != "install" ]]; then
    return 0;
  else 
    return 1;
  fi
}

f_install_oracle_cluster () {
  echo -e "\n* Installing Oracle Cluster components"
  INDENTATION="        "
  COMPONENT_APPLICATIONS="false"; #verhindert das Installieren von Applications
  
  f_create_oracle_cluster_pool_persistence_layer
  f_create_oracle_cluster_register_tables
  
  f_xynafactory set -key xyna.scheduler.orderbackupwaitingforscheduling -value true #FIXME warum?
  
  echo "    + register cluster"
  f_xynafactory registerclusterprovider -name OracleRACClusterProvider
  if [[ $? != 0 ]] ; then
    err_msg "cluster installation failed...";
    exit 99;
  fi;

  CLUSTER_COMMAND="create";
  if [[ -n "${CLUSTER_REMOTE_IPADDRESS}" ]]; then
    echo "    + checking Xyna Factory status on the remote machine"
    f_check_open_port "${CLUSTER_REMOTE_IPADDRESS}" "${CLUSTER_REMOTE_RMI_PORT}"
    if [[ $? -ne 0 ]]; then
      CLUSTER_COMMAND="create";
    else
      CLUSTER_COMMAND="join";
    fi
    echo "      -> ${CLUSTER_COMMAND} cluster"
  fi

  local ORACLE_ID;
  f_install_cluster_type ${CLUSTER_COMMAND} OracleRACClusterProvider ORACLE_ID
  echo "    + id for clustertype 'OracleRACClusterProvider': ${ORACLE_ID}"
  
  local RMI_ID;
  f_install_cluster_type ${CLUSTER_COMMAND} RMIClusterProvider RMI_ID
  echo "    + id for clustertype 'RMIClusterProvider': ${RMI_ID}"
  
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
  f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component 'Time Constraint Management'
  f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component 'Veto Management'
  f_xynafactory configurecomponentforcluster -clusterInstanceId "${RMI_ID}" -component 'Factory Runtime Statistics'
  
  save_components_file "${PRODUCT_NAME}.${PRODUCT_INSTANCE}.cluster"
  
}

f_install_cluster_type () {
  local CLUSTER_TYPE=$2
  local CLUSTER_ID_NAME=$3; #Name der Variable, in der die Id abgelegt werden soll
  case "$1" in
    "create")
      local CLUSTER_SETUP_OR_JOIN="setupnewcluster"
      local COMMAND_PARAMETERS="-initParameters"
      local CLUSTER_PREFIX="Created new cluster instance with id = "
      ;;
    "join")
      local CLUSTER_SETUP_OR_JOIN="joincluster"
      local COMMAND_PARAMETERS="-connectionParameters"
      local CLUSTER_PREFIX="Joined cluster. Local cluster instance id = "
      ;;
    *)
      err_msg "install_cluster_type: Cluster command '${1}' not recognized."
      exit 99
      ;;
  esac
  local CLUSTER_PARAMETERS;
  case "${CLUSTER_TYPE}" in
    "OracleRACClusterProvider")
      CLUSTER_PARAMETERS="\"${CLUSTER_DB_USER}\" \"${CLUSTER_DB_PASSWORD}\" \"${CLUSTER_DB_JDBC_URL}\" 15000"
      CLUSTER_PARAMETERS="${CLUSTER_PARAMETERS} -instanceDescription oraclecluster"
      ;;
    "RMIClusterProvider")
      if [[ $1 == "create" ]] ; then 
        CLUSTER_PARAMETERS="${CLUSTER_LOCAL_RMI_PORT} ${CLUSTER_LOCAL_IPADDRESS}"
      else
        CLUSTER_PARAMETERS="${CLUSTER_REMOTE_IPADDRESS} ${CLUSTER_REMOTE_RMI_PORT} ${CLUSTER_LOCAL_IPADDRESS} ${CLUSTER_LOCAL_RMI_PORT}"
      fi
      CLUSTER_PARAMETERS="${CLUSTER_PARAMETERS} -instanceDescription rmicluster"
      ;;
    *) 
      err_msg "install_cluster_type: Cluster Type '${CLUSTER_TYPE}' not recognized."
      exit 99
      ;;
  esac

  f_xynafactory "${CLUSTER_SETUP_OR_JOIN}" -clusterType ${CLUSTER_TYPE} "${COMMAND_PARAMETERS}" ${CLUSTER_PARAMETERS}
  if [[ $? != 0 ]] ; then 
    exit 99;
  fi;
  local CLUSTER_ID=${XYNA_FACTORY_OUTPUT##${CLUSTER_PREFIX}}
  if f_is_integer ${CLUSTER_ID} ; then
   eval ${CLUSTER_ID_NAME}=${CLUSTER_ID};
  else
    attention_msg "Failed to ${CLUSTER_SETUP_OR_JOIN} ${CLUSTER_TYPE} : ${XYNA_FACTORY_OUTPUT}"
    exit 99
  fi
}

f_create_oracle_cluster_pool_persistence_layer () {
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
  
}

f_create_oracle_cluster_register_tables () {
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
}

