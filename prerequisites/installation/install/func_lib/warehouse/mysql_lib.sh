
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



# MySQL library


#  DB-Verbindung zu einer mysql-DB fuer bestimmten Benutzer testen
f_check_mysql_connection() {
  local STR_DB_USER="${1:-root}"
  local STR_DB_PASS="${2:-root}"
  local STR_DB_HOST="${3:-localhost}"
  local STR_EXTRA_ARGS="${4}"
  local ret_val=0
    
  echo -en "\n* Checking database connection for: '${STR_DB_USER}' @ '${STR_DB_HOST}'"
  
  result="$(which mysql)"
  if [[ -z "${result}" ]]; then
    err_msg "f_check_mysql_connection: mysql not found"
    return ${EX_OSFILE}
  fi
    
  mysql -u ${STR_DB_USER} -p${STR_DB_PASS} -h ${STR_DB_HOST} -e "exit" 2>/dev/null
  ret_val=$?
  if [[ ${ret_val} -ne 0 ]]; then
    f_failed
  else
    f_ok
  fi 
  
  return ${ret_val}
}
