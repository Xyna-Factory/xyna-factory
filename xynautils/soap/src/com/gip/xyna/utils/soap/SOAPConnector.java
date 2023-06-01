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



import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.soap.Codes;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.security.KeyStore;
import java.util.HashMap;

import java.util.Iterator;

import javax.net.ssl.*;

import org.apache.log4j.Logger;



/**
 * Kapselt HttpURLConnection mit Util Methoden für Soap.
 * 
 */
public class SOAPConnector {

  public static final String PROPERTYKEY_CONTENT_TYPE = "Content-type";
  public static final String PROPERTYKEY_CONTENT_LENGTH = "Content-length";
  public static final String PROPERTYKEY_SOAP_ACTION = "SOAPAction";
  public static final String PROPERTYKEY_USER_AGENT = "User-Agent";

  private String protocol = null;
  private String hostname = null;
  private int port = -1;
  private String service = null;
  private int connectTimeout = 0;
  private int readTimeout = 0;
  private File keyStoreFile = null;
  private String keyStorePassword = "";
  private HashMap<String, String> properties = new HashMap<String, String>();
  private HttpURLConnection httpConn = null;

  private static Logger logger = Logger.getLogger("xyna.utils.soap.soapconnector");


  public SOAPConnector() {
    setRequestProperty(PROPERTYKEY_USER_AGENT, "XynaUtils.SOAP");
  }


  public SOAPConnector(String URL) {
    this();
    setURL(URL);
    setRequestProperty("Host", hostname + ":" + port);
  }


  /**
   * @param protocol
   * @param hostname
   * @param port
   * @param service context-root and endpoint
   */
  public SOAPConnector(String protocol, String hostname, int port, String service) {
    this();
    this.protocol = protocol;
    this.hostname = hostname;
    this.port = port;
    this.service = service;
    setRequestProperty("Host", hostname + ":" + port);
  }


  public void open() throws XynaException {
    if (httpConn != null) {
      close();
    }
    try {
      URL url = new URL(protocol, hostname, port, service);
      logger.debug(url);

      httpConn = (HttpURLConnection) url.openConnection(); // Cast erlaubt, da URL mit http
      if (httpConn instanceof HttpsURLConnection) {
        logger.debug("httpConn is HttpsURLConnection");
        ((HttpsURLConnection) httpConn).setHostnameVerifier(new NullHostnameVerifier());
        if (keyStoreFile != null) {
          ((HttpsURLConnection) httpConn).setSSLSocketFactory(createSSLSocketFactory(keyStoreFile, keyStorePassword));
        }
      }
      httpConn.setConnectTimeout(connectTimeout);
      httpConn.setReadTimeout(readTimeout);
      // Connection für Input und Output einrichten
      httpConn.setDoInput(true);
      httpConn.setDoOutput(true);
      // kein Cache, da dynamische Daten
      httpConn.setUseCaches(false);
      // Angaben zur Übertragung (siehe http://www.ietf.org/rfc/rfc2068.txt)
      httpConn.setRequestMethod("POST");
      Iterator<String> it = properties.keySet().iterator();
      while (it.hasNext()) {
        String key = it.next();
        String val = properties.get(key);
        httpConn.setRequestProperty(key, val);
      }
    }
    catch (MalformedURLException mue) {
      throw new XynaException(Codes.CODE_URL_INVALID(protocol, hostname, port, service));
    }
    catch (IOException ioe) {
      throw new XynaException(Codes.CODE_CONNECTION_WONT_OPEN);
    }
  }


  /**
   * falls httpConnection offen, wird property dort gelesen, ansonsten aus lokalem cache.
   * 
   * @param key
   * @return
   */
  public String getRequestProperty(String key) {
    if (httpConn != null) {
      return httpConn.getRequestProperty(key);
    }
    return properties.get(key);
  }


  /**
   * falls httpConnection offen, wird die property dort gesetzt, ansonsten beim nächsten öffnen
   * 
   * @param key
   * @param value
   */
  public void setRequestProperty(String key,
                                 String value) {
    if (httpConn != null) {
      httpConn.setRequestProperty(key, value); // das geht wohl immer schief
      logger.debug("Set Http-Request-Property: " + key + "=" + value);
    }
    properties.put(key, value);
  }


  /**
   * kurz für setRequestProperty(PROPERTYKEY_CONTENT_TYPE, ct);
   * 
   * @param ct
   */
  public void setContentType(String ct) {
    setRequestProperty(PROPERTYKEY_CONTENT_TYPE, ct);
  }


  /**
   * kurz für setRequestProperty(PROPERTYKEY_USER_AGENT, userAgent);
   * 
   * @param userAgent The userAgent to set.
   */
  public void setUserAgent(String userAgent) {
    setRequestProperty(PROPERTYKEY_USER_AGENT, userAgent);
  }


  public OutputStream getOutputStream() throws IOException {
    OutputStream stream = null;
    if (httpConn != null) {
      stream = httpConn.getOutputStream();
    }
    return stream;
  }


  public InputStream getInputStream() {
    InputStream stream = null;
    if (httpConn != null) {
      try {
        stream = httpConn.getInputStream();
      }
      catch (IOException e) {
        logger.error(e.getMessage(), e);
        stream = httpConn.getErrorStream();
      }
    }
    return stream;
  }


  public void close() {
    if (httpConn != null) {
      httpConn.disconnect();
      httpConn = null;
    }
  }


  /**
   * @param url komplette URL des SoapServers "protocol://hostname:port/service"
   */
  public void setURL(String url) {
    String[] urlPart = url.split("/");
    protocol = urlPart[0].substring(0, urlPart[0].length() - 1);
    int pos = urlPart[2].indexOf(":");
    hostname = urlPart[2].substring(0, pos);
    port = Integer.parseInt(urlPart[2].substring(pos + 1));
    service = "";
    for (int p = 3; p < urlPart.length; ++p) {
      service += "/" + urlPart[p];
    }
    logger.debug("URL: " + protocol + "://" + hostname + ":" + port + service);
    setRequestProperty("Host", hostname + ":" + port);
  }


  /**
   * @param protocol the protocol to set
   */
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  /**
   * @return the protocol
   */
  public String getProtocol() {
    return protocol;
  }


  /**
   * @param hostname the hostname to set
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }


  /**
   * @return the hostname
   */
  public String getHostname() {
    return hostname;
  }


  /**
   * @param port the port to set
   */
  public void setPort(int port) {
    this.port = port;
  }


  /**
   * @return the port
   */
  public int getPort() {
    return port;
  }


  /**
   * @param service the service to set
   */
  public void setService(String service) {
    this.service = service;
  }


  /**
   * @return the service
   */
  public String getService() {
    return service;
  }


  public HttpURLConnection getHttpCon() {
    return httpConn;
  }


  /**
   * @param connectTimeout in ms
   */
  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }


  /**
   * @return connectTimeout
   */
  public int getConnectTimeout() {
    return connectTimeout;
  }


  /**
   * @param readTimeout in ms
   */
  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }


  /**
   * @return readTimeout
   */
  public int getReadTimeout() {
    return readTimeout;
  }


  /**
   * getKeyStoreFile
   * 
   * @return
   */
  public File getKeyStoreFile() {
    return keyStoreFile;
  }


  /**
   * setKeyStoreFile
   * 
   * @param keyStoreFile
   */
  public void setKeyStoreFile(File keyStoreFile) {
    this.keyStoreFile = keyStoreFile;
  }


  /**
   * getKeyStorePassword
   * 
   * @return
   */
  public String getKeyStorePassword() {
    return keyStorePassword;
  }


  /**
   * setKeyStorePassword
   * 
   * @param keyStorePassword
   */
  public void setKeyStorePassword(String keyStorePassword) {
    this.keyStorePassword = keyStorePassword;
  }


  /**
   * createSSLSocketFactory
   * 
   * @param keyStoreFile
   * @param keyStorePassword
   * @return
   * @throws Exception
   */
  private static SSLSocketFactory createSSLSocketFactory(File keyStoreFile,
                                                         String keyStorePassword) throws IOException {
    try {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      FileInputStream fileInputStream = new FileInputStream(keyStoreFile);
      keyStore.load(fileInputStream, keyStorePassword.toCharArray());
      fileInputStream.close();

      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(keyStore);
      SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
      return sslContext.getSocketFactory();
    }
    catch (Exception e) {
      throw new IOException("Could not create send SSLSocketFactory keyStoreFile=" + keyStoreFile + ", Error Message: " + e.getMessage());
    }
  }


  public static class NullHostnameVerifier implements HostnameVerifier {

    public boolean verify(String hostname,
                          SSLSession session) {
      return true;
    }

  }


}
