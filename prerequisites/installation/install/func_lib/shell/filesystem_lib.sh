
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


# Shell -> Filesystem library

#  Skript abbrechen wenn Datei nicht vorhanden ist
#+ Optional ist ein zweiter Parameter, der textuell ausgegeben wird
exit_if_not_exists () {
  if [[ ! -f "${1}" ]]; then
    if [[ -n "${2}" ]]; then attention_msg "${2}"; fi
    f_exit_with_message ${EX_OSFILE} "exit_if_not_exists: Unable to read '${1}'. Abort!"
  fi
}

#  Skript abbrechen wenn Verzeichnis nicht gefunden wurde
#+ Optional ist ein zweiter Parameter, der textuell ausgegeben wird
exit_if_dir_not_found () {
  if [[ ! -d "${1}" ]]; then
    if [[ -n "${2}" ]]; then attention_msg "${2}"; fi
    f_exit_with_message ${EX_OSDIR} "exit_if_dir_not_found: Unable to locate directory '${1}'. Abort!"
  fi
}


#  Kopiert die Quelldatei in das Zielverzeichnis.
#+ Der optionale dritte Parameter setzt die Dateiberechtigungen.
#+ Wird der dritte Parameter angegeben, so muss das Ziel eine Datei sein.
#+ Falls nicht, werden die Dateiberechtigungen auf den
#+ genannten Ordnder angewandt!
#
#  Beispiel:
#    install_file demon/etc/snmpAdapterDemon.properties ${TARGET_DIR}/.
#    install_file demon/bin/snmpAdapterDemon.sh         ${TARGET_DIR}/snmpAdapterDemon.sh 755
#
install_file () {
  if [[ "x" == "x${2}" ]]; then
    echo "Specify target file for installation."
  else
    exit_if_not_exists ${1}
    ${VOLATILE_CP} ${1} ${2}
    if [[ "x" != "x${3}" ]]; then
      ${VOLATILE_CHMOD} ${3} ${2}
    fi
  fi
}


#  Skripte nach '${INSTALL_PREFIX}/bin' kopieren
install_scripts () {
  local TARGET_DIR="${INSTALL_PREFIX}/bin"

  echo -e "\n* Installing scripts to ${TARGET_DIR}"
  if [[ ! -d "${TARGET_DIR}" ]]; then ${VOLATILE_MKDIR} -p "${TARGET_DIR}"; fi
  for i in scripts/*
  do
    echo "    + $(basename "${i}")"
    install_file "${i}" "${TARGET_DIR}/$(basename "${i}")" 755
  done
}


##############################
# Skripte installieren
# Installiere neuere Version im angegeben Verz,
# o 1 : Name inkl Pfad des Skripts, default: ./func_lib.sh
# o 2 : Zielverz.   default: ${XYNA_CACHE_DIR}"
# o 3 : Variablennamen zum Versions-Vgl., default MAGIC_NUMBER_OF_THIS_FUNC_LIB
f_install_script_when_newer () {

  f_set_environment_dir 

  local SRC_FILE="${1:-${THIS_FUNC_LIB_SH}}"
  local FILENAME=$(basename $1)
  local TARGET_DIR="${2:-${XYNA_CACHE_DIR}}"
  local MARKER="${3:-MAGIC_NUMBER_OF_THIS_FUNC_LIB}"
  local ret_val=0  
  local TARGET_FILE="${TARGET_DIR}/${FILENAME}"                 
                   
  if [[ ! -f ${SRC_FILE} ]];then
     echo "!! ${SRC_FILE} does not exist !!"
     return ${EX_OSFILE}
  fi      
  
  TARGET_MAGIC_NUMBER=0
  SRC_MAGIC_NUMBER=0
  SRC_MAGIC_NUMBER=$(f_get_magic_number ${SRC_FILE} ${MARKER})
  
  if [[ -r ${TARGET_FILE} ]]; then
    TARGET_MAGIC_NUMBER=$(f_get_magic_number ${TARGET_FILE} ${MARKER})
  fi
  
  if [[ ${SRC_MAGIC_NUMBER} -gt  ${TARGET_MAGIC_NUMBER} ]]; then                                 
    ${VOLATILE_MKDIR} -p ${TARGET_DIR}   
    if [[ -f ${TARGET_FILE} ]]; then
      ${VOLATILE_CHMOD} u+w ${TARGET_FILE}
      if [[ -w ${TARGET_FILE} ]]; then
        echo "* Updating '${FILENAME}' from version '${TARGET_MAGIC_NUMBER}' to '${SRC_MAGIC_NUMBER}'"
      else
        echo "* Skipped - Not enought rights to update '${TARGET_FILE}'"
        return ${EX_NOPERM}
      fi         
     else
      echo "* Installing '${FILENAME}' with version '${SRC_MAGIC_NUMBER}'"
    fi
     ${VOLATILE_CP} -p ${SRC_FILE} ${TARGET_FILE} 
     ret_val=$? 
  fi
  
  return ${ret_val}
}  



#  Sicherungskopie einer Datei erstellen
#+ Dazu ${TIMESTAMP} an den Dateinamen anhaengen
backup_file () {
  if [[ -f "${1}" && ! -f "${1}_${TIMESTAMP}" ]]; then
    ${VOLATILE_CP} -p "${1}" "${1}_${TIMESTAMP}"
  fi
}

#  Sicherungskopie eines Verzeichnis erstellen
#+ Dazu ${TIMESTAMP} an den Verzeichnisnamen anhaengen
#Aufruf a) backup_dir <Dir>     
#          legt Backup von <Dir> in <Dir>_${TIMESTAMP} an
#       b) backup_dir <BaseDir> <RelDir> [<Mode>] [<Output>]
#          legt Backup von <BaseDir>/<RelDir> in <BaseDir>/backup/${TIMESTAMP}/<RelDir> an, 
#          <Mode> kann "optional" oder "mandatory" (default) sein:
#          bei "mandatory" wird mit Fehler abgebrochen, falls <BaseDir>/<RelDir> nicht existiert
#          <Output> kann "silent" (default) oder "all" sein: weniger Ausgaben
backup_dir () {
  local STR_TARGET_DIR;
  local STR_BACKUP_DIR;
  local STR_BACKUP_EXISTENCE;
  local STR_OUTPUT;
  if [[ $# = 1 ]] ; then
    STR_TARGET_DIR="${1}"
    STR_BACKUP_DIR="${STR_TARGET_DIR}_${TIMESTAMP}"
    STR_BACKUP_EXISTENCE="mandatory";
    STR_OUTPUT="all";
  else 
    STR_TARGET_DIR=${1}/${2}
    STR_BACKUP_DIR="${1}/backup/${TIMESTAMP}/${2}"
    STR_BACKUP_EXISTENCE="${3:-mandatory}"
    STR_OUTPUT="${4:-silent}";
  fi;
  local STR_BACKUP_BASE=$(dirname ${STR_BACKUP_DIR})
  
  if [[ ! -d "${STR_TARGET_DIR}" ]]; then
    if [[ "${STR_BACKUP_EXISTENCE}" == "optional" ]] ; then
      # Existenz des zu backuppenden Verzeichnis ist nicht erforderlich 
      return 0;
    else 
      err_msg "Unable to locate directory '${STR_TARGET_DIR}'. Skipping..."
      return 1
    fi
  else
    if [[ ${STR_OUTPUT} != "silent" ]] ; then
      echo -e "\n* Creating backup of '${STR_TARGET_DIR}'"
    fi;
    
    ${VOLATILE_MKDIR} -p ${STR_BACKUP_BASE}
    debug_msg "Calculating size of '${STR_TARGET_DIR}' ..."
    local STR_SIZE_OF_TARGET_DIR=$(${VOLATILE_DU} -sk "${STR_TARGET_DIR}" | ${VOLATILE_AWK} '{ print $1 }')
    debug_msg "... done - size is: ${STR_SIZE_OF_TARGET_DIR} KB"

    debug_msg "Checking if enough space is left on device ..."
    #  Bei 'df' ist die Anzahl der noch verfuegbare Speicher in Spalte 4:
    local STR_FREE_SPACE_ON_DEVICE=$(${VOLATILE_DF} "${STR_DF_OPTIONS}" "${STR_BACKUP_BASE}" | ${VOLATILE_AWK} 'NR > 1 { print $4 }')
    local STR_MIN_FREE_SPACE_NEEDED="$(echo "${STR_SIZE_OF_TARGET_DIR} 1.01 * p" | LANG="C" ${VOLATILE_DC} | ${VOLATILE_AWK} 'BEGIN { FS="." } { print $1 }')"
    local BOOL_ENOUGH_FREE_SPACE="false"
    if [[ "${STR_MIN_FREE_SPACE_NEEDED}" -lt "${STR_FREE_SPACE_ON_DEVICE}" ]]; then
      BOOL_ENOUGH_FREE_SPACE="true"
    fi
    debug_msg "... done - enough space left: ${BOOL_ENOUGH_FREE_SPACE} (${STR_MIN_FREE_SPACE_NEEDED} < ${STR_FREE_SPACE_ON_DEVICE})"

    if [[ "${BOOL_ENOUGH_FREE_SPACE:-false}" == "true" ]]; then
      echo "    + backing up '${STR_TARGET_DIR}' as '${STR_BACKUP_DIR}'"
      ${VOLATILE_CP} -rp "${STR_TARGET_DIR}" "${STR_BACKUP_DIR}"
    else
      f_exit_with_message ${EX_NOSPACE} "backup_dir: Unable to backup '${STR_TARGET_DIR}' as '${STR_BACKUP_DIR}'. Not enough space!"
    fi
  fi
}

#  Sicherungskopie einer Datei (nach backup_file) wiederherstellen
restore_file () {
  if [[ -f "${1}_${TIMESTAMP}" ]]; then
    ${VOLATILE_CP} -p "${1}_${TIMESTAMP}" "${1}" 
  fi
}

#  Sicherungskopie einer Datei (nach backup_dir) wiederherstellen
#Aufruf restore_file_from_dir <BaseDir> <RelDir> <FileName>
#kopiert  <BaseDir>/backup/${TIMESTAMP}/<RelDir>/<FileName> nach <BaseDir>/<RelDir>/<FileName>
restore_file_from_dir () {
  local BASEDIR="${1}"
  local RELDIR="${2}"
  local FILENAME="${3}"
  local TARGET="${BASEDIR}/${RELDIR}/${FILENAME}"
  local BACKUP="${BASEDIR}/backup/${TIMESTAMP}/${RELDIR}/${FILENAME}"
  if [[ -f "${BACKUP}" ]]; then
    ${VOLATILE_CP} -p "${BACKUP}" "${TARGET}" 
  fi
}

###################
#  Wenn ein chown aufgerufen werden soll, dann muss der User und die Gruppe existieren
#+ Also z.B. 'chown xyna:xyna'.
#+ Existiert die Gruppe nicht, dann ist ein 'chown xyna' ausreichend.
#+ Diese Funktion generiert den passenden String, also 'xyna' oder 'xyna:xyna' ...
#
#  Eingabeparameter:
#+  1 = Username [optional], default ist $XYNA_USER, falls nichts angegeben wird
#
f_generate_chown_parameter () {
    local ret_val="${1:-${XYNA_USER}}"

    if [[ $(${VOLATILE_AWK} -F: 'BEGIN { i=0 } $1 == "'${XYNA_GROUP}'" { i=i+1 } END {print i}' "/etc/group") -eq 1 ]]; then
  ret_val="${ret_val}:${XYNA_GROUP}"
    fi

    echo "${ret_val}"
}


##############################
# Set recursive permission on files and folders
# o 1 : Parent directory or file
# o 2 : Berechtigung fuer Dateien, default 660
# o 3 : Berechtigung fuer Verzeichnisse  default 775
f_set_bulk_permission() {
 
 local ROOT_DIR=${1} 
 if [[ "x${ROOT_DIR}" == "x" ]]; then
   attention_msg "f_set_permission: no directory where given"
   return ${EX_PARAMETER}
 fi

 local dliste="$( ${VOLATILE_FIND} ${ROOT_DIR} -type d)"
 local fliste="$( ${VOLATILE_FIND} ${ROOT_DIR} -type f)"     
 local dright="${2:-775}"
 local fright="${3:-660}" 
 if [[ "x${dliste}" != "x" ]]; then
    ${VOLATILE_CHMOD} -f ${dright} ${dliste}
 fi
 if [[ "x${fliste}" != "x" ]]; then
   ${VOLATILE_CHMOD} -f ${fright} ${fliste}
 fi
   
 return 0           
}           

##############################
# Set group and permission in xyna-etc-diretory                           
f_set_etc_permission() {
  
    local BENUTZER_GROUP=$(${VOLATILE_GROUPS} 2>/dev/null | ${VOLATILE_AWK} '{print $1}')
    local BENUTZER=$(ps -o "user=" -p $$ | sed -e "s+ ++g")
    local ETC_DIR=${XYNA_ENVIRONMENT_DIR}
    local ETC_ROOT_DIR=$(dirname ${XYNA_ENVIRONMENT_DIR})
    local VAR_DIR=${XYNA_CACHE_DIR}
    local ETC_GROUP="root"
    local ETC_FILE_PERM="666"
    local ETC_DIR_PERM="777"
    local BLN_HAS_GIPGROUP=0
    local BLN_USE_GIPGROUP=0
        
    if [[ -r "/etc/group" ]]; then
      BLN_HAS_GIPGROUP=$(${VOLATILE_GREP} -c "${GIPGROUP}" /etc/group)
    else
      attention_mgs "Cannot read '/etc/group'"
    fi
    
    #Wenn Benutzer selbst in gipgroup ist
    BLN_USE_GIPGROUP=$(${VOLATILE_GROUPS} 2>/dev/null | ${VOLATILE_GREP} -c ${GIPGROUP})
        
    #oder wenn root-Benutzer und gipgroup existiert     
    if [[ "x${BENUTZER}" == "xroot" && ${BLN_HAS_GIPGROUP} -ne 0 ]]; then
      #dann benutze gipgroup als gruppe
      BLN_USE_GIPGROUP=1
    fi
       
    # aber nur falls Environmen-Daten nicht im Home-Directory liegen
    if [[ "x${ETC_ROOT_DIR}" == "x${HOME}" ]]; then
      ## dann nehme Benutzergruppe
      ETC_GROUP=${BENUTZER_GROUP}
      ETC_FILE_PERM="664"
      ETC_DIR_PERM="775"
    else 
      # ansonsten wenn moeglich gipgroup 
      if [[ ${BLN_USE_GIPGROUP} -gt 0 ]]; then
        ETC_GROUP=${GIPGROUP}
        ETC_FILE_PERM="660"
        ETC_DIR_PERM="775"       
      fi
    fi
      
    # Setze Berechtigungen   
    if [[ -d "${ETC_DIR}" ]]; then
      debug_msg "Checking permission in '${ETC_DIR}'"
      f_set_bulk_permission "${ETC_DIR}" "${ETC_DIR_PERM}" "${ETC_FILE_PERM}"
      ${VOLATILE_CHGRP} -Rf "${ETC_GROUP}" "${ETC_DIR}"
    fi
    if [[ -d "${VAR_DIR}" ]]; then
      debug_msg "Checking permission in '${VAR_DIR}'"
      f_set_bulk_permission "${VAR_DIR}" "${ETC_DIR_PERM}" "${ETC_FILE_PERM}"
      ${VOLATILE_CHGRP} -Rf "${ETC_GROUP}" "${VAR_DIR}"
    fi
    
  return 0
}

#  Tokens in Datei ersetzen
#
#  Eingabeparameter:
#o   1 = Quell-Dateiname
#o   2 = Ziel-Dateiname
#o   3-n = beliebige sed-Anweisung
# Beispiel: f_replace_token source.dat dest.dat "s+TOKEN_A+hallo+" "s+TOKEN_C+a b+"
f_replace_token() {
  local SRC_FILE=$1;
  local DEST_FILE=$2;
  ${VOLATILE_CP} -p -f ${SRC_FILE} ${TMP_FILE}
  ${VOLATILE_CP} -p -f ${TMP_FILE} ${DEST_FILE} #DEST_FILE anlegen, damit Attribute erhalten bleiben
  for ((i=3; i<=$#; i++ )) ; do 
    ${VOLATILE_SED} -e "${!i}" ${TMP_FILE} > ${DEST_FILE} #da DEST_FILE bereits existiert, bleiben Attribute erhalten
    ${VOLATILE_CP} -p -f ${DEST_FILE} ${TMP_FILE} #kein Move, damit DEST_FILE erhalten bleibt
  done;
  ${VOLATILE_MV} ${TMP_FILE} ${DEST_FILE}
  ${VOLATILE_RM} -f ${TMP_FILE}
}

#  Tokens in Datei ersetzen
#
#  Eingabeparameter:
#o   1 = Dateiname
#o   2-n = beliebige sed-Anweisung
# Beispiel: f_replace_in_file file.dat "s+TOKEN_A+hallo+" "s+TOKEN_C+a b+"
f_replace_in_file() {
  local FILE_TO_EDIT=$1;
  ${VOLATILE_CP} -p -f ${FILE_TO_EDIT} ${TMP_FILE}
  for ((i=2; i<=$#; i++ )) ; do 
    ${VOLATILE_SED} -e "${!i}" ${TMP_FILE} > ${FILE_TO_EDIT} #da FILE_TO_EDIT bereits existiert, bleiben Attribute erhalten
    ${VOLATILE_CP} -p -f ${FILE_TO_EDIT} ${TMP_FILE} #kein Move, damit FILE_TO_EDIT erhalten bleibt
  done;
  ${VOLATILE_MV} ${TMP_FILE} ${FILE_TO_EDIT}
  ${VOLATILE_RM} -f ${TMP_FILE}
}

#  Einfügen in eine Datei vor einem Token
#
#  Eingabeparameter:
#o   1 = Dateiname
#o   2 = Token
#o   3 = einzufügender Text
# Beispiel: f_insert_in_file_before_line "${FILE_TO_EDIT}" "</xynapropertiesTable>"  ${PROPERTY}
f_insert_in_file_before_line () {
  local FILE_TO_EDIT=$1;
  ${VOLATILE_CP} -p -f ${FILE_TO_EDIT} ${TMP_FILE}
  #da FILE_TO_EDIT bereits existiert, bleiben Attribute erhalten
${VOLATILE_AWK} -v pattern="${2}" -v insert="${3}" '$0~pattern {print insert} {print $0}'  ${TMP_FILE} > ${FILE_TO_EDIT}
  ${VOLATILE_RM} -f ${TMP_FILE}
}

######################################################################
#  Prüfung, ob mindestens eines der übergebenen Files existieren 
#
#  Eingabeparameter
#o   File-Liste
#  Rückgabeparameter
#    boolean
# Beispiel: if f_files_exist ${FILE} ${FILE2} ; then ...
######################################################################
f_files_exist () {
  for fn in "$@" ; do 
    if [ -e $fn ] ; then
      return 0;
    fi;
  done; 
  return 1; #nichts gefunden
}

######################################################################
#  Prüfung, wie häufig der angegebene String in einer Datei auftritt
#
#  Eingabeparameter
#  o  String
#  o  File 
#  Rückgabeparameter
#    Anzahl
# Beispiel: if f_files_exist ${FILE} ${FILE2} ; then ...
######################################################################
f_count_occurrencies_in_file () {
  echo $(${VOLATILE_GREP} -c "$1" "$2")
}

#Anlegen eines Verzeichnisses
# Testet, ob Verzeichnis danach existiert
f_mkdir () {
  mkdir -p "${1}" 2>/dev/null
  if [[ ! -d "${1}" ]] ; then
    #  Anlegen des Verzeichnisses hat nicht geklappt
    err_msg "Unable to create directory '${1}/'. Abort!"
    exit 91
  fi
}
