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

# Portal -> Apache Geronimo library

#  Zu einer gegebenen Komponente wird der Modulname aus Geronimo ermittelt:
#+ Bsp: ${geronimo.home}/bin/deploy.sh list-modules | grep XynaBlackEditionWebServices
#+      com.gip.xyna/XynaBlackEditionWebServices/3.2/car
#
#  Eingabeparameter:  Name der gesuchten Komponente
#  Ausgabe:           Modulnamen oder leerer String, falls Modul nicht gefunden wurde
#
#  Benoetigte Umgebungsvariablen:
#+ GERONIMO_HOME      Basispfad zur Geronimoinstallation
#+ GERONIMO_USER      Zugangsdaten fuer Geronimo
#+ GERONIMO_PASSWORD  Zugangsdaten fuer Geronimo
#+ NAMING_PORT        RMI-Port
#+ PORT_OFFSET        Portoffset wird zum RMI-Port hinzugezaehlt
#
f_get_geronimo_module_name () {
  if [[ "x" == "x${1}" ]]; then
    err_msg "f_get_geronimo_module_name: Unable to work without component name!"
  else
    local GERONIMO_DEPLOY_SH="${GERONIMO_HOME}/bin/deploy.sh"
    if [[ -f "${GERONIMO_HOME}/bin/deploy" ]]; then
  GERONIMO_DEPLOY_SH="${GERONIMO_HOME}/bin/deploy"
    fi
    local MODULENAME=""
    MODULENAME="$(${GERONIMO_DEPLOY_SH} --port $(( NAMING_PORT + PORT_OFFSET )) --user ${GERONIMO_USER} --password ${GERONIMO_PASSWORD} list-modules | ${VOLATILE_GREP} -v GERONIMO_HOME | ${VOLATILE_GREP} "${1}" | ${VOLATILE_AWK} '{print $NF}')"
    echo "${MODULENAME}"
  fi
}

#  Der Pfad setzt sich beim Deployen aus den Informationen aus geronimo-web.xml zusammen:
#  Bsp: groupId/AritfactId/version/ArtifactID-version.type
#       com/gip/xyna/XynaBlackEditionWebServices/3.2/XynaBlackEditionWebServices-3.2.car
#
#  Bei groupId sind Punkte '.' durch Verzeichnistrenner '/' zu ersetzen.
#
#  Eingabeparameter: voller Pfad zur geronimo-web.xml
#  Ausgabe:          Pfad, der aus den Informationen aus geronimo-web.xml zusammengestellt ist
#
f_get_geronimo_module_dirname () {
  local GERONIMO_WEB_XML="${1}"
  if [[ ! -f "${GERONIMO_WEB_XML}" ]]; then
    err_msg "f_get_geronimo_module_dirname: Unable to work without geronimo-web.xml."
  else
    local GROUPID=""
    GROUPID=$(${VOLATILE_GREP} -i "groupId>" "${GERONIMO_WEB_XML}" | ${VOLATILE_SED} -e "s+^[^>]*>++" -e "s+<.*$++" -e "s+\.+/+g")

    local ARTIFACTID=""
    ARTIFACTID=$(${VOLATILE_GREP} -i "artifactId>" "${GERONIMO_WEB_XML}" | ${VOLATILE_SED} -e "s+^[^>]*>++" -e "s+<.*$++")

    local VERSION=""
    VERSION=$(${VOLATILE_GREP} -i "version>" "${GERONIMO_WEB_XML}" | ${VOLATILE_SED} -e "s+^[^>]*>++" -e "s+<.*$++")

    local TYPE=""
    TYPE=$(${VOLATILE_GREP} -i "type>" "${GERONIMO_WEB_XML}" | ${VOLATILE_SED} -e "s+^[^>]*>++" -e "s+<.*$++")

    #  Ergebnis zusammenstellen
    echo "${GROUPID}/${ARTIFACTID}/${VERSION}/${ARTIFACTID}-${VERSION}.${TYPE}"
  fi
}

#  Ein WAR-File wird unter den Namen '${COMPONENT_NAME}' installiert.
#
#  Wieso wird undeployed und dann deployed anstatt einfach 'redeploy' zu verwenden?
#+ Wenn sich die Version aendert, funktioniert 'redeploy' nicht zuverlaessig.
#+ 'Undeploy' und anschliessendes 'deploy' ist sicherer!
#
#  Eingabeparameter:
#+   full/path/to/war.file
#+   Name der Komponente, z.B. 'OneIP-MPLS-Factory'
#
#  Ausgabe:           keine
#
#  Benoetigte Umgebungsvariablen:
#+ GERONIMO_HOME           Basispfad zur Geronimoinstallation
#+ GERONIMO_USER           Zugangsdaten fuer Geronimo
#+ GERONIMO_PASSWORD       Zugangsdaten fuer Geronimo
#+ GERONIMO_RMI_PORT       NamingPort
#+ GERONIMO_IP_ADDRESS     IP-Adresse auf der Geronimo lauscht
#+ GERONIMO_DEPLOYER_HOME  Pfad zum Geronimo Client Deployer Packager
#
#  Umgebungsvariablen, die gesetzt werden:
#+ NAMING_PORT        RMI-Port
#+ PORT_OFFSET        Portoffset wird zum RMI-Port hinzugezaehlt
#
f_deploy_war_into_geronimo () {
  local COMPONENT_WAR_FILE="${1:-filename_missing}"
  local COMPONENT_NAME="${2:-componentname_missing}"

  if [[ "x${COMPONENT_WAR_FILE}" == "x" || ! -f "${COMPONENT_WAR_FILE}" ]]; then
    err_msg "f_deploy_war_into_geronimo: Unable to work without war-file."
  else
    echo -e "\n* Deploying ${COMPONENT_NAME} into Apache Geronimo"

    #  GERONIMO_IP_ADDRESS und GERONIMO_DEPLOYER_HOME muessen mit sinnvollen Werten besetzt sein
    check_ip_address "${GERONIMO_IP_ADDRESS}"
    if [[ $? -eq 0 && -d "${GERONIMO_DEPLOYER_HOME}" ]]; then

        #  (1) Client Deployer Package verwenden, falls vorhanden
  f_check_open_port "${GERONIMO_IP_ADDRESS}" "${GERONIMO_RMI_PORT}"
  if [[ $? -eq 0 ]]; then

      local GERONIMO_DEPLOY_SH="${GERONIMO_DEPLOYER_HOME}/bin/deploy.sh"
      if [[ -f "${GERONIMO_DEPLOYER_HOME}/bin/deploy" ]]; then
    GERONIMO_DEPLOY_SH="${GERONIMO_DEPLOYER_HOME}/bin/deploy"
      fi

      echo "    + checking deployment"
      local MODULENAME=""
      MODULENAME="$(${GERONIMO_DEPLOY_SH} --port ${GERONIMO_RMI_PORT} --host ${GERONIMO_IP_ADDRESS} --user ${GERONIMO_USER} --password ${GERONIMO_PASSWORD} list-modules | ${VOLATILE_GREP} -v GERONIMO_HOME | ${VOLATILE_GREP} "${COMPONENT_NAME}" | ${VOLATILE_AWK} '{print $NF}')"
      if [[ "x${MODULENAME}" == "x" ]]; then
    local NUMBER_OF_MODULES="0"
      else
    local NUMBER_OF_MODULES=$(echo "${MODULENAME}" | ${VOLATILE_AWK} 'END {print NR}')
      fi
      case ${NUMBER_OF_MODULES} in
    0)  #  gesuchtes Modul ist nicht installiert, es kann direkt deployed werden
        ;;
    1)  #  gesuchtes Modul ist installiert, es muss re-deployed werden
        echo "    + undeploying ${COMPONENT_NAME}"
        ${GERONIMO_DEPLOY_SH} --port ${GERONIMO_RMI_PORT} --host ${GERONIMO_IP_ADDRESS} --user ${GERONIMO_USER} --password ${GERONIMO_PASSWORD} undeploy ${MODULENAME}
        ;;
    *)  #  gesuchtes Modul kann nicht eindeutig identifiziert werden
        err_msg "Unable to identify module '${COMPONENT_NAME}'. Check for multiple deployments and resolve manually!"
        return;;
      esac

      #  Deploy
      local GERONIMOWEB_XML="$(dirname "${COMPONENT_WAR_FILE}")/geronimo-web.xml"
      if [[ ! -f "${GERONIMOWEB_XML}" ]]; then
                #  Ohne geronimo-web.xml gehts nicht. Vielleicht enthaelt das WAR-File die benoetigte Datei...
    ${VOLATILE_UNZIP} -o -d "$(dirname ${TARGET_FILE})" -j "${TARGET_FILE}" WEB-INF/geronimo-web.xml
      fi
      
      if [[ -f "${GERONIMOWEB_XML}" ]]; then
    echo "    + deploying ${COMPONENT_NAME}"
    ${VOLATILE_SED} -e "s+TOKEN_COMPONENT_NAME+${COMPONENT_NAME}+" "${GERONIMOWEB_XML}" > "${TMP_FILE}"
    ${GERONIMO_DEPLOY_SH} --port ${GERONIMO_RMI_PORT} --host ${GERONIMO_IP_ADDRESS} --user "${GERONIMO_USER}" --password "${GERONIMO_PASSWORD}" deploy "${COMPONENT_WAR_FILE}" "${TMP_FILE}"
      else
    err_msg "f_deploy_war_into_geronimo: Unable to work without geronimo-web.xml."
      fi

  else
      attention_msg "Apache Geronimo on '${GERONIMO_IP_ADDRESS}:${GERONIMO_RMI_PORT}' is not running."
  fi

    else
  
  #  (2) Fallback auf die alte Loesung: Lokal deployen
  local CONFIG_SUBSTITUTIONS="${GERONIMO_HOME}/var/config/config-substitutions.properties"
  if [[ ! -f ${CONFIG_SUBSTITUTIONS} ]]; then
      echo "    + unable to locate Apache Geronimo installation in '${GERONIMO_HOME}'"
  else
      export NAMING_PORT=$(${VOLATILE_SED} -e "s+ = +=+g" "${CONFIG_SUBSTITUTIONS}" | ${VOLATILE_AWK} 'BEGIN { FS="=" } $1 == "NamingPort" {print $2}')
      export PORT_OFFSET=$(${VOLATILE_SED} -e "s+ = +=+g" "${CONFIG_SUBSTITUTIONS}" | ${VOLATILE_AWK} 'BEGIN { FS="=" } $1 == "PortOffset" {print $2}')

      f_check_open_port "${HOSTNAME}" $(( NAMING_PORT + PORT_OFFSET ))
      if [[ $? -eq 0 ]]; then
    
    local GERONIMO_DEPLOY_SH="${GERONIMO_HOME}/bin/deploy.sh"
    if [[ -f "${GERONIMO_HOME}/bin/deploy" ]]; then
        GERONIMO_DEPLOY_SH="${GERONIMO_HOME}/bin/deploy"
    fi

    echo "    + checking deployment"
                #  check, if application is already deployed
    local XYNAWS_MODULENAME="$(f_get_geronimo_module_name ${COMPONENT_NAME})"
    if [[ "x${XYNAWS_MODULENAME}" == "x" ]]; then
        local NUMBER_OF_MODULES="0"
    else
        local NUMBER_OF_MODULES=$(echo "${XYNAWS_MODULENAME}" | ${VOLATILE_AWK} 'END {print NR}')
    fi
    case ${NUMBER_OF_MODULES} in
        0)  #  gesuchtes Modul ist nicht installiert, es kann direkt deployed werden
      ;;
        1)  #  gesuchtes Modul ist installiert, es muss re-deployed werden
      echo "    + undeploying ${COMPONENT_NAME}"
      ${GERONIMO_DEPLOY_SH} --port $(( NAMING_PORT + PORT_OFFSET )) --user ${GERONIMO_USER} --password ${GERONIMO_PASSWORD} undeploy ${XYNAWS_MODULENAME}
      ;;
        *)  #  gesuchtes Modul kann nicht eindeutig identifiziert werden
      err_msg "Unable to identify module '${COMPONENT_NAME}'. Check for multiple deployments and resolve manually!"
      return;;
    esac
    
                #  Deploy
    local GERONIMOWEB_XML="$(dirname "${COMPONENT_WAR_FILE}")/geronimo-web.xml"
    if [[ ! -f "${GERONIMOWEB_XML}" ]]; then
                    #  Ohne geronimo-web.xml gehts nicht. Vielleicht enthaelt das WAR-File die benoetigte Datei...
        ${VOLATILE_UNZIP} -o -d "$(dirname ${TARGET_FILE})" -j "${TARGET_FILE}" WEB-INF/geronimo-web.xml
    fi
    
    if [[ -f "${GERONIMOWEB_XML}" ]]; then
        echo "    + deploying ${COMPONENT_NAME}"
        ${VOLATILE_SED} -e "s+TOKEN_COMPONENT_NAME+${COMPONENT_NAME}+" "${GERONIMOWEB_XML}" > "${TMP_FILE}"
        ${GERONIMO_DEPLOY_SH} --port $(( NAMING_PORT + PORT_OFFSET )) --user "${GERONIMO_USER}" --password "${GERONIMO_PASSWORD}" deploy "${COMPONENT_WAR_FILE}" "${TMP_FILE}"
    else
        err_msg "f_deploy_war_into_geronimo: Unable to work without geronimo-web.xml."
    fi
      else
    attention_msg "Apache Geronimo in '${GERONIMO_HOME}' is not running."
      fi

  fi

    fi
  fi
}



f_install_geronimo_deployer () {
    echo -e "\n* Installing Geronimo Client Deployer into '${GERONIMO_DEPLOYER_HOME}'."

    PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
    PORT_OFFSET=$(( ($((10#${PRODUCT_INSTANCE})) - 1) * 10 ))

    #  Vorkompiliertes Paket entpacken
    local GERONIMO_ZIP=$(ls application/geronimo/geronimo-tomcat*.tar.gz)
    if [[ ! -f "${GERONIMO_ZIP}" ]]; then
      echo "    + Unable to locate Geronimo Client Deployer - skipping"
    else
      if [[ -d ${GERONIMO_DEPLOYER_HOME} ]]; then
        echo "    + Replacing existing Geronimo Client Deployer"
        ${VOLATILE_RM} -rf ${GERONIMO_DEPLOYER_HOME}
      fi
      local GERONIMO_PACKAGE=$(basename ${GERONIMO_ZIP} -bin.tar.gz)
      ${VOLATILE_TAR} -xz --directory=$(dirname ${GERONIMO_DEPLOYER_HOME}) -f ${GERONIMO_ZIP}
      ${VOLATILE_MV} $(dirname ${GERONIMO_DEPLOYER_HOME})/${GERONIMO_PACKAGE} ${GERONIMO_DEPLOYER_HOME}
      if [[ ! -d "${GERONIMO_DEPLOYER_HOME}/deploy" ]]; then
        ${VOLATILE_MKDIR} ${GERONIMO_DEPLOYER_HOME}/deploy
      fi
  
      FILE_TO_EDIT="${GERONIMO_DEPLOYER_HOME}/var/config/config-substitutions.properties"
      backup_file "${FILE_TO_EDIT}"
      f_replace_in_file "${FILE_TO_EDIT}" \
        "s+^JMXPort[ ]*=.*+JMXPort=$(( GERONIMO_JMX_PORT - ${PORT_OFFSET} ))+" \
        "s+^NamingPort[ ]*=.*+NamingPort=$(( GERONIMO_RMI_PORT - ${PORT_OFFSET} ))+" \
        "s+^PortOffset[ ]*=.*+PortOffset=${PORT_OFFSET}+" \
        "s+^HTTPPort[ ]*=.*+HTTPPort=$(( GERONIMO_HTTP_PORT - ${PORT_OFFSET} ))+" \
        "s+^HTTPSPort[ ]*=.*+HTTPSPort=$(( GERONIMO_SSL_PORT - ${PORT_OFFSET} ))+"

      local USERHOME=$(${VOLATILE_AWK} 'BEGIN {FS=":"} $1 == "'${XYNA_USER}'" {print $6}' "/etc/passwd")
      if [[ -d ${USERHOME} ]]; then
        set_java_environment ${USERHOME}/.bashrc
        set_java_environment ${USERHOME}/.bash_profile
        ${VOLATILE_CHOWN} -R "$(f_generate_chown_parameter)" "${GERONIMO_DEPLOYER_HOME}" "${USERHOME}/.bashrc" "${USERHOME}/.bash_profile"
      fi
    fi
    set_java_environment ${HOME}/.bashrc
    if [[ ! -f ${HOME}/.bash_profile ]]; then
      ${VOLATILE_CAT} > ${HOME}/.bash_profile << A_HERE_DOCUMENT

#  Source global definitions
if [[ -f ~/.bashrc ]]; then
        . ~/.bashrc 
fi
A_HERE_DOCUMENT

      set_java_environment ${HOME}/.bash_profile
    fi
}

install_geronimo () {
  echo -e "\n* Installing Geronimo into '${GERONIMO_HOME}'."

  PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  PORT_OFFSET=$(( ($((10#${PRODUCT_INSTANCE})) - 1) * 10 ))

  #  Vorkompiliertes Paket entpacken
  local GERONIMO_ZIP=$(ls application/geronimo/geronimo-tomcat*.tar.gz)
  exit_if_not_exists ${GERONIMO_ZIP}
  local GERONIMO_PACKAGE=$(basename ${GERONIMO_ZIP} -bin.tar.gz)
  ${VOLATILE_TAR} -xz --directory=$(dirname ${GERONIMO_HOME}) -f ${GERONIMO_ZIP}
  ${VOLATILE_MV} $(dirname ${GERONIMO_HOME})/${GERONIMO_PACKAGE} ${GERONIMO_HOME}
  if [[ ! -d "${GERONIMO_HOME}/deploy" ]]; then
      ${VOLATILE_MKDIR} ${GERONIMO_HOME}/deploy
  fi

  #  install shared libraries into geronimo
  for i in $(${VOLATILE_LS} application/geronimo/lib/*)
  do
    install_file ${i} ${GERONIMO_HOME}/lib/endorsed/.
  done

  #  Facility local2 verwenden
  FILE_TO_EDIT="${GERONIMO_HOME}/var/log/server-log4j.properties"
  exit_if_not_exists "${FILE_TO_EDIT}"
  echo "    + enabling syslog"
  backup_file "${FILE_TO_EDIT}"
    #  Zeile finden, nach der eingefuegt werden kann
  INSERT_LINE=$(${VOLATILE_GREP} -n "^log4j.rootLogger" "${FILE_TO_EDIT}" | ${VOLATILE_AWK} 'BEGIN { FS=":"} {print $1}')
  if [[ ${INSERT_LINE:-0} -lt 1 ]]; then
    f_exit_with_message ${EX_PARAMETER} "Can't get line number for regexp '^log4j.rootLogger'."
  else
    #  SYSLOG zu den root-Loggern hinzufuegen
    f_replace_in_file "${FILE_TO_EDIT}" "s+log4j.rootLogger=\(.*\)+log4j.rootLogger=\1,SYSLOG+"
    #  Konfiguration fuer den Appender SYSLOG
    ${VOLATILE_CAT} > "${TMP_FILE}"2 << A_HERE_DOCUMENT
> log4j.appender.SYSLOG=com.gip.xyna.utils.logging.XynaSyslogAppender
> log4j.appender.SYSLOG.syslogHost=localhost
> log4j.appender.SYSLOG.layout=org.apache.log4j.PatternLayout
> #http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html: %C, %M, %L sind extrem langsam
> #log4j.appender.SYSLOG.layout.ConversionPattern=%d{dd-MM-yyyy HH:mm:ss} GERONIMO_${PRODUCT_INSTANCE} %-5p (%C:%M:%L) - [%x] %m%n
> log4j.appender.SYSLOG.layout.ConversionPattern=%d{ISO8601} GERONIMO_${PRODUCT_INSTANCE} %-5p [%t] [%x] %c: %m%n
> log4j.appender.SYSLOG.Facility=LOCAL2
A_HERE_DOCUMENT
    #  Position fuer das Einfuegen bestimmen
    let START_LINE=INSERT_LINE+1
    LINES_TO_INSERT=$(${VOLATILE_AWK} 'END {print NR}' "${TMP_FILE}"2)
    let END_LINE=INSERT_LINE+LINES_TO_INSERT
    #  Einfuegeposition ins Patch-File eintragen
    echo "${INSERT_LINE}a${START_LINE},${END_LINE}" > "${TMP_FILE}"1
    ${VOLATILE_CAT} "${TMP_FILE}"1 "${TMP_FILE}"2 > "${TMP_FILE}"
    ${VOLATILE_RM} "${TMP_FILE}"1 "${TMP_FILE}"2
    #  Patchen
    ${VOLATILE_PATCH} -l -i "${TMP_FILE}" "${FILE_TO_EDIT}"
    ${VOLATILE_RM} "${TMP_FILE}"
  fi

  f_setenv_sh "${GERONIMO_HOME}/bin/setenv.sh"

  echo "    + enabling user and role"
  FILE_TO_EDIT="${GERONIMO_HOME}/var/security/groups.properties"
  backup_file "${FILE_TO_EDIT}"
  f_replace_in_file "${FILE_TO_EDIT}" "s+admin=\(.*\)+admin=\1,${GERONIMO_USER}+"

  FILE_TO_EDIT="${GERONIMO_HOME}/var/security/users.properties"
  backup_file "${FILE_TO_EDIT}"
  echo "${GERONIMO_USER}=${GERONIMO_PASSWORD}" >> "${FILE_TO_EDIT}"

  echo "    + enabling SSL"
  ${VOLATILE_CAT} > "${TMP_FILE}" << A_HERE_DOCUMENT
92,93c92,94
<                    keystoreFile="../security/keystores/geronimo-default"
<                    keystorePass="secret"
---
>                    keystoreFile="${SSL_CERTIFICATE_DIR}/xyna_ssl.pfx"
>                    keystorePass="${KEYSTORE_PASS}"
>                    keystoreType="PKCS12"
A_HERE_DOCUMENT
  #  apply the patch
  FILE_TO_EDIT="${GERONIMO_HOME}/var/catalina/server.xml"
  backup_file "${FILE_TO_EDIT}"
  ${VOLATILE_PATCH} -l -i "${TMP_FILE}" "${FILE_TO_EDIT}"
  ${VOLATILE_RM} -f "${TMP_FILE}"

  echo "    + modify ports"
  #  Fuer HTTP und HTTPS muss der PortOffset abgezogen werden. Warum?
  #+ Weil beim Starten der PortOffset wieder draufgerechnet wird.
  #+ So wird der Wert aktiv, den der User in den Properties vorgegeben hat.
  FILE_TO_EDIT="${GERONIMO_HOME}/var/config/config-substitutions.properties"
  backup_file "${FILE_TO_EDIT}"
  f_replace_in_file "${FILE_TO_EDIT}" \
    "s+^RemoteDeployHostname[ ]*=.*+RemoteDeployHostname=${GERONIMO_IP_ADDRESS}+" \
    "s+^JMXPort[ ]*=.*+JMXPort=$(( GERONIMO_JMX_PORT - ${PORT_OFFSET} ))+" \
    "s+^NamingPort[ ]*=.*+NamingPort=$(( GERONIMO_RMI_PORT - ${PORT_OFFSET} ))+" \
    "s+^PortOffset[ ]*=.*+PortOffset=${PORT_OFFSET}+" \
    "s+^HTTPPort[ ]*=.*+HTTPPort=$(( GERONIMO_HTTP_PORT - ${PORT_OFFSET} ))+" \
    "s+^HTTPSPort[ ]*=.*+HTTPSPort=$(( GERONIMO_SSL_PORT - ${PORT_OFFSET} ))+"

  local USERHOME=$(${VOLATILE_AWK} 'BEGIN {FS=":"} $1 == "'${XYNA_USER}'" {print $6}' "/etc/passwd")
  if [[ -d ${USERHOME} ]]; then
    set_java_environment ${USERHOME}/.bashrc
    set_java_environment ${USERHOME}/.bash_profile
    ${VOLATILE_CHOWN} -R "$(f_generate_chown_parameter)" "${GERONIMO_HOME}" "${USERHOME}/.bashrc" "${USERHOME}/.bash_profile"
  fi
  set_java_environment ${HOME}/.bashrc
  if [[ ! -f ${HOME}/.bash_profile ]]; then
    ${VOLATILE_CAT} > ${HOME}/.bash_profile << A_HERE_DOCUMENT

#  Source global definitions
if [[ -f ~/.bashrc ]]; then
        . ~/.bashrc 
fi
A_HERE_DOCUMENT
  fi
  set_java_environment ${HOME}/.bash_profile
}


etc_initd_files_geronimo () {
f_etc_initd_files "geronimo" "application/geronimo" \
    "s+TOKEN_INSTALL_DIR+${GERONIMO_HOME}+" \
    "s+TOKEN_PROVIDES_GERONIMO+geronimo_${PRODUCT_INSTANCE}+" \
    "s+TOKEN_GERONIMO_USER+${GERONIMO_USER}+" \
    "s+TOKEN_GERONIMO_PASSWORD+${GERONIMO_PASSWORD}+" \
    "s+TOKEN_XYNA_USER+${XYNA_USER}+"
}


update_geronimo () {
  if [[ -d ${GERONIMO_HOME} ]]; then
    backup_dir "${GERONIMO_HOME}"
    ${VOLATILE_RM} -rf ${GERONIMO_HOME}
  fi
  install_geronimo
}

start_geronimo () {
  VOLATILE_JAVA=$(get_full_path_to_executable java)
  if [[ "x${VOLATILE_JAVA}" == "xXXX_command_java_not_found_XXX" ]]; then
    err_msg "Unable to locate java installation. Skipping..."
  else
    if [[ -f "$(f_get_lazy_exe ${GERONIMO_HOME}/bin/startup)" ]]; then
       ${VOLATILE_SU} - ${XYNA_USER} -c $(f_get_lazy_exe ${GERONIMO_HOME}/bin/startup) || {
        attention_msg "Check '${GERONIMO_HOME}/var/log/geronimo.out' for details..."
        f_exit_with_message ${EX_TEMPFAIL} "Unable to start Geronimo. Abort!"
      }
    else
      err_msg "Unable to locate Geronimo installation in '${GERONIMO_HOME}'. Skipping startup..."
    fi
  fi
}

stop_geronimo () {
  CONFIG_SUBSTITUTIONS="${GERONIMO_HOME}/var/config/config-substitutions.properties"
  if [[ -f ${CONFIG_SUBSTITUTIONS} ]]; then
    NAMING_PORT=$(${VOLATILE_SED} -e "s+ = +=+g" "${CONFIG_SUBSTITUTIONS}" | ${VOLATILE_AWK} 'BEGIN { FS="=" } $1 == "NamingPort" {print $2}')
    $(f_get_lazy_exe ${GERONIMO_HOME}/bin/shutdown) --port ${NAMING_PORT} --user ${GERONIMO_USER} --password ${GERONIMO_PASSWORD}
  else
    err_msg "Unable to locate Geronimo installation in '${GERONIMO_HOME}'. Skipping shutdown..."
  fi
}