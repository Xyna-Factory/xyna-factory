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

package com.gip.xyna.xact.filter.actions.monitor;

import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.actions.H5xFilterAction;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.startorder.Endpoint;
import com.gip.xyna.xact.filter.monitor.GetAuditRequestProcessor;
import com.gip.xyna.xact.filter.session.XMOMGuiReply;
import com.gip.xyna.xact.filter.util.ReadonlyUtil;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;

import xmcp.processmonitor.datatypes.response.GetAuditResponse;

public class OpenAuditAction extends H5xFilterAction implements Endpoint {

  private static final String BASE_PATH = "/" + PathElements.AUDITS;
  private static final Logger logger = CentralFactoryLogging.getLogger(OpenAuditAction.class);


  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH+"/h5.data/Simple/open", "h5.data.Simple");
  }

  @Override
  public String getTitle() {
    return "Audits-OrderId";
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }


  @Override
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    
    if(!checkLoginAndRights(tc, jfai, Rights.ORDERARCHIVE_DETAILS.name())) {
      return jfai;
    }
    
    XMOMGuiReply reply = new XMOMGuiReply();
    GetAuditRequestProcessor getAuditRequestProcessor = new GetAuditRequestProcessor();
    GetAuditResponse gar = getAuditRequestProcessor.processGetAuditRequest(Long.valueOf(url.getPathElement(1)));
    reply.setXynaObject(gar);
    ReadonlyUtil.setReadonlyRecursive(reply);
    jfai.sendJson(tc, HTTPTriggerConnection.HTTP_OK, reply.getJson());
    return jfai;
  }

  @Override
  public boolean match(URLPath url, Method arg1) {
    return url.getPath().startsWith(BASE_PATH) && url.getPathLength() == 2;
  }


  @Override
  public GeneralXynaObject execute(XynaPlainSessionCredentials creds, URLPath url, Method method, String payload) {
    try {
      return new GetAuditRequestProcessor().processGetAuditRequest(Long.valueOf(url.getPathElement(1)));
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not process open audit request.", e);
      }
      return null;
    }
  }
  
}
