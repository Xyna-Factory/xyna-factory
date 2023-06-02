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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.Serializable;
import java.util.Objects;



public class OrderEntrance implements Serializable {

  private static final long serialVersionUID = 1L;

  public static enum OrderEntranceType {
    RMI(true),
    CLI(true),
    triggerInstance(false),
    filterInstance(false),
    custom(false);
    
    // is it a global setting for the runtimeContext or is an additional qualifier needed for identification
    private final boolean contextGlobal;
    
    private OrderEntranceType(boolean contextGlobal) {
      this.contextGlobal = contextGlobal;
    }
    
    public boolean isContextGlobal() {
      return contextGlobal;
    }
    
  }
  
  protected OrderEntranceType type;
  protected String name;
  
  public OrderEntrance(OrderEntranceType type, String name) {
    this.type = type;
    this.name = name;
  }

  public OrderEntranceType getType() {
    return type;
  }
  
  public String getName() {
    return name;
  }
  
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    OrderEntrance other = (OrderEntrance) obj;
    return Objects.equals(name, other.name) && Objects.equals(type, other.type);
  }
  
  public int hashCode() {
    return Objects.hash(name, type);
  }
  
}
