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

package com.gip.xyna.xfmg.xfctrl.dependencies;



import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;



public final class DependencyRegisterDeploymentHandler implements DeploymentHandler {

  private static final Logger logger = CentralFactoryLogging.getLogger(DependencyRegisterDeploymentHandler.class);

  private final DependencyRegister dependencyRegister;


  DependencyRegisterDeploymentHandler(DependencyRegister dependencyRegister) {
    this.dependencyRegister = dependencyRegister;
  }

  public void exec(GenerationBase object, DeploymentMode mode) {

    // first remove the old entries since some or all may be obsolete 
    DependencyNode toBeRemoved = null;
    if (object instanceof WF) {
      toBeRemoved = new DependencyNode(object.getOriginalFqName(), DependencySourceType.WORKFLOW, object.getRevision());
    } else if (object instanceof DOM) {
      toBeRemoved = new DependencyNode(object.getOriginalFqName(), DependencySourceType.DATATYPE, object.getRevision());
    } else if (object instanceof ExceptionGeneration) {
      toBeRemoved = new DependencyNode(object.getOriginalFqName(), DependencySourceType.XYNAEXCEPTION, object.getRevision());
    }
    if (toBeRemoved != null) {
      dependencyRegister.removeMyUsedObjects(toBeRemoved);
    }

    // now add the updated dependencies
    if (object instanceof WF) {
      dependencyRegister.createEmptyNodeIfDoesNotExist(object.getOriginalFqName(), DependencySourceType.WORKFLOW, object.getRevision());
      handleWorkflowDependency((WF) object);
    } else if (object instanceof DOM) {
      dependencyRegister.createEmptyNodeIfDoesNotExist(object.getOriginalFqName(), DependencySourceType.DATATYPE, object.getRevision());
      handleDatatypeDependency((DOM) object);
    } else if (object instanceof ExceptionGeneration) {
      dependencyRegister.createEmptyNodeIfDoesNotExist(object.getOriginalFqName(), DependencySourceType.XYNAEXCEPTION, object.getRevision());
      handleExceptionDependency((ExceptionGeneration) object);
    }

  }


  private void handleWorkflowDependency(WF wf) {
    Set<GenerationBase> usedObjects = wf.getDirectlyDependentObjects();
    for (GenerationBase gb : usedObjects) {
      DependencySourceType newType;
      if (gb instanceof WF) {
        newType = DependencySourceType.WORKFLOW;
      } else if (gb instanceof DOM) {
        newType = DependencySourceType.DATATYPE;
      } else if (gb instanceof ExceptionGeneration) {
        newType = DependencySourceType.XYNAEXCEPTION;
      } else {
        logger.warn("Found " + GenerationBase.class.getSimpleName() + " object (" + gb.getClass().getSimpleName()
                        + ") that was neither a workflow, exception nor a datatype");
        continue;
      }
      if (!gb.exists()) {
        continue;
      }
      dependencyRegister.addDependency(newType, gb.getOriginalFqName(), gb.getRevision(), DependencySourceType.WORKFLOW, wf
                      .getOriginalFqName(), wf.getRevision());
    }
  }


  private void handleDatatypeDependency(DOM dom) {

    Set<GenerationBase> usedObjects = dom.getDirectlyDependentObjects();
    for (GenerationBase gb : usedObjects) {
      DependencySourceType newType;
      if (gb instanceof WF) {
        newType = DependencySourceType.WORKFLOW;
      } else if (gb instanceof DOM) {
        newType = DependencySourceType.DATATYPE;
      } else if (gb instanceof ExceptionGeneration) {
        newType = DependencySourceType.XYNAEXCEPTION;
      } else {
        logger.warn("Found " + GenerationBase.class.getSimpleName() + " object (" + gb.getClass().getSimpleName()
            + ") that was neither a workflow, exception nor a datatype");
        continue;
      }
      if (!gb.exists()) {
        continue;
      }
      dependencyRegister.addDependency(newType, gb.getOriginalFqName(), gb.getRevision(), DependencySourceType.DATATYPE,
                                       dom.getOriginalFqName(), dom.getRevision());
    }
    dependencyRegister.addDependenciesForAdditionalDependencies(dom.getAdditionalDependencies(),
                                                                DependencySourceType.DATATYPE, dom.getOriginalFqName(),
                                                                dom.getRevision());

    dependencyRegister.addDependenciesToSharedLibs(DependencySourceType.DATATYPE, dom.getOriginalFqName(), dom.getRevision(), dom.getSharedLibs());
  }


  private void handleExceptionDependency(ExceptionGeneration exceptionGen) {
    Set<GenerationBase> usedObjects = exceptionGen.getDirectlyDependentObjects();
    for (GenerationBase gb : usedObjects) {
      DependencySourceType newType;
      if (gb instanceof ExceptionGeneration) {
        newType = DependencySourceType.XYNAEXCEPTION;
      } else if (gb instanceof DOM) {
        newType = DependencySourceType.DATATYPE;
      } else {
        logger.warn("Found " + GenerationBase.class.getSimpleName() + " object (" + gb.getClass().getSimpleName()
                        + ") that was not expected as a dependency for an exception");
        continue;
      }
      if (!gb.exists()) {
        continue;
      }
      dependencyRegister.addDependency(newType, gb.getOriginalFqName(), gb.getRevision(), DependencySourceType.XYNAEXCEPTION,
                                       exceptionGen.getOriginalFqName(), exceptionGen.getRevision());
    }
  }

  public void finish(boolean success) throws XPRC_DeploymentHandlerException {
  }

  @Override
  public void begin() throws XPRC_DeploymentHandlerException {
  }


}
