/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
import com.gip.xyna.xact.filter.URLPath.URLPathQuery;
import com.gip.xyna.xact.filter.xmom.paths.XmomPaths;
import com.gip.xyna.xact.filter.xmom.paths.XmomPaths.Mode;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;


/**
 *
 */
public class PathsAction extends RuntimeContextDependendAction {

  private static final String BASE_PATH = "/" + PathElements.XMOM + "/" + PathElements.PATHS;
  
  @Override
  protected boolean matchRuntimeContextIndependent( URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH) 
        && method == Method.GET;
  }

  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    String[] rights = {GuiRight.PROCESS_MODELLER.getKey(), Rights.READ_MDM.toString()};

    if (!checkLoginAndRights(tc, jfai, rights)) {
      return jfai;
    }

    return openPath(rc, revision, url, tc);

  }

  @Override
  public String getTitle() {
    return "XMOM-Path";
  }

  @Override
  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH, PathElements.PATHS);
    paragraphDef.lineBreak();
    paragraphDef.link(RuntimeContextDependendAction.EXAMPLE_PATH+BASE_PATH, PathElements.PATHS);
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }
  
  private FilterActionInstance openPath(RuntimeContext rc, Long revision, URLPath url, HTTPTriggerConnection tc) throws SocketNotAvailableException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    try {

      URLPathQuery filter = url.getQuery("filter");
      URLPathQuery hierarchy = url.getQuery("hierarchy");
      
      
      XmomPaths xmomPaths = new XmomPaths(revision);
      
      //FIXME filter
      //xmomPaths.filter();
      
      Mode mode = Mode.full;
      if( hierarchy != null ) {
        mode = Mode.valueOf(hierarchy.getValue());
      }
      xmomPaths.transformToPathItems(mode);
      
      jfai.sendJson(tc, xmomPaths.toJson());
    } catch( Exception e ) {
      jfai.sendError(tc, e);
    }
    return jfai;
  }

}

