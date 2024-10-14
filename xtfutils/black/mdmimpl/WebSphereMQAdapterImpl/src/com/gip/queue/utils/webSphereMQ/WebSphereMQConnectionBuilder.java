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

package com.gip.queue.utils.webSphereMQ;

import javax.jms.*;

import com.gip.queue.utils.QueueConnection;
import com.gip.queue.utils.QueueConnectionBuilder;
import com.gip.queue.utils.exception.QueueException;
import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQConnectionFactory;


public class WebSphereMQConnectionBuilder implements QueueConnectionBuilder {

  //private Destination _destination = null;
  private MQConnectionFactory _factory = null;
  //private Queue _queue = null;
  private WebSphereMQConfig _queueConfig = null;
  //private WebSphereMQConnectData _connData;


  public WebSphereMQConnectionBuilder(WebSphereMQConfig config) throws QueueException {
    _queueConfig = config;
    try {
      initFactory(config);
    }
    catch (JMSException e) {
      throw new QueueException(e);
    }
  }


  public QueueConnection build() {
    return new WebSphereMQConnection(_factory, _queueConfig.getQueueName());
  }


  private void initFactory(WebSphereMQConfig queueConfig) throws JMSException {
    _factory = new MQConnectionFactory();

    if (queueConfig.getTransportType() == null) {
      _factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
    }
    else {
      _factory.setTransportType(queueConfig.getTransportType());
    }
    _factory.setHostName(queueConfig.getHostName());
    _factory.setPort(queueConfig.getPort());
    _factory.setQueueManager(queueConfig.getQueueManager());
    _factory.setChannel(queueConfig.getChannel());
  }

}
