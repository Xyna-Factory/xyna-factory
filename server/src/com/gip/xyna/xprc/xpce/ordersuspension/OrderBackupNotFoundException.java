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
package com.gip.xyna.xprc.xpce.ordersuspension;

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


/**
 *
 */
public class OrderBackupNotFoundException extends Exception {
  
  private static final long serialVersionUID = 1L;

  private Long orderId;

  public OrderBackupNotFoundException(Long orderId, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
    super(e);
    this.orderId = orderId;
  }

  public OrderBackupNotFoundException(Long orderId) {
    this.orderId = orderId;
  }

  /**
   * @return the orderId
   */
  public Long getOrderId() {
    return orderId;
  }
  
}
