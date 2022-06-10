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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

public class JMSUtils {

   private QueueSession session = null;
   private QueueConnection connection = null;

   /**
    * Beginnt eine neue QueueSession, die für die übrigen Methoden benötigt
    * wird. Für AQ bitte anderen Konstruktor verwenden. Dieser funktioniert über
    * JNDI.
    * 
    * @param url
    *              zb opmn:ormi://10.0.0.173:6003:xyna_soa
    * @param username
    *              vom AS bzgl url
    * @param password
    * @param connectionFactory
    *              (JNDI-location)
    * @throws Exception
    */
   public JMSUtils(String url, String username, String password,
         String connectionFactory) throws Exception {
      // TODO: use QueueConnector
      // TODO: generic use possible (JDK5.0)
      Hashtable props = new Hashtable();
      props.put(Context.INITIAL_CONTEXT_FACTORY,
            "com.evermind.server.ApplicationClientInitialContextFactory");
      props.put(Context.PROVIDER_URL, url);
      props.put(Context.SECURITY_PRINCIPAL, username);
      props.put(Context.SECURITY_CREDENTIALS, password);
      Context ctx = new InitialContext(props);
      QueueConnectionFactory factory = (QueueConnectionFactory) ctx
            .lookup(connectionFactory);
      connection = factory.createQueueConnection();
      connection.start();
      session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
   }

   /**
    * Gibt die Anzahl der Nachrichten zurück, die sich in der Queue befinden.
    * 
    * @param queueName
    * @return
    * @throws Exception
    */
   public int getMessageCount(String queueName) throws Exception {
      return getMessagesWithoutDequeue(queueName).length;
   }

   /**
    * Gibt alle Nachrichten in der Queue zurück, ohne sie auszulesen.
    * 
    * @param queueName
    * @return
    * @throws Exception
    */
   public TextMessage[] getMessagesWithoutDequeue(String queueName)
         throws Exception {
      Queue queue = session.createQueue(queueName);
      QueueBrowser browser = session.createBrowser(queue);
      Enumeration messages = browser.getEnumeration();
      // TODO: generic use possible (JDK5.0)
      Vector v = new Vector();
      while (messages.hasMoreElements()) {
         v.add((TextMessage) messages.nextElement());
      }
      return (TextMessage[]) v.toArray(new TextMessage[] {});
   }

   /**
    * löscht alle Nachrichten aus der Queue
    * 
    * @param queueName
    * @throws Exception
    */
   public void deleteAllMessages(String queueName) throws Exception {
      Queue queue = session.createQueue(queueName);
      QueueReceiver consumer = session.createReceiver(queue);
      while (consumer.receiveNoWait() != null) {

      }
      consumer.close();
   }

   /**
    * löscht alle Nachrichten mit der angegebenen correlationID aus der Queue
    * 
    * @param queueName
    * @param correlationId
    * @throws Exception
    */
   public void deleteAllCorrelatedMessages(String queueName,
         String correlationId) throws Exception {
      Queue queue = session.createQueue(queueName);
      QueueReceiver consumer = session.createReceiver(queue,
            "JMSCorrelationID = '" + correlationId + "'");
      while (consumer.receiveNoWait() != null) {

      }
      consumer.close();
   }

   /**
    * stellt eine neue Textnachricht mit übergebenem Text und correlationID in
    * die Queue ein
    * 
    * @param queueName
    * @param text
    * @param correlationId
    * @throws Exception
    * @deprecated use QueueConnecter.enqueue
    */
   public void enqueue(String queueName, String text, String correlationId)
         throws Exception {
      Queue queue = session.createQueue(queueName);
      QueueSender sender = session.createSender(queue);
      TextMessage msg = session.createTextMessage(text);
      msg.setJMSCorrelationID(correlationId);
      sender.send(msg);
      sender.close();
   }

   /**
    * liest alle Textnachrichten mit passender correlationId aus.
    * 
    * @param queueName
    * @param correlationId
    * @return
    * @throws Exception
    */
   public TextMessage[] dequeue(String queueName, String correlationId)
         throws Exception {
      Queue queue = session.createQueue(queueName);
      QueueReceiver consumer = session.createReceiver(queue,
            "JMSCorrelationID = '" + correlationId + "'");
      ArrayList list = new ArrayList();
      for (Message msg = consumer.receiveNoWait(); msg != null; msg = consumer
            .receiveNoWait()) {
         list.add(msg);
      }
      consumer.close();
      return (TextMessage[]) list.toArray(new TextMessage[] {});
   }

   /**
    * liest die erste Textnachricht mit passender correlationID aus.
    * 
    * @param queueName
    * @param correlationId
    * @return
    * @throws Exception
    */
   public TextMessage dequeueFirst(String queueName, String correlationId)
         throws Exception {
      Queue queue = session.createQueue(queueName);
      QueueReceiver consumer = session.createReceiver(queue,
            "JMSCorrelationID = '" + correlationId + "'");
      TextMessage msg = (TextMessage) consumer.receiveNoWait();
      consumer.close();
      return msg;
   }

   /**
    * räumt session und connection auf.
    * 
    * @throws Exception
    */
   protected void finalize() throws Exception {
      try {
         if (session != null) {
            session.close();
         }
      } finally {
         if (connection != null) {
            connection.close();
         }
      }
   }

}
