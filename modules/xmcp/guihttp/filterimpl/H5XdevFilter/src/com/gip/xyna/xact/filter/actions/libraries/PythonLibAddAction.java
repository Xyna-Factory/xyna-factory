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
package com.gip.xyna.xact.filter.actions.libraries;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.SessionBasedData;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XmomGuiSession;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.RuntimeContextDependendAction;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;

public class PythonLibAddAction extends RuntimeContextDependendAction {

 private XMOMGui xmomGui;
  
  public PythonLibAddAction(XMOMGui xmomGui) {
    this.xmomGui = xmomGui;
  }
  
  @Override
  protected boolean matchRuntimeContextIndependent( URLPath url, Method method) {
    return url.getPathLength() == 5
        && url.getPathElement(0).equals(PathElements.XMOM)
        && (url.getPathElement(1).equals(PathElements.DATA_TYPES)
         || url.getPathElement(1).equals(PathElements.SERVICES_GROUPS))
        && url.getPathElement(4).equals(PathElements.PYTHONLIB)
        && method == Method.PUT;
  }
  
  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance actionInstance = new JsonFilterActionInstance();

    if(!checkLoginAndRights(tc, actionInstance, GuiRight.PROCESS_MODELLER.getKey(), Rights.EDIT_MDM.toString())) {
      return actionInstance;
    }

    GenerationBaseObject gbo = xmomGui.getGbo(getSession(tc), rc, revision, url);
    
    int index;
    String fileId;
    TransientFile file;
    try {
      index = Integer.valueOf(tc.getFirstValueOfParameterOrDefault("index", "-1"));
      fileId = tc.getFirstValueOfParameter("fileId");
      if (fileId == null || fileId.trim().isEmpty()) {
        throw new RuntimeException("Missing FileId");
      }
      file = uploadFile(fileId);
    } catch (Exception e) {
      AuthUtils.replyError(tc, actionInstance, e);
      return actionInstance;
    }

    gbo.addSgLibToUpload(fileId);
    gbo.getDOM().addPythonLibrary(index, file.getOriginalFilename());
    actionInstance.sendJson(tc, gbo.buildXMOMGuiReply().getJson());
    return actionInstance;
  }
  
  private TransientFile uploadFile(String fileId) {
    FileManagement fileManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    return fileManagement.retrieve(fileId);
  }

  @Override
  public String getTitle() {
    // Wird nie aufgerufen
    return null;
  }


  @Override
  public void appendIndexPage(HTMLPart body) {
    // Wird nie aufgerufen
  }


  @Override
  public boolean hasIndexPageChanged() {
    // Wird nie aufgerufen
    return false;
  }
}