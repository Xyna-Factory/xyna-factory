
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



# Shell library


##############################
# MD5-HASH ausgeben und vergleichen
# o 1 : Datei fuer die der md5hash berechnet werden soll  
# o 2 : Ein Vergleichs-Hash , optional
# Falls kein Vergleichs-Hash angegeben wird, so wird nur der md5-Hash der angegeben Datei ausgegeben.
# 
f_check_md5() {
 local STR_FILE="$1"
 local STR_COMPARE_HASH="$2"
 local ret_val=0

  if [[ ! -r ${STR_FILE} ]]; then 
  err_msg "'${STR_FILE}' is not readable!"
  return ${EX_OSFILE} 
 fi
  
 STR_HASH=$(${VOLATILE_MD5} ${STR_FILE} | ${VOLATILE_AWK} '{print $1}')
 ret_val=$?
 
 if [[ -z "${STR_COMPARE_HASH}" ]]; then
   echo ${STR_HASH}  
 else
   debug_msg "HASH =         ${STR_HASH}"
   debug_msg "COMPARE_HASH = ${STR_COMPARE_HASH}"
   if [[ "x${STR_HASH}" == "x${STR_COMPARE_HASH}" ]]; then 
    echo "${STR_FILE}: OK"
   else
     echo "${STR_FILE}: Failed"
     ret_val=1
   fi
 fi
 
 return ${ret_val}
}


#  Zufaelliges Passwort generieren
#
#  Eingabeparameter:
#o   1 = Paswortlaenge - default: 10
#o   2 = Minimale Anzahl von Grossbuchstaben im Passwort - default: 1
#o   3 = Minimale Anzahl von Kleinbuchstaben im Passwort - default: 1
#o   4 = Minimale Anzahl von Ziffern im Passwort - default: 1
#o   5 = Minimale Anzahl von Sonderzeichen (!?#$) im Passwort - default: 1
#o   6 = Entropie / Erzeuge initial soviele Passwoerter und verwerfe dann schrittweise bis Bedingungen 
#        Bei Komplexen Passwortbedingungen muss dies ggf. erhoht werden, auf Kosten der performance 
#        - default: 20
f_make_password() {   
  
  local LENGTH=${1:-10}
  local MIN_U=${2:-1}
  local MIN_L=${3:-1}
  local MIN_D=${4:-1}
  local MIN_S=${5:-1}
  local ENTROPIE=${6:-20}
  local FOUND=0
  
  local PASSWORTLISTE=""
  
  if [[ ${MIN_S} -gt 0 ]]; then
    PASSWORTLISTE=$(${VOLATILE_TR} -dc [:alnum:]@#!=+_ </dev/urandom |  ${VOLATILE_FOLD} -w ${LENGTH} |  ${VOLATILE_GREP} "^[a-zA-Z][@#!=+_]." | ${VOLATILE_HEAD} -${ENTROPIE})
  else
    PASSWORTLISTE=$(${VOLATILE_TR} -dc [:alnum:] </dev/urandom |  ${VOLATILE_FOLD} -w ${LENGTH} |  ${VOLATILE_GREP} "^[a-zA-Z]" | ${VOLATILE_HEAD} -${ENTROPIE})
  fi
  
  for i in $PASSWORTLISTE; do
    UPPERS=$(echo $i |  ${VOLATILE_AWK} '{print gsub(/[A-Z]/,"")}')
    LOWERS=$(echo $i |  ${VOLATILE_AWK} '{print gsub(/[a-z]/,"")}')
    DIGITS=$(echo $i |  ${VOLATILE_AWK} '{print gsub(/[0-9]/,"")}')
    SPECIAL=$(echo $i |  ${VOLATILE_AWK} '{print gsub(/[@#!=+_]/,"")}')
    if [[ ${UPPERS} -ge ${MIN_U} && ${LOWERS} -ge ${MIN_L} && ${DIGITS} -ge ${MIN_D} && ${SPECIAL} -ge ${MIN_S} ]]; then
      FOUND=1; break
    fi
  done

  if [[ ${FOUND} -eq 0 ]];then
    err_msg "f_make_password: Could not generate appropriate password - try again"
    return ${EX_TEMPFAIL}
  else
    echo $i
    return 0
  fi
}




f_get_lazy_exe () {
 local LAZY_EXE=${1}
 local EXE=""
 local ret_val=1
 
 if [[ -x ${LAZY_EXE} ]]; then
   EXE=${LAZY_EXE}
   ret_val=0
 fi
        
 if [[ -x ${LAZY_EXE}.sh ]]; then
   EXE=${LAZY_EXE}.sh
   ret_val=0
 fi  
  
 echo ${EXE} 
 return ${ret_val}
}


