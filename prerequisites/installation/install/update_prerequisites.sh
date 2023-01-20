#!/bin/bash

# ---------------------------------------------------
#  Copyright GIP AG 2018
#  (http://www.gip.com)
#
#  Hechtsheimer Str. 35-37
#  55131 Mainz
# ---------------------------------------------------
#  $Revision: 69292 $
#  $Date: 2010-06-01 16:08:05 +0200 (Di, 01. Jun 2010) $
# ---------------------------------------------------

if [[ "x$(ps -o "user=" -p $$ | sed -e "s+ ++g")" != "xroot" ]]; then
  echo "This script can only be run as root. Abort!"
  exit 90
fi

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
load_functions "$(dirname "$0")/prerequisites_lib.sh"

display_usage () {
   ${VOLATILE_CAT} << A_HERE_DOCUMENT
  usage: $(basename "$0") <component> -i <instance number> [-v] ( [-n] | [-s] | [-u <comma or space separated list of update numbers>] )
<component> is one of "tomcat".
-i: instance number of installation. default=1
-n: dry run: shows what the update will do
-s: show available updates
-u: select specific updates to install (shown by -s). default=all updates
-v: verbose    

  example: $(basename "$0") tomcat -u "1, 3" -v

A_HERE_DOCUMENT
}

#  Aufruf mit vollem Pfad ermoeglichen
cd "$(dirname "$0")"
check_target_platform

if [[ $# -lt 1 ]]; then display_usage; exit; fi

debug () {  
  if [[ "x${DEBUG_ENABLED}" == "xtrue" ]]; then
    if [[ "x$1" == "x" ]]; then
      echo "";
    else 
      echo "--$1";
    fi 
  fi
}

error () {
  echo "!!!!!!!!!!!!!!!!!!!!! ERROR !!!!!!!!!!!!!!!!!!!!!"
  echo "!!!! ${1}"   
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo ""
}

parse_commandline_arguments () {
  DRY_RUN="false";
  SHOW_UPDATES="false";
  DEBUG_ENABLED="false";
  export INSTANCE_NUMBER="1";
  #component auslesen und parameter um eins shiften, damit die component nicht bei getopts mit berücksichtigt wird
  COMPONENT_NAME=$1
  shift 1
  while getopts ":i:nsvu:" OPTION;
  do
    case ${OPTION} in
      	i) export INSTANCE_NUMBER="${OPTARG}";;
        n) DRY_RUN="true";;
        s) SHOW_UPDATES="true";;
        u) SELECTED_UPDATES=($(f_split_to_array "${OPTARG}"));;        
        v) DEBUG_ENABLED="true";;
        *) error "unexpected option: ${OPTARG}"; display_usage; exit;;        
    esac
  done
  
  debug "Command line parameter are:"
  debug " component: ${COMPONENT_NAME}"
  debug " dry run: ${DRY_RUN}"
  debug " show updates: ${SHOW_UPDATES}"
  debug " verbose: ${DEBUG_ENABLED}"
  debug " selected updates: ${SELECTED_UPDATES[*]}"
  debug " instance number: ${INSTANCE_NUMBER}"
  debug ""
}

parse_commandline_arguments "$@"

#check valid component
if [[ "x${COMPONENT_NAME}" == "xtomcat" ]]; then
  #ntbd
  :
else
   error "Unknown component" 
   display_usage; 
   exit; 
fi

get_available_updates () {
# suche alle updates für die in $1 übergebene component und setze 
# $AVAILABLE_UPDATES in der form: für jedes update gibt es eine zeile mit 3 spalten. 
# { filename, nummer, beschreibung }
# falls nummer mit 0 beginnt, gilt das update als bereits installiert und wird nicht angezeigt.
# TODO nach component sortieren
  debug "checking updates ..."
  debug ""
  AVAILABLE_UPDATES=$(for i in $(find updates -name "update*.sh"); do output="$($i -c | grep -v "^0")"; if [[ "x${output}" != "x" ]]; then echo "$i $output"; fi; done)
}

get_available_updates ${COMPONENT_NAME};

show_updates () {
  if [[ "x${AVAILABLE_UPDATES}" == "x" ]]; then
    echo "No updates found"
  else
    echo "Available updates:"
    echo "${AVAILABLE_UPDATES}" | ${VOLATILE_AWK} '{print $2 ") "; $1=$2=""; print $0 }'
  fi
}

if [[ "x${SHOW_UPDATES}" == "xtrue" ]]; then
  show_updates;
  exit
fi

select_updates () { 
# $SELECTED_UPDATES validieren und korrekt befüllen
# kann bisher leer sein, dann alle available updates verwenden
  if [[ "x${SELECTED_UPDATES[*]}" == "x" ]]; then
    SELECTED_UPDATES=( $(echo "${AVAILABLE_UPDATES}" | ${VOLATILE_AWK} '{print $2}') )
  else
    ALL_UPDATES=( $(echo "${AVAILABLE_UPDATES}" | ${VOLATILE_AWK} '{print $2}') )
#    echo "allupdates: ${ALL_UPDATES[*]}"
#    echo "selected: ${SELECTED_UPDATES[*]}"
    SELECTED_UPDATES_NEW=()
    for item1 in "${SELECTED_UPDATES[@]}"; do
#      echo "itemsel: $item1"
      let found=0
      for item2 in "${ALL_UPDATES[@]}"; do
#        echo "itemall: $item2"
        if [[ "$item1" == "$item2" ]]; then
          SELECTED_UPDATES_NEW+=( "$item1" )
          found=1
          break
        fi
      done
      if [[ "${found}" == "0" ]]; then
        error "Selected update <${item1}> not found."
        exit 1;
      fi        
    done
    SELECTED_UPDATES=${SELECTED_UPDATES_NEW}
  fi
  debug "Validated selected updates: ${SELECTED_UPDATES[*]}"
}

select_updates

dry_run () {
  echo "The following updates for ${COMPONENT_NAME} are chosen:"
  for item1 in "${SELECTED_UPDATES[@]}"; do
    echo "${AVAILABLE_UPDATES}" | ${VOLATILE_GREP} "^[^ ]*.sh $item1 "|  ${VOLATILE_AWK} '{print $2 ") "; $1=$2=""; print $0 }'
  done
  exit
}

if [[ "x${DRY_RUN}" == "xtrue" ]]; then
  dry_run;
  exit
fi

debug "Executing selected updates..."
for item1 in "${SELECTED_UPDATES[@]}"; do
  echo "${AVAILABLE_UPDATES}" | ${VOLATILE_GREP} "^[^ ]*.sh $item1 "|  ${VOLATILE_AWK} '{print "Updating " $2 ") "; $1=$2=""; print $0 }'
  echo ""
  PROG=$(echo "${AVAILABLE_UPDATES}" | ${VOLATILE_GREP} "^[^ ]*.sh $item1 "|  ${VOLATILE_AWK} '{print $1 }')
  $PROG -e
done

echo ""
echo "Update finished!"
