# -*- coding: utf-8 -*-
import json
import sys
import os
import xml.etree.ElementTree as ET

class Xmomdiff:

  def getXMLFiles(self, basepath):
    filelist = []
    for root, dirs, files in os.walk(basepath):
      for file in files:
        if file.endswith(".xml"):
          filename = os.path.join(root, file)
          filelist.append(filename)
    return filelist


  def fillDatatype(self, root):
    result = {}
    nsp_map = {"nsp": "http://www.gip.com/xyna/xdev/xfractmod"}
    result["TypePath"] = root.get('TypePath')
    result["TypeName"] = root.get('TypeName')
    result["BaseTypePath"] = root.get('BaseTypePath')
    result["BaseTypeName"] = root.get('BaseTypeName')
    result["Label"] = root.get('Label')
    
    datalist = self.fillData(root)
    result["Data"] = datalist  
    servicelist = self.fillServices(root, nsp_map)
    result["Services"] = servicelist
    
    return result


  def fillServices(self, root, nsp_map):
    result = []
    
    service = self.findChild(root, "Service")
    if service == None:
      return result
      
    for operation in service.findall("./*"):
      if not operation.tag.endswith("Operation"):
        continue
        
      singleResult = {}
      singleResult["Name"] = operation.get("Name")
      input = self.findChild(operation, "Input")
      if input == None:
        singleResult["input"] = []
      else:
        singleResult["input"] = self.fillData(input, False)
      output = self.findChild(operation, "Output")
      if output == None:
        singleResult["output"] = []
      else:
        singleResult["output"] = self.fillData(output, False)
      throws = self.findChild(operation, "Throws")
      if throws == None:
        singleResult["throws"] = []
      else:
        singleResult["throws"] = self.fillData(throws, False, "Exception")
      result.append(singleResult)
    
    return result


  #using .findall(nsp:<Tag>, nsp_map)
  #does not work for datatypes after it was called for an exception
  def fillData(self, root, includeVarName = True, tag = "Data"):
    datalist = []

    for data in root.findall("./*"):
      if not data.tag.endswith(tag):
        continue
      dataobj = {}
      if includeVarName:
        dataobj["VariableName"] = data.get('VariableName')
      if (data.get('IsList') is not None):
        dataobj["IsList"] = data.get('IsList')
      else:
        dataobj["IsList"] = "false"
  
      if data.get('ReferencePath') is not None:
        dataobj["ReferencePath"] = data.get('ReferencePath')
        dataobj["ReferenceName"] = data.get('ReferenceName')
      else:
        meta = self.findChild(data, "Meta")
        if meta is not None:
          typenode = self.findChild(meta, "Type")
          if typenode is not None:
            dataobj['Type'] = typenode.text
      datalist.append(dataobj)
    return datalist


  def findChild(self, root, childTag):
    for child in root.findall("./*"):
      if child.tag.endswith(childTag):
        return child      
    return None

  def fillException(self, root):
    result = {}
    nsp_map = {"nsp": "http://www.gip.com/xyna/3.0/utils/message/storage/1.1"}
    
    root = root.find('nsp:ExceptionType', nsp_map)
    
    if root == None:
      return None
    
    result["TypePath"] = root.get('TypePath')
    result["TypeName"] = root.get('TypeName')
    result["BaseTypePath"] = root.get('BaseTypePath')
    result["BaseTypeName"] = root.get('BaseTypeName')
    result["Label"] = root.get('Label')
    datalist = self.fillData(root)
    result["Data"] = datalist
    
    return result
    

  # returns a datatype, or None, if path does not lead to a datatype
  # datatype= map of name, supertype, isAbstract, list of members
  # member = map of name, type, isList
  def fillDT(self, path):
    result = None
    tree = ET.parse(path)
    root = tree.getroot()
    
    #not a datatype (like application.xml
    if len(root.tag.split('}', 1)) == 1:
      return None
    
    rootname = root.tag.split('}', 1)[1]
    if rootname != 'DataType' and rootname != 'ExceptionStore':
      return result
    
    if rootname == 'DataType':
      return self.fillDatatype(root)
    
    if rootname == 'ExceptionStore':
      return self.fillException(root)

    
    return result


  # path points to <appName>
  # list of datatypes
  def getDTs(self, path, pathPrefix):
    xmlFiles = self.getXMLFiles(path)
    result = []
    for file in xmlFiles:
      dt = self.fillDT(file)
      if dt is not None:
        fileWoPrefix = file[len(pathPrefix):]
        fileWoPrefix = fileWoPrefix.lower()
        dt["path"] = fileWoPrefix
        result.append(dt)

    return result
    
    
  def calcPathToReleasedApp(self, pathInRelease, pathToApp):
    nameOfApp = pathToApp[pathToApp.rindex("/")+1:] # remove Path
    for root, dirs, files in os.walk(pathInRelease):
      for dir in dirs:
        if nameOfApp.lower() == dir.lower(): 
          return pathInRelease + dir
    

  #returns entry in appDTs that matches typePath and typeName.
  #returns None, if no match is found => entire dt was removed
  def findAppDT(self, appDTs, path):   
    for appDT in appDTs:
      if appDT["path"] == path:
        return appDT
    return None


  def compareDTHeads(self, dtFQN, releaseDT, appDT):
    changes = []
    
    if releaseDT["BaseTypePath"] != appDT["BaseTypePath"] or releaseDT["BaseTypeName"] != appDT["BaseTypeName"]:
       oldParentFQN = str(releaseDT["BaseTypePath"]) + "." + str(releaseDT["BaseTypeName"])
       newParentFQN = str(appDT["BaseTypePath"]) + "." + str(appDT["BaseTypeName"])
       changes.append(dtFQN + " parent changed from " + oldParentFQN + " to " + newParentFQN)
    return changes
    

  #returns candidate where member[tag] == candidate[tag]
  #or None, if not found
  #example for None: this member was not part of the XMOM item
  def findMatchingTagValue(self, member, candidates, tag):
    for candidate in candidates:
      if tag not in candidate:
        print("This thing does not have this tag" + tag+ "!")
        print(json.dumps(candidate))
        continue
        
      if candidate[tag] == member[tag]:
        return candidate
    return None


  def determineType(self, member):
    if "Type" in member:
      return member["Type"]
    if "ReferencePath" in member:
      return member["ReferencePath"] + "." + member["ReferenceName"]   
    
  
  
  def compareDTData(self, dtFQN, releaseDT, appDT):
    changes = []
    allMembers = []
    
    allMembers.extend(releaseDT["Data"])
    allMembers.extend(appDT["Data"])
    
    for member in allMembers:
      oldMember = self.findMatchingTagValue(member, releaseDT["Data"], "VariableName")
      newMember = self.findMatchingTagValue(member, appDT["Data"], "VariableName")
      
      if oldMember == None:
        changes.append(dtFQN + " got a new member: " + member["VariableName"])
        continue
        
      if newMember == None:
        changes.append(dtFQN + " lost a member " + member["VariableName"])
        continue
        
      if oldMember == newMember:
        continue
      
      #changed members are in the list twice - this removes the duplicate
      if member == oldMember:
        continue    
      
      change = dtFQN + " changed Member: " + member["VariableName"] + " "
      oldType = self.determineType(oldMember)
      newType = self.determineType(newMember)
      if oldType != newType:
        change = change + "type changed from " + oldType + " to " + newType + " "
    
      if oldMember["IsList"] != newMember["IsList"]:
        change = change + "isList changed from " + oldMember["IsList"] + " to " + newMember["IsList"] + " "
      
      changes.append(change)
    
    return changes


  
  def compareDTServices(self, dtFQN, releaseDT, appDT):
    #both are exceptions
    if "Services" not in releaseDT and "Services" not in appDT:
      return []
      
    #was a datatype, is now an exceptiontype
    if "Services" not in releaseDT:
      return []
      
    #was an exceptiontype, is now a datatype
    if "Services" not in releaseDT:
      return []
  
    if len(releaseDT["Services"]) == 0 and len(appDT["Services"]) == 0:
      return []
      
      
    result = []
      
    allServices = []
    allServices.extend(releaseDT["Services"])
    allServices.extend(appDT["Services"])
    
    for service in allServices:
      oldService = self.findMatchingTagValue(service, releaseDT["Services"], "Name")
      newService = self.findMatchingTagValue(service, appDT["Services"], "Name")
      
      if oldService == None:
        result.append(dtFQN + " got a new service: " + service["Name"])
        continue
        
      if newService == None:
        result.append(dtFQN + " lost a service: " + service["Name"])
        continue
        
      if oldService == newService:
        continue
        
      #changed services are in the list twice - this removes the duplicate
      if service == oldService:
        continue
        
      change = dtFQN + " changed signature of service " + service["Name"] + ". "
      if oldService["input"] != newService["input"]:
        oldInput = list(map(lambda x: self.getType(x), oldService["input"]))
        newInput = list(map(lambda x: self.getType(x), newService["input"]))
        change = change + "Input: " + str(oldInput) + " to " + str(newInput) + " "
      if oldService["output"] != newService["output"]:
        oldOutput = list(map(lambda x: self.getType(x), oldService["output"]))
        newOutput = list(map(lambda x: self.getType(x), newService["output"]))
        change = change + "Output: " + str(oldOutput) + " to " + str(newOutput) + " "
      if oldService["throws"] != newService["throws"]:
        oldThrow = list(map(lambda x: self.getType(x), oldService["throws"]))
        newThrow = list(map(lambda x: self.getType(x), newService["throws"]))
        change = change + "Throws: " + str(oldThrow) + " to " + str(newThrow) + " "
      result.append(change)

    return result


  # obj either contains ReferencePath and ReferenceName, or Type
  def getType(self, obj):
    if "Type" in obj:
      return obj["Type"]
    else:
      return obj["ReferencePath"] + "." + obj["ReferenceName"]
      

  # actually compares two datatypes.
  # returns list of changes
  def calcDTDiff(self, dtFQN, releaseDT, appDT):
    changes = []
    changes.extend(self.compareDTHeads(dtFQN, releaseDT, appDT))
    changes.extend(self.compareDTData(dtFQN, releaseDT, appDT))
    changes.extend(self.compareDTServices(dtFQN, releaseDT, appDT))
    
    #if len(changes) == 0:
    #  print(dtFQN + " did not change")
    return changes

  
  # list of differences
  # difference = DTName, what changed => is now abstract, member changed
  def calcDiffOfDTs(self, releaseDTs, appDTs):
    result = []
    for releaseDT in releaseDTs:
      appDT = self.findAppDT(appDTs, releaseDT["path"])
      dtFQN = releaseDT["TypePath"] + "." + releaseDT["TypeName"]
      if appDT == None:
        result.append(dtFQN + " was removed.")
        continue
      
      changesThisDT = self.calcDTDiff(dtFQN, releaseDT, appDT)
      result.extend(changesThisDT)      
  
    return result


  def calcAppDiff(self, pathInRelease, pathToApp):
    pathToReleasedApp = self.calcPathToReleasedApp(pathInRelease, pathToApp)

    #print("calcAppDiff - " + pathInRelease + ", " + pathToApp + ", " + pathToReleasedApp)
    
    result = {}
    result["Application"] = pathToReleasedApp #[len(pathInRelease):]
    
    appDTs = self.getDTs(pathToApp, pathToApp[:pathToApp.rindex("/")+1])
    releaseDTs = self.getDTs(pathToReleasedApp, pathInRelease)
    differences = self.calcDiffOfDTs(releaseDTs, appDTs)
    result["differences"] = differences
    
    return result


class CombineDiffs:

  # appNameWithPath does not include pathToApps.
  # returns pathToRelease/<pathToApp>
  def findPathInRelease(self, pathToRelease, appNameWithPath):
    appName = appNameWithPath[appNameWithPath.rindex("/")+1:]
    appPath = appNameWithPath[1:appNameWithPath.rindex("/")]
    if "/" in appPath:  # prune to last
      appPath = appPath[appPath.rindex("/")+1:] 

    for root, dirs, files in os.walk(pathToRelease):
      for dir in dirs:
        if appName.lower() == dir.lower():
          #check if we are at an application -> otherwise
          #we return /base instead of /base/base
          files = os.listdir(os.path.join(root, dir))
          for file in files:
            if "application.xml" in file:
              #check if we are indeed in the correct directory
              #should be ensured by appName, but ssh and radius
              #store their files under <...>/server/
              parent = root[root.rindex("/")+1:]
              if parent == appPath:
                return root + "/"
          
    print("did not find a folder called " + appNameWithPath + " in " + pathToRelease)


  #returns a list of paths to apps.
  def findApps(self, pathToApps):
    result = []
    for root, dirs, files in os.walk(pathToApps):
      if root.startswith("."):
        continue
      for candidate in dirs:
        if candidate.startswith("."): #ignore hidden directories
          continue
        files = os.listdir(os.path.join(root, candidate))
        for file in files:
          if "application.xml" in file:
            result.append(os.path.join(root, candidate))
    return result


  # pathToRelease -> like ./release
  # pathToApps -> like ./currentApps
  def execute(self, pathToRelease, pathToApps, resultFile):
    apps = self.findApps(pathToApps)
    completeResult = []
    for appNameWithPath in apps:
      appName = appNameWithPath[appNameWithPath.rindex("/")+1:]
      pathInRelease = self.findPathInRelease(pathToRelease, appNameWithPath[len(pathToApps):])
      
      if pathInRelease == None:
        print("skipping " + appName + ". Not in release")
        continue
      
      xmomdiff = Xmomdiff()
      result = xmomdiff.calcAppDiff(pathInRelease, appNameWithPath)
      completeResult.append(result)
    f = open(resultFile, "w")
    print(str(json.dumps(completeResult, indent=2, sort_keys=True)))
    f.write(json.dumps(completeResult, indent=2, sort_keys=True))
    f.close()





if len(sys.argv) <= 2:
  print("execute: ", sys.argv[0], " SINGLE or ALL for help.")
  sys.exit(1)

if sys.argv[1] == "SINGLE":
  if len(sys.argv) <= 3:
    print("Usage: ", sys.argv[0], " SINGLE <path of app in release> <path to app> ")
    sys.exit(1)

  xmomdiff = Xmomdiff()
  result = xmomdiff.calcAppDiff(sys.argv[2], sys.argv[3])
  result = json.dumps(result, indent=2, sort_keys=True)
  print(result)

if sys.argv[1] == "ALL":
  if len(sys.argv) <= 4:
    print("Usage: ", sys.argv[0], "ALL <path to release> <path to apps> <resultFile>")
    exit(1)

  combineDiffs = CombineDiffs()
  combineDiffs.execute(sys.argv[2], sys.argv[3], sys.argv[4])