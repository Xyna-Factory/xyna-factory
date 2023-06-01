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
package com.gip.xyna.xact.trigger.oracleaq.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



/**
 *
 */
public class DequeueOptions {
  
  private String queueName;
  private Integer timeout = 10;  // DBMS_AQ.FOREVER = -1, DBMS_AQ.NOWAIT = 0
  private String dequeueCondition;
  private String consumerName;
  private List<String> additional;
 
  
  private DequeueOptions() {
    additional = Collections.emptyList();
  }

  public DequeueOptions(String queueName) {
    this.queueName = queueName;
    additional = Collections.emptyList();
  }
  public DequeueOptions(String queueName, String consumerName) {
    this.queueName = queueName;
    this.consumerName = consumerName;
    additional = Collections.emptyList();
  }

  public DequeueOptions(DequeueOptions data) {
    this.queueName = data.queueName;
    this.dequeueCondition = data.dequeueCondition;
    this.timeout = data.timeout;
    this.consumerName = data.consumerName;
    if( data.additional == null || data.additional.isEmpty() ) {
      this.additional = Collections.emptyList();
    } else {
      this.additional = Collections.unmodifiableList( new ArrayList<String>(data.additional));
    }
  }

  public String getQueueName() {
    return queueName;
  }
  
  public Integer getTimeout() {
    return timeout;
  }
  
  public String getDequeueCondition() {
    return dequeueCondition;
  }

  public String getConsumerName() {
    return consumerName;
  }
  
  public List<String> getAdditional() {
    return additional;
  }
  
  public static DequeueOptionsBuilder newDequeueOptions() {
    return new DequeueOptionsBuilder();
  }

  public static DequeueOptionsBuilder newDequeueOptions(String queueName) {
    return new DequeueOptionsBuilder().queueName(queueName);
  }

  public static class DequeueOptionsBuilder {
    protected DequeueOptions data = new DequeueOptions();
 
    public DequeueOptionsBuilder queueName(String queueName) {
      data.queueName = queueName;
      return this;
    }
    
    public DequeueOptionsBuilder dequeueCondition(String dequeueCondition) {
      data.dequeueCondition = dequeueCondition;
      return this;
    }
    
    public DequeueOptionsBuilder dequeueCondition_CorrId(String corrId) {
      if( corrId == null ) {
        data.dequeueCondition = "corrId = ''";
      } else {
        data.dequeueCondition = "corrId = '" + corrId + "'";
      }
      return this;
    }
    
    public DequeueOptionsBuilder timeout(Integer timeout) {
      if( timeout != null && timeout.intValue() >= -1 ) {
        data.timeout = timeout;
      }
      return this;
    }
    
    public DequeueOptionsBuilder consumerName(String consumerName) {
      data.consumerName = consumerName;
      return this;
    }
    
    public DequeueOptionsBuilder additional(List<String> additional) {
      if( additional == null ) {
        data.additional = null;
      } else {
        data.additional = Collections.unmodifiableList( new ArrayList<String>(additional));
      }
      return this;
    }

    public final DequeueOptions build() {
      if( data.queueName == null || data.queueName.length() == 0 ) {
        throw new IllegalArgumentException( "queueName must not be empty");
      }
      if( data.timeout == null || data.timeout.intValue() < -1 ) {
        throw new IllegalArgumentException( "timeout must not be null or < -1");
      }
      if( data.additional == null ) {
        data.additional = Collections.emptyList();
      }
      return new DequeueOptions(data);
    }

  }

}
