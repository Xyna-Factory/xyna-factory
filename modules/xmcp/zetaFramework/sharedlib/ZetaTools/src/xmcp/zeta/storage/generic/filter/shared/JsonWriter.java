/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.zeta.storage.generic.filter.shared;


public class JsonWriter {

  private StringBuilder _str = new StringBuilder();
  private int _level = 0;
  
  
  public JsonWriter openListAttribute(String name) {
    newline();
    indent().addInQuotes(name).append(": [");
    newline();
    _level++;
    return this;
  }
  
  public JsonWriter closeList() {
    _level--;
    newline();
    indent().append("]");
    return this;
  }
  
  public JsonWriter continueList() {
    append(",");
    newline();
    return this;
  }
  
  public JsonWriter continueObject() {
    append(",");
    newline();
    return this;
  }
  
  public JsonWriter openObjectAttribute(String name) {
    indent().addInQuotes(name).append(": {");
    newline();
    _level++;
    return this;
  }
  
  
  public JsonWriter closeObject() {
    _level--;
    newline();
    indent().append("}");
    return this;
  }
  
  
  public JsonWriter addAttribute(String key, String value) {
    indent().addInQuotes(key).append(": ").addInQuotes(value);
    return this;
  }
  
  public JsonWriter newline() {
    _str.append("\n");
    return this;
  }
  
  
  public JsonWriter addInQuotes(String val) {
    _str.append("\"").append(val).append("\"");
    return this;
  }
  
  
  public JsonWriter indent() {
    for (int i = 0; i < _level; i++) {
      _str.append("  ");
    }
    return this;
  }
  
  public JsonWriter append(String val) {
    _str.append(val);
    return this;
  }
  
  public void clear() {
    _str.setLength(0);
  }
  
  @Override
  public String toString() {
    return _str.toString();
  }
  
}
