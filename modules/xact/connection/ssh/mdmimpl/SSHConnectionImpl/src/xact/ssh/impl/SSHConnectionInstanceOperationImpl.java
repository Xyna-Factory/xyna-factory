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
package xact.ssh.impl;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import xact.connection.Command;
import xact.connection.CommandResponseTuple;
import xact.connection.ConnectionAlreadyClosed;
import xact.connection.DeviceType;
import xact.connection.ReadTimeout;
import xact.connection.Response;
import xact.connection.SendParameter;
import xact.ssh.AuthenticationMethod;
import xact.ssh.EncryptionType;
import xact.ssh.HostKeyCheckingMode;
import xact.ssh.HostKeyStorableRepository;
import xact.ssh.IdentityStorableRepository;
import xact.ssh.LogAdapter;
import xact.ssh.PassphraseRetrievingUserInfo;
import xact.ssh.ProxyParameter;
import xact.ssh.SSHConnection;
import xact.ssh.SSHConnectionInstanceOperation;
import xact.ssh.SSHConnectionParameter;
import xact.ssh.SSHConnectionSuperProxy;
import xact.ssh.SSHProxyParameter;
import xact.ssh.SSHSendParameter;
import xact.ssh.SecureStorablePassphraseStore;
import xact.ssh.SupportedHostNameFeature;
import xact.ssh.XynaHostKeyRepository;
import xact.ssh.XynaIdentityRepository;
import xact.templates.DocumentType;
import xfmg.xfmon.protocolmsg.ProtocolMessage;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xprc.exceptions.XPRC_TTLExpirationBeforeHandlerRegistration;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandler;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.events.AbortServiceStepEvent;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;


public abstract class SSHConnectionInstanceOperationImpl extends SSHConnectionSuperProxy implements SSHConnectionInstanceOperation, ServiceStepEventHandler<AbortServiceStepEvent> {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(SSHConnectionInstanceOperationImpl.class);
  private static final String CONFIG_KEY_SERVER_HOST_KEY = "server_host_key";
  private static final SleepCounter sleepTemplate = new SleepCounter(10, 250, 25);
  private static final XynaPropertyBoolean timeoutexceptionDefault = 
                         new XynaPropertyBoolean("xact.ssh.readtimeout.use.default", true)
                               .setDefaultDocumentation(DocumentationLanguage.EN, "If the ssh send parameter for the decision of throwing read timeout exceptions is not set, this default will be used.");
  private static final XynaPropertyBuilds<Set<SupportedHostNameFeature>> supportedFeatures = 
                         new XynaPropertyBuilds<Set<SupportedHostNameFeature>>("xact.ssh.hostkeys.supportedfeatures",
                                                                               new XynaPropertyBuilds.Builder<Set<SupportedHostNameFeature>>() {

                                                                                public Set<SupportedHostNameFeature> fromString(String arg0)
                                                                                                throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
                                                                                  return SupportedHostNameFeature.fromStringList(arg0);
                                                                                }

                                                                                public String toString(Set<SupportedHostNameFeature> arg0) {
                                                                                  StringBuilder sb = new StringBuilder();
                                                                                  Iterator<SupportedHostNameFeature> iter = arg0.iterator();
                                                                                  while (iter.hasNext()) {
                                                                                    sb.append(iter.next().toString());
                                                                                    if (iter.hasNext()) {
                                                                                      sb.append(", ");  
                                                                                    }
                                                                                  }
                                                                                  return sb.toString();
                                                                                }
                           
                                                                               },
                                                                               SupportedHostNameFeature.all())
                               .setDefaultDocumentation(DocumentationLanguage.EN, "Supported features for the HostKeyRepository, turning features off can improve performance");
  public static final XynaPropertyInt substringLengthProperty = new XynaPropertyInt("xact.connection.ssh.partialResponseLength", 0);
  private static final XynaPropertyBoolean legacyErrorMessage = new XynaPropertyBoolean("xact.ssh.connect.timeout.errormessage.legacy", false)
                               .setDefaultDocumentation(DocumentationLanguage.EN, "Error message when socket connect fails with timeout is similar to what jsch creates when not using a socketfactory (\"timeout: socket is not established\")");
  private static final Thread pipedStreamHolder = new Thread(new Runnable() {
    public void run() {
      while (!XynaFactory.getInstance().isShuttingDown()) {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
      }
    }
  });
  
  static {
    pipedStreamHolder.setDaemon(true);
    pipedStreamHolder.setName("PipedStreamHolder");
    pipedStreamHolder.start();
  }
  
  protected transient JSch jsch;
  protected transient LogAdapter adapter;
  private transient TransientConnectionData transientConnectionData;
  private transient boolean commandSend = false;
  
  protected boolean prepared = false;
  
  protected long transientDataId;
  private volatile boolean isCanceled = false;
  private volatile AbortionCause cause;
  private boolean reconnectAfterRestart = true;
  private ProtocolMessageHandler protocolMessageHandler; 
  private StringBuilder accumulatedResponse;

  public SSHConnectionInstanceOperationImpl(SSHConnection instanceVar) {
    super(instanceVar);
    transientDataId = -1;
    transientConnectionData = new TransientConnectionData();
    protocolMessageHandler = ProtocolMessageHandler.newInstance(); 
  }
  
  protected SSHConnection getInstanceVar() {
    return (SSHConnection) instanceVar;
  }
  
  protected SSHConnectionParameter getSSHConnectionParameter() {
    return (SSHConnectionParameter) instanceVar.getConnectionParameter();
  }
  
  public void connect()  {
    try {
      jsch = initJSch();
      transientConnectionData.setSession(createSession(getSSHConnectionParameter()));
      transientDataId = SSHConnectionServiceOperationImpl.registerOpenConnection(transientDataId,transientConnectionData);
    } catch (JSchException e) {
      // TODO catch com.jcraft.jsch.JSchException: reject HostKey: x.x.x.x
      throw new RuntimeException(e);
    }
  }
  
  private Session createSession(SSHConnectionParameter conParams) throws JSchException {
    Proxy proxy = null;
    ProxyParameter proxyParam = conParams.getProxy();
    if( proxyParam != null ) {
      if( proxyParam instanceof SSHProxyParameter ) {
        SSHProxyParameter sshProxyParam = (SSHProxyParameter)proxyParam;
        Session proxySession = createSession(sshProxyParam.getSSHConnectionParameter());
        proxy = new SSHProxy(proxySession);
      } else {
        logger.warn("Ignoring unexpected ProxyParameter "+ proxyParam );
      }
    }
    Session session = createSession(conParams, proxy );
    return session;
  }  
  

  private Session createSession(SSHConnectionParameter conParams, Proxy proxy) throws JSchException {
    int port = 22;
    if (conParams.getPort() != null) {
      port = conParams.getPort();
    }
    final int connectionTimeout;
    if (conParams.getConnectionTimeoutInMilliseconds() > 0) {
      connectionTimeout = (int)Math.min(conParams.getConnectionTimeoutInMilliseconds(), Integer.MAX_VALUE);
    } else {
      connectionTimeout = -1;
    }
    
    Session s = jsch.getSession(conParams.getUserName(), conParams.getHost(), port);
    s.setSocketFactory(new SocketFactory() {

      @Override
      public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        if (connectionTimeout <= 0) { //nicht gesetzt oder unendlich (0)
          Socket s = new Socket(host, port);
          s.setKeepAlive(true);
          return s;
        } else {
          Socket s = new Socket();
          try {
            s.setKeepAlive(true);
            s.connect(new InetSocketAddress(host, port), connectionTimeout);
          } catch (SocketTimeoutException e) {
            if (legacyErrorMessage.get()) {
              //abwärtskompatiblere fehlermeldung erzeugen. jsch erzeugt die fehlermeldung mittels Util.createSocket und eigenem thread...
              throw (SocketTimeoutException)(new SocketTimeoutException("timeout: socket is not established").initCause(e));
            } else {
              throw e;
            }
          }
          return s;
        }
      }

      @Override
      public InputStream getInputStream(Socket arg0) throws IOException {
        return arg0.getInputStream();
      }

      @Override
      public OutputStream getOutputStream(Socket arg0) throws IOException {
        return arg0.getOutputStream();
      }
      
    });
    if( proxy != null ) {
      s.setProxy(proxy);
    }
    
    PassphraseRetrievingUserInfo userInfo = new PassphraseRetrievingUserInfo(new SecureStorablePassphraseStore(), adapter);
    s.setUserInfo(userInfo);
    HostKeyCheckingMode checkingMode = HostKeyCheckingMode.getByXynaRepresentation(conParams.getHostKeyChecking());
    s.setConfig("StrictHostKeyChecking", checkingMode.getStringRepresentation());
    if (conParams.getPassword() != null && conParams.getPassword().length() > 0) {
      s.setPassword(conParams.getPassword());
      userInfo.setPassword(conParams.getPassword());
    }
    if (conParams.getAuthenticationModes() != null && conParams.getAuthenticationModes().size() > 0) { 
      List<AuthenticationMethod> methods = AuthenticationMethod.getByXynaRepresentation(conParams.getAuthenticationModes());
      StringBuilder builder = new StringBuilder();
      for (AuthenticationMethod auth : methods) {
        for (String identifier : auth.getIdentifiers()) {
          builder.append(identifier)
                 .append(",");
        }
      }
      String authMethods = builder.toString();
      authMethods = authMethods.substring(0, authMethods.length() - 1);
      s.setConfig("PreferredAuthentications", authMethods);
    }
    if (conParams.getHostKeyAlias() != null && conParams.getHostKeyAlias().length() > 0) {
      s.setHostKeyAlias(conParams.getHostKeyAlias());
    }
    
    try {
      EncryptionType type = evaluateHost(conParams);
      if (type != null) {
        String currentConfig = s.getConfig(CONFIG_KEY_SERVER_HOST_KEY);
        logger.debug("currentConfig: " + currentConfig);
        String filteredConfig = filterConfig(currentConfig, type);
        logger.debug("filteredConfig: " + filteredConfig);
        if (filteredConfig != null &&
            filteredConfig.length() > 0) {
          s.setConfig(CONFIG_KEY_SERVER_HOST_KEY, filteredConfig);
        }
        if (jsch.getIdentityRepository() instanceof XynaIdentityRepository) {
          XynaIdentityRepository repo = (XynaIdentityRepository) jsch.getIdentityRepository();
          ReorderingIdentityStorableRepository risr = new ReorderingIdentityStorableRepository(repo, type);
          jsch.setIdentityRepository(risr);
        }
      }
    } catch (RuntimeException e) {
      logger.warn("Failed to adjust keyExchange for hostKey", e);
    }
    
    if (connectionTimeout != -1) {
      s.connect(connectionTimeout);
    } else {
      s.connect();
    }
    return s;
  }
  
  
  private final Pattern DSA_FILTER = Pattern.compile("[dD][sS][aAsS]");
  private final Pattern RSA_FILTER = Pattern.compile("[rR][sS][aA]");
  
  public String filterConfig(String currentConfig, EncryptionType typeToRemain) {
    logger.debug("currentConfig: " + currentConfig + "   filtering to: " + typeToRemain.getStringRepresentation());
    Pattern filterPattern;
    switch (typeToRemain) {
      case DSA :
        filterPattern = DSA_FILTER;
        break;
      case RSA :
        filterPattern = RSA_FILTER;
        break;
      default :
        return null;
    }
    ArrayList<String> filtered = new ArrayList<String>();
    String[] split = currentConfig.split(",");
    for (String splitEntry : split) {
      Matcher matcher = filterPattern.matcher(splitEntry);
      if (matcher.find()) {
        filtered.add(splitEntry);
      }
    }
    return StringUtils.joinStringArray(filtered.toArray(new String[0]), ",");
  }
  
  
  private EncryptionType evaluateHost(SSHConnectionParameter conParams) {
    Set<EncryptionType> types = new HashSet<EncryptionType>();
    HostKeyRepository hkr = jsch.getHostKeyRepository();
    if (hkr instanceof HostKeyStorableRepository) {
      HostKey[] keys = ((HostKeyStorableRepository) hkr).getHostKey(determineHostIdentifier(conParams));
      for (HostKey hk : keys) {
        types.add(EncryptionType.getBySshStringRepresentation(hk.getType()));
      }
    } else {
      Collection<String> identifiers = determinePossibleHostIdentifiers(conParams);
      HostKey[] keys = hkr.getHostKey();
      for (HostKey hk : keys) {
        for (String identifier : identifiers) {
          if (hk.getHost().equals(identifier)) {
            types.add(EncryptionType.getBySshStringRepresentation(hk.getType()));
          }
        }
      }
    }
    logger.debug("evaluated hosttypes: " + String.valueOf(types));
    if (types.size() == 1) {
      return types.iterator().next();
    } else {
      return null;
    }
  }
  

  private Collection<String> determinePossibleHostIdentifiers(SSHConnectionParameter conParams) {
    List<String> identifiers = new ArrayList<String>();
    if (conParams.getHostKeyAlias() != null) {
      identifiers.add(conParams.getHostKeyAlias());
    } else {
      identifiers.add(conParams.getHost());
      if (conParams.getPort() != null && 
          !conParams.getPort().equals(22)) {
        identifiers.add("["+conParams.getHost()+"]:"+conParams.getPort());
      }
    }
    logger.debug("possibleHostIdentifiers: " + identifiers);
    return identifiers;
  }
  
  
  private String determineHostIdentifier(SSHConnectionParameter conParams) {
    if (conParams.getHostKeyAlias() != null) {
      return conParams.getHostKeyAlias();
    } else {
      return conParams.getHost();
    }
  }


  public void disconnect()  {
    SSHConnectionServiceOperationImpl.removeTransientData(transientDataId);
    transientConnectionData.disconnect();
  }
  
  private void handleEventSource() {
    ServiceStepEventSource eventSource = ServiceStepEventHandling.getEventSource();
    if (eventSource != null) {
      try {
        eventSource.listenOnAbortEvents(this);
      } catch (XPRC_TTLExpirationBeforeHandlerRegistration e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  private void prepareChannel(SendParameter sendParameter, DocumentType documentType, DeviceType deviceType, Command command) throws ConnectionAlreadyClosed, ReadTimeout {
    if (!prepared) {
      reconnectIfNecessary();
      try {
        initChannelAndStreams(sendParameter, documentType, deviceType, command);
      } catch (RuntimeException e) {
        try {
          transientConnectionData.disconnect();
        } catch (Throwable t) {
          logger.debug("Error while trying to disconnect on failed init", t);
        } finally {
          jsch = null;
        }
        if (e.getCause() instanceof ReadTimeout) {
          throw (ReadTimeout) e.getCause();
        } else {
          throw e;
        }
      }
      prepared = true;
    } else if ( transientConnectionData.isChannelNullOrClosed() ) {
      throw new ConnectionAlreadyClosed(command);
    } else {
      //Channel ist verwendbar: erste Verwendung direkt nach connect oder Suspend direkt nach connect vor send
    }
   
  }
 
  protected boolean getThrowReadTimeoutException(Boolean parameter) {
    if (parameter == null) {
      return timeoutexceptionDefault.get();
    }
    return parameter;
  }
  
  //sendAndReceive!
  public CommandResponseTuple send(Command command, DocumentType documentType, DeviceType deviceType, SendParameter sendParameter) throws ConnectionAlreadyClosed, ReadTimeout {
    adjustReconnectAfterRestartSetting(sendParameter);
    try {
      handleEventSource();
      prepareChannel(sendParameter, documentType, deviceType, new Command(""));
      
      byte[] enrichedCommand = transformCommandForSend(command.getContent(), deviceType);
      //Command verschicken    
      write(enrichedCommand);
      
      commandSend = true;
      protocolMessageHandler.handleProtocol(this, command.getContent(), "out", commandSend, System.currentTimeMillis());
       
      //Response erhalten
      String response =
          readFromInputStream(getInputStream(), getChannel(), documentType, deviceType,
                              ((SSHSendParameter) sendParameter).getReadTimeoutInMilliseconds(), command,
                              getThrowReadTimeoutException(((SSHSendParameter) sendParameter).getThrowExceptionOnReadTimeout()));
      return new CommandResponseTuple(command, generateResponse(response, deviceType));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  
  private static final XynaPropertyDuration sendPartitionSleepTime =
      new XynaPropertyDuration("xact.ssh.connection.send.partition.sleep", "50", TimeUnit.MILLISECONDS)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "SSH Send Service will wait this time after each n bytes sent (n is configurable in property xact.ssh.connection.send.partition.size or after next linebreak (see property xact.ssh.connection.send.partition.linebreak).");
  private static final XynaPropertyInt sendPartitionSize = new XynaPropertyInt("xact.ssh.connection.send.partition.size", 1024)
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "SSH Send Service will wait a short time after each n bytes sent. n is configured with this property. The time to wait is configurable in property xact.ssh.connection.send.partition.sleep.");
  private static final XynaPropertyBoolean sendPartitionByLineBreak = new XynaPropertyBoolean("xact.ssh.connection.send.partition.linebreak", true);
  private static final byte LINEBREAK_BYTE = "\n".getBytes()[0];


  protected void write(byte[] bytes) throws IOException {
    accumulatedResponse = new StringBuilder();
    int offset = 0;
    long millis = sendPartitionSleepTime.getMillis();
    int size = sendPartitionSize.get();
    boolean byLineBreak = sendPartitionByLineBreak.get();
    while (offset < bytes.length) {
      if (offset > 0) {
        //TODO das ist eigtl ein Workaround für Bug 21939. Schöner wäre es, wenn wir das Buffering besser verstehen und konfigurieren würden
        readNewInput(getInputStream(), accumulatedResponse);
        if (millis > 0) {
          try {
            Thread.sleep(millis);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          readNewInput(getInputStream(), accumulatedResponse);
        }
      }
      int len;
      if (byLineBreak) {
        int pos = offset;
        while (pos < bytes.length && bytes[pos] != LINEBREAK_BYTE) {
          pos++;
        }
        pos++; //linebreak auch schicken
        len = Math.min(bytes.length - offset, pos - offset);
      } else {
        len = Math.min(bytes.length - offset, size);
      }
      getOutputStream().write(bytes, offset, len);
      offset += len;
      getOutputStream().flush();
    }
  }


  public Response receive(DocumentType documentType, DeviceType deviceType, SendParameter sendParameter) throws ConnectionAlreadyClosed, ReadTimeout {
    adjustReconnectAfterRestartSetting(sendParameter);
    try {
      handleEventSource();
      prepareChannel(sendParameter, documentType, deviceType, new Command());

      //Response erhalten
      String response =
          readFromInputStream(getInputStream(), getChannel(), documentType, deviceType,
                              ((SSHSendParameter) sendParameter).getReadTimeoutInMilliseconds(), new Command(""),
                              getThrowReadTimeoutException(((SSHSendParameter) sendParameter).getThrowExceptionOnReadTimeout()));
      return generateResponse(response, deviceType);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  protected Response generateResponse(String response, DeviceType deviceType) {
    return new Response(response);
  }
  
  
  protected byte[] transformCommandForSend(String commandString, DeviceType deviceType) throws UnsupportedEncodingException {
    return commandString.getBytes(Constants.DEFAULT_ENCODING);
  }
  
  
  protected abstract void initChannelAndStreams(SendParameter sendParameter, DocumentType documentType, DeviceType deviceType, Command cmd);
  

  
  
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
    TransientConnectionData data = SSHConnectionServiceOperationImpl.getTransientData(transientDataId);
    if (data == null) {
      if (reconnectAfterRestart) {
        prepared = false;
      }
      transientConnectionData = new TransientConnectionData();
      transientDataId = -1;
    } else {
      transientConnectionData = data;
    }
  }
  
  
  protected void reconnectIfNecessary() {
    if (jsch == null) {
      jsch = initJSch();
    }
    if (getSession() == null) {
      connect();
    }
  }
  
  protected JSch initJSch() {
    adapter = new LogAdapter(logger);
    JSch.setLogger(adapter);
    jsch  = new JSch();
    XynaHostKeyRepository hostRepo = new HostKeyStorableRepository(supportedFeatures.get());
    jsch.setHostKeyRepository(hostRepo);
    XynaIdentityRepository idRepo = new IdentityStorableRepository();
    jsch.setIdentityRepository(idRepo);
    return jsch;
  }
  
  protected String readFromInputStream(InputStream input, Channel channel, DocumentType documentType, DeviceType deviceType, long timeoutInMillis, Command cmd) throws UnsupportedEncodingException, IOException, ReadTimeout {
    return readFromInputStream(input, channel, documentType, deviceType, timeoutInMillis, cmd, true);
  }
  
  protected String readFromInputStream(InputStream input, Channel channel, DocumentType documentType, DeviceType deviceType, long timeoutInMillis, Command cmd, boolean throwTimeoutException) throws UnsupportedEncodingException, IOException, ReadTimeout {
    long timeout = System.currentTimeMillis() + timeoutInMillis;
    StringBuilder responseBuilder = new StringBuilder();
    boolean checkOnce = false;
    if (accumulatedResponse != null) {
      responseBuilder.append(accumulatedResponse);
      checkOnce = accumulatedResponse.length() > 0;
      accumulatedResponse = null;
    }
    long responseReceiveTime = System.currentTimeMillis();
    try {
      SleepCounter sleep = sleepTemplate.clone();
      while(true){
        int oldLength = responseBuilder.length();
        boolean newInput = readNewInput( input, responseBuilder);
        if (newInput || checkOnce) {
          int newLength = responseBuilder.length();
          checkOnce = false;
          sleep.reset();
          timeout = System.currentTimeMillis() + timeoutInMillis;
          String partialResponse = responseBuilder.toString();
          
          int numberOfNewCharacters = newLength - oldLength;
          responseReceiveTime = System.currentTimeMillis();
          
          int substringLength = 0;
          if(substringLengthProperty.get() > 0)
              substringLength = Math.max(substringLengthProperty.get(), numberOfNewCharacters);
          int beginIndex = substringLength > 0 ? partialResponse.length() - substringLength : 0;
          beginIndex = Math.max(beginIndex, 0); //make sure beginIndex is positive
          if (documentType.isResponseComplete(partialResponse) ||
              deviceType.isResponseComplete(partialResponse.substring(beginIndex), documentType, instanceVar, cmd)) {
            break;
          }
        } else if (timeoutInMillis > 0 && System.currentTimeMillis() > timeout) {
          if (throwTimeoutException) {
            throw new ReadTimeout(timeoutInMillis, null);
          } else {
            break;
          }
        }
        if (channel.isClosed()) {
          break;
        }
        if (isCanceled) {
          throw new RuntimeException("Send has been cancelled with cause: " + cause.toString());
        }
        try { sleep.sleep(); } catch(Exception ee){ } //interruption soll über cancel geschehen
      }
      return responseBuilder.toString();
    } finally {
      updateStreamHolder(input);
      try {
        protocolMessageHandler.handleProtocol(this, responseBuilder.toString(), "in", commandSend, responseReceiveTime);
      } catch (Throwable t) {
        logger.warn("Error while writing message to messageStore",t);
      }
    }
  }
  
  private boolean readNewInput(InputStream input, StringBuilder responseBuilder) throws UnsupportedEncodingException, IOException {
    int bufferSize = 1024;
    byte[] tmp=new byte[bufferSize];
    boolean newInput = false;
    while (input.available() > 0){
      int i = input.read(tmp, 0, bufferSize);
      if (i < 0) {break;}
      responseBuilder.append(new String(tmp, 0, i, Constants.DEFAULT_ENCODING));
      newInput = true;
    }
    return newInput;
  }

  protected abstract ProtocolMessage createPartialProtocolMessage(String content);
  
  
  private void adjustReconnectAfterRestartSetting(SendParameter sendParameter) {
    if (sendParameter != null && sendParameter instanceof SSHSendParameter) {
      reconnectAfterRestart = ((SSHSendParameter)sendParameter).getReconnectAfterRestart();
    }
  }
  
  
  public void handleServiceStepEvent(AbortServiceStepEvent event) {
    logger.debug("SSHConnectionInstanceOperationImpl handleServiceStepEvent " + event);
    cause = event.getAbortionReason();
    isCanceled = true;
  }
  
  protected Session getSession() {
    return transientConnectionData.getSession();
  }
  
  protected OutputStream getOutputStream() {
    return transientConnectionData.getOutputStream();
  }
  
  protected InputStream getInputStream() {
    return transientConnectionData.getInputStream();
  }
 
  protected void setChannelAndStreams(Channel channel) throws IOException {
    transientConnectionData.setChannelAndStreams(channel);
  }

  protected Channel getChannel() {
    return transientConnectionData.getChannel();
  }  
  
  private static void updateStreamHolder(InputStream input) {
    // workaround for jsch bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=359184
    // if previous receiver thread died and the pipedStream receives anything
    // prior to a new receiver an 'Read end dead'-Exception would be thrown
    // we therefore update the readSide to an immortal thread after receiving
    if (input instanceof PipedInputStream) {
      try {
        Field readSideField = PipedInputStream.class.getDeclaredField("readSide");
        readSideField.setAccessible(true);
        readSideField.set(input, pipedStreamHolder);
      } catch (NoSuchFieldException e) {
        // no workaround if field is not found
        logger.debug("readSideField update failed", e);
      } catch (SecurityException e) {
        // should not happen
        logger.debug("readSideField update failed", e);
      } catch (IllegalArgumentException e) {
        // should not happen
        logger.debug("readSideField update failed", e);
      } catch (IllegalAccessException e) {
        // should not happen
        logger.debug("readSideField update failed", e);
      }
      
    }
  }
  
  
}
