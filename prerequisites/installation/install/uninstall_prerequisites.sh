#!/bin/bash

# ---------------------------------------------------
#  Copyright GIP AG 2013
#  (http://www.gip.com)
#
#  Hechtsheimer Str. 35-37
#  55131 Mainz
# ---------------------------------------------------
#  $Revision: 195562 $
#  $Date: 2017-02-01 16:03:35 +0100 (Mi, 01 Feb 2017) $
# ---------------------------------------------------

if [[ "x$(ps -o "user=" -p $$ | sed -e "s+ ++g")" != "xroot" ]]; then
  echo "This script can only be run as root. Abort!"
  exit 90
fi

#  Diese Variablen muessen gesetzt sein.
PRODUCT_NAME="black_edition"
#  Diese Property wird in der host-spezifischen Properties-Datei abgelegt
HOST_PROPERTIES="ant.folder black_edition.instances"
#  Diese Properties werden in der produkt-spezifischen Properties-Datei abgelegt
PRODUCT_PROPERTIES="installation.folder geronimo.folder geronimo.user geronimo.password tomcat.folder"

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
load_functions "$(dirname "$0")/prerequisites_lib.sh"

#  overwrite some functions that are different from "prerequisites_lib.sh"
display_usage () {
  ${VOLATILE_CAT} << A_HERE_DOCUMENT

  usage: $(basename "$0") -nacfgjstuACFGIJSTU -i instance

-n    dry-run, display the evaluated commandline parameters
-i    specify instancenumber, default is 1
-gG   [don't] uninstall Geronimo (Java EE, incl. Tomcat)
-tT   [don't] uninstall Tomcat

* lower-case letters mean to include the specific component
* UPPER-CASE letters mean to exclude the specific component
* subsequent arguments override prior arguments

Examples:

(1) "$(basename "$0")" -g
      uninstall Geronimo

(2) "$(basename "$0")" -g -t
      install everything

A_HERE_DOCUMENT
}

#  Read host-specific properties
PROP_FILE=$(get_properties_filename "host")
for i in ${HOST_PROPERTIES} ; do
  get_property ${i} "${PROP_FILE}"
  case ${i} in
    ant.folder) ANT_HOME="${CURRENT_PROPERTY}";;
    black_edition.instances) BLACK_EDITION_INSTANCES="${CURRENT_PROPERTY}";;
  esac
done

#  Check, if instance number '-i' in the allowed range
INSTANCE_NUMBER="1"
parse_commandline_arguments "$@"

#  Create next instance, if instance number '-i' is 'NEW'
#  Instance counter is incremented automatically
if [[ "x${INSTANCE_NUMBER}" == "xNEW" ]]; then
  DISPLAY_USAGE="true"
fi

if [[ $# -lt 1 ]]; then DISPLAY_USAGE="true"; fi
if [[ "x${DISPLAY_USAGE}" == "xtrue" ]]; then display_usage; exit; fi

#  Read product-specific properties
PROP_FILE=$(get_properties_filename "product" "${INSTANCE_NUMBER}")
for i in ${PRODUCT_PROPERTIES} ; do
  get_property ${i} "${PROP_FILE}"
  case ${i} in
    geronimo.folder)  GERONIMO_HOME="${CURRENT_PROPERTY}";;
    geronimo.user)  GERONIMO_USER="${CURRENT_PROPERTY}";;
    geronimo.password)  GERONIMO_PASSWORD="${CURRENT_PROPERTY}";;
    tomcat.folder)  TOMCAT_HOME="${CURRENT_PROPERTY}";;
    installation.folder)  INSTALL_PREFIX="${CURRENT_PROPERTY}";;
  esac
done

#  start main logic
if [[ "x${DRY_RUN}" == "xtrue" ]]; then debug_variables; exit; fi
check_component_status "black_edition_prerequisites"
set_platform_dependent_properties
PRODUCT_INSTANCE=$(printf "%03g" ${INSTANCE_NUMBER:-1})

#  Nur entfernen, was installiert wurde.
if [[ "x${COMPONENT_UPDATE}" == "xinstall" ]]; then
  err_msg "Instance '${PRODUCT_INSTANCE}' is not installed. Abort!"
  exit 99
fi

if [[ "${INSTALLATION_PLATFORM}" == "solaris" ]]; then
  FIVE_DIGITS=$(head -10 /dev/urandom | digest -a md5 | tr -d [a-z] | tr -d 0 | cut -c1-5)
else
  FIVE_DIGITS=$(head -10 /dev/urandom | md5sum | tr -d [a-z] | tr -d 0 | cut -c1-5)
fi
${VOLATILE_CAT} << A_HERE_DOCUMENT

###########################################################################
  Removing installation.                                     ${FIVE_DIGITS}
  This is only for development!! NEVER use on production systems!!
###########################################################################

Instance '${PRODUCT_INSTANCE}' is about to be removed:
A_HERE_DOCUMENT

if [[ "x${COMPONENT_GERONIMO}" == "xtrue" ]]; then echo "  + ${GERONIMO_HOME}"; fi
if [[ "x${COMPONENT_TOMCAT}"   == "xtrue" ]]; then echo "  + ${TOMCAT_HOME}"; fi

${VOLATILE_CAT} << A_HERE_DOCUMENT

Enter the code given above to proceed with removing:
A_HERE_DOCUMENT
read ANSWER
if [[ "x${FIVE_DIGITS}" == "x${ANSWER}" ]]; then
  echo "Code OK - proceeding with removing the installation."
  if [[ "x${COMPONENT_GERONIMO}" == "xtrue" ]]; then
    if [[ -d "${GERONIMO_HOME}" ]]; then
      stop_geronimo
      ${VOLATILE_RM} -rf "${GERONIMO_HOME}"
    else
      attention_msg "Unable to locate '${GERONIMO_HOME}'"
    fi
    if [[ -f "/etc/init.d/geronimo_${PRODUCT_INSTANCE}" ]]; then
      ${VOLATILE_RM} -f "/etc/init.d/geronimo_${PRODUCT_INSTANCE}"
    fi
  fi
  if [[ "x${COMPONENT_TOMCAT}"   == "xtrue" ]]; then
    if [[ -d "${TOMCAT_HOME}" ]]; then
      stop_tomcat
      ${VOLATILE_RM} -rf "${TOMCAT_HOME}"
    else
      attention_msg "Unable to locate '${TOMCAT_HOME}'"
    fi
    if [[ -f "/etc/init.d/tomcat_${PRODUCT_INSTANCE}" ]]; then
      ${VOLATILE_RM} -f "/etc/init.d/tomcat_${PRODUCT_INSTANCE}"
    fi
  fi
  #  Update version and installation date
  clear_components_file "black_edition_prerequisites"
else
  echo "Code FAILED - exiting script, nothing happened."
fi

#  EOF
