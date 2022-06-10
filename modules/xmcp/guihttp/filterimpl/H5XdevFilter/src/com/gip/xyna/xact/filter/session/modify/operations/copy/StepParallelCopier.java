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

package com.gip.xyna.xact.filter.session.modify.operations.copy;



import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;



/*package*/ class StepParallelCopier implements IStepCopier<StepParallel> {

  private static IStepCopier<StepSerial> stepSerialCopier = new StepSerialCopier();


  @Override
  public StepParallel copyStep(StepParallel source, ScopeStep parentScope, GenerationBase creator, CopyData cpyData) {
    StepParallel target = new StepParallel(parentScope, creator);
    for (int i = 0; i < source.getChildSteps().size(); i++) {
      StepSerial sourceSerial = (StepSerial) source.getChildSteps().get(i);
      StepSerial targetSerial = stepSerialCopier.copyStep(sourceSerial, parentScope, creator, cpyData);
      target.addChild(i, targetSerial);
    }
    cpyData.getTargetStepMap().addStep(target);
    return target;
  }
}
