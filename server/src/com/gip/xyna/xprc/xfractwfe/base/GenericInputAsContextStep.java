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
package com.gip.xyna.xprc.xfractwfe.base;

import java.util.Optional;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep.FractalProcessStepFilter;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep.FractalProcessStepMarkerInterface;


public interface GenericInputAsContextStep extends FractalProcessStepMarkerInterface {

  public GeneralXynaObject getContextVariable();
  
  public String getContextIdentifier();
  
  public void clearContextVariable();
  
  
  // called from TransactionContext.retrieveFromContext
  public static Optional<GeneralXynaObject> retrieveFromContext(final FractalProcessStep<?> executingStep, String contextIdentifier) {
    GenericInputAsContextStep openContext = executingStep.findMarkedProcessStepInExecutionStack(GenericInputAsContextStep.class, new FractalProcessStepFilter<GenericInputAsContextStep>() {

      public boolean matches(GenericInputAsContextStep step) {
        if (step.getContextIdentifier() != null &&
            step.getContextIdentifier().equals(contextIdentifier) &&
            step.getContextVariable() != null) {
          return true;
        } else {
          return false;
        }
      }
      
    });
    if (openContext == null) {
      return Optional.empty();
    } else {
      return Optional.of(openContext.getContextVariable());
    }
  }
  
}
