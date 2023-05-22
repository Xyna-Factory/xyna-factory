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

import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;

public class SFTPTriggerConnection extends TriggerConnection {

  /**
   * 
   */
  private static final long serialVersionUID = -5147487763750951711L;

  //private static Logger logger = CentralFactoryLogging.getLogger(SFTPTriggerConnection.class);

  private ConcurrentHashMap<Long, String> replyqueue = null;
  private QueueEntry currentrequest = null;
    
  // arbitrary constructor
  public SFTPTriggerConnection(ConcurrentHashMap<Long, String> replies, QueueEntry request) {
    replyqueue = replies;
    currentrequest = request;
  }

  
  public void setReply(long id, String content)
  {
    //QueueEntry qe = new QueueEntry(id,content);
      replyqueue.put(id,content);
    
  }


  public QueueEntry getCurrentrequest() {
    return currentrequest;
  }



  
}
