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
package com.gip.xyna.xprc;



import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



public class ThreadPoolStatistics {

  private int activeCount;
  private int currentPoolSize;
  private long completedTasks;
  private int corePoolSize; // werden nicht durch keep alive aufgeräumt, entspricht minPoolSize
  private int maxPoolSize;
  private int largestPoolSize;
  private long keepAliveSeconds;
  private int maxQueueSize;
  private int currentQueueSize;
  private String id;
  private String queueType;
  private long rejectedTasks;


  public ThreadPoolStatistics(String id, ThreadPoolExecutor tpe, int maxQueueSize) {
    this.id = id;
    activeCount = tpe.getActiveCount();
    currentPoolSize = tpe.getPoolSize();
    completedTasks = tpe.getCompletedTaskCount();
    largestPoolSize = tpe.getLargestPoolSize();
    maxPoolSize = tpe.getMaximumPoolSize();
    corePoolSize = tpe.getCorePoolSize();
    // tpe.getTaskCount();
    keepAliveSeconds = tpe.getKeepAliveTime(TimeUnit.SECONDS);
    if (maxQueueSize > 0) {
      currentQueueSize = tpe.getQueue().size();
    } else {
      currentQueueSize = 0;
    }
    this.maxQueueSize = maxQueueSize;
    rejectedTasks = 0;
    if (tpe instanceof XynaThreadPoolExecutor) {
      XynaThreadPoolExecutor xtpe = (XynaThreadPoolExecutor) tpe;
      rejectedTasks = xtpe.getRejectedTasks();
      queueType = xtpe.isRingBuffer() ? "ringBuffer" : "default";
    } else {
      this.queueType = "default";
    }

  }


  public int getCorePoolSize() {
    return corePoolSize;
  }


  public int getActiveCount() {
    return activeCount;
  }


  public int getCurrentPoolSize() {
    return currentPoolSize;
  }


  public long getCompletedTasks() {
    return completedTasks;
  }


  public int getMaxPoolSize() {
    return maxPoolSize;
  }


  public int getLargestPoolSize() {
    return largestPoolSize;
  }


  public long getKeepAliveSeconds() {
    return keepAliveSeconds;
  }


  public int getMaxQueueSize() {
    return maxQueueSize;
  }


  public int getCurrentQueueSize() {
    return currentQueueSize;
  }


  public String getId() {
    return id;
  }


  public String getQueueType() {
    return this.queueType;
  }


  public long getRejectedTasks() {
    return this.rejectedTasks;
  }

}
