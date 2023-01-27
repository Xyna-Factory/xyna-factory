
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



# OS -> SSH library

##############################
# SSH konfigurieren
f_configure_ssh () {
  f_configure_ssh_server
  f_configure_ssh_client
}


##############################
# SSH Server konfigurieren
f_configure_ssh_server () {
  local ret_val=0
  echo -e "\n* Configuring SSH server"
  check_user root true
  
  if [[ "${INSTALLATION_PLATFORM}" == "solaris" ]]; then
      echo "    + not inserting wicked options on Solaris - skipping"
      return ${ret_val}
  fi

  local FILE_TO_EDIT="/etc/ssh/sshd_config"
  exit_if_not_exists ${FILE_TO_EDIT}
  backup_file ${FILE_TO_EDIT}
  #  Die Option ClientAliveInterval einfuegen, falls noch nicht vorhanden
  local ret_val=0
  ${VOLATILE_GREP} "^ClientAliveInterval" ${FILE_TO_EDIT} > /dev/null && ret_val=1
  if [[ ${ret_val-0} -eq 0 ]]; then
    echo >> ${FILE_TO_EDIT}
    echo "# Sets a timeout interval in seconds after  which,  if  no" >> ${FILE_TO_EDIT}
    echo "# data  has  been  received  from the client, sshd sends a" >> ${FILE_TO_EDIT}
    echo "# message through  the  encrypted  channel  to  request  a" >> ${FILE_TO_EDIT}
    echo "# response  from  the client." >> ${FILE_TO_EDIT}
    echo "ClientAliveInterval 300" >> ${FILE_TO_EDIT}
  fi
  unset ret_val
  #  bugz 8323: Weitere Parameter fuer sshd
  ${VOLATILE_GREP} "^PermitEmptyPasswords" ${FILE_TO_EDIT} > /dev/null && ret_val=1
  if [[ ${ret_val-0} -eq 0 ]]; then
    echo "PermitEmptyPasswords no" >> ${FILE_TO_EDIT}
  else
    f_replace_in_file ${FILE_TO_EDIT} "s+^PermitEmptyPasswords.*+PermitEmptyPasswords no+"
  fi
  unset ret_val
  ${VOLATILE_GREP} "^IgnoreRhosts" ${FILE_TO_EDIT} > /dev/null && ret_val=1
  if [[ ${ret_val-0} -eq 0 ]]; then
    echo "IgnoreRhosts yes" >> ${FILE_TO_EDIT}
  else
    f_replace_in_file ${FILE_TO_EDIT} "s+^IgnoreRhosts.*+IgnoreRhosts yes+"
  fi
  unset ret_val
  ${VOLATILE_GREP} "^Ciphers" ${FILE_TO_EDIT} > /dev/null && ret_val=1
  if [[ ${ret_val-0} -eq 0 ]]; then
    echo "Ciphers aes128-cbc,blowfish-cbc,arcfour256,arcfour,aes192-cbc,aes256-cbc,aes128-ctr,aes192-ctr,aes256-ctr"\
      >> ${FILE_TO_EDIT}
  else
    f_replace_in_file ${FILE_TO_EDIT} "s+^Ciphers.*+Ciphers aes128-cbc,blowfish-cbc,arcfour256,arcfour,aes192-cbc,aes256-cbc,aes128-ctr,aes192-ctr,aes256-ctr+"
  fi
  unset ret_val
  ${VOLATILE_GREP} "^UseDNS" ${FILE_TO_EDIT} > /dev/null && ret_val=1
  if [[ ${ret_val-0} -eq 0 ]]; then
    echo "UseDNS no" >> ${FILE_TO_EDIT}
  else
    f_replace_in_file ${FILE_TO_EDIT} "s+^UseDNS.*+UseDNS no+"
  fi
  unset ret_val

  case ${INSTALLATION_PLATFORM} in
    sles)    /etc/init.d/sshd reload;;
    rhel|oracle|centos)  
             if [[ "x${SYSTEMD_ENV}" == "xtrue" ]] ; then
               systemctl reload sshd
             else
               /etc/init.d/sshd reload
             fi
             ;;
    debian)  /etc/init.d/ssh reload;;
    ubuntu)  /etc/init.d/ssh reload;;
    solaris) svcadm refresh ssh;;
  esac 

  ret_val=$?
  return ${ret_val}
}


##############################
# SSH Client konfigurieren
f_configure_ssh_client () {
  echo -e "\n* Configuring SSH client"
  
  if [[ ! -d ${HOME}/.ssh ]]; then
    echo "    + creating directory: ${HOME}/.ssh"
    ${VOLATILE_MKDIR} -p ${HOME}/.ssh
    ${VOLATILE_CHMOD} 700 ${HOME}/.ssh
  else
    echo "    + nothing to do, directory exists: ${HOME}/.ssh"
  fi

  OUT_FILE="${HOME}/.ssh/id_rsa"
  if [[ ! -f ${OUT_FILE} ]]; then
    echo "    + generating key: ${OUT_FILE}"
    ssh-keygen -q -b 2048 -f ${OUT_FILE} -N ''
  else
    echo "    + nothing to do, key found: ${OUT_FILE}"
  fi

  FILE_TO_EDIT="${HOME}/.ssh/config"
  if [[ ! -f ${FILE_TO_EDIT} ]]; then
    echo "StrictHostKeyChecking no" > ${FILE_TO_EDIT}
  else
    ${VOLATILE_GREP} "^StrictHostKeyChecking" ${FILE_TO_EDIT} > /dev/null && ret_val=1
    if [[ ${ret_val-0} -eq 0 ]]; then
      echo "StrictHostKeyChecking no" >> ${FILE_TO_EDIT}
    fi
  fi
}



#Ueberpruefe ob ein keybasierter SSH-Login moeglich ist
f_check_keybased_ssh_access() {
 local STR_USER=$1
 local STR_HOST=${2:-localhost}
 local ret_val=${EX_NOPERM}
  
  ssh -o 'PasswordAuthentication no'  -o 'ChallengeResponseAuthentication no'  -o 'PreferredAuthentications publickey' ${STR_USER}@${STR_HOST} hostname 2>/dev/null 1>/dev/null
  ret_val=$?
  echo ${ret_val}

  return ${ret_val}
}


#Ueberpruefe ob ein keybasierter SSH-Login moeglich ist
f_check_remote_file_exists() {
 local STR_USER=${1}
 local STR_HOST=${2:-localhost}
 local STR_FILE=${3}
 local ret_val=${EX_NOPERM}

 ssh ${STR_USER}@${STR_HOST}  "if [[ -f ${STR_FILE} ]]; then echo 1; else echo 0; fi" 2>/dev/null
 ret_val=$?
 return ${ret_val} 
}

