/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
import java.util.List;

import xfmg.xfctrl.datamodel.json.JSONKeyValue;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONVALTYPES;


/**
 * Utility class to create json dom elements, without creating wrapper objects
 */
public class JsonBuilder {

  public JSONValue buildEmptyObject() {
    JSONValue ret = new JSONValue();
    ret.unversionedSetType(JSONVALTYPES.OBJECT);
    ret.unversionedSetObjectValue(new JSONObject());
    return ret;
  }

  public JSONValue buildEmptyArray() {
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.ARRAY);
    child.unversionedSetArrayValue(new ArrayList<JSONValue>());
    return child;
  }

  public JSONValue buildNullValue() {
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.NULL);
    return child;
  }


  public JSONValue buildStringValue(String val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.STRING);
    child.unversionedSetStringOrNumberValue(val);
    return child;
  }

  public JSONValue buildBooleanValue(boolean val) {
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.BOOLEAN);
    child.unversionedSetBooleanValue(val);
    return child;
  }

  public JSONValue buildBooleanValue(String val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.BOOLEAN);
    child.unversionedSetBooleanValue("true".equals(val));
    return child;
  }

  public JSONValue buildDoubleValue(Double val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.NUMBER);
    child.unversionedSetStringOrNumberValue(val.toString());
    return child;
  }


  public JSONValue buildLongValue(Long val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.NUMBER);
    child.unversionedSetStringOrNumberValue(val.toString());
    return child;
  }


  public JSONValue buildIntegerValue(Integer val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.NUMBER);
    child.unversionedSetStringOrNumberValue(val.toString());
    return child;
  }


  public JSONValue buildNumberValue(String val) {
    if (val == null) {
      return buildNullValue();
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.NUMBER);
    child.unversionedSetStringOrNumberValue(val.toString());
    return child;
  }


  public void addToObject(JSONValue value, String name, JSONValue child) {
    if ((value == null) || (value.getType() != JSONVALTYPES.OBJECT)) {
      throw new RuntimeException("JsonValue does not contain a json object.");
    }
    if (value.getObjectValue() == null) {
      value.unversionedSetObjectValue(new JSONObject());
    }
    List<? extends JSONKeyValue> oldList = value.getObjectValue().getMembers();
    List<JSONKeyValue> list = oldList == null ? new ArrayList<>() : new ArrayList<>(oldList);
    list.add(new JSONKeyValue(name, child));
    value.getObjectValue().unversionedSetMembers(list);
  }


  public void addToArray(JSONValue value, JSONValue child) {
    if ((value == null) || (value.getType() != JSONVALTYPES.ARRAY)) {
      throw new RuntimeException("JsonValue does not contain a json array.");
    }
    List<? extends JSONValue> oldList = value.getArrayValue();
    List<JSONValue> list = oldList == null ? new ArrayList<>() : new ArrayList<>(oldList);
    list.add(child);
    value.unversionedSetArrayValue(list);
  }

}
