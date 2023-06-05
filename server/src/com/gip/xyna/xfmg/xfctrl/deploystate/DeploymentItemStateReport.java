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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarker;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceResolutionContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.MemberVariableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.MissingVarId;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.PrototypeElement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeMissmatch;
import com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem.DeploymentItemColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse.SerializableExceptionInformation;
import com.gip.xyna.xnwh.selection.parsing.Selection;


public class DeploymentItemStateReport implements Serializable {
  
  
  private static final long serialVersionUID = 1L;
  
  private final String fqName;
  private final XMOMType type;
  private final String specialType;
  private final long lastModified;
  private final String lastModifiedBy;
  private final long lastStateChange;
  private final String lastStateChangeBy;
  private final Boolean rollbackOccurred;
  private final Boolean buildExceptionOccurred;
  private final SerializableExceptionInformation rollbackCause;
  private final SerializableExceptionInformation rollbackException;
  private final SerializableExceptionInformation buildException;
  private final DisplayState state;
  private List<Inconsistency> inconsitencies;
  private List<ResolutionFailure> unresolvable;
  private List<ServiceImplInconsistency> serviceImplInconsistencies;
  private List<Dependency> dependencies;
  private String label;
  private Integer openTaskCount;
  private List<DeploymentMarker> deploymentMarker;
  
  DeploymentItemStateReport(String fqName, XMOMType type, long lastModified, String lastModifiedBy, long lastStateChange, String lastStateChangeBy,
                            DisplayState state, Optional<SerializableExceptionInformation> rollbackCause, Optional<SerializableExceptionInformation> rollbackError, Optional<SerializableExceptionInformation> buildError) {
    this(fqName, type, null, lastModified, lastModifiedBy, lastStateChange, lastStateChangeBy, state, rollbackCause, rollbackError, buildError, null);
  }

  DeploymentItemStateReport(String fqName, XMOMType type, String specialType, long lastModified, String lastModifiedBy, long lastStateChange, String lastStateChangeBy,
                            DisplayState state, Optional<SerializableExceptionInformation> rollbackCause, Optional<SerializableExceptionInformation> rollbackError,
                            Optional<SerializableExceptionInformation> buildError, Selection selection) {
    this.fqName = fqName;
    this.specialType = specialType;
    this.type = type;
    this.lastModified = (selection == null || selection.containsColumn(DeploymentItemColumn.LASTMODIFIED)) ? lastModified : 0;
    this.lastModifiedBy = (selection == null || selection.containsColumn(DeploymentItemColumn.LASTMODIFIEDBY)) ? lastModifiedBy : null;
    this.lastStateChange = (selection == null || selection.containsColumn(DeploymentItemColumn.LASTSTATECHANGE)) ? lastStateChange : 0;
    this.lastStateChangeBy = (selection == null || selection.containsColumn(DeploymentItemColumn.LASTSTATECHANGEBY)) ? lastStateChangeBy : null;
    this.state = (selection == null || selection.containsColumn(DeploymentItemColumn.STATE)) ? state : null;
    this.rollbackOccurred = (selection == null || selection.containsColumn(DeploymentItemColumn.ROLLBACKOCCURRED)) ? rollbackCause.isPresent() : null;
    this.buildExceptionOccurred = (selection == null || selection.containsColumn(DeploymentItemColumn.BUILDEXCEPTIONOCCURRED)) ? buildError.isPresent() : null;
    
    boolean selectRollbackCause = rollbackCause.isPresent() && (selection == null || selection.containsColumn(DeploymentItemColumn.ROLLBACKCAUSE));
    this.rollbackCause = selectRollbackCause ? rollbackCause.get() : null;
    
    boolean selectRollbackException = rollbackError.isPresent() && (selection == null || selection.containsColumn(DeploymentItemColumn.ROLLBACKEXCEPTION));
    this.rollbackException = selectRollbackException ? rollbackError.get() : null;

    boolean selectBuildException = buildError.isPresent() && (selection == null || selection.containsColumn(DeploymentItemColumn.ROLLBACKEXCEPTION));
    this.buildException = selectBuildException ? buildError.get() : null;
    
    this.deploymentMarker = new ArrayList<DeploymentMarker>();
  }
  
  public String getFqName() {
    return fqName;
  }

  public XMOMType getType() {
    return type;
  }
  
  public String getSpecialType() {
    return specialType;
  }
  
  public long getLastModified() {
    return lastModified;
  }
  
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }
  
  public long getLastStateChange() {
    return lastStateChange;
  }
  
  public String getLastStateChangeBy() {
    return lastStateChangeBy;
  }
  
  public Boolean rollbackOccurred() {
    return rollbackOccurred;
  }
  
  
  public Boolean buildExceptionOccurred() {
    return buildExceptionOccurred;
  }
  
  public SerializableExceptionInformation getRollbackCause() {
    return rollbackCause;
  }
  
  public SerializableExceptionInformation getRollbackException() {
    return rollbackException;
  }

  public SerializableExceptionInformation getBuildException() {
    return buildException;
  }
  
  public DisplayState getState() {
    return state;
  }

  public List<Inconsistency> getInconsitencies() {
    return inconsitencies;
  }
  
  public void setInconsistencies(List<Inconsistency> inconsitencies) {
    this.inconsitencies = inconsitencies;
  }
  
  public List<Dependency> getDependencies() {
    return dependencies;
  }
  
  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
  }
  
  public List<ResolutionFailure> getUnresolvable() {
    return unresolvable;
  }
  
  public void setUnresolvable(List<ResolutionFailure> unresolvable) {
    this.unresolvable = unresolvable;
  }
  
  
  public List<ServiceImplInconsistency> getServiceImplInconsistencies() {
    return serviceImplInconsistencies;
  }
  
  
  public void setImplInconsistencies(List<ServiceImplInconsistency> inconsistencies) {
    this.serviceImplInconsistencies = inconsistencies;
  }
  
  
  public String getLabel() {
    return label;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public Integer getOpenTaskCount() {
    return openTaskCount;
  }
  
  public void setOpenTaskCount(Integer openTaskCount) {
    this.openTaskCount = openTaskCount;
  }
  
  public List<DeploymentMarker> getDeploymentMarker() {
    return deploymentMarker;
  }
  
  public void addDeploymentMarker(List<DeploymentMarker> deploymentMarker) {
    if (deploymentMarker != null) {
      this.deploymentMarker.addAll(deploymentMarker);
    }
  }
  
  public void clearDeploymentMarker() {
    deploymentMarker.clear();
  }
  

  public static enum ProblemType {

    MEMBER_VARIABLE_ACCESS,
    SERVICE_INVOCATION,
    TYPE_MISMATCH,
    MISSING_VAR_ID,
    PROTOTYPE_ELEMENT,
    TYPE_CAST,
    ABSTRACT_OPERATION_IN_HIERARCHY,
    METHOD_IS_INACTIVE;

  }
  
  public static class Inconsistency implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final InconsistencyState type;
    private final String employmentDescription;
    private final ProblemType employmentType;
    private final String fqName;
    private final XMOMType xmomtype;
    private final boolean itemExists;
    private final String creationHint;
    private final Map<String, String> additionalData;
    
    Inconsistency(InconsistencyState type, DeploymentItemInterface invocation, boolean itemExists, String creationHint) {
      this(type, invocation, InterfaceResolutionContext.getProviderType(invocation), itemExists, creationHint);
    }
    
    
    Inconsistency(InconsistencyState type, DeploymentItemInterface invocation, DeploymentItemIdentifier relevantType, boolean itemExists, String creationHint) {
      this.type = type;
      this.fqName = relevantType.getName();
      this.xmomtype = relevantType.getType();
      this.employmentDescription = invocation.getDescription();
      this.additionalData = new HashMap<String, String>();
      DeploymentItemInterface dii = invocation;
      if (dii instanceof InterfaceEmployment) {
        dii = ((InterfaceEmployment)dii).unwrap();
      }
      if (dii instanceof OperationInterface) {
        employmentType = ((OperationInterface)dii).getProblemType();
      } else if (dii instanceof MemberVariableInterface) {
        employmentType = ProblemType.MEMBER_VARIABLE_ACCESS;
      } else if (dii instanceof TypeMissmatch) {
        employmentType = ProblemType.TYPE_CAST;
        additionalData.put(ResolutionFailure.TARGET_TYPE_KEY, relevantType.getName());
        additionalData.put(ResolutionFailure.SOURCE_TYPE_KEY, invocation.getDescription());
      } else {
        employmentType = null;
      }
      this.itemExists = itemExists;
      this.creationHint = creationHint;
    }
    
    public InconsistencyState getType() {
      return type;
    }

    public String getEmploymentDescription() {
      return employmentDescription;
    }
    
    public ProblemType getEmploymentType() {
      return employmentType;
    }
    
    public String getFqName() {
      return fqName;
    }
    
    public XMOMType getXmomtype() {
      return xmomtype;
    }
    
    public boolean isItemExists() {
      return itemExists;
    }
    
    public String getCreationHint() {
      return creationHint;
    }
    
    public Map<String, String> getAdditionalData() {
      return additionalData;
    }

    @Override
    public String toString() {
      return employmentDescription + " in " + fqName + " item exists: " + itemExists + (employmentType == null ? "" : "[" + employmentType + "]");
    }


    public String toFriendlyString() {
      StringBuilder sb = new StringBuilder();
      sb.append("There are problems with the usage of " + (xmomtype != null ? xmomtype.getNiceName() : "UNKNOWN")  + " " + fqName + ": ");
      if (!itemExists) {
        sb.append("It doesn't exist. ");
      }
      if (employmentType != null) {
        switch (employmentType) {
          case ABSTRACT_OPERATION_IN_HIERARCHY :
            sb.append("There exists an abstract operation in the datatype hierarchy.");
            break;
          case MEMBER_VARIABLE_ACCESS :
            sb.append("Access to member variable " + employmentDescription + " can not be resolved.");
            break;
          case METHOD_IS_INACTIVE :
            sb.append("Operation " + employmentDescription + " is marked as inactive.");
            break;
          case MISSING_VAR_ID :
            sb.append("Variable " + employmentDescription + " can not be resolved.");
            break;
          case PROTOTYPE_ELEMENT :
            sb.append("Workflow uses prototype element " + employmentDescription + ".");
            break;
          case SERVICE_INVOCATION :
            sb.append("A used operation can not be resolved: " + employmentDescription + ".");
            break;
          case TYPE_CAST :
            sb.append("Type " + additionalData.get(ResolutionFailure.SOURCE_TYPE_KEY) + " can not be assigned to "
                + additionalData.get(ResolutionFailure.TARGET_TYPE_KEY) + ".");
            break;
          case TYPE_MISMATCH :
          default :
            sb.append("Unsupported problem type " + employmentType + ".");
            break;
        }
      } else {
        sb.append("Description: ").append(employmentDescription);
        if (!employmentDescription.endsWith(".")) {
          sb.append(".");
        }
      }

      sb.append(" ");
      switch ( type) {
        case INVALID_0001 :
        case INVALID_0011:
          //falls application, könnte man das "only" auch weglassen.
          sb.append("The inconsistency exists only in deployed. The using object must be redeployed.");
          break;
        case INVALID_0101 :
          sb.append("The inconsistency exists only in deployed. The used object must be redeployed.");
          break;
        case INVALID_0010 :
          sb.append("The inconsistency only applies to the relation between the using object in deployed and the used object in saved.");
          break;
        case INVALID_0100 :
          sb.append("The inconsistency only applies to the relation between the using object in saved and the used object in deployed.");
          break;
        case INVALID_0110 :
          sb.append("The inconsistency is neither in saved nor in deployed but if only one of the objects is deployed it will become inconsistent.");
          break;
        case INVALID_1000 :
          sb.append("The inconsistency exists only in saved.");
          break;
        case INVALID_10xy : 
        case INVALID_11xx :
          sb.append("The inconsistency is in saved."); //TODO mehr aussagekraft?
          break;
          default: sb.append("[unsupported type " + type + "]");
      }
      return sb.toString().trim();
    }

  }
  
  
  public static class Dependency implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String fqName;
    private final XMOMType xmomtype;
    private final DependencyState type;
    
    Dependency(DeploymentItemIdentifier relevantType, DependencyState type) {
      this.fqName = relevantType.getName();
      this.xmomtype = relevantType.getType();
      this.type = type;
    }
    
    public String getFqName() {
      return fqName;
    }

    public XMOMType getXmomtype() {
      return xmomtype;
    }
    
    public DependencyState getType() {
      return type;
    }
    
  }
  
  
  public enum DependencyState {
    DEPLOYABLE, ADJUSTMENTS_REQUIRED;
    
    public static DependencyState get(DeploymentItemInterface diii,
                                      Set<DeploymentItemInterface> ss) {
      final boolean inc_ss = ss.contains(diii);
      if (inc_ss) {
        return ADJUSTMENTS_REQUIRED;
      } else {
        return DEPLOYABLE;
      }
    }
  }
  
  
  public static class ResolutionFailure implements Serializable {
    
    private static final long serialVersionUID = 1L;
    public static final String PROBLEM_TYPE_KEY = "problemType";
    public static final String TARGET_TYPE_KEY = "targetType";
    public static final String SOURCE_TYPE_KEY = "sourceType";
    
    private final TypeOfUsage type;
    private final String id;
    private String stepId;
    private Map<String, String> additionalData;
    
    ResolutionFailure(TypeOfUsage type, String id, String stepId) {
      this.type = type;
      this.id = id;
      this.stepId = stepId;
    }

    public TypeOfUsage getType() {
      return type;
    }
    
    public String getId() {
      return id;
    }
    
    public String getStepId() {
      return stepId;
    }
    
    public Map<String, String> getAdditionalData() {
      return additionalData;
    }
    
    @Override
    public String toString() {
      return "ResolutionFailure [" + type.toString() + " - " + additionalData + "] " + id + " @" + stepId;
    }
    
    static ResolutionFailure of(UnresolvableInterface unresolvable) {
      String stepId = "";
      if (unresolvable.getStepId() != null) {
        stepId = String.valueOf(unresolvable.getStepId());
      }
      ResolutionFailure resFail = new ResolutionFailure(unresolvable.getTypeOfUsage(), unresolvable.getId(), stepId); 
      if (unresolvable instanceof MissingVarId) {
        resFail.additionalData = new HashMap<String, String>();
        resFail.additionalData.put(PROBLEM_TYPE_KEY, ProblemType.MISSING_VAR_ID.name());
      } else if (unresolvable instanceof TypeMissmatch) {
        resFail.additionalData = new HashMap<String, String>();
        resFail.additionalData.put(PROBLEM_TYPE_KEY, ProblemType.TYPE_MISMATCH.name());
        resFail.additionalData.put(TARGET_TYPE_KEY, ((TypeMissmatch)unresolvable).getTargetType().getName());
        resFail.additionalData.put(SOURCE_TYPE_KEY, ((TypeMissmatch)unresolvable).getSourceType().getName());
      } else if (unresolvable instanceof PrototypeElement) {
        resFail.additionalData = new HashMap<String, String>();
        resFail.additionalData.put(PROBLEM_TYPE_KEY, ProblemType.PROTOTYPE_ELEMENT.name());
      }
      return resFail;
    }
    
    public static ResolutionFailure of(TypeOfUsage type, String id) {
      return new ResolutionFailure(type, id, null);
    }

    public static ResolutionFailure of(OperationInterface inActiveInterface) {
      ResolutionFailure resFail = new ResolutionFailure(TypeOfUsage.SERVICE_REFERENCE, inActiveInterface.getName(), "");
      resFail.additionalData = new HashMap<String, String>();
      resFail.additionalData.put(PROBLEM_TYPE_KEY, ProblemType.METHOD_IS_INACTIVE.toString());
      return resFail;
    }


    public String toFriendlyString() {
      StringBuilder sb = new StringBuilder();
      if (id != null && id.length() > 0) {
        sb.append("Object '").append(id).append("'");
      } else {
        sb.append("An object");
      }
      sb.append(" of type ").append(type.toString());
      if (stepId != null && stepId.length() > 0) {
        sb.append(" used in step ").append(stepId);
      }
      sb.append(" can not be resolved.");
      if (additionalData != null) {
        String problemType = additionalData.get(PROBLEM_TYPE_KEY);
        ProblemType pt = ProblemType.valueOf(problemType);
        sb.append(" Cause is ");
        switch (pt) {
          case MISSING_VAR_ID :
            sb.append("a missing variable id.");
            break;
          case TYPE_MISMATCH :
            sb.append("type mismatch between ").append(additionalData.get(SOURCE_TYPE_KEY)).append(" and ")
                .append(additionalData.get(TARGET_TYPE_KEY)).append(".");
            break;
          case PROTOTYPE_ELEMENT :
            sb.append("a prototype element.");
            break;
          case METHOD_IS_INACTIVE :
            sb.append("that it the operation is marked as inactive.");
            break;
          default :
            sb.append("Unsupported problem type: " + pt);
        }
      }
      return sb.toString();
    }

  }

  public static class ServiceImplInconsistency implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final ServiceImplInconsistencyState type;
    private final long lastChange;
    private final long lastJarChange;
    private String usedFqName;
    private XMOMType usedXmomType;
    
    ServiceImplInconsistency(ServiceImplInconsistencyState type, long lastChange, long lastJarChange) {
      this.type = type;
      this.lastChange = lastChange;
      this.lastJarChange = lastJarChange;
    }
    
    public ServiceImplInconsistencyState getType() {
      return type;
    }
    
    public long getLastChange() {
      return lastChange;
    }
    
    public long getLastJarChange() {
      return lastJarChange;
    }
    
    public void setUsedDeploymentItem(DeploymentItemIdentifier di) {
      usedFqName = di.getName();
      usedXmomType = di.getType();
    }
    
    public DeploymentItemIdentifier getUsedDeploymentItem() {
      if (usedFqName != null) {
        return new DeploymentItemIdentificationBase(usedXmomType, usedFqName);
      }
      
      return null;
    }
    
    
    @Override
    public String toString() {
      SimpleDateFormat format = Constants.defaultUTCSimpleDateFormatWithMS();
      return type + " lastChange: " + format.format(lastChange) + " lastJarChange: " + format.format(lastJarChange);
    }
  }

  public enum ServiceImplInconsistencyState {
    SAVED_INTERFACE_CHANGE,       //ImplJar ist vom <lastJarChange>, aber ServiceGroup wurde am <lastChange> geändert
    DEPLOYED_INTERFACE_CHANGE,    //ImplJar ist vom <lastJarChange>, aber ServiceGroup wurde am <lastChange> deployed
    SAVED_USED_OBJECT_CHANGE,     //ImplJar ist vom <lastJarChange>, aber der verwendete <usedXmomType> <usedFqName> wurde am <lastChange> geändert
    DEPLOYED_USED_OBJECT_CHANGE,  //ImplJar ist vom <lastJarChange>, aber der verwendete <usedXmomType> <usedFqName> wurde am <lastChange> deployed
    SAVED_MISSING_JAR,            //ImplJar wird von mindestens einem Service verwendet, ist aber in SAVED nicht vorhanden
    DEPLOYED_MISSING_JAR;         //ImplJar wird von mindestens einem Service verwendet, ist aber in DEPLOYED nicht vorhanden

    public static ServiceImplInconsistencyState get(DeploymentLocation location, boolean interfaceChange) {
      switch (location) {
        case SAVED:
          if (interfaceChange) {
            return SAVED_INTERFACE_CHANGE;
          } else {
            return SAVED_USED_OBJECT_CHANGE;
          }
        case DEPLOYED:
          if (interfaceChange) {
            return DEPLOYED_INTERFACE_CHANGE;
          } else {
            return DEPLOYED_USED_OBJECT_CHANGE;
          }
        default:
          throw new IllegalArgumentException("Unknown location " + location);
      }
    }
  }
  
}
