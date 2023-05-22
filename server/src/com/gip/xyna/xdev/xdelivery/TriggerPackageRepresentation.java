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
package com.gip.xyna.xdev.xdelivery;

import java.io.File;

import com.gip.xyna.xact.trigger.Trigger;


public class TriggerPackageRepresentation {

  private final File[] jarFiles;
  private final String fqTriggerClassName;
  private final String triggerName;
  private final String[] sharedLibs;
  private final boolean includeAllInstances;
  
  public TriggerPackageRepresentation(Trigger trigger, boolean includeAllInstances) {
    this.jarFiles =trigger.getJarFiles();
    this.fqTriggerClassName =trigger.getFQTriggerClassName();
    this.triggerName =trigger.getTriggerName();
    this.sharedLibs = trigger.getSharedLibs();
    this.includeAllInstances = includeAllInstances;
  }

  
  public File[] getJarFiles() {
    return jarFiles;
  }

  
  public String getFqTriggerClassName() {
    return fqTriggerClassName;
  }

  
  public String getTriggerName() {
    return triggerName;
  }

  
  public String[] getSharedLibs() {
    return sharedLibs;
  }

  
  public boolean isIncludeAllInstances() {
    return includeAllInstances;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null ||  !((obj instanceof TriggerPackageRepresentation) || (obj instanceof Trigger))) {
      return false;
    }
    if (obj instanceof Trigger) {
      return this.fqTriggerClassName.equals(((Trigger)obj).getFQTriggerClassName()) && this.triggerName.equals(((Trigger)obj).getTriggerName());
    } else {
      return this.fqTriggerClassName.equals(((TriggerPackageRepresentation)obj).getFqTriggerClassName()) && this.triggerName.equals(((TriggerPackageRepresentation)obj).getTriggerName());
    }
  }
  
  @Override
  public int hashCode() {
    return fqTriggerClassName.hashCode() ^ triggerName.hashCode();
  }
}
