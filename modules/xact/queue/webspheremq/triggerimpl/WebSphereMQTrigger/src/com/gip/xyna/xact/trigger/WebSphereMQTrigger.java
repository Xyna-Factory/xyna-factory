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



import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.trigger.exception.WebSphereMQTrigger_CouldNotBeStartedException;
import com.gip.xyna.xact.trigger.ssl.SSLConfig;
import com.gip.xyna.xact.trigger.ssl.SSLTools;
import com.gip.xyna.xact.trigger.ssl.WebSphereMQTriggerSSLConfigBuilder;
import com.gip.xyna.xact.trigger.ssl.WebSphereMQTriggerSSLConfigBuilder.XynaPropertyPrefixForSSLKeystoreConfig;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.WebSphereMQConnectData;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;


public class WebSphereMQTrigger extends EventListener<WebSphereMQTriggerConnection, WebSphereMQStartParameter> {

  private static Logger logger = CentralFactoryLogging.getLogger(WebSphereMQTrigger.class);

  private WebSphereMQStartParameter startParameter;
  private MessageConsumer consumer;
  private Connection connection;
  private volatile boolean running = false;
  private SSLConfig sslConfig;
  private boolean useSSL = false;


  public WebSphereMQTrigger() {
  }


  private LinkedBlockingQueue<Message> internalQueue;


  private static class XynaMessageListener implements MessageListener {

    private final LinkedBlockingQueue<Message> queue;

    public XynaMessageListener(LinkedBlockingQueue<Message> queue) {
      this.queue = queue;
    }

    public void onMessage(Message message) {
      queue.add(message);
    }

  }


  private class XynaExceptionListener implements ExceptionListener {

    public void onException(JMSException e) {
      WebSphereMQTrigger.this.handlePotentialDisconnect(e);
    }

  }


  public void start(WebSphereMQStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {
    this.startParameter = sp;
    if (sp.getUseSSL()) {
        this.useSSL = true;
        XynaPropertyPrefixForSSLKeystoreConfig prefix =
                        new XynaPropertyPrefixForSSLKeystoreConfig(sp.getXynaPropertiesForSSLPrefix());
        this.sslConfig = new WebSphereMQTriggerSSLConfigBuilder(prefix).build();
    }
    openConnection();
  }


  public WebSphereMQTriggerConnection receive() {
    while (running) {
      Message msg;
      if (startParameter.receiveAsynchronously()) {
        try {
          msg = internalQueue.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          continue;
        }
      } else {
        try {
          msg = consumer.receive(startParameter.getReceiveTimeout());
        } catch (JMSException e) {
          handlePotentialDisconnect(e);
          continue;
        }
      }
      if (msg != null) {
        if (logger.isTraceEnabled()) {
          traceMessage(msg);
        }
        return new WebSphereMQTriggerConnection(msg, startParameter.getXynaQueueMgmtQueueName());
      }
    }
    return null;
  }


  private void handlePotentialDisconnect(JMSException e) {
    if (running) {
      if (!startParameter.isAutoReconnect()) {
        running = false;
        logJMSException(Level.WARN, e, "Caught exception, closing trigger connection for good.");
        try {
          connection.close();
        }
        catch (JMSException e1) {
          logJMSException(Level.INFO, e1, "connection could not be closed");
        }
      }
      else {

        String errorMsg =
            "error trying to receive message from queue " + startParameter.getQueueName()
                + ". restarting connection in " + startParameter.getReconnectIntervalAfterError()
                + " milliseconds. errormessages will be shown at debug level";
        logJMSException(Level.WARN, e, errorMsg);
        try {
          connection.close();
        }
        catch (JMSException e1) {
          logJMSException(Level.INFO, e1, "connection could not be closed");
        }

        boolean failedToReconnect = true;
        while (running && failedToReconnect) {
          try {
            Thread.sleep(startParameter.getReconnectIntervalAfterError());
          }
          catch (InterruptedException e2) {
          }
          try {
            openConnection();
            failedToReconnect = false;
          }
          catch (XynaException e1) {
            logger.error("could not restart connection to queue " + startParameter.getQueueName() + " after error.", e1);
          }
        }
      }
    }

  }


  /**
   * called by Xyna Processing to stop the Trigger. should make sure, that start() may be called again directly
   * afterwards. connection instances returned by the method receive() should not be expected to work after stop() has
   * been called.
   */
  public void stop() {
    if (running) {
      running = false;
      if (logger.isInfoEnabled()) {
        logger.info("closing down connection to " + startParameter.getQueueName());
      }
      try {
        consumer.close();
      } catch (JMSException e) {
        logJMSException(Level.ERROR, e, "error closing consumer on " + startParameter.getQueueName());
      }
      try {
        connection.stop();
      } catch (JMSException e) {
        logJMSException(Level.ERROR, e, "error stopping connection from " + startParameter.getHostname() + ":" + startParameter.getPort()
                        + "/" + startParameter.getQueueName());
      }
      try {
        connection.close();
      } catch (JMSException e) {
        logJMSException(Level.ERROR, e, "error closing connection from " + startParameter.getHostname() + ":" + startParameter.getPort()
                        + "/" + startParameter.getQueueName());
      }
    }
  }


  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter registered to this trigger
   *
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(WebSphereMQTriggerConnection con) {
    try {
      Message message = con.getMessage();
      logger.warn("no filter found for jms message with correlationid = " + message.getJMSCorrelationID());
      if (logger.isDebugEnabled()) {
        debugMessage(message);
      }
      sendMessageToErrorQueue(message);
    }
    catch (JMSException e) {
      logJMSException(Level.ERROR, e, "no filter found for jms message. correlationid could not be read");
    }
  }

  private static void logJMSException(Level loglevel, JMSException e, String text) {
    if (e != null) {
      logger.log(loglevel, text, e);
      if (e.getLinkedException() != null) {
        logger.log(loglevel, "linked exception: ", e.getLinkedException());
      }
    } else {
      logger.log(loglevel, text);
    }
  }


  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    return "connection to Websphere MQ";
  }


  @Override
  protected void onProcessingRejected(String cause,
                                      WebSphereMQTriggerConnection triggerConnection) {
    try {
      Message message = triggerConnection.getMessage();
      logger.warn("jms message with correlationid = " + message.getJMSCorrelationID() + " was rejected: " + cause);
      if (logger.isDebugEnabled()) {
        debugMessage(message);
      }
      sendMessageToErrorQueue(message);
    }
    catch (JMSException e) {
      logJMSException(Level.ERROR, e, "jms message rejected. correlationid could not be read. cause = " + cause);
    }
  }


  private void openConnection() throws XACT_TriggerCouldNotBeStartedException {
    try {
      MQConnectionFactory factory = new MQConnectionFactory();
      factory.setTransportType(WMQConstants.WMQ_CM_DIRECT_TCPIP);
      factory.setHostName(startParameter.getHostname());
      factory.setPort(startParameter.getPort());
      factory.setQueueManager(startParameter.getQueueManager());
      factory.setChannel(startParameter.getChannel());

      if (this.useSSL) {
        SSLTools.adjustForSSL(factory, this.sslConfig);
      }

      connection = factory.createConnection(startParameter.getUserName(), startParameter.getPassword());
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue("queue:///" + startParameter.getQueueName());
      consumer = session.createConsumer(queue);
      connection.start(); // Note: this must only be done when receiving - not when sending.
      if (startParameter.receiveAsynchronously()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Registering " + MessageListener.class.getSimpleName() + " to receive messages asynchronously.");
        }
        connection.setExceptionListener(new XynaExceptionListener());
        internalQueue = new LinkedBlockingQueue<Message>();
        consumer.setMessageListener(new XynaMessageListener(internalQueue));
      }
      running = true;
    } catch (JMSException e) {
      if (connection != null) {
        try {
          // Note: there is no need to close the sessions, producers, and consumers of a closed connection.
          connection.close();
        } catch (JMSException ex) {
          logJMSException(Level.ERROR, ex, "Failed to close connection.");
        }
      }

      WebSphereMQTrigger_CouldNotBeStartedException rethrow =
          new WebSphereMQTrigger_CouldNotBeStartedException("Failed to create connection " + startParameter.getQueueName());
      if (e.getLinkedException() != null) {
        throw (WebSphereMQTrigger_CouldNotBeStartedException) rethrow.initCauses(new Throwable[] {e, e.getLinkedException()});
      } else {
        throw rethrow.initCause(e);
      }
    }
  }


  private void traceMessage(Message msg) {
    logger.trace("message = " + msg);
    try {
      logger.trace("correlationId = " + msg.getJMSCorrelationID());
    }
    catch (JMSException e1) {
      logJMSException(Level.TRACE, e1, "correlationId unknown");
    }
    if (msg instanceof TextMessage) {
      TextMessage tm = (TextMessage) msg;
      try {
        logger.trace("content=" + tm.getText());
      }
      catch (JMSException e) {
        logJMSException(Level.TRACE, e, "content unknown");
      }
    }
  }


  private void debugMessage(Message msg) {
    logger.debug("message = " + msg);
    try {
      logger.debug("correlationId = " + msg.getJMSCorrelationID());
    }
    catch (JMSException e1) {
      logJMSException(Level.DEBUG, e1, "correlationId unknown");
    }
    if (msg instanceof TextMessage) {
      TextMessage tm = (TextMessage) msg;
      try {
        logger.debug("content=" + tm.getText());
      }
      catch (JMSException e) {
        logJMSException(Level.DEBUG, e, "content unknown");
      }
    }
  }


  public static void sendToQueue(com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue queue,
                                 Message message, boolean useSSL, SSLConfig sslConfig, String userName, String password) throws JMSException {
    Connection connection = null;
    try {
      WebSphereMQConnectData connData = (WebSphereMQConnectData) queue.getConnectData();

      MQConnectionFactory factory = new MQConnectionFactory();
      factory.setTransportType(WMQConstants.WMQ_CM_DIRECT_TCPIP);
      factory.setHostName(connData.getHostname());
      factory.setPort(connData.getPort());
      factory.setQueueManager(connData.getQueueManager());
      factory.setChannel(connData.getChannel());

      if (useSSL) {
        SSLTools.adjustForSSL(factory, sslConfig);
      }

      connection = factory.createConnection(userName, password); // empty username and password
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue destination = session.createQueue("queue:///" + queue.getExternalName());
      MessageProducer producer = session.createProducer(destination);

      producer.send(message);
    } finally {
      if (connection != null) {
        try {
          // Note: there is no need to close the sessions, producers, and consumers of a closed connection.
          connection.close();
        }
        catch (JMSException e) {
          logJMSException(Level.ERROR, e, "Failed to close connection.");
        }
      }
    }
  }
  
  public static void sendToQueue(com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue queue,
                                 Message message, boolean useSSL, SSLConfig sslConfig) throws JMSException {
    sendToQueue(queue, message, useSSL, sslConfig, "", "");
  }

  public void sendMessageToErrorQueue(Message message) throws JMSException {
    com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue errorQueue = startParameter.getErrorQueue();
    if (errorQueue != null) {
      try {
        sendToQueue(errorQueue, message, this.useSSL, this.sslConfig, startParameter.getUserName(), startParameter.getPassword());
      }
      catch (JMSException e) {
        logJMSException(Level.ERROR, e, "Error sending to error queue, will try redundant queue instead.");
        sendMessageToErrorQueueRedundant(message);
      }
    }
  }


  private void sendMessageToErrorQueueRedundant(Message message) throws JMSException {
    com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue queue = startParameter.getErrorQueueRedundant();
    if (queue != null) {
      sendToQueue(queue, message, this.useSSL, this.sslConfig, startParameter.getUserName(), startParameter.getPassword());
    }
  }


  public WebSphereMQStartParameter getStartParameter() {
    return startParameter;
  }

}
