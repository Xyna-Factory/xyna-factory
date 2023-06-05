
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



# Processing -> Xyna Applications library


######################################################################
# Versionsnummer
#  Eingabeparameter
######################################################################
f_get_factory_version () {
  local ret_val="false"

  #Zeile mit "Xyna Factory Server" suchen und letztes Feld ausgeben
  local STR_FACTORY_VERSION=$(f_xynafactory version | ${VOLATILE_AWK} '$0~".*Xyna Factory Server.*" { print $NF; }' )
  
  ret_val=$?
  echo ${STR_FACTORY_VERSION}
  return $ret_val
}


######################################################################
# Versionsnummer
#  Eingabeparameter
#o   Version1
#o   Vergleichsoperator <, <=, ==, >, >= 
#o   Version2
# Beispiel:  if f_version_compare ${STR_FACTORY_VERSION} ">=" 5.1.4.8; then ...
######################################################################
f_version_compare () {
  local OPERATOR="${2}";
  local RESPONSE  #hier schon local, damit local nicht ExitCode kaputt macht
  local ret_val=1;
  $(echo "${1}.x.${3}.x" | ${VOLATILE_AWK} -F. '{ if( $5!="x" || $10!="x") { exit 2 };
    diff=$1-$6; if( diff != 0 ) { exit (diff '${OPERATOR}' 0 )? 0 : 1; }
    diff=$2-$7; if( diff != 0 ) { exit (diff '${OPERATOR}' 0 )? 0 : 1; }
    diff=$3-$8; if( diff != 0 ) { exit (diff '${OPERATOR}' 0 )? 0 : 1; }
    diff=$4-$9;                   exit (diff '${OPERATOR}' 0 )? 0 : 1;
    }');
  ret_val=$?
  if [[ ${ret_val} == 2 ]] ; then 
    f_add_to_error_buffer "Invalid version compare '${1}' ${2} '${3}'";
  fi;
  return ${ret_val}
}
