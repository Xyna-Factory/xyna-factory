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
package com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization;

import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


/**
 *
 */
public class SuspensionCause_Await extends SuspensionCause {
  private static final long serialVersionUID = 1L;
  
  private boolean needToFreeCapacities;

  public SuspensionCause_Await(boolean freeCaps) {
    this.needToFreeCapacities = freeCaps;
  }

  @Override
  public String getName() {
    return "AWAITING_SYNCHRONIZATION";
  }
  
  @Override
  public boolean needToFreeCapacities() {
    return needToFreeCapacities;
  }

}
