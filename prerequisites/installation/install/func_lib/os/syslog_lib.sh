# ---------------------------------------------------
#  Copyright GIP AG 2013
#  (http://www.gip.com)
#
#  Hechtsheimer Str. 35-37
#  55131 Mainz
# ---------------------------------------------------
#  $Revision: 143988 $
#  $Date: 2013-12-17 10:19:43 +0100 (Di, 17. Dez 2013) $
# ---------------------------------------------------

# OS -> Syslog library

# Log Facility for Xyna
f_get_xyna_log_facility() {
  case ${INSTANCE_NUMBER} in
    1) echo "local0";;
    2) echo "local3";;
    3) echo "local5";;
    *) echo "local0";;
  esac 
}

#Erkennung der drei Syslog-Arten
f_detect_syslog_type () {
  if [[ ${INSTALLATION_PLATFORM} == solaris ]] ; then
    echo "solaris_syslog";
  elif [[ -f "/etc/syslog-ng/syslog-ng.conf" ]]; then
    echo "syslog-ng"; 
  elif [[ -f "/etc/rsyslog.conf" ]]; then
    if (( 0 == $(f_count_occurrencies_in_file "\$ModLoad" "/etc/rsyslog.conf") )) ; then
      echo "rsyslog_v8";
      #TODO bessere Erkennung?
      #v8 steht hier für mehrere Features:
      # a) module(load=... statt $ModLoad ....
      # b) $IncludeConfig statt alles in rsyslog.conf
      # c) $FileOwner syslog und $FileGroup adm
    else 
      echo "rsyslog";
    fi;
  elif [[ -f "/etc/syslog.conf" ]]; then
    echo "syslog";
  else
    err_msg "f_detect_syslog_type: Unable to detect syslog species - skipping"
    echo "unknown";
  fi
}


install_syslog_and_logrotation () {
  local SYSLOG_TYPE=$(f_detect_syslog_type);
  echo -e "\n* Configuring syslog demon (${SYSLOG_TYPE})"
  
  local TARGET_DIR="$(dirname "${XYNA_SYSLOG_FILE}")"
  local TARGET_DIR2=""
  if [[  "x${COMPONENT_GERONIMO}" == "xtrue" ]]; then
    TARGET_DIR2="$(dirname "${GERONIMO_SYSLOG_FILE}")"
  fi
  
  if [[ ${SYSLOG_TYPE} != "rsyslog_v8" ]] ; then
    #Unter rsyslog_v8 soll Log erst bei Bedarf durch rsyslog selbst angelegt werden
    if [[ -n "${TARGET_DIR}" ]]; then
      ${VOLATILE_MKDIR} -p "${TARGET_DIR}"
      #FIXME warum chown auf root:XYNA_GROUP ?
      ${VOLATILE_CHOWN} "$(f_generate_chown_parameter "root")" "${TARGET_DIR}"
      ${VOLATILE_CHMOD} 775 "${TARGET_DIR}"
    fi
    if [[ -n "${TARGET_DIR2}" ]]; then
      ${VOLATILE_MKDIR} -p "${TARGET_DIR2}"
      ${VOLATILE_CHOWN} "$(f_generate_chown_parameter "root")" "${TARGET_DIR2}"
      ${VOLATILE_CHMOD} 775 "${TARGET_DIR2}"
    fi  
  elif  [ "x${INSTALLATION_PLATFORM}" == "xubuntu" ] && [[ $(echo ${INSTALLATION_PLATFORM_VERSION} | ${VOLATILE_SED} -e "s+^18.*+18+") == "18"  || $(echo ${INSTALLATION_PLATFORM_VERSION} | ${VOLATILE_SED} -e "s+^20.*+20+") == "20" ]]; then
    for i in ${TARGET_DIR} ${TARGET_DIR2}
    do
      if [[ -n "${i}" ]]; then
        ${VOLATILE_MKDIR} -p "${i}"
        ${VOLATILE_CHOWN} syslog:xyna "${i}"
        ${VOLATILE_CHMOD} 775 "${i}"
      fi
    done
  fi

  #  bugz 14421: Link 'log' nach ${INSTALL_PREFIX} legen
  if [[ -d "${INSTALL_PREFIX}" && ! -e "${INSTALL_PREFIX}/log" ]]; then
    check_if_user_exists "${XYNA_USER}"
    if [[ $? -eq 1 ]]; then
      ${VOLATILE_SU} - "${XYNA_USER}" -c "${VOLATILE_LN} -s ${TARGET_DIR} ${INSTALL_PREFIX}/log"
    else
      ${VOLATILE_LN} -s "${TARGET_DIR}" "${INSTALL_PREFIX}/log"
    fi
  fi

  case ${SYSLOG_TYPE} in 
    syslog-ng)      f_configure_syslog_ng ;;
    rsyslog)        f_configure_rsyslog ;;
    rsyslog_v8)     f_configure_rsyslogv8 ;;
    syslog)         f_configure_syslog ;;
    solaris_syslog) f_configure_solaris_syslog;;
    *) err_msg "install_syslog_and_logrotation: Type '${syslogType}' is not supported.";;
  esac;
}


f_configure_syslog () {
  local TARGET_FILE="/etc/syslog.conf"
  exit_if_not_exists "${TARGET_FILE}"
  backup_file "${TARGET_FILE}"

  for i in 0 1 2 3 4 5 6 7
  do
    ret_val=$(${VOLATILE_GREP} "/var/log/messages" "${TARGET_FILE}" | ${VOLATILE_GREP} -c local${i}.none)
    if [[ ${ret_val} -eq 0 ]]; then
      echo "    + disabling facility 'local${i}' for destination '/var/log/messages'"
      ${VOLATILE_CP} -p -f "${TARGET_FILE}" "${TMP_FILE}"
      ${VOLATILE_AWK} '$2 == "/var/log/messages" {printf "%s;local'${i}'.none\t%s\n", $1, $2} $2 != "/var/log/messages" {print $0}' "${TARGET_FILE}" > "${TMP_FILE}"
      ${VOLATILE_MV} "${TMP_FILE}" "${TARGET_FILE}"
    else
      echo "    + facility 'local${i}' for destination '/var/log/messages' already disabled"
    fi
  done

  local FILE_TO_EDIT="/etc/sysconfig/syslog"
  exit_if_not_exists "${FILE_TO_EDIT}"
  backup_file "${FILE_TO_EDIT}"

  #  read current options
  local SYSLOGD_OPTIONS=$(${VOLATILE_GREP} "^SYSLOGD_OPTIONS" "${FILE_TO_EDIT}" | ${VOLATILE_AWK} -F = '{print $2}' | ${VOLATILE_TR} -d \")

  #  add '-r' if not set already
  SYSLOGD_OPTIONS=$(add_elements_to_string "${SYSLOGD_OPTIONS}" '-r')

  #  save changes to file
  f_replace_in_file "${FILE_TO_EDIT}" "s+^SYSLOGD_OPTIONS=.*+SYSLOGD_OPTIONS=\"${SYSLOGD_OPTIONS}\"+"

  check_if_user_exists "${XYNA_USER}"
  if [[ $? -eq 1 ]]; then
     if [[ -f "${XYNA_SYSLOG_FILE}" ]]; then
       ${VOLATILE_CHOWN} "$(f_generate_chown_parameter)" "${XYNA_SYSLOG_FILE}" 
     fi
     if [[ -f "${GERONIMO_SYSLOG_FILE}" ]]; then
       ${VOLATILE_CHOWN} "$(f_generate_chown_parameter)" "${GERONIMO_SYSLOG_FILE}"
     fi
  fi
  /etc/init.d/syslog restart
}

f_configure_syslog_ng () {
  local TARGET_FILE="/etc/syslog-ng/syslog-ng.conf"
  exit_if_not_exists "${TARGET_FILE}"
  backup_file "${TARGET_FILE}"

  ${VOLATILE_CAT} > "${TMP_FILE}" << A_HERE_DOCUMENT
60c60
<       #udp(ip("0.0.0.0") port(514));
---
>       udp(ip("0.0.0.0") port(514));
98c98
< filter f_messages   { not facility(news, mail) and not filter(f_iptables); };
---
> filter f_messages   { not facility(news, mail) and not filter(f_iptables) and not filter(f_local); };
208,209c208,209
< destination localmessages { file("/var/log/localmessages"); };
< log { source(src); filter(f_local); destination(localmessages); };
---
> #destination localmessages { file("/var/log/localmessages"); };
> #log { source(src); filter(f_local); destination(localmessages); };
A_HERE_DOCUMENT

${VOLATILE_PATCH} --quiet -l -f -i "${TMP_FILE}" "${TARGET_FILE}"
  ${VOLATILE_RM} -f "${TMP_FILE}"

  check_if_user_exists "${XYNA_USER}"
  if [[ $? -eq 1 ]]; then
     if [[ -f "${XYNA_SYSLOG_FILE}" ]]; then
       ${VOLATILE_CHOWN} "$(f_generate_chown_parameter)" "${XYNA_SYSLOG_FILE}" 
     fi
     if [[ -f "${GERONIMO_SYSLOG_FILE}" ]]; then
       ${VOLATILE_CHOWN} "$(f_generate_chown_parameter)" "${GERONIMO_SYSLOG_FILE}"
     fi
  fi
  
  /etc/init.d/syslog restart
}

f_configure_rsyslog () {
  local TARGET_FILE="/etc/rsyslog.conf"
  exit_if_not_exists "${TARGET_FILE}"
  backup_file "${TARGET_FILE}"

  f_replace_in_file "${TARGET_FILE}" \
    "s+#\$ModLoad imudp+\$ModLoad imudp+" \
    "s+#\$UDPServerRun 514+\$UDPServerRun 514+"

  check_if_user_exists "${XYNA_USER}"
  if [[ $? -eq 1 ]]; then
    if [[ -f "${XYNA_SYSLOG_FILE}" ]]; then
       ${VOLATILE_CHOWN} "$(f_generate_chown_parameter)" "${XYNA_SYSLOG_FILE}" 
    fi
    if [[ -f "${GERONIMO_SYSLOG_FILE}" ]]; then
       ${VOLATILE_CHOWN} "$(f_generate_chown_parameter)" "${GERONIMO_SYSLOG_FILE}"
    fi
  fi
  f_restart_rsyslog
}

f_configure_rsyslogv8 () {
  local TARGET_FILE="/etc/rsyslog.conf"
  exit_if_not_exists "${TARGET_FILE}"
  
  backup_file "${TARGET_FILE}"
  f_replace_in_file "${TARGET_FILE}" \
    "s+#module(load=\"imudp\")+module(load=\"imudp\")+" \
    "s+#input(type=\"imudp\" port=\"514\")+input(type=\"imudp\" port=\"514\")+"
 
  f_restart_rsyslog
}



f_configure_solaris_syslog () {
  local TARGET_FILE="/etc/syslog.conf"
  exit_if_not_exists "${TARGET_FILE}"
  backup_file "${TARGET_FILE}"

  check_if_user_exists "${XYNA_USER}"
  if [[ $? -eq 1 ]]; then
    if [[ -f "${XYNA_SYSLOG_FILE}" ]]; then
       ${VOLATILE_CHOWN} "$(f_generate_chown_parameter)" "${XYNA_SYSLOG_FILE}" 
    fi
    if [[ -f "${GERONIMO_SYSLOG_FILE}" ]]; then
       ${VOLATILE_CHOWN} "$(f_generate_chown_parameter)" "${GERONIMO_SYSLOG_FILE}"
    fi
  fi
  /usr/sbin/svccfg -s svc:/system/system-log setprop config/log_from_remote=true
  /usr/sbin/svcadm restart svc:/system/system-log:default
}


#  Syslog Facility und Log-File einrichten
#
#  Eingabeparameter:
#+  1 = Facility Name, e.g. "local0"
#+  2 = Full path to log file, e.g. "/var/log/xyna/tomcat.log"
#
f_add_syslog_facility () {
  local FACILITY_NAME="${1}"
  local FULL_PATH_TO_LOG_FILE="${2}"
  local SYSLOG_TYPE=$(f_detect_syslog_type);
  
  if [[ -z "${FACILITY_NAME}" ]]; then
    err_msg "f_add_syslog_facility: unable to work without facility name."
    return ${EX_PARAMETER}
  fi
  if [[ -z "${FULL_PATH_TO_LOG_FILE}" ]]; then
    err_msg "f_add_syslog_facility: unable to work without logfile name."
    return ${EX_PARAMETER}
  fi

  echo -e "\n* Configuring syslog facility '${FACILITY_NAME}' to '${FULL_PATH_TO_LOG_FILE}'"

  if [[ ${SYSLOG_TYPE} != "rsyslog_v8" ]] ; then
    #FIXME warum nochmal?
    local TARGET_DIR="$(dirname "${FULL_PATH_TO_LOG_FILE}")"
    if [[ ! -d "${TARGET_DIR}" ]]; then ${VOLATILE_MKDIR} -p "${TARGET_DIR}"; fi
    ${VOLATILE_CHOWN} "root" "${TARGET_DIR}"
    ${VOLATILE_CHMOD} 775 "${TARGET_DIR}"
  fi;
   
  case ${SYSLOG_TYPE} in 
    syslog-ng)      f_add_syslog_facility_syslog_ng "${FACILITY_NAME}" "${FULL_PATH_TO_LOG_FILE}" ;;
    rsyslog)        f_add_syslog_facility_rsyslog "${FACILITY_NAME}" "${FULL_PATH_TO_LOG_FILE}" ;;
    syslog)         f_add_syslog_facility_syslog "${FACILITY_NAME}" "${FULL_PATH_TO_LOG_FILE}" ;;
    rsyslog_v8 )    f_add_syslog_facility_rsyslogv8 "${FACILITY_NAME}" "${FULL_PATH_TO_LOG_FILE}" ;;
    solaris_syslog) f_add_syslog_facility_solaris_syslog "${FACILITY_NAME}" "${FULL_PATH_TO_LOG_FILE}";;
    *) err_msg "f_add_syslog_facility: Syslog Type '${SYSLOG_TYPE}' is not supported.";;
  esac
    
  #Logrotation
  case ${SYSLOG_TYPE} in 
    solaris_syslog) f_configure_solaris_logrotation "${FACILITY_NAME}" "${FULL_PATH_TO_LOG_FILE}";;
    *)              f_configure_logrotation "${FACILITY_NAME}" "${FULL_PATH_TO_LOG_FILE}";;
  esac
}

#  DO NOT CALL DIRECTLY! This is called via 'f_add_syslog_facility'...
f_add_syslog_facility_syslog () {
    local FACILITY_NAME="${1}"
    local FULL_PATH_TO_LOG_FILE="${2}"
    local TARGET_FILE="/etc/syslog.conf"
    exit_if_not_exists "${TARGET_FILE}"
    backup_file "${TARGET_FILE}"
    local ret_val=$(${VOLATILE_GREP} -c "^${FACILITY_NAME}.\*" "${TARGET_FILE}")
    case ${ret_val} in
  0)  echo "    + creating destination '${FULL_PATH_TO_LOG_FILE}'"
      echo -e "${FACILITY_NAME}.*\t${FULL_PATH_TO_LOG_FILE}" >> "${TARGET_FILE}"
      ${VOLATILE_TOUCH} "${FULL_PATH_TO_LOG_FILE}"
      /etc/init.d/syslog restart
      ;;
  1)  echo "    + destination '${FACILITY_NAME}' already exists";;
  *)  err_msg "Configuration in '${TARGET_FILE}' is not as expected. Check manually!";;
    esac
}

#  DO NOT CALL DIRECTLY! This is called via 'f_add_syslog_facility'...
f_add_syslog_facility_syslog_ng () {
    local FACILITY_NAME="${1}"
    local FULL_PATH_TO_LOG_FILE="${2}"
    local TARGET_FILE="/etc/syslog-ng/syslog-ng.conf"
    exit_if_not_exists "${TARGET_FILE}"
    backup_file "${TARGET_FILE}"

    #  hier kommt es auf den Whitespace hinter 'f_local ' an!
    local ret_val=$(${VOLATILE_GREP} -c "facility(${FACILITY_NAME})" "${TARGET_FILE}")
    case ${ret_val} in
  0)  echo "    + creating destination '${FULL_PATH_TO_LOG_FILE}'"
      echo -e "#\nfilter f_${FACILITY_NAME} { facility(${FACILITY_NAME}); };\ndestination ${FACILITY_NAME} { file(\"${FULL_PATH_TO_LOG_FILE}\" perm(0644)); };\nlog { source(src); filter(f_${FACILITY_NAME}); destination(${FACILITY_NAME}); };" >> "${TARGET_FILE}"
      ${VOLATILE_TOUCH} "${FULL_PATH_TO_LOG_FILE}"
      /etc/init.d/syslog restart
      ;;
  1)  echo "    + destination '${FACILITY_NAME}' already exists";;
  *)  err_msg "Configuration in '${TARGET_FILE}' is not as expected. Check manually!";;
    esac
}

f_restart_rsyslog () {
  case ${INSTALLATION_PLATFORM} in
    rhel|oracle)
      case ${INSTALLATION_PLATFORM_VERSION} in
        7.*)
          systemctl restart rsyslog.service
          ;;
        *)
          /etc/init.d/rsyslog restart
          ;;
      esac
      ;;
   ubuntu|centos)
     systemctl restart rsyslog.service
     ;;
    *)
      /etc/init.d/rsyslog restart
      ;;
  esac
}

#  DO NOT CALL DIRECTLY! This is called via 'f_add_syslog_facility'...
f_add_syslog_facility_rsyslog () {
  local FACILITY_NAME="${1}"
  local FULL_PATH_TO_LOG_FILE="${2}"
  local TARGET_FILE="/etc/rsyslog.conf"
  exit_if_not_exists "${TARGET_FILE}"
  
  local ret_val=$(${VOLATILE_GREP} -c "^${FACILITY_NAME}.\*" "${TARGET_FILE}")
  case ${ret_val} in
    0) echo "    + creating destination '${FULL_PATH_TO_LOG_FILE}'"
       backup_file "${TARGET_FILE}"
       echo -e "${FACILITY_NAME}.*\t${FULL_PATH_TO_LOG_FILE}" >> "${TARGET_FILE}"
       ${VOLATILE_TOUCH} "${FULL_PATH_TO_LOG_FILE}"
       f_restart_rsyslog
       ;;
    1)  echo "    + destination '${FACILITY_NAME}' already exists";;
    *)  err_msg "Configuration in '${TARGET_FILE}' is not as expected. Check manually!";;
  esac
}

f_add_syslog_facility_rsyslogv8 () {
  local FACILITY_NAME="${1}"
  local FULL_PATH_TO_LOG_FILE="${2}"
  local TARGET_FILE="/etc/rsyslog.d/10-xyna.conf"
  
  if [[ ! -e "${TARGET_FILE}" ]] ; then
    echo -e "# Xyna logging configuration\n\
# \n\
# Note: fileOwner and fileGroup are only used if \$PrivDropToUser \n\
#       and \$PrivDropToGroup are not set in /etc/rsyslog.conf \n\
# \n" > "${TARGET_FILE}"
  fi;

  local LOGFILE_OWNER=${XYNA_USER}
  if [ "x${INSTALLATION_PLATFORM}" == "xubuntu" ] && [[ $(echo ${INSTALLATION_PLATFORM_VERSION} | ${VOLATILE_SED} -e "s+^18.*+18+") == "18"  || $(echo ${INSTALLATION_PLATFORM_VERSION} | ${VOLATILE_SED} -e "s+^20.*+20+") == "20" ]]; then
    LOGFILE_OWNER=syslog
  fi
  
  case $(f_count_occurrencies_in_file "^${FACILITY_NAME}" "${TARGET_FILE}") in
    0) echo "    + creating destination '${FULL_PATH_TO_LOG_FILE}'"
       backup_file "${TARGET_FILE}"
       
       echo -e "\n${FACILITY_NAME}.* action(type=\"omfile\" \n\
                file=\"${FULL_PATH_TO_LOG_FILE}\"\n\
                fileOwner=\"${LOGFILE_OWNER}\" fileGroup=\"${XYNA_GROUP}\"\n\
                fileCreateMode=\"0640\" dirCreateMode=\"0755\")\n\
         & stop\n" >> "${TARGET_FILE}"
       f_restart_rsyslog
       ;;
    1)  echo "    + destination '${FACILITY_NAME}' already exists";;
    *)  err_msg "Configuration in '${TARGET_FILE}' is not as expected. Check manually!";;
  esac
  
  if [ "x${INSTALLATION_PLATFORM}" == "xubuntu" ] && [[ $(echo ${INSTALLATION_PLATFORM_VERSION} | ${VOLATILE_SED} -e "s+^18.*+18+") == "18"  || $(echo ${INSTALLATION_PLATFORM_VERSION} | ${VOLATILE_SED} -e "s+^20.*+20+") == "20" ]]; then
    ${VOLATILE_SED} -i -e "s+^\$PrivDropToUser syslog+#\$PrivDropToUser syslog+" \
                       -e "s+^\$PrivDropToGroup syslog+#\$PrivDropToGroup syslog+" /etc/rsyslog.conf
  fi
}




#  DO NOT CALL DIRECTLY! This is called via 'f_add_syslog_facility'...
f_add_syslog_facility_solaris_syslog () {
    local FACILITY_NAME="${1}"
    local FULL_PATH_TO_LOG_FILE="${2}"
    local TARGET_FILE="/etc/syslog.conf"
    exit_if_not_exists "${TARGET_FILE}"
    backup_file "${TARGET_FILE}"

    local ret_val=$(${VOLATILE_GREP} -c "${FACILITY_NAME}.debug" "${TARGET_FILE}")
    case ${ret_val} in
  0) echo "    + creating destination '${FULL_PATH_TO_LOG_FILE}'"
     /usr/bin/echo "${FACILITY_NAME}.debug\t${FULL_PATH_TO_LOG_FILE}" >> "${TARGET_FILE}"
     ${VOLATILE_TOUCH} "${FULL_PATH_TO_LOG_FILE}"
     /usr/sbin/svcadm restart svc:/system/system-log:default
     ;;
  1) echo "    + destination '${FACILITY_NAME}' already exists";;
  *) err_msg "Configuration in '${FULL_PATH_TO_LOG_FILE}' is not as expected. Check manually!";;
    esac
}

#  DO NOT CALL DIRECTLY! This is called via 'f_add_syslog_facility'...
f_configure_solaris_logrotation () {
    local FACILITY_NAME="${1}"
    local FULL_PATH_TO_LOG_FILE="${2}"
    local TARGET_FILE="/etc/logadm.conf"
    exit_if_not_exists "${TARGET_FILE}"
    backup_file "${TARGET_FILE}"

    local ret_val=$(${VOLATILE_GREP} -c "${FULL_PATH_TO_LOG_FILE}" "${TARGET_FILE}")
    case ${ret_val} in
  0)  echo "    + enabling logrotation for '${FULL_PATH_TO_LOG_FILE}'"
      echo "${FULL_PATH_TO_LOG_FILE} -C 30 -c -s200m -z 7" >> "${TARGET_FILE}";;
  1)  echo "    + logrotation for '${FACILITY_NAME}' already enabled";;
  *)  err_msg "Configuration in '${TARGET_FILE}' is not as expected. Check manually!";;
    esac

    f_add_crontab_entry "3 0 * * * /usr/sbin/logadm ${FULL_PATH_TO_LOG_FILE} && /usr/sbin/svcadm restart system-log"
}

#  DO NOT CALL DIRECTLY! This is called via 'f_add_syslog_facility'...
f_configure_logrotation () {
    local FACILITY_NAME="${1}"
    local FULL_PATH_TO_LOG_FILE="${2}"
    local FILE_TO_EDIT="/etc/logrotate.d/${FACILITY_NAME}"
    local RESTART_CMD=""
    if   [[ -f "/etc/syslog-ng/syslog-ng.conf" ]]; then
      RESTART_CMD="/etc/init.d/syslog restart"
    elif [[ -f "/etc/rsyslog.conf" ]]; then
      case ${INSTALLATION_PLATFORM} in
        rhel|oracle)
          case ${INSTALLATION_PLATFORM_VERSION} in
            7.*)
              RESTART_CMD="systemctl restart rsyslog.service"
              ;;
            *)
              RESTART_CMD="/etc/init.d/rsyslog restart"
              ;;
          esac
          ;;
        centos)
         RESTART_CMD="systemctl restart rsyslog.service"
         ;;
        *)
          RESTART_CMD="/etc/init.d/rsyslog restart"
          ;;
      esac 
    elif [[ -f "/etc/syslog.conf" ]]; then
      RESTART_CMD="/etc/init.d/syslog restart"
    else
      err_msg "f_configure_logrotation: Unable to detect syslog species - skipping"
      return 1
    fi

    local LOGFILE_OWNER=${XYNA_USER}
    if [ "x${INSTALLATION_PLATFORM}" == "xubuntu" ] && [[ $(echo ${INSTALLATION_PLATFORM_VERSION} | ${VOLATILE_SED} -e "s+^18.*+18+") == "18"  || $(echo ${INSTALLATION_PLATFORM_VERSION} | ${VOLATILE_SED} -e "s+^20.*+20+") == "20" ]]; then
      LOGFILE_OWNER=syslog
    fi

    ${VOLATILE_CAT} > "${FILE_TO_EDIT}" << A_HERE_DOCUMENT
${FULL_PATH_TO_LOG_FILE}
{
    daily
    create
    compress
    delaycompress
    dateext
    maxage 30
    missingok
    nomail
    noolddir
    rotate 30
    sharedscripts
    postrotate
            ${RESTART_CMD} > /dev/null
    endscript
    su ${LOGFILE_OWNER} ${XYNA_GROUP} 
}
A_HERE_DOCUMENT

    #  bugz 13513: Logrotation auf Mitternacht verschieben
    if [[ -d "/var/spool/cron/lastrun" ]]; then
  ${VOLATILE_TOUCH} -t "$(${VOLATILE_DATE} +"%Y%m%d")0000" "/var/spool/cron/lastrun/cron.daily"
    fi
}
