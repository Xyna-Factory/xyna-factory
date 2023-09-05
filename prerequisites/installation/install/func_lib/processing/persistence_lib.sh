
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



# Processing -> Xyna Persistence Layer library


#  Es stehen mehrere Persistencelayer zur Verfuegung.
#+ Von diesen muessen zur Benutzung Instanzen gebildet werden.
#
#  Der Ablauf ist daher wie folgt:
#o   Schritt 1 : Persistencelayer finden           ./xynafactory.sh listpersistencelayers
#o   Schritt 2 : Instantiierten Layer finden.      ./xynafactory.sh listpersistencelayerinstances
#o   Schritt 2a: Layer instantiieren, falls erforderlich (Schritt 2 hatte kein Ergebnis)
#o   Schritt 3 : Tabellen am Persistence Layer registrieren, falls erforderlich
#
#  Eingabeparameter
#o   Basispfad zur Xyna Factory                    ${black_home}
#o   Filterkriterium fuer Persistencelayer         grep -i ${filter_criteria}
#
#  Rueckgabewert
#o   ID des gefundenen Persistencelayers, falls erfolgreich
#o   'false', falls kein Persistencelayer ermittelt werden konnte
#
f_get_available_persistancelayer_id () {
  local BLACK_HOME="${1}"
  local STR_FILTER_CRITERIA="${2}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  local ret_val="false"

  if [[ -z "${BLACK_HOME}" || -z "${STR_FILTER_CRITERIA}" ]]; then
    err_msg "f_get_available_persistancelayer_id: Missing parameters"
  else
    f_check_xyna_online ${BLACK_HOME}
    #  Schritt 1: Persistencelayer finden - ./xynafactory.sh listpersistencelayers
    #o    got:  - 0  com.gip.xyna.xnwh.persistence.memory.XynaMemoryPersistenceLayer
    #o    got:  - 1  com.gip.xyna.xnwh.persistence.javaserialization.XynaJavaSerializationPersistenceLayer
    #o    got:  - 2  com.gip.xyna.xnwh.persistence.mysql.MySQLPersistenceLayer
    #o    got:  - 3  com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer
    #o    got:  - 4  com.gip.xyna.xnwh.persistence.devnull.XynaDevNullPersistenceLayer
    #o    got:  - 6  com.gip.xyna.xnwh.persistence.oracle.OraclePersistenceLayer
    #
    #  Die erste Spalte "got: " rausschneiden. Das deckt den Fall ab, dass eine
    #+ Fabrik laeuft, die die Spalte "got: " nicht ausgibt.
    #
    #  Es gilt die ID herauszufiltern, also die '2' in Spalte 2:
    #
    local PERSISTENCE_LAYER_ID=$(${XYNA_FACTORY_SH} listpersistencelayers | ${VOLATILE_SED} -e "s+^got: ++" | ${VOLATILE_GREP} -i "${STR_FILTER_CRITERIA}" | ${VOLATILE_AWK} '{print $2}')

    if [[ -n "${PERSISTENCE_LAYER_ID}" ]]; then
      ret_val="${PERSISTENCE_LAYER_ID}"
    else
      err_msg "f_get_available_persistancelayer_id: Unable to get persistenceLayerID."
    fi
  fi

  echo "${ret_val}"
}

#  Es stehen mehrere Persistencelayer zur Verfuegung.
#+ Von diesen muessen zur Benutzung Instanzen gebildet werden.
#
#  Eingabeparameter
#o   Basispfad zur Xyna Factory                    ${black_home}
#o   Filterkriterium fuer Persistencelayer         grep ${filter_criteria}
#o   Persistencelayer-ID aus Schritt 1             f_get_available_persistancelayer_id
#o   Verbindungstyp (optional)                     'DEFAULT'
#
#  Rueckgabewert
#o   ID des gefundenen Persistencelayers, falls erfolgreich
#o   'false', falls kein Persistencelayer ermittelt werden konnte
#o   'duplicate', falls mehrere Persistencelayer ermittelt wurden
#
f_get_instantiated_persistencelayer_id () {
  local BLACK_HOME="${1}"
  local STR_FILTER_CRITERIA="${2}"
  local PERSISTENCE_LAYER_ID="${3}"
  local STR_CONNECTION_TYPE="${4:-DEFAULT}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  local ret_val="false"

  if [[ -z "${BLACK_HOME}" || -z "${STR_FILTER_CRITERIA}" || -z "${PERSISTENCE_LAYER_ID}" ]]; then
    err_msg "f_get_instantiated_persistencelayer_id: Missing parameters"
  else
    f_check_xyna_online ${BLACK_HOME}
    #  Schritt 2: Instantiierten Layer finden. - ./xynafactory.sh listpersistencelayerinstances
    #o    got:  - 12  0    DEFAULT  Memory (1 known Tables)
    #o    got:  - 13  3    HISTORY  XML Persistence (storage/Configuration)
    #o    got:  - 14  3    DEFAULT  XML Persistence (storage/XynaFactoryWarehouse)
    #o    got:  - 15  0    DEFAULT  Memory (4 known Tables)
    #o    got:  - 16  2    HISTORY  com.gip.xyna.xnwh.persistence.mysql.MySQLPersistenceLayer@4ebac9b9 (dpileasequery@jdbc:mysql://localhost/audit poolSize=20 timeout=60000)
    #o    got:  - 17  2    HISTORY  com.gip.xyna.xnwh.persistence.mysql.MySQLPersistenceLayer@c5a67c9 (dhcptriggerv4@jdbc:mysql://localhost/xynadhcp poolSize=5 timeout=5000)
    #
    #  Die erste Spalte "got: " rausschneiden. Das deckt den Fall ab, dass eine
    #+ Fabrik laeuft, die die Spalte "got: " nicht ausgibt.
    #
    #  Es gilt die ID in Spalte 2 zu finden, wenn in Spalte 3 die ID aus f_get_available_persistancelayer_id steht, hier 16 und 17:
    #
    local PERSISTENCE_LAYERINSTANCE_ID=$(${XYNA_FACTORY_SH} listpersistencelayerinstances | ${VOLATILE_GREP} "${STR_FILTER_CRITERIA}" | ${VOLATILE_SED} -e "s+^got: ++" | ${VOLATILE_AWK} '$3 == '${PERSISTENCE_LAYER_ID}' && $4 == "'${STR_CONNECTION_TYPE}'" {print $2}')

    if [[ -n "${PERSISTENCE_LAYERINSTANCE_ID}" ]]; then
      ret_val="${PERSISTENCE_LAYERINSTANCE_ID}"
    fi

    if [[ $(echo "${PERSISTENCE_LAYERINSTANCE_ID}" | ${VOLATILE_AWK} 'END {print NR}') -gt 1 ]]; then
  err_msg "f_get_instantiated_persistencelayer_id: Multiple IDs found - improve your filter criteria"
  ret_val="duplicate"
    fi

  fi

  echo "${ret_val}"
}

#  Es stehen mehrere Persistencelayer zur Verfuegung.
#+ Von diesen muessen zur Benutzung Instanzen gebildet werden.
#
#  Eingabeparameter
#o   Basispfad zur Xyna Factory                    ${black_home}
#o   Filterkriterium fuer Persistencelayer         grep ${filter_criteria}
#o   Persistencelayer-ID aus Schritt 1             f_get_available_persistancelayer_id
#o   Verbindungstyp                                DEFAULT, HISTORY, ...
#o   Department                                    xprc, XynaActivation, ...
#o   persistencelayerspezifizische Angaben         $6, $7, $8, ....
#
#  Rueckgabewert
#o   ID des gefundenen Persistencelayers, falls erfolgreich
#o   'false', falls kein Persistencelayer ermittelt werden konnte
#
f_instantiate_persistencelayer () {
  local BLACK_HOME="${1}"
  local STR_FILTER_CRITERIA="${2}"
  local PERSISTENCE_LAYER_ID="${3}"
  local STR_CONNECTION_TYPE="${4}"
  local STR_DEPARTMENT="${5}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  local ret_val="false"

  if [[ -z "${BLACK_HOME}" || -z "${STR_FILTER_CRITERIA}" || -z "${STR_CONNECTION_TYPE}" || -z "${STR_DEPARTMENT}" || -z "${PERSISTENCE_LAYER_ID}" || $# -lt 5 ]]; then
    err_msg "f_instantiate_persistencelayer: Missing parameters"
  else
    shift 5
    f_check_xyna_online ${BLACK_HOME}
    #  Schritt 2a: Layer instantiieren, falls erforderlich (Schritt 2 hatte kein Ergebnis)
    local CURRENT_INSTANCE_ID=$(f_get_instantiated_persistencelayer_id "${BLACK_HOME}" "${STR_FILTER_CRITERIA}" "${PERSISTENCE_LAYER_ID}" "${STR_CONNECTION_TYPE}")
    local STR_FACTORY_RESPONSE=""

    if [[ "${CURRENT_INSTANCE_ID}" == "false" ]]; then
  
  #  Wurden 5 oder mehr Parameter angegeben?
  #+ Bei mehr als 5 Parameter ist '-persistenceLayerSpecifics' anzugeben...
  #
  #  Wegen dem 'shift 5' weiter oben kann hier gegen '$#' geprueft werden
  if [[ $# -gt 0 ]]; then
      debug_msg "${XYNA_FACTORY_SH} instantiatepersistencelayer -connectionType \"${STR_CONNECTION_TYPE}\" -department \"${STR_DEPARTMENT}\" -persistenceLayerID \"${PERSISTENCE_LAYER_ID}\" -persistenceLayerSpecifics \"$*\""
      STR_FACTORY_RESPONSE=$(${XYNA_FACTORY_SH} instantiatepersistencelayer -connectionType "${STR_CONNECTION_TYPE}" -department "${STR_DEPARTMENT}" -persistenceLayerID "${PERSISTENCE_LAYER_ID}" -persistenceLayerSpecifics "$@" 2>&1)
  else
      debug_msg "${XYNA_FACTORY_SH} instantiatepersistencelayer -connectionType \"${STR_CONNECTION_TYPE}\" -department \"${STR_DEPARTMENT}\" -persistenceLayerID \"${PERSISTENCE_LAYER_ID}\""
      STR_FACTORY_RESPONSE=$(${XYNA_FACTORY_SH} instantiatepersistencelayer -connectionType "${STR_CONNECTION_TYPE}" -department "${STR_DEPARTMENT}" -persistenceLayerID "${PERSISTENCE_LAYER_ID}" 2>&1)
  fi
  
  sleep 5
  CURRENT_INSTANCE_ID=$(f_get_instantiated_persistencelayer_id "${BLACK_HOME}" "${STR_FILTER_CRITERIA}" "${PERSISTENCE_LAYER_ID}" "${STR_CONNECTION_TYPE}")
    fi

    #  Bugz 15865: Keine neuen Persistencelayerinstanzen anlegen, wenn unklare Verhaeltnisse vorliegen...
    if [[ "${CURRENT_INSTANCE_ID}" == "duplicate" ]]; then
      err_msg "f_instantiate_persistencelayer: Multiple IDs found - improve your filter criteria"
      echo "${ret_val}"
      return
    fi

    if [[ "${CURRENT_INSTANCE_ID}" != "false" ]]; then
      ret_val="${CURRENT_INSTANCE_ID}"
    else
      err_msg "f_instantiate_persistencelayer: Unable to instantiate persistencelayer."
      if [[ -n "${STR_FACTORY_RESPONSE}" ]]; then
    echo >&2
    echo "${STR_FACTORY_RESPONSE}" >&2
      fi
    fi
  fi

  echo "${ret_val}"
}

#  Es stehen mehrere Persistencelayer zur Verfuegung.
#+ Von diesen muessen zur Benutzung Instanzen gebildet werden.
#
#  Eingabeparameter
#o   Basispfad zur Xyna Factory                    ${black_home}
#o   Persistencelayer-Name                         gewuenschter Name
#o   Verbindungstyp                                DEFAULT, HISTORY, ...
#o   Department                                    xprc, XynaActivation, ...
#o   persistencelayerspezifizische Angaben         $6, $7, $8, ....
#
#  Rueckgabewert
#o   'true', falls erfolgreich
#o   'false', falls kein Persistencelayer ermittelt werden konnte
#
f_instantiate_named_persistencelayer () {
  local BLACK_HOME="${1}"
  local PERSISTENCE_LAYER_INSTANCE_NAME="${2}"
  local PERSISTENCE_LAYER_NAME="${3}"
  local STR_CONNECTION_TYPE="${4}"
  local STR_DEPARTMENT="${5}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  local ret_val="false"

  if [[ -z "${BLACK_HOME}" || -z "${STR_CONNECTION_TYPE}" || -z "${STR_DEPARTMENT}" || -z "${PERSISTENCE_LAYER_INSTANCE_NAME}"  || -z "${PERSISTENCE_LAYER_NAME}" || $# -lt 4 ]]; then
    err_msg "f_instantiate_persistencelayer: Missing parameters"
  else
    shift 5
    f_check_xyna_online ${BLACK_HOME}

    #  Wurden 5 oder mehr Parameter angegeben?
    #+ Bei mehr als 5 Parameter ist '-persistenceLayerSpecifics' anzugeben...
    #
    #  Wegen dem 'shift 5' weiter oben kann hier gegen '$#' geprueft werden
    if [[ $# -gt 0 ]]; then
      debug_msg "${XYNA_FACTORY_SH} instantiatepersistencelayer -connectionType \"${STR_CONNECTION_TYPE}\" -department \"${STR_DEPARTMENT}\" -persistenceLayerName \"${PERSISTENCE_LAYER_NAME}\" -persistenceLayerSpecifics \"$*\""
      STR_FACTORY_RESPONSE=$(${XYNA_FACTORY_SH} instantiatepersistencelayer -connectionType "${STR_CONNECTION_TYPE}" -department "${STR_DEPARTMENT}" -persistenceLayerName "${PERSISTENCE_LAYER_NAME}" -persistenceLayerInstanceName "${PERSISTENCE_LAYER_INSTANCE_NAME}" -persistenceLayerSpecifics "$@" 2>&1)
      ret_val="true"
    else
      debug_msg "${XYNA_FACTORY_SH} instantiatepersistencelayer -connectionType \"${STR_CONNECTION_TYPE}\" -department \"${STR_DEPARTMENT}\" -persistenceLayerName \"${PERSISTENCE_LAYER_NAME}\""
      STR_FACTORY_RESPONSE=$(${XYNA_FACTORY_SH} instantiatepersistencelayer -connectionType "${STR_CONNECTION_TYPE}" -department "${STR_DEPARTMENT}" -persistenceLayerName "${PERSISTENCE_LAYER_NAME}" -persistenceLayerInstanceName "${PERSISTENCE_LAYER_INSTANCE_NAME}" 2>&1)
      ret_val="true"
    fi
  fi

  echo "${ret_val}"
}

#  Es stehen mehrere Persistencelayer zur Verfuegung.
#+ Tabellen in Xyna Factory werden den Persistencelayern zugeordnet.
#
#  Eingabeparameter
#o   Basispfad zur Xyna Factory                    ${black_home}
#o   Tabellenname
#o   Verbindungstyp (optional)                     'DEFAULT'
#
#  Rueckgabewert
#o   ID des gefundenen Persistencelayers, falls erfolgreich
#o   'false', falls kein Persistencelayer ermittelt werden konnte
#
f_list_table_config () {
  local BLACK_HOME="${1}"
  local TABLE_NAME="${2}"
  local STR_CONNECTION_TYPE="${3:-DEFAULT}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  local ret_val="false"

  if [[ -z "${BLACK_HOME}" || -z "${TABLE_NAME}" ]]; then
    err_msg "f_list_table_config: Missing parameters"
  else
    f_check_xyna_online ${BLACK_HOME}
    #  Herausfinden, an welcher Instanz die Tabelle haengt
    #o    got:  - idgeneration (DEFAULT)  ->  8=Java Serialization
    #o    got:  - optionsv4 (HISTORY)  ->  17=com.gip.xyna.mysql.MySQLPersistenceLayer@c5a67c9 (xyna@jdbc:mysql://localhost/xynadb poolSize=5 timeout=5000)
    #
    #  Die erste Spalte "got: " rausschneiden. Das deckt den Fall ab, dass eine
    #+ Fabrik laeuft, die die Spalte "got: " nicht ausgibt.
    #
    #  Tabellenname (Spalte 2) und Connection-Type (Spalte 3) filtern, '=', '(' und ')' entfernen,
    #+ dann ist die gesuchte Instanz-ID in Spalte 5
    #
    local CURRENT_INSTANCE_ID=$(${XYNA_FACTORY_SH} listtableconfig | ${VOLATILE_SED} -e "s+[=()]+ +g" | ${VOLATILE_SED} -e "s+^got: ++" | ${VOLATILE_AWK} '$2 == "'${TABLE_NAME}'" && $3 == "'${STR_CONNECTION_TYPE}'" {print $5}')
    
    if [[ -n "${CURRENT_INSTANCE_ID}" ]]; then
      ret_val="${CURRENT_INSTANCE_ID}"
    fi
  fi

  echo "${ret_val}"
}

#  Es stehen mehrere Persistencelayer zur Verfuegung.
#+ Tabellen in Xyna Factory werden den Persistencelayern zugeordnet.
#
#  Eingabeparameter
#o   Basispfad zur Xyna Factory                    ${black_home}
#o   Persistencelayer-ID aus Schritt 2             f_get_instantiated_persistencelayer_id
#o   Tabellenname
#o   Verbindungstyp (optional)                     'DEFAULT'
#o   zusaetzliche Parameter                        werden als "$@" weitergereicht
#
#  Rueckgabewert
#o   nichts, es muss mittels f_list_table_config gesondert
#o           ueberprueft werden, ob das registrieren erfolgreich war.
#
f_register_table () {
  local BLACK_HOME="${1}"
  local PERSISTENCE_LAYER_ID="${2}"
  local TABLE_NAME="${3}"
  local STR_CONNECTION_TYPE="${4:-DEFAULT}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  local ret_val="false"

  #  Parameter shiften, damit in "$@" das Richtige drinsteht
  if [[ $# -le 3 ]]; then 
    shift $#
  else
    shift 4
  fi

  if [[ -z "${BLACK_HOME}" || -z "${TABLE_NAME}" || -z "${PERSISTENCE_LAYER_ID}" ]]; then
    err_msg "f_list_table_config: Missing parameters"
  else   
    f_check_xyna_online ${BLACK_HOME}
    #  Tabelle umhaengen, falls sie an der falschen Persistencelayerinstanz registriert ist
    local CURRENT_INSTANCE_ID=$(f_list_table_config "${BLACK_HOME}" "${TABLE_NAME}" "${STR_CONNECTION_TYPE}")
    if [[ "${CURRENT_INSTANCE_ID}" != "${PERSISTENCE_LAYER_ID}" ]]; then
      debug_msg "${XYNA_FACTORY_SH} registertable -tableName \"${TABLE_NAME}\" -persistenceLayerInstanceID \"${PERSISTENCE_LAYER_ID}\""
      ${XYNA_FACTORY_SH} -q registertable -tableName "${TABLE_NAME}" -persistenceLayerInstanceID "${PERSISTENCE_LAYER_ID}" "$@"
    fi
  fi
}

#  Es stehen mehrere Persistencelayer zur Verfuegung.
#+ Tabellen in Xyna Factory werden den Persistencelayern zugeordnet.
#
#  Eingabeparameter
#o   Basispfad zur Xyna Factory                    ${black_home}
#o   Persistencelayer-Name aus Schritt 2           f_instantiate_named_persistencelayer
#o   Tabellenname
#o   Verbindungstyp (optional)                     'DEFAULT'
#o   zusaetzliche Parameter                        werden als "$@" weitergereicht
#
#  Rueckgabewert
#o   nichts, es muss mittels f_list_table_config gesondert
#o           ueberprueft werden, ob das registrieren erfolgreich war.
#
f_register_table_by_name () {
  local BLACK_HOME="${1}"
  local PERSISTENCE_LAYER_NAME="${2}"
  local TABLE_NAME="${3}"
  local STR_CONNECTION_TYPE="${4:-DEFAULT}"
  local XYNA_FACTORY_SH="${BLACK_HOME}/server/xynafactory.sh"
  local ret_val="false"

  #  Parameter shiften, damit in "$@" das Richtige drinsteht
  if [[ $# -le 3 ]]; then 
    shift $#
  else
    shift 4
  fi

  if [[ -z "${BLACK_HOME}" || -z "${TABLE_NAME}" || -z "${PERSISTENCE_LAYER_NAME}" ]]; then
    err_msg "f_list_table_config: Missing parameters"
  else   
    f_check_xyna_online ${BLACK_HOME}
    f_xynafactory registertable -tableName "${TABLE_NAME}" -persistenceLayerInstanceName "${PERSISTENCE_LAYER_NAME}" "$@"
  fi
}

