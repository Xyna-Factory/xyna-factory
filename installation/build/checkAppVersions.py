#!/usr/bin/python3

import xml.etree.ElementTree as ET
import io
import re
import glob
from xml.dom import minidom

application={}

def print_ver_huddel(appName,version_in_blackediton_lib):
   if (application[appName] != version_in_blackediton_lib):
      print ("Application "+appName+" has versionName "+application[appName]+" in application.xml, but versionName "+version_in_blackediton_lib+" in blackedition_lib.sh")
      return 1
   else:
      return 0

def check_blackedition_lib_sh():
   found_huddel=0
   with io.open("../../blackedition/blackedition_lib.sh", mode="r",encoding="utf8",errors='ignore') as f:
      for line in f:
         if re.search("^APPMGMTVERSION=",line):
            app_mgmt_ver=line.split("=")[1].strip()
         elif re.search("^GUIHTTPVERSION=",line):
            gui_http_ver=line.split("=")[1].strip()
         elif re.search("^SNMPSTATVERSION=",line):
            snmp_stat_ver=line.split("=")[1].strip()
         elif re.search("^PROCESSINGVERSION=",line):
            processing_ver=line.split("=")[1].strip()
         else:
            continue
   found_huddel+=print_ver_huddel("GlobalApplicationMgmt",app_mgmt_ver)
   found_huddel+=print_ver_huddel("GuiHttp",gui_http_ver)
   found_huddel+=print_ver_huddel("SNMPStatistics",snmp_stat_ver)
   found_huddel+=print_ver_huddel("Processing",processing_ver)

   return found_huddel 

      





for appl_xml in glob.glob("../../**/application.xml",recursive=True):
   # Alle Ordner test werden ignoriert
   if 'test' in appl_xml or 'XMOM' in appl_xml:
      continue;
   print (appl_xml)
   xmldoc = minidom.parse(appl_xml)
   appl=xmldoc.getElementsByTagName('Application')
   version = appl[0].attributes['versionName'].value
   appl_name = appl[0].attributes['applicationName'].value
   application[appl_name]=version

found_huddel=False
for appl_xml in glob.glob("../../**/application.xml",recursive=True):
   # Alle Ordner test werden ignoriert
   if 'test' in appl_xml:
      continue;
   root = ET.parse(appl_xml).getroot() 
   app_under_invenstigation=root.get('applicationName')
   for req in root.findall('ApplicationInfo/RuntimeContextRequirements/RuntimeContextRequirement'):
      app_name=req.find('ApplicationName').text
      ver_name=req.find('VersionName').text
      if app_name in application:
         if (application[app_name] != ver_name):
            print ('application '+app_under_invenstigation+" has a reference to application "+app_name+" which has the wrong versionName")
            print ('versionName Reference:'+ver_name)
            print ('versionName in Source:'+application[app_name])
            print ('file is '+appl_xml)
            found_huddel=True
      else:
         if app_name in ['XSD Datamodel Base', 'Service_Activation_and_Configuration_Data_Model', 'Service_Catalog_Management_Client']:
            # XSD Datamodel Base wird dynamisch vom Datenmodell erzeugt, da ist nichts eingecheckt
            # Service* Apps are generated during build from yamls
            pass
         else:
            print ('application '+app_under_invenstigation+" has a reference to application "+app_name+" that does not exist")
            print ('file is '+appl_xml)
            found_huddel=True

if (check_blackedition_lib_sh() > 0):
   found_huddel=True

if found_huddel:
   exit(99)
  
   
