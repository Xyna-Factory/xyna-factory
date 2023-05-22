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
package com.gip.xyna.utils.jms;

import java.util.Hashtable;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.TemporaryQueue;
import javax.naming.NamingException;

/**
 * Class for sending and receiving (text) messages using jms queues.
 * 
 * 
 * @deprecated use InMemoryQueueConnector instead
 * 
 */
public class QueueConnector implements JMSConnector {
   
   private InMemoryQueueConnector connector;

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
    *              Name of the ConnectionFactory to use (JNDI-Name)
    * @throws NamingException
    * @throws JMSException
    */
   public QueueConnector(Hashtable environment,
         String queueConnectionFactoryName) throws JMSException {
      connector = new InMemoryQueueConnector(environment, queueConnectionFactoryName);
   }

   /**
    * Initializes a new QueueConnector without environment for use in
    * application server
    * 
    * @param queueConnectionFactoryName
    *              Name of the ConnectionFactory to use (JNDI-Name)
    * @throws NamingException
    * @throws JMSException
    */
   public QueueConnector(String queueConnectionFactoryName) throws JMSException {
      connector = new InMemoryQueueConnector(queueConnectionFactoryName);
   }

   public QueueConnector(String initial_context_factory, String provider_url,
         String username, String password, String connection_factory)
         throws JMSException {
      connector = new InMemoryQueueConnector(initial_context_factory, provider_url, username, password, connection_factory);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#enqueue(java.lang.String,
    *      java.lang.String, java.lang.String)
    */
   public void enqueue(String queueName, String msgText, String correlationID)
         throws JMSException, NamingException {
      connector.enqueue(queueName, msgText, correlationID);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#enqueue(java.lang.String,
    *      java.lang.String, java.lang.String, java.util.Map)
    */
   public void enqueue(String queueName, String msgText, String correlationID,
         Map properties) throws JMSException, NamingException {
      connector.enqueue(queueName, msgText, correlationID, properties);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String)
    */
   public String dequeue(String queueName) throws JMSException {
      return connector.dequeue(queueName);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String,
    *      java.lang.String)
    */
   public String dequeue(String queueName, String correlationID)
         throws JMSException {
      return connector.dequeue(queueName, correlationID);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String,
    *      java.lang.String, long)
    */
   public String dequeue(String queueName, String correlationID, long timeout)
         throws JMSException {
     return connector.dequeue(queueName, correlationID, timeout);
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
      return connector.createTemporaryQueue(queueName);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.gip.xyna.utils.jms.JMSConnector#close()
    */
   public void close() throws JMSException {
      connector.close();
   }
}
