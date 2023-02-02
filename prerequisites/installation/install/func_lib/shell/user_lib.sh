
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



# Shell -> User library


#  Ein neuer User mit zugehoeriger Gruppe wird installiert.
install_xyna_user () {
  echo -e "\n* Installing unprivileged user"

  for i in ${XYNA_GROUP} gipgroup
  do
    check_if_group_exists "${i}"
    case $? in
      0)
        echo "    + adding group '${i}'"
        ${VOLATILE_GROUPADD} ${i}
        ;;
      1) echo "    + nothing to be done - group '${i}' already exists";;
      *) err_msg "Error in '${FILE_TO_EDIT}'. Skipping...";;
    esac
  done

  check_if_user_exists "${XYNA_USER}"
  case $? in
    0)
      echo "    + adding user '${XYNA_USER}'"
      ${VOLATILE_USERADD} -d ${USERHOME_PREFIX}/${XYNA_USER} -s /bin/bash -g ${XYNA_GROUP} -G ${XYNA_GROUP},gipgroup -m ${XYNA_USER}
      ${VOLATILE_CAT} > ${USERHOME_PREFIX}/${XYNA_USER}/.bash_profile << A_HERE_DOCUMENT

#  Source global definitions
if [[ -f ~/.bashrc ]]; then
        . ~/.bashrc 
fi
A_HERE_DOCUMENT

      local TARGET_FILE="${USERHOME_PREFIX}/${XYNA_USER}/.bashrc"
      if [[ ! -f "${TARGET_FILE}" ]]; then ${VOLATILE_TOUCH} "${TARGET_FILE}"; fi

      if [[ $(${VOLATILE_GREP} -c "PS1" "${TARGET_FILE}") -eq 0 ]]; then
  echo >> "${TARGET_FILE}"
  echo "# set shell prompt" >> "${TARGET_FILE}"
        echo "export PS1=\"\\u@\\h:\\w> \"" >> "${TARGET_FILE}"
      fi

      if [[ $(${VOLATILE_GREP} -c "PATH" "${TARGET_FILE}") -eq 0 ]]; then
        echo >> "${TARGET_FILE}"
  echo "# set environment variable 'PATH'" >> "${TARGET_FILE}"
        echo "export PATH=\"\${PATH}\"" >> "${TARGET_FILE}"
      fi

      ${VOLATILE_CHOWN} -R "$(f_generate_chown_parameter)" "${TARGET_FILE}"

      echo "    + setting password '${XYNA_PASSWORD}'"
      local FILE_TO_EDIT="/etc/shadow"
      local ENCRYPTED_PASSWORD=$(perl -e 'print crypt($ARGV[0], "password")' "${XYNA_PASSWORD}")
      ${VOLATILE_CP} -p -f "${FILE_TO_EDIT}" "${TMP_FILE}"
      ${VOLATILE_AWK} 'BEGIN { FS=":" } $1 != "'${XYNA_USER}'" {print $0} $1 == "'${XYNA_USER}'" {printf "%s:%s:%s:%s:%s:%s:%s:%s:%s\n", $1, "'${ENCRYPTED_PASSWORD}'", $3, $4, $5, $6, $7, $8, $9}' "${FILE_TO_EDIT}" > "${TMP_FILE}"
      ${VOLATILE_MV} "${TMP_FILE}" "${FILE_TO_EDIT}"

      if [[ -f "${USERHOME_PREFIX}/${XYNA_USER}/.bash_logout" ]]; then
        ${VOLATILE_RM} -f "${USERHOME_PREFIX}/${XYNA_USER}/.bash_logout"
      fi
      ;;
    1) echo "    + nothing to be done - user '${XYNA_USER}' already exists"
       STR_HINWEIS_AM_ENDE="${STR_HINWEIS_AM_ENDE} If existing, close now all terminal session of the user '${XYNA_USER}'. "
       ;;
    *) err_msg "Error in '${FILE_TO_EDIT}'. Skipping...";;
  esac

  #  add 'gipgroup' to the user
  local ALREADY_GROUPMEMBER=$(${VOLATILE_ID} -G "${XYNA_USER}" | ${VOLATILE_SED} -e "s+ +,+g")
  ${VOLATILE_USERMOD} -G "${ALREADY_GROUPMEMBER},${XYNA_GROUP},gipgroup" "${XYNA_USER}"
}


#  Ueberpruefe ob aktuelle Benutzer mit dem erforderlichen Benutzer
#  uebereinstimmt.
#  Eingabeparameter
#1:   Login-Name des erforderlichen Benutzers
#2:   (optional) wenn "true" gesetzt wird, falls Benutzer nicht vorhanden, exit ausgeloest  wird, falls Benutzer nicht vorhanden, exit ausgeloest
#
check_user () {
  local abort=${2:-false}
  local ret_val=1

  CURRENT_USER=$(${VOLATILE_PS} -o "user=" -p $$ | ${VOLATILE_SED} -e "s+ ++g")
  if [[ "x${CURRENT_USER}" != "x${1}" ]]; then
    ret_val=0
    if [[ "x${abort}" == "xtrue" ]];then
      f_exit_with_message ${EX_WRONGUSER} "check_user: This script can only be run as user '${1}' (current user is '${CURRENT_USER}'). Abort!"
    fi
  fi
  return ${ret_val}
}



#  Ueberpruefe ob ein bestimmter Benutzer im System vorhanden ist
#
#  Eingabeparameter
#1:   Login-Name des Benutzers
#2:  (optional) wenn "true" gesetzt wird, falls Benutzer nicht vorhanden, exit ausgeloest
#
#  Rueckgabe
#    0: wenn Benutzer nicht vorhanden ist.
#    1: wenn Benutzer vorhanden ist.
check_if_user_exists () {
  local abort=${2:-false}

  local TARGET_FILE="/etc/passwd"
  exit_if_not_exists "${TARGET_FILE}"

  local ret_val=$(${VOLATILE_GREP} -c "^${1}[:]" "${TARGET_FILE}")

  if [[ ${ret_val:-0} -eq 0 ]] && [[ "x${abort}" == "xtrue" ]]; then
    f_exit_with_message ${EX_WRONGUSER} "check_if_user_exists: The user '${1}' does not exist on this system. Abort!"
  fi
  return ${ret_val}
}


#  Ueberpruefe ob eine bestimmte Gruppe im System vorhanden ist
#
#  Eingabeparameter
#    1: Name der Gruppe
#    2: (optional) wenn "true" gesetzt wird, falls Gruppe nicht vorhanden, exit ausgeloest
#
#  Rueckgabe
#    0: wenn Gruppe nicht vorhanden ist.
#    1: wenn Gruppe vorhanden ist.
#
check_if_group_exists () {
  local abort=${2:-false}

  local TARGET_FILE="/etc/group"
  exit_if_not_exists "${TARGET_FILE}"

  local ret_val=$(${VOLATILE_GREP} -c "^${1}[:]" "${TARGET_FILE}")

  if [[ ${ret_val:-0} -eq 0 ]] && [[ "x${abort}" == "xtrue" ]]; then
    f_exit_with_message ${EX_WRONGGROUP} "check_if_group_exists: The group '${1}' does not exist on this system. Abort!"
  fi
  return ${ret_val}
}