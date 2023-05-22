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

package com.gip.xyna.xact.filter.util;



import java.util.Objects;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable;



public class ReferencedVarIdentification extends AVariableIdentification {

  private final AVariable var;
  private final boolean isForeachOutput;

  ReferencedVarIdentification(AVariable var, boolean isForeachOutput) {
    this.var = var;
    this.isForeachOutput = isForeachOutput;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ReferencedVarIdentification)) {
      return false;
    }
    ReferencedVarIdentification other = (ReferencedVarIdentification) obj;
    return Objects.equals(internalGuiId.createId(), other.internalGuiId.createId());
  }


  @Override
  public int hashCode() {
    return Objects.hash(internalGuiId.createId());
  }

  public static ReferencedVarIdentification of(AVariable var) {
    return new ReferencedVarIdentification(var, false);
  }

  public static ReferencedVarIdentification of(AVariable var, boolean isForeachOutput) {
    return new ReferencedVarIdentification(var, isForeachOutput);
  }


  @Override
  public String toString() {
    return "ReferencedVarId: varId=" + getIdentifiedVariable().getId() + " var(" + getIdentifiedVariable().toString() + ")";
  }


  public AVariable getIdentifiedVariable() {
    return var;
  }


  @Override
  public void setLabel(String label) {
    getIdentifiedVariable().setLabel(label);
  }
  
  @Override
  public AVariableIdentification createClone() {
    AVariableIdentification clone = new ReferencedVarIdentification(var, isForeachOutput);
    clone.connectedness = connectedness;
    clone.idprovider = idprovider;
    clone.internalGuiId = internalGuiId;
    return clone;
  }

  @Override
  public boolean isReadonly() {
    return super.isReadonly() || isForeachOutput;
  }

  public boolean isForeachOutput() {
    return isForeachOutput;
  }
  
}
