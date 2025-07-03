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
  private final int _listIndex;
  private final List<ListKey> _listKeys;
  private final boolean _isListKeyLeaf;
  
  
  public YangXmlPathElem(String elemName, String namespace, String textValue, List<ListKey> listKeys, boolean isListKeyLeaf) {
    if (elemName == null) { throw new IllegalArgumentException("YangXmlPathElem: element name is missing"); }
    if (listKeys == null) { throw new IllegalArgumentException("YangXmlPathElem: Got null list for list keys"); }
    this._elemName = elemName;
    this._namespace = namespace;
    this._textValue = textValue;
    this._listIndex = -1;
    if (listKeys instanceof ArrayList<?>) {
      Collections.sort(listKeys);
      this._listKeys = listKeys;
    } else {
      ArrayList<ListKey> tmp = new ArrayList<>(listKeys);
      Collections.sort(tmp);
      this._listKeys = tmp;
    }
    this._isListKeyLeaf = isListKeyLeaf;
  }
  
  
  private YangXmlPathElem(int listIndex) {
    this._elemName = Constants.DEFAULT_LIST_INDEX_ELEM_NAME;
    this._listIndex = listIndex;
    this._namespace = "";
    this._textValue = "";
    this._listKeys = new ArrayList<ListKey>();
    this._isListKeyLeaf = false;
  }
  
  
  public static YangXmlPathElem buildListIndexElem(int index) {
    return new YangXmlPathElem(index);
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
    
  public int getListIndex() {
    return _listIndex;
  }

  public boolean hasListIndex() {
    return _listIndex >= 0;
  }
  
  public boolean hasListKeys() {
    return _listKeys.size() > 0;
  }
  
  public List<ListKey> getListKeys() {
    return _listKeys;
  }
  
  public boolean getIsListKeyLeaf() {
    return _isListKeyLeaf;
  }


  public String toCsv(IdOfNamespaceMap map) {
    StringBuilder str = new StringBuilder();
    writeCsv(map, str, new CharEscapeTool());
    return str.toString();
  }
  
  
  /*
   * format: element-name # namespace-id # text-value # list index # list-key-name = list-key-value % list-key-name = list-key-value % ... 
   */
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
    if (_listIndex >= 0) {
      str.append(_listIndex);
    }
    str.append(Constants.YangXmlCsv.SEP_PATH_ELEM_ATTR);
    if (_isListKeyLeaf) {
      str.append(Constants.YangXmlCsv.VALUE_FOR_IS_LIST_KEY_LEAF);
    } else {
      boolean isfirst = true;
      for (ListKey lk : getListKeys()) {
        if (isfirst) { isfirst = false; }
        else { str.append(Constants.YangXmlCsv.SEP_LIST_KEY_LIST_ELEMS); }
        lk.writeCsv(str, escaper, map);
      }
    }
  }
  
  
  public static YangXmlPathElem fromCsv(NamespaceOfIdMap map, String csv) {
    return fromCsv(map, csv, new CharEscapeTool());
  }
  
  
  public static YangXmlPathElem fromCsv(NamespaceOfIdMap map, String csv, CharEscapeTool escaper) {
    String[] parts = csv.split(Constants.YangXmlCsv.SEP_PATH_ELEM_ATTR, -1);
    if (parts.length != 5) { throw new IllegalArgumentException("Could not parse csv for path element: " + csv); }
    
    if (parts[3].length() > 0) {
      int listIndex = Integer.parseInt(parts[3]);
      return YangXmlPathElem.buildListIndexElem(listIndex);
    }
    PathElemBuilder builder = new PathElemBuilder();
    builder.elemName(escaper.unescapeCharacters(parts[0]));
    if (parts[1].length() > 0) {
      String nsp = map.getExpectedNamespace(parts[1]);
      builder.namespace(nsp);
    }
    if (parts[2].length() > 0) {
      builder.textValue(escaper.unescapeCharacters(parts[2]));
    }    
    handleListKeyString(parts[4], builder, escaper, map);
    return builder.build();
  }
  
  
  private static void handleListKeyString(String listKeyValIn, PathElemBuilder builder, CharEscapeTool escaper,
                                          NamespaceOfIdMap map) {
    if (listKeyValIn == null) { return; }
    String listKeyVal = listKeyValIn.trim();
    if (listKeyVal.length() <= 0) { return; }
    if (listKeyVal.contains(Constants.YangXmlCsv.SEP_LIST_KEY_VALUE)) {
      addListKeys(builder, listKeyVal, escaper, map);
    } else if (Constants.YangXmlCsv.VALUE_FOR_IS_LIST_KEY_LEAF.equals(listKeyVal)) {
      builder.setIsListKeyLeaf(true);
    }
  }
  
  
  private static void addListKeys(PathElemBuilder builder, String csv, CharEscapeTool escaper, NamespaceOfIdMap map) {
    String[] parts = csv.split(Constants.YangXmlCsv.SEP_LIST_KEY_LIST_ELEMS);
    for (String item : parts) {
      ListKey lk = ListKey.fromCsv(item, escaper, map);
      builder.addListKey(lk);
    }
  }

  
  public Optional<PathElemBuilder> copyIfHasNoListIndex() {
    if (hasListIndex()) {
      return Optional.empty();
    }
    PathElemBuilder ret = builder().elemName(_elemName).namespace(_namespace).textValue(_textValue).addListKeyList(_listKeys);
    return Optional.ofNullable(ret);
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
    val = Integer.compare(_listIndex, elem._listIndex);
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
