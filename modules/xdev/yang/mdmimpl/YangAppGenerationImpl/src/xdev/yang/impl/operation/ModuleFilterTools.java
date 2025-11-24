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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.yangcentral.yangkit.model.api.stmt.Import;
import org.yangcentral.yangkit.model.api.stmt.Include;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.parser.YangYinParser;
import org.apache.log4j.Logger;
import org.yangcentral.yangkit.model.api.schema.ModuleId;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;

import xdev.yang.impl.YangCapabilityUtils;
import xdev.yang.impl.YangCapabilityUtils.YangDeviceCapability;


public class ModuleFilterTools {
  
  public static class MatchData {
    private final ModuleGroup group;
    private Set<ModuleId> matchedIds = new HashSet<>();    
    public MatchData(ModuleGroup group) {
      this.group = group;
    }    
    public Set<ModuleId> getMatchedIds() {
      return matchedIds;
    }    
    public ModuleGroup getGroup() {
      return group;
    }
  }
  
  
  private static Logger _logger = Logger.getLogger(ModuleFilterTools.class);
  
  
  public List<Module> filterAndReload(List<ModuleGroup> grouplist, List<YangDeviceCapability> capabilities) {
    List<MatchData> matchedGroups = new ArrayList<>();
    for (ModuleGroup group: grouplist) {
      MatchData matched = checkCapabilities(group, capabilities);
      if (matched.matchedIds.size() > 0) {
        matchedGroups.add(matched);
      }
    }
    Set<ComparableModuleId> idsToReload = new HashSet<>();
    ModuleGroup mergedGroup = new ModuleGroup();
    for (MatchData matched : matchedGroups) {
      prepareForReload(matched, idsToReload);
      Collection<ModuleParseData> allParsed = matched.getGroup().getAllModuleParseData();
      for (ModuleParseData parsed : allParsed) {
        mergedGroup.add(parsed);
      }
    }
    return reloadModules(mergedGroup, idsToReload);
  }
  
  
  private void prepareForReload(MatchData matched, Set<ComparableModuleId> idsToReload) {
    ModuleGroup group = matched.getGroup();
    Set<ModuleId> extendedSet = new HashSet<>();
    
    for (ModuleId nextid : matched.getMatchedIds()) {
      followReferences(nextid, group, extendedSet);
    }
    Set<ComparableModuleId> adaptedSet = group.adaptEmptyModuleRevisions(extendedSet);
    idsToReload.addAll(adaptedSet);
  }
  
  
  private List<Module> reloadModules(ModuleGroup oldGroup, Set<ComparableModuleId> idset) {
    List<Module> ret = new ArrayList<>();
    ModuleGroup newGroup = buildFilteredModuleGroup(oldGroup, idset);
    YangSchemaContext context = null;
    for (ModuleParseData data : newGroup.getAllModuleParseData()) {
      java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(data.getSourceStringBytes());
      try {
        context = YangYinParser.parse(is, "module.yang", context);
      }
      catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    if (context == null) { return ret; }
    context.validate();
    for (Module mod : context.getModules()) {
      if (idset.contains(mod.getModuleId())) {
        ret.add(mod);
      }
    }
    return ret;
  }
  
  
  private ModuleGroup buildFilteredModuleGroup(ModuleGroup oldGroup, Set<ComparableModuleId> idset) {
    ModuleGroup newGroup = new ModuleGroup();
    for (ModuleId id : idset) {
      Optional<ModuleParseData> opt = newGroup.getModuleParseData(id);
      if (opt.isPresent()) {
        continue;
      }
      opt = oldGroup.getModuleParseData(id);
      if (opt.isPresent()) {
        newGroup.add(opt.get());  
      }
      else {
        _logger.warn("Could not find module with id " + idToString(id));
      }
    }
    return newGroup;
  }
  
  
  private void followReferences(ModuleId id, ModuleGroup group, Set<ModuleId> extended) {
    if (extended.contains(id)) { return; }
    extended.add(id);
    Set<ModuleId> newids = getReferencedModuleIds(id, group);
    for (ModuleId nextid : newids) {
      followReferences(nextid, group, extended);
    }
  }
  
  
  private Set<ModuleId> getReferencedModuleIds(ModuleId id, ModuleGroup group) {
    Set<ModuleId> ret = new HashSet<>();
    Optional<Module> opt = group.getModule(id);
    if (!opt.isPresent()) {
      _logger.warn("Could not find referenced module: " + idToString(id));
      return ret;
    }
    Module mod = opt.get();
    for (Include sub : mod.getIncludes()) {
      String name = sub.getArgStr();
      String revision = null;
      if (sub.getRevisionDate() != null) {
        revision = sub.getRevisionDate().getArgStr();
      }
      ModuleId nextid = new ModuleId(name, revision);
      ret.add(nextid);
    }
    for (Import sub : mod.getImports()) {
      String name = sub.getArgStr();
      String revision = null;
      if (sub.getRevisionDate() != null) {
        revision = sub.getRevisionDate().getArgStr();
      }
      ModuleId nextid = new ModuleId(name, revision);
      ret.add(nextid);
    }
    return ret;
  }
  
  
  public String idToString(ModuleId id) {
    return "ModuleId " + id.getModuleName() + " " + (id.getRevision() == null ? "" : id.getRevision());
  }
  
  
  private MatchData checkCapabilities(ModuleGroup group, List<YangDeviceCapability> capabilities) {
    MatchData ret = new MatchData(group);
    for (Module module : group.getModuleList()) {
      boolean matches = YangCapabilityUtils.isModuleInCapabilities(capabilities, module);
      if (matches) {
        ret.getMatchedIds().add(module.getModuleId());
      }
    }
    return ret;
  }

}
