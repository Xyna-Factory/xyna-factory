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
FACTORY_CLI_PORT="TOKEN_FACTORY_CLI_PORT"
INSTANCE_NUMBER="TOKEN_INSTANCE_NUMBER"
NETCAT="TOKEN_NETCAT"
VERBOSE="false"
NAMEDPIPE=

# erkennung vom ende des outputs der factory
filter_output () {
  trap eoftofifo SIGPIPE SIGHUP SIGINT SIGQUIT SIGTERM;
  local LINE
  while IFS='' read -r LINE; do
      echo "${LINE}"
  done
  CALLRESULT=${LINE}
  echo "${CALLRESULT}" >&2 
  # netcat beenden (bugz 23034)
  echo -n "" > "${NAMEDPIPE}_2"
}


cleanup() {
  local DELDIR="$(dirname "$NAMEDPIPE")"
  rm -rf "$DELDIR"
  exit
}

eoftofifo() {
  # das wartende cat beenden, falls filter_output abgebrochen wird (z.b. xynafactory.sh <langlaufender befehl> | less -> quit vor beendigung des befehls)
  echo -n "" > "${NAMEDPIPE}_2"
  exit
}


fast_factory_call () {
  local GROUP_SEP=$'\x1D';            #\x1D=GS Group Separator
  local RECORD_SEP=$'\x1E';           #\x1E=RS Record Separator
  local END_OF_TRANSMISSION=$'\x04';  #\x04=EOT End of Transmission
  local CALL="";
  local SEP=${GROUP_SEP};
  for i in "$@"; do 
    CALL="${CALL}$i${SEP}"
    SEP=${RECORD_SEP};
  done
  CALL="${CALL}${GROUP_SEP}"
  if [[ -n ${XYNA_CLI_ADDITIONAL+x} ]] ; then
    CALL="${CALL}${XYNA_CLI_ADDITIONAL}${RECORD_SEP}"
  fi
  CALL="${CALL}${END_OF_TRANSMISSION}"

  local CALLRESULT #hier schon local, damit local nicht ExitCode kaputt macht
  exec 3>&1 #Output umleiten, so dass nicht von $(...) gefangen
  set -o pipefail #local RC=$? soll alle Pipe-Commands auswerten, nicht nur das letzte

  # namedpipe erstellen und dafuer sorgen, dass sie durch die trap automatisch aufgeraeumt wird
  NAMEDPIPE="$(mktemp -d)/nr$$"
  mkfifo "$NAMEDPIPE"
  mkfifo "${NAMEDPIPE}_2"
  trap cleanup SIGHUP SIGINT SIGQUIT SIGTERM
  (
    exec 6>"$NAMEDPIPE"
    echo -n "${CALL}" >&6
    # warten, dass netcat fertig ist
    cat "${NAMEDPIPE}_2" >> /dev/null
    # pipe schliessen
    exec 6>&-
  ) & 

  # per netcat mit factory kommunizieren. input fuer netcat sind die befehlsparameter und die pipe, die am ende die beendigung von netcat das eof enthaelt
  CALLRESULT=$( { cat "$NAMEDPIPE" | ${NETCAT} localhost ${FACTORY_CLI_PORT} | filter_output 1>&3 ; } 2>&1  );
  local RC=$?

  # aufraeumenA
  local DELDIR="$(dirname "$NAMEDPIPE")"
  rm -rf "$DELDIR"
  trap SIGHUP SIGINT SIGQUIT SIGTERM

  if [[ "${RC}" != "0" ]] ; then
    if [[ "${CALLRESULT}" == *Connection\ refused* ]] ; then
      CALL_RESULT="Unable to execute command \"${1}\": Xyna Factory is not running on port ${FACTORY_CLI_PORT}#";
      return 254;
    else
      fast_factory_status
      local STATUS=$?
      if [[ 5 == ${STATUS} ]] ; then
        CALL_RESULT="Unable to execute command \"${1}\": Xyna Factory is not running on port ${FACTORY_CLI_PORT}#";
        return 254;
        #oder aber bei sehr gravierenden Fehlern hat fast_factory_status bereits echo und exit 255; aufgerufen
      else
        echo "Unexpected: response \"${CALLRESULT}\" and status \"${STATUS_RESULT}\"";
        exit 255;
      fi;
    fi;
  fi;
  
  case ${CALLRESULT} in
    ENDOFSTREAM_SUCCESS)
      return 0;
      ;;
    ENDOFSTREAM_SILENT)
      return 0;
      ;;
    ENDOFSTREAM_SUCCESS_BUT_NO_CHANGE)
      return 9;
      ;;
    ENDOFSTREAM_UNKNOWN_COMMAND)
      return 10;
      ;;
    ENDOFSTREAM_REJECTED)
      return 11;
      ;;
    ENDOFSTREAM_SUCCESS_WITH_PROBLEM)
      return 12;
      ;;
    ENDOFSTREAM_XYNA_EXCEPTION)
      return 253;
      ;;
    ENDOFSTREAM_COMMUNICATION_FAILED)
      return 254;
      ;;
    ENDOFSTREAM_GENERAL_ERROR)
      return 255;
      ;;
    ENDOFSTREAM_STATUS_STARTING)
      return 1;
      ;;
    ENDOFSTREAM_STATUS_STOPPING)
      return 6;
      ;;
    ENDOFSTREAM_STATUS_UP_AND_RUNNING*)
      return 0;
      ;;
    *)
      echo "Unexpected ReturnCode ${RC} ${CALLRESULT}";
      exit 255;
      ;;
  esac
}

fast_factory_status () {
  local CALL=$'status\x1D\x1D\x04'; #GS GS EOT
  local RESPONSE  #hier schon local, damit local nicht ExitCode kaputt macht
  RESPONSE="$(echo -n ${CALL} | ${NETCAT} -v localhost ${FACTORY_CLI_PORT} 2>&1 )";
  local RC=$?
  if [[ "${RC}" != "0" ]] ; then
    if [[ "${RESPONSE}" == *Connection\ refused* ]] ; then
      STATUS_RESULT="Status: 'Not running'";
      return 5;
    else 
      echo "Unexpected: ${RESPONSE}";
      exit 255;
    fi
  fi;
  local MSG=${RESPONSE%ENDOFSTREAM*}
  local RC=${RESPONSE:${#MSG}}
  case ${RC} in
    ENDOFSTREAM_STATUS_UP_AND_RUNNING*)
      STATUS_RESULT="Status: 'Up and running'";
      return 0;
      ;;
    ENDOFSTREAM_STATUS_STARTING*)
      STATUS_RESULT="Status: 'Starting'";
      return 1;
      ;;
    ENDOFSTREAM_STATUS_ALREADY_RUNNING*)
      STATUS_RESULT="Status: 'Up and running'";
      return 2;
      ;;
    ENDOFSTREAM_STATUS_NOT_RUNNING*)
      STATUS_RESULT="Status: 'Not running'";
      return 5;
      ;;
    ENDOFSTREAM_STATUS_STOPPING*)
      STATUS_RESULT="Status: 'Stopping'";
      return 6;
      ;;
    *)
      echo "Unexpected ReturnCode ${RC}";
      exit 255;
      ;;
  esac

}

if [[ "$1" == "-q" ]] ; then
  CMD="$2";
  QUIET="true";
else 
  CMD="$1";
  QUIET="false";
fi
case "${CMD}" in #lowercase
  status)
    fast_factory_status
    RC=$?
    if [[ "${QUIET}" == "false" ]] ; then
      echo "${STATUS_RESULT}"
    fi
    if [[ "$@" == "status -v" && ( $RC -lt 3 || $RC -eq 6 ) ]] ; then
      fast_factory_call "extendedstatus"
      if [[ ${#CALL_RESULT} > 1 ]] ; then
        echo "${CALL_RESULT:0:${#CALL_RESULT}-1}" #Zeilenumbruch abschneiden
      fi
    fi
    exit ${RC}
    ;;
  start|stop|restart)
    ;;
  install)
    ;;
  create_cluster_using_oracle|join_cluster_using_oracle|remove_cluster_using_oracle)
    ;;
  create_cluster|join_cluster|remove_cluster)
   ;;
  -n|-d|-f|-F)
    ;;
  -q)
    #unerwartetes doppeltes -q... wird halt langsamer bearbeitet
    ;;
  *)
    if [[ "${QUIET}" == "true" ]] ; then
      shift 1
      fast_factory_call "$@"
      RC=$?
      exit ${RC}
    else
      fast_factory_call "$@"
      RC=$?
      if [[ ${#CALL_RESULT} > 1 ]] ; then
        echo "${CALL_RESULT:0:${#CALL_RESULT}-1}" #Zeilenumbruch abschneiden
      fi
      exit ${RC}
    fi
    ;;
esac

unset CMD QUIET


# Hierhin gelangt dieses Skript nur, wenn keine schnelle Factory-Abfrage verwendet werden konnte.
# In diesem Fall beginnt nun die bisherige langsame Bearbeitung, beginnend mit dem Import weiterer Bibliotheken. 
# Fuer die schnelle Factory-Abfrage ist keine Pruefung des ausfuehrenden Benutzers noetig, da Benutzer ausreichend 
# autorisiert ist, wenn er dieses Skript ausfuehren darf. Die Factory erbt ja auch keine Benutzerrechte ueber 
# den Socket.     

#  Einige Funktionen sind in Bibliotheken ausgelagert.
#+ Diese Bibliotheken importieren
#
#  Eingabeparameter:
#o   Dateiname der zu importierenden Bibliothek
load_functions () {
  SOURCE_FILE="${1}"
  if [[ ! -f ${SOURCE_FILE} ]]; then
    echo "Unable to import functions from '${SOURCE_FILE}'. Abort!"; exit 99;
  else
    source ${SOURCE_FILE}
  fi
}

#  Generische Funktionen importieren.
load_functions "$(dirname "$0")/func_lib/func_lib.sh"

#  Produktspezifische Funktionen importieren.
load_functions "$(dirname "$0")/product_lib.sh"

#  bugz 8834: Aufruf mit relativem/absoluten Pfad moeglich
cd "$(dirname "$0")"

#  No arguments is an error, so give help to the user.
if [[ $# -lt 1 ]]; then DISPLAY_USAGE="true"; fi

parse_commandline_arguments "$@"
shift $((OPTIND-1))

if [[ "x${DISPLAY_USAGE}" == "xtrue" ]]; then display_usage; exit; fi

if [[ "x${DRY_RUN}" == "xtrue" ]]; then debug_variables "${QUIET_OPTION}" -p "${FACTORY_CLI_PORT}" "$@"; exit; fi

f_read_properties

#Pruefung der Benutzerrechte, evtl. sudo oder su auf XYNA_USER. Ist eigentlich nur fuer start und restart noetig 
#und fuer die Spezialaufrufe, die eh besser in install_black_edition.sh aufgehoben waeren.
CURRENT_USER=$(${VOLATILE_PS} -o "user=" -p $$ | ${VOLATILE_SED} -e "s+ ++g")
if [[ "x${CURRENT_USER}" != "x${XYNA_USER}" ]]; then
  XF_CALL="$(${VOLATILE_PWD})/$(basename "$0")"
  if [[ "x${CURRENT_USER}" == "xroot" ]] ; then
    #echo "Executing command as user '${XYNA_USER}'"
    su - ${XYNA_USER} -c "${XF_CALL} $@"
    exit $?
  else
    INT_SUDO_ALLOWED=$(sudo -l | ${VOLATILE_GREP} -c "${XF_CALL}")
    if [[ "x${INT_SUDO_ALLOWED}" == "x0" ]]; then
      message_fail_and_exit "This instance is configured for user '${XYNA_USER}'. Abort, because '${CURRENT_USER}' is not allowed!"
    else
      #echo "Executing command as user '${XYNA_USER}'"
      sudo -u ${XYNA_USER} ${XF_CALL} $@
      exit $?
    fi
  fi
fi

#Java aus richtigem Verzeichnis nehmen (konfiguriert in blackedition.properties) 
VOLATILE_JAVA=${JAVA_HOME}/bin/java
get_class_jars
FACTORY_CLI_CMD="${VOLATILE_JAVA} -classpath ${CLASSES}"

#  start main logic
case "${1}" in
  start)  j_start_xynafactory "${QUIET_OPTION}" -p "${FACTORY_CLI_PORT}" "$@";;
  stop)
    #  cleanly shutdown
    if [[ "${BOOL_EXIT:-false}" == "false" && "${BOOL_KILL:-false}" == "false" ]]; then
      j_stop_xynafactory "${QUIET_OPTION}" -p "${FACTORY_CLI_PORT}" "$@"
    fi

    #  call System.exit() from within the factory
    if [[ "${BOOL_EXIT:-false}" == "true" ]]; then
      run_xynafactory_cmd "${QUIET_OPTION}" -p "${FACTORY_CLI_PORT}" "forceexit"
      EXIT_CODE=$?
    fi

    #  send SIGKILL to the factory process
    if [[ "${BOOL_KILL:-false}" == "true" ]]; then

      XYNAFACTORY_PROCESS_PID=$(f_get_xynafactory_pid)

      if [[ "${XYNAFACTORY_PROCESS_PID}" != "false" ]]; then
        echo -e "\n* Forcing XynaFactory with pid '${XYNAFACTORY_PROCESS_PID}' to quit..."
        echo -e "\nSome information about the process follows:\n"
        ${VOLATILE_PS} -f -p "${XYNAFACTORY_PROCESS_PID}"
        echo
        kill -9 "${XYNAFACTORY_PROCESS_PID}"
        attention_msg "XynaFactory has been forced to quit. Unsaved data has been lost."
        exit 99
      fi

    fi
    ;;
  restart)
    j_stop_xynafactory "${QUIET_OPTION}" -p "${FACTORY_CLI_PORT}" stop
    sleep 2
    j_start_xynafactory "${QUIET_OPTION}" -p "${FACTORY_CLI_PORT}" start
    ;;
  install|create_cluster_using_oracle|join_cluster_using_oracle|remove_cluster_using_oracle)
    echo "FAILED: Target '${1}' is not supported anymore."
    echo "Hint: Use 'install_black_edition.sh' from the delivery item instead."
    EXIT_CODE=99
    ;;
  create_cluster)
    f_do_xyna_cluster "create"
    EXIT_CODE=$?
    ;;
  join_cluster)
    f_do_xyna_cluster "join"
    EXIT_CODE=$?
    ;;
  remove_cluster)
    f_remove_cluster
    EXIT_CODE=$?
    ;;
  *)
    echo "FAILED: Target '${1}' is not recognized."
    echo "Hint: Maybe a spelling error or uppercase letters?"
    EXIT_CODE=99
    ;;
esac

exit ${EXIT_CODE:-0}
