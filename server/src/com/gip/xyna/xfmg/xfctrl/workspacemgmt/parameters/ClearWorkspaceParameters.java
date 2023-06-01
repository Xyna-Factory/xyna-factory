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
package com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters;

import java.io.Serializable;
import java.util.List;


public class ClearWorkspaceParameters implements Serializable{

  private static final long serialVersionUID = 1L;

  private boolean ignoreRunningOrders;
  List<String> removeSubtypesOf;
  
  
  public boolean isIgnoreRunningOrders() {
    return ignoreRunningOrders;
  }
  
  public void setIgnoreRunningOrders(boolean ignoreRunningOrders) {
    this.ignoreRunningOrders = ignoreRunningOrders;
  }

  
  public List<String> getRemoveSubtypesOf() {
    return removeSubtypesOf;
  }

  
  public void setRemoveSubtypesOf(List<String> removeSubtypesOf) {
    this.removeSubtypesOf = removeSubtypesOf;
  }
  
  
}
