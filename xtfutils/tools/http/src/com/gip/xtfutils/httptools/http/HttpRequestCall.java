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

package com.gip.xtfutils.httptools.http;

import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;

import javax.net.ssl.*;

import org.apache.log4j.Logger;



public abstract class HttpRequestCall {

  public static class NullHostnameVerifier implements HostnameVerifier {
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
  }


  protected static Logger _logger = Logger.getLogger(HttpRequestCall.class);
  protected HttpInput _input = null;

  public HttpRequestCall(HttpInput input) {
    _input = input;
  }


  /**
   * opens and returns a connection to the specified url
   * @throws HttpException
   */
  public HttpURLConnection openConnection() throws HttpException {
    URL url = null;
    try {
      url = new URL(_input.getUrl());
    }
    catch (MalformedURLException e) {
      throw new HttpException("Could not parse url", e);
    }

    //http
    HttpURLConnection connection = null;
    try {
      URLConnection urlConn = url.openConnection();
      if (urlConn instanceof HttpURLConnection) {
        connection = (HttpURLConnection)urlConn;
      }
      else {
        throw new HttpException("Could not create HttpURLConnection from provided http request string");
      }
    }
    catch (IOException e) {
      throw new HttpException("Could not open connection for request " + _input.getUrl());
    }

    //https
    try {
      HttpsURLConnection httpsConn = (HttpsURLConnection) connection;
      configureHttps(httpsConn);
    }
    catch (Exception e) {
      _logger.debug("Connection is no HttpsURLConnection");
    }

    _logger.debug("Setting read timeout to millis: " + _input.getReadTimeoutMillis());
    connection.setReadTimeout(_input.getReadTimeoutMillis());

    _logger.debug("Setting connect timeout to millis: " + _input.getConnectTimeoutMillis());
    connection.setConnectTimeout(_input.getConnectTimeoutMillis());
    return connection;
  }


  protected void configureHttps(HttpsURLConnection conn) {
    if (_input.getDisableSSLHostnameVerification()) {
      conn.setHostnameVerifier(new NullHostnameVerifier());
    }
    if (_input.getDisableSSLCertificateCheck()) {
      disableSSLHostnameVerification(conn);
    }
  }


  protected void disableSSLHostnameVerification(HttpsURLConnection conn) {
    TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                            }
                        public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
                };
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      //HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      conn.setSSLSocketFactory(sc.getSocketFactory());
    }
    catch (GeneralSecurityException e) {
      //do nothing
    }
  }


  /**
   * executes the http request, returns response
   */
  public HttpResponse receive(HttpURLConnection connection)
        throws HttpException {
    InputStreamReader isr = null;
    try {
      isr = getInputStreamReader(connection);
      HttpResponse ret = receive(connection, isr);
      return ret;
    }
    finally {
      try {
        _logger.debug("Closing http input stream");
        isr.close();
      }
      catch (Exception e) {
        //do nothing
      }
    }
  }


  public HttpResponse receive(HttpURLConnection connection, InputStreamReader isr)
        throws HttpException {
    HttpResponse ret = new HttpResponse();
    try {
      // Result Status
      ret.setResponseCode(connection.getResponseCode());
      ret.setResponseCodeMessage(connection.getResponseMessage());

      _logger.debug("RespCode = " + ret.getResponseCode());

      // Auslesen der Antwort auf den Http Request
      StringBuilder sb = new StringBuilder();
      BufferedReader br = new BufferedReader(isr);
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
        sb.append("\n");
      }
      ret.setResponsePayload(sb.toString());
      return ret;
    }
    catch(FileNotFoundException e) {
      //_logger.debug("", e);
      throw new HttpException("Could not find specified location: " + _input.getUrl(), e);
    }
    catch (SocketTimeoutException se) {
      //_logger.debug("", se);
      throw new HttpException("Timeout while reading " + _input.getUrl(), se);
    }
    catch (IOException e) {
      //_logger.debug("", e);
      throw new HttpException("Could not get result message for HTTP Request " + _input.getUrl(), e);
    }
  }


/*
  public InputStreamReader getInputStreamReader(HttpURLConnection connection)
        throws HttpException {
    try {
      InputStream is = connection.getInputStream();
      String encodingName = _input.getEncoding().getStringValue();
      InputStreamReader isr = new InputStreamReader(is, encodingName);
      return isr;
    }
    catch (IOException e) {
      //_logger.debug("", e);
      throw new HttpException("Could not get result message for HTTP Request " + _input.getUrl(), e);
    }
  }
*/

  protected InputStreamReader getInputStreamReader(HttpURLConnection connection)
                   throws HttpException {
    InputStreamReader isr;
    try {
      InputStream is = connection.getInputStream();
      isr = new InputStreamReader(is, _input.getEncoding().getStringValue());
    }
    catch (IOException e) {
      InputStream is = connection.getErrorStream();
      if (is == null) {
        throw new HttpException("Could not get Error Stream of Connection. URL = " + _input.getUrl(), e);
      }
      isr = new InputStreamReader(is);
    }
    return isr;
  }

}
