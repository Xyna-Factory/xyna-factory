# -*- coding: utf-8 -*-

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
import json
import re
import sys
import os
import datetime
import traceback
import getpass

class Testgenerator:

  def readConfig(self, cfgFile):
    with open(cfgFile, "r") as jsonFile:
      config_json = json.load(jsonFile)
    if "ignoreList" in config_json:
      if not isinstance(config_json["ignoreList"], list):
        raise Exception("ignoreList in config was not a list!")
      self.ignoreList = config_json["ignoreList"]


    if "ignorePaths" in config_json:
      if not isinstance(config_json["ignorePaths"], list):
        raise Exception("ignorePaths in config was not a list!")
      self.ignorePaths = config_json["ignorePaths"]


    if "listIndexKeys" in config_json:
      if not isinstance(config_json["listIndexKeys"], list):
        raise Exception("listIndexKeys in config was not a list!")
      self.listIndexKeys = config_json["listIndexKeys"]

    if "listIndexKeyToReplaceValue" in config_json:
      if not isinstance(config_json["listIndexKeys"], list):
        raise Exception("listIndexKeyToReplaceValue in config was not a list!")
      self.listIndexKeyToReplaceValue = config_json["listIndexKeyToReplaceValue"]


    if "urlPrefix" in config_json:
      if not isinstance(config_json["urlPrefix"], str):
        raise Exception("listIndexKeys in config was not a string!")
      self.urlPrefix = config_json["urlPrefix"]

    now = datetime.datetime.now()

    if "meta" in config_json:
      self.meta = config_json["meta"]
      if isinstance(self.meta, dict):
        for key in self.meta:
          if self.meta[key] == "CURRENT_DATE":
            self.meta[key] = str(now.day) + "." + str(now.month) + "." + str(now.year)
          if self.meta[key] == "CURRENT_USER":
            try:
              self.meta[key] = getpass.getuser()
            except Exception:
              self.meta[key] = "tester"
              print("cound not determine username for meta element: " + key)

    if "createReads" in config_json:
      self.createReads = bool(config_json["createReads"])

    if "blacklistedStartOrders" in config_json:
      if not isinstance(config_json["blacklistedStartOrders"], list):
        raise Exception("blacklistedStartOrders in config was not a list!")
      self.blacklistedStartOrders = config_json["blacklistedStartOrders"]



  def __init__(self):

    #create fields + assign defaults
    self.ignoreList = []
    self.ignorePaths = []
    self.listIndexKeys = ["name", "id"] # sorted
    self.listIndexKeyToReplaceValue = ["id"]
    self.urlPrefix = ".*/XynaBlackEditionWebServices/io"
    self.meta = {}
    self.createReads = True
    self.blacklistedStartOrders = []
    self.configFilePath = "config.json"
    self.readConfig(self.configFilePath)
    # contains dictionaries from path to data for every request
    # basis of read operations
    self.knowledge = []

    #contains paths that we tried to create a read operation for
    self.alreadyReplacingPaths = []

     # operations
    self.result = []

    #key is variable name, value is value of variable
    #key DOES NOT INCLUDE "!"
    self.createdVariables = {}

    if self.createReads:
      #substitutionFunctions - url + payload strings will be replaced by these
      self.substitutionFunctions = [ self.substituteSingleFromVariables, self.tryToCreateReadForPart, self.createMultiSetForPart ]
    else:
      self.substitutionFunctions = [] # do not replace anything
    self.nextReadId = 0


  def createCallOperation(self, url, method, payload, response):
    result = {}
    result["operation"] = "call"
    result["requestType"] = method
    result["url"] = url
    result["callId"] = len(self.knowledge) # so we know where to insert reads. will be removed before creating output
    if not (payload == None):
      result["payload"] = payload

    if "status" in response and response["status"] != 200:
      result["acceptError"] = True

    if "factoryIndex" in response:
      result["factoryIndex"] = response["factoryIndex"]

    self.result.append(result)


  #do not include requests that are on ignoreList
  def includeRequest(self, request):
    url = request["url"]
    oldUrl = url
    url = self.removePrefixFromUrl(url)

    # prefix did not match
    if oldUrl == url:
      return False

    for ignoreItem in self.ignoreList:
      exp = ignoreItem
      if re.search(exp, url):
        return False

    return True


  #checks if there is another list element of type dict with el[key] = value -> returns true if there is only one
  def isUnique(self, key, value, list):
    foundOnce = False
    for listEntry in list:
      if not isinstance(listEntry, dict):
        continue

      if key in listEntry and listEntry[key] == value:
        if not foundOnce:
          foundOnce = True
          continue
        else:
          return False
    return True


  #creates an index for the index^th element in list.
  #if possible, it uses a top level element of list[index] with a key of name
  def createIndex(self, list, index, avoid = ""):
    listEntry = list[index]

    if isinstance(listEntry, dict):
      for listIndexKey in self.listIndexKeys:
        if listIndexKey == avoid:
          continue
        if listIndexKey in listEntry:
          if self.isUnique(listIndexKey, listEntry[listIndexKey],list):
            valueReplaced = self.replaceFromVariable(listEntry[listIndexKey])
            if isinstance(valueReplaced, int) or isinstance(valueReplaced, dict):
              valueReplaced = str(valueReplaced)
            return listIndexKey + "=" + valueReplaced

    #return number.
    return index


  #removes occurencies of =value] from path
  def removeSelfFromPath(self, value, path, parent, gparent):
    for listIndexKey in self.listIndexKeys:
      valueEsc = re.escape(value)
      listIndexKeyEsc = re.escape(listIndexKey)
      exp = "\["+ listIndexKeyEsc + "=" + valueEsc + "]/" +  listIndexKeyEsc+ "$"
      if re.search(exp, path):
        #we have to change path.
        oldIndex = gparent.index(parent)
        newIndex = self.createIndex(gparent, oldIndex, listIndexKey)
        path = path[0: len(path) - len(listIndexKey + "=" + value + "]/" + listIndexKey)] #rmv old
        path = path + str(newIndex) + "]/" + listIndexKey

    return path

  def addResponseObjToKnowledge(self, newEntry, path, obj, parent, gparent):
    if newEntry:
      self.knowledge.append({})

    pathPrefix = "" if len(path) == 0 else "/"

    if isinstance(obj, dict):
      for key in obj:
        self.addResponseObjToKnowledge(False, path + pathPrefix + key, obj[key], obj, parent)
    elif isinstance(obj, list):
      for i in range(0,len(obj)):
        index = self.createIndex(obj, i)
        if isinstance(index, int):
          index = str(index)
        self.addResponseObjToKnowledge(False, path + "[" + index + "]", obj[i], obj, parent)
    else:
      #actually add knowledge
      ob = obj
      try:
        ob = str(obj)
      except Exception:
        ob = obj.encode('utf-8')
        ob = str(ob)
        ob = ob.decode('unicode-escape')
      path = self.removeSelfFromPath(ob, path, parent, gparent)
      self.knowledge[len(self.knowledge)-1][path] = ob


  def addKnowledge(self, response):
    if "content" not in response or "text" not in response["content"]:
      return

    try:
      obj = json.JSONDecoder().decode(response["content"]["text"])
      self.addResponseObjToKnowledge(True, "", obj, None, None)
    except(ValueError):
      pass #result was not json

  def escapeAgain(self, str):
    org = str
    str = str.replace( "\\", "\\\\\\")
    str = str.replace("\\", "")
    str = str.replace("\'", "\\\'")
    str = str.replace("\"", "\\\"")
    str = str.replace("\n", "\\\n")
    str = str.replace("\r", "\\\r")
    str = str.replace("\t", "\\\t")

    return str


  #result does not contain !
  # adds new name to createdVariables
  # reduces length of varname to 48 - before adding suffix
  def createVarName(self, part):
    name = part
    name = name.replace(" ", "_")
    name = name.replace("!", "x")
    name = name.replace("\\", "x")
    name = name.replace("\"", "x")
    name = name.replace("\n", "")

    #constant values tend to become long
    if len(name) > 48:
      name = name[:48]

    prepName = name

    i = 0
    while name in self.createdVariables:
      name = prepName + str(i)
      i=i+1

    self.createdVariables[name] = ""

    return name


  def substituteSingleFromVariables(self, part):
    if len(part) == 0:
        return part

    partReplaced = part

    #try direct read
    for key in self.createdVariables:
      value = self.createdVariables[key]
      if value == part:
        partReplaced = "!" + key + "!"
        return partReplaced


    #try modification read. Separate loop do make sure we catch a direct read if possible
    for key in self.createdVariables:
      value = self.createdVariables[key]
      if value.endswith("." + part):
        #create modification to get part from value
        oldVarName = "!" + key + "!"
        shortenedValue = value[value.rindex(".") + 1: len(value)]
        newVarName = self.createVarName(shortenedValue)
        index = len(self.result) #add at end
        self.createSubstringModification(index, ".", oldVarName, newVarName, None, "after")
        self.createdVariables[newVarName] = shortenedValue
        partReplaced = "!" + newVarName + "!"
        return partReplaced

    return part


  # returns lowest index of self.knowledge with an entry where value == part
  # or -1 if there is none
  def getResponseIndexFor(self, part):
    for i in range(0, len(self.knowledge)):
      knowledge = self.knowledge[i]
      for key in knowledge:
        if knowledge[key] == part:
          return i
    return -1


  # returns path from self.knowledge[responseIndex] where the value is part
  # does not return paths that contain part as list index.
  def getPathOfValue(self, part, responseIndex):
    knowledge = self.knowledge[responseIndex]
    for key in knowledge:
      if "="+part+"]" in key:
        continue
      if knowledge[key] == part:
        return key

    raise Exception("not found: " + part + " in " + str(responseIndex))


  # find index of call operation with callId == callIndex
  def findIndexAfterCall(self, callIndex):
    result = -1
    for i in range(len(self.result)):
      if "callId" in self.result[i]:
        if self.result[i]["callId"] == callIndex:
          #continue to push result further (until we find a call operation with callId > callIndex
          result =  i +1
          continue
        if self.result[i]["callId"] > callIndex:
          return i # last position before next call
      if result != -1:
        result = i


    if result != -1:
      return result

    raise Exception("can't place read for callIndex: " + str(callIndex))



  def lateReplaceListIndex(self, path):
    exp = "(\[(.*?)=([^!]*?)\])"
    matches = re.findall(exp, path)
    if matches == None:
      return path

    for (match, key, value) in matches:
      all = match
      toReplace = value
      if key in self.listIndexKeyToReplaceValue:
        replaced = self.applySubstitutions(toReplace, self.substitutionFunctions)
        path = re.sub("="+value, "="+replaced, path, 1)

    return path


  def alreadyReplacing(self, path):
    result =  path in self.alreadyReplacingPaths
    self.alreadyReplacingPaths.append(path)
    return result


  def createSetOperation(self, variable, value):
    setOp = {}
    setOp["operation"] = "set"
    setOp["variable"] = "!" + variable + "!"
    setOp["value"] = value
    self.result.insert(1, setOp) #at the beginning, after initial multiSet


  def createReadOperation(self, indexInKnowledge, variable, path):

    if self.alreadyReplacing(path):
      value = self.knowledge[indexInKnowledge][path]
      self.createSetOperation(variable, value)
      return

    readId = self.nextReadId
    self.nextReadId = self.nextReadId + 1
    path = self.lateReplaceListIndex(path)
    indexInResult = self.findIndexAfterCall(indexInKnowledge)

    readOp = {}
    readOp["operation"] = "read"
    readOp["pathInResponse"] = path
    readOp["targetVariable"] = "!" + variable + "!"
    readOp["readId"] = readId
    readOp["callIdRef"] = indexInKnowledge
    if isinstance(self.createdVariables[variable], str):
      readOp["unquoteResult"] = True

    self.result.insert(indexInResult, readOp)


  #escapes variable (with !), but not value
  def createSubstringModification(self, index, divider, value, variable, direction, keep, readId = -1):
    result = {}
    result["operation"] = "modification"
    result["targetVariable"] = "!" + variable + "!"
    result["variable"] = value
    result["modification"] = {}
    result["modification"]["operation"] = "substring"
    result["modification"]["divider"] = divider
    result["modification"]["keep"] = keep

    if direction != None:
      result["modification"]["direction"] = direction

    result["readId"] = readId

    self.result.insert(index, result)


  def findResultIndexOfRead(self, readId):
    for i in range(len(self.result)):
      operation = self.result[i]
      if "readId" not in operation:
        continue
      if operation["readId"] == readId:
        return i
    print("could not find readId: " + str(readId))


  #searches previous knowledge for part.* and *.part
  #if nothing is found, returns part
  #if something is found, returns !<variable>!
  #  and creates read and mofification operations.
  def tryToCreateReadModifyForPart(self, part):
    if len(part) == 0:
      return part

    partEsc = re.escape(part)

    exp1 = "^" + partEsc + "\.[^\.]*$"
    exp2 = ".*\." + partEsc + "$"

    for i in range(0, len(self.knowledge)):
      knowledge = self.knowledge[i]
      for key in knowledge:
        value = knowledge[key]
        if re.search(exp1, value):
          #create read after i^th call
          varName = self.createVarName(value)
          readId = self.nextReadId
          self.createReadOperation(i, varName, key)
          responseIndex = self.findResultIndexOfRead(readId)
          #create modification after that read
          responseIndex = responseIndex + 1 # after read
          shortenedValue = value[0 : value.rindex(".")]
          modifiedVarName = self.createVarName(shortenedValue)
          self.createSubstringModification(responseIndex, ".", "!"+ varName +"!", modifiedVarName, "inversed", "before", readId)

          self.createdVariables[varName] = value
          self.createdVariables[modifiedVarName] = shortenedValue
          part = "!" + modifiedVarName + "!"
          return part
        elif re.search(exp2, value):
          #create read after i^th call
          varName = self.createVarName(value)
          readId = self.nextReadId
          self.createReadOperation(i, varName, key)
          responseIndex = self.findResultIndexOfRead(readId)
          #create modification after that read
          responseIndex = responseIndex + 1 # after read
          shortenedValue = value[value.rindex(".")+1: len(value)]
          modifiedVarName = self.createVarName(shortenedValue)
          self.createSubstringModification(responseIndex, ".", "!"+ varName +"!", modifiedVarName, "inversed", "after", readId)

          self.createdVariables[varName] = value
          self.createdVariables[modifiedVarName] = shortenedValue
          part = "!" + modifiedVarName + "!"
          return part

    return part


  def tryToCreateReadForPart(self, part):
    if len(part) == 0:
      return part

    responseIndex = self.getResponseIndexFor(part)
    if responseIndex == -1:
      partReplaced = self.tryToCreateReadModifyForPart(part)
    else:
      path = self.getPathOfValue(part, responseIndex)
      variable = self.createVarName(part)
      self.createReadOperation(responseIndex, variable, path)
      partReplaced = "!" + variable + "!"
      self.createdVariables[variable] = part
    return partReplaced


  def addToMultiSet(self, varName, part):
    multiSet = None
    if len(self.result) == 0 or "callId" in self.result[0]:
      #create MultiSet
      multiSet = {}
      multiSet["operation"] = "multiSet"
      multiSet["data"] = []
      self.result.insert(0, multiSet)
    else:
      multiSet = self.result[0]

    data = {}
    value = part
    value = self.escapeAgain(part)

    data["!" + varName + "!"] = value
    multiSet["data"].append(data)


  def createMultiSetForPart(self, part):
    partReplaced = part
    if len(part) == 0:
      pass
    #do not update placeholders
    elif part.startswith("!") and part.endswith("!"):
      pass
    #ignores
    elif part in self.ignorePaths:
      pass
    else:
      # this part has to be set
      varName = self.createVarName(part)
      self.createdVariables[varName] = part
      self.addToMultiSet(varName, part)
      partReplaced = "!" + varName + "!"
    return partReplaced


  def replaceFromVariable(self, toReplace):
    for entry in self.createdVariables:
      if self.createdVariables[entry] == toReplace:
        return "!" + entry + "!"
    return toReplace


  #TODO: should not have the assigning twice!
  #replaces strings/numbers/boolean in given object, if there is a variable holding that value.
  def replaceInJson(self, obj):
    if isinstance(obj, list):
      if(obj == []):
        return obj
      for i in range(0, len(obj)):
        entry = obj[i]
        if isinstance(entry, (str, int, float, complex)):
          newValue = self.applySubstitutions(entry, self.substitutionFunctions)
          obj[i] = newValue
        elif isinstance(entry, dict) or isinstance(entry, list):
          obj[i] = self.replaceInJson(entry)
    elif isinstance(obj, dict):
      for key in obj:
        entry = obj[key]
        if isinstance(entry, (str, int, float, complex)):
          newValue = self.applySubstitutions(entry, self.substitutionFunctions)
          obj[key] = newValue
        elif isinstance(entry, dict) or isinstance(entry, list):
          obj[key] = self.replaceInJson(entry)

    return obj


  #applies functions to part until part changes
  def applySubstitutions(self, part, functions):
    #only work on strings atm
    if not isinstance(part, str):
      return part
    if len(part) == 0:
      return part
    if part in self.ignorePaths:
      return part

    newPart = part
    for function in functions:
      newPart = function(part)
      if newPart != part:
        break
    return newPart


  def replaceInUrl(self, url):
    parts = url.split("/")
    url = ""

    for part in parts:
      parameters = []
      if "?" in part:
        all = part.split("?")
        realPart = all[0]
        parameters = all[1:len(all)]
        part = realPart
      partReplaced = self.applySubstitutions(part, self.substitutionFunctions)
      if len(parameters) > 0:
        params = "?"
        for par in parameters:
          params = params + par + "?"
        params = params[0: len(params)-1] #remove last "?"
        partReplaced = partReplaced + params
      url = url + partReplaced + "/"

    url = url[0: len(url)-1] # remove last "/"
    return url


  #removes prefix from url, or returns url if prefix not in it
  def removePrefixFromUrl(self, url):
    m = re.search(self.urlPrefix, url)
    if not m:
      return url
    url = url[len(m.group(0)): len(url)]
    return url


  def createResultEntries(self, request, response):
    url = request["url"]
    url = self.removePrefixFromUrl(url)
    method = request["method"]
    payload = None

    url = self.replaceInUrl(url )


    if "postData" in request and "text" in request["postData"]:
      payload = request["postData"]["text"]
      try:
        payload = json.JSONDecoder().decode(payload)
      except(ValueError):
        print("--------------------")
        print("Invalid JSON in Request:")
        print(str(payload))
        print("--------------------")
        payload = ""
      payload = self.replaceInJson(payload)


    # create call operation and add to result
    self.createCallOperation(url, method, payload, response)
    dontAddToknowledge = payload is not None and ("orderType" in payload) and (payload["orderType"] not in self.blacklistedStartOrders)
    if not dontAddToknowledge:
      self.addKnowledge(response)


  #remove callId and readId from result
  def cleanupResult(self):
    for operation in self.result["operations"]:
      if "callId" in operation:
        del operation["callId"]
      if "callIdRef" in operation:
        del operation["callIdRef"]
      if "readId" in operation:
        del operation["readId"]


  def createTest(self, input):
    entries = input["log"]["entries"]
    for entry in entries:
      if "request" not in entry:
        continue
      if "response" not in entry:
        continue
      request = entry["request"]
      if not self.includeRequest(request):
        continue
      response = entry["response"]

      #create result entries
      self.createResultEntries(request, response)

      self.alreadyReplacingPaths = [] #clear

    #create result
    self.createResult()


    #write knowledge
    if len(self.knowledge) > 0:
      self.writeknowledge()

    result =  json.dumps(self.result, indent=2, sort_keys=True)
    #result = result.decode('unicode-escape')

    return result


  def writeknowledge(self):
    with open("lastknowledge.txt", "w") as f:
      knowledge = self.knowledge[len(self.knowledge)-1]
      for key in knowledge:
        f.write(key)
        f.write(": ")
        f.write(knowledge[key])
        f.write("\n")

    with open("completeknowledge.txt", "w") as f:
      i = 0
      for knowledge in self.knowledge:
        f.write("===== " + str(i) + " =====\n")
        for key in knowledge:
          f.write(key + " :")
          f.write(knowledge[key])
          f.write("\n")
        i = i + 1


  def createDataflowAssertOperation(self):
    assertOp = {}
    assertOp["operation"] = "assert"
    assertOp["constraint"] = "isInList"
    assertOp["pathToList"] = "connections/$list"
    assertOp["entries"] = []
    return assertOp


  def findDataflowEntry(self, entries):
    cpy = list(entries)
    cpy.reverse()

    for entry in cpy:
      if "url" not in entry["request"]:
        continue
      if entry["request"]["url"].endswith("/dataflow"):
        return entry

    raise Exception("no dataflow call found.")


  #entry is dataflow connection object
  def addIsInListEntry(self, entry):
    type = entry["type"]
    targetId = entry["targetId"]
    targetId = self.applySubstitutions(targetId, self.substitutionFunctions) #TODO: problem: id can only be resolved by checking fqn and fqn can only be resolved checking id

    result = {}
    result["targetId"] = targetId
    result["type"] = type

    sourceId = None
    if "sourceId" in entry:
      sourceId = entry["sourceId"]
      sourceId = self.applySubstitutions(sourceId, self.substitutionFunctions)
      result["sourceId"] = sourceId

    assertOp = self.result[len(self.result)-1]
    assertOp["entries"].append(result)


  def fillDataflowAssert(self, response):
    obj = json.JSONDecoder().decode(response["content"]["text"])
    connectionList = obj["connections"]["$list"]

    for entry in connectionList:
      self.addIsInListEntry(entry)


  def createAssertListLengthOperation(self, response):
    obj = json.JSONDecoder().decode(response["content"]["text"])
    connectionList = obj["connections"]["$list"]

    operation = {}
    operation["operation"] = "assert"
    operation["constraint"] = "listLength"
    operation["expectedValue"] = len(connectionList)
    operation["path"] = "connections/$list"

    return operation


  def createDataflowTest(self, input):
    entries = input["log"]["entries"]
    for entry in entries:
      if "request" not in entry:
        continue
      if "response" not in entry:
        continue
      request = entry["request"]
      if not self.includeRequest(request):
        continue
      response = entry["response"]

      #create result entries
      self.createResultEntries(request, response)


    #create empty assert operation
    op = self.createDataflowAssertOperation()
    self.result.append(op)

    #find (last) dataflow entry
    dataflowEntry = self.findDataflowEntry(entries)
    response = dataflowEntry["response"]
    self.fillDataflowAssert(response)

    #create assert list length operation
    op = self.createAssertListLengthOperation(response)
    self.result.append(op)


    #create result
    self.createResult()

    json.dumps(self.result, indent=2, sort_keys=True)
    result = result.decode('unicode-escape')

    return result



  def createResult(self):
    allResultEntries = self.result
    self.result = {}
    self.result["operations"] = allResultEntries

    self.cleanupResult()
    self.result["meta"] = self.meta


def loadHarFiles(firstHarFileIndex, argv, numberOfHarFiles):
  result = []
  for i in range(firstHarFileIndex, firstHarFileIndex + (numberOfHarFiles * 2), 2):
    harFilePath = argv[i]
    try:
      harfile = open(harFilePath)
      harfile_json = json.loads(harfile.read())
      result.append(harfile_json)
    except Exception as err:
      print(str(err))
      sys.exit(1)
  return result


def readFactoryIndices(firstHarFileIndex, argv, numberOfHarFiles):
  result = []
  for i in range(firstHarFileIndex+1, firstHarFileIndex + (numberOfHarFiles * 2), 2):
    result.append(int(argv[i]))
  return result


def annotateFactoryIndexInHarFile(harfile, factoryIndex):
    entries = harfile["log"]["entries"]
    for entry in entries:
      if "request" not in entry:
        continue
      if "response" not in entry:
        continue
      response = entry["response"]
      response["factoryIndex"] = factoryIndex


def mergeHarFiles(harfiles):
  merged = harfiles[0]

  for i in range(1, len(harfiles)):
    merged["log"]["entries"].extend(harfiles[i]["log"]["entries"])

  return merged


# entries: [{u'serverIPAddress': u'[::1]', u'startedDateTime': u'2021-07-30T12:25:38.581Z', u'_initiator': {u'type': u'other'}, u'cache': {}, u'request': {u
#, reverse=True) for sorted
def sortHarFile(harFile):
  entries = harFile["log"]["entries"]
  sortedEntries = sorted(entries, key=lambda k: transformTime(k['startedDateTime']))
  harFile["log"]["entries"] = sortedEntries


def transformTime(timeString):
  [formattedTimeString, offsetDatetime] = formatTime(timeString)
  baseDatetime = datetime.datetime.strptime(formattedTimeString, "%Y-%m-%dT%H:%M:%S.%fZ")

  finalDatetime = baseDatetime - offsetDatetime
  result = int(float(finalDatetime.strftime('%s.%f'))*1000)

  return result


def formatTime(timeString):
  baseTime = timeString
  offset = datetime.timedelta(hours=0, minutes=0)
  indexOfZ = timeString.index("Z") if "Z" in timeString else -1

  if indexOfZ == -1: # timeString does contain offset
    offset = timeString[len(timeString)-5:-1]
    hours = int(offset[0:2])
    minutes = int(offset[3:5])
    offset = datetime.timedelta(hours=hours, minutes=minutes)
    baseTime=timeString[0:-6] + "Z"

  return [baseTime, offset]


# open all har files
# add factory index information
# sort requests by start time - "startedDateTime": "2021-09-14T06:14:51.798Z" on request/response level
# create merged har file using sorted requests
def combineHarFiles(firstHarFileIndex, argv, numberOfHarFiles):
  harFiles = loadHarFiles(firstHarFileIndex, argv, numberOfHarFiles)
  factoryIndices = readFactoryIndices(firstHarFileIndex, argv, numberOfHarFiles)

  for i in range(0, len(harFiles)):
    annotateFactoryIndexInHarFile(harFiles[i], factoryIndices[i])

  mergedHarFile = mergeHarFiles(harFiles)
  sortHarFile(mergedHarFile)

  return mergedHarFile


def createCombinedHarFile(firstHarFileIndex, argv):
  numberOfHarFiles = (len(argv) - firstHarFileIndex) / 2
  if numberOfHarFiles <= 1:
    try:
      harfile = open(harFilePath)
      harfile_json = json.loads(harfile.read())
    except Exception as err:
      print(str(err))
      sys.exit(1)
    return harfile_json
  else:
    return combineHarFiles(firstHarFileIndex, argv, numberOfHarFiles)



### main ###
if len(sys.argv) < 2:
  print("Usage: python testgenerator.py [-dataflow] <inputFile.har> [factoryIndex, <inputFile.har>. factoryIndex...]")
  sys.exit(1)

firstHarFileIndex = 1
harFilePath = sys.argv[1]
createDataflowTest = False

if sys.argv[1] == "-dataflow":
  firstHarFileIndex = 2
  harFilePath = sys.argv[2]
  createDataflowTest = True
  print("creating dataflow test")


harfile_json = createCombinedHarFile(firstHarFileIndex, sys.argv)

returncode = 0
testgenerator = Testgenerator()
try:
  if createDataflowTest:
    result = testgenerator.createDataflowTest(harfile_json)
  else:
    result = testgenerator.createTest(harfile_json)
  filename = harFilePath[:-4]
  with open(filename + ".json", "w") as f:
    f.write(result)
except Exception as exception:
  returncode = 1
  print("Exception: " + str(exception))
  traceback.print_exc(file=sys.stdout)


sys.exit(returncode)