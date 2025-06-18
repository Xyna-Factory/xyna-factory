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


public class CsvPathsAndNspsWithIds {

  private final List<String> _csvPathList;
  private final List<String> _namespaceWithIdList;
  
  
  public CsvPathsAndNspsWithIds(CsvPathsAndNspsWithIdsBuilder builder) {
    _csvPathList = builder.getCsvPathList();
    _namespaceWithIdList = builder.getNamespaceWithIdList();
  }
  
  
  public CsvPathsAndNspsWithIds(YangXmlPathList input) {
    IdOfNamespaceMap map = new IdOfNamespaceMap();
    _csvPathList = input.toCsvList(map);
    _namespaceWithIdList = map.toPrefixNamespacePairList();
  }
  
  
  public static CsvPathsAndNspsWithIdsBuilder builder() {
    return new CsvPathsAndNspsWithIdsBuilder();
  }
  
  
  public List<String> getCsvPathList() {
    return _csvPathList;
  }
  
  
  public List<String> getNamespaceWithIdList() {
    return _namespaceWithIdList;
  }
  
  
  public CsvPathsAndNspsWithIds merge(CsvPathsAndNspsWithIds input) {
    YangXmlPathList pathlist = YangXmlPathList.fromCsv(this);
    pathlist.addAll(input);
    pathlist.sort();
    CsvPathsAndNspsWithIds ret = new CsvPathsAndNspsWithIds(pathlist);
    return ret;
  }
  
}
