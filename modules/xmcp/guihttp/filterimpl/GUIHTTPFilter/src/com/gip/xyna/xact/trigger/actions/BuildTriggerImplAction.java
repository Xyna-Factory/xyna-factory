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
package com.gip.xyna.xact.trigger.actions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterAction;
import com.gip.xyna.xact.trigger.GUIHTTPFilter;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;


/**
 *
 */
public class BuildTriggerImplAction implements FilterAction {
  
  public boolean match(String uri, String method) {
    return uri.startsWith(GUIHTTPFilter.BUILD_TRIGGER_IMPL_TEMPLATE_REQUEST);
  }
  
  public String getTitle() {
    return "Build TriggerImpl";
  }

  public void appendForm(StringBuilder sb, String indentation) {
    sb.append(indentation).append("<form action=\""+ GUIHTTPFilter.BUILD_TRIGGER_IMPL_TEMPLATE_REQUEST + "\" method=\"get\">\n");
    sb.append(indentation).append("  TriggerName </br>\n");
    sb.append(indentation).append("  <input type=\"text\" size=\"30\" maxlength=\"100\" name=\"p\"> </br>\n");
    sb.append(indentation).append("  </br>\n");
    sb.append(indentation).append("  <input type=\"submit\" value=\"get Trigger\" >\n");
    sb.append(indentation).append("</form>\n");
  }  

  public FilterResponse act(Logger logger, HTTPTriggerConnection tc) throws XynaException {
    logger.info("got trigger implementation template request");
    String triggerName = tc.getParas().getProperty("p");

    InputStream is = XynaFactory.getInstance().getXynaMultiChannelPortal().getTriggerImplTemplate(triggerName, true);
    try {

      logger.debug("sending built trigger implementation template");
      tc.sendResponse(HTTPTriggerConnection.HTTP_OK, HTTPTriggerConnection.MIME_DEFAULT_BINARY, new Properties(), is);

    } finally {
      try {
        is.close();
      } catch (IOException e) {
        throw new Ex_FileAccessException("unknown", e);
      }
    }

    return FilterResponse.responsibleWithoutXynaorder();
  }

}
