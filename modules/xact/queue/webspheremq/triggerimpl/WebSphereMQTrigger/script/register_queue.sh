#! /bin/bash

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


HERE=$(dirname $0)
cd $HERE
HERE=$(pwd)


source server.properties
source queue.properties


cd ${LOCAL_SERVER_PATH} && ./${XYNA_FACTORY_SH} registerqueue -uniqueName $Q1_UNIQUE -externalName $Q1_NAME -queueType WEBSPHERE_MQ -connectParameters  $Q1_QUEUE_MANAGER $Q1_HOST $Q1_PORT $Q1_CHANNEL

