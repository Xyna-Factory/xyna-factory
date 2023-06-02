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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * class wrapping data of an oracle AQ message;
 * intended to be used for dequeued messages;
 * for enqueueing subclasses with additional properties have to be used
 *
 */
public class OracleAQMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  protected String corrID = null;
  protected String text = null;
  protected Integer priority = null;
  protected String msgId;
  protected long enqueueTime;
  protected Map<String, Object> properties;

  protected OracleAQMessage() {
  }
  
  public OracleAQMessage(String corrID, String text) {
    this.corrID = corrID;
    this.text = text;
  }

  public OracleAQMessage(String corrID, String text, Integer priority) {
    this.corrID = corrID;
    this.text = text;
    this.priority = priority;
  }

  public OracleAQMessage(OracleAQMessage msg) {
    this.corrID = msg.corrID;
    this.text = msg.text;
    this.priority = msg.priority;
  }

  public String getCorrelationID() {
    return corrID;
  }

  public String getText() {
    return text;
  }

  public Integer getPriority() {
    return priority;
  }

  public boolean isPrioritySet() {
    return (priority != null);
  }
  
  public String getMsgId() {
    return msgId;
  }
  
  public long getEnqueueTime() {
    return enqueueTime;
  }
  
  public boolean hasProperties() {
    return properties != null && properties.size() != 0;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public Object getProperty(String key) {
    if( properties == null ) {
      return null;
    }
    return properties.get(key);
  }
  
  public String getStringProperty(String key) {
    if( properties == null ) {
      return null;
    }
    Object val = properties.get(key);
    if( val == null ) {
      return null;
    } else {
      return String.valueOf(properties.get(key));
    }
  }
 
  @Override
  public String toString() {
     return "OracleAQMessage("
         +corrID+","
         +((text == null || text.length() <100 ) ? text : text.substring(0,100)+"..." )+","
         +priority+","
         +msgId+","
         +enqueueTime+","
         +properties
         +")";
  }
  
  public static Builder newOracleAQMessage() {
    return new Builder(new OracleAQMessage());
  }
  
  public static Builder newOracleAQMessage(OracleAQMessage msg) {
    return new Builder(new OracleAQMessage(msg));
  }
 
  public static class Builder {
    OracleAQMessage msg;
    HashMap<String,Object> properties;  
    
    public Builder(OracleAQMessage msg) {
      this.msg = msg;
      if( msg.hasProperties() ) {
        properties = new HashMap<String,Object>(msg.getProperties());
      }
    }
    
    public OracleAQMessage build() {
      return msg;
    }
     
    public Builder corrID(String corrID) {
      msg.corrID = corrID;
      return this;
    }
    
    public Builder text(String text) {
      msg.text = text;
      return this;
    }

    public Builder priority(Integer priority) {
      msg.priority = priority;
      return this;
    }

    public Builder msgId(String msgId) {
      msg.msgId = msgId;
      return this;
    }

    public Builder enqueueTime(long enqueueTime) {
      msg.enqueueTime = enqueueTime;
      return this;
    }

    public Builder addProperty(String key, Object value) {
      if( properties == null ) {
        properties = new HashMap<String,Object>();
        msg.properties = Collections.unmodifiableMap(properties);
      }
      properties.put(key,value);
      return this;
    }
    
  }
  
}
