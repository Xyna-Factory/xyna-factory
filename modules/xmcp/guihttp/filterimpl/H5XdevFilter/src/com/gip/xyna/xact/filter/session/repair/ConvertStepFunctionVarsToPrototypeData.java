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

package com.gip.xyna.xact.filter.session.repair;



import java.util.List;
import java.util.function.Function;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;



/*package*/class ConvertStepFunctionVarsToPrototypeData {


  private StepFunction step;
  private List<AVariable> variables;
  private Function<Integer, String> idGenerator;
  private String[] casts;
  private String[] variableIds;
  private boolean isInput;


  public StepFunction getStep() {
    return step;
  }


  public void setStep(StepFunction step) {
    this.step = step;
  }


  public List<AVariable> getVariables() {
    return variables;
  }


  public void setVariables(List<AVariable> variables) {
    this.variables = variables;
  }


  public Function<Integer, String> getIdGenerator() {
    return idGenerator;
  }


  public void setIdGenerator(Function<Integer, String> idGenerator) {
    this.idGenerator = idGenerator;
  }


  public String[] getCasts() {
    return casts;
  }


  public void setCasts(String[] casts) {
    this.casts = casts;
  }


  public String[] getVariableIds() {
    return variableIds;
  }


  public void setVariableIds(String[] variableIds) {
    this.variableIds = variableIds;
  }


  public boolean getIsInput() {
    return isInput;
  }


  public void setIsInput(boolean isInput) {
    this.isInput = isInput;
  }
}
