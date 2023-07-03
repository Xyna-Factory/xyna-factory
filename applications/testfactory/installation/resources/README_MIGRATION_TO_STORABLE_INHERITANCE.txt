XTF-Migration
=============

Beschrieben wird die Migration von einer XTF-Installation die noch keine StorableInheritance verwendet zu einer Version bei der dies der Fall ist.


1) Vor der Fabrik-Installation muss bereits umgestellt werden auf eine XynaTestFactoryInfrastructure-Application Version, die verhindert, dass alle TestDaten in eine Tabelle zusammenfallen. Dies geschieht in folgenden Schritten:
1.1) Import der benötigten Abhängigkeiten (Processing, Zeta) und Entfernung veralteten Abhängigkeiten (DOM-Inspector), zB.:

./xynafactory.sh importapplication ~/Lieferungen/Lieferung_20190513/factory/XynaBlackEdition_v8.1.0.20_20190417_1708/components/xprc/Processing.1.0.10.app

./xynafactory.sh migrateruntimecontext -fromApplicationName Processing -fromVersionName 1.0.8 -toApplicationName Processing -toVersionName 1.0.10 -f

./xynafactory.sh changeruntimecontextdependencies -ownerApplicationName XynaTestFactoryInfrastructure -ownerVersionName 8.1.7.3 -changes "r:DOM Inspector/1.0.1"

./xynafactory.sh importapplication ~/Lieferungen/Lieferung_20190513/factory/XynaBlackEdition_v8.1.0.20_20190417_1708/components/xmcp/ZetaFramework.0.6.4.app


- Import der aktuellen XynaTestFactoryInfrastructure aus dem XTF-Lieferegegenstand

./install_testfactory.sh -i 1 -d


1.2) Migration aller bisher präsententen XynaTestFactoryInfrastructure Versionen auf die neue Version.
     Falls auf dem System noch TestProjekte existieren, die gar keine XynaTestFactoryInfrastructure verwenden sondern die Infrastruktur noch als Kopie in ihrem Workspace verwenden, müssen diese auch umgestellt werden (hinzufügen der XynaTestFactoryInfrastructure-App zu ihren Dependencies und dann Kollisionen löschen).

./xynafactory.sh migrateruntimecontext -fromApplicationName XynaTestFactoryInfrastructure -fromVersionName 7.0.4.1 -toApplicationName XynaTestFactoryInfrastructure -toVersionName 8.2.0.5 -f

./xynafactory.sh migrateruntimecontext -fromApplicationName XynaTestFactoryInfrastructure -fromVersionName 7.0.4.2 -toApplicationName XynaTestFactoryInfrastructure -toVersionName 8.2.0.5 -f

./xynafactory.sh migrateruntimecontext -fromApplicationName XynaTestFactoryInfrastructure -fromVersionName 8.1.7.3 -toApplicationName XynaTestFactoryInfrastructure -toVersionName 8.2.0.5 -f


2) Installation der Fabrik, je nach grösse des Systems sollten die Timeout-Parameter des Inmstallations-Scriptes angepasst werden.
func_lib/processing/processing_lib.sh
  local SLEEP=${2:-22}vi 
  local MAX_RETRIES=${3:-18000}

./install_black_edition.sh -i 1 -v -x SSH,GuiHttp,Http,Mail,Queue,ActiveMQ,WebSphereMQ,CapacityMgmt,FileMgmt,Net,RegExp,XynaPropertyMgmt,UserSessionMgmt -c xynafactory,fractalmodeller -w blackedition -d tomcat -g xsd -p


3) Nach der Fabrik-Installation können die restlichen Bestandteile der XTF-Lieferung installiert werden.

./install_testfactory.sh  -i 1 -fo


4) Migration der TestObjects über folgendes Statement (für AV und RMK/AT Xyna mit New Features):

INSERT INTO testobjectmetadata (parentuid, unid, idx, testobjectid, typename)
  SELECT parentuid, unid, idx, id, "xdev.xtestfactory.infrastructure.storables.TestObjectMetaData" FROM testobject2;
  
