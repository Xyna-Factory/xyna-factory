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
package com.gip.xyna.xact.filter;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.actions.H5xFilterAction;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.XmomGuiSession;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;

/**
 * Basis für alle FilterActions, die wahlweise mit und ohne rtc aufgerufen werden können
 *
 */
public abstract class RuntimeContextDependendAction extends H5xFilterAction {

  public static final String BASE_PATH = "/" + PathElements.RTCS;
  public static final String EXAMPLE_PATH = BASE_PATH + "/W-default%20workspace";
  
  @Override
  public boolean match(URLPath url, Method method) {
    String path = url.getPath();
    if( path.startsWith(BASE_PATH) ) {
      return matchRuntimeContextIndependent( url.subURL(2), method);
    }
    return matchRuntimeContextIndependent( url, method);
  }
  
  @Override
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    RuntimeContext rc = null;
    Long revision = null;
    URLPath subUrl = null;
    
    String path = url.getPath();
    if( path.startsWith(BASE_PATH) ) {
      String rcName = url.decodeSubPath(1,2).substring(1); //application hat / escaped (%2F), damit das valueOf funktioniert
      rc = RuntimeContext.valueOf(rcName);
      
      if ( (rc instanceof Application) &&
           (((Application)rc).getVersionName().equals("")) ) {
        // no version has been specified -> use highest running version
        ApplicationManagementImpl am = (ApplicationManagementImpl)XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
        String highestRunningVersion = am.getHighestVersion(rc.getName(), true);
        rc = new Application(rc.getName(), highestRunningVersion);
      }
      
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      revision = rm.getRevision(rc);
      subUrl = url.subURL(2);
    } else {
      revision = H5XdevFilter.DEFAULT_WORKSPACE.get();
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      rc = rm.getRuntimeContext(revision);
      subUrl = url;
    }
    return act( rc, revision, subUrl, tc.getMethodEnum(), tc);
  }
  

  protected abstract boolean matchRuntimeContextIndependent( URLPath url, Method method);
  
  protected abstract FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException;
  
  protected XmomGuiSession getSession(HTTPTriggerConnection tc) throws XynaException {
    String id = AuthUtils.readCredentialsFromCookies(tc).getSessionId();
    return new XmomGuiSession(id);
  }
  


}
