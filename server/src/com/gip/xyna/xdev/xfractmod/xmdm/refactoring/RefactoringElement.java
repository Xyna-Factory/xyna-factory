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

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

public class RefactoringElement {
  protected final RefactoringTargetType type;
  protected final String fqXmlNameOld;
  protected final String packageOld;
  protected final String nameOld;
  protected final String labelOld;
  protected final String fqXmlNameNew;
  protected final String packageNew;
  protected final String nameNew;
  protected final String labelNew;
  
  public RefactoringElement(String fqXmlNameOld, String fqXmlNameNew, String newLabel, String oldLabel, RefactoringTargetType type) {
    this.fqXmlNameOld = fqXmlNameOld;
    this.packageOld = GenerationBase.getPackageNameFromFQName(fqXmlNameOld);
    this.nameOld = GenerationBase.getSimpleNameFromFQName(fqXmlNameOld);
    if (oldLabel == null || oldLabel.length() <= 0) {
      oldLabel = nameOld;
    }
    this.labelOld = oldLabel;
    this.fqXmlNameNew = fqXmlNameNew;
    this.packageNew = GenerationBase.getPackageNameFromFQName(fqXmlNameNew);
    this.nameNew = GenerationBase.getSimpleNameFromFQName(fqXmlNameNew);
    if (newLabel == null || newLabel.length() <= 0) {
      this.labelNew = nameNew;
    } else {
      this.labelNew = newLabel;
    }
    this.type = type;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof RefactoringElement)) {
      return false;
    }
    return fqXmlNameOld.equals(((RefactoringElement)obj).fqXmlNameOld);
  }
  
  @Override
  public int hashCode() {
    return fqXmlNameOld.hashCode();
  }
  
  @Override
  public String toString() {
    return "Refactor old=" + fqXmlNameOld + ", new=" + fqXmlNameNew;
  }
}
