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
package com.gip.xyna.xact.filter.actions;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.XmomGuiAction;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation;
import com.gip.xyna.xact.filter.util.XmomObjectsPath;
import com.gip.xyna.xact.filter.util.XmomObjectsPath.SortType;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;


/**
 *
 */
public class ServiceGroupsAction extends XmomGuiAction {

  private static final String BASE_PATH = "/" + PathElements.XMOM + "/" + PathElements.SERVICES_GROUPS;
  
  public ServiceGroupsAction(XMOMGui xmomGui) {
    super(xmomGui, 0, 0, Operation.Create, Type.serviceGroup, true);
  }

  @Override
  protected boolean matchRuntimeContextIndependent( URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH) 
        && url.getPathLength() == 2
        && methodIn(method, Method.GET, Method.POST);
  }

  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    if (method == Method.GET) {
      if (isLoggedIn(tc) && hasModellerRight(tc)) {
        if (!hasRight(tc, Rights.READ_MDM.toString())) {
          Role role = getRole(tc);
          AuthUtils.replyError(tc, jfai, Status.forbidden,
                               new XFMG_ACCESS_VIOLATION(Rights.READ_MDM.toString(), role == null ? "" : role.getName()));
          return jfai;
        }

        return listPath( rc, revision, url, tc );
      } else {
        AuthUtils.replyModellerLoginRequiredError(tc, jfai);
        return jfai;
      }
    } else {
      return super.act(rc, revision, url, method, tc);
    }
  }

  @Override
  public String getTitle() {
    return "XMOM-Datatypes";
  }

  @Override
  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH, "all datatypes");
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }
  
  private FilterActionInstance listPath(RuntimeContext rc, Long revision, URLPath url, HTTPTriggerConnection tc) throws SocketNotAvailableException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    try {
      SortType sortType = SortType.valueOf(tc.getFirstValueOfParameterOrDefault("sort", SortType.typeAware.name()));

      XmomObjectsPath xmomObjectsPath = new XmomObjectsPath(xmomGui.getXmomLoader(), "serviceGroups");
      xmomObjectsPath.filterTypes(ObjectIdentifierJson.Type.serviceGroup);
      xmomObjectsPath.search(revision, rc, "*", sortType);
      
      jfai.sendJson(tc, xmomObjectsPath.toJson());
    } catch( Exception e ) {
      jfai.sendError(tc, e);
    }
    return jfai;
  }

  @Override
  protected boolean isEditAction(Method method) {
    // users without the right EDIT_MDM should still be able to create a new document (without being able to save it)
    return false;
  }

}

