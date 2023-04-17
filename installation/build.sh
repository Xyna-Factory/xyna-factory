#!/bin/bash

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2023 GIP SmartMercial GmbH, Germany
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


print_help() {
  echo "$0: build some or all parts of xyna."
  echo "available options are: xynautils, all, compose"
}

check_dependencies() {
  echo "checking dependencies..."
  java --version
  mvn --version
  ant -version
  git --version
  zip --version
}

checkout_factory() {
  echo "cheking out factory..."
  # $1 where to check out
}

build_xynautils_exceptions() {
  echo "building xynautils-exceptions..."
  cd $SCRIPT_DIR/../xynautils/exceptions
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp integration
  mv ../releases/xynautils-exceptions/xynautils-exceptions-I*[0-9].jar ../releases/xynautils-exceptions/xynautils-exceptions.jar
  mvn install:install-file -Dfile=../releases/xynautils-exceptions/xynautils-exceptions.jar -DpomFile=./pom.xml -Dversion=I20210705_1332
}

build_xynautils_logging() {
  echo "building xynautils-logging..."
  cd $SCRIPT_DIR/../xynautils/logging
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp integration
  mv ../releases/xynautils-logging/xynautils-logging-I*[0-9].jar ../releases/xynautils-logging/xynautils-logging.jar
  mvn install:install-file -Dfile=../releases/xynautils-logging/xynautils-logging.jar -DpomFile=./pom.xml -Dversion=I20181114_1211
  mvn install:install-file -Dfile=../releases/xynautils-logging/xynautils-logging.jar -DpomFile=./pom.xml -Dversion=3.0.0.0
}

build_xynautils_database() {
  echo "building xynautils-database..."
  cd $SCRIPT_DIR/../xynautils/database
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp integration
  mv ../releases/xynautils-database/xynautils-database-I*[0-9].jar ../releases/xynautils-database/xynautils-database.jar
  mvn install:install-file -Dfile=../releases/xynautils-database/xynautils-database.jar -DpomFile=./pom.xml -Dversion=I20211207_0946
  mvn install:install-file -Dfile=../releases/xynautils-database/xynautils-database.jar -DpomFile=./pom.xml -Dversion=I20190829_1328
  mvn install:install-file -Dfile=../releases/xynautils-database/xynautils-database.jar -DpomFile=./pom.xml -Dversion=2.4.0.1
  mvn install:install-file -Dfile=../releases/xynautils-database/xynautils-database.jar -DpomFile=./pom.xml -Dversion=3.0.0
}

build_xynautils_snmp() {
  echo "building xynautils-snmp..."
  cd $SCRIPT_DIR/../xynautils/snmp
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp integration
  mv ../releases/xynautils-snmp/xynautils-snmp-I*[0-9].jar ../releases/xynautils-snmp/xynautils-snmp.jar
  mvn install:install-file -Dfile=../releases/xynautils-snmp/xynautils-snmp.jar -DpomFile=./pom.xml -Dversion=I20190729_1044
  mvn install:install-file -Dfile=../releases/xynautils-snmp/xynautils-snmp.jar -DpomFile=./pom.xml -Dversion=4.0.0
  mvn install:install-file -Dfile=../releases/xynautils-snmp/xynautils-snmp.jar -DpomFile=./pom.xml -Dversion=I20181112_0943
}

build_xynautils_ldap() {
  echo "building xynautils-ldap..."
  cd $SCRIPT_DIR/../xynautils/ldap
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp integration
  mv ../releases/xynautils-ldap/xynautils-ldap-I*[0-9].jar ../releases/xynautils-ldap/xynautils-ldap.jar
  mvn install:install-file -Dfile=../releases/xynautils-ldap/xynautils-ldap.jar -DpomFile=./pom.xml
}

build_xynautils_misc() {
  echo "building xynautils-misc..."
  cd $SCRIPT_DIR/../xynautils/misc
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp integration
  mv ../releases/xynautils-misc/xynautils-misc-I*[0-9].jar ../releases/xynautils-misc/xynautils-misc.jar
  mvn install:install-file -Dfile=../releases/xynautils-misc/xynautils-misc.jar -DpomFile=./pom.xml -Dversion=2.3.0.0
  mvn install:install-file -Dfile=../releases/xynautils-misc/xynautils-misc.jar -DpomFile=./pom.xml -Dversion=3.0.1
}

build_misc() {
  echo "building misc..."
  cd $SCRIPT_DIR/../misc
  mkdir -p lib/xyna
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  sed -i 's/ depends="resolve"//' build.xml
  ant -Doracle.home=/tmp build
  mvn install:install-file -Dfile=./deploy/misc.jar -DpomFile=./pom.xml
}

build_xynafactory_jar() {
  echo "building xynafactory.jar..."
  cd $SCRIPT_DIR/../server
  mkdir -p lib/internal_xyna
  cp build.xml build.xml.bak
  sed -i 's/depends="resolve, /depends="/' build.xml
  sed -i "s/XynaFactoryServer/xynafactory/" pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>ecj</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>XynaJavaSerializationPersistenceLayer</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>OraclePersistenceLayer</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>XynaLocalMemoryPersistenceLayer</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>XynaMemoryPersistenceLayer</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>XynaXMLShellPersistenceLayer</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>xynafactoryCLIGenerator</{N;N;d}}}' pom.xml
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp buildCliClassGeneratorJar
  mvn install:install-file -Dfile=./deploy/xynafactoryCLIGenerator.jar -DpomFile=./pom.xml -Dversion=1.0.0 -DartifactId=xynafactoryCLIGenerator -DgroudId="com.gip.xyna"
  cp ./deploy/xynafactoryCLIGenerator.jar lib/xynafactoryCLIGenerator-1.0.0.jar
  ant -Doracle.home=/tmp build
  mvn install:install-file -Dfile=./deploy/xynafactory.jar -DpomFile=./pom.xml -Dversion=9.0.0
  mvn install:install-file -Dfile=./deploy/xynafactory.jar -DpomFile=./pom.xml -Dversion=9.0.0.0
  cp lib/xynafactoryCLIGenerator-1.0.0.jar .
  
  
  ant -Doracle.home=/tmp -Dxyna.clusterprovider.OracleRACClusterProvider=false build
  
  rm -rf lib build.xml
  mv build.xml.bak build.xml
  mkdir -p lib/internal_xyna
  mv xynafactoryCLIGenerator-1.0.0.jar lib/
}

build_defaultconnectionpooltypes() {
  cd components/xnwh/pools/DefaultConnectionPoolTypes
  sed -i 's/depends="resolve"//' build.xml
  mkdir lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp build
  mvn install:install-file -Dfile=./deploy/DefaultConnectionPoolTypes.jar -DpomFile=./pom.xml 
}


prepare_modules() {
  echo "prepareing modules..."
  cd $SCRIPT_DIR/..
  # sed -i '/websphere/d' modules/xact/queue/build.xml # can not build without unavailable libs
  sed -i '/<dependency>/{N;N;{/<artifactId>com.ibm.mq.traceControl/{N;N;d}}}' modules/xact/queue/webspheremq/sharedlib/webspheremq/pom.xml
  sed -i '/<dependency>/{N;N;{/fscontext/{N;N;d}}}' modules/xact/queue/webspheremq/sharedlib/webspheremq/pom.xml
  sed -i '/<dependency>/{N;N;{/javaee-api/{N;N;d}}}' modules/xact/queue/webspheremq/sharedlib/webspheremq/pom.xml
  sed -i '/<dependency>/{N;N;{/providerutil/{N;N;d}}}' modules/xact/queue/webspheremq/sharedlib/webspheremq/pom.xml
  sed -i '/<dependency>/{N;N;{/jms/{N;N;d}}}' modules/xact/queue/webspheremq/sharedlib/webspheremq/pom.xml
  sed -i '/<\/dependencies>/,$d'  modules/xact/queue/webspheremq/sharedlib/webspheremq/pom.xml
  echo "<dependency><groupId>org.apache.geronimo.specs</groupId><artifactId>geronimo-jms_1.1_spec</artifactId><version>1.1.1</version></dependency>" >> modules/xact/queue/webspheremq/sharedlib/webspheremq/pom.xml
  echo "</dependencies></project>" >> modules/xact/queue/webspheremq/sharedlib/webspheremq/pom.xml
  sed -i '/<copy/d' modules/xact/queue/webspheremq/sharedlib/webspheremq/build.xml
  sed -i '/<dependency>/{N;N;{/jms/{N;N;d}}}' modules/xact/queue/webspheremq/mdmimpl/WebSphereMQImpl/pom.xml
  sed -i '/<\/dependencies>/,$d'  modules/xact/queue/webspheremq/mdmimpl/WebSphereMQImpl/pom.xml
  echo "<dependency><groupId>org.apache.geronimo.specs</groupId><artifactId>geronimo-jms_1.1_spec</artifactId><version>1.1.1</version></dependency>" >> modules/xact/queue/webspheremq/mdmimpl/WebSphereMQImpl/pom.xml
  echo "</dependencies></project>" >> modules/xact/queue/webspheremq/mdmimpl/WebSphereMQImpl/pom.xml
  sed -i '/<dependency>/{N;N;{/jms/{N;N;d}}}' modules/xact/queue/webspheremq/triggerimpl/WebSphereMQTrigger/test_filter/pom.xml
  sed -i '/<\/dependencies>/,$d' modules/xact/queue/webspheremq/triggerimpl/WebSphereMQTrigger/test_filter/pom.xml
  echo "<dependency><groupId>org.apache.geronimo.specs</groupId><artifactId>geronimo-jms_1.1_spec</artifactId><version>1.1.1</version></dependency>" >> modules/xact/queue/webspheremq/triggerimpl/WebSphereMQTrigger/test_filter/pom.xml
  echo "</dependencies></project>" >> modules/xact/queue/webspheremq/triggerimpl/WebSphereMQTrigger/test_filter/pom.xml
  mkdir -p modules/xact/queue/webspheremq/sharedlib/webspheremq/lib
  mkdir -p modules/xact/queue/webspheremq/triggerimpl/WebSphereMQTrigger/test_filter/lib
  mkdir -p modules/xact/queue/webspheremq/mdmimpl/WebSphereMQImpl/lib
  cd modules/xact/queue/webspheremq/sharedlib/webspheremq
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  cd ../../../../../../
  cd modules/xact/queue/webspheremq/mdmimpl/WebSphereMQImpl/
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  cd ../../../../../../
  cd modules/xact/queue/webspheremq/triggerimpl/WebSphereMQTrigger/test_filter
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
}

build_oracle_aq_tools() {
  echo "build oracleAQ Tools"
  cd $SCRIPT_DIR/build
  sed -i 's#name="prepareLibs"#name="prepareLibsX"#' ../../modules/xact/queue/oracleaq/sharedlib/OracleAQTools/build.xml
  ant -Doracle.home=/tmp -f ../../modules/xact/queue/oracleaq/sharedlib/OracleAQTools/build.xml build
  mvn install:install-file -Dfile=../../modules/xact/queue/oracleaq/sharedlib/OracleAQTools/deploy/OracleAQTools.jar -DpomFile=../../modules/xact/queue/oracleaq/sharedlib/OracleAQTools/pom.xml 
}

build_modules() {
  echo "building modules..."
  cd $SCRIPT_DIR/../modules
  ant -Doracle.home=/tmp
}

build_conpooltypes() {
  echo "building connectionpooltypes..."
  cd $SCRIPT_DIR/build
  ant -Doracle.home=/tmp conpooltypes
  mvn install:install-file -Dfile=../../common/lib/xyna/DefaultConnectionPoolTypes.jar -DpomFile=../../components/xnwh/pools/DefaultConnectionPoolTypes/pom.xml
}

build_persistencelayers() {
  echo "building persistencelayers..."
  cd $SCRIPT_DIR/build
  
  #build and install memory persistencelayer
  ant -Doracle.home=/tmp -f  ../../persistencelayers/XynaMemoryPersistenceLayer/build.xml
  mvn install:install-file -Dfile=../../persistencelayers/XynaMemoryPersistenceLayer/deploy/XynaMemoryPersistenceLayer.jar -DpomFile=../../persistencelayers/XynaMemoryPersistenceLayer/pom.xml

  #build and install install XynaJavaSerializationPersistenceLayer
  ant -Doracle.home=/tmp -f ../../persistencelayers/XynaJavaSerializationPersistenceLayer/build.xml
  mvn install:install-file -Dfile=../../persistencelayers/XynaJavaSerializationPersistenceLayer/deploy/XynaJavaSerializationPersistenceLayer.jar -DpomFile=../../persistencelayers/XynaJavaSerializationPersistenceLayer/pom.xml

  # build all persistencelayers
  ant -Doracle.home=/tmp buildPersistenceLayers  
}

build_plugins() {
  echo "building plugins..."
  cd $SCRIPT_DIR/build
  ant -Doracle.home=/tmp buildPlugins
}

build_xyna_factory() {
  echo "building artifact"
  cd $SCRIPT_DIR/..
  rm -rf release
  mkdir -p release
  
  compose_checkscripts
  compose_components
  compose_dhcpd
  compose_doc
  compose_etc
  compose_func_lib
  compose_templateMechanismStandalone
  compose_thridparties
  compose_server
  compose_files
  zip_result
}

#TODO: version name
zip_result() {
  zip -r ../XynaFactory.zip $SCRIPT_DIR/../release
}

#TODO: - mb mvn call like for server/lib?
compose_thridparties() {
  cd $SCRIPT_DIR/../release
  mkdir thrid_parties
}

#TODO: buildTemplateMechanismStandalone is a target in installation/build/build.xml
#ant -Doracle.home=/tmp buildTemplateMechanismStandalone
# requires com.gip.xyna:RemoteGenericODSAccess:jar:1.0.0
# seems like RemoteGenericODSAccess can be added to Repository - already has a ticket.
compose_templateMechanismStandalone() {
  cd $SCRIPT_DIR/../release
  mkdir TemplateMechanismStandalone
}

compose_func_lib() {
  cd $SCRIPT_DIR/../release
  cp -r ../prerequisites/installation/install/func_lib .
}


compose_etc() {
  cd $SCRIPT_DIR/../release
  mkdir etc
  cp ../installation/etc/initd_dhcpd etc
}

compose_doc() {
  cd $SCRIPT_DIR/../release
  mkdir doc
  # just an empty folder...
}

#TODO:
compose_dhcpd() {
  cd $SCRIPT_DIR/../release
  mkdir dhcpd

}


compose_checkscripts() {
  cd $SCRIPT_DIR/../release
  cp -r ../installation/CheckScripts .
}

compose_files() {
  cd $SCRIPT_DIR/../release
  cp ../blackedition/blackedition_lib.sh .
  cp ../blackedition/install_black_edition.sh .
  cp ../blackedition/uninstall_black_edition.sh .
}

# TODO: currently only modules applications
# buildNetworkAvailability is a target in installation/build/build.xml 
compose_components() {
  cd $SCRIPT_DIR/../release
  mkdir components
  cd components
  cp -r ../../modules/deploy/* .
}


compose_server() {
  cd $SCRIPT_DIR/../release
  mkdir server
  cd server
  compose_server_clusterproviders
  compose_server_conpooltypes
  compose_server_datamodeltypes
  compose_server_exceptions
  compose_server_lib
  compose_server_orderinpoutsourcetypes
  compose_server_persistencelayers
  compose_server_repositoryaccess
  compose_server_resources
  compose_server_storage
  compose_server_files
}

#TODO
compose_server_files() {
  cd $SCRIPT_DIR/../release
  #log4j.xml
  #NSNDSLAMExceptionmappings.xml
  #product_lib.sh
  cp ../server/server.policy ./server
  cp ../server/deploy/TemplateImpl.zip ./server
  #TemplateImplNew.zip
  cp ../server/xynafactory.sh ./server
}

#TODO: dhcp
compose_server_storage() {
  cd $SCRIPT_DIR/../release/server
  mkdir storage
  mkdir storage/BlackExceptionCodeManagement
  cp ../../installation/codegroup.xml storage/BlackExceptionCodeManagement
  cp ../../installation/codepattern.xml storage/BlackExceptionCodeManagement
  #TODO: dhcp -> optionsv4 and optionsv6
  cp $SCRIPT_DIR/../persistencelayers/persistencelayers.xml storage
}

compose_server_resources() {
  cd $SCRIPT_DIR/../release/server
  mkdir resources
  cp ../../localization/localization.xml resources
  cp ../../_Interfaces/XMDM.xsd resources
  cp -r ../../installation/sql/orderarchive resources
}

#TODO: repositoryaccess build and copy
compose_server_repositoryaccess() {
  cd $SCRIPT_DIR/../release/server
  mkdir repositoryaccess
}

compose_server_persistencelayers() {
  cd $SCRIPT_DIR/../release/server
  cp -r $SCRIPT_DIR/../localbuild/server/persistencelayers/ .
  
  #cluster
  cp -r $SCRIPT_DIR/../components/xnwh/xcs $SCRIPT_DIR/../release/components/xnwh
}

compose_server_orderinpoutsourcetypes() {
  cd $SCRIPT_DIR/../release/server
  cp -r $SCRIPT_DIR/../localbuild/server/orderinputsourcetypes .
}

#TODO: INCLUDE License
compose_server_lib() {
  cd $SCRIPT_DIR/../release/server
  mkdir lib
  cd $SCRIPT_DIR/delivery
  mvn dependency:resolve
  mvn -DoutputDirectory="$SCRIPT_DIR/../release/server/lib" dependency:copy-dependencies
  
  # licenses
  mvn license:download-licenses -s pom.xml license:download-licenses -DlicensesOutputDirectory=../../release/server/lib -DlicensesOutputFile=../../release/server/lib/licenses.xml
}

compose_server_exceptions() {
  cd $SCRIPT_DIR/../release/server
  cp -r $SCRIPT_DIR/../server/exceptions exceptions
}

compose_server_datamodeltypes() {
  cd $SCRIPT_DIR/../release/server
  cp -r $SCRIPT_DIR/../localbuild/server/datamodeltypes  .
}

compose_server_conpooltypes() {
  cd $SCRIPT_DIR/../release/server
  cp -r $SCRIPT_DIR/../localbuild/server/conpooltypes $SCRIPT_DIR/../release/server/conpooltypes
}

#TODO: clusterprovider build and copy
compose_server_clusterproviders(){
  cd $SCRIPT_DIR/../release/server
  mkdir clusterproviders
}


prepare_build() {
  mkdir -p /opt/common
  cd $SCRIPT_DIR/build
  sed -i "s#<url>file:\/\/.*<\/url>#<url>file://$HOME/.m2/repository<\/url>#" $SCRIPT_DIR/build/defaultMavenSettings.xml #TODO: allow config
  mvn install
}


build_xynautils() {
 echo "building xynautils..."
  build_xynautils_exceptions
  build_xynautils_logging
  build_xynautils_database
  build_xynautils_snmp
  build_xynautils_ldap
  build_xynautils_misc
}

build_all() {
  build_xynautils
  build_misc
  build_xynafactory_jar
  build_persistencelayers
  prepare_modules
  build_oracle_aq_tools
  build_modules
  build_conpooltypes
  build_plugins
  build_xyna_factory
}


# main
if [ $# -eq 0 ]
then
  print_help
  exit 0
fi

check_dependencies
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
prepare_build

case $1 in
  "xynautils")
    build_xynautils
	;;
  "all")
    build_all
	;;
  "compose")
    build_xyna_factory
	;;
  *)
    echo "unknown argument: $1"
	exit 1
	;;
esac

exit 0
