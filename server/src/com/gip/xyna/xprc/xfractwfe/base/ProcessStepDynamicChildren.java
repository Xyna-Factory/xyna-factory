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
package com.gip.xyna.xprc.xfractwfe.base;

import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;



public abstract class ProcessStepDynamicChildren<T extends Scope> extends FractalProcessStep<T> {

  private static final long serialVersionUID = -2326702086838015552L;

  public ProcessStepDynamicChildren(int i) {
    super(i);
  }

  @Override
  public void init(T p) {
    this.parentScope = p;
    Scope scope = p;
    while (scope.getParentScope() != null) {
      scope = scope.getParentScope();
    }
    this.parentProcess = (XynaProcess)scope;
  }

  @Override
  /**
   * nicht gecachte kinder ausf�hren
   */
  protected void executeChildren(int i) throws XynaException {
    for (FractalProcessStep<?> step : getChildren(i)) {
      // check whether the step has already been executed before
      if (!step.hasExecutedSuccessfully()) {
        step.execute();
      }
    }
  }

  @Override
  protected void compensateChildren() throws XynaException {
    for (int i = getChildrenTypesLength() - 1; i >= 0; i--) {
      FractalProcessStep[] childSteps = getChildren(i);
      for (int j = childSteps.length - 1; j >= 0; j--) {
        childSteps[j].compensate();
      }
    }
  }


  @Override
  public void prepareForRetryRecursivly(boolean resetExecutionCounters) {
    if (!hasBegunExecution) {
      return;
    }
    reinitialize();
    if (resetExecutionCounters && this instanceof IRetryStep) {
      ((IRetryStep) this).resetExecutionsCounter();
    }
    int l = getChildrenTypesLength();
    if (l > 0) {
      for (int childTypes = 0; childTypes < l; childTypes++) {
        FractalProcessStep[] children = getChildren(childTypes);
        int li = children.length;
        for (int j = 0; j < li; j++) {
          FractalProcessStep<?> step = children[j];
          step.prepareForRetryRecursivly(resetExecutionCounters);
        }
      }
    }
  }

  abstract public List<? extends ForEachScope> getChildSteps();

}
