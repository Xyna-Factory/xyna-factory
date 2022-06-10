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
package com.gip.xyna.utils.jms;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Class for sending and receiving (text) messages using jms queues.
 * 
 * TODO: example for usage
 * 
 * @deprecated use jmsqueueutils
 * 
 */
public class InMemoryQueueConnector implements JMSConnector {
   private Context ctx = null;

   private QueueSession session = null;

   private QueueConnection connection = null;

   public static final int DEFAULT_TIMEOUT = 10000; // 10 Sek. Timeout beim

   // Lesen aus der Queue

   /**
    * Initializes a new QueueConnector. Needed environment variables are
    * INITIAL_CONTEXT_FACTORY (eg.
    * com.evermind.server.ApplicationClientInitialContextFactory), PROVIDER_URL,
    * PRINCIPAL and CREDENTIALS.
    * 
    * @param environment
    * @param queueConnectionFactoryName
    *           Name of the ConnectionFactory to use (JNDI-Name)
    * @throws NamingException
    * @throws JMSException
    */
   public InMemoryQueueConnector(Hashtable environment,
         String queueConnectionFactoryName) throws JMSException {
      init(environment, queueConnectionFactoryName);
   }

   /**
    * Initializes a new QueueConnector without environment for use in
    * application server
    * 
    * @param queueConnectionFactoryName
    *           Name of the ConnectionFactory to use (JNDI-Name)
    * @throws NamingException
    * @throws JMSException
    */
   public InMemoryQueueConnector(String queueConnectionFactoryName)
         throws JMSException {
      init(null, queueConnectionFactoryName);
   }

   private void init(Hashtable environment, String queueConnectionFactoryName)
         throws JMSException {
      QueueConnectionFactory factory = null;
      try {
         if (null == environment) {
            ctx = new InitialContext(); // wenn "innerhalb" des
            // ApplicationServers
         } else {
            ctx = new InitialContext(environment); // für "externe" Clients
            // oder Zugriff auf einen
            // anderen
            // AppServer/Container
         }

         factory = (QueueConnectionFactory) ctx
               .lookup(queueConnectionFactoryName);
         connection = factory.createQueueConnection();
         connection.start();

         session = connection.createQueueSession(false,
               Session.AUTO_ACKNOWLEDGE);
      } catch (NamingException e) {
         JMSException exp = new JMSException(e.getExplanation());
         exp.setLinkedException(e);
         throw exp;
      }
   }

   public InMemoryQueueConnector(String initial_context_factory,
         String provider_url, String username, String password,
         String connection_factory) throws JMSException {
      Hashtable environment = new Hashtable();
      environment.put(Context.INITIAL_CONTEXT_FACTORY, initial_context_factory);
      environment.put(Context.PROVIDER_URL, provider_url);
      environment.put(Context.SECURITY_PRINCIPAL, username);
      environment.put(Context.SECURITY_CREDENTIALS, password);
      init(environment, connection_factory);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#enqueue(java.lang.String,
    * java.lang.String, java.lang.String)
    */
   public void enqueue(String queueName, String msgText, String correlationID)
         throws JMSException, NamingException {
      enqueue(queueName, msgText, correlationID, null);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#enqueue(java.lang.String,
    * java.lang.String, java.lang.String, java.util.Map)
    */
   public void enqueue(String queueName, String msgText, String correlationID,
         Map properties) throws JMSException, NamingException {
      if ((queueName == null) || (queueName.equals(""))) {
         throw new JMSException("Queue name must be declared");
      }
      Queue queue = (Queue) ctx.lookup(queueName);
      QueueSender sender = null;
      try {
         TextMessage msg = session.createTextMessage(msgText);
         msg.setJMSCorrelationID(correlationID);

         // Wir verwenden hier die allgemeine Methode setObjectProperty()
         // Besser waere es vermutlich, die typspezifischen Methoden zu
         // verwenden
         if (null != properties && properties.size() > 0) {
            String key;
            Object value;
            for (Iterator iter = properties.keySet().iterator(); iter.hasNext();) {
               key = (String) iter.next();
               value = properties.get(key);
               // Fuer Messages mit Delay (geht mit AQ): Das
               // MessageProperty heisst: 'JMS_OracleDelay'
               if (null != value) {
                  msg.setObjectProperty(key, value);
               }
            }
         }
         sender = session.createSender(queue);
         sender.send(msg);
      } finally {
         if (sender != null) {
            sender.close();
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String)
    */
   public String dequeue(String queueName) throws JMSException {
      return dequeue(queueName, null, DEFAULT_TIMEOUT);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String,
    * java.lang.String)
    */
   public String dequeue(String queueName, String correlationID)
         throws JMSException {
      return dequeue(queueName, correlationID, DEFAULT_TIMEOUT);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String,
    * java.lang.String, long)
    */
   public String dequeue(String queueName, String correlationID, long timeout)
         throws JMSException {
      Queue queue;
      try {
         queue = (Queue) ctx.lookup(queueName);
      } catch (NamingException e) {
         JMSException exp = new JMSException(e.getExplanation());
         exp.setLinkedException(e);
         throw exp;
      }
      QueueReceiver receiver = null;
      String msgText = null;
      try {
         if (null != correlationID && correlationID.length() > 0) {
            receiver = session.createReceiver(queue, "JMSCorrelationID = '"
                  + correlationID + "'");
         } else {
            receiver = session.createReceiver(queue);
         }
         TextMessage msg = null;
         if (timeout > 0) {
            msg = (TextMessage) receiver.receive(timeout);
         } else {
            msg = (TextMessage) receiver.receiveNoWait();
         }
         if (msg == null) {
            if (null != correlationID && correlationID.length() > 0) {
               throw new JMSException(
                     "Unable to get message with correlation id: "
                           + correlationID + " from queue: " + queueName);
            }
            throw new JMSException("Unable to get next message from queue: "
                  + queueName);
         }
         msgText = msg.getText();
      } finally {
         if (receiver != null) {
            receiver.close();
         }
      }
      return msgText;
   }

   /**
    * Creates a temporary Queue, which will disappear when the QueueConnector is
    * closed
    * 
    * @param queueName
    * @return
    * @throws JMSException
    */
   public TemporaryQueue createTemporaryQueue(String queueName)
         throws JMSException {
      return session.createTemporaryQueue();
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#close()
    */
   public void close() throws JMSException {
      if (session != null)
         session.close();
      if (connection != null)
         connection.close();
   }

}
