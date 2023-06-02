
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



# Shell -> Messages library

ERROR_BUFFER=""

#  Globale Fehlercodes im Bereich von 64 - 78
#+ siehe auch: http://tldp.org/LDP/abs/html/exitcodes.html
EX_OK=0
EX_USAGE=64
EX_PARAMETER=65
EX_NOINPUT=66
EX_NOUSER=67
EX_WRONGUSER=68
EX_WRONGGROUP=69
EX_UNSUPPORTED=70
EX_OSDIR=71
EX_OSFILE=72
EX_NOSPACE=73
EX_IOERR=74
EX_TEMPFAIL=75
EX_PROTOCOL=76
EX_NOPERM=77
EX_XYNA=78

#  Eingabeparameter: Fehlercode
#  Rueckgabewert   : String mit der ausfuerhlichen Fehlerbeschreibung
f_get_error_description () {
    local INT_ERROR_CODE="${1:-255}"
    case "${INT_ERROR_CODE}" in
  0)  echo "successful termination";;
  64) echo "command line usage error";;
  65) echo "parameter missing";;
  68) echo "wrong system user";;
  69) echo "wrong system group";;
  70) echo "platform not supported";;
  71) echo "critical OS directory missing";;
  72) echo "critical OS file missing";;
  73) echo "not enough space";;
  75) echo "temp failure; user is invited to retry";;
  77) echo "permission denied";;
  78) echo "xynafactory error";;

  66) echo "cannot open input";;
  67) echo "addressee unknown";;
  74) echo "input/output error";;
  76) echo "remote error in protocol";;

  *)  echo "error code '${INT_ERROR_CODE}' is undefined";;
    esac
}



#  Beendet das Skript und gibt eine sinnvolle Fehlermeldung aus.
#
#  Eingabeparameter:
#    1. Fehlercode (siehe auch f_get_error_description)
#    2. [optional] Zeichenkette mit Fehlerinformationen
#
f_exit_with_message () {
    local INT_ERROR_CODE="${1:-255}"
    local STR_ERROR_MSG="${2}"

    if [[ -z "${STR_ERROR_MSG}" ]]; then
  STR_ERROR_MSG=$(f_get_error_description "${INT_ERROR_CODE}")
    fi

    err_msg "${STR_ERROR_MSG}"
    exit ${INT_ERROR_CODE}
}



#  Eingabeparameter:
#    Zeichenkette mit Informationen
#
attention_msg () {
  echo "################################################################################"
  echo "##"
  echo "##  Attention"
  if [[ -n "${1}" ]]; then
    echo "##"
    echo "##    ${1}"
  fi
  echo "##"
  echo "################################################################################"
}

#  Eingabeparameter:
#    Mehrzeilige Zeichenkette mit Informationen
#
attention_multi_msg () {
  echo "################################################################################"
  echo "##"
  echo "##  Attention"
  echo "##"
  if [[ -n "${1}" ]]; then
    echo "${1}" | ${VOLATILE_SED} 's/\\n/\n/g' | ${VOLATILE_SED} 's/^/##  /' 
  fi
  echo "##"
  echo "################################################################################"
}


#  Eingabeparameter:
#    Zeichenkette mit Fehlerinformationen
#
err_msg () {
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" >&2
  echo "!!" >&2
  echo "!!  Error" >&2
  if [[ -n "${1}" ]]; then
    echo "!!" >&2
    echo "!!    ${1}" >&2
  fi
  echo "!!" >&2
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" >&2
}

#  Fehlerausgabe, wenn beim Aufruf von 'mysql' ein Problem aufgetreten ist.
mysql_err_msg () {
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo "!!"
  echo "!!  Fehler beim Aufruf von MySQL"
  if [[ -n "${1}" ]]; then
    echo "!!"
    echo "!!    ${1}"
  fi
  echo "!!"
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
}

#  Ausgabe einer Debug-Nachricht abhaengig vom DEBUG-Level:
#
#  Jeder DEBUG-Nachricht wird ein DEBUG-Level zugeordnert (default 1).
#+ Ist der DEBUG-Level der Nachricht groesser oder gleich dem
#+ globalen DEBUG-Level, so wird die Nachricht ausgegeben.
#
#  Globale Parameter:
#   DEBUG="<int>"
#
#  Eingabeparameter:
#   1: Nachricht, welche falls DEBUG-Level >= 1 ist, ausgegeben werden soll
#   2: DEBUG-Level fuer diese Nachricht (optional, default 1)
#
# Rueckgabewert:
#   0: wenn keine Nachricht ausgegeben wurde
#   1: wenn eine Nachricht ausgegeben wurde
#
debug_msg () {
  local STR_MSG="${1}"
  local STR_DEBUGLABEL=""
  local INT_DEBUGLEVEL="${2:-1}"
  local INT_GLOBAL_DEBUGLEVEL="${DEBUG:-0}"
  local INT_RET_VAL=0

  if [[ ${INT_DEBUGLEVEL} -le ${INT_GLOBAL_DEBUGLEVEL} ]]; then
    let i=0
    while [[ ${i} -lt ${INT_DEBUGLEVEL} ]]
    do
      STR_DEBUGLABEL="${STR_DEBUGLABEL}*"
      let i=i+1
    done
    echo -e "${STR_DEBUGLABEL} ${STR_MSG}" >&2
    INT_RET_VAL=1
  fi
  return ${INT_RET_VAL}
}


#  Eingabeparameter:
f_failed()  {
  echo " ... FAILED"
}

#  Eingabeparameter:
f_ok() {
  echo " ... OK"
}

message_ok () {
  #  Zeichenkette ist 4 Zeichen lang
  echo "  OK: $@"
}

message_fail_and_exit () {
  #  Zeichenkette ist 4 Zeichen lang
  echo "FAIL: $@"
  exit 1
}

#  Eingabeparameter:
f_clear_error_buffer() {
  ERROR_BUFFER=""
  return 0
}


#  Eingabeparameter:
f_add_to_error_buffer() {
   
 local STR_MSG="${1}"
 if [[ -z "${ERROR_BUFFER}" ]]; then
   ERROR_BUFFER="${STR_MSG}"
 else
   ERROR_BUFFER="${ERROR_BUFFER}\n${STR_MSG}"
 fi 
 return 0 
}

#  Eingabeparameter:
f_has_error_in_buffer() {
  if [[ -z "${ERROR_BUFFER}" ]]; then
    return 0
  else
    return 1
  fi
}

#  Eingabeparameter:
f_get_error_buffer() {
 echo -e "${ERROR_BUFFER}"

 return $(f_has_error_in_buffer)
}

#Einrücken der Ausgabe
#Beispiel
# OUTPUT=$(zip ....)
# f_indent "     " ${OUTPUT}"
f_indent () {
  local INDENT=${1};
  shift 1
  local OUTPUT="$@"
  echo "${OUTPUT}" | sed "s+^+${INDENT}+" #Einrücken
}

