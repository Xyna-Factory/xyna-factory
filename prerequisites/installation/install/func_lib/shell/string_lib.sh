
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



# Shell -> String library

#  erweitert einen String um ein oder mehrere weitere Elemente, sofern sie noch nicht
#+ im String enthalten sind
#
#  Parameter:
#o     1 = String
#o  ab 2 = Element, dass hinzugefuegt werden soll
#
add_elements_to_string () {
  local ADD=""
  local SEARCH_EXPRESSION=""
  local STRING=${1}
  shift

  for i in $@
  do
    SEARCH_EXPRESSION=$(echo "${i}" | ${VOLATILE_SED} -e "s+-+\\\\-+g")
    if [[ $(echo "${STRING}${ADD}" | ${VOLATILE_GREP} -c "${SEARCH_EXPRESSION}") -eq 0 ]]; then
      ADD="${ADD} ${i}"
    fi
  done
  echo "${STRING}${ADD}"
}


#  Ueberprueft, ob eine IP-Adresse gueltig ist.
#+ Eine gueltige IP-Adresse besteht aus vier Oktets.
#+ Jedes Oktet darf nur aus 1 bis 3 Zahlen bestehen.
#+ Am Ende muss die Subnetzmaske durch einen / von
#+ der IP-Adresse getrennt sein.
#
#  Beispiel fuer eine gueltige Notation:
#    10.20.0.1/16
#
#  Ergebniswerte:
#o   0 = erfolgreich
#o   1 = fehlgeschlagen
#
check_ip_address_and_subnet () {
  local k=$(echo ${1} | ${VOLATILE_SED} -e "s+[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}/[0-9]\{1,3\}+IP_ADDRESS_CHECK_PASSED+")
  if [[ "xIP_ADDRESS_CHECK_PASSED" == "x${k}" ]]; then
    return 0
  fi
  return 1
}

#  Ueberprueft, ob eine IP-Adresse gueltig ist.
#+ Eine gueltige IP-Adresse besteht aus vier Oktets.
#+ Jedes Oktet darf nur aus 1 bis 3 Zahlen bestehen.
#
#  Beispiel fuer eine gueltige Notation:
#    10.20.0.1
#
#  Ergebniswerte:
#o   0 = erfolgreich
#o   1 = fehlgeschlagen
#
check_ip_address () {
  local k=$(echo ${1} | ${VOLATILE_SED} -e "s+[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}+IP_ADDRESS_CHECK_PASSED+")
  if [[ "xIP_ADDRESS_CHECK_PASSED" == "x${k}" ]]; then
    return 0
  fi
  return 1
}

#  Ueberprueft ob Property eine gueltige IP-Adresseist.
f_check_is_ip_address() {
  local STR_PROPERTY="${1-Current Property}"
  local STR_VALUE="${2-${CURRENT_PROPERTY}}"
  local ret_val=0
  
  check_ip_address ${STR_VALUE}
  ret_val=$?
  if [[ ${ret_val} -gt 0 ]]; then
    f_add_to_error_buffer "'${STR_PROPERTY}' must be a valid ip-address. Current value is: '${STR_VALUE}'"
  fi
  return ${ret_val}
}

#  Ueberprueft ob Property nicht leer ist.
f_check_is_not_empty() {
  local STR_PROPERTY="${1-Current Property}"
  local STR_VALUE="${2-${CURRENT_PROPERTY}}"
  local ret_val=0
  
  if [[ -z "${STR_VALUE}" ]]; then
    ret_val=1
  fi

  if [[ ${ret_val} -gt 0 ]]; then
    f_add_to_error_buffer "'${STR_PROPERTY}' must have a non-empty value. Current value is empty"
  fi
  
  return ${ret_val}
}

#  Ueberprueft ob integer wert.
f_check_is_integer() {
  local STR_PROPERTY="${1-Current Property}"
  local STR_VALUE="${2-${CURRENT_PROPERTY}}"
  local ret_val=0
  
  local k=$(echo "${STR_VALUE}" | ${VOLATILE_SED} -e "s+[0-9]\{1,\}+INTEGER_CHECK_PASSED+")
  if [[ "x${k}" != "xINTEGER_CHECK_PASSED" ]]; then
    ret_val=1
  fi

  if [[ ${ret_val} -gt 0 ]]; then
    f_add_to_error_buffer "'${STR_PROPERTY}' must be an integer. Current value is: '${STR_VALUE}'"
  fi
  
  return ${ret_val}
}

#  Ueberprueft ob Zahl.
f_check_is_number() {
  local STR_PROPERTY="${1-Current Property}"
  local STR_VALUE="${2-${CURRENT_PROPERTY}}"
  local ret_val=0
  
  local k=$(echo "${STR_VALUE}" | ${VOLATILE_SED} -e "s+[0-9.-]\{1,\}+NUMBER_CHECK_PASSED+")
  if [[ "x${k}" != "xNUMBER_CHECK_PASSED" ]]; then
    ret_val=1
  fi

  if [[ ${ret_val} -gt 0 ]]; then
    f_add_to_error_buffer "'${STR_PROPERTY}' must be a integer. Current value is: '${STR_VALUE}'"
  fi
  
  return ${ret_val}
}


#  Ueberprueft ob alpha numerisch.
f_check_is_alpha_numeric() {
  local STR_PROPERTY="${1-Current Property}"
  local STR_VALUE="${2-${CURRENT_PROPERTY}}"
  local ret_val=0
  
  local k=$(echo "${STR_VALUE}" | ${VOLATILE_SED} -e "s+[A-Za-z0-9_-]\{1,\}+ALNUM_CHECK_PASSED+")
  if [[ "x${k}" != "xALNUM_CHECK_PASSED" ]]; then
    ret_val=1
  fi

  if [[ ${ret_val} -gt 0 ]]; then
    f_add_to_error_buffer "'${STR_PROPERTY}' must be a alpha numeric value [A-Za-z0-9_]. Current value is: '${STR_VALUE}'"
  fi
  
  return ${ret_val}
}


#  Ueberprueft ob alpha numerisch.
f_check_is_password() {
  local STR_PROPERTY="${1-Current Property}"
  local STR_VALUE="${2-${CURRENT_PROPERTY}}"
  local ret_val=0
  local k=$(echo "${STR_VALUE}" | ${VOLATILE_SED} -e "s+[A-Za-z0-9@#!=+_-]\{1,\}+PASSWD_CHECK_PASSED+")
  if [[ "x${k}" != "xPASSWD_CHECK_PASSED" ]]; then
    ret_val=1
  fi

  if [[ ${ret_val} -gt 0 ]]; then
    f_add_to_error_buffer "'${STR_PROPERTY}' must be a alpha numeric value [A-Za-z0-9@#!=+_-]. Current value is: '${STR_VALUE}'"
  fi
  
  return ${ret_val}
}


#  Ueberprueft ob md5 hash.
f_check_is_md5() {
  local STR_PROPERTY="${1-Current Property}"
  local STR_VALUE="${2-${CURRENT_PROPERTY}}"
  local ret_val=0
  
  local k=$(echo "${STR_VALUE}" | ${VOLATILE_SED} -e "s+[A-Za-z0-9]\{32\}+MD5_CHECK_PASSED+")
  # Check auf alphanumerische Zeichen und Stringlaenge von 32
  if [[ "x${k}" != "xMD5_CHECK_PASSED" ]]; then
    ret_val=1
  fi

  if [[ ${ret_val} -gt 0 ]]; then
    f_add_to_error_buffer "'${STR_PROPERTY}' must be a md5-hash. Current value is: '${STR_VALUE}'"
  fi
  
  return ${ret_val}
}


#  Ueberprueft ob url.
f_check_is_url() {
  local STR_PROPERTY="${1-Current Property}"
  local STR_VALUE="${2-${CURRENT_PROPERTY}}"
  local ret_val=0
  
  # http://www.w3.org/Addressing/URL/url-spec.txt
  local k=$(echo "${STR_VALUE}" | ${VOLATILE_SED} -e "s+https\?://[a-zA-Z.0-9-]\{3,\}[:0-9/]*[a-zA-Z/?]*+URL_CHECK_PASSED+")
  if [[ "x${k}" != "xURL_CHECK_PASSED" ]]; then
    ret_val=1
  fi

  if [[ ${ret_val} -gt 0 ]]; then
    f_add_to_error_buffer "'${STR_PROPERTY}' must be a valid url. Current value is: '${STR_VALUE}'"
  fi
  
  return ${ret_val}
}


#  Ueberprueft ob Pfad.
f_check_is_path() {
  local STR_PROPERTY="${1-Current Property}"
  local STR_VALUE="${2-${CURRENT_PROPERTY}}"
  local ret_val=0
  
  # http://www.w3.org/Addressing/URL/url-spec.txt
  local k=$(echo "${STR_VALUE}" | ${VOLATILE_SED} -e "s+/*[a-zA-Z0-9/_.-]\{3,\}+PATH_CHECK_PASSED+")
  if [[ "x${k}" != "xPATH_CHECK_PASSED" ]]; then
    ret_val=1
  fi

  if [[ ${ret_val} -gt 0 ]]; then
    f_add_to_error_buffer "'${STR_PROPERTY}' must be a valid path. Current value is: '${STR_VALUE}'"
  fi
  
  return ${ret_val}
}

#  Zeichenkette konvertieren
#
#  Eingabeparameter:
#o   1 = Zu konvertierente Zeichenkette
#o   2 = Ziel-Zeichensatz
#o   3 = Ausgangs-Zeichensatz
f_string_convert() {
  local STR_ORIGINAL=$1
  local STR_TO=${2-LOCAL}
  local STR_FROM=${3-LOCAL}
  local OPTION_FROM=""
  local OPTION_TO=""

  if [[ "x${STR_FROM}" != "xLOCAL" ]]; then
    OPTION_FROM="-f ${STR_FROM}"
  fi
  
  if [[ "x${STR_TO}" != "xLOCAL" ]]; then
    OPTION_TO="-t ${STR_TO}"
  fi
  echo ${STR_ORIGINAL} | ${VOLATILE_ICONV} ${OPTION_FROM}  ${OPTION_TO}
  return $?
}


#  Zeilenumbrueche von Windows entfernen
#
#  Eingabeparameter:
#o   1 = Name der zu konvertierenden Datei
#
f_dos2unix () {
  local FILE_TO_EDIT="${1}"
  if [[ -f "${FILE_TO_EDIT}" ]]; then
    ${VOLATILE_CP} -p -f "${FILE_TO_EDIT}" "${TMP_FILE}"
    ${VOLATILE_CAT} "${FILE_TO_EDIT}" | ${VOLATILE_TR} -d "\015" > "${TMP_FILE}"
    ${VOLATILE_MV} "${TMP_FILE}" "${FILE_TO_EDIT}"
  else
    err_msg "f_dos2unix: Unable to locate '${FILE_TO_EDIT}'."
  fi
}

#  Vergleicht zwei Versionsnummern
#
#  Eingabeparameter
#   1: Versionsnummer, z.B. 1.0.10.4
#   2: Versionsnummer, z.B. 1.2.9.3
#
#  Rueckgabewert
#   -1 falls $version1 <  $version2
#    0 falls $version2 == $version2
#    1 falls $version1 >  $version2
#
f_compare_versions () {
  local INT_LESS_THAN=-1
  local INT_EQUAL=0
  local INT_GREATER_THAN=1

  local STR_VERSION_1="${1}"
  local STR_VERSION_2="${2}"

  if [[ ${DEBUG:-0} -gt 0 ]]; then
    echo >&2
    echo "DBG: f_compare_versions | version1 = *${STR_VERSION_1}*" >&2
    echo "DBG: f_compare_versions | version2 = *${STR_VERSION_2}*" >&2
  fi

  #  Die einzelnen Teile der Versionsnummer sind mit Punkt '.' voneinander getrennt.
  #+ Stringvergleiche fuehren dazu, dass 1.10 < 1.9 ist!
  #+ Daher muss jeder Teil der Versionsnummer getrennt von den anderen verglichen werden.

  #  Vordersten Teil der Versionsnummer zum Vergleich extrahieren
  #+ Warum awk? Damit "merkwuerdige" Nummer, wie z.B. 008, einen Integer ergeben.
  local INT_MAJOR_1=$(echo ${STR_VERSION_1} | ${VOLATILE_AWK} 'BEGIN { FS="." } {printf "%d", $1}')
  local INT_MAJOR_2=$(echo ${STR_VERSION_2} | ${VOLATILE_AWK} 'BEGIN { FS="." } {printf "%d", $1}')

  if [[ ${DEBUG:-0} -gt 0 ]]; then
    echo "DBG: f_compare_versions | major1 = *${INT_MAJOR_1}*" >&2
    echo "DBG: f_compare_versions | major2 = *${INT_MAJOR_2}*" >&2
  fi

  #  Wenn es nix zu vergleichen gibt, dann abbrechen (wegen Rekursion)
  local INT_RETURN_VALUE=${INT_EQUAL}
  if [[ -n ${STR_VERSION_1} || -n ${STR_VERSION_2} ]]; then
    #  Vergleich starten
    if [[ ${INT_MAJOR_1} -lt ${INT_MAJOR_2} ]]; then
      INT_RETURN_VALUE=${INT_LESS_THAN}
    elif [[ ${INT_MAJOR_1} -gt ${INT_MAJOR_2} ]]; then
      INT_RETURN_VALUE=${INT_GREATER_THAN}
    else
      #  Vorderer Teil der Versionsnummer ist gleich.

      #  Gibt es noch einen hinteren Teil der Versionsnummer, der weiter verglichen werden muss?
      local INT_MINOR_1=$(echo ${STR_VERSION_1} | ${VOLATILE_AWK} '{pos = index($0, "."); value = substr($0, pos+1); if (pos > 0) print(value);}')
      local INT_MINOR_2=$(echo ${STR_VERSION_2} | ${VOLATILE_AWK} '{pos = index($0, "."); value = substr($0, pos+1); if (pos > 0) print(value);}')

      if [[ ${DEBUG:-0} -gt 0 ]]; then
        echo "DBG: f_compare_versions | minor1 = *${INT_MINOR_1}*" >&2
        echo "DBG: f_compare_versions | minor2 = *${INT_MINOR_2}*" >&2
      fi

      #  Die hinteren Versionsnummern nochmal vergleichen (rekursiver Aufruf)
      INT_RETURN_VALUE=$(f_compare_versions "${INT_MINOR_1}" "${INT_MINOR_2}")
    fi
  fi

  #  Ergebnis zurueckgeben
  echo ${INT_RETURN_VALUE}
}

#  Beginnt übergebene Zeichenkette mit einem Kleinbuchstaben?
#
#  Eingabeparameter:
#o   1 = Zeichenkette
#  Rueckgabewert
#    0 beginnt mit Kleinbuchstaben
#    1 beginnt mit Großbuchstabenfalls
#    2 ansonsten 
f_starts_with_lowercase () {
  case ${1} in
    [[:lower:]]*) return 0;;
    [[:upper:]]*) return 1;;
    *) return 2;;
  esac
}


# Trennt übergebenen String anhand Komma oder Leerzeichen, wandelt alles in Kleinbuchstaben außer "ALL" 
# und gibt als Leerzeichen-separierten String aus
#Aufruf  f_split_to_array "abc Fgh ALL,ijk,LMN" -> [abc fgh ALL ijk lmn] 
f_split_to_array () {
  echo "$1" | 
    ${VOLATILE_AWK} '{
      a=split($0,arr,",| "); 
      for(i=1;i<=a;++i) { 
        val=(arr[i]=="ALL"?arr[i]:tolower(arr[i])); 
        printf("%s ",val);
      }
    }'
}

# Auswertung, ob mindestens eine der übergebenen Variablen true ist
f_selected () {
  for v in "$@" ; do
    if [[ "x${v}" == "xtrue" ]] ; then
      return 0;
    fi;
  done
  return 1;
}

# Auswertung, ob Übergabe $1 true ist
#  Eingabeparameter:
#    1 = Zeichenkette
#  Rückgabeparameter
#    boolean
# Beispiel: if f_is_true ${VAL} ; then ...
f_is_true () {
  if [[ "true" == $1 ]]; then
    return 0
  else
    return 1
  fi;
}

# Auswertung, ob Übergabe $1 eine Ganzzahl ist
#  Eingabeparameter:
#    1 = Zeichenkette
#  Rückgabeparameter
#    boolean
# Beispiel: if f_is_integer ${VAL} ; then ...
f_is_integer () {
  local re='^[0-9]+$' #separat, da Versionsunterschiede in bash sonst Probleme machen
  if [[ "$1" =~ $re ]] ; then
    return 0
  else
    return 1
  fi;
}

