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

# Oracle Database library

###################
#Umgebunsgvariablen setzen
f_set_environment () { # <- maybe strip this and moce somewhere else (is used by install_prerequisites.sh)
  DIR_ORACLE_BASE="/opt/oracle"
  DIR_ORACLE_CLIENT_HOME="${DIR_ORACLE_BASE}/instantclient"
  DIR_ORACLE_TNS_ADMIN="${DIR_ORACLE_CLIENT_HOME}/network/admin"
  LST_JDBC_FILES="orai18n.jar ojdbc7.jar ojdbc8.jar"

  VOLATILE_SQLPLUS="${DIR_ORACLE_CLIENT_HOME}/sqlplus"
  
  SSL_CERTIFICATE_DIR="${INSTALL_PREFIX}/.ssl"
}