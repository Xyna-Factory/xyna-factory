/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.CallStatistics.StatisticsEntry;
import com.gip.xyna.xact.filter.FilterAction.FilterActionInstance;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

/**
 *
 */
public class DefaultFilterActionInstance implements FilterActionInstance {

  
  private static Logger logger = Logger.getLogger(DefaultFilterActionInstance.class);
  
  private static final long serialVersionUID = 1L;

  private static final String HTTP_NOTMODIFIED = "304 Not Modified";
  protected Properties properties;

  protected String status;

  public DefaultFilterActionInstance() {
  }

  public FilterResponse filterResponse() throws XynaException {
    return FilterResponse.responsibleWithoutXynaorder();
  }

  public void onResponse(GeneralXynaObject response, HTTPTriggerConnection tc) {
    //nichts zu tun: keine XynaOrder, keine Response
  }
  
  public void onResponsibleWithoutXynaOrder(HTTPTriggerConnection tc) {
    //nichts zu tun
  }

  public void onError(XynaException[] xynaExceptions, HTTPTriggerConnection tc) {
    try {
      String failure = createFailureText(xynaExceptions);
      sendError(tc, failure);
    } catch (SocketNotAvailableException e) {
      throw new RuntimeException(e);
    }
  }
  
  private String createFailureText(XynaException[] xynaExceptions) {
    int size = xynaExceptions.length;
    if( size == 0 ) {
      logger.warn( "Failed without exception");
      return "Failed without exception";
    } else if( size == 1 ) {
      XynaException xe = xynaExceptions[0];
      logger.warn( "Failed", xe);
      StringBuilder sb = new StringBuilder("Failed with ");
      appendException( sb, xe );
      return sb.toString();
    } else {
      logger.warn("Multiple Exceptions ("+size+")" );
      StringBuilder sb = new StringBuilder();
      sb.append("Failed with ").append(size).append(" exceptions:\n");
      for( int i=0; i<size; ++i ) {
        XynaException xe = xynaExceptions[i];
        logger.warn("Workflow failed "+i+"/"+size+" ", xe);
        sb.append(i).append(". ");
        appendException( sb, xe );
        sb.append("\n");
      }
      return sb.toString();
    }
  }

  public void sendError(HTTPTriggerConnection tc, Exception exception) {
    try {
      String failure = createFailureText(exception);
      sendError(tc, failure);
    } catch (SocketNotAvailableException e) {
      throw new RuntimeException(e);
    }
  }

  private String createFailureText(Exception exception) {
    logger.warn( "Failed", exception);
    StringBuilder sb = new StringBuilder("Failed with ");
    appendException( sb, exception );
    return sb.toString();
  }

  
  private void appendException(StringBuilder sb, Exception e) {
    if( e != null ) {
      sb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
      sb.append("\n\n");
      StringWriter sw = new StringWriter();
      e.printStackTrace( new PrintWriter( sw ) );
      sb.append(sw);
    } else {
      sb.append("null");
    }
  }

  public void sendHtml(HTTPTriggerConnection tc, String response) throws SocketNotAvailableException {
    sendHtml(tc, getBytes(response) );
  }
  
  public void sendHtml(HTTPTriggerConnection tc, byte[] htmlBytes) throws SocketNotAvailableException {
    sendResponseInternal(tc, HTTPTriggerConnection.HTTP_OK, HTTPTriggerConnection.MIME_HTML + charset(), new ByteArrayInputStream(htmlBytes));
  }
 
  public void sendStream(HTTPTriggerConnection tc, String mime, InputStream stream) throws SocketNotAvailableException {
    if (mime != null && (isTextBased(mime)) && !mime.contains("charset=")) {
      mime += charset();
    }
    sendResponseInternal( tc, HTTPTriggerConnection.HTTP_OK, mime, stream);
  }
  
  private boolean isTextBased(String mime) {
    return mime.contains("text") || mime.contains("json");
  }

  public void sendError(HTTPTriggerConnection tc, String response) throws SocketNotAvailableException {
    sendResponseInternal(tc, HTTPTriggerConnection.HTTP_INTERNALERROR, HTTPTriggerConnection.MIME_PLAINTEXT + charset(), 
        new ByteArrayInputStream(getBytes(response)));
  }
  
  public void sendError(HTTPTriggerConnection tc, String statusCode,  String response) throws SocketNotAvailableException {
    sendResponseInternal(tc, statusCode, HTTPTriggerConnection.MIME_PLAINTEXT + charset(), 
        new ByteArrayInputStream(getBytes(response)));
  }

  protected static String charset() {
    return "; charset=" + CONTENT_ENCODING;
  }

  public void setProperty(String key, Object value) {
    if( properties == null ) {
      properties = new Properties();
    }
    properties.put(key, value);
  }

  public void sendNotModified(HTTPTriggerConnection tc, String mime) throws SocketNotAvailableException {
    sendResponseInternal(tc, HTTP_NOTMODIFIED, mime, null);
  }

  public static final String CONTENT_ENCODING = "UTF-8";

  public static byte[] getBytes(String html) {
    try {
      return html.getBytes(CONTENT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("charset UTF-8 is unsupported.");
    }
  }

  public void sendProperties(HTTPTriggerConnection tc) throws SocketNotAvailableException {
    sendResponseInternal(tc, HTTPTriggerConnection.HTTP_OK, null, null );
  }
  
  public void sendJson(HTTPTriggerConnection tc, String json) throws SocketNotAvailableException {
    ByteArrayInputStream bais = new ByteArrayInputStream(getBytes(json));
    sendResponseInternal(tc, HTTPTriggerConnection.HTTP_OK, "application/json" + charset(), bais);
  }

  protected void sendResponseInternal(HTTPTriggerConnection tc, String status, String mime, InputStream inputStream) throws SocketNotAvailableException {
    this.status = status;
    tc.sendResponse(status, mime, properties, inputStream);
  }
  
  @Override
  public void fillStatistics(StatisticsEntry statisticsEntry) {
    statisticsEntry.setStatus(status);
  }

  

}
