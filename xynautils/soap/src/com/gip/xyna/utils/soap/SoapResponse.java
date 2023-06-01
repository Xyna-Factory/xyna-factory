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
package com.gip.xyna.utils.soap;

import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.soap.Codes;
import com.gip.xyna.utils.soap.serializer.SoapEnvelope;
import com.gip.xyna.utils.soap.serializer.SoapEnvelopeSerializer;
import com.gip.xyna.utils.soap.serializer.SoapFault;
import com.gip.xyna.utils.xml.serializer.XynaFaultSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.HttpURLConnection;

import org.apache.log4j.Logger;


public class SoapResponse {

  private Logger logger = Logger.getLogger("xyna.utils.soap.SoapResponse");
  private String response;
  private HttpURLConnection httpConn;

  public SoapResponse(HttpURLConnection httpConn,
                      Encoding enc) throws IOException {
    this.httpConn = httpConn;
    // erst httpConn.getInputStream() schickt den Request ab!
    try {
      response =
          readerToString(new InputStreamReader(httpConn.getInputStream(),
                                               enc.getEncoding()));
    } catch (IOException e) {
      response =
          readerToString(new InputStreamReader(httpConn.getErrorStream(),
                                               enc.getEncoding()));
    }
    if (logger.isDebugEnabled()) {
      printHeader(httpConn);
    }
    logger.debug("Response erhalten ");
  }


  /**
   * @param httpConn
   */
  private void printHeader(HttpURLConnection httpConn) {
    String h = "Header";
    int n = 0;
    while (true) {
      String key = httpConn.getHeaderFieldKey(n);
      if (key == null)
        break;
      String value = httpConn.getHeaderField(n);
      h += "\n" + n + ": " + key + " -> " + value;
      ++n;
    }
    logger.debug(h);
  }

  /**
   * @param reader
   * @return
   * @throws IOException
   */
  private String readerToString(Reader reader) throws IOException {
    BufferedReader br = new BufferedReader(reader);
    StringBuffer sb = new StringBuffer();
    String line;
    while (null != ((line = br.readLine()))) {
      sb.append(line).append("\n");
    }
    br.close();
    return sb.toString();
  }

  public boolean hasError() {
    return getError() != null;
  }

  /**
   * gibt Fehler zurück, oder falls kein Fehler aufgetreten ist: null.
   * @return
   */
  public Exception getError() {
    if (response == null || response.length() == 0) {
      return null;
    }
    if (containsSoapFault()) {
      try {
        SoapFault sf = getXMLObject().getBody().getSoapFault();
        //check, ob xynafault enthalten:
        try {
          XynaFault_ctype xf =
            new XynaFaultSerializer(sf.getDetail()).toBean();
          return xf;
        } catch (Exception f) {
          return new XynaException(Codes.CODE_RESPONSE_SOAP_FAULT(sf.getFaultCode(),
                                                                  sf.getFaultString(),
                                                                  sf.getDetail()));
        }
      } catch (Exception e) {
        logger.error("couldn't parse SoapFault", e);
      }
    }
    if (hasHttpResponseCodeError()) {
      return new XynaException(Codes.CODE_HTTP_RESPONSE_CODE_ERROR(getHttpResponseCode()));
    }
    try {
      SoapEnvelope se = getXMLObject();
    } catch (Exception e) {
      return new XynaException(Codes.CODE_RESPONSE_PARSING_ERROR).initCause(e);
    }
    return null;
  }

  public String getXMLString() {
    return response;
  }

  public SoapEnvelope getXMLObject() throws Exception {
    return new SoapEnvelopeSerializer(response).toBean();
  }

  public HttpURLConnection getHttpCon() {
    return httpConn;
  }

  /**
   * gibt -1 zurück, falls keine httpConnection offen ist, oder eien IOException aufgetreten ist
   * @return
   */
  public int getHttpResponseCode() {
    try {
      return httpConn.getResponseCode();
    } catch (Exception e) {
      return -1;
    }
  }

  public boolean hasHttpResponseCodeError() {
    return getHttpResponseCode() >= 400;
  }

  public boolean containsSoapFault() {
    try {
      getXMLObject().getBody().getSoapFault();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
