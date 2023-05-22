#!/bin/bash

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2023 Xyna GmbH, Germany
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


RMI_OPTIONS="-Djava.security.policy=server.policy"
LOG4J_OPTIONS="-Dlog4j.debug=false -Dlog4j.configuration=file:log4j.properties";
#DEBUG_OPTIONS="-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=4000 -Xdebug -Xnoagent -Djava.compiler=NONE";
#PROFILING_OPTIONS="-Xrunhprof:cpu=samples,format=a,file=hprof.txt,cutoff=0,depth=12,thread=y"
#  EXCEPTION_OPTIONS="-Dexceptions.storage=Exceptions.xml -DBLACK_SERVER_HOME=${PWD}";
#exec 2>&-
java -classpath XynaCoherence.jar:log4j-1.2.16.jar:rmiio-2.0.2.jar:xynautils-exceptions-I20110203_1540.jar:xynautils-logging-3.0.0.0.jar ${LOG4J_OPTIONS} ${DEBUG_OPTIONS} ${PROFILING_OPTIONS} ${RMI_OPTIONS} com.gip.xyna.coherence.standalone.CoherenceClientStandalone "$@"
