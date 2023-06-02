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
package com.gip.xyna.xact.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.actions.DefaultFilterActionInstance;
import com.gip.xyna.xact.filter.actions.OptionsAction;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;



public class JsonFilterActionInstance extends DefaultFilterActionInstance {

  private static final long serialVersionUID = 1L;

  @Override
  public void onError(XynaException[] xynaExceptions, HTTPTriggerConnection tc) {
    try {
      sendError(tc, Utils.xoToJson(Utils.error(xynaExceptions)));
    } catch (SocketNotAvailableException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void sendError(HTTPTriggerConnection tc, Exception exception) {
    try {
      sendError(tc, Utils.xoToJson(Utils.error(exception)));
    } catch (SocketNotAvailableException e) {
      throw new RuntimeException(e);
    }
  }

  public void sendJson(HTTPTriggerConnection tc, String status, String json) throws SocketNotAvailableException {
    ByteArrayInputStream bais = new ByteArrayInputStream(getBytes(json));
    sendResponseInternal(tc, status, "application/json", bais);
  }
  
  public void sendOk(HTTPTriggerConnection tc) throws SocketNotAvailableException {
    sendResponseInternal(tc, HTTPTriggerConnection.HTTP_OK, null, null);
  }

  @Override
  public void sendJson(HTTPTriggerConnection tc, String json) throws SocketNotAvailableException {
    sendJson(tc, HTTPTriggerConnection.HTTP_OK, json);
  }
  
  @Override
  protected void sendResponseInternal(HTTPTriggerConnection tc, String status, String mime, InputStream inputStream)
      throws SocketNotAvailableException {
    this.status = status;
    new OptionsAction(H5XdevFilter.ACCESS_CONTROL_ALLOW_ORIGIN).setAccessControlParameter(tc, this);
    if(shouldZip(tc)) {
      try {
        inputStream = zip(tc, status, mime, inputStream);
      } catch (IOException e) {
        tc.sendResponse("500", mime, properties, new ByteArrayInputStream(getBytes(e.getMessage())));
      }
    }
    tc.sendResponse(status, mime, properties, inputStream);
  }
  

  private boolean shouldZip(HTTPTriggerConnection tc) {
    String acceptedEncodings = (String) tc.getHeader().getOrDefault("accept-encoding", "");
    if (!acceptedEncodings.contains("gzip")) {
      return false; //caller does not support gzip
    }

    return H5XdevFilter.CompressResponse.get();
  }


  private InputStream zip(HTTPTriggerConnection tc, String status, String mime, InputStream inputStream) throws IOException {
    ByteArrayOutputStream boutSteam = new ByteArrayOutputStream();
    GZIPOutputStream gzipOutStream;
    gzipOutStream = new GZIPOutputStream(boutSteam);
    gzipOutStream.write(inputStream.readAllBytes());
    gzipOutStream.close();
    inputStream = new ByteArrayInputStream(boutSteam.toByteArray());
    properties.put("Content-Encoding", "gzip");
    return inputStream;
  }
}
