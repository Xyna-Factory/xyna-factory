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


public class YangXmlPathList {

  private List<YangXmlPath> _pathList = new ArrayList<>(); 
  
  
  public List<YangXmlPath> getPathList() {
    return _pathList;
  }

  public void add(YangXmlPath path) {
    _pathList.add(path);
  }
  
  
  public List<String> toCsvList(IdOfNamespaceMap map) {
    return toCsvList(map, new CharEscapeTool());
  }
  
  
  public List<String> toCsvList(IdOfNamespaceMap map, CharEscapeTool escaper) {
    List<String> ret = new ArrayList<>();
    for (YangXmlPath path : _pathList) {
      String csv = path.toCsv(map, escaper);
      ret.add(csv);
    }
    return ret;
  }
  
  
  public List<String> toXPathList(IdOfNamespaceMap map) {
    return toXPathList(map, new CharEscapeTool());
  }
  
  
  public List<String> toXPathList(IdOfNamespaceMap map, CharEscapeTool escaper) {
    List<String> ret = new ArrayList<>();
    for (YangXmlPath path : _pathList) {
      String str = path.toXPath(map, escaper);
      ret.add(str);
    }
    return ret;
  }
  
  
  public void sort() {
    Collections.sort(_pathList);
  }
  
  
  public void addAll(CsvPathsAndNspsWithIds input) {
    YangXmlPathList toAdd = YangXmlPathList.fromCsv(input);
    addAll(toAdd);
  }
  
  
  public void addAll(YangXmlPathList input) {
    _pathList.addAll(input._pathList);
  }
  
  
  public static YangXmlPathList fromCsv(CsvPathsAndNspsWithIds input) {
    NamespaceOfIdMap map = new NamespaceOfIdMap();
    map.initFromPrefixNamespacePairs(input.getNamespaceWithIdList());
    return fromCsv(map, input.getCsvPathList());
  }
  
  
  public static YangXmlPathList fromCsv(NamespaceOfIdMap map, List<String> csvList) {
    return fromCsv(map, csvList, new CharEscapeTool());
  }
  
  
  public static YangXmlPathList fromCsv(NamespaceOfIdMap map, List<String> csvList, CharEscapeTool escaper) {
    YangXmlPathList ret = new YangXmlPathList();
    for (String csv : csvList) {
      YangXmlPath path = YangXmlPath.fromCsv(map, csv, escaper);
      ret._pathList.add(path);
    }
    ret.sort();
    return ret;
  }
  
  
  public YangXmlPathList replaceListIndicesWithKeys() {
    ListKeyAnalyzer analyzer = new ListKeyAnalyzer(this);
    YangXmlPathList ret = analyzer.getResult();
    ret.sort();
    return ret;
  }
  
  
  public String toXml() {
    return toTree().toXml();
  }
  
  
  public YangXmlPathElemTree toTree() {
    return new YangXmlPathElemTree(this);
  }
  
}
