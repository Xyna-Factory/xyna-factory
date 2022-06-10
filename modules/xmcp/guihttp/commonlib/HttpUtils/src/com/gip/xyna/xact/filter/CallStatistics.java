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
package com.gip.xyna.xact.filter;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.utils.collections.queues.RingbufferBlockingQueue;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;

/**
 *
 */
public class CallStatistics {

  private RingbufferBlockingQueue<StatisticsEntry> allRingBuffer; //alle Requests
  private RingbufferBlockingQueue<StatisticsEntry> orderRingBuffer; //nur Requests, die zu XynaOrder führten
  
  
  private int allCounter;
  private int orderCounter;
  
  public CallStatistics(Integer statisticsSize) {
    allRingBuffer = new RingbufferBlockingQueue<StatisticsEntry>(statisticsSize);
    orderRingBuffer = new RingbufferBlockingQueue<StatisticsEntry>(statisticsSize);
  }

  public StatisticsEntry newRequest(String uri, Method method, HTTPTriggerConnection tc) {
    StatisticsEntry se = new StatisticsEntry(uri,method);
    allRingBuffer.offerOrExchange(se);
    ++allCounter;
    //se.addAdditional("header", tc.getHeader().toString());
    se.setCaller(tc.getSocket().getInetAddress().getHostAddress() );
    return se;
  }
  
  
  public static class StatisticsEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private long timestamp;
    private String uri;
    private Method method;
    private HashMap<String,String> additional;
    private String status;
    private String caller;

    public StatisticsEntry(String uri, Method method) {
      this.timestamp = System.currentTimeMillis();
      this.uri = uri;
      this.method = method;
    }
    
    public void addAdditional(String key, String value) {
      if( additional == null ) {
        additional = new HashMap<String, String>();
      }
      additional.put(key, value);
    }

    public long getTimestamp() {
      return timestamp;
    }
    
    public String getUri() {
      return uri;
    }
    
    public Method getMethod() {
      return method;
    }

    public Map<String,String> getAdditional() {
      return additional;
    }
    
    public String getAdditional(String key) {
      if( additional == null ) {
        return null;
      } else {
        return additional.get(key);
      }
    }
    
    public void setStatus(String status) {
      this.status = status;
    }
    
    public String getStatus() {
      return status;
    }

    public void setCaller(String caller) {
      this.caller = caller;
    }
    
    public String getCaller() {
      return caller;
    }
    
  }


  public Collection<StatisticsEntry> getAllStatisticsEntries() {
    return Collections.unmodifiableCollection(allRingBuffer);
  }

  public Collection<StatisticsEntry> getOrderStatisticsEntries() {
    return Collections.unmodifiableCollection(orderRingBuffer);
  }

  public void responsible(StatisticsEntry statisticsEntry) {
    orderRingBuffer.offerOrExchange(statisticsEntry);
    ++orderCounter;
  }
  
  public int getOrderCounter() {
    return orderCounter;
  }
  
  public int getAllCounter() {
    return allCounter;
  }
}
