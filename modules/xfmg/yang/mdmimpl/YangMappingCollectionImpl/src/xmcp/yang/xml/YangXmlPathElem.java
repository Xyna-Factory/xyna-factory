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

import java.util.List;
import java.util.Optional;


public class YangXmlPathElem {

  private final String _elemName;
  private final String _namespace;
  private final String _textValue;
  private final List<ListKey> _listKeys;
  
  
  public YangXmlPathElem(String elemName, String namespace, String textValue, List<ListKey> listKeys) {
    this._elemName = elemName;
    this._namespace = namespace;
    this._textValue = textValue;
    this._listKeys = listKeys;
  }
  
  public static PathElemBuilder builder() {
    return new PathElemBuilder();
  }
  
  public String getElemName() {
    return _elemName;
  }
  
  public boolean hasNamespace() {
    return _namespace != null;
  }
  
  public Optional<String> getNamespace() {
    return Optional.ofNullable(_namespace);
  }
  
  public boolean hasTextValue() {
    return _textValue != null;
  }
  
  public Optional<String> getTextValue() {
    return Optional.ofNullable(_textValue);
  }
  
  public boolean hasListKeys() {
    return _listKeys.size() > 0;
  }
  
  public List<ListKey> getListKeys() {
    return _listKeys;
  }
  
  
  public String toCsv() {
    StringBuilder str = new StringBuilder();
    writeCsv(str, new CharEscapeTool());
    return str.toString();
  }
  
  
  public void writeCsv(StringBuilder str, CharEscapeTool escaper) {
    str.append(escaper.escapeCharacters(_elemName));
    str.append(Constants.YangXmlCsv.SEP_PATH_ELEM_ATTR);
    
    // TODO: nsp -> id
    str.append(escaper.escapeCharacters(_namespace));
    str.append(Constants.YangXmlCsv.SEP_PATH_ELEM_ATTR);
    str.append(escaper.escapeCharacters(_textValue));
    str.append(Constants.YangXmlCsv.SEP_PATH_ELEM_ATTR);
    if (_listKeys == null) { return; }
    boolean isfirst = true;
    for (ListKey lk : getListKeys()) {
      if (isfirst) { isfirst = false; }
      else { str.append(Constants.YangXmlCsv.SEP_LIST_KEY_LIST_ELEMS); }
      lk.writeCsv(str, escaper);
    }
  }
  
  
  public static YangXmlPathElem fromCsv(String csv) {
    return fromCsv(csv, new CharEscapeTool());
  }
  
  
  public static YangXmlPathElem fromCsv(String csv, CharEscapeTool escaper) {
    PathElemBuilder builder = new PathElemBuilder();
    String[] parts = csv.split(Constants.YangXmlCsv.SEP_PATH_ELEM_ATTR);
    if (parts.length != 4) { throw new IllegalArgumentException("Could not parse csv for path element: " + csv); }
    builder.elemName(escaper.unescapeCharacters(parts[0]));
    if (parts[1].length() > 0) {
      builder.namespace(escaper.unescapeCharacters(parts[1]));
    }
    if (parts[2].length() > 0) {
      builder.textValue(escaper.unescapeCharacters(parts[2]));
    }
    if (parts[3].length() > 0) {
      addListKeys(builder, parts[3], escaper);
    }
    return builder.build();
  }
  
  
  private static void addListKeys(PathElemBuilder builder, String csv, CharEscapeTool escaper) {
    String[] parts = csv.split(Constants.YangXmlCsv.SEP_LIST_KEY_LIST_ELEMS);
    for (String item : parts) {
      ListKey lk = ListKey.fromCsv(item, escaper);
      builder.addListKey(lk);
    }
  }
  
}
