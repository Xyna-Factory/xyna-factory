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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQConnectionFactory;


public class ATestWebSphereMQTrigger extends TestCase {


  private static Logger _logger = Logger.getLogger(ATestWebSphereMQTrigger.class);


  public void atest1() throws Exception {
    send("localhost", "1414", "QM_gipwin234", "foobarChannel", "test1queue", "Hallo Test 1");
  }
  
  private static final ThreadPoolExecutor tpe = new ThreadPoolExecutor(12,12, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
  
  public void atest2() throws InterruptedException {
    for (int i = 0; i<3; i++) {
    tpe.execute(new Runnable() {

      public void run() {
        try {
          innertest2();
        } catch (JMSException e) {
          e.printStackTrace();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        System.out.println("fertig");
      }
      
    });
    }
    Thread.sleep(10000000);
  }
  
  public void innertest2() throws JMSException, InterruptedException {
    String hostname = "localhost";
    String port = "1414";
    String queueManager = "QM_gipwin234";
    String channel = "foobarChannel";
    String queueName = "test1queue";
    
    MQConnectionFactory factory = new MQConnectionFactory();
    factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
    factory.setHostName(hostname);
    int portnum = Integer.parseInt(port);
    factory.setPort(portnum);
    factory.setQueueManager(queueManager);
    factory.setChannel(channel);

    for (int i = 0; i < 100; i++) {
      Connection connection = null;
      MessageConsumer consumer = null;
      MessageProducer producer = null;
      try {
        connection = factory.createConnection("", ""); // empty username and password
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue("queue:///" + queueName);
        consumer = session.createConsumer(queue);
        producer = session.createProducer(queue);
        connection.start();
        readAndWriteAsync(session, consumer, producer);
        Thread.sleep(100);
      } catch (JMSException e) {
        _logger.info("Failed to send message.");
        e.printStackTrace();
      } finally {
        closeConcurrently(connection, consumer);
      }
    }
  }
  
  private static final AtomicLong l = new AtomicLong(0);

  private void readAndWriteAsync(final Session session, final MessageConsumer consumer, final MessageProducer producer) {
    tpe.execute(new Runnable() {

      public void run() {
        try {
          TextMessage textMessage = session.createTextMessage("test " + l.incrementAndGet());
         // producer.send(textMessage);
          producer.close();
          Message receive = consumer.receive(500000);
          if (receive == null) {
            System.out.println("received null");
          } else {
            System.out.println("received " + ((TextMessage)receive).getText());
          }
        } catch (JMSException e) {
          e.printStackTrace();
        }
        
      }
      
    });
  }

  private void closeConcurrently(final Connection connection, final MessageConsumer consumer) {
    final CountDownLatch l = new CountDownLatch(2);
    Runnable closeConnection = new Runnable() {

      public void run() {
        try {
          System.out.println("starting stop");
          connection.stop();
          System.out.println("starting close");
          connection.close();
          System.out.println("closed connection");
        } catch (JMSException e) {
          e.printStackTrace();
        }
        l.countDown();
      }
      
    };
    Runnable closeConsumer = new Runnable() {

      public void run() {
        try {
          consumer.close();
          System.out.println("closed consumer");
        } catch (JMSException e) {
          e.printStackTrace();
        }
        l.countDown();
      }
      
    };
    tpe.execute(closeConsumer);
    tpe.execute(closeConnection);
    try {
      l.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void send(String hostname, String port, String queueManager, String channel,
                                     String queueName, String message)  throws Exception {
    Connection connection = null;
    try {
      MQConnectionFactory factory = new MQConnectionFactory();
      factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
      factory.setHostName(hostname);
      int portnum = Integer.parseInt(port);
      factory.setPort(portnum);
      factory.setQueueManager(queueManager);
      factory.setChannel(channel);

      connection = factory.createConnection("", ""); // empty username and password
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue("queue:///" + queueName);
      MessageProducer producer = session.createProducer(queue);

      _logger.info("Going to send TextMessage to queue: Message = \n" + message);

      TextMessage textMessage = session.createTextMessage(message);
      producer.send(textMessage);
    }
    catch (JMSException e) {
      _logger.info("Failed to send message.");
      e.printStackTrace();
    }
    finally {
      if (connection != null) {
        try {
          // Note: there is no need to close the sessions, producers, and consumers of a closed connection.
          connection.close();
        }
        catch (JMSException e) {
          //do nothing
        }
      }
    }
  }


  public static void main(String[] args) {
    try {
      new ATestWebSphereMQTrigger().atest1();
    }
    catch (Exception e) {
      _logger.error("", e);
    }
  }


}
