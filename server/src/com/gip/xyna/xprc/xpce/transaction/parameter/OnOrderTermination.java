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
package com.gip.xyna.xprc.xpce.transaction.parameter;

import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

public final class OnOrderTermination implements DisposalStrategyParameter {
  
  private final Long orderId;
  private final DestinationKey dk;
  
  
  public OnOrderTermination(XynaOrder xo) {
    this(xo.getId(), xo.getDestinationKey());
  }
  
  public OnOrderTermination(Long orderId, DestinationKey dk) {
    this.orderId = orderId;
    this.dk = dk;
  }
  
  public Long getOrderId() {
    return orderId;
  }
  
  public DestinationKey getDestinationKey() {
    return dk;
  }
  
}