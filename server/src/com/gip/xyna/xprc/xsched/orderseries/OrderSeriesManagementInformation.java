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
package com.gip.xyna.xprc.xsched.orderseries;

import java.util.List;

import com.gip.xyna.utils.collections.Pair;


/**
 *
 */
public class OrderSeriesManagementInformation {

  /**
   *
   */
  public static class WaitingOrder {
    
    public static final long UNKNOWN_ID = Long.MIN_VALUE;

    private long id = UNKNOWN_ID;
    private int binding;
    private String correlationId;
    private List<WaitingOrder> predecessors;
      
    public List<WaitingOrder> getPredecessors() {
      return predecessors;
    }
    
    public void setPredecessors(List<WaitingOrder> predecessors) {
      this.predecessors = predecessors;
    }

    public int getBinding() {
      return binding;
    }
    
    public void setBinding(int binding) {
      this.binding = binding;
    }
    
    public String getCorrelationId() {
      return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
      this.correlationId = correlationId;
    }

    public long getId() {
      return id;
    }

    public void setId(long id) {
      this.id = id;
    }

  }

  /**
    *
   */
  public enum Mode {
    Basic, Orders, Predecessors;
  }
  
  public static enum TaskConsumerState { 
    Running, Paused, Finished;
  }
  
  private int waitingOrders; //Anzahl der im OrderSeriesManagement wartenden Auftr�ge
  private int readyOrders;   //Anzahl der im OrderSeriesManagement noch nicht eingestellten lauff�higen Auftr�ge
  private int predecessorTreesSize; //Anzahl der gecachten Predecessor-Informationen
  
  private int cacheSize;  //Gr��e des SeriesInformationStorable-Caches
  private int currentTasks; //aktuelle Anzahl wartender Tasks in der Queue 
  
  private List<WaitingOrder> waitingOrderList;
  private TaskConsumerState taskConsumerState;
  private List<Pair<String, Integer>> tasksCount;
  private List<String> failedTasks;
  
  
  
  public int getPredecessorTreesSize() {
    return predecessorTreesSize;
  }

  public void setPredecessorTreesSize(int predecessorTreesSize) {
    this.predecessorTreesSize = predecessorTreesSize;
  }
  
  public List<WaitingOrder> getWaitingOrderList() {
    return waitingOrderList;
  }

  public void setWaitingOrderList(List<WaitingOrder> waitingOrderList) {
    this.waitingOrderList = waitingOrderList;
  }

  public int getWaitingOrders() {
    return waitingOrders;
  }
  
  public void setWaitingOrders(int waitingOrders) {
    this.waitingOrders = waitingOrders;
  }
  
  public int getReadyOrders() {
    return readyOrders;
  }
  
  public void setReadyOrders(int readyOrders) {
    this.readyOrders = readyOrders;
  }
  
  public int getCacheSize() {
    return cacheSize;
  }
  
  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }
  
  public int getCurrentTasks() {
    return currentTasks;
  }
  
  public void setCurrentTasks(int currentTasks) {
    this.currentTasks = currentTasks;
  }

  public void setTaskConsumerState(TaskConsumerState taskConsumerState) {
    this.taskConsumerState = taskConsumerState;
  }

  public TaskConsumerState getTaskConsumerState() {
    return taskConsumerState;
  }


  public void setTasksCount(List<Pair<String, Integer>> tasksCount) {
    this.tasksCount = tasksCount;
  }

  public List<Pair<String, Integer>> getTasksCount() {
    return tasksCount;
  }
  
  public void setFailedTasks(List<String> failedTasks) {
    this.failedTasks = failedTasks;
  }
  
  public List<String> getFailedTasks() {
    return failedTasks;
  }
  
}
