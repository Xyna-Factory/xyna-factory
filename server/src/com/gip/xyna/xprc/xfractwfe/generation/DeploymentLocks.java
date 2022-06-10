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

package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport.ValueProcessor;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;



public class DeploymentLocks {

  private static final Logger logger = CentralFactoryLogging.getLogger(DeploymentLocks.class);


  private static class DeploymentLockCompound extends ObjectWithRemovalSupport {

    public static final int PERMITS = 100000;
    private final Semaphore semaphore = new Semaphore(PERMITS, true); //fair, weil sonst das "writelock" ewig warten könnte
    private final DeploymentReadLock readLock;
    private final DeploymentWriteLock writeLock;
    protected String reason;


    DeploymentLockCompound(String uniqueName, DependencySourceType type) {
      readLock = new DeploymentReadLock(this);
      writeLock = new DeploymentWriteLock(uniqueName, type, this);
    }


    public DeploymentReadLock getReadLock() {
      return readLock;
    }


    public DeploymentWriteLock getWriteLock() {
      return writeLock;
    }


    void setReason(String reason) {
      this.reason = reason;
    }
    
    String getReason() {
      return reason;
    }


    @Override
    protected boolean shouldBeDeleted() {
      return semaphore.availablePermits() == PERMITS; 
    }
    
    public String toString() {
      return "permits: " + semaphore.availablePermits();
    }
  }


  public static class DeploymentReadLock {

    private DeploymentLockCompound parent;
    

    public DeploymentReadLock(DeploymentLockCompound parent) {
      this.parent = parent;
    }


    public void lock(String reason) {
      try {
        parent.semaphore.acquire();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      parent.setReason(reason);
    }


    public void unlock() {
      parent.semaphore.release();
      if (parent.semaphore.availablePermits() > DeploymentLockCompound.PERMITS) {
        logger.warn("too many unlocks", new Exception());
      }
    }
  }


  public static class DeploymentWriteLock {

    private String uniqueName;
    private DependencySourceType type;
    private DeploymentLockCompound parent;
    

    public DeploymentWriteLock(String uniqueName, DependencySourceType type, DeploymentLockCompound parent) {
      this.uniqueName = uniqueName;
      this.type = type;
      this.parent = parent;
    }


    public void lock(String reason) throws XPRC_ExclusiveDeploymentInProgress {
      if (!parent.semaphore.tryAcquire(DeploymentLockCompound.PERMITS)) {
        throw new XPRC_ExclusiveDeploymentInProgress(this.uniqueName, "Could not get lock for " + uniqueName + " ("
            + type + "). " + parent.getReason() + " already in progress.");
      }

      parent.setReason(reason);
    }


    public void unlock() {
      parent.semaphore.release(DeploymentLockCompound.PERMITS);
      if (parent.semaphore.availablePermits() > DeploymentLockCompound.PERMITS) {
        logger.warn("too many unlocks", new Exception());
      }
    }
  }
  
  private static class MapPerRevisionAndType extends ObjectWithRemovalSupport {
    
    private final DependencySourceType type;
    private MapPerRevisionAndType(DependencySourceType type) {
      this.type = type;
    }
    
    private final ConcurrentMapWithObjectRemovalSupport<String, DeploymentLockCompound> innerMap = new ConcurrentMapWithObjectRemovalSupport<String, DeploymentLockCompound>() {

      private static final long serialVersionUID = 1L;

      @Override
      public DeploymentLockCompound createValue(String key) {
        return new DeploymentLockCompound(key, type);
      }
      
    };

    @Override
    protected boolean shouldBeDeleted() {
      return innerMap.isEmpty();
    }
    
    public String toString() {
      return innerMap.toString();
    }
    
  }
  
  private static class MapPerRevision extends ObjectWithRemovalSupport {
    
    private final ConcurrentMapWithObjectRemovalSupport<DependencySourceType, MapPerRevisionAndType> innerMap = new ConcurrentMapWithObjectRemovalSupport<DependencySourceType, MapPerRevisionAndType>() {

      private static final long serialVersionUID = 1L;

      @Override
      public MapPerRevisionAndType createValue(DependencySourceType key) {
        return new MapPerRevisionAndType(key);
      }
    }; 

    @Override
    protected boolean shouldBeDeleted() {
      return innerMap.isEmpty();
    }
    
    public String toString() {
      return innerMap.toString();
    }
    
  }

  private static ConcurrentMapWithObjectRemovalSupport<Long, MapPerRevision> allLocks = new ConcurrentMapWithObjectRemovalSupport<Long, MapPerRevision>() {

    private static final long serialVersionUID = 1L;

    @Override
    public MapPerRevision createValue(Long key) {
      return new MapPerRevision();
    }
  };
  
  private static RevisionManagement revisionMgmt;


  public static void readLock(final String uniqueName, final DependencySourceType type, final String reason, final Long revision) {    
    doForLockCompound(uniqueName, type, revision, new ValueProcessor<DeploymentLockCompound, Void>() {

      public Void exec(DeploymentLockCompound v) {
        if (logger.isTraceEnabled()) {
          logger.trace("readlock " + uniqueName + "@" + revision + " " + reason);
        }
        v.getReadLock().lock(reason);
        return null;
      }
      
    });
  }


  private static void doForLockCompound(String uniqueName, DependencySourceType type, Long revision,
                                        ValueProcessor<DeploymentLockCompound, Void> vp) {
    if (isNotWorkspace(revision)) {
      return;
    }
    if (type == DependencySourceType.XYNAPROPERTY) {
      return;
    }
    MapPerRevision mpr = allLocks.lazyCreateGet(revision);
    try {
      MapPerRevisionAndType mprat = mpr.innerMap.lazyCreateGet(type);
      try {
        mprat.innerMap.process(uniqueName, vp);
      } finally {
        mpr.innerMap.cleanup(type);
      }
    } finally {
      allLocks.cleanup(revision);
    }

  }

  private static boolean isNotWorkspace(Long revision) {
    if (revisionMgmt == null) {
      revisionMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    }
    return !revisionMgmt.isWorkspaceRevision(revision);
  }

  /**
   * passiert immer mit dem gleichen thread als das zugehörige readlock.
   * TODO: fehler werfen, wenn unlock mit einem anderen thread passiert
   */
  public static void readUnlock(final String uniqueName, DependencySourceType type, final Long revision) {
    doForLockCompound(uniqueName, type, revision, new ValueProcessor<DeploymentLockCompound, Void>() {

      public Void exec(DeploymentLockCompound v) {
        if (logger.isTraceEnabled()) {
          logger.trace("readUnlock " + uniqueName + "@" + revision);
        }
        v.getReadLock().unlock();
        return null;
      }
      
    });
  }
  
  public static void writeLock(final String uniqueName, DependencySourceType type, final String reason, final Long revision)
      throws XPRC_ExclusiveDeploymentInProgress {
    try {
      doForLockCompound(uniqueName, type, revision, new ValueProcessor<DeploymentLockCompound, Void>() {

        public Void exec(DeploymentLockCompound v) {
          if (logger.isTraceEnabled()) {
            logger.trace("writelock " + uniqueName + "@" + revision + " " + reason);
          }
          try {
            v.getWriteLock().lock(reason);
          } catch (XPRC_ExclusiveDeploymentInProgress e) {
            throw new RuntimeException(e);
          }
          return null;
        }

      });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof XPRC_ExclusiveDeploymentInProgress) {
        throw (XPRC_ExclusiveDeploymentInProgress) e.getCause();
      } else {
        throw e;
      }
    }
  }
  
  /**
   * passiert evtl mit einem anderen thread als das zugehörige writelock
   */
  public static void writeUnlock(final String uniqueName, DependencySourceType type, final Long revision) {
    doForLockCompound(uniqueName, type, revision, new ValueProcessor<DeploymentLockCompound, Void>() {

      public Void exec(DeploymentLockCompound v) {
        if (logger.isTraceEnabled()) {
          logger.trace("writeUnlock " + uniqueName + "@" + revision);
        }
        v.getWriteLock().unlock();
        return null;
      }

    });
  }

  

}
