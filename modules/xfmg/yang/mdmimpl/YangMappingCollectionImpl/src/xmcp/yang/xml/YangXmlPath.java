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
import java.util.List;


public class YangXmlPath implements Comparable<YangXmlPath> {

  private List<YangXmlPathElem> _path = new ArrayList<>();
  
  
  public List<YangXmlPathElem> getPath() {
    return _path;
  }


  public void add(YangXmlPathElem elem) {
    _path.add(elem);
  }
  
  
  public String toCsv(IdOfNamespaceMap map) {
    return toCsv(map, new CharEscapeTool());
  }
  
  
  public String toCsv(IdOfNamespaceMap map, CharEscapeTool escaper) {
    StringBuilder str = new StringBuilder();
    writeCsv(map, str, escaper);
    return str.toString();
  }
  
  
  /*
   * format: path-elem , path-elem , ...
   */
  public void writeCsv(IdOfNamespaceMap map, StringBuilder str, CharEscapeTool escaper) {
    boolean isfirst = true;
    for (YangXmlPathElem elem : _path) {
      if (isfirst) { isfirst = false; }
      else { str.append(Constants.YangXmlCsv.SEP_PATH_ELEM); }
      elem.writeCsv(map, str, escaper);
    }
  }
  
  
  public static YangXmlPath fromCsv(NamespaceOfIdMap map, String csv) {
    return fromCsv(map, csv, new CharEscapeTool());
  }
  
  
  public static YangXmlPath fromCsv(NamespaceOfIdMap map, String csv, CharEscapeTool escaper) {
    String[] parts = csv.split(Constants.YangXmlCsv.SEP_PATH_ELEM);
    YangXmlPath ret = new YangXmlPath(); 
    for (String item : parts) {
      YangXmlPathElem elem = YangXmlPathElem.fromCsv(map, item, escaper);
      ret.add(elem);
    }
    return ret;
  }

  
  @Override
  public int compareTo(YangXmlPath input) {
    int max = Integer.max(_path.size(), input._path.size());
    for (int i = 0; i < max; i ++) {
      boolean atEnd1 = (i >= _path.size());
      boolean atEnd2 = (i >= input._path.size());
      if (atEnd1) { return -1; }
      if (atEnd2) { return 1; }
      int val = _path.get(i).compareTo(input._path.get(i));
      if (val != 0) { return val; }
    }
    return 0;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof YangXmlPath) {
      return compareTo((YangXmlPath) obj) == 0;
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return _path.size();
  }
  
}
