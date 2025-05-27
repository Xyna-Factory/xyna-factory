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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class YangXmlPathElem implements Comparable<YangXmlPathElem> {

  private final String _elemName;
  private final String _namespace;
  private final String _textValue;
  private final List<ListKey> _listKeys;
  
  
  public YangXmlPathElem(String elemName, String namespace, String textValue, List<ListKey> listKeys) {
    if (elemName == null) { throw new IllegalArgumentException("YangXmlPathElem: element name is missing"); }
    if (listKeys == null) { throw new IllegalArgumentException("YangXmlPathElem: Got null list for list keys"); }
    this._elemName = elemName;
    this._namespace = namespace;
    this._textValue = textValue;
    if (listKeys instanceof ArrayList<?>) {
      Collections.sort(listKeys);
      this._listKeys = listKeys;
    } else {
      ArrayList<ListKey> tmp = new ArrayList<>(listKeys);
      Collections.sort(tmp);
      this._listKeys = tmp;
    }
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
  
  
  public String toCsv(IdOfNamespaceMap map) {
    StringBuilder str = new StringBuilder();
    writeCsv(map, str, new CharEscapeTool());
    return str.toString();
  }
  
  
  public void writeCsv(IdOfNamespaceMap map, StringBuilder str, CharEscapeTool escaper) {
    str.append(escaper.escapeCharacters(_elemName));
    str.append(Constants.YangXmlCsv.SEP_PATH_ELEM_ATTR);
    if (_namespace != null) {
      long id = map.getId(_namespace);
      str.append(id);
    }
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
  
  
  public static YangXmlPathElem fromCsv(NamespaceOfIdMap map, String csv) {
    return fromCsv(map, csv, new CharEscapeTool());
  }
  
  
  public static YangXmlPathElem fromCsv(NamespaceOfIdMap map, String csv, CharEscapeTool escaper) {
    PathElemBuilder builder = new PathElemBuilder();
    String[] parts = csv.split(Constants.YangXmlCsv.SEP_PATH_ELEM_ATTR, -1);
    if (parts.length != 4) { throw new IllegalArgumentException("Could not parse csv for path element: " + csv); }
    builder.elemName(escaper.unescapeCharacters(parts[0]));
    if (parts[1].length() > 0) {
      long id = Long.parseLong(parts[1]);
      if (!map.getNamespace(id).isPresent()) { throw new IllegalArgumentException("Could not find namespace for id: " + id); }
      builder.namespace(map.getNamespace(id).get());
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

  /*
   * text value is not used for comparison
   */
  @Override
  public int compareTo(YangXmlPathElem elem) {
    if (hasNamespace() && !elem.hasNamespace()) {
      return -1;
    }
    if (elem.hasNamespace() && !hasNamespace()) {
      return 1;
    }
    int val = 0;
    val = _namespace.compareTo(elem._namespace);
    if (val != 0) { return val; }
    val = _elemName.compareTo(elem._elemName);
    if (val != 0) { return val; }
    val = Integer.compare(_listKeys.size(), elem._listKeys.size());
    if (val != 0) { return val; }
    for (int i = 0; i < _listKeys.size(); i++) {
      val = _listKeys.get(i).compareTo(elem._listKeys.get(i));
      if (val != 0) { return val; }
    }
    return 0;
  }
  
  
  @Override 
  public boolean equals(Object obj) {
    if (obj instanceof YangXmlPathElem) {
      return compareTo((YangXmlPathElem) obj) == 0;
    }
    return false;
  }
  
  
  @Override
  public int hashCode() {
    return _elemName.hashCode();
  }
  
}
