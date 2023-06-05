/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.xyna.utils.json.dom;

import java.util.ArrayList;

import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONObject;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValue;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValueType;


/**
 * Utility class to create json dom elements, without creating wrapper objects
 */
public class JsonBuilder {

  public JSONValue buildEmptyObject() {
    JSONValue ret = new JSONValue();
    ret.type = JSONValueType.OBJECT;
    ret.objectValue = new JSONObject();
    return ret;
  }

  public JSONValue buildEmptyArray() {
    JSONValue child = new JSONValue();
    child.type = JSONValueType.ARRAY;
    child.arrayValue = new ArrayList<JSONValue>();
    return child;
  }

  public JSONValue buildNullValue() {
    JSONValue child = new JSONValue();
    child.type = JSONValueType.NULL;
    child.stringOrNumberValue = null;
    return child;
  }


  public JSONValue buildStringValue(String val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.type = JSONValueType.STRING;
    child.stringOrNumberValue = val;
    return child;
  }

  public JSONValue buildBooleanValue(boolean val) {
    JSONValue child = new JSONValue();
    child.type = JSONValueType.BOOLEAN;
    child.booleanValue = val;
    return child;
  }

  public JSONValue buildBooleanValue(String val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.type = JSONValueType.BOOLEAN;
    child.booleanValue = ("true".equals(val));
    return child;
  }

  public JSONValue buildDoubleValue(Double val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.type = JSONValueType.NUMBER;
    child.stringOrNumberValue = val.toString();
    return child;
  }


  public JSONValue buildLongValue(Long val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.type = JSONValueType.NUMBER;
    child.stringOrNumberValue = val.toString();
    return child;
  }


  public JSONValue buildIntegerValue(Integer val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.type = JSONValueType.NUMBER;
    child.stringOrNumberValue = val.toString();
    return child;
  }


  public JSONValue buildNumberValue(String val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.type = JSONValueType.NUMBER;
    child.stringOrNumberValue = val;
    return child;
  }


  public void addToObject(JSONValue value, String name, JSONValue child) {
    if ((value == null) || (value.type != JSONValueType.OBJECT)) {
      throw new RuntimeException("JsonValue does not contain a json object.");
    }
    if (value.objectValue == null) {
      value.objectValue = new JSONObject();
    }
    value.objectValue.objects.put(name, child);
  }


  public void addToArray(JSONValue value, JSONValue child) {
    if ((value == null) || (value.type != JSONValueType.ARRAY)) {
      throw new RuntimeException("JsonValue does not contain a json array.");
    }
    if (value.arrayValue == null) {
      value.arrayValue = new ArrayList<JSONValue>();
    }
    value.arrayValue.add(child);
  }

}
