import json
import sys


if len(sys.argv) != 2:
  print("Usage: python formatJson.py <unformattetJsonFile>")
  exit(1)

jsonFilePath = sys.argv[1]
jsonfile = open(jsonFilePath)
jsonfile_json = json.loads(jsonfile.read())
jsonfile.close()

output = json.dumps(jsonfile_json, indent=2, sort_keys=True)

f = open(jsonFilePath + "_formatted.json", "w")
f.write(output)
f.close()