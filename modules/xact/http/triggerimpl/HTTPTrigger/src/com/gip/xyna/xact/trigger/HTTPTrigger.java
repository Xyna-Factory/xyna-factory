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
package com.gip.xyna.xact.trigger;



import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.security.AccessControlException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_NetworkInterfaceNotFoundException;
import com.gip.xyna.xact.trigger.http.HTTPTRIGGER_HTTP_RECEIVE_ERROR;
import com.gip.xyna.xact.trigger.http.HTTPTRIGGER_SSL_CERTIFICATE_ERROR;
import com.gip.xyna.xact.trigger.http.HTTPTRIGGER_ServerSocketCreationException;
import com.gip.xyna.xact.trigger.http.HTTPTRIGGER_SocketCloseException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreConversionError;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStore;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.AggregationStatisticsFactory;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsNodeTraversal;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PullStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.Statistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.IntegerStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class HTTPTrigger extends EventListener<HTTPTriggerConnection, HTTPStartParameter> {


  private static final Logger logger = CentralFactoryLogging.getLogger(HTTPTrigger.class);


  private ServerSocket serverSocketForSSL;
  private ServerSocketChannel serverSocketChannelForUnsecureMode;
  
  private HTTPStartParameter startParams;

  private volatile boolean isStopping = false;

  private String ownIP; // IP
  private String ownHostname; // name

  private AtomicLong receivedCounter = new AtomicLong(0);
  private AtomicLong rejectCounter = new AtomicLong(0);


  // the stopping task has to be initialized when the object is created because the jar file may already have changed
  // when stop is called
  private final BindTask stoppingBindTask = new BindTask() {

    public void execute() throws IOException {
      if (serverSocketChannelForUnsecureMode != null && serverSocketChannelForUnsecureMode.isOpen()) {
        serverSocketChannelForUnsecureMode.close();
      }
      if (serverSocketForSSL != null && !serverSocketForSSL.isClosed()) {
        serverSocketForSSL.close();
      }
    }

  };


  public HTTPTrigger() {
  }

  
  private static ServerSocketFactory getServerSocketFactory(HTTPStartParameter params) throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, XFMG_KeyStoreConversionError, XFMG_UnknownKeyStoreType, XFMG_UnknownKeyStore, StringParameterParsingException {
    switch (params.getKeyStoreParameter()) {
      case KEY_MGMT :
        KeyManagement km = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
        KeyManagerFactory kmf = null;
        if (params.getKeyStoreName() != null && 
            params.getKeyStoreName().length() > 0) {
          Map<String, String> map = new HashMap<String, String>();
          kmf = km.getKeyStore(params.getKeyStoreName(), KeyManagerFactory.class, map);
        }
        TrustManagerFactory tmf = null;
        if (params.getTrustStoreName() != null && 
            params.getTrustStoreName().length() > 0) {
          Map<String, String> map = new HashMap<String, String>();
          tmf = km.getKeyStore(params.getTrustStoreName(), TrustManagerFactory.class, map);
        }
        return getServerSocketFactory(kmf, tmf);
      case FILE :
      case TRUE :
        return getServerSocketFactory(params.getKeyStorePath(), params.getKeyStoreType(), params.getKeyStorePassword());
      case NONE :
      case FALSE :
        // should never be encountered
      default :
        return SSLServerSocketFactory.getDefault();
    }
  }
                                                            

  private static ServerSocketFactory getServerSocketFactory(String keyStorePath, String keyStoreType,
                                                            String keyStorePassword) throws KeyStoreException,
      NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, FileNotFoundException, IOException,
      KeyManagementException {

    if (keyStoreType.equals("default")) {
      return SSLServerSocketFactory.getDefault();
    }
    // set up key manager to do server authentication
    KeyManagerFactory kmf;
    KeyStore ks;
    char[] passphrase = keyStorePassword.toCharArray();

    kmf = KeyManagerFactory.getInstance("SunX509");
    ks = KeyStore.getInstance(keyStoreType);

    ks.load(new FileInputStream(keyStorePath), passphrase);
    kmf.init(ks, passphrase);
    
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);
    

    return getServerSocketFactory(kmf, tmf);
  }
  
  
  private static ServerSocketFactory getServerSocketFactory(KeyManagerFactory kmf, TrustManagerFactory tmf) throws NoSuchAlgorithmException, KeyManagementException {
    SSLServerSocketFactory ssf = null;
    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(kmf == null ? null : kmf.getKeyManagers(), tmf == null ? null : tmf.getTrustManagers(), null);
    ssf = ctx.getServerSocketFactory();
    return ssf;
  }


  private void createServerSocketSSL(HTTPStartParameter sp) throws IOException, HTTPTRIGGER_SSL_CERTIFICATE_ERROR,
      XACT_NetworkInterfaceNotFoundException, XACT_InterfaceNoIPConfiguredException {

    // lesen! http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html
    try {
      ServerSocketFactory ssocketFactory = getServerSocketFactory(sp);
      if (sp.getKeyStoreType().equals("default")) {
        serverSocketForSSL = ssocketFactory.createServerSocket(sp.getPort());
      } else {
        if (sp.getIP() == null) {
          serverSocketForSSL = ssocketFactory.createServerSocket(sp.getPort(), 0);
        } else {
          serverSocketForSSL = ssocketFactory.createServerSocket(sp.getPort(), 0, sp.getIP());
        }
        String[] ciphersuites = ((SSLServerSocket) serverSocketForSSL).getSupportedCipherSuites();
        ((SSLServerSocket) serverSocketForSSL).setEnabledCipherSuites(ciphersuites);
        switch( sp.getClientAuth() ) {
          case require:
            ((SSLServerSocket) serverSocketForSSL).setNeedClientAuth(true);
            break;
          case optional:
            ((SSLServerSocket) serverSocketForSSL).setWantClientAuth(true);
            break;
          case none:
            ((SSLServerSocket) serverSocketForSSL).setNeedClientAuth(false);
            ((SSLServerSocket) serverSocketForSSL).setWantClientAuth(false);
            break;
          default: 
            //sollte nicht vorkommen, da in startparameter klasse gehandlet.
            throw new IllegalArgumentException("invalid client auth parameter: " + sp.getClientAuth());
        }
      }
    } catch (KeyManagementException e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    } catch (KeyStoreException e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    } catch (NoSuchAlgorithmException e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    } catch (UnrecoverableKeyException e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    } catch (CertificateException e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    } catch (FileNotFoundException e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    } catch (XFMG_KeyStoreConversionError e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    } catch (XFMG_UnknownKeyStoreType e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    } catch (XFMG_UnknownKeyStore e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    } catch (StringParameterParsingException e) {
      throw new HTTPTRIGGER_SSL_CERTIFICATE_ERROR(sp.getPort(), e);
    }

  }


  public void start(final HTTPStartParameter sp) throws HTTPTRIGGER_ServerSocketCreationException {
    startParams = sp;
    isStopping = false;
    if (logger.isDebugEnabled()) {
      logger.debug("httptrigger.start: " + getClass().getClassLoader());
    }
    try {
      if (!sp.useHTTPs()) {
        serverSocketChannelForUnsecureMode = ServerSocketChannel.open();
      }
      retryBindException(new BindTask() {

        public void execute() throws IOException, XynaException {
          if (sp.useHTTPs()) {
            createServerSocketSSL(sp);
          } else {
            if (sp.getIP() == null) {
              getSocket().bind(new InetSocketAddress(sp.getPort()));
            } else {
              getSocket().bind(new InetSocketAddress(sp.getIP(), sp.getPort()));
            }
          }
        }

      }, 50, 100);
    } catch (IOException | XynaException  e) {
      throw new HTTPTRIGGER_ServerSocketCreationException(sp.toString() + ". " + e.getMessage(), e);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("listening for incoming http" + (sp.useHTTPs() ? "s" : "") + " on "
                      + sp.getAddress() + ", port " + sp.getPort() + " ...");
      if (logger.isTraceEnabled()) {
        try {
          logger.trace("SO_TIMEOUT=" + getSocket().getSoTimeout());
        } catch (IOException e) {
          logger.trace(null, e);
        }
      }
    }

    InetAddress address = getSocket().getInetAddress();
    // First get the IP address
    ownIP = address.getHostAddress();
    if (logger.isDebugEnabled()) {
      logger.debug("My IP is " + ownIP);
    }
    ownHostname = address.getHostName();
    if (logger.isDebugEnabled()) {
      logger.debug("My hostname is " + ownHostname);
    }

    //addStatistics
    try {
      TriggerInstanceIdentification i = getTriggerInstanceIdentification();
      registerStatistics();
    } catch (Exception e) {
      logger.info("HTTPTrigger Statistics could not be initialized. ", e);
    }
  }


  private ServerSocket getSocket() {
    if (serverSocketChannelForUnsecureMode != null) {
      return serverSocketChannelForUnsecureMode.socket();
    } else if (serverSocketForSSL != null) {
      return serverSocketForSSL;
    } else {
      throw new RuntimeException();
    }
  }
  
  
  public HTTPStartParameter getStartParameter() {
    return startParams;
  }


  public HTTPTriggerConnection receive() {
    try {
      receivedCounter.incrementAndGet();
      if (serverSocketChannelForUnsecureMode != null) {
        return new HTTPTriggerConnection(this, serverSocketChannelForUnsecureMode.accept(), startParams.suppressRequestLogging());
      } else if (serverSocketForSSL != null) {
        return new HTTPTriggerConnection(this, serverSocketForSSL.accept(), startParams.suppressRequestLogging());
      } else {
        throw new RuntimeException();
      }
    } catch (AccessControlException e) {
      // not onErrorDuringReceive to log less
      logger.error("error receiving connection: " + e.getMessage());
    } catch (IOException e) {
      if (!isStopping) {
        onErrorDuringReceive(new HTTPTRIGGER_HTTP_RECEIVE_ERROR(e.getMessage(), e));
      }
      //else socketclosedexception?
    } catch (XynaException e) {
      onErrorDuringReceive(e);
    }
    return null;
  }


  public void stop() throws HTTPTRIGGER_SocketCloseException {
    isStopping = true;
    logger.debug("stopping httptrigger");
    try {
      retryBindException(stoppingBindTask, 50, 100);
    } catch (IOException e) {
      throw new HTTPTRIGGER_SocketCloseException(getSocket().getLocalPort(), e);
    } catch (XynaException e) {
      throw new HTTPTRIGGER_SocketCloseException(getSocket().getLocalPort(), e);
    }

    //removeStatistics
    FactoryRuntimeStatistics statistics = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
    try {
      statistics.unregisterStatistic(getInstanceBasePath().append(StatisticsPathImpl.ALL));
    } catch (XFMG_InvalidStatisticsPath e) {
      logger.warn("invalid path supplied when trying to unregister statistics",e);
    }

    logger.debug("stopped httptrigger");
  }


  private void onErrorDuringReceive(XynaException e) {
    logger.error("error receiving connection", e);
  }


  public void onNoFilterFound(HTTPTriggerConnection con) {
    try {
      logger.warn("No filter responsible for " + con.getMethod().toString() + " " + con.getUri() + ". Returning " + HTTPTriggerConnection.HTTP_NOTFOUND);
      if (logger.isTraceEnabled()) {
        logger.trace("No filter found for connection " + con);
      }
      String msg = "<html><body><h3>" + HTTPTriggerConnection.HTTP_NOTFOUND + "</h3></body></html>";
      byte[] msgBytes = msg.getBytes(con.getCharSet());
      con.sendResponse(HTTPTriggerConnection.HTTP_NOTFOUND, HTTPTriggerConnection.MIME_HTML, null,
                       new ByteArrayInputStream(msgBytes), (long)msgBytes.length);
    } catch (UnsupportedEncodingException e) {
      con.handleUnsupportedEncoding();
    } catch (SocketNotAvailableException e) {
      logger.error("socket was unexpectedly not available when trying to send errormessage to client", e);
    }
  }


  @Override
  public String getClassDescription() {
    return "HTTP Trigger";
  }


  public String getOwnIp() {
    return ownIP;
  }


  public String getOwnHostname() {
    return ownHostname;
  }


  @Override
  public void onProcessingRejected(String s, HTTPTriggerConnection con) {
    try {
      rejectCounter.incrementAndGet();
      con.sendError(s);
    } catch (SocketNotAvailableException e) {
      logger.info("socket was unexpectedly not available when trying to send errormessage to client", e);
    }
  }
  
  
  private enum HttpTriggerStatisticType implements StatisticsPathPart {
    INSTANCENAME("InstanceName"),
    MAXEVENTS("ConfiguredMaxTriggerEvents"),
    CURRENTEVENTS("CurrentTriggerEvents"),
    RECEIVED("RequestsReceived"),
    REJECTED("RequestsRejected"),
    PROCESSED("RequestsProcessed");
    
    private HttpTriggerStatisticType(String partname) {
      this.partname = partname;
    }
 
    private final String partname;
    
    public String getPartName() {
      return partname;
    }

    public StatisticsNodeTraversal getStatisticsNodeTraversal() {
      return StatisticsNodeTraversal.SINGLE;
    }
    
  }
  
  private StatisticsPath instancePathPath;
  
  private StatisticsPath getInstanceBasePath() {
    if (instancePathPath == null) {
      String rtc = "WorkingSet";
      TriggerInstanceIdentification triggerId = getTriggerInstanceIdentification();
      if (triggerId.getRevision() != null && triggerId.getRevision() != -1L) {
        try {
          RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
          RuntimeContext runtimeContext = rm.getRuntimeContext(triggerId.getRevision());
          if(runtimeContext instanceof Application) {
            rtc = "Application-" + runtimeContext.getName();
          }
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          // "WorkingSet" als RuntimeContext in StatisticsPath eintragen
        }
      }
      
      instancePathPath = PredefinedXynaStatisticsPath.HTTPTRIGGER.append(rtc).append(triggerId.getInstanceName());
    } 
    return instancePathPath;
  }
  

  private void registerStatistics() {
    FactoryRuntimeStatistics statistics = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
      try {
        statistics.registerStatistic(new PullStatistics<String, StringStatisticsValue>(getInstanceBasePath().append(HttpTriggerStatisticType.INSTANCENAME)) {
          @Override
          public StringStatisticsValue getValueObject() { return new StringStatisticsValue(getTriggerInstanceIdentification().getInstanceName()); }
          @Override
          public String getDescription() { return "HttpTrigger instance name"; }
        });
      statistics.registerStatistic(new PullStatistics<Integer, IntegerStatisticsValue>(getInstanceBasePath()
          .append(HttpTriggerStatisticType.MAXEVENTS)) {
        @Override
        public IntegerStatisticsValue getValueObject() {
          long receives = getReceiveControlAlgorithm().getMaxReceivesInParallel();
          int result;
          if (receives > 0) {
            result = receives > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) receives;
          } else {
            result = receives < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) receives;
          }
          return new IntegerStatisticsValue(result);
        }
        @Override
        public String getDescription() {
          return "Maximum of parallel requests the trigger is configured to handle";
        }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath().append(HttpTriggerStatisticType.CURRENTEVENTS)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(getReceiveControlAlgorithm().getCurrentActiveEvents()); }
        @Override
        public String getDescription() { return "Amount of events the trigger is currently handling"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath().append(HttpTriggerStatisticType.RECEIVED)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(receivedCounter.get()); }
        @Override
        public String getDescription() { return "The amount of received events"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath().append(HttpTriggerStatisticType.REJECTED)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(rejectCounter.get()); }
        @Override
        public String getDescription() { return "The amount of rejected events"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath().append(HttpTriggerStatisticType.PROCESSED)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(receivedCounter.get() - rejectCounter.get()); }
        @Override
        public String getDescription() { return "The amount of events that were received and not rejected"; }
      });
    
      // register aggregations over applications
      for (HttpTriggerStatisticType statisticType : HttpTriggerStatisticType.values()) {
        StatisticsPath ownPath = PredefinedXynaStatisticsPath.HTTPTRIGGER.append(StatisticsPathImpl.simplePathPart("All"))
                                                                         .append(getTriggerInstanceIdentification().getInstanceName())
                                                                         .append(statisticType);
        
        Statistics existingStatistics;
        try {
          existingStatistics = statistics.getStatistic(ownPath);
        } catch (XFMG_InvalidStatisticsPath e) {
          throw new RuntimeException(e);
        }
        
        if (existingStatistics == null) {
          StatisticsPath pathToAggregate = PredefinedXynaStatisticsPath.HTTPTRIGGER
              .append(new StatisticsPathImpl.BlackListFilter("All"))
              .append(getTriggerInstanceIdentification().getInstanceName())
              .append(statisticType);
          Statistics aggregate = AggregationStatisticsFactory.generateDefaultAggregationStatistics(ownPath, pathToAggregate);
          try {
            statistics.registerStatistic(aggregate);
          } catch (XFMG_StatisticAlreadyRegistered e) {
            //ignore. dann war offenbar eine andere triggerinstanz schneller
          }
        }
      }
    } catch (XFMG_InvalidStatisticsPath e) {
      throw new RuntimeException(e);
    } catch (XFMG_StatisticAlreadyRegistered e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  protected void onFilterFailed(HTTPTriggerConnection tc, ConnectionFilter<?> connectionFilter, Throwable cause) {
    try {
      tc.sendError("Filter Failed");
    } catch (SocketNotAvailableException e) {
    }
    //super -> rollback macht nichts in triggerconnection
  }

}
