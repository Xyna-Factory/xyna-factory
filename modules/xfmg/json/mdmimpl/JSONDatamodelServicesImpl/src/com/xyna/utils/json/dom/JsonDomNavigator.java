/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import xfmg.xfctrl.datamodel.json.impl.JSONParser;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONObject;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValue;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValueType;


/**
 * Utility class to navigate through a json dom tree, with one single navigator object
 */
public class JsonDomNavigator {

  private final JSONValue _root;
  private final List<JSONValue> _path = new ArrayList<>();


  public JsonDomNavigator() {
    JSONValue value = new JSONValue();
    JSONObject obj = new JSONObject();
    value.objectValue = obj;
    value.type = JSONValueType.OBJECT;
    this._root = value;
  }

  public JsonDomNavigator(JSONValue root) {
    this._root = root;
    this._path.add(root);
  }

  public JsonDomNavigator(JsonBuildValue val) {
    this(val.getValue());
  }

  public JsonDomNavigator(JSONValue root, List<JSONValue> path) {
    this._root = root;
    for (JSONValue item : path) {
      this._path.add(item);
    }
  }

  public static JsonDomNavigator parseJsonString(String json) {
    if (json == null) {
      return new JsonDomNavigator();
    }
    JSONParser parser = new JSONParser(json);
    JSONObject obj = new JSONObject();
    parser.fillObject(new JSONTokenizer().tokenize(json), 0, obj);
    JSONValue value = new JSONValue();
    value.objectValue = obj;
    value.type = JSONValueType.OBJECT;
    return new JsonDomNavigator(value);
  }


  public JSONValue getRoot() {
    return _root;
  }

  public boolean isNavigatorPositionUndefined() {
    return (_path.size() < 1);
  }

  private void setUndefined() {
    _path.clear();
  }

  public JSONValue getCurrent() {
    if (isNavigatorPositionUndefined()) { return null; }
    return _path.get(_path.size() - 1);
  }

  public JsonBuildValue getCurrentAsJsonBuildValue() {
    if (isNavigatorPositionUndefined()) { return null; }
    JSONValue val = _path.get(_path.size() - 1);
    return new JsonBuildValue(val);
  }

  public JSONValueType getValueType() {
    if (isNavigatorPositionUndefined()) { return null; }
    return getCurrent().type;
  }

  public JsonDomNavigator gotoRoot() {
    _path.clear();
    this._path.add(_root);
    return this;
  }

  public JsonDomNavigator gotoParent() {
    if (_path.size() <= 1) {
      setUndefined();
    }
    else {
      _path.remove(_path.size() - 1);
    }
    return this;
  }


  public boolean isJsonNull() {
    if (isNavigatorPositionUndefined()) { return false; }
    return (getValueType() == JSONValueType.NULL);
  }

  public boolean isJsonObject() {
    if (isNavigatorPositionUndefined()) { return false; }
    return (getValueType() == JSONValueType.OBJECT);
  }

  public boolean isJsonArray() {
    if (isNavigatorPositionUndefined()) { return false; }
    return (getValueType() == JSONValueType.ARRAY);
  }

  public boolean isJsonString() {
    if (isNavigatorPositionUndefined()) { return false; }
    return (getValueType() == JSONValueType.STRING);
  }

  public boolean isJsonNumber() {
    if (isNavigatorPositionUndefined()) { return false; }
    return (getValueType() == JSONValueType.NUMBER);
  }

  public boolean isJsonBoolean() {
    if (isNavigatorPositionUndefined()) { return false; }
    return (getValueType() == JSONValueType.BOOLEAN);
  }

  public boolean isJsonPrimitive() {
    return (isJsonString() || isJsonNumber() || isJsonBoolean());
  }


  protected boolean isJsonObject(JSONValue val) {
    return (val.type == JSONValueType.OBJECT);
  }

  protected boolean isJsonArray(JSONValue val) {
    return (val.type == JSONValueType.ARRAY);
  }


  public JsonDomNavigator descendInObject(String field) {
    if (isNavigatorPositionUndefined()) { return this; }
    if (!isJsonObject()) {
      setUndefined();
    }
    JSONObject obj = getCurrent().objectValue;
    if (obj == null) {
      setUndefined();
      return this;
    }
    JSONValue next = obj.objects.get(field);
    if (next == null) {
      setUndefined();
    }
    else {
      _path.add(next);
    }
    return this;
  }


  public JsonDomNavigator descendInArray(int index) {
    if (!isJsonArray()) {
      setUndefined();
      return this;
    }
    if ((getCurrent().arrayValue == null) || (index >= getCurrent().arrayValue.size())) {
      setUndefined();
      return this;
    }
    JSONValue next = getCurrent().arrayValue.get(index);
    if (next == null) {
      setUndefined();
    }
    else {
      _path.add(next);
    }
    return this;
  }


  public List<JsonDomNavigator> getAllChildren() {
    List<JsonDomNavigator> ret = new ArrayList<>();
    if (this.isJsonArray()) {
      for (int i = 0; i < getArraySize(); i++) {
        JsonDomNavigator nav = this.clone().descendInArray(i);
        ret.add(nav);
      }
    }
    else if (this.isJsonObject()) {
      for (String field : this.getFieldNames()) {
        JsonDomNavigator nav = this.clone().descendInObject(field);
        ret.add(nav);
      }
    }
    return ret;
  }


  public int getArraySize() {
    if (!isJsonArray()) {
      return -1;
    }
    if (getCurrent().arrayValue == null) {
      return -1;
    }
    return getCurrent().arrayValue.size();
  }


  public Set<String> getFieldNames() {
    if (!isJsonObject()) {
      return new HashSet<String>();
    }
    return getCurrent().objectValue.objects.keySet();
  }


  public String asString() {
    if (isNavigatorPositionUndefined()) {
      return "UNDEFINED";
    }
    if (isJsonNumber() || isJsonString()) {
      return getCurrent().stringOrNumberValue;
    }
    else if (isJsonNull()) {
      return null;
    }
    else if (isJsonBoolean()) {
      return "" + getCurrent().booleanValue;
    }
    return getCurrent().toJSON("  ");
  }

  public double asDouble() {
    if (isJsonNumber() || isJsonString()) {
      try {
        return Double.parseDouble(getCurrent().stringOrNumberValue);
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
    return getCurrent().booleanValue;
  }

  public int asInt() {
    if (isJsonNumber() || isJsonString()) {
      try {
        return Integer.parseInt(getCurrent().stringOrNumberValue);
      } catch (Exception e) {
        return 0;
      }
    }
    return 0;
  }

  public long asLong() {
    if (isJsonNumber() || isJsonString()) {
      try {
        return Long.parseLong(getCurrent().stringOrNumberValue);
      } catch (Exception e) {
        return 0L;
      }
    }
    return 0L;
  }

  @Override
  public String toString() {
    return asString();
  }


  public JsonDomNavigator clone() {
    return new JsonDomNavigator(this._root, this._path);
  }

}
