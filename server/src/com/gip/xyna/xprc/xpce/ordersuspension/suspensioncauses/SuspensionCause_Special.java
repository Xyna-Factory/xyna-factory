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
package com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses;


/**
 *
 */
public class SuspensionCause_Special extends SuspensionCause {
  private static final long serialVersionUID = 1L;

  private final boolean needToFreeCapacities;
  private final boolean needToFreeVetos;
   

  public SuspensionCause_Special() {
    this.needToFreeCapacities = false;
    this.needToFreeVetos = false;
  }
  
  public SuspensionCause_Special(boolean needToFreeCapacities) {
    this.needToFreeCapacities = needToFreeCapacities;
    this.needToFreeVetos = false;
  }

  public SuspensionCause_Special(boolean needToFreeCapacities, boolean needToFreeVetos) {
    this.needToFreeCapacities = needToFreeCapacities;
    this.needToFreeVetos = needToFreeVetos;
  }
  
  @Override
  public String getName() {
    return "SPECIAL";
  }
  
  public boolean needToFreeCapacities() {
    return needToFreeCapacities; 
  }
  
  public boolean needToFreeVetos() {
    return needToFreeVetos;
  }

}
