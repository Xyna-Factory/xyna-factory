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

import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;


public class InterfaceEmployment implements DeploymentItemInterface {

  private final TypeInterface provider;
  private DeploymentItemInterface wrapped;
  
  private InterfaceEmployment(TypeInterface provider, DeploymentItemInterface wrapped) {
    this.provider = provider;
    this.wrapped = wrapped;
  }
  
  
  public boolean matches(DeploymentItemInterface other) {
    return false;
  }

  
  public DeploymentItemInterface unwrap() {
    return wrapped;
  }


  public void cloneWrapped() {
    if (wrapped instanceof MemberVariableInterface) {
      wrapped = MemberVariableInterface.of(((MemberVariableInterface) wrapped).getName(), ((MemberVariableInterface) wrapped).getType());
    } else {
      throw new RuntimeException("unsupported");
    }
  }


  public TypeInterface getProvider() {
    return provider;
  }


  public static InterfaceEmployment of(TypeInterface provider, DeploymentItemInterface wrapped) {
    return new InterfaceEmployment(provider, wrapped);
  }

  
  public String toString() {
    return new StringBuilder("InterfaceEmployment in ").append(provider).append(":").append(Constants.LINE_SEPARATOR).append("    ").append(wrapped).toString();
  }


  public String getDescription() {
    return wrapped.getDescription();
  }


  public boolean resolve() {
    DeploymentItemState state = InterfaceResolutionContext.resCtx.get().resolveProvider(this);
    if (state.exists()) {
      InterfaceResolutionContext.updateCtx(state);
      try {
        return wrapped.resolve();
      } finally {
        InterfaceResolutionContext.revertCtx();
      }
    } else {
      return false;
    }
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof InterfaceEmployment)) {
      return false;
    }
    InterfaceEmployment other = (InterfaceEmployment)obj;
    if (provider.equals(other.provider)) {
      return wrapped.equals(other.wrapped);
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    return provider.hashCode() + wrapped.hashCode(); 
  }

}
