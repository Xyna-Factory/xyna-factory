# -*- coding: utf-8 -*-
import os
import subprocess
import json
import traceback
import copy
import sys
import re

#TODO: does not check/update inner Operations (-> inside iterate)


#Updates an autotest to fit a new environment
#Test can be changed in the following ways:
# - read operation path changes
# - read operation is moved up (before original call)
class TestFileUpdater:

  emptyVarName = "emptyVar" # to mark end of variable value -> !genVarX!:\n <value> \n !emtpyVar!
  generatedVarNameBase = "genVar" #generated variables to read from results are named !<this><#>!
  newKnowladgeVarName = "newInfo"

  autoTesterPath = ""
  resetEnvironmentProgram = ""
  changeEnvironmentProgram = ""
  resultSuffix = ""
  listIndexKeys = []

  def loadConfig(self, path):
    #path to autoTester -- something like ../autoTester.py
    #path to executable to reset  environment -- something like disablePerformanceOptimization.sh
    #path to executable to change environment -- something like enablePerformanceOptimization.sh
    #result suffix
    
    with open(path, "r") as jsonFile:
      configJson = json.load(jsonFile)
      self.autoTesterPath = str(configJson["autoTesterPath"])
      self.resetEnvironmentProgram = str(configJson["resetEnvironmentProgram"])
      self.changeEnvironmentProgram = str(configJson["changeEnvironmentProgram"])
      self.resultSuffix = configJson["resultSuffix"]
      self.listIndexKeys = configJson["listIndexKeys"]

  def __init__(self):
    self.loadConfig(os.path.dirname(os.path.realpath(sys.argv[0])) + "/config.json")

    self.oldKnowlage = {} # maps from genVarName (w/o !) to value (in old test) 
    self.newKnowladge = [] #contains entire response as json; used to update asserts
    self.knowlage = [] # contains dictionaries from path to data for every request ; used to update reads
    self.nextCallIndex = 0
    self.genVarMapping = {} # maps from variable in oldTest to generated Variable name; key includes !, value does not include !



  def update(self, testcase):
    oldTestComplete = self.readOldTestJson(testcase)
    self.resetEnvironment()
    oldKnowlage = self.runTestOld(oldTestComplete)
    testParts = self.splitTest(oldTestComplete)
    newTestJson = {}
    self.copyMeta(oldTestComplete, newTestJson)
    self.createOperationsContainer(newTestJson)
    self.changeEnvironment()
    i=0
    for testPart in testParts:
      print("updating test part " + str(i) + "/" + str(len(testParts)))
      self.updateTestPart(testPart, newTestJson) 
      i = i + 1
      
    self.cleanupResult(newTestJson)
      
    newTestJson = json.dumps(newTestJson, indent=2, sort_keys=True, separators=(',', ': '))
    self.writeResult(testcase, newTestJson)    
    return newTestJson


  #removes callIds
  def cleanupResult(self, newTestJson):
    for operation in newTestJson["operations"]:
      if "callId" in operation:
        del operation["callId"]

  #TODO: merge with writeTmp
  def writeResult(self, path, newTestJson):
    fileName = path[:len(path)-5] + self.resultSuffix + ".json" 
    f = open(fileName, "w")
    f.write(newTestJson)
    f.close()
    

  def copyMeta(self, oldTestComplete, newTestJson):
    for key in oldTestComplete.keys():
      if key == "operations":
        continue #do not copy operations
      newTestJson[key] = copy.deepcopy(oldTestComplete[key])
  
  
  def createOperationsContainer(self, newTestJson):
    newTestJson["operations"] = []  


  #input: path to testcase
  #output: json object
  def readOldTestJson(self, testcase):
    with open(testcase, "r") as jsonFile:
      oldTestComplete = json.load(jsonFile)
    return oldTestComplete


  #execute old test in old environment. 
  #add additional print statements to test
  #collect values of read variables => oldKnowlage
  def runTestOld(self, oldTestJson):
    print("starting.. runTestOld")
    result = []
    oldTestAndVars = self.addPrintsToOldTest(oldTestJson) #numVariables, oldTestWithPrints
    numVariables = oldTestAndVars["variableCount"]
    oldTestWithPrints = oldTestAndVars["testcase"]
    tmpTest = self.createTmpTestFile(oldTestWithPrints)
    (output, err) = self.callAutoTester(tmpTest)
    completeOutput = (output, err)
    if not self.testCaseSucceeded(completeOutput):
      print("err: " + err)
      print("out: " + output)
      raise Exception("old Test failed!")
    for i in range(0, numVariables):
      value = self.readVariableValue(output, i)
      self.oldKnowlage[self.getGenVarName(i)] = value
      
    print("finished.. runTestOld")
    return result

  
  #returns Tuple {#ofVariables, extendedTestCaseJson} - keys: variables, testcase
  #creates a new object for extendedTestCaseJson
  #before a read operation uses a variable, 
  # create a copy of it with distinct name
  # and print that new variable.
  def addPrintsToOldTest(self, testcaseJson):
    result = {}  
    variableCount = 0
    extendedTest = copy.deepcopy(testcaseJson)
    
    setOp = {}
    setOp["operation"] = "set"
    setOp["variable"] = "!" + self.emptyVarName + "!"
    setOp["value"] = ""
    extendedTest["operations"].insert(0, setOp)
    
    for i in range(len(extendedTest["operations"]) -1,0,-1): #TODO: are we missing the last one?
      operation = extendedTest["operations"][i]
      if operation["operation"] != "read":
        continue # not a call operation => no read/print needed
      self.addReadAndPrint(extendedTest, i, operation["pathInResponse"], variableCount)
      self.genVarMapping[operation["targetVariable"]] = self.getGenVarName(variableCount)
      variableCount = variableCount + 1
    
    result["variableCount"] = variableCount
    result["testcase"] = extendedTest
    return result

    
  def addReadAndPrint(self, testcase, operationIndex, path, currentVariableCount):
    operations = testcase["operations"]
    varName = self.getGenVarName(currentVariableCount)
    printOperation = self.createPrintOperation(self.emptyVarName)
    operations.insert(operationIndex, printOperation)
    printOperation = self.createPrintOperation(varName)
    operations.insert(operationIndex, printOperation)
    readOperation = self.createReadOperation(varName, path, currentVariableCount, -1)
    operations.insert(operationIndex, readOperation)
    
    
  #newTestJson should end with a call operation.
  #returns a new object that reads entire response into a variable and prints it
  def addPrintToNewTest(self, newTestJson):
    result = copy.deepcopy(newTestJson)
    
    
    setOp = {}
    setOp["operation"] = "set"
    setOp["variable"] = "!" + self.emptyVarName + "!"
    setOp["value"] = ""
    result["operations"].insert(0, setOp)
    
    readOp = self.createReadOperation(self.newKnowladgeVarName, "/", -1, -1, False) #do not unquote entire response
    result["operations"].append(readOp)
    varName = self.newKnowladgeVarName
    printOperation = self.createPrintOperation(varName)
    result["operations"].append(printOperation)
    printOperation = self.createPrintOperation(self.emptyVarName)
    result["operations"].append(printOperation)
    return result


  def resetEnvironment(self):
    self.executeProgram("bash", [self.resetEnvironmentProgram,])


  #setup environment for test after update
  def changeEnvironment(self):
    self.executeProgram("bash", [self.changeEnvironmentProgram])


  # iterate through operations of testcase and cut at call, assert and read operations
  # returns a list of tuples: {type, testPart} =>  keys are type and testPart
  # type is either call or read
  def splitTest(self, testcaseJson):
    print("starting.. splitTest")
    result = []
    currentPart = []
    operations = copy.deepcopy(testcaseJson["operations"])
    for operation in operations:
      if operation == None or "operation" not in operation:
        print("None operation/no operation specified: " + operation)
        continue
      currentPart.append(operation)
      operationType = operation["operation"]
      if operationType == "call" or operationType == "read" or operationType == "assert": # call or read or assert operation
        resultEntry = {}
        resultEntry["type"] = operationType
        resultEntry["testPart"] = currentPart
        result.append(resultEntry)
        currentPart = [] #reset
    
    print("finished.. splitTest. TestParts:" + str(len(result)))
    return result

    
  #TODO: oldTestPart naming
  #oldTestPart is Tuple: {type, testPart}
  #modifies newTestJson - mostly by appending oldTestPart
  def updateTestPart(self, oldTestPart, newTestJson):
    if oldTestPart["type"] == "call":
      self.updateTestPartCall(oldTestPart["testPart"], newTestJson)
      newTestWithPrint = self.addPrintToNewTest(newTestJson)
      self.gatherNewKnowladge(newTestWithPrint)
    elif oldTestPart["type"] == "read":
      self.updateTestPartRead(oldTestPart["testPart"], newTestJson)
    else:
      self.updateTestPartAssert(oldTestPart["testPart"], newTestJson)
      
    
    if not self.doesTestWork(newTestJson):
      raise Exception("updating test part was not successfull.")


  #just append oldTestPart to newTestPart
  #also set call index.
  def updateTestPartCall(self, oldTestPart, newTestJson):
    for operation in oldTestPart:
      newTestJson["operations"].append(operation)
    newTestJson["operations"][-1]["callId"] = self.nextCallIndex
    self.nextCallIndex = self.nextCallIndex + 1


  #last operation in oldTestPart is an assert
  #append all operations in oldTestPart to newTestJson, except for the last
  #if it still works, append it
  #otherwise => updateReadOperaiton
  def updateTestPartAssert(self, oldTestPart, newTestJson):
    if not self.updateRequired(oldTestPart, newTestJson):
      return
    # listlength, fieldNotInResult, isInList
    constraint = newTestJson["operations"][-1]["constraint"]
    if constraint == "listLength":
      self.updateAssertListLength(newTestJson)
    elif constraint == "isInList":
      self.updateAssertIsInList(newTestJson)
    elif constraint == "fieldNotInResult":
      self.updateFieldNotInResult(newTestJson)
    else:
      raise Exception("assert needs update")
    
  
  #we need to change the last Operation in newTestJson.
  #It is an assert listLength operation.
  def updateAssertListLength(self, newTestJson):
    operation = newTestJson["operations"][-1]
    oldPath = operation["path"]
    know = self.newKnowladge[-1]
    know = json.JSONDecoder().decode(know)
    know = json.dumps(know, indent=2, sort_keys=True)
#    print("oldPath: " + oldPath)

    #else see if we can use an older result
    #TODO: Problem: variable values might change
    for i in range(len(self.newKnowladge)-1, 0, -1):
      if self.IsPathInResult(self.newKnowladge[i], oldPath, operation):
#        print("assert needs to move up to " + str(i) + " call opertion.")
        self.moveAssertUp(newTestJson, i)
        return
      
    raise Exception("could not update AssertListLength")


  #TODO: merge with updateAssertListLength
  def updateAssertIsInList(self, newTestJson):
    operation = newTestJson["operations"][-1]
    oldPath = operation["pathToList"]
    know = self.newKnowladge[-1]
    know = json.JSONDecoder().decode(know)
    know = json.dumps(know, indent=2, sort_keys=True)
#    print("oldPath: " + oldPath)
    
    operation["path"] = operation["pathToList"]

    #else see if we can use an older result
    #TODO: Problem: variable values might change
    for i in range(len(self.newKnowladge)-1, 0, -1):
      if self.IsPathInResult(self.newKnowladge[i], oldPath, operation):
#        print("assert needs to move up to " + str(i) + " call opertion.")
        self.moveAssertUp(newTestJson, i)
        operation["pathToList"] = operation["path"]
        del operation["path"]
        return
      
    raise Exception("could not update AssertIsInList")

  #TODO: merge with updateAssertListLength
  def updateFieldNotInResult(self, newTestJson):
    operation = newTestJson["operations"][-1]
    oldPath = operation["basePath"]
    know = self.newKnowladge[-1]
    know = json.JSONDecoder().decode(know)
    know = json.dumps(know, indent=2, sort_keys=True)
#    print("oldPath: " + oldPath)
    
    operation["path"] = operation["basePath"]

    #else see if we can use an older result
    #TODO: Problem: variable values might change
    for i in range(len(self.newKnowladge)-1, 0, -1):
      if self.IsPathInResult(self.newKnowladge[i], oldPath, operation):
#        print("assert needs to move up to " + str(i) + " call opertion.")
        self.moveAssertUp(newTestJson, i)
        operation["basPath"] = operation["path"]
        del operation["basPath"]
        return
      
    raise Exception("could not update AssertFieldNotInResult")

  #moves the last operation in newTestJson up, so that it is the last operation before a call with callId == callIndex+1
  def moveAssertUp(self, newTestJson, callIndex):
    candidatePosition = -1
    operations = newTestJson["operations"]
    for i in range(0, len(operations)):
      operation = newTestJson["operations"][i]
      if "callId" not in operation:
        candidatePosition = i
      elif operation["callId"] == callIndex + 1:
        break
    operationToMove = operations[-1]
    del operations[-1]
    operations.insert(candidatePosition, operationToMove)


  def IsPathInResult(self, knowladge, path, operation):
    print("searching know for path: " + path)
    idsToFind = self.determineIdToFind(path)
    print("idsToFind:" + str(idsToFind))
    for i in range(len(idsToFind) -1, 0, -1):
      idToFind = idsToFind[i]
      idToFind = idToFind[3:] #remove id=
      varToFind = idToFind
      idToFind = self.genVarMapping[idToFind] #translate from old test variable name to generated variable name
      idToFind = self.oldKnowlage[idToFind] #translate to value
      #print("idToFind: " +  str(idToFind))
      know = json.JSONDecoder().decode(knowladge)
    
      newPath = operation["path"][operation["path"].rfind("id=" + varToFind):]
    

      if "updates" not in know:
        return False

      candidates = know["updates"]
      candidates = candidates["$list"]
      for candidate in candidates:
        if "id" in candidate:
          #print("candidate Id: " + str(candidate["id"]))
          if candidate["id"] == idToFind:
            operation["path"] = "updates/$list[" + newPath
            print("updating path to " + operation["path"])
            print("value: " + str(json.dumps(candidate, indent=2, sort_keys=True)))
            return True
    return False


  def determineIdToFind(self, path):
    regex = re.compile('.*?\[(id=.*?)\]', re.IGNORECASE) #finds all id=<value>
    result = re.findall(regex, path)
    
    return result


  def doesTestWork(self, newTestJson):
    tmpTest = self.createTmpTestFile(newTestJson)
    output = self.callAutoTester(tmpTest)
    if self.testCaseSucceeded(output):
      return True
    return False  
    

  def updateRequired(self, oldTestPart, newTestJson):
    for operation in oldTestPart:
      newTestJson["operations"].append(operation)
    return not self.doesTestWork(newTestJson)

    
  #last operation in oldTestPart is a read operation.
  #append all operations in oldTestPart to newTestJson, except for the last
  #check if it still works in the new environment
  #if it still works, append it
  #otherwise => updateReadOperation
  def updateTestPartRead(self, oldTestPart, newTestJson):
    if not self.updateRequired(oldTestPart, newTestJson):
      return
    
    #try to find read variable elsewhere in response, or try to move read up
    #if read cannot be updated, create exception and write testcase -> check error in update?
    readOperation = oldTestPart[-1]
    genVarName = self.genVarMapping[readOperation["targetVariable"]]
    oldValue = self.oldKnowlage[genVarName]
    
    self.checkValueStillInResults(oldValue)
    
    #newCallIndex = self.getKnowladgeIndexForValue(oldValue)
    #newPath = self.getPathInKnowladge(newCallIndex, oldValue)

    readOperation["path"] = readOperation["pathInResponse"]
    found = False
    for i in range(len(self.newKnowladge)-1, 0, -1):
      if self.IsPathInResult(self.newKnowladge[i], readOperation["pathInResponse"], readOperation):
        newCallIndex = i
        readOperation["pathInResponse"] = readOperation["path"]
        newPath = readOperation["pathInResponse"]
        found = True
        break

    del readOperation["path"]
    
    if not found:
      raise Exception("could not find: " + oldValue + " for " + readOperation["targetVariable"])

    
#    print("problematic variable: " + readOperation["targetVariable"])
#    print("problematic path in response: " + readOperation["pathInResponse"])
#    print("oldValue: " + oldValue)
#    print("newPath: " + newPath)
#    print("newIndex: " + str(newCallIndex))
    
#    newTestJson["operations"][-1]["pathInResponse"] = newPath
    self.moveReadUpIfNecessary(newTestJson, newCallIndex)


  #raises Exception, if oldValue is not in newKnowladge
  def checkValueStillInResults(self, oldValue):
    for know in self.newKnowladge:
      if oldValue in str(know):
#        print("value still somewhere")
        return
        
    print("Value no longer in results: " + oldValue)
#    for know in self.newKnowladge:
#      print("candidate" + str(know))
#    raise Exception("Value no longer in results: " + oldValue)

       
  #if last operation needs to move up, it will move up.
  def moveReadUpIfNecessary(self, newTestJson, newCallIndex):
    operations = newTestJson["operations"]
    currentCallIndex = -1
    moveUpCandidateIndex = -1
    moveUp = False
    for i in range(0, len(operations)):
      operation = operations[i]
      if "callId" in operation:
        currentCallIndex = operation["callId"]
        if currentCallIndex == newCallIndex:
          moveCandidateIndex = i + 1 #we could move up to here
        if currentCallIndex > newCallIndex:
          moveUp = True
          break
          
    if moveUp:
#      print("we have to move up!")
      operation = operations[-1]
      del operations[-1]
      operations.insert(moveUpCandidateIndex, operation)
          
    

  def getKnowladgeIndexForValue(self, value):
    for i in range(len(self.knowlage)-1, 0, -1): #TODO: missing one?
     know = self.knowlage[i]
     for key in know:
       if know[key] == value:
         return i
       else:
         print(str(value) + " - not this: " + str(know[key]))
    raise Exception("did not find " + value + " in knowlage")


  def getPathInKnowladge(self, callIndex, value):
    for key in self.knowlage[callIndex].keys():
      if self.knowlage[callIndex][key] == value:
        return key
    raise Exception("did not find " + value + " in knowlage " + str(callIndex))

    
  #TODO: merge with readVariableValue
  #execute part of new Test. Read result and add it to NewKnowladge
  def gatherNewKnowladge(self, newTestJson):
    tmpTest = self.createTmpTestFile(newTestJson, "newKnowTest_")
    (testOutput, err) = self.callAutoTester(tmpTest)
  
    variableName = self.newKnowladgeVarName
    regex = re.compile('.*\nprint: !' + variableName + "!:\n(.*)\nprint: !" +self.emptyVarName + "!:", re.IGNORECASE)
    result = re.findall(regex, testOutput)
    
    if len(result) == 0:
      print("variableName: " + variableName)
      print("output: " + testOutput)
      raise Exception("error reading result.")
    
    self.newKnowladge.append(result[0])

    result = json.JSONDecoder().decode(result[0])
    self.addResponseObjToKnowlage(True, "", result, None, None)
    
    
  #returns variable value from autoTester output
  def readVariableValue(self, output, variableId):
    variableName = self.getGenVarName(variableId)
    regex = re.compile('.*\nprint: !' + variableName + "!:\n(.*)\nprint: !" +self.emptyVarName + "!:", re.IGNORECASE)
    result = re.findall(regex, output)
    if len(result) == 0:
      print("regex: " + str(regex))
      print("output: " + output)
      raise Exception("Could not read Variable.")
    return result[0]

    
  #executes testcase and returns output
  def callAutoTester(self, testcase):
    testSeries = {"tests":[testcase] }
    testSeriesFile = self.createTmpTestFile(testSeries, "testseries_")
    return self.executeProgram("python", [self.autoTesterPath, testSeriesFile])
    
    
  #returns program output
  def executeProgram(self, program, arguments):
    arguments.insert(0, program)
    p = subprocess.Popen(arguments, stdout = subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    return p.communicate() #(result, error)
   

  
  #returns path to tmp test
  def createTmpTestFile(self, testJson, prefix = ""):
    tmpFileName = prefix + "tempTestNew.json"
    f = open(tmpFileName, "w")
    content = json.dumps(testJson, indent=2, sort_keys=True)
    f.write(content)
    f.close()
    return tmpFileName
   
   
  # returns True, if test was executed successfully
  def testCaseSucceeded(self, output):
    (out, err) = output
    return err == None or len(err) == 0 and "Fails: 1" not in out

  
  #does not include "!"
  def getGenVarName(self, index):
    return self.generatedVarNameBase + str(index)

    
  #transelate from oldVarName (!someThing!) to value (someThing)
  def findVariableOfOldValue(self, oldVariable):
    genVarName = self.genVarMapping[oldVariable]
    oldValue = self.oldKnowladge[genVarName]
    return oldValue


#######
  def createReadOperation(self, variable, path, readId, indexInKnowladge, unquote = True): #TODO: pass old value and only unquoteResult if it is a baseString
    readOp = {}
    readOp["operation"] = "read"
    readOp["pathInResponse"] = path
    readOp["targetVariable"] = "!" + variable + "!"
    readOp["readId"] = readId
    readOp["callIdRef"] = indexInKnowladge
    if unquote:
      readOp["unquoteResult"] = True
    return readOp
    
    
  def createPrintOperation(self, variable):
    printOp = {}
    printOp["operation"] = "print"
    printOp["variable"] = "!" + variable + "!"
    return printOp
#######



########

  def addResponseObjToKnowlage(self, newEntry, path, obj, parent, gparent):
    if newEntry:
      self.knowlage.append({})
      
    pathPrefix = "" if len(path) == 0 else "/"
    
    if isinstance(obj, dict):
      for key in obj:
        self.addResponseObjToKnowlage(False, path + pathPrefix + key, obj[key], obj, parent)    
    elif isinstance(obj, list):
      for i in range(0,len(obj)):
        index = self.createIndex(obj, i)
        if isinstance(index, int):
          index = str(index)
        self.addResponseObjToKnowlage(False, path + "[" + index + "]", obj[i], obj, parent)
    else:
      #actually add knowlage
      ob = obj
      try:
        ob = str(obj)
      except Exception:
        ob = obj.encode('utf-8')
        ob = str(ob)
        ob = ob.decode('unicode-escape')
      path = self.removeSelfFromPath(ob, path, parent, gparent)
      self.knowlage[len(self.knowlage)-1][path] = ob


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
    
  
  #should return toReplace, or an oldVarName. Never a genVarName!
  def replaceFromVariable(self, toReplace):
    for entry in self.oldKnowlage:
      if self.oldKnowlage[entry] == toReplace:
        for key in self.genVarMapping.keys():
          if self.genVarMapping[key] == entry:
            #result = key[1:-1] #-> remove "!"
            return key
    return toReplace


  #removes occurencies of =value] from path 
  def removeSelfFromPath(self, value, path, parent, gparent):
  
    #TODO:
    #we have id=!step6_input! in path, but we end with /id and that id points to step6_input => path needs to change!
    #we would catch that, if we replace !step6_input! with the value -> step6_input
    #->if value is a key in self.genVarMapping, then replace it with self.oldKnowladge value for that variable
    #if isinstance(value, basestring) and value.startswith("!"):
    #  print("maybe replace: " + value + " in " + path)
    #  oldValue = self.findVariableOfOldValue(value)
    #  path.replace(value, oldValue)
    #  print("replaced " + value + " with old value: " + oldValue)
    #else:
    #  print("what is value?: " + str(type(value)) + " - " + str(value))
  
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
    
    
########

  
###  
# main
###

if len(sys.argv) != 2:
  print("python testFileUpdater.py <testcaseToUpdate>")
  exit(1)

updater = TestFileUpdater()
newTestJson = updater.update(sys.argv[1])
print("\nDone.")
sys.exit(0)