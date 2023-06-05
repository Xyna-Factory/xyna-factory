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
package com.gip.xyna.xfmg.xfctrl.revisionmgmt;

import java.io.Serializable;

import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;


public abstract class RuntimeContext implements Serializable, StringSerializable<RuntimeContext>, Comparable<RuntimeContext>{

  private static final long serialVersionUID = 1L;

  public enum RuntimeContextType {
    Application(RuntimeDependencyContextType.Application), Workspace(RuntimeDependencyContextType.Workspace), DataModel(null);
    
    private final RuntimeDependencyContextType type;
    
    RuntimeContextType(RuntimeDependencyContextType type) {
      this.type = type;
    }
    
    public RuntimeDependencyContextType getType() {
      return type;
    }
  }
  
  private final String name;
  
  
  public RuntimeContext(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name may not be null.");
    }
    this.name = name;
  }

  public abstract RuntimeContextType getType();
  public abstract String getGUIRepresentation();
  
  public RuntimeDependencyContextType getRuntimeDependencyContextType() {
    return getType().getType();
  }
  
  public String getName() {
    return name;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RuntimeContext other = (RuntimeContext) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }


  /**
   * Vergleicht die Klassennamen miteinander, um z.B. Applications und Workspaces sortieren zu können
   * @param class1
   * @return
   */
  public int compareToClass(Class<?> class1) {
    return class1.getName().compareTo(this.getClass().getName());
  }
  
  public static int compareToClass(Class<?> class1, Class<?> class2) {
    return class1.getName().compareTo(class2.getName());
  }

  
  public RuntimeContext deserializeFromString(String string) {
      return RuntimeContext.valueOf(string);
    }
  
  public static RuntimeContext valueOf(String string) {
    if (string == null) {
      return null;
    }
    
    int idx = 0;
    StringBuilder name = new StringBuilder();
    idx += StringUtils.readMaskedUntil( name, string.substring(idx), '/' );
    String version = string.substring(idx);
    if (version.length() == 0 && string.charAt(idx-1) != '/') {
      return new Workspace( name.toString() );
    } else {
      return new Application(name.toString(), version);
    }
  }
  
  public RuntimeContext asCorrespondingRuntimeContext() {
    return this;
  }
  

}
