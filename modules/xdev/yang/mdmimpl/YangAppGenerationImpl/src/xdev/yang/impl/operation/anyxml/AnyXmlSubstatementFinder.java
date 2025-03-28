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
package xdev.yang.impl.operation.anyxml;



import java.util.List;

import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.stmt.MainModule;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.SubModule;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;



public class AnyXmlSubstatementFinder {

  private List<Module> modules;


  public AnyXmlSubstatementFinder(List<Module> modules) {
    this.modules = modules;
  }
  
  public YangStatement find(String name, String namespace) {
    Module module = findModule(namespace);
    if(module == null) {
      return null;
    }
    
    for(YangElement subStatement : module.getSubElements()) {
      if(subStatement instanceof YangStatement) {
        YangStatement sub = (YangStatement)subStatement;
        if(sub.getArgStr().equals(name)) {
          return sub;
        }
      }
    }
    return null;
  }
  
  private Module findModule(String namespace) {
    for(Module module : modules) {
      if(module instanceof MainModule) {
        MainModule mainModule = (MainModule)module;
        if(namespace.equals(mainModule.getNamespace().getArgStr())) {
          return module;
        }
      } else if(module instanceof SubModule) {
        SubModule subModule = (SubModule)module;
        if(namespace.equals(subModule.getMainModule().getNamespace().getArgStr())) {
          return module;
        }
      }
    }
    
    return null;
  }
}
