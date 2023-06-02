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
package com.gip.xyna.xact.trigger;

import java.io.Serializable;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.trigger.FilterInstanceStorable.FilterInstanceState;
import com.gip.xyna.xact.trigger.FilterStorable.FilterState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer;


public class FilterInformation implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  String fqFilterClassName;
  String filterName;
  String triggerName;
  String description;
  RuntimeContext runtimeContext;
  private List<FilterInstanceInformation> filterInstances;
  private FilterState filterState;
  private String errorCause;
  private String[] sharedLibs;
  private AdditionalDependencyContainer additionalDeps;
  private List<StringParameter<?>> configurationParameter;
  
  public FilterInformation(FilterStorable filterStorable, 
      List<FilterInstanceInformation> filterInstanceInformation,
      RuntimeContext runtimeContext, 
      AdditionalDependencyContainer additionalDeps, List<StringParameter<?>> configurationParameter) {
    
    this.filterName = filterStorable.getFilterName();
    this.triggerName = filterStorable.getTriggerName();
    this.description = filterStorable.getDescription();
    this.filterInstances = filterInstanceInformation;
    this.fqFilterClassName = filterStorable.getFqFilterClassName();
    this.runtimeContext = runtimeContext;
    this.filterState = filterStorable.getStateAsEnum();
    this.errorCause = filterStorable.getErrorCause();
    this.sharedLibs = filterStorable.getSharedLibsArray();
    this.additionalDeps = additionalDeps;
    this.configurationParameter = configurationParameter;
  }

  public String getFilterName() {
    return filterName;
  }
  
  public String getTriggerName() {
    return triggerName;
  }

  public String getDescription() {
    return description;
  }
  
  public List<FilterInstanceInformation> getFilterInstances() {
    return filterInstances;
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
  
  
  public String getFqFilterClassName() {
    return fqFilterClassName;
  }
  
  
  public FilterState getFilterState() {
    return filterState;
  }
  
  
  public String getErrorCause() {
    return errorCause;
  }
  
  public List<StringParameter<?>> getConfigurationParameter() {
    return configurationParameter;
  }
  
  public static class FilterInstanceInformation implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    String filterInstanceName;
    String triggerInstanceName;
    String filterName;
    String description;
    private FilterInstanceState state;
    private String errorCause;
    private boolean optional;
    private final long revision;
    private final RuntimeContext runtimeContext;
    private List<String> configuration;
    

    public FilterInstanceInformation(FilterInstanceStorable filterInstanceStorable) {
      this.filterName = filterInstanceStorable.getFilterName();
      this.filterInstanceName = filterInstanceStorable.getFilterInstanceName();
      this.triggerInstanceName = filterInstanceStorable.getTriggerInstanceName();
      this.description = filterInstanceStorable.getDescription();
      this.state = filterInstanceStorable.getStateAsEnum();
      this.errorCause = filterInstanceStorable.getErrorCause();
      this.optional = filterInstanceStorable.isOptional();
      this.revision = filterInstanceStorable.getRevision();
      this.configuration = filterInstanceStorable.getConfiguration();
      try {
        this.runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(this.revision);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    }
    
    public String getFilterInstanceName() {
      return filterInstanceName;
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
      return state == FilterInstanceState.ENABLED;
    }
    
    public String getFilterName() {
      return filterName;
    }
    
    public FilterInstanceState getState() {
      return state;
    }
    
    public String getErrorCause() {
      return errorCause;
    }
    
    public boolean isOptional() {
      return optional;
    }

    public Long getRevision() {
      return revision;
    }
    
    public RuntimeContext getRuntimeContext() {
      return runtimeContext;
    }

    public List<String> getConfiguration() {
      return configuration;
    }
  }


  public AdditionalDependencyContainer getAdditionalDependencies() {
    return additionalDeps;
  }

  public String[] getSharedLibs() {
    return sharedLibs;
  }
  
}