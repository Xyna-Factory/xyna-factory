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
package com.gip.xyna.xprc.xpce.manualinteraction;

import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;


/**
 *
 */
public class SuspensionCause_ManualInteraction extends SuspensionCause {
  private static final long serialVersionUID = 1L;
  
  private OrderInstanceStatus oldInstanceStatus;

  public SuspensionCause_ManualInteraction(OrderInstanceStatus oldInstanceStatus) {
    this.oldInstanceStatus = oldInstanceStatus;
  }

  @Override
  public String getName() {
    return "MANUAL_INTERACTION";
  }

  /**
   * @return the oldInstanceStatus
   */
  public OrderInstanceStatus getOldInstanceStatus() {
    return oldInstanceStatus;
  }
  
}
