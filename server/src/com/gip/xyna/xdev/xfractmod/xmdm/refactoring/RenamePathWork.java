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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.Configuration;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.DecomposedWork;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.WorkUnit;


public class RenamePathWork extends BaseWorkCollection<RefactoringElement> {

  private final List<WorkUnit> workUnits;
  
  
  protected RenamePathWork(Collection<RefactoringElement> refactorings, Configuration config) {
    super(refactorings, config);
    workUnits = new ArrayList<WorkUnit>();
    workUnits.add(new RefactorAllReferences(RefactoringTargetType.EXCEPTION, RefactoringTargetType.WORKFLOW,
                                            RefactoringTargetType.DATATYPE, RefactoringTargetType.FORM));
    workUnits.add(new MoveRefactoringTarget(RefactoringTargetType.DATATYPE, RefactoringTargetType.WORKFLOW,
                                            RefactoringTargetType.EXCEPTION, RefactoringTargetType.FORM));
    workUnits.add(new RefactorBaseType(RefactoringTargetType.DATATYPE, RefactoringTargetType.EXCEPTION));
    workUnits.add(new RefactorDataModelInDefinition());
    workUnits.add(new RefactorDataModelInMapping());
    workUnits.add(new RefactorPersistenceReferences());
    workUnits.add(new RefactorSubtypeChoices());
    workUnits.add(new RefactorDatatypeXFLReferences());
    workUnits.add(new RefactorAdditionalDependencies());
    workUnits.add(new RefactorFilterAdditionalDependencies());
    workUnits.add(new RefactorExpectedTypesInServiceCalls());
  }

  @Override
  public List<WorkUnit> getWorkUnits() {
    return workUnits;
  }
  
  
  public static DecomposedWork getAsWork(Collection<RefactoringElement> refactorings, Configuration config) {
    RenamePathWork rpw = new RenamePathWork(refactorings, config);
    DecomposedWork work = new DecomposedWork();
    for (WorkUnit workUnit : rpw.getWorkUnits()) {
      work.addUnit(workUnit);
    }
    return work;
  }

}
