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

package com.gip.xyna.xfmg.xopctrl.managedsessions.priviliges;

import java.util.List;

import com.gip.xyna.xfmg.xopctrl.managedsessions.ASessionPrivilege;



public class MDMModificationPrivilige extends ASessionPrivilege {

  private static final long serialVersionUID = -4616813025804454352L;
  
  private final List<String> affectedFullyQualifiedNames;


  public MDMModificationPrivilige(List<String> affectedFullyQualifiedNames) {
    this.affectedFullyQualifiedNames = affectedFullyQualifiedNames;
  }


  @Override
  public boolean isInConflictWith(ASessionPrivilege otherPrivilige) {

    if (otherPrivilige instanceof MDMModificationPrivilige) {
      for (String s : affectedFullyQualifiedNames) {
        List<String> otherPriviligesList = ((MDMModificationPrivilige) otherPrivilige).getAffectedFullyQualifiedNames();
        if (otherPriviligesList.contains(s)) {
          return true;
        }
      }
    }

    return false;

  }

  public List<String> getAffectedFullyQualifiedNames() {
    //return Collections.unmodifiableList(affectedFullyQualifiedNames);
    return affectedFullyQualifiedNames;
  }

}
