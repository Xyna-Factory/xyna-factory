#!/bin/bash

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2023 Xyna GmbH, Germany
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


#
#  Diese Datei wird von install_prerequisites.sh geladen.
#  Alle Funktionen befinden sich hier.

f_read_properties () {
  
  f_read_host_properties   
  #  Create next instance, if instance number '-i' is 'NEW'
  if [[ "x${INSTANCE_NUMBER}" == "xNEW" ]]; then
    if [[ ${BLACK_EDITION_INSTANCES} < ${XYNA_INSTANCES_MAX} ]]; then
      let BLACK_EDITION_INSTANCES++
      INSTANCE_NUMBER="${BLACK_EDITION_INSTANCES}"
      #  save the new value when script starts working. 'dry-run' should not change the value
    else
      echo "Error: More than ${XYNA_INSTANCES_MAX} instances are not supported."
      exit ${EX_PARAMETER}
    fi
    if [[ "x${DRY_RUN}" == "xtrue" ]]; then exit; fi
  fi
  convert_properties
    
  f_read_product_properties
}

parse_commandline_arguments () {
  
  BLN_FORCE_INSTALLATION="false"
  #kmq
  while getopts ":nhHabcdefi:lprsuvwxyzABCDFILPRSUWXYZ" OPTION
  do
    if [[ "x${OPTARG:0:1}" == "x-" ]]; then DISPLAY_USAGE="true"; fi
    case ${OPTION} in
	n) DRY_RUN="true";;
	a) COMPONENT_INSTALLATION_FOLDER="true"
	   COMPONENT_SSL_CERTIFICATE="true"
	   COMPONENT_DEPLOYER="true"
	   COMPONENT_SYSLOG="true"
	   COMPONENT_XYNAUSER="true" 
	   COMPONENT_SCRIPTS="true"
	   COMPONENT_FIREWALL="true"
	   COMPONENT_SNMPD="true"
	   COMPONENT_SSH="true"
	   COMPONENT_LIMITS="true"
	   COMPONENT_TIME="true"
	   COMPONENT_INITD_XYNA="true"
           COMPONENT_CHECK_SELINUX="true";;
        b) COMPONENT_CHECK_SELINUX="true";;
	c) COMPONENT_SSL_CERTIFICATE="true";;
	d) COMPONENT_DEPLOYER="true";;
	e) BLN_FORCE_INSTALLATION="true";;
	f) COMPONENT_INSTALLATION_FOLDER="true";;
	i) export INSTANCE_NUMBER="${OPTARG}";;
	l) COMPONENT_LIMITS="true";;
	s) COMPONENT_SYSLOG="true";;
	p) COMPONENT_SNMPD="true";;
	r) COMPONENT_SSH="true";;
	u) COMPONENT_XYNAUSER="true";;
	v) VERBOSE="true"
       DEBUG="1";;
	w) COMPONENT_FIREWALL="true";;
	x) COMPONENT_INITD_XYNA="true";;
	y) COMPONENT_NETWORK_AVAILABILITY_DEMON="true";;
	z) COMPONENT_TIME="true";;
	A) COMPONENT_INSTALLATION_FOLDER="false"
	   COMPONENT_SSL_CERTIFICATE="false"
	   COMPONENT_DEPLOYER="false"
	   COMPONENT_SYSLOG="false"
	   COMPONENT_XYNAUSER="false"
	   COMPONENT_SCRIPTS="false"
	   COMPONENT_FIREWALL="false"
	   COMPONENT_SSH="false"
	   COMPONENT_SNMPD="false"
	   COMPONENT_LIMITS="false"
	   COMPONENT_TIME="false"
	   COMPONENT_INITD_XYNA="false"
           COMPONENT_CHECK_SELINUX="false";;
        B) COMPONENT_CHECK_SELINUX="false";;
	C) COMPONENT_SSL_CERTIFICATE="false";;
	D) COMPONENT_DEPLOYER="false";;
	F) COMPONENT_INSTALLATION_FOLDER="false";;
	I) COMPONENT_SCRIPTS="true";;
	L) COMPONENT_LIMITS="false";;
	S) COMPONENT_SYSLOG="false";;
	P) COMPONENT_SNMPD="false";;
	R) COMPONENT_SSH="false";;
	U) COMPONENT_XYNAUSER="false";;
	W) COMPONENT_FIREWALL="false";;
	X) COMPONENT_INITD_XYNA="false";;
	Y) COMPONENT_NETWORK_AVAILABILITY_DEMON="false";;
	Z) COMPONENT_TIME="false";;
	*) DISPLAY_USAGE="true";;
    esac
  done
}


#  print all variables for debugging, then exit
debug_variables () {
  if [[ "${DRY_RUN:-false}" == "true" ]]; then
    echo
    echo "-- Selected components --"
    echo " instance_number       : ${INSTANCE_NUMBER}"
    echo " installation folder   : ${COMPONENT_INSTALLATION_FOLDER:-false}"
    echo " firewall              : ${COMPONENT_FIREWALL:-false}"
    echo " SSH                   : ${COMPONENT_SSH:-false}"
    echo " remote deployment tool: ${COMPONENT_DEPLOYER:-false}"
    echo " unprivileged user     : ${COMPONENT_XYNAUSER:-false}"
    echo " syslog and logrotation: ${COMPONENT_SYSLOG:-false}"
    echo " limits                : ${COMPONENT_LIMITS:-false}"
    echo " SSL certificate       : ${COMPONENT_SSL_CERTIFICATE:-false}"
    echo " scripts               : ${COMPONENT_SCRIPTS:-false}"
    echo " snmpd                 : ${COMPONENT_SNMPD:-false}"
    echo " ntp                   : ${COMPONENT_TIME:-false}"
    echo " initd/xyna-script     : ${COMPONENT_INITD_XYNA:-false}"
	echo " network-availability-demon: ${COMPONENT_NETWORK_AVAILABILITY_DEMON:-false}"
    echo " check SELinux/AppArmor: ${COMPONENT_CHECK_SELINUX:-false}"
    echo
    echo "-- Addons    --"
  fi
}

display_usage () {
  ${VOLATILE_CAT} << A_HERE_DOCUMENT

  usage: $(basename "$0") -nacfgjstuACFGIJSTU -i instance

-n    dry-run, display the evaluated commandline parameters
-e    force, do the installation even some of system requirements are not fullfiled (ONLY for experts!!!)
-v    verbose mode, print additional debug output
-i    specify instancenumber or 'NEW' to create next instance
      default is 1; 'NEW' is only needed for instances starting at 2
-I    install scripts into '\${INSTALL_PREFIX}/bin'
-aA   include / exclude all components
-bB   [don't] check SELinux/AppArmor
-cC   [don't] install SSL certificate
-dD   [don't] install remote deployment tools
-fF   [don't] create installation folder
-lL   [don't] set limits
-pP   [don't] configure SNMP-Daemon
-rR   [don't] configure SSH-Daemon und SSH-Client
-sS   [don't] configure syslog and logrotation
-uU   [don't] create unprivileged user
-wW   [don't] configure firewall settings
-xX   [don't] create /etc/init.d-script for xynafactory
-zZ   [don't] configure NTP-Client and time zone

Extra options (not included in -aA)
-yY   [don't] create /etc/init.d-script and respawning for network-availability-demon


* lower-case letters mean to include the specific component
* UPPER-CASE letters mean to exclude the specific component
* subsequent arguments override prior arguments

Examples:

(1) "$(basename "$0")" -a -n
      dry-run: install nothing, but the properties file is created anyway

      This step is useful if you want to create a default properties
      file that you can edit to suit your needs.

(2) "$(basename "$0")" -s
      install only setup syslog and logrotation

A_HERE_DOCUMENT
}


###########################
# Variablen Ueberpruefen
f_check_parameters () {
  local STR_WARNING=""
  
  f_check_system_before_installation
  if [[ $? -gt 0 ]]; then
     f_add_to_error_buffer "! System requirement for installation are not fulfilled. See above\n"                   
     write_settings_to_cache
  fi
  
  echo -e "\n* Checking if all parameters are valid:"
  
  #  Check, if instance number '-i' in the allowed range
  if [[ ( 1 -le ${INSTANCE_NUMBER} && ${INSTANCE_NUMBER} -le ${BLACK_EDITION_INSTANCES} ) || "x${INSTANCE_NUMBER}" == "xNEW" ]]; then
    #  nichts machen, alles bestens
    :
  else
    #f_add_to_error_buffer "Instance number '-i' is not in the allowed range [1..${BLACK_EDITION_INSTANCES}].\n"
    BLACK_EDITION_INSTANCES=${INSTANCE_NUMBER}
  fi
   
  if [[ "x" != "x${STR_WARNING}" ]]; then
    echo  " ..... WARNING"
    attention_msg  "Parameters are generating the following warning(s): "
    echo -e "\n${STR_WARNING}"
  fi
  
  f_has_error_in_buffer
  if [[ $? -gt 0 ]]; then
    err_msg "Parameters are invalid. Reason(s) follow..."
    f_get_error_buffer
    echo -e "\n"
    if [[ "x${BLN_FORCE_INSTALLATION}" == "xtrue" ]]; then
       echo -n "Are you sure you want to continue the installation and ignore all errors above (yes/NO) ? "
       read answer
       if [[ "x${answer}" != "xyes" ]]; then
         exit ${EX_PARAMETER}
       fi
    else
      exit ${EX_PARAMETER}
    fi
  else
    echo  " ..... OK"
  fi
  
  f_clear_error_buffer
}


#Uberpruefe System vor Beginn der Installation
f_check_system_before_installation () {
  
  local failed=0
  echo -e "\n* Checking if system requirements are fulfilled:"

      
  local result=17
 
 #Check installation hardware
 echo -en "\n  * Checking installation hardware is 'x86_64'. Current Value is '${INSTALLATION_HARDWARE}'"
 if [[ "${INSTALLATION_HARDWARE}" == "x86_64" || "${INSTALLATION_HARDWARE}" == "i86pc_64" ]]; then
   f_ok
 else
   f_failed; failed=1
 fi 
 
 #Check RAM
 echo -en "\n  * Checking main memory (RAM) is minimum 1024 MB. Current Value is '${ALL_MEM_IN_MB:-0} MB'"
 
 if [[ ${ALL_MEM_IN_MB:0} -gt 1023 ]]; then
   f_ok
 else
   f_failed; failed=1
 fi
  
  #Check free disk space
  local STR_TARGET_DIR="${INSTALL_PREFIX}"
  while [[ ! -d "${STR_TARGET_DIR}" ]]; do
    STR_TARGET_DIR="$(dirname ${STR_TARGET_DIR})"
  done
    
  local STR_FREE_SPACE_ON_DEVICE=$(${VOLATILE_DF} "${STR_DF_OPTIONS}" "${STR_TARGET_DIR}" | ${VOLATILE_AWK} 'NR > 1 { print $4 }')
  STR_FREE_SPACE_ON_DEVICE=$(echo "${STR_FREE_SPACE_ON_DEVICE} 1024 / p" | ${VOLATILE_DC})
  local STR_MIN_FREE_SPACE_NEEDED=1024 #MB
  echo -en "\n  * Checking free disk space for installation in '${STR_TARGET_DIR}' is minimum ${STR_MIN_FREE_SPACE_NEEDED} MB. Current Value is: ${STR_FREE_SPACE_ON_DEVICE:-0} MB"
  #  Bei 'df' ist die Anzahl der noch verfuegbare Speicher in Spalte 4:

  if [[ ${STR_MIN_FREE_SPACE_NEEDED} -lt ${STR_FREE_SPACE_ON_DEVICE:-0} ]]; then
    f_ok
  else
    f_failed; failed=1
  fi
    
  #Check for SELinux
 if [[ "x${COMPONENT_CHECK_SELINUX}" == "xtrue" ]]; then
  if [[ "x${INSTALLATION_PLATFORM}" != "xsolaris"  ]]; then
    if [[ "x${INSTALLATION_PLATFORM}" == "xsles"  ]]; then
      echo -en "\n  * Checking AppArmor is disabled" 
      VOLATILE_APPARMOR=$(which apparmor_status 2>/dev/null)
      #  search "well-known" paths if nothing found in $PATH
      if [[ "x${VOLATILE_APPARMOR}" == "x" ]]; then
        for i in /bin /sbin /usr/bin /usr/sbin ;do
          if [[ -x "${i}/apparmor_status" ]]; then
            VOLATILE_APPARMOR="${i}/apparmor_status"
            break
          fi
        done
      fi
      if [[ "x${VOLATILE_APPARMOR}" != "x" ]]; then
        ${VOLATILE_APPARMOR} 1>/dev/null 2/dev/null
        result=$?
        # Upon exiting, apparmor_status will set its return value to the following values:
        # 0   if apparmor is enabled and policy is loaded.
        # 1   if apparmor is not enabled/loaded.
        # 2   if apparmor is enabled but no policy is loaded.
        # 3   if the apparmor control files aren't available under /sys/kernel/security/.
        # 4   if the user running the script doesn't have enough privileges to read the apparmor control files.
      fi
    else
      echo -en "\n  * Checking SELinux is disabled" 
      VOLATILE_SELINUX=$(which selinuxenabled 2>/dev/null)
      #  search "well-known" paths if nothing found in $PATH
      if [[ "x${VOLATILE_SELINUX}" == "x" ]]; then
        for i in /bin /sbin /usr/bin /usr/sbin ;do
          if [[ -x "${i}/selinuxenabled" ]]; then
            VOLATILE_SELINUX="${i}/selinuxenabled"
            break
          fi
        done
      fi
      if [[ "x${VOLATILE_SELINUX}" != "x" ]]; then
        ${VOLATILE_SELINUX} 1>/dev/null 2>/dev/null
        result=$?
        # 'selinuxenabled' Indicates whether SELinux is enabled or disabled.
        # It exits with status 0 if SELinux is enabled and 1 if it is not enabled.
      fi
    fi
    if [[ ${result:-17} -eq 0 ]]; then
      f_failed; failed=1
    else
      f_ok
    fi
  fi
 fi
  
 #Check for JAVA
  echo -en "\n  * Checking environment variable 'JAVA_HOME' is set"
  if [[ -n "${JAVA_HOME}" ]]; then
    f_ok
    BIN_PATH="bin"
    LIB_PATH="lib"
    echo -en "\n  * Checking program 'java' is available at 'JAVA_HOME=${JAVA_HOME}/${BIN_PATH}'"
    VOLATILE_JAVA=${JAVA_HOME}/${BIN_PATH}/java
    if [[ -x ${VOLATILE_JAVA} ]]; then
      f_ok
    else
      f_failed; failed=1
    fi
    echo -en "\n  * Checking program 'jar' is available at '${JAVA_HOME}/${BIN_PATH}'"
    VOLATILE_JAR=${JAVA_HOME}/${BIN_PATH}/jar
    if [[ -x ${VOLATILE_JAR} ]]; then
      f_ok
    else
      f_failed; failed=1
    fi
  else
    f_failed; failed=1
  fi
  
  if [[ "x${COMPONENT_SNMPD}"               == "xtrue" ]]; then
    
    local SNMPD_CMD="/etc/init.d/snmpd"
    local SNMPD_TEST_CMD="/etc/init.d/snmpd status"
    if [[ "x${INSTALLATION_PLATFORM}" == "xsolaris" ]]; then
      SNMPD_CMD="/usr/local/sbin/snmpd"
      SNMPD_TEST_CMD="/usr/local/sbin/snmpd -v"
	fi  
	if [[ "x${INSTALLATION_PLATFORM}" == "xoracle" ]]; then
		SNMPD_CMD="snmpd"
		SNMPD_TEST_CMD="systemctl status snmpd"
    fi
	if [[ "x${INSTALLATION_PLATFORM}" == "xrhel" ]]; then
		SNMPD_CMD="snmpd"
		SNMPD_TEST_CMD="systemctl status snmpd"
    fi
	
	
    echo -en "\n  * Checking SNMP-Deamon ('${SNMPD_CMD}') is installed and running"
    ${SNMPD_TEST_CMD} 1>/dev/null 2>/dev/null
    result=$?
    if [[ ${result} -gt 0 ]]; then
      f_failed; failed=1
    else
      f_ok
    fi
 fi 
 
 if [[ "x${COMPONENT_TIME}"                == "xtrue" ]]; then
    local  _TEST_CMD=""
   case ${INSTALLATION_PLATFORM} in
     sles)    NTP_CMD="/etc/init.d/ntp"; NTP_TEST_CMD="${NTP_CMD} status";;
     rhel)    NTP_CMD="ntpd"; NTP_TEST_CMD="systemctl status ntpd";;
     oracle)  NTP_CMD="ntpd"; NTP_TEST_CMD="systemctl status ntpd";;
     ubuntu)  NTP_CMD="ntpd"; NTP_TEST_CMD="systemctl status ntp";;
     debian)  NTP_CMD="svcs -o STATE -H ntp"; NTP_TEST_CMD="${NTP_CMD} 2>/dev/null | ${VOLATILE_GREP} -cvi disabled  1>/dev/null";;
     solaris) NTP_CMD="svcadm restart ntp";         NTP_TEST_CMD="svcadm refresh ntp";;
   esac  
     echo -en "\n  * Checking NTP-Deamon ('${NTP_CMD}')  is installed and running"
    ${NTP_TEST_CMD}  1>/dev/null 2>/dev/null
    result=$?
    if [[ ${result} -gt 0 ]]; then
      f_failed; failed=1
    else
      f_ok
    fi
 fi
 echo -en "\n  * Checking that additional required programms like 'grep', 'awk', 'openssl' are available on this system"
 local STR_MISSING_COMMANDS=$(${VOLATILE_CAT} ${FUNC_LIB_CACHE_FILE} | ${VOLATILE_GREP} "^VOLATILE_" |  ${VOLATILE_GREP} "not_found_XXX" | ${VOLATILE_AWK} -F_ '{print $4}')
 if [[ -n ${STR_MISSING_COMMANDS} ]]; then
   f_failed; failed=1
   echo "!!! The following programs are missing!!!"
    echo ${STR_MISSING_COMMANDS}
   echo "!!!!!!!!!!!!!!!!!!!!!!!"
   write_settings_to_cache
 else
   f_ok
 fi

 return ${failed}
}


f_configure_firewall () {
  # Removed cause all ports are just geronimo-specific
  # f_configure_firewall_service "application_server" "TCP" "${GERONIMO_RMI_PORT}" "${GERONIMO_JMX_PORT}" "${GERONIMO_HTTP_PORT}" "${GERONIMO_SSL_PORT}"
  f_configure_firewall_service "snmpd"              "UDP" "161" "162"

  #  Interfaces sind per default in der externen Zone
  f_configure_firewall_zone "EXT" application_server snmpd
}

f_install_initd_xyna () {
  echo -e "\n  * Installation of etc/init.d-script for xynafactory";
  f_etc_initd_xyna_files;
}

f_install_network_availability_demon () {
  echo -e "\n  * Installation of network availability demon";
  f_etc_initd_network_availability_demon_files;
  f_etc_respawn_network_availability_demon;
}

# Set group and permission in etc-opt-xyna-directory
f_set_etc_opt_xyna_permission() {
  local PROP_FILE_HOST=$(get_properties_filename "host")
  local PROP_FILE_COMPONENT=$(get_properties_filename "component")
  local PROP_FILE_PRODUCT=$(get_properties_filename "product" "${INSTANCE_NUMBER}")
  local ETC_DIR=${XYNA_ENVIRONMENT_DIR}
  local VAR_DIR=${XYNA_CACHE_DIR}
  local ETC_ROOT_DIR=$(dirname ${XYNA_ENVIRONMENT_DIR})
  
  #Alle Dateien sollen GIPGROUP gehören
  if ! check_if_group_exists ${GIPGROUP} ; then
    ${VOLATILE_CHGRP} -Rf "${GIPGROUP}" "${ETC_ROOT_DIR}"
  fi
   
  ${VOLATILE_CHMOD} -f 775 ${ETC_DIR} ${VAR_DIR}
  
  ${VOLATILE_CHMOD} -f 640 ${PROP_FILE_HOST}
  ${VOLATILE_CHMOD} -f 660 ${PROP_FILE_COMPONENT}
  ${VOLATILE_CHMOD} -f 640 ${PROP_FILE_PRODUCT}
  
  if ! check_if_user_exists ${XYNA_USER} ; then
    ${VOLATILE_CHOWN} ${XYNA_USER} ${PROP_FILE_PRODUCT}
  fi
}

#  EOF

