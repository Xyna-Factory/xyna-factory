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

package xdev.yang.impl.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.schema.ModuleId;


public class ModuleRefValidationData {

  private Map<String, List<ModuleId>> _map = new HashMap<>();
  
  public void register(ModuleGroup group) {
    if (group == null) { return; }
    for (ModuleId id : group.getModuleIds()) {
      register(id);
    }
  }
  
  
  public void register(List<Module> list) {
    if (list == null) { return; }
    for (Module mod : list) {
      if (mod == null) { continue; }
      register(mod.getModuleId());
    }
  }
  
  
  public void register(ModuleId id) {
    if (id == null) { return; }
    if (id.getModuleName() == null) { return; }
    String name = id.getModuleName();
    List<ModuleId> list = _map.get(name);
    if (list == null) {
      list = new ArrayList<ModuleId>();
      _map.put(name, list);
    }
    list.add(id);
  }
  
  
  public void validate() {
    StringBuilder s = new StringBuilder("Duplicate module references: ");
    boolean isfirst = true;
    boolean found = false;
    for (Entry<String, List<ModuleId>> entry : _map.entrySet()) {
      List<ModuleId> list = entry.getValue();
      if (list.size() > 1) {
        found = true;
        for (ModuleId id : list) {
          if (isfirst) { isfirst = false; }
          else { s.append(", "); }
          s.append(id.getModuleName());
          if (id.getRevision() != null) {
            s.append(" (Rev. " + id.getRevision()+ ")");
          }
        }
      }
    }
    if (found) {
      throw new RuntimeException(s.toString());
    }
  }
  
}
