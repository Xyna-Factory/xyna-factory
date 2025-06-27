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
    _tools.createVariables(result, meta, inputVarNames);
    result.append("\n");
    List<OperationMapping> mappings = OperationMapping.loadMappings(meta);
    result.append("xmcp.yang.YangMappingPath path = null;").append("\n");
    result.append("List<xmcp.yang.YangMappingPath> pathList = new ArrayList<>();").append("\n");
    result.append("try {").append("\n");
    
    for (int i = 0; i < mappings.size(); i++) {
      OperationMapping mapping = mappings.get(i);
      handleMapping(mapping, result, listConfigs);
    }
    result.append("  ").append("\n");
    result.append("  com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean prop = ");
    result.append("new com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean(");
    result.append("\"xmcp.yang.TraceYangMappingCollectionContent\", false);").append("\n");
    
    result.append("  Boolean flag = prop.get();").append("\n");
    result.append("  if (flag == null) { flag = false; }").append("\n");
    result.append("  xmcp.yang.YangMappingCollection coll2 = null;").append("\n");
    
    result.append("  if (flag) {").append("\n");
    result.append("    coll2 = new xmcp.yang.TraceYangMappingCollection();").append("\n");
    result.append("  } else {").append("\n");
    result.append("    coll2 = new xmcp.yang.YangMappingCollection();").append("\n");
    result.append("  }").append("\n");
    
    result.append("  coll2.overwriteContent(pathList);").append("\n");
    result.append("  xmcp.yang.YangMappingCollection ret = ").append(inputVarNames.get(0)).append(".merge(coll2);").append("\n");
    result.append("  return ret;").append("\n");
    
    result.append("} catch(Exception e) {").append("\n");
    result.append("  throw new RuntimeException(e);").append("\n");
    result.append("}").append("\n");
    
    return result.toString();
  }
  
  
  private void handleMapping(OperationMapping mapping, StringBuilder result, List<ListConfiguration> listConfigs) {
    ListCounter localCounter = new ListCounter();
    result.append("\n");
    result.append("  //").append(mapping.getMappingYangPath()).append(" -> ").append(mapping.getValue()).append("\n");
    List<MappingPathElement> mappingList = mapping.createPathList();
    result.append("  path = new xmcp.yang.YangMappingPath();").append("\n");
    result.append("  {").append("\n");
    boolean isListKeyMapping = false;
    ListConfiguration parentListConfig = null;
    for (int k = 0; k < mappingList.size(); k++) {
      MappingPathElement elem = mappingList.get(k);
      ListConfiguration listConfig = _tools.getListConfigOrNull(mappingList.subList(0, k + 1), listConfigs);
      if (listConfig != null) {
        parentListConfig = listConfig;
      }
      boolean isDynList = _tools.isDynamicList(listConfig);
      ListInfo listInfo = null;
      if (isDynList) {
        localCounter.count++;
        listInfo = this.writeListStart(result, listConfig, localCounter);
        result.append("    path.addToPath(new xmcp.yang.YangMappingPathElement.Builder().elementName(\"")
              .append(_tools.cleanupTag(elem.getYangPath())).append("\")").append(".listIndex(")
              .append(listInfo.counterVarname).append(")").append(".instance());").append("\n");
      } else if (OpImplTools.listKeywords.contains(elem.getKeyword())) {
        Optional<Integer> index = _tools.getOptionalConstListIndex(mappingList, k);
        if (index.isPresent()) {
          result.append("    path.addToPath(new xmcp.yang.YangMappingPathElement.Builder().elementName(\"")
                .append(_tools.cleanupTag(elem.getYangPath())).append("\")").append(".listIndex(")
                .append(index.get()).append(")").append(".instance());").append("\n");
        }
      } else if (!OpImplTools.hiddenYangKeywords.contains(elem.getKeyword())) {
        result.append("    path.addToPath(new xmcp.yang.YangMappingPathElement.Builder().elementName(\"")
            .append(_tools.cleanupTag(elem.getYangPath())).append("\")").append(".namespace(\"")
            .append(elem.getNamespace()).append("\")").append(".instance());").append("\n");
      }
      if (k == mappingList.size() - 1) {
        isListKeyMapping = isListKeyLeaf(parentListConfig, elem);
      }
    }
    String val = _tools.determineMappingString(mapping.getValue());
    result.append("    path.setValue(").append(val).append(");").append("\n");
    if (isListKeyMapping) {
      result.append("    path.setIsListKey(").append(isListKeyMapping).append(");").append("\n");
    }
    result.append("    pathList.add(path);").append("\n");
    closeLoops(result, localCounter);
    result.append("  }").append("\n");
  }
  
  
  private boolean isListKeyLeaf(ListConfiguration listConfig, MappingPathElement elem) {
    if (listConfig == null) { return false; }
    if (listConfig.getListKeyNames() == null) { return false; }
    if (elem == null) { return false; }
    if (elem.getYangPath() == null) { return false; }
    for (String name : listConfig.getListKeyNames()) {
      if (name == null) { continue; }
      if (name.equals(elem.getYangPath())) {
        return true;
      }
    }
    return false;
  }
  
  
  private ListInfo writeListStart(StringBuilder result, ListConfiguration listConfig, ListCounter counter) {
    ListInfo ret = new ListInfo();
    result.append("    xmcp.yang.YangMappingPath backupPath_").append(counter.count).append( " = path;").append("\n");
    DynamicListLengthConfig dynListConfig = (DynamicListLengthConfig) listConfig.getConfig();
    String loopVareName = dynListConfig.getVariable();
    String loopVarPath = _tools.determineMappingValueObject(dynListConfig.getPath());
    String counterVarName = String.format("i_%d", counter.count);
    ret.counterVarname = counterVarName;
    result.append("    List<?> ").append(loopVareName).append("_list = (List<?>)").append(loopVarPath).append(";").append("\n");
    result.append("    for (int ").append(counterVarName).append(" = 0; ").append(counterVarName).append(" < ")
          .append(loopVareName).append("_list.size(); ").append(counterVarName).append("++) {").append("\n");
    result.append("    Object ").append(loopVareName).append(" = ").append(loopVareName).append("_list.get(")
          .append(counterVarName).append(");").append("\n");
    result.append("    path = backupPath_").append(counter.count).append( ".clone();").append("\n");
    return ret;
  }
  
  
  private void closeLoops(StringBuilder result, ListCounter counter) {
    for (int i = 0; i < counter.count; i++) {
      result.append("    }").append("\n");
    }
  }
  
}
