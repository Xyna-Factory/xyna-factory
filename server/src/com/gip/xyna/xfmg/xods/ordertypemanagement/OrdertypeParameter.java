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
package com.gip.xyna.xfmg.xods.ordertypemanagement;



import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.XynaProcessing.SerializableDestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.planning.Capacity;



public class OrdertypeParameter implements Serializable {

  private static final long serialVersionUID = -6720799658433676769L;

  private String ordertypeName;
  private DestinationValueParameter planningDestinationValue;
  private boolean planningDestinationIsCustom = false;
  private DestinationValueParameter executionDestinationValue;
  private boolean executionDestinationIsCustom = false;
  private DestinationValueParameter cleanupDestinationValue;
  private boolean cleanupDestinationIsCustom = false;
  private Set<Capacity> requiredCapacities;
  private String documentation;
  private Integer monitoringLevel;
  private boolean monitoringLevelIsCustom = false;
  private Integer priority;
  private boolean priorityIsCustom = false;
  private String applicationName; //nur noch wegen Abwärtskompatibilität drin
  private String versionName; //nur noch wegen Abwärtskompatibilität drin
  private RuntimeContext runtimeContext = RevisionManagement.DEFAULT_WORKSPACE;
  private Map<ParameterType, List<InheritanceRule>> parameterInheritanceRules;

  public OrdertypeParameter() {
  }


  public OrdertypeParameter(String ordertypeName, DestinationValueParameter planningDestinationValue,
                            DestinationValueParameter executionDestinationValue, DestinationValueParameter cleanupDestinationValue,
                            Set<Capacity> requiredCapacities, String documentation, Integer monitoringLevel, Integer priority) {
    this.ordertypeName = ordertypeName;
    this.planningDestinationValue = planningDestinationValue;
    this.executionDestinationValue = executionDestinationValue;
    this.cleanupDestinationValue = cleanupDestinationValue;
    this.requiredCapacities = requiredCapacities;
    this.documentation = documentation;
    this.monitoringLevel = monitoringLevel;
    this.priority = priority;
  }


  public OrdertypeParameter(OrdertypeInformation ordertypeInformation) {
    this.ordertypeName = ordertypeInformation.getOrdertypeName();
    this.documentation = ordertypeInformation.getDocumentation();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("OrdertypeParameter(").append(ordertypeName).append(",");
    if( planningDestinationIsCustom ) {
      sb.append("planning=").append(planningDestinationValue == null ? "null" : planningDestinationValue.getFullQualifiedName()).append(",");
    }
    if( executionDestinationIsCustom ) {
      sb.append("execution=").append(executionDestinationValue == null ? "null" : executionDestinationValue.getFullQualifiedName()).append(",");
    }
    if( cleanupDestinationIsCustom ) {
      sb.append("cleanup=").append(cleanupDestinationValue  == null ? "null" : cleanupDestinationValue.getFullQualifiedName()).append(",");
    }
    if( requiredCapacities != null ) {
      sb.append("capacities=").append(requiredCapacities).append(",");
    }
    if( monitoringLevelIsCustom ) {
      sb.append("monitoringLevel=").append(monitoringLevel).append(",");
    }
    if( priorityIsCustom ) {
      sb.append("priority=").append(priority).append(",");
    }
    sb.append("runtimeContext=").append(runtimeContext).append(",");
    if( parameterInheritanceRules != null ) {
      sb.append("parameterInheritanceRules=").append(parameterInheritanceRules).append(",");
    }
    if( documentation != null ) {
      sb.append("documentation=\"").append(documentation).append("\"");
    }
    sb.append(")");
    return sb.toString();
  }
  
  


  public String getApplicationName() {
    if (runtimeContext instanceof Application) {
      return runtimeContext.getName();
    }
    return applicationName;
  }


  @Deprecated
  public void setApplicationName(String applicationName) {
    if (applicationName == null) {
      setRuntimeContext(RevisionManagement.DEFAULT_WORKSPACE);
      return;
    }
    
    this.applicationName = applicationName;
    if (runtimeContext instanceof Application) {
      this.versionName = ((Application) runtimeContext).getVersionName();
    }
    this.runtimeContext = null;
  }


  public String getVersionName() {
    if (runtimeContext instanceof Application) {
      return ((Application)runtimeContext).getVersionName();
    }
    return versionName;
  }


  @Deprecated
  public void setVersionName(String versionName) {
    this.versionName = versionName;
    if (runtimeContext instanceof Application) {
      this.applicationName = runtimeContext.getName();
    }
    runtimeContext = null;
  }


  public void setRuntimeContext(RuntimeContext runtimeContext) {
    if (runtimeContext == null) {
      throw new IllegalArgumentException("RuntimeContext may not be null.");
    }
    
    this.applicationName = null;
    this.versionName = null;
    this.runtimeContext = runtimeContext;
  }

  public String getWorkspaceName() {
    if (runtimeContext instanceof Workspace) {
      return runtimeContext.getName();
    }
    
    return null;
  }
  
  public RuntimeContext getRuntimeContext() {
    if (runtimeContext == null) {
      if (applicationName != null) {
        runtimeContext = new Application(applicationName, versionName);
      } else {
        runtimeContext = RevisionManagement.DEFAULT_WORKSPACE;
      }
      applicationName = null;
      versionName = null;
    }
    
    return runtimeContext;
  }


  public String getOrdertypeName() {
    return ordertypeName;
  }


  public DestinationValueParameter getPlanningDestinationValue() {
    return planningDestinationValue;
  }


  public DestinationValueParameter getExecutionDestinationValue() {
    return executionDestinationValue;
  }


  public DestinationValueParameter getCleanupDestinationValue() {
    return cleanupDestinationValue;
  }


  public Set<Capacity> getRequiredCapacities() {
    return requiredCapacities;
  }


  public String getDocumentation() {
    return documentation;
  }


  public Integer getMonitoringLevel() {
    return monitoringLevel;
  }


  public Integer getPriority() {
    return priority;
  }

  public List<InheritanceRule> getParameterInheritanceRules(ParameterType parameterType) {
    if (parameterInheritanceRules == null) {
      return null;
    }
    return parameterInheritanceRules.get(parameterType);
  }

  public Map<ParameterType, List<InheritanceRule>> getParameterInheritanceRules() {
    return parameterInheritanceRules;
  }
  
  public void setOrdertypeName(String ordertypeName) {
    this.ordertypeName = ordertypeName;
  }


  public void setPlanningDestinationValue(DestinationValueParameter planningDestinationValue) {
    this.planningDestinationValue = planningDestinationValue;
    planningDestinationIsCustom = false;
  }

  public void setCustomPlanningDestinationValue(DestinationValueParameter planningDestinationValue) {
    this.planningDestinationValue = planningDestinationValue;
    planningDestinationIsCustom = true;
  }

  public void setExecutionDestinationValue(DestinationValueParameter executionDestinationValue) {
    this.executionDestinationValue = executionDestinationValue;
    executionDestinationIsCustom = false;
  }

  public void setCustomExecutionDestinationValue(DestinationValueParameter executionDestinationValue) {
    this.executionDestinationValue = executionDestinationValue;
    executionDestinationIsCustom = true;
  }

  public void setCleanupDestinationValue(DestinationValueParameter cleanupDestinationValue) {
    this.cleanupDestinationValue = cleanupDestinationValue;
    cleanupDestinationIsCustom = false;
  }

  public void setCustomCleanupDestinationValue(DestinationValueParameter cleanupDestinationValue) {
    this.cleanupDestinationValue = cleanupDestinationValue;
    cleanupDestinationIsCustom = true;
  }

  public void setRequiredCapacities(Set<Capacity> requiredCapacities) {
    this.requiredCapacities = requiredCapacities;
  }


  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }


    public void setMonitoringLevel(Integer monitoringLevel) {
      this.monitoringLevel = monitoringLevel;
    }

  public void setCustomMonitoringLevel(Integer monitoringLevel) {
    this.monitoringLevel = monitoringLevel;
    monitoringLevelIsCustom = monitoringLevel != null;
  }


  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public void setCustomPriority(Integer priority) {
    this.priority = priority;
    priorityIsCustom = priority != null;
  }

  
  public void setParameterInheritanceRules(Map<ParameterType, List<InheritanceRule>> parameterInheritanceRules) {
    if (parameterInheritanceRules == null) {
      this.parameterInheritanceRules = null;
    } else {
      this.parameterInheritanceRules = new HashMap<>(parameterInheritanceRules.size());
      for (Entry<ParameterType, List<InheritanceRule>> e : parameterInheritanceRules.entrySet()) {
        this.parameterInheritanceRules.put(e.getKey(), new ArrayList<>(e.getValue()));
      }
    }
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof OrdertypeParameter)) {
      return false;
    } else {
      return ordertypeName.equals(((OrdertypeParameter) obj).ordertypeName);
    }
  }


  @Override
  public int hashCode() {
    return ordertypeName.hashCode();
  }


  public static class DestinationValueParameter implements Serializable {

    private static final long serialVersionUID = -7782742373529716154L;

    private String fullQualifiedName;
    private String destinationType;


    public DestinationValueParameter() {
    }


    public DestinationValueParameter(String fullQualifiedName, String destinationType) {
      this.fullQualifiedName = fullQualifiedName;
      this.destinationType = destinationType;
    }


    public DestinationValueParameter(DestinationValue destinationValue) {
      this.fullQualifiedName = destinationValue.getFQName();
      this.destinationType = destinationValue.getDestinationType().getTypeAsString();
    }


    public DestinationValueParameter(SerializableDestinationValue destinationValue) {
      this.fullQualifiedName = destinationValue.getFqName();
      this.destinationType = destinationValue.getDestinationType();
    }


    public String getFullQualifiedName() {
      return fullQualifiedName;
    }


    public String getDestinationType() {
      return destinationType;
    }


    public void setFullQualifiedName(String fullQualifiedName) {
      this.fullQualifiedName = fullQualifiedName;
    }


    public void setDestinationType(String destinationType) {
      this.destinationType = destinationType;
    }


    public ExecutionType getDestinationTypeEnum() {
      return ExecutionType.getByTypeString(destinationType);
    }

  }


  public boolean containsCustomConfig() {
    return executionDestinationIsCustom || (requiredCapacities != null && requiredCapacities.size() > 0) || monitoringLevelIsCustom
        || documentation != null || planningDestinationIsCustom || priorityIsCustom || cleanupDestinationIsCustom
        || parameterInheritanceRules != null;
  }


  public boolean isCustomPlanningDestinationValue() {
    return planningDestinationIsCustom;
  }
  
  public boolean isCustomCleanupDestinationValue() {
    return cleanupDestinationIsCustom;
  }

  public boolean isCustomExecutionDestinationValue() {
    return executionDestinationIsCustom;
  }
  
  // sets isCustom to false if the values correspond to previous settings
  public void adjustDestinationValueIsCustomSetting(OrdertypeParameter oldSettings) {
    if (oldSettings != null) {
      if (planningDestinationValue != null &&
          oldSettings.planningDestinationValue != null &&
          planningDestinationValue.destinationType.equals(oldSettings.planningDestinationValue.destinationType) &&
          planningDestinationValue.fullQualifiedName.equals(oldSettings.planningDestinationValue.fullQualifiedName)) {
        planningDestinationIsCustom = false;
      }
      if (executionDestinationValue != null &&
          oldSettings.executionDestinationValue != null &&
          executionDestinationValue.destinationType.equals(oldSettings.executionDestinationValue.destinationType) &&
          executionDestinationValue.fullQualifiedName.equals(oldSettings.executionDestinationValue.fullQualifiedName)) {
        executionDestinationIsCustom = false;
      }
      if (cleanupDestinationValue != null &&
          oldSettings.cleanupDestinationValue != null &&
          cleanupDestinationValue.destinationType.equals(oldSettings.cleanupDestinationValue.destinationType) &&
          cleanupDestinationValue.fullQualifiedName.equals(oldSettings.cleanupDestinationValue.fullQualifiedName)) {
        cleanupDestinationIsCustom = false;
      }
    }
  }
  
  
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    getRuntimeContext();
  }

}
