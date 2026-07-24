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
package com.gip.xyna.xprc.xsched.vetos;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;

public class VetoInformation implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String name;
  private final OrderInformation usingOrder;
  private final List<Long> sharedOrderIds;
  private final Long pendingExclusiveOrderId;
  private final boolean administrative;
  private final int binding;
  private final Long created;
  private String documentation;
  
  
  public VetoInformation(AdministrativeVeto administrativeVeto, Long created, int binding) {
    this.name = administrativeVeto.getName();
    this.usingOrder = null;
    this.sharedOrderIds = Collections.emptyList();
    this.pendingExclusiveOrderId = null;
    this.administrative = true;
    this.binding =  binding;
    this.created = created;
    this.documentation = administrativeVeto.getDocumentation();
  }

  public VetoInformation(String name, OrderInformation usingOrder, Long created, int binding) {
    this.name = name;
    this.usingOrder = usingOrder;
    this.sharedOrderIds = Collections.emptyList();
    this.pendingExclusiveOrderId = null;
    this.administrative = false;
    this.binding =  binding;
    this.created = created;
  }

  public VetoInformation(String name, Long pendingExclusiveOrderId, Long created, int binding) {
    this.name = name;
    this.usingOrder = null;
    this.sharedOrderIds = Collections.emptyList();
    this.pendingExclusiveOrderId = pendingExclusiveOrderId;
    this.administrative = false;
    this.binding =  binding;
    this.created = created;
  }

  public VetoInformation(String name, List<Long> sharedOrderIds, Long created, int binding) {
    this.name = name;
    this.usingOrder = null;
    this.sharedOrderIds = sharedOrderIds;
    this.pendingExclusiveOrderId = null;
    this.administrative = false;
    this.binding =  binding;
    this.created = created;
  }

  public VetoInformation(String name, OrderInformation usingOrder, List<Long> sharedOrderIds, Long pendingExclusiveOrderId, String documentation, Long created, int binding) {
    this.name = name;
    this.usingOrder = usingOrder;
    this.sharedOrderIds = sharedOrderIds;
    this.pendingExclusiveOrderId = pendingExclusiveOrderId;
    this.documentation = documentation;
    this.administrative = AdministrativeVeto.ADMIN_VETO_ORDERID.equals(usingOrder.getOrderId());
    this.binding = binding;
    this.created = created;
  }
  
  public VetoInformation(String name) {
    this.name = name;
    this.usingOrder = null;
    this.sharedOrderIds = Collections.emptyList();
    this.pendingExclusiveOrderId = null;
    this.administrative = false;
    this.binding = 0;
    this.created = null;
  }
  
  @Override
  public String toString() {
    if( administrative ) {
      return "VetoInformation("+name+": "+documentation+": "+created+")";
    } else {
      String identifier = binding != 0 ? binding+"-"+name : name;
      if (isAllocatedExclusive()) {
        return "VetoInformation("+identifier+": allocated exclusive to "+usingOrder+": "+created+")";
      } else if (isAllocatedShared()) {
        return "VetoInformation("+identifier+": allocated shared by "+sharedOrderIds+": "+created+")";
      } else if (isPendingExclusiveAllocation()) {
        return "VetoInformation("+identifier+": pending exclusive allocation to "+pendingExclusiveOrderId+": "+created+")";
      } else {
        return "VetoInformation("+identifier+": unallocated: "+created+")";
      }
    }
  } 
  
  public OrderInformation getOrderInformation() {
    if( usingOrder != null ) {
      return usingOrder;
    } else {
      return AdministrativeVeto.ADMIN_VETO_ORDER_INFORMATION;
    }
  }
  

  public Long getUsingOrderId() {
    if( usingOrder != null ) {
      return usingOrder.getOrderId();
    } else {
      return AdministrativeVeto.ADMIN_VETO_ORDERID;
    }
  }
  
  public Long getUsingRootOrderId() {
    if( usingOrder != null ) {
      return usingOrder.getRootOrderId();
    } else {
      return AdministrativeVeto.ADMIN_VETO_ORDERID;
    }
  }

  public boolean isAdministrative() {
    return administrative;
  }
  
  public String getName() {
    return name;
  }

  //package private
  void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  public String getDocumentation() {
    if( administrative ) {
      return documentation;
    } else {
      return usingOrder.getRuntimeContext();
    }
  }
  
  public String getUsingOrderType() {
    if( usingOrder != null ) {
      return usingOrder.getOrderType();
    } else {
      return AdministrativeVeto.ADMIN_VETO_ORDERTYPE;
    }
  }
  
  public Long getCreated() {
    return created;
  }

  public int getBinding() {
    return binding;
  }

  public OrderInformation getUsingOrder() {
    return usingOrder;
  }

  public List<Long> getSharedOrderIds() {
    return sharedOrderIds;
  }

  public Long getPendingExclusiveOrderId() {
    return pendingExclusiveOrderId;
  }

  public boolean isAllocatedExclusive() {
    return usingOrder != null && sharedOrderIds.isEmpty() && pendingExclusiveOrderId == null;
  }

  public boolean isAllocatedShared() {
    return usingOrder == null && !sharedOrderIds.isEmpty() && pendingExclusiveOrderId == null;
  }

  public boolean isPendingExclusiveAllocation() {
    return usingOrder == null && pendingExclusiveOrderId != null;
  }

  public static Transformation<VetoInformation, String> extractName = new Transformation<VetoInformation, String>() {

    @Override
    public String transform(VetoInformation from) {
      return from.getName();
    }
    
  };

}
