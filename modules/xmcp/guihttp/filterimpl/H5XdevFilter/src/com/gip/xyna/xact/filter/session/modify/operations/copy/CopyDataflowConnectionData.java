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



import java.util.Map;

import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;



//one object per copyDataflowConnection attempt
public class CopyDataflowConnectionData {

  private AVariable variableToConnect;
  private AVariable sourceVar;
  private Dataflow dataflowToUpdate;
  private Dataflow originalDataflow;
  private StepSerial toUpdateGlobalStepSerial; //for constants
  private Map<AVariableIdentification, AVariableIdentification> varCopies;


  public AVariable getVariableToConnect() {
    return variableToConnect;
  }


  public void setVariableToConnect(AVariable variableToConnect) {
    this.variableToConnect = variableToConnect;
  }


  public Dataflow getDataflowToUpdate() {
    return dataflowToUpdate;
  }


  public void setDataflowToUpdate(Dataflow dataflowToUpdate) {
    this.dataflowToUpdate = dataflowToUpdate;
  }


  public Dataflow getOriginalDataflow() {
    return originalDataflow;
  }


  public void setOriginalDataflow(Dataflow originalDataflow) {
    this.originalDataflow = originalDataflow;
  }


  public StepSerial getToUpdateGlobalStepSerial() {
    return toUpdateGlobalStepSerial;
  }


  public void setToUpdateGlobalStepSerial(StepSerial toUpdateGlobalStepSerial) {
    this.toUpdateGlobalStepSerial = toUpdateGlobalStepSerial;
  }


  public AVariable getSourceVar() {
    return sourceVar;
  }


  public void setSourceVar(AVariable sourceVar) {
    this.sourceVar = sourceVar;
  }


  public Map<AVariableIdentification, AVariableIdentification> getVarCopies() {
    return varCopies;
  }


  public void setVarCopies(Map<AVariableIdentification, AVariableIdentification> varCopies) {
    this.varCopies = varCopies;
  }
}
