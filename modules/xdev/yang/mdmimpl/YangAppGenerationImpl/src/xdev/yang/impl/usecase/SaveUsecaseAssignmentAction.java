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
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import xact.http.URLPath;
import xact.http.URLPathQuery;
import xact.http.enums.httpmethods.DELETE;
import xact.http.enums.httpmethods.GET;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.httpmethods.PUT;
import xdev.yang.impl.Constants;
import xdev.yang.impl.GuiHttpInteraction;
import xmcp.processmodeller.datatypes.response.GetDataTypeResponse;
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
    updateAssignmentsMeta(order, fqn, workspaceName, usecase, xml, meta.getFirst());
    updateUsecaseImpl(order, fqn, workspaceName, usecase, meta);
  }

  private void updateUsecaseImpl(XynaOrderServerExtension order, String fqn, String workspaceName, String usecase, Pair<Integer, Document> meta) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    String label = fqn.substring(fqn.lastIndexOf(".") + 1);
    String workspaceNameEscaped = UseCaseAssignmentUtils.urlEncode(workspaceName);
    Integer id = meta.getFirst();
    String commonPath = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + path + "/" + label;
    
    //open datatype
    String endpoint = commonPath;
    URLPath url = new URLPath(endpoint, null, null);
    HTTPMethod method = new GET();
    GetDataTypeResponse response = (GetDataTypeResponse)UseCaseAssignmentUtils.executeRunnable(runnable, url, method, null, "could not open datatype");
    List<String> inputVarNames = GuiHttpInteraction.loadVarNames(response, meta.getFirst());
    
    //Update implementation
    String newImpl = createImpl(meta.getSecond(), inputVarNames);
    newImpl = newImpl.replaceAll("\n", "\\\\n").replaceAll("\"", "\\\\\"");
    newImpl = "{ \"implementation\": \"" + newImpl + "\"}";
    endpoint = commonPath + "/objects/memberMethod" + id + "/change";
    url = new URLPath(endpoint, null, null);
    method = new PUT();
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, newImpl, "could not open datatype");
    
    UseCaseAssignmentUtils.saveDatatype(path, path, label, workspaceName, order);
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


  private void updateAssignmentsMeta(XynaOrderServerExtension order, String fqn, String workspaceName, String usecase, String xml, int oldMetaTagIndex) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    String label = fqn.substring(fqn.lastIndexOf(".") + 1);
    String workspaceNameEscaped = UseCaseAssignmentUtils.urlEncode(workspaceName);

    //open datatype
    String endpoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + path + "/" + label;
    URLPath url = new URLPath(endpoint, null, null);
    HTTPMethod method = new GET();
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, null, "could not open datatype");
    
    //remove old meta tag
    endpoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + path + "/" + label + "/services/" + usecase + "/meta";
    List<URLPathQuery> query = new ArrayList<>();
    query.add(new URLPathQuery.Builder().attribute("metaTagId").value("metaTag"+oldMetaTagIndex).instance());
    url = new URLPath(endpoint, query, null);
    method = new DELETE();
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, "", "could not remove old meta tag");

    //add new meta tag
    url = new URLPath(endpoint, null, null);
    method = new PUT();
    xml = xml.replaceAll("\n", "\\\\n").replaceAll("\"", "\\\\\"");
    String payload = "{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.request.MetaTagRequest\"},\"metaTag\":{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.MetaTag\"},\"deletable\":true,\"tag\":\""+ xml +"\"}}";
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "could not add new meta tag");
    
    UseCaseAssignmentUtils.saveDatatype(path, path, label, workspaceName, order);
  }
}
