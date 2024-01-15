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
package com.gip.xyna.xact.filter.session.gb;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;

public enum ObjectType {

  workflow,
  step,
  orderInputSource,
  remoteDestinationArea,
  remoteDestination,
  remoteDestinationParameter,
  labelArea,
  documentationArea,
  variable,
  formula,
  expression,
  formulaArea,
  branchArea,
  caseArea,
  service,
  distinctionBranch,
  distinctionCase,
  throwStep,
  exceptionHandling,
  exceptionHandlingWf,
  exceptionMessage,
  exceptionMessageArea,
  storableProperty,
  typeInfoArea,
  memberVar,
  memberVarArea,
  memberMethod,
  memberMethodsArea,
  overriddenMethodsArea,

  queryFilterCriterion,
  querySortCriterion,
  querySortingArea,
  queryFilterArea,
  querySelectionMasksArea,
  querySelectionMask,
  
  libs,
  serviceGroupLib,
  serviceGroupSharedLib,
  
  staticMethod,
  
  methodVarArea,
  datatype,
  operation,
  exception, 
  servicegroup,
  
  memberDocumentationArea,
  operationDocumentationArea,
  
  clipboardEntry,
  warning,
  reference
  ;
  
  public static ObjectType of(XMOMType type) {
    switch(type) {
      case DATATYPE:
        return datatype;
      case EXCEPTION:
        return exception;
      case WORKFLOW:
        return workflow;
      default:
        return null;
    }
  }
}
