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


# Username of the AS admin
AS_USERID=${4}
# Password of the AS admin
AS_PASSWORD=${5}

# User for instance level security
OC4J_USER=${2}
OC4J_PASS=${3}

OC4J_INSTANCE=${6}

################################ DON'T CHANGE BEYOND THIS LINE ################################

REALM="jazn.com"
ROLES="users"

if [ $# -lt 6 ]; then
	echo "usage: ${0} create|drop OC4J_USER OC4J_PASS AS_USERID AS_PASSWORD OC4J_INSTANCE"
	echo
	echo "Creates or drops the user 'OC4J_USER' in the instance 'OC4J_INSTANCE'."
	echo
	echo "Abort!"
	exit 99
fi

case ${1} in
	create|drop) ;;
	*) echo "usage: ${0} create|drop"; exit 98;;
esac

if [ "x${ORACLE_HOME}" = "x" ]; then
	echo "Environment \$ORACLE_HOME is not set."
	echo
	echo "Abort!"
	exit 97
fi

if [ ! -d ${ORACLE_HOME} ]; then
	echo "Directory '${ORACLE_HOME}' not found."
	echo
	echo "Abort!"
	exit 96
fi

JAVA_volatile="${ORACLE_HOME}/jre/1.4.2/bin/java"
if [ ! -f ${JAVA_volatile} ]; then
	echo "File '${JAVA_volatile}' not found."
	echo
	echo "Abort!"
	exit 95
fi

JAZN_volatile="${ORACLE_HOME}/j2ee/home/jazn.jar"
if [ ! -f ${JAZN_volatile} ]; then
	echo "File '${JAZN_volatile}' not found."
	echo
	echo "Abort!"
	exit 94
fi

OC4J_INSTANCE_DIR="${ORACLE_HOME}/j2ee/${OC4J_INSTANCE}"
if [ ! -d ${OC4J_INSTANCE_DIR} ]; then
	echo "Directory '${OC4J_INSTANCE_DIR}' not found."
	echo
	echo "Abort!"
	exit 93
fi

## Check, if the realm exists
echo "Checking realm ..."
${JAVA_volatile} -jar -Doracle.j2ee.home=${OC4J_INSTANCE_DIR} ${JAZN_volatile} -user ${AS_USERID} -password ${AS_PASSWORD} -listrealms | grep ${REALM} >/dev/null
if [ $? -ne 0 ]; then
	echo "Realm '${REALM}' not found."
	echo
	echo "Abort!"
	exit 92
fi

echo "Checking roles ..."
for i in ${ROLES}
do
	${JAVA_volatile} -jar -Doracle.j2ee.home=${OC4J_INSTANCE_DIR} ${JAZN_volatile} -user ${AS_USERID} -password ${AS_PASSWORD} -listroles | grep ${i} >/dev/null
	if [ $? -ne 0 ]; then
		echo "Role '${i}' not found."
		echo
		echo "Abort!"
		exit 91
	fi
done

echo "Checking for user '${OC4J_USER}' ..."
case ${1} in
	create) ${JAVA_volatile} -jar -Doracle.j2ee.home=${OC4J_INSTANCE_DIR} ${JAZN_volatile} -user ${AS_USERID} -password ${AS_PASSWORD} -listusers ${REALM} | grep ${OC4J_USER} >/dev/null;
		if [ $? -ne 0 ]; then
			echo "Adding user '${OC4J_USER}' ..."
			${JAVA_volatile} -jar -Doracle.j2ee.home=${OC4J_INSTANCE_DIR} ${JAZN_volatile} -user ${AS_USERID} -password ${AS_PASSWORD} -adduser ${REALM} ${OC4J_USER} ${OC4J_PASS};
			for i in ${ROLES}; do
				echo "Granting role '${i}' to user '${OC4J_USER}' ..."
				${JAVA_volatile} -jar -Doracle.j2ee.home=${OC4J_INSTANCE_DIR} ${JAZN_volatile} -user ${AS_USERID} -password ${AS_PASSWORD} -grantrole ${i} ${REALM} ${OC4J_USER};
			done
		else
			echo "User '${OC4J_USER}' already exists.";
		fi
		;;
	drop) ${JAVA_volatile} -jar -Doracle.j2ee.home=${OC4J_INSTANCE_DIR} ${JAZN_volatile} -user ${AS_USERID} -password ${AS_PASSWORD} -listusers ${REALM} | grep ${OC4J_USER} >/dev/null;
		if [ $? -eq 0 ]; then
			for i in ${ROLES}; do
				echo "Revoking role '${i}' from user '${OC4J_USER}' ..."
				${JAVA_volatile} -jar -Doracle.j2ee.home=${OC4J_INSTANCE_DIR} ${JAZN_volatile} -user ${AS_USERID} -password ${AS_PASSWORD} -revokerole ${i} ${REALM} ${OC4J_USER};
			done
			echo "Removing user '${OC4J_USER}' ..."
			${JAVA_volatile} -jar -Doracle.j2ee.home=${OC4J_INSTANCE_DIR} ${JAZN_volatile} -user ${AS_USERID} -password ${AS_PASSWORD} -remuser ${REALM} ${OC4J_USER};
		else
			echo "User '${OC4J_USER}' not found.";
		fi
		;;
	*) # should not go here
		;;
esac
