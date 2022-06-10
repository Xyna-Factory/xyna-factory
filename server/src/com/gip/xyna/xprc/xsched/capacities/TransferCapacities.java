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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.xprc.xpce.planning.Capacity;


/**
 * TransferCapacities wird im CapacityManagement bzw. genauer in {@link CMAbstract#allocateCapacities(com.gip.xyna.xprc.xsched.scheduling.OrderInformation, com.gip.xyna.xprc.xsched.SchedulingData)}
 * und {@link CMAbstract#transferCapacities(com.gip.xyna.xprc.XynaOrderServerExtension, TransferCapacities) } verwendet, um Capacities von einem Auftrag auf einen anderen übertragen zu können.
 * <br>
 * Dazu muss die OrderId des Capacity-spendenden Auftrags angegeben werden. Zusätzlich muss der spendende Auftrag seine 
 * Capacities bei der Allozierung als "transferable" markiert haben.  
 *
 */
public class TransferCapacities implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private final List<Capacity> capacities;
  private final long fromOrderId;
  
  public TransferCapacities() {
    capacities = new ArrayList<>();
    fromOrderId = -1;
  }
  
  public TransferCapacities(List<Capacity> capacities) {
    this(0L, capacities );
  }
  public TransferCapacities(Capacity ... capacities) {
    this(0L, Arrays.asList(capacities) );
  }
  
  public TransferCapacities(long fromOrderId, List<Capacity> capacities) {
    this.fromOrderId = fromOrderId;
    this.capacities = new ArrayList<Capacity>(capacities);
  }
  public TransferCapacities(long fromOrderId, Capacity ... capacities) {
    this(fromOrderId, Arrays.asList(capacities) );
  }

  public List<Capacity> getCapacities() {
    return capacities;
  }
  
  public long getFromOrderId() {
    return fromOrderId;
  }
  
}
