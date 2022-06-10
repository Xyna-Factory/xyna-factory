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

package com.gip.xyna.xprc.xprcods.abandonedorders;



import java.io.Serializable;



public class AbandonedOrderDetails implements Serializable {

  private static final long serialVersionUID = 1L;

  private final long orderID;
  private final long rootOrderID;
  private final long displayID;


  public AbandonedOrderDetails(long orderID, long rootOrderID) {
    this(orderID, rootOrderID, rootOrderID);
  }


  public AbandonedOrderDetails(long orderID, long rootOrderID, long displayID) {
    this.orderID = orderID;
    this.rootOrderID = rootOrderID;
    this.displayID = displayID;
  }


  public long getOrderID() {
    return this.orderID;
  }


  public long getRootOrderID() {
    return this.rootOrderID;
  }


  public long getDisplayID() {
    return this.displayID;
  }
}
