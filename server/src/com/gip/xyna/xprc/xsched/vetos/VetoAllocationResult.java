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
package com.gip.xyna.xprc.xsched.vetos;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;

public class VetoAllocationResult {
  public static final VetoAllocationResult SUCCESS = new VetoAllocationResult(true,null);
  public static final VetoAllocationResult FAILED = new VetoAllocationResult(false,OrderInstanceStatus.WAITING_FOR_VETO);
  public static final VetoAllocationResult UNSUPPORTED = new 
      VetoAllocationResult( new VetoInformation(new AdministrativeVeto("vetos currently unsupported","vetos currently unsupported"),0));
  
  private boolean allocated;

  private XynaException xynaException;
  private OrderInstanceStatus status;
  private VetoInformation existingVeto;

  public VetoAllocationResult(boolean allocated,OrderInstanceStatus status) {
    this.allocated = allocated;
    this.status = status;
  }
  
  public VetoAllocationResult(XynaException xynaException) {
    this.allocated = false;
    this.xynaException = xynaException;
  }

  public VetoAllocationResult(VetoInformation existing) {
    this.allocated = false;
    this.existingVeto = existing;
    this.status = existing.isAdministrative() ? OrderInstanceStatus.WAITING_FOR_VETO : OrderInstanceStatus.SCHEDULING_VETO;
  }

  public boolean isAllocated() {
    return allocated;
  }

  public String getVetoName() {
    if( existingVeto != null ) {
      return existingVeto.getName();
    }
    return null;
  }

  @Override
  public String toString() {
    return new StringBuilder("VetoAllocationResult(allocated=").append(allocated).append(",name=").append(getVetoName())
        .append(",holdBy=").append(existingVeto.getUsingOrderId()).append(")").toString();
  }

  public XynaException getXynaException() {
    return xynaException;
  }

  public OrderInstanceStatus getOrderInstanceStatus() {
    return status;
  }

  public VetoInformation getExistingVeto() {
    return existingVeto;
  }

}
