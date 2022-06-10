# -*- coding: utf-8 -*-
import subprocess
import re
import sys
import os
import traceback
import string
import xml.etree.ElementTree as ET
import StringIO

class StatisticsClearer:
  
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
    
    
  def removeXmlEntries(self):
    print("removeXmlEntries...")
    
    #create copy just in case
    arguments = ["cp", "/opt/xyna/xyna_001/server/storage/defaultHISTORY/orderstatistics.xml", "/opt/xyna/xyna_001/server/storage/defaultHISTORY/orderstatistics.xml.bak"]
    p = subprocess.Popen(arguments, stdout = subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    p.communicate()
    
    self.stopFactory()
    
    tree=ET.parse('/opt/xyna/xyna_001/server/storage/defaultHISTORY/orderstatistics.xml')
    root = tree.getroot()
    transaction = root.attrib["transaction"]
    root.clear()
    root.attrib["transaction"] = transaction
    tree.write(open('/opt/xyna/xyna_001/server/storage/defaultHISTORY/orderstatistics.xml', 'w'), encoding='UTF-8')
    
    
    self.startFactory()
    
    
  def clear(self):
    self.removeXmlEntries()
    
    
### main ###
print("starting")
worker = StatisticsClearer()
worker.clear()
print("done")