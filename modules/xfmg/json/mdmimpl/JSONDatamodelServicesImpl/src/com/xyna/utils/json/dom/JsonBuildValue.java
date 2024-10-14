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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import xfmg.xfctrl.datamodel.json.JSONKeyValue;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONVALTYPES;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValueWriter;


/**
 * Utility class to construct and represent a json dom element;
 * a JsonBuildValue-instance is a wrapper object intended to simplify accessing and modifying the json content;
 * the actual json data is contained in field _value of type JSONValue
 */
public class JsonBuildValue {

  private static final JsonBuilder _builder = new JsonBuilder();

  private final JSONValue _value;
  private final boolean _exists;

  public JsonBuildValue() {
    this(null);
  }

  public JsonBuildValue(JSONValue val) {
    if (val == null) {
      _value = new JSONValue();
      _value.unversionedSetType(JSONVALTYPES.NULL);
      _exists = false;
    }
    else {
      _value = val;
      _exists = true;
    }
  }

  public boolean existsInDom() {
    return _exists;
  }

  public JSONValue getValue() {
    return _value;
  }

  public String getValueType() {
    return _value.getType();
  }

  public boolean isJsonNull() {
    return JSONVALTYPES.NULL.equals(getValueType());
  }
  public boolean isJsonObject() {
    return JSONVALTYPES.OBJECT.equals(getValueType());
  }
  public boolean isJsonArray() {
    return JSONVALTYPES.ARRAY.equals(getValueType());
  }
  public boolean isJsonString() {
    return JSONVALTYPES.STRING.equals(getValueType());
  }
  public boolean isJsonNumber() {
    return JSONVALTYPES.NUMBER.equals(getValueType());
  }
  public boolean isJsonBoolean() {
    return JSONVALTYPES.BOOLEAN.equals(getValueType());
  }


  public JsonBuildValue getInObject(String field) {
    if (!isJsonObject()) {
      return new JsonBuildValue();
    }
    JSONObject obj = _value.getObjectValue();
    if (obj == null) {
      return new JsonBuildValue();
    }
    return new JsonBuildValue(obj.getMembers().stream().filter(x -> x.getKey().equals(field)).map(x -> x.getValue()).findFirst().get());
  }

  public JsonBuildValue getInArray(int index) {
    if (!isJsonArray()) {
      return new JsonBuildValue();
    }
    if ((_value.getArrayValue() == null) || (index >= _value.getArrayValue().size())) {
      return new JsonBuildValue();
    }
    return new JsonBuildValue(_value.getArrayValue().get(index));
  }

  public int getArraySize() {
    if (!isJsonArray()) {
      return -1;
    }
    if (_value.getArrayValue() == null) {
      return -1;
    }
    return _value.getArrayValue().size();
  }

  public Set<String> getFieldNames() {
    if (!isJsonObject()) {
      return new HashSet<String>();
    }
    return _value.getObjectValue().getMembers().stream().map(x -> x.getKey()).collect(Collectors.toSet());
  }

  public String asString() {
    if (isJsonNumber() || isJsonString()) {
      return _value.getStringOrNumberValue();
    }
    else if (isJsonNull()) {
      return null;
    }
    else if (isJsonBoolean()) {
      return "" + _value.getBooleanValue();
    }
    return JSONValueWriter.toJSON("  ",  _value);
  }

  public double asDouble() {
    if (isJsonNumber() || isJsonString()) {
      try {
        return Double.parseDouble(_value.getStringOrNumberValue());
      } catch (Exception e) {
        return 0;
      }
    }
    return 0;
  }

  public boolean asBoolean() {
    if (!isJsonBoolean()) {
      return false;
    }
    return _value.getBooleanValue();
  }

  public int asInt() {
    if (isJsonNumber() || isJsonString()) {
      try {
        return Integer.parseInt(_value.getStringOrNumberValue());
      } catch (Exception e) {
        return 0;
      }
    }
    return 0;
  }


  public void addToObject(String name, JsonBuildValue child) {
    _builder.addToObject(this._value, name, child.getValue());
  }


  public void addToArray(JsonBuildValue child) {
    _builder.addToArray(this._value, child.getValue());
  }


  private void addToObject(String name, JSONValue child) {
    if ((this._value == null) || (this._value.getType().equals(JSONVALTYPES.OBJECT))) {
      throw new RuntimeException("JsonBuildValue does not contain a json object.");
    }
    if (this._value.getObjectValue() == null) {
      this._value.unversionedSetObjectValue(new JSONObject());
    }
    List<? extends JSONKeyValue> oldList = this._value.getObjectValue().getMembers();
    List<JSONKeyValue> list = oldList == null ? new ArrayList<>() : new ArrayList<>(oldList);
    list.add(new JSONKeyValue(name, child));
    this._value.getObjectValue().unversionedSetMembers(list);
  }


  public static JsonBuildValue createEmptyObject() {
    JSONValue val = _builder.buildEmptyObject();
    return new JsonBuildValue(val);
  }

  public static JsonBuildValue createEmptyArray() {
    JSONValue val = _builder.buildEmptyArray();
    return new JsonBuildValue(val);
  }

  public static JsonBuildValue createNullValue() {
    JSONValue val = _builder.buildNullValue();
    return new JsonBuildValue(val);
  }


  public static JsonBuildValue createStringValue(String inp) {
    JSONValue val = _builder.buildStringValue(inp);
    return new JsonBuildValue(val);
  }

  public static JsonBuildValue createBooleanValue(boolean inp) {
    JSONValue val = _builder.buildBooleanValue(inp);
    return new JsonBuildValue(val);
  }

  public static JsonBuildValue createBooleanValue(String inp) {
    JSONValue val = _builder.buildBooleanValue(inp);
    return new JsonBuildValue(val);
  }

  public static JsonBuildValue createDoubleValue(Double inp) {
    JSONValue val = _builder.buildDoubleValue(inp);
    return new JsonBuildValue(val);
  }

  public static JsonBuildValue createLongValue(Long inp) {
    JSONValue val = _builder.buildLongValue(inp);
    return new JsonBuildValue(val);
  }

  public static JsonBuildValue createIntegerValue(Integer inp) {
    JSONValue val = _builder.buildIntegerValue(inp);
    return new JsonBuildValue(val);
  }

  public static JsonBuildValue createNumberValue(String inp) {
    JSONValue val = _builder.buildNumberValue(inp);
    return new JsonBuildValue(val);
  }

  public JsonBuildValue createArrayField(String name) {
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.ARRAY);
    child.unversionedSetArrayValue(new ArrayList<JSONValue>());
    addToObject(name, child);
    return new JsonBuildValue(child);
  }

  public JsonBuildValue createObjectField(String name) {
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.OBJECT);
    child.unversionedSetObjectValue(new JSONObject());
    addToObject(name, child);
    return new JsonBuildValue(child);
  }

  public void createNullField(String name) {
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.NULL);
    addToObject(name, child);
  }

  public void createStringField(String name, String val) {
    if (val == null) {
      createNullField(name);
      return;
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.STRING);
    child.unversionedSetStringOrNumberValue(val);
    addToObject(name, child);
  }

  public void createIntegerField(String name, Integer val) {
    if (val == null) {
      createNullField(name);
      return;
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.NUMBER);
    child.unversionedSetStringOrNumberValue(val.toString());
    addToObject(name, child);
  }

  public void createLongField(String name, Long val) {
    if (val == null) {
      createNullField(name);
      return;
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.NUMBER);
    child.unversionedSetStringOrNumberValue(val.toString());
    addToObject(name, child);
  }

  public void createDoubleField(String name, Double val) {
    if (val == null) {
      createNullField(name);
      return;
    }
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.NUMBER);
    child.unversionedSetStringOrNumberValue(val.toString());
    addToObject(name, child);
  }

  public void createBooleanField(String name, boolean val) {
    JSONValue child = new JSONValue();
    child.unversionedSetType(JSONVALTYPES.BOOLEAN);
    child.unversionedSetBooleanValue(val);
    addToObject(name, child);
  }

  public JsonBuildValue addObjectToArray() {
    JSONValue next = new JSONValue();
    next.unversionedSetType(JSONVALTYPES.OBJECT);
    next.unversionedSetObjectValue(new JSONObject());
    addToArray(next);
    return new JsonBuildValue(next);
  }

  public JsonBuildValue addArrayToArray() {
    JSONValue next = new JSONValue();
    next.unversionedSetType(JSONVALTYPES.ARRAY);
    next.unversionedSetArrayValue(new ArrayList<JSONValue>());
    addToArray(next);
    return new JsonBuildValue(next);
  }

  public void addStringToArray(String val) {
    if (val == null) {
      addNullToArray();
      return;
    }
    JSONValue next = new JSONValue();
    next.unversionedSetType(JSONVALTYPES.STRING);
    next.unversionedSetStringOrNumberValue(val);
    addToArray(next);
  }

  public void addIntegerToArray(Integer val) {
    if (val == null) {
      addNullToArray();
      return;
    }
    JSONValue next = new JSONValue();
    next.unversionedSetType(JSONVALTYPES.NUMBER);
    next.unversionedSetStringOrNumberValue(val.toString());
    addToArray(next);
  }

  public void addDoubleToArray(Double val) {
    if (val == null) {
      addNullToArray();
      return;
    }
    JSONValue next = new JSONValue();
    next.unversionedSetType(JSONVALTYPES.NUMBER);
    next.unversionedSetStringOrNumberValue(val.toString());
    addToArray(next);
  }

  public void addBooleanToArray(boolean val) {
    JSONValue next = new JSONValue();
    next.unversionedSetType(JSONVALTYPES.BOOLEAN);
    next.unversionedSetBooleanValue(val);
    addToArray(next);
  }

  public void addNullToArray() {
    JSONValue next = new JSONValue();
    next.unversionedSetType(JSONVALTYPES.NULL);
    addToArray(next);
  }


  private void addToArray(JSONValue next) {
    if ((this._value == null) || (!JSONVALTYPES.ARRAY.equals(this._value.getType()))) {
      throw new RuntimeException("JsonBuildValue does not contain a json array.");
    }
    List<? extends JSONValue> oldList = this._value.getArrayValue();
    List<JSONValue> list = oldList == null ? new ArrayList<>() : new ArrayList<>(oldList);
    list.add(next);
    this._value.unversionedSetArrayValue(list);
  }


  @Override
  public String toString() {
    return asString();
  }

}
