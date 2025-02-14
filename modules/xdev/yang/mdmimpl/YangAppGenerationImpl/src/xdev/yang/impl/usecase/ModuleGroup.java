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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.yangcentral.yangkit.model.api.schema.ModuleId;
import org.yangcentral.yangkit.model.api.stmt.Module;


public class ModuleGroup {

  private static Logger _logger = Logger.getLogger(ModuleGroup.class);
  private Map<ComparableModuleId, ModuleParseData> _map = new TreeMap<>();
  private ModuleRevisionData _revisionData = new ModuleRevisionData();
  
  
  public List<Module> getModuleList() {
    List<Module> ret = new ArrayList<>();
    for (ComparableModuleId id : _map.keySet()) {
      ModuleParseData data = _map.get(id);
      if (data == null) { continue; }
      Optional<Module> opt = data.getModule(id);
      if (opt.isPresent()) {
        ret.add(opt.get());
      }
    }
    return ret;
  }
  
  
  public Collection<ModuleParseData> getAllModuleParseData() {
    return Collections.unmodifiableCollection(_map.values());
  }
  
  
  public Set<ModuleId> getModuleIds() {
    return Collections.unmodifiableSet(_map.keySet());
  }
  
  
  private ComparableModuleId adapt(ModuleId id) {
    if (id instanceof ComparableModuleId) { return (ComparableModuleId) id; }
    return new ComparableModuleId(id);
  }
  
  
  public Optional<ModuleParseData> getModuleParseData(ModuleId id) {
    return getModuleParseDataImpl(adapt(id));
  }
  
  
  private Optional<ModuleParseData> getModuleParseDataImpl(ComparableModuleId id) {
    ModuleParseData ret = _map.get(id);
    return Optional.ofNullable(ret);
  }
  
  
  public void add(ModuleParseData data) {
    if (data == null) { return; }
    for (Module mod : data.getModuleList()) {
      if (mod == null) { continue; }
      if (mod.getModuleId() == null) { continue; }
      ComparableModuleId id = new ComparableModuleId(mod.getModuleId());
      _map.put(id, data);
      _revisionData.register(mod);
    }    
  }

  
  public Optional<Module> getModule(ModuleId id) {
    return getModuleImpl(adapt(id));
  }
  
  private Optional<Module> getModuleImpl(ComparableModuleId id) {
    Optional<ModuleParseData> opt = getModuleParseData(id);
    if (opt.isPresent()) {
      return opt.get().getModule(id);
    }    
    Optional<ComparableModuleId> revisionId = _revisionData.findNewestExistingRevisionForModule(id);
    if (!revisionId.isPresent()) {
      return Optional.empty();
    }
    opt = getModuleParseData(revisionId.get());
    if (opt.isPresent()) {
      return opt.get().getModule(revisionId.get());
    }
    else {
      throw new RuntimeException("Inconsistent data im ModuleGroup: No ModuleParseData found for registered " +
                                 "revision of module " + id.getModuleName());
    }
  }
  
  
  public Set<ComparableModuleId> adaptEmptyModuleRevisions(Set<ModuleId> input) {
    Set<ComparableModuleId> ret = new HashSet<>();
    for (ModuleId id : input) {
      if (id.getModuleName() == null) { continue; }
      if (id.getRevision() != null) {
        ret.add(new ComparableModuleId(id));
        continue;
      }
      Optional<ComparableModuleId> opt = this._revisionData.findDefaultRevisionForModule(id.getModuleName());
      if (opt.isPresent()) {
        ret.add(opt.get());
      }
      else {
        _logger.warn("Could not find default revision for module " + id.getModuleName());
      }
    }
    return ret;
  }
  
}
