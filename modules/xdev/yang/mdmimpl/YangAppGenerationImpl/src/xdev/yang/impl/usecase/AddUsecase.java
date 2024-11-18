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



import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_XMOMObjectDoesNotExist;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;

import xact.http.URLPath;
import xact.http.enums.httpmethods.HTTPMethod;
import xdev.yang.impl.Constants;
import xdev.yang.impl.GuiHttpInteraction;
import xmcp.processmodeller.datatypes.response.GetDataTypeResponse;
import xmcp.processmodeller.datatypes.response.UpdateXMOMItemResponse;
import xprc.xpce.Workspace;



public class AddUsecase {

  public void addUsecase(String fqn, String usecaseName, Workspace workspace, XynaOrderServerExtension order, String rpc, String deviceFqn, String rpcNs) {
    String label = fqn.substring(fqn.lastIndexOf(".") + 1);
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    UsecaseCreationParameter parameter = new UsecaseCreationParameter();
    parameter.deviceFqn = deviceFqn;
    parameter.label = label;
    parameter.order = order;
    parameter.path = path;
    parameter.rpc = rpc;
    parameter.rpcNs = rpcNs;
    parameter.workspace = workspace.getName();
    createUsecase(parameter);
  }
  
  public Usecase createUsecase(UsecaseCreationParameter parameter) {
    String path = parameter.path;
    String label = parameter.label;
    String workspace = parameter.workspace;
    XynaOrderServerExtension order = parameter.order;
    String usecase = parameter.usecase;
    String fqn = path + "." + label;
    try {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = revMgmt.getRevision(null, null, workspace);
      DOM dom = DOM.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
      if (!doesDomExist(dom)) {
        String tmpPath = createDatatype(label, workspace, order);
        addParentToDatatype(tmpPath, label, workspace, order);
        GuiHttpInteraction.saveDatatype(tmpPath, path, label, workspace, "datatypes", order);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    addUsecaseToDatatype(parameter);
    Usecase result = Usecase.open(order, fqn, workspace, usecase);;
    result.addInput("xmcp.yang.MessageId");
    result.addOutput("xact.templates.Document");
    result.updateImplementation("return null;");
    String meta = createMetaTag(result, parameter.deviceFqn);
    result.setMetaTag(meta);
    result.save();
    return result;
  }


  private String createMetaTag(Usecase usecase, String deviceFqn) {
    String mappings = "<" + Constants.TAG_MAPPINGS + "/>";
    String device = "<" + Constants.TAG_DEVICE_FQN + ">" + deviceFqn + "</" + Constants.TAG_DEVICE_FQN + ">";
    String rpcNsTag = "<" + Constants.TAG_RPC_NS + ">" + usecase.getRpcNamespace() + "</" + Constants.TAG_RPC_NS + ">";
    String rpcTag = "<" + Constants.TAG_RPC + ">" + usecase.getRpcName() + "</" + Constants.TAG_RPC + ">";
    String YangStartTag = "<" + Constants.TAG_YANG + " " + Constants.ATT_YANG_TYPE + "=\\\"" + Constants.VAL_USECASE + "\\\">";
    return YangStartTag + rpcTag + device + rpcNsTag + mappings + "</" + Constants.TAG_YANG + ">";
  }


  private static boolean doesDomExist(DOM dom) {
    try {
      dom.parseGeneration(false, false);
      return dom.exists();
    } catch (XPRC_XMOMObjectDoesNotExist e) {
      return false;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private static String createDatatype(String label, String workspaceName, XynaOrderServerExtension order) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = GuiHttpInteraction.urlEncode(workspaceName);
    URLPath url = new URLPath("/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes", null, null);
    HTTPMethod method = GuiHttpInteraction.METHOD_POST;
    String payload = "{\"label\":\"" + label + "\"}";
    GetDataTypeResponse json;
    json = (GetDataTypeResponse) GuiHttpInteraction.executeRunnable(runnable, url, method, payload, "Could not create datatype.");
    if (json == null) {
      throw new RuntimeException("Could not create datatype.");
    }
    String fqn = json.getXmomItem().getFqn();
    return fqn.substring(0, fqn.lastIndexOf("."));
  }
  
  private static void addParentToDatatype(String path, String label, String workspace, XynaOrderServerExtension order) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = GuiHttpInteraction.urlEncode(workspace);
    String fqnUrl = path + "/" + label;
    String endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + fqnUrl + "/objects/typeInfoArea/change";
    URLPath url = new URLPath(endPoint, null, null);
    String payload = "{\"baseType\":\"xmcp.yang.YangUsecaseImplementation\"}";
    GuiHttpInteraction.executeRunnable(runnable, url, GuiHttpInteraction.METHOD_PUT, payload, "Could not add supertype to datatype.");
  }
  
  public static void addUsecaseToDatatype(UsecaseCreationParameter parameter) {
    String rpcNs = parameter.rpcNs;
    String deviceFqn = parameter.deviceFqn;
    String rpc = parameter.rpc;
    String workspaceName = parameter.workspace;
    String path = parameter.path;
    String label = parameter.label;
    rpcNs = rpcNs == null || rpcNs.isBlank() ? UseCaseAssignmentUtils.loadRpcNs(rpc, deviceFqn, workspaceName) : rpcNs;

    UpdateXMOMItemResponse json = createService(parameter);
    String serviceNumber = String.valueOf(GuiHttpInteraction.loadServiceId(json, rpc));
    if (serviceNumber.equals("-1")) {
      throw new RuntimeException("could not add service " + rpc + " to datatype " + path + "." + label);
    }
  }


  private static UpdateXMOMItemResponse createService(UsecaseCreationParameter parameter) {
    String rpc = parameter.rpc;
    String workspaceNameEscaped = GuiHttpInteraction.urlEncode(parameter.workspace);
    String fqnUrl = parameter.path + "/" + parameter.label;
    RunnableForFilterAccess runnable = parameter.order.getRunnableForFilterAccess("H5XdevFilter");
    String endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/servicegroups/" + fqnUrl + "/objects/memberMethodsArea/insert";
    URLPath url = new URLPath(endPoint, null, null);
    String payload = "{\"index\":-1,\"content\":{\"type\":\"memberService\",\"label\":\"" + rpc + "\"}}";
    String errorMsg = "Could not add service to datatype.";
    HTTPMethod method = GuiHttpInteraction.METHOD_POST;
    return (UpdateXMOMItemResponse) GuiHttpInteraction.executeRunnable(runnable, url, method, payload, errorMsg);
  }
  
  
  public static class UsecaseCreationParameter {

    private XynaOrderServerExtension order;
    private String path;
    private String label;
    private String workspace;
    private String usecase;
    private String rpc;
    private String rpcNs;
    private String deviceFqn;
  }

}
