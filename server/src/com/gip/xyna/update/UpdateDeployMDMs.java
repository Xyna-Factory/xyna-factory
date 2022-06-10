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
package com.gip.xyna.update;



import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;



/**
 * Falls das Update vor der Initialisierung der Factory-Komponenten ausgeführt wird (ExecutionTime.initialUpdate),
 * werden die Objekte redeployed ohne die deploymenthandler durchzuführen.
 * also nur codegenerierung und compile, etc.
 * ACHTUNG: das funktioniert auch nur, wenn es nicht codegenerierungsänderungen gibt, die vorher in abhängigen objekten durchgeführt werden müssen.
 *          es gab z.b. den bug, dass dieses update fehlschlug, weil super-aufrufe nicht gefunden wurden (die super-typen hatten noch
 *          alten code-stand, weil regenerate-deployed noch nicht gelaufen ist). 
 * 
 * objekte, die vorher nicht deployed waren, werden nicht deployed (wegen den fehlenden deploymenthandlern macht das meist sinn).
 * @deprecated durch modularisierung benötigt man das nicht mehr. statt dessen werden neue versionen von entsprechenden modulen geliefert
 *   die klasse wird noch aufgehoben. es gibt zumindest noch den usecase, dass projekte die basiskomponenten im defaultworkspace haben
 *   und die installation derart ausführen, dass, die objekte geupdated werden können.
 */
@Deprecated
public class UpdateDeployMDMs extends Update {

  private Version newVersion;
  private Version oldVersion;
  private String[] workflows;
  private String[] datatypes;
  private String[] exceptions;
  private boolean mustUpdateGeneratedClasses = false;
  private boolean deploymentIsOptional = false;
  private boolean inheritCodeChanged = false;
  private boolean deployNew = false;


  public UpdateDeployMDMs(Version oldVersion, Version newVersion, String[] workflows, String[] datatypes, String[] exceptions) {
    this.newVersion = newVersion;
    this.oldVersion = oldVersion;
    this.workflows = workflows;
    this.datatypes = datatypes;
    this.exceptions = exceptions;
    setExecutionTime(ExecutionTime.endOfUpdateAndEndOfFactoryStart);
  }


  public UpdateDeployMDMs(Version oldVersion, Version newVersion, String[] workflows, String[] datatypes, String[] exceptions,
                          boolean mustUpdateGeneratedClasses) {
    this(oldVersion, newVersion, workflows, datatypes, exceptions);
    this.mustUpdateGeneratedClasses = mustUpdateGeneratedClasses;
  }


  public UpdateDeployMDMs(Version oldVersion, Version newVersion, String[] workflows, String[] datatypes, String[] exceptions,
                          boolean deploymentIsOptional, boolean mustUpdateGeneratedClasses) {
    this(oldVersion, newVersion, workflows, datatypes, exceptions, mustUpdateGeneratedClasses);
    this.deploymentIsOptional = deploymentIsOptional;
  }


  public UpdateDeployMDMs(Version oldVersion, Version newVersion, String[] workflows, String[] datatypes, String[] exceptions,
                          boolean deploymentIsOptional, boolean inheritCodeChanged, boolean mustUpdateGeneratedClasses) {
    this(oldVersion, newVersion, workflows, datatypes, exceptions, deploymentIsOptional, mustUpdateGeneratedClasses);
    this.inheritCodeChanged = inheritCodeChanged;
  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return mustUpdateGeneratedClasses;
  }


  @Override
  protected Version getAllowedVersionForUpdate() {
    return oldVersion;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return newVersion;
  }

  /**
   * führt dazu, dass die objekte auch deployed werden, wenn sie noch nicht deployed sind.
   */
  public void setDeployNew() {
    deployNew = true;
  }


  @Override
  protected void update() throws XynaException {
    //bei Erstinstallationen darf das Update nicht ausgeführt werden, da die Objekte durch die Modularisierung nicht vorhanden sind
    if (Updater.getInstance().isInitialInstallation()) {
      return;
    }
    
    //falls Ausführung vor Initialisierung der DeplyomentHandler, dann
    //objekte nur deployen, wenn vorher bereits deployed gewesen. ansonsten werden sie lazy deployed
    //wenn sie das erste mal verwendet werden

    //TODO was ist mit deployments in application versions?

    GenerationBase.clearGlobalCache();
    boolean oldValue = GenerationBase.removeFromCache;
    GenerationBase.removeFromCache = true;

    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    WorkflowDatabase wfdb = WorkflowDatabase.getWorkflowDatabasePreInit();
    List<String> deployedWfs = null;
    List<String> deployedDatatypes = null;
    List<String> deployedExceptions = null;
    List<String> implsToCopy = new ArrayList<String>();
    List<GenerationBase> toDeploy = new ArrayList<GenerationBase>();
    try {
      if (getCurrentExecutionTime().mustMockFactory()) {
        UpdateGeneratedClasses.mockFactory();
      }
      try {
        if (workflows != null && workflows.length > 0) {
          for (String fqName : workflows) {
            if (deployedWfs == null) {
              deployedWfs = wfdb.getDeployedWfs().get(VersionManagement.REVISION_WORKINGSET);
            }
            if (deployNew || (deployedWfs != null && deployedWfs.contains(fqName))) {
              WF wf = WF.getInstance(fqName);
              toDeploy.add(wf);
            }
          }
        }
      } catch (XynaException e) {
        if (!deploymentIsOptional) {
          throw e;
        } else {
          logger.warn(null, e);
        }
      } catch (RuntimeException e) {
        if (!deploymentIsOptional) {
          throw e;
        } else {
          logger.warn(null, e);
        }
      }

      try {
        if (datatypes != null && datatypes.length > 0) {
          for (String fqName : datatypes) {
            if (deployedDatatypes == null) {
              deployedDatatypes = wfdb.getDeployedDatatypes().get(VersionManagement.REVISION_WORKINGSET);
            }
            if (deployNew || (deployedDatatypes != null && deployedDatatypes.contains(fqName))) {
              DOM dom = DOM.getInstance(fqName);
              toDeploy.add(dom);
              implsToCopy.add(fqName);
            }
          }
        }
      } catch (XynaException e) {
        if (!deploymentIsOptional) {
          throw e;
        } else {
          logger.warn(null, e);
        }
      } catch (RuntimeException e) {
        if (!deploymentIsOptional) {
          throw e;
        } else {
          logger.warn(null, e);
        }
      }

      try {
        if (exceptions != null && exceptions.length > 0) {
          for (String fqName : exceptions) {
            if (deployedExceptions == null) {
              deployedExceptions = wfdb.getDeployedExceptions().get(VersionManagement.REVISION_WORKINGSET);
            }
            if (deployNew || (deployedExceptions != null && deployedExceptions.contains(fqName))) {
              ExceptionGeneration excep = ExceptionGeneration.getInstance(fqName);
              toDeploy.add(excep);
            }
          }
        }
      } catch (XynaException e) {
        if (!deploymentIsOptional) {
          throw e;
        } else {
          logger.warn(null, e);
        }
      } catch (RuntimeException e) {
        if (!deploymentIsOptional) {
          throw e;
        } else {
          logger.warn(null, e);
        }
      }

      try {
        GenerationBase.deploy(toDeploy, DeploymentMode.codeChanged, inheritCodeChanged, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
        copyImpls(implsToCopy); //kann nach dem deploy passieren, weil keine deploymenthandler aufgerufen werden, die schimpfen würden, dass noch das alte jar beim deployment in deployed liegt.
      } catch (MDMParallelDeploymentException e) {
        if (!deploymentIsOptional) {
          throw e;
        } else {
          logger.warn(null, e);
        }
        Set<String> subSet = new HashSet<String>(implsToCopy);
        for (GenerationBase gb : e.getFailedObjects()) {
          subSet.remove(gb.getOriginalFqName());
        }
        copyImpls(subSet);
      } catch (XynaException e) {
        if (!deploymentIsOptional) {
          throw e;
        } else {
          logger.warn(null, e);
        }
      } catch (RuntimeException e) {
        if (!deploymentIsOptional) {
          throw e;
        } else {
          logger.warn(null, e);
        }
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
      GenerationBase.clearGlobalCache();
      GenerationBase.removeFromCache = oldValue;
    }

  }
  
  
  private void copyImpls(Collection<String> fqNames) {
    if (getCurrentExecutionTime().mustMockFactory()) { //äquivalent zu: offenbar gibts noch keine deploymenthandler und deshalb müssen jars manuell kopiert wrden
      for (String fqName : fqNames) {
        String fqClassName;
        try {
          fqClassName = GenerationBase.transformNameForJava(fqName);
          File savedImpls = new File(GenerationBase.getFileLocationOfServiceLibsForSaving(fqClassName, VersionManagement.REVISION_WORKINGSET));
          if (savedImpls.exists()) {
            // copy possibly changed impl
            FileUtils.copyRecursively(savedImpls, new File(GenerationBase.getFileLocationOfServiceLibsForDeployment(fqClassName, VersionManagement.REVISION_WORKINGSET)));
          }
        } catch (XPRC_InvalidPackageNameException e) {
          logger.warn(null, e);
        } catch (Ex_FileAccessException e) {
          logger.warn(null, e);
        }
      }
    }
  }


}
