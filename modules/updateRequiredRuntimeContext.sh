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

#Beispielaufruf ./updateRequiredRuntimeContext.sh Base 1.0.6 

REQUIRED_APP=$1
NEW_VERSION=$2

APPLIST=$(find . -name application.xml | xargs grep "ApplicationName>${REQUIRED_APP}" -l )

#echo ${APPLIST}

for app in ${APPLIST} ; do 
  head -n 15 $app | awk '
  function getAttribute(attr) { 
    for( i=1; i<NF; ++i ) { 
      if( index($i,attr) > 0 ) { 
        return substr($i,length(attr)+3,length($i)-length(attr)-3)
      }
    }
  }  
  index($0,"<Application ") > 0 { 
    print getAttribute("applicationName"), getAttribute("versionName") 
  }
  '
done;


for app in ${APPLIST} ; do 
  #echo $app
  awk -vreqapp=${REQUIRED_APP} -vnewver=${NEW_VERSION} '
   foundVersion == 0 { print $0 }  #Ausgeben aller Zeilen bis auf den Versionsstring
   index($0, "<RuntimeContextRequirements>")>0 { rcr=1 } #Weitere Suche auf Block einschraenken
   index($0, "</RuntimeContextRequirements>")>0 { rcr=0 }
   rcr==1 && index($0,"<ApplicationName") >0 { #ApplicationName pruefen
       line=$0; 
       gsub("ApplicationName", "", line); 
       gsub("[<>/ \r]", "", line);
       if( line==reqapp ) { 
         foundVersion = 1;
       }
     }
   rcr==1 && foundVersion == 1 && index($0,"<VersionName") >0 { #Version ersetzen
       print "        <VersionName>"newver"</VersionName>";
       foundVersion=0;
     }
  ' $app > dummy.xml
  mv dummy.xml $app
done;

echo "SVN Checkin durch: "
echo "svn ci -m \"Neue RequiredRuntimeContext-Version ${REQUIRED_APP} ${NEW_VERSION}\"" ${APPLIST}
