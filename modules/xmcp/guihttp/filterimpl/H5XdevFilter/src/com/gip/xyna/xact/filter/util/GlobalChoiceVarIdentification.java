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
package com.gip.xyna.xact.filter.util;

import java.util.Objects;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;

//output variablen von choices
public class GlobalChoiceVarIdentification extends AVariableIdentification {

  private final AVariable var;
  private final Integer stepId;
  
  GlobalChoiceVarIdentification(AVariable var, Integer stepId) {
    this.var = var;
    this.stepId = stepId;

    setDeletable(false); // TODO: user-definied outputs should be deletable (PMOD-11, PMOD-87)
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof GlobalChoiceVarIdentification)) {
      return false;
    }
    GlobalChoiceVarIdentification other = (GlobalChoiceVarIdentification) obj;
    return Objects.equals(internalGuiId.createId(), other.internalGuiId.createId());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(internalGuiId.createId());
  }
  
  
  @Override
  public AVariable getIdentifiedVariable() {
    return var;
  }

  
  public static GlobalChoiceVarIdentification of(AVariable var, StepChoice step) {
    return new GlobalChoiceVarIdentification(var, step.getXmlId());
  }
  
  @Override
  public String toString() {
    return "GlobalChoiceVarIdentification: varId="+var.getId()+" ("+stepId+"_" + System.identityHashCode(this) + ")@" + " var(" + var.toString()+")";
  }
  
  @Override
  public void setLabel(String label) {
    getIdentifiedVariable().setLabel(label);
  }
  
  @Override
  public AVariableIdentification createClone() {
    AVariableIdentification clone = new GlobalChoiceVarIdentification(var, stepId);
    clone.connectedness = connectedness;
    clone.idprovider = idprovider;
    clone.internalGuiId = internalGuiId;
    return clone;
  }
}
