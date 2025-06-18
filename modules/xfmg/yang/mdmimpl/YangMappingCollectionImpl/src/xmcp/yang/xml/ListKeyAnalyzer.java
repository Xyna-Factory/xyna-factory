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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/*
 * adapt yang xml paths to replace list indices with list keys; that means list items will not be identified by
 * an integer index but the values of xml sub-elements of the list item
 */
public class ListKeyAnalyzer {

  private Map<String, ListKeySearchInfo> _listKeyMap = new HashMap<>();
  private List<List<PathElemBuilder>> _buildPaths = new ArrayList<>(); 
  private YangXmlPathList _result = new YangXmlPathList();
  
  
  public ListKeyAnalyzer(YangXmlPathList input) {
    init(input);
  }
  
  
  public YangXmlPathList getResult() {
    return _result;
  }
  
  
  public void init(YangXmlPathList input) {
    IdOfNamespaceMap nspMap = new IdOfNamespaceMap();
    for (YangXmlPath path : input.getPathList()) {
      handlePath(path, nspMap);
    }
    for (ListKeySearchInfo lksi : _listKeyMap.values()) {
      adaptElems(lksi);
    }
    finishInit();
  }
  
  
  private void finishInit() {
    for (List<PathElemBuilder> list : _buildPaths) {
      finishInit(list);
    }
  }
  
  
  private void finishInit(List<PathElemBuilder> list) {
    YangXmlPath ret = new YangXmlPath();
    for (PathElemBuilder builder : list) {
      ret.add(builder.build());
    }
    _result.add(ret);
  }
  
  
  private void adaptElems(ListKeySearchInfo lksi) {
    for (PathElemBuilder builder : lksi.getElements()) {
      for (ListKey lk : lksi.getListKeys()) {
        builder.addListKey(lk);
      }
    }
  }
  
  
  /*
   * collect all list keys that belong to the same list item, that means have identical paths
   * (including list indices) above the leaf level
   */
  private void handlePath(YangXmlPath path, IdOfNamespaceMap nspMap) {
    StringBuilder csv = new StringBuilder("");
    List<PathElemBuilder> copyPath = new ArrayList<>();
    for (int i = 0; i < path.getPath().size(); i++) {
      YangXmlPathElem elem = path.getPath().get(i);
      Optional<PathElemBuilder> copyElem = elem.copyIfHasNoListIndex();
      if (copyElem.isPresent()) {
        copyPath.add(copyElem.get());
      }
      if (!elem.getIsListKeyLeaf()) {
      }
      boolean doHandleList = false;
      if (i >= 2) {
        YangXmlPathElem parent = path.getPath().get(i - 1);
        YangXmlPathElem grandParent = path.getPath().get(i - 2);
        if (grandParent.hasListIndex() && !parent.hasListIndex()) {
          doHandleList = true;
        }
      }
      if (doHandleList) {
        handleListInPath(elem, copyPath, csv.toString());
      }
      csv.append(elem.toCsv(nspMap)).append(Constants.YangXmlCsv.SEP_PATH_ELEM);
    }
    _buildPaths.add(copyPath);
  }
  
  
  private void handleListInPath(YangXmlPathElem elem, List<PathElemBuilder> copyPath, String csvStr) {
    ListKeySearchInfo lksi = _listKeyMap.get(csvStr);
    if (lksi == null) {
      lksi = new ListKeySearchInfo();
      _listKeyMap.put(csvStr, lksi);
    }
    if (elem.hasTextValue() && elem.getIsListKeyLeaf()) {
      ListKey lk = new ListKeyBuilder().listKeyElemName(elem.getElemName())
                                       .listKeyValue(elem.getTextValue().get()).build();
      lksi.getListKeys().add(lk);
    }
    // collect parent element, i.e. the list element where later the list key info will be added to replace the list index:
    lksi.getElements().add(copyPath.get(copyPath.size() - 2));
  }
  
}
