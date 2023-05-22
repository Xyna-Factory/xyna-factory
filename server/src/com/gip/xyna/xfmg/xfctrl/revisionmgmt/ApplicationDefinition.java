/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

public class ApplicationDefinition implements Serializable, RuntimeDependencyContext {
  
  private static final long serialVersionUID = -1701307807211036394L;
  
  private final String name;
  private final Workspace parentWorkspace;

  public ApplicationDefinition(String name, Workspace parentWorkspace) {
    this.name = name;
    this.parentWorkspace = parentWorkspace;
  }
  
  public Workspace getParentWorkspace() {
    return parentWorkspace;
  }


  public String toString() {
    return "ApplicationDefinition '" + name + "', ParentWorkspace '" + parentWorkspace.getName() + "'";
  }

  public String getName() {
    return name;
  }

  @Override
  public String getAdditionalIdentifier() {
    return parentWorkspace.getName();
  }

  public RuntimeDependencyContextType getRuntimeDependencyContextType() {
    return RuntimeDependencyContextType.ApplicationDefinition;
  }

  public RuntimeContext asCorrespondingRuntimeContext() {
    return parentWorkspace;
  }

  public int compareTo(RuntimeDependencyContext o) {
    if (o instanceof ApplicationDefinition) {
      ApplicationDefinition other = (ApplicationDefinition) o;
      int i = getParentWorkspace().compareTo(other.getParentWorkspace());
      if (i == 0) {
        i = getName().compareTo(other.getName());
      }
      return i;
    } else {
      return RuntimeContext.compareToClass(o.getClass(), this.getClass());
    }
  }

  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result + 31 * parentWorkspace.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ApplicationDefinition)) {
      return false;
    }
    ApplicationDefinition other = (ApplicationDefinition) obj;
    if (!other.getName().equals(getName())) {
      return false;
    }
    return other.getParentWorkspace().equals(getParentWorkspace());
  }

  
  public String getGUIRepresentation() {
    return "A:\"" + getName() + "\"-V:\"\"-WS:\"" + getParentWorkspace().getName() + "\"";
  }

  public static ApplicationDefinition deserializeFromString(String string) {
    int idx = 0;
    StringBuilder name = new StringBuilder();
    idx += StringUtils.readMaskedUntil( name, string.substring(idx), '/' );
    if (string.length() > idx &&
        string.charAt(idx) == '/') {
      String parentWorkspace = string.substring(idx + 1);
      if (parentWorkspace.length() <= 0) {
        return null;
      } else {
        return new ApplicationDefinition(name.toString(), new Workspace(parentWorkspace));
      }
    } else {
      return null;
    }
  }
  
  
}
