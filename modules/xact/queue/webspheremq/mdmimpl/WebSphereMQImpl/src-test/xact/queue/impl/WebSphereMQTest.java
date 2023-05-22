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

import java.util.List;

import xact.queue.CorrelationId;
import xact.queue.DequeueFailedException;
import xact.queue.DequeueOptions;
import xact.queue.EnqueueFailedException;
import xact.queue.EnqueueOptions;
import xact.queue.MessageProperties;
import xact.queue.Next;
import xact.queue.NoConnectionException;
import xact.queue.NoSuchMessageException;
import xact.queue.Property;
import xact.queue.QueueMessage;
import xact.queue.admin.WebSphereMQConfig;
import xact.queue.impl.WebSphereMQueue;

import com.gip.xyna.xprc.xsched.xynaobjects.RelativeDate;


/**
 *
 */
public class WebSphereMQTest {

  
  
  public static void main(String[] args) throws EnqueueFailedException, NoConnectionException, DequeueFailedException, NoSuchMessageException, InterruptedException {
    
   
    WebSphereMQConfig config = new WebSphereMQConfig();
    config.setHostname("localhost");
    config.setPort(1414);
    config.setQueueManager("QM_gipwin234");
    config.setChannel("foobarChannel");
    
    config.setName_externalQueue("test1queue");
    
    WebSphereMQueue queue = new WebSphereMQueue(config);
     
    
    EnqueueOptions enqueueOptions = new EnqueueOptions();
    enqueueOptions.setExpiration( new RelativeDate("2 min") );
    
    QueueMessage queueMessageSend = new QueueMessage();
    queueMessageSend.setMessage("Dies ist ein Test");
    queueMessageSend.setMessageProperties(new MessageProperties());
    queueMessageSend.getMessageProperties().addToProperties(new Property("prop1", "value"));
    queueMessageSend.getMessageProperties().addToProperties(new Property("prop2", "test"));
        
    enqueue(queue, "hiho-125", queueMessageSend, enqueueOptions);
    queue.close();
    
    
    queue = new WebSphereMQueue(config);
    
    
    //Thread.sleep(1000);
    
    DequeueOptions dequeueOptions = new DequeueOptions();
    //dequeueOptions.setIdentification( new CorrelationId("hiho-121") );
    dequeueOptions.setIdentification(new Next() );
    //dequeueOptions.setWait( new RelativeDate("10 ms") );
       
    QueueMessage qm = dequeue(queue, dequeueOptions );
    System.out.println( "Message "+ qm.getMessage() );
    System.out.println( "CorrId "+ qm.getMessageProperties().getCorrelationId().getCorrelationId() );
    printProperties(qm.getMessageProperties().getProperties());
  }

  private static void printProperties(List<? extends Property> properties) {
    if( properties == null ) {
      System.out.println( "null" );
      return;
    }
    for( Property p : properties ) {
      System.out.println( p.getKey() + " -> " + p.getValue() );
    }
  }

  private static QueueMessage dequeue(WebSphereMQueue queue, DequeueOptions dequeueOptions) throws NoConnectionException, DequeueFailedException, NoSuchMessageException {
    try {
      queue.connect();
      return queue.receive(dequeueOptions );
    } finally {
      queue.close();
    }
  }


  private static void enqueue(WebSphereMQueue queue, String corrId, QueueMessage queueMessage, 
                              EnqueueOptions enqueueOptions) throws EnqueueFailedException, NoConnectionException {
    CorrelationId correlationId = new CorrelationId(corrId);
    try {
      queue.connect();
      queue.send(correlationId, queueMessage, enqueueOptions );
    } finally {
      queue.close();
    }
  }
  
  
  
  
}
