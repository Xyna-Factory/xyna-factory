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

package com.gip.xyna.xprc.xsched.orderseries.tasks;

import java.util.List;

import com.gip.xyna.xprc.xsched.orderseries.OSMInterface.OrderState;

/**
 * OSMTask_ReadyToRun:
 * <br>
 * Falls Auftrag im OrderSeriesManagement nicht gestartet werden kann, wird dieser Task eingestellt,
 * um das Starten nach kurzer Wartezeit erneut zu versuchen.
 */
public class OSMTask_ReadyToRun extends OSMTask {

  private String correlationId;
  private Long orderId;
  private OrderState orderState;
  private List<String> cycle;

  
  /**
   * @param orderId
   * @param orderState
   * @param cycle
   */
  OSMTask_ReadyToRun(String correlationId, Long orderId, OrderState orderState, List<String> cycle) {
    this.correlationId = correlationId;
    this.orderId = orderId;
    this.orderState = orderState;
    this.cycle = cycle;
  }

  @Override
  public String getCorrelationId() {
    return correlationId;
  }

  @Override
  protected void executeInternal() {
    osm.readyToRun(correlationId, orderId, orderState, cycle);
  }

}
