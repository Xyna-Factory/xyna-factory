
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



# OS -> Time/NTP library

##############################
# Time konfigurieren
f_configure_time () {
  f_configure_ntp_client
  f_configure_timezone
}

##############################
# NTP Server installieren
f_configure_ntp_client () {
  local ret_val=0
  local FILE_TO_EDIT="/etc/ntp.conf"
  local DRIFT_FILE="/var/lib/ntp/drift/ntp.drift"
  
  # TODO  Aenderung: Falls Konfiguration vorhanden diese nicht ueberschreiben
  echo -e "\n* Configuring NTP client"
  
  case ${INSTALLATION_PLATFORM} in
    rhel|oracle|centos)    DRIFT_FILE="/var/lib/ntp/drift";;
    solaris) FILE_TO_EDIT="/etc/inet/ntp.conf";;
  esac
 
  if [[ ! -d "$(dirname "${DRIFT_FILE}")" ]]; then ${VOLATILE_MKDIR} -p "$(dirname "${DRIFT_FILE}")"; fi
  if [[ ! -f "${DRIFT_FILE}" ]]; then echo "0.0" > "${DRIFT_FILE}"; fi

  echo "    + adding server '${NTP1_SERVER}'"
  echo "    + adding server '${NTP2_SERVER}'"
  backup_file ${FILE_TO_EDIT}
  ${VOLATILE_CAT} > ${FILE_TO_EDIT} << A_HERE_DOCUMENT
driftfile ${DRIFT_FILE}
server ${NTP1_SERVER} iburst dynamic
server ${NTP2_SERVER} iburst dynamic
A_HERE_DOCUMENT

  case ${INSTALLATION_PLATFORM} in
    sles)    chkconfig ntp 35;          /etc/init.d/ntp restart;;
    rhel|oracle|centos)  
             if [[ "x${SYSTEMD_ENV}" == "xtrue" ]] ; then
               systemctl start ntpd; systemctl enable ntpd
             else
               chkconfig ntpd on;         /etc/init.d/ntpd restart
             fi
             ;;
    debian)  update-rc.d ntp defaults;  /etc/init.d/ntp restart;;
    ubuntu)  update-rc.d ntp defaults;  /etc/init.d/ntp restart;;
    solaris) svcadm enable ntp;         svcadm refresh ntp;;
  esac 

  ret_val=$?
  return ${ret_val}
}


##############################
# Zeitzone auf UTC einstellen
f_configure_timezone () {
# TODO  Zeitzone per Property konfigurierbar machen oder aus target "time" abspalten
    
    local ret_val=0
    echo -e "\n* Configuring timezone"
    echo "    + setting timezone 'UTC'"

    case ${INSTALLATION_PLATFORM} in
  sles|rhel|oracle|centos) 
      if [[ "x${SYSTEMD_ENV}" == "xtrue" ]] ; then
         timedatectl set-timezone UTC
      else
         local FILE_TO_EDIT="/etc/sysconfig/clock"
         exit_if_not_exists ${FILE_TO_EDIT}
         backup_file ${FILE_TO_EDIT}
         #  set timezone to UTC in /etc/sysconfig/clock
         f_replace_in_file ${FILE_TO_EDIT} "s+TIMEZONE=.*+TIMEZONE=\"UTC\"+"
         #  re-link /etc/localtime to /usr/share/zoneinfo/UTC
         ${VOLATILE_LN} -sf /usr/share/zoneinfo/UTC /etc/localtime
      fi
      ;;
  
  debian|ubuntu)
      local FILE_TO_EDIT="/etc/timezone"
      exit_if_not_exists ${FILE_TO_EDIT}
      backup_file ${FILE_TO_EDIT}
      echo UTC > ${FILE_TO_EDIT}
            #  re-link /etc/localtime to /usr/share/zoneinfo/UTC
      ${VOLATILE_LN} -sf /usr/share/zoneinfo/UTC /etc/localtime
      ;;
  
  solaris)   
      local FILE_TO_EDIT="/etc/default/init"
      exit_if_not_exists ${FILE_TO_EDIT}
      backup_file ${FILE_TO_EDIT}
      f_replace_in_file ${FILE_TO_EDIT} "s+TZ=.*+TZ=\"UTC\"+"
      ;;
  
  *) err_msg "f_configure_timezone: Platform '${INSTALLATION_PLATFORM}' is not supported.";;
    esac           
    
    ret_val=$?
    return ${ret_val}
}
