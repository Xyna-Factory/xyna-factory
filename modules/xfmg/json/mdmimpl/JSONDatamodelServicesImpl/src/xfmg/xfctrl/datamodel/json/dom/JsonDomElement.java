/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package xfmg.xfctrl.datamodel.json.dom;

import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONObject;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValue;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValueType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Deprecated
public class JsonDomElement {

  private final JSONValue _value;
  private final boolean _exists;

  public JsonDomElement() {
    this(null);
  }

  public JsonDomElement(JSONValue val) {
    if (val == null) {
      _value = new JSONValue();
      _value.type = JSONValueType.NULL;
      _exists = false;
    } else {
      _value = val;
      _exists = true;
    }
  }

  public boolean existsInDom() {
    return _exists;
  }

  public JSONValueType getValueType() {
    return _value.type;
  }

  public boolean isJsonNull() {
    return (getValueType() == JSONValueType.NULL);
  }
  public boolean isJsonObject() {
    return (getValueType() == JSONValueType.OBJECT);
  }
  public boolean isJsonArray() {
    return (getValueType() == JSONValueType.ARRAY);
  }
  public boolean isJsonString() {
    return (getValueType() == JSONValueType.STRING);
  }
  public boolean isJsonNumber() {
    return (getValueType() == JSONValueType.NUMBER);
  }
  public boolean isJsonBoolean() {
    return (getValueType() == JSONValueType.BOOLEAN);
  }

  public JsonDomElement getInObject(String field) {
    if (!isJsonObject()) {
      return new JsonDomElement();
    }
    JSONObject obj = _value.objectValue;
    if (obj == null) {
      return new JsonDomElement();
    }
    return new JsonDomElement(obj.objects.get(field));
  }

  public JsonDomElement getInArray(int index) {
    if (!isJsonArray()) {
      return new JsonDomElement();
    }
    if ((_value.arrayValue == null) || (index >= _value.arrayValue.size())) {
      return new JsonDomElement();
    }
    return new JsonDomElement(_value.arrayValue.get(index));
  }

  public int getArraySize() {
    if (!isJsonArray()) {
      return -1;
    }
    if (_value.arrayValue == null) {
      return -1;
    }
    return _value.arrayValue.size();
  }

  public Set<String> getFieldNames() {
    if (!isJsonObject()) {
      return new HashSet<String>();
    }
    return _value.objectValue.objects.keySet();
  }

  public String asString() {
    if (isJsonNumber() || isJsonString()) {
      return _value.stringOrNumberValue;
    }
    else if (isJsonNull()) {
      return null;
    }
    else if (isJsonBoolean()) {
      return "" + _value.booleanValue;
    }
    return _value.toJSON("  ");
  }

  public double asDouble() {
    if (isJsonNumber() || isJsonString()) {
      try {
        return Double.parseDouble(_value.stringOrNumberValue);
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
    return _value.booleanValue;
  }

  public int asInt() {
    if (isJsonNumber() || isJsonString()) {
      try {
        return Integer.parseInt(_value.stringOrNumberValue);
      } catch (Exception e) {
        return 0;
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    return asString();
  }


  public JsonDomElement addObjectToArray() {
    JSONValue next = new JSONValue();
    next.type = JSONValueType.OBJECT;
    next.objectValue = new JSONObject();
    addToArray(next);
    return new JsonDomElement(next);
  }

  public JsonDomElement addArrayToArray() {
    JSONValue next = new JSONValue();
    next.type = JSONValueType.ARRAY;
    next.arrayValue = new ArrayList<JSONValue>();
    addToArray(next);
    return new JsonDomElement(next);
  }

  public void addStringToArray(String val) {
    if (val == null) {
      addNullToArray();
      return;
    }
    JSONValue next = new JSONValue();
    next.type = JSONValueType.STRING;
    next.stringOrNumberValue = val;
    addToArray(next);
  }

  public void addIntegerToArray(Integer val) {
    if (val == null) {
      addNullToArray();
      return;
    }
    JSONValue next = new JSONValue();
    next.type = JSONValueType.NUMBER;
    next.stringOrNumberValue = val.toString();
    addToArray(next);
  }

  public void addDoubleToArray(Double val) {
    if (val == null) {
      addNullToArray();
      return;
    }
    JSONValue next = new JSONValue();
    next.type = JSONValueType.NUMBER;
    next.stringOrNumberValue = val.toString();
    addToArray(next);
  }

  public void addBooleanToArray(boolean val) {
    JSONValue next = new JSONValue();
    next.type = JSONValueType.BOOLEAN;
    next.booleanValue = val;
    addToArray(next);
  }

  public void addNullToArray() {
    JSONValue next = new JSONValue();
    next.type = JSONValueType.NULL;
    addToArray(next);
  }


  private void addToArray(JSONValue next) {
    if ((this._value == null) || (this._value.type != JSONValueType.ARRAY)) {
      throw new RuntimeException("JsonDomElement does not contain a json array.");
    }
    if (this._value.arrayValue == null) {
      this._value.arrayValue = new ArrayList<JSONValue>();
    }
    this._value.arrayValue.add(next);
  }


  public JsonDomElement createArrayField(String name) {
    JSONValue child = new JSONValue();
    child.type = JSONValueType.ARRAY;
    child.arrayValue = new ArrayList<JSONValue>();
    addToObject(name, child);
    return new JsonDomElement(child);
  }

  public JsonDomElement createObjectField(String name) {
    JSONValue child = new JSONValue();
    child.type = JSONValueType.OBJECT;
    child.objectValue = new JSONObject();
    addToObject(name, child);
    return new JsonDomElement(child);
  }

  public void createNullField(String name) {
    JSONValue child = new JSONValue();
    child.type = JSONValueType.NULL;
    child.stringOrNumberValue = null;
    addToObject(name, child);
  }

  public void createStringField(String name, String val) {
    if (val == null) {
      createNullField(name);
      return;
    }
    JSONValue child = new JSONValue();
    child.type = JSONValueType.STRING;
    child.stringOrNumberValue = val;
    addToObject(name, child);
  }

  public void createIntegerField(String name, Integer val) {
    if (val == null) {
      createNullField(name);
      return;
    }
    JSONValue child = new JSONValue();
    child.type = JSONValueType.NUMBER;
    child.stringOrNumberValue = val.toString();
    addToObject(name, child);
  }

  public void createDoubleField(String name, Double val) {
    if (val == null) {
      createNullField(name);
      return;
    }
    JSONValue child = new JSONValue();
    child.type = JSONValueType.NUMBER;
    child.stringOrNumberValue = val.toString();
    addToObject(name, child);
  }

  public void createBooleanField(String name, boolean val) {
    JSONValue child = new JSONValue();
    child.type = JSONValueType.BOOLEAN;
    child.booleanValue = val;
    addToObject(name, child);
  }

  private void addToObject(String name, JSONValue child) {
    if ((this._value == null) || (this._value.type != JSONValueType.OBJECT)) {
      throw new RuntimeException("JsonDomElement does not contain a json object.");
    }
    if (this._value.objectValue == null) {
      this._value.objectValue = new JSONObject();
    }
    this._value.objectValue.objects.put(name, child);
  }

}
