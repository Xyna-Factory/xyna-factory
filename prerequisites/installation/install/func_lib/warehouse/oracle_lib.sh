
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2022 Xyna GmbH, Germany
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