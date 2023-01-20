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

# Oracle Database library

###################
#Umgebunsgvariablen setzen
f_set_environment () {
  DIR_ORACLE_BASE="/opt/oracle"
  DIR_ORACLE_CLIENT_HOME="${DIR_ORACLE_BASE}/instantclient"
  DIR_ORACLE_TNS_ADMIN="${DIR_ORACLE_CLIENT_HOME}/network/admin"
  LST_JDBC_FILES="orai18n.jar ojdbc7.jar ojdbc8.jar"

  VOLATILE_SQLPLUS="${DIR_ORACLE_CLIENT_HOME}/sqlplus"
  
  SSL_CERTIFICATE_DIR="${INSTALL_PREFIX}/.ssl"
}


##############################
# Umgebungsvariablen fuer ORACLE setzen
f_configure_profile_for_oracle () {
  local PUSER="${1}"
  local TARGET_FILE="${USERHOME_PREFIX}/${PUSER}/.bashrc"
  if [[ "x${PUSER}" == "xroot" ]]; then
    TARGET_FILE="/root/.bashrc"
  fi
  echo -e "    + Setting \$ORACLE_BASE, \$ORACLE_HOME und \$TNS_ADMIN in '${TARGET_FILE}'"
  check_if_user_exists ${PUSER} true

  backup_file "${TARGET_FILE}"
  if [[ ! -f "${TARGET_FILE}" ]]; then 
    echo >> "${TARGET_FILE}"
    echo "# set environment variable 'PATH'" >> "${TARGET_FILE}"
    echo "export PATH=\"\${PATH}\"" >> "${TARGET_FILE}"
    ${VOLATILE_CHOWN} "$(f_generate_chown_parameter "${PUSER}")" "${TARGET_FILE}"
  fi

  #  ORACLE-Variablen am Anfang der Datei einfuegen
  ${VOLATILE_CP} -p -f "${TARGET_FILE}" "${TMP_FILE}"
  echo "export ORACLE_BASE=\"${DIR_ORACLE_BASE}/\"" > "${TMP_FILE}"
  echo "export ORACLE_HOME=\"\${ORACLE_BASE}/${DIR_ORACLE_CLIENT_HOME#${DIR_ORACLE_BASE}/}\"" >> "${TMP_FILE}"
  echo "export TNS_ADMIN=\"\${ORACLE_HOME}/${DIR_ORACLE_TNS_ADMIN#${DIR_ORACLE_CLIENT_HOME}/}\"" >> "${TMP_FILE}"
 
  #  Fuege den Rest aus der Originaldatei hinzu
  ${VOLATILE_GREP} -v "export ORACLE_BASE" "${TARGET_FILE}" | ${VOLATILE_GREP} -v "export ORACLE_HOME" | \
  ${VOLATILE_GREP} -v "export TNS_ADMIN" >> "${TMP_FILE}"

  #  angepasste Datei ersetzt das Original
  ${VOLATILE_MV} "${TMP_FILE}" "${TARGET_FILE}"

  #  ORACLE_HOME in den PATH einfuegen, falls erforderlich
  case $(${VOLATILE_GREP} -v "^#" "${TARGET_FILE}" | ${VOLATILE_GREP} -c " PATH=") in
    0) echo >> "${TARGET_FILE}"
       echo "# set environment variable 'PATH'" >> "${TARGET_FILE}"
       echo "export PATH=\"\${PATH}:\${ORACLE_HOME}\"" >> "${TARGET_FILE}"
       ;;
    1) local STR_PATH="$(${VOLATILE_AWK} 'BEGIN { FS="=" } $1 == "export PATH" {print $2}' "${TARGET_FILE}")"
       local NEW_PATH="$(add_elements_to_string "${STR_PATH}" "\${ORACLE_HOME}" | ${VOLATILE_TR} " " ":" | ${VOLATILE_TR} -d "\"")"
       f_replace_in_file "${TARGET_FILE}" "s+export PATH=.*+export PATH=\"${NEW_PATH}\"+"
       ;;
    *) err_msg "${TARGET_FILE} contains multiple PATH statements - this needs to be fixed manually!";;
  esac

  #  ORACLE_HOME in LD_LIBRARY_PATH einfuegen, falls erforderlich
  case $(${VOLATILE_GREP} -v "^#" "${TARGET_FILE}" | ${VOLATILE_GREP} -c " LD_LIBRARY_PATH=") in
    0) echo >> "${TARGET_FILE}"
       echo "# set environment variable 'LD_LIBRARY_PATH'" >> "${TARGET_FILE}"
       echo "export LD_LIBRARY_PATH=\"\$LD_LIBRARY_PATH:/usr/lib:/usr/lib64:\${ORACLE_HOME}\"" >> "${TARGET_FILE}"
       ;;
    1) local STR_PATH="$(${VOLATILE_AWK} 'BEGIN { FS="=" } $1 == "export LD_LIBRARY_PATH" {print $2}' "${TARGET_FILE}")"
       local NEW_PATH="$(add_elements_to_string "${STR_PATH}" "\${ORACLE_HOME}" | ${VOLATILE_TR} " " ":" | ${VOLATILE_TR} -d "\"")"
       f_replace_in_file "${TARGET_FILE}" "s+export LD_LIBRARY_PATH=.*+export LD_LIBRARY_PATH=\"${NEW_PATH}\"+"
       ;;
    *) err_msg "${TARGET_FILE} contains multiple LD_LIBRARY_PATH statements - this needs to be fixed manually!";;
  esac

  #  .bashrc neuladen aber nur fuer aktuellen Benutzer
  check_user ${PUSER}
  if [[ $? -eq 0 ]]; then
    . "${TARGET_FILE}"
  fi
}

##############################
# Oracle Prerequisite
f_install_oracle() {
  
  echo -e "\n* Installing Oracle Instant Client"
  f_configure_profile_for_oracle root
  
  # Bashrc anlegen
  check_if_user_exists ${XYNA_USER}
  if [[ $? -eq 1 ]]; then
    f_configure_profile_for_oracle ${XYNA_USER}
  fi  
    
  if [[ ! -x ${VOLATILE_SQLPLUS} ]]; then
    echo "    + unzipping Oracle instant client"
    ${VOLATILE_MKDIR} -p ${DIR_ORACLE_CLIENT_HOME}
    ${VOLATILE_UNZIP} -q application/oracle/instantclient-basic-*.zip  -d ${DIR_ORACLE_CLIENT_HOME}
    ${VOLATILE_UNZIP} -q application/oracle/instantclient-jdbc-*.zip -d ${DIR_ORACLE_CLIENT_HOME}
    ${VOLATILE_UNZIP} -q application/oracle/instantclient-sqlplus-*.zip -d ${DIR_ORACLE_CLIENT_HOME}   
    ${VOLATILE_MV} ${DIR_ORACLE_CLIENT_HOME}/instantclient_*/* ${DIR_ORACLE_CLIENT_HOME}
    ${VOLATILE_RM} -rf ${DIR_ORACLE_CLIENT_HOME}/instantclient_*/*
  else 
    echo "    + ${VOLATILE_SQLPLUS} already exists"
  fi
  
  if [[ ! -d ${DIR_ORACLE_TNS_ADMIN} ]]; then
    echo -e "\n* Creating directory ${DIR_ORACLE_TNS_ADMIN}"
    ${VOLATILE_MKDIR} -p ${DIR_ORACLE_TNS_ADMIN}
  else
    echo "    + ${DIR_ORACLE_TNS_ADMIN} already exists"
  fi
  f_install_ojdbc_driver

 return ${ret_val}
}




##############################
# OJDBC-Treiber fuer Xyna installieren
f_install_ojdbc_driver() {

  local ret_val=0

  echo -e "\n* Extracting OJDBC driver for Xyna Factory"

  local DIR_XYNA_USERLIB="${INSTALL_PREFIX}/lib"

  if [[ ! -d ${DIR_ORACLE_CLIENT_HOME}  ]]; then
    echo "Please install Oracle Database Client first!"
    return ${ret_val}
  fi  
        
  if [[ ! -d ${DIR_XYNA_USERLIB} ]]; then
    echo "    + creating ${DIR_XYNA_USERLIB}"
    ${VOLATILE_MKDIR} ${DIR_XYNA_USERLIB}
  else
    echo "    + ${DIR_XYNA_USERLIB} already exists"
  fi

  for FILE_I in ${LST_JDBC_FILES}
  do
    local STR_NAME=$(basename ${FILE_I})
    if [[ ! -f "${DIR_XYNA_USERLIB}/${STR_NAME}" ]] ; then
      echo "    + copying ${FILE_I} to ${DIR_XYNA_USERLIB}"
      ${VOLATILE_CP} ${DIR_ORACLE_CLIENT_HOME}/${FILE_I} ${DIR_XYNA_USERLIB}/
      ret_val=$?
    else
      echo "    + ${FILE_I} already exists in ${DIR_XYNA_USERLIB}"
    fi
  done

  check_if_user_exists "${XYNA_USER}"
  if [[ $? -eq 1 ]]; then
    ${VOLATILE_CHOWN} -R "$(f_generate_chown_parameter)" "${DIR_XYNA_USERLIB}"
  fi

  return ${ret_val}
}




#  Ruft SQL*Plus auf und fuehrt eine SQL-Datei aus.
#
#  Voraussetzungen:
#    + SQL-Datei existiert und enthaelt gueltigen PL/SQL-Code
#    + SQL-Datei endet mit 'QUIT;' und einer leeren Zeile
#    + SQL-Datei muss die Endung '.sql' haben
#
#  Eingabeparameter:
#    1: Name des Datenbankbenutzers
#    2: Password des Datenbankbenutzers
#    3: Connectionstring zur Datenbank (ohne '@')
#    4: Name der zu verwendenden SQL-Datei
#    5: [optional] weitere Argumente, wie z.B. "as sysdba"
# ab 6: [optional] weitere Argumente, die als Parameter an die SQL-Datei uebergeben werden
#        ACHTUNG!  Der fuenfte Parameter muss gesetzt sein, kann auch "" sein...
#
#  Rueckgabewert:
#    Die gesamte Ausgabe des Aufrufs von SQL*Plus
#
#  Beispiel:
#    echo -e "SELECT COUNT(*) FROM XYNACLUSTERSETUP;\nQUIT;\n" > ${TMP_FILE}.sql
#    TXT_SQLPLUS_OUTPUT=$(f_run_sqlplus_from_file "xynauser" "xynapw" "(DESCRIPTION=[...])'" "${TMP_FILE}.sql")
#
f_run_sqlplus_from_file () {
  local STR_DB_USER="${1:-sys}"
  local STR_DB_PASS="${2:-oracle}"
  local STR_DB_CONNECTSTRING="${3:-(DESCRIPTION=())}"
  local TARGET_FILE="${4:-${TMP_FILE}}"
  local STR_EXTRA_ARGS="${5}"
  
  if [[ $# -le 4 ]]; then
    shift $#
  else  
    shift 5
  fi
  
  if [[ "x" == "x${ORACLE_HOME}" ]]; then
    f_exit_with_message ${EX_PARAMETER} "f_run_sqlplus_from_file: \$ORACLE_HOME is not set."
  fi

  VOLATILE_SQLPLUS=$(get_full_path_to_executable sqlplus)
  if [[ ! -x "${VOLATILE_SQLPLUS}" ]]; then
    f_exit_with_message ${EX_NOPERM} "f_run_sqlplus_from_file: '${VOLATILE_SQLPLUS}' is not executable."
  fi

  if [[ ! -f "${TARGET_FILE}" ]]; then
    f_exit_with_message ${EX_OSFILE} "f_run_sqlplus_from_file: Unable to locate file '${TARGET_FILE}'."
  fi

  ${VOLATILE_SQLPLUS} -L -S ${STR_DB_USER}/${STR_DB_PASS}@"${STR_DB_CONNECTSTRING#*@}" "${STR_EXTRA_ARGS}" @ "${TARGET_FILE}" "$@"
}

#  DB-Verbindung zu einer Oracle-DB fuer bestimmten Benutzer mittels SQL*Plus testen
#
#  Eingabeparameter
#    1: Name des Benutzers
#    2: Passwort des Benutzers
#    3: Connectionstring zur Datenbank
#    4: [optional] Weitere Argumente, wie z.B. "as sysdba"
#
#  Rueckgabewert:
#    0 = Verbindung erfolgreich
#    1 = Verbindung fehlgeschlagen
#
f_check_db_connection() {
  local STR_DB_USER="${1:-sys}"
  local STR_DB_PASS="${2:-oracle}"
  local STR_DB_CONNECTSTRING="${3:-@(DESCRIPTION=())}"
  local STR_EXTRA_ARGS="${4}"
  
  echo -e "\n* Checking database connection with credentials: '${STR_DB_USER}' / '${STR_DB_PASS}'"

  #  prepare the SQL-File
  echo -e "SELECT 'Connection works' FROM dual;\nQUIT;\n" > "${TMP_FILE}.sql"

  #  execute the SQL-File using SQL*Plus and save output
  local TXT_SQLPLUS_OUTPUT=$(f_run_sqlplus_from_file "${STR_DB_USER}" "${STR_DB_PASS}" "${STR_DB_CONNECTSTRING}" "${TMP_FILE}.sql" "${STR_EXTRA_ARGS}")
  ${VOLATILE_RM} -f "${TMP_FILE}.sql"

  #  check, if the expected string appears in the output
  if [[ $(echo "${TXT_SQLPLUS_OUTPUT}" | ${VOLATILE_AWK} 'NR == 4 {print $0}') == "Connection works" ]]; then
    return 0
  fi

  return 1
}

