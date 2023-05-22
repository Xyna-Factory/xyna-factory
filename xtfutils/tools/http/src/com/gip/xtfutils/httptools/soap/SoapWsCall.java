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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import com.gip.xtfutils.httptools.http.HttpException;
import com.gip.xtfutils.httptools.http.HttpPost;
import com.gip.xtfutils.httptools.http.HttpResponse;


public class SoapWsCall extends HttpPost {

  private SoapInput _soapInput = null;


  public SoapWsCall(SoapInput input) {
    super(input);
    _soapInput = input;
  }



  public static HttpResponse execute(SoapInput input) throws HttpException {
    return new SoapWsCall(input).execute();
  }


  public HttpResponse execute() throws HttpException {
    _input.verify();
    HttpURLConnection connection = null;
    try {
      connection = openConnection();
      //connection.setRequestProperty("Content-Type","text/xml; charset=utf-8");
      connection.setRequestProperty("User-Agent", _soapInput.getHttpPropertyUserAgent());
      connection.setRequestProperty("Content-Type","text/xml");
      connection.setRequestProperty("soapaction", "\"" + _soapInput.getSoapAction() + "\"");
      connection.setUseCaches(false);
      connection.setDoInput(true);
      connection.setDoOutput(true);

      post(connection, _soapInput.getPayload());
      HttpResponse ret = receive(connection);
      return ret;
    }
    /*
    catch (HttpException e) {
      throw e;
    }
    catch (Exception e) {
      //_logger.debug("", e);
      throw new HttpException("Error in SoapRequest.execute", e);
    }
    */
    finally {
      try {
        connection.disconnect();
      }
      catch (Exception ex2) {
        //do nothing
      }
    }
  }


  protected void post(HttpURLConnection connection, String payload) throws HttpException {
    try {
      ByteArrayOutputStream messageBaos = new ByteArrayOutputStream();

      OutputStreamWriter osw = null;
      try {
        osw = new OutputStreamWriter(messageBaos, _input.getEncoding().getStringValue());
        osw.write(payload);
      }
      catch (IOException ex1) {
        throw ex1;
      }
      finally {
        try {
          osw.close();
        }
        catch (Exception e) {
          //do nothing
        }
      }
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Length", "" + messageBaos.size());

      messageBaos.writeTo(connection.getOutputStream());
      messageBaos.flush();
    }
    catch (IOException e) {
      //_logger.debug("", e);
      throw new HttpException("Could not send POST message to server", e);
    }
  }



  /*
  protected InputStreamReader getInputStreamReader(HttpURLConnection connection, EncodingName encoding)
                   throws HttpException {
    InputStreamReader isr;
    try {
      InputStream is = connection.getInputStream();
      isr = new InputStreamReader(is, encoding.getStringValue());
    }
    catch (IOException e) {
      InputStream is = connection.getErrorStream();
      if (is == null) {
        throw new HttpException("Could not get Error Stream of Connection.", e);
      }
      isr = new InputStreamReader(is);
    }
    return isr;
  }
  */


  /*
  public HttpURLConnection openConnection() throws HttpException {
     HttpURLConnection ret = openConnection();
     //_logger.info(ret.getClass().getName());
     try {
       HttpsURLConnection conn = (HttpsURLConnection) ret;
       conn.setHostnameVerifier(new NullHostnameVerifier());
       //_logger.info(ret.getClass().getName());
       //_logger.info("set hostname verifier");
     }
     catch (Exception e) {
       _logger.debug("Connection is no HttpsURLConnection");
     }
     return ret;
  }
  */
}
