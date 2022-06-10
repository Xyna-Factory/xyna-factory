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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;

public class TriggerConnectionUtils {
  
  private Properties properties;

  
  public void sendHtml(HTTPTriggerConnection tc, String response) throws SocketNotAvailableException {
    sendHtml(tc, getBytes(response) );
  }
  
  
  public void sendHtml(HTTPTriggerConnection tc, byte[] htmlBytes) throws SocketNotAvailableException {
    tc.sendResponse(HTTPTriggerConnection.HTTP_OK, HTTPTriggerConnection.MIME_HTML, properties, new ByteArrayInputStream(htmlBytes));
  }
 
  public void sendStream(HTTPTriggerConnection tc, String mime, InputStream stream) throws SocketNotAvailableException {
    tc.sendResponse(HTTPTriggerConnection.HTTP_OK, mime, properties, stream);
  }
  
  public void sendError(HTTPTriggerConnection tc, String response) throws SocketNotAvailableException {
    tc.sendResponse(HTTPTriggerConnection.HTTP_INTERNALERROR, HTTPTriggerConnection.MIME_PLAINTEXT, 
        properties, new ByteArrayInputStream(getBytes(response)));
  }

  public static byte[] getBytes(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("charset UTF-8 is unsupported.");
    }
  }

  public void sendJson(HTTPTriggerConnection tc, String payload) throws SocketNotAvailableException {
    ByteArrayInputStream bais = new ByteArrayInputStream(getBytes(payload));
    tc.sendResponse(HTTPTriggerConnection.HTTP_OK, "application/json", properties, bais);
  }
  
}
