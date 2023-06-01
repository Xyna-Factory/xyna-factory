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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem.Collision.RuntimeContextCollisionType;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport;
import com.gip.xyna.xfmg.xfctrl.deploystate.DisplayState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;


public abstract class RuntimeContextProblem implements Serializable {

  private static final Logger logger = CentralFactoryLogging.getLogger(RuntimeContextProblem.class);
  private final static long serialVersionUID = 1L;

  private final static String TYPE_KEY = "type";
  private final static String RUNTIME_CONTEXT_KEY = "runtimeContext";
  private final static String COLLISION_TYPE_KEY = "collisionType";
  private final static String NAME_KEY = "name";
  private final static String STATE_KEY = "state";
  //private final static String CONTAINS_SELF_KEY = "containsSelf"; // needed from GUI?

  public enum RuntimeContextProblemType {
    
    CYCLE("circular requirement", true, true), //runtimeContext (mehrere)
    COLLISION("duplicate element", true, false), //collisionType, name, (mehrere) type, (mehrere) runtimeContext
    UNRESOLVABLE_REQUIREMENT("missing runtime context", true, true), //runtimeContext
    ERRONEOUS_REQUIREMENT("erroneous requirement", true, false), //runtimeContext
    ERRONEOUS_ORDER_ENTRANCE("erroneous order entrance", false, false),
    DEPLOYMENT_ITEM_STATE("deployment item state", false, false); //name, type, state
    
    private final String description;
    private final boolean causesErrorState;
    private final boolean causesValidationError;

    private RuntimeContextProblemType(String description, boolean causesErrorState, boolean causesValidationError) {
      this.description = description;
      this.causesErrorState = causesErrorState;
      this.causesValidationError = causesValidationError;
    }
    
    public String getDescription() {
      return description;
    }
    
    public boolean causesErrorState() {
      return causesErrorState;
    }
    
    public boolean causesValidationError() {
      return causesValidationError;
    }
    
  }
  

  private final RuntimeContextProblemType id;
  private final List<SerializablePair<String,String>> details;

  
  private RuntimeContextProblem(RuntimeContextProblemType id) {
    this.id = id;
    details = new ArrayList<SerializablePair<String,String>>();
  }
  
  public RuntimeContextProblemType getId() {
    return id;
  }
  
  
  
  protected String getValue(String key) {
    for (SerializablePair<String, String> keyValuePair : details) {
      if (keyValuePair.getFirst().equals(key)) {
        return keyValuePair.getSecond();
      }
    }
    return null;
  }

  
  protected void add(String key, String value) {
    details.add(SerializablePair.of(key, value));
  }
  
  
  public static Cycle cycle() {
    return new Cycle();
  }
  
  public static Cycle cycle(Collection<RuntimeDependencyContext> rcs) {
    Cycle cycle = new Cycle();
    for (RuntimeDependencyContext rc : rcs) {
      cycle.addRuntimeContext(rc);
    }
    return cycle;
  }

  public static UnresolvableRequirement unresolvableRequirement(RuntimeDependencyContext rc) {
    return new UnresolvableRequirement(rc);
  }

  
  public static Collision collision(RuntimeContextCollisionType type, String name) {
    return new Collision(type, name);
  }
  

  public static ErroneousRequirement erroneousRequirement(RuntimeDependencyContext rc) {
    return new ErroneousRequirement(rc);
  }

  public static ErroneousOrderEntrance erroneousOrderEntrance(OrderEntrance orderEntrance) {
    return new ErroneousOrderEntrance(orderEntrance);
  }
  
  public static DeploymentState deploymentState(DeploymentItemStateReport disr, RuntimeDependencyContext rc) {
    return new DeploymentState(disr, rc);
  }
  
  public String getMessage() {
    StringBuilder sb = new StringBuilder();
    sb.append(id.getDescription()).append(Constants.LINE_SEPARATOR);
    for (Pair<String, String> pair : details) {
      sb.append("    ").append(pair.getFirst()).append(": ").append(pair.getSecond()).append(Constants.LINE_SEPARATOR);
    }
    
    return sb.toString();
  }
  
  public List<SerializablePair<String,String>> getDetails() {
    return details;
  }

  public boolean causeErrorStatus() {
    return id.causesErrorState();
  }
  
  
  public boolean causeValidationError() {
    return id.causesValidationError();
  }
  
  
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof RuntimeContextProblem)) {
      return false;
    }
    RuntimeContextProblem other = (RuntimeContextProblem) obj;
    if (this.id != other.id) {
      return false;
    }
    if (details.size() != other.details.size()) {
      return false;
    }
    for (int i = 0; i < details.size(); i++) {
      if (!details.get(i).equals(other.details.get(i))) {
        return false;
      }
    }
    return true;
  }
  
  
  public int hashCode() {
    return getMessage().hashCode();
  }
  
  
  public static class UnresolvableRequirement extends RuntimeContextProblem {
    
    private static final long serialVersionUID = 1L;
    private final RuntimeDependencyContext rc;
    
    private UnresolvableRequirement(RuntimeDependencyContext rc) {
      super(RuntimeContextProblemType.UNRESOLVABLE_REQUIREMENT);
      this.rc = rc;
      add(RUNTIME_CONTEXT_KEY, rc.getGUIRepresentation());
    }
    
    
    public RuntimeDependencyContext getRuntimeContext() {
      return rc; 
    }
    
  }
  
  
  public static class Cycle extends RuntimeContextProblem {
    
    private static final long serialVersionUID = 1L;
    
    private Cycle() {
      super(RuntimeContextProblemType.CYCLE);
    }
    
    public void addRuntimeContext(RuntimeDependencyContext rc) {
      add(RUNTIME_CONTEXT_KEY, rc.getGUIRepresentation());
    }
    
  }
  
  
  
  public static class Collision extends RuntimeContextProblem {
    
    // TODO Übergabe String mit GUI-Menschen klären
    public enum RuntimeContextCollisionType {
      ORDERTYPE,
      ACTIVATION,
      XMOM;
    }
    
    private static final long serialVersionUID = 1L;
    private final RuntimeContextCollisionType collisionType;
    private final String name;
    private int participating;
    
    private Collision(RuntimeContextCollisionType collisionType, String name) {
      super(RuntimeContextProblemType.COLLISION);
      this.collisionType = collisionType;
      add(COLLISION_TYPE_KEY, collisionType.toString());
      this.name = name;
      add(NAME_KEY, name);
    }
    
    
    public RuntimeContextCollisionType getCollisionType() {
        return collisionType;
    }
    
    
    public void addRuntimeContext(RuntimeDependencyContext runtimeContext, String type) {
      add(RUNTIME_CONTEXT_KEY, runtimeContext.getGUIRepresentation());
      add(TYPE_KEY, type);
      participating++;
    }
    
    public String getName() {
      return name;
    }


    public int getParticipatingRuntimeContexts() {
      return participating;
    }


    public String getType() {
      return getValue(TYPE_KEY);
    }
    
  }
  
  
  public static class ErroneousRequirement extends RuntimeContextProblem {
    
    private static final long serialVersionUID = 1L;
    
    private ErroneousRequirement(RuntimeDependencyContext rc) {
      super(RuntimeContextProblemType.ERRONEOUS_REQUIREMENT);
      add(RUNTIME_CONTEXT_KEY, rc.getGUIRepresentation());
    }
    
  }
  
  
  
  public static class ErroneousOrderEntrance extends RuntimeContextProblem {
    
    private static final long serialVersionUID = 1L;
    
    private ErroneousOrderEntrance(OrderEntrance orderEntrance) {
      super(RuntimeContextProblemType.ERRONEOUS_ORDER_ENTRANCE);
      add(NAME_KEY, orderEntrance.getName());
      add(TYPE_KEY, orderEntrance.getType().toString()); // TODO mit GUI-Menschen klären
    }
    
  }
  
  
  public static class DeploymentState extends RuntimeContextProblem {
    
    private static final long serialVersionUID = 1L;
    
    private DeploymentState(DeploymentItemStateReport disr, RuntimeDependencyContext rc) {
      super(RuntimeContextProblemType.DEPLOYMENT_ITEM_STATE);
      add(NAME_KEY, disr.getFqName());
      add(TYPE_KEY, disr.getType().getNiceName());
      add(STATE_KEY, disr.getState().name());
      add(RUNTIME_CONTEXT_KEY, rc.getGUIRepresentation());
    }
    
    private DeploymentState(DeploymentItemState dis, RuntimeDependencyContext rc, Throwable e) {
      super(RuntimeContextProblemType.DEPLOYMENT_ITEM_STATE);
      //TODO exception übergeben? loggen?
      add(NAME_KEY, dis.getName());
      add(TYPE_KEY, dis.getType().getNiceName());
      add(STATE_KEY, DisplayState.INVALID.name());
      add(RUNTIME_CONTEXT_KEY, rc.getGUIRepresentation());
    }
    
  }


  public static RuntimeContextProblem deploymentStateError(DeploymentItemState dis, RuntimeDependencyContext rc, Throwable e) {
    return new DeploymentState(dis, rc, e);
  }

}
