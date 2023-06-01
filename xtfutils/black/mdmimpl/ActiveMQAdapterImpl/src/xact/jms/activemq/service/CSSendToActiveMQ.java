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

package xact.jms.activemq.service;

import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.ActiveMQAdapterException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.ActiveMQConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueManagement;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;

import xact.jms.activemq.datatypes.ActiveMQMessage;
import xact.jms.activemq.datatypes.CorrelationID;
import xact.jms.activemq.datatypes.XynaQueueMgmtQueueName;

@Deprecated
public class CSSendToActiveMQ {

  private static Logger _logger = Logger.getLogger(CSSendToActiveMQ.class);


  protected String getCodedServiceName() {
    return "sendToActiveMQ";
  }

  public void execute(XynaQueueMgmtQueueName xynaQueueMgmtQueueName, ActiveMQMessage activeMQMessage,
                      CorrelationID correlationID) throws XynaException {
    try {
      _logger.debug("### Entering Coded service " + getCodedServiceName());
      executeImpl(xynaQueueMgmtQueueName, activeMQMessage, correlationID);
    }
    catch (Throwable t) {
      _logger.error("Error in coded service " + getCodedServiceName(), t);
      throw new ActiveMQAdapterException("Error in coded service " + getCodedServiceName() +
                                                     ":" + t.getMessage(), t);
    }
  }


  private void executeImpl(XynaQueueMgmtQueueName xynaQueueMgmtQueueName, ActiveMQMessage activeMQMessage,
                             CorrelationID correlationID) throws XynaException {
    // Implemented as code snippet!
  }

//'tcp://10.0.10.76:61616'
/*
  private void send(String message, ActiveMQConfig config, String correlationID)
                    throws ActiveMQAdapterException {
    _logger.debug("Entering send()");
    Session session = null;
    MessageProducer msgProd = null;
    Connection connection = null;
    _logger.debug("Queuename = " + config.getQueueName());
    _logger.debug("Queue URL = " + config.getUrl());
    try {
      ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(config.getUrl());
      connection = cf.createConnection();
      connection.start();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Destination destination = session.createQueue(config.getQueueName());
      msgProd = session.createProducer(destination);
      TextMessage msg = session.createTextMessage(message);
      msg.setJMSCorrelationID(correlationID);
      msgProd.send(msg);
      _logger.debug("Sent Message to queue: \n" + msg.getText());
    }
    catch (Exception e) {
      _logger.error("Error while trying to enqueue message", e);
      throw new RuntimeException("Error while trying to enqueue message", e);
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
*/

  private ActiveMQConnectData getConnectData(Queue queue) {
    //Queue queue = getStoredQueue(uniqueName);
    return (ActiveMQConnectData) queue.getConnectData();
  }


  private Queue getStoredQueue(String uniqueName) {
    try {
      QueueManagement mgmt = new QueueManagement();
      Queue ret = mgmt.getQueue(uniqueName);

      _logger.debug("Got Stored Queue: " + ret.toString());
      if (ret.getQueueType() != QueueType.ACTIVE_MQ) {
        throw new RuntimeException("Error getting registered queue data (name = " + uniqueName + "):" +
                                   " Wrong queue type.");
      }
      return ret;
    }
    catch (Exception e) {
      throw new RuntimeException("Error getting registered queue data (name = " + uniqueName + ")", e);
    }
  }

}
