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
package com.gip.xyna.xfmg.xfctrl.classloading;

import java.io.File;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xact.trigger.CommandWithFolderBackup;
import com.gip.xyna.xact.trigger.CommandWithFolderBackup.InternalException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.OrderEntryInterfacesCouldNotBeClosedException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;


public class SharedLibDeploymentAlgorithm {
  
  public static void deploySharedLib(final String name, final Long revision) throws XFMG_ClassLoaderRedeploymentException,
      XFMG_SHARED_LIB_NOT_FOUND, OrderEntryInterfacesCouldNotBeClosedException {
    deploySharedLib(name, revision, new SingleRepositoryEvent(revision));
  }

  public static void deploySharedLib(final String name, final Long revision, RepositoryEvent repositoryEvent) throws XFMG_ClassLoaderRedeploymentException,
    XFMG_SHARED_LIB_NOT_FOUND, OrderEntryInterfacesCouldNotBeClosedException {
    if (".".equals(name)) {
      throw new XFMG_SHARED_LIB_NOT_FOUND(name);
    }
    
    File savedSharedLibFolder = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision, false) + name);
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    if (revisionManagement.isWorkspaceRevision(revision) && savedSharedLibFolder.exists()) {
      if (isRedeployment(name, revision)) {
        CommandWithFolderBackup cwfb = new CommandWithFolderBackup() {
          
          @Override
          protected void rollbackFailureTreatment(Throwable t) throws InternalException {
            // anything to cleanup?
          }
        
          @Override
          protected void executeInternally() throws InternalException {
            boolean reloadOnly = getCurrentPhase() == ExecutionPhase.ROLLBACK;
            try {
              getClassLoaderDispatcher().reloadSharedLib(name, revision, reloadOnly ? null : getLibExchangingSwitcher(name, revision));
            } catch (XFMG_ClassLoaderRedeploymentException e) {
              throw new InternalException(e);
            } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
              throw new InternalException(e);
            } catch (OrderEntryInterfacesCouldNotBeClosedException e) {
              throw new InternalException(e);
            }
          }
        };
        try {
          File deploymentSharedLibFolder = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision) + name);
          cwfb.execute(deploymentSharedLibFolder);
        } catch (Ex_FileAccessException e) {
          throw new RuntimeException(e);
        } catch (XACT_JarFileUnzipProblem e) {
          throw new RuntimeException(e);
        } catch (InternalException e) {
          checkAndThrowSignatureExceptions(e.getCause());
        }
      } else { // initial deployment
        try {
          getLibExchangingSwitcher(name, revision).switchClassLoader();
          getClassLoaderDispatcher().getSharedLibClassLoaderLazyCreate(name, revision);
        } catch (XynaException e) {
          checkAndThrowSignatureExceptions(e);
        }
        
      }
      
      repositoryEvent.addEvent(new ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent(EventType.SHAREDLIB_DEPLOY, name));
    } else {
      boolean success = false;
      try {
        Pair<Operation, Operation> failure =
          CommandControl.wlock(CommandControl.Operation.SHAREDLIB_RELOAD,
                               ClassLoaderDispatcher.operationsToLockForReloadSharedLib, revision);
        if (failure != null) {
          throw new RuntimeException(failure.getFirst() + " could not be locked because it is locked by another process of type "
                 + failure.getSecond() + ".");
        }
        try {
          getClassLoaderDispatcher().reloadSharedLib(name, revision, null);
          success = true;
        } finally {
          CommandControl.wunlock(ClassLoaderDispatcher.operationsToLockForReloadSharedLib, revision);
        }
      } finally {
        if (!success) {
          // anything to cleanup ?
        }
      }
    }
  }
  
  
  private final static void checkAndThrowSignatureExceptions(Throwable t) throws XFMG_ClassLoaderRedeploymentException,
    XFMG_SHARED_LIB_NOT_FOUND, OrderEntryInterfacesCouldNotBeClosedException {
    if (t instanceof XFMG_ClassLoaderRedeploymentException) {
      throw (XFMG_ClassLoaderRedeploymentException)t;
    }
    if (t instanceof XFMG_SHARED_LIB_NOT_FOUND) {
      throw (XFMG_SHARED_LIB_NOT_FOUND)t;
    }
    if (t instanceof OrderEntryInterfacesCouldNotBeClosedException) {
      throw (OrderEntryInterfacesCouldNotBeClosedException)t;
    }
    throw new RuntimeException(t);
  }


  private static boolean isRedeployment(String name, Long revision) {
    return getClassLoaderDispatcher().getClassLoaderByType(ClassLoaderType.SharedLib, name, revision) != null;
  }


  private static ClassLoaderDispatcher getClassLoaderDispatcher() {
    return XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getClassLoaderDispatcher();
  }
  
  
  private static ClassLoaderSwitcher getLibExchangingSwitcher(final String sharedLibName, final Long revision) {
    return new ClassLoaderSwitcher() {
      
      public void switchClassLoader() throws XynaException {
        File savedSharedLibFolder = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision, false) + sharedLibName);
        File deploymentSharedLibFolder = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision) + sharedLibName);
        FileUtils.deleteDirectoryRecursively(deploymentSharedLibFolder);
        FileUtils.copyRecursively(savedSharedLibFolder, deploymentSharedLibFolder);
      }
    };
  }
  

}
