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
package com.gip.xyna.xprc;

import java.io.Serializable;

import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;


/**
 * Immutable!
 *
 */
public class XynaOrderInfo implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private Long orderId;
  private DestinationKey destinationKey;
  private OrderInstanceStatus status;
  private Long rootOrderId;
  
  public XynaOrderInfo(SchedulingOrder so) {
    this.orderId = so.getOrderId();
    this.rootOrderId = so.getRootOrderId();
    this.destinationKey = so.getDestinationKey();
    this.status = so.getOrderStatus();
  }

  public XynaOrderInfo(XynaOrderServerExtension xo, OrderInstanceStatus status) {
    this.orderId = xo.getId();
    this.rootOrderId = xo.getRootOrder().getId();
    this.destinationKey = xo.getDestinationKey();
    this.status = status;
  }
  

  public Long getOrderId() {
    return orderId;
  }
  
  public OrderInstanceStatus getStatus() {
    return status;
  }
  
  public Long getRootOrderId() {
    return rootOrderId;
  }
  
  public DestinationKey getDestinationKey() {
    return destinationKey;
  }
  
}
