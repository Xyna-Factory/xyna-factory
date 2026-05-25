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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomNodeInfo.XmomNodeInfoList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


public class WriterXmomJson {

  private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
  
  public String toJsonString(XmomTree tree) {
    JsonObject root = toJson(tree);
    //return root.toString();
    return gson.toJson(root);
  }
  
  
  public JsonObject toJson(XmomTree tree) {
    JsonObject root = new JsonObject();
    Optional<JsonElement> child = toJson(tree.getRoot());
    if (child.isPresent()) {
      root.add(tree.getRoot().getName(), child.get());
    }
    return root;
  }
  
  
  private Optional<JsonElement> toJson(XmomNodeInfo xmom) {
    if (!xmom.hasChildren()) {
      if (!xmom.hasValue()) {
        return Optional.empty();
      }
      JsonPrimitive ret = new JsonPrimitive(xmom.getValue().get());
      return Optional.ofNullable(ret);
    }
    JsonObject ret = new JsonObject();
    Map<String, XmomNodeInfoList> map = xmom.getChildMap();
    
    for (Entry<String, XmomNodeInfoList> entry : map.entrySet()) {
      String name = entry.getKey();
      XmomNodeInfoList list = entry.getValue();
      if (list.getList().size() < 1) { continue; }
      else if (list.getList().size() == 1) {
        Optional<JsonElement> childJson = toJson(list.getList().get(0));
        if (childJson.isPresent()) {
          ret.add(name, childJson.get());
        }
      } else {
        JsonArray array = new JsonArray();
        ret.add(name, array);
        handleChildren(list, array);
      }
    }
    return Optional.ofNullable(ret);
  }
  
  
  private void handleChildren(XmomNodeInfoList list, JsonArray json) {
    for (XmomNodeInfo info : list.getList()) {
      Optional<JsonElement> childJson = toJson(info);
      if (childJson.isPresent()) {
        json.add(childJson.get());
      }
    }
  }
  
}
