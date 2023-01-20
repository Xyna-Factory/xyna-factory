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

init () {
  load_functions () {
    SOURCE_FILE="${1}"
    if [[ ! -f ${SOURCE_FILE} ]]; then
      echo "Unable to import functions from '${SOURCE_FILE}'. Abort!"; exit 99;
    else
      source ${SOURCE_FILE}
    fi
  }

  #  Generische Funktionen importieren.
  load_functions "$(dirname "$0")/../func_lib/func_lib.sh"

  #  Produktspezifische Funktionen importieren.
  load_functions "$(dirname "$0")/../prerequisites_lib.sh"

  f_read_properties
  f_set_environment


  if [[ ! -d "${TOMCAT_HOME}/webapps" ]]; then
    err_msg " f_start_or_stop_tomcat: unable to locate Apache Tomcat installation in '${TOMCAT_HOME}'"
    exit ${EX_OSFILE}
  fi
}

if [[ "x$1" == "x-c" ]]; then

  init
  FOUND_FILTER_ENTRY=$(grep -c "<!-- update xyna tomcat cachecontrol -->" "${TOMCAT_HOME}/conf/web.xml")
  if [[ "$FOUND_FILTER_ENTRY" != "0" ]]; then
    # update nicht anzeigen, ist bereits durchgeführt
    echo "0 Update has already been applied."
    exit
  fi


  echo "1 Modifies Tomcat web.xml, so that the following HTTP headers are set: Cache-Control: no-cache, no-store, must-revalidate, Pragma: no-cache, Expires: 0"
else #-e
  #execute

  echo "Executing update CacheControlFilter ..."
  echo ""

# folgende schritte durchführen:  
# checken, dass tomcat lokal erreichbar ist
# checken, dass web.xml nicht bereits angepasst ist
# tomcat herunterfahren
# web.xml ändern
# tomcat starten

  init
  
  # f_check_open_port "${HOSTNAME}" "${TOMCAT_HTTP_PORT}"
  # TODO abbrechen, wenn port nicht gefunden?

  FOUND_FILTER_ENTRY=$(grep -c "<!-- update xyna tomcat cachecontrol -->" "${TOMCAT_HOME}/conf/web.xml")
  if [[ "$FOUND_FILTER_ENTRY" != "0" ]]; then
    echo " Update has already been applied."
    exit
  fi
  
  export XML_SNIPPET=$($VOLATILE_CAT << A_HERE_DOCUMENT
  
    <!-- update xyna tomcat cachecontrol -->
    <filter>
       <filter-name>SetCacheControl</filter-name>
       <filter-class>com.gip.xyna.http.CacheControlFilter</filter-class>
    </filter>
    <filter-mapping>
       <filter-name>SetCacheControl</filter-name>
       <url-pattern>/*</url-pattern>
       <dispatcher>REQUEST</dispatcher>
    </filter-mapping>
  
A_HERE_DOCUMENT
)
 
  echo "Stopping tomcat ..."
  echo ""
  stop_tomcat
  
  #copy cachecontroljar, falls nicht vorhanden
  if [ ! -f "${TOMCAT_HOME}/lib/cachecontrolfilter.jar" ]; then
    cp "application/tomcat/lib/cachecontrolfilter.jar" "${TOMCAT_HOME}/lib/cachecontrolfilter.jar"
    echo "Copied cachecontrolfilter.jar to ${TOMCAT_HOME}/lib/"
  else
    echo "cachecontrolfilter.jar already in tomcat/lib"
  fi
  
   
  # backup des web.xmls
  ${VOLATILE_CP} "${TOMCAT_HOME}/conf/web.xml" "${TOMCAT_HOME}/conf/web.xml_$TIMESTAMP"
  # insert xmlsnippet before </web-app>  
  ${VOLATILE_SED} '\|</web-app>| {
                            e echo "$XML_SNIPPET"
                       }' "${TOMCAT_HOME}/conf/web.xml" > tmpfile
  ${VOLATILE_MV} tmpfile "${TOMCAT_HOME}/conf/web.xml"
  echo "Modified ${TOMCAT_HOME}/conf/web.xml"
  echo ""
  
  echo "Starting tomcat ..."
  echo ""
  start_tomcat

fi #end of program