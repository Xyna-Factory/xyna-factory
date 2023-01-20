#! /bin/bash

# ---------------------------------------------------
#  Copyright GIP AG 2012
#  (http://www.gip.com)
#
#  Hechtsheimer Str. 35-37
#  55131 Mainz
# ---------------------------------------------------

ME=$(basename $0)
VERBOSE="false"

usage () {
echo "usage: ${ME} [xynafactory|geronimo|apache|netstat_num] <instance nummer>"
        echo
        echo "  Returns the state of the specified service as a number for monitoring with Cacti."
  echo
}

#  bugz 8205: Falls $HOME und $HOSTNAME nicht gesetzt ist, dann einen Wert annehmen
if [[ "x" == "x${HOME}" ]]; then export HOME="/root"; fi
if [[ "x" == "x${HOSTNAME}" ]]; then export HOSTNAME=$(/bin/hostname); fi

export LANG=C

SERVICE=${1}
PRODUCT_INSTANCE=${2}
state="255"

if [[ "x${PRODUCT_INSTANCE}" = "x" ]]; then
  echo ${state}
fi

case ${SERVICE} in
  xynafactory)
    XYNAFACTORY_INIT_SCRIPT="/etc/init.d/xynafactory_${PRODUCT_INSTANCE}"
    if [[ -x ${XYNAFACTORY_INIT_SCRIPT} ]]; then
      ${XYNAFACTORY_INIT_SCRIPT} status 1>/dev/null 2>/dev/null
      state=$(echo "$?")
    fi
  ;;
  geronimo)
    GERONIMO_INIT_SCRIPT="/etc/init.d/geronimo_${PRODUCT_INSTANCE}"
    if [[ -x ${GERONIMO_INIT_SCRIPT} ]]; then
       ${GERONIMO_INIT_SCRIPT} status 1>/dev/null 2>/dev/null
      state=$(echo "$?")
   fi
  ;;
  apache2)
    APACHE2_INIT_SCRIPT="/etc/init.d/apache2"
    if [[ -x ${APACHE2_INIT_SCRIPT} ]]; then
      ${APACHE2_INIT_SCRIPT} status 1>/dev/null 2>/dev/null
      state=$(echo "$?")
    fi
  ;;
  netstat_num)
    state=$(netstat -t | grep -c ESTABLISHED)
  ;;
  *) usage; exit 67;;
esac

#  Ausgabe des Status fuer Cacti
echo ${state}

#EOF

