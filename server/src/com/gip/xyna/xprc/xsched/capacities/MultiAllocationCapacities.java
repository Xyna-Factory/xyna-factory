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
 * MultiAllocationCapacities wird im CapacityManagement bzw. genauer in {@link CMAbstract#allocateCapacities(com.gip.xyna.xprc.xsched.scheduling.OrderInformation, com.gip.xyna.xprc.xsched.SchedulingData)}
 * verwendet, um Capacities mehrfach allokieren zu können. 
 * 
 * Mehrfache bedeutet hier nicht, dass die Kardinalität einer Capacity höher ist als 1, sondern dass die 
 * gesamte CapacityGruppe mehrfach allokiert wird.
 * 
 * Beispiel: MultiAllocationCapacities ({(A,1),(B,2)},min=3,max=5) möchte eine Capacitygruppe mindestens 
 * 3 mal und maximal 5 allokieren, die Capacitygruppe besteht dabei aus 1 mal "A" und 2 mal "B".
 * <pre>
 * Vorhanden -----&gt; allokiert                Kommentar
 * A    B           A     B    allocations  
 * 20   20          5     10             5   genügend vorhanden, um max=5 zu erhalten
 * 20    9          4      8             4   reicht nur noch, um Gruppe 4 mal zu erhalten
 * 4    20          4      8             4   reicht nur noch, um Gruppe 4 mal zu erhalten
 * 10    6          3      6             3   reicht nur noch, um Gruppe 3 mal zu erhalten
 * 10    5          0      0             0   reicht nicht mehr, um min=3 zu erfüllen -&gt; Auftrag wird nicht geschedult. 
 * </pre>
 * 
 * Über das Flag "transferable" können die allokierten Capacities  
 */
public class MultiAllocationCapacities implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private List<Capacity> capacities;
  private int minAllocation = 1;
  private int maxAllocation = Integer.MAX_VALUE;
  private int allocations;   //wie häufig konnte allokiert werden? 
  private boolean transferable; //werden in CapacityCache als transferierbar markiert
  //private String orderType ?,

  public MultiAllocationCapacities() {
  }
  
  public MultiAllocationCapacities(List<Capacity> capacities) {
    this.capacities = new ArrayList<Capacity>(capacities);
  }
  public MultiAllocationCapacities(Capacity ... capacities) {
    this(Arrays.asList(capacities));
  }

  @Override
  public String toString() {
    return "MultiAllocationCapacities("+capacities+","+allocations+",min="+minAllocation+",max="+maxAllocation+")";
  }
  
  
  public List<Capacity> getCapacities() {
    return capacities;
  }

  
  public void setCapacities(List<Capacity> capacities) {
    this.capacities = new ArrayList<Capacity>(capacities);
  }

  
  public int getMinAllocation() {
    return minAllocation;
  }

  
  public void setMinAllocation(int minAllocation) {
    this.minAllocation = minAllocation;
  }

  
  public int getMaxAllocation() {
    return maxAllocation;
  }

  
  public void setMaxAllocation(int maxAllocation) {
    this.maxAllocation = maxAllocation;
  }

  
  public int getAllocations() {
    return allocations;
  }

  
  public void setAllocations(int allocations) {
    this.allocations = allocations;
  }

  
  public boolean isTransferable() {
    return transferable;
  }

  
  public void setTransferable(boolean transferable) {
    this.transferable = transferable;
  }

}
