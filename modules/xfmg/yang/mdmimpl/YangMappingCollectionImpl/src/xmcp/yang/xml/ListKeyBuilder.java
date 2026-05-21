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

package xmcp.yang.xml;


public class ListKeyBuilder {

  private String _elementName;
  private String _value;
  private String _namespace;
  
  
  public ListKey build() {
    return new ListKey(_elementName, _value, _namespace);
  }
  
  public ListKeyBuilder listKeyElemName(String elementName) {
    this._elementName = elementName;
    return this;
  }
  
  public ListKeyBuilder listKeyValue(String value) {
    this._value = value;
    return this;
  }

  public ListKeyBuilder listKeyNamespace(String nsp) {
    this._namespace = nsp;
    return this;
  }
  
}
