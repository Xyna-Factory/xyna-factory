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
package com.gip.xyna.xact.filter.actions.listpaths;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.XmomGuiAction;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation;
import com.gip.xyna.xact.filter.util.XmomObjectsPath;
import com.gip.xyna.xact.filter.util.XmomObjectsPath.SortType;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;

import xact.templates.Document;
import xact.templates.JSON;

public abstract class ListPathsAction extends XmomGuiAction {

  protected String path;
  protected String base_path;
  protected static final String[] rights = {GuiRight.PROCESS_MODELLER.getKey(), Rights.READ_MDM.toString()};
  
  public ListPathsAction(XMOMGui xmomGui, Type type, String path, String pathElement) {
    super(xmomGui, 0, 0, Operation.Create, type, true);
    this.path = path;
    base_path =  "/" + PathElements.XMOM + "/" + pathElement;
  }
  
  @Override
  protected boolean matchRuntimeContextIndependent( URLPath url, Method method) {
    return url.getPath().startsWith(base_path) 
        && url.getPathLength() == 2
        && methodIn(method, Method.GET, Method.POST);
  }
  
  private XmomObjectsPath listPath(RuntimeContext rc, Long revision, URLPath url) throws Exception {
    SortType sortType = SortType.typeAware;
    if (url.getQueryList() != null && url.getQuery("sort") != null) {
      sortType = SortType.valueOf(url.getQuery("sort").getValue());
    }

    XmomObjectsPath xmomObjectsPath = new XmomObjectsPath(xmomGui.getXmomLoader(), path);
    xmomObjectsPath.filterTypes(getFilterTypes());
    xmomObjectsPath.search(revision, rc, "*", sortType);
    return xmomObjectsPath;
  }
  
  protected abstract Type[] getFilterTypes();
  
  private FilterActionInstance listPath(RuntimeContext rc, Long revision, URLPath url, HTTPTriggerConnection tc) throws SocketNotAvailableException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    try {
      XmomObjectsPath xmomObjectsPath = listPath(rc, revision, url);
      jfai.sendJson(tc, xmomObjectsPath.toJson());
    } catch( Exception e ) {
      jfai.sendError(tc, e);
    }
    return jfai;
  }
  

  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    if (method == Method.GET) {
      if (!checkLoginAndRights(tc, jfai, rights)) {
        return jfai;
      }

      return listPath(rc, revision, url, tc);
    } else {
      return super.act(rc, revision, url, method, tc);
    }
  }
  
  @Override
  public GeneralXynaObject execute(XynaPlainSessionCredentials creds, URLPath url, Method method, String payload) {
    if (method == Method.GET) {
      if (!checkLoginAndRights(creds, rights)) {
        return null;
      }
      
      try {
        RTCInfo info = extractRTCInfo(url);
        return new Document.Builder().text(listPath(info.rc, info.revision, url).toJson()).documentType(new JSON()).instance();
      } catch (Exception e) {
        return new xmcp.processmodeller.datatypes.Error.Builder().exceptionMessage(e.getMessage()).instance();
      }
    } else {
      return super.execute(creds, url, method, payload);
    }
  }
  

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }

  @Override
  protected boolean isEditAction(Method method) {
    // users without the right EDIT_MDM should still be able to create a new document (without being able to save it)
    return false;
  }
}
