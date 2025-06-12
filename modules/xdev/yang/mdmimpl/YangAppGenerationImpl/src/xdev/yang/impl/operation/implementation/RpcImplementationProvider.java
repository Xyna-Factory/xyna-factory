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
package xdev.yang.impl.operation.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Document;

import xdev.yang.impl.Constants;
import xdev.yang.impl.operation.ListConfiguration;
import xdev.yang.impl.operation.ListConfiguration.DynamicListLengthConfig;
import xdev.yang.impl.operation.MappingPathElement;
import xdev.yang.impl.operation.OperationAssignmentUtils;
import xdev.yang.impl.operation.OperationMapping;


public class RpcImplementationProvider implements ImplementationProvider {
  
  private final OpImplTools _tools = new OpImplTools();
  

  public String createImpl(Document meta, List<String> inputVarNames) {
    StringBuilder result = new StringBuilder();
    String rpcName = OperationAssignmentUtils.readRpcName(meta);
    String rpcNs = OperationAssignmentUtils.readRpcNamespace(meta);
    result
        .append("com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder builder = new com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder();\n")
        .append("builder.startElementWithAttributes(\"rpc\");\n")
        .append("builder.addAttribute(\"message-id\", ").append(inputVarNames.get(0)).append(".getId());\n")
        .append("builder.addAttribute(\"xmlns\", \"").append(Constants.NETCONF_NS).append("\");\n")
        .append("builder.endAttributes();\n")
        .append("builder.startElementWithAttributes(\"").append(rpcName).append("\");\n")
        .append("builder.addAttribute(\"xmlns\", \"").append(rpcNs).append("\");\n")
        .append("builder.endAttributes();\n\n");
    
    _tools.createVariables(result, meta, inputVarNames);
    createMappingImpl(result, meta);
    
    result
        .append("builder.endElement(\"").append(rpcName).append("\");\n")
        .append("builder.endElement(\"rpc\");\n")
        .append("return new Document.Builder().documentType(new xact.templates.NETCONF()).text(builder.toString()).instance();\n");

    return result.toString();
  }
  

  private void createMappingImpl(StringBuilder result, Document meta) {
    String rpcName = OperationAssignmentUtils.readRpcName(meta);
    String rpcNs = OperationAssignmentUtils.readRpcNamespace(meta);
    List<OperationMapping> mappings = OperationMapping.loadMappings(meta);
    List<ListConfiguration> listConfigs = ListConfiguration.loadListConfigurations(meta);
    Collections.sort(mappings);
    
    result.append("try {\n");
    
    List<MappingPathElement> position = new ArrayList<>();
    position.add(new MappingPathElement(rpcName, rpcNs, Constants.TYPE_RPC));
    ImplCreationData data = new ImplCreationData();
    data.position = position;
    data.nextVariable = 0;
    data.listConfigs = listConfigs;
    for(OperationMapping mapping : mappings) {
      createMappingImpl(result, mapping, data);
    }
    closeTags(result, data, 0);
    
    result
      .append("} catch(Exception e) {\n")
      .append("  throw new RuntimeException(e);\n")
      .append("}\n");
  }
  
  private void createMappingImpl(StringBuilder result, OperationMapping mapping, ImplCreationData data) {
    List<MappingPathElement> position = data.position;
    result.append("\n//").append(mapping.getMappingYangPath()).append(" -> ").append(mapping.getValue()).append("\n");
    
    List<MappingPathElement> mappingList = mapping.createPathList();
    int insertIndex = 0;
    for (int i = 0; i < position.size(); i++) {
      if (mappingList.size()-1 < i) {
        break;
      }
      MappingPathElement curPos = position.get(i);
      MappingPathElement mapPos = mappingList.get(i);
      boolean bothHidden = OpImplTools.hiddenYangKeywords.contains(curPos.getKeyword()) && OpImplTools.hiddenYangKeywords.contains(mapPos.getKeyword());
      if (!Objects.equals(curPos, mapPos) && !bothHidden) {
        break;
      }
      insertIndex = i;
    }

    if (insertIndex < position.size() -1) {
      closeTags(result, data, insertIndex);
    }

    for (int i = insertIndex + 1; i < mappingList.size(); i++) {
      String tag = _tools.cleanupTag(mappingList.get(i).getYangPath());
      ListConfiguration listConfig = _tools.isDynamicList(mappingList.subList(0, i + 1), data.listConfigs);
      if(listConfig != null) {
        DynamicListLengthConfig dynListConfig = (DynamicListLengthConfig)listConfig.getConfig();
        String loopVareName = dynListConfig.getVariable();
        String loopVarPath = _tools.determineMappingValueObject(dynListConfig.getPath());
        String counterVarName = String.format("i_%d", data.nextVariable++);
        result.append("List<?> ").append(loopVareName).append("_list = (List<?>)").append(loopVarPath).append(";\n")
          .append("for (int ").append(counterVarName).append(" = 0; ").append(counterVarName).append(" < ")
          .append(loopVareName).append("_list.size(); ").append(counterVarName).append("++) {\n")
          .append("Object ").append(loopVareName).append(" = ").append(loopVareName).append("_list.get(").append(counterVarName).append(");\n");
      }

      if(!OpImplTools.hiddenYangKeywords.contains(mappingList.get(i).getKeyword())) {
        result.append("builder.startElementWithAttributes(\"").append(tag).append("\");\n")
          .append("builder.addAttribute(\"xmlns\", \"").append(mappingList.get(i).getNamespace()).append("\");\n");
        if (i != mappingList.size() - 1) { //do not close the final tag, because we want to set the value
          result.append("builder.endAttributes();\n");
        }
      }

      if (i != mappingList.size() - 1) {
        position.add(mappingList.get(i)); //we will close the final tag. As a result, we do not need to add that position
      }

    }
    String tag = _tools.cleanupTag(mappingList.get(mappingList.size() - 1).getYangPath());
    String value = _tools.determineMappingString(mapping.getValue());
    result.append("builder.endAttributesAndElement(").append(value).append(", \"").append(tag).append("\");\n");
  }
  
  
  private void closeTags(StringBuilder sb, ImplCreationData data, int index) {
    List<MappingPathElement> tags = data.position;
    for (int i = tags.size() - 1; i > index; i--) {
      MappingPathElement element = tags.get(i);

      if (!OpImplTools.hiddenYangKeywords.contains(element.getKeyword())) {
        String tag = _tools.cleanupTag(element.getYangPath());
        sb.append("builder.endElement(\"").append(tag).append("\");\n");

      }

      if (_tools.isDynamicList(tags.subList(0, i + 1), data.listConfigs) != null) {
        sb.append("}\n");
      }

      if (!OpImplTools.hiddenYangKeywords.contains(element.getKeyword())) {
        tags.remove(i);
      }
    }
  }


  private static class ImplCreationData {

    private List<MappingPathElement> position;
    private int nextVariable;
    private List<ListConfiguration> listConfigs;
  }
}
