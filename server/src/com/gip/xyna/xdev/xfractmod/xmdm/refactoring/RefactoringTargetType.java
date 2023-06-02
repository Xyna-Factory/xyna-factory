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
package com.gip.xyna.xdev.xfractmod.xmdm.refactoring;

import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer.AdditionalDependencyType;

public enum RefactoringTargetType {
  
  WORKFLOW(XMOMType.WORKFLOW, DependencySourceType.WORKFLOW, XMOMDatabaseType.WORKFLOW, ApplicationEntryType.WORKFLOW, AdditionalDependencyType.WORKFLOW, AdditionalDependencyType.ORDERTYPE),
  DATATYPE(XMOMType.DATATYPE, DependencySourceType.DATATYPE, XMOMDatabaseType.DATATYPE, ApplicationEntryType.DATATYPE, AdditionalDependencyType.DATATYPE),
  EXCEPTION(XMOMType.EXCEPTION, DependencySourceType.XYNAEXCEPTION, XMOMDatabaseType.EXCEPTION, ApplicationEntryType.EXCEPTION, AdditionalDependencyType.EXCEPTION),
  FORM(XMOMType.FORM, null, null, ApplicationEntryType.FORMDEFINITION),
  FILTER(null, DependencySourceType.FILTER, null, ApplicationEntryType.FILTER, AdditionalDependencyType.FILTER),
  APPLICATION(null, null, null , null),
  ORDERTYPE_CONFIG(null, DependencySourceType.ORDERTYPE, null, ApplicationEntryType.ORDERTYPE),
  CRONJOB(null, null, null, null),
  INPUTSOURCE(null, null, null, null);
  
  private final XMOMType correspondingXMOMType;
  private final DependencySourceType correspondingDependencyType;
  private final XMOMDatabaseType correspondingXMOMDatabaseType;
  private final AdditionalDependencyType[] relevantAdditionalDependencies;
  private final ApplicationEntryType correspondingApplicationEntryType;
  
  private RefactoringTargetType(XMOMType correspondingXMOMType, DependencySourceType correspondingDependencyType, XMOMDatabaseType correspondingXMOMDatabaseType, ApplicationEntryType correspondingApplicationEntryType, AdditionalDependencyType... relevantAdditionalDependencies) {
    this.correspondingXMOMType = correspondingXMOMType;
    this.correspondingDependencyType = correspondingDependencyType;
    this.correspondingXMOMDatabaseType = correspondingXMOMDatabaseType;
    this.relevantAdditionalDependencies = relevantAdditionalDependencies;
    this.correspondingApplicationEntryType = correspondingApplicationEntryType;
  }
  
  public boolean hasCorrespondingXMOMType() {
    return correspondingXMOMType != null;
  }
  
  public boolean hasCorrespondingGenerationBaseRepresentation() {
    return correspondingXMOMType != null && correspondingXMOMType != XMOMType.FORM;
  }
  
  public XMOMType getXMOMType() {
    return correspondingXMOMType; 
  }
  
  public boolean hasCorrespondingDependencySourceType() {
    return correspondingDependencyType != null;
  }
  
  public DependencySourceType getDependencySourceType() {
    return correspondingDependencyType; 
  }
  
  public boolean hasCorrespondingApplicationEntryType() {
    return correspondingApplicationEntryType != null;
  }
  
  public ApplicationEntryType getApplicationEntryType() {
    return correspondingApplicationEntryType; 
  }

  public boolean hasCorrespondingXMOMDatabaseType() {
    return correspondingXMOMDatabaseType != null;
  }
  
  public XMOMDatabaseType getXMOMDatabaseType() {
    return correspondingXMOMDatabaseType; 
  }
  
  public boolean hasRelevantAdditionalDependencies() {
    return relevantAdditionalDependencies != null;
  }
  
  public AdditionalDependencyType[] getAdditionalDependencies() {
    return relevantAdditionalDependencies; 
  }
  
  public static RefactoringTargetType fromXMOMType(XMOMType type) {
    for (RefactoringTargetType targetType : values()) {
      if (targetType.hasCorrespondingXMOMType() && 
          targetType.correspondingXMOMType == type) {
        return targetType;
      }
    }
    throw new IllegalArgumentException("Invalid XMOMType as refactoring target: " + type);
  }
  
  public static RefactoringTargetType fromDependencyType(DependencySourceType type) {
    for (RefactoringTargetType targetType : values()) {
      if (targetType.hasCorrespondingDependencySourceType() && 
          targetType.correspondingDependencyType == type) {
        return targetType;
      }
    }
    throw new IllegalArgumentException("Invalid DependencySourceType as refactoring target: " + type);
  }
  
  public static RefactoringTargetType fromXMOMType(XMOMDatabaseType type) {
    for (RefactoringTargetType targetType : values()) {
      if (targetType.hasCorrespondingXMOMType() && 
          targetType.correspondingXMOMDatabaseType == type) {
        return targetType;
      }
    }
    throw new IllegalArgumentException("Invalid XMOMDatabaseType as refactoring target: " + type);
  }

}
