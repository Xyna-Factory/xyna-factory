unterhalb dieses ordners liegen ordner f�r sequentielle updates von einer version zur n�chsten.
jeder ordner muss den namen haben, der in order.properties angegeben ist.
die updates werden in der reihenfolge ausgef�hrt, die durch order.properties vorgegeben ist.

die updates werden von update.xml unter ../install sequentiell aufgerufen.

in update.xml m�ssen an den stellen, wo in xml kommentaren ein ? steht, die korrekten versions-
nummern eingetragen werden.

damit das update funktioniert, m�ssen im projektspezifischen environment property file
folgende properties gesetzt sein:

#user/schema in the database
db.userid=processing
#password of the database user
db.password=processing


jeder update-order enth�lt mindestens folgende dateien:

1. version.properties

mit dem inhalt

#start version des updates
version.start=1.2.3.4
#end version
version.end=1.2.3.5


2. update.sql

mit dem inhalt

-----------------------------------------------------
-- Copyright 2023 Xyna GmbH, Germany
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--  http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-----------------------------------------------------


EXIT;


3. update.xml

mit dem inhalt

<?xml version="1.0" encoding="UTF-8"?>
<project name="update"
         default="updateAll"
         basedir="."
         xmlns:oracle="antlib:oracle">
         
  <!-- wird ausgefuehrt, falls db noch nicht up to date ist. beinhaltet 
       datenbank- und applicationserverseitiges update -->
  <target name="updateAll" > <!-- 1.2.3.4 to 1.2.3.5 -->
      <sqlplus user="${db.userid}"
                 password="${db.password}"
                 path="${sql.release.version.dir}"
                 file="update.sql"
                 parameter="${db.userid}"
                 connectstring="${db.connectstring}" />
      <antcall target="updateNoDB" inheritrefs="true" inheritall="true" />
  </target>
  
  <!-- wird ausgefuehrt, falls db schon up to date ist. beinhaltet nur
       applicationserverseitiges update -->
  <target name="updateNoDB" >
  </target>
         
</project>