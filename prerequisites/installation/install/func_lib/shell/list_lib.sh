
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


# Shell -> List library

#######################################################
#
# Listen-Operationen
#
# Achtung: die Listen werden alle nur per Namen übergeben!
#######################################################

#Hinzufügen zu Liste
#  Eingabeparameter:
#    1 = Liste
#    2-n = weitere Listenelemente
#  Rückgabeparameter
#    neue Liste
#  Beispiel 
#    LS=$(f_add_to_list LS "clivie" "dahlie");
f_add_to_list() {
  local LIST="${!1}"
  shift 1
  if [[ -z ${LIST} ]] ; then
    echo "$@"
  else
   echo "${LIST} $@"
  fi;
}

#Hinzufügen zum Anfang der Liste
#  Eingabeparameter:
#    1 = Liste
#    2-n = weitere Listenelemente
#  Rückgabeparameter
#    neue Liste
#  Beispiel 
#    LS=$(f_add_to_list_begin LS "clivie" "dahlie");
f_add_to_list_begin() {
  local LIST="${!1}"
  shift 1
  if [[ -z ${LIST} ]] ; then
    echo "$@"
  else
   echo "$@ ${LIST}"
  fi;
}

#Hinzufügen zur Liste, wenn Eintrag noch fehlt
#  Eingabeparameter:
#    1 = Liste
#    2-n = weitere Listenelemente
#  Rückgabeparameter
#    neue Liste
#  Beispiel 
#    LS=$(f_add_to_list_once LS "clivie" "dahlie");
f_add_to_list_once() {
  local LIST="${!1}"
  local LE;
  if [[ $# == 2 ]] ; then
    for LE in ${LIST} ; do
      if [[ ${2} == ${LE} ]] ; then
        echo ${LIST};
        return;
      fi;
    done;
    echo "${LIST} ${2}"
  else
    shift 1
    local NEWLIST="${LIST}";
    for LE in $@ ; do
      if ! f_is_in_list NEWLIST ${LE} ; then
        NEWLIST="${NEWLIST} ${LE}";
      fi;
    done
    echo ${NEWLIST}
  fi;
}

#Ist Wert in Liste enthalten
#  Eingabeparameter:
#    1 = Liste
#    2 = gesuchtes Element
#  Rückgabeparameter
#    boolean
#  Beispiel: 
#    if f_is_in_list LS "begonie" ; then ...
f_is_in_list() {
  local LIST="${!1}"
  #echo -e "is $2 in $LIST ? $@"
  local LE;
  for LE in ${LIST} ; do
    if [[ ${LE} == $2 ]] ; then 
      return 0;
    fi;
  done
  return 1;
}

#Entfernen aus Liste
#  Eingabeparameter:
#    1 = Liste
#    2-n = zu entfernende Listenelemente
#  Rückgabeparameter
#    neue Liste
#  Beispiel 
#    LS=$(f_remove_from_list LS "clivie" "dahlie");
f_remove_from_list() {
  local NEWLIST;
  local LIST="${!1}"
  local LE;
  if [[ $# == 2 ]] ; then
    for LE in ${LIST} ; do
      if [[ ${2} != ${LE} ]] ; then
        NEWLIST="${NEWLIST} ${LE}";
      fi;
    done
  else
    shift 1
    local REMOVE=$@;
    for LE in ${LIST} ; do
      if ! f_is_in_list REMOVE ${LE} ; then
        NEWLIST="${NEWLIST} ${LE}";
      fi;
    done
  fi;
  #echo  "\"${LIST}\" ohne \"${REMOVE}\" -> \"${NEWLIST}\"";
  echo ${NEWLIST}
}


#Rückgabe erster Wert in Liste
#  Eingabeparameter:
#    1 = Liste
#  Rückgabeparameter
#    erster Wert
#  Beispiel 
#    VAL=$(f_first_in_list LS);
f_first_in_list() {
  local LIST="${!1}"
  local LE;
  for LE in ${LIST} ; do
    echo ${LE};
    return;
  done
  echo
}

#Rückgabe letzter Wert in Liste
#  Eingabeparameter:
#    1 = Liste
#  Rückgabeparameter
#    letzter Wert
#  Beispiel 
#    VAL=$(f_last_in_list LS);
f_last_in_list() {
  local LIST="${!1}"
  local LE;
  for LE in ${LIST} ; do 
    LE=${LE};
  done
  echo ${LE};
}


