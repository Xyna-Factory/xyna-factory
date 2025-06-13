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
package xdev.yang.impl.operation;



import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xact.http.URLPath;
import xact.http.URLPathQuery;
import xact.http.enums.httpmethods.HTTPMethod;
import xdev.yang.impl.GuiHttpInteraction;
import xdev.yang.impl.operation.AsyncDeployment.DeployData;
import xmcp.processmodeller.datatypes.response.GetServiceGroupResponse;



public class Operation implements AutoCloseable {

  private String path;
  private String label;
  private String name;
  private String fqnUrl;
  private String workspaceNameEscaped;
  private String operationName;
  private String serviceNumber;
  private int metaTagIndex;
  private Document meta;
  private String rpcName;
  private String rpcNamespace;
  private String baseUrl;
  private List<String> inputVarNames;
  private RunnableForFilterAccess runnable;


  
  public String getPath() {
    return path;
  }


  
  public String getLabel() {
    return label;
  }


  
  public String getName() {
    return name;
  }


  
  public String getFqnUrl() {
    return fqnUrl;
  }


  
  public String getWorkspaceNameEscaped() {
    return workspaceNameEscaped;
  }


  
  public String getOperationName() {
    return operationName;
  }


  
  public String getServiceNumber() {
    return serviceNumber;
  }


  
  
  public int getMetaTagIndex() {
    return metaTagIndex;
  }



  
  public Document getMeta() {
    return meta;
  }



  public String getRpcName() {
    return rpcName;
  }


  
  public String getRpcNamespace() {
    return rpcNamespace;
  }


  
  public String getBaseUrl() {
    return baseUrl;
  }


  public List<String> getInputVarNames() {
    return inputVarNames;
  }


  public RunnableForFilterAccess getRunnable() {
    return runnable;
  }


  private Operation() {
  }


  public static Operation open(XynaOrderServerExtension order, String fqn, String workspace, String operation) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = GuiHttpInteraction.urlEncode(workspace);
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    String name = fqn.substring(fqn.lastIndexOf(".") + 1);
    String endpoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/servicegroups/" + path + "/" + name;
    URLPath url = new URLPath(endpoint, null, null);
    HTTPMethod method = GuiHttpInteraction.METHOD_GET;
    GetServiceGroupResponse obj = (GetServiceGroupResponse) executeRunnable(runnable, url, method, null, "could not open datatype");
    Pair<Integer, Document> meta = OperationAssignmentUtils.loadOperationMeta(fqn, workspace, operation);
    Operation result = new Operation();
    result.path = path;
    result.label = obj.getXmomItem().getLabel();
    result.name = name;
    result.fqnUrl = path + "/" + name;
    result.workspaceNameEscaped = workspaceNameEscaped;
    result.operationName = operation;
    result.serviceNumber = GuiHttpInteraction.loadServiceId(obj, operation);
    result.metaTagIndex = meta.getFirst();
    result.meta = meta.getSecond();
    result.rpcName = OperationAssignmentUtils.readRpcName(meta.getSecond());
    result.rpcNamespace = OperationAssignmentUtils.readRpcNamespace(meta.getSecond());
    result.runnable = runnable;
    result.baseUrl = "/runtimeContext/" + result.workspaceNameEscaped + "/xmom/servicegroups/" + result.fqnUrl;
    result.inputVarNames = GuiHttpInteraction.loadVarNames(obj, Integer.valueOf(result.serviceNumber));
    return result;
  }

  private void reloadInputVarNames() {
    URLPath url = new URLPath(baseUrl, null, null);
    HTTPMethod method = GuiHttpInteraction.METHOD_GET;
    GetServiceGroupResponse obj = (GetServiceGroupResponse) executeRunnable(runnable, url, method, null, "could not open datatype");
    inputVarNames = GuiHttpInteraction.loadVarNames(obj, Integer.valueOf(serviceNumber));
  }

  
  public void addOutput(String varName, String fqn) {
    URLPath url = new URLPath(baseUrl + "/objects/methodVarArea" + serviceNumber + "_output/insert", null, null);
    String payload = "{\"index\":-1,\"content\":{\"type\":\"variable\",\"label\":\"" + varName + 
                     "\",\"fqn\":\"" + fqn + "\",\"isList\":false}}";
    executeRunnable(runnable, url, GuiHttpInteraction.METHOD_POST, payload, "Could not add output variable to service.");
    reloadInputVarNames();
  }


  public void addInput(String varName, String fqn) {
    String endPoint = baseUrl + "/objects/methodVarArea" + serviceNumber + "_input/insert";
    URLPath url = new URLPath(endPoint, null, null);
    String payload = "{\"index\":-1,\"content\":{\"type\":\"variable\",\"label\":\""+varName+"\",\"fqn\":\"" + fqn+ "\",\"isList\":false}}";
    executeRunnable(runnable, url, GuiHttpInteraction.METHOD_POST, payload, "Could not add input variable to service.");
    reloadInputVarNames();
  }
  
  public void deleteInput(int indexOfAdditionalInput) {
    int absoluteIndex = indexOfAdditionalInput + 1;
    String endPoint = baseUrl + "/objects/var" + serviceNumber + "-in" + absoluteIndex + "/delete";
    URLPath url = new URLPath(endPoint, null, null);
    String payload = "{\"force\":false}";
    executeRunnable(runnable, url, GuiHttpInteraction.METHOD_POST, payload, "Could not remove input variable from service.");
    reloadInputVarNames();
  }


  public void save() {
    URLPath url = new URLPath(baseUrl + "/save", null, null);
    String payload = "{\"force\":false,\"revision\":2,\"path\":\"" + path + "\",\"label\":\"" + label + "\"}";
    executeRunnable(runnable, url, GuiHttpInteraction.METHOD_POST, payload, "Could not save datatype.");
    reloadInputVarNames();
  }
  
  public void deploy() {
    URLPath url = new URLPath(baseUrl + "/deploy", null, null);
    String payload = "{\"revision\":3}";
    if(AsyncDeployment.PROP_ASYNC_DEPLOY.get()) {
      DeployData data = new DeployData(getRunnable(), url);
      AsyncDeployment.getInstance().requestAsyncDeploy(data);
      return;
    }
    executeRunnable(runnable, url, GuiHttpInteraction.METHOD_POST, payload, "Could not deploy datatype.");
  }


  public void updateImplementation(String impl) {
    URLPath url = new URLPath(baseUrl + "/objects/memberMethod" + serviceNumber + "/change", null, null);
    String newImpl = impl.replaceAll("\n", "\\\\n").replaceAll("\"", "\\\\\"");
    newImpl = "{ \"implementation\": \"" + newImpl + "\"}";
    executeRunnable(runnable, url, GuiHttpInteraction.METHOD_PUT, newImpl, "could not update implementation");
  }


  public void updateMeta() {
    String xml = XMLUtils.getXMLString(meta.getDocumentElement(), false);
    //remove old meta tag
    String endpoint = baseUrl + "/services/" + operationName + "/meta";
    List<URLPathQuery> query = new ArrayList<>();
    query.add(new URLPathQuery.Builder().attribute("metaTagId").value("metaTag"+metaTagIndex).instance());
    URLPath url = new URLPath(endpoint, query, null);
    GuiHttpInteraction.executeRunnable(runnable, url, GuiHttpInteraction.METHOD_DELETE, "", "could not remove old meta tag");

    //add new meta tag
    url = new URLPath(endpoint, null, null);
    xml = xml.replaceAll("\n", "\\\\n").replaceAll("\"", "\\\\\"");
    String payload = "{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.request.MetaTagRequest\"},"
        + "\"metaTag\":{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.MetaTag\"},\"deletable\":true,\"tag\":\"" + xml + "\"}}";
    executeRunnable(url, GuiHttpInteraction.METHOD_PUT, payload, "could not add new meta tag");
  }

  @Override
  public void close() throws Exception {
    URLPath url = new URLPath(baseUrl + "/close", null, null);
    executeRunnable(url, GuiHttpInteraction.METHOD_POST, null, "Could not close datatype.");
  }

  private Object executeRunnable(URLPath url, HTTPMethod method, String payload, String msg) {
    return executeRunnable(runnable, url, method, payload, msg);
  }


  private static Object executeRunnable(RunnableForFilterAccess runnable, URLPath url, HTTPMethod method, String payload, String msg) {
    try {
      return runnable.execute(url, method, payload);
    } catch (XynaException e) {
      throw new RuntimeException(msg, e);
    }
  }


  @Override
  public String toString() {
    return new StringBuilder().append("[Operation: '").append(operationName).append("': ").append(path).append(".").append(name).toString();
  }



}
