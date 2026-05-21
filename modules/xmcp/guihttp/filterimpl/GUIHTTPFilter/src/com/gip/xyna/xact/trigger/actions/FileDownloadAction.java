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
package com.gip.xyna.xact.trigger.actions;

import java.io.InputStream;
import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterAction;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;


/**
 *
 */
public class FileDownloadAction implements FilterAction {

  
  public boolean match(String uri, String method) {
    return uri.startsWith("/download");
  }
  
  public String getTitle() {
    return "FileDownload";
  }

  public void appendForm(StringBuilder sb, String indentation) {
    sb.append(indentation).append("<form action=\"download\" method=\"get\" enctype=\"multipart/form-data\">\n");
    sb.append(indentation).append("  <p>File ID:<br>\n");
    sb.append(indentation).append("  <input type=\"text\" size=\"30\" maxlength=\"100\" name=\"p0\"> </br>\n");
    sb.append(indentation).append("  </p>\n");
    sb.append(indentation).append("  </br>\n");
    sb.append(indentation).append("  <input type=\"submit\" value=\"Download\" >\n");
    sb.append(indentation).append("</form>\n");
  }  


  public FilterResponse act(Logger logger, HTTPTriggerConnection tc) throws XynaException {
    FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    
    try {
      String fileId = tc.getFirstValueOfParameter("p0");
      TransientFile file = fm.retrieve( fileId );
      InputStream is = file.openInputStream();
      try {
        tc.sendBinaryResponse(is, file.getOriginalFilename(), file.getSize());
      } finally {
        is.close();
      }
    } catch (Exception e) {
      logger.warn( "FileDownload failed", e);
      tc.sendResponse(e.getMessage());
    }
    return FilterResponse.responsibleWithoutXynaorder();
  }
  

}
