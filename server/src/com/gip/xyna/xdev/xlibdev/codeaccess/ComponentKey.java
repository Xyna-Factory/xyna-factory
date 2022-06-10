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
package com.gip.xyna.xdev.xlibdev.codeaccess;

import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.ComponentType;


public class ComponentKey {

  private final String componentName;
  private final ComponentType compType;
  
  public ComponentKey(String componentName, ComponentType compType) {
    this.componentName = componentName;
    this.compType = compType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((compType == null) ? 0 : compType.hashCode());
    result = prime * result + ((componentName == null) ? 0 : componentName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ComponentKey other = (ComponentKey) obj;
    if (compType != other.compType)
      return false;
    if (componentName == null) {
      if (other.componentName != null)
        return false;
    }
    else if (!componentName.equals(other.componentName))
      return false;
    return true;
  }
  
  @Override
  public String toString() {
    return compType.toString() + " '" + componentName + "'";
  }
}
