# --------------------------------------------------
#  Copyright GIP AG 2013
#  (http://www.gip.com)
#
#  Hechtsheimer Str. 35-37
#  55131 Mainz
# ---------------------------------------------------
#  $Revision: 143988 $
#  $Date: 2013-12-17 10:19:43 +0100 (Di, 17. Dez 2013) $
# ---------------------------------------------------

# Shell -> Properties to Variable Mapping library


#  Diese Variablen muessen gesetzt sein.
PRODUCT_NAME="black_edition"
#  Diese Property wird in der host-spezifischen Properties-Datei abgelegt
HOST_PROPERTIES="\
                 ant.folder black_edition.instances\
                 system.vendor\
                 system.type\
                 ntp1.ipAddress\
                 ntp2.ipAddress\
"
#  Diese Properties werden in der produkt-spezifischen Properties-Datei abgelegt
PRODUCT_GROUP_PROPERTIES="cluster geronimo sipadapter"
PRODUCT_PROPERTIES="\
                    as.userid\
                    as.password \
                    cluster.availability.demon.delay\
                    cluster.instance.description\
                    cluster.interconnect.correction.queue.length\
                    cluster.interconnect.local.port\
                    cluster.interconnect.remote.ipAddress\
                    cluster.interconnect.remote.port\
                    cluster.management.communication.port\
                    cluster.management.local.ipAddress\
                    cluster.management.local.port\
                    cluster.management.remote.ipAddress\
                    cluster.management.remote.port\
                    cluster.node.id\
                    cluster.node.preference\
                    cluster.oracle.db.additional.options\
                    cluster.oracle.db.connectionpoolsize.default\
                    cluster.oracle.db.connectionpoolsize.history\
                    cluster.oracle.db.connect.timeout.default\
                    cluster.oracle.db.connect.timeout.history\
                    cluster.oracle.db.jdbc.url\
                    cluster.oracle.db.password\
                    cluster.oracle.db.socket.timeout.default\
                    cluster.oracle.db.socket.timeout.history\
                    cluster.oracle.db.timeout.default\
                    cluster.oracle.db.timeout.history\
                    cluster.oracle.db.user\
                    cluster.persistence.interval\
                    cluster.persistence.max.batch.size\
                    cluster.persistence.synchronous\
                    cluster.rmi.local.ipAddress\
                    cluster.rmi.local.port\
                    cluster.rmi.remote.ipAddress\
                    cluster.rmi.remote.port\
                    default.monitoringlevel\
                    geronimo.deployer.folder\
                    geronimo.folder\
                    geronimo.http.port\
                    geronimo.ipAddress\
                    geronimo.jmx.port\
                    geronimo.password\
                    geronimo.rmi.port\
                    geronimo.ssl.port\
                    geronimo.syslog.file\
                    geronimo.syslog.facility\
                    geronimo.user\
                    installation.folder\
                    java.home\
                    jvm.maxheap.size\
                    jvm.minheap.size\
                    jvm.option.additional\
                    jvm.option.debug\
                    jvm.option.exception\
                    jvm.option.gc\
                    jvm.option.log4j\
                    jvm.option.profiling\
                    jvm.option.rmi\
                    jvm.option.xml.backup\
                    jvm.permgenspace.size\
                    os.locale\
                    pid.folder\
                    project.prefix.uppercase\
                    scheduler.stop.timeout.offset\
                    securestorage.seed\
                    sipadapter.high.port\
                    sipadapter.low.port\
                    sipadapter.notify.responsetimeout\
                    svn.hookmanager.port\
                    svn.server\
                    trigger.http.port\
                    trigger.nsnhix5600.port\
                    trigger.snmp.port\
                    velocity.parser.pool.size\
                    xyna.group\
                    xyna.instancename\
                    xyna.password\
                    xyna.rmi.local.ipAddress\
                    xyna.rmi.local.port\
                    xyna.syslog.file\
                    xyna.syslog.facility\
                    xyna.user\
"

#  Falls eine Eigenschaft keinen Wert hat, dann einen Default-Wert annehmen
#
#  Eingabeparameter:
#o   1 = Name der Eigenschaft
#    globale Variable ${PRODUCT_INSTANCE:-1}
default_value_for_property () {
  PROPERTY="${1}"
  if [[ -z "${PROPERTY}" ]]; then
    echo "Unable to work without a property name. Abort!"
    exit 95
  fi
  local PRODUCT_INSTANCE_STR=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  local PORT_OFFSET=$(( ($((10#${INSTANCE_NUMBER:-1})) - 1) * 10 ))
case ${PROPERTY} in
  ant.folder)                                   echo "/opt/ant";;
  as.password)                                  f_make_password;;
  as.userid)                                    echo "oc4jadmin";;
  black_edition.instances)                      echo "1";;
  cluster)                                      echo "false";;
  cluster.availability.demon.delay)             echo "30000";;
  cluster.instance.description)                 echo "";;
  cluster.interconnect.correction.queue.length) echo "100";;
  cluster.interconnect.local.port)              echo "$(( 1712 + ${PORT_OFFSET} ))";;
  cluster.interconnect.remote.ipAddress)        echo "";;
  cluster.interconnect.remote.port)             echo "$(( 1712 + ${PORT_OFFSET} ))";;
  cluster.management.communication.port)        echo "0";;
  cluster.management.local.ipAddress)           echo "";;
  cluster.management.local.port)                echo "$(( 1103 + ${PORT_OFFSET} ))";;
  cluster.management.remote.ipAddress)          echo "";;
  cluster.management.remote.port)               echo "$(( 1103 + ${PORT_OFFSET} ))";;
  cluster.node.id)                              echo "1";;
  cluster.node.preference)                      echo "true";;
  cluster.oracle.db.additional.options)         echo "zippedBlobs=true";;
  cluster.oracle.db.connectionpoolsize.default) echo "120";;
  cluster.oracle.db.connectionpoolsize.history) echo "40";;
  cluster.oracle.db.connect.timeout.default)    echo "15";;
  cluster.oracle.db.connect.timeout.history)    echo "15";;
  cluster.oracle.db.jdbc.url)                   echo "jdbc:oracle:thin:@//10.0.0.1000:1521/xynadb";;
  cluster.oracle.db.password)                   f_make_password;;
  cluster.oracle.db.socket.timeout.default)     echo "120";;
  cluster.oracle.db.socket.timeout.history)     echo "120";;
  cluster.oracle.db.timeout.default)            echo "60000";;
  cluster.oracle.db.timeout.history)            echo "60000";;
  cluster.oracle.db.user)                       echo "oracle";;
  cluster.persistence.max.batch.size)           echo "100";;
  cluster.persistence.interval)                 echo "5000";;
  cluster.persistence.synchronous)              echo "false";;
  cluster.rmi.local.ipAddress)                  echo "";;
  cluster.rmi.local.port)                       echo "$(( 1099 + ${PORT_OFFSET} ))";;
  cluster.rmi.remote.ipAddress)                 echo "";;
  cluster.rmi.remote.port)                      echo "$(( 1099 + ${PORT_OFFSET} ))";;
  default.monitoringlevel)                      echo "5";;
  geronimo)                                     echo "false";;
  geronimo.deployer.folder)                     echo "$(f_get_property_installation_folder)/geronimo-deployer";;
  geronimo.folder)                              echo "$(f_get_property_installation_folder)/geronimo";;
  geronimo.http.port)                           echo "$(( 8080 + ${PORT_OFFSET} ))";;
  geronimo.jmx.port)                            echo "$(( 9999 + ${PORT_OFFSET} ))";;
  geronimo.password)                            f_make_password;;
  geronimo.rmi.port)                            echo "$(( 1098 + ${PORT_OFFSET} ))";;
  geronimo.ssl.port)                            echo "$(( 8443 + ${PORT_OFFSET} ))";;
  geronimo.syslog.file)                         echo "/var/log/xyna/xyna_${PRODUCT_INSTANCE_STR}/geronimo.log";;
  geronimo.syslog.facility)                     echo "$(f_get_tomcat_log_facility)";;
  geronimo.user)                                echo "geronimo";;
  installation.folder)                          echo "/opt/xyna/xyna_${PRODUCT_INSTANCE_STR}";;
  java.home)                                    echo "${JAVA_HOME}";;
  jvm.maxheap.size)                             echo "${TOMCAT_MEMORY}m";;
  jvm.minheap.size)                             echo "${TOMCAT_MEMORY}m";;
  jvm.option.additional)                        echo "-Dxnwh.securestorage.seedfile=${XYNA_ENVIRONMENT_DIR}/black_edition_${PRODUCT_INSTANCE_STR}.properties";;
  jvm.option.debug)                             echo "";;
  jvm.option.exception)                         echo "-Dexceptions.storage=Exceptions.xml";;
  jvm.option.gc)                                echo "";;
  jvm.option.log4j)                             echo "-Dlog4j.configurationFile=log4j2.xml";;
  jvm.option.profiling)                         echo "";;
  jvm.option.rmi)                               echo "-Djava.security.policy=server.policy";;
  jvm.option.xml.backup)                        echo "-Dxnwh.persistence.xml.backup.enabled=false";;
  jvm.permgenspace.size)                        echo "${JAVA_PERMSIZE}m";;
  ntp1.ipAddress)                               echo "192.53.103.108";;    # ptbtime1.ptb.de
  ntp2.ipAddress)                               echo "192.53.103.104";;    # ptbtime2.ptb.de
  os.locale)                                    echo "";;
  pid.folder)                                   echo "$(f_get_property_installation_folder)/server";;
  project.prefix.uppercase)                     echo "MYPROJECT";;
  scheduler.stop.timeout.offset)                echo "20000";;
  securestorage.seed)                           echo "change me to some unique string";;
  sipadapter)                                   echo "false";;
  sipadapter.high.port)                         echo "$(( 4259 + ${PORT_OFFSET} ))";;
  sipadapter.low.port)                          echo "$(( 4250 + ${PORT_OFFSET} ))";;
  sipadapter.notify.responsetimeout)            echo "10000";;
  svn.hookmanager.port)                         echo "";;
  svn.server)                                   echo "";;
  system.vendor)                                echo "gip";;
  system.type)                                  echo "production";;
  trigger.http.port)                            echo "$(( 4245 + ${PORT_OFFSET} ))";;
  trigger.nsnhix5600.port)                      echo "$(( 162 + ${PORT_OFFSET} ))";;
  trigger.snmp.port)                            echo "$(( 5999 + ${PORT_OFFSET} ))";;
  velocity.parser.pool.size)                    echo "20";;
  xyna.group)                                   echo "xyna";;
  xyna.instancename)                            echo "Xyna Factory ${PRODUCT_INSTANCE_STR}";;
  xyna.password)                                f_make_password;;
  xyna.rmi.local.ipAddress)                     echo "";;
  xyna.rmi.local.port)                          echo "$(( 1099 + ${PORT_OFFSET} ))";;
  xyna.syslog.file)                             echo "/var/log/xyna/xyna_${PRODUCT_INSTANCE_STR}/xyna.log";;
  xyna.syslog.facility)                         echo "$(f_get_xyna_log_facility)";;
  xyna.user)                                    echo "xyna";;
esac
}

f_get_property_installation_folder() {
  local PROP_FILE=$(get_properties_filename "product" "${INSTANCE_NUMBER}")
  get_property installation.folder ${PROP_FILE}
  echo ${CURRENT_PROPERTY}
}

f_read_host_properties () {
  check_properties_file "host"
  local PROP_FILE=$(get_properties_filename "host");
  f_clear_error_buffer
  for i in ${HOST_PROPERTIES}; do
    get_property ${i} "${PROP_FILE}"
    case ${i} in
      ant.folder)              f_check_is_path "${i}";      ANT_HOME="${CURRENT_PROPERTY}";;
      black_edition.instances) f_check_is_integer "${i}";   BLACK_EDITION_INSTANCES="${CURRENT_PROPERTY}";;
      system.vendor)           f_check_is_not_empty "${i}"; PROP_SYSTEM_VENDOR=${CURRENT_PROPERTY};;
      system.type)                                          PROP_SYSTEM_TYPE=${CURRENT_PROPERTY};;
    esac
  done
}

f_read_product_group_properties() {
  PROP_FILE=$(get_properties_filename "product" "${INSTANCE_NUMBER}")
  for i in ${PRODUCT_GROUP_PROPERTIES} ; do
    get_property ${i} "${PROP_FILE}"
    case ${i} in
      cluster)                                          CLUSTER_SELECTED="${CURRENT_PROPERTY}";;
      geronimo)                                         GERONIMO_SELECTED="${CURRENT_PROPERTY}";;
      sipadapter)                                       SIPADAPTER_SELECTED="${CURRENT_PROPERTY}";;
    esac
  done
}

#Liest alle Properties aus der Liste ${PRODUCT_PROPERTIES}, und erzeugt dabei fehlende Properties 
#neu (durch Benutzerinteraktion) und speichert diese.
f_read_product_properties() { 
  #  Read product-specific properties
  check_properties_file "${PRODUCT_NAME}" "${INSTANCE_NUMBER}"
  PROP_FILE=$(get_properties_filename "product" "${INSTANCE_NUMBER}")
  for i in ${PRODUCT_PROPERTIES} ; do
    get_property ${i} "${PROP_FILE}"
    f_map_current_property ${i}
  done
}

#Liest die Properties aus der Product-Property-Datei. Fehlende Properties werden ignoriert. 
#Ablauf forkt dadurch wesentlich weniger!
f_read_product_properties_fast() { 
  #  Read product-specific properties
  check_properties_file "${PRODUCT_NAME}" "${INSTANCE_NUMBER}"
  PROP_FILE=$(get_properties_filename "product" "${INSTANCE_NUMBER}")
  local key;
  local value;
  while IFS='=' read -r key value ; do
    if [[ ${key:0:1} != '#' ]] ; then
      CURRENT_PROPERTY=${value}
      f_map_current_property ${key}
      #echo "read property ${key} : ${CURRENT_PROPERTY}"
    fi;
  done < "${PROP_FILE}"
}

f_map_current_property() {
  local i=${1}
  case ${i} in
    as.password)         f_check_is_password "${i}";   ORACLE_AS_PASSWORD="${CURRENT_PROPERTY}";;
    as.userid)           f_check_is_not_empty "${i}"; ORACLE_AS_USERID="${CURRENT_PROPERTY}";;
    default.monitoringlevel)                          DEFAULT_MONITORINGLEVEL="${CURRENT_PROPERTY}";;
    installation.folder)      f_check_is_path "${i}";          INSTALL_PREFIX="${CURRENT_PROPERTY}";;
    java.home)                                        JAVA_HOME="${CURRENT_PROPERTY}";;
    jvm.maxheap.size)                                 JVM_OPTIONS_MAXHEAP_SIZE="${CURRENT_PROPERTY}";;
    jvm.minheap.size)                                 JVM_OPTIONS_MINHEAP_SIZE="${CURRENT_PROPERTY}";;
    jvm.option.additional)                            ADDITIONAL_OPTIONS="${CURRENT_PROPERTY}";;
    jvm.option.debug)                                 DEBUG_OPTIONS="${CURRENT_PROPERTY}";;
    jvm.option.exception)                             EXCEPTION_OPTIONS="${CURRENT_PROPERTY}";;
    jvm.option.gc)                                    GC_OPTIONS="${CURRENT_PROPERTY}";;
    jvm.option.log4j)                                 LOG4J_OPTIONS="${CURRENT_PROPERTY}";;
    jvm.option.profiling)                             PROFILING_OPTIONS="${CURRENT_PROPERTY}";;
    jvm.option.xml.backup)                            XML_BACKUP_OPTIONS="${CURRENT_PROPERTY}";;
    jvm.option.rmi)                                   RMI_OPTIONS="${CURRENT_PROPERTY}";;
    ntp1.ipAddress)      f_check_is_ip_address "${i}"; NTP1_SERVER="${CURRENT_PROPERTY}";;
    ntp2.ipAddress)                                   NTP2_SERVER="${CURRENT_PROPERTY}";;
    os.locale)                                        OS_LOCALE="${CURRENT_PROPERTY}";;
    pid.folder)                                       PID_FOLDER="${CURRENT_PROPERTY}";;
    project.prefix.uppercase)                         PROJECT_PREFIX_UPPERCASE="${CURRENT_PROPERTY}";;
    scheduler.stop.timeout.offset)                    SCHEDULER_STOP_TIMEOUT_OFFSET="${CURRENT_PROPERTY}";;
    svn.hookmanager.port)                             SVN_HOOKMANAGER_PORT="${CURRENT_PROPERTY}";;
    svn.server)                                       SVN_SERVER="${CURRENT_PROPERTY}";;
    trigger.http.port)                                 HTTP_TRIGGER_PORT="${CURRENT_PROPERTY}";;
    trigger.nsnhix5600.port)                           SNMP_NSNHIX_PORT="${CURRENT_PROPERTY}";;
    trigger.snmp.port)                                 SNMP_TRIGGER_PORT="${CURRENT_PROPERTY}";;
    velocity.parser.pool.size)                         VELOCITY_PARSER_POOL_SIZE="${CURRENT_PROPERTY}";;
    xyna.group)     f_check_is_alpha_numeric "${i}";   XYNA_GROUP="${CURRENT_PROPERTY}";;
    xyna.instancename)                                 XYNA_INSTANCENAME="${CURRENT_PROPERTY}";;
    xyna.password)  f_check_is_password "${i}";        XYNA_PASSWORD="${CURRENT_PROPERTY}";;
    xyna.rmi.local.ipAddress)                          XYNA_RMI_IPADDRESS="${CURRENT_PROPERTY}";;
    xyna.rmi.local.port)                               XYNA_RMI_PORT="${CURRENT_PROPERTY}";;
    xyna.syslog.file) f_check_is_path "${i}";          XYNA_SYSLOG_FILE="${CURRENT_PROPERTY}";;
    xyna.syslog.facility)                              XYNA_SYSLOG_FACILITY="${CURRENT_PROPERTY}";;
    xyna.user)        f_check_is_alpha_numeric "${i}"; XYNA_USER="${CURRENT_PROPERTY}";;
  esac
  
  if f_selected ${CLUSTER_SELECTED} ; then
  case ${i} in
    cluster.availability.demon.delay)                 CLUSTER_AVAILABILITY_DEMON_DELAY="${CURRENT_PROPERTY}";;
    cluster.instance.description)                     CLUSTER_INSTANCE_DESCRIPTION="${CURRENT_PROPERTY}";;
    cluster.interconnect.correction.queue.length)     CLUSTER_INTERCONNECT_CORRECTION_QUEUE_LENGTH="${CURRENT_PROPERTY}";;
    cluster.interconnect.local.port)                  CLUSTER_INTERCONNECT_LOCAL_PORT="${CURRENT_PROPERTY}";;
    cluster.interconnect.remote.ipAddress)            CLUSTER_INTERCONNECT_REMOTE_IPADDRESS="${CURRENT_PROPERTY}";;
    cluster.interconnect.remote.port)                 CLUSTER_INTERCONNECT_REMOTE_PORT="${CURRENT_PROPERTY}";;
    cluster.management.communication.port)            CLUSTER_MANAGEMENT_COMMUNICATION_PORT="${CURRENT_PROPERTY}";;
    cluster.management.local.ipAddress)               CLUSTER_MANAGEMENT_LOCAL_IPADDRESS="${CURRENT_PROPERTY}";;
    cluster.management.local.port)                    CLUSTER_MANAGEMENT_LOCAL_PORT="${CURRENT_PROPERTY}";;
    cluster.management.remote.ipAddress)              CLUSTER_MANAGEMENT_REMOTE_IPADDRESS="${CURRENT_PROPERTY}";;
    cluster.management.remote.port)                   CLUSTER_MANAGEMENT_REMOTE_PORT="${CURRENT_PROPERTY}";;
    cluster.node.id)                                  CLUSTER_NODE_ID="${CURRENT_PROPERTY}";;
    cluster.node.preference)                          CLUSTER_NODE_PREFERENCE="${CURRENT_PROPERTY}";;
    cluster.oracle.db.additional.options)             CLUSTER_ADDITIONAL_OPTIONS="${CURRENT_PROPERTY}";;
    cluster.oracle.db.connectionpoolsize.default)     CLUSTER_DB_CONNECTIONPOOLSIZE_DEFAULT="${CURRENT_PROPERTY}";;
    cluster.oracle.db.connectionpoolsize.history)     CLUSTER_DB_CONNECTIONPOOLSIZE_HISTORY="${CURRENT_PROPERTY}";;
    cluster.oracle.db.connect.timeout.default)        CLUSTER_CONNECT_TIMEOUT_DEFAULT="${CURRENT_PROPERTY}";;
    cluster.oracle.db.connect.timeout.history)        CLUSTER_CONNECT_TIMEOUT_HISTORY="${CURRENT_PROPERTY}";;
    cluster.oracle.db.jdbc.url)                       CLUSTER_DB_JDBC_URL="${CURRENT_PROPERTY}";;
    cluster.oracle.db.password)                       CLUSTER_DB_PASSWORD="${CURRENT_PROPERTY}";;
    cluster.oracle.db.socket.timeout.default)         CLUSTER_SOCKET_TIMEOUT_DEFAULT="${CURRENT_PROPERTY}";;
    cluster.oracle.db.socket.timeout.history)         CLUSTER_SOCKET_TIMEOUT_HISTORY="${CURRENT_PROPERTY}";;
    cluster.oracle.db.timeout.default)                CLUSTER_DB_TIMEOUT_DEFAULT="${CURRENT_PROPERTY}";;
    cluster.oracle.db.timeout.history)                CLUSTER_DB_TIMEOUT_HISTORY="${CURRENT_PROPERTY}";;
    cluster.oracle.db.user)                           CLUSTER_DB_USER="${CURRENT_PROPERTY}";;
    cluster.persistence.interval)                     CLUSTER_PERSISTENCE_INTERVAL="${CURRENT_PROPERTY}";;
    cluster.persistence.max.batch.size)               CLUSTER_PERSISTENCE_MAX_BATCH_SIZE="${CURRENT_PROPERTY}";;
    cluster.persistence.synchronous)                  CLUSTER_PERSISTENCE_SYNCHRONOUS="${CURRENT_PROPERTY}";; 
    cluster.rmi.local.ipAddress)                      CLUSTER_LOCAL_IPADDRESS="${CURRENT_PROPERTY}";;
    cluster.rmi.local.port)                           CLUSTER_LOCAL_RMI_PORT="${CURRENT_PROPERTY}";;
    cluster.rmi.remote.ipAddress)                     CLUSTER_REMOTE_IPADDRESS="${CURRENT_PROPERTY}";;
    cluster.rmi.remote.port)                          CLUSTER_REMOTE_RMI_PORT="${CURRENT_PROPERTY}";;
  esac
  fi
    
  if f_selected ${GERONIMO_SELECTED} ; then
  case ${i} in
    geronimo.deployer.folder) f_check_is_path "${i}";          GERONIMO_DEPLOYER_HOME="${CURRENT_PROPERTY}";;
    geronimo.folder)          f_check_is_path "${i}";          GERONIMO_HOME="${CURRENT_PROPERTY}";;
    geronimo.http.port)       f_check_is_integer "${i}";       GERONIMO_HTTP_PORT="${CURRENT_PROPERTY}";;
    geronimo.ipAddress)       f_check_is_ip_address "${i}";    GERONIMO_IP_ADDRESS="${CURRENT_PROPERTY}";;
    geronimo.jmx.port)        f_check_is_integer "${i}";       GERONIMO_JMX_PORT="${CURRENT_PROPERTY}";;
    geronimo.password)        f_check_is_password "${i}";      GERONIMO_PASSWORD="${CURRENT_PROPERTY}";;
    geronimo.rmi.port)        f_check_is_integer "${i}";       GERONIMO_RMI_PORT="${CURRENT_PROPERTY}";;
    geronimo.ssl.port)        f_check_is_integer "${i}";       GERONIMO_SSL_PORT="${CURRENT_PROPERTY}";;
    geronimo.user)            f_check_is_alpha_numeric "${i}"; GERONIMO_USER="${CURRENT_PROPERTY}";;
    geronimo.syslog.file)     f_check_is_path "${i}";          GERONIMO_SYSLOG_FILE="${CURRENT_PROPERTY}";;
    geronimo.syslog.facility)                                  GERONIMO_SYSLOG_FACILITY="${CURRENT_PROPERTY}";;
    esac
  fi
    
  if f_selected ${SIPADAPTER_SELECTED} ; then
  case ${i} in
    sipadapter.high.port)                             SIPADAPTER_HIGH_PORT="${CURRENT_PROPERTY}";;
    sipadapter.low.port)                              SIPADAPTER_LOW_PORT="${CURRENT_PROPERTY}";;
    sipadapter.notify.responsetimeout)                SIPADAPTER_NOTIFY_RESPONSETIMEOUT="${CURRENT_PROPERTY}";;
  esac
  fi
}
