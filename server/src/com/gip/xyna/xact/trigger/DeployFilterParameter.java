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
package com.gip.xyna.xact.trigger;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class DeployFilterParameter implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private String filterName;
  private String instanceName;
  private String triggerInstanceName;
  private String description;
  private boolean optional;
  private List<String> configuration;
  private long revision;

  public String getFilterName() {
    return filterName;
  }

  public String getInstanceName() {
    return instanceName;
  }

  public String getTriggerInstanceName() {
    return triggerInstanceName;
  }

  public String getDescription() {
    return description;
  }

  public boolean isOptional() {
    return optional;
  }

  public List<String> getConfiguration() {
    return configuration;
  }
  
  public long getRevision() {
    return revision;
  }

  public static class Builder {
    DeployFilterParameter instance = new DeployFilterParameter();
    
    public DeployFilterParameter build() {
      return instance;
    }

    public Builder filterName(String filterName) {
      instance.filterName = filterName;
      return this;
    }

    public Builder instanceName(String instanceName) {
      if (instanceName.contains("#")) {
        // Zeichen wird für Primary-Key in Tabelle verwendetet ... verboten!
        throw new RuntimeException("Illegal character '#' in name " + instanceName);
      }
      instance.instanceName = instanceName;
      return this;
    }

    public Builder triggerInstanceName(String triggerInstanceName) {
      instance.triggerInstanceName = triggerInstanceName;
      return this;
    }

    public Builder description(String description) {
      instance.description = description;
      return this;
    }

    public Builder optional(boolean optional) {
      instance.optional = optional;
      return this;
    }

    public Builder configuration(List<String> configurationParameter) {
      if( configurationParameter != null && instance.configuration == null) {
        instance.configuration = configurationParameter;
      }
      return this;
    }
    public Builder configuration(String[] configurationParameter) {
      if( configurationParameter != null && instance.configuration == null) {
        instance.configuration = Arrays.asList(configurationParameter);
      }
      return this;
    }

    public Builder revision(long revision) {
      instance.revision = revision;
      return this;
    }

  }

}
