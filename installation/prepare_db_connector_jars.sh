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

INSTANCE=001


if [[ $# == 1 ]];
  INSTANCE=$1
fi

echo "processing xyna instance ${INSTANCE}"

if [[ ! -f /etc/opt/xyna/environment/black_edition_${INSTANCE}.properties ]] ; then
  echo "/etc/opt/xyna/environment/black_edition_${INSTANCE}.properties does not exist!"
  exit 1
fi

XYNA_PATH=$(grep "installation.folder" /etc/opt/xyna/environment/black_edition_${INSTANCE}.properties|cut -d'=' -f2)

if [[ ! -d $XYNA_PATH/server ]] ; then
  echo "$XYNA_PATH/server does not exist!"
  exit 1
fi

# copy to userlib
mkdir -p $XYNA_PATH/server/userlib
cp mysql-connector*.jar $XYNA_PATH/server/userlib
cp ojdbc*.jar $XYNA_PATH/server/userlib

# copy licenses
mkdir -p $XYNA_PATH/third_parties/db
cp licenses.xml $XYNA_PATH/third_parties/db
cp 'oracle free use terms and conditions (futc) - oracle-free-license.html' $XYNA_PATH/third_parties/db

exit 0