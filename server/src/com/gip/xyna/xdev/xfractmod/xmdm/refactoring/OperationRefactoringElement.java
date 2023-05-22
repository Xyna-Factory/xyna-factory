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
package com.gip.xyna.xdev.xfractmod.xmdm.refactoring;


public class OperationRefactoringElement extends RefactoringElement {

  protected final String fqOperationNameOld;
  protected final String fqOperationNameNew;
  protected final String serviceGroupNameOld;
  protected final String operationNameOld;
  protected final String serviceGroupNameNew;
  protected final String operationNameNew;
  
  protected final boolean operationRename;
  protected final boolean operationMove;
  
  public OperationRefactoringElement(String fqXmlNameOld, String fqXmlNameNew, String newLabel, String oldLabel) {
    super(RefactoringManagement.splitOperationFqName(fqXmlNameOld)[0], RefactoringManagement.splitOperationFqName(fqXmlNameNew)[0],
          (newLabel == null || newLabel.length() <= 0) ? RefactoringManagement.splitOperationFqName(fqXmlNameNew)[2] : newLabel,
          oldLabel, RefactoringTargetType.DATATYPE);
    this.fqOperationNameOld = fqXmlNameOld;
    this.fqOperationNameNew = fqXmlNameNew;
    String[] oldNames = RefactoringManagement.splitOperationFqName(fqXmlNameOld);
    String[] newNames = RefactoringManagement.splitOperationFqName(fqXmlNameNew);
    this.serviceGroupNameOld = oldNames[1];
    this.operationNameOld = oldNames[2];
    this.serviceGroupNameNew = newNames[1];
    this.operationNameNew = newNames[2];
    operationRename = !operationNameOld.equals(operationNameNew);
    operationMove = !(this.fqXmlNameOld.equals(this.fqXmlNameNew) && serviceGroupNameOld.equals(serviceGroupNameNew));
  }
  
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof OperationRefactoringElement)) {
      return false;
    }
    return fqOperationNameOld.equals(((OperationRefactoringElement)obj).fqOperationNameOld);
  }
  
  @Override
  public int hashCode() {
    return fqOperationNameOld.hashCode();
  }
  
  @Override
  public String toString() {
    return "Refactor operation old=" + fqOperationNameOld + ", new=" + fqOperationNameNew;
  }
  
}
