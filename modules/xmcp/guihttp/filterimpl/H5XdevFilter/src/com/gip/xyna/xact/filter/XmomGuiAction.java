/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter;

import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.URLPath.URLPathQuery;
import com.gip.xyna.xact.filter.actions.startorder.Endpoint;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XMOMGuiReply;
import com.gip.xyna.xact.filter.session.XMOMGuiRequest;
import com.gip.xyna.xact.filter.session.XmomGuiSession;
import com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;

public abstract class XmomGuiAction extends RuntimeContextDependendAction implements Endpoint {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(XmomGuiAction.class);
  
  protected final XMOMGui xmomGui;
  
  private final Operation operation;
  private final boolean hasJson;
  private final Type type;
  
  private int indexPath;
  private int indexName;
  private int indexObjectId;
  

  public XmomGuiAction(XMOMGui xmomGui, Operation operation, Type type, boolean hasJson) {
    this.xmomGui = xmomGui;
    this.operation = operation;
    this.type = type;
    this.hasJson = hasJson;
  }

  public XmomGuiAction(XMOMGui xmomGui, int indexObjectId, Operation operation, Type type, boolean hasJson) {
    this.xmomGui = xmomGui;
    this.indexObjectId = indexObjectId;
    this.operation = operation;
    this.type = type;
    this.hasJson = hasJson;
  }

  public XmomGuiAction(XMOMGui xmomGui, int indexPath, int indexName, Operation operation, Type type, boolean hasJson) {
    this.xmomGui = xmomGui;
    this.indexPath = indexPath;
    this.indexName = indexName;
    this.operation = operation;
    this.type = type;
    this.hasJson = hasJson;
  }
  
  public XmomGuiAction(XMOMGui xmomGui, int indexPath, int indexName, int indexObjectId, Operation operation, Type type, boolean hasJson) {
    this.xmomGui = xmomGui;
    this.indexPath = indexPath;
    this.indexName = indexName;
    this.indexObjectId = indexObjectId;
    this.operation = operation;
    this.type = type;
    this.hasJson = hasJson;
  }


  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method,
      HTTPTriggerConnection tc) throws XynaException {
    XMOMGuiRequest request = createRequest(method, tc.getPayload(), rc, revision, url, indexPath, indexName, indexObjectId, operation, type, hasJson);
    return act(tc, request);
  }
  
  private XMOMGuiRequest createRequest(Method method, String payload, RuntimeContext rc, Long revision, URLPath url, 
      int indexPath, int indexName, int indexObjectId, Operation operation, Type type, boolean hasJson ) {
    XMOMGuiRequest request = new XMOMGuiRequest(rc, revision, operation, type);
    if( indexPath != 0 ) {
      request.setTypePath( url.getPathElement(indexPath) );
    } 
    if( indexName != 0 ) {
      request.setTypeName( url.getPathElement(indexName) );
    }
    if( indexObjectId != 0 ) {
      request.setObjectId( url.getPathElement(indexObjectId) );
    }
   
    if( hasJson && method != Method.GET ) {
      request.setJson( payload );
    }
    for(URLPathQuery e : url.getQueryList()) {
      request.addParameter(e.getAttribute(), e.getValue());
    }
    return request;
  }
  
  protected boolean isReadAction(Method method) {
    return method == Method.GET;
  }
  
  protected boolean isEditAction(Method method) {
    return method != Method.GET;
  }
  
  protected FilterActionInstance act(HTTPTriggerConnection tc, XMOMGuiRequest request) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    if(!checkLoginAndRights(tc, jfai)) {
      return jfai;
    }

    try {
      XMOMGuiReply reply = xmomGui.processRequest(getSession(tc), request);
      jfai.sendJson(tc, reply.getHttpStatus(), reply.getJson());
    } catch (Exception e) {
      jfai.sendError(tc, e);
    }
    return jfai;
  }
  
  private boolean checkLoginAndRights(HTTPTriggerConnection tc, JsonFilterActionInstance jfai) throws XynaException {
    List<String> rights = collectRights(tc.getMethodEnum());
    return checkLoginAndRights(tc, jfai, rights.toArray(new String[0]));
  }

  private boolean checkLoginAndRights(Method method, XynaPlainSessionCredentials creds) {
    List<String> rights = collectRights(method);
    return checkLoginAndRights(creds, rights.toArray(new String[0]));
  }
  
  private List<String> collectRights(Method method) {
    List<String> rights = new LinkedList<String>();
    rights.add(GuiRight.PROCESS_MODELLER.getKey());
    if (isReadAction(method)) {
      rights.add(Rights.READ_MDM.toString());
    } else if (isEditAction(method)) {
      rights.add(Rights.EDIT_MDM.toString());
    }
    return rights;
  }
  

  @Override
  public GeneralXynaObject execute(XynaPlainSessionCredentials creds, URLPath url, Method method, String payload) {
    XMOMGuiRequest request = null;
    try {
      RTCInfo info = extractRTCInfo(url);
      request = createRequest(method, payload, info.rc, info.revision, info.subUrl, indexPath, indexName, indexObjectId, operation, type, hasJson);
    } catch (Exception e) {
      return null;
    }
    if (!checkLoginAndRights(method, creds)) {
      return null;
    }


    try {
      return xmomGui.processRequest(new XmomGuiSession(creds.getSessionId(), creds.getToken()), request).getXynaObject();
    } catch (Exception e) {
      if(logger.isWarnEnabled()) {
        logger.warn("Exception executing "+ url.getPath() +": " + e);
      }
      return null;
    }
  }

  protected boolean methodIn(Method method, Method ... in) {
    for( Method m : in ) {
      if( method == m ) {
        return true;
      }
    }
    return false;
  }

}
