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
package xact.telnet.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.log4j.Logger;

import xact.connection.Command;
import xact.connection.CommandResponseTuple;
import xact.connection.ConnectionAlreadyClosed;
import xact.connection.ConnectionTypeSpecificExtension;
import xact.connection.DeviceType;
import xact.connection.ReadTimeout;
import xact.connection.Response;
import xact.connection.SendParameter;
import xact.telnet.TelnetConnection;
import xact.telnet.TelnetConnectionInstanceOperation;
import xact.telnet.TelnetConnectionParameter;
import xact.telnet.TelnetConnectionSuperProxy;
import xact.telnet.TelnetMessagePayload;
import xact.telnet.TelnetSendParameter;
import xact.telnet.TelnetShellPromptExtractor;
import xact.telnet.TelnetShellResponse;
import xact.telnet.TelnetSpecificExtension;
import xact.templates.DocumentType;
import xfmg.xfmon.protocolmsg.ProtocolMessage;
import xfmg.xfmon.protocolmsg.ProtocolMessageStore;
import xfmg.xfmon.protocolmsg.StoreParameter;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_TTLExpirationBeforeHandlerRegistration;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandler;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.events.AbortServiceStepEvent;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;

public class TelnetConnectionInstanceOperationImpl extends TelnetConnectionSuperProxy 
                                                   implements TelnetConnectionInstanceOperation, ServiceStepEventHandler<AbortServiceStepEvent> {

    private static Logger logger = Logger.getLogger(TelnetConnectionInstanceOperationImpl.class);
    private static final long serialVersionUID = 1L;
    private static final SleepCounter sleepTemplate = new SleepCounter(10, 250, 25);

    private long transientDataId;
    private boolean prepared = false;

    private transient TransientConnectionData transientConnectionData;

    private volatile boolean isCanceled = false;
    private volatile AbortionCause cause;
    private String loginResult;
    private boolean reconnectAfterRestart = true; 
    

  public TelnetConnectionInstanceOperationImpl(TelnetConnection instanceVar) {
    super(instanceVar);
    transientDataId = -1;
    transientConnectionData = new TransientConnectionData();
  }
  
  private static enum CommunicationDirection {
    IN {
      public String getLineSeperatorFromParameter(TelnetSpecificExtension tse) {
        if (tse.getLineSeperatorIn() == null) {
          return tse.getLineSeperator();
        } else {
          return tse.getLineSeperatorIn();
        }
      }
    },
    OUT {
      public String getLineSeperatorFromParameter(TelnetSpecificExtension tse) {
        return tse.getLineSeperator();
      }
    };
    
    public abstract String getLineSeperatorFromParameter(TelnetSpecificExtension tse);
  }


  private TelnetConnectionParameter getTelnetConnectionParameter() {
    return (TelnetConnectionParameter) instanceVar.getConnectionParameter();
  }


  public void connect() {
    TelnetConnectionParameter conParams = getTelnetConnectionParameter();
    int port = 23;
    if (conParams.getPort() != null) {
      port = conParams.getPort();
    }

    TelnetClient telnetSession;
    if (conParams != null &&
        conParams.getTerminalType() != null &&
        !conParams.getTerminalType().isEmpty()) {
      telnetSession = new TelnetClient(conParams.getTerminalType());
    } else {
      telnetSession = new TelnetClient();
    }
    transientConnectionData.setSession(telnetSession);
    
    logger.info("Starting to connect to " + conParams.getHost() + "...");
    try {
      telnetSession.connect(conParams.getHost(), port);
      transientConnectionData.setChannelAndStreams();
    } catch (SocketException e) {
      throw new RuntimeException("SocketException during connect.", e);
    } catch (IOException e) {
      throw new RuntimeException("IOException during connect.", e);
    }

    logger.info("Connected to " + conParams.getHost() + ".");

    transientDataId = TelnetConnectionServiceOperationImpl.registerOpenConnection(transientDataId, transientConnectionData);
  }


  public void disconnect() {
    TelnetConnectionServiceOperationImpl.removeTransientData(transientDataId);
    transientConnectionData.disconnect();
  }


  public CommandResponseTuple send(Command command, DocumentType documentType, DeviceType deviceType,
                                   SendParameter sendParameter) throws ConnectionAlreadyClosed, ReadTimeout {
    adjustReconnectAfterRestartSetting(sendParameter);
    try {
      ServiceStepEventSource eventSource = ServiceStepEventHandling.getEventSource();
      if (eventSource != null) {
        try {
          eventSource.listenOnAbortEvents(this);
        } catch (XPRC_TTLExpirationBeforeHandlerRegistration e) {
          throw new RuntimeException(e);
        }
      }
      
      try {

        if (!prepared) {
          reconnectIfNecessary();
          initChannelAndStreams(sendParameter, documentType, deviceType, command);
          prepared = true;
        } else if (transientConnectionData.isSessionNullOrNotConnected(((TelnetSendParameter) sendParameter)
                        .getConnectionTimeoutInMilliseconds())) {
          throw new ConnectionAlreadyClosed(command);
        } else {
          // connection ist verwendbar: erste Verwendung direkt nach connect oder Suspend direkt nach connect vor send
        }
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      byte[] enrichedCommand = transformCommandForSend(command.getContent(), deviceType);

      logger.info("Sending command: " + command.getContent());

      transientConnectionData.getOutputStream().write(enrichedCommand);
      transientConnectionData.getOutputStream().flush();

      // extract timout for AYT (Are you there - request)
      long aytTimeout = 0;
      if (((TelnetSendParameter) sendParameter).getConnectionTimeoutInMilliseconds() != 0) {
        aytTimeout = ((TelnetSendParameter) sendParameter).getConnectionTimeoutInMilliseconds();
      }

      ProtocolMessage msg = createProtocolMessage(command.getContent(), "out", System.currentTimeMillis());
      storeProtocolMessage(msg);
      
      String response = readFromInputStream(transientConnectionData.getInputStream(), documentType, deviceType,
                                            ((TelnetSendParameter) sendParameter).getReadTimeoutInMilliseconds(),
                                            aytTimeout, command, throwReadTimeoutException(sendParameter));

      logger.info("Got response : " + response);

      return new CommandResponseTuple(command, generateResponse(response, deviceType));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }


  protected Response generateResponse(String response, DeviceType deviceType) {
    response = response.replaceAll(getLineSeperator(deviceType, CommunicationDirection.IN), Constants.LINE_SEPARATOR);
    List<? extends ConnectionTypeSpecificExtension> extensions = deviceType.getConnectionTypeSpecificExtension();
    for (ConnectionTypeSpecificExtension conTypeSpecificExtension : extensions) {
      if (conTypeSpecificExtension instanceof TelnetShellPromptExtractor) {
        TelnetShellPromptExtractor promptExtractor = (TelnetShellPromptExtractor) conTypeSpecificExtension;

        String prompt = promptExtractor.extractPrompt(response);

        if (logger.isDebugEnabled()) {
          logger.debug("Extracted prompt: " + prompt);
          logger.debug("Extracted content: " + response);
        }

        return new TelnetShellResponse(response, prompt);
      }
    }
    return new Response(response);
  }


  
  private String getLineSeperator(DeviceType deviceType, CommunicationDirection direction) {
    String lineSeperator = "\n";
    if (deviceType.getConnectionTypeSpecificExtension() != null) {
      for (ConnectionTypeSpecificExtension extension : deviceType.getConnectionTypeSpecificExtension() ) {
        if (extension instanceof TelnetSpecificExtension) {
          String value = direction.getLineSeperatorFromParameter((TelnetSpecificExtension) extension);
          if (value != null) {
            if (value.equals("\\n")) {
              lineSeperator = "\n";
            } else if (value.equals("\\r")) {
              lineSeperator = "\r";
            } else if (value.equals("\\r\\n")) {
              lineSeperator = "\r\n";
            } else if (value.equals("\\n\\r")) {
              lineSeperator = "\n\r";
            } else if (value == null || value.length() == 0) {
              lineSeperator = ""; //daf�r verwendet, dass man z.b. ctrl-c dr�ckt. das wird nicht mit enter best�tigt.
            }
          }
        }
      }
    }
    return lineSeperator;
  }


  private void initChannelAndStreams(SendParameter sendParameter, DocumentType documentType, DeviceType deviceType,
                                     Command command) throws ConnectionAlreadyClosed {

    // extract connection timeout for ping
    long pingTimeout = 0;
    if (((TelnetSendParameter) sendParameter).getConnectionTimeoutInMilliseconds() != 0) {
      pingTimeout = ((TelnetSendParameter) sendParameter).getConnectionTimeoutInMilliseconds();
    }
    try {
      loginResult = readFromInputStream(transientConnectionData.getInputStream(), documentType, deviceType,
                                        ((TelnetSendParameter) sendParameter).getReadTimeoutInMilliseconds(),
                                        pingTimeout, command, throwReadTimeoutException(sendParameter));
      logger.info("Login result: " + loginResult);

    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ReadTimeout e) {
      throw new RuntimeException(e);
    }
  }
  
  private static final XynaPropertyBoolean timeoutexceptionDefault = new XynaPropertyBoolean("xact.telnet.readtimeout.use.default", true).setDefaultDocumentation(DocumentationLanguage.EN, "If the telnet send parameter for the decision of throwing read timeout exceptions is not set, this default will be used.");
  

  private boolean throwReadTimeoutException(SendParameter sendParameter) {
    Boolean b = ((TelnetSendParameter) sendParameter).getThrowExceptionOnReadTimeout();
    if (b == null) {
      return timeoutexceptionDefault.get();
    }
    return b;
  }


  protected String readFromInputStream(InputStream input, DocumentType documentType, DeviceType deviceType,
                                       long timeoutInMillis, long pingTimeout, Command cmd, boolean throwExceptionOnTimeout)
                  throws UnsupportedEncodingException, IOException, ConnectionAlreadyClosed, ReadTimeout {
    long timeout = System.currentTimeMillis() + timeoutInMillis;
    StringBuilder responseBuilder = new StringBuilder();
    int bufferSize = 1024;
    byte[] tmp = new byte[bufferSize];
    SleepCounter sleep = sleepTemplate.clone();
    long receiveTime = System.currentTimeMillis();
    while (true) {
      boolean newInput = false;
      while (input.available() > 0) {
        int i = input.read(tmp, 0, Math.min(input.available(), bufferSize));
        if (i < 0) {
          break;
        }
        responseBuilder.append(new String(tmp, 0, i, Constants.DEFAULT_ENCODING));
        newInput = true;
      }
      if (newInput) {
        sleep.reset();
        timeout = System.currentTimeMillis() + timeoutInMillis;
        String partialResponse = responseBuilder.toString();
        receiveTime = System.currentTimeMillis();
        if (documentType.isResponseComplete(partialResponse)
                        || deviceType.isResponseComplete(partialResponse, documentType, instanceVar, cmd)) {
          break;
        }
      } else if (timeoutInMillis > 0 && System.currentTimeMillis() > timeout) {
        // ### connection checks
        if (pingTimeout != 0) {
          try {
            // Available-Check der Telnet-Session
            // if (!telnetSession.isAvailable()) {
            // throw new ConnectionAlreadyClosed(cmd);
            // }

            // Ping-aehnliche Verbindungspruefung
            InetAddress neAddress = InetAddress.getByName(getTelnetConnectionParameter().getHost());
            if (!neAddress.isReachable((int) pingTimeout)) {
              throw new ConnectionAlreadyClosed(cmd);
            }
          } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
          }
          if (throwExceptionOnTimeout) {
            throw new ReadTimeout(timeoutInMillis, null);
          }
        }
        // ####
        break;
      }
      if (isCanceled) {
        throw new RuntimeException("Send has been canceled with cause: " + cause.toString());
      }

      try { sleep.sleep(); } catch (Exception ee) { }
    }
    
    String response = responseBuilder.toString();
    
    ProtocolMessage msg = createProtocolMessage(response, "in", receiveTime);
    storeProtocolMessage(msg);
    
    return response;
  }


  private void reconnectIfNecessary() {
    if (transientConnectionData.getSession() == null) {
      connect();
    }
  }


  protected byte[] transformCommandForSend(String commandString, DeviceType deviceType)
                  throws UnsupportedEncodingException {
    String lineSeperator = getLineSeperator(deviceType, CommunicationDirection.OUT);
    if (commandString.endsWith(Constants.LINE_SEPARATOR)) {
      commandString = commandString.substring(0, commandString.length() - Constants.LINE_SEPARATOR.length());
    }
    if (!commandString.endsWith(lineSeperator)) {
      commandString += lineSeperator;
    }
    return commandString.getBytes(Constants.DEFAULT_ENCODING);
  }


  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    // change if needed to store instance context
    s.defaultWriteObject();
  }


  private void adjustReconnectAfterRestartSetting(SendParameter sendParameter) {
    if (sendParameter != null && sendParameter instanceof TelnetSendParameter) {
      reconnectAfterRestart = ((TelnetSendParameter) sendParameter).getReconnectAfterRestart();
    }
  }


  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    // change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
    TransientConnectionData data = TelnetConnectionServiceOperationImpl.getTransientData(transientDataId);
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


  public void handleServiceStepEvent(AbortServiceStepEvent event) {
    logger.debug("SSHConnectionInstanceOperationImpl handleServiceStepEvent " + event);
    cause = event.getAbortionReason();
    isCanceled = true;
  }


  public CommandResponseTuple readLoginResult(DocumentType documentType, DeviceType deviceType,
                                              SendParameter sendParameter) throws ConnectionAlreadyClosed {
    ServiceStepEventSource eventSource = ServiceStepEventHandling.getEventSource();
    if (eventSource != null) {
      try {
        eventSource.listenOnAbortEvents(this);
      } catch (XPRC_TTLExpirationBeforeHandlerRegistration e) {
        throw new RuntimeException(e);
      }
    }

    Command cmd = new Command();
    try {
      if (!prepared) {
        reconnectIfNecessary();
        initChannelAndStreams(sendParameter, documentType, deviceType, new Command());
        prepared = true;
      } else if (transientConnectionData.isSessionNullOrNotConnected(((TelnetSendParameter) sendParameter)
                      .getConnectionTimeoutInMilliseconds())) {
        throw new ConnectionAlreadyClosed(cmd);
      } else {
        // connection ist verwendbar: erste Verwendung direkt nach
        // connect oder Suspend direkt nach connect vor send
      }
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return new CommandResponseTuple(cmd, generateResponse(loginResult, deviceType));
  }
  
  
  private ProtocolMessage createProtocolMessage(String content, String communicationDirection, long time) {
    ProtocolMessage msg = new ProtocolMessage();
    msg.setPayload(new TelnetMessagePayload(content));
    msg.setProtocolAdapterName("TelentConnection");
    msg.setProtocolName("Telnet");
    TelnetConnectionParameter connectionParams = getTelnetConnectionParameter();
    msg.setCommunicationDirection(communicationDirection);
    msg.setConnectionId(String.valueOf(transientDataId));
    msg.setLocalAddress(transientConnectionData.getSession().getLocalAddress() + ":" + transientConnectionData.getSession().getLocalPort());
    msg.setPartnerAddress(connectionParams.getHost() + ":" + (connectionParams.getPort() == null ? "23" : String.valueOf(connectionParams.getPort())));
    msg.setTime(time);
    msg.setMessageType("Communication");
    tryToSetDataFromOrderContext(msg);
    return msg;
  }
  
  
  private void storeProtocolMessage(ProtocolMessage msg) {
    StoreParameter storeParams = new StoreParameter();
    try {
      ProtocolMessageStore.store(msg, storeParams);
    } catch (XynaException e) {
      logger.debug("Failed to store protocol message",e);
    }
  }
  
  
  private void tryToSetDataFromOrderContext(ProtocolMessage partialMessage) {
    try {
      OrderContext ctx = XynaProcessing.getOrderContext();
      partialMessage.setOriginId(String.valueOf(ctx.getOrderId()));
      partialMessage.setRootOrderId(ctx.getRootOrderContext().getOrderId());
    } catch (IllegalStateException e) {
      logger.debug("Could not retrieve order ids for protocol message",e);
    }
  }
  

}
