
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2022 GIP SmartMercial GmbH, Germany
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
