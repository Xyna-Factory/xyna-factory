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

package com.gip.xyna.xact.filter.util;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xfractwfe.generation.Parameter;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.Catchable;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;

public class WorkflowUtils {
  
  
  private WorkflowUtils() {
    
  }
  
  public static void prepareWorkflowForMonitor(Step step) {
    wrapParallelismLanes(step);
    wrapCatchables(step);
  }
  
  public static void prepareWorkflow(Step step) {
    wrapParallelismLanes(step);
    wrapCatchables(step);
  }
  
  /**
   * Wraps every lane of a StepParallel in a StepSerial (in case it's not already wrapped in one).
   * 
   * This is necessary since the GUI treats every lane as a container to drop elements into/remove elements from.
   */
  private static void wrapParallelismLanes(Step step) {
    List<Step> childSteps = step.getChildSteps();
    if (childSteps == null) {
      return;
    }

    for (Step childStep : childSteps) {
      wrapParallelismLanes(childStep);

      if (!(childStep instanceof StepParallel)) {
        continue;
      }

      StepParallel stepParallel = (StepParallel)childStep;
      for (int laneNo = 0; laneNo < stepParallel.getChildSteps().size(); laneNo++) {
        Step parallelismChild = stepParallel.getChildSteps().get(laneNo);
        if ( (parallelismChild instanceof StepSerial) &&
            !(parallelismChild instanceof StepParallel) ) { // necessary, since StepParallel is a sub-class of StepSerial
          continue;
        }

        StepSerial wrapperStep = new StepSerial(parallelismChild.getParentScope(), parallelismChild.getCreator());
        wrapperStep.createEmpty();
        wrapperStep.addChild(0, parallelismChild);
        stepParallel.replaceChild(laneNo, wrapperStep);
      }
    }
  }

  private static void wrapCatchables(Step step) {
    if ( ( (step instanceof StepFunction) && !(((StepFunction)step)).isPrototype() ) ||
         ((step instanceof StepSerial) && (step == step.getParentWFObject().getWfAsStep().getChildStep())) ) {
      Catchable catchableStep = (Catchable)step;
      Step catchProxy = catchableStep.getProxyForCatch();

      // is step already wrapped in a catch step?
      if (!(catchProxy instanceof StepCatch)) {
        StepCatch stepCatch = new StepCatch(step.getParentScope(), step, step.getCreator());
        catchableStep.setCatchStep(stepCatch);

        // copy parameter list to be used in process monitor
        List<Parameter> parameterList = step.getParameterList();
        if (parameterList != null) {
          stepCatch.setParameterList(new ArrayList<Parameter>(step.getParameterList()));
        }

        step.getParentStep().replaceChild(step, stepCatch);
      }
    }

    List<Step> childSteps = step.getChildSteps();
    if (childSteps == null) {
      return;
    }

    for (Step childStep : childSteps) {
      wrapCatchables(childStep);
    }
  }
  
}
