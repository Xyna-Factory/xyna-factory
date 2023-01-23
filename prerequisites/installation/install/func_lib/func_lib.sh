#!/bin/bash -x

# ---------------------------------------------------
#  Copyright GIP AG 2013
#  (http://www.gip.com)
#
#  Hechtsheimer Str. 35-37
#  55131 Mainz
# ---------------------------------------------------
#  $Revision: 242598 $
#  $Date: 2018-11-23 17:05:36 +0100 (Fr, 23 Nov 2018) $
# ---------------------------------------------------
# Basis-Bibliothek fuer Xyna
#
#o Funktionen, die von den anderen Installationsskripten verwendet werden.
#o Properties-Datei kann ausgelesen werden.

RELEASE_NAME="TOKEN_RELEASE_NAME"
RELEASE_NUMBER="TOKEN_RELEASE_NUMBER"


##############################################################################
#  Diese Zahl anpassen, wenn Aenderung vorgenommen wurden
#  Vorsicht: Es muss eine Zahl und kein "String" sein!
##############################################################################
MAGIC_NUMBER_OF_THIS_FUNC_LIB=1082

#Zu Debug-Zwecke, 'exit' fangen
#trap '$SHELL $HOME/dasEnde' 0

######################################################################
#  Einige Funktionen sind in Bibliotheken ausgelagert.
#+ Diese Bibliotheken importieren
#
#  Eingabeparameter:
#o   Dateiname der zu importierenden Bibliothek
#o   Pfad zur Datei, ansonsten wird im aktuellen Verz. 
#    und unter /etc/opt/xyna/var bzw. /etc/xyna/var danach gesucht.
f_load_library () {
  
  local SOURCE_FILE="${1}"
  local SOUCE_DIR="${2}"
  local THIS_DIR="$(dirname ${BASH_SOURCE[0]})"
  local XYNA_VAR_DIR="/etc/opt/xyna/var"
  local XYNA_VAR_DIR_OLD="/etc/xyna/var"
  local HOME_VAR_DIR="$HOME/var"
  local retval=0
   
  if [[ "x${SOUCE_DIR}" == "x" ]]; then
    # Suche in '/etc/xyna/var'
    if [[ -r "${XYNA_VAR_DIR_OLD}/${SOURCE_FILE}" ]]; then
      SOUCE_DIR=${XYNA_VAR_DIR_OLD}   
    fi
     # Suche in '/etc/opt/xyna/var'
    if [[ -r "${XYNA_VAR_DIR}/${SOURCE_FILE}" ]]; then
        SOUCE_DIR=${XYNA_VAR_DIR}
    fi
    # Suche in '$HOME/var'
    if [[ -r ${HOME_VAR_DIR} && "x$HOME" != "x" ]]; then
      SOUCE_DIR=${HOME_VAR_DIR}
    fi
    # Suche in '.'
    if [[ -r "${THIS_DIR}/${SOURCE_FILE}" ]]; then
      SOUCE_DIR=${THIS_DIR}  
    fi
  fi
  if [[ ! -r "${SOUCE_DIR}/${SOURCE_FILE}" ]]; then
    echo "Unable to import functions from '${SOUCE_DIR}/${SOURCE_FILE}'. Abort!"; exit 99;
    retval=72;
  else
    #echo -e "* Loading '${SOUCE_DIR}/${SOURCE_FILE}'"
    source "${SOUCE_DIR}/${SOURCE_FILE}"
    retval=$?
    if [[ ${retval} -gt 0 ]]; then
      echo "ERROR loading '${SOUCE_DIR}/${SOURCE_FILE}'"
    fi
  fi
  
  return ${retval}
}

f_load_library  "processing/processing_lib.sh"
f_load_library  "processing/application_lib.sh"
f_load_library  "processing/persistence_lib.sh"
f_load_library  "processing/version_lib.sh"
f_load_library  "processing/cluster_lib.sh"
f_load_library  "processing/installation_lib.sh"

f_load_library  "portal/portal_lib.sh"
f_load_library  "portal/oc4j_lib.sh"
f_load_library  "portal/weblogic_lib.sh"

f_load_library  "os/os_lib.sh"
f_load_library  "os/syslog_lib.sh"
f_load_library  "os/network_lib.sh"
f_load_library  "os/system_lib.sh"
f_load_library  "os/ssh_lib.sh"
f_load_library  "os/time_lib.sh"
f_load_library  "os/snmp_lib.sh"

f_load_library  "warehouse/warehouse_lib.sh"
f_load_library  "warehouse/mysql_lib.sh"
f_load_library  "warehouse/oracle_lib.sh"

f_load_library  "shell/string_lib.sh"
f_load_library  "shell/messages_lib.sh"
f_load_library  "shell/filesystem_lib.sh"
f_load_library  "shell/list_lib.sh"
f_load_library  "shell/properties_lib.sh"
f_load_library  "shell/properties_map_lib.sh"
f_load_library  "shell/components_lib.sh"
f_load_library  "shell/user_lib.sh"
f_load_library  "shell/shell_lib.sh"



##############################
# Magic Number einer Datei bestimmen
# o 1 : Datei aus der der Marker ausgelesen werden soll, default: diese datei  
# o 2 : Der Name des Version-Marker in der Datei, default "MAGIC_NUMBER_OF_THIS_FUNC_LIB" 
# Wenn kein Dateiname uebergeben wird, 
# dann wird die "MAGIC_NUMBER_OF_THIS_FUNC_LIB" dieser Datei ausgegeben  
f_get_magic_number() {
 local SRC_FILE="${1}"
 local VERSION_MARKER="${2:-MAGIC_NUMBER_OF_THIS_FUNC_LIB}"
 
 if [[ -z "${SRC_FILE}" ]]; then
   echo ${MAGIC_NUMBER_OF_THIS_FUNC_LIB:-0}
 else
   get_property ${VERSION_MARKER} ${SRC_FILE}
   echo ${CURRENT_PROPERTY} 
 fi
 return 0
}

####################
#Bibliothek installieren
f_install_func_lib() {
  local INSTALL_DIR=${1}
  # Dieses Skript auf dem System aktualisieren, falls neuer
  f_install_script_when_newer "${THIS_FUNC_LIB_SH}" "${INSTALL_DIR}"
}

####################
#Aufraeumen
f_cleanup_tmp_file () { 
  if [[ -f "${TMP_FILE}" ]]; then 
    ${VOLATILE_RM} -f "${TMP_FILE}"
  fi
}

####################
#Interupt behandeln
f_handle_interrupt() {
 trap - INT TERM EXIT
 f_cleanup_tmp_file
 kill $$
}

####################
#Exit behandeln
f_handle_exit() {
 trap - INT TERM EXIT 
 f_cleanup_tmp_file
 #  Der Exit-Code darf hier nicht ueberschrieben werden!!
 exit
}


#############################################################################
# Hauptprogramm
THIS_FUNC_LIB_SH=${BASH_SOURCE[0]}
umask 0022
load_settings_from_cache
check_target_platform
set_platform_dependent_properties

TIMESTAMP="$(${VOLATILE_DATE} +"%Y%m%d_%H%M%S")"
CALLER="${0}"
if [[ "x${CALLER}" == "x-bash" ]];then
  CALLER="bash_func_lib.sh"
else
  CALLER=$(basename $CALLER)
fi
TMP_FILE="$(${VOLATILE_MKTEMP} -t "${CALLER}.XXXXXXXX")"

trap f_handle_interrupt INT TERM 
trap f_handle_exit EXIT 
debug_msg "'${THIS_FUNC_LIB_SH}' loaded"
#  EOF
