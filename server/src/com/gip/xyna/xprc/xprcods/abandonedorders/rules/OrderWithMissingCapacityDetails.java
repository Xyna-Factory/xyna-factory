/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xprcods.abandonedorders.rules;



import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetails;



public class OrderWithMissingCapacityDetails extends AbandonedOrderDetails {

  private static final long serialVersionUID = 7794878631752412243L;
  private String orderType;
  private String capacityName;


  public OrderWithMissingCapacityDetails(String capacityName, String orderType, long orderID, long rootOrderID) {
    super(orderID, rootOrderID);
    this.orderType = orderType;
    this.capacityName = capacityName;
  }


  public String getOrderType() {
    return orderType;
  }


  public String getCapacityName() {
    return capacityName;
  }

}
