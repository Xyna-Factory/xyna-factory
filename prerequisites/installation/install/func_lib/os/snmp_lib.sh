# ---------------------------------------------------
#  Copyright GIP AG 2013
#  (http://www.gip.com)
#
#  Hechtsheimer Str. 35-37
#  55131 Mainz
# ---------------------------------------------------
#  $Revision: 143988 $
#  $Date: 2013-12-17 10:19:43 +0100 (Di, 17. Dez 2013) $
# ---------------------------------------------------

# OS -> SNMP library


##############################
# SNMP installieren
f_install_snmpd () {
  echo -e "\n* Configuring SNMP-Daemon"
  echo -e "It is assumed that all properties in /etc/xyna/environment are valid."

  local ret_val=0
  local FILE_TO_EDIT="/etc/snmp/snmpd.conf"
  if [[ "x${INSTALLATION_PLATFORM}" == "xsolaris" ]]; then
    FILE_TO_EDIT="/usr/local/share/snmp/snmpd.conf"
  fi

  case ${INSTALLATION_PLATFORM} in
    sles)    chkconfig snmpd 35;;
    oracle|rhel|centos)
             if [[ "x${SYSTEMD_ENV}" == "xtrue" ]] ; then
               systemctl enable snmpd
             else
               chkconfig snmpd on
             fi
             ;;

    debian)  update-rc.d snmpd defaults;;
    ubuntu)  update-rc.d snmpd defaults;;
    solaris) local TARGET_FILE="/etc/init.d/snmpd"
       ${VOLATILE_CAT} > "${TARGET_FILE}" << A_HERE_DOCUMENT
#!/bin/sh 

case "\$1" in
  start)  /usr/local/sbin/snmpd -c ${FILE_TO_EDIT} -Lsd;;
  stop)   pkill -f snmpd.conf;;
  status) pgrep -l -f snmpd.conf;;
  restart)
        \$0 stop
        \$0 start
        ;;
  *)
        echo ""
        echo "\tUsage: \$0 { start | stop | restart | status }"
        echo ""
        exit 1
esac

exit 0
A_HERE_DOCUMENT
       ${VOLATILE_CHMOD} 0755 "${TARGET_FILE}"
       if [[ ! -L /etc/rc3.d/S98snmpd ]]; then ${VOLATILE_LN} -s "${TARGET_FILE}" /etc/rc3.d/S98snmpd; fi
       if [[ ! -L /etc/rc3.d/K98snmpd ]]; then ${VOLATILE_LN} -s "${TARGET_FILE}" /etc/rc3.d/K98snmpd; fi
       ;;
    *) err_msg "f_install_snmpd: Platform '${INSTALLATION_PLATFORM}' is not supported."
       return ${ret_val};;
  esac 

  local SNMP_TRIGGER_PORT
  local SNMP_TRIGGER_IP
  let i=0
  while [[ ${i} -le ${BLACK_EDITION_INSTANCES} ]]; do
    let i=i+1
    local PROP_FILE=$(get_properties_filename product "${i}")
    if [ ! -e ${PROP_FILE} ] ; then
      echo "Property file ${PROP_FILE} does not exist. Skipping...";
      SNMP_TRIGGER_PORT[${i}]="not installed"
      SNMP_TRIGGER_IP[${i}]="not installed"
      continue;
    fi;
    get_property "trigger.snmp.port" ${PROP_FILE}
    SNMP_TRIGGER_PORT[${i}]="${CURRENT_PROPERTY}"
    get_property "network.interface.snmptrigger" ${PROP_FILE}
    OWN_IP_SELECTION="$(get_interface_number "${CURRENT_PROPERTY}")" 
    if [[  "x${OWN_IP_SELECTION}" == "x" ]]; then
      err_msg "ip-address of the property network.interface.snmptrigger could not be resolved, using 127.0.0.1 instead"
      SNMP_TRIGGER_IP[${i}]="127.0.0.1"
    else
      SNMP_TRIGGER_IP[${i}]=${local_interface[${OWN_IP_SELECTION}]}
    fi
  done
  
  backup_file ${FILE_TO_EDIT}
  ${VOLATILE_CAT} > "${FILE_TO_EDIT}" << A_HERE_DOCUMENT
# System contact information
syslocation Server Room (configure ${FILE_TO_EDIT})
syscontact Root <root@localhost> (configure ${FILE_TO_EDIT})

# First, map the community name "public" into a "security name"

#                    sec.name       source        community
com2sec              notConfigUser  default       public
A_HERE_DOCUMENT
  let i=0;
  while [[ ${i} -lt ${BLACK_EDITION_INSTANCES} ]]; do
    let i=i+1
    if [[ "${SNMP_TRIGGER_PORT[${i}]}" == "not installed" ]]; then continue; fi
    local PRODUCT_INSTANCE=$(printf "%03g" ${i})
    echo "com2sec -Cn xyna_${PRODUCT_INSTANCE} notConfigUser  default       xyna_${PRODUCT_INSTANCE}" >> "${FILE_TO_EDIT}" 
  done
  ${VOLATILE_CAT} >> "${FILE_TO_EDIT}" << A_HERE_DOCUMENT

# Second, map the security name into a group name:

#       groupName      securityModel securityName
group   notConfigGroup v1            notConfigUser
group   notConfigGroup v2c           notConfigUser

# Third, create a view for us to let the group have rights to:

# Make at least  snmpwalk -v 1 localhost -c public system fast again.
view    systemview     included      .1.3.6.1.2.1.1
view    systemview     included      .1.3.6.1.2.1.25.1.1
view    allview        included      .1

# Finally, grant the group read-only access to the systemview view.

#       group          context  sec.model sec.level prefix read       write  notif
access  notConfigGroup ""       any       noauth    exact  allview    none   none
A_HERE_DOCUMENT
  let i=0;
  while [[ ${i} -lt ${BLACK_EDITION_INSTANCES} ]]; do
    let i=i+1
    if [[ "${SNMP_TRIGGER_PORT[${i}]}" == "not installed" ]]; then continue; fi
    local PRODUCT_INSTANCE=$(printf "%03g" ${i})
    echo "access  notConfigGroup xyna_${PRODUCT_INSTANCE} any       noauth    exact  allview    none   none" >> "${FILE_TO_EDIT}"
  done

${VOLATILE_CAT} >> "${FILE_TO_EDIT}" << A_HERE_DOCUMENT
# Relay some OIDs to known subsystems

extend .1.3.6.1.4.1.28747.1.5.901 001 /bin/bash ${INSTALL_PREFIX}/bin/snmp_status.sh netstat_num 001
extend .1.3.6.1.4.1.28747.1.5.901 002 /bin/bash ${INSTALL_PREFIX}/bin/snmp_status.sh apache2 002
A_HERE_DOCUMENT
  let i=0; 
  while [[ ${i} -lt ${BLACK_EDITION_INSTANCES} ]]; do
    let i=i+1
    if [[ "${SNMP_TRIGGER_PORT[${i}]}" == "not installed" ]]; then continue; fi
    local PRODUCT_INSTANCE=$(printf "%03g" ${i})
    echo -e "\n### Xyna Factory ${PRODUCT_INSTANCE}" >> "${FILE_TO_EDIT}"
    echo "extend .1.3.6.1.4.1.28747.1.5.902 ${PRODUCT_INSTANCE} /bin/bash ${INSTALL_PREFIX}/bin/snmp_status.sh xynafactory ${PRODUCT_INSTANCE}" >> "${FILE_TO_EDIT}"
    echo "proxy -Cn xyna_${PRODUCT_INSTANCE} -v 2c -c public ${SNMP_TRIGGER_IP[${i}]}:${SNMP_TRIGGER_PORT[${i}]} .1.3.6.1.4.1.28747.1.11" >> "${FILE_TO_EDIT}"
    echo "proxy -Cn xyna_${PRODUCT_INSTANCE} -v 2c -c public localhost .1.3.6.1.4.1.28747.1.5" >> "${FILE_TO_EDIT}"
    if [[ ${i} -eq 1 ]]; then
      echo "# Bind community public to first instance" >> "${FILE_TO_EDIT}"
      echo "proxy -v 2c -c public ${SNMP_TRIGGER_IP[${i}]}:${SNMP_TRIGGER_PORT[${i}]} .1.3.6.1.4.1.28747.1.11" >> "${FILE_TO_EDIT}"
    fi
  done
  ${VOLATILE_CAT} >> "${FILE_TO_EDIT}" << A_HERE_DOCUMENT

####
# Enable Agent Extensibility Protocol (AgentX subagents)

master agentx

# EOF
A_HERE_DOCUMENT

  # SNMP-Skripte installieren
  local TARGET_DIR="${INSTALL_PREFIX}/bin" 
  local LISTFILES="scripts/snmp_status.sh"
  ${VOLATILE_MKDIR} -p ${TARGET_DIR}                
  for FILE in ${LISTFILES}; do
    install_file ${FILE} ${TARGET_DIR} 750      
  done
  ${VOLATILE_CHOWN} -R "$(f_generate_chown_parameter)"  ${TARGET_DIR}
  

  if [[ "x${SYSTEMD_ENV}" == "xtrue" ]] ; then
    systemctl restart snmpd
  else
    /etc/init.d/snmpd restart
    
  fi

  ret_val=$?
  
  return ${ret_val}
}
