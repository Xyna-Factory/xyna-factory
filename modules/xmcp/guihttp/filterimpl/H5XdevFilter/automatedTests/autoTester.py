# -*- coding: utf-8 -*-

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2024 Xyna GmbH, Germany
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
import subprocess
import re
import datetime
import sys
import os
import traceback
import urllib.parse
import string
import random
import xml.etree.ElementTree as ET
from io import BytesIO
import time
import threading

#constants
baseURLToFormat = "http{0}://{1}:{2}{3}{4}"  #{0} is s or ''; {1} is ip; {2} is port; {3} is url prefix specified in apache. May be left blank when directly connecting to trigger. If set, starts with '/'; {4} is remainder of url. starts with '/'
keyOperation = "operation"
keyOperationCall = "call"
keyOperationRead = "read"
keyOperationMod = "modification"
keyOperationInvoke = "invoke"
keyOperationPrint = "print"
keyOperationSet = "set"
keyOperationAssert = "assert"
keyOperationSetRandom = "setRandom"
keyOperationIterate = "iterate"
keyOperationSetFromVariable = "setFromVariable"
keyOperationSubtestcase = "subtestcase"
keyOperationSuccess = "success"
keyOperationMultiset = "multiSet"
keyOperationSelectFromXml = "selectFromXml"
keyOperationToJson  = "convertToJson"
keyOperationUpload = "upload"
keyOperationCurrentTime = "getTime"
keyOperationAdd = "add"
keyOperationWait = "wait"
keyOperationGetUsername = "getUsername"
keyOperationGetValueFromTag = "getValueFromTag"
keyOperationJoinAsyncCall="joinAsyncCall"
keyOperationMergeList="mergeLists"


class SuccessException(Exception):
  pass

class FailedException(Exception):
  pass

class OperationException(Exception):
  innerException = None

class MaxRetriesExccededException(Exception):
  pass

class NoValidFactoryConfigException(Exception):
  pass

class Factory:
  ip = ""
  port = -1
  prefix = ""
  https = False
  usename = ""
  password = ""
  cookieFile = ""
  tags = {}

class RequestTester:

  port = "8080"
  seedPrefix = ""
  debug = False

  factoryConstraintValidatorFunctions = {}
  testHasConstraints = False


  def readConfig_json(self, cfgFile):
    with open(cfgFile) as configAsJson:
      cfg = json.loads(configAsJson.read())

    if "debug" in cfg and bool(cfg["debug"]):
      self.debug = True

    if "functions" not in cfg:
      print("functions required.")
    self.functions = pathOfScript + "/" + cfg['functions']

    if "factories" not in cfg:
      print("factories required.")
      sys.exit(3)

    if len(cfg["factories"]) == 0:
      print("at least one factory required")
      sys.exit(3)

    self.factories = []
    for factory in cfg["factories"]:
      f = Factory()
      f.username = factory["username"]
      f.password = factory["password"]
      f.ip = factory["ip"]
      f.port = factory["port"] if "port" in factory else 8080
      f.https = bool(factory["https"]) if "https" in factory else False
      f.prefix = factory["prefix"]
      f.cookieFile = self.cookieFile = pathOfScript + "/" + factory["cookieFile"]
      f.tags = factory["tags"] if "tags" in factory else {}
      self.factories.append(f)


  def loadFunctionFile(self, path, file):
    if self.debug:
      print("loading function file: " + path + file)
    with open(path + file, "r") as jsonFile:
      try:
        completeJson = json.load(jsonFile)
      except Exception as e:
        print("Error loading function file '{0}{1}': {2}".format(path, file, e))
        raise e

    for entry in completeJson:
      if entry["type"] == "additionalFunctionFile":
        newPath = entry["value"][0: entry["value"].rfind('/')+1]
        functionFile = entry["value"][len(newPath):]
        self.loadFunctionFile(path + newPath, functionFile)
      if entry["type"] == "function":
        if "functionName" not in entry:
          raise Exception("function: " + path + file + " does not define required field 'functionName'")
        if entry["functionName"] in self.functions:
          raise Exception("trying to register functionName twice: " + entry["functionName"])
        self.functions[entry["functionName"]] = entry


  def __init__(self, cfgFile):
    self.readConfig_json(cfgFile)
    path = self.functions[0: self.functions.rfind('/')+1]
    functionFile = self.functions[len(path):]
    self.functions = {}
    self.loadFunctionFile(path, functionFile)
    if self.debug:
        print("functions loaded")
    sys.stdout.flush()

    self.successes = 0
    self.fails = 0
    self.failedList = []
    self.lastResponse = ""
    self.currentTest = ""
    self.seedPrefix = datetime.datetime.now()
    self.factoryIndexMap = {} # key: factoryIndex, value: index of factory in config file
    self.factoryConstraintValidatorFunctions = self.initFactoryConstraintValidatorFunctions()
    self.testHasConstraints = False
    self.threads = {}#key: callId, value Thread object
    self.lock = threading.RLock() # acquire, release
    self.threadResults = {} #key: callId, value result


  def initFactoryConstraintValidatorFunctions(self):
    result = {}

    result["differentIP"] = self.validateConstraintDifferentIP
    result["sameIP"] = self.validateConstraintSameIP
    result["tag"] = self.validateConstraintTag

    return result


  def checkFactoryIndices(self, candidateMap, constraint):
    if "factoryIndices" not in constraint:
      raise Exception("invalid sameIP constraint. Missing factoryIndices. " + str(constraint))

    factoryIndices = constraint["factoryIndices"]
    factoryIndices = self.translateFactoryIndices(factoryIndices, len(candidateMap))

    highestIndex = max(factoryIndices)
    if len(candidateMap) <= highestIndex:
      raise Exception("Invalid factory Constraint: Constraint involves factory with index '" + str(highestIndex) + "' but test uses just '" + str(len(candidateMap)) + "' factories.")


  def validateValuesInDict(self, dict, values, usecase):
    for value in values:
      if value not in dict:
        raise Exception("Missing: " + str(value) + " for: " + str(usecase))

  def validateConstraintTag(self, candidateMap, constraint):
    self.validateValuesInDict(constraint, ["factoryIndex", "tag"], "Tag Constraint")
    factoryIndex = constraint["factoryIndex"] #which factory is this constraint about?
    expectedTag = constraint["tag"] #what tag should the factory have?
    expectedValue = constraint["value"] if "value" in constraint else None
    factoryIndexInConfig = candidateMap[factoryIndex]
    factory = self.factories[factoryIndexInConfig]

    if isinstance(factory.tags, dict):
      if expectedTag not in factory.tags:
        return False
      if expectedValue == None:
        return True
      else:
        return factory.tags[expectedTag] == expectedValue
    else:
      return False


  def validateConstraintSameIP(self, candidateMap, constraint):
    self.checkFactoryIndices(candidateMap, constraint)

    factoryIndices = constraint["factoryIndices"]
    factoryIndices = self.translateFactoryIndices(factoryIndices, len(candidateMap))
    factoryIndexInConfig = candidateMap[0]
    expectedIP = self.factories[factoryIndexInConfig].ip

    for factoryIndex in factoryIndices:
      factoryIndexInConfig = candidateMap[factoryIndex]
      ip = self.factories[factoryIndexInConfig].ip
      if ip != expectedIP:
        return False
    return True


  #factoryIndices are either a list of numbers, or "all"
  #if "all" is passed, it returns a list counting to length
  def translateFactoryIndices(self, factoryIndices, length):
    # instead of passing a list, there are other ways to identify factoryIndices
    if not isinstance(factoryIndices, list):
      if factoryIndices == "all":
        factoryIndices = range(0, length, 1)
    return factoryIndices


  def validateConstraintDifferentIP(self, candidateMap, constraint):
    self.checkFactoryIndices(candidateMap, constraint)

    seenIPs = []
    factoryIndices = constraint["factoryIndices"]
    factoryIndices = self.translateFactoryIndices(factoryIndices, len(candidateMap))


    for factoryIndex in factoryIndices:
      factoryIndexInConfig = candidateMap[factoryIndex]
      ip = self.factories[factoryIndexInConfig].ip
      if ip in seenIPs:
        return False
      seenIPs.append(ip)
    return True



  def formatUrl(self, factoryIndexTranslated):
    s = "s" if self.factories[factoryIndexTranslated].https else ""
    ip = self.factories[factoryIndexTranslated].ip
    port = self.factories[factoryIndexTranslated].port
    prefix = self.factories[factoryIndexTranslated].prefix
    return baseURLToFormat.format(s, ip, port, prefix, self.urlExtension);


  def createSubprocessArguments(self, requestType, rdyUrl, rdyPayload, factoryIndexTranslated, writeCookies):
    result = ['curl', '-k', '-X', requestType, rdyUrl]

    if requestType == "POST" or requestType == "PUT":
      result.append('-d')
      result.append(rdyPayload)

    self.addCookiesToArguments(result, factoryIndexTranslated, writeCookies)

    return result


  def printCookies(self, factoryIndexTranslated):
    cookieFile = self.factories[factoryIndexTranslated].cookieFile
    print("  cookie file: " + str(cookieFile))
    with open(cookieFile, "r") as cookieFi:
      for line in cookieFi:
        print("  >> " + line)


  #factory index is index as defined by test. Not translated using self.factoryIndexMap!
  def executeRequest(self, urlExtension, requestType, payload, factoryIndex, writeCookies):
    factoryIndexTranslated = self.factoryIndexMap[factoryIndex]
    self.urlExtension = urlExtension
    self.payload = payload
    rdyUrl = self.formatUrl(factoryIndexTranslated)
    arguments = self.createSubprocessArguments(requestType, rdyUrl, self.payload, factoryIndexTranslated, writeCookies)

    if self.debug:
        print("making request:")
        print("  url: " + rdyUrl)
        print("  payload: " + self.payload)

        print("  executing call. requestType: " + requestType)
        print("  executing call. Arguments: " + str(arguments))
        #self.printCookies(factoryIndexTranslated)
        print("\n")

    p = subprocess.Popen(arguments, stdout = subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    (result, error) = p.communicate()

    if self.debug:
        print("result: " + str(result))
        print("error:" + str(error))

    unicodeResult = str(result, "UTF-8")
    return unicodeResult


  def addCookiesToArguments(self, arguments, factoryIndexTranslated, writeCookies):
    cookieFile = self.factories[factoryIndexTranslated].cookieFile
    if writeCookies:
      arguments.append('--cookie-jar')
      arguments.append(cookieFile)
    arguments.append('--cookie')
    arguments.append(cookieFile)

  # expects factoryIndex as defined by test. Not translated using self.factoryIndexMap
  def createUploadArguments(self, filepath, factoryIndex):
    factoryIndexTranslated = self.factoryIndexMap[factoryIndex]
    self.urlExtension = "/upload"
    rdyUrl = self.formatUrl(factoryIndexTranslated)
    result = ['curl', "-k", "-F", "file=@" + filepath, rdyUrl]

    self.addCookiesToArguments(result, factoryIndexTranslated, False)

    return result


  def executeUpload(self, filePath, factoryIndex):
    arguments = self.createUploadArguments(filePath, factoryIndex)
    p = subprocess.Popen(arguments, stdout = subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    (result, error) = p.communicate()

    if self.debug:
      print("upload:\n" + str(error))

    return result.decode('utf-8')


  #replaces all occurences of !<parameter>! in value
  #if !<parameter>! is json, it will be converted to a string first
  #  additionally, a leading and trailing '"' will be removed
  #if value was json, result is json
  def replacePlaceholders(self, value, parameters):
    result = value
    regex = re.compile('(!.*?[^!]!)', re.IGNORECASE)
    returnJson = False
    if not isinstance(result, str):
      returnJson = True
      result = json.dumps(result)

    usedParameters = re.findall(regex, result) #value # find all used parameters in value
    #print "used parameters: " + str(usedParameters)

    for upara in usedParameters:
      if upara not in parameters:
        if self.debug:
          print("unknown parameter: " + upara)
        parameterValue = upara
      else:
        parameterValue = parameters[upara]
      if not isinstance(parameterValue, str):
        parameterValue = json.dumps(parameterValue)
        includeQuotes = '"' + upara + '"'
        result = result.replace(includeQuotes, parameterValue)
      #print "replacing '" + upara + "' with '" + parameters[upara] + "'"
      result = result.replace(upara, parameterValue) #replace parameter with value

    if returnJson:
      result = json.JSONDecoder().decode(result)

    return result


  def createOperationExceptionMsg(self, operationJson):
    result = "Error during operation: '"
    result = result + json.dumps(operationJson, indent=2) + "'"
    return result


  def determineListIndex(self, pathStep, parameters, subresponse):
    listKeyName = pathStep
    if "[" in listKeyName:
      listKeyName = listKeyName[0:listKeyName.index("[")]
    result = 0
    #path step is <something>[ <something> ] ... care: something and /
    regex = re.compile('\[(.*[^\]])\]', re.IGNORECASE)
    indexDescription = re.search(regex, pathStep)
    try:
      indexDescription = indexDescription.group(0)
    except Exception as err:
      if err is KeyboardInterrupt:
        raise err
      raise Exception("could not determine ListIndexDescription. PathStep: " + str(pathStep))
    indexDescription = indexDescription[1:len(indexDescription)-1]

    #index is 'last' -> return last Index of list
    if indexDescription == 'last':
      return len(subresponse[listKeyName])-1

    #index was directly in the path -> $list[0]
    try:
      result = int(indexDescription)
      return result
    except ValueError:
      pass

    #index is a parameter we read before -> $list[!index!]
    if indexDescription[0] == '!' and indexDescription[len(indexDescription)-1] == '!' and indexDescription in parameters:
      return int(self.getParameter(parameters, indexDescription))

    #we have something a little more complex
    indexDescription = self.splitIndexDescription(indexDescription)
    for candidate in subresponse[listKeyName]:
      matches = self.complexListIndexMatches(indexDescription, candidate)
      if matches:
        result = subresponse[listKeyName].index(candidate)
        return result

    raise Exception("could not determine list index. Not found. " + str(indexDescription) + ". Listlength was: " + str(len(subresponse[listKeyName])))


  def splitIndexDescription(self, indexDescriptionString):
    result = []
    parts = indexDescriptionString.split("&")
    for part in parts:
      data = part.split("=")
      tuple = (str(data[0]), str(data[1])) # TODO: trim?
      result.append(tuple)
    return result


  #indexDescription is a list of tuples (key, value)
  def complexListIndexMatches(self, indexDescription, candidate):
    for [key, value] in indexDescription:
      if key not in candidate:
        return False
      if str(candidate[key]) != str(value):
        return False
    return True


  #response is json, path is '/' separated path to an item in it
  #path may contain [index] if we want to grap value of index in list
  #returns the item identified by path.
  #returns json (may also be a string)
  def readValueFromJson(self, value, path, parameters):
    pathSteps = path.split('/')
    obj = ""

    #convert to json, if value is string
    if isinstance(value, str):
     try:
       obj = json.JSONDecoder().decode(value)
     except ValueError:
       # try adding quotes
       value = "\""+value + "\""
       try:
         obj = json.JSONDecoder().decode(value)
       except ValueError:
         raise Exception("could not decode response: '" + value + "'")
    else:
      obj = value

    current = obj;
    pathSoFar = ""
    for pathStep in pathSteps:
      if len(pathStep) == 0:
        continue
      pathSoFar = pathSoFar + "/" + pathStep
      #if pathStep contains [] -> it will be $list[_]
      #calculate index
      #after obj = obj[modifiedPathStep]
      #also select index obj = obj[index]
      if "[" in pathStep:
        completePathStep = pathStep
        index = self.determineListIndex(pathStep, parameters, obj)
        pathStep = pathStep[0:pathStep.index("[")]

        if len(pathStep) > 0:
          if pathStep not in obj:
            raise Exception("unexpected json format: " + pathStep + " not found in: " + str(obj))
          obj = obj[pathStep]

        try:
          if isinstance(obj, dict) and index not in obj: #no IndexError if we try a number and object is a dict.
            keys = list(obj.keys())
            raise Exception("Index Error. Could not read from Json: index: '" + str(index) + "'. completePathStep:'" + str(completePathStep)+"'. Object is dict with keys: " + str(keys))

          obj = obj[index]
        except IndexError:
          raise Exception("Index Error. Could not read from Json: index: '" + str(index) + "'. completePathStep: '" + str(completePathStep) + "'. length of object: " + str(len(obj)))
      else:
        if pathStep not in obj:
          raise Exception("pathStep: '" + pathStep + "' was not in value. path: " + path + \
          ". Path so far: " + pathSoFar + \
          ". Object: " + str(obj))
        obj = obj[pathStep]

    return obj


  def readFromResponse(self, response, path, parameters):
    #if self.debug:
    #  print "reading from response. At '" + path + "'. " + str(response).decode('utf-8') + "\n"

    obj = self.readValueFromJson(response, path, parameters)
    return json.dumps(obj)


  def readListLengthFromResponse(self, response, path, parameters):
    obj = self.readValueFromJson(response, path, parameters)
    return len(obj)


  def checkNoException(self, response, url, parameters, payload):
    entireResponse = response

    if response == "<html><body><h3>404 Not Found</h3></body></html>" or "<!doctype html><html lang=\"en\"><head><title>HTTP Status 404 – Not Found</title>" in response:
      shortenedPayload = payload
      if len(payload) > 100:
        shortenedPayload = payload[0:100]
      raise Exception("404 not found: '" + url + "'. payload (max 100): '" + shortenedPayload + "'")

    try:
      response = self.readFromResponse(response, "$meta/fqn", parameters)
    except Exception as err:
      if err is KeyboardInterrupt:
        raise err
      return # if there is no "$meta/fqn", it is not an ErrorResponse
      #print entireResponse #comment in to display entire response
      #raise Exception("error during exception check: '" + response + "' " + url + ". payload: '" + payload + "'")

    if response == "\"xmcp.processmodeller.datatypes.Error\"":
      try:
        response = self.readFromResponse(entireResponse, "exceptionMessage", parameters)
      except Exception as err:
        if err is KeyboardInterrupt:
          raise err
        try:
          response = self.readFromResponse(entireResponse, "message", parameters)
        except Exception as innerErr:
          if innerErr is KeyboardInterrupt:
            raise err
          response = entireResponse # add entire response if we can not find a message

      #cap response/error length
      if len(response) > 200:
        response = response[0:200]
      raise Exception("got an error. url was: " + url + " msg: " + response)
    #print entireResponse


  def modifyParameter(self, oldValue, operationJson, parameters):
    result = oldValue

    #substring
    if operationJson["operation"] == "substring":
      index = -1

      # if there is no divider in string, return complete string
      try:
        result.index(operationJson["divider"])
      except ValueError:
        return oldValue

      if "direction" in operationJson and operationJson["direction"] == "inversed":
        index = result.rindex(operationJson["divider"])
      else:
        index = result.index(operationJson["divider"])

      if "keep" in operationJson and operationJson["keep"] == "before":
        result = result[:index]
      else:
        result = result[index + 1:]

    #concat
    if operationJson["operation"] == "concat":
      result = result + self.replacePlaceholders(operationJson["value"], parameters)

    #urlencode
    if operationJson["operation"] == "urlencode":
      result =  urllib.parse.quote(oldValue)

    #replace
    if operationJson["operation"] == "replace":
      result = oldValue.replace(operationJson["toReplace"], operationJson["replacement"])

    return result


  #function call
  def handleInvoke(self, operationJson, parameters):
    if "functionName" not in operationJson:
      raise Exception("functionName required for invoke." + json.dumps(operationJson))

    functionName = operationJson["functionName"]
    function = self.functions[functionName]
    steps = function["operations"]
    functionParameters = {}

    #set function inputs
    if "input" in function:
      for inputVarName in function["input"]:
        if "mapInput" not in operationJson:
          raise Exception("mapInput not defined, but function requires input. FunctionName: " + functionName)
        if inputVarName in operationJson["mapInput"]:
          ourInputValueWithPlaceholders = operationJson["mapInput"][inputVarName]
        else:
          ourInputValueWithPlaceholders = ""
          print("WARN: not setting function input: " + inputVarName + " of function: " + function["functionName"])

        inputVariableValue = self.replacePlaceholders(ourInputValueWithPlaceholders, parameters)

        if isinstance(inputVariableValue, int) or len(inputVariableValue) > 0:
          functionParameters[inputVarName] = inputVariableValue
        else:
          functionParameters[inputVarName] = ""

    #'execute function'
    for step in steps:
      self.handleOperation(step, functionParameters)

    #TODO: make sure that function caller does not try to access an output that is not there! -> Warning
    #set function outputs
    if "output" in function and "mapOutput" in operationJson:
      for outputVarName in function["output"]:
        if outputVarName in operationJson["mapOutput"]:
          parameters[operationJson["mapOutput"][outputVarName]] = functionParameters[outputVarName]


  def handleAssertEquals(self, operationJson, parameters, response):
   [variable, value] = self.prepareAssertMaybeEquals(operationJson, parameters)
   if not self.checkAreEqual(variable, value):
     raise AssertionError("Assert equals failed: '" + str(variable) + "' is not '" + str(value) + "'")


  def handleAssertNotEquals(self, operationJson, parameters, response):
   [variable, value] = self.prepareAssertMaybeEquals(operationJson, parameters)
   if self.checkAreEqual(variable, value):
     raise AssertionError("Assert not equals failed. Both are: '" + str(variable))


  def checkAreEqual(self, variable, value):
    if str(variable) != str(value):
      try:
        #maybe both are json objects and only order is different
        variable = json.loads(variable)
        variable = json.dumps(variable, indent=2, sort_keys=True)
        value = json.dumps(value, indent=2, sort_keys=True)
      except Exception as err:
        if err is KeyboardInterrupt:
          raise err
      if str(variable) != str(value):
        return False
    return True


  def prepareAssertMaybeEquals(self, operationJson, parameters):
    if operationJson["variable"] not in parameters:
      for v in parameters.keys():
        print("parameterName: " + v)
      raise Exception(operationJson["variable"] + " is not in parameters")

    variable = self.getParameter(parameters, operationJson["variable"])
    value = self.replacePlaceholders(operationJson["value"], parameters)
    return [variable, value]


  def handleAssertListlength(self, operationJson, parameters, response):
   expectedValue = self.replacePlaceholders(operationJson["expectedValue"], parameters)

   if "path" in operationJson:
     path = self.replacePlaceholders(operationJson["path"], parameters)
     length = self.readListLengthFromResponse(response, path, parameters)
     placeDescription = path
   else:
     length = len(self.getParameter(parameters, operationJson["variable"]))
     placeDescription = operationJson["variable"]

   expectedValueInt = -1

   try:
     expectedValueInt = int(expectedValue)
   except:
     raise TypeError("Could not determine expected listLength. This is not an int: " + expectedValue)

   if int(expectedValue) != length:
     raise AssertionError("Assert listLength failed: '" + placeDescription + "' has " + str(length) + " entries. Expected: " + str(expectedValue))


  def handleAssertBigger(self, operationJson, parameters, response):
    variable = self.getParameter(parameters, operationJson["variable"])
    value = self.replacePlaceholders(operationJson["value"], parameters)

    try:
      variable = int(variable)
      value = int(value)
    except:
      raise TypeError("Assert bigger failed. Number problems: '" + variable + "' , '" + value + "'")

    if "orEquals" in operationJson and bool(operationJson["orEquals"]):
      if variable < value:
        raise AssertionError("Assert bigger failed: '" + str(variable) + "' is not bigger than or equal to '" + str(value) + "'")
    elif variable <= value:
      raise AssertionError("Assert bigger failed: '" + str(variable) + "' is not bigger than '" + str(value) + "'")


  def handleAssertFieldNotInResult(self, operationJson, parameters, response):
    variable = operationJson["fieldName"]
    basePath = operationJson["basePath"]
    basePath = self.replacePlaceholders(basePath, parameters)
    obj = self.readFromResponse(response, basePath, parameters)
    obj = json.JSONDecoder().decode(obj)

    if variable in obj:
      raise AssertionError("Assert fieldNotInResult failed: '" + str(variable) + "' is in result ('"+str(obj)+"'). value: '" + str(obj[variable]))


  def handleAssertEqualJson(self, operationJson, parameters, response):
    variable = self.getParameter(parameters, operationJson["variable"])
    value = self.replacePlaceholders(operationJson["value"], parameters)

    variable = variable.replace("\\n", "")
    value = value.replace("\\n", "")
    variable = variable.replace("\\\"", "\"")
    value = value.replace("\\\"", "\"")
    varJson = json.loads(variable)
    varValue = json.loads(value)

    varToString = json.dumps(varJson)
    valueToString = json.dumps(varValue)

    if varToString != valueToString:
      raise AssertionError("Assert equal Json failed. " + varToString + " is not " + valueToString)


  #entry is a dict
  #list is a list containing dicts
  # entry is in list, if there is a dict in list, where alle keys in entry exist and have the values specified in entry
  # variables in entry values are replaced
  def dictInList(self, entry, list, parameters):
    for candidate in list:
      if not isinstance(candidate, dict):
        continue
      if self.entryInDict(entry, candidate, parameters):
        return True
    return False


  def entryInDict(self, entry, candidate, parameters):
    result = True
    for key in entry:
      if key not in candidate:
        return False
      if isinstance(entry[key], dict):
        if not isinstance(candidate[key], dict):
          return False
        partResult = self.entryInDict(entry[key], candidate[key], parameters)
        result = result and partResult
      elif isinstance(entry[key], list):
        if not isinstance(candidate[key], list):
          return False
        partResult = self.isSublist(entry[key], candidate[key], parameters)
        result = result and partResult
      else:
        valueReplaced = self.replacePlaceholders(entry[key], parameters)
        partResult = valueReplaced == candidate[key]
        result = result and partResult

    return result


  def isSublist(self, subList, superList, parameters):
    result = True
    for element in subList:
      if isinstance(element, dict):
        partResult = self.dictInList(element, superList, parameters)
        result = result and partResult
      elif isinstance(element, list):
        partResult = False
        for superElement in superList:
          if not isinstance(superElement, list):
            continue
          if self.isSublist(element, superElement, parameters):
            partResult = True
            break
        result = result and partResult
      else:
        partResult = element in superList
        result = result and partResult
    return result


  def handleAssertIsInList(self, operationJson, parameters, response):
    pathToList = ""
    list = []
    if "pathToList" in operationJson:
      pathToList = operationJson["pathToList"]
      list = self.readFromResponse(response, pathToList, parameters)
      list = json.loads(list) # convert list to json
    else:
      if "variable" not in operationJson:
        raise Exception("assertIsInList requires pathToList or variable!")
      variableToUse = operationJson["variable"]
      list = self.getParameter(parameters, variableToUse)
      pathToList = str(variableToUse)

    entriesToFind = operationJson["entries"]
    invertLogic = False
    if "invertLogic" in operationJson:
      invertLogic = bool(operationJson["invertLogic"])

    for entry in entriesToFind:
      if invertLogic:
        if self.dictInList(entry, list, parameters):
          raise AssertionError("Assert isInList failed. Logic is inverted. Entry " + str(entry) + " is in " + pathToList + ". Data: " + str(list))
      else:
        if not self.dictInList(entry, list, parameters):
          raise AssertionError("Assert isInList failed. " + str(entry) + " is not in " + pathToList + ". Data: " + str(json.dumps(list, indent=2)))


  def handleAssertContains(self, operationJson, parameters, response):
    [variable, value] = self.prepareContains(operationJson, parameters)
    if not value in variable:
      raise AssertionError("Assert contains failed. '" + str(variable) + "' does not contain '" + value + "'")


  def handleAssertDoesNotContain(self, operationJson, parameters, response):
    [variable, value] = self.prepareContains(operationJson, parameters)
    if value in variable:
      raise AssertionError("Assert contains failed. '" + str(variable) + "' does contain '" + value + "'")


  def prepareContains(self, operationJson, parameters):
    variable = self.replacePlaceholders(operationJson["variable"], parameters)
    value = self.replacePlaceholders(operationJson["value"], parameters)

    variable = str(variable).encode('utf-8')
    value = str(value).encode('utf-8')
    return [variable, value]


  def handleAssertStartsWith(self, operationJson, parameters, response):
    variable = self.replacePlaceholders(operationJson["variable"], parameters)
    value = self.replacePlaceholders(operationJson["value"], parameters)

    variable = str(variable).encode('utf-8')
    value = str(value).encode('utf-8')

    if not variable.startswith(value):
      raise AssertionError("Assert startsWith failed. '" + str(variable) + "' does not start with '" + value + "'")


  def handleAssertEndsWith(self, operationJson, parameters, response):
    variable = self.replacePlaceholders(operationJson["variable"], parameters)
    value = self.replacePlaceholders(operationJson["value"], parameters)

    variable = str(variable).encode('utf-8')
    value = str(value).encode('utf-8')

    if not variable.endswith(value):
      raise AssertionError("Assert endsWith failed. '" + str(variable) + "' does not end with '" + value + "'")


  def handleAssertSortedList(self, operationJson, parameters, response):
    variable = self.replacePlaceholders(operationJson["variable"], parameters)
    invertLogic = False
    if "invertLogic" in operationJson and bool(operationJson["invertLogic"]):
      invertLogic = True

    sortedList = json.loads(variable)
    unsortedList = json.loads(variable)

    if "member" in operationJson:
      memberName = operationJson["member"]
      sortedList = [x[memberName] for x in sortedList]
      unsortedList = [x[memberName] for x in unsortedList]

    sortedList = [x.lower() for x in sortedList]
    unsortedList =[x.lower() for x in unsortedList]

    sortedList.sort()

    if not invertLogic and sortedList != unsortedList:
      raise AssertionError("Variable is not sorted. " + str(unsortedList) + " should have been " + str(sortedList))
    elif invertLogic and sortedList == unsortedList:
      raise AssertionError("Variable is sorted, but should not be." + str(sortedList))


  #asssertions
  def handleAssert(self, operationJson, parameters, response = None):
    constraint = operationJson["constraint"]

    if response == None:
      response = self.lastResponse

    if constraint == "equals":
      self.handleAssertEquals(operationJson, parameters, response)
    elif constraint == "notEquals":
      self.handleAssertNotEquals(operationJson, parameters, response)
    elif constraint == "listLength":
      self.handleAssertListlength(operationJson, parameters, response)
    elif constraint == "bigger":
      self.handleAssertBigger(operationJson, parameters, response)
    elif constraint == "fieldNotInResult":
      self.handleAssertFieldNotInResult(operationJson, parameters, response)
    elif constraint == "equalJson":
      self.handleAssertEqualJson(operationJson, parameters, response)
    elif constraint == "isInList":
      self.handleAssertIsInList(operationJson, parameters, response)
    elif constraint == "contains":
      self.handleAssertContains(operationJson, parameters, response)
    elif constraint == "doesNotContain":
      self.handleAssertDoesNotContain(operationJson, parameters, response)
    elif constraint == "startsWith":
      self.handleAssertStartsWith(operationJson, parameters, response)
    elif constraint == "endsWith":
      self.handleAssertEndsWith(operationJson, parameters, response)
    elif constraint == "sortedList":
      self.handleAssertSortedList(operationJson, parameters, response)
    else:
      raise Exception("Unknown assertion constraint: '" + constraint + "'")


  def handleSetVariable(self, operationJson, parameters):
    variableName = operationJson["variable"]
    if variableName not in parameters:
      parameters[variableName] = ""

    variable = self.getParameter(parameters, variableName)

    value = self.replacePlaceholders(operationJson["value"], parameters)
    result = value

    #jsonDecode works, if value is json.
    #if value is a simple string (not quoted), the decoding fails and we use it as a string
    try:
      value = json.JSONDecoder().decode(value)
    except Exception as err:
      if err is KeyboardInterrupt:
        raise err
      pass


    #we only set part of the variable (variable is json)
    if "path" in operationJson:
      result = variable
      pathReplaced = self.replacePlaceholders(operationJson["path"], parameters)
      toUpdate = result
      lastPart = pathReplaced
      if '/' in pathReplaced:
        shortenedPath = pathReplaced[0:pathReplaced.rfind('/')]
        #print "shortenedPath: " + shortenedPath
        #print "calling readValueFromJson: " + result + ", " + shortenedPath
        toUpdate = self.readValueFromJson(result, shortenedPath, parameters)
        lastPart = pathReplaced[len(shortenedPath) + 1:] #+1 is /
      toUpdate[lastPart] = value

    parameters[variableName] = result


  def handleSetRandom(self, operation, parameters):

    if "variable" not in operation:
      raise Exception("setRandom requires variable.")

    if "length" in operation:
      length = operation["length"]
      if not isinstance(length, int):
        raise Exception("length must be an integer. was: '" + length + "'")
    else:
      length = 8

    if "seed" in operation:
      random.seed(str(self.seedPrefix) + operation["seed"])

    value = "".join(random.choice(string.ascii_letters) for x in range(length))
    parameters[operation["variable"]] = value


  def handleIteration(self, operation, parameters):
    singleVarName = operation["singleVariableName"]

    if "variable" not in operation and "count" not in operation:
      raise Exception("variable or count required")

    if "variable" in operation:
      variableIteratingOver = self.getParameter(parameters, operation["variable"])
      if not isinstance(variableIteratingOver, list):
        variableIteratingOver = json.JSONDecoder().decode(variableIteratingOver)
        if not isinstance(variableIteratingOver, list):
          raise Exception("can't iterate over something that is not a list! " + str(type(variableIteratingOver)))
    else:
      numberOfIterations = self.replacePlaceholders(operation["count"], parameters)
      variableIteratingOver = range(0, int(numberOfIterations))

    for singleVar in variableIteratingOver:
      parameters[singleVarName] = singleVar
      for subOperation in operation["operations"]:
        self.handleOperation(subOperation, parameters)

    #remove single variable from parameters -- TODO: might remove some outer variable of same name.
    #parameters.pop(singleVarName)


  def handlePrintVariable(self, operation, parameters):
    if not "variable" in operation:
      raise Exception("invalid print operation. variable required. " + str(operation))

    value = self.getParameter(parameters, operation["variable"])

    if isinstance(value, str):
      pass
    else:
      try:
        value = str(json.dumps(value, indent=2))
      except Exception as err:
        if err is KeyboardInterrupt:
          raise err

    print("print: " + operation["variable"]  + " (" + str(type(self.getParameter(parameters, operation["variable"]))) + "):")
    print(value)


  def handleSetFromVariable(self, operationJson, parameters):
    variableNametoSet = operationJson["targetVariable"]
    variableNameToRead = operationJson["sourceVariable"]

    obj = self.getParameter(parameters, variableNameToRead)

    if isinstance(obj, dict):
      obj = json.dumps(obj)

    if isinstance(obj, list):
      obj = json.dumps(obj)

    try:
      obj = json.JSONDecoder().decode(obj)
    except Exception as err:
      if err is KeyboardInterrupt:
        raise err
      raise Exception("could not decode json. obj was: " + str(obj))

    path = operationJson["path"]
    path = self.replacePlaceholders(path, parameters)

    obj = self.readFromResponse(obj, path, parameters)
    obj = self.checkAndUnquoteResult(operationJson, obj)

    obj = self.convertToObject(operationJson, obj)

    parameters[variableNametoSet] = obj


  def convertToObject(self, operationJson, obj):
    if "convertToObject" in operationJson and bool(operationJson["convertToObject"]):
      try:
        obj = json.JSONDecoder().decode(obj)
      except Exception as err:
        if err is KeyboardInterrupt:
          raise err
        print("exception converting into object: " + str(err) + " - " + str(obj))
        #object remains string
    return obj


  def handleSubtestcase(self, operation, parameters):
    testcaseName = ""

    if "subtestcaseName" in operation:
      testcaseName = self.replacePlaceholders(operation["subtestcaseName"], parameters)
      print("starting subtestcase: " + testcaseName)
    else:
      print("starting unnamed subtestcase")
    sys.stdout.flush()

    operations = operation["operations"]

    try:
      for operation in operations:
        self.handleOperation(operation, parameters)
      self.successes = self.successes + 1
    except SuccessException:
      self.successes = self.successes + 1
    except KeyboardInterrupt as err:
        raise err
    except Exception as err:
      self.fails = self.fails + 1
      if len(testcaseName) > 0:
        print("subtestcase failed: " + testcaseName)
        self.failedList.append("(subtestcase) " + testcaseName + " " + str(err))
      else:
        print("subtestcase failed")
      sys.stdout.flush()


  def handleCall(self, operationJson, parameters):
    url = self.replacePlaceholders(operationJson["url"], parameters)
    payload = ""
    factoryIndex = operationJson["factoryIndex"] if "factoryIndex" in operationJson else 0
    if "payload" in operationJson:
      payload = json.dumps(operationJson["payload"])
      payload = self.replacePlaceholders(payload, parameters)
      try:
        payload = json.dumps(json.loads(payload), ensure_ascii=False)
      except Exception as e:
        print("Invalid json: " + str(payload))
        raise e
    #TODO: it seems like this does not trigger?
    if "requestType" not in operationJson:
      raise Exception("requestType required for call operation.")

    if "async" in operationJson and bool(operationJson["async"]):
      self.executeAsyncCall(operationJson, parameters, url, payload, factoryIndex)
    else:
      self.executeSyncCall(operationJson, parameters, url, payload, factoryIndex)


  def threadAsyncCall(self, operationJson, parameters, url, payload, factoryIndex, callId):
    try:
      response =  self.executeCallReturnResult(operationJson, parameters, url, payload, factoryIndex)
    except MaxRetriesExccededException as err:
      response = err
    self.lock.acquire()
    self.threadResults[callId] = response
    self.lock.release()

  def executeAsyncCall(self, operationJson, parameters, url, payload, factoryIndex):
    if "callId" not in operationJson:
      raise Exception("callId missing in async call operation")
    callId = operationJson["callId"]

    threadParameters = parameters.copy()

    thread = threading.Thread(target = self.threadAsyncCall, args = (operationJson, threadParameters, url, payload, factoryIndex, callId))
    self.threads[callId] = thread
    thread.start()


  # if call defines retries, this function may make multiple calls if necessary
  def executeCallReturnResult(self, operationJson, parameters, url, payload, factoryIndex):

    maxRetries = int(self.replacePlaceholders(operationJson["retries"]["maxRetries"], parameters)) if "retries" in operationJson and "maxRetries" in operationJson["retries"] else 0
    takenTries = 0

    while takenTries <= maxRetries:
      response = self.executeRequest(url, operationJson["requestType"], payload, factoryIndex, False)
      if "acceptError" not in operationJson or not bool(operationJson["acceptError"]):
        self.checkNoException(response, url, parameters, payload)
      anotherTryNecessary = self.checkRetryNecessary(response, operationJson, parameters)
      if not anotherTryNecessary:
        break
      takenTries = takenTries + 1

    if takenTries > maxRetries:
      raise MaxRetriesExccededException("maxRetriesExceeded. Another call is necessary after reaching " + str(maxRetries) + " retries.")

    return response


  def executeSyncCall(self, operationJson, parameters, url, payload, factoryIndex):
    self.lastResponse = self.executeCallReturnResult(operationJson, parameters, url, payload, factoryIndex)


  def checkAndUnquoteResult(self, operationJson, value):
    if "unquoteResult" in operationJson and bool(operationJson["unquoteResult"]):
      if len(value) < 2 or value[0] != "\"" or value[len(value)-1] != "\"":
        raise Exception("could not unquote value: '" + value + "'")
      value = value[1:len(value)-1]
    return value


  # checks if the response satisfies a condition defined in operationJson
  def checkRetryNecessary(self, response, operationJson, parameters):
    if "retries" not in operationJson:
      return False # if there are no retries configured, another try is not necessary
    if "retryCondition" not in operationJson["retries"]:
      raise Exception("No retry condition set!")

    if "variable" in operationJson["retries"]:
      variableName = operationJson["retries"]["variable"]
      parameters[variableName] = response

    retryCondition = operationJson["retries"]["retryCondition"]
    try:
      for operation in retryCondition:
        self.handleOperation(operation, parameters)
    except OperationException as e:
      if isinstance(e.innerException, AssertionError):
        return True
      else:
        print("Exception during operation in retry: " + str(e))
        return False
    except Exception as err:
      print("Exception during Retry check: " + str(err))
      return False

  def handleReadFromResponse(self, operationJson, parameters):
    if "pathInResponse" not in operationJson:
      raise Exception("pathInResponse required for read operation, but not set.")

    if "targetVariable" not in operationJson:
      raise Exception("targetVariable required for read operation, but not set.")

    path =  operationJson["pathInResponse"]
    path = self.replacePlaceholders(path, parameters)

    if path == "":
      value = self.lastResponse
    else:
      value = self.readFromResponse(self.lastResponse, path, parameters)

    value = self.checkAndUnquoteResult(operationJson, value)

    if "getListLength" in operationJson and bool(operationJson["getListLength"]):
      value = json.JSONDecoder().decode(value)
      if not isinstance(value, list):
        raise Exception("can't determine listlength for: '" + str(value) + "'. Not a list.")
      else:
        value = len(value)

    value = self.convertToObject(operationJson, value)

    parameters[operationJson["targetVariable"]] = value


  def handleModification(self, operationJson, parameters):
    if "divider" in operationJson["modification"]:
      divider = operationJson["modification"]["divider"]
      if divider[0] == "!" and divider[len(divider)-1] == "!":
        operationJson["modification"]["divider"] = self.getParameter(parameters, divider)
    variableName = operationJson["variable"]

    #if result is suppost to be assigned to a different variable
    if "targetVariable" in operationJson:
      variableName = operationJson["targetVariable"]

    variable = self.getParameter(parameters, operationJson["variable"])
    parameters[variableName] = self.modifyParameter(variable, operationJson["modification"], parameters)


  def handleMultiSet(self, operationJson, parameters):
    for entry in operationJson["data"]:
      for key in entry.keys():
        parameters[key] = self.replacePlaceholders(entry[key], parameters)


  def handleSelectFromXml(self, operationJson, parameters):
    if "inputVariable" not in operationJson:
      raise Exception("inputVariable field required for selectFromXml")
    inputVarName = operationJson["inputVariable"]

    if "xpath" not in operationJson:
      raise Exception("xpath field required for selectFromXml")
    xpath = operationJson["xpath"]

    if "outputVariable" not in operationJson:
      raise Exception("variable field required for selectFromXml")

    if inputVarName not in parameters:
      raise Exception("undefined inputVariable: '" + inputVarName+ "'")
    inputVar = self.getParameter(parameters, inputVarName)


    if "jsonDecode" in operationJson and bool(operationJson["jsonDecode"]):
      inputVar = json.JSONDecoder().decode(inputVar)

    try:
      file = BytesIO(inputVar.encode('utf-8'))
    except UnicodeDecodeError:
      file = BytesIO(inputVar)

    root = ET.parse(file)

    if len(xpath) > 0:
      if "returnNode" in operationJson and bool(operationJson["returnNode"]):
        result = root.findall(xpath)
      else:
        result = root.find(xpath)
    else:
      result = root.getroot()
    file.close()


    if result is None:
      raise Exception("SelectFromXml. Could not find result. xpath: " + xpath)

    if "attribute" in operationJson:
      result = result.attrib[operationJson["attribute"]]
    elif "returnNode" in operationJson and bool(operationJson["returnNode"]):
      finalResult = ""
      if isinstance(result, list):
        for part in result:
          finalResult = finalResult + str(ET.tostring(part, encoding='utf-8', method='xml'))
        result = finalResult
      else:
       result = ET.tostring(result, encoding='utf-8', method='xml')
    else:
      result = result.text

    parameters[operationJson["outputVariable"]] = result


  def handleConvertToJson(self, operationJson, parameters):
    variableName = operationJson["variable"]
    variable = self.getParameter(parameters, variableName)
    targetVariableName = variableName
    if "targetVariable" in operationJson:
      targetVariableName = operationJson["targetVariable"]

    #variable is a string containing json, but it includes escaped characters!
    variable = variable.replace("\\n", "")
    variable = variable.replace("\\\\", "\\")
    variable = variable.replace("\\\"", "\"")
    parameters[targetVariableName] = variable


  def handleUpload(self, operationJson, parameters):
    targetVariableName = operationJson["fileIdVar"]
    file = operationJson["file"]
    factoryIndex = operationJson["factoryIndex"] if "factoryIndex" in operationJson else 0

    if "relativeToThis" in operationJson and bool(operationJson["relativeToThis"]):
      file = os.path.join(self.currentTest, file)

    result = self.executeUpload(file, factoryIndex)
    orgResult = result
    regex = re.compile('[0-9]+$', re.IGNORECASE)
    result = re.findall(regex, result)

    if len(result) != 1:
      raise Exception("Upload failed! " + str(orgResult))

    parameters[targetVariableName] = result[0]


  def handleCurrentTime(self, operationJson, parameters):
    milliseconds = int(round(time.time() * 1000))
    targetVarName = operationJson["targetVariable"]
    parameters[targetVarName] = milliseconds


  def handleWait(self, operationJson, parameters):
    seconds = operationJson["seconds"]
    time.sleep(seconds)

  def handleGetUsername(self, operationJson, parameters):
    factoryIndexInTest = 0
    if "factoryIndex" in operationJson:
      factoryIndexInTest = operationJson["factoryIndex"]
    factoryIndexTranslated = self.factoryIndexMap[factoryIndexInTest]
    targetVarName = operationJson["targetVariable"]
    parameters[targetVarName] = self.factories[factoryIndexTranslated].username

  def handleGetValueFromTag(self, operationJson, parameters):
      variableName = operationJson["targetVariable"]
      tag = operationJson["tag"]
      factoryIndexInTest = operationJson["factoryIndex"] if "factoryIndex" in operationJson else 0
      factoryIndexTranslated = self.factoryIndexMap[factoryIndexInTest]
      value = self.factories[factoryIndexTranslated].tags[tag]
      parameters[variableName] = value


  def handleMergeList(self, operationJson, parameters):
    baseListVarName = operationJson["baseList"]
    toAddVarName = operationJson["listToAdd"]

    baseList = self.getParameter(parameters, baseListVarName)
    toAddList = self.getParameter(parameters, toAddVarName)

    baseList.extend(toAddList)


  def handleJoinAsync(self, operationJson, parameters):
    entries = operationJson["threads"]
    for entry in entries:
      self.joinByCallId(entry, parameters)


  def joinByCallId(self, entry, parameters):
    callId = entry["callId"]
    thread = self.threads[callId]
    thread.join(1000)
    if "variable" in entry:
      variable = entry["variable"]
      self.lock.acquire()
      result = self.threadResults[callId]
      parameters[variable] = result
      self.lock.release()
      del self.threads[callId]
      if isinstance(result, MaxRetriesExccededException):
        raise result


  def handleAdd(self, operationJson, parameters):
    targetVarName = operationJson["targetVariable"]
    result = 0
    if("sourceVariable" in operationJson):
      sourceVarName = operationJson["sourceVariable"]
      result = self.getParameter(parameters, sourceVarName)
    elif("sourceConstant" in operationJson):
      result = operationJson["sourceConstant"]

    if("additionVariable" in operationJson):
      additionVarName = operationJson["additionVariable"]
      result = int(result) + int(self.getParameter(parameters, additionVarName))
    if("additionConstant" in operationJson):
      result = int(result) + int(operationJson["additionConstant"])

    parameters[targetVarName] = result


  def handleOperation(self, operationJson, parameters):
    if "ignore" in operationJson and bool(operationJson["ignore"]):
      if keyOperation in operationJson:
        operation = operationJson[keyOperation]
      else:
        operation = "<no operation set>"
      print("ignoring operation: " + operation)
      return

    if keyOperation not in operationJson:
      raise Exception("missing " + keyOperation + " in " + str(operationJson))

    operation = operationJson[keyOperation]

    try:
      if operation == keyOperationCall:
       self.handleCall(operationJson, parameters)
      if operation == keyOperationRead:
        self.handleReadFromResponse(operationJson, parameters)
      if operation == keyOperationMod:
        self.handleModification(operationJson, parameters)
      if operation == keyOperationPrint:
        self.handlePrintVariable(operationJson, parameters)
      if operation == keyOperationSet:
        self.handleSetVariable(operationJson, parameters)
      if operation == keyOperationInvoke:
        self.handleInvoke(operationJson, parameters)
      if operation == keyOperationAssert:
        self.handleAssert(operationJson, parameters)
      if operation == keyOperationSetRandom:
        self.handleSetRandom(operationJson, parameters)
      if operation == keyOperationIterate:
        self.handleIteration(operationJson, parameters)
      if operation == keyOperationSetFromVariable:
        self.handleSetFromVariable(operationJson, parameters)
      if operation == keyOperationSubtestcase:
        self.handleSubtestcase(operationJson, parameters)
      if operation == keyOperationSuccess:
        raise SuccessException()
      if operation  == keyOperationMultiset:
        self.handleMultiSet(operationJson, parameters)
      if operation == keyOperationSelectFromXml:
        self.handleSelectFromXml(operationJson, parameters)
      if operation == keyOperationToJson:
        self.handleConvertToJson(operationJson, parameters)
      if operation == keyOperationUpload:
        self.handleUpload(operationJson, parameters)
      if operation == keyOperationCurrentTime:
        self.handleCurrentTime(operationJson, parameters)
      if operation == keyOperationAdd:
        self.handleAdd(operationJson, parameters)
      if operation == keyOperationWait:
        self.handleWait(operationJson, parameters)
      if operation == keyOperationGetUsername:
        self.handleGetUsername(operationJson, parameters)
      if operation == keyOperationGetValueFromTag:
        self.handleGetValueFromTag(operationJson, parameters)
      if operation == keyOperationJoinAsyncCall:
        self.handleJoinAsync(operationJson, parameters)
      if operation == keyOperationMergeList:
        self.handleMergeList(operationJson, parameters)
    except SuccessException as err:
        raise(err)
    except Exception as err:
      if err is KeyboardInterrupt:
        raise err
      exceptionMsg = self.createOperationExceptionMsg(operationJson)
      opException = OperationException(exceptionMsg + "\n Reason: " + str(err))
      opException.innerException = err
      raise opException


  def runTestCase(self, descriptionFile, completeJson):
    print("    test case: " + descriptionFile)
    sys.stdout.flush()

    if "operations" not in completeJson:
      raise Exception("no operations in '" + descriptionFile + "'")

    operations = completeJson["operations"]

    #validate (all called functions are registered)
    for operation in operations:
      if "operation" not in operation:
        continue
      if operation["operation"] == "invoke":
        if operation["functionName"] not in self.functions:
          raise Exception("Function '" + operation["functionName"] + "' not registered.")

    #execute.
    self.lastResponse = ""
    self.currentTest = os.path.dirname(os.path.abspath(descriptionFile))
    parameters = {}

    try:
      for tuple in operations:
        self.handleOperation(tuple, parameters)
    except SuccessException as err:
      #check for running threads
      self.checkRunningThreads()
      raise err #only raise err, if there are no exceptions during running threads

    #check for running threads
    self.checkRunningThreads()

    # clear threads
    self.threads = {}
    threadResults = {}

  def checkRunningThreads(self):
    for callId in self.threads:
      threadObj = self.threads[callId]
      print("WARN: Call not joined: " + str(callId))

    for callId in self.threads:
      threadObj.join()


  def login(self, factoryIndex):
    factoryIndexTranslated = self.factoryIndexMap[factoryIndex]
    username = self.factories[factoryIndexTranslated].username
    password = self.factories[factoryIndexTranslated].password
    payload = '{"username": "' + username + '", "password": "' + password + '", "path": "/"}'
    response = self.executeRequest("/auth/login", 'POST', payload, factoryIndex, True)
    self.checkNoException(response, "/auth/login", [], payload)


  def logout(self, factoryIndex):
    self.executeRequest("/auth/logout", 'POST', '', factoryIndex, True)


  def logoutForTest(self, testJson):
    if "factoryCount" not in testJson:
      self.logout(0)
    else:
      requiredFactories = int(testJson["factoryCount"])
      for i in range(0,requiredFactories,1):
        self.logout(i)


  def loginForTest(self, testJson):
    if "factoryCount" not in testJson:
      self.login(0)
    else:
      requiredFactories = int(testJson["factoryCount"])
      for i in range(0,requiredFactories,1):
        self.login(i)


  def getParameter(self, parameters, parameterName):
    if parameterName not in parameters:
      raise Exception("Undefined Parameter '" + parameterName +"'. Defined Parameters: " + str(parameters))
    return parameters[parameterName]


  #sets self.factoryIndexMap
  #called per test
  def resolveFactoryConstraints(self, testJson):
    if self.debug:
      print("resolving constraints")

    if "factoryCount" in testJson:
      requiredFactories = int(testJson["factoryCount"])
      if len(self.factories) < requiredFactories:
        raise NoValidFactoryConfigException(f"Insufficient factories configured. Test requires {requiredFactories} factories, but only {len(self.factories)} are configured!")

    self.factoryIndexMap = {}
    self.testHasConstraints = False
    factoryCount = int(testJson["factoryCount"]) if "factoryCount" in testJson else 1
    if "factoryConstraints" not in testJson:
      #assign as they are in the config
      for i in range(0, factoryCount,1):
        self.factoryIndexMap[i] = i
      return

    self.testHasConstraints = True
    #find combination of factories that satisfies the constraints
    constraints = testJson["factoryConstraints"]
    candidates = self.createCandidateMaps(factoryCount, len(self.factories))
    for candidate in candidates:
      if self.isValidFactoryCombination(candidate, constraints):
        self.factoryIndexMap = candidate
        break
    if len(self.factoryIndexMap) == 0:
      raise NoValidFactoryConfigException("Could not find valid combination of factories.")
    if self.debug:
      print("factoryIndexMap ( id in testcase to id in configuration ): " + str(self.factoryIndexMap))
    return


  def isValidFactoryCombination(self, candidate, constraints):
    for constraint in constraints:
      if not self.validateFactoryConstraint(candidate, constraint):
        return False
    return True


  def validateFactoryConstraint(self, candidateMap, constraint):
    if "constraintType" not in constraint:
      print("no constraint type set: " + str(constraint))
      return True
    constraintType = constraint["constraintType"]

    if constraintType not in self.factoryConstraintValidatorFunctions:
      raise Exception("Unknown Constraint type: " + str(constraintType))
    constraintValidatorFunction = self.factoryConstraintValidatorFunctions[constraintType]
    return constraintValidatorFunction(candidateMap, constraint)


  #returns a list containing all possible candidate maps
  #TODO: slow, should stream
  def createCandidateMaps(self, requiredFactories, availableFactories):
    result = []
    if availableFactories < requiredFactories:
      return result

    result = self.recursiveCreateCandidateMap(requiredFactories, availableFactories, {})

    return result


  def recursiveCreateCandidateMap(self, remainingToAdd, availableFactories, resultSoFar):
    if remainingToAdd <= 0:
      return [resultSoFar]
    results = []
    for i in range(0, availableFactories, 1):
      if i not in resultSoFar.keys():
        copy = resultSoFar.copy()
        copy[len(resultSoFar)] = i
        furtherResults = self.recursiveCreateCandidateMap(remainingToAdd-1, availableFactories, copy)
        results = results + furtherResults
    return results


  def readTestContent(self, descriptionFile):
    if self.debug:
      print("reading test content for: " + str(descriptionFile))
    completeJson = ""
    try:
      with open(descriptionFile, "r", encoding="utf8") as jsonFile:
        completeJson = json.load(jsonFile)
    except KeyboardInterrupt as e:
      raise e
    except Exception as e1:
      print("invalid json in '" + str(descriptionFile) + "'. " + str(e1))
      return None
    return completeJson


  def runTestSeries(self, seriesFile, path, openSeries = []):
    print("running test series: " + path + seriesFile)

    completePathAndFile = (path + seriesFile)
    if completePathAndFile in openSeries:
      print("not executing " + completePathAndFile + " again.")

    openSeries.append(completePathAndFile)

    completeJson = ""
    with open(path + seriesFile, "r") as jsonFile:
      try:
        completeJson = json.load(jsonFile)
      except Exception as e:
        print("Could not parse test series file '{0}': {1}".format(completePathAndFile, e))
        raise e

    # execute tests
    if "tests" in completeJson:
      for test in completeJson["tests"]:
        testcontent = self.readTestContent(path + test)
        if testcontent == None:
          self.fails = self.fails + 1
          self.failedList.append(path + test + "(from "+ path + seriesFile + ") - invalid Test json.")
          continue #invalid json

        try:
          self.resolveFactoryConstraints(testcontent)
        except NoValidFactoryConfigException as e:
          self.fails = self.fails + 1
          self.failedList.append(f"{path}{test} (from {path}{seriesFile}) - {e}")
          continue

        self.loginForTest(testcontent)

        try:
          self.runTestCase(path + test, testcontent)
          self.successes = self.successes + 1
        except ValueError as ve:
          self.fails = self.fails + 1
          self.failedList.append(path + test + "(from "+ path + seriesFile + ") - invalid JSON. " + str(ve))
          print("Test failed: " + test)
          traceback.print_exc(file=sys.stdout)  #comment in for stacktrace on exception

        except SuccessException:
            self.successes = self.successes + 1
            self.logoutForTest(testcontent)
            continue
        except Exception as err:
          if err is KeyboardInterrupt:
            self.logoutForTest(testcontent)
            raise err

          self.fails = self.fails + 1
          self.failedList.append(path + test + " (from "+ path + seriesFile + "): " + str(err))
          print("Test failed: " + test + ("" if not self.testHasConstraints else " factory index map (id in test -> id in config): " + str(self.factoryIndexMap)))
          #print "Error msg: " + str(err)
          traceback.print_exc(file=sys.stdout) #comment in for stacktrace on exception
        self.logoutForTest(testcontent)

    # execute sub testseries
    if "testseries" in completeJson:
      for testseries in completeJson["testseries"]:
        additionalPath = testseries[0:testseries.rfind('/')+1]
        newPath = os.path.join(path, additionalPath)
        testseries = testseries[len(additionalPath):]
        self.runTestSeries(testseries, newPath, openSeries)

#path
pathOfScript = os.path.dirname(os.path.realpath(sys.argv[0]))

#determine testseries to execute
testSeriesName = pathOfScript + "/testcases/alltests.json"

returncode = 0

if len(sys.argv) > 1:
  testSeriesName = sys.argv[1]

#run testseries
configPath =  pathOfScript + "/config.cfg"
rt = RequestTester(configPath)
startTime = datetime.datetime.now();
path = testSeriesName[0: testSeriesName.rfind('/')+1]
testSeriesName = testSeriesName[len(path):]
try:
  rt.runTestSeries(testSeriesName, path)
except Exception as err:
  if err is KeyboardInterrupt:
    print("KeyboardInterrupt. Testing aborted.")
  else:
    print("exception: " + str(err))
  returncode = 1
endTime = datetime.datetime.now()

successes = rt.successes
fails = rt.fails
failedList = rt.failedList

#print output
if fails > 0:
  print("Failed Tests:")
  for failedTest in failedList:
    print("  " + failedTest)
  returncode = 2
print("done. Tests took: " + str(endTime - startTime) + ". Successes: " + str(successes) + ", Fails: " + str(fails))

sys.exit(returncode)
