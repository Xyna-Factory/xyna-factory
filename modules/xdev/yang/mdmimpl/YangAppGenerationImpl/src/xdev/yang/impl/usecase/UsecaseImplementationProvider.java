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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.w3c.dom.Document;

import xdev.yang.impl.Constants;
import xdev.yang.impl.usecase.ListConfiguration.DynamicListLengthConfig;



public class UsecaseImplementationProvider {
  public static final Set<String> hiddenYangKeywords = Set.of(
                                                              Constants.TYPE_GROUPING,
                                                              Constants.TYPE_USES,
                                                              Constants.TYPE_CHOICE, 
                                                              Constants.TYPE_CASE,
                                                              Constants.TYPE_LEAFLIST,
                                                              Constants.TYPE_LIST);

  public String createImpl(Document meta, List<String> inputVarNames) {
    StringBuilder result = new StringBuilder();
    String rpcName = UseCaseAssignmentUtils.readRpcName(meta);
    String rpcNs = UseCaseAssignmentUtils.readRpcNamespace(meta);
    result
        .append("com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder builder = new com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder();\n")
        .append("builder.startElementWithAttributes(\"rpc\");\n")
        .append("builder.addAttribute(\"message-Id\", ").append(inputVarNames.get(0)).append(".getId());\n")
        .append("builder.addAttribute(\"xmlns\", \"").append(Constants.NETCONF_NS).append("\");\n")
        .append("builder.endAttributes();\n")
        .append("builder.startElementWithAttributes(\"").append(rpcName).append("\");\n")
        .append("builder.addAttribute(\"xmlns\", \"").append(rpcNs).append("\");\n")
        .append("builder.endAttributes();\n\n");
    
    createVariables(result, meta, inputVarNames);
    createMappingImpl(result, meta);
    
    result
        .append("builder.endElement(\"").append(rpcName).append("\");\n")
        .append("builder.endElement(\"rpc\");\n")
        .append("return new Document.Builder().documentType(new xact.templates.NETCONF()).text(builder.toString()).instance();\n");

    return result.toString();
  }
  
  private void createVariables(StringBuilder result, Document meta, List<String> inputVarNames) {
    List<UsecaseSignatureVariable> variables = UsecaseSignatureVariable.loadSignatureEntries(meta, Constants.VAL_LOCATION_INPUT);  
    for (int i = 0; i < variables.size(); i++) {
      UsecaseSignatureVariable variable = variables.get(i);
      String serviceInputVarName = inputVarNames.get(i + 1);
      String fqn = variable.getFqn();
      String customVarName = variable.getVarName();
      result.append(fqn).append(" ").append(customVarName).append(" = ").append(serviceInputVarName).append(";\n");
    }
  }

  private void createMappingImpl(StringBuilder result, Document meta) {
    String rpcName = UseCaseAssignmentUtils.readRpcName(meta);
    String rpcNs = UseCaseAssignmentUtils.readRpcNamespace(meta);
    List<UseCaseMapping> mappings = UseCaseMapping.loadMappings(meta);
    List<ListConfiguration> listConfigs = ListConfiguration.loadListConfigurations(meta);
    Collections.sort(mappings);
    
    result.append("try {\n");
    
    List<MappingPathElement> position = new ArrayList<>();
    position.add(new MappingPathElement(rpcName, rpcNs, Constants.TYPE_RPC));
    ImplCreationData data = new ImplCreationData();
    data.position = position;
    data.nextVariable = 0;
    data.listConfigs = listConfigs;
    for(UseCaseMapping mapping : mappings) {
      createMappingImpl(result, mapping, data);
    }
    closeTags(result, data, 0);
    
    result
      .append("} catch(Exception e) {\n")
      .append("  throw new RuntimeException(e);\n")
      .append("}\n");
  }
  
  private void createMappingImpl(StringBuilder result, UseCaseMapping mapping, ImplCreationData data) {
    List<MappingPathElement> position = data.position;
    result.append("\n//").append(mapping.getMappingYangPath()).append(" -> ").append(mapping.getValue()).append("\n");
    
    List<MappingPathElement> mappingList = mapping.createPathList();
    int insertIndex = 0;
    for (int i = 0; i < position.size(); i++) {
      if (mappingList.size() < i) {
        break;
      }
      MappingPathElement curPos = position.get(i);
      MappingPathElement mapPos = mappingList.get(i);
      if (!Objects.equals(curPos, mapPos)) {
        break;
      }
      insertIndex = i;
    }

    if (insertIndex < position.size() -1) {
      closeTags(result, data, insertIndex);
    }

    for (int i = insertIndex + 1; i < mappingList.size(); i++) {
      String tag = cleanupTag(mappingList.get(i).getYangPath());
      ListConfiguration listConfig = isDynamicList(mappingList.subList(0, i + 1), data.listConfigs);
      if(listConfig != null) {
        DynamicListLengthConfig dynListConfig = (DynamicListLengthConfig)listConfig.getConfig();
        String loopVareName = dynListConfig.getVariable();
        String loopVarPath = determineMappingValueObject(dynListConfig.getPath());
        String counterVarName = String.format("i_%d", data.nextVariable++);
        result.append("List<?> ").append(loopVareName).append("_list = (List<?>)").append(loopVarPath).append(";\n")
          .append("for (int ").append(counterVarName).append(" = 0; ").append(counterVarName).append(" < ")
          .append(loopVareName).append("_list.size(); ").append(counterVarName).append("++) {\n")
          .append("Object ").append(loopVareName).append(" = ").append(loopVareName).append("_list.get(").append(counterVarName).append(");\n");
        position.add(mappingList.get(i)); // dynamic lists are hidden, but we need to keep track of opened lists anyway
      } else if(Constants.TYPE_LIST.equals(mappingList.get(i).getKeyword())) {
        position.add(mappingList.get(i)); //static complex list
      }
      if(!hiddenYangKeywords.contains(mappingList.get(i).getKeyword())) {
        result.append("builder.startElementWithAttributes(\"").append(tag).append("\");\n")
          .append("builder.addAttribute(\"xmlns\", \"").append(mappingList.get(i).getNamespace()).append("\");\n");
        if (i != mappingList.size() - 1) { //do not close the final tag, because we want to set the value
          result.append("builder.endAttributes();\n");
          position.add(mappingList.get(i)); //we close the final tag. As a result, we do not need to add that position
        }
      }
    }
    String tag = cleanupTag(mappingList.get(mappingList.size() - 1).getYangPath());
    String value = determineMappingString(mapping.getValue());
    result.append("builder.endAttributesAndElement(").append(value).append(", \"").append(tag).append("\");\n");
  }
  
  
  private ListConfiguration isDynamicList(List<MappingPathElement> mappingElements, List<ListConfiguration> listConfigs) {
    String keyword = mappingElements.get(mappingElements.size()-1).getKeyword();
    if (Constants.TYPE_LEAFLIST.equals(keyword) || Constants.TYPE_LIST.equals(keyword)) {
      for (ListConfiguration listConfig : listConfigs) {
        if (!(listConfig.getConfig() instanceof DynamicListLengthConfig)) {
          continue;
        }
        List<MappingPathElement> listPath = UseCaseMapping.createPathList(listConfig.getYang(), listConfig.getNamespaces(), listConfig.getKeywords());
        if (MappingPathElement.compareLists(mappingElements, listPath) == 0) {
          return listConfig;
        }
      }
    }
    return null;
  }
  

  private String cleanupTag(String tag) {
    int listIndexSeparatorIndex = tag.indexOf(Constants.LIST_INDEX_SEPARATOR);
    if (listIndexSeparatorIndex > 0) {
      tag = tag.substring(listIndexSeparatorIndex + Constants.LIST_INDEX_SEPARATOR.length());
    }
    return tag;
  }
  

  private String determineMappingValueObject(String mappingValue) {
    int firstDot = mappingValue.indexOf(".");
    if (firstDot == -1 || mappingValue.startsWith("\"")) {
      return mappingValue;
    } else {
      String variable = mappingValue.substring(0, firstDot);
      String path = mappingValue.substring(firstDot + 1);
      return String.format("%s.get(\"%s\")", variable, path);
    }
  }


  private String determineMappingString(String mappingValue) {
    int firstDot = mappingValue.indexOf(".");
    if (firstDot == -1) {
      if (mappingValue.startsWith("\"")) {
        return mappingValue;
      } else {
        return String.format("String.valueOf(%s)", mappingValue);
      }
    } else {
      String variable = mappingValue.substring(0, firstDot);
      String path = mappingValue.substring(firstDot + 1);
      return String.format("String.valueOf(((GeneralXynaObject)%s).get(\"%s\"))", variable, path);
    }
  }


  private void closeTags(StringBuilder sb, ImplCreationData data, int index) {
    List<MappingPathElement> tags = data.position;
    for (int i = tags.size() - 1; i > index; i--) {
      MappingPathElement element = tags.get(i);

      if (!hiddenYangKeywords.contains(element.getKeyword())) {
        String tag = cleanupTag(element.getYangPath());
        sb.append("builder.endElement(\"").append(tag).append("\");\n");

      }

      if (isDynamicList(tags.subList(0, i + 1), data.listConfigs) != null) {
        sb.append("}\n");
      }

      if (!hiddenYangKeywords.contains(element.getKeyword())) {
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
