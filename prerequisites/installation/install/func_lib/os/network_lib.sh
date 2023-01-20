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

# OS -> Network/Firewall library

#  Richtet eine IP-Adresse auf einer Netzwerkschnittstelle ein.
f_configure_interface_ipaddress () {
    local STR_INTERFACE_NAME="${1}"
    local TARGET_IP="${2}"
    local k=1

    #  IPv6-Adressen sind die anderen Parameter, also die ersten beiden Parameter shiften
    shift 2
    
    if [[ "x${STR_INTERFACE_NAME}" == "x" ]]; then
  err_msg "f_configure_interface_ipaddress: unable to work without interface name."
  return ${EX_PARAMETER}
    fi
    
    check_ip_address_and_subnet "${TARGET_IP}"
    if [[ $? -ne 0 ]]; then
  err_msg "f_configure_interface_ipaddress: IP address '${TARGET_IP}' is invalid."
  return ${EX_PARAMETER}
    fi

    echo -e "\n* Configuring network interface '${STR_INTERFACE_NAME}'"

    case ${INSTALLATION_PLATFORM} in
  sles)
      local TARGET_FILE="/etc/sysconfig/network/ifcfg-${STR_INTERFACE_NAME}"
      echo -e "STARTMODE='auto'\nBOOTPROTO='static'\nIPADDR='${TARGET_IP}'\nUSERCONTROL='no'\nNAME='${STR_INTERFACE_NAME}'\n" > ${TARGET_FILE}
      for j in "$@"
      do
        echo -e "IPADDR_${k}='${j}'\n" >> ${TARGET_FILE}
        let k=k+1
      done

      #  localhost ist 127.0.0.1 und _nicht_ 127.0.0.2
      local FILE_TO_EDIT="/etc/sysconfig/network/ifcfg-lo"
      if [[ -f "${FILE_TO_EDIT}" ]]; then
        ${VOLATILE_CP} -p -f "${FILE_TO_EDIT}" "${TMP_FILE}"
        ${VOLATILE_GREP} -v "127.0.0.2" "${FILE_TO_EDIT}" > "${TMP_FILE}"
        ${VOLATILE_MV} "${TMP_FILE}" "${FILE_TO_EDIT}"
      fi
      if [[ $(ip addr show dev lo | ${VOLATILE_GREP} -c 127.0.0.2) -ne 0 ]]; then
        ip addr del 127.0.0.2/8 dev lo
      fi
      
      /etc/init.d/network restart
      ;;
  rhel|oracle|centos)
      local IP_ADDRESS_ONLY=$(f_split_ip_address "${TARGET_IP}")
      local PREFIXLEN_ONLY=$(f_split_prefixlen "${TARGET_IP}")
      local TARGET_NETMASK=$(f_convert_prefixlen_to_netmask "${PREFIXLEN_ONLY}")
      local IPV6_SECONDARIES=""

      local TARGET_FILE="/etc/sysconfig/network-scripts/ifcfg-${STR_INTERFACE_NAME}"
      echo -e "DEVICE='${STR_INTERFACE_NAME}'\nBOOTPROTO='none'\nONBOOT='yes'\nIPADDR='${IP_ADDRESS_ONLY}'\nNETMASK='${TARGET_NETMASK}'\nUSERCTL='no'\nNAME='${STR_INTERFACE_NAME}'\n" > ${TARGET_FILE}
      for j in "$@"
      do
    if [[ ${k} -eq 1 ]]; then
        echo -e "IPV6INIT='yes'\nIPV6ADDR='${j}'\n" >> ${TARGET_FILE}
    else
        if [[ -z "${IPV6_SECONDARIES}" ]]; then
      IPV6_SECONDARIES="${j}"
        else
      IPV6_SECONDARIES="${IPV6_SECONDARIES} ${j}"
        fi
    fi
    let k=k+1
      done
      if [[ -n "${IPV6_SECONDARIES}" ]]; then
    echo -e "IPV6ADDR_SECONDARIES='${IPV6_SECONDARIES}'\n" >> ${TARGET_FILE}
      fi

      service network restart
      ;;
  debian) 
      # FIXME: implement this
      echo "    + not implemented yet - skipping"
      ;;
  ubuntu) 
      # FIXME: implement this
      echo "    + not implemented yet - skipping"
      ;;
    solaris)
      # FIXME: implement this
      echo "    + not implemented yet - skipping"
      ;;
  *)  err_msg "f_configure_interface_ipaddress: Platform '${INSTALLATION_PLATFORM}' is not supported.";;
    esac
    
    return ${EX_OK}
}

#  Variable muss global definiert sein
declare -a local_interface
declare -a local_interface_v6
declare -a local_interface_v6_with_netmask
declare -a local_interface_name
declare -a local_interface_mac
#  Hole die Netzwerkadressen aller lokaler Interfaces.
#
#  Ergebnis:
#o   ${local_interface_counter}            = Anzahl der gefundenen Interfaces
#o   ${local_interface_name[i]}            = Array mit den Interfacenamen
#o   ${local_interface_mac[i]}             = Array mit den MAC-Adressen der Interfaces
#o   ${local_interface[i]}                 = Array mit den IPv4-Adressen
#o   ${local_interface_v6[i]}              = Array mit den IPv6-Adressen, jedes Element ist ein Leerzeichengetrennter String
#o   ${local_interface_v6_with_netmask[i]} = Array mit den IPv6-Adressen, jedes Element ist ein Leerzeichengetrennter String
#
#  Ausgaben von 'ifconfig -a' filtern:
#+   - Sprache auf 'C' stellen:
#+   - Alle Interfaces ausser localhost (127.0.0.1) finden:
#+       * localhost hat keine Broadcastadresse
#+   - IP-Adresse ist in Spalte 2:
#+       * ggfs. ist der String 'addr:' zu entfernen
#
#  Linux (Debian/SLES):
#    LANG=C /sbin/ifconfig -a | grep cast
#o inet addr:10.0.0.67  Bcast:10.0.0.255  Mask:255.255.255.0
#o inet addr:172.16.4.1  Bcast:172.16.4.255  Mask:255.255.255.0
#
#    LANG=C /sbin/ifconfig -a | grep inet6
#o inet6 addr: fe80::224:81ff:fee2:286a/64 Scope:Link
#o inet6 addr: 2001:0:0:1::110/64 Scope:Global
#o inet6 addr: ::1/128 Scope:Host
#
#  Solaris:
#    LANG=C /sbin/ifconfig -a | grep cast
#o inet 10.0.0.219 netmask ffffff00 broadcast 10.0.0.255
#o inet 192.168.178.23 netmask ffffff00 broadcast 192.168.178.255
#
#    LANG=C /sbin/ifconfig -a | grep inet6
#o inet6 ::1/128
#o inet6 fe80::20c:29ff:febe:81f0/10

#  Der Interfacename ist eine Zeile vor der IP-Adresse!
#+ Nachdem alle IP-Adressen ermittelt sind ($local_interface) mit einer
#+ Schleife ueber alle IP-Adressen gehen. Mit awk den Interfacenamen
#+ aus Spalte 1 nehmen, falls die IP-Adresse nicht in der Zeile
#+ in Spalte 2 enthalten ist.
#+ Den zuvor gespeicherten Interfacenamen ausgeben, wenn die Zeile in Spalte 2
#+ die IP-Adresse enthaelt.
#
#o nge0 flags=1000843<UP,BROADCAST,RUNNING,MULTICAST,IPv4> mtu 1500 index 2
#o         inet 10.0.0.219 netmask ffffff00 broadcast 10.0.0.255
#
#  Bei IPv6 sieht alles ein bisschen anders aus:
#+ Jedes Interface darf mehrere IP-Adressen haben.
#+ Scope:Host   sind nur fuer den Host gueltig, also loopback-Adressen
#+              Starten mit '::', das wird weggefiltert.
#+ Scope:Link   sind link-lokale Adressen und werden nicht geroutet.
#+              Sozusagen ein privater Adressraum...
#+              Starten mit 'fe80', das wird weggefiltert.
#+ Scope:Global sind ueberall gueltig. Das ist die gesuchte IP-Adresse.
#+              Problem: Kann mehrfach vorkommen!
#
get_local_interfaces () {
  local_interface_counter=0

  #  nach Betriebssystem unterscheiden
  case $(${VOLATILE_UNAME}) in
    Linux)
      #  Liste mit allen Interfaces generieren
      ALL_INTERFACE_NAMES=$(LANG=C ${VOLATILE_IFCONFIG} | ${VOLATILE_GREP} 'Link encap' | ${VOLATILE_SED} -e "s+: + +" | ${VOLATILE_AWK} '{print $1}' | ${VOLATILE_SORT} -u)
      case ${INSTALLATION_PLATFORM} in
        rhel|oracle|centos)
          case ${INSTALLATION_PLATFORM_VERSION} in
            7.*|8.*)
              ALL_INTERFACE_NAMES=$(LANG=C ${VOLATILE_IFCONFIG} | ${VOLATILE_GREP} ': flags' | ${VOLATILE_SED} -e "s+: + +" | ${VOLATILE_AWK} '{print $1}' | ${VOLATILE_SORT} -u)
              ;;
            *)
              ;;
          esac
          ;;
        ubuntu)
          case ${INSTALLATION_PLATFORM_VERSION} in
           18.*|20.*)
              ALL_INTERFACE_NAMES=$(LANG=C ${VOLATILE_IFCONFIG} | ${VOLATILE_GREP} ': flags' | ${VOLATILE_SED} -e "s+: + +" | ${VOLATILE_AWK} '{print $1}' | ${VOLATILE_SORT} -u)
              ;;
             *)
              ;;
          esac
          ;;

        *)
          ;;
      esac

      #  IPv4 und IPv6 Adressen holen
      local_interface_counter=0
      for i in ${ALL_INTERFACE_NAMES}
      do
        local_interface_name[${local_interface_counter}]="${i}"
        local_interface_mac[${local_interface_counter}]=$(LANG=C ${VOLATILE_IFCONFIG} ${i} | ${VOLATILE_GREP} -i "hwaddr" | ${VOLATILE_AWK} '{print $NF}')
        local_interface[${local_interface_counter}]=$(LANG=C ${VOLATILE_IFCONFIG} ${i} | ${VOLATILE_GREP} "inet " | ${VOLATILE_SED} -e "s+addr:++" | ${VOLATILE_AWK} '{print $2}')
        local_interface_v6[${local_interface_counter}]=$(add_elements_to_string "" $(LANG=C ${VOLATILE_IFCONFIG} ${i} | ${VOLATILE_GREP} "inet6" | ${VOLATILE_SED} -e "s+addr:++" -e "s+/.*++" | ${VOLATILE_AWK} '{print $2}') )
        local_interface_v6_with_netmask[${local_interface_counter}]=$(add_elements_to_string "" $(LANG=C ${VOLATILE_IFCONFIG} ${i} | ${VOLATILE_GREP} "inet6" | ${VOLATILE_SED} -e "s+addr:++" | ${VOLATILE_AWK} '{print $2}') )
        let local_interface_counter=local_interface_counter+1
      done
      ;;
    SunOS)
      #  Liste mit allen Interfaces generieren
      ALL_INTERFACE_NAMES=$(LANG=C ${VOLATILE_IFCONFIG} -a | ${VOLATILE_GREP} 'flag' | ${VOLATILE_SED} -e "s+: + +" | ${VOLATILE_AWK} '{print $1}' | ${VOLATILE_SORT} -u)

      #  IPv4 und IPv6 Adressen holen
      local_interface_counter=0
      for i in ${ALL_INTERFACE_NAMES}
      do
        local_interface_name[${local_interface_counter}]="${i}"
        local_interface_mac[${local_interface_counter}]=$(LANG=C ${VOLATILE_IFCONFIG} ${i} | ${VOLATILE_GREP} "ether" | ${VOLATILE_AWK} '{print $NF}')
        local_interface[${local_interface_counter}]=$(LANG=C ${VOLATILE_IFCONFIG} ${i} inet | ${VOLATILE_GREP} "inet" | ${VOLATILE_SED} -e "s+addr:++" | ${VOLATILE_AWK} '{print $2}')
        local_interface_v6[${local_interface_counter}]=$(add_elements_to_string "" $(LANG=C ${VOLATILE_IFCONFIG} ${i} inet6 | ${VOLATILE_GREP} "inet" | ${VOLATILE_SED} -e "s+addr:++" -e "s+/.*++" | ${VOLATILE_AWK} '{print $2}') )
        local_interface_v6_with_netmask[${local_interface_counter}]=$(add_elements_to_string "" $(LANG=C ${VOLATILE_IFCONFIG} ${i} inet6 | ${VOLATILE_GREP} "inet" | ${VOLATILE_SED} -e "s+addr:++" | ${VOLATILE_AWK} '{print $2}') )
        let local_interface_counter=local_interface_counter+1
      done
      ;;
    *)  echo "Platform '$(${VOLATILE_UNAME})' is not supported. Abort!"; exit 99;;
  esac

  #  An die Liste aller Netzwerkinterfaces noch '*' anfuegen.
  #+ '*' bedeutet, dass eine Bindung an alle Netzwerkinterfaces moeglich sein soll.
  local_interface[${local_interface_counter}]="0.0.0.0"
  local_interface_v6[${local_interface_counter}]="::"
  local_interface_name[${local_interface_counter}]='*'
}


#  Test remote host:port availability (TCP-only as UDP does not reply)
f_check_open_port () {
  local TARGET_HOST="${1:-localhost}"
  local TARGET_PORT="${2:-22}"

  (echo >/dev/tcp/${TARGET_HOST}/${TARGET_PORT}) &>/dev/null
  return $?
}

#  Netzmaske zu gegebener Praefixlaenge berechnen
f_convert_prefixlen_to_netmask () {
    local PREFIXLEN="${1}"
    local ALL_NETMASK=( "0.0.0.0" "128.0.0.0" "192.0.0.0" "224.0.0.0" "240.0.0.0" "248.0.0.0" "252.0.0.0" "254.0.0.0" "255.0.0.0" "255.128.0.0" "255.192.0.0" "255.224.0.0" "255.240.0.0" "255.248.0.0" "255.252.0.0" "255.254.0.0" "255.255.0.0" "255.255.128.0" "255.255.192.0" "255.255.224.0" "255.255.240.0" "255.255.248.0" "255.255.252.0" "255.255.254.0" "255.255.255.0" "255.255.255.128" "255.255.255.192" "255.255.255.224" "255.255.255.240" "255.255.255.248" "255.255.255.252" "255.255.255.254" "255.255.255.255" )

    echo "${ALL_NETMASK[${PREFIXLEN}]}"
}

#  IP-Adresse umdrehen, also die Oktets vertauschen.
reverse_ip_address () {
   check_ip_address ${1};
  if [[ $? -ne 0 ]]; then echo "ip-address ${1} is invalid."; exit 99; fi
  echo ${1} | ${VOLATILE_AWK} -F. '{printf ("%s.%s.%s.%s", $4, $3, $2, $1) }'
}


#  Splittet die IP-Adresse aus dem String "IP-Adresse/Praefixlaenge" ab
#
#  Eingabeparameter:
#o   1 = IP-Adresse/Praefixlaenge
#        192.168.178.1/24
#
#  Rueckgabewert:
#o   IP-Adresse
#
f_split_ip_address () {
    local IP_ADDRESS_WITH_PREFIX="${1}"

    echo "$(echo "${IP_ADDRESS_WITH_PREFIX}" | ${VOLATILE_SED} -e "s+/.*++")"
}


#  Splittet die Praefixlaenge aus dem String "IP-Adresse/Praefixlaenge" ab
#
#  Eingabeparameter:
#o   1 = IP-Adresse/Praefixlaenge
#        192.168.178.1/24
#
#  Rueckgabewert:
#o   Praefixlaenge
#
f_split_prefixlen () {
    local IP_ADDRESS_WITH_PREFIX="${1}"

    echo "$(echo "${IP_ADDRESS_WITH_PREFIX}" | ${VOLATILE_SED} -e "s+.*/++")"
}


#  Es wird eine Auswahl aller Netzwerkinterfaces angezeigt,
#+ falls mehr als ein Netzwerkinterface zur Verfuegung steht.
#
#  Optional besteht die Moeglichkeit '*' als Netzwerkinterface
#+ auszuwaehlen. '*' bedeutet, dass sich ein Trigger an
#+ alle Netzwerkinterfaces binden soll.
#
#  Eingabeparameter: allow_star_selection, falls true, wird
#+                   '*' als weitere Auswahlmoeglichkeit angeboten.
choose_ip_address () {
  ALLOW_STAR_SELECTION="${1:-false}"
  INTERFACE_SELECTION_COUNTER="${local_interface_counter}"
  #  Falls '*' als Netzwerkinterface waehlbar sein soll, den Zaehler
  if [[ "${ALLOW_STAR_SELECTION}" == "true" ]]; then
    let INTERFACE_SELECTION_COUNTER=INTERFACE_SELECTION_COUNTER+1
  fi
  #  default is ${local_interface[0]}
  OWN_IP_SELECTION="0"
  case ${INTERFACE_SELECTION_COUNTER} in
    0)  err_msg "Unable to determine my own ip address. Abort!"
        exit 96
        ;;
    1)  # nothing to be done, ip-address found only once
        ;;
    *)  # multiple entries - choose one
        OWN_IP_SELECTION="-1"
        until [[ ${OWN_IP_SELECTION} -ge 0 && ${OWN_IP_SELECTION} -lt ${INTERFACE_SELECTION_COUNTER} ]]; do
          echo "Following ip-addresses found:"
          let OWN_IP_INDEX=0
          while [[ "${OWN_IP_INDEX}" -lt "${local_interface_counter}" ]]
          do
            echo "  (${OWN_IP_INDEX}): ${local_interface_name[${OWN_IP_INDEX}]} (${local_interface[${OWN_IP_INDEX}]})"
            let OWN_IP_INDEX=OWN_IP_INDEX+1
          done
          #  '*' als Netzwerkinterface zur Auswahl anbieten
          if [[ "${ALLOW_STAR_SELECTION}" == "true" ]]; then
            echo "  (${OWN_IP_INDEX}): ${local_interface_name[${OWN_IP_INDEX}]} (${local_interface[${OWN_IP_INDEX}]}) <-- choose this to listen on all interfaces"
          fi
          echo; echo -n "Enter number to choose one: "
          read OWN_IP_SELECTION
          if [[ -z ${OWN_IP_SELECTION} ]]; then OWN_IP_SELECTION="-1"; fi
        done
        ;;
  esac
}

######################################################################
#  Eingabeparameter: Interfacename
#  Ausgabeparameter: Laufende Nummer
get_interface_number () {
  if [[ "x" == "x${1}" ]]; then
    #  falls kein Interface uebergeben wurde, muss der Benutzer interaktiv auswaehlen
    :
  else
    let OWN_IP_INDEX=0
    while [[ "${OWN_IP_INDEX}" -le "${local_interface_counter}" ]]
    do
      if [[ "x${local_interface_name[${OWN_IP_INDEX}]}" == "x${1}" ]]; then
        echo "${OWN_IP_INDEX}"
      fi
      let OWN_IP_INDEX=OWN_IP_INDEX+1
    done
  fi
}




#  Erstellt eine Servicedefinition fuer die Firewall bestehend
#+ aus dem Servicenamen und den freizuschaltenden Ports
#
#  Eingabeparameter:
#o   1       = Servicename, z.B. 'tomcat'
#o   2       = Protokoll:
#o               TCP
#o               UDP
#o               TCP+UDP
#o   3 + ff. = Portnummern, z.B. '8080' '8443'
#
#  Rueckgabewert:
#o   Fehlernummer
#
f_configure_firewall_service () {
    local SERVICE_NAME="${1}"
    local STR_PROTOCOL="${2}"

    #  Ports sind die anderen Parameter, also die ersten zwei Parameter shiften
    shift 2
    
    if [[ "x${SERVICE_NAME}" == "x" ]]; then
  err_msg "f_configure_firewall_service: unable to work without service name."
  return ${EX_PARAMETER}
    fi
    
    if [[ "x${STR_PROTOCOL}" == "x" ]]; then
  err_msg "f_configure_firewall_service: unable to work without protocol name."
  return ${EX_PARAMETER}
    fi
    
    echo -e "\n* Configuring firewall service '${SERVICE_NAME}'"

    case ${INSTALLATION_PLATFORM} in
  sles)
      local TARGET_DIR="/etc/sysconfig/SuSEfirewall2.d/services"
      if [[ ! -d "${TARGET_DIR}" ]]; then ${VOLATILE_MKDIR} -p "${TARGET_DIR}"; fi
      
      local FILE_TO_EDIT="${TARGET_DIR}/${SERVICE_NAME}"
      
            #  leere Datei erstellen, falls erforderlich
      if [[ ! -f "${FILE_TO_EDIT}" ]]; then
    echo "    + creating service '${SERVICE_NAME}'"
    ${VOLATILE_CAT} > "${FILE_TO_EDIT}" << A_HERE_DOCUMENT
## Name: ${SERVICE_NAME}
TCP=""
UDP=""
RPC=""
IP=""
BROADCAST=""
A_HERE_DOCUMENT
      fi

            #  aktuelle Portfreischaltungen einlesen
      for i in "TCP" "UDP"
      do
    #  enthaelt die Protokollzeichenkette die Schluesselworte "UDP" oder "TCP" oder beides?
    if [[ $(echo ${STR_PROTOCOL} | ${VOLATILE_GREP} -ci "${i}") -ne 0 ]]; then
        local CURRENT_PORTS=$(${VOLATILE_GREP} "^${i}" "${FILE_TO_EDIT}" | ${VOLATILE_AWK} -F= '{print $2}' | ${VOLATILE_TR} -d \")
        echo "    + adding ${i} ports to service '${SERVICE_NAME}':"
        echo "      $*"
        CURRENT_PORTS=$(add_elements_to_string "${CURRENT_PORTS}" "$@")
        f_replace_in_file "${FILE_TO_EDIT}" "s+^${i}=.*$+${i}=\"${CURRENT_PORTS}\"+"
    fi
      done
        
            #  Firewall neu starten
      /sbin/SuSEfirewall2 --bootunlock start
      ;;
  rhel|oracle|centos)
    case ${INSTALLATION_PLATFORM_VERSION} in
        7.*|8.*)
          #https://access.redhat.com/documentation/en-US/Red_Hat_Enterprise_Linux/7/html/Security_Guide/sec-Using_Firewalls.html
          if [[ $(systemctl status firewalld | grep -c "active (running)") -eq 1 ]]; then
            for i in "tcp" "udp" 
            do
              if [[ $(echo ${STR_PROTOCOL} | grep -ci "${i}") -ne 0 ]]; then
                for j in "$@" 
                do
                  firewall-cmd --zone=public --add-port=${j}/${i} --permanent
                done
              fi
            done
            firewall-cmd --reload
          else 
            for i in "tcp" "udp"  
            do
              if [[ $(echo ${STR_PROTOCOL} | grep -ci "${i}") -ne 0 ]]; then
                for j in "$@" 
                do
                  firewall-offline-cmd --zone=public --add-port=${j}/${i}
                done
              fi
            done
          fi
        ;;
        *) 
      local INT_REJECT_RULE
      local PORT_COUNT
      
      # Bug 17053
      if [[ -x /etc/init.d/iptables ]]; then
        # Falls Firewall aktiv dann restart um alle Regeln neu zu laden
        /etc/init.d/iptables condrestart
      else
        attention_msg "Skipping firewall configuration. iptables seems not be installed."
        return
      fi
      
      local CHAIN_NAME="RH-Firewall-1-INPUT"
      
            #  iptables --line-numbers -nL RH-Firewall-1-INPUT

      if [[ $(iptables -nL | ${VOLATILE_GREP} -c "${CHAIN_NAME}") -eq 0 ]]; then
    #  Mit RHEL 6.3 wurde der Name der Firewall-Chain geaendert.
    CHAIN_NAME="INPUT"
    
    if [[ $(iptables -nL | ${VOLATILE_GREP} -c "${CHAIN_NAME}") -eq 0 ]]; then
        attention_msg "Skipping firewall configuration. It seems to be disabled."
        return
    fi
      fi
      
            #  Regeln immer am Ende, also eine Zeile vor 'REJECT' eintragen
      for i in "$@"
      do
    
                #  in die IPv4-Firewall eintragen
    PORT_COUNT=$(iptables --line-numbers -nL "${CHAIN_NAME}" | ${VOLATILE_AWK} 'BEGIN { i=0 } $NF == "dpt:'${i}'" { i=i+1 } END {print i}')
    if [[ ${PORT_COUNT} -eq 0 ]]; then
        INT_REJECT_RULE=$(iptables --line-numbers -nL "${CHAIN_NAME}" | ${VOLATILE_AWK} '$2 == "REJECT" { print $1 }')
        if [[ -n  "${INT_REJECT_RULE}" ]]; then
          echo "    + adding port ${i} to IPv4 firewall chain '${CHAIN_NAME}'"      
          iptables -I "${CHAIN_NAME}" "${INT_REJECT_RULE}" -m state --state NEW -m tcp -p tcp --dport "${i}" -j ACCEPT
        else
          attention_msg "No REJECT-RULE found in IPv4 firewall. Please check manualy for open port; 'tcp ${i}'"
        fi
    fi
    
                #  in die IPv6-Firewall eintragen
    PORT_COUNT=$(ip6tables --line-numbers -nL "${CHAIN_NAME}" | ${VOLATILE_AWK} 'BEGIN { i=0 } $NF == "dpt:'${i}'" { i=i+1 } END {print i}')
    if [[ ${PORT_COUNT} -eq 0 ]]; then
          INT_REJECT_RULE=$(ip6tables --line-numbers -nL "${CHAIN_NAME}" | ${VOLATILE_AWK} '$2 == "REJECT" { print $1 }')
          if [[ -n  "${INT_REJECT_RULE}" ]]; then
            echo "    + adding port ${i} to IPv6 firewall chain '${CHAIN_NAME}'"      
            ip6tables -I "${CHAIN_NAME}" "${INT_REJECT_RULE}" -m tcp -p tcp --dport ${i} -j ACCEPT
          else
            attention_msg "No REJECT-RULE found in IPv6 firewall. Please check manualy for open port; 'tcp ${i}'"
         fi
    fi
    
      done
      
      echo "    + saving current firewall settings"
      service iptables save
      service ip6tables save
     ;;
    esac
      ;;
  debian) 
      echo "    + no firewall enabled on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
  ubuntu) 
      echo "    + no firewall enabled on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
    solaris)
      echo "    + no firewall enabled on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
  *)  err_msg "f_configure_firewall_service: Platform '${INSTALLATION_PLATFORM}' is not supported.";;
    esac
    
    return ${EX_OK}
}

#  Fuegt einen oder mehrere zuvor definierte Services (f_configure_firewall_service)
f_configure_firewall_zone () {
    local STR_FW_ZONE="${1}"

    #  Servicenamen sind die anderen Parameter, also den ersten Parameter shiften
    shift
    
    if [[ "x${STR_FW_ZONE}" == "x" ]]; then
  err_msg "f_configure_firewall_zone: unable to work without firewall zone."
  return ${EX_PARAMETER}
    fi
    
    echo -e "\n* Configuring services in firewall zone '${STR_FW_ZONE}'"

    case ${INSTALLATION_PLATFORM} in
  sles)
      case "${STR_FW_ZONE}" in
    INT|EXT|DMZ)
                    #  Interfaces sind per default in der externen Zone
        FILE_TO_EDIT="/etc/sysconfig/SuSEfirewall2"
        backup_file "${FILE_TO_EDIT}"
        
        local FW_CONFIGURATIONS=$(${VOLATILE_GREP} "^FW_CONFIGURATIONS_${STR_FW_ZONE}" "${FILE_TO_EDIT}" | ${VOLATILE_AWK} -F= '{print $2}' | ${VOLATILE_TR} -d \")
        echo "    + adding services to firewall zone '${STR_FW_ZONE}':"
        echo "      $*"
        FW_CONFIGURATIONS=$(add_elements_to_string "${FW_CONFIGURATIONS}" "$@")
        
        f_replace_in_file ${FILE_TO_EDIT} "s+^FW_CONFIGURATIONS_${STR_FW_ZONE}=.*$+FW_CONFIGURATIONS_${STR_FW_ZONE}=\"${FW_CONFIGURATIONS}\"+" 
        
        #  Firewall neu starten
        /sbin/SuSEfirewall2 --bootunlock start
        ;;
    *)
        err_msg "f_configure_firewall_zone: firewall zone needs to be one of [EXT|INT|DMZ]."
        return ${EX_PARAMETER}
        ;;
      esac
      ;;
  rhel|oracle|centos)
      echo "    + no firewall zones on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
  debian) 
      echo "    + no firewall zones on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
  ubuntu) 
      echo "    + no firewall zones on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
    solaris)
      echo "    + no firewall zones on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
  *)  err_msg "f_configure_firewall_service: Platform '${INSTALLATION_PLATFORM}' is not supported.";;
    esac
    
    return ${EX_OK}
}

#  Fuegt eine Netzwerkschnittstelle in eine Zone der Firewall ein.
f_configure_interface_zone () {
    local STR_FW_ZONE="${1}"

    #  Interfacenamen sind die anderen Parameter, also den ersten Parameter shiften
    shift
    
    if [[ "x${STR_FW_ZONE}" == "x" ]]; then
      err_msg "f_configure_interface_zone: unable to work without firewall zone."
      return ${EX_PARAMETER}
    fi
    
    echo -e "\n* Configuring interfaces in firewall zone '${STR_FW_ZONE}'"

    case ${INSTALLATION_PLATFORM} in
  sles)
      case "${STR_FW_ZONE}" in
    INT|EXT|DMZ)
        #  Interfaces sind per default in der externen Zone
        FILE_TO_EDIT="/etc/sysconfig/SuSEfirewall2"
        backup_file "${FILE_TO_EDIT}"
        f_replace_in_file ${FILE_TO_EDIT} "s+^FW_DEV_${STR_FW_ZONE}=.*$+FW_DEV_${STR_FW_ZONE}=\"$*\"+"
        #  Firewall neu starten
        /sbin/SuSEfirewall2 --bootunlock start
        ;;
      esac
      ;;
  rhel|oracle|centos)
      echo "    + no firewall zones on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
  debian) 
      echo "    + no firewall zones on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
  ubuntu) 
      echo "    + no firewall zones on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
    solaris)
      echo "    + no firewall zones on '${INSTALLATION_PLATFORM}' - skipping"
      ;;
  *)  err_msg "f_configure_firewall_service: Platform '${INSTALLATION_PLATFORM}' is not supported.";;
    esac
    
    return ${EX_OK}
}
