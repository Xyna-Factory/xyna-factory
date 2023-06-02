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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentifier;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;


public class SupertypeInterface implements DeploymentItemInterface, DeploymentItemIdentifier {

  private String name;
  private XMOMType type; // with null = any
  
  private SupertypeInterface(String name) {
    this.name = name;
  }
  
  private SupertypeInterface(String name, XMOMType type) {
    this(name);
    this.type = type;
  }
  
  public boolean matches(DeploymentItemInterface other) {
    if (other instanceof SupertypeInterface) {
      SupertypeInterface otherType = (SupertypeInterface)other;
      if (this.name.equals(otherType.name)) {
        return type == null || otherType.type == null || type == otherType.type; 
      } else {
        return false;
      }
    } else {
      return false;
    }
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
    if (!(obj instanceof SupertypeInterface)) {
      return false;
    }
    DeploymentItemIdentifier otherObj = (DeploymentItemIdentifier) obj;
    return this.type == otherObj.getType() &&
           this.name.equals(otherObj.getName());
  }

  public String getName() {
    return name;
  }
  
  
  public XMOMType getType() {
    return type;
  }
  
  public static SupertypeInterface of(DomOrExceptionGenerationBase domOrException) {
    return new SupertypeInterface(domOrException.getSuperClassGenerationObject().getOriginalFqName());
  }
  
  public static SupertypeInterface of(String name) {
    return new SupertypeInterface(name);
  }
  
  public static SupertypeInterface of(String name, XMOMType type) {
    return new SupertypeInterface(name, type);
  }
  
  public static SupertypeInterface of(TypeInfo typeInfo) {
    return of(TypeInterface.of(typeInfo, false));
  }
  
  public static SupertypeInterface of(TypeInterface type) {
    return of(type.getName(), type.getType());
  }
  
  @Override
  public String toString() {
    return "extends " + (type == null ? "UNKNOWN" : type.toString()) + " " + name;
  }


  public String getDescription() {
    return "extends " + (type == null ? name : type.toString() + " " + name);
  }

  public boolean resolve() {
    InterfaceResolutionContext context = InterfaceResolutionContext.resCtx.get();
    DeploymentItemState state = context.resolveProvider(this);
    if (!state.exists()) {
      return false;
    }
    if (TypeInterface.of(this).isAssignableFrom(context.getLocalType())) {
      return true;
    } else {
      Optional<TypeInterface> supertype = context.getLocalSupertype();
      if (supertype.isPresent()) {
        return TypeInterface.of(this).isAssignableFrom(supertype.get());
      } else {
        return false;
      }
    }
  }

}
