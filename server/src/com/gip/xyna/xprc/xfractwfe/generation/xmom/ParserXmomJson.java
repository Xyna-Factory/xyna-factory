/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */

package com.gip.xyna.xprc.xfractwfe.generation.xmom;

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;


public class ParserXmomJson {

  public XmomTree build(String json) {
    JsonElement element = JsonParser.parseString(json);
    JsonObject root = element.getAsJsonObject();
    return build(root);
  }
  
  
  public XmomTree build(JsonObject root) {
    if (root.entrySet().size() != 1) {
      throw new IllegalArgumentException("Could not parse xmom json.");
    }
    IdMapping idMapping = new IdMapping();
    XmomNodeInfo info = null;
    for (Entry<String, JsonElement> entry : root.entrySet()) {
      String childName = entry.getKey();
      JsonElement elem = entry.getValue();
      if (!elem.isJsonObject()) {
        throw new IllegalArgumentException("Could not parse xmom json.");
      }
      info = parseObject(childName, elem.getAsJsonObject(), idMapping);
    }
    return new XmomTree(info);
  }
  
  
  private XmomNodeInfo parseObject(String name, JsonObject obj, IdMapping idMapping) {
    XmomNodeInfo ret = new XmomNodeInfo(name, idMapping);
    for (Entry<String, JsonElement> entry : obj.entrySet()) {
      String childName = entry.getKey();
      JsonElement elem = entry.getValue();
      if (elem.isJsonArray()) {
        parseArray(ret, childName, elem.getAsJsonArray(), idMapping);
      } else if (elem.isJsonObject()) {
        XmomNodeInfo child = parseObject(childName, elem.getAsJsonObject(), idMapping);
        ret.addChild(child);
      } else if (elem.isJsonPrimitive()) {
        XmomNodeInfo child = parsePrimitive(childName, elem.getAsJsonPrimitive(), idMapping);
        ret.addChild(child);
      }
    }
    return ret;
  }
  
  
  private void parseArray(XmomNodeInfo parent, String name, JsonArray array, IdMapping idMapping) {
    for (JsonElement item : array) {
      if (item.isJsonObject()) {
        XmomNodeInfo child = parseObject(name, item.getAsJsonObject(), idMapping);
        parent.addChild(child);
      } else if (item.isJsonPrimitive()) {
        XmomNodeInfo child = parsePrimitive(name, item.getAsJsonPrimitive(), idMapping);
        parent.addChild(child);
      }
    }
  }
  
  
  private XmomNodeInfo parsePrimitive(String name, JsonPrimitive prim, IdMapping idMapping) {
    return new XmomNodeInfo(name, prim.getAsString(), idMapping);
  }
  
}
