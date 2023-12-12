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
import sys
import argparse


class Configurator:

  globalSecttings = {
    "debug": bool,
    "functions": str
  }

  factorySettings = {
    "ip": int,
    "port": bool,
    "username": str,
    "password": str,
    "https": bool,
    "cookieFile": str,
    "prefix": str
  }

  def readConfig(self, configFile):
    try:
      f = open(configFile)
      f.close()
    except IOError:
      f2 = open(configFile, "a")
      f2.write("{}")
      f2.close()

    with open(configFile, "r") as jsonFile:
      config_json = json.load(jsonFile)
    return config_json


  def writeConfig(self, configFile, config):
    result = json.dumps(config, indent=2, sort_keys=True)
    with open(configFile, "wb") as file:
      file.write(result.encode('utf8'))


  def addFactory(self, configFile):
    configFile["factories"].append({})


  def ensureEnoughFactories(self, config, factoryId):
   if "factories" not in config:
      config["factories"] = []

   numberOfFactories = len(config["factories"])

   if numberOfFactories < factoryId+1:
     factoriesToAdd = factoryId+1 - numberOfFactories
     for i in range(factoriesToAdd):
       self.addFactory(config)


  def configureTag(self, configFile, factoryId, key, value):
    config = self.readConfig(configFile)
    self.ensureEnoughFactories(config, factoryId)

    tags = config["factories"][factoryId].get("tags")
    if tags == None or not isinstance(tags, dict):
      tags = {}
      config["factories"][factoryId]["tags"] = tags

    if value == None:
      tags.pop(key, None)
    else:
      tags[key] = value
    self.writeConfig(configFile, config)


  def configureFactory(self, configFile, factoryId, field, value, strict):
    value = self.validate("factory setting", field, value, Configurator.factorySettings, strict)

    config = self.readConfig(configFile)
    self.ensureEnoughFactories(config, factoryId)

    config["factories"][factoryId][field] = value
    self.writeConfig(configFile, config)


  def validate(self, settingType, key, value, values, strict):
    if key not in values:
      message = f"Unknown {settingType} key: {key}. Options: {values}"
      if strict:
        raise Exception(message)
      print(message)
      return value

    try:
      if values[key] == bool:
        return value.lower() == "true"
      else:
        return values[key](value)
    except Exception:
      message = f"Unexpected value type for {settingType} key: {key}. Got {type(value)} expected {values[key]}"
      if strict:
        raise Exception(message)
      print(message)
      return value

  def configureGlobalSetting(self, configFile, globalSetting, value, strict):
    value = self.validate("global setting", globalSetting, value, Configurator.globalSecttings, strict)

    config = self.readConfig(configFile)
    config[globalSetting] = value

    self.writeConfig(configFile, config)


  def resetConfig(self, configFile):
    with open(configFile, "w") as f:
      f.write("{}")

  def createExample(self, configFile):
    config = {
      "debug": False,
      "functions": "functions/allfunctions.json",
      "factories": [
        {
          "ip": "127.0.0.1",
          "prefix": "modeller-api",
          "port": 443,
          "https": True,
          "password": "secret",
          "username": "user",
          "cookieFile": "cookies.txt"
        }
      ]
    }
    with open(configFile, "w") as f:
      f.write(json.dumps(config, indent=2, sort_keys=True))

def create_argparser():
  parser = argparse.ArgumentParser()
  parser.add_argument("-file", required=True)
  parser.add_argument("-factoryId", type=int)
  parser.add_argument("-key")
  parser.add_argument("-value")
  parser.add_argument("-strict", action='store_true')
  parser.add_argument("-reset", action='store_true')
  parser.add_argument("-example", action='store_true')
  parser.add_argument("-tagName")
  parser.add_argument("-tagValue")
  return parser


if __name__ == "__main__":
  configurator = Configurator()
  parser = create_argparser()
  args = parser.parse_args()

  if args.reset:
    configurator.resetConfig(args.file)
    sys.exit()

  if args.example:
    configurator.createExample(args.file)
    sys.exit()

  if args.factoryId is None:
    configurator.configureGlobalSetting(args.file, args.key, args.value, args.strict)
    sys.exit()

  if args.key is not None and args.value is not None:
    configurator.configureFactory(args.file, args.factoryId,  args.key, args.value, args.strict)

  if args.tagName is not None and args.tagValue is not None:
    configurator.configureTag(args.file, args.factoryId, args.tagName, args.tagValue)


