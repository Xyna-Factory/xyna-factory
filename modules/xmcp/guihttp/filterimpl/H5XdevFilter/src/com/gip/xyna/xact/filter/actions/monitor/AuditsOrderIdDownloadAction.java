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

package com.gip.xyna.xact.filter.actions.monitor;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.H5xFilterAction;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.monitor.ExportAuditProcessor;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;


public class AuditsOrderIdDownloadAction extends H5xFilterAction{

  private static final String BASE_PATH = "/" + PathElements.AUDITS;
  

  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH+"/h5.data/Simple/open", "h5.data.Simple");
  }

  @Override
  public String getTitle() {
    return "Audits-OrderId-Download";
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
    
    try {
      Long orderId = Long.valueOf(url.getPathElement(1));
      
      ExportAuditProcessor processor = new ExportAuditProcessor(orderId);
      String xml = processor.createExportXML();
      ExportAuditProcessor.MetaData meta = processor.getMetaData();
      
      
      Properties header = new Properties();
      header.put("Content-Disposition", "attachment; filename=\"" + meta.getXmlFileName() + "\"");
      
      byte[] bytes = xml.getBytes();
      
      tc.sendResponse(HTTPTriggerConnection.HTTP_OK, "application/xml", header, new ByteArrayInputStream(bytes), (long) bytes.length);
      
    } catch (NumberFormatException ex) {
      AuthUtils.replyError(tc, jfai, ex);
    }
    return jfai;
  }

  @Override
  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH) 
        && url.getPathLength() == 3
        && url.getPathElement(2).equalsIgnoreCase(PathElements.DOWNLOAD)
        && method == Method.GET;
  }
}
