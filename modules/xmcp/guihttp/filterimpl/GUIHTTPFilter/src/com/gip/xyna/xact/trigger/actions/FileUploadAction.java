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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.util.Streams;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterAction;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;


/**
 *
 */
public class FileUploadAction implements FilterAction {

  
  private static Logger logger = CentralFactoryLogging.getLogger(FileUploadAction.class);
  
  public boolean match(String uri, String method) {
    return uri.startsWith("/upload");
  }
  
  public String getTitle() {
    return "FileUpload";
  }

  public void appendForm(StringBuilder sb, String indentation) {
    sb.append(indentation).append("<form action=\"upload\" method=\"post\" enctype=\"multipart/form-data\">\n");
    sb.append(indentation).append("  <input type=\"text \" size=\"30\" maxlength=\"100\" name=\"hint\" value=\"File:none:upload\"> </br>\n");
    sb.append(indentation).append("  <p>File to upload:<br>\n");
    sb.append(indentation).append("  <input name=\"File\" type=\"file\" size=\"50\" maxlength=\"100000\" accept=\"text/*\">\n");
    sb.append(indentation).append("  </p>\n");
    sb.append(indentation).append("  </br>\n");
    sb.append(indentation).append("  <input type=\"submit\" value=\"Upload\" >\n");
    sb.append(indentation).append("</form>\n");
  }  


  public FilterResponse act(Logger logger, HTTPTriggerConnection tc) throws XynaException {
    
    
    FileUpload upload = new FileUpload();
    
    StringBuilder response = new StringBuilder();
     
    StreamRequestContext request = new StreamRequestContext(tc.getInputStream(), tc.getHeader(), tc.getCharSet());
    
    FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    
    try {
      FileItemIterator iter = upload.getItemIterator(request);
      String locationHint = null;
      while (iter.hasNext()) {
        FileItemStream item = iter.next();
        String name = item.getFieldName();
        InputStream stream = item.openStream();
        
        if (item.isFormField()) {
          String value = Streams.asString(stream);
          logger.info("Form field \""+name+"\" with value \""+value+"\" detected.");
          if( name.equals("hint") ) {
            locationHint = createLocationHint(value);
          }
        } else {
          if( name.startsWith("File:") ) {
            locationHint = createLocationHint(name);
          }
          logger.info("File field \""+name+"\" with file name \""+item.getName()+"\" detected.");
          
          
          String fileId = fm.store( locationHint, item.getName(), stream );
          response.append("File ").append(item.getName()).append(" stored with id ").append(fileId).append("\n");
        }
      }
            
      tc.sendResponse(response.toString());
      
    } catch( Exception e) { //IOException, FileUploadIOException
      logger.warn( "FileUpload failed", e);
      tc.sendResponse(e.getMessage());
    }
    return FilterResponse.responsibleWithoutXynaorder();
  }
  
  private String createLocationHint(String value) {
    if( value == null ) {
      return null;
    }
    if( value.startsWith("File:") ) {
      value = value.substring(5);
    }
    String loc = value.replaceAll(":", File.separator );
    loc = loc.replaceAll("\\W", "_");
    return loc;
  }
  
  private static class StreamRequestContext implements RequestContext {

    private String contentType;
    private String contentLength;
    private InputStream in;
    private String encoding;
    
    public StreamRequestContext(InputStream in, Properties properties, String encoding) {
      this.contentType = properties.getProperty("content-type");
      this.contentLength = properties.getProperty("content-length");
      this.in = in;
      this.encoding = encoding;
      logger.info("StreamRequestContext("+contentLength+", "+contentType+")");
    }

    public String getCharacterEncoding() {
      return encoding;
    }

    public String getContentType() {
      return contentType;
    }

    public int getContentLength() {
      return Integer.parseInt(contentLength);
    }

    public InputStream getInputStream() throws IOException {
      return in;
    }
    
  }

}
