/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface.MatchableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface.AvariableNotResolvableException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;


public class MemberVariableInterface implements MatchableInterface {
  
  private final String name;
  private TypeInterface type;
  
  
  private MemberVariableInterface(String name, TypeInterface type) {
    this.name = name;
    this.type = type;
  }

  public boolean matches(DeploymentItemInterface other) {
    if (other instanceof MemberVariableInterface) {
      MemberVariableInterface otherVar = (MemberVariableInterface)other;
      if (name.equals(otherVar.name)) {
        if (type == null || otherVar.type == null) {
          return true;
        } else {
          return type.isAssignableFrom(otherVar.type);
        }
      }
    }
    return false;
  }

  
  public boolean resolve() {
    if (resolveLocal()) {
      return true;
    } else {
      InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
      Optional<TypeInterface> superType = resCtx.getLocalSupertype();
      while (superType.isPresent()) {
        DeploymentItemState dis = resCtx.resolveProvider(superType.get());
        if (dis != null && dis.exists()) {
          InterfaceResolutionContext.updateCtx(dis);
          try {
            if (resolveLocal()) {
              return true;
            } else {
              superType = resCtx.getLocalSupertype();
            }
          } finally {
            InterfaceResolutionContext.revertCtx();
          }
        } else {
          return false;
        }
      }
    }
    return false;
  }
  
  
  private boolean resolveLocal() {
    return InterfaceResolutionContext.resCtx.get().getPublishedInterfaces().containsMatchingMemberVar(this);
  }
  
  
  public String getName() {
    return name;
  }
  
  @Override
  public int hashCode() {
    return name.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof MemberVariableInterface)) {
      return false;
    }
    MemberVariableInterface otherVar = (MemberVariableInterface) obj;
    if (name.equals(otherVar.name) &&
       ((type == null && otherVar.type == null) || (type != null && otherVar.type != null && type.equals(otherVar.type)))) {
      return true; 
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return (type == null ? "UNKNOWN" : type) + " " + name;
  }

  public String getDescription() {
    return (type == null ? name : type + " " + name);
  }
  
  
  public TypeInterface getType() {
    return type;
  }
  
  public boolean isUntyped() {
    return type == null;
  }
  
  public void inferType(TypeInterface type) {
    this.type = type;
  }

  public static MemberVariableInterface of(AVariable memVar) throws AvariableNotResolvableException {
    return new MemberVariableInterface(memVar.getVarName(), TypeInterface.of(memVar));
  }
  
  public static MemberVariableInterface of(String name, TypeInterface type) {
    return new MemberVariableInterface(name, type);
  }
  
  public static MemberVariableInterface of(String name, String type, Boolean isJavaBaseType, Boolean isList) {
    TypeInterface typeIf;
    if (type == null) {
      typeIf = null;
    } else {
      if (isJavaBaseType == null) {
        typeIf = TypeInterface.of(type, false, isList);
      } else {
        typeIf = TypeInterface.of(type, isJavaBaseType, isList);
      }
    }
    return new MemberVariableInterface(name, typeIf);
  }
  
  public static MemberVariableInterface of(String name) {
    return new MemberVariableInterface(name, null);
  }

}
