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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.schema.ModuleId;


public class ModuleGroup {

  private Map<ModuleId, ModuleParseData> map = new TreeMap<>(); 
  
  
  public List<Module> getModuleList() {
    List<Module> ret = new ArrayList<>();
    for (ModuleId id : map.keySet()) {
      ModuleParseData data = map.get(id);
      if (data == null) { continue; }
      Optional<Module> opt = data.getModule(id);
      if (opt.isPresent()) {
        ret.add(opt.get());
      }
    }
    return ret;
  }
  
  
  public Collection<ModuleParseData> getAllModuleParseData() {
    return Collections.unmodifiableCollection(map.values());
  }
  
  
  public Set<ModuleId> getModuleIds() {
    return Collections.unmodifiableSet(map.keySet());
  }
  
  
  public Optional<ModuleParseData> getModuleParseData(ModuleId id) {
    ModuleParseData ret = map.get(id);
    return Optional.ofNullable(ret);
  }
  
  
  public void add(ModuleParseData data) {
    if (data == null) { return; }
    for (Module mod : data.getModuleList()) {
      if (mod == null) { continue; }
      if (mod.getModuleId() == null) { continue; }
      map.put(mod.getModuleId(), data);
    }    
  }

  
  public Optional<Module> getModule(ModuleId id) {
    Optional<ModuleParseData> opt = getModuleParseData(id);
    if (opt.isPresent()) {
      return opt.get().getModule(id);
    }
    return Optional.empty();
  }
  
}
