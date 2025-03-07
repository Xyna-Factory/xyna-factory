/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

package xdev.yang.impl.usecase;

import java.util.ArrayList;
import java.util.List;

import xdev.yang.impl.Constants;
import xmcp.yang.LoadYangAssignmentsData;


public class SchemaNodePath {

  public static class PathData {
    public List<String> localnameList = new ArrayList<>();
    public List<String> namespaceList = new ArrayList<>();
    public List<String> keywordList = new ArrayList<>();
  }
  
  private final PathData _pathData;
  private final List<SchemaNodePath> _containedPathsOfUsedGroupings = new ArrayList<>();
  
  
  private SchemaNodePath(PathData pathData) {
    this._pathData = pathData;
  }
  
  public SchemaNodePath(LoadYangAssignmentsData data) {
    _pathData = new PathData();
    LoadYangAssignmentsDataContent content = new LoadYangAssignmentsDataContent(data);
    List<PathData> groupingPaths = new ArrayList<>();
    for (int i = 0; i < content.getLength(); i++) {
      if (Constants.TYPE_USES.equals(content.getKeywordPart(i))) {
        groupingPaths.add(new PathData());
      }
      if (Constants.SCHEMA_NODE_TYPE_NAMES.contains(content.getKeywordPart(i))) { 
        _pathData.localnameList.add(content.getPart(i));
        _pathData.namespaceList.add(content.getNamespacePart(i));
        _pathData.keywordList.add(content.getKeywordPart(i));
        for (PathData pd : groupingPaths) {
          pd.localnameList.add(content.getPart(i));
          pd.namespaceList.add(content.getNamespacePart(i)); 
          pd.keywordList.add(content.getKeywordPart(i));
        }
      }
    }
    for (PathData pd : groupingPaths) {
      _containedPathsOfUsedGroupings.add(new SchemaNodePath(pd));
    }
  }


  public List<String> getLocalnameList() {
    return _pathData.localnameList;
  }
  
  public List<String> getNamespaceList() {
    return _pathData.namespaceList;
  }
  
  public List<String> getKeywordList() {
    return _pathData.keywordList;
  }
  
  public List<SchemaNodePath> getContainedPathsOfUsedGroupings() {
    return _containedPathsOfUsedGroupings;
  }

}
