import sys
import os
import glob
import subprocess
import json


def notATestFile(file):
  with open(file, "r") as f:
    try:
      jsonFile = json.load(f)
      return "operations" not in jsonFile
    except Exception:
      print("Exception. - file: " + file)
      return True

def notAlreadyOptimized(file, files):
  return file[0:-5] + "_optimized.json" in files #optimized already      

#returns a list of paths to apps.
def findTests(path):
  result = []
  for root, dirs, files in os.walk(path):
    for candidate in dirs:
      files = os.listdir(os.path.join(root, candidate))
      for file in files:
        if not ".json" in file:
          continue
        if "testseries.json" in file:
          continue
        if "_optimized" in file:
          continue
        if notAlreadyOptimized(file, files):
          print("already optimized: " + file)
          continue
        completePath = os.path.join(os.path.join(root, candidate), file)
        if notATestFile(completePath):
          continue
        result.append(completePath)
  return result


def updateTest(path):
  arguments = ["python", "testFileUpdater.py", path]
  p = subprocess.Popen(arguments, stdout = subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
  (out, err) = p.communicate()
  print("out : " + out)
  print("err : " + err)
  if len(err) > 0:
    errors.append(path)



if len(sys.argv) != 2:
  print("python updateFolder.py <folderToUpdate>")
  sys.exit(1)  

folderToUpdate = sys.argv[1]
errors = []

testFiles = findTests(folderToUpdate)
print("testFile count: " + str(len(testFiles)))

for file in testFiles:
  print("update: " + file)
  updateTest(file)
  
print("done")

for error in errors:
  print("error: " + error)

sys.exit(0)
