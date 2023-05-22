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

package com.gip.xyna.xprc.xsched.capacities;

import java.io.Serializable;



public final class CapacityUsageSlotInformation implements Serializable {

  private static final long serialVersionUID = 4570655282553481633L;
  
  private final String capacityName;
  private final Integer privateCapacityIndex;
  private final boolean occupied;

  private final String usingOrderType;
  private final Long usingOrderId;
  private final boolean transferable;

  private final int binding;
  private final int slotIndex;
  private final int maxSlotIndex;


  public CapacityUsageSlotInformation(String capacityName, Integer privateCapacityIndex, boolean occupied,
                               String usingOrderType, Long usingOrderId, boolean transferable, int binding, int slotIndex, int maxSlotIndex) {
    this.capacityName = capacityName;
    this.privateCapacityIndex = privateCapacityIndex;
    this.occupied = occupied;
    this.usingOrderType = usingOrderType;
    this.usingOrderId = usingOrderId;
    this.transferable = transferable;
    this.binding = binding;
    this.slotIndex = slotIndex;
    this.maxSlotIndex = maxSlotIndex;
  }


  public String getCapacityName() {
    return this.capacityName;
  }


  public Integer getPrivateCapacityIndex() {
    return this.privateCapacityIndex;
  }


  public boolean isOccupied() {
    return this.occupied;
  }


  public String getUsingOrderType() {
    return this.usingOrderType;
  }


  public Long getUsingOrderId() {
    return this.usingOrderId;
  }

  public int getBinding() {
    return binding;
  }
  
  public int getSlotIndex() {
    return this.slotIndex;
  }

  public int getMaxSlotIndex() {
    return this.maxSlotIndex;
  }

  public boolean isTransferable() {
    return transferable;
  }
  
}
