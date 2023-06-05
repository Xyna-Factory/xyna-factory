
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



# Processing -> Xyna Applications library

# Einstiegspunkt für import_applications() aus blackedition_lib.sh/install_black_edition.sh 
#
# Aufruf-Parameter 1=APP_FILE (Beispiel "xprc/Processing.1.0.app")
# Kommunikation mit Aufrufer über folgende Variablen:
# * APP_LIST             #noch zu installierende Apps
# * IMPORTED_APP_LIST    #importierte Apps
# * FAILED_APP_LIST      #Apps mit Fehler: nicht gefunden; fehlgeschlagener Import 
# * SKIPPED_APP_LIST     #Apps die geskipped werden
# * REQUIRING_APP_LIST   #Apps, die andere Apps als Voraussetzung haben
# * IMPORT_APPLICATIONS_GLOBALLY #Sollen Applications global installiert werden? "" oder "--global"
#
f_import_applications_internal() {
  local APP_FILE=${1}
  local RC;
  local DO_IMPORT=false;
  
  #Überspringen, wenn bereits als fehlerhaft bekannt
  if f_is_in_list FAILED_APP_LIST ${APP_FILE} ; then
    return;
  fi;
  
  #alle erforderten Applications auswerten
  local REQ_APP_FILE_LIST;
  local REQ_APP_FAILED_LIST;
  f_import_applications_internal_get_required_applications ${APP_FILE}
  RC=$?
  
  debug_msg "f_import_applications_internal_get_required_applications: ${RC} ${REQ_APP_FILE_LIST}";
  if [[ ${RC} == 0 ]] ; then
    DO_IMPORT=true;
  elif [[ ${RC} == 1 ]] ; then
    #es konnten nicht alle erforderten Application ermittelt werden, dennoch importieren?
    DO_IMPORT=true;
  else
    #unerwarteter Fehler -> Application kann nicht installiert werden
    FAILED_APP_LIST=$(f_add_to_list FAILED_APP_LIST ${APP_FILE});
    return;
  fi;

  if f_is_true ${DO_IMPORT} ; then
    if [[ -n ${REQ_APP_FILE_LIST} ]] ; then
      #Requirements liegen vor, daher APP_FILE jetzt noch nicht importieren
      DO_IMPORT=false;
      f_import_applications_internal_required;
    fi;
  fi;
    
  if f_is_true ${DO_IMPORT} ; then 
    echo "  - Importing ${APP_FILE}"
    local INDENTATION="    ";
    f_import_application ${APP_FILE}
    RC=$?
    #Austragen, wenn evtl. eingetragen
    REQUIRING_APP_LIST=$(f_remove_from_list REQUIRING_APP_LIST ${APP_FILE});
    
    if [[ ${RC} == 0 ]] ; then
      IMPORTED_APP_LIST=$(f_add_to_list IMPORTED_APP_LIST ${APP_FILE});
    elif [[ ${RC} == 100 ]] ; then
      SKIPPED_APP_LIST=$(f_add_to_list SKIPPED_APP_LIST ${APP_FILE});
    else
      FAILED_APP_LIST=$(f_add_to_list FAILED_APP_LIST ${APP_FILE});
    fi;
  fi;
}

# Ermittlung der von einer Application vorausgesetzten Applications
#
# Aufruf-Parameter 1=APP_FILE (Beispiel "xprc/Processing.1.0.app")
# Kommunikation mit Aufrufer über folgende Variablen:
# * REQ_APP_FILE_LIST    #erforderte Apps
# * FAILED_APP_LIST      #Apps mit Fehler: nicht gefunden
#
f_import_applications_internal_get_required_applications() {
  local APP_FILE=${1}
  local INDENTATION="      ";
  local RC;
  f_xynafactory_silent listapplicationdetails -fileName ${PWD}/components/${APP_FILE} --onlyMissingRequirements
  debug_msg "listapplicationdetails: ${XYNA_FACTORY_RC} ${XYNA_FACTORY_OUTPUT}"
  if [[ ${XYNA_FACTORY_RC} == 0 ]] ; then
    RC=0;
  elif [[ ${XYNA_FACTORY_RC} == 12 ]] ; then
    #required applications
    RC=0;
    local REQ_APP_LIST;
    REQ_APP_LIST=$(echo -e "${XYNA_FACTORY_OUTPUT}" | ${VOLATILE_AWK} '{gsub(/^    /,""); gsub(/ /,"."); print $0}' | tr  '\n' ' ')
    local APP;
    for APP in ${REQ_APP_LIST} ; do
      local APP_FILE=$(f_find_application_in_components ${APP});
      if [[ -z ${APP_FILE} ]] ; then
        if ! f_is_in_list FAILED_APP_LIST ${APP} ; then
          attention_msg "Required application ${APP} cannot be found";
          FAILED_APP_LIST=$(f_add_to_list FAILED_APP_LIST ${APP});
        fi;
        RC=1;
      else
        REQ_APP_FILE_LIST=$(f_add_to_list REQ_APP_FILE_LIST ${APP_FILE});
      fi;
    done;
  else
    #unerwarteter Fehler
    return ${XYNA_FACTORY_RC}
  fi;
  return ${RC}
}

# Verwaltung der von einer Application vorausgesetzten Applications:
# Eintrag in die Verwaltungs-Listen; Prüfung dass kein Abhängigkeits-Zirkel auftritt
#
# Kommunikation mit Aufrufer über folgende Variablen:
# * REQ_APP_FILE_LIST    #erforderte Apps
# * FAILED_APP_LIST      #Apps mit Fehler: nicht gefunden
# * APP_LIST             #noch zu installierende Apps
# * FAILED_APP_LIST      #Apps mit Fehler: nicht gefunden; fehlgeschlagener Import 
# * REQUIRING_APP_LIST   #Apps, die andere Apps als Voraussetzung haben
#
f_import_applications_internal_required () {
  debug_msg "f_import_applications_internal_required ${REQ_APP_FILE_LIST}";
  if f_is_in_list REQUIRING_APP_LIST ${APP_FILE} ; then
    REQUIRING_APP_LIST=$(f_remove_from_list REQUIRING_APP_LIST ${APP_FILE});
    FAILED_APP_LIST=$(f_add_to_list FAILED_APP_LIST ${APP_FILE});
    local MSG="Application ${APP_FILE}\n";
    MSG="${MSG}seems to have still missing requirements ${REQ_APP_FILE_LIST}\n";
    MSG="${MSG}or to have a circle in required applications";
    attention_multi_msg "${MSG}"
    return 1;
  fi;
  
  #aktuelle App kann noch nicht installiert werden, deswegen wieder in Liste eintragen
  APP_LIST=$(f_add_to_list_begin APP_LIST ${APP_FILE});
      
  #aktuelle App in REQUIRING_APP_LIST eintragen, um Zirkel erkennen zu können 
  REQUIRING_APP_LIST=$(f_add_to_list REQUIRING_APP_LIST ${APP_FILE});
  
  local REQ_APP_FILE;
  for REQ_APP_FILE in ${REQ_APP_FILE_LIST} ; do
    #evtl in Gesamt-Liste löschen, damit nicht doppelt
    APP_LIST=$(f_remove_from_list APP_LIST ${REQ_APP_FILE});
    #Requirement vorne eintragen, damit als nächstes ausgewertet
    APP_LIST=$(f_add_to_list_begin APP_LIST ${REQ_APP_FILE});
  done
}


# Eigentlicher Import einer Application
#
# Aufruf-Parameter 1=APP_FILE (Beispiel "xprc/Processing.1.0.app")
#
f_import_application () {
  local APP_FILE=${1}
  local NEW_APP=${APP_FILE%.app}
  NEW_APP=${NEW_APP##*/}
  local FULL_APP_NAME=${NEW_APP}
  IFS='.' read -a SPLIT <<< "${NEW_APP}"
  NEW_APP=${SPLIT[0]}
  local NEW_MAJOR=${SPLIT[1]}
  local NEW_MINOR=${SPLIT[2]}
  local NEW_BUGFIX=${SPLIT[3]}
  local NEW_BRANCH=${SPLIT[4]}
  local NEW_VERSION=${NEW_MAJOR}.${NEW_MINOR}
  INDENTATION="      "

  local PATTERN='^\w+(\.[0-9]+){2,4}$'
  if [[ ${FULL_APP_NAME} =~ ${PATTERN} ]]; then
    echo 
  else
      f_xynafactory importapplication --force -filename ${PWD}/components/${APP_FILE} --stop ${IMPORT_APPLICATIONS_GLOBALLY}
      echo -ne '\a'
      echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
      echo "!!!"
      echo "!!!   Unsupported version representation. Manual migration to ${FULL_APP_NAME} required!"
      echo "!!!"
      echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
      return 100;
  fi

  if [ "x${NEW_BUGFIX}" != "x" ]; then
    NEW_VERSION=${NEW_MAJOR}.${NEW_MINOR}.${NEW_BUGFIX}
  fi
  
  if [ "x${NEW_BRANCH}" != "x" ]; then
    NEW_VERSION=${NEW_MAJOR}.${NEW_MINOR}.${NEW_BUGFIX}.${NEW_BRANCH}
  fi

  # Checken, ob die Application bereits in der gleichen Version existiert
  local APP_EXISTS=$(f_xynafactory listapplications -h | ${VOLATILE_GREP} -v "STATUS: 'AUDIT_MODE'" | ${VOLATILE_GREP} "'${NEW_APP}' '${NEW_VERSION}'" | wc -l)
  if [ ${APP_EXISTS} = 1 ]; then
    # skip
    return 100;
  fi

  # tail -1 gibt nur die höchste Versionsnummer zurück, weil die Ausgabe von listapplications bereits sortiert ist. Migration von älteren Applications muss manuell erfolgen
  local OLD_APP=$(f_xynafactory listapplications -h | ${VOLATILE_GREP} -v "STATUS: 'AUDIT_MODE'" | ${VOLATILE_GREP} "'${NEW_APP}' '" | tail -1)
  local OLD_APP_LINE=$OLD_APP
  # Erzeuge Array in LINE  (Separiert nach leerzeichen), d.h. Name, Version, etc separat
  IFS=' ' read -a LINE <<< "${OLD_APP}"
  OLD_APP=${LINE[0]//\'/}
  local OLD_VERSION=${LINE[1]//\'/}
  # Erzeuge Array der einzelnen Versionsstellen (Separiert nach Punkt)
  IFS='.' read -a SUB_VERSION <<< "${OLD_VERSION}"
  local OLD_MAJOR=${SUB_VERSION[0]}
  local OLD_MINOR=${SUB_VERSION[1]}
  local OLD_BUGFIX=${SUB_VERSION[2]}
  local OLD_BRANCH=${SUB_VERSION[3]}
  local INSTALL=0
  local BUGFIX_INSTALL=0
  local CLEAN_INSTALL=0

  # Check Version
  if [ "x${NEW_APP}" = "x${OLD_APP}" ]; then 
    if ((${NEW_MAJOR} > ${OLD_MAJOR})); then # new major version
      echo "${INDENTATION}major update (${NEW_VERSION} > ${OLD_VERSION})"
      INSTALL=1
    elif ((${NEW_MAJOR} < ${OLD_MAJOR})); then
      echo "${INDENTATION}Found newer version on system."
      INSTALL=1
    elif ((${NEW_MINOR} > ${OLD_MINOR})); then # new minor version
      echo "${INDENTATION}minor update (${NEW_VERSION} > ${OLD_VERSION})"
      INSTALL=1
    elif ((${NEW_MINOR} < ${OLD_MINOR})); then
     echo "${INDENTATION}Found newer version on system."
     INSTALL=1
    elif [ "x${OLD_BUGFIX}" != "x" -o "x${NEW_BUGFIX}" != "x" ]; then #either version has bugfix set
      if [ "x${OLD_BUGFIX}" = "x" -a "x${NEW_BUGFIX}" != "x" ]; then #if old version was not bugfix, but new version is
        echo "${INDENTATION}bugfix update (${NEW_VERSION} > ${OLD_VERSION})"
        INSTALL=1
        BUGFIX_INSTALL=1
      elif [ "x${OLD_BUGFIX}" != "x" -a "x${NEW_BUGFIX}" == "x" ]; then #if old version was not bugfix, but new version is
        echo "newer branch version installed"
        INSTALL=1
      elif [ "x${OLD_BUGFIX}" != "x" -a "x${NEW_BUGFIX}" != "x" ]; then #if old version was bugfix, and new version is bugfix
        if ((${NEW_BUGFIX} > ${OLD_BUGFIX})); then # if bugfix is newer
          echo "${INDENTATION}bugfix update (${NEW_VERSION} > ${OLD_VERSION})"
          INSTALL=1
          BUGFIX_INSTALL=1
        elif ((${NEW_BUGFIX} < ${OLD_BUGFIX})); then # if bugfix is older
          echo "${INDENTATION}Found newer version on system."
          INSTALL=1
        elif [ "x${NEW_BUGFIX}" = "x${OLD_BUGFIX}" ]; then #if both have same bugfix version
          if [ "x${OLD_BRANCH}" != "x" -o "x${NEW_BRANCH}" != "x" ]; then #either version has branch set
            if [ "x${OLD_BRANCH}" = "x" -a "x${NEW_BRANCH}" != "x" ]; then #if old version was not branch, but new version is
              echo "${INDENTATION}branch update (${NEW_VERSION} > ${OLD_VERSION})"
              INSTALL=1
              BUGFIX_INSTALL=1
            elif [ "x${OLD_BRANCH}" != "x" -a "x${NEW_BRANCH}" != "x" ]; then #if old version was branch, and new version is branch
              if ((${NEW_BUGFIX} > ${OLD_BRANCH})); then # if new branch is newer
                echo "${INDENTATION}branch update (${NEW_VERSION} > ${OLD_VERSION})"
                INSTALL=1
                BUGFIX_INSTALL=1
              fi
            elif [ "x${OLD_BRANCH}" != "x" -a "x${NEW_BRANCH}" == "x" ]; then #if old version was not branch, and new version is branch
              echo "${INDENTATION}Found newer version on system."
              INSTALL=1
            fi
          fi
        fi
      fi
    fi
  else
   
    echo "${INDENTATION}installing new app"
    INSTALL=1
    CLEAN_INSTALL=1
  fi

  if [ ${INSTALL} = 1 ]; then
    local MANUAL_MIGRATION=0

    #  Neue Version importieren
    f_xynafactory importapplication --force -filename ${PWD}/components/${APP_FILE} --stop ${IMPORT_APPLICATIONS_GLOBALLY}
    local IMPORT_RESULT=$?
 
    if [ ${IMPORT_RESULT} = 0 -a ${BUGFIX_INSTALL} = 1 ]; then
      #  RuntimeContext-Dependencies umziehen
      f_xynafactory migrateruntimecontext -f -fromApplicationName ${OLD_APP} -fromVersionName ${OLD_VERSION} -migrationTargets All -toApplicationName ${NEW_APP} -toVersionName ${NEW_VERSION}
      
      if [ $? = 0 ] ; then
        APP_RUNNING=$( ${VOLATILE_GREP} "STATUS: 'RUNNING'" <<< "${OLD_APP_LINE}" )
        if [[ "x${APP_RUNNING}" != "x" ]]; then
          f_xynafactory stopapplication -applicationName ${OLD_APP} -versionName ${OLD_VERSION}
        fi
        
        echo "${INDENTATION}removing previous version ${OLD_APP} ${OLD_VERSION}"
      	f_xynafactory removeapplication -f -applicationName ${OLD_APP} -versionName ${OLD_VERSION}
      	
      	if [ $? != 0 ] ; then
      	  echo -ne '\a'
          echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
          echo "!!!"
          echo "!!!   Failed to remove outdated application ${OLD_APP} with version ${OLD_VERSION}!"
          echo "!!!"
          echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
      	fi
      else
        MANUAL_MIGRATION=1;
      fi
    else
      MANUAL_MIGRATION=1;
    fi
    
    if [ ${MANUAL_MIGRATION} = 1 -a ${CLEAN_INSTALL} != 1 ]; then
      echo -ne '\a'
      echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
      echo "!!!"
      echo "!!!   Manual migration of ${OLD_APP} from version ${OLD_VERSION} to ${NEW_VERSION} required!"
      echo "!!!"
      echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    fi
    return ${IMPORT_RESULT}
  else
    # skipped
    return 100;
  fi
}


# Prüfung, ob Application unter components liegt
#
# Aufruf-Parameter 1=Name
# Rückgabe:
#   0, wenn Datei existiert
#
f_application_exists_in_components () {
  local FILENAME;
  local EXISTS;
  #ohne Version im Namen suchen
  FILENAME=$(${VOLATILE_LS} components/*/${1}.[0-9].* 2> /dev/null)
  EXISTS=$?
  if [ ${EXISTS} != 0 ] ; then
    #nochmal direkt suchen (mit Version im Namen)
    FILENAME=$(${VOLATILE_LS} components/*/${1} 2> /dev/null)
    EXISTS=$?
  fi;
  return ${EXISTS}
}

# Rückgabe des DateiNamens, mit der Application unter components liegt
#
# Aufruf-Parameter 1=Name
# Ausgabe: 
#   Dateiname (Beispiel "xprc/Processing.1.0.app")
# Rückgabe:
#   0, wenn Datei existiert
#
f_find_application_in_components () {
  local FILTER;
  local FILENAME;
  local EXISTS;
  FILTER="components/*/${1}.[0-9]*.app";
  FILENAME=$(${VOLATILE_LS} ${FILTER} 2> /dev/null)
  EXISTS=$?
  if [ ${EXISTS} != 0 ] ; then
    FILTER="components/*/${1}.app";
    FILENAME=$(${VOLATILE_LS} ${FILTER} 2> /dev/null)
    EXISTS=$?
  fi;
  if [ ${EXISTS} != 0 ] ; then
    FILTER="components/*/${1}";
    FILENAME=$(${VOLATILE_LS} ${FILTER} 2> /dev/null)
    EXISTS=$?
  fi;
  if [ ${EXISTS} == 0 ] ; then
    echo ${FILENAME#components/};
  fi;
  return ${EXISTS}
}

# Rückgabe der DateiNamen aller Applications unter components
#

# Ausgabe: 
#   Liste Dateiname (Beispiel "xprc/Processing.1.0.app")
#
f_list_applications_in_components () {
  echo $( ${VOLATILE_FIND} components/ -name "*.app" |
          ${VOLATILE_AWK} '{ gsub( /.*\//, "" ); gsub(/\.[0-9\.]+\.app/, "" ); print $0 }' |
          ${VOLATILE_SORT} );
}














