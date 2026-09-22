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
package com.gip.xyna.xact.filter.serialization;



import java.util.List;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonSerializable;

import xfmg.xfctrl.datamodel.json.JSONKeyValue;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;



public class JsonObjectSerializer implements JsonSerializable {

  private final JSONObject obj;


  public JsonObjectSerializer(JSONObject obj) {
    this.obj = obj;
  }


  @Override
  public void toJson(JsonBuilder jb) {
    if (obj.getMembers() == null) {
      return;
    }
    for (JSONKeyValue value : obj.getMembers()) {
      switch (value.getValue().getType()) {
        case "STRING" :
          jb.addStringAttribute(value.getKey(), value.getValue().getStringOrNumberValue());
          break;
        case "NUMBER" :
          if (value.getValue().getStringOrNumberValue().contains(".")) {
            jb.addNumberAttribute(value.getKey(), Double.valueOf(value.getValue().getStringOrNumberValue()));
          } else {
            jb.addNumberAttribute(value.getKey(), Long.valueOf(value.getValue().getStringOrNumberValue()));
          }
          break;
        case "BOOLEAN" :
          jb.addBooleanAttribute(value.getKey(), value.getValue().getBooleanValue());
          break;
        case "ARRAY" :
          jb.addListAttribute(value.getKey());
          List<? extends JSONValue> list = value.getValue().getArrayValue();
          for (JSONValue v : list) {
            new JsonValueSerializer(v).toJson(jb);
          }
          jb.endList();
          break;
        case "OBJECT" :
          jb.addObjectAttribute(value.getKey(), new JsonObjectSerializer(value.getValue().getObjectValue()));
          break;
        case "NULL" :
          break;
        default :
          break;
      }
    }
  }


  private static class JsonValueSerializer {

    private JSONValue value;


    public JsonValueSerializer(JSONValue value) {
      this.value = value;
    }


    public void toJson(JsonBuilder jb) {
      switch (value.getType()) {
        case "STRING" :
          jb.addStringListElement(value.getStringOrNumberValue());
          break;
        case "NUMBER" :
        case "BOOLEAN" :
          jb.addPrimitiveListElement(value.getStringOrNumberValue());
          break;
        case "ARRAY" :
          jb.startList();
          List<? extends JSONValue> list = value.getArrayValue();
          for (JSONValue v : list) {
            new JsonValueSerializer(v).toJson(jb);
          }
          jb.endList();
          break;
        case "OBJECT" :
          jb.startObject();
          new JsonObjectSerializer(value.getObjectValue()).toJson(jb);;
          jb.endObject();
          break;
        case "NULL" :
          break;
        default :
          break;
      }
    }
  }

}
