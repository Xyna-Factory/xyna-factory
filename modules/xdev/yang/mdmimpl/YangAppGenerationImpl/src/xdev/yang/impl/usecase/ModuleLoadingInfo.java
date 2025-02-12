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
import java.util.List;

import org.yangcentral.yangkit.model.api.stmt.Module;


public class ModuleLoadingInfo {

  public static enum ModuleLoadingStrategy {
    IGNORE_IMPORTS, FOLLOW_IMPORTS;
  }

  private List<Module> modulelist = new ArrayList<>();
  private final String moduleFqXmomTypename;  
  private final Long entryRevision;
  
  public ModuleLoadingInfo(String moduleFqXmomTypename, Long entryRevision) {
    this.moduleFqXmomTypename = moduleFqXmomTypename;
    this.entryRevision = entryRevision;
  }
  
  public void addModule(Module module) {
    modulelist.add(module);
  }
  
  public List<Module> getModuleList() {
    return modulelist;
  }
  
  public String getModuleFqXmomTypename() {
    return moduleFqXmomTypename;
  }
  
  public Long getEntryRevision() {
    return entryRevision;
  }
  
  public static List<Module> toModuleList(List<ModuleLoadingInfo> list) {
    List<Module> ret = new ArrayList<>();
    for (ModuleLoadingInfo item : list) {
      ret.addAll(item.getModuleList());
    }
    return ret;
  }
  
}
