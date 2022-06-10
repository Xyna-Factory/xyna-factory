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

import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;


public class TestActiveMQTrigger {


  private static Logger _logger = Logger.getLogger(TestActiveMQTrigger.class);


  static void test1() throws Exception {
    String msg = "Hallo Test! ABC 987 xyz okokok ...";
    send("testQ2", "ssl://10.3.14.50:61617", msg);
  }


  public static void send(String queueName, String url, String message) {
    Session session = null;
    MessageProducer msgProd = null;
    Connection connection = null;
    _logger.info("Queuename = " + queueName);
    _logger.info("Queue URL = " + url);
    try {
      ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(url);
      connection = cf.createConnection();
      connection.start();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Destination destination = session.createQueue(queueName);
      msgProd = session.createProducer(destination);
      TextMessage msg = session.createTextMessage(message);
      //msg.setJMSCorrelationID(correlationID);
      msgProd.send(msg);
      _logger.info("Sent Message to queue: \n" + msg.getText());
    }
    catch (Exception e) {
      _logger.info("Error while trying to enqueue message", e);
    }
    finally {
      try {
        msgProd.close();
        session.close();
        connection.close();
      }
      catch (Exception e) {
        //do nothing
      }
    }
  }


  public static void setTruststore(String path, String password) {
    System.setProperty("javax.net.ssl.trustStore", path);
    System.setProperty("javax.net.ssl.trustStorePassword", password);
  }



  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      setTruststore("src_test/geronimo.jks", "changeit");
      test1();
    }
    catch (Exception e) {
      _logger.error("", e);
    }
  }

}
