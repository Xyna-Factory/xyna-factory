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
package com.gip.xyna.xfmg.xfctrl.deploystate;



import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface.MatchableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.MemberVariableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.SupertypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;



public class PublishedInterfaces {

  private final Map<String, OperationInterface> operationsByName = new HashMap<String, OperationInterface>(1);
  private final Map<String, MemberVariableInterface> memberVarsByName = new HashMap<String, MemberVariableInterface>(1);
  private SupertypeInterface superType; //TODO performance: nach aussen hin will man immer das zugehörige TypeInterface wissen. gleich dieses speichern?
  private TypeInterface ownType;


  public boolean containsEqualOperation(OperationInterface op) {
    OperationInterface o = operationsByName.get(op.getName());
    if (o == null) {
      return false;
    }
    return op.equals(o);
  }


  private OperationInterface getMatchingOperation(OperationInterface op) {
    OperationInterface o = operationsByName.get(op.getName());
    if (o != null) {
      if (o.matches(op)) {
        return o;
      }
    }
    return null;
  }


  /**
   * gibt es eine operation, die auf die übergebene matched
   */
  public boolean containsMatchingOperation(OperationInterface op) {
    return getMatchingOperation(op) != null;
  }


  public Collection<OperationInterface> getAllOperations() {
    return operationsByName.values();
  }


  public OperationInterface getFirstInactiveOperation() {
    for (OperationInterface o : operationsByName.values()) {
      if (!o.isActive()) {
        return o;
      }
    }
    return null;
  }


  public Optional<TypeInterface> getSupertype() {
    if (superType != null) {
      return Optional.of(TypeInterface.of(superType));
    }
    return Optional.empty();
  }


  public boolean containsType(TypeInterface typeInterface) {
    return typeInterface.matches(ownType);
  }


  private MemberVariableInterface getMatchingMemberVar(MemberVariableInterface mem) {
    MemberVariableInterface m = memberVarsByName.get(mem.getName());
    if (m != null) {
      if (m.matches(mem)) {
        return m;
      }
    }
    return null;
  }


  /**
   * gibt es eine membervar, die auf die übergebene matched 
   */
  public boolean containsMatchingMemberVar(MemberVariableInterface mem) {
    return getMatchingMemberVar(mem) != null;
  }


  public MatchableInterface getMatchingType(MatchableInterface mi) {
    if (mi instanceof MemberVariableInterface) {
      return getMatchingMemberVar((MemberVariableInterface) mi);
    } else if (mi instanceof OperationInterface) {
      return getMatchingOperation((OperationInterface) mi);
    }
    throw new RuntimeException("unsupported type: " + mi.getClass());
  }


  public boolean containsMatchingType(MatchableInterface mi) {
    if (mi instanceof MemberVariableInterface) {
      return containsMatchingMemberVar((MemberVariableInterface) mi);
    } else if (mi instanceof OperationInterface) {
      return containsMatchingOperation((OperationInterface) mi);
    }
    throw new RuntimeException("unsupported type: " + mi.getClass());
  }


  @SuppressWarnings("unchecked")
  public <I extends DeploymentItemInterface> Set<I> filterInterfaces(Class<I> interfaceType) {
    if (interfaceType == TypeInterface.class) {
      return (Set<I>) setOfTyp();
    } else if (interfaceType == OperationInterface.class) {
      return (Set<I>) new HashSet<OperationInterface>(operationsByName.values());
    } else if (interfaceType == MemberVariableInterface.class) {
      return (Set<I>) new HashSet<MemberVariableInterface>(memberVarsByName.values());
    } else if (interfaceType == SupertypeInterface.class) {
      return (Set<I>) setOfSuperTyp();
    } else {
      throw new RuntimeException("unsupported type: " + interfaceType.getName());
    }
  }


  private Set<SupertypeInterface> setOfSuperTyp() {
    if (superType == null) {
      return Collections.emptySet();
    }
    Set<SupertypeInterface> set = new HashSet<SupertypeInterface>(1);
    set.add(superType);
    return set;
  }


  private Set<TypeInterface> setOfTyp() {
    Set<TypeInterface> set = new HashSet<TypeInterface>(1);
    set.add(ownType);
    return set;
  }


  public void addAll(Set<DeploymentItemInterface> set) {
    for (DeploymentItemInterface dii : set) {
      add(dii);
    }
  }


  //FIXME concurrency: anstatt clear und addAll sollte besser PublishedInterfaces Objekt immutable werden 
  public void clear() {
    ownType = null;
    superType = null;
    operationsByName.clear();
    memberVarsByName.clear();
  }


  public Set<DeploymentItemInterface> getAll() {
    Set<DeploymentItemInterface> s = new HashSet<DeploymentItemInterface>(operationsByName.size() + memberVarsByName.size() + 2);
    if (ownType != null) {
      s.add(ownType);
    }
    if (superType != null) {
      s.add(superType);
    }
    s.addAll(operationsByName.values());
    s.addAll(memberVarsByName.values());
    return s;
  }


  private void add(DeploymentItemInterface dii) {
    if (dii instanceof TypeInterface) {
      ownType = (TypeInterface) dii;
    } else if (dii instanceof SupertypeInterface) {
      superType = (SupertypeInterface) dii;
    } else if (dii instanceof OperationInterface) {
      OperationInterface oi = (OperationInterface) dii;
      operationsByName.put(oi.getName(), oi);
    } else if (dii instanceof MemberVariableInterface) {
      MemberVariableInterface mi = (MemberVariableInterface) dii;
      memberVarsByName.put(mi.getName(), mi);
    } else {
      throw new RuntimeException("unsupported type: " + dii.getClass().getName());
    }
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{").append(ownType);
    if (superType != null) {
      sb.append(",super=").append("superType");
    }
    if (memberVarsByName.size() > 0) {
      sb.append(",membs=").append(memberVarsByName);
    }
    if (operationsByName.size() > 0) {
      sb.append(",ops=").append(operationsByName);
    }
    sb.append("}");
    return sb.toString();
  }

}
