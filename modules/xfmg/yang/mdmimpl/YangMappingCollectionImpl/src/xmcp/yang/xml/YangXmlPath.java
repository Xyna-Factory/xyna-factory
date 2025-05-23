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

public class YangXmlPath {

  private List<YangXmlPathElem> _path = new ArrayList<>();
  
  protected void add(YangXmlPathElem elem) {
    _path.add(elem);
  }
  
  
  public String toCsv() {
    return toCsv(new CharEscapeTool());
  }
  
  
  public String toCsv(CharEscapeTool escaper) {
    StringBuilder str = new StringBuilder();
    writeCsv(str, escaper);
    return str.toString();
  }
  
  
  public void writeCsv(StringBuilder str, CharEscapeTool escaper) {
    boolean isfirst = true;
    for (YangXmlPathElem elem : _path) {
      if (isfirst) { isfirst = false; }
      else { str.append(Constants.YangXmlCsv.SEP_PATH_ELEM); }
      elem.writeCsv(str, escaper);
    }
  }
  
  
  public static YangXmlPath fromCsv(String csv) {
    return fromCsv(csv, new CharEscapeTool());
  }
  
  
  public static YangXmlPath fromCsv(String csv, CharEscapeTool escaper) {
    String[] parts = csv.split(Constants.YangXmlCsv.SEP_PATH_ELEM);
    YangXmlPath ret = new YangXmlPath(); 
    for (String item : parts) {
      YangXmlPathElem elem = YangXmlPathElem.fromCsv(item, escaper);
      ret.add(elem);
    }
    return ret;
  }
  
}
