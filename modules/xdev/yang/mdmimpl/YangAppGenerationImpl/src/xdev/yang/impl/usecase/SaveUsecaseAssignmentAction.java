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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import xdev.yang.impl.Constants;
import xdev.yang.impl.GuiHttpInteraction;
import xmcp.processmodeller.datatypes.response.GetServiceGroupResponse;
import xmcp.yang.UseCaseAssignmentTableData;

public class SaveUsecaseAssignmentAction {

  public void saveUsecaseAssignment(XynaOrderServerExtension order, UseCaseAssignmentTableData data) {
    String fqn = data.getLoadYangAssignmentsData().getFqn();
    String workspaceName = data.getLoadYangAssignmentsData().getWorkspaceName();
    String usecase = data.getLoadYangAssignmentsData().getUsecase();
    String totalYangPath = data.getLoadYangAssignmentsData().getTotalYangPath();
    String totalNamespaces = data.getLoadYangAssignmentsData().getTotalNamespaces();
    boolean update = false;
    Pair<Integer, Document> meta = UseCaseAssignmentUtils.loadOperationMeta(fqn, workspaceName, usecase);
    if(meta == null) {
      return;
    }
    List<Element> mappings = UseCaseMapping.loadMappingElements(meta.getSecond());
    List<Pair<String, String>> pathList = UseCaseMapping.createPathList(totalYangPath, totalNamespaces);
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
      UseCaseMapping mapping = new UseCaseMapping(totalYangPath, totalNamespaces, data.getValue());
      mapping.createAndAddElement(meta.getSecond());
    }
    
    //write updated meta back to Datatype
    String xml = XMLUtils.getXMLString(meta.getSecond().getDocumentElement(), false);
    GuiHttpInteraction.updateAssignmentsMeta(order, fqn, workspaceName, usecase, xml, meta.getFirst());
    updateUsecaseImpl(order, fqn, workspaceName, usecase, meta);
  }

  private void updateUsecaseImpl(XynaOrderServerExtension order, String fqn, String workspaceName, String usecase, Pair<Integer, Document> meta) {
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    String label = fqn.substring(fqn.lastIndexOf(".") + 1);
    
    //open datatype
    GetServiceGroupResponse response = (GetServiceGroupResponse)GuiHttpInteraction.openDatatype(order, fqn, workspaceName, "servicegroups");
    Integer id = Integer.valueOf(GuiHttpInteraction.loadServiceId(response, usecase));
    List<String> inputVarNames = GuiHttpInteraction.loadVarNames(response, id);

    //Update implementation
    String newImpl = createImpl(meta.getSecond(), inputVarNames);
    GuiHttpInteraction.updateImplementation(order, fqn, workspaceName, "servicegroups", id, newImpl);

    GuiHttpInteraction.saveDatatype(path, path, label, workspaceName, "servicegroups", order);
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
        .append("builder.endAttributes();\n");

    createMappingImpl(result, meta);
    
    result
        .append("builder.endElement(\"").append(rpcName).append("\");\n")
        .append("builder.endElement(\"rpc\");\n")
        .append("return new Document.Builder().documentType(new xact.templates.NETCONF()).text(builder.toString()).instance();\n");

    return result.toString();
  }

  private void createMappingImpl(StringBuilder result, Document meta) {
    String rpcName = UseCaseAssignmentUtils.readRpcName(meta);
    String rpcNs = UseCaseAssignmentUtils.readRpcNamespace(meta);
    List<UseCaseMapping> mappings = UseCaseMapping.loadMappings(meta);
    Collections.sort(mappings);
    
    List<Pair<String, String>> position = new ArrayList<>();
    position.add(new Pair<>(rpcName, rpcNs));
    for(UseCaseMapping mapping : mappings) {
      createMappingImpl(result, mapping, position);
    }
    closeTags(result, position, 0);
  }

  private void createMappingImpl(StringBuilder result, UseCaseMapping mapping, List<Pair<String, String>> position) {
    result.append("\n//").append(mapping.getMappingYangPath()).append(" -> ").append(mapping.getValue()).append("\n");
    
    List<Pair<String, String>> mappingList = mapping.createPathList();
    mappingList.removeIf(x -> x.getFirst().startsWith("uses:")); // remove tags <uses:grouping_name> in the mapping implementation
    int insertIndex = 0;
    for (int i = 0; i < position.size(); i++) {
      if (mappingList.size() < i) {
        break;
      }
      Pair<String, String> curPos = position.get(i);
      Pair<String, String> mapPos = mappingList.get(i);
      if (!Objects.equals(curPos.getFirst(), mapPos.getFirst()) || !Objects.equals(curPos.getSecond(), mapPos.getSecond())) {
        break;
      }
      insertIndex = i;
    }

    if (insertIndex < position.size() -1) {
      closeTags(result, position, insertIndex);
    }

    for (int i = insertIndex + 1; i < mappingList.size(); i++) {
      result.append("builder.startElementWithAttributes(\"").append(mappingList.get(i).getFirst()).append("\");\n");
      result.append("builder.addAttribute(\"xmlns\", \"").append(mappingList.get(i).getSecond()).append("\");\n");
      if (i != mappingList.size() - 1) { //do not close the final tag, because we want to set the value
        result.append("builder.endAttributes();\n");
        position.add(mappingList.get(i)); //we close the final tag. As a result, we do not need to add that position
      }
    }
    String tag = mappingList.get(mappingList.size() - 1).getFirst();
    result.append("builder.endAttributesAndElement(\"").append(mapping.getValue()).append("\", \"").append(tag).append("\");\n");
  }


  private void closeTags(StringBuilder sb, List<Pair<String, String>> tags, int index) {
    for (int i = tags.size()-1; i > index; i--) {
      sb.append("builder.endElement(\"").append(tags.get(i).getFirst()).append("\");\n");
      tags.remove(i);
    }
  }


  
}
