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


public class ListKey implements Comparable<ListKey> {

  private final String _elementName;
  private final String _value;
  private final String _namespace;
  
  
  public ListKey(String elementName, String value) {
    this(elementName, value, null);
  }
  
  public ListKey(String elementName, String value, String namespace) {
    if (elementName == null) { throw new IllegalArgumentException("ListKey: elementName is null."); } 
    if (value == null) { throw new IllegalArgumentException("ListKey: value is null."); }
    this._elementName = elementName;
    this._value = value;
    this._namespace = namespace;
  }
  
  public String getElementName() {
    return _elementName;
  }
  
  public String getValue() {
    return _value;
  }
  
  public String getNamespace() {
    return _namespace;
  }

  public static ListKey fromCsv(String csv, CharEscapeTool escaper, NamespaceOfIdMap map) {
    String[] parts = csv.split(Constants.YangXmlCsv.SEP_LIST_KEY_VALUE);
    if (parts.length != 2) { throw new IllegalArgumentException("Could not parse csv for list key: " + csv); }
    String parts0 = escaper.unescapeCharacters(parts[0]);
    String parts1 = escaper.unescapeCharacters(parts[1]);
    String name = parts0;
    String nsp = null;
    if (name.contains(Constants.YangXmlCsv.SEP_LIST_KEY_NAMESPACE)) {
      String[] split2 = name.split(Constants.YangXmlCsv.SEP_LIST_KEY_NAMESPACE);
      if (split2.length != 2) { throw new IllegalArgumentException("Could not parse csv for list key: " + csv); }
      name = split2[0];
      nsp = map.getExpectedNamespace(split2[1]);
    }
    return new ListKeyBuilder().listKeyElemName(name).listKeyNamespace(nsp).listKeyValue(parts1).build();
  }
  
  
  public String toCsv(IdOfNamespaceMap map) {
    StringBuilder str = new StringBuilder();
    writeCsv(str, new CharEscapeTool(), map);
    return str.toString();
  }
  
  
  public void writeCsv(StringBuilder str, CharEscapeTool escaper, IdOfNamespaceMap map) {
    str.append(escaper.escapeCharacters(getElementName()));
    if (getNamespace() != null) {
      str.append(Constants.YangXmlCsv.SEP_LIST_KEY_NAMESPACE);
      long id = map.getId(getNamespace());
      str.append(id);
    }
    str.append(Constants.YangXmlCsv.SEP_LIST_KEY_VALUE);
    str.append(escaper.escapeCharacters(getValue()));
  }

  
  public void writeXPath(StringBuilder str, CharEscapeTool escaper, IdOfNamespaceMap map) {
    str.append("[");
    if (getNamespace() != null) {
      long id = map.getId(getNamespace());
      str.append(Constants.PREFIX_OF_PREFIX).append(id).append(":");
    }
    str.append(escaper.escapeCharacters(getElementName()));
    str.append("/text()=");
    str.append(escaper.escapeCharacters(getValue()));
    str.append("]");
  }
  
  
  @Override
  public int compareTo(ListKey lk) {
    int val = _elementName.compareTo(lk.getElementName());
    if (val != 0) { return val; }
    return _value.compareTo(lk.getValue());
  }
  
}
