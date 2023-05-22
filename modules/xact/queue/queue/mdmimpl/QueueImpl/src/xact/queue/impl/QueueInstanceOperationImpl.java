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
package xact.queue.impl;


import xact.queue.CorrelationId;
import xact.queue.DequeueFailedException;
import xact.queue.DequeueOptions;
import xact.queue.EnqueueFailedException;
import xact.queue.EnqueueOptions;
import xact.queue.Identification;
import xact.queue.MessageId;
import xact.queue.Next;
import xact.queue.NoConnectionException;
import xact.queue.NoSuchMessageException;
import xact.queue.Queue;
import xact.queue.QueueInstanceOperation;
import xact.queue.QueueMessage;
import xact.queue.QueueSuperProxy;

import com.gip.xyna.xprc.XynaOrderServerExtension;


public abstract class QueueInstanceOperationImpl extends QueueSuperProxy implements QueueInstanceOperation {

  private static final long serialVersionUID = 1L;

  public QueueInstanceOperationImpl(Queue instanceVar) {
    super(instanceVar);
  }

  public QueueMessage dequeueAsynchronously(CorrelationId correlationId) throws NoConnectionException, NoSuchMessageException, DequeueFailedException {
    // Implemented as workflow!
    return null;
  }

  public QueueMessage dequeueAsynchronously_withOptions(DequeueOptions dequeueOptions) throws NoSuchMessageException, NoConnectionException, DequeueFailedException {
    // Implemented as workflow!
    return null;
  }

  public abstract QueueMessage dequeueSynchronously_withOptions(XynaOrderServerExtension correlatedXynaOrder, DequeueOptions dequeueOptions) throws NoSuchMessageException, NoConnectionException, DequeueFailedException;

  public void enqueue(XynaOrderServerExtension correlatedXynaOrder, CorrelationId correlationId, QueueMessage queueMessage) throws NoConnectionException, EnqueueFailedException {
    enqueue_withOptions(correlatedXynaOrder, correlationId, queueMessage, new EnqueueOptions());
  }

  public abstract void enqueue_withOptions(XynaOrderServerExtension correlatedXynaOrder, CorrelationId correlationId, QueueMessage queueMessage, EnqueueOptions enqueueOptions) throws NoConnectionException, EnqueueFailedException;

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }
  
  public abstract void close();
  
  public static enum MessageIdentification {
    ByCorrelationId {
      public String toMessageIdentificationString(DequeueOptions dequeueOptions) {
        return "ByCorrelationId "+ getIdentification(dequeueOptions);
      }
      public String getIdentification(DequeueOptions dequeueOptions) {
        return ((CorrelationId)dequeueOptions.getIdentification()).getCorrelationId();
      }     
    },
    ByMessageId {
      public String toMessageIdentificationString(DequeueOptions dequeueOptions) {
        return "ByMessageId "+ getIdentification(dequeueOptions);
      } 
      public String getIdentification(DequeueOptions dequeueOptions) {
        return ((MessageId)dequeueOptions.getIdentification()).getMessageId();
      }
    },
    Next {
      public String toMessageIdentificationString(DequeueOptions dequeueOptions) {
        return "Next";
      }
      public String getIdentification(DequeueOptions dequeueOptions) {
        return "";
      }
    };
    
    public static MessageIdentification valueOf( Identification identification ) {
      if( identification instanceof CorrelationId ) {
        return ByCorrelationId;
      } else if( identification instanceof MessageId ) {
        return ByMessageId;
      } else if( identification instanceof Next ) {
        return Next;
      } else {
        throw new UnsupportedOperationException("Unknown message identification "+identification );
      }
    }

    public abstract String toMessageIdentificationString(DequeueOptions dequeueOptions);

    public abstract String getIdentification(DequeueOptions dequeueOptions);
    
  }


}
