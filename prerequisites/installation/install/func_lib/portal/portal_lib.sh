
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


# Portal library


KEYSTORE_PASS="fw8967bsfqboeaDFSDFDAFuivnq0sffn23f5hd4wghxdhdvbhj20DFG9lhg68zhwrthdklh56S467mw4q5"

create_certificate () {
  echo -e "\n* Creating certificate"
  if [[ ! -d "${INSTALL_PREFIX}" ]]; then
    echo "    + unable to locate directory '${INSTALL_PREFIX}'. Skipping..."
  else
    if [[ ! -d "${SSL_CERTIFICATE_DIR}" ]]; then
      ${VOLATILE_MKDIR} -p "${SSL_CERTIFICATE_DIR}"
    fi

    if [[ ! -f "${SSL_CERTIFICATE_DIR}/xyna_ssl.crt" || ! -f "${SSL_CERTIFICATE_DIR}/xyna_ssl.key" ]]; then
      echo "    + creating certificate and its key in '${SSL_CERTIFICATE_DIR}'"
      ${VOLATILE_OPENSSL} req -x509 -nodes -days 3600 -newkey rsa:2048 -sha256 -out "${SSL_CERTIFICATE_DIR}/xyna_ssl.crt" -keyout "${SSL_CERTIFICATE_DIR}/xyna_ssl.key" -subj "/CN=${STR_FQDN}/OU=GIPAG/O=GIPAG/L=Mainz/ST=Rhineland-Palatinate/C=DE"
    else
      echo "    + nothing to do: certificate + key '${SSL_CERTIFICATE_DIR}/xyna_ssl.crt' already exists"
    fi

    if [[ ! -f "${SSL_CERTIFICATE_DIR}/xyna_ssl.pfx" ]]; then
      echo "    + combining certificate and its key into a PKCS12 keystore"
      ${VOLATILE_OPENSSL} pkcs12 -export -out "${SSL_CERTIFICATE_DIR}/xyna_ssl.pfx" -in "${SSL_CERTIFICATE_DIR}/xyna_ssl.crt" -inkey "${SSL_CERTIFICATE_DIR}/xyna_ssl.key" -passout pass:"${KEYSTORE_PASS}"
    else
      echo "    + nothing to do: pkcs12 keystore   '${SSL_CERTIFICATE_DIR}/xyna_ssl.pfx' already exists"
    fi

    ${VOLATILE_CHGRP} -R gipgroup "${SSL_CERTIFICATE_DIR}"
    ${VOLATILE_CHMOD} 0750 "${SSL_CERTIFICATE_DIR}"
    ${VOLATILE_CHMOD} 0640 "${SSL_CERTIFICATE_DIR}"/*
  fi
}


set_java_environment () {
  #  JAVA_HOME dauerhaft speichern (ab dem naechsten Login aktiv)
  FILE_TO_EDIT="${1}"
  backup_file "${FILE_TO_EDIT}"
  if [[ ! -f "${FILE_TO_EDIT}" ]]; then ${VOLATILE_TOUCH} "${FILE_TO_EDIT}"; fi

  ${VOLATILE_GREP} "JAVA_HOME" "${FILE_TO_EDIT}" > /dev/null && ret_val=1
  if [[ ${ret_val-0} -eq 0 ]]; then
    echo >> "${FILE_TO_EDIT}"
    echo "# JAVA_HOME" >> "${FILE_TO_EDIT}"
    echo "export JAVA_HOME=\"${JAVA_HOME}\"" >> "${FILE_TO_EDIT}"
    echo "export PATH=\"${JAVA_HOME}/bin:$PATH\"" >> "${FILE_TO_EDIT}"
  fi; unset ret_val
}


f_setenv_sh () {
  FILE_TO_EDIT="${1}"
  if [[ -z "${FILE_TO_EDIT}" ]]; then
    echo "f_setenv_sh: Unable to work without input parameter - skipping"
  else
    backup_file "${FILE_TO_EDIT}"

    if [[ ! -f "${FILE_TO_EDIT}" ]]; then ${VOLATILE_TOUCH} "${FILE_TO_EDIT}"; fi
    
    echo -en "\n  * Find out Java Version"
    if [[ -n "${JAVA_HOME}" ]]; then
      VOLATILE_JAVA=${JAVA_HOME}/${BIN_PATH}/java
      if [[ -x ${VOLATILE_JAVA} ]]; then
        ${VOLATILE_JAVA} -version 2> ${TMP_FILE} 1>${TMP_FILE}
        JAVA_VERSION=$(${VOLATILE_GREP} "java version" ${TMP_FILE} | ${VOLATILE_AWK} -F\" '{print $2}')
        if [[ "x${JAVA_VERSION}" == "x" ]]; then
          #openjdk
          JAVA_VERSION=$(${VOLATILE_GREP} "jdk version" ${TMP_FILE} | ${VOLATILE_AWK} -F\" '{print $2}')
        fi
      fi
    fi
    
    echo "    + configuring jvm memory: ${FILE_TO_EDIT}"
    ${VOLATILE_GREP} "JAVA_OPTS" "${FILE_TO_EDIT}" > /dev/null && ret_val=1
    if [[ ${ret_val-0} -eq 0 ]]; then
    if [[ "x${JAVA_VERSION:0:2}" == "x11" ]]; then
        echo "JAVA_OPTS=\"-Xms${TOMCAT_MEMORY}m -Xmx${TOMCAT_MEMORY}m\"" >> "${FILE_TO_EDIT}"
      else
        echo "Unsupported java version detected: ${JAVA_VERSION}"
        exit 1
      fi
    fi; unset ret_val

    echo "    + configuring pid file: ${FILE_TO_EDIT}"
    ${VOLATILE_GREP} "CATALINA_PID" "${FILE_TO_EDIT}" > /dev/null && ret_val=1
    if [[ ${ret_val-0} -eq 0 ]]; then
      echo "CATALINA_PID=\${CATALINA_HOME}/work/catalina.pid" >> "${FILE_TO_EDIT}"
    fi; unset ret_val
  fi
}
