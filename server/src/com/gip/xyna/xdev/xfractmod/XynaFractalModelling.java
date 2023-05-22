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
package com.gip.xyna.xdev.xfractmod;



import com.gip.xyna.Section;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringManagement;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement;



public class XynaFractalModelling extends Section {

  public static final String DEFAULT_NAME = "Xyna Fractal Modelling";


  public XynaFractalModelling() throws XynaException {
    super();
  }


  @Override
  public void init() throws XynaException {
    deployFunctionGroup(new RefactoringManagement());
    deployFunctionGroup(new LockManagement());
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  
  public RefactoringManagement getRefactoringManagement() {
    return (RefactoringManagement) getFunctionGroup(RefactoringManagement.DEFAULT_NAME);
  }


  public LockManagement getLockManagement() {
    return (LockManagement) getFunctionGroup(LockManagement.DEFAULT_NAME);
  }

}
