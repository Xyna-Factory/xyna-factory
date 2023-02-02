
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



# Shell -> Components library

#  Prueft, ob in der Component.properties-Datei der Wert einer
#+ Komponente gesetzt ist.
#
#  Eingabeparameter
#o   Name der Komponente, die geprueft werden soll:
#o     mysql,db,bos,dpp,dns,management,migration
#
#  Rueckgabewert
#o   "install" == Neuinstallation
#o   "update"  == Update
#
#  Folgende Faelle koennen auftreten
#o   1. Datei ist nicht vorhanden => Es ist eine Neuinstallation
#o      save_components_file wird die Datei anlegen
#o   2. Datei ist vorhanden, Komponente wird nicht gefunden
#o      => Neuinstallation
#o   3. Komponente wird gefunden => Update
check_components_file () {
  if [[ -z "${1}" ]] ; then 
    f_exit_with_message ${EX_PARAMETER} "check_components_file: Component must be set. Abort!";
  fi;
  local COMPONENT_PROPERTY="${1}.version"
  local PROP_FILE=$(get_properties_filename component)

  if [[ ! -f ${PROP_FILE} ]]; then
    #  Fall 1: Datei nicht gefunden => Neuinstallation
    echo "install"
  else
    get_property "${COMPONENT_PROPERTY}" "${PROP_FILE}"
    if [[ "x" == "x${CURRENT_PROPERTY}" ]]; then
      #  Fall 2: Komponente nicht gefunden => Neuinstallation
      echo "install"
    else
      #  Fall 3: Komponente gefunden => Update
      echo "update"
    fi
  fi
}


#  Eingabeparameter:
#o   Name der Komponente
check_component_status () {
  COMPONENT_NAME="${1}"
  PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})

  #  Prueft, ob die Komponente installiert ('install') oder aktualisiert ('update') werden muss
  COMPONENT_UPDATE=$(check_components_file "${COMPONENT_NAME}.${PRODUCT_INSTANCE}")
  if [[ "x${COMPONENT_UPDATE}" == "xinstall" ]]; then
    echo -e "\n-- Installation --\n"
  else
    echo -e "\n-- Update --\n"
  fi
}


#  Speichert in der Component.properties-Datei den Wert einer
#+ Komponente ab - Protokoll, dass die Komponente installiert wurde.
#
#  Eingabeparameter
#o   Name der Komponente, die geprueft werden soll:
#o     mysql,db,bos,dpp,dns,management,migration
#o   Wert der Komponente [optional]:
#o     $RELEASE_NUMBER, falls Wert nicht uebergeben wird
#
#  Folgende Faelle koennen auftreten
#o   1. Datei ist nicht vorhanden => Datei anlegen
#o   2. Datei ist vorhanden, Komponente wird nicht gefunden
#o      => Eintrag hinzufuegen
#o   3. Komponente wird gefunden => Eintrag aktualisieren
save_components_file () {
  if [[ -z "${1}" ]] ; then 
    f_exit_with_message ${EX_PARAMETER} "save_components_file: Component must be set. Abort!";
  fi;
  local COMPONENT_DATE="${1}.date"
  local COMPONENT_PROPERTY="${1}.version"
  local COMPONENT_VALUE="${2:-${RELEASE_NUMBER}}"
  local PROP_FILE=$(get_properties_filename component)

  if [[ ! -f ${PROP_FILE} ]]; then
    #  Fall 1: Datei nicht gefunden => Datei anlegen
    echo "#  DO NOT EDIT MANUALLY!" > ${PROP_FILE}
    echo "#  This file is generated automatically." >> ${PROP_FILE}
    echo "#  DO NOT EDIT MANUALLY!" >> ${PROP_FILE}
    echo "#" >> ${PROP_FILE}
  fi
  if [[ ! -f ${PROP_FILE} ]]; then
    echo "!! Unable to save component status to '${PROP_FILE}'. Abort!"
    exit 91
  fi

  local INSTALL_DATE=$(${VOLATILE_DATE} +"%H:%M:%S %d.%m.%Y")
  set_property "${COMPONENT_PROPERTY}" "${COMPONENT_VALUE}" "${PROP_FILE}"
  set_property "${COMPONENT_DATE}" "${INSTALL_DATE}" "${PROP_FILE}"
}

#  Entfernt in der Component.properties-Datei den Wert einer Komponente
#
#  Eingabeparameter
#o   Name der Komponente, die geprueft werden soll:
#o     mysql,db,bos,dpp,dns,management,migration
#
#  Folgende Faelle koennen auftreten
#o   1. Datei ist nicht vorhanden => Nichts machen
#o   2. Datei ist vorhanden, Komponente wird nicht gefunden
#o      => Nichts machen
#o   3. Komponente wird gefunden => Eintrag entfernen
clear_components_file () {
  if [[ -z "${1}" ]] ; then 
    f_exit_with_message ${EX_PARAMETER} "save_components_file: Component must be set. Abort!";
  fi;
  local COMPONENT_DATE="${1}.date"
  local COMPONENT_PROPERTY="${1}.version"
  local PROP_FILE=$(get_properties_filename component)

  if [[ ! -f ${PROP_FILE} ]]; then
    #  Fall 1: Datei nicht gefunden => Nichts machen
    return 0
  fi

  local INSTALL_DATE=$(${VOLATILE_DATE} +"%H:%M:%S %d.%m.%Y")
  set_property "${COMPONENT_PROPERTY}" ''  "${PROP_FILE}"
  set_property "${COMPONENT_DATE}" "${INSTALL_DATE}" "${PROP_FILE}"
}

get_component_property () {
  local PROP_FILE=$(get_properties_filename "component")
  get_property "${1}" "${PROP_FILE}"
  echo "${CURRENT_PROPERTY}"
}