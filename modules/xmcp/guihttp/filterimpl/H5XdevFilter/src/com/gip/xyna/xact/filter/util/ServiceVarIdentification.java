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
import com.gip.xyna.xprc.xfractwfe.generation.Service;



//Inputs/Outputs von Service-Aufruf in Workflow
public class ServiceVarIdentification extends ReferencedVarIdentification {

  private final Service service;


  ServiceVarIdentification(Service service, AVariable var) {
    super(var, false);
    this.service = service;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ServiceVarIdentification)) {
      return false;
    }
    ServiceVarIdentification other = (ServiceVarIdentification) obj;
    return Objects.equals(internalGuiId.createId(), other.internalGuiId.createId());
  }


  public static ServiceVarIdentification of(Service service, AVariable var) {
    return new ServiceVarIdentification(service, var);
  }


  @Override
  public String toString() {
    return "ServiceVarId: varId=" + getIdentifiedVariable().getId() + " service(" + service.toString() + ")@" + " var("
        + getIdentifiedVariable().toString() + ") - " + internalGuiId.createId();
  }


  @Override
  public void setLabel(String label) {
    if (service.isPrototype()) {
      getIdentifiedVariable().setLabel(label);
    } else {
      throw new RuntimeException();
    }
  }

  @Override
  public AVariableIdentification createClone() {
    AVariableIdentification clone = new ServiceVarIdentification(service, getIdentifiedVariable());
    clone.connectedness = connectedness;
    clone.idprovider = idprovider;
    clone.internalGuiId = internalGuiId;
    return clone;
  }
  
}
