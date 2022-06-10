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
package com.gip.xyna.utils.exceptions.soap;

import com.gip.xyna.utils.exceptions.ExceptionStorage;
import org.apache.log4j.Logger;
import java.io.InputStream;

public class Codes {

  public static Logger logger = Logger.getLogger(Codes.class.getName());

  static {
    try {
      InputStream is = Codes.class.getResourceAsStream("/SOAPExceptions.xml");
      if (is == null) {
        throw new Exception("Resource not found.");
      }
      ExceptionStorage.loadFromStream(is);
    } catch (Exception e) {
      logger.error("Fehler beim Laden der Fehlermeldungen.", e);
      e.printStackTrace();
    }
  }

  public static final String[] CODE_HTTP_RESPONSE_CODE_ERROR(int responseCode) {
    return new String[]{"XYNA-12345", "" + responseCode};
  }

  public static final String[] CODE_URL_INVALID(String protocol, String hostName, int port, String service) {
    return new String[]{"XYNA-12346", protocol, hostName, "" + port, service};
  }

  public static final String CODE_CONNECTION_WONT_OPEN = "XYNA-12347";
  public static final String CODE_RESPONSE_PARSING_ERROR = "XYNA-12348";
  public static final String[] CODE_RESPONSE_SOAP_FAULT(String code, String faultString, String details) {
    return new String[]{"XYNA-12349", code, faultString, details};
  }

  public static final String CODE_REQUEST_INVALID_XML = "XYNA-12349";

}
