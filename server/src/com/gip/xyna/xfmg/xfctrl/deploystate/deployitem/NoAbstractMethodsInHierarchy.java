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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ProblemType;
import com.gip.xyna.xfmg.xfctrl.deploystate.InconsistencyProvider;
import com.gip.xyna.xfmg.xfctrl.deploystate.PublishedInterfaces;


public class NoAbstractMethodsInHierarchy implements DeploymentItemInterface, InconsistencyProvider {

  public boolean resolve() {
    Optional<Pair<TypeInterface, OperationInterface>> check = findFirstUnimplementedAbstractOperation();
    return !check.isPresent();
  }
  
  
  private Optional<Pair<TypeInterface, OperationInterface>> findFirstUnimplementedAbstractOperation() {
    InterfaceResolutionContext context = InterfaceResolutionContext.resCtx.get();
    Map<String, OperationInterface> concreteOperations = new HashMap<String, OperationInterface>();
    Optional<TypeInterface> supertype = Optional.of(context.getLocalType());
    while (supertype.isPresent()) {
      DeploymentItemState dis = context.resolve(supertype.get());
      if (dis == null) {
        return Optional.empty();
      }
      
      PublishedInterfaces publInt = dis.getPublishedInterfaces(context.getLocation());

      for (OperationInterface op : publInt.getAllOperations()) {
        switch (op.getImplType()) {
          case ABSTRACT :
            if (!containsImplementation(concreteOperations, op)) {
              return Optional.of(Pair.of(supertype.get(), op));
            }
            break;
          case CONCRETE :
            concreteOperations.put(op.getName(), op);
            break;
          default :
            break;
        }
      }
      supertype = publInt.getSupertype();
    }
    return Optional.empty();
  }


  private boolean containsImplementation(Map<String, OperationInterface> concreteOperations, OperationInterface op) {
    OperationInterface oi = concreteOperations.get(op.getName());
    if (oi == null) {
      return false;
    }

    OperationInterface ourClone = oi.clone();
    OperationInterface theirClone = op.clone();

    //remove 'this' parameter
    if (ourClone.getInput() != null && ourClone.getInput().size() > 0) {
      ourClone.getInput().remove(0);
    }
    if (theirClone.getInput() != null && theirClone.getInput().size() > 0) {
      theirClone.getInput().remove(0);
    }

    return theirClone.equals(ourClone); //nicht matches verwenden, weil inputs identisch sein m�ssen
  }


  public String getDescription() {
    return "NoAbstractMethodsInHierarchy";
  }
  
  public static NoAbstractMethodsInHierarchy get() {
    return new NoAbstractMethodsInHierarchy();
  }


  public DeploymentItemInterface getInconsistency() {
    Optional<Pair<TypeInterface, OperationInterface>> check = findFirstUnimplementedAbstractOperation();
    if (check.isPresent()) {
      return InterfaceEmployment.of(check.get().getFirst(), OperationInterface.of(check.get().getSecond(), ProblemType.ABSTRACT_OPERATION_IN_HIERARCHY));
    } else {
      return null;
    }
  }

}
