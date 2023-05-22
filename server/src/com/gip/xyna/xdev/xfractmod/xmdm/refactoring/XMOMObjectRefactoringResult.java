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

import java.io.Serializable;

import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringManagement.RefactoringType;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.LabelInformation;

public class XMOMObjectRefactoringResult implements Serializable {

  private static final long serialVersionUID = -3107611194479373350L;
  final String fqXmlNameNew;
  final String fqXmlNameOld;
  private final long revision;
  final RefactoringTargetType targetType;
  RefactoringType refactoringType;
  private LabelInformation[] unmodifiedLabels;


  XMOMObjectRefactoringResult(String fqXmlNameNew, String fqXmlNameOld, RefactoringTargetType targetType,
                                      RefactoringType type, LabelInformation[] unmodifiedLabels, long revision) {
    this.fqXmlNameNew = fqXmlNameNew;
    this.fqXmlNameOld = fqXmlNameOld;
    this.refactoringType = type;
    this.targetType = targetType;
    this.unmodifiedLabels = unmodifiedLabels;
    this.revision = revision;
  }


  public String getFqXmlNameNew() {
    return fqXmlNameNew;
  }


  public String getFqXmlNameOld() {
    return fqXmlNameOld;
  }


  public RefactoringType getType() {
    return refactoringType;
  }


  public RefactoringTargetType getRefactoringTargetType() {
    return targetType;
  }


  public LabelInformation[] getUnmodifiedLabels() {
    return unmodifiedLabels;
  }
  
  public long getRevision() {
    return revision;
  }


  void mergeWith(XMOMObjectRefactoringResult o) {
    if (refactoringType != o.refactoringType) {
      switch (refactoringType) {
        case CHANGE :
          refactoringType = o.refactoringType;
          break;
        case MOVE :
          // ntbd
          break;
        default :
          break;
      }
    }
    if (unmodifiedLabels == null || unmodifiedLabels.length <= 0) {
      unmodifiedLabels = o.unmodifiedLabels;
    } else if (o.unmodifiedLabels == null || o.unmodifiedLabels.length <= 0) {
      // ntbd
    } else {
      LabelInformation[] newUnmodifiedLabels = new LabelInformation[unmodifiedLabels.length + o.unmodifiedLabels.length];
      System.arraycopy(unmodifiedLabels, 0, newUnmodifiedLabels, 0, unmodifiedLabels.length);
      System.arraycopy(o.unmodifiedLabels, 0, newUnmodifiedLabels, unmodifiedLabels.length, o.unmodifiedLabels.length);
    }
  }
  
}
