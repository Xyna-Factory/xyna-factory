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
package com.gip.xyna.xact.trigger.oracleaq.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



/**
 *
 */
public class EnqueueOptions {
  
  private String queueName;
  private Integer delay = Integer.valueOf(0); //entspricht dem Default DBMS_AQ.NO_DELAY
  private Integer expiration = Integer.valueOf(-1); //entspricht dem Default DBMS_AQ.NEVER
  private Integer defaultPriority = 5;
  private String consumerName;
  private List<String> additional; 
  
  private EnqueueOptions() {
    additional = Collections.emptyList();
  }
  
  public EnqueueOptions(String queueName) {
    this.queueName = queueName;
    additional = Collections.emptyList();
  }
  
  public EnqueueOptions(String queueName, Integer delay, Integer expiration, Integer defaultPriority) {
    this.queueName = queueName;
    this.delay = delay;
    this.expiration = expiration;
    this.defaultPriority = defaultPriority;
    additional = Collections.emptyList();
  }

  public EnqueueOptions(EnqueueOptions data) {
    this.queueName = data.queueName;
    this.delay = data.delay;
    this.expiration = data.expiration;
    this.defaultPriority = data.defaultPriority;
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
  
  public Integer getDelay() {
    return delay;
  }

  public Integer getExpiration() {
    return expiration;
  }

  public Integer getDefaultPriority() {
    return defaultPriority;
  }

  public Integer getPriority(Integer priority) {
    return priority != null ? priority : defaultPriority;
  }

  public String getConsumerName() {
    return consumerName;
  }
  
  public List<String> getAdditional() {
    return additional;
  }  
  
  public static EnqueueOptionsBuilder newEnqueueOptions() {
    return new EnqueueOptionsBuilder();
  }

  public static EnqueueOptionsBuilder newEnqueueOptions(String queueName) {
    return new EnqueueOptionsBuilder().queueName(queueName);
  }

  public static class EnqueueOptionsBuilder {
    EnqueueOptions data = new EnqueueOptions();
    
    public EnqueueOptionsBuilder queueName(String queueName) {
      data.queueName = queueName;
      return this;
    }
    
    public EnqueueOptionsBuilder delay(Integer delay) {
      if( delay != null && delay.intValue() >= 0 ) {
        data.delay = delay;
      }
      return this;
    }
    
    public EnqueueOptionsBuilder expiration(Integer expiration) {
      if( expiration != null && expiration.intValue() >= -1 ) {
        data.expiration = expiration;
      }
      return this;
    }
    
    public EnqueueOptionsBuilder defaultPriority(Integer defaultPriority) {
      if( defaultPriority != null ) {
        data.defaultPriority = defaultPriority;
      }
      return this;
    }
    
    public EnqueueOptionsBuilder consumerName(String consumerName) {
      data.consumerName = consumerName;
      return this;
    }
    
    public EnqueueOptionsBuilder additional(List<String> additional) {
      if( additional == null ) {
        data.additional = null;
      } else {
        data.additional = Collections.unmodifiableList( new ArrayList<String>(additional));
      }
      return this;
    }

    public EnqueueOptions build() {
      if( data.queueName == null || data.queueName.length() == 0 ) {
        throw new IllegalArgumentException( "queueName must not be empty");
      }
      if( data.delay == null || data.delay.intValue() < 0 ) {
        throw new IllegalArgumentException( "delay must not be null or < 0");
      }
      if( data.expiration == null || data.delay.intValue() < -1 ) {
        throw new IllegalArgumentException( "expiration must not be null or < -1");
      }
      if( data.defaultPriority == null ) {
        throw new IllegalArgumentException( "defaultPriority must not be null");
      }
      if( data.additional == null ) {
        data.additional = Collections.emptyList();
      }
      return new EnqueueOptions(data);
    }


  

   
    
  }

  
  
}
