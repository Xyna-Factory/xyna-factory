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
import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import xdev.yang.impl.Constants;
import xmcp.yang.UseCaseAssignmentTableData;

public class SaveUsecaseAssignmentAction {
  
  public static final Set<String> hiddenYangKeywords = Set.of(
                                                              Constants.TYPE_GROUPING,
                                                              Constants.TYPE_USES,
                                                              Constants.TYPE_CHOICE, 
                                                              Constants.TYPE_CASE);


  public void saveUsecaseAssignment(XynaOrderServerExtension order, UseCaseAssignmentTableData data) {
    String fqn = data.getLoadYangAssignmentsData().getFqn();
    String workspaceName = data.getLoadYangAssignmentsData().getWorkspaceName();
    String usecaseName = data.getLoadYangAssignmentsData().getUsecase();
    String totalYangPath = data.getLoadYangAssignmentsData().getTotalYangPath();
    String totalNamespaces = data.getLoadYangAssignmentsData().getTotalNamespaces();
    String totalKeywords = data.getLoadYangAssignmentsData().getTotalKeywords();
    
    boolean update = false;
    Pair<Integer, Document> meta = UseCaseAssignmentUtils.loadOperationMeta(fqn, workspaceName, usecaseName);
    if(meta == null) {
      return;
    }
    List<Element> mappings = UseCaseMapping.loadMappingElements(meta.getSecond());
    List<MappingPathElement> pathList = UseCaseMapping.createPathList(totalYangPath, totalNamespaces, totalKeywords);
    for(Element mappingEle : mappings) {
      UseCaseMapping mapping = UseCaseMapping.loadUseCaseMapping(mappingEle);
      mapping.setValue(data.getValue());
      if(mapping.match(pathList)) {
        mapping.updateNode(mappingEle);
        update = true;
        break;
      }
    }
    if(!update) {
      UseCaseMapping mapping = new UseCaseMapping(totalYangPath, totalNamespaces, data.getValue(), totalKeywords);
      mapping.createAndAddElement(meta.getSecond());
    }
    
    
    try(Usecase usecase = Usecase.open(order, fqn, workspaceName, usecaseName)) {
      String xml = XMLUtils.getXMLString(meta.getSecond().getDocumentElement(), false);
      usecase.updateMeta(xml, meta.getFirst());
      String newImpl = createImpl(meta.getSecond(), usecase.getInputVarNames());
      usecase.updateImplementation(newImpl);
      usecase.save();
      usecase.deploy();
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }


  private String createImpl(Document meta, List<String> inputVarNames) {
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
    Collections.sort(mappings);
    
    result.append("try {\n");
    
    List<MappingPathElement> position = new ArrayList<>();
    position.add(new MappingPathElement(rpcName, rpcNs, Constants.TYPE_RPC));
    for(UseCaseMapping mapping : mappings) {
      createMappingImpl(result, mapping, position);
    }
    closeTags(result, position, 0);
    
    result
      .append("} catch(Exception e) {\n")
      .append("  throw new RuntimeException(e);\n")
      .append("}\n");
  }

  private void createMappingImpl(StringBuilder result, UseCaseMapping mapping, List<MappingPathElement> position) {
    result.append("\n//").append(mapping.getMappingYangPath()).append(" -> ").append(mapping.getValue()).append("\n");
    
    List<MappingPathElement> mappingList = mapping.createPathList();
    mappingList.removeIf(x -> hiddenYangKeywords.contains(x.getKeyword()));
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
      closeTags(result, position, insertIndex);
    }

    for (int i = insertIndex + 1; i < mappingList.size(); i++) {
      result.append("builder.startElementWithAttributes(\"").append(mappingList.get(i).getYangPath()).append("\");\n");
      result.append("builder.addAttribute(\"xmlns\", \"").append(mappingList.get(i).getNamespace()).append("\");\n");
      if (i != mappingList.size() - 1) { //do not close the final tag, because we want to set the value
        result.append("builder.endAttributes();\n");
        position.add(mappingList.get(i)); //we close the final tag. As a result, we do not need to add that position
      }
    }
    String tag = mappingList.get(mappingList.size() - 1).getYangPath();
    String value = determineMappingValue(mapping.getValue());
    result.append("builder.endAttributesAndElement(").append(value).append(", \"").append(tag).append("\");\n");
  }
  

  private String determineMappingValue(String mappingValue) {
    int firstDot = mappingValue.indexOf(".");
    if (firstDot == -1) {
      if (!mappingValue.startsWith("\"")) {
        mappingValue = String.format("\"%s\"", mappingValue);
      }
      return mappingValue;
    } else {
      String variable = mappingValue.substring(0, firstDot);
      String path = mappingValue.substring(firstDot + 1);
      return String.format("String.valueOf(%s.get(\"%s\"))", variable, path);
    }
  }


  private void closeTags(StringBuilder sb, List<MappingPathElement> tags, int index) {
    for (int i = tags.size()-1; i > index; i--) {
      sb.append("builder.endElement(\"").append(tags.get(i).getYangPath()).append("\");\n");
      tags.remove(i);
    }
  }


  
}
