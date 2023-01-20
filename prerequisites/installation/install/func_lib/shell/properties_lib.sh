# --------------------------------------------------
#  Copyright GIP AG 2013
#  (http://www.gip.com)
#
#  Hechtsheimer Str. 35-37
#  55131 Mainz
# ---------------------------------------------------
#  $Revision: 143988 $
#  $Date: 2013-12-17 10:19:43 +0100 (Di, 17. Dez 2013) $
# ---------------------------------------------------

# Shell -> Properties library

#  Vordefinierte Parameter, die immer benoetigt werden
XYNA_ETC_DIR="/etc/opt/xyna"
XYNA_ETC_DIR_OBSOLETE="/etc/xyna"

##############################
# Variablen XYNA_ENVIRONMENT_DIR und XYNA_CACHE_DIR setzen
#
f_set_environment_dir () {
   #+ $VOLATILE stehen zu diesem Zeitpunkt evtl. noch nicht zur Verfuegung!
  local BENUTZER=$(ps -o "user=" -p $$ | sed -e "s+ ++g")

  if [[ "x${BENUTZER}" == "xroot" && -d "${XYNA_ETC_DIR_OBSOLETE}" && ! -L "${XYNA_ETC_DIR_OBSOLETE}" ]]; then
      echo -n "* Migrating '${XYNA_ETC_DIR_OBSOLETE}' -> '${XYNA_ETC_DIR}' ... "
      mkdir -p "${XYNA_ETC_DIR}/"
      cp -rp ${XYNA_ETC_DIR_OBSOLETE}/* "${XYNA_ETC_DIR}/"
      if [[ $? -gt 0 ]]; then
    echo "failed"
    err_msg "Cannot migrate Xyna etc directory - check manually"
      else
    echo "ok"
    rm -rf "${XYNA_ETC_DIR_OBSOLETE}/"
          # Backward-Compatibility for older func_lib.sh on the system 
    ln -s "${XYNA_ETC_DIR}" "${XYNA_ETC_DIR_OBSOLETE}"
      fi
  fi

  if [[ "x${BENUTZER}" == "xroot" && -d "${HOME}/environment" && -d "${HOME}/var" ]]; then
      XYNA_ENVIRONMENT_DIR="${HOME}/environment"
      XYNA_CACHE_DIR="${HOME}/var"
      return
  fi

  if [[ "x${BENUTZER}" == "xroot" ]]; then
    f_mkdir "${XYNA_ETC_DIR}/environment"
  fi

  if [[ -d "${XYNA_ETC_DIR}" ]]; then
      XYNA_ENVIRONMENT_DIR="${XYNA_ETC_DIR}/environment"
      XYNA_CACHE_DIR="${XYNA_ETC_DIR}/var"
  elif [[ -d "${XYNA_ETC_DIR_OBSOLETE}" ]]; then
      XYNA_ENVIRONMENT_DIR="${XYNA_ETC_DIR_OBSOLETE}/environment"
      XYNA_CACHE_DIR="${XYNA_ETC_DIR_OBSOLETE}/var"
  else
      XYNA_ENVIRONMENT_DIR="${HOME}/environment"
      XYNA_CACHE_DIR="${HOME}/var"
  fi

  f_mkdir "${XYNA_ENVIRONMENT_DIR}"
}

f_check_or_create_property_file () {
  if [[ -n "${2}" ]]; then
    local PRODUCT_INSTANCE="${2}"
  fi
  if [[ "x${PRODUCT_NAME}" == "xhost" ]]; then f_exit_with_message ${EX_PARAMETER} "check_properties_file: PRODUCT_NAME must not be 'host'. Abort!"; fi
  if [[ "x${PRODUCT_NAME}" == "x" ]]; then f_exit_with_message ${EX_PARAMETER} "check_properties_file: PRODUCT_NAME needs to be set. Abort!"; fi

  local PROP_FILE;
  case "${1}" in
    host)            PROP_FILE=$(get_properties_filename host );;
    ${PRODUCT_NAME}) PROP_FILE=$(get_properties_filename product ${PRODUCT_INSTANCE} );;
    *) f_exit_with_message ${EX_PARAMETER} "check_properties_file: Missing parameter: Choose 'host' or '${PRODUCT_NAME}'. Abort!";;
  esac

  f_set_environment_dir
  if [[ "$?" -ne 0 ]]; then return; fi
  
  #  Ist die Datei vorhanden, falls nein => anlegen
  if [[ ! -f "${PROP_FILE}" ]]; then
    echo "# Edit to suit your needs" > "${PROP_FILE}"
    echo "# $(${VOLATILE_DATE} +"%H:%M:%S %d.%m.%Y")" >> "${PROP_FILE}"
    echo "#" >> "${PROP_FILE}"
  fi
  
  if [[ ! -f ${PROP_FILE} ]]; then
    echo "!! Unable to create file '$(basename "${PROP_FILE}")'.Abort!"
    exit 91
  fi
  
  echo "${PROP_FILE}";
}


#  Properties-Datei pruefen:
#o ist die Datei vorhanden, falls nein => anlegen
#o sind alle Properties enthalten, falls nein => nachtragen
#o sind alle Properties mit Werten belegt, falls nein => Fehlermeldung + Abbruch
#
#  Eingabeparameter
#o   host, steuert ob Host- oder Produkt-Properties geprueft werden sollen.
#o   ${PRODUCT_NAME}
#
#  Optionale Parameter
#o   Instanznummer.
#
#  Notwendige Umgebungsvariable:
#o   PRODUCT_NAME, Daraus wird der Name der zu pruefenden Properties-Datei hergeleitet
check_properties_file () {
  local PRODUCT_INSTANCE="${2}"
  local PROP_FILE=$(f_check_or_create_property_file ${1} ${2});
  
  if f_selected "${VERBOSE}" ; then 
    echo "* Updating properties in '$(basename "${PROP_FILE}")' if needed.";
  fi
  
  case "${1}" in
    host)
      f_check_all_properties "${PROP_FILE}" "${HOST_PROPERTIES}"
      ;;
    ${PRODUCT_NAME})
      f_check_all_properties "${PROP_FILE}" "${PRODUCT_GROUP_PROPERTIES}"
      f_read_product_group_properties
      f_check_all_properties "${PROP_FILE}" "${PRODUCT_PROPERTIES}"
      ;;
  esac
}

f_check_all_properties () {
  local PROP_FILE="${1}"
  local FEHLER;
  # Sind alle Properties enthalten und mit Werten belegt
  for i in ${2} ; do
    # Pruefen, ob die Property ueberhaupt in der Datei vorhanden ist (evtl. auch mit leerem Wert)
    check_property ${i} "${PROP_FILE}"
    ret_val=$?
    case ${ret_val} in
  #   1) FEHLER="${FEHLER}Property added: ${i}\n";;
      2) FEHLER="${FEHLER}Multiple instances of '${i}'. Fix manually!\n";;
    esac
    unset ret_val
  done
  if [[ "x" != "x${FEHLER}" ]]; then
    echo -e "!! Error while reading '"${PROP_FILE}"'. Abort!\n\n${FEHLER}"
    exit 94
  fi
}

#  Wert der Property auslesen oder Default-Wert nehmen
#
#  Eingabeparameter:
#  o   Name der Property
#  o   Name der Property-Datei. Wenn fehlend, wird $(get_properties_filename_default) verwendet
#
#  Falls die Property nciht gesetzt ist, wird der Default aus  $(default_value_for_property ${PROPERTY}) verwendet
#  Der Wert wird in die globale Variable CURRENT_PROPERTY geschrieben
get_property () {
  PROPERTY="${1}"
  local PROP_FILE="${2}"
  if [[ -z ${PROP_FILE} ]] ; then
    PROP_FILE=$(get_properties_filename_default)
  fi;
  
  if [[ -z "${PROPERTY}" ]]; then
    f_exit_with_message ${EX_PARAMETER} "get_property: Unable to work without a property name. Abort!"
  fi
  CURRENT_PROPERTY=""
  if [[ -r "${PROP_FILE}" ]]; then
    #export CURRENT_PROPERTY=$(${VOLATILE_AWK} 'BEGIN { FS="=" } (NF == 2) && ($1 == "'${PROPERTY}'") { print $2 }' "${PROP_FILE}")
    #export CURRENT_PROPERTY=$(${VOLATILE_AWK} 'BEGIN { FS="=" } ($1 == "'${PROPERTY}'") { pos = index($0, "="); value = substr($0, pos+1); print(value); }' "${PROP_FILE}")
    export CURRENT_PROPERTY=$(${VOLATILE_PERL} -e '$prop = shift; while (<>) {chomp(); ($key, $val) = split(/=/, $_, 2); if ($key eq $prop) {print ($val);}}' "${PROPERTY}" "${PROP_FILE}")
  else
     f_exit_with_message ${EX_OSFILE} "Cannot open file '${PROP_FILE}'"
  fi
  if [[ -z "${CURRENT_PROPERTY}" ]]; then
    export CURRENT_PROPERTY=$(default_value_for_property ${PROPERTY})
  fi
}

#  Wert der Property setzen oder loeschen
#
#  Eingabeparameter:
#  o   Name der Property
#  o   Wert der Property
#  o   Name der Property-Datei. Wenn fehlend, wird $(get_properties_filename_default) verwendet
#
#  Ein leerer Wert ist gleichbedeutend mit dem Loeschen der Property!
set_property () {
  local PROPERTY="${1}"
  local VALUE="${2}"
  local PROP_FILE="${3}"
  if [[ -z ${PROP_FILE} ]] ; then
    PROP_FILE=$(get_properties_filename_default)
  fi;
  
  if [[ -z "${PROPERTY}" ]]; then
    f_exit_with_message ${EX_PARAMETER} "set_property: Unable to work without a property name. Abort!"
  fi
  if [[ -w "${PROP_FILE}" ]]; then
    check_property "${PROPERTY}" "${PROP_FILE}" "none"
    f_replace_in_file "${PROP_FILE}" "s+^${PROPERTY}=.*+${PROPERTY}=${VALUE}+"
  else
    attention_msg "set_property \"${PROPERTY}=${VALUE}\" in ${PROP_FILE} failed: not writeable"
  fi
}

#  Ist die Property ueberhaupt in der Datei enthalten?
#+ Falls nein, die Property mit Default-Wert anlegen
#
#  Eingabeparameter
#o   Name der Property
#o   Property-File-Name
#o   [optional] {confirm,default,none} 
#                confirm: neuen Wert betaetigen lassen; default: default-Wert erzeugen; none: Leerstring
#
#  Ergebniswerte
#o   0 = erfolgreich, ein Eintrag fuer die Property existiert.
#+       Ob jedoch ein Wert eingetragen ist, wurde nicht ueberprueft.
#+       Hierzu get_property verwenden!
#o   1 = Property nicht gefunden, Eintrag wurde der Datei angefuegt.
#o   2 = Property mehrfach vorhanden, Fehler muss manuell beseitigt werden.
check_property () {
  local PROPERTY="${1}"
  local PROP_FILE="${2}"
  local CREATION_MODE="${3:-confirm}"
  
  if [[ -z "${PROPERTY}" ]]; then
    f_exit_with_message ${EX_PARAMETER} "check_property: Unable to work without a property name. Abort!"
  fi
  if [[ -z ${PROP_FILE} ]] ; then
    PROP_FILE=$(get_properties_filename_default)
  fi;
  
  local exit_code="0"
  if [[ -f "${PROP_FILE}" ]]; then
    local ret_val=$(f_count_occurrencies_in_file "^${PROPERTY}[= ]" "${PROP_FILE}")
    case ${ret_val} in
      0) # Property ist nicht vorhanden, also anlegen, falls benötigt
        local create_prop=$(f_check_property_creation ${PROPERTY});
        if f_selected ${create_prop} ; then
          local COMMENT=$(f_create_comment ${PROPERTY});
          if [[ -n "${COMMENT}" ]]; then COMMENT="${COMMENT}\n"; fi
          local VALUE="";
          case ${CREATION_MODE} in
            none)    VALUE="" ;;
            default) VALUE=$(default_value_for_property "${PROPERTY}") ;;
            confirm) VALUE=$(default_value_for_property "${PROPERTY}")
                     VALUE=$(f_confirm_property "${PROPERTY}" "${VALUE}")
                     ;;
            *) f_exit_with_message ${EX_PARAMETER} "check_property: Invalid CREATION_MODE=${CREATION_MODE}. Abort!"
          esac
          echo -e "${COMMENT}${PROPERTY}=${VALUE}" >> "${PROP_FILE}"
          f_fill_selectors ${PROPERTY} ${VALUE};
          unset COMMENT
        fi;
        exit_code="1";;
      1) # Property ist vorhanden, nichts zu tun
        exit_code="0";;
      *) # Mehrfaches Auftreten des Properties ist ein Fehler!
        exit_code="2";;
    esac
  else
    # Property-Datei existiert nicht
    exit_code="3";
  fi
  return ${exit_code}
}

f_check_property_creation () {
  if [[ ${PROPERTY%%.*} != ${PROPERTY} ]] ; then
    case ${PROPERTY%%.*} in
      cluster)    echo ${CLUSTER_SELECTED} ;;
      geronimo)   echo ${GERONIMO_SELECTED} ;;
      sipadapter) echo ${SIPADAPTER_SELECTED} ;;
      *)          echo "true";
     esac
  else
     echo "true";
  fi
}

f_fill_selectors () {
  case ${1} in
    cluster)    CLUSTER_SELECTED=${2} ;;
    geronimo)   GERONIMO_SELECTED=${2} ;;
    sipadapter) SIPADAPTER_SELECTED=${2} ;;
  esac
}

get_properties_filename_default () {
   echo $(get_properties_filename "product" ${INSTANCE_NUMBER})
}

f_create_comment () {
  local PROPERTY="${1}"
  local COMMENT;
  case ${PROPERTY##*.} in
    port)      COMMENT="# portnumber";;
    user)      COMMENT="# username";;
    password)  COMMENT="# non-encrypted password";;
    filter)    COMMENT="# hostname filter rule per user, e.g. enter '%' for every host";;
    ipAddress) COMMENT="#\n# ip-address in dotted notation, e.g. 192.168.178.35";;
    netmask)   COMMENT="# netmask in CIDR notation, e.g. enter 24 if you want 255.255.255.0 as netmask";;
    folder)    COMMENT="# folder without trailing slash";;
    size)      COMMENT="# size in MB";;
  esac
  case ${PROPERTY} in
    jvm.option.gc)        COMMENT="# jvm.option.gc=-verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails";;
    jvm.option.debug)     COMMENT="# jvm.option.debug=-Xrunhprof:cpu=samples,format=a,file=hprof.txt,cutoff=0,depth=12,thread=y";;
    jvm.option.profiling) COMMENT="# jvm.option.profiling=-javaagent:profiling/profile/profile.jar -Dprofile.properties=profiling/profile.properties";;
    jvm.option.additional) COMMENT="# jvm.option.additional=-Doracle.net.tns_admin=/etc/xyna/environment -Duser.timezone=GMT";;
    jvm.option.xml.backup) COMMENT="# true/false configures if backup files for xml persistence will be created";;
  esac
  echo ${COMMENT}
}

#  Interaktive Rueckfrage wie der Wert einer Property zu setzen ist
#+ Ohne Eingabe eines Wertes wird der vorgeschlagene Wert verwendet
#
#  Eingabeparameter
#o   Name der Property
#o   Defaultwert
#o   [optional] Bestaetigung des Wertes ueberspringen
#
#  Rueckgabe
#o   String mit dem zu verwendenden Wert
#
f_confirm_property () {
    local PROPERTY="${1}"
    local VALUE="${2}"
    local BOOL_SKIP_CONFIRM="${3:-false}"
    local ANSWER=""
    local DEFAULT_LOCALE=""

  case "${PROPERTY}" in
    *.local.ipAddress|geronimo.ipAddress)
      #  Gibts mehrere Interfaces, dann diese anzeigen
      if [[ ${local_interface_counter:-0} -ge 2 ]]; then
        echo "Following ip-addresses found:" >&2
        local i=0
        while [[ ${i} -lt ${local_interface_counter} ]] ; do
          echo "   ${local_interface[${i}]} [${local_interface_name[${i}]}]" >&2
          let i=i+1
        done
      fi
      #  Wurde keine Wert angegeben, dann das erste Interface als default vorschlagen
      if [[ -z "${VALUE}" ]]; then
        VALUE="${local_interface[0]}"
      fi
      ;;
    os.locale)
      #  eine Auswahl der verfuegbaren Locale anzeigen
      echo "Following valid locales exist:" >&2
      locale -a | ${VOLATILE_GREP} -i utf >&2
      #  Sinnvollen Vorgabewert ermitteln
      #+ 1. versuchen 'de_DE.utf8' zu finden
      DEFAULT_LOCALE=$(locale -a | ${VOLATILE_GREP} -i "utf" | ${VOLATILE_GREP} "de_DE")
      #+ 2. versuchen 'C.UTF-8' zu finden
      if [[ -z "${DEFAULT_LOCALE}" ]]; then
        DEFAULT_LOCALE=$(locale -a | ${VOLATILE_GREP} -i "utf" | ${VOLATILE_GREP} "^C")
      fi
      #+ 3. 'C' geht immer
      if [[ -z "${DEFAULT_LOCALE}" ]]; then
        DEFAULT_LOCALE="C"
      fi
      #  Wurde keine Wert angegeben, dann ein passendes locale vorschlagen
      if [[ -z "${VALUE}" ]]; then
        VALUE="${DEFAULT_LOCALE}"
      else
        #  Pruefen, ob der gegebene Wert sinnvoll ist
        #+ Gibt das 'grep' eine Eins, so wurde die angegebene locale gefunden
        #+ Ist der Wert ungleich Eins, so wurde die locale nicht gefunden => Vorgabe verwenden
        if [[ $(locale -a | ${VOLATILE_GREP} -c "${VALUE}") -ne 1 ]]; then
          VALUE="${DEFAULT_LOCALE}"
        fi
      fi
      ;;
    esac
    
    if [[ "x${BOOL_SKIP_CONFIRM}" != "xtrue" ]]; then
      echo >&2
      echo "Enter '${PROPERTY}' [${VALUE}]:" >&2
      read ANSWER
    fi

    if [[ -z "${ANSWER}" ]]; then ANSWER="${VALUE}"; fi
    
    echo "${ANSWER}"
}

#  Dateiname fuer die Properties-Datei setzen
#
#  Eingabeparameter
#o   host fuer host-spezifischen Dateinamen
#o   product fuer product-spezifischen Dateinamen
#
#  Optionale Parameter
#o   Instanznummer
get_properties_filename () {
  if [[ "x" == "x${HOSTNAME}" ]]; then
    f_exit_with_message ${EX_PARAMETER} "get_properties_filename: \$HOSTNAME is not set. Abort!"
  fi
  
  f_set_environment_dir 
  case "${1}" in
    host) 
      echo "${XYNA_ENVIRONMENT_DIR}/${HOSTNAME}.properties"
      ;;
    product)
      if [[ -z "${PRODUCT_NAME}" ]]; then 
        f_exit_with_message ${EX_PARAMETER} "get_properties_filename: PRODUCT_NAME needs to be set. Abort!"; 
      fi
      if [[ -z "${2}" ]] ; then 
        echo "${XYNA_ENVIRONMENT_DIR}/${PRODUCT_NAME}.properties"
      else
        local PRODUCT_INSTANCE=$(printf "%03g" ${2:--1})
        echo "${XYNA_ENVIRONMENT_DIR}/${PRODUCT_NAME}_${PRODUCT_INSTANCE}.properties"
      fi;
      ;;
    component)  
      echo "${XYNA_ENVIRONMENT_DIR}/${HOSTNAME}.component.properties"
      ;;
    *) f_exit_with_message ${EX_PARAMETER} "get_properties_filename: Unable to generate filename for property '${1}'. Abort!";;
  esac
}


#  Alte Properties ins neue Format konvertieren
#+ Altes Format entspricht der Instanz Nummer eins.
convert_properties () {
  comp_state[0]=$(check_components_file "black_edition_prerequisites")
  comp_state[1]=$(check_components_file "black_edition")

  #  Ist Prerequisites oder BlackEdition bereits installiert,
  #+ dann die Properties ins neue Format konvertieren.
  if [[ "x${comp_state[0]}" != "xinstall" || "x${comp_state[1]}" != "xinstall" ]]; then
    local PROPERTIES_FILE_OLD=$(get_properties_filename host)
    local PROPERTIES_FILE_NEW=$(get_properties_filename product "1")
    ${VOLATILE_CP} -p "${PROPERTIES_FILE_OLD}" "${PROPERTIES_FILE_NEW}"
  fi

  PROP_FILE=$(get_properties_filename component)
  ORIGINAL_RELEASE_NUMBER="${RELEASE_NUMBER}"

  #  Komponenten umsetzen
  if [[ "x${comp_state[0]}" != "xinstall" ]]; then
    get_property "black_edition_prerequisites.version" ${PROP_FILE}
    RELEASE_NUMBER="${CURRENT_PROPERTY}"

    clear_components_file "black_edition_prerequisites"
    local PRODUCT_INSTANCE=$(printf "%03g" "1")
    save_components_file "black_edition_prerequisites.${PRODUCT_INSTANCE}"
  fi

  if [[ "x${comp_state[1]}" != "xinstall" ]]; then
    get_property "black_edition.version" ${PROP_FILE}
    RELEASE_NUMBER="${CURRENT_PROPERTY}"

    clear_components_file "black_edition"
    local PRODUCT_INSTANCE=$(printf "%03g" "1")
    save_components_file "black_edition.${PRODUCT_INSTANCE}"
  fi

  RELEASE_NUMBER="${ORIGINAL_RELEASE_NUMBER}"
}


#  Datei A enthaelt einige Properties
#+ Datei B enthaelt andere Properties, es kann eine Schnittmenge mit Datei A geben
#
#  Die Properties aus A sollen nach B transferiert werden.
#+ Ein blosses Kopieren der Datei ist nicht moeglich, da hierbei
#+ die Properties in B zerstoert werden wuerden.
#
#  Eingabeparameter
#o   Datei A, aus dieser werden die Properties gelesen
#o   Datei B, in diese werden die Properties geschrieben
#
transfer_properties () {
  local FROM_FILE="${1}"
  local TO_FILE="${2}"

  #  Sind Eingabeparameter in Ordnung?
  if [[ -z "${FROM_FILE}" || -z "${TO_FILE}" ]]; then
    f_exit_with_message ${EX_OSFILE} "transfer_properties: Which files do you want me to work on? Abort!"
  fi

  #  Sind die Dateien vorhanden?
  if [[ ! -f "${FROM_FILE}" ]]; then
    f_exit_with_message ${EX_OSFILE} "transfer_properties: Unable to locate input file '${FROM_FILE}'. Abort!"
  fi
  if [[ ! -f "${TO_FILE}" ]]; then
    if [[ "x${VERBOSE}" != "xfalse" ]]; then echo "* Creating output file '${TO_FILE}'."; fi
    ${VOLATILE_TOUCH} "${TO_FILE}"
  fi

  #  Aus der Eingangsdatei eine Liste aller Properties erstellen
  #+ Mit awk allen Zeilen, die nicht mit einem Kommentar beginnen filtern,
  #+ und mit dem Spaltentrenner '=' die erste Spalte extrahieren.
  for i in $(${VOLATILE_AWK} 'BEGIN { FS="=" } $0 !~ /^#/ { print $1 }' "${FROM_FILE}")
  do
    #  Fuer jedes Element der Liste die Property aus der Eingangsdatei lesen...
    get_property ${i} "${FROM_FILE}"
    #  ...und in die Ausgangsdatei schreiben
    set_property ${i} "${CURRENT_PROPERTY}" "${TO_FILE}"
  done
}



