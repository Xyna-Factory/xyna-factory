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
package xact.ssh.impl;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.SocketFactory;

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
import xact.ssh.HostKeyAliasMapping;
import xact.ssh.HostKeyCheckingMode;
import xact.ssh.HostKeyStorableRepository;
import xact.ssh.IdentityStorableRepository;
import xact.ssh.ProxyParameter;
import xact.ssh.SSHConnection;
import xact.ssh.SSHConnectionInstanceOperation;
import xact.ssh.SSHConnectionParameter;
import xact.ssh.SSHConnectionSuperProxy;
import xact.ssh.SSHProxyParameter;
import xact.ssh.SSHSendParameter;
import xact.ssh.SupportedHostNameFeature;
import xact.ssh.FactoryUtils;
import xact.ssh.Utils;
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

import net.schmizz.sshj.Config;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Factory.Named;
import net.schmizz.sshj.common.SSHException;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.Channel;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.cipher.Cipher;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPassword;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;

import net.schmizz.sshj.transport.mac.MAC;

import com.hierynomus.sshj.key.KeyAlgorithm;
import com.hierynomus.sshj.transport.mac.Macs;


import java.security.*;



public abstract class SSHConnectionInstanceOperationImpl extends SSHConnectionSuperProxy
    implements
      SSHConnectionInstanceOperation,
      ServiceStepEventHandler<AbortServiceStepEvent> {

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

  protected transient SSHClient client;
  private transient TransientConnectionData transientConnectionData;
  private transient boolean commandSend = false;

  protected boolean prepared = false;

  protected long transientDataId;
  private volatile boolean isCanceled = false;
  private volatile AbortionCause cause;
  private boolean reconnectAfterRestart = true;
  protected ProtocolMessageHandler protocolMessageHandler;
  private StringBuilder accumulatedResponse;

  private boolean setAuthNone;

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


  public void connect() {

    initClient();

    try {
      transientConnectionData.setSession(createSession(getSSHConnectionParameter()));
      transientConnectionData.setTransport(client.getTransport());
    } catch (xact.connection.SSHException sshE) {
        logger.trace("Error (SSHException) in Connect",sshE);
        throw new RuntimeException(sshE);
    }
    
    transientDataId = SSHConnectionServiceOperationImpl.registerOpenConnection(transientDataId, transientConnectionData);
  }


  private Session createSession(SSHConnectionParameter conParams) throws xact.connection.SSHException {
    Optional<Proxy> proxy = Optional.empty();
    ProxyParameter proxyParam = conParams.getProxy();
    if (proxyParam != null) {
      if (proxyParam instanceof SSHProxyParameter) {
        logger.warn("Proxy parameter currently ignored");
      } else {
        logger.warn("Ignoring unexpected ProxyParameter " + proxyParam);
      }
    }
    Session session = createSession(conParams, proxy);
    return session;
  }


  protected Session createSession(SSHConnectionParameter conParams, Optional<Proxy> proxy) throws xact.connection.SSHException {
    int port = 22;
    if (conParams.getPort() != null) {
      port = conParams.getPort();
    }

    boolean persist=false;
    if (HostKeyCheckingMode.getByXynaRepresentation(conParams.getHostKeyChecking()).equals(HostKeyCheckingMode.ASK)) {
      persist = true;
    }
    HostKeyAliasMapping.injectHostname(conParams.getHost(), conParams.getHostKeyAlias(), persist);

    if ((conParams.getHostKeyAlias() != null) && (!conParams.getHostKeyAlias().isEmpty())) {
      HostKeyStorableRepository tmpHostRepo = new HostKeyStorableRepository(supportedFeatures.get());
      tmpHostRepo.injectHostKey(conParams.getHostKeyAlias());
    } else {
      HostKeyStorableRepository tmpHostRepo = new HostKeyStorableRepository(supportedFeatures.get());
      tmpHostRepo.injectHostKey(conParams.getHost());
    }

    final int connectionTimeout;
    if (conParams.getConnectionTimeoutInMilliseconds() > 0) {
      connectionTimeout = (int) Math.min(conParams.getConnectionTimeoutInMilliseconds(), Integer.MAX_VALUE);
    } else {
      connectionTimeout = -1;
    }

    client.setSocketFactory(new SocketFactory() {

      // client uses only socketFactory.createSocket()
      @Override
      public Socket createSocket() throws IOException {
        Socket s;
        if (proxy.isPresent()) {
          s = new Socket(proxy.get());
        } else {
          s = new Socket();
        }
        s.setKeepAlive(true);
        s.setTcpNoDelay(true);
        return s;
      }


      // Client uses only socketFactory.createSocket(), method is required but will not have an effect!
      @Override
      public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        Socket s;
        if (proxy.isPresent()) {
          s = new Socket(proxy.get());
        } else {
          s = new Socket();
        }
        connectSocket(s, new InetSocketAddress(host, port));
        return s;
      }


      // Client uses only socketFactory.createSocket(), method is required but will not have an effect!
      @Override
      public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        Socket s;
        if (proxy.isPresent()) {
          s = new Socket(proxy.get());
        } else {
          s = new Socket();
        }
        connectSocket(s, new InetSocketAddress(host, port));
        return s;
      }


      // Client uses only socketFactory.createSocket(), method is required but will not have an effect!
      @Override
      public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket s;
        if (proxy.isPresent()) {
          s = new Socket(proxy.get());
        } else {
          s = new Socket();
        }
        connectSocket(s, new InetSocketAddress(host, port));
        return s;
      }


      // Client uses only socketFactory.createSocket(), method is required but will not have an effect!
      @Override
      public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket s;
        if (proxy.isPresent()) {
          s = new Socket(proxy.get());
        } else {
          s = new Socket();
        }
        connectSocket(s, new InetSocketAddress(address, port));
        return s;
      }


      // Client uses only socketFactory.createSocket(), sub-method will not have an effect!
      private void connectSocket(Socket s, InetSocketAddress host) throws IOException {
        s.setKeepAlive(true);
        s.setTcpNoDelay(true);
        if (connectionTimeout > 0) {
          s.connect(host, connectionTimeout);
        } else {
          s.connect(host);
        }
      }

    });

    try {
      // TODO Default Settings unsupported vs login-server

      Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
 
      // Client uses only socketFactory.createSocket() and overrides with setConnectTimeout and setTimeout
      client.setConnectTimeout(connectionTimeout);

      XynaIdentityRepository idRepo = new IdentityStorableRepository(client.getTransport().getConfig());
      client.connect(conParams.getHost(), port);
      setAuthNone=true;
      authenticate(conParams, idRepo);
      return client.startSession();

    } catch (IOException e) {
      if (e instanceof SSHException) {
        //MarkerImprovedErrorLogging
        logger.trace("Error (SSHException) in CreateSession",e);
        throw Utils.toSshException((SSHException) e);
      } else {
        //MarkerImprovedErrorLogging
        logger.trace("Error in CreateSession",e);
        throw new RuntimeException(e);
      }
    }

  }


  private void authenticate(SSHConnectionParameter conParams, XynaIdentityRepository idRepo) throws SSHException {
    List<AuthenticationMethod> methods = AuthenticationMethod.getByXynaRepresentation(conParams.getAuthenticationModes());
    Collection<AuthMethod> aMethod = methods.stream().flatMap(m -> convertAuthMethod(m, conParams, idRepo).stream()).collect(Collectors.toList());
    client.auth(conParams.getUserName(), aMethod);
  }

  private Collection<AuthMethod> convertAuthMethod(AuthenticationMethod method, SSHConnectionParameter conParams, XynaIdentityRepository idRepo) {
    Collection<AuthMethod> aMethodResponse = new ArrayList<AuthMethod>();
    if (setAuthNone) {
      aMethodResponse.add(new net.schmizz.sshj.userauth.method.AuthNone());
      setAuthNone=false;
    };
    switch (method) {
      case PASSWORD :
        if (conParams.getPassword() == null) {
          conParams.setPassword("");
        } ;
        
        Collection<AuthMethod> addMethodPassword = Collections.singleton(new AuthPassword(new PasswordFinder() {

          public boolean shouldRetry(Resource<?> resource) {
            return false;
          }


          public char[] reqPassword(Resource<?> resource) {
            return conParams.getPassword().toCharArray();
          }
        }));
        
        aMethodResponse.addAll(addMethodPassword);
        return aMethodResponse;
      case HOSTBASED :
        throw new IllegalArgumentException("AuthenticationMethod disabled (security) '" + method.toString() + "'.");
      case PUBLICKEY :
        Collection<KeyProvider> keys = generateKeyProvider(conParams,idRepo);
        Collection<AuthMethod> addMethodKey = keys.stream().map(AuthPublickey::new).collect(Collectors.toList());
        aMethodResponse.addAll(addMethodKey);
        return aMethodResponse;
      default :
        throw new IllegalArgumentException("Unknown AuthenticationMethod '" + method.toString() + "'.");
    }
  }

  private Optional<String> getAlgoType(SSHConnectionParameter conParams) {
    Optional<String> algoTypeOpt = Optional.empty();
    int port = 22;
    if (conParams.getPort() != null) {
      port = conParams.getPort();
    }
    String hostname = conParams.getHost();
    XynaHostKeyRepository hostRepo = new HostKeyStorableRepository(supportedFeatures.get());
    List<String> algoList = hostRepo.findExistingAlgorithms(hostname, port);
    if (algoList.size() > 0) {
      boolean univariate = true;
      String firstElement = algoList.get(0).trim();
      EncryptionType encryFirstElement = EncryptionType.getBySshStringRepresentation(firstElement);
      for (Iterator<String> iter = algoList.iterator(); iter.hasNext(); ) {
        String element = iter.next().trim();
        if (!element.equalsIgnoreCase(firstElement)) {
          univariate = false;
        }
      }
      if (univariate) {
        algoTypeOpt = Optional.ofNullable(encryFirstElement.getStringRepresentation());
      }
    }
    return algoTypeOpt;
  }

  //Preservation of the Connection-App - copy of "generateKeyProvider"
  private Collection<KeyProvider> generateKeyProvider(SSHConnectionParameter conParams, XynaIdentityRepository idRepo) {
    List<KeyProvider> kpl = new ArrayList<KeyProvider>();
    HostKeyCheckingMode checkingMode = HostKeyCheckingMode.getByXynaRepresentation(conParams.getHostKeyChecking());
    if (checkingMode.getStringRepresentation().equalsIgnoreCase("yes") || checkingMode.getStringRepresentation().equalsIgnoreCase("ask")) {
      kpl = idRepo.getKey(null, getAlgoType(conParams));
    } else {
      kpl = idRepo.getKey(null, Optional.empty());
    }
    return kpl;
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


  private Collection<String> determinePossibleHostIdentifiers(SSHConnectionParameter conParams) {
    List<String> identifiers = new ArrayList<String>();
    if (conParams.getHostKeyAlias() != null) {
      identifiers.add(conParams.getHostKeyAlias());
    } else {
      identifiers.add(conParams.getHost());
      if (conParams.getPort() != null && !conParams.getPort().equals(22)) {
        identifiers.add("[" + conParams.getHost() + "]:" + conParams.getPort());
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


  public void disconnect() {
    SSHConnectionServiceOperationImpl.removeTransientData(transientDataId);
    try {
      transientConnectionData.disconnect();
    } catch (TransportException e) {
      throw new RuntimeException(e);
    } catch (ConnectionException e) {
      // Directly close session: Workaround for "net.schmizz.sshj.connection.ConnectionException: Broken transport; encountered EOF."
      // -> see github.com/hierynomus/sshj (If a future release fixes the problem, fix the ConnectionException below)
      logger.debug("Error while trying to close connection", e);
    }
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


  private void prepareChannel(SendParameter sendParameter, DocumentType documentType, DeviceType deviceType, Command command)
      throws ConnectionAlreadyClosed, ReadTimeout {
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
          client = null;
        }
        if (e.getCause() instanceof ReadTimeout) {
          throw (ReadTimeout) e.getCause();
        } else {
          throw e;
        }
      }
      prepared = true;
    } else if (transientConnectionData.isChannelNullOrClosed()) {
      throw new ConnectionAlreadyClosed(command);
    }
  }


  protected boolean getThrowReadTimeoutException(Boolean parameter) {
    if (parameter == null) {
      return timeoutexceptionDefault.get();
    }
    return parameter;
  }


  // sendAndReceive!
  public CommandResponseTuple send(Command command, DocumentType documentType, DeviceType deviceType, SendParameter sendParameter)
      throws ConnectionAlreadyClosed, ReadTimeout {
    adjustReconnectAfterRestartSetting(sendParameter);
    try {
      handleEventSource();
      prepareChannel(sendParameter, documentType, deviceType, new Command(""));

      byte[] enrichedCommand = transformCommandForSend(command.getContent(), deviceType);
      // Command verschicken
      write(enrichedCommand);

      commandSend = true;
      protocolMessageHandler.handleProtocol(this, command.getContent(), "out", commandSend, System.currentTimeMillis());

      // Response erhalten
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
  private static final XynaPropertyBoolean sendPartitionByLineBreak =
      new XynaPropertyBoolean("xact.ssh.connection.send.partition.linebreak", true);
  private static final byte LINEBREAK_BYTE = "\n".getBytes()[0];


  protected void write(byte[] bytes) throws IOException {
    accumulatedResponse = new StringBuilder();
    int offset = 0;
    long millis = sendPartitionSleepTime.getMillis();
    int size = sendPartitionSize.get();
    boolean byLineBreak = sendPartitionByLineBreak.get();
    while (offset < bytes.length) {
      if (offset > 0) {
        // TODO das ist eigtl ein Workaround für Bug 21939. Schöner wäre es, wenn wir
        // das Buffering besser verstehen und konfigurieren würden
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
        pos++; // linebreak auch schicken
        len = Math.min(bytes.length - offset, pos - offset);
      } else {
        len = Math.min(bytes.length - offset, size);
      }
      getOutputStream().write(bytes, offset, len);
      offset += len;
      getOutputStream().flush();
    }
  }


  public Response receive(DocumentType documentType, DeviceType deviceType, SendParameter sendParameter)
      throws ConnectionAlreadyClosed, ReadTimeout {
    adjustReconnectAfterRestartSetting(sendParameter);
    try {
      handleEventSource();
      prepareChannel(sendParameter, documentType, deviceType, new Command());

      // Response erhalten
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
    // change if needed to store instance context
    s.defaultWriteObject();
  }


  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    // change if needed to restore instance-context during deserialization of order
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
    if (client == null) {
      initClient();
    }
    if (getSession() == null) {
      connect();
    }
  }


  protected void initClient() {
    client = new SSHClient();

    HostKeyCheckingMode checkingMode = HostKeyCheckingMode.getByXynaRepresentation(getSSHConnectionParameter().getHostKeyChecking());
    logger.debug("SSHConnectionInstanceOperationImpl checkingMode: " + checkingMode.getStringRepresentation());
    if (checkingMode.getStringRepresentation().equalsIgnoreCase("no")) {
      client.addHostKeyVerifier(new PromiscuousVerifier());
    } else {
      XynaHostKeyRepository hostRepo = new HostKeyStorableRepository(supportedFeatures.get());
      client.addHostKeyVerifier(hostRepo);
    }

    Config config = client.getTransport().getConfig();

    List<Named<KeyAlgorithm>> keyAlgs = createKeyAlgsList(getSSHConnectionParameter().getKeyAlgorithms0());
    config.setKeyAlgorithms(keyAlgs);

    List<Named<MAC>> macs = createMacList(getSSHConnectionParameter().getMessageAuthenticationCodes());
    config.setMACFactories(macs);

    boolean ciphersSet = getSSHConnectionParameter().getCiphers() != null && !getSSHConnectionParameter().getCiphers().isEmpty();
    List<Named<Cipher>> ciphers = ciphersSet ? createCiphers(getSSHConnectionParameter().getCiphers()) : config.getCipherFactories();
    config.setCipherFactories(ciphers);
  }

  private List<Named<Cipher>> createCiphers(List<String> ciphers) {
    List<Named<Cipher>> result = new ArrayList<>();
    for(String cipher: ciphers) {
      var cipherSupplier = Utils.CipherFactories.get(cipher);
      if(cipherSupplier == null) {
        throw new RuntimeException("Unknown cipher: " + cipher);
      }
      result.add(cipherSupplier.get());
    }
    return result;
  }

  private List<Named<KeyAlgorithm>> createKeyAlgsList(List<String> keyAlgorithms) {
    if(keyAlgorithms == null || keyAlgorithms.isEmpty()) {
      return java.util.Arrays.<net.schmizz.sshj.common.Factory.Named<com.hierynomus.sshj.key.KeyAlgorithm>> asList(
                           com.hierynomus.sshj.key.KeyAlgorithms.SSHDSA(),
                           com.hierynomus.sshj.key.KeyAlgorithms.SSHRSA(),
                           com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp521(), //This KeyAlgorithm is necessary
                           com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp256(),
                           com.hierynomus.sshj.key.KeyAlgorithms.RSASHA512(),
                           com.hierynomus.sshj.key.KeyAlgorithms.RSASHA256()
                            );
    }
    List<Named<KeyAlgorithm>> result = new ArrayList<>();
    for(String keyAlg : keyAlgorithms) {
      var algSupplier = FactoryUtils.KeyAlgFactories.get(keyAlg);
      if(algSupplier == null) {
        throw new RuntimeException("Unknown key algorithm " + keyAlg);
      }
      result.add(algSupplier.get());
    }
    return result;
  }
  
  private List<Named<MAC>> createMacList(List<String> macs) {
    if(macs == null || macs.isEmpty()) {
      return java.util.Arrays.<net.schmizz.sshj.common.Factory.Named<MAC>> asList(
                             Macs.HMACSHA2256(),
                             Macs.HMACSHA2256Etm(),
                             Macs.HMACSHA2512(),
                             Macs.HMACSHA2512Etm(),
                             Macs.HMACSHA1(),
                             Macs.HMACSHA1Etm(),
                             Macs.HMACSHA196(),
                             Macs.HMACSHA196Etm(),
                             Macs.HMACMD5(),
                             Macs.HMACMD5Etm(),
                             Macs.HMACMD596(),
                             Macs.HMACMD596Etm(),
                             Macs.HMACRIPEMD160(),
                             Macs.HMACRIPEMD160Etm(),
                             Macs.HMACRIPEMD16096(),
                             Macs.HMACRIPEMD160OpenSsh()
                             );
    }
    List<Named<MAC>> result = new ArrayList<>();
    for(String mac : macs) {
      var macSupplier = FactoryUtils.macFactories.get(mac);
      if(macSupplier == null) {
        throw new RuntimeException("Unknown message authentication code type " + mac);
      }
      result.add(macSupplier.get());
    }
    return result;
  }


  protected String readFromInputStream(InputStream input, Channel channel, DocumentType documentType, DeviceType deviceType,
                                       long timeoutInMillis, Command cmd)
      throws UnsupportedEncodingException, IOException, ReadTimeout {
    return readFromInputStream(input, channel, documentType, deviceType, timeoutInMillis, cmd, true);
  }


  protected String readFromInputStream(InputStream input, Channel channel, DocumentType documentType, DeviceType deviceType,
                                       long timeoutInMillis, Command cmd, boolean throwTimeoutException)
      throws UnsupportedEncodingException, IOException, ReadTimeout {
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
      while (true) {
        int oldLength = responseBuilder.length();
        boolean newInput = readNewInput(input, responseBuilder);
        if (newInput || checkOnce) {
          int newLength = responseBuilder.length();
          checkOnce = false;
          sleep.reset();
          timeout = System.currentTimeMillis() + timeoutInMillis;
          String partialResponse = responseBuilder.toString();

          int numberOfNewCharacters = newLength - oldLength;
          responseReceiveTime = System.currentTimeMillis();

          int substringLength = 0;
          if (substringLengthProperty.get() > 0)
            substringLength = Math.max(substringLengthProperty.get(), numberOfNewCharacters);
          int beginIndex = substringLength > 0 ? partialResponse.length() - substringLength : 0;
          beginIndex = Math.max(beginIndex, 0); // make sure beginIndex is positive
          if (documentType.isResponseComplete(partialResponse)
              || deviceType.isResponseComplete(partialResponse.substring(beginIndex), documentType, instanceVar, cmd)) {
            break;
          }
        } else if (timeoutInMillis > 0 && System.currentTimeMillis() > timeout) {
          if (throwTimeoutException) {
            throw new ReadTimeout(timeoutInMillis, null);
          } else {
            break;
          }
        }
        if (!channel.isOpen()) {
          break;
        }
        if (isCanceled) {
          throw new RuntimeException("Send has been cancelled with cause: " + cause.toString());
        }
        try {
          sleep.sleep();
        } catch (Exception ee) {
        } // interruption soll über cancel geschehen
      }
      return responseBuilder.toString();
    } finally {
      updateStreamHolder(input);
      try {
        protocolMessageHandler.handleProtocol(this, responseBuilder.toString(), "in", commandSend, responseReceiveTime);
      } catch (Throwable t) {
        logger.warn("Error while writing message to messageStore", t);
      }
    }
  }


  private boolean readNewInput(InputStream input, StringBuilder responseBuilder) throws UnsupportedEncodingException, IOException {
    int bufferSize = 1024;
    byte[] tmp = new byte[bufferSize];
    boolean newInput = false;
    while (input.available() > 0) {
      int i = input.read(tmp, 0, bufferSize);
      if (i < 0) {
        break;
      }
      responseBuilder.append(new String(tmp, 0, i, Constants.DEFAULT_ENCODING));
      newInput = true;
    }
    return newInput;
  }


  protected abstract ProtocolMessage createPartialProtocolMessage(String content);


  private void adjustReconnectAfterRestartSetting(SendParameter sendParameter) {
    if (sendParameter != null && sendParameter instanceof SSHSendParameter) {
      reconnectAfterRestart = ((SSHSendParameter) sendParameter).getReconnectAfterRestart();
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


  protected Channel getChannel() {
    return transientConnectionData.getChannel();
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

