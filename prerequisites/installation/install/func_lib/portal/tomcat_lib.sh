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

# Portal -> Apache Tomcat library

####################
# ant xml-datei fuer tomcat-task erzeugen
#  Eingabeparameter:
#+   output filename
#+   full/path/to/war.file
#+   [optional] Name der Komponente, z.B. 'OneIP-MPLS-Factory'
#
#  Ausgabe:           keine
#
#  Benoetigte Umgebungsvariablen:
#+ TOMCAT_HOME      Basispfad zur Tomcatinstallation
#+ TOMCAT_USER      Zugangsdaten fuer Tomcat
#+ TOMCAT_PASSWORD  Zugangsdaten fuer Tomcat
#+ TOMCAT_HTTP_PORT Port auf dem Tomcat auf http lauscht
f_write_tomcat_ant_file() {

  local FILE_TO_EDIT="${1}"
  local COMPONENT_WAR_FILE="${2:-ALL.war}"
  local COMPONENT_NAME="${3:-${COMPONENT_WAR_FILE%.war}}"
  local TOMCAT_VERSION=""
  local TOMCAT_URL="manager"
  
  if [[ -z "${FILE_TO_EDIT}" ]]; then
    err_msg "f_write_tomcat_ant_file: No outpufile given"
    return ${EX_PARAMETER}
  fi

  #  Manager URL differs from Tomcat 6 to Tomcat 7
  TOMCAT_VERSION=$(${TOMCAT_HOME}/bin/version.sh | ${VOLATILE_GREP} "number" | ${VOLATILE_AWK} '{print $NF}' | ${VOLATILE_AWK} -F. '{print $1}')
  
  case ${TOMCAT_VERSION} in
      6)  TOMCAT_URL="manager"
    ;;
      7)  TOMCAT_URL="manager/text"
    ;;
      8)  TOMCAT_URL="manager/text"
    ;;
      *)  err_msg "f_write_tomcat_ant_file: Tomcat version '${TOMCAT_VERSION}' is not supported!"
    return
    ;;
  esac


  ${VOLATILE_CAT} > ${FILE_TO_EDIT} << A_HERE_DOCUMENT
 <project name="generated file" default="list" basedir=".">

  <!-- Configure the directory into which the web application is built -->
  <property name="build"    value="/tmp/build"/>

  <!-- Configure the context path for this application -->
  <property name="path"     value="/$(basename "${COMPONENT_NAME}")"/>

  <!-- Configure properties to access the Manager application -->
  <property name="url"      value="http://${TOMCAT_IP_ADDRESS}:${TOMCAT_HTTP_PORT}/${TOMCAT_URL}"/>
  <property name="username" value="${TOMCAT_USER}"/>
  <property name="password" value="${TOMCAT_PASSWORD}"/>
  
  <path id="tomcat.ant.classpath">
    <fileset dir="${TOMCAT_HOME}/lib">
           <include name="catalina-ant.jar"/>
           <include name="tomcat-coyote.jar"/>
           <include name="tomcat-util.jar"/>
           <include name="catalina.jar"/>
    </fileset>
    <fileset dir="${TOMCAT_HOME}/bin">
               <include name="tomcat-juli.jar"/>
    </fileset>
  </path>
    
  <!-- Configure the custom Ant tasks for the Manager application -->
  <taskdef name="deploy"    classname="org.apache.catalina.ant.DeployTask"    classpathref="tomcat.ant.classpath"/>
  <taskdef name="list"      classname="org.apache.catalina.ant.ListTask"      classpathref="tomcat.ant.classpath"/>
  <taskdef name="reload"    classname="org.apache.catalina.ant.ReloadTask"    classpathref="tomcat.ant.classpath"/>
  <taskdef name="resources" classname="org.apache.catalina.ant.ResourcesTask" classpathref="tomcat.ant.classpath"/>
  <taskdef name="start"     classname="org.apache.catalina.ant.StartTask"     classpathref="tomcat.ant.classpath"/>
  <taskdef name="stop"      classname="org.apache.catalina.ant.StopTask"      classpathref="tomcat.ant.classpath"/>
  <taskdef name="undeploy"  classname="org.apache.catalina.ant.UndeployTask"  classpathref="tomcat.ant.classpath"/>
  
    <!-- Executable Targets -->

  <target name="deploy" description="Install web application">
    <deploy url="\${url}" username="\${username}" password="\${password}"
            path="\${path}" war="file:${COMPONENT_WAR_FILE}"/>
  </target>

  <target name="reload" description="Reload web application">
    <reload  url="\${url}" username="\${username}" password="\${password}"
            path="\${path}"/>
  </target>

  <target name="undeploy" description="Remove web application">
    <undeploy url="\${url}" username="\${username}" password="\${password}"
            path="\${path}"/>
  </target>

  <target name="start" description="Start web application">
    <start url="\${url}" username="\${username}" password="\${password}"
            path="\${path}"/>
  </target>
              
  <target name="stop" description="Stop web application">
    <stop url="\${url}" username="\${username}" password="\${password}"
            path="\${path}"/>
  </target>
                           
  <target name="list" description="List web application">
    <list url="\${url}" username="\${username}" password="\${password}"/>               
  </target>

</project>
A_HERE_DOCUMENT

return ${EX_OK}
}
 
               
#  Ein WAR-File wird unter den Namen '${COMPONENT_NAME}' installiert.
#
#  Eingabeparameter:
#+   full/path/to/war.file
#+   [optional] Name der Komponente, z.B. 'OneIP-MPLS-Factory'
#+   [optional] Undeploy [true]
#+   [optional] Deploy   [true]
#
#  Ausgabe:           keine
#
#  Benoetigte Umgebungsvariablen:
#+ TOMCAT_HOME           Basispfad zur Tomcatinstallation
#+ TOMCAT_USER           Zugangsdaten fuer Tomcat
#+ TOMCAT_PASSWORD       Zugangsdaten fuer Tomcat
#+ TOMCAT_HTTP_PORT      Port auf dem Tomcat auf http lauscht
#+ TOMCAT_IP_ADDRESS     IP-Adresse auf der Tomcat lauscht
#+ TOMCAT_DEPLOYER_HOME  Pfad zum Tomcat Client Deployer Package
#
#  Deployment geschieht _nicht_ ueber SSL, falls z.B. das Zertifikat vergessen wurde...
#
f_deploy_war_into_tomcat () {
  local COMPONENT_WAR_FILE="${1:-filename_missing}"
  local COMPONENT_NAME="${2:-${COMPONENT_WAR_FILE%.war}}"
  local BLN_UNDEPLOY="${3:-true}"
  local BLN_DEPLOY="${4:-true}"
  local PRINT_HEADING="${5:-true}"
  local ret_val=0
  
  if  [[ "x${BLN_DEPLOY}" == "xtrue" ]]; then
    if [[ "x${COMPONENT_WAR_FILE}" == "x" || ! -f "${COMPONENT_WAR_FILE}" ]]; then
      err_msg "f_deploy_war_into_tomcat: Unable to work without war-file."
      return ${EX_OSFILE}
    fi
    if [[ "x${PRINT_HEADING}" == "xtrue" ]]; then
      echo -e "\n  * Deploying ${COMPONENT_NAME} into Apache Tomcat"
    fi;
  else
    if  [[ "x${BLN_UNDEPLOY}" == "xtrue" ]]; then
      if [[ "x${PRINT_HEADING}" == "xtrue" ]]; then
        echo -e "\n  * Undeploying ${COMPONENT_NAME} from Apache Tomcat"
      fi;
    fi
  fi  

  INDENTATION="    |   ";
  #  TOMCAT_IP_ADDRESS und TOMCAT_DEPLOYER_HOME muessen mit sinnvollen Werten besetzt sein
  check_ip_address "${TOMCAT_IP_ADDRESS}"
  if [[ $? -eq 0 && -d "${TOMCAT_DEPLOYER_HOME}" ]]; then

    #  (1) Client Deployer Package verwenden, falls vorhanden
    f_check_open_port "${TOMCAT_IP_ADDRESS}" "${TOMCAT_HTTP_PORT}"
    if [[ $? -eq 0 ]]; then
      local TOMCAT_VERSION=$(${VOLATILE_AWK} -F= '$1 == "tomcat.version" {print $2}' "${TOMCAT_DEPLOYER_HOME}/version.txt")
    
      case ${TOMCAT_VERSION} in
        6) TOMCAT_URL="manager"
           ;;
        7) TOMCAT_URL="manager/text"
           ;;
        8) TOMCAT_URL="manager/text"
           ;;
        *) err_msg "f_deploy_war_into_tomcat: Tomcat version '${TOMCAT_VERSION}' is not supported!"
           return
           ;;
      esac

      local FILE_TO_EDIT="${TOMCAT_DEPLOYER_HOME}/build.xml"
      f_write_tomcat_ant_file  ${FILE_TO_EDIT} ${COMPONENT_WAR_FILE} ${COMPONENT_NAME}
      if [[ "x${BLN_UNDEPLOY}" == "xtrue" ]]; then
        echo -e "\n    + Undeploying previous deployment"
        f_ant "${FILE_TO_EDIT}" undeploy
        ret_val=$?
      fi
      if [[ "x${BLN_DEPLOY}" == "xtrue" ]]; then
        echo -e "\n    + Deploying new deployment"
        f_ant "${FILE_TO_EDIT}" deploy
        ret_val=$?
      fi
      ${VOLATILE_RM} -f "${FILE_TO_EDIT}"

    else
      attention_msg "Apache Tomcat on '${TOMCAT_IP_ADDRESS}:${TOMCAT_HTTP_PORT}' is not running."
    fi
  else

    #  (2) Fallback auf die alte Loesung: Lokal deployen
    if [[ ! -d "${TOMCAT_HOME}/webapps" ]]; then
      err_msg "f_deploy_war_into_tomcat: unable to locate Apache Tomcat installation in '${TOMCAT_HOME}'"
      return ${EX_OSFILE}
    fi
      
    f_check_open_port "${HOSTNAME}" "${TOMCAT_HTTP_PORT}"
    if [[ $? -eq 0 ]]; then
      local FILE_TO_EDIT="$(realpath $HOSTNAME)/$(basename ${FILE_TO_DEPLOY} .war)_manage_tomcat.xml"
      ${VOLATILE_MKDIR} -p $(dirname ${FILE_TO_EDIT})
      f_write_tomcat_ant_file ${FILE_TO_EDIT} ${COMPONENT_WAR_FILE} ${COMPONENT_NAME}
    
      if [[ "x${BLN_UNDEPLOY}" == "xtrue" ]]; then
              echo -e "\n    + Undeploying previous deployment"
              f_ant "${FILE_TO_EDIT}" undeploy
              ret_val=$?
      fi
      if [[ "x${BLN_DEPLOY}" == "xtrue" ]]; then
              echo -e "\n    + Deploying new deployment"
              f_ant "${FILE_TO_EDIT}" deploy
              ret_val=$?
      fi
      ${VOLATILE_RM} -f "${FILE_TO_EDIT}"
    else
      attention_msg "Apache Tomcat in '${TOMCAT_HOME}' is not running."
    fi
  fi

  return ${ret_val}
}

#  Eine Application im tomcat unter den Namen '${COMPONENT_NAME}' wird gestoppt.
#
#  Eingabeparameter:
#+   Name der Komponente, z.B. 'OneIP-MPLS-Factory'
#
#  Ausgabe:           keine
#
#  Benoetigte Umgebungsvariablen:
#+ TOMCAT_HOME           Basispfad zur Tomcatinstallation
#+ TOMCAT_USER           Zugangsdaten fuer Tomcat
#+ TOMCAT_PASSWORD       Zugangsdaten fuer Tomcat
#+ TOMCAT_HTTP_PORT      Port auf dem Tomcat auf http lauscht
#+ TOMCAT_IP_ADDRESS     IP-Adresse auf der Tomcat lauscht
#+ TOMCAT_DEPLOYER_HOME  Pfad zum Tomcat Client Deployer Package
#
#  Stop geschieht _nicht_ ueber SSL, falls z.B. das Zertifikat vergessen wurde...
#
f_stop_application_in_tomcat () {
  local COMPONENT_WAR_FILE="${1:-filename_missing}"
  local COMPONENT_NAME="${2}"  
  local PRINT_HEADING="${3:-true}"
  local ret_val=0
  

  if [[ "x${PRINT_HEADING}" == "xtrue" ]]; then
    echo -e "\n  * Stopping ${COMPONENT_NAME} in Apache Tomcat"
  fi;
  

  INDENTATION="    |   ";
  #  TOMCAT_IP_ADDRESS und TOMCAT_DEPLOYER_HOME muessen mit sinnvollen Werten besetzt sein
  check_ip_address "${TOMCAT_IP_ADDRESS}"
  if [[ $? -eq 0 && -d "${TOMCAT_DEPLOYER_HOME}" ]]; then

    #  (1) Client Deployer Package verwenden, falls vorhanden
    f_check_open_port "${TOMCAT_IP_ADDRESS}" "${TOMCAT_HTTP_PORT}"
    if [[ $? -eq 0 ]]; then
      local TOMCAT_VERSION=$(${VOLATILE_AWK} -F= '$1 == "tomcat.version" {print $2}' "${TOMCAT_DEPLOYER_HOME}/version.txt")
    
      case ${TOMCAT_VERSION} in
        6) TOMCAT_URL="manager"
           ;;
        7) TOMCAT_URL="manager/text"
           ;;
        8) TOMCAT_URL="manager/text"
           ;;
        *) err_msg "f_deploy_war_into_tomcat: Tomcat version '${TOMCAT_VERSION}' is not supported!"
           return
           ;;
      esac

      local FILE_TO_EDIT="${TOMCAT_DEPLOYER_HOME}/build.xml"
      f_write_tomcat_ant_file  ${FILE_TO_EDIT} ${COMPONENT_WAR_FILE} ${COMPONENT_NAME}
    
      f_ant "${FILE_TO_EDIT}" stop
      ret_val=$?
      
      ${VOLATILE_RM} -f "${FILE_TO_EDIT}"

    else
      attention_msg "Apache Tomcat on '${TOMCAT_IP_ADDRESS}:${TOMCAT_HTTP_PORT}' is not running."
    fi
  else

    #  (2) Fallback auf die alte Loesung: Lokal deployen
    if [[ ! -d "${TOMCAT_HOME}/webapps" ]]; then
      err_msg "f_deploy_war_into_tomcat: unable to locate Apache Tomcat installation in '${TOMCAT_HOME}'"
      return ${EX_OSFILE}
    fi
      
    f_check_open_port "${HOSTNAME}" "${TOMCAT_HTTP_PORT}"
    if [[ $? -eq 0 ]]; then
      local FILE_TO_EDIT="$(dirname ${COMPONENT_WAR_FILE})/manage_tomcat.xml"
      ${VOLATILE_MKDIR} -p $(dirname ${FILE_TO_EDIT})
      f_write_tomcat_ant_file ${FILE_TO_EDIT} ${COMPONENT_WAR_FILE} ${COMPONENT_NAME}
    
      f_ant "${FILE_TO_EDIT}" stop
      ret_val=$?

      ${VOLATILE_RM} -f "${FILE_TO_EDIT}"
    else
      attention_msg "Apache Tomcat in '${TOMCAT_HOME}' is not running."
    fi
  fi

  return ${ret_val}
}

## TODO in welche lib passt das?
# Ant ausführen, dabei Output einrücken
#Parameter ANT_FILE ANT_TARGET
#Rückgabe ist in globaler Variable ANT_OUTPUT zu finden
f_ant () {
  local ANT_FILE=${1}
  local ANT_TARGET=${2}
  local VOLATILE_ANT=$(get_full_path_to_executable_in_path ant ${ANT_HOME}/bin) #TODO in system_lib.sh aufnehmen!
  local RC;
  ANT_OUTPUT=$(export ANT_HOME="${ANT_HOME}"; ${VOLATILE_ANT} -f ${ANT_FILE} ${ANT_TARGET})
  RC=$?
  if [[ ${RC} == 0 ]] ; then
    if f_selected ${VERBOSE} ; then 
      echo "${INDENTATION}${VOLATILE_ANT} -f ${ANT_FILE} ${ANT_TARGET}";
    fi
    echo "${ANT_OUTPUT}" | sed "s+^+${INDENTATION}+" #Einrücken
  else
    local MSG="${VOLATILE_ANT} -f ${ANT_FILE} ${ANT_TARGET}\nfailed with return code ${RC}:\n\n${ANT_OUTPUT}";
    attention_multi_msg "${MSG}";
  fi;
  
  return ${RC}
}




        
#  Eine application '${COMPONENT_NAME}' stoppen oder starten.
#
#  Eingabeparameter:
#+   Name der Komponente, z.B. 'OneIP-MPLS-Factory'
#+   [optional] Stop  [false]
#+   [optional] Start [false]
#
#  Ausgabe:           keine
#
#  Benoetigte Umgebungsvariablen:
#+ TOMCAT_HOME      Basispfad zur Tomcatinstallation
#+ TOMCAT_USER      Zugangsdaten fuer Tomcat
#+ TOMCAT_PASSWORD  Zugangsdaten fuer Tomcat
#+ TOMCAT_HTTP_PORT Port auf dem Tomcat auf http lauscht
#
#
f_start_or_stop_tomcat () {

  local COMPONENT_NAME="${1}"
  local BLN_STOP="${2:-false}"
  local BLN_START="${3:-false}"
  local ret_val=0
  
  if [[ "x${COMPONENT_NAME}" == "x" ]]; then
    err_msg "f_start_or_stop_tomcat: Unable to work without application name."
    return ${EX_OSFILE}
  fi
  
  if [[ ! -d "${TOMCAT_HOME}/webapps" ]]; then
    err_msg " f_start_or_stop_tomcat: unable to locate Apache Tomcat installation in '${TOMCAT_HOME}'"
    return ${EX_OSFILE}
  fi
  
  f_check_open_port "${HOSTNAME}" "${TOMCAT_HTTP_PORT}"
  if [[ $? -eq 0 ]]; then
      local FILE_TO_EDIT="${TMP_FILE}_manage_tomcat.xml"
      
      ${VOLATILE_MKDIR} -p $(dirname ${FILE_TO_EDIT})
      f_write_tomcat_ant_file ${FILE_TO_EDIT} "" ${COMPONENT_NAME}
   
      if  [[ "x${BLN_STOP}" == "xtrue" ]]; then  
       echo -e "\n* Stopping ${COMPONENT_NAME} in Apache Tomcat"
       f_ant "${FILE_TO_EDIT}" stop
       ret_val=$?
      fi
      if  [[ "x${BLN_START}" == "xtrue" ]]; then
        echo -e "\n* Starting ${COMPONENT_NAME} in Apache Tomcat"
        f_ant "${FILE_TO_EDIT}" start
        ret_val=$?
     fi
      ${VOLATILE_RM} -f "${FILE_TO_EDIT}"
  else
      attention_msg "Apache Tomcat in '${TOMCAT_HOME}' is not running."
  fi

 return ${ret_val}
}


#  Status einer oder aller applications in tomcat ueberpruefen.
#
#  Eingabeparameter:
#+ [optional]  Name der Komponente, z.B. 'OneIP-MPLS-Factory'
#  Ausgabe:           keine
#
#  Benoetigte Umgebungsvariablen:
#+ TOMCAT_HOME      Basispfad zur Tomcatinstallation
#+ TOMCAT_USER      Zugangsdaten fuer Tomcat
#+ TOMCAT_PASSWORD  Zugangsdaten fuer Tomcat
#+ TOMCAT_HTTP_PORT Port auf dem Tomcat auf http lauscht
#
#
f_check_tomcat_application_status () {

  local COMPONENT_NAME="${1}"
  local ret_val=0
  
  if [[ ! -d "${TOMCAT_HOME}/webapps" ]]; then
    err_msg " f_start_or_stop_tomcat: unable to locate Apache Tomcat installation in '${TOMCAT_HOME}'"
    return ${EX_OSFILE}
  fi
  
  f_check_open_port "${HOSTNAME}" "${TOMCAT_HTTP_PORT}"
  if [[ $? -eq 0 ]]; then
      local FILE_TO_EDIT="${TMP_FILE}_manage_tomcat.xml"
      
      ${VOLATILE_MKDIR} -p $(dirname ${FILE_TO_EDIT})
      f_write_tomcat_ant_file ${FILE_TO_EDIT} "" ${COMPONENT_NAME}
   
      if  [[ -n "${COMPONENT_NAME}" ]]; then  
       f_ant "${FILE_TO_EDIT}" list 
       ret_val=$?
       echo "${ANT_OUTPUT}" | ${VOLATILE_GREP} -w ${COMPONENT_NAME} |  ${VOLATILE_AWK} -F: '{print $2}'
       else
        f_ant "${FILE_TO_EDIT}" list
        ret_val=$?
      fi
      ${VOLATILE_RM} -f "${FILE_TO_EDIT}"
  else
      attention_msg "Apache Tomcat in '${TOMCAT_HOME}' is not running."
  fi

 return ${ret_val}
}

etc_initd_files_tomcat () {
  check_target_platform
  echo "SYSTEMD_ENV ${SYSTEMD_ENV}"
  if [[ "x${SYSTEMD_ENV}" == "xtrue" ]] ; then
    f_etc_systemd_files "tomcat" "application/tomcat" \
    "s+TOKEN_INSTALL_DIR+${TOMCAT_HOME}+" \
    "s+TOKEN_PROVIDES_TOMCAT+tomcat_${PRODUCT_INSTANCE}+" \
    "s+TOKEN_XYNA_USER+${XYNA_USER}+" \
    "s+TOKEN_XYNA_GROUP+${XYNA_GROUP}+" \
    "s+TOKEN_JAVA_HOME+${JAVA_HOME}+"
  else 
    f_etc_initd_files "tomcat" "application/tomcat" \
    "s+TOKEN_INSTALL_DIR+${TOMCAT_HOME}+" \
    "s+TOKEN_PROVIDES_TOMCAT+tomcat_${PRODUCT_INSTANCE}+" \
    "s+TOKEN_XYNA_USER+${XYNA_USER}+"
  fi;
}

update_tomcat () {
  if [[ -d ${TOMCAT_HOME} ]]; then
    backup_dir "${TOMCAT_HOME}"
    ${VOLATILE_RM} -rf ${TOMCAT_HOME}
  fi
  install_tomcat
}


f_install_tomcat_deployer () {
    echo -e "\n* Installing Tomcat Client Deployer into '${TOMCAT_DEPLOYER_HOME}'."

    #  Vorkompiliertes Paket entpacken
    local TOMCAT_ZIP=$(ls application/tomcat/apache-tomcat-*-deployer.tar.gz)
    exit_if_not_exists ${TOMCAT_ZIP}
    if [[ -d ${TOMCAT_DEPLOYER_HOME} ]]; then
      echo "    + Replacing existing Tomcat Client Deployer"
      ${VOLATILE_RM} -rf ${TOMCAT_DEPLOYER_HOME}
    fi
    local TOMCAT_PACKAGE=$(basename ${TOMCAT_ZIP} .tar.gz)
    local TOMCAT_VERSION=$(echo ${TOMCAT_PACKAGE} | ${VOLATILE_AWK} -F- '{print $3}' |  ${VOLATILE_AWK} -F. '{print $1}')
    ${VOLATILE_TAR} -xz --directory=$(dirname ${TOMCAT_DEPLOYER_HOME}) -f ${TOMCAT_ZIP}
    ${VOLATILE_MV} $(dirname ${TOMCAT_DEPLOYER_HOME})/${TOMCAT_PACKAGE} ${TOMCAT_DEPLOYER_HOME}
    echo "tomcat.version=${TOMCAT_VERSION}" > ${TOMCAT_DEPLOYER_HOME}/version.txt
 
    #  count lines to find patch position
    local FILE_TO_EDIT="${TOMCAT_DEPLOYER_HOME}/build.xml"
    local NUMBER_OF_LINES=$(${VOLATILE_AWK} 'END {print NR}' "${FILE_TO_EDIT}")
    ${VOLATILE_CAT} > "${TMP_FILE}" << A_HERE_DOCUMENT
$((NUMBER_OF_LINES - 1))a$((NUMBER_OF_LINES)),$((NUMBER_OF_LINES + 2))
>   <target name="list" description="List web application">
>     <list url="\${url}" username="\${username}" password="\${password}"/>               
>   </target>
A_HERE_DOCUMENT
    #  apply the patch
    ${VOLATILE_PATCH} -l -i "${TMP_FILE}" "${FILE_TO_EDIT}"
    ${VOLATILE_RM} -f "${TMP_FILE}"
    
    local USERHOME=$(${VOLATILE_AWK} 'BEGIN {FS=":"} $1 == "'${XYNA_USER}'" {print $6}' "/etc/passwd")
    if [[ -d ${USERHOME} ]]; then
      set_java_environment ${USERHOME}/.bashrc
      set_java_environment ${USERHOME}/.bash_profile
      ${VOLATILE_CHOWN} -R "$(f_generate_chown_parameter)" "${TOMCAT_DEPLOYER_HOME}" "${USERHOME}/.bashrc" "${USERHOME}/.bash_profile"
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

install_tomcat () {
  echo -e "\n* Installing Tomcat into '${TOMCAT_HOME}'."

  PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
  PORT_OFFSET=$(( ($((10#${PRODUCT_INSTANCE})) - 1) * 10 ))

  #  Vorkompiliertes Paket entpacken
  local TOMCAT_ZIP=$(ls application/tomcat/apache-tomcat-*.tar.gz | ${VOLATILE_GREP} -vi deployer)
  exit_if_not_exists ${TOMCAT_ZIP}
  local TOMCAT_PACKAGE=$(basename ${TOMCAT_ZIP} .tar.gz)
  local TOMCAT_VERSION=$(echo ${TOMCAT_PACKAGE} | ${VOLATILE_AWK} -F- '{print $3}' |  ${VOLATILE_AWK} -F. '{print $1"."$2}')
  ${VOLATILE_TAR} -xz --directory=$(dirname ${TOMCAT_HOME}) -f ${TOMCAT_ZIP}
  ${VOLATILE_MV} $(dirname ${TOMCAT_HOME})/${TOMCAT_PACKAGE} ${TOMCAT_HOME}
  echo "tomcat.version=${TOMCAT_VERSION}" > ${TOMCAT_HOME}/version.txt

  #  install shared libraries into tomcat
  for i in $(${VOLATILE_LS} application/tomcat/lib/*)
  do
    install_file ${i} ${TOMCAT_HOME}/lib/.
  done
  if [[ -f "${TOMCAT_HOME}/lib/tomcat-juli.jar" ]]; then
      ${VOLATILE_MV} "${TOMCAT_HOME}/lib/tomcat-juli.jar" "${TOMCAT_HOME}/bin/tomcat-juli.jar"
  fi

  #  Facility local2 verwenden
  FILE_TO_EDIT="${TOMCAT_HOME}/lib/log4j.properties"
  if [[ -f ${FILE_TO_EDIT} ]]; then
    f_replace_in_file "${FILE_TO_EDIT}" \
       "s+^log4j.appender.SYSLOG.Facility.*+log4j.appender.SYSLOG.Facility=LOCAL2+" \
       "s+ TOMCAT + TOMCAT_${PRODUCT_INSTANCE} +g"
  fi
  FILE_TO_EDIT="${TOMCAT_HOME}/lib/log4j2.xml"
  if [[ -f ${FILE_TO_EDIT} ]]; then
     f_replace_in_file "${FILE_TO_EDIT}" \
        "s+ facility=\"LOCAL0\"+ facility=\"LOCAL2\"+" \
        "s+ TOMCAT + TOMCAT_${PRODUCT_INSTANCE} +g"
  fi

  echo "    + enabling SSL"


  ${VOLATILE_CAT} > "${TMP_FILE}" << A_HERE_DOCUMENT
22c22
< <Server port="8005" shutdown="SHUTDOWN">
---
> <Server port="$(( 8006 + ${PORT_OFFSET} ))" shutdown="SHUTDOWN">
67,69c69,75
<     <Connector port="8080" protocol="HTTP/1.1"
<                connectionTimeout="20000"
<                redirectPort="8443" />
---
>     <Connector port="${TOMCAT_HTTP_PORT}" protocol="HTTP/1.1"
>                connectionTimeout="20000"
>                redirectPort="${TOMCAT_SSL_PORT}" 
>                compression="${TOMCAT_COMPRESSION}"
>                compressionMinSize="2048"
>                noCompressionUserAgents="gozilla, traviata"
>                compressableMimeType="text/html,text/xml" />
73,76c79,82
<                port="8080" protocol="HTTP/1.1"
<                connectionTimeout="20000"
<                redirectPort="8443" />
<     -->
---
>                port="${TOMCAT_HTTP_PORT}" protocol="HTTP/1.1"
>                connectionTimeout="20000"
>                redirectPort="${TOMCAT_SSL_PORT}" />
>     -->
A_HERE_DOCUMENT


# server.xml unterscheiden sich bei tomcat8 und tomcat6/7
case ${TOMCAT_VERSION} in
    8.*)  ${VOLATILE_CAT} >> "${TMP_FILE}" << A_HERE_DOCUMENT
81,82c83
<     <!--
<     <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
---
>     <Connector port="${TOMCAT_SSL_PORT}" protocol="org.apache.coyote.http11.Http11NioProtocol"
A_HERE_DOCUMENT
    ;;
      *)  ${VOLATILE_CAT} >> "${TMP_FILE}" << A_HERE_DOCUMENT
81,82c83
<     <!--
<     <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
---
>     <Connector port="${TOMCAT_SSL_PORT}" protocol="HTTP/1.1" SSLEnabled="true"
A_HERE_DOCUMENT
    ;;
  esac

case ${TOMCAT_VERSION} in
    8.5)
 ${VOLATILE_CAT} >> "${TMP_FILE}" << A_HERE_DOCUMENT
92,98c92,102
<                maxThreads="150" SSLEnabled="true">
<         <SSLHostConfig>
<             <Certificate certificateKeystoreFile="conf/localhost-rsa.jks"
<                          type="RSA" />
<         </SSLHostConfig>
<     </Connector>
<     -->
---
>                maxThreads="150" SSLEnabled="true"
>                clientAuth="false" sslProtocol="TLS"
>                ciphers="SSL_RSA_WITH_RC4_128_MD5, SSL_RSA_WITH_RC4_128_SHA, TLS_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_DSS_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA, SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA, SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA"
>                server="XynaTomcat"
>                keystoreFile="${SSL_CERTIFICATE_DIR}/xyna_ssl.pfx"
>                keystorePass="${KEYSTORE_PASS}"
>                keystoreType="PKCS12"
>                compression="${TOMCAT_COMPRESSION}"
>                compressionMinSize="2048"
>                noCompressionUserAgents="gozilla, traviata"
>                compressableMimeType="text/html,text/xml" />
119c123,125
<     <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
---
>     <!--
>     <Connector port="8009" protocol="AJP/1.3" redirectPort="${TOMCAT_SSL_PORT}" />
>     -->
A_HERE_DOCUMENT
     ;;
   *)
 ${VOLATILE_CAT} >> "${TMP_FILE}" << A_HERE_DOCUMENT
84,85c91,100
<                clientAuth="false" sslProtocol="TLS" />
<     -->
---
>                clientAuth="false" sslProtocol="TLS"
>                ciphers="SSL_RSA_WITH_RC4_128_MD5, SSL_RSA_WITH_RC4_128_SHA, TLS_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_DSS_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA, SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA, SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA"
>                server="XynaTomcat"
>                keystoreFile="${SSL_CERTIFICATE_DIR}/xyna_ssl.pfx"
>                keystorePass="${KEYSTORE_PASS}"
>                keystoreType="PKCS12" 
>                compression="${TOMCAT_COMPRESSION}"
>                compressionMinSize="2048"
>                noCompressionUserAgents="gozilla, traviata"
>                compressableMimeType="text/html,text/xml" />
88c101,103
<     <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
---
>     <!--
>     <Connector port="8009" protocol="AJP/1.3" redirectPort="${TOMCAT_SSL_PORT}" />
>     -->
A_HERE_DOCUMENT
     ;;
  esac


  #  apply the patch
  FILE_TO_EDIT="${TOMCAT_HOME}/conf/server.xml"
  backup_file "${FILE_TO_EDIT}"
  ${VOLATILE_PATCH} -l -i "${TMP_FILE}" "${FILE_TO_EDIT}"
  ${VOLATILE_RM} -f "${TMP_FILE}"

  echo "    + enabling user and role"
  FILE_TO_EDIT="${TOMCAT_HOME}/conf/tomcat-users.xml"
  backup_file "${FILE_TO_EDIT}"
  #  count lines to find patch position
  local NUMBER_OF_LINES=$(${VOLATILE_AWK} 'END {print NR}' "${FILE_TO_EDIT}")
  ${VOLATILE_CAT} > "${TMP_FILE}" << A_HERE_DOCUMENT
$((NUMBER_OF_LINES - 1))a$((NUMBER_OF_LINES)),$((NUMBER_OF_LINES + 2))
>   <role rolename="manager-gui"/>
>   <role rolename="manager-script"/>
>   <user username="${TOMCAT_USER}" password="${TOMCAT_PASSWORD}" roles="manager-gui,manager-script"/>
A_HERE_DOCUMENT
  #  apply the patch
  ${VOLATILE_PATCH} -l -i "${TMP_FILE}" "${FILE_TO_EDIT}"
  ${VOLATILE_RM} -f "${TMP_FILE}"


  # Tomcat 8.5 mag serienmaessig kein remote deployment
  if [[ "x${TOMCAT_VERSION}" == "x8.5" ]] ; then
    ${VOLATILE_CAT} > "${TMP_FILE}" << A_HERE_DOCUMENT
20,21c20,21
<   <Valve className="org.apache.catalina.valves.RemoteAddrValve"
<          allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1" />
---
>   <!--Valve className="org.apache.catalina.valves.RemoteAddrValve"
>          allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1" /-->
A_HERE_DOCUMENT
    #  apply the patch
    FILE_TO_EDIT="${TOMCAT_HOME}/webapps/manager/META-INF/context.xml"
    backup_file "${FILE_TO_EDIT}"
    ${VOLATILE_PATCH} -l -i "${TMP_FILE}" "${FILE_TO_EDIT}"
    ${VOLATILE_RM} -f "${TMP_FILE}"
  fi

  #  bugz 8313: tomcat Aenderungen
  FILE_TO_EDIT="${TOMCAT_HOME}/conf/context.xml"
  backup_file "${FILE_TO_EDIT}"
  #  fix problem with no newline at EOF, so that sed does not delete last line on Solaris...
  echo >> "${FILE_TO_EDIT}"
  f_replace_in_file "${FILE_TO_EDIT}" "s+<Context>+<Context cookies='false'>+"

  f_setenv_sh "${TOMCAT_HOME}/bin/setenv.sh"

  local USERHOME=$(${VOLATILE_AWK} 'BEGIN {FS=":"} $1 == "'${XYNA_USER}'" {print $6}' "/etc/passwd")
  if [[ -d ${USERHOME} ]]; then
    set_java_environment ${USERHOME}/.bashrc
    set_java_environment ${USERHOME}/.bash_profile
    ${VOLATILE_CHOWN} -R "$(f_generate_chown_parameter)" "${TOMCAT_HOME}" "${USERHOME}/.bashrc" "${USERHOME}/.bash_profile"
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



start_tomcat () {
  if [[ "x${SYSTEMD_ENV}" == "xtrue" ]] ; then
    local PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})
    systemctl start "tomcat_${PRODUCT_INSTANCE}"
  else
    if [[ -f "$(f_get_lazy_exe ${TOMCAT_HOME}/bin/startup.sh)" ]]; then
      export CATALINA_OPTS="-XX:MaxPermSize=${JAVA_PERMSIZE}m -Xms${TOMCAT_MEMORY}m -Xmx${TOMCAT_MEMORY}m"
      ${VOLATILE_SU} - ${XYNA_USER} -c $(f_get_lazy_exe ${TOMCAT_HOME}/bin/startup.sh) || {
        attention_msg "Check '${TOMCAT_HOME}/logs/catalina.out' for details..."
        f_exit_with_message ${EX_TEMPFAIL} "Unable to start Tomcat. Abort!"
      }
    else
      err_msg "Unable to locate Tomcat installation in '${TOMCAT_HOME}'. Skipping startup..."
    fi
  fi
}

stop_tomcat () {
  if [[ -f $(f_get_lazy_exe ${TOMCAT_HOME}/bin/shutdown.sh) ]]; then
    $(f_get_lazy_exe ${TOMCAT_HOME}/bin/shutdown.sh)
  else
    err_msg "Unable to locate Tomcat installation in '${TOMCAT_HOME}'. Skipping shutdown..."
  fi
}

# Neustart Tomcat
f_restart_tomcat() {  
 echo -e "\n* Restarting Tomcat${PRODUCT_INSTANCE:-"001"}"
 str_CMD=/etc/init.d/tomcat_${PRODUCT_INSTANCE-"001"}
 if [[ ! -x ${str_CMD} ]]; then
   str_CMD=${INSTALL_PREFIX}/etc/init.d/tomcat
 fi
 exit_if_not_exists  ${str_CMD} " - Tomcat not proper installed!"
 ${str_CMD} restart
 return $?
}  
