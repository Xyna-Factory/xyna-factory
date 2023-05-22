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
package com.gip.xyna.xact.trigger;

import java.io.Serializable;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xact.trigger.TriggerStorable.TriggerState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer;


public class TriggerInformation implements Serializable {

  private static final long serialVersionUID = 1L;
  
  String triggerName;
  String fqTriggerClassName;
  String[][] startParameterDocumentation;
  String description;
  RuntimeContext runtimeContext = RevisionManagement.DEFAULT_WORKSPACE;
  private List<TriggerInstanceInformation> triggerInstances;
  private List<StringParameter<?>> enhancedStartParameter;
  private TriggerState triggerState;
  private String errorCause;
  private String[] sharedLibs;
  private AdditionalDependencyContainer additionalDependencies;
  
  TriggerInformation(String triggerName, String fqTriggerClassName, String[][] startParameterDocumentation,
                     List<StringParameter<?>> enhancedStartParameter, 
                     String description, List<TriggerInstanceInformation> triggerInstances,
                     RuntimeContext runtimeContext, TriggerState triggerState, String errorCause, String[] sharedLibs, AdditionalDependencyContainer additionalDependencies) {
    this.triggerName = triggerName;
    this.fqTriggerClassName = fqTriggerClassName;
    this.startParameterDocumentation = startParameterDocumentation;
    this.enhancedStartParameter = enhancedStartParameter;
    this.description = description;
    this.triggerInstances = triggerInstances;
    this.runtimeContext = runtimeContext;
    this.triggerState = triggerState;
    this.errorCause = errorCause;
    this.sharedLibs = sharedLibs;
    this.additionalDependencies = additionalDependencies;
  }

  public AdditionalDependencyContainer getAdditionalDependencies() {
    return additionalDependencies;
  }

  public String[] getSharedLibs() {
    return sharedLibs;
  }
  
  public String getTriggerName() {
    return triggerName;
  }
  
  public String getFqTriggerClassName() {
    return fqTriggerClassName;
  }
  
  public String[][] getStartParameterDocumentation() {
    return startParameterDocumentation;
  }

  public String getDescription() {
    return description;
  }
  
  public TriggerState getTriggerState() {
    return triggerState;
  }

  public String getErrorCause() {
    return errorCause;
  }
  
  
  public List<TriggerInstanceInformation> getTriggerInstances() {
    return triggerInstances;
  }
  
  public String getApplicationName() {
    if (runtimeContext instanceof Application) {
      return runtimeContext.getName();
    }
    
    return null;
  }
  
  public String getVersionName() {
    if (runtimeContext instanceof Application) {
      return ((Application) runtimeContext).getVersionName();
    }
    
    return null;
  }
  
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }
  
  public List<StringParameter<?>> getEnhancedStartParameter() {
    return enhancedStartParameter;
  }
  
  
  public static class TriggerInstanceInformation implements Serializable {

    private static final long serialVersionUID = -6444300529933436701L;
    
    String triggerInstanceName;
    String triggerName;
    String description;
    private List<String> startParameter;
    String startParameterAsString;
    private TriggerInstanceState state;
    private String errorCause;
    private final long revision;
    private final RuntimeContext runtimeContext;

    TriggerInstanceInformation(String triggerInstanceName, String triggerName, String description, TriggerInstanceState state,
                               List<String> startParameter, String startParameterAsString,
                               String errorCause, long revision) {
      this.triggerName = triggerName;
      this.triggerInstanceName = triggerInstanceName;
      this.description = description;
      this.state = state;
      this.startParameter = startParameter;
      this.startParameterAsString = startParameterAsString;
      this.errorCause = errorCause;
      try {
        this.runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      this.revision = revision;
    }

    public String getTriggerInstanceName() {
      return triggerInstanceName;
    }
    
    public String getDescription() {
      return description;
    }
    
    /**
     * @deprecated use getState instead
     * @return
     */
    @Deprecated
    public boolean isEnabled() {
      return state == TriggerInstanceState.ENABLED;
    }
    
    public List<String> getStartParameter() {
      return startParameter;
    }

    public String getTriggerName() {
      return triggerName;
    }
    
    public String getStartParameterAsString() {
      return startParameterAsString;
    }
    
    public TriggerInstanceState getState() {
      return state;
    }
    
    public String getErrorCause() {
      return errorCause;
    }

    public Long getRevision() {
      return revision;
    }
    
    public RuntimeContext getRuntimeContext() {
      return runtimeContext;
    }
  }
}



