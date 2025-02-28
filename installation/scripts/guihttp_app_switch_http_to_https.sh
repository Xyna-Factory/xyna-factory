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
set -e

DRYRUN="no"
INSTANCE="001"
KEYSTORENAME=""
PORT="4245"
SSL="TLSv1.3"

usage() {
  echo "Usage: $0 -d (Dry run) -i <INSTANCE> (Default: ${INSTANCE}) -k <KEYSTORENAME> (Default: Select keystore with xynafactory.sh listkeystores) -p <PORT> (Default: ${PORT}) -s <SSL> (Default: ${SSL})"
  exit 1
}

while getopts "di:k:p:s:h" option; do
  case "${option}" in
    d)
      DRYRUN="yes"
      ;;
    i)
      INSTANCE=${OPTARG}
      ;;
    k)
      KEYSTORENAME=${OPTARG}
      ;;
    p)
      PORT=${OPTARG}
      ;;
    s)
      SSL=${OPTARG}
      ;;
    h)
      usage
      ;;
    *)
      usage
      ;;
  esac
done


if [[ ! -f /etc/opt/xyna/environment/black_edition_${INSTANCE}.properties ]] ; then
  echo "/etc/opt/xyna/environment/black_edition_${INSTANCE}.properties does not exist!"
  exit 1
fi

XYNA_PATH=$(grep "installation.folder" /etc/opt/xyna/environment/black_edition_${INSTANCE}.properties|cut -d'=' -f2)
XYNA_FACTORY_SH=${XYNA_PATH}/server/xynafactory.sh

if [[ ! -f ${XYNA_FACTORY_SH} ]] ; then
  echo "${XYNA_FACTORY_SH} does not exist!"
  exit 1
fi


# Get Version of Application GuiHttp
GUI_HTTP_APP_VERSION=$(${XYNA_FACTORY_SH} listapplications -applicationName GuiHttp | tail -n -1 | awk '{split($0,a," "); print a[2]}' | sed  "s/'//g")

# Get keystorename
if [[ -z ${KEYSTORENAME} ]]; then
  KEYSTORE_COUNT=$(${XYNA_FACTORY_SH} listkeystores | tail -n +3 | awk '{split($0,a," "); print a[1]}' | wc -l)
  if [[ "$KEYSTORE_COUNT" == "0" ]]; then
    echo "No available keystore found!"
    exit 1
  elif [[ "$KEYSTORE_COUNT" != "1" ]]; then
    KEYSTORE_LIST=$(${XYNA_FACTORY_SH} listkeystores | tail -n +3 | awk '{split($0,a," "); print a[1]}' | awk -v RS="" '{gsub (/\n/," ")}1')
    echo "More then 1 available keystores ($KEYSTORE_LIST) found!"
    exit 1
  fi
  KEYSTORENAME=$(${XYNA_FACTORY_SH} listkeystores | tail -n -1 | awk '{split($0,a," "); print a[1]}')
fi

if [[ "$DRYRUN" == "yes" ]]; then
  echo "# dry run"
fi
echo "# Configuration: (INSTANCE=${INSTANCE}, KEYSTORENAME=${KEYSTORENAME}, GUI_HTTP_APP_VERSION=${GUI_HTTP_APP_VERSION}, PORT=${PORT}, SSL=${SSL})"

echo "# Step 1: Stop the GuiHttp application" 
echo "${XYNA_FACTORY_SH} stopapplication -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION}"
if [[ "$DRYRUN" == "no" ]]; then
  ${XYNA_FACTORY_SH} stopapplication -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION}
fi

echo "# Step 2: Deploy a new https trigger instance" 
echo "${XYNA_FACTORY_SH} deploytrigger -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -triggerName Http -triggerInstanceName Https -startParameters port=${PORT} https=KEY_MGMT clientauth=none keystorename=${KEYSTORENAME} ssl=${SSL}"
if [[ "$DRYRUN" == "no" ]]; then
  ${XYNA_FACTORY_SH} deploytrigger -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -triggerName Http -triggerInstanceName Https -startParameters port=${PORT} https=KEY_MGMT clientauth=none keystorename=${KEYSTORENAME} ssl=${SSL}
fi

echo "# Step 3: Remove the old filter instance H5XdevFilterinstance connected to the http trigger" 
echo "${XYNA_FACTORY_SH} undeployfilter -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -filterInstanceName H5XdevFilterinstance"
if [[ "$DRYRUN" == "no" ]]; then
  ${XYNA_FACTORY_SH} undeployfilter -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -filterInstanceName H5XdevFilterinstance
fi

echo "# Step 4: Remove the old filter instance HttpIni connected to the http trigger"
echo "${XYNA_FACTORY_SH} undeployfilter -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -filterInstanceName HttpIni"
if [[ "$DRYRUN" == "no" ]]; then
  ${XYNA_FACTORY_SH} undeployfilter -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -filterInstanceName HttpIni
fi

echo "# Step 5: Deploy new filter instance HttpIni connected to the https trigger"
echo "${XYNA_FACTORY_SH} deployfilter -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -filterName GUIHTTP -filterInstanceName HttpIni -triggerInstanceName Https"
if [[ "$DRYRUN" == "no" ]]; then
  ${XYNA_FACTORY_SH} deployfilter -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -filterName GUIHTTP -filterInstanceName HttpIni -triggerInstanceName Https
fi

echo "# Step 6: Deploy new filter instance H5XdevFilterinstance connected to the https trigger"
echo "${XYNA_FACTORY_SH} deployfilter -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -filterName H5XdevFilter -filterInstanceName H5XdevFilterinstance -triggerInstanceName Https"
if [[ "$DRYRUN" == "no" ]]; then
  ${XYNA_FACTORY_SH} deployfilter -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION} -filterName H5XdevFilter -filterInstanceName H5XdevFilterinstance -triggerInstanceName Https
fi

echo "# Step 7: Start the GuiHttp application"
echo "${XYNA_FACTORY_SH} startapplication -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION}"
if [[ "$DRYRUN" == "no" ]]; then
  ${XYNA_FACTORY_SH} startapplication -applicationName GuiHttp -versionName ${GUI_HTTP_APP_VERSION}
fi
