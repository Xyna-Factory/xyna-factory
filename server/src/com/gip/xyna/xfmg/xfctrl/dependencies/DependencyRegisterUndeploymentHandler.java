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

package com.gip.xyna.xfmg.xfctrl.dependencies;



import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;



public final class DependencyRegisterUndeploymentHandler implements UndeploymentHandler {

  private final DependencyRegister dependencyRegister;


  DependencyRegisterUndeploymentHandler(DependencyRegister dependencyRegister) {
    this.dependencyRegister = dependencyRegister;
  }


  public void exec(GenerationBase object) {

    DependencyNode toBeRemoved = null;
    if (object instanceof WF) {
      toBeRemoved = new DependencyNode(object.getOriginalFqName(), DependencySourceType.WORKFLOW, object.getRevision());
    } else if (object instanceof DOM) {
      toBeRemoved = new DependencyNode(object.getOriginalFqName(), DependencySourceType.DATATYPE, object.getRevision());
    } else if (object instanceof ExceptionGeneration) {
      toBeRemoved = new DependencyNode(object.getOriginalFqName(), DependencySourceType.XYNAEXCEPTION, object.getRevision());
    }
    if (toBeRemoved != null) {
      dependencyRegister.removeAllDependencies(toBeRemoved, object.getRevision());
    }

  }


  public void exec(FilterInstanceStorable object) {
    //beim undeployment muss nichts gemacht werden. beim remove gibt es einen anderen mechanismus
  }


  public void exec(TriggerInstanceStorable object) {
    //beim undeployment muss nichts gemacht werden. beim remove gibt es einen anderen mechanismus
  }


  public void exec(Capacity cap) {
    //es gibt keine capacitities im dependencyregister
  }  


  public void exec(DestinationKey object) {
    //dependency wird im xynadispatcher bereits entfernt
  }


  public void finish() throws XPRC_UnDeploymentHandlerException {
  }

  public boolean executeForReservedServerObjects(){
    return true;
  }


  public void exec(FilterStorable object) {
  }


  public void exec(TriggerStorable object) {
  }
}
