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


BASEDIR=${1}
HEADER_FILE=${2}


#java files
for i in $(find ${BASEDIR} -name "*java"|xargs grep -iL Copyright); 
do 
	cat ${HEADER_FILE} $i > tmp.txt;
 	mv tmp.txt $i; 
done


#sql files
#sed -e "s+*+-+" ${HEADER_FILE} > ${HEADER_FILE}.tmp
#sed -e "s+/*+--+" ${HEADER_FILE}.tmp > ${HEADER_FILE}_2.tmp
#rm ${HEADER_FILE}.tmp
#sed -e "s+*/++" ${HEADER_FILE}_2.tmp > ${HEADER_FILE}_SQL
#rm ${HEADER_FILE}_2.tmp

#for i in $(find ${BASEDIR} -name "*sql" -type f|xargs grep -iL Copyright); 
#do 	
#	cat ${HEADER_FILE}_SQL $i > tmp.txt;
# 	mv tmp.txt $i; 
#done 

#rm ${HEADER_FILE}_SQL


#xsd and wsdl files
#sed -e "s+--+__+" ${HEADER_FILE} > ${HEADER_FILE}.tmp
#sed -e "s+/*+<!--+" ${HEADER_FILE}.tmp > ${HEADER_FILE}_2.tmp
#rm ${HEADER_FILE}.tmp
#sed -e "s+*/+-->+" ${HEADER_FILE}_2.tmp > ${HEADER_FILE}_XML
#rm ${HEADER_FILE}_2.tmp
#cat "<?xml version="1.0" encoding="UTF-8" ?>" >> ${HEADER_FILE}_XML;

#TODO: inculde wsdl files
#for i in $(find ${BASEDIR} -name "*xsd"|xargs grep -iL Copyright); 
#do 
#	less ${HEADER_FILE}_XML | sed "s+<?xml [.]*+&+"
#done
