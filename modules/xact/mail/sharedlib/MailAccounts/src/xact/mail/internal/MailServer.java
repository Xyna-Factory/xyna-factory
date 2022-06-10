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
package xact.mail.internal;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreConversionError;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStore;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyManagement;
import com.sun.mail.util.MailSSLSocketFactory;

import xact.mail.MailLogger;
import xact.mail.Receiver;
import xact.mail.account.MailAccountData;
import xact.mail.account.MailAccountData.KnownAccountProtocol;
import xact.mail.account.MailAccountData.KnownTransportProtocol;
import xact.mail.account.MailAccountData.MailAccountProperty;
import xact.mail.account.MailAccountData.Protocol;
import xact.mail.account.MailAccountData.Security;
import xact.mail.account.MailAccountStorage;

public class MailServer implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(MailServer.class);
  private static final int DEFAULT_SOCKET_TIMEOUT = 30 * 1000;
  
  

  private final MailAccountData mailAccount;
  private final SocketParameter socketParameter;
  private Properties properties;
  private boolean lockRequired;
  
  public MailServer(MailAccountData mailAccount) {
    this(mailAccount, new SocketParameter());
  }
  
  public MailServer(MailAccountData mailAccount, SocketParameter socketParameter) {
    this.mailAccount = mailAccount;
    if (mailAccount.getAccountProtocol() == KnownAccountProtocol.NONE &&
        mailAccount.getTransportProtocol() == KnownTransportProtocol.NONE) {
      throw new IllegalStateException("Internal mail account '" + mailAccount.getName() + "' can not be used for MailServer.");
    }
    this.socketParameter = socketParameter;
  }
  
  public MailAccountData getMailAccount() {
    return mailAccount;
  }
  
  private Session startSession() {
    Session session = null;
    if( mailAccount.getPassword() != null ) {
      Authenticator authenticator = new Authenticator() {
        @Override
        public PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(mailAccount.getUser(),mailAccount.getPassword());
        }
      };
      session = Session.getInstance(getOrCreateProperties(), authenticator );
    } else {
      session = Session.getInstance(getOrCreateProperties());
    }
    if( logger.isTraceEnabled() ) {
      session.setDebugOut( MailLogger.createLogging(logger, Level.DEBUG) );
      session.setDebug(true);
    }
    return session;
  }
  
  private Properties getOrCreateProperties() {
    if( properties != null ) {
      return properties;
    }
    Properties props = new Properties();
    for( MailAccountProperty prop : mailAccount.getProperties() ) {
      props.setProperty(prop.getKey(), prop.getValue() );
    }
    
    String protocol = mailAccount.getAccountProtocol().getProtocolIdentifier();
    setGenericConnectionParams(protocol, mailAccount.getAccountPort(), props);
    
    String transportProtocol = mailAccount.getTransportProtocol().getProtocolIdentifier();
    setGenericConnectionParams(transportProtocol, mailAccount.getTransportPort(), props);
    
    SSLSocketFactory sf = createSSLSocketFactory(mailAccount);
    props.put("mail."+protocol+".ssl.socketFactory", sf);
    props.put("mail."+transportProtocol+".ssl.socketFactory", sf);
    
    setSecurityParams(props, mailAccount.getAccountSecurity(), mailAccount.getAccountProtocol());
    setSecurityParams(props, mailAccount.getTransportSecurity(), mailAccount.getTransportProtocol());
    
    properties = props;
    return properties;
  }

  
  private void setGenericConnectionParams(String protocol, int port, Properties props) {
    props.setProperty("mail."+protocol+".host", mailAccount.getHost());
    if( mailAccount.getUser() != null ) {
      props.setProperty("mail."+protocol+".user", mailAccount.getUser());
    }
    props.setProperty("mail."+protocol+".port", Integer.toString(port));
    
    props.setProperty("mail."+protocol+".timeout", Integer.toString(socketParameter.getCommunicationTimeout()));
    props.setProperty("mail."+protocol+".connectiontimeout", Integer.toString(socketParameter.getConnectionTimeout()));
  }

  private static void setSecurityParams(Properties props, Security security, Protocol protocol) {
    switch (security) {
      case NONE :
        break;
      case SSL :
        props.setProperty("mail." + protocol.getProtocolIdentifier() + ".ssl.enable", "true");
        break;
      case STARTTLS :
        props.setProperty("mail." + protocol.getProtocolIdentifier() + ".starttls.enable", "true");
        break;
      default :
        break;
    }
  }

  private Transport connectTransport(Session session) throws MessagingException {
    Transport transport = session.getTransport(mailAccount.getTransportProtocol().getProtocolIdentifier());
    if( transport == null ) {
      throw new MessagingException("No transport obtained");
    }
    transport.connect(mailAccount.getHost(), mailAccount.getUser(), mailAccount.getPassword());
    return transport;
  }

  private Store connectStore(Session session, String protocol) throws MessagingException {
    Store store = session.getStore(protocol);
    store.connect(mailAccount.getHost(), mailAccount.getUser(), mailAccount.getPassword() );
    return store;
  }


  public String getProtocol() {
    return mailAccount.getAccountProtocol().getProtocolIdentifier();
  }

  public interface FolderExecutor {

    void execute(Folder folder) throws MessagingException;
    
  }
  
  public interface SessionExecutor {

    void execute(Session session, MailServer mailServer) throws MessagingException;
    
  }
  
  public interface SessionTransportExecutor {

    void execute(Session session, Transport transport) throws Exception;
    
  }
  
  public void startSessionAndExecute(SessionExecutor executor) throws MessagingException {
    Session session = startSession();
    ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(MailServer.class.getClassLoader());
      executor.execute(session,this);
    } finally {
      Thread.currentThread().setContextClassLoader(previousContextClassLoader);
    }
  }
  
  
  
  public void startSessionTransportAndExecute(SessionTransportExecutor executor) throws Exception {
    /*
    MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
    mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
    mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
    mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
    mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
    mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822"); 
    */
    Session session = startSession();
    ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
    logger.info("##11## previous classloader "+previousContextClassLoader );
    
    Transport transport = null;
    try {
      Thread.currentThread().setContextClassLoader(MailServer.class.getClassLoader());
      logger.info("##11## current classloader "+Thread.currentThread().getContextClassLoader());
      
      transport = connectTransport(session);
      executor.execute(session,transport);
    } finally {
      Thread.currentThread().setContextClassLoader(previousContextClassLoader);
      if( transport != null ) {
        try {
          transport.close();
        } catch (MessagingException e) {
          logger.warn("Could not close transport",e);
        }
      }
    }
  }
  
  public void openFolderAndExecute(String folderName, boolean writeAccess, FolderExecutor executor) throws MessagingException {
    Session session = startSession();
    ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
    if( lockRequired ) {
      MailAccountStorage.getInstance().lock(mailAccount.getName());
    }
    try {
      // http://www.oracle.com/technetwork/java/faq-135477.html#castmultipart
      Thread.currentThread().setContextClassLoader(MailServer.class.getClassLoader());
      openFolderAndExecuteInternal(session, folderName, writeAccess, executor);
    } finally {
      if( lockRequired ) {
        MailAccountStorage.getInstance().unlock(mailAccount.getName());
      }
      Thread.currentThread().setContextClassLoader(previousContextClassLoader);
    }
  }

  private void openFolderAndExecuteInternal(Session session, String folderName, boolean writeAccess,
      FolderExecutor executor) throws MessagingException {
    Store store = null;
    Folder folder = null;
    try {
      store = connectStore(session, getProtocol() );
      folder = store.getFolder(folderName);
      folder.open( writeAccess ? Folder.READ_WRITE : Folder.READ_ONLY);
      executor.execute(folder);
    } finally {
      if( folder != null ) {
        folder.close(writeAccess); //TODO so ok? 
      }
      if( store != null ) {
        store.close();
      }
    }
  }

  public SMTPImpl createSender() {
    return new SMTPImpl(this);
  }

  public Receiver getReceiver() {
    if( mailAccount.getAccountProtocol() == KnownAccountProtocol.NONE ) {
      throw new IllegalStateException("MailAccount is not configured for receiving mails");
    }
    return mailAccount.getAccountProtocol().getReceiver(this);
  }


  public void setLockRequired() {
    this.lockRequired = true;
  }
  
  
  private SSLSocketFactory createSSLSocketFactory(MailAccountData mailAccount) {
    try {
      return createSslSocketFactory(mailAccount.getKeyStore(), mailAccount.getTrustStore());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private SSLSocketFactory createSslSocketFactory(String keyStore, String trustStore) throws KeyManagementException, NoSuchAlgorithmException, XFMG_KeyStoreConversionError,
                                                                          XFMG_UnknownKeyStoreType, XFMG_UnknownKeyStore, StringParameterParsingException {
     Map<String, String> map = new HashMap<>();
     
     KeyManagerFactory kmf = null;
     if (keyStore != null &&
         keyStore.length() > 0) {
       KeyManagement km = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
       kmf = km.getKeyStore(keyStore, KeyManagerFactory.class, map);
     }
     
    TrustManagerFactory tmf = null;
    if (trustStore != null &&
        trustStore.length() > 0) {
      KeyManagement km = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
      tmf = km.getKeyStore(keyStore, TrustManagerFactory.class, map);
    }
    
    SSLSocketFactory sf = null;
    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(kmf == null ? null : kmf.getKeyManagers(),
             tmf == null ? null : tmf.getTrustManagers(),
             null);
    sf = ctx.getSocketFactory();
    return sf;
  }
  
  
  public static class SocketParameter {
    
    private final int connectionTimeout;
    private final int communicationTimeout;
    
    public SocketParameter() {
      this(DEFAULT_SOCKET_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
    }

    public SocketParameter(int connectionTimeout, int communicationTimeout) {
      this.connectionTimeout = connectionTimeout;
      this.communicationTimeout = communicationTimeout;
    }

    public int getConnectionTimeout() {
      return connectionTimeout;
    }
    
    public int getCommunicationTimeout() {
      return communicationTimeout;
    }
    
  }

}
