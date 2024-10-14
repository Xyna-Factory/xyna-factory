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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt;

import java.io.Serializable;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;

public class ListRuntimeDependencyContextParameter implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private final boolean selectProblems;
  private final Set<RuntimeDependencyContextType> selectedTypes;
  
  public ListRuntimeDependencyContextParameter(boolean selectProblems, Set<RuntimeDependencyContextType> selectedTypes) {
    this.selectProblems = selectProblems;
    this.selectedTypes = selectedTypes;
  }
  
  public boolean isSelectProblems() {
    return selectProblems;
  }
  
  public boolean isSelectRuntimeDependencyContextType(RuntimeDependencyContextType type) {
    return selectedTypes.contains(type);
  }
  
  public Set<RuntimeDependencyContextType> getSelectedTypes() {
    return selectedTypes; 
  }
  
}
