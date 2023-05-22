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
package com.gip.xyna.xact.trigger;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.trigger.ssl.SSLConfig;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;

public class WebSphereMQTriggerConnection extends TriggerConnection {

  private static Logger logger = CentralFactoryLogging.getLogger(WebSphereMQTriggerConnection.class);

  private transient Message msg;
  private transient String xynaQueueMgmtQueueName;

  // arbitrary constructor
  /*
  public WebSphereMQTriggerConnection(Message msg) {
    this.msg = msg;
  }
  */

  public WebSphereMQTriggerConnection(Message msg, String qName) {
    this.msg = msg;
    this.xynaQueueMgmtQueueName = qName;
  }

  public Message getMessage() {
    return msg;
  }

  public String getXynaQueueMgmtQueueName() {
    return xynaQueueMgmtQueueName;
  }


  /*
  public WebSphereMQTrigger getTrigger() {
    return (WebSphereMQTrigger) super.getTrigger();
  }
  */


  public void sendToQueue(com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue queue,
                                 Message message, boolean useSSL, SSLConfig sslConfig) throws JMSException {
    WebSphereMQTrigger t = (WebSphereMQTrigger) super.getTrigger();
    WebSphereMQTrigger.sendToQueue(queue, message, useSSL, sslConfig, t.getStartParameter().getUserName(), t.getStartParameter().getPassword());
  }

  public void sendMessageToErrorQueue(Message message) throws JMSException {
    ((WebSphereMQTrigger) super.getTrigger()).sendMessageToErrorQueue(message);
  }

}
