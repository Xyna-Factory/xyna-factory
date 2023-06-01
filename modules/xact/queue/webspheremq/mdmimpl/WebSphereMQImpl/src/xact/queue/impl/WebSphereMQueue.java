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
package xact.queue.impl;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

import xact.queue.CorrelationId;
import xact.queue.DequeueFailedException;
import xact.queue.DequeueOptions;
import xact.queue.EnqueueFailedException;
import xact.queue.EnqueueOptions;
import xact.queue.MessageProperties;
import xact.queue.NoConnectionException;
import xact.queue.NoSuchMessageException;
import xact.queue.Property;
import xact.queue.QueueMessage;
import xact.queue.QueueName;
import xact.queue.admin.WebSphereMQConfig;
import xact.queue.impl.QueueInstanceOperationImpl.MessageIdentification;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueManagement;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;
import com.gip.xyna.xprc.xsched.xynaobjects.Date;
import com.gip.xyna.xprc.xsched.xynaobjects.Forever;
import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueue;
import com.ibm.mq.jms.MQQueueConnection;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQQueueReceiver;
import com.ibm.mq.jms.MQQueueSession;


/**
 *
 */
public class WebSphereMQueue {

  
  private static Logger logger = CentralFactoryLogging.getLogger(WebSphereMQueue.class);
  
  private WebSphereMQConfig config;
  private MQQueueConnectionFactory connectionFactory;
  private String connectionTarget;
  private MQQueueConnection connection;
  private MQQueueSession session;
  private MQQueue queue;
  private MQQueueReceiver receiver;

  private MessageProducer producer;
  
  public WebSphereMQueue(WebSphereMQConfig config) {
    this.config = config;
  }

  public void connect() throws NoConnectionException {
    connectionTarget =  config.getName_externalQueue()+"@"+config.getHostname()+":"+config.getPort();
    try {
      connectionFactory = new MQQueueConnectionFactory();
      connectionFactory.setHostName(config.getHostname());
      connectionFactory.setPort(config.getPort());
      connectionFactory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP); //TODO 
      connectionFactory.setQueueManager(config.getQueueManager());
      connectionFactory.setChannel(config.getChannel());
      
      
      connection = (MQQueueConnection) connectionFactory.createConnection("","");
      //connection = (MQQueueConnection) connectionFactory.createQueueConnection();
      connection.start();
      
      session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
      queue = (MQQueue) session.createQueue("queue:///"+config.getName_externalQueue() );
    } catch( JMSException e ) {
      throw new NoConnectionException(connectionTarget, e);
    }
  }

  public void close() {
    if( producer != null ) {
      try {
        producer.close();
      } catch (JMSException e) {
        logger.warn("Could not close producer", e);
      }
    }
    if( receiver != null ) {
      try {
        receiver.close();
      } catch (JMSException e) {
        logger.warn("Could not close receiver", e);
      } 
    }
    if( session != null ) {
      try {
        session.close();
      } catch (JMSException e) {
        logger.warn("Could not close session", e);
      }   
    }
    if( connection != null ) {
      try {
        connection.close();
      } catch (JMSException e) {
        logger.warn("Could not close connection", e);
      }  
    }
  }

  public void send(CorrelationId correlationId, QueueMessage queueMessage, 
                   EnqueueOptions enqueueOptions) throws EnqueueFailedException {
    try {
      if( producer == null ) {
        producer = session.createProducer(queue);
      }
      
      //FIXME ByteMessage?
      TextMessage message = session.createTextMessage(queueMessage.getMessage());     
      message.setJMSCorrelationID(correlationId.getCorrelationId());
      
      addProperties( message, queueMessage);
      
      setJMSReplyTo(message, enqueueOptions.getReplyTo() );
      
      //http://www-01.ibm.com/support/knowledgecenter/api/content/SSFKSJ_7.0.1/ 
      //   com.ibm.mq.javadoc.doc/WMQJMSClasses/com/ibm/mq/jms/MQMessageProducer.html
      producer.send(message,
                    DeliveryMode.PERSISTENT,
                    getPriority(queueMessage.getMessageProperties()),
                    getTimeToLive(enqueueOptions) );

    } catch (JMSException e) {
      throw new EnqueueFailedException(e.getMessage(), e);
    }
  }

  private void addProperties(TextMessage message, QueueMessage queueMessage) throws JMSException {
    if( queueMessage.getMessageProperties() == null) {
      return; //nichts zu tun
    }
    if( queueMessage.getMessageProperties().getProperties() == null ) {
      return; //nichts zu tun
    }
    for( Property p : queueMessage.getMessageProperties().getProperties() ) {
      message.setStringProperty(p.getKey(), p.getValue());
    }
  }

  private long getTimeToLive(EnqueueOptions enqueueOptions) {
    //Millisekunden!
    if( enqueueOptions.getExpiration() == null ) {
      return 0L; //ewig
    } else {
      return toRelativeMilliSeconds(enqueueOptions.getExpiration());
    }
  }

  private int getPriority(MessageProperties messageProperties) {
    int prio = 4;
    if( messageProperties != null && messageProperties.getPriority() != null ) {
      prio = messageProperties.getPriority().intValue();
    }
    if( prio < 0 ) {
      prio = 0;
    } else if( prio > 9 ) {
      prio = 9;
    }
    return prio;
  }

  public QueueMessage receive(DequeueOptions dequeueOptions) throws DequeueFailedException, NoSuchMessageException {
    MessageConsumer consumer = null;
    
    MessageIdentification identification = MessageIdentification.valueOf(dequeueOptions.getIdentification());
    
    try {
      String selector = createSelector(identification,dequeueOptions);
      consumer = session.createConsumer(queue, selector);

      Message message = null;
      
      if( dequeueOptions.getWait() == null ) {
        //TODO 
        message = consumer.receiveNoWait();// hat ein Problem: findet meistens nichts. Daher 10 ms warten...
        //message = consumer.receive(10);
      } else if( dequeueOptions.getWait() instanceof Forever ) {
        message = consumer.receive();
      } else {
        long wait = toRelativeMilliSeconds( dequeueOptions.getWait() );
        if( wait <= 0 ) {
          //TODO message = consumer.receiveNoWait(); hat ein Problem: findet meistens nichts. Daher 10 ms warten...
          message = consumer.receive(10);
        } else {
          message = consumer.receive(wait);
        }
      }
      
      if( message == null ) {
        throw new NoSuchMessageException(identification.toMessageIdentificationString(dequeueOptions) );
      }
      
      QueueMessage queueMessage = new QueueMessage();
      if (message instanceof TextMessage) {
        TextMessage textMessage = (TextMessage) message;
        queueMessage.setMessage(textMessage.getText());
      } else {
        queueMessage.setMessage(message.toString());
      }

      MessageProperties messageProperties = new MessageProperties();
      messageProperties.setCorrelationId(new CorrelationId( message.getJMSCorrelationID()));
      messageProperties.setProperties( getProperties(message) );
      queueMessage.setMessageProperties(messageProperties);
      
      return queueMessage;
    } catch (JMSException e) {
      throw new DequeueFailedException(e.getMessage(), e);
    }
  }

  private List<Property> getProperties(Message message) throws JMSException {
    Enumeration<?> enums = message.getPropertyNames();
    if( enums == null || ! enums.hasMoreElements() ) {
      return null;
    }
    List<Property> properties = new ArrayList<Property>();
    while( enums.hasMoreElements() ) {
      String key = String.valueOf(enums.nextElement());
      properties.add( new Property( key, message.getStringProperty(key) ) );
    }
    return properties;
  }
  
  private void setJMSReplyTo(TextMessage message, QueueName replyTo) throws JMSException {
    if( replyTo == null ) {
      return; //nichts zu tun
    }
    com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue replyToQueueData;
    try {
      replyToQueueData = getQueueManagement().getQueue(replyTo.getName());
    } catch (Exception e) {
      logger.warn("Could not set JMSReplyTo: Queue \""+replyTo.getName()+"\" not registered", e);
      return;
    }
    
    Queue replyToQueue = session.createQueue("queue:///"+ replyToQueueData.getExternalName() );
    message.setJMSReplyTo(replyToQueue);
  }
  private QueueManagement getQueueManagement() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getQueueManagement(); 
  }

  private String createSelector(MessageIdentification identification, DequeueOptions dequeueOptions) {
    switch( identification ) {
      case ByCorrelationId:
        return "JMSCorrelationID='"+identification.getIdentification(dequeueOptions)+"'";
      case ByMessageId:
        return "JMSMessageID='"+identification.getIdentification(dequeueOptions)+"'";
      case Next:
        return null;
      default:
        throw new UnsupportedOperationException("Unknown message identification "+identification );
    }
  }
  
  private long toRelativeMilliSeconds(Date date) {
    AbsRelTime absRelTime = date.toAbsRelTime();
    if( absRelTime.isAbsolute() ) {
      absRelTime = absRelTime.toRelative(System.currentTimeMillis());
    }
    return absRelTime.getTime();
  }
  
}
