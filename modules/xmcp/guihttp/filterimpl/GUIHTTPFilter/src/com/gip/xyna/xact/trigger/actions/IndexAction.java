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

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.FilterAction;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


/**

 */
public class IndexAction implements FilterAction {

  private static String INDEX_HTML = null;

  private List<FilterAction> allFilterActions;

  public IndexAction(List<FilterAction> allFilterActions) {
    this.allFilterActions = allFilterActions;
  }

  public boolean match(String uri, String method) {
    return method.equals("GET") && uri.contains("index.html");
  }

  public String getTitle() {
    return null;
  }

  public void appendForm(StringBuilder sb, String indentation) {
    //nichts zu tun
  }  
  
  public FilterResponse act(Logger logger, HTTPTriggerConnection tc) throws SocketNotAvailableException {
    tc.sendHtmlResponse(getOrCreateIndexHtml());
    return FilterResponse.responsibleWithoutXynaorder();
  }

  private String getOrCreateIndexHtml() {
    if( INDEX_HTML != null ) {
      return INDEX_HTML;
    }
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    StringBuilder sb = new StringBuilder();
    
    sb.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n");
    sb.append("<html>\n");
    sb.append("  <head>\n");
    try {
      sb.append("    <title>GUI-HTTP-Filter (in ").append(rm.getRuntimeContext(rm.getRevision(getClass()))).append(")</title>\n");
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    sb.append("  </head>\n");
    sb.append("  <body>\n");
    sb.append("    <h1>").append("GUI-HTTP-Filter").append("</h1>\n");
    for( FilterAction fa : allFilterActions ) {
      String title = fa.getTitle();
      if( title != null ) {
        sb.append("    <h3>").append(fa.getTitle()).append("</h3>\n");
        fa.appendForm( sb, "    " );
        sb.append("    </br>\n\n");
      }
    }
    sb.append("  </body>\n");
    sb.append("</html>\n");
    
    INDEX_HTML = sb.toString();
    return INDEX_HTML;
  }
  
  
}
