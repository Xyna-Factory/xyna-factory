# -*- coding: utf-8 -*-
import subprocess
import re
import sys
import os
import traceback
import string
import xml.etree.ElementTree as ET
import StringIO

class TableConfigClearer:
  #returns list of tableNames
  def readExistingConfiguration(self):
    result = []
    root = ET.parse('/opt/xyna/xyna_001/server/storage/persistence/tableconfiguration.xml').getroot()
    print("root" + str(root))
    for table in root.findall("tableconfiguration/table"):
      result.append(table.text)
      #print("added: " + table.text)
    return result

    
  #returns list of valid tableNames (-> have xmomodsmappings)
  def determineValidTablesNames(self, allTableNames):
    arguments = ["/opt/xyna/xyna_001/server/xynafactory.sh", "listxmomodsnames"]
    p = subprocess.Popen(arguments, stdout = subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    (result, error) = p.communicate()
    
    tablesToKeep = ["advanceduser", \
    "codegroup", \
    "codepattern", \
    "cronlikeorders", \
    "idgeneration", \
    "listadditionalaliasrights", \
    "listroles", \
    "mailaccounts", \
    "orderarchive", \
    "orderbackup", \
    "orderinfo", \
    "testdata", \
    "testdatausageinfo", \
    "testdataselector", \
    "testdataselectorinstance", \
    "xynaproperties", \
    "triggers", \
    "triggerinstances", \
    "filters", \
    "filterinstances", \
    "triggerconfiguration", \
    "orderseriesmanagement", \
    "securestorage", \
    "ordercontextconfig", \
    "warehousejobs", \
    "capacities", \
    "version", \
    "orderinfo", \
    "topologies", \
    "topologies", \
    "pool", \
    "batchprocessarchive", \
    "bpcustomization", \
    "bprestartinformation", \
    "bpruntimeinformation", \
    "applicationstorable", \
    "timeseriesinfo", \
    "reflisttimeseriesinfo", \
    "plannedstoptime", \
    "lastupdatetime", \
    "listtimeseriesinfo", \
    "starttime", \
    "endtime", \
    "fqctrltaskinformation"]
    
    validTables = []
    
    for tableName in allTableNames:
      if tableName in result or tableName in tablesToKeep:
        validTables.append(tableName)
        print("validTable: " + tableName)
  
    return validTables


  def stopFactory(self):
    print("stopping factory")
    arguments = ["/opt/xyna/xyna_001/server/xynafactory.sh", "stop"]
    p = subprocess.Popen(arguments, stdout = subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    p.communicate()

    
  def startFactory(self):
    print("starting factory")
    arguments = ["/opt/xyna/xyna_001/server/xynafactory.sh", "-d", "4000", "start"]
    p = subprocess.Popen(arguments, stdout = subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    p.communicate()

    
  def removeXmlEntries(self, tablesToKeep):
    print("removeXmlEntries...")
    
    #create copy just in case
    arguments = ["cp", "/opt/xyna/xyna_001/server/storage/persistence/tableconfiguration.xml", "/opt/xyna/xyna_001/server/storage/persistence/tableconfiguration.xml.bak"]
    p = subprocess.Popen(arguments, stdout = subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    p.communicate()
    
    self.stopFactory()
    
    tree=ET.parse('/opt/xyna/xyna_001/server/storage/persistence/tableconfiguration.xml')
    root = tree.getroot()
    
    toRemove = []
    
    for element in root:
      if self.keepElement(element, tablesToKeep):
        continue
      toRemove.append(element)
    
    for element in toRemove:    
      root.remove(element)
    
    tree.write(open('/opt/xyna/xyna_001/server/storage/persistence/tableconfiguration.xml', 'w'), encoding='UTF-8')
    
    self.startFactory()
 

  def keepElement(self, element, tablesToKeep):
    for child in element:
      if child.tag == "table" and child.text in tablesToKeep:
        return True
    return False          
    
  def clear(self):
    existingConfigration = self.readExistingConfiguration()
    validTableNames = self.determineValidTablesNames(existingConfigration)
    self.removeXmlEntries(validTableNames)


### main ###
print("starting")
worker = TableConfigClearer()
worker.clear()
print("done")