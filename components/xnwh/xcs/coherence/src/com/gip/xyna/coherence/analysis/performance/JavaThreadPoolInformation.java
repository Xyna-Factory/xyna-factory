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

package com.gip.xyna.coherence.analysis.performance;



import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



public class JavaThreadPoolInformation extends ThreadPoolInformation {

  private int activeCount;
  private int currentPoolSize;
  private long completedTasks;
  private int corePoolSize; //werden nicht durch keep alive aufger�umt, entspricht minPoolSize
  private int maxPoolSize;
  private int largestPoolSize;
  private long keepAliveSeconds;
  private int maxQueueSize;
  private int currentQueueSize;
  private String id;


  //private String queueType; //TODO

  public JavaThreadPoolInformation(String id, ThreadPoolExecutor tpe, int maxQueueSize) {
    this.id = id;
    activeCount = tpe.getActiveCount();
    currentPoolSize = tpe.getPoolSize();
    completedTasks = tpe.getCompletedTaskCount();
    largestPoolSize = tpe.getLargestPoolSize();
    maxPoolSize = tpe.getMaximumPoolSize();
    corePoolSize = tpe.getCorePoolSize();
    //      tpe.getTaskCount();
    keepAliveSeconds = tpe.getKeepAliveTime(TimeUnit.SECONDS);
    if (maxQueueSize > 0) {
      currentQueueSize = tpe.getQueue().size();
    } else {
      currentQueueSize = 0;
    }
    this.maxQueueSize = maxQueueSize;
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


  private static final String headline =
      "ThreadPools: name, active threads/current poolsize/min poolsize/largest poolsize/max poolsize,"
          + " waiting in queue/max queuesize, keepalive seconds, completed tasks";


  @Override
  public void printInformationInternally(OutputStream target, String encoding) throws IOException {
    target.write(new String(headline).getBytes(encoding));
    String output =
        getId() + ": " + getActiveCount() + "/" + getCurrentPoolSize() + "/" + getCorePoolSize() + "/"
            + getLargestPoolSize() + "/" + getMaxPoolSize() + " " + getCurrentQueueSize() + "/" + getMaxQueueSize()
            + " " + getKeepAliveSeconds() + " " + getCompletedTasks();
    target.write(output.getBytes(encoding));
  }

}
