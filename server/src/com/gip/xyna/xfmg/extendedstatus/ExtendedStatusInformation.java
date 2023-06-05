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
package com.gip.xyna.xfmg.extendedstatus;

import java.io.Serializable;

import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagementInterface.StepStatus;

public class ExtendedStatusInformation implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private String componentName;
  private StepStatus step;
  private String additionalInformation;
  
  public ExtendedStatusInformation(StepStatus step, String componentName, String additionalInformation) {
    this.componentName = componentName;
    this.step = step;
    this.additionalInformation = additionalInformation;
  }

  
  public String getComponentName() {
    return componentName;
  }

  
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  
  public StepStatus getStep() {
    return step;
  }

  
  public void setStep(StepStatus step) {
    this.step = step;
  }

  
  public String getAdditionalInformation() {
    return additionalInformation;
  }

  
  public void setAdditionalInformation(String additionalInformation) {
    this.additionalInformation = additionalInformation;
  }
      
}