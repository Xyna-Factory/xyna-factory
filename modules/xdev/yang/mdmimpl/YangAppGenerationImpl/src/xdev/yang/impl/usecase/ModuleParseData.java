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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.yangcentral.yangkit.model.api.schema.ModuleId;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.parser.YangYinParser;

import com.gip.xyna.utils.misc.Base64;


public class ModuleParseData {

  private final byte[] sourceString;
  private final Map<ModuleId, Module> modules = new TreeMap<>();
  
  
  public ModuleParseData(byte[] sourceString, List<Module> modulelist) {
    this.sourceString = sourceString;
    for (Module mod : modulelist) {
      modules.put(mod.getModuleId(), mod);
    }
  }

  public List<Module> getModuleList() {
    List<Module> ret = new ArrayList<>();
    ret.addAll(modules.values());
    return ret;
  }
  
  public Set<ModuleId> getModuleIds() {
    return Collections.unmodifiableSet(modules.keySet());
  }


  public byte[] getSourceStringBytes() {
    return sourceString;
  }
  
  public Optional<Module> getModule(ModuleId id) {
    return Optional.ofNullable(modules.get(id));
  }
    
  
}
