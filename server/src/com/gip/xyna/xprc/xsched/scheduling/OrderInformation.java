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
package com.gip.xyna.xprc.xsched.scheduling;

import java.io.Serializable;

import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

public class OrderInformation implements Serializable{

  private static final long serialVersionUID = 1L;

  private final Long orderId;
  private final Long rootOrderId;
  private final DestinationKey destinationKey;

  public OrderInformation(XynaOrderServerExtension xo) {
    this.orderId = xo.getId();
    this.rootOrderId = xo.getRootOrder().getId();
    this.destinationKey = xo.getDestinationKey();
  }

  
  public OrderInformation(Long orderId, Long rootOrderId, String orderType) {
    this.orderId = orderId;
    this.rootOrderId = rootOrderId;
    this.destinationKey = new DestinationKey(orderType); //Achtung: RuntimeContext fehlt
  }

  @Override
  public String toString() {
    if( orderId != null && orderId.equals(rootOrderId) ) {
      return "OrderInformation("+orderId+","+destinationKey+")";
    } else {
      return "OrderInformation("+orderId+","+rootOrderId+","+destinationKey+")";
    }
  }

  public Long getOrderId() {
    return orderId;
  }

  public Long getRootOrderId() {
    return rootOrderId;
  }
  
  public String getOrderType() {
    return destinationKey.getOrderType();
  }

  public String getRuntimeContext() {
    return destinationKey.getRuntimeContext().getGUIRepresentation();
  }

  public DestinationKey getDestinationKey() {
    return destinationKey;
  }

}
