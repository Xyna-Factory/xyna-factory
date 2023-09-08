#!/bin/bash

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

export LANG="UTF-8"
export LC_ALL="UTF-8"

CLASSES="";
  for i in lib/*.jar; do
    if [[ -f "${i}" ]]; then
      CLASSES="${i}:${CLASSES}"
    fi
  done

#DEBUG_OPTIONS="-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=4011 -Xdebug -Xnoagent -Djava.compiler=NONE";

  #GC_OPTIONS="-verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails"
  LOG4J_OPTIONS="-Dlog4j.debug=false -Dlog4j.configuration=file:log4j.properties";
  EXCEPTION_OPTIONS="-Dexceptions.storage=Exceptions.xml -DBLACK_SERVER_HOME=${PWD}";

  exec 2>&-

 java -classpath lasttest:bin:$CLASSES -Xmx128M ${LOG4J_OPTIONS} ${EXCEPTION_OPTIONS} ${GC_OPTIONS} ${DEBUG_OPTIONS} com.gip.xyna.LastTestClient $* 2>&1
