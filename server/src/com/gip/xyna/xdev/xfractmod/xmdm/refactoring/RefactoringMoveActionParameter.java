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
package com.gip.xyna.xdev.xfractmod.xmdm.refactoring;

import java.util.Set;

import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringManagement.RefactoringType;


public class RefactoringMoveActionParameter extends RefactoringActionParameter {

  private static final long serialVersionUID = 1L;

  private String fqXmlNameOld;
  private String fqXmlNameNew;
  private String targetLabel;
  private RefactoringTargetRootType targetRootType;
  
  
  public RefactoringMoveActionParameter() {
    super(RefactoringType.MOVE);
  }
  
    
  public String getFqXmlNameOld() {
    return fqXmlNameOld;
  }
  
  public RefactoringMoveActionParameter setFqXmlNameOld(String fqXmlNameOld) {
    this.fqXmlNameOld = fqXmlNameOld;
    return this;
  }
  
  public String getFqXmlNameNew() {
    return fqXmlNameNew;
  }
  
  public RefactoringMoveActionParameter setFqXmlNameNew(String fqXmlNameNew) {
    this.fqXmlNameNew = fqXmlNameNew;
    return this;
  }
  
  public String getTargetLabel() {
    return targetLabel;
  }
  
  public RefactoringMoveActionParameter setTargetLabel(String targetLabel) {
    this.targetLabel = targetLabel;
    return this;
  }
  
  public RefactoringTargetRootType getTargetRootType() {
    return targetRootType;
  }
  
  public RefactoringMoveActionParameter setTargetRootType(RefactoringTargetRootType targetRootType) {
    this.targetRootType = targetRootType;
    return this;
  }
  
}
