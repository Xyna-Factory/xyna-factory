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



public class HttpInput {

  private String _url = null;
  private EncodingName _encoding = EncodingName.UTF_8;
  //private Logger logger = null;
  private int _readTimeoutMillis = 60000;
  private int _connectTimeoutMillis = 60000;
  private boolean _disableSSLCertificateCheck = false;
  private boolean _disableSSLHostnameVerification = false;


  public String getUrl() {
    return _url;
  }


  public EncodingName getEncoding() {
    return _encoding;
  }


  public int getReadTimeoutMillis() {
    return _readTimeoutMillis;
  }


  public int getConnectTimeoutMillis() {
    return _connectTimeoutMillis;
  }


  public boolean getDisableSSLCertificateCheck() {
    return _disableSSLCertificateCheck;
  }

  public void disableSSLCertificateCheck() {
    this._disableSSLCertificateCheck = true;
  }

  public boolean getDisableSSLHostnameVerification() {
    return _disableSSLHostnameVerification;
  }

  public void disableSSLHostnameVerification() {
    this._disableSSLHostnameVerification = true;
  }


  public void setUrl(String _url) {
    this._url = _url;
  }


  public void setEncoding(EncodingName _encoding) {
    this._encoding = _encoding;
  }


  public void setReadTimeoutMillis(int _readTimeoutMillis) {
    this._readTimeoutMillis = _readTimeoutMillis;
  }


  public void setConnectTimeoutMillis(int _connectTimeoutMillis) {
    this._connectTimeoutMillis = _connectTimeoutMillis;
  }


  public void verify() {
    if (_url == null) {
      throw new IllegalArgumentException("URL is null.");
    }
  }

}
