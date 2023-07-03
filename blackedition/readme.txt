vgl auch https://wiki.gip.com/wiki/Xyna_Black_Edition:Xyna_Black_Edition/Entwickler_Doku

Xyna Factory Server / Xyna Net Factory / 3.x / Black Edition
===========================================================

* Dokumentation findet man unter
  ~/data/Xyna/Xyna Black Edition/3.x

* Einrichten des Java-Workspace
  1. Entwicklungsverzeichnis ist im folgenden blackedition
  1. Verzeichnis blackedition/installation muss parallel zu blackedition/server ausgecheckt sein
  2. ant -f server/build.xml genCodeClass genCliClass (Erstellung generierter Klassen)

* Einrichten des Flex-Workspace (Eclipse)
  1. Unter Window -> Preferences -> Flex -> Installed Flex SDKs das SDK aus
     /opt/common/flex_sdk auswählen und als default auswählen.
  2. Unter Window -> Preferences -> General -> Workspace -> Linked Resources
     eine Path Variable mit Namen BLACKEDITION anlegen, der als Wert der lokle Pfad zu den ausgecheckten
     Daten zugewiesen wird, z.B. ~/devel/xyna/blackedition

* Xyna Server lokal starten
  1. Verzeichnis /etc/xyna/environment/ muss für den Entwickler schreibbar sein
     (su - ;  mkdir -p /etc/xyna/environment/ ;  chown -R <xynauser> /etc/xyna )
  2. Aus einer Lieferung (im Verzeichnis ~/data/Xyna/Xyna\ Black\ Edition/3.x/06_Releases/ )
     die Verzeichnisse MDM, server/storage, server/persistencelayers und server/resources
     ins Entwicklungs-Verzeichnis blackedition kopieren
  3. blackedition/func_lib.sh nach blackedition/server/ kopieren
  4. In xynafactory.sh sind nach jedem Kopieren zwei Anpassungen vorzunehmen:
     FACTORY_CLI_PORT="TOKEN_FACTORY_CLI_PORT" -> FACTORY_CLI_PORT="4242"
     INSTANCE_NUMBER="TOKEN_INSTANCE_NUMBER"   -> INSTANCE_NUMBER="1"
  5. für logging-Ausgaben blackedition/server/product_lib.sh anpassen:
     Zeile       "nohup ${FACTORY_CLI_CMD} .... 2>&1 | ${VOLATILE_LOGGER} -p local0.debug &"
     anpassen zu "nohup ${FACTORY_CLI_CMD} .... 2>&1 > server.log &"
  6. In /etc/xyna/environment/black_edition_001.properties den Entwickler als berechtigten
     User eintragen: xyna.user=<username>
  7. server kompilieren mit eclipse und ant -f server/build.xml build
  8. ./xynafactory.sh start

* GUI+Tomcat lokal starten
  1. Tomcat installieren:
     a) Aus dem Lieferungsverzeichnis (im Verzeichnis ~/data/Xyna/Xyna\ Black\ Edition/3.x/06_Releases/ )
        aktuelles XynaBlackEditionPrerequisites_<....>.zip entpacken
     b) Darin findet sich XynaBlackEditionPrerequisites_<...>/application/tomcat/apache-tomcat-log4j.tgz, dieses entpacken
     c) Tomcat mit apache-tomcat-6.0.20/bin/startup.sh starten
  2. Die Datei /etc/xyna/environment/${HOSTNAME}.properties ist anzupassen: tomcat.folder, installation.folder
  3. Aus einer Lieferung folgende WARs deployen durch Kopieren ins apache-tomcat-6.0.20/webapps-Verzeichnis:
     server/webservices/XynaBlackEditionWebServices.war, server/webservices/XynaTopologyModeller.war, server/xfracmod/FractalModeller.war
  4. Editieren von apache-tomcat-6.0.20/webapps/FractalModeller/FractalModelling.properties:
     eigene Ip eintragen, auf http und Port 8080 umstellen




* Server mit lokal installierter GUI+tomcat
  1. Eine Lieferung erstellen oder aus dem Lieferungsverzeichnis besorgen
  2. Lieferung entpacken
  3. Unterverzeichnis tomcat anlegen, darin tomcat-tar auspacken, tomcat starten.
  4. Die Datei /etc/xyna/environment/${HOSTNAME}.properties ist anzupassen: tomcat.folder, installation.folder
     Falls das Verzeichnis /etc/xyna/environment nicht existert, dann als root-User einmalig anlegen und dem User mit dem man die Installation vornimmt (im folgenden xynauser genannt) zuordnen:
       su -
       mkdir -p /etc/xyna/environment/
       chown -R xynauser /etc/xyna
  5. Falls gewünscht: in server/xynaserver.sh Logging aktivieren, indem man in server/xynaserver.sh "> /dev/null" durch "> server.log" oder ">> server.log" ersetzt.
  6. server/mysql.properties anpassen
  7. install_black_edition.sh ausführen. Unerwünschte Komponenten lassen sich durch Kommandozeilenparameter unterdrücken. (Hint: install_black_edition.sh -h)
  8. tomcat auf https umkonfigurieren. danach tomcat neu starten
  9. server/xynaserver.sh stop - dann im devel-verzeichnis den xynaserver starten.


* Build-Skripte starten
  1. Für die Kompilierung von Flex mit ant müssen dem Skript VM Argumente mitgegeben werden:
        export ANT_HOME=/opt/common/apache-ant-1.8.3
        export LANG=de_DE.utf8
        export PATH=/opt/jdk11/bin:$PATH
        export JAVA_HOME=/opt/jdk11
        export ANT_OPTS="-ms1024M -mx1024M"
        export NODE_OPTIONS=--max_old_space_size=2048
    a. In Eclipse können die Argumente über die Run Configuration im Tab JRE angegeben werden. Alternativ kann das Argument auch direkt an das JRE angehängt werden. Dazu unter Window -> Preferences -> Java -> Installed JREs die aktuelle JRE editieren und Default VM Argument angeben.
    b. Bei der Verwendung der Shell erst den Befehl export ANT_OPTS="-XX:MaxPermSize=1024M -ms1024M -mx1024M" eingeben, dann ant ausführen.

Anmerkung: Bei 32bit JVMs darf XX:MaxPermSize nicht so hoch gewählt werden, da sonst die Java Virtual Machine nicht gestartet werden kann.
Also z.B. -XX:MaxPermSize=256M

* System Voraussetzungen (Installation)
  1. Sun Java 1.6.0





Merge in einen Branch
#####################

Welche Branches gibt es?
========================
svn ls svn://svn/branches/ | grep XYNA_BLACK

Auschecken des Branches
=======================
cd devel/blackBranches

BRANCH=6.1.2.9;

#Hauptverzeichnis flach auschecken
mkdir ${BRANCH}; svn --depth=files co svn://svn/branches/XYNA_BLACK_v${BRANCH}_branch/xyna/blackedition ${BRANCH}

#benötigte Unterverzeichnisse vollständig
cd ${BRANCH}; svn up --depth=infinity server ; cd ..
cd ${BRANCH}; svn up --depth=infinity XynaBlackEditionWebServices ; cd ..
cd ${BRANCH}; svn up --depth=infinity installation/ ; cd .. #damit "cd server; ant buildJar" möglich wird

Mergen
======

svn merge -c176003 svn://svn/trunk/xyna/blackedition/
svn ci -m "Merge aus Trunk rev 176003: <Ursprünglicher Kommentar>" .

neue Branch-Version liefern
===========================

#neue Version eintragen
VERSION=6.1.2.41
kwrite server/src/com/gip/xyna/update/Updater.java
svn ci -m "neue version ${VERSION}" server/src/com/gip/xyna/update/Updater.java


Neuen Branch erstellen
======================
Identifiziere Revision X im Source-Zweig (z.b. Trunk), die die aktuellste Revision im Branch sein soll und Versionsname des Branches (anhand der letzten gelieferten Versionsnummer). Dann führe aus:
svn cp svn://svn/trunk@<X> svn://svn/branches/XYNA_BLACK_v<VERSION>_branch/
z.b.
svn cp svn://svn/trunk@193292 svn://svn/branches/XYNA_BLACK_v7.0.2.27_branch/





Bau einer Lieferung
###################
ein lokaler build is möglicherweise aufgrund von fehlender npm/node Einrichtung oder Platzbedarf nicht auf dem eigenem Entwicklungsrechner möglich, login4 ist für einen delivery build eingerichtet worden.
vgl auch https://wiki.gip.com/wiki/Xyna_Black_Edition:Xyna_Black_Edition/Entwickler_Doku/Lieferungserstellung

cd blackedition
cd installation/delivery/
vi delivery.properties # neue Version eintragen
svn ci -m "neue version <Version>" delivery.properties
#Checke SVN Historie auf Änderungen in Modulen seit letzter Lieferung
#Erstmal manuell: svn log -v svn://svn/trunk/xyna/blackedition/modules | less
#Passe application.xmls an, wo es Änderungen gab (typischerweise sollten diese Änderungen abwärtskompatibel sein, also in der 3ten Stelle Version anpassen)
#Abhängigkeiten anderer application.xmls anpassen

#Umgebungsvariablen setzen
export ANT_HOME=/opt/common/apache-ant-1.8.3
export LANG=de_DE.utf8
export PATH=/opt/jdk11/bin:$PATH
export JAVA_HOME=/opt/jdk11
export ANT_OPTS="-ms1024M -mx1024M"
export NODE_OPTIONS=--max_old_space_size=4096

#Release bauen
ant -f delivery.xml release

#Lieferungsverzeichnis anlegen 
BRANCH=6.1.2.7
VERSION=6.1.2.41
mkdir ~/data/Xyna/Xyna\ Black\ Edition/3.x/06_Releases/Beta/v${BRANCH}/Hotfixes/v${VERSION}
mv /tmp/XynaBlackEdition_v${VERSION}_* ~/data/Xyna/Xyna\ Black\ Edition/3.x/06_Releases/Beta/v${BRANCH}/Hotfixes/v${VERSION}


#ReleaseNotes Erstellung
vgl auch https://wiki.gip.com/wiki/Xyna_Black_Edition:Xyna_Black_Edition/Entwickler_Doku/Releasenotes

cd ~/data/Xyna/Xyna\ Black\ Edition/3.x/06_Releases/Beta/v${BRANCH}/Hotfixes/v${VERSION}
vi ReleaseNotes${VERSION}.txt
Der Text für die Releasenotes kann per Workflow aus Bugzilla-Daten erstellt werden:
Workflow 
  xdoc.bugzilla.wf.GenerateReleasenotesXynaSingleVersion
oder für größere Versionsintervalle mit
  xdoc.bugzilla.wf.GenerateReleasenotesXynaBetweenTwoVersions
auf https://10.0.10.106:8443/FractalModeller/
Credentials sind: release/release

Damit da ordentliche Texte drin stehen, sollten alle (betroffenen) Bugs in Bugzilla folgende Eigenschaften haben:
- Target Milestone = <Release-Version>
- Version = <Version, seit der der Bug existiert>
- Ein Kommentar sollte folgendes Format haben, damit die Releasenotes extrahiert werden können. Falls kein solcher Kommentar gefunden wird, wird die Bug Summary für die Releasenotes benutzt.
<Irgendein Text, der für die Releasenotes nicht relevant ist>
#Releasenotes
<Releasenotes Text, wie er später im generierten Dokument auftauchen soll>

Beispiel:
Bug 23382

