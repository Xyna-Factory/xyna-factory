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
package com.gip.xyna.xact.trigger.actions;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.FilterAction;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;


/**
 *
 */
public class SendCrossDomainXmlAction implements FilterAction {

  public boolean match(String uri, String method) {
    return method.equals("GET") && uri.contains("crossdomain.xml");
  }
  
  public String getTitle() {
    return "Get CrossDomain.xml";
  }

  public void appendForm(StringBuilder sb, String indentation) {
    sb.append(indentation).append("<form action=\"crossdomain.xml\" method=\"get\">\n");
    sb.append(indentation).append("  <input type=\"submit\" name=\"Name\" value=\"get crossdomain.xml\">\n");
    sb.append(indentation).append("</form>\n");
  }  

  public FilterResponse act(Logger logger, HTTPTriggerConnection tc) throws SocketNotAvailableException {
    logger.debug("returning crossdomain.xml");

    String crossDomainXML = XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal().getCrossDomainXML();

    if (crossDomainXML == null || crossDomainXML.trim().length() == 0) {
      String ip = tc.getTriggerIp();
      String ownHostname = tc.getTriggerHostname();
      crossDomainXML = XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal()
                      .getMinimalCrossDomainXML(ip, ownHostname, "*");
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("sending Crossdomain XML: " + crossDomainXML);
    }
    
    tc.sendResponse(crossDomainXML);

    return FilterResponse.responsibleWithoutXynaorder();
  }
  
}
