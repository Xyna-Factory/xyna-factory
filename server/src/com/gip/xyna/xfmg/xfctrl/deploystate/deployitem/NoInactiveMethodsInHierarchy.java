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
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ProblemType;
import com.gip.xyna.xfmg.xfctrl.deploystate.InconsistencyProvider;
import com.gip.xyna.xfmg.xfctrl.deploystate.PublishedInterfaces;


public class NoInactiveMethodsInHierarchy implements DeploymentItemInterface, InconsistencyProvider {

  public boolean resolve() {
    Optional<Pair<TypeInterface, OperationInterface>> check = findFirstInactiveOperation();
    return !check.isPresent();
  }
  
  
  private Optional<Pair<TypeInterface, OperationInterface>> findFirstInactiveOperation() {

    InterfaceResolutionContext context = InterfaceResolutionContext.resCtx.get();
    Optional<TypeInterface> supertype = Optional.of(context.getLocalType());
    while (supertype.isPresent()) {
      DeploymentItemState dis = context.resolve(supertype.get());
      if (dis == null) {
        return Optional.empty();
      }
      
      PublishedInterfaces publInt = dis.getPublishedInterfaces(context.getLocation());
      OperationInterface oi = publInt.getFirstInactiveOperation();
      if (oi != null) {
        return Optional.of(Pair.of(supertype.get(), oi));
      }
      
      supertype = publInt.getSupertype();
    }

    return Optional.empty();

  }


  public String getDescription() {
    return "NoInactiveMethodsInHierarchy";
  }
  
  public static NoInactiveMethodsInHierarchy get() {
    return new NoInactiveMethodsInHierarchy();
  }


  public DeploymentItemInterface getInconsistency() {
    Optional<Pair<TypeInterface, OperationInterface>> check = findFirstInactiveOperation();
    if (check.isPresent()) {
      return InterfaceEmployment.of(check.get().getFirst(), OperationInterface.of(check.get().getSecond(), ProblemType.METHOD_IS_INACTIVE));
    } else {
      return null;
    }
  }

}
