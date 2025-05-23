#/bin/bash

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

#$1 local path
#$2 delivery name
#$3 target(s) to call

. asenv.sh

var=`ls $1/$2/install/uninstall.xml`

if [ "x${var}" != "x" ]; then
  $ORACLE_HOME/ant/bin/ant -f $var $3

  if [ $? -ne 0 ]; then
    exit -1
  fi

fi
