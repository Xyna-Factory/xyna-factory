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
package xdev.yang.impl;



import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_XMOMObjectDoesNotExist;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;

import base.Text;
import xact.http.URLPath;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.httpmethods.POST;
import xact.http.enums.httpmethods.PUT;
import xmcp.processmodeller.datatypes.response.GetDataTypeResponse;
import xprc.xpce.Workspace;



public class AddUsecase {

  private static final Logger logger = CentralFactoryLogging.getLogger(AddUsecase.class);


  public void addUsecase(String fqn, String usecaseName, Workspace workspace, XynaOrderServerExtension order, Text rpc) {
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
        }

        if (logger.isDebugEnabled()) {
          logger.debug(order.getId() + ": Adding service to datatype. Current datatype path: " + currentPath);
        }
        addParentToDatatype(currentPath, label, workspaceName, order);
        addServiceToDatatype(currentPath, label, usecaseName, workspaceName, order, rpc);
        saveDatatype(currentPath, path, label, workspaceName, order);
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

  private void addParentToDatatype(String path, String label, String workspace, XynaOrderServerExtension order) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = urlEncode(workspace);
    String fqnUrl = path + "/" + label;
    String endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + fqnUrl + "/objects/typeInfoArea/change";
    URLPath url = new URLPath(endPoint, null, null);
    HTTPMethod method = new PUT();
    String payload = "{\"baseType\":\"xmcp.yang.YangUsecaseImplementation\"}";
    executeRunnable(runnable, url, method, payload, "Could not add supertype to datatype.");
    
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
    String workspaceNameEscaped = urlEncode(workspaceName);
    String endpoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + currentPath + "/" + label + "/close";
    URLPath url = new URLPath(endpoint, null, null);
    HTTPMethod method = new POST();
    String payload = "{\"force\":false,\"revision\":0}";
    executeRunnable(runnable, url, method, payload, "could not close datatype");

    if (logger.isDebugEnabled()) {
      logger.debug(order.getId() + ": Datatype created. Temporary path: " + currentPath);
    }
  }


  private String createDatatype(String label, String workspaceName, XynaOrderServerExtension order) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = urlEncode(workspaceName);
    URLPath url = new URLPath("/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes", null, null);
    HTTPMethod method = new POST();
    String payload = "{\"label\":\"" + label + "\"}";
    GetDataTypeResponse json;
    json = (GetDataTypeResponse) executeRunnable(runnable, url, method, payload, "Could not create datatype.");
    if (json == null) {
      throw new RuntimeException("Could not create datatype.");
    }
    String fqn = json.getXmomItem().getFqn();
    return fqn.substring(0, fqn.lastIndexOf("."));
  }

  private void addServiceToDatatype(String path, String label, String service, String workspace, XynaOrderServerExtension order, Text rpc) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = urlEncode(workspace);
    String fqnUrl = path + "/" + label;
    String endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + fqnUrl + "/objects/memberMethodsArea/insert";
    URLPath url = new URLPath(endPoint, null, null);
    HTTPMethod method = new POST();
    String payload = "{\"index\":-1,\"content\":{\"type\":\"memberMethod\",\"label\":\"" +  service + "\"},\"revision\":0}";
    executeRunnable(runnable, url, method, payload, "Could not add service to datatype.");
    
    // set meta tag
    String serviceName = service; //TODO: read correct name from datatype
    endPoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + fqnUrl + "/services/" + serviceName + "/meta";
    url = new URLPath(endPoint, null, null);
    method = new PUT();
    String mappings = "<" + Constants.TAG_MAPPINGS + "/>";
    String tag = "<Yang type=\\\"Usecase\\\">\n  <Rpc>"+ rpc.getText() + "</Rpc>\n  "+ mappings +"\n</Yang>";
    payload = "{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.request.MetaTagRequest\"},\"metaTag\":{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.MetaTag\"},\"deletable\":true,\"tag\":\"" + tag + "\"}}";
    executeRunnable(runnable, url, method, payload, "Could not add meta tag to service.");
  }

  private void saveDatatype(String path, String targetPath, String label, String workspace, XynaOrderServerExtension order) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = urlEncode(workspace);
    String baseUrl = "/runtimeContext/" + workspaceNameEscaped + "/xmom/datatypes/" + path + "/" + label;
    URLPath url = new URLPath(baseUrl + "/save", null, null);
    HTTPMethod method = new POST();
    String payload = "{\"force\":false,\"revision\":2,\"path\":\"" + targetPath + "\",\"label\":\"" + label + "\"}";
    executeRunnable(runnable, url, method, payload, "Could not save datatype.");
    
    //deploy
    url = new URLPath(baseUrl + "/deploy", null, null);
    payload = "{\"revision\":3}";
    executeRunnable(runnable, url, method, payload, "Could not deploy datatype.");
  }
  
  
  private Object executeRunnable(RunnableForFilterAccess runnable, URLPath url, HTTPMethod method, String payload, String msg) {
    try {
      return runnable.execute(url, method, payload);
    } catch (XynaException e) {
      throw new RuntimeException(msg, e);
    }
  }


  private String urlEncode(String in) {
    return URLEncoder.encode(in, Charset.forName("UTF-8"));
  }
}
