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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.io.ZippingSocket;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;


public class RMISSLClientSocketFactory implements RMIClientSocketFactory, Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(RMISSLClientSocketFactory.class);
 
  //verwendung von ssl parametern bei deserialisierung von remote socketfactory
  public static ThreadLocal<Pair<Duration, ClientSocketConnectionParameter>> threadLocalConParams = new ThreadLocal<Pair<Duration, ClientSocketConnectionParameter>>();
  
  private transient ClientSocketConnectionParameter conParams;

  private Duration timeout;

  public RMISSLClientSocketFactory() {
  }

  public Socket createSocket(String host, int port) throws IOException {
    conParams.port = port; //TODO derzeit ist port im Konstruktor nicht gesetzt, da unbekannt
    return conParams.createSocket(timeout);
  }
  
  /*
   * TODO: pattern mit threadlocal und con-parameter allgemein zum rmi management umziehen und dort auch f�r die nicht-ssl-clientsocketfactory verwenden
   */
  public static class ClientSocketConnectionParameter {
    
    private final String host; //Host und Port werden hier gespeichert, da der Server/Host evtl. nicht wei�, ...
    private int port;    //... wie er vom Client aus erreichbar ist. (Port-Forwarding, NAT, 0.0.0.0, ...)
    //TODO port ist nicht im Konstruktor bekannt...
    private final String keystoreFileName;
    private final String keystoreType;
    private final String keystorePassphrase;
    private final String truststoreFileName;
    private final String truststoreType;
    private final String truststorePassphrase;
    
    private static final String DEFAULT_KEYSTORE_TYPE_PARAMETER = "JKS";
    
    public ClientSocketConnectionParameter(String host, int port, String keystoreFileName, String keystoreType) {
      this(host, port, keystoreFileName, keystoreType, null, null, null, null);
    }


    public ClientSocketConnectionParameter(String host, int port, 
                                        String keystoreFileName, String keystoreType, String keystorePassphrase,
                                        String truststoreFileName, String truststoreType, String truststorePassphrase) {
      this.host = host;
      this.port = port;
      if (keystoreFileName == null) {
        logger.debug("no keystore provided, using xynaproperty");
        this.keystoreFileName = XynaProperty.RMI_IL_SSL_KEYSTORE_FILE.get();
        this.keystoreType = XynaProperty.RMI_IL_SSL_KEYSTORE_TYPE.get();
        this.keystorePassphrase = XynaProperty.RMI_IL_SSL_KEYSTORE_PASSPHRASE.get();
      } else {
        this.keystoreFileName = keystoreFileName;
        if (keystoreType == null) {
          this.keystoreType = DEFAULT_KEYSTORE_TYPE_PARAMETER;
        } else {
          this.keystoreType = keystoreType;
        }
        this.keystorePassphrase = keystorePassphrase;
      }
      this.truststoreFileName = truststoreFileName;
      this.truststorePassphrase = truststorePassphrase;
      if (truststoreType == null) {
        this.truststoreType = DEFAULT_KEYSTORE_TYPE_PARAMETER;
      } else {
        this.truststoreType = truststoreType;
      }
    }

    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((keystoreFileName == null) ? 0 : keystoreFileName.hashCode());
      result = prime * result + ((keystorePassphrase == null) ? 0 : keystorePassphrase.hashCode());
      result = prime * result + ((keystoreType == null) ? 0 : keystoreType.hashCode());
      result = prime * result + ((truststoreFileName == null) ? 0 : truststoreFileName.hashCode());
      result = prime * result + ((truststorePassphrase == null) ? 0 : truststorePassphrase.hashCode());
      result = prime * result + ((truststoreType == null) ? 0 : truststoreType.hashCode());
      result = prime * result + ((host == null) ? 0 : host.hashCode());
      result = prime * result + port;
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
      ClientSocketConnectionParameter other = (ClientSocketConnectionParameter) obj;
      if( ! stringEquals( keystoreFileName, other.keystoreFileName ) ) {
        return false;
      }
      if( ! stringEquals( keystorePassphrase, other.keystorePassphrase ) ) {
        return false;
      }
      if( ! stringEquals( keystoreType, other.keystoreType ) ) {
        return false;
      }
      if( ! stringEquals( truststoreFileName, other.truststoreFileName ) ) {
        return false;
      }
      if( ! stringEquals( truststorePassphrase, other.truststorePassphrase ) ) {
        return false;
      }
      if( ! stringEquals( truststoreType, other.truststoreType ) ) {
        return false;
      }
      if( ! stringEquals( host, other.host ) ) {
        return false;
      }
      if( port != other.port ) {
        return false;
      }
      return true;
    }

    private boolean stringEquals(String a, String b) {
      if (a == null) {
        return b == null;
      } else {
        return a.equals(b);
      }
    }

    public Socket createSocket(Duration timeout) throws IOException {
      try {
        KeyManagerFactory kmf = createKeyManagerFactory();
        TrustManagerFactory tmf = createTrustManagerFactory();
    
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf == null ? null : tmf.getTrustManagers(), null);
    
        SSLSocket socket =  (SSLSocket) ctx.getSocketFactory().createSocket(host, port);
        socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
        socket.setEnabledProtocols(socket.getSupportedProtocols());
        
        if( timeout != null ) {
          socket.setSoTimeout((int)timeout.getDurationInMillis());
        }
        if (XynaProperty.RMI_IL_SOCKET_USE_COMPRESSION.get()) {
          return new ZippingSocket(socket, XynaProperty.RMI_IL_SOCKET_COMPRESSION_BUFFERSIZE.get());
        } 
        return socket;
      } catch (IOException e) {
        throw e;
      } catch (Exception e) {
        throw new IOException("Could not create connection to " + host + ":" + port, e);
      }
    }
    
  
    private KeyManagerFactory createKeyManagerFactory() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, UnrecoverableKeyException {
      if (keystoreFileName == null) {
        throw new RuntimeException("No keystore file given!");
      }
      
      KeyStore ks = KeyStore.getInstance(keystoreType);
      
      char[] passphraseChars = null;
      if (keystorePassphrase != null &&
          keystorePassphrase.length() > 0) {
        passphraseChars = keystorePassphrase.toCharArray();
      }
      
      try (FileInputStream fis = new FileInputStream(keystoreFileName)) {
        ks.load(fis, passphraseChars);
      }
      
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(ks, passphraseChars);
      return kmf;
    }

    private TrustManagerFactory createTrustManagerFactory() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
      TrustManagerFactory tmf = null;
      if (truststoreFileName != null) {
        KeyStore ts = KeyStore.getInstance(truststoreType);

        char[] passphraseTSChars = null;
        if (truststorePassphrase != null && truststorePassphrase.length() > 0) {
          passphraseTSChars = truststorePassphrase.toCharArray();
        }

        try (FileInputStream fis = new FileInputStream(truststoreFileName)) {
          ts.load(fis, passphraseTSChars);
        }
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
      }
      return tmf;
    }


    public String getKeystoreFileName() {
      return keystoreFileName;
    }

    public String getKeystoreType() {
      return keystoreType;
    }
    
    public String getKeystorePassphrase() {
      return keystorePassphrase;
    }
 
    public String getTruststorePassphrase() {
      return truststorePassphrase;
    }

    public String getTruststoreType() {
      return truststoreType;
    }

    public String getTruststoreFileName() {
      return truststoreFileName;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }
  }
  
  
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    Pair<Duration, ClientSocketConnectionParameter> pair = threadLocalConParams.get();
    conParams = pair.getSecond();
    timeout = pair.getFirst();
  }
  
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  public String toString() {
   return RMISSLClientSocketFactory.class.getSimpleName()+"@"+Integer.toHexString(System.identityHashCode(this))
    +"(timeout=" + timeout
    +", conParams="+(conParams==null?"null":Integer.toHexString(System.identityHashCode(conParams)))
    +", host="+(conParams==null?"null":conParams.getHost())
    +")";
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((conParams == null) ? 0 : conParams.hashCode());
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
    RMISSLClientSocketFactory other = (RMISSLClientSocketFactory) obj;
    if (conParams == null) {
      if (other.conParams != null)
        return false;
    } else if (!conParams.equals(other.conParams))
      return false;
    return true;
  }

  
  
}
