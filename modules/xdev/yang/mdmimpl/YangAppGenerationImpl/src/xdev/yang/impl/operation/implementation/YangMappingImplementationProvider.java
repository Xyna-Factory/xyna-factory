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

package xdev.yang.impl.operation.implementation;

import java.util.List;

import org.w3c.dom.Document;

import xdev.yang.impl.operation.MappingPathElement;
import xdev.yang.impl.operation.OperationMapping;


public class YangMappingImplementationProvider implements ImplementationProvider {
  
  private final OpImplTools _tools = new OpImplTools();
  

  public String createImpl(Document meta, List<String> inputVarNames) {
    StringBuilder result = new StringBuilder();
    _tools.createVariables(result, meta, inputVarNames);
    List<OperationMapping> mappings = OperationMapping.loadMappings(meta);
    result.append("YangMappingPath path = null;");
    result.append("List<xmcp.yang.YangMappingPath> pathList = new ArrayList<>();").append("\n");
    
    for (int i = 0; i < mappings.size(); i++) {
      OperationMapping mapping = mappings.get(i);
      List<MappingPathElement> mappingList = mapping.createPathList();
      result.append("path = new xmcp.yang.YangMappingPath();").append("\n");
      for (MappingPathElement elem : mappingList) {
        if (OpImplTools.hiddenYangKeywords.contains(elem.getKeyword())) { continue; }
        result.append("path.addToPath(new xmcp.yang.YangMappingPathElement.Builder().elementName(").append(elem.getYangPath()).append(")");
        result.append(".namespace(").append(elem.getNamespace()).append(").instance());").append("\n");
      }
      String val = _tools.determineMappingString(mapping.getValue());
      result.append("path.setValue(").append(val).append(");").append("\n");
      result.append("pathList.add(path);").append("\n");
      result.append("xmcp.yang.YangMappingCollection coll2 = new xmcp.yang.YangMappingCollection();").append("\n");
      result.append("coll2.overwriteContent(pathList);").append("\n");
      result.append("xmcp.yang.YangMappingCollection ret = ").append(inputVarNames.get(0)).append(".merge(coll2);").append("\n");
      result.append("return ret;").append("\n");
    }
    return result.toString();
  }
  
}
