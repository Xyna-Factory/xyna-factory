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
package com.gip.xyna.xdev.xfractmod.xmdm.refactoring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Filter;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_RefactoringFault;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringManagement.RefactoringType;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.AutosaveFilter;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.Path;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.xfractwfe.generation.DeploymentLocks;

/**
 * locks gegen konkurrierende deployments und shutdown des servers
 * gegen laufende auftr�ge wird w�hrend des undeployments unabh�ngig von diesem lock gelockt.
 */
class RefactoringLock {
  
  
  private final RefactoringContext context;
  

  public RefactoringLock(RefactoringContext context) {
    this.context = context;
  }



  public void lock() throws XDEV_RefactoringFault {
    lock(Arrays.<LockAlgorithm<?>>asList(
                       new AutosaveLockAlgorithm(),
                       new XMOMLockAlgorithm(),
                       new DeploymentLockAlgorithm(),
                       new ShutdownLockAlgorithm()));    
  }


  @SuppressWarnings({"rawtypes", "unchecked"})
  public void lock(List<LockAlgorithm<?>> locks) throws XDEV_RefactoringFault {
    for (int i = 0; i < locks.size(); i++) {
      LockAlgorithm lock = locks.get(i);
      Collection entities = lock.getEntitites();
      int count = 0;
      Iterator iter = entities.iterator();
      boolean success = false;
      try {
        while (iter.hasNext()) {
          lock.lock(iter.next());
          count++;
        }
        success = true;
      } catch (XDEV_RefactoringFault t) {
        throw t;
      } catch (Throwable t) {
        Department.handleThrowable(t);
        // TODO can we be more specific?
        throw createFault("UnknownElement", "lock failed", t);
      } finally {
        if (!success) {
          for (int j = count; j >= 0; j--) {
            iter = entities.iterator();
            try {
              while (iter.hasNext()) {
                lock.unlock(iter.next());
              }
            } catch (Throwable tt) {
              Department.handleThrowable(tt);
              // ignore
            }
          }
          if (i > 0) {
            List<LockAlgorithm<?>> unlocks = locks.subList(0, i);
            Collections.reverse(unlocks);
            unlock(unlocks);
          }
        }
      }

    }
  }


  public void unlock() {
    unlock(Arrays.<LockAlgorithm<?>>asList(
                       new ShutdownLockAlgorithm(),
                       new DeploymentLockAlgorithm(),
                       new XMOMLockAlgorithm(),
                       new AutosaveLockAlgorithm()));  
  }
  
  
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void unlock(List<LockAlgorithm<?>> locks) {
    for (int i = 0; i < locks.size(); i++) {
      LockAlgorithm lock = locks.get(i);
      Collection entities = lock.getEntitites();
      Iterator iter = entities.iterator();
      while (iter.hasNext()) {
        try {
          lock.unlock(iter.next());
        } catch (Throwable t) {
          Department.handleThrowable(t);
          // ignore and unlock as much as possible
          // TODO log
        }
      }
      
    }
  }
  
  
  private static XDEV_RefactoringFault createFault(RefactoringElement element, Throwable cause) {
    return createFault(element.fqXmlNameOld, element.type, cause);
  }
  
  private static XDEV_RefactoringFault createFault(String elementName, DependencySourceType type, Throwable cause) {
    return createFault(elementName, RefactoringTargetType.fromDependencyType(type), cause);
  }
  
  private static XDEV_RefactoringFault createFault(String elementName, XMOMDatabaseType type, Throwable cause) {
    return createFault(elementName, RefactoringTargetType.fromXMOMType(type), cause);
  }
  
  private static XDEV_RefactoringFault createFault(String elementName, RefactoringTargetType type, Throwable cause) {
    return createFault(elementName, type.name(), cause);
  }
  
  private static XDEV_RefactoringFault createFault(String elementName, String type, Throwable cause) {
    return new XDEV_RefactoringFault(elementName, type, RefactoringType.MOVE.toString(), cause);
  }
  
  
  private String getCause(String identifier) {
    return "Refactoring of " + identifier;
  }

  
  private static abstract class LockAlgorithm<E> {
    
    protected abstract void lock(E entity) throws XDEV_RefactoringFault;
    
    protected abstract void unlock(E entity) throws XDEV_RefactoringFault;
    
    protected abstract Collection<E> getEntitites();

  }  
  
  
  private class XMOMLockAlgorithm extends LockAlgorithm<XMOMDatabaseSearchResultEntry> {

    private final LockManagement lockMgmt;
    
    protected XMOMLockAlgorithm() {
      lockMgmt = XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement();
    }

    @Override
    protected void lock(XMOMDatabaseSearchResultEntry entity) throws XDEV_RefactoringFault {
      String dependencyFqName = adjustFqName(entity);
      Path path = new Path(dependencyFqName, context.getRevision());
      try {
        if (!lockMgmt.lockXMOM(context.getSessionId(), context.getCreator(), path, getBaseTypeNiceName(entity))) {
          throw createFault(entity.getFqName(), entity.getType() , new Exception("Failed to lock " + dependencyFqName + " (" + context.getRevision() + ")"));
        }
      } catch (XynaException e) {
        throw createFault(entity.getFqName(), entity.getType() , new Exception("Failed to lock " + dependencyFqName + " (" + context.getRevision() + ")"));
      }
    }

    @Override
    protected void unlock(XMOMDatabaseSearchResultEntry entity) throws XDEV_RefactoringFault {
      String dependencyFqName = adjustFqName(entity);
      Path path = new Path(dependencyFqName, context.getRevision());
      try {
        if (!lockMgmt.unlockXMOM(context.getSessionId(), context.getCreator(), path, getBaseTypeNiceName(entity))) {
          throw createFault(entity.getFqName(), entity.getType(), new Exception("Failed to unlock " + dependencyFqName + " (" + context.getRevision() + ")"));
        }
      } catch (XynaException e) {
        throw createFault(entity.getFqName(), entity.getType(), e);
      }
    }

    @Override
    protected Collection<XMOMDatabaseSearchResultEntry> getEntitites() {
      // TODO order to avoid race conditions where two concurrent refactorings might fail at each others locks
      Set<XMOMDatabaseSearchResultEntry> dependencies = context.getSavedDependencies();
      if (context.isRefactorOperation()) {
        Iterator<? extends RefactoringElement> i = context.getRefactoringElements().iterator();
        while (i.hasNext()) {
          RefactoringElement e = i.next();
          dependencies.add(new XMOMDatabaseSearchResultEntry(e.fqXmlNameNew, XMOMDatabaseType.DATATYPE, 1));
        }
      }
      return dependencies;
    }
    
    private String adjustFqName(XMOMDatabaseSearchResultEntry entry) {
      switch (entry.getType()) {
        case DATATYPE :
          return entry.getFqName();
        case SERVICEGROUP :
          return entry.getFqName().substring(0, entry.getFqName().lastIndexOf('.'));
        case OPERATION :
          String serviceName = entry.getFqName().substring(0, entry.getFqName().lastIndexOf('.'));
          return serviceName.substring(0, serviceName.lastIndexOf('.'));
        case EXCEPTION :
          return entry.getFqName();
        case WORKFLOW :
          return entry.getFqName();
        default :
          throw new IllegalArgumentException("The type " + entry.getType() + " is abstract and should not have been received!");
      }
    }
    
    private String getBaseTypeNiceName(XMOMDatabaseSearchResultEntry entry) {
      switch (entry.getType()) {
        case DATATYPE :
        case OPERATION :
        case SERVICEGROUP :
          return XMOMType.DATATYPE.getNiceName();
        case EXCEPTION :
          return XMOMType.EXCEPTION.getNiceName();
        case WORKFLOW :
          return XMOMType.WORKFLOW.getNiceName();
        default :
          throw new IllegalArgumentException("The type " + entry.getType() + " is abstract and should not have been received!");
      }
    }

  }
  
  
  
  private class DeploymentLockAlgorithm extends LockAlgorithm<DependencyNode> {

    @Override
    protected void lock(DependencyNode entity) throws XDEV_RefactoringFault {
      try {
        DeploymentLocks.writeLock(entity.getUniqueName(), entity.getType(), getCause(entity.getUniqueName()), entity.getRevision());
      } catch (XPRC_ExclusiveDeploymentInProgress e) {
        throw createFault(entity.getUniqueName(), entity.getType(), e);
      }
    }

    @Override
    protected void unlock(DependencyNode entity) throws XDEV_RefactoringFault {
      DeploymentLocks.writeUnlock(entity.getUniqueName(), entity.getType(), entity.getRevision());
    }

    @Override
    protected Collection<DependencyNode> getEntitites() {
      // TODO order to avoid race conditions where two concurrent refactorings might fail at each others locks
      return CollectionUtils.filter(context.getDeployedDependencies(), new Filter<DependencyNode>() {

        @Override
        public boolean accept(DependencyNode value) {
          return value.getType() == DependencySourceType.DATATYPE || value.getType() == DependencySourceType.WORKFLOW
              || value.getType() == DependencySourceType.XYNAEXCEPTION;
        }

      });
    }
    
  }
  
  
  private class AutosaveLockAlgorithm extends LockAlgorithm<AutosaveFilter> {

    private final LockManagement lockMgmt;
    
    protected AutosaveLockAlgorithm() {
      lockMgmt = XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement();
    }

    @Override
    protected void lock(AutosaveFilter entity) throws XDEV_RefactoringFault {
      try {
        lockMgmt.registerAutosaveGuard(context.getSessionId(), entity);
      } catch (XynaException e) { // TODO could we be more specific?
        throw createFault(context.getRefactoringElements().iterator().next(), e);
      }
    }

    @Override
    protected void unlock(AutosaveFilter entity) throws XDEV_RefactoringFault {
      lockMgmt.unregisterAutosaveGuard(context.getSessionId());
    }

    @Override
    protected Set<AutosaveFilter> getEntitites() {
      return Collections.<AutosaveFilter>singleton(context.getAutosaveFilter());
    }
    
  }
  
  
  private class ShutdownLockAlgorithm extends LockAlgorithm<XynaFactoryBase> {

    @Override
    protected void lock(XynaFactoryBase entity) throws XDEV_RefactoringFault {
      if (!entity.lockShutdown(getCause(context.getRefactoringElements().iterator().next().fqXmlNameOld))) { // TODO append all fqNames together?
        throw createFault(context.getRefactoringElements().iterator().next(), new RuntimeException("shutdown in progress"));
      }
    }

    @Override
    protected void unlock(XynaFactoryBase entity) throws XDEV_RefactoringFault {
      entity.unlockShutdown();
    }

    @Override
    protected Set<XynaFactoryBase> getEntitites() {
      return Collections.singleton(XynaFactory.getInstance());
    }
    
  }
  

}
