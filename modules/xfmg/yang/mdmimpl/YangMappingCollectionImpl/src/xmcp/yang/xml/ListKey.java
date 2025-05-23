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


public class ListKey {

  private final String _elementName;
  private final String _value;
  
  public ListKey(String elementName, String value) {
    if (elementName == null) { throw new IllegalArgumentException("ListKey: elementName is null."); } 
    if (value == null) { throw new IllegalArgumentException("ListKey: value is null."); }
    this._elementName = elementName;
    this._value = value;
  }
  
  public String getElementName() {
    return _elementName;
  }
  
  public String getValue() {
    return _value;
  }
  
  
  public static ListKey fromCsv(String csv, CharEscapeTool escaper) {
    String[] parts = csv.split(Constants.YangXmlCsv.SEP_LIST_KEY_VALUE);
    if (parts.length != 2) { throw new IllegalArgumentException("Could not parse csv for list key: " + csv); }
    String parts0 = escaper.unescapeCharacters(parts[0]);
    String parts1 = escaper.unescapeCharacters(parts[1]);
    return new ListKeyBuilder().elementName(parts0).listKeyValue(parts1).build();
  }
  
  
  public String toCsv() {
    StringBuilder str = new StringBuilder();
    writeCsv(str, new CharEscapeTool());
    return str.toString();
  }
  
  
  public void writeCsv(StringBuilder str, CharEscapeTool escaper) {
    str.append(escaper.escapeCharacters(this.getElementName()));
    str.append(Constants.YangXmlCsv.SEP_LIST_KEY_VALUE);
    str.append(escaper.escapeCharacters(this.getValue()));
  }
  
}
