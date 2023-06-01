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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMIServerSocketFactory;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.gip.xyna.utils.io.WrappingServerSocket;
import com.gip.xyna.utils.io.ZippingSocket;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;

public class RMISSLServerSocketFactory implements RMIServerSocketFactory {
  
  private final SSLServerSocketFactory factory;
  private final boolean needClientAuth;
  private final String hostname;
    
  public RMISSLServerSocketFactory(String hostname) {
    this.hostname = hostname;
    //FIXME codeduplication in RMISSLClientSocketFactory.ClientSocketConnectionParameter
    try {
      
      KeyManagerFactory kmf;
      TrustManagerFactory tmf = null;
      
      String fileName = XynaProperty.RMI_IL_SSL_KEYSTORE_FILE.get();
      if (fileName == null) {
        throw new RuntimeException("No keystore file given!");
      }

      KeyStore ks = KeyStore.getInstance(XynaProperty.RMI_IL_SSL_KEYSTORE_TYPE.get());

      String passphrase = XynaProperty.RMI_IL_SSL_KEYSTORE_PASSPHRASE.get();
      char[] passphraseChars = null;
      if (passphrase != null && passphrase.length() > 0) {
        passphraseChars = passphrase.toCharArray();
      }

      try (FileInputStream fis = new FileInputStream(XynaProperty.RMI_IL_SSL_KEYSTORE_FILE.get())) {
        ks.load(fis, passphraseChars);
      }
      
      kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(ks, passphraseChars);

      String trustStoreFile = XynaProperty.RMI_IL_SSL_TRUSTSTORE_FILE.get();
      if (trustStoreFile != null) {
        KeyStore ts = KeyStore.getInstance(XynaProperty.RMI_IL_SSL_TRUSTSTORE_TYPE.get());

        String passphraseTS = XynaProperty.RMI_IL_SSL_TRUSTSTORE_PASSPHRASE.get();
        char[] passphraseTSChars = null;
        if (passphraseTS != null && passphraseTS.length() > 0) {
          passphraseTSChars = passphraseTS.toCharArray();
        }

        try (FileInputStream fis = new FileInputStream(trustStoreFile)) {
          ts.load(fis, passphraseTSChars);
        }
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
      }

      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(kmf.getKeyManagers(), tmf == null ? null : tmf.getTrustManagers(), null);
      needClientAuth = tmf != null;
      factory = ctx.getServerSocketFactory();   
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
  
  public ServerSocket createServerSocket(int port) throws IOException {
    final SSLServerSocket ss =(SSLServerSocket) factory.createServerSocket(port, -1, InetAddress.getByName(hostname));
    ss.setEnabledCipherSuites(ss.getSupportedCipherSuites());
    ss.setEnabledProtocols(ss.getSupportedProtocols());
    //konfigurierbar machen?
    if (needClientAuth) {
      ss.setNeedClientAuth(true); //TODO wantclientauth konfigurierbar?
    }
    return new WrappingServerSocket(ss) { //alle methoden an SSLServerSocket delegieren
    
      @Override
      public Socket accept() throws IOException {
        Socket s;
        if (XynaProperty.RMI_IL_SOCKET_USE_COMPRESSION.get()) {
          s = new ZippingSocket(super.accept(), XynaProperty.RMI_IL_SOCKET_COMPRESSION_BUFFERSIZE.get());
        } else {
          s = super.accept();
        }
        //TODO das timeout von oben ohne nachdenken zu verwenden ist nicht gut, weil verschiedene timeouts nicht ohne weiteres realisiert werden können
        //siehe bug 23373 
        //problem ist grob, dass TCPEndpoint in der RMI-Infrastruktur equals auf sowohl ClientSocketFactory als auch ServerSocketFactory benutzt,
        //verschiedene timeouts zusammen mit gleichem communicationport führt dann dazu, dass der Endpoint als unterschiedliche befunden wird
        //aber nicht ans Socket gebunden werden kann (Port already in use)
        
        //der timeout hier ist auch nicht "tragisch", weil sockettimeout an diesem socket gibt es ja nur, wenn der server also auf den client wartet.
        //das sollte nur der fall sein, falls die verbindung mal verloren geht.
        s.setSoTimeout((int) XynaProperty.RMI_SERVER_SOCKET_TIMEOUT.get().getDurationInMillis());
        return s;
      }

    };
  }

  public String toString() {
    return RMISSLServerSocketFactory.class.getSimpleName()+"@"+Integer.toHexString(System.identityHashCode(this))
     +"(needClientAuth="+needClientAuth+", factory="+factory+")";
   }
  

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (needClientAuth ? 1231 : 1237);
    return result;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RMISSLServerSocketFactory other = (RMISSLServerSocketFactory) obj;
    if (needClientAuth != other.needClientAuth)
      return false;
    return true;
  }
  
}