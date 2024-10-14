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
package com.gip.xyna.xprc.xsched.orderseries;

import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable.OrderStatus;



/**
 *
 */
public class RescheduleSeriesOrderInformation {
    
  public enum Problem {
    MissingSeriesInformation, MissingSeriesInformation_UnreadableXynaOrder, 
    DuplicateCorrelationId, OrderNotFound, UnexpectedOrderState, MissingPredecessors, 
    WaitingPredecessors, OrderSeemsRunning, OrderAlreadyFinished, OtherBinding,
    Unimplemented };
    
  public enum Solution { 
    Disappeared, OrderFinished, OrderStarted, None, SuccessorsStarted, 
    MissingSeriesInformationInserted, RemovedFromOSM, NoProblem };
  
  private OrderStatus orderStatus;
  private Problem problem;
  private Solution solution;
  private String comment;
  private String info;
  private int binding;
  private String correlationId;
  
  public OrderStatus getOrderStatus() {
    return orderStatus;
  }
  
  public void setOrderStatus(OrderStatus orderStatus) {
    this.orderStatus = orderStatus;
  }
  
  public Problem getProblem() {
    return problem;
  }
  
  public void setProblem(Problem problem) {
    this.problem = problem;
  }
  
  public Solution getSolution() {
    return solution;
  }
  
  public void setSolution(Solution solution) {
    this.solution = solution;
  }
  
  public String getComment() {
    return comment;
  }
  
  public void setComment(String comment) {
    this.comment = comment;
  }
  
  public String getInfo() {
    return info;
  }
  
  public void setInfo(String info) {
    this.info = info;
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
 
}
