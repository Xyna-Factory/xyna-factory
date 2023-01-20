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
