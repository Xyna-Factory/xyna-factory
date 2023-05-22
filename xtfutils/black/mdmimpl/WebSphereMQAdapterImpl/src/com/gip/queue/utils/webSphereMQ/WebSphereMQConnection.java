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

package com.gip.queue.utils.webSphereMQ;

import java.io.UnsupportedEncodingException;

import javax.jms.*;

import org.apache.log4j.Logger;

import com.gip.queue.utils.*;
import com.gip.queue.utils.MsgToSend.JmsProperty;
import com.gip.queue.utils.MsgToSend.MsgType;
import com.gip.queue.utils.QueueConnection;
import com.gip.queue.utils.exception.QueueException;
import com.ibm.mq.jms.MQConnectionFactory;


public class WebSphereMQConnection extends QueueConnection {

  private static Logger _logger = Logger.getLogger(WebSphereMQConnection.class);

  private final MQConnectionFactory _factory;

  private final String _queueName;
  private Session _session = null;
  private MessageProducer _producer = null;
  private Connection _connection = null;


  public WebSphereMQConnection(MQConnectionFactory factory, String queuename) {
    _factory = factory;
    _queueName = queuename;
  }


  public void open() throws QueueException {
    try {
      openImpl();
    }
    catch (JMSException e) {
      setOrLogLinkedExceptionCause(e);
      throw new QueueException(e);
    }
  }

  public void close() throws QueueException {
    // Note: there is no need to close the sessions, producers, and consumers of a closed connection.
    try {
      _connection.close();
    }
    catch (JMSException e) {
      setOrLogLinkedExceptionCause(e);
      throw new QueueException(e);
    }
  }


  private void openImpl() throws JMSException {
    _connection = _factory.createConnection("", ""); // empty username and password
    _session = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    Queue queue = _session.createQueue("queue:///" + _queueName);
    _producer = _session.createProducer(queue);
  }


  public void send(MsgToSend input) throws QueueException {
    Message msg = null;
    try {
      if (input.getMessageType() == MsgType.TEXT_MSG) {
        msg = buildTextMsg(input);
      }
      else if (input.getMessageType() == MsgType.BYTES_MSG) {
        msg = buildBytesMsg(input);
      }
      else {
        throw new RuntimeException("Unsupported jms message type: " + input.getMessageType().name());
      }
      if ((input.getCorrId() != null) && (input.getCorrId().trim().length() > 0)) {
        msg.setJMSCorrelationID(input.getCorrId());
        _logger.debug("Setting correlationID to " + input.getCorrId());
      }
      if ((input.getJmsReplyToQueueName() != null) && (input.getJmsReplyToQueueName().trim().length() > 0)) {
        Queue queue = _session.createQueue("queue:///" + input.getJmsReplyToQueueName());
        msg.setJMSReplyTo(queue);
        _logger.debug("Setting jms reply to queue to " + input.getJmsReplyToQueueName());
      }
      for (JmsProperty prop : input.getJmsPropertyList()) {
        _logger.debug("Adding JMS Property to message: Name = " + prop.getKey()
                     + ", value = " + prop.getValue());
        msg.setStringProperty(prop.getKey(), prop.getValue());
      }
      _producer.send(msg);
    } catch (JMSException e) {
      setOrLogLinkedExceptionCause(e);
      throw new QueueException(e);
    } catch (UnsupportedEncodingException e) {
      throw new QueueException(e);
    }
  }


  private void setOrLogLinkedExceptionCause(JMSException e) {
    if (e.getLinkedException() != null) {
      try {
        Throwable t = e;
        if (t.getCause() != null) {
          t = t.getCause();
        }
        t.initCause(e.getLinkedException());
      } catch (Exception f) {
        _logger.warn("error sending to queue:", e.getLinkedException());
      }
    }
  }


  private TextMessage buildTextMsg(MsgToSend input) throws JMSException {
    TextMessage msg = _session.createTextMessage(input.getMessage());
    return msg;
  }

  private BytesMessage buildBytesMsg(MsgToSend input) throws JMSException, UnsupportedEncodingException {
    BytesMessage msg = _session.createBytesMessage();
    msg.writeBytes(input.getMessage().getBytes("UTF-8"));
    return msg;
  }

}
