/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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



import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.trigger.jmsmq.ActiveMQTRIGGER_QueueConnectionCreationException;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;



public class ActiveMQTrigger extends EventListener<ActiveMQTriggerConnection, ActiveMQStartParameter> {

  private static Logger logger = CentralFactoryLogging.getLogger(ActiveMQTrigger.class);

  private MessageConsumer consumer;
  private Session session;
  private Connection con;
  private volatile boolean running = false;
  private ActiveMQStartParameter startParameter;
  private ActiveMQConnectionFactory cf;
  private String url;


  public ActiveMQTrigger() {
  }


  public void start(ActiveMQStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {
    startParameter = sp;
    if (sp.isUseSsl()) {
      url = "ssl://";
      setSystemProperties(sp);
    }
    else {
      url = "tcp://";
    }
    url += sp.getHostname() + ":" + sp.getPort();
    cf = new ActiveMQConnectionFactory(url);
    openConnection();

  }


  private void setSystemProperties(ActiveMQStartParameter sp) {
    System.setProperty("javax.net.ssl.keyStore", sp.getKeystore());
    System.setProperty("javax.net.ssl.keyStorePassword", sp.getKeystorePassword());
    System.setProperty("javax.net.ssl.trustStore", sp.getTruststore());
    System.setProperty("javax.net.ssl.trustStorePassword", sp.getTruststorePassword());
  }


  private void openConnection() throws XACT_TriggerCouldNotBeStartedException {
    try {
      con = cf.createConnection(); //connection mit zugangsdaten aufmachen => konfigurierbar
      con.start();
      session = con.createSession(false, Session.AUTO_ACKNOWLEDGE); //TODO transacted session konfigurierbar?
      Destination destination = session.createQueue(startParameter.getQueueName());
      consumer = session.createConsumer(destination);
      if (logger.isInfoEnabled()) {
        logger.info("connected to queue " + startParameter.getXynaQueueName() + ", " + startParameter.getQueueName() + " on " + url);
      }
      running = true;
    } catch (JMSException e) {
      throw new ActiveMQTRIGGER_QueueConnectionCreationException(startParameter.getQueueName(), url, e);
    }
  }


  public ActiveMQTriggerConnection receive() {
    while (running) {
      try {
        Message msg = consumer.receive(startParameter.getReceiveTimeout());
        if (msg != null) {
          if (logger.isTraceEnabled()) {
            traceMessage(msg);
          }
          return new ActiveMQTriggerConnection(msg, startParameter.getXynaQueueName(), startParameter.getQueueName());
        }
      } catch (JMSException e) {
        if (running) {
          handleJMSExceptionWhenRunning(e);
        }
      }
    }
    if (logger.isInfoEnabled()) {
      logger.info("closing down connection to " + startParameter.getXynaQueueName() + ", " + startParameter.getQueueName());
    }
    try {
      session.close();
      con.close();
    } catch (JMSException e) {
      logger.error("error closing connection to " + startParameter.getXynaQueueName() + ", "
          + startParameter.getHostname() + ":" + startParameter.getPort() + "/"
          + startParameter.getQueueName(), e);
    }
    return null;
  }


  private void handleJMSExceptionWhenRunning(JMSException e) {
    if (!startParameter.isAutoReconnect()) {
      handleJMSExceptionNoAutoReconnect(e);
    }
    else {
      logger.warn("error trying to receive message from queue " + startParameter.getQueueName()
                      + ". restarting connection in " + startParameter.getReconnectIntervalAfterError()
                      + " milliseconds. errormessages will be shown at debug level", e);
      try {
        session.close();
      }
      catch (JMSException e1) {
        logger.info("session could not be closed", e1);
      }
      try {
        con.close();
      }
      catch (JMSException e1) {
        logger.info("connection could not be closed", e1);
      }

      boolean failedToReconnect = true;
      while (running && failedToReconnect) {
        try {
          Thread.sleep(startParameter.getReconnectIntervalAfterError());
        }
        catch (InterruptedException e2) {
          //do nothing
        }
        try {
          openConnection();
          failedToReconnect = false;
        }
        catch (XynaException e1) {
          logger.debug("could not restart connection to queue " + startParameter.getQueueName()
                                          + " after error.", e1);
        }
      }
    }
  }


  private void handleJMSExceptionNoAutoReconnect(JMSException e) {
    logger.warn("error trying to receive message from queue " + startParameter.getQueueName(), e);
    try {
      session.close();
    }
    catch (Exception e1) {
      logger.debug("session could not be closed", e1);
    }
    try {
      con.close();
    }
    catch (Exception e1) {
      logger.debug("connection could not be closed", e1);
    }
    logger.warn("Closed trigger connection for good.");
    running = false;
  }


  private void traceMessage(Message msg) {
    logger.trace("message = " + msg);
    try {
      logger.trace("correlationId = " + msg.getJMSCorrelationID());
    } catch (JMSException e1) {
      logger.trace("correlationId unknown", e1);
    }
    if (msg instanceof TextMessage) {
      TextMessage tm = (TextMessage) msg;
      try {
        logger.trace("content=" + tm.getText());
      } catch (JMSException e) {
        logger.trace("content unknown", e);
      }
    }
  }


  private void debugMessage(Message msg) {
    logger.debug("message = " + msg);
    try {
      logger.debug("correlationId = " + msg.getJMSCorrelationID());
    } catch (JMSException e1) {
      logger.debug("correlationId unknown", e1);
    }
    if (msg instanceof TextMessage) {
      TextMessage tm = (TextMessage) msg;
      try {
        logger.debug("content=" + tm.getText());
      } catch (JMSException e) {
        logger.debug("content unknown", e);
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
      try {
        consumer.close();
      } catch (JMSException e) {
        logger.error("error closing consumer from " + startParameter.getHostname() + ":" + startParameter.getPort()
                        + "/" + startParameter.getQueueName(), e);
      }
    }
  }


  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter registered to this trigger
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(ActiveMQTriggerConnection con) {
    try {
      logger.warn("no filter found for jms message with correlationid = " + con.getMessage().getJMSCorrelationID());
      if (logger.isDebugEnabled()) {
        debugMessage(con.getMessage());
      }
    } catch (JMSException e) {
      logger.error("no filter found for jms message. correlationid could not be read", e);
    }
  }


  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    return "connection to ActiveMQ";
  }


  @Override
  public void onProcessingRejected(String s, ActiveMQTriggerConnection con) {
    try {
      logger.warn("jms message with correlationid = " + con.getMessage().getJMSCorrelationID() + " was rejected: " + s);
      if (logger.isDebugEnabled()) {
        debugMessage(con.getMessage());
      }
    } catch (JMSException e) {
      logger.error("jms message rejected. correlationid could not be read", e);
    }
  }

}
