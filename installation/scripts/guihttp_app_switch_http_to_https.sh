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
GUIHTTP_APP_VERSION=""
KEYSTORENAME=""
PORT="4245"
SSL="TLSv1.3"

usage() {
  echo "Usage: $0"
  echo " -h : print this help"
  echo " -d : Dry run, whih print the commands to be executed"
  echo " -i <INSTANCE> : Instance of the xyna-factory"
  echo "                 Default value: ${INSTANCE}"
  echo " -v <GUIHTTP_APP_VERSION> : Version of the GuiHttp Application"
  echo "                            Default value: the version is determined with the command: 'xynafactory.sh listapplications -applicationName GuiHttp'"
  echo " -k <KEYSTORENAME> : Name of the keystore in the xyna-factoy"
  echo "                     Default value: the keystore namedetermined with the command: 'xynafactory.sh listkeystores'"
  echo " -p <PORT> : Parameter Port of the https trigger"
  echo "             Default value: ${PORT}"
  echo " -s <SSL> : Parameter ssl of the https trigger"
  echo "            Default value: ${SSL}"
  exit 1
}

while getopts "di:v:k:p:s:h" option; do
  case "${option}" in
    d)
      DRYRUN="yes"
      ;;
    i)
      INSTANCE=${OPTARG}
      ;;
    v)
      GUIHTTP_APP_VERSION=${OPTARG}
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
XYNAFACTORY_SH=${XYNA_PATH}/server/xynafactory.sh

if [[ ! -f ${XYNAFACTORY_SH} ]] ; then
  echo "${XYNAFACTORY_SH} does not exist!"
  exit 1
fi

# Get Version of Application GuiHttp
if [[ -z ${GUIHTTP_APP_VERSION} ]]; then
  GUIHTTP_APP_COUNT=$(${XYNAFACTORY_SH} listapplications -applicationName GuiHttp -t | tail -n +3 | awk '{split($3,a," "); print a[1]}' | wc -l)
  if [[ "$GUIHTTP_APP_COUNT" == "0" ]]; then
    echo "No available GuiHttp application version found! (${XYNAFACTORY_SH} listapplications -applicationName GuiHttp -t)"
    exit 1
  elif [[ "$GUIHTTP_APP_COUNT" != "1" ]]; then
    GUIHTTP_APP_LIST=$(${XYNAFACTORY_SH} listapplications -applicationName GuiHttp -t | tail -n +3 | awk '{split($3,a," "); print a[1]}' | awk -v RS="" '{gsub (/\n/," ")}1')
    echo "More then 1 available GuiHttp application versions ($GUIHTTP_APP_LIST) found! (${XYNAFACTORY_SH} listapplications -applicationName GuiHttp -t)"
    echo "Use -v <GUIHTTP_APP_VERSION> to select the desired version!"
    exit 1
  fi
  GUIHTTP_APP_VERSION=$(${XYNAFACTORY_SH} listapplications -applicationName GuiHttp -t | tail -n -1 | awk '{split($3,a," "); print a[1]}')
fi


# Get keystorename
if [[ -z ${KEYSTORENAME} ]]; then
  KEYSTORE_COUNT=$(${XYNAFACTORY_SH} listkeystores | tail -n +3 | awk '{split($0,a," "); print a[1]}' | wc -l)
  if [[ "$KEYSTORE_COUNT" == "0" ]]; then
    echo "No available keystore name found! (${XYNAFACTORY_SH} listkeystores)"
    exit 1
  elif [[ "$KEYSTORE_COUNT" != "1" ]]; then
    KEYSTORE_LIST=$(${XYNAFACTORY_SH} listkeystores | tail -n +3 | awk '{split($0,a," "); print a[1]}' | awk -v RS="" '{gsub (/\n/," ")}1')
    echo "More then 1 available keystore names ($KEYSTORE_LIST) found! (${XYNAFACTORY_SH} listkeystores)"
	echo "Use -k <KEYSTORENAME> to select the desired keystore name!"
    exit 1
  fi
  KEYSTORENAME=$(${XYNAFACTORY_SH} listkeystores | tail -n -1 | awk '{split($0,a," "); print a[1]}')
fi

echo "# Date: $(date)"
echo "# Configuration"
echo "#   Dry run             : ${DRYRUN}"
echo "#   INSTANCE            : ${INSTANCE}"
echo "#   GUIHTTP_APP_VERSION : ${GUIHTTP_APP_VERSION}"
echo "#   KEYSTORENAME        : ${KEYSTORENAME}"
echo "#   PORT                : ${PORT}"
echo "#   SSL                 : ${SSL})"

echo "# Step 1: Stop the GuiHttp application" 
echo "${XYNAFACTORY_SH} stopapplication -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION}"
if [[ "${DRYRUN}" == "no" ]]; then
  ${XYNAFACTORY_SH} stopapplication -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION}
fi

echo "# Step 2: Deploy a new https trigger instance" 
echo "${XYNAFACTORY_SH} deploytrigger -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -triggerName Http -triggerInstanceName Https -startParameters port=${PORT} https=KEY_MGMT clientauth=none keystorename=${KEYSTORENAME} ssl=${SSL}"
if [[ "${DRYRUN}" == "no" ]]; then
  ${XYNAFACTORY_SH} deploytrigger -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -triggerName Http -triggerInstanceName Https -startParameters port=${PORT} https=KEY_MGMT clientauth=none keystorename=${KEYSTORENAME} ssl=${SSL}
fi

echo "# Step 3: Remove the old filter instance H5XdevFilterinstance connected to the http trigger" 
echo "${XYNAFACTORY_SH} undeployfilter -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -filterInstanceName H5XdevFilterinstance"
if [[ "${DRYRUN}" == "no" ]]; then
  ${XYNAFACTORY_SH} undeployfilter -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -filterInstanceName H5XdevFilterinstance
fi

echo "# Step 4: Remove the old filter instance HttpIni connected to the http trigger"
echo "${XYNAFACTORY_SH} undeployfilter -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -filterInstanceName HttpIni"
if [[ "${DRYRUN}" == "no" ]]; then
  ${XYNAFACTORY_SH} undeployfilter -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -filterInstanceName HttpIni
fi

echo "# Step 5: Deploy new filter instance HttpIni connected to the https trigger"
echo "${XYNAFACTORY_SH} deployfilter -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -filterName GUIHTTP -filterInstanceName HttpIni -triggerInstanceName Https"
if [[ "${DRYRUN}" == "no" ]]; then
  ${XYNAFACTORY_SH} deployfilter -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -filterName GUIHTTP -filterInstanceName HttpIni -triggerInstanceName Https
fi

echo "# Step 6: Deploy new filter instance H5XdevFilterinstance connected to the https trigger"
echo "${XYNAFACTORY_SH} deployfilter -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -filterName H5XdevFilter -filterInstanceName H5XdevFilterinstance -triggerInstanceName Https"
if [[ "${DRYRUN}" == "no" ]]; then
  ${XYNAFACTORY_SH} deployfilter -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION} -filterName H5XdevFilter -filterInstanceName H5XdevFilterinstance -triggerInstanceName Https
fi

echo "# Step 7: Start the GuiHttp application"
echo "${XYNAFACTORY_SH} startapplication -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION}"
if [[ "${DRYRUN}" == "no" ]]; then
  ${XYNAFACTORY_SH} startapplication -applicationName GuiHttp -versionName ${GUIHTTP_APP_VERSION}
fi
