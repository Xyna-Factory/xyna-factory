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

import java.util.Map;

import javax.jms.JMSException;
import javax.naming.NamingException;

/**
 *
 */
public interface JMSConnector {

   /**
    * Sends a TextMessage with the specified correlationid over the given queue.
    * 
    * @param queueName
    *              Name of the queue to use (JNDI-Name)
    * @param msgText
    *              Message to send
    * @param correlationID
    *              Correlation ID of the message
    * @throws JMSException
    * @throws NamingException
    */
   public void enqueue(String queueName, String msgText,
         String correlationID) throws JMSException, NamingException;

   /**
    * Sends a TextMessage with the specified correlationid to the given queue.
    * Message Properties will be set, if properties Parameter contain some
    * 
    * @param queueName
    *              Name of the queue to use (JNDI-Name)
    * @param msgText
    *              Message to send
    * @param correlationID
    *              Correlation ID of the message
    * @throws JMSException
    * @throws NamingException
    */
   public void enqueue(String queueName, String msgText,
         String correlationID, Map properties) throws JMSException,
         NamingException;

   /**
    * Receives (blocking) the next TextMessage from the given queue. Waits
    * DEFAULT_TIMEOUT ms bevor giving up and returning null to the caller
    * 
    * @param queueName
    *              Name of the queue (JNDI-Name)
    * @param correlationID
    *              Correlation ID of the message
    * @return A TextMessage
    * @throws JMSException
    * @throws NamingException
    */
   public String dequeue(String queueName) throws JMSException;

   /**
    * Receives (blocking) a TextMessage with the specified correlation id from
    * the given queue. Waits DEFAULT_TIMEOUT ms bevor giving up and returning
    * null to the caller
    * 
    * @param queueName
    *              Name of the queue (JNDI-Name)
    * @param correlationID
    *              Correlation ID of the message
    * @return A TextMessage
    * @throws JMSException
    * @throws NamingException
    */
   public String dequeue(String queueName, String correlationID)
         throws JMSException;

   /**
    * Receives a TextMessage with the specified correlation id from the given
    * queue. Waits the amount of ms bevor giving up and returning null to the
    * caller
    * 
    * @param queueName
    *              Name of the queue (JNDI-Name)
    * @param correlationID
    *              Correlation ID of the message
    * @return A TextMessage
    * @throws JMSException
    * @throws NamingException
    */
   public String dequeue(String queueName, String correlationID,
         long timeout) throws JMSException;

   /**
    * Closes the QueueConnector.
    * 
    * @throws JMSException
    */
   public void close() throws JMSException;

}