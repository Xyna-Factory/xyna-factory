/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xdev.yang.impl.usecase;

import org.yangcentral.yangkit.model.api.schema.ModuleId;


public class ComparableModuleId extends ModuleId implements Comparable<ComparableModuleId> {

  public ComparableModuleId(String moduleName, String revision) {
    super(moduleName, revision);
  }

  public ComparableModuleId(ModuleId id) {
    super(id.getModuleName(), id.getRevision());
  }
  
  @Override
  public int compareTo(ComparableModuleId id) {
    if (this.equals(id)) { return 0; }
    if (id == null) { return 1; }
    String name1 = this.getModuleName();
    String name2 = id.getModuleName();
    if (name1 == null) { return -1; }
    if (name2 == null) { return 1; }
    
    int val = name1.compareTo(name2);
    if (val != 0) { return val; } 
    
    String rev1 = this.getRevision();
    String rev2 = id.getRevision();
    if (rev1 == null) { return -1; }
    if (rev2 == null) { return 1; }    
    return rev1.compareTo(rev2);
  }

  @Override
  public String toString() {
    return "ComparableModuleId " + getModuleName() + " " + (getRevision() == null ? "" : getRevision());
  }
  
}
