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



import java.util.List;

import org.apache.log4j.Logger;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.Rpc;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_XMOMObjectDoesNotExist;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;

import xact.http.URLPath;
import xact.http.enums.httpmethods.GET;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.httpmethods.POST;
import xact.http.enums.httpmethods.PUT;
import xdev.yang.impl.Constants;
import xdev.yang.impl.GuiHttpInteraction;
import xmcp.processmodeller.datatypes.response.GetDataTypeResponse;
import xmcp.processmodeller.datatypes.response.UpdateXMOMItemResponse;
import xprc.xpce.Workspace;



public class AddUsecase {

  private static final Logger logger = CentralFactoryLogging.getLogger(AddUsecase.class);


  public void addUsecase(String fqn, String usecaseName, Workspace workspace, XynaOrderServerExtension order, String rpc, String deviceFqn, String rpcNs) {
    try {
      String workspaceName = workspace.getName();
      if (logger.isDebugEnabled()) {
        logger.debug("addUsecase: " + usecaseName + " to " + fqn + " in workspace " + workspaceName + " using order: " + order.getId());
      }
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = revMgmt.getRevision(null, null, workspaceName);
      DOM dom = DOM.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
      String label = fqn.substring(fqn.lastIndexOf(".") + 1);
      String path = fqn.substring(0, fqn.lastIndexOf("."));
      String currentPath = path;
      if (logger.isDebugEnabled()) {
        logger.debug(order.getId() + ": Read from input: Workspace revision: " + revision + ", label: " + label + ", path: " + path);
      }
      try {
        if (!doesDomExist(dom)) {
          currentPath = createDatatype(label, workspaceName, order);
          addParentToDatatype(currentPath, label, workspaceName, order);
          UseCaseAssignmentUtils.saveDatatype(currentPath, path, label, workspaceName, "datatypes", order);
          currentPath = path;
        }

        if (logger.isDebugEnabled()) {
          logger.debug(order.getId() + ": Adding service to datatype. Current datatype path: " + currentPath);
        }

        rpcNs = rpcNs == null || rpcNs.isBlank() ? loadRpcNs(rpc, deviceFqn, workspaceName) : rpcNs;
        addServiceToDatatype(currentPath, label, usecaseName, workspaceName, order, rpc, deviceFqn, rpcNs);
        UseCaseAssignmentUtils.saveDatatype(currentPath, path, label, workspaceName, "servicegroups", order);
      } finally {
        if (logger.isDebugEnabled()) {
          logger.debug(order.getId() + ": Closing datatype.");
        }
        closeDatatype(currentPath, label, workspaceName, order);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String loadRpcNs(String rpc, String deviceFqn, String workspaceName) {
    List<Module> modules = UseCaseAssignmentUtils.loadModules(workspaceName);
    //filter modules to supported by device
    List<String> capabilities = UseCaseAssignmentUtils.loadCapabilities(deviceFqn, workspaceName);
    modules = UseCaseAssignmentUtils.filterModules(modules, capabilities);
    List<Rpc> candidates = UseCaseAssignmentUtils.findRpcs(modules, rpc);
    if(candidates.size() != 1) {
      throw new RuntimeException("Could not determine rpc namespace. There are " + candidates.size() + " candidates.");
    }
    return candidates.get(0).getContext().getNamespace().getUri().toString();
  }

  private void addParentToDatatype(String path, String label, String workspace, XynaOrderServerExtension order) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = UseCaseAssignmentUtils.urlEncode(workspace);
    String fqnUrl = path + "/" + label;
    String endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + fqnUrl + "/objects/typeInfoArea/change";
    URLPath url = new URLPath(endPoint, null, null);
    HTTPMethod method = new PUT();
    String payload = "{\"baseType\":\"xmcp.yang.YangUsecaseImplementation\"}";
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "Could not add supertype to datatype.");
  }

  private boolean doesDomExist(DOM dom) {
    try {
      dom.parseGeneration(false, false);
      return dom.exists();
    } catch(XPRC_XMOMObjectDoesNotExist e) {
      return false;
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void closeDatatype(String currentPath, String label, String workspaceName, XynaOrderServerExtension order) {
    if (logger.isDebugEnabled()) {
      logger.debug(order.getId() + ": Datatype does not exist");
    }
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = UseCaseAssignmentUtils.urlEncode(workspaceName);
    String endpoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + currentPath + "/" + label + "/close";
    URLPath url = new URLPath(endpoint, null, null);
    HTTPMethod method = new POST();
    String payload = "{\"force\":false,\"revision\":0}";
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "could not close datatype");

    if (logger.isDebugEnabled()) {
      logger.debug(order.getId() + ": Datatype created. Temporary path: " + currentPath);
    }
  }


  private String createDatatype(String label, String workspaceName, XynaOrderServerExtension order) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = UseCaseAssignmentUtils.urlEncode(workspaceName);
    URLPath url = new URLPath("/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes", null, null);
    HTTPMethod method = new POST();
    String payload = "{\"label\":\"" + label + "\"}";
    GetDataTypeResponse json;
    json = (GetDataTypeResponse) UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "Could not create datatype.");
    if (json == null) {
      throw new RuntimeException("Could not create datatype.");
    }
    String fqn = json.getXmomItem().getFqn();
    return fqn.substring(0, fqn.lastIndexOf("."));
  }

  private void addServiceToDatatype(String path, String label, String service, String workspace, XynaOrderServerExtension order, String rpc, String deviceFqn, String rpcNs) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = UseCaseAssignmentUtils.urlEncode(workspace);
    String fqnUrl = path + "/" + label;

    //open datatype as service group
    String endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/servicegroups/" + fqnUrl;
    URLPath url = new URLPath(endPoint, null, null);
    HTTPMethod method = new GET();
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, null, "Could not open service group.");

    // create service
    endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/servicegroups/" + fqnUrl + "/objects/memberMethodsArea/insert";
    url = new URLPath(endPoint, null, null);
    method = new POST();
    String payload = "{\"index\":-1,\"content\":{\"type\":\"memberService\",\"label\":\"" + service + "\"}}";
    UpdateXMOMItemResponse json;
    json = (UpdateXMOMItemResponse)UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "Could not add service to datatype.");

    String serviceNumber = String.valueOf(GuiHttpInteraction.loadServiceId(json, service));

    // add xmcp.yang.MessageId as input variable
    endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/servicegroups/" + fqnUrl + "/objects/methodVarArea" + serviceNumber + "_input/insert";
    url = new URLPath(endPoint, null, null);
    payload = "{\"index\":-1,\"content\":{\"type\":\"variable\",\"label\":\"MessageId\",\"fqn\":\"xmcp.yang.MessageId\",\"isList\":false}}";
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "Could not add input variable to service.");

    // add xact.templates.Document as output variable
    endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/servicegroups/" + fqnUrl + "/objects/methodVarArea" + serviceNumber + "_output/insert";
    url = new URLPath(endPoint, null, null);
    payload = "{\"index\":-1,\"content\":{\"type\":\"variable\",\"label\":\"Document\",\"fqn\":\"xact.templates.Document\",\"isList\":false}}";
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "Could not add output variable to service.");

    // set implementation to "return null;"
    endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/servicegroups/" + fqnUrl + "/objects/memberMethod" + serviceNumber + "/change";
    url = new URLPath(endPoint, null, null);
    payload = "{\"implementation\":\"return null;\"}";
    method = new PUT();
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "Could not set implementation of service.");

    // set meta tag
    endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/servicegroups/" + fqnUrl + "/services/" + service + "/meta";
    url = new URLPath(endPoint, null, null);
    String mappings = "<" + Constants.TAG_MAPPINGS + "/>";
    String device = "<" + Constants.TAG_DEVICE_FQN + ">" + deviceFqn + "</" + Constants.TAG_DEVICE_FQN + ">";
    String rpcNsTag = "<" + Constants.TAG_RPC_NS + ">" + rpcNs + "</" + Constants.TAG_RPC_NS + ">";
    String rpcTag = "<" + Constants.TAG_RPC + ">" + rpc + "</" + Constants.TAG_RPC + ">";
    String YangStartTag = "<"+ Constants.TAG_YANG + " " + Constants.ATT_YANG_TYPE + "=\\\"" + Constants.VAL_USECASE + "\\\">";
    String tag = YangStartTag + rpcTag + device + rpcNsTag + mappings +"</" + Constants.TAG_YANG + ">";
    payload = "{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.request.MetaTagRequest\"},\"metaTag\":{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.MetaTag\"},\"deletable\":true,\"tag\":\"" + tag + "\"}}";
    UseCaseAssignmentUtils.executeRunnable(runnable, url, method, payload, "Could not add meta tag to service.");
  }
}
