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

package com.gip.xyna.xact.filter.session.modify.operations.copy;



import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.gb.StepMap;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;



//one object per Copy Operation
//members are updated (but not replaced) during the copy process
public class CopyData {

  private StepMap targetStepMap;
  private Dataflow targetDataflow;
  private Dataflow sourceDataflow;
  private Map<String, String> variableIdMap;
  private Map<AVariableIdentification, AVariableIdentification> variableIdentCopies;
  private Map<AVariable, AVariable> variableCopies;


  public CopyData(StepMap targetStepMap, Dataflow targetDataflow, Dataflow sourceDataflow) {
    this.targetStepMap = targetStepMap;
    this.targetDataflow = targetDataflow;
    this.sourceDataflow = sourceDataflow;
    this.variableIdMap = new HashMap<String, String>();
    this.variableIdentCopies = new HashMap<AVariableIdentification, AVariableIdentification>();
    this.variableCopies = new HashMap<AVariable, AVariable>();
  }


  public StepMap getTargetStepMap() {
    return targetStepMap;
  }


  public Map<String, String> getVariableIdMap() {
    return variableIdMap;
  }


  public Dataflow getTargetDataflow() {
    return targetDataflow;
  }


  public Dataflow getSourceDataflow() {
    return sourceDataflow;
  }


  public Map<AVariableIdentification, AVariableIdentification> getVariableIdentCopies() {
    return variableIdentCopies;
  }


  public Map<AVariable, AVariable> getVariableCopies() {
    return variableCopies;
  }

}