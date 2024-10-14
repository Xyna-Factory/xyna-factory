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

package xfmg.xfctrl.datamodel.json.dom;

import xfmg.xfctrl.datamodel.json.impl.JSONParser;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONVALTYPES;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer;
import xfmg.xfctrl.datamodel.json.JSONObject;

@Deprecated
public class JsonDom {

  private final JsonDomElement _root;

  public JsonDom() {
    JSONValue val = new JSONValue();
    val.unversionedSetType(JSONVALTYPES.OBJECT);
    val.unversionedSetObjectValue(new JSONObject());
    _root = new JsonDomElement(val);
  }

  public JsonDom(String json) {
    JSONParser parser = new JSONParser(json);
    JSONObject obj = new JSONObject();
    parser.fillObject(new JSONTokenizer().tokenize(json), 0, obj);
    JSONValue value = new JSONValue();
    value.unversionedSetObjectValue(obj);
    value.unversionedSetType(JSONVALTYPES.OBJECT);
    _root = new JsonDomElement(value);
  }

  public JsonDomElement getRoot() {
    return _root;
  }


  @Override
  public String toString() {
    return _root.toString();
  }

}
