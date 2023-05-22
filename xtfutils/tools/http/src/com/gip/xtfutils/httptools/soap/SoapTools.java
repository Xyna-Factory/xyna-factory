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

package com.gip.xtfutils.httptools.soap;

import org.apache.log4j.Logger;

import com.gip.xtfutils.httptools.http.HttpException;
import com.gip.xtfutils.httptools.http.HttpResponse;


public class SoapTools {

  private static Logger _logger = Logger.getLogger(SoapTools.class);


  public static HttpResponse invokeWebservice(SoapInput input) throws HttpException {
    HttpResponse resp;
    try {
      resp = SoapWsCall.execute(input);
    }
    catch (HttpException ex1) {
      throw ex1;
    }
    catch (Exception e) {
      _logger.error("", e);
      throw new HttpException("Error trying to invoke webservice", e);
    }
    if (!resp.isResponseCodeOK()) {
      _logger.error("Webservice returns Fault: \n" + resp.toString());
      throw new HttpException("Webservice returns Fault: \n" + resp.toString());
    }
    return resp;
  }


}
