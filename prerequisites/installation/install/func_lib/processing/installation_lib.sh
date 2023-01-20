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

# Processing -> Xyna Installation library


# Installation des /etc/init.d/xynafactory_???-Skripts
f_etc_initd_xyna_files () {
   #Basis-Ersetzungen
  local -a SEDS=("s+TOKEN_INSTALL_PREFIX+${INSTALL_PREFIX}+" \
                 "s+TOKEN_PROVIDES_XYNA_FACTORY+xynafactory_${PRODUCT_INSTANCE}+" \
                 "s+TOKEN_XYNA_USER+${XYNA_USER}+");
 
  if [[ "x${SYSTEMD_ENV}" == "xtrue" ]] ; then
  
    SEDS=("${SEDS[@]}" "s+TOKEN_XYNA_GROUP+${XYNA_GROUP}+");
    
    #TODO auch hier DB in After eintragen?

    #Anlegen des /etc/systemd/system/xynafactory???.service-Skripts
    f_etc_systemd_files "xynafactory" "application/xynafactory" "${SEDS[@]}"
  else 
  
  #Spezial-Ersetzungen erforderliche DBs (nur SLES und Debian!)
  for i in oracle-xe mysql oracle ; do
    if [[ -f "/etc/init.d/${i}" ]]; then
      SEDS=("${SEDS[@]}" "s+\(.*Required-Start.*\)+\1 ${i}+" "s+\(.*Required-Stop.*\)+\1 ${i}+");
    fi
  done
 
  #Anlegen des /etc/init.d/xynafactory_???-Skripts
  f_etc_initd_files "xynafactory" "application/xynafactory" "${SEDS[@]}"
  fi;
}
