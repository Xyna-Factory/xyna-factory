
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



# OS -> System/Distribution library


#  Nur auf unterstuetzten Plattformen installieren
check_target_platform () {
  export INSTALLATION_HARDWARE=$(${VOLATILE_UNAME} -m)
  case $(${VOLATILE_UNAME}) in
    Linux)
      local TARGET_FILE="/etc/issue"
      exit_if_not_exists ${TARGET_FILE}
      local i;
      for i in sles debian oracle rhel centos ubuntu; do
        local TOKEN_ETC_ISSUE;
        case "${i}" in
          sles) TOKEN_ETC_ISSUE="SUSE Linux Enterprise Server";;
          debian) TOKEN_ETC_ISSUE="Debian GNU/Linux";;
          oracle)
            TOKEN_ETC_ISSUE="Oracle Linux Server"
            if [[ -f "/etc/oracle-release" ]]; then
              TARGET_FILE="/etc/oracle-release"
            fi
            ;;
          rhel)
            TOKEN_ETC_ISSUE="Red Hat Enterprise Linux Server"
            if [[ -f "/etc/redhat-release" ]]; then
              TARGET_FILE="/etc/redhat-release"
            fi
            ;;
          ubuntu) TOKEN_ETC_ISSUE="Ubuntu"
            if [[ -f "/etc/lsb-release" ]]; then
              TARGET_FILE="/etc/lsb-release"
            else
              local VOLATILE_LSB_RELEASE=$(get_full_path_to_executable lsb_release)
              TARGET_FILE="/tmp/lsb-release"
              ${VOLATILE_LSB_RELEASE} > ${TARGET_FILE}
              if [[ $(${VOLATILE_WC} -l ${TARGET_FILE}) -lt 2 ]]; then
                 TARGET_FILE="/etc/issue"
              fi
            fi
            ;;
          centos)
            TOKEN_ETC_ISSUE="CentOS Linux"
            if [[ -f "/etc/redhat-release" ]]; then
              TARGET_FILE="/etc/redhat-release"
            fi
            if [[ -f "/etc/centos-release" ]]; then
              TARGET_FILE="/etc/centos-release"
            fi
            ;;
          *)
            ;;
        esac
        if [[ $(${VOLATILE_GREP} -ci "${TOKEN_ETC_ISSUE}" "${TARGET_FILE}") -gt 0 ]]; then
          export INSTALLATION_PLATFORM="${i}"
          INSTALLATION_PLATFORM_VERSION=$(check_target_version "${i}" ${TARGET_FILE})
          set_systemd ${INSTALLATION_PLATFORM} ${INSTALLATION_PLATFORM_VERSION}
          export INSTALLATION_PLATFORM_VERSION=${INSTALLATION_PLATFORM_VERSION}
          return;
        fi
      done

      # Fallback: Debian 11
      INSTALLATION_PLATFORM="debian"
      INSTALLATION_PLATFORM_VERSION="11"
      ;;
    *)  f_exit_with_message ${EX_UNSUPPORTED} "check_target_platform: Platform '$(${VOLATILE_UNAME})' is not supported. Abort!";;
  esac
}

set_systemd(){
  local PLATFORM="$1";
  local PLATFORM_VERSION="$2"
  case "${PLATFORM}" in
    rhel|oracle|centos)
      case ${INSTALLATION_PLATFORM_VERSION} in
        5.7|5.9|6.*)
          SYSTEMD_ENV="false"
          ;;
        *)
          SYSTEMD_ENV="true"
          ;;
      esac;
      ;;
    debian|sles)
      SYSTEMD_ENV="false"
      ;;
    ubuntu)
      SYSTEMD_ENV="true"
      ;;
  esac
}

check_target_version () {
  local PLATFORM="$1";
  local TARGET_FILE=$2;
  case "${PLATFORM}" in
    sles)
      if [[ $(${VOLATILE_GREP} -ci "SUSE Linux Enterprise Server 11 SP1" "${TARGET_FILE}") -eq 1 ]]; then
        echo "11.1";
        return 0;
      fi
      if [[ $(${VOLATILE_GREP} -ci "SUSE Linux Enterprise Server 11 SP2" "${TARGET_FILE}") -eq 1 ]]; then
        echo "11.2";
        return 0;
      fi
      echo "11"
      return 0
      ;;
    rhel)
      local INSTALLATION_PLATFORM_VERSION=$(${VOLATILE_CAT} ${TARGET_FILE} | ${VOLATILE_AWK} '$1=="Red" { print $7 }');
      echo ${INSTALLATION_PLATFORM_VERSION}
      ;;
    debian)
      cat /etc/debian_version
      return 0
      ;;
    oracle)
      local INSTALLATION_PLATFORM_VERSION=$(${VOLATILE_CAT} ${TARGET_FILE} | ${VOLATILE_AWK} '$1=="Oracle" { print $5 }');
      echo ${INSTALLATION_PLATFORM_VERSION}
      ;;
    ubuntu)
      local INSTALLATION_PLATFORM_VERSION
      if [[ ${TARGET_FILE} =~ "lsb-release" ]]; then
        INSTALLATION_PLATFORM_VERSION=$(${VOLATILE_AWK} 'BEGIN{FS="="} $1=="DISTRIB_RELEASE" { print $2 }' ${TARGET_FILE});
      else
        INSTALLATION_PLATFORM_VERSION=$(${VOLATILE_AWK} '$1=="Ubuntu" { print $2 }' ${TARGET_FILE});
      fi
      echo ${INSTALLATION_PLATFORM_VERSION}
      ;;
    centos)
      local INSTALLATION_PLATFORM_VERSION=$(${VOLATILE_CAT} ${TARGET_FILE} | ${VOLATILE_AWK} '$1=="CentOS" { print $4 }');
      echo ${INSTALLATION_PLATFORM_VERSION}
      ;;
  esac
}




set_platform_dependent_properties () {
  case ${INSTALLATION_PLATFORM} in
    sles|rhel|oracle|centos)
      USERHOME_PREFIX="/home"
      PACKAGE_INSTALLER=$(get_full_path_to_executable rpm)
      ALL_MEM_IN_KB="$(${VOLATILE_AWK} '/^MemTotal/ {print $2}' /proc/meminfo)"
      ALL_MEM_IN_MB="$(echo "${ALL_MEM_IN_KB} 1024 / p" | ${VOLATILE_DC})"
      STR_FQDN="$(hostname --fqdn)"
      STR_DF_OPTIONS="-Pk"
      ;;
    debian)
      USERHOME_PREFIX="/home"
      PACKAGE_INSTALLER=$(get_full_path_to_executable aptitude)
      ALL_MEM_IN_KB="$(${VOLATILE_AWK} '/^MemTotal/ {print $2}' /proc/meminfo)"
      ALL_MEM_IN_MB="$(echo "${ALL_MEM_IN_KB} 1024 / p" | ${VOLATILE_DC})"
      STR_FQDN="$(hostname --fqdn)"
      STR_DF_OPTIONS="-Pk"
      ;;
    ubuntu)
      USERHOME_PREFIX="/home"
      PACKAGE_INSTALLER=$(get_full_path_to_executable apt)
      ALL_MEM_IN_KB="$(${VOLATILE_AWK} '/^MemTotal/ {print $2}' /proc/meminfo)"
      ALL_MEM_IN_MB="$(echo "${ALL_MEM_IN_KB} 1024 / p" | ${VOLATILE_DC})"
      STR_FQDN="$(hostname --fqdn)"
      STR_DF_OPTIONS="-Pk"
      VOLATILE_NETCAT=$(get_full_path_to_executable nc.traditional)
      ;;
    solaris)
      USERHOME_PREFIX="/export/home"
      PACKAGE_INSTALLER=$(get_full_path_to_executable pkgadd)
      ALL_MEM_IN_MB=$(/usr/sbin/prtconf | ${VOLATILE_AWK} '/^Memory/ { print $3 }')
      VOLATILE_TAR=$(get_full_path_to_executable gtar)
      VOLATILE_AWK=$(get_full_path_to_executable nawk)
      VOLATILE_GREP=$(get_full_path_to_executable ggrep)
      VOLATILE_MD5="$(get_full_path_to_executable digest) -a md5"
      VOLATILE_ID="/usr/xpg4/bin/id"
      STR_FQDN="$(${VOLATILE_UNAME} -n)"
      STR_DF_OPTIONS="-k"
      TARGET_FILE="/etc/resolv.conf"
      if [[ -f "${TARGET_FILE}" ]]; then
        STR_FQDN="${STR_FQDN}$(${VOLATILE_AWK} '$1 == "domain" {printf ".%s", $2}' "${TARGET_FILE}")"
      fi
      #Dummy für besseres Syntax-Highlighting"
      ;;
    *) f_exit_with_message ${EX_UNSUPPORTED} "set_platform_dependent_properties: Platform '${INSTALLATION_PLATFORM}' is not supported.";;
  esac

  TOMCAT_MEMORY="$(echo "${ALL_MEM_IN_MB} 4 / p" | ${VOLATILE_DC})" # What has this to do with tomcat?
  if [[ ${TOMCAT_MEMORY} -lt 256  ]]; then TOMCAT_MEMORY="256" ; fi
  if [[ ${TOMCAT_MEMORY} -gt 2048 ]]; then TOMCAT_MEMORY="2048"; fi

  JAVA_PERMSIZE="$(echo "${TOMCAT_MEMORY} 4 / p" | ${VOLATILE_DC})"
  if [[ ${JAVA_PERMSIZE} -lt 128  ]]; then JAVA_PERMSIZE="128" ; fi
  if [[ ${JAVA_PERMSIZE} -gt 512  ]]; then JAVA_PERMSIZE="512" ; fi
}


#  Der volle Pfad zu einem Executable wird ermittelt.
#Gesucht wird dabei nur in den übergebenen Pfaden, nicht in $PATH.
#  Eingabeparameter:
#o   1 = Name des Executables, z.B. 'ant'
#o   2-n Pfade, z.B ${ANT_HOME}/bin
#  Rueckgabewert:
#o   Voller Pfad zum Executable
get_full_path_to_executable_in_path () {
  local FULL_PATH_TO_EXECUTABLE=$(get_full_path_to_executable_in_path_internal $@)
  print_full_path_to_executable "${FULL_PATH_TO_EXECUTABLE}" ${1}
}

get_full_path_to_executable_in_path_internal () {
  local EXECUTABLE="${1}"
  local path;
  for path in "${@:2}" ; do 
    if [[ -f "${path}/${EXECUTABLE}" ]]; then
      echo "${path}/${EXECUTABLE}"
      break;
    fi
  done
}

print_full_path_to_executable () {
  local FULL_PATH_TO_EXECUTABLE=$1
  if [[ -z ${FULL_PATH_TO_EXECUTABLE} ]]; then
    #no executable found
    if [[ $# == 2 ]] ; then
      echo "!! Unable to locate executable '${2}'. Abort!" >&2
      echo "XXX_command_${2}_not_found_XXX"
    else 
      local EXECUTABLES="${@:2}";
      echo "!! Unable to locate executables '${EXECUTABLES}'. Abort!" >&2
      echo "XXX_command_${EXECUTABLES// /_}_not_found_XXX" 
    fi; 
    #möglich für Bug 22964:   kill -s TERM $$
    exit 99
  else
    # TODO warum wird hier ein readlink benötigt?
    echo "$(__readlink ${FULL_PATH_TO_EXECUTABLE})"
  fi
}


#  Der volle Pfad zu einem Executable wird ermittelt.
#+ Falls auf $PATH nichts gefunden wird, werden einige
#+ "well-known" Pfade durchsucht.
#+ Wird kein Executable ermittelt, so wird das weitere Skript abgebrochen!
#
#  Eingabeparameter:
#o   1 = Name des Executables, z.B. 'awk'
#
#  Rueckgabewert:
#o   Voller Pfad zum Executable
get_full_path_to_executable () {
  if (( $# == 0 )) ; then 
    echo "Unable to work without a executable name. Abort!" >&2
    echo "XXX_input_needed_XXX"
    exit 95
  fi
  local exe;
  FULL_PATH_TO_EXECUTABLE=""
  for exe in "${@}" ; do
    #  search $PATH
    FULL_PATH_TO_EXECUTABLE=$(which ${exe} 2>/dev/null)
    #  Unter Solaris liefert which die Fehlermeldungen auf stdout
    #+ anstatt auf stderr. Daher die Woerter der Ausgabe zaehlen:
    #+ Bei mehr als einem Wort ist es eine Fehlerausgabe, der Befehl wurde nicht gefunden.
    if [[ $(echo "${FULL_PATH_TO_EXECUTABLE}" | wc -w | awk '{print $1}') -ne 1 ]]; then
      FULL_PATH_TO_EXECUTABLE=""
    fi
    if [[ -n ${FULL_PATH_TO_EXECUTABLE} ]]; then
      break;
    fi
  done
  
  if [[ -z ${FULL_PATH_TO_EXECUTABLE} ]]; then
    #  search "well-known" paths if nothing found in $PATH
    local SEARCH_PATH="/usr/java/latest/bin /sbin /usr/sbin /usr/sfw/bin /usr/xpg4/bin/ ${ORACLE_HOME}/opmn/bin ${ORACLE_HOME}/bin ${ORACLE_HOME}"
    for exe in "${@}" ; do
      FULL_PATH_TO_EXECUTABLE=$(get_full_path_to_executable_in_path_internal ${exe} ${SEARCH_PATH})
      if [[ $? = 0 ]] ; then
        break;
      else
        FULL_PATH_TO_EXECUTABLE="";
     fi;
    done
  fi
  print_full_path_to_executable "${FULL_PATH_TO_EXECUTABLE}" "${@}"
}

#  Es gibt ein Programm namens 'readlink'. Dieses loest alle Symlinks auf
#+ und gibt den wirklichen Namen des Programms zurueck.
#
#  Es kann nicht vorausgesetzt werden, dass das Programm auf allen
#+ Betriebssystemen existiert. Daher hier ein Rewrite als BASH-Funktion.
#
#  Eingabeparameter:
#o   1 = Name des Executables, z.B. '/usr/bin/java'
#o   2 = Rekursionszaehler, falls Links im Kreis gesetzt sind
#
#  Rueckgabewert:
#o   Voller Pfad zum Executable ohne Link
__readlink () {
HERE="${PWD}"
EXECUTABLE="${1}"
RECURSION_COUNTER="${2:-0}"
if [[ "x" == "x${EXECUTABLE}" ]]; then
  echo "Unable to work without a executable name. Abort!" >&2
  echo "XXX_input_needed_XXX"
  exit 95
fi

if [[ ${RECURSION_COUNTER} -le 30 ]]; then
  #  Weiter, falls die Rekursionstiefe noch o.k. ist...

  if [[ -L "${EXECUTABLE}" ]]; then
    #  Ist's ein Link, dann diesem folgen. ...
    link_target=$(LANG=C ls -l "${EXECUTABLE}" | awk '{print $NF}')

    #  relative Pfade muessen korrekt aufgeloest werden. Daher den Pfaden schrittweise folgen.
    #
    #  Wieso schrittweise? Relative und absolute Pfade funktionieren nur aus dem Ursprungsverzeichnis heraus korrekt.
    cd "$(dirname "${EXECUTABLE}")"
    cd "$(dirname "${link_target}")"

    #  ... Vorsicht - Rekursion
    ret_val=$(__readlink "${PWD}/$(basename "${link_target}")" "$(( ${RECURSION_COUNTER} + 1 ))")
  else
    #  Kein Link, dann Ergebnis ausgeben
    ret_val="${EXECUTABLE}"
  fi
else
  #  Maximale Rekursionstiefe erreicht - Notbremse ziehen
  echo "XXX_max_recursion_depth_reached_XXX"
fi

#  Umgebung wiederherstellen
cd "${HERE}"
echo "${ret_val}"
}

#  bugz 10895: Kommandozeile ist langsam
#  Das Ermitteln der absoluten Pfade zu den einzelnen Executables
#+ ist sehr langsam.
#
#  Sind die Pfade einmal ermittelt, so werden die Einstellungen
#+ in einem Cache gespeichert.
#
#  Im Cache wird eine "magic number" gespeichert. Mit dieser wird
#+ erkannt, ob der Cache erneuert werden muss.
#
#  Der Cache ist fuer alle User freigegeben, so dass im Multiuser-
#+ betrieb der Cache von allen ueberschrieben werden kann.
load_settings_from_cache () {
  
  f_set_environment_dir 
  #  Cache-File im /tmp ist nicht gut, da dieses die Berechtigung +t hat.
  #+ $VOLATILE stehen zu diesem Zeitpunkt noch nicht zur Verfuegung!
  if [[ ! -d "${XYNA_CACHE_DIR}" ]]; then mkdir -p "${XYNA_CACHE_DIR}"; fi
  if [[ "x$(ps -o "user=" -p $$ | sed -e "s+ ++g")" == "xroot" ]]; then 
     chmod 777 "${XYNA_CACHE_DIR}";
  fi
  
  FUNC_LIB_CACHE_FILE="${XYNA_CACHE_DIR}/xyna_func_lib_cache.sh"

  if [[ -f "${FUNC_LIB_CACHE_FILE}" ]]; then
    #  Ist der Cache vorhanden, dann diesen laden
    source "${FUNC_LIB_CACHE_FILE}"

    #  Ist der Cache veraltet, dann einen neuen schreiben
    if [[ $(f_compare_versions "${MAGIC_NUMBER_OF_THIS_FUNC_LIB}" "${MAGIC_NUMBER_OF_CACHE_FILE}") -gt 0 ]]; then
      write_settings_to_cache
    fi
  else
    #  Kein Cache, dann einen erzeugen
    write_settings_to_cache
    ${VOLATILE_CHMOD} 666 "${FUNC_LIB_CACHE_FILE}"
  fi
}

#  Neuen Cache schreiben
write_settings_to_cache () { 
  debug_msg "* Updating cache in '$(basename "${FUNC_LIB_CACHE_FILE}")'."
  get_volatile_settings
  echo "MAGIC_NUMBER_OF_CACHE_FILE=${MAGIC_NUMBER_OF_THIS_FUNC_LIB}" > "${FUNC_LIB_CACHE_FILE}"
  set | ${VOLATILE_GREP} "^VOLATILE_" >> "${FUNC_LIB_CACHE_FILE}" 
}


#  Volle Pfade zu benoetigten Programmen ermitteln
get_volatile_settings () {
  ##############################################################################
  ##  Achtung: MAGIC_NUMBER_OF_THIS_FUNC_LIB in func_lib.sh anpassen          ##
  ##############################################################################
  VOLATILE_CP=$(get_full_path_to_executable cp)
  VOLATILE_DC=$(get_full_path_to_executable dc)
  VOLATILE_DF=$(get_full_path_to_executable df)
  VOLATILE_DU=$(get_full_path_to_executable du)
  VOLATILE_ID=$(get_full_path_to_executable id)
  VOLATILE_LS=$(get_full_path_to_executable ls)
  VOLATILE_LN=$(get_full_path_to_executable ln)
  VOLATILE_MV=$(get_full_path_to_executable mv)
  VOLATILE_PS=$(get_full_path_to_executable ps)
  VOLATILE_RM=$(get_full_path_to_executable rm)
  VOLATILE_SU=$(get_full_path_to_executable su)
  VOLATILE_TR=$(get_full_path_to_executable tr)
  VOLATILE_AWK=$(get_full_path_to_executable awk)
  VOLATILE_CAT=$(get_full_path_to_executable cat)
  VOLATILE_MD5=$(get_full_path_to_executable md5sum)
  VOLATILE_PWD=$(get_full_path_to_executable pwd)
  VOLATILE_SED=$(get_full_path_to_executable sed)
  VOLATILE_TAR=$(get_full_path_to_executable tar)
  VOLATILE_ZIP=$(get_full_path_to_executable zip)
  VOLATILE_DATE=$(get_full_path_to_executable date)
  VOLATILE_DIFF=$(get_full_path_to_executable diff)
  VOLATILE_FIND=$(get_full_path_to_executable find)
  VOLATILE_FOLD=$(get_full_path_to_executable fold)
  VOLATILE_GREP=$(get_full_path_to_executable grep)
  VOLATILE_HEAD=$(get_full_path_to_executable head)
  VOLATILE_PERL=$(get_full_path_to_executable perl)
  VOLATILE_SORT=$(get_full_path_to_executable sort)
  VOLATILE_CHGRP=$(get_full_path_to_executable chgrp)
  VOLATILE_CHMOD=$(get_full_path_to_executable chmod)
  VOLATILE_CHOWN=$(get_full_path_to_executable chown)
  VOLATILE_ICONV=$(get_full_path_to_executable iconv)
  VOLATILE_MKDIR=$(get_full_path_to_executable mkdir)
  VOLATILE_PATCH=$(get_full_path_to_executable patch)
  VOLATILE_TOUCH=$(get_full_path_to_executable touch)
  VOLATILE_UNAME=$(get_full_path_to_executable uname)
  VOLATILE_UNZIP=$(get_full_path_to_executable unzip)
  VOLATILE_GROUPS=$(get_full_path_to_executable groups)
  VOLATILE_MKTEMP=$(get_full_path_to_executable mktemp)
  VOLATILE_NETCAT=$(get_full_path_to_executable netcat nc)
  if [[ "x1" == "x$(${VOLATILE_NETCAT} -4 -v 127.0.0.1 9999 2>&1 | grep Connection | wc -l)" ]]; then
    # v4 kommunikation benutzen
    VOLATILE_NETCAT="${VOLATILE_NETCAT} -4"
  fi
  VOLATILE_PASSWD=$(get_full_path_to_executable passwd)
  VOLATILE_LOGGER=$(get_full_path_to_executable logger)
  VOLATILE_OPENSSL=$(get_full_path_to_executable openssl)
  VOLATILE_USERADD=$(get_full_path_to_executable useradd)
  VOLATILE_USERMOD=$(get_full_path_to_executable usermod)
  VOLATILE_GROUPADD=$(get_full_path_to_executable groupadd)
  VOLATILE_IFCONFIG=$(get_full_path_to_executable ifconfig)
  VOLATILE_XMLLINT=$(get_full_path_to_executable xmllint)
  ##############################################################################
  ##  Achtung: MAGIC_NUMBER_OF_THIS_FUNC_LIB in func_lib.sh anpassen          ##
  ##############################################################################
}

