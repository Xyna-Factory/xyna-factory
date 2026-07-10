/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xsched.capacities;



public class SharedResourceFreeCapacitiesRequest {

  /* package */ enum CapacityMethod {
    free, force, transferable
  }


  private final CapacityMethod method;
  private final Long orderId;


  public SharedResourceFreeCapacitiesRequest(CapacityMethod method, Long orderId) {
    this.method = method;
    this.orderId = orderId;
  }


  public CapacityMethod getMethod() {
    return method;
  }


  public Long getOrderId() {
    return orderId;
  }


  @Override
  public String toString() {
    return String.format("[%d - %s]", orderId, method);
  }

}
