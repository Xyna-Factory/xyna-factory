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

package com.gip.xyna.xact.trigger.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

import javax.net.ssl.*;

import org.apache.log4j.Logger;

import com.ibm.mq.jms.MQConnectionFactory;


public class SSLTools {

  private static final Logger _logger = Logger.getLogger(SSLTools.class);


  public static void adjustForSSL(MQConnectionFactory factory, SSLConfig config) {
    try {
      SSLSocketFactory ssf = buildSSLSocketFactory(config);
      factory.setSSLSocketFactory(ssf);
      factory.setSSLCipherSuite(config.getCipherSuiteName());
      factory.setSSLFipsRequired(false);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public static SSLSocketFactory buildSSLSocketFactory(SSLConfig config) throws NoSuchAlgorithmException,
                                                                                        CertificateException,
                                                                                        IOException,
                                                                                        KeyStoreException,
                                                                                        UnrecoverableKeyException,
                                                                                        KeyManagementException {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    FileInputStream keyStoreInput = new FileInputStream(config.getKeystorePath());
    try {
      keyStore.load(keyStoreInput, config.getKeystorePassword().toCharArray());
    }
    finally {
      keyStoreInput.close();
    }
    KeyStore trustStore = KeyStore.getInstance("JKS");
    FileInputStream trustStoreInput = new FileInputStream(config.getTruststorePath());
    try {
      trustStore.load(trustStoreInput, config.getTruststorePassword().toCharArray());
    }
    finally {
      trustStoreInput.close();
    }
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

    keyManagerFactory.init(keyStore, config.getKeystorePassword().toCharArray());
    trustManagerFactory.init(trustStore);
    SSLContext sslContext = SSLContext.getInstance("TLS");
    _logger.debug("SSLContext provider: " + sslContext.getProvider().toString());

    sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
    return sslSocketFactory;
  }

}
