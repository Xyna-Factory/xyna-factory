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
import os
import traceback
try:
    import StringIO
except ImportError:
    from io import StringIO


class Configurator:


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


  def ensureEnoughFactories(self, config):
   if "factories" not in config:
      config["factories"] = []

   numberOfFactories = len(config["factories"])

   if numberOfFactories < factoryId+1:
     factoriesToAdd = factoryId+1 - numberOfFactories
     for i in range(factoriesToAdd):
       self.addFactory(config)


  def configureTag(self, configFile, factoryId, key, value):
    config = self.readConfig(configFile)
    self.ensureEnoughFactories(config)

    tags = config["factories"][factoryId]["tags"]
    if tags == None or not isinstance(tags, dict):
      tags = {}
      config["factories"][factoryId]["tags"] = tags

    if value == None:
      tags.pop(key, None)
    else:
      tags[key] = value
    self.writeConfig(configFile, config)


  def configureFactory(self, configFile, factoryId, field, value):
    config = self.readConfig(configFile)
    self.ensureEnoughFactories(config)

    config["factories"][factoryId][field] = value
    self.writeConfig(configFile, config)


  def configureGlobalSetting(self, configFile, globalSetting, value):
    if globalSetting != "cookieFile" and globalSetting != "functions":
      raise Exception("unknown global setting: " + globalSetting)

    config = self.readConfig(configFile)
    config[globalSetting] = value

    self.writeConfig(configFile, config)


  def resetConfig(self, configFile):
    with open(configFile, "w") as f:
      f.write("{}")



#main
configurator = Configurator()

if len(sys.argv) < 2:
  print("use: " + str(sys.argv[0]) + " configFile factoryId field value OR " + str(sys.argv[0]) + " configFile globalSetting value OR " + str(sys.argv[0]) + " reset")
  sys.exit()

configFile = sys.argv[1]


if sys.argv[2] == "reset":
  configurator.resetConfig(configFile)
  sys.exit()

isConfigFactory = True
factoryId = -1

try:
  factoryId = int(sys.argv[2])
except Exception as e:
  isConfigFactory = False

if isConfigFactory:
  field = sys.argv[3]

  if field == "tags":
    key = sys.argv[4]
    value = sys.argv[5] if len(sys.argv) == 6 else None # None => remove
    configurator.configureTag(configFile, factoryId, key, value)
  else:
    value = sys.argv[4]
    configurator.configureFactory(configFile, factoryId, field, value)
else:
  globalSetting = sys.argv[2]
  value = sys.argv[3]
  configurator.configureGlobalSetting(configFile, globalSetting, value)
