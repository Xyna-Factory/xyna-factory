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
package com.gip.xyna.xact.filter.actions;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.RuntimeContextDependendAction;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.util.XmomObjectsPath;
import com.gip.xyna.xact.filter.util.XmomObjectsPath.SortType;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;


/**
 *
 */
public class WorkflowsPathAction extends RuntimeContextDependendAction {

  private static final String BASE_PATH = "/" + PathElements.XMOM + "/" + PathElements.WORKFLOWS;
  private final XMOMGui xmomGui;
  
  public WorkflowsPathAction(XMOMGui xmomGui) {
    this.xmomGui = xmomGui;
  }
  
  @Override
  protected boolean matchRuntimeContextIndependent( URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH) 
        && url.getPathLength() == 3
        && method == Method.GET;
  }

  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    if (!checkLoginAndRights(tc, jfai, Rights.READ_MDM.toString())) {
      return jfai;
    }

    return listPath(rc, revision, url, tc);
  }

  @Override
  public String getTitle() {
    return "XMOM-Workflows-TypePath";
  }

  @Override
  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH+"/xact.http", "xact.http");
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }
  
  private FilterActionInstance listPath(RuntimeContext rc, Long revision, URLPath url, HTTPTriggerConnection tc) throws SocketNotAvailableException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    try {
      String path = url.getPathElement(2);
      SortType sortType = SortType.typeAware;
      if(tc.getParas() != null && tc.getParas().contains("sort")) {
        sortType = SortType.valueOf(tc.getParas().getProperty("sort"));
      }
      
      XmomObjectsPath xmomObjectsPath = new XmomObjectsPath(xmomGui.getXmomLoader(), "workflows");
      xmomObjectsPath.filterTypes(ObjectIdentifierJson.Type.workflow);
      xmomObjectsPath.search(revision, rc, path, sortType);
      
      jfai.sendJson(tc, xmomObjectsPath.toJson());
    } catch( Exception e ) {
      jfai.sendError(tc, e);
    }
    return jfai;
  }

}

