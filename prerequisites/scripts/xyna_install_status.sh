#!/bin/bash

# ---------------------------------------------------
#  Copyright GIP AG 2009
#  (http://www.gip.com)
#
#  Heinrich-von-Brentano-Str. 2
#  55130 Mainz
# ---------------------------------------------------
#  $Revision: 62055 $
#  $Date: 2010-02-17 13:44:02 +0100 (Mi, 17. Feb 2010) $
# ---------------------------------------------------
#
#  Den Installationsstatus aller Komponenten anzeigen.
#  Eigentlich eine aufgehuebschte Variante von
#     cat /etc/xyna/environment/${HOSTNAME}.component.properties

XYNA_ENVIRONMENT_DIR="/etc/xyna/environment"
PROPERTIES_FILE_NAME="${XYNA_ENVIRONMENT_DIR}/${HOSTNAME}.component.properties"
VOLATILE_AWK="/usr/bin/awk"
LENGTH_COMPONENT_NAME="40"
LENGTH_COMPONENT_VERSION="15"

if [[ ! -f "${PROPERTIES_FILE_NAME}" ]]; then
  echo "No components installed. Abort!"
  exit 1
fi

#  Liste aller installierter Komponenten bestimmen
ALL_COMPONENTS=$(${VOLATILE_AWK} 'BEGIN { FS="=" } (NF == 2)  && $1 ~ /.*.version/ { print $1 }' ${PROPERTIES_FILE_NAME} | sort)

#  Ueber alle Komponenten iterieren
for i in ${ALL_COMPONENTS}
do
  #  Status lesen
  unset COMPONENT_DATE
  COMPONENT_DATE=$(${VOLATILE_AWK} 'BEGIN { FS="=" } (NF == 2)  && ($1 == "'"${i%version}date"'") { print $2 }' ${PROPERTIES_FILE_NAME})
  COMPONENT_VERSION=$(${VOLATILE_AWK} 'BEGIN { FS="=" } (NF == 2)  && ($1 == "'"${i}"'") { print $2 }' ${PROPERTIES_FILE_NAME})
  if [[ "x${COMPONENT_VERSION}" == "x" ]]; then
    COMPONENT_VERSION="_removed_"
  fi

  #  Text fuer die Ausgabe etwas aufhuebschen
  OUT_COMPONENT_NAME=$(printf "%-${LENGTH_COMPONENT_NAME}s" "${i}")
  OUT_COMPONENT_VERSION=$(printf "%-${LENGTH_COMPONENT_VERSION}s" "${COMPONENT_VERSION}")
  echo "${OUT_COMPONENT_NAME}: ${OUT_COMPONENT_VERSION} (${COMPONENT_DATE})"
done

#  EOF
