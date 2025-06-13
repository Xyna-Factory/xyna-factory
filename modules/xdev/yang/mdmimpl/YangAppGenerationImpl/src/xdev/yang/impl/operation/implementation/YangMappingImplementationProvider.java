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
import java.util.Optional;

import org.w3c.dom.Document;

import xdev.yang.impl.operation.ListConfiguration;
import xdev.yang.impl.operation.MappingPathElement;
import xdev.yang.impl.operation.OperationMapping;
import xdev.yang.impl.operation.ListConfiguration.DynamicListLengthConfig;


public class YangMappingImplementationProvider implements ImplementationProvider {
  
  public static class ListCounter {
    public int count = 0;
  }
  
  public static class ListInfo {
    public String counterVarname = null;
  }
  
  private final OpImplTools _tools = new OpImplTools();
  

  public String createImpl(Document meta, List<String> inputVarNames) {
    StringBuilder result = new StringBuilder();
    List<ListConfiguration> listConfigs = ListConfiguration.loadListConfigurations(meta);
    ListCounter globalCounter = new ListCounter();
    _tools.createVariables(result, meta, inputVarNames);
    result.append("\n");
    List<OperationMapping> mappings = OperationMapping.loadMappings(meta);
    result.append("xmcp.yang.YangMappingPath path = null;").append("\n");
    result.append("List<xmcp.yang.YangMappingPath> pathList = new ArrayList<>();").append("\n");
    result.append("try {").append("\n");
    
    for (int i = 0; i < mappings.size(); i++) {
      OperationMapping mapping = mappings.get(i);
      handleMapping(mapping, result, listConfigs, globalCounter);
    }
    result.append("  xmcp.yang.YangMappingCollection coll2 = new xmcp.yang.YangMappingCollection();").append("\n");
    result.append("  coll2.overwriteContent(pathList);").append("\n");
    result.append("  xmcp.yang.YangMappingCollection ret = ").append(inputVarNames.get(0)).append(".merge(coll2);").append("\n");
    result.append("  return ret;").append("\n");
    
    result.append("} catch(Exception e) {").append("\n");
    result.append("  throw new RuntimeException(e);").append("\n");
    result.append("}").append("\n");
    
    return result.toString();
  }
  
  
  private void handleMapping(OperationMapping mapping, StringBuilder result, List<ListConfiguration> listConfigs,
                             ListCounter globalCounter) {
    ListCounter localCounter = new ListCounter();
    result.append("\n");
    result.append("  //").append(mapping.getMappingYangPath()).append(" -> ").append(mapping.getValue()).append("\n");
    List<MappingPathElement> mappingList = mapping.createPathList();
    result.append("  path = new xmcp.yang.YangMappingPath();").append("\n");
    
    for (int k = 0; k < mappingList.size(); k++) {
      MappingPathElement elem = mappingList.get(k);
      ListConfiguration listConfig = _tools.isDynamicList(mappingList.subList(0, k + 1), listConfigs);
      boolean isDynList = (listConfig != null);
      ListInfo listInfo = null;
      if (isDynList) {
        globalCounter.count++;
        localCounter.count++;
        listInfo = this.writeListStart(result, listConfig, globalCounter);
        result.append("  path.addToPath(new xmcp.yang.YangMappingPathElement.Builder().elementName(\"")
              .append(_tools.cleanupTag(elem.getYangPath())).append("\")").append(".listIndex(")
              .append(listInfo.counterVarname).append(")").append(".instance());").append("\n");
      } else if (OpImplTools.listKeywords.contains(elem.getKeyword())) {
        Optional<Integer> index = _tools.getOptionalConstListIndex(mappingList, k);
        if (index.isPresent()) {
          result.append("  path.addToPath(new xmcp.yang.YangMappingPathElement.Builder().elementName(\"")
                .append(_tools.cleanupTag(elem.getYangPath())).append("\")").append(".listIndex(")
                .append(index.get()).append(")").append(".instance());").append("\n");
        }
      } else if (!OpImplTools.hiddenYangKeywords.contains(elem.getKeyword())) {
        result.append("  path.addToPath(new xmcp.yang.YangMappingPathElement.Builder().elementName(\"")
            .append(_tools.cleanupTag(elem.getYangPath())).append("\")").append(".namespace(\"")
            .append(elem.getNamespace()).append("\")").append(".instance());").append("\n");
      }
    }
    String val = _tools.determineMappingString(mapping.getValue());
    result.append("  path.setValue(").append(val).append(");").append("\n");
    result.append("  pathList.add(path);").append("\n");
    closeLoops(result, localCounter);
  }
  
  
  private ListInfo writeListStart(StringBuilder result, ListConfiguration listConfig, ListCounter counter) {
    ListInfo ret = new ListInfo();
    result.append("  xmcp.yang.YangMappingPath backupPath_").append(counter.count).append( " = path;").append("\n");
    DynamicListLengthConfig dynListConfig = (DynamicListLengthConfig) listConfig.getConfig();
    String loopVareName = dynListConfig.getVariable();
    String loopVarPath = _tools.determineMappingValueObject(dynListConfig.getPath());
    String counterVarName = String.format("i_%d", counter.count);
    ret.counterVarname = counterVarName;
    result.append("  List<?> ").append(loopVareName).append("_list = (List<?>)").append(loopVarPath).append(";").append("\n");
    result.append("  for (int ").append(counterVarName).append(" = 0; ").append(counterVarName).append(" < ")
          .append(loopVareName).append("_list.size(); ").append(counterVarName).append("++) {").append("\n");
    result.append("  Object ").append(loopVareName).append(" = ").append(loopVareName).append("_list.get(")
          .append(counterVarName).append(");").append("\n");
    result.append("  path = backupPath_").append(counter.count).append( ".clone();").append("\n");
    return ret;
  }
  
  
  private void closeLoops(StringBuilder result, ListCounter counter) {
    for (int i = 0; i < counter.count; i++) {
      result.append("  }").append("\n");
    }
  }
  
}
