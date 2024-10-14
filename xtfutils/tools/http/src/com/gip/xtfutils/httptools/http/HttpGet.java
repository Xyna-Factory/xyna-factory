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


package com.gip.xtfutils.httptools.http;

import java.io.IOException;
import java.net.HttpURLConnection;




/**
 * class that performs http get requests
 */
public class HttpGet extends HttpRequestCall {

  public HttpGet(HttpInput input) {
    super(input);
  }

  public static HttpResponse execute(HttpInput input) throws HttpException {
    return new HttpGet(input).execute();
  }

  public HttpResponse execute() throws HttpException {
    _input.verify();
    HttpURLConnection connection = null;
    try {
      connection = openConnection();
      try {
        connection.setRequestMethod("GET");
      }
      catch (IOException e) {
        throw new HttpException("Could not send GET message to server");
      }
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


}
