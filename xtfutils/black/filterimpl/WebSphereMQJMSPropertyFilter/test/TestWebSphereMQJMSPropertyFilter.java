
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyStore;

import javax.jms.*;
import javax.net.ssl.*;

import org.apache.log4j.Logger;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;


public class TestWebSphereMQJMSPropertyFilter {

  public static Logger _logger = Logger.getLogger(TestWebSphereMQJMSPropertyFilter.class);

  private static String _currentQueue = "Adonis_BNG2a_BLTVREBidRequest";

  public static void send(String msg) throws Exception {
    send("", "", "", "foobarChannel", _currentQueue, msg);
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

      textMessage.setStringProperty("myProp1", "val_1");
      textMessage.setStringProperty("myProp2", "val_2");

      textMessage.setJMSCorrelationID("mycorrid_1");
      textMessage.setJMSMessageID("msgid_1");

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
        // ARGS:
        // -Djavax.net.ssl.trustStore=C:\LocalView\Workspace_Aydin\jmsui\resources\myTruststore.jks
        // -Djavax.net.ssl.trustStorePassword=aydin123
        // -Djavax.net.ssl.keyStore=C:\LocalView\Workspace_Aydin\jmsui\resources\myKeystore_TST1.jks
        // -Djavax.net.ssl.keyStorePassword=aydin123 -Dssl.debug=true
        // -Djavax.net.debug=ssl:verbose

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
    //setSslProperties();
    String xml = "test 1234 okok xyz";
    send(xml);
  }


  public static void test2() throws Exception {
    //String xml = readFile("test/data/BLT_GetResDataReq.xml");
    String xml = readFile("test/data/bug1.xml");
    //setSslProperties();
    //String xml = "test 1234 okok xyz";
    send(xml);
  }

  public static void main(String[] args) {
    try {
      test2();
    }
    catch (Throwable e) {
      _logger.error("", e);
    }
  }

}
