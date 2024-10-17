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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xact.http.URLPath;
import xact.http.URLPathQuery;
import xact.http.enums.httpmethods.DELETE;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.httpmethods.PUT;
import xmcp.yang.UseCaseAssignementTableData;

public class SaveUsecaseAssignmentAction {

  public void saveUsecaseAssignment(XynaOrderServerExtension order, UseCaseAssignementTableData data) {
    String fqn = data.getLoadYangAssignmentsData().getFqn();
    String workspaceName = data.getLoadYangAssignmentsData().getWorkspaceName();
    String usecase = data.getLoadYangAssignmentsData().getUsecase();
    String totalYangPath = data.getLoadYangAssignmentsData().getTotalYangPath();
    boolean update = false;
    Pair<Integer, Document> meta = UseCaseAssignmentUtils.loadOperationMeta(fqn, workspaceName, usecase);
    if(meta == null) {
      return;
    }
    List<Element> mappings = UseCaseMapping.loadMappingElements(meta.getSecond());
    for(Element mappingEle : mappings) {
      UseCaseMapping mapping = UseCaseMapping.loadUseCaseMapping(mappingEle);
      mapping.setValue(data.getValue());
      if(mapping.getMappingYangPath().equals(totalYangPath)) {
        mapping.updateNode(mappingEle);
        update = true;
        break;
      }
    }
    if(!update) {
      //TODO: name space
      UseCaseMapping mapping = new UseCaseMapping(totalYangPath, "", data.getValue());
      mapping.createAndAddElement(meta.getSecond());
    }
    
    //write updated meta back to Datatype
    String xml = XMLUtils.getXMLString(meta.getSecond().getDocumentElement(), false);
    updateAssignmentsMeta(order, fqn, workspaceName, usecase, xml, meta.getFirst());
  }

  private void updateAssignmentsMeta(XynaOrderServerExtension order, String fqn, String workspaceName, String usecase, String xml, int oldMetaTagIndex) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    String label = fqn.substring(fqn.lastIndexOf(".") + 1);
    String workspaceNameEscaped = UseCaseAssignmentUtils.urlEncode(workspaceName);
    //remove old meta tag
    String endpoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + path + "/" + label + "/services/" + usecase + "/meta";
    List<URLPathQuery> query = new ArrayList<>();
    query.add(new URLPathQuery.Builder().attribute("metaTagId").value("metaTag"+oldMetaTagIndex).instance());
    URLPath url = new URLPath(endpoint, query, null);
    HTTPMethod method = new DELETE();
    String payload = "";
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "could not remove old meta tag");

    //add new meta tag
    url = new URLPath(endpoint, null, null);
    method = new PUT();
    xml = xml.replaceAll("\n", "\\\\n").replaceAll("\"", "\\\\\"");
    payload = "{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.request.MetaTagRequest\"},\"metaTag\":{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.MetaTag\"},\"deletable\":true,\"tag\":\""+ xml +"\"}}";
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "could not add new meta tag");
    
    UseCaseAssignmentUtils.saveDatatype(path, path, label, workspaceName, order);
  }
}
