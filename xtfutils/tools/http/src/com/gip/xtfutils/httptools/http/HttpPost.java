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


package com.gip.xtfutils.httptools.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;



/**
 * class that performs http post requests
 */
public class HttpPost extends HttpRequestCall {

  private HttpPostInput _postInput = null;


  public HttpPost(HttpPostInput input) {
    super(input);
    _postInput = input;
  }

  public static HttpResponse execute(HttpPostInput input) throws HttpException {
    return new HttpPost(input).execute();
  }

  public HttpResponse execute() throws HttpException {
    _input.verify();
    HttpURLConnection connection = null;
    try {
      connection = openConnection();
      post(connection);
      HttpResponse ret = receive(connection);
      return ret;
    }
    finally {
      try {
        connection.disconnect();
      }
      catch (Exception ex2) {
        //do nothing
      }
    }
  }


  protected void post(HttpURLConnection connection) throws HttpException {
    connection.setDoOutput(true);
    OutputStreamWriter out = null;
    try {
      connection.setRequestMethod("POST");
      out = new OutputStreamWriter(connection.getOutputStream(), _input.getEncoding().getStringValue());
      out.write(_postInput.getPayload());
      out.flush();
    }
    catch (IOException e) {
      throw new HttpException("Could not send POST message to server.", e);
    }
    finally {
      try {
        if (out != null) {
          out.close();
        }
      }
      catch (IOException e) {
        throw new HttpException("Could not close outputstream while sending POST request.", e);
      }
    }
  }

}
