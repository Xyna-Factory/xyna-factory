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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.yangcentral.yangkit.model.api.stmt.Module;


public class ModuleRevisionData {

  private Map<String, TreeSet<ComparableModuleId>> _map = new HashMap<>();
  
  
  public void register(Module module) {
    if (module == null) { return; }
    if (module.getModuleId() == null) { return; }
    String name = module.getModuleId().getModuleName();
    if (name == null) { return; }
    TreeSet<ComparableModuleId> set = _map.get(name);
    if (set == null) {
      set = new TreeSet<ComparableModuleId>();
      _map.put(name, set);
    }
    ComparableModuleId id = new ComparableModuleId(module.getModuleId());
    if (!set.contains(id)) {
      set.add(id);
    }
  }
  
  public Optional<ComparableModuleId> findNewestExistingRevisionForModule(String modulename) {
    if (modulename == null) { return Optional.empty(); }
    TreeSet<ComparableModuleId> set = _map.get(modulename);
    if (set == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(set.last());
  }
  
  public Optional<ComparableModuleId> findDefaultRevisionForModule(String modulename) {
    if (modulename == null) { return Optional.empty(); }
    TreeSet<ComparableModuleId> set = _map.get(modulename);
    if (set == null) { return Optional.empty(); }
    if (set.size() != 1) { return Optional.empty(); }
    return Optional.ofNullable(set.first());
  }
  
}
