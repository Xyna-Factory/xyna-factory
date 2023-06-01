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
package com.gip.xyna.xact.trigger;

import javax.jms.Message;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;

public class ActiveMQTriggerConnection extends TriggerConnection {

  private static final long serialVersionUID = -9015557773519779486L;


  private static Logger logger = CentralFactoryLogging.getLogger(ActiveMQTriggerConnection.class);


  private transient Message msg;
  private transient String xynaQueueMgmtQueueName;
  
  public ActiveMQTriggerConnection(Message msg, String xynaQueueMgmtQueueName) {
    this.msg = msg;
    this.xynaQueueMgmtQueueName = xynaQueueMgmtQueueName;
  }
  
  public Message getMessage() {
    return msg;
  }
  
  public String getXynaQueueMgmtQueueName() {
    return xynaQueueMgmtQueueName;
  }
  
}
