#!/bin/bash

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2024 Xyna GmbH, Germany
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

print_help() {
  echo "Usage: $0 xynautils"
  echo "Usage: $0 build"
  echo "Usage: $0 all -b GIT_BRANCH_XYNA_MODELLER"
  echo "Usage: $0 compose"
  echo "Usage: $0 plugins"
  echo "Usage: $0 clusterproviders"
  echo "Usage: $0 conpooltypes"
  echo "Usage: $0 install_libs"
}

check_dependencies() {
  echo "checking dependencies..."
  java --version
  mvn --version
  ant -version
  git --version
  zip --version
}

check_dependencies_frontend() {
  node --version
}

checkout_factory() {
  echo "cheking out factory..."
  # $1 where to check out
}

install_libs() {
  echo "installing libs..."
  if [[ -z ${MAVEN_RESOLVER_ANT_TASKS_VERSION} ]]; then
    print_help
    echo "Error: MAVEN_RESOLVER_ANT_TASKS_VERSION is not set"; 
    exit 1
  fi
  if [[ -z ${ANT_CONTRIB_TASKS_VERSION} ]]; then
    print_help
    echo "Error: ANT_CONTRIB_TASKS_VERSION is not set"; 
    exit 1
  fi
  HTTP_CODE_OK=200
  
  mkdir -p ${HOME}/.ant/lib
  URL=https://repo1.maven.org/maven2/org/apache/maven/resolver/maven-resolver-ant-tasks/${MAVEN_RESOLVER_ANT_TASKS_VERSION}/maven-resolver-ant-tasks-${MAVEN_RESOLVER_ANT_TASKS_VERSION}-uber.jar
  TARGET_FILE=${HOME}/.ant/lib/maven-resolver-ant-tasks-${MAVEN_RESOLVER_ANT_TASKS_VERSION}-uber.jar
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" ${URL})
  if [ "$HTTP_CODE" != "$HTTP_CODE_OK" ]; then 
    echo "Error: HTTP_CODE=${HTTP_CODE}, URL=${URL}"; 
    exit 1
  fi
  curl -s ${URL} -o "${TARGET_FILE}"
  
  URL=https://repo1.maven.org/maven2/ant-contrib/ant-contrib/${ANT_CONTRIB_TASKS_VERSION}/ant-contrib-${ANT_CONTRIB_TASKS_VERSION}.jar
  TARGET_FILE="${HOME}/.ant/lib/ant-contrib-${ANT_CONTRIB_TASKS_VERSION}.jar"
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" ${URL})
  if [ "$HTTP_CODE" != "$HTTP_CODE_OK" ]; then 
    echo "Error: HTTP_CODE=${HTTP_CODE}, URL=${URL}"; 
    exit 1
  fi
  curl -s ${URL} -o "${TARGET_FILE}"
  echo "ls -l ${HOME}/.ant/lib"
  ls -l ${HOME}/.ant/lib
}

build_xynautils_exceptions() {
  echo "building xynautils-exceptions..."
  cd $SCRIPT_DIR/../xynautils/exceptions
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp installMavenArtifact
}

build_xynautils_logging() {
  echo "building xynautils-logging..."
  cd $SCRIPT_DIR/../xynautils/logging
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp installMavenArtifact
}

build_xynautils_database() {
  echo "building xynautils-database..."
  cd $SCRIPT_DIR/../xynautils/database
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp installMavenArtifact
}

build_xynautils_snmp() {
  echo "building xynautils-snmp..."
  cd $SCRIPT_DIR/../xynautils/snmp
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp installMavenArtifact
}

build_xynautils_ldap() {
  echo "building xynautils-ldap..."
  cd $SCRIPT_DIR/../xynautils/ldap
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp installMavenArtifact
}

build_xynautils_misc() {
  echo "building xynautils-misc..."
  cd $SCRIPT_DIR/../xynautils/misc
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp installMavenArtifact
}

build_xynautils_xml() {
  echo "building xynautils-xml..."
  cd $SCRIPT_DIR/../xynautils/xml
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp installMavenArtifact
}

build_xynautils_soap() {
  echo "building xynautils-soap..."
  cd $SCRIPT_DIR/../xynautils/soap
  mkdir -p lib
  mvn dependency:resolve
  mvn -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp installMavenArtifact
}

build_misc() {
  echo "building misc..."
  cd $SCRIPT_DIR/../misc
  ant -Doracle.home=/tmp installMavenArtifact
}

build_cligenerator_jar() {
  echo "building cligenerator"
  cd $SCRIPT_DIR/../server
  mvn -f cligenerator.pom.xml dependency:resolve
  mvn -f cligenerator.pom.xml -DoutputDirectory="$(pwd)/lib" dependency:copy-dependencies
  ant -Doracle.home=/tmp installMavenArtifactCLIGenerator
}

build_xynafactory_jar() {
  echo "building xynafactory.jar..."
  cd $SCRIPT_DIR/../server
  ant -Doracle.home=/tmp -Dxyna.clusterprovider.OracleRACClusterProvider=false installMavenArtifactXynaFactory
}

build_oracle_aq_tools() {
  echo "build oracleAQ Tools"
  cd $SCRIPT_DIR/build
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
  mvn install:install-file -Dfile=../../common/lib/xyna/DefaultConnectionPoolTypes-1.0.1.jar -DpomFile=../../components/xnwh/pools/DefaultConnectionPoolTypes/pom.xml
}

build_persistencelayers() {
  echo "building persistencelayers..."
  cd $SCRIPT_DIR/build
  
  #build and install memory persistencelayer
  ant -Doracle.home=/tmp -f  ../../persistencelayers/XynaMemoryPersistenceLayer/build.xml installMavenArtifact
 
  #build and install install XynaJavaSerializationPersistenceLayer
  ant -Doracle.home=/tmp -f ../../persistencelayers/XynaJavaSerializationPersistenceLayer/build.xml installMavenArtifact
 
  # build all persistencelayers
  ant -Doracle.home=/tmp buildPersistenceLayers  
}

build_plugins() {
  echo "building plugins..."
  cd $SCRIPT_DIR/build
  ant -Doracle.home=/tmp buildPlugins
}

build_clusterproviders() {
  echo "building clusterproviders..."
  
  #oracle rac cluster provider
  cd $SCRIPT_DIR/../clusterproviders/OracleRACClusterProvider
  ant -Doracle.home=/tmp
  
  #xsor cluster provider
  cd $SCRIPT_DIR/../clusterproviders/XSORClusterProvider
  ant -Doracle.home=/tmp
}

build_networkavailability() {
  echo "building networkavailability..."
  
  #build and install demon
  cd $SCRIPT_DIR/../components/xact/demon
  ant -Doracle.home=/tmp installMavenArtifact

  #build networkavailability
  cd $SCRIPT_DIR/../components/xact/NetworkAvailability
  ant -Doracle.home=/tmp
}

compose_networkavailability() {
  cd $SCRIPT_DIR/../release
  mkdir -p components/xact/NetworkAvailability
  cd components/xact/NetworkAvailability
  cp -r $SCRIPT_DIR/../components/xact/NetworkAvailability/config .
  cp -r $SCRIPT_DIR/../components/xact/NetworkAvailability/lib .
  cp $SCRIPT_DIR/../components/xact/NetworkAvailability/*.sh .
  cp $SCRIPT_DIR/../components/xact/NetworkAvailability/log4j.properties .
}

build_prerequisites() {
  echo "building prerequisites..."
  cd $SCRIPT_DIR/../prerequisites/installation/delivery
  ant -f delivery.xml
}

build_modeller() { 
  if [[ -z ${GIT_BRANCH_XYNA_MODELLER} ]]; then
    RELEASE_NUMBER=$(cat ${SCRIPT_DIR}/delivery/delivery.properties | grep ^release.number | cut -d'=' -f2)
    # branch is RELEASE_NUMBER without the first 'v'
    GIT_BRANCH_XYNA_MODELLER=${RELEASE_NUMBER:1}
  fi
  echo "building Modeller GUI from branch ${GIT_BRANCH_XYNA_MODELLER}"
  cd $SCRIPT_DIR/build
  ant -f build-gui.xml -Dmodeller.branch=${GIT_BRANCH_XYNA_MODELLER}
}

build_xyna_factory() {
  echo "building artifact"
  cd $SCRIPT_DIR/..
  rm -rf release
  mkdir -p release
  
  compose_checkscripts
  compose_scripts
  compose_components
  compose_dhcpd
  compose_doc
  compose_etc
  compose_func_lib
  compose_templateMechanismStandalone
  compose_thirdparties
  compose_server
  compose_files
  compose_networkavailability
  setversion
  zip_xyna
  compose_prerequisites
  compose_modeller
  compose_connectors
  compose_readmefile
  zip_result
}


compose_connectors() {
  cd $SCRIPT_DIR
  mkdir -p $SCRIPT_DIR/../release/third_parties
  mvn -f db.connector.pom.xml dependency:resolve -DexcludeTransitive=true
  mvn -f db.connector.pom.xml -DoutputDirectory="${SCRIPT_DIR}/../release/third_parties" dependency:copy-dependencies -DexcludeTransitive=true
  mvn -f db.connector.pom.xml license:download-licenses -DlicensesOutputDirectory=${SCRIPT_DIR}/../release/third_parties -DlicensesOutputFile=${SCRIPT_DIR}/../release/third_parties/licenses.xml -DlicensesConfigFile=${SCRIPT_DIR}/db_connector_license_config.xml -DlicensesOutputFileEol=LF
  cp ${SCRIPT_DIR}/prepare_db_connector_jars.sh ${SCRIPT_DIR}/../release
}

compose_readmefile() {
  cd $SCRIPT_DIR
  cp ../blackedition/readme.txt ${SCRIPT_DIR}/../release
}


setversion() {
  VERSION=$(cat ${SCRIPT_DIR}/delivery/delivery.properties | grep ^release.number | cut -d'=' -f2) #e.g. v9.0.0.0
  DATE=$(date +"%Y%m%d_%H%M") #e.g. 20230530_1055
}


zip_xyna() {
  echo "zipping content of XynaFactory without Prerequisites"
  cd $SCRIPT_DIR/../release
  mkdir ../XynaFactory_${VERSION}_${DATE}
  mv * ../XynaFactory_${VERSION}_${DATE}
  mv ../XynaFactory_${VERSION}_${DATE} .
  zip -r XynaFactory_${VERSION}_${DATE}.zip .
  rm -r XynaFactory_${VERSION}_${DATE}
}


zip_result() {
  echo "creating "
  mv $SCRIPT_DIR/../release $SCRIPT_DIR/../XynaFactory_${VERSION}_${DATE}_bundle
  mkdir $SCRIPT_DIR/../release
  mv $SCRIPT_DIR/../XynaFactory_${VERSION}_${DATE}_bundle $SCRIPT_DIR/../release
  cd $SCRIPT_DIR/../release
  zip -r ../XynaFactory_${VERSION}_${DATE}_bundle.zip .
}


compose_thirdparties() {
  echo "downloading third party licenses"
  cd $SCRIPT_DIR/../release
  mkdir third_parties
  cd $SCRIPT_DIR/build
  # backup pom.xml
  cp pom.xml pom.xml-bak
  # comment "dependencyManagement"-tags
  sed -i s/\<dependencyManagement\>/\<\!--dependencyManagement--\>/g pom.xml
  sed -i s:\</dependencyManagement\>:\<\!--/dependencyManagement--\>:g pom.xml
  # delete unfree or erroneous dependencies
  sed -i '/<dependency>/{N;N;{/<artifactId>demonlib</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>DHCPClusterStateSharedLib</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>DHCPv6DBStorablesSharedLib</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>DHCPSharedLib</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>OraclePersistenceLayer</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>RemoteGenericODSAccess</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>SFTPTrigger</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>XynaContentStorables</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>XynaLocalMemoryPersistenceLayer</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>XynaXMLShellPersistenceLayer</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>com.ibm.mq.allclient</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>com.ibm.mq.traceControl</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>tools</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>fscontext</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>providerutil</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>javaee-api</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>jms</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>jradius-core</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>jradius-dictionary</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>jradius-extended</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>ecj</{N;N;d}}}' pom.xml
  sed -i '/<dependency>/{N;N;{/<artifactId>gnu-crypto</{N;N;d}}}' pom.xml
  # run license downloads (bom must have name "pom.xml")
  mvn license:download-licenses -DlicensesOutputDirectory=$SCRIPT_DIR/../release/third_parties -DlicensesOutputFile=$SCRIPT_DIR/../release/third_parties/licenses.xml
  # restore backup
  rm pom.xml
  mv pom.xml-bak pom.xml
  echo "license-download done"
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

compose_scripts() {
  cd $SCRIPT_DIR/../release
  cp -r ../installation/scripts .
}

compose_files() {
  cd $SCRIPT_DIR/../release
  cp ../blackedition/blackedition_lib.sh .
  cp ../blackedition/install_black_edition.sh .
  cp ../blackedition/uninstall_black_edition.sh .
}


compose_components() {
  cd $SCRIPT_DIR/../release
  mkdir components
  cd components
  cp -r ../../modules/deploy/* .
}


compose_server() {
  cd $SCRIPT_DIR/../release
  mkdir revisions
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

compose_server_files() {
  cd $SCRIPT_DIR/../release
  cp ../server/log4j2.xml ./server
  cp ../server/product_lib.sh ./server
  cp ../server/server.policy ./server
  cp ../server/deploy/TemplateImpl.zip ./server
  cp ../server/deploy/TemplateImplNew.zip ./server
  cp ../server/xynafactory.sh ./server
  cp ../server/Exceptions.xml ./server
}

buildTemplateImplNew() {
  echo "buildTemplateImplNew..."
  cd $SCRIPT_DIR/../server
  ant -Doracle.home=/tmp buildTemplateNew
}

#TODO: dhcp
compose_server_storage() {
  cd $SCRIPT_DIR/../release/server
  mkdir storage
  mkdir storage/BlackExceptionCodeManagement
  cp ../../installation/codegroup.xml storage/BlackExceptionCodeManagement
  cp ../../installation/codepattern.xml storage/BlackExceptionCodeManagement
  #TODO: dhcp -> optionsv4 and optionsv6

  mkdir storage/persistence
  cp $SCRIPT_DIR/../localbuild/server/storage/persistence/persistencelayers.xml storage/persistence
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
  mkdir -p $SCRIPT_DIR/../release/components/xnwh/xcs/XynaClusterPersistenceLayer
  cp $SCRIPT_DIR/../components/xnwh/xcs/XynaClusterPersistenceLayer/lib/xyna/XynaMemoryPersistenceLayer-1.0.0.jar $SCRIPT_DIR/../release/components/xnwh/xcs/XynaClusterPersistenceLayer
  cp $SCRIPT_DIR/../localbuild/components/xnwh/xcs/XynaClusterPersistenceLayer/XynaClusterPersistenceLayer.jar $SCRIPT_DIR/../release/components/xnwh/xcs/XynaClusterPersistenceLayer

  #xsor
  mkdir -p $SCRIPT_DIR/../release/components/xnwh/xcs/xsor
  cp $SCRIPT_DIR/../clusterproviders/XSORClusterProvider/lib/xsor.jar $SCRIPT_DIR/../release/components/xnwh/xcs/xsor
}

compose_server_orderinpoutsourcetypes() {
  cd $SCRIPT_DIR/../release/server
  rm -rf $SCRIPT_DIR/../localbuild/server/orderinputsourcetypes/*/xyna
  cp -r $SCRIPT_DIR/../localbuild/server/orderinputsourcetypes .
}

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

compose_server_clusterproviders(){
  cd $SCRIPT_DIR/../release/server
  mkdir -p clusterproviders
  
  mkdir -p clusterproviders/OracleRACClusterProvider
  cp $SCRIPT_DIR/../clusterproviders/OracleRACClusterProvider/deploy/* $SCRIPT_DIR/../release/server/clusterproviders/OracleRACClusterProvider
  cp $SCRIPT_DIR/../modules/xact/queue/oracleaq/sharedlib/OracleAQTools/deploy/OracleAQTools.jar $SCRIPT_DIR/../release/server/clusterproviders/OracleRACClusterProvider
  
  mkdir -p clusterproviders/XSORClusterProvider
  cp $SCRIPT_DIR/../clusterproviders/XSORClusterProvider/deploy/* $SCRIPT_DIR/../release/server/clusterproviders/XSORClusterProvider
}

compose_prerequisites() {
  cp $SCRIPT_DIR/../prerequisites/release/*.zip $SCRIPT_DIR/../release
}

compose_modeller() {
  mv $SCRIPT_DIR/../*.war $SCRIPT_DIR/../release
}


prepare_build() {
  mkdir -p /opt/common
  cd $SCRIPT_DIR/build
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
  build_xynautils_xml
  build_xynautils_soap
}

fill_lib() {
  echo "fill lib..."
  cd $SCRIPT_DIR/build/lib
  ant resolve
}

build_all() {
  build
  build_modules
  build_plugins
  build_clusterproviders
  build_networkavailability
  buildTemplateImplNew
  build_prerequisites
  build_modeller
  build_xyna_factory
}

build() {
  build_xynautils
  build_misc
  build_cligenerator_jar
  build_xynafactory_jar
  build_conpooltypes
  build_persistencelayers
  fill_lib
  build_oracle_aq_tools
}

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
. ${SCRIPT_DIR}/build.conf
GIT_BRANCH_XYNA_MODELLER=""

check_dependencies
prepare_build

case $1 in
  "xynautils")
    build_xynautils
    ;;
  "build")
    build
    ;;
  "all")
    OPTIND=2
    while getopts ":b:" options; do
      case "${options}" in 
        b)
          GIT_BRANCH_XYNA_MODELLER=${OPTARG}
          ;;
        *) # If unknown (any other) option:
          print_help
          exit 1
          ;;
      esac
    done
    check_dependencies_frontend
    build_all
    ;;
  "compose")
    build_xyna_factory
    ;;
  "plugins")
    build_plugins
    ;;
  "clusterproviders")
    build_clusterproviders
    ;;
  "conpooltypes")
    build_conpooltypes
    ;;
  "install_libs")
    install_libs
    ;;
  *)
    print_help
    exit 1
    ;;
esac

exit 0
