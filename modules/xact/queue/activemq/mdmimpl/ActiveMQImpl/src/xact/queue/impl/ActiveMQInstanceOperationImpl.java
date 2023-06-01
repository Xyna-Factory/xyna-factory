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
package xact.queue.impl;


import xact.queue.ActiveMQ;
import xact.queue.ActiveMQInstanceOperation;
import xact.queue.ActiveMQSuperProxy;
import xact.queue.CorrelationId;
import xact.queue.DequeueFailedException;
import xact.queue.DequeueOptions;
import xact.queue.EnqueueFailedException;
import xact.queue.EnqueueOptions;
import xact.queue.NoConnectionException;
import xact.queue.NoSuchMessageException;
import xact.queue.QueueMessage;
import xact.queue.admin.ActiveMQConfig;

import com.gip.xyna.xprc.XynaOrderServerExtension;


public class ActiveMQInstanceOperationImpl extends ActiveMQSuperProxy implements ActiveMQInstanceOperation {

  private static final long serialVersionUID = 1L;
  
  private ActiveMQConfig config;

  public ActiveMQInstanceOperationImpl(ActiveMQ instanceVar) {
    super(instanceVar);
    if( instanceVar.getQueueConfigType() instanceof ActiveMQConfig) {
      config = (ActiveMQConfig)instanceVar.getQueueConfigType();
    } else {
      throw new IllegalStateException("Expected WebSphereMQConfig, got "+instanceVar.getQueueConfigType());
    }
  }

  public QueueMessage dequeueSynchronously_withOptions(XynaOrderServerExtension correlatedXynaOrder,
                                                       DequeueOptions dequeueOptions) throws NoSuchMessageException,
      NoConnectionException, DequeueFailedException {
    ActiveMQueue queue = new ActiveMQueue(config);
    try {
      queue.connect(); //TODO cachen in Pool...
      return queue.receive(dequeueOptions);
    } finally {
      queue.close();
    }
  }

  public void enqueue_withOptions(XynaOrderServerExtension correlatedXynaOrder, CorrelationId correlationId,
                                  QueueMessage queueMessage, EnqueueOptions enqueueOptions)
      throws NoConnectionException, EnqueueFailedException {
    ActiveMQueue queue = new ActiveMQueue(config);
    try {
      queue.connect(); //TODO cachen in Pool...
      queue.send(correlationId, queueMessage, enqueueOptions);
    } finally {
      queue.close();
    }
  }

  public void close() {
    //derzeit nichts zu tun
  }

  
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }


}
