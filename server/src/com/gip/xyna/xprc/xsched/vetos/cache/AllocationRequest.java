/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xsched.vetos.cache;

import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;


public class AllocationRequest {
  
  public static enum VetoType {
    EXCLUSIVE, SHARED
  }
  
  public static enum PendingType {
    PENDING, NON_PENDING
  }
  
  public static enum AllocationMode {
    CREATE, UPDATE, KEEP, UNKNOWN
  }
  
  
  private final String vetoName;
  private final VetoType vetoType;
  private final OrderInformation orderInformation;
  private VetoAllocationResult result;
  private VetoCacheEntry cacheEntry;
  private PendingType pendingType = PendingType.NON_PENDING;
  private AllocationMode allocationMode = AllocationMode.UNKNOWN;
  
  
  public AllocationRequest(String vetoNameIn, VetoType vetoTypeIn, OrderInformation orderInformationIn) {
    String vetoName = ((vetoNameIn == null) ? "__UNKNOWN__" : vetoNameIn);
    VetoType vetoType = ((vetoTypeIn == null) ? VetoType.EXCLUSIVE : vetoTypeIn);
    OrderInformation orderInformation = orderInformationIn;
    if ((orderInformation == null) || (orderInformation.getOrderId() == null)) {
      orderInformation = new OrderInformation(-100L);
    }
    this.vetoName = vetoName;
    this.vetoType = vetoType;
    this.orderInformation= orderInformation;
  }

  
  public VetoAllocationResult getResult() {
    return result;
  }

  
  public void setResult(VetoAllocationResult result) {
    this.result = result;
  }

  
  public VetoCacheEntry getCacheEntry() {
    return cacheEntry;
  }

  
  public void setCacheEntry(VetoCacheEntry cacheEntry) {
    this.cacheEntry = cacheEntry;
  }

  
  public PendingType getPendingType() {
    return pendingType;
  }

  
  public void setPendingType(PendingType pendingType) {
    this.pendingType = pendingType;
  }

  
  public String getVetoName() {
    return vetoName;
  }

  
  public VetoType getVetoType() {
    return vetoType;
  }


  public AllocationMode getAllocationMode() {
    return allocationMode;
  }


  public void setAllocationMode(AllocationMode allocationMode) {
    this.allocationMode = allocationMode;
  }


  public OrderInformation getOrderInformation() {
    return orderInformation;
  }
  
}
