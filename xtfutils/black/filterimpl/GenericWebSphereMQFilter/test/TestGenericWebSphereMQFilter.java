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

import org.apache.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;

import javax.jms.*;
import javax.net.ssl.*;

import org.apache.log4j.Logger;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;


public class TestGenericWebSphereMQFilter {

  public static Logger _logger = Logger.getLogger(TestGenericWebSphereMQFilter.class);

  private static String _currentQueue = "test2queue";


  public static void send(String msg) throws Exception {
    send("localhost", "1414", "QM_gipwin234", "foobarChannel", _currentQueue, msg);
  }

  public static String listen() throws Exception {
    return listen("localhost", "1414", "QM_gipwin234", "foobarChannel", _currentQueue);
  }

  public static String listen(String hostname, String port, String queueManager, String channel,
                            String queueName) {
    Connection connection = null;
    try {
      MQConnectionFactory factory = new MQConnectionFactory();
      factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
      factory.setHostName(hostname);
      int portnum = Integer.parseInt(port);
      factory.setPort(portnum);
      factory.setQueueManager(queueManager);
      factory.setChannel(channel);

      connection = (Connection) factory.createConnection("", ""); // empty username and password
      Session session = (Session) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = (Queue) session.createQueue("queue:///" + queueName);
      MessageConsumer consumer;
      consumer = (MessageConsumer) session.createConsumer(queue);
      connection.start();
      Message msg = consumer.receive(10000);
      if (msg == null) {
        _logger.info("Received no message.");
        return null;
      }
      _logger.info("Received message:");
      _logger.info("Correlation ID = " + msg.getJMSCorrelationID() + ", JMS timestamp = " + msg.getJMSTimestamp());
      TextMessage txtMsg;
      try {
        txtMsg = (TextMessage) msg;
      }
      catch (ClassCastException e) {
        _logger.info("Received JMS Message is no TextMessage.");
        return null;
      }
      //_logger.info("Message Text:");
      //_logger.info(txtMsg.getText());
      return txtMsg.getText();
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    finally {
      if (connection != null) {
        try {
          // Note: there is no need to close the sessions, producers, and consumers of a closed connection.
          connection.close();
        }
        catch (JMSException ex) {
          _logger.error("Failed to close connection.", ex);
        }
      }
    }
  }



  public static void send(String hostname, String port, String queueManager, String channel,
                                     String queueName, String message)
                              throws Exception {
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

      //_logger.info("Going to send TextMessage to queue: Message = \n" + message);

      TextMessage textMessage = session.createTextMessage(message);
      producer.send(textMessage);
    }
    catch (JMSException e) {
      _logger.info("Failed to send message.");
      throw e;
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


  public static SSLSocketFactory getSSLSocketFactory() throws Exception {

        System.out.println("Loading SSLSocketFactory...");

        String keyStorePath = "";
        String kpassword = "";

        String trustStorePath = "";
        String tpassword = "";
        

        // Create a keystore object for the keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");

        // Open our file and read the keystore
        FileInputStream keyStoreInput = new FileInputStream(keyStorePath);
        try {
            keyStore.load(keyStoreInput, kpassword.toCharArray());
        } finally {
            keyStoreInput.close();
        }

        // Create a keystore object for the truststore
        KeyStore trustStore = KeyStore.getInstance("JKS");

        // Open our file and read the truststore (no password)
        FileInputStream trustStoreInput = new FileInputStream(trustStorePath);
        try {
            trustStore.load(trustStoreInput, tpassword.toCharArray());
        } finally {
            trustStoreInput.close();
        }

        // Create a default trust and key manager
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        // Initialise the managers
        keyManagerFactory.init(keyStore, kpassword.toCharArray());
        trustManagerFactory.init(trustStore);

        // Get an SSL context. For more information on providers see:
        // http://www.ibm.com/developerworks/library/j-ibmsecurity.html
        // Note: Not all providers support all CipherSuites.
        SSLContext sslContext = SSLContext.getInstance("TLS");

        System.out.println("SSLContext provider: " + sslContext.getProvider().toString());

        // Initialise our SSL context from the key/trust managers
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        // Get an SSLSocketFactory to pass to WMQ
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();


        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket();
        System.out.println("PROTOCOLS:");
        for (String s : sslSocket.getEnabledProtocols()) {
          System.out.println(s + ", ");
        }
        //sslSocket.getEnabledProtocols();
        //sslSocket.setEnabledProtocols(new String[] {"SSLv3", "TLSv1"});
        sslSocket.setEnabledProtocols(new String[] {"TLSv1"});

        System.out.println("PROTOCOLS:");
        for (String s : sslSocket.getEnabledProtocols()) {
          System.out.println(s + ", ");
        }
        return sslSocketFactory;
  }


  public static String listenSsl(String hostname, String port, String queueManager, String channel,
                            String queueName) {
    Connection connection = null;
    try {
      MQConnectionFactory factory = new MQConnectionFactory();
      factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
      factory.setHostName(hostname);
      int portnum = Integer.parseInt(port);
      factory.setPort(portnum);
      factory.setQueueManager(queueManager);
      factory.setChannel(channel);

      //factory.setSSLCipherSuite("SSL_RSA_WITH_NULL_MD5");
      factory.setSSLCipherSuite("SSL_RSA_WITH_RC4_128_SHA");

      factory.setSSLSocketFactory(getSSLSocketFactory());

      connection = (Connection) factory.createConnection("", ""); // empty username and password
      Session session = (Session) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = (Queue) session.createQueue("queue:///" + queueName);
      MessageConsumer consumer;
      consumer = (MessageConsumer) session.createConsumer(queue);
      connection.start();
      Message msg = consumer.receive(10000);
      if (msg == null) {
        _logger.info("Received no message.");
        return null;
      }
      _logger.info("Received message:");
      _logger.info("Correlation ID = " + msg.getJMSCorrelationID() + ", JMS timestamp = " + msg.getJMSTimestamp());
      TextMessage txtMsg;
      try {
        txtMsg = (TextMessage) msg;
      }
      catch (ClassCastException e) {
        _logger.info("Received JMS Message is no TextMessage.");
        return null;
      }
      //_logger.info("Message Text:");
      //_logger.info(txtMsg.getText());
      return txtMsg.getText();
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    finally {
      if (connection != null) {
        try {
          // Note: there is no need to close the sessions, producers, and consumers of a closed connection.
          connection.close();
        }
        catch (JMSException ex) {
          _logger.error("Failed to close connection.", ex);
        }
      }
    }
  }




  private static JmsConnectionFactory getConnFactorynonJNDI(String hostname, String port, String queueManager,
                                                            String channel)
                  throws JMSException, Exception {
          // nonJNDI Version
          JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER); // com.ibm.msg.client.wmq
          JmsConnectionFactory cf = ff.createConnectionFactory();

          cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, hostname);
          cf.setIntProperty(WMQConstants.WMQ_PORT, Integer.parseInt(port));

          cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channel); //in Wirk ein anderes, daher von aussen konfigurierbar halten

          cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
          cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManager);


          cf.setObjectProperty(WMQConstants.WMQ_SSL_SOCKET_FACTORY, getSSLSocketFactory());
          cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "SSL_RSA_WITH_3DES_EDE_CBC_SHA");
          //cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "TRIPLE_DES_SHA_US");

          return cf;
  }


  public static void sendSsl(String hostname, String port, String queueManager, String channel,
                                     String queueName, String message)
                              throws Exception {
    Connection connection = null;
    try {

      MQConnectionFactory factory = new MQConnectionFactory();
      //factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
      factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
      factory.setHostName(hostname);
      int portnum = Integer.parseInt(port);
      factory.setPort(portnum);
      factory.setQueueManager(queueManager);
      factory.setChannel(channel);

      //factory.setSSLCipherSuite("TRIPLE_DES_SHA_US");
      factory.setSSLCipherSuite("SSL_RSA_WITH_3DES_EDE_CBC_SHA");
      //factory.setSSLCipherSuite("SSL_RSA_WITH_RC4_128_SHA");
      //factory.setSSLCipherSuite("");
      //factory.setSSLCipherSuite("SSL_RSA_WITH_NULL_MD5");
      factory.setSSLSocketFactory(getSSLSocketFactory());
      factory.setSSLFipsRequired(false);

      //JmsConnectionFactory factory = getConnFactorynonJNDI(hostname, port, queueManager, channel);

      connection = factory.createConnection("", ""); // empty username and password
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue("queue:///" + queueName);
      MessageProducer producer = session.createProducer(queue);

      //_logger.info("Going to send TextMessage to queue: Message = \n" + message);

      TextMessage textMessage = session.createTextMessage(message);
      producer.send(textMessage);
    }
    catch (JMSException e) {
      _logger.info("Failed to send message.");
      throw e;
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


  public static String readFile(String filename) throws Exception {
    try {
      String line;
      StringBuilder builder = new StringBuilder("");
      BufferedReader f = new BufferedReader(new FileReader(filename));
      while ((line = f.readLine()) != null) {
        builder.append(line).append('\n');
      }
      return builder.toString();
    } catch (Exception e) {
      throw e;
    }
  }


  private static void setSslProperties() {
  }


  public static void test1() throws Exception {
    //String xml = readFile("test/");
    String xml = "test 1234 okok xyz";
    send(xml);
  }

  public static void test2() throws Exception {
    String xml = readFile("test/data/test.xml");
    _logger.debug("Read file: \n " + xml);
    send(xml);
  }

  public static void test3() throws Exception {
    String xml = readFile("test/data/portreserv.xml");
    _logger.debug("Read file: \n " + xml);
    xml = xml.replace("\n", "  ");
    xml = xml.replaceAll("xmlns=\".*?\"", "");
    _logger.info("adjusted file: \n " + xml);
  }

  public static void test4() throws Exception {
    String xml = readFile("test/data/large_response.xml");
    //String xml = readFile("test/data/short_response.xml");
    //setSslProperties();
    //String xml = "test 1234 okok xyz";
    send(xml);
  }

  public static void test5() throws Exception {
    String xml = listen();
    _logger.debug("Got msg: \n " + xml);
  }

  public static void main(String[] args) {
    try {
      test4();
    }
    catch (Throwable e) {
      _logger.error("", e);
      if (e instanceof JMSException) {
        _logger.error("", ((JMSException) e).getLinkedException());
      }
    }
  }

}
