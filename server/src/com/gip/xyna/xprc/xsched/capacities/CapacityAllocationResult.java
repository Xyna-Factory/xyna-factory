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

package com.gip.xyna.xprc.xsched.capacities;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



public class CapacityAllocationResult {

  public static final CapacityAllocationResult SUCCESS = new CapacityAllocationResult(true);

  private boolean allocated;
  private int demand = -1;
  private int freeCardinality = -1;
  private String capName;
  private XynaException xynaException = null;
  private OrderInstanceStatus status;

  private CapacityAllocationResult(boolean allocated) {
    this.allocated = allocated;
    this.capName = "";
    this.status = null;
  }

  public CapacityAllocationResult(String capName) {
    this.allocated = false;
    this.capName = capName;
    this.status = null;
  }

  public CapacityAllocationResult(String capName, int demand, int freeCardinality, boolean waiting) {
    this.allocated = false;
    this.capName = capName;
    this.demand = demand;
    this.freeCardinality = freeCardinality;
    this.status = waiting ? OrderInstanceStatus.WAITING_FOR_CAPACITY : OrderInstanceStatus.SCHEDULING_CAPACITY;
  }

  public CapacityAllocationResult(String capName, XynaException xynaException) {
    this.allocated = false;
    this.capName = capName;
    this.xynaException = xynaException;
  }


  public boolean isAllocated() {
    return allocated;
  }


  public int getFreeCardinality() {
    return freeCardinality;
  }

  
  public int getDemand() {
    return demand;
  }


  public String getCapName() {
    return capName;
  }

  @Override
  public String toString() {
    return "CapacityAllocationResult("+allocated+","+capName+")";
  }

  public XynaException getXynaException() {
    return xynaException;
  }

  public OrderInstanceStatus getOrderInstanceStatus() {
    return status;
  }
  
}
