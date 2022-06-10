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
package com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagementInterface.StepStatus;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderBackupHelperProcessAbstract.PrioritizedRootId;



public class OrderStartupAndMigrationManagement {

  protected static final Logger logger = CentralFactoryLogging.getLogger(OrderStartupAndMigrationManagement.class);

  private static volatile OrderStartupAndMigrationManagement instance = null;


  private volatile boolean migrationRunning, loadingAtStartupRunning, abortWaiting;
  private Thread loadingAtStartupThread;
  private Thread migratingThread;
  private MigrateMIsAndCronsAndOrderBackups migratingHelperProcess;
  private LoadOrderBackupWithOwnBindingAndDifferentBootCountId loadingAtStartungHelperProcess;
  private List<PrioritizedRootId> migrateDirectlyList;
  private List<PrioritizedRootId> loadDirectlyList;
  private Object timeoutWaitObject, waitUntilStartupObject;
  private int ownBinding;
  private ReentrantLock migratingLock, loadingLock;
  private Throwable migrationAbortedException, loadingAbortedException;
  private boolean loadingAtStartupWasRunning;


  private OrderStartupAndMigrationManagement(int ownBinding) {
    timeoutWaitObject = new Object();
    waitUntilStartupObject = new Object();
    migrateDirectlyList = Collections.synchronizedList(new LinkedList<PrioritizedRootId>());
    loadDirectlyList = Collections.synchronizedList(new LinkedList<PrioritizedRootId>());
    migratingLock = new ReentrantLock();
    loadingLock = new ReentrantLock();
    this.ownBinding = ownBinding;
    loadingAtStartupWasRunning = false;
  }
  

  public void startLoadingAtStartup(ClusterState clusterState) {
    if(logger.isDebugEnabled()) {
      logger.debug("called startLoading with clusterstate <" + clusterState + ">");
    }
    if(clusterState ==  ClusterState.CONNECTED || clusterState ==  ClusterState.SINGLE 
                    || clusterState ==  ClusterState.NO_CLUSTER || clusterState ==  ClusterState.DISCONNECTED_MASTER) {
    
      loadingLock.lock();
      try {
        // Prüfen, ob Thread noch läuft ... vllt. ist der Status veraltet?
        if(loadingAtStartupThread != null && loadingAtStartupRunning) {
          if(!loadingAtStartupThread.isAlive()) {
            loadingAtStartupThread = null;
            loadingAtStartungHelperProcess = null;
            loadingAtStartupRunning = false;
          }            
        }
        if(loadingAtStartupRunning) {
          logger.warn("The orderbackup is already loading. Abort this request.");
          return;
        }
        loadingAtStartungHelperProcess = new LoadOrderBackupWithOwnBindingAndDifferentBootCountId(loadDirectlyList, ownBinding);
        loadingAtStartupRunning = true;
        loadingAtStartupThread = new Thread(new Runnable() {
          
          public void run() {
            try {
              XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement()
                        .registerStep(StepStatus.POSTSTARTUP, LoadOrderBackupWithOwnBindingAndDifferentBootCountId.COMPONENTDISPLAYNAME, null);
              loadingAbortedException = null;
              synchronized (waitUntilStartupObject) {
                waitUntilStartupObject.notifyAll();
              }
              loadingAtStartungHelperProcess.runInternally();
            } catch(Throwable e) {
              Department.handleThrowable(e);
              logger.error("Error while loading order backup at startup.", e);
              loadingAbortedException = e;
            } finally {
              loadingLock.lock();
              try {
                loadingAtStartungHelperProcess = null;
                loadingAtStartupRunning = false;
                // evt. letzte Elemente notifizieren
                synchronized (loadDirectlyList) {
                  for( PrioritizedRootId pri : loadDirectlyList) {
                    pri.countDown();
                  }
                  loadDirectlyList.clear();
                }                
              } finally {
                loadingLock.unlock();
              }
              XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement()
                              .deregisterStep(LoadOrderBackupWithOwnBindingAndDifferentBootCountId.COMPONENTDISPLAYNAME);
            }
          }
        }, "LoadingOrderBackupAtStartupThread");
        
        loadingAtStartupWasRunning = true;
        loadingAtStartupThread.start();
        
      } finally {
        loadingLock.unlock();
      }
    }
    
  }
  
  public synchronized void startMigrating(ClusterState clusterState, long waitTime) {
    if(logger.isDebugEnabled()) {
      logger.debug("called startMigrating with clusterstate <" + clusterState + "> and wait time " + waitTime);
    }
    
    if(clusterState ==  ClusterState.DISCONNECTED_MASTER) {
      migratingLock.lock();
      try {
        // Prüfen, ob Thread noch läuft ... vllt. ist der Status veraltet?
        if(migratingThread != null && !migratingThread.isAlive()) {
          migratingThread = null;
          migratingHelperProcess = null;
          migrationRunning = false;      
        }
        if(migrationRunning) {
          // TODO wird stoppen der Migration 100%ig unterstützt?
          //logger.debug("Stop running migration.");
          //stopMigrating();
          return;
        }
        
        migrationRunning = true;  
        
        if (waitTime != 0) {
          abortWaiting = false;
          XynaFactory.getInstance()
                          .getFactoryManagement()
                          .getXynaExtendedStatusManagement()
                          .registerStep(StepStatus.MIGRATION, MigrateMIsAndCronsAndOrderBackups.COMPONENTDISPLAYNAME,
                                          "Waiting for Timeout.");
          
          synchronized (timeoutWaitObject) {
            migratingLock.unlock();
            try {
              timeoutWaitObject.wait(waitTime);
            } catch (InterruptedException e) {
              migrationRunning = false;
              return; // wenn interrupted, dann brechen wir komplett ab ...
            }
          }
          migratingLock.lock();
        
          if(abortWaiting) {
            // offensichtlich wurde während der Wartezeit abgebrochen. 
            logger.debug("Stop running migration while waiting.");
            migrationRunning = false;
            XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement()
                .deregisterStep(MigrateMIsAndCronsAndOrderBackups.COMPONENTDISPLAYNAME);
            return;
          }
        }
        
        migratingHelperProcess = new MigrateMIsAndCronsAndOrderBackups(migrateDirectlyList, ownBinding);
        migratingThread = new Thread(new Runnable() {
          
          public void run() {
            try {
              XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement()
                              .registerStep(StepStatus.MIGRATION, MigrateMIsAndCronsAndOrderBackups.COMPONENTDISPLAYNAME, null);
              migrationAbortedException = null;
              migratingHelperProcess.runInternally();
            } catch(Throwable e) {
              Department.handleThrowable(e);
              logger.error("Error while migrating order backup.", e);
              migrationAbortedException = e;
            } finally {
              migratingLock.lock();
              try {
                migratingHelperProcess = null;
                migrationRunning = false;
                // evt. letzte Elemente notifizieren
                synchronized (migrateDirectlyList) {
                  for( PrioritizedRootId pri : migrateDirectlyList) {
                    pri.countDown();
                  }
                  migrateDirectlyList.clear();
                }
              } finally {
                migratingLock.unlock();
              }
              XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement()
                              .deregisterStep(MigrateMIsAndCronsAndOrderBackups.COMPONENTDISPLAYNAME);
              migratingThread = null;
            }
          }
          
        }, "MigrationOrderBackupThread");
        
        migratingThread.start();
        
      } finally {
        if(migratingLock.isHeldByCurrentThread()) {
          migratingLock.unlock();
        }
      }
    }
  }
  
  private void waitUntilStartupIsRunning() {
    loadingLock.lock();
    try {
      if (!loadingAtStartupWasRunning) {
        synchronized (waitUntilStartupObject) {
          try {
            if (logger.isDebugEnabled()) {
              logger.debug("Loading thread is still not started.");
            }
            loadingLock.unlock();
            waitUntilStartupObject.wait();
          } catch (InterruptedException e) {
            return;
          }
        }
      }
    } finally {
      if (loadingLock.isLocked() && (loadingLock.isHeldByCurrentThread())) {
        loadingLock.unlock();
      }
    }
  }

  public boolean waitUntilRootOrderIsAccessible(long rootOrderId) throws LoadingAbortedWithErrorException, MigrationAbortedWithErrorException, InterruptedException {
    
    waitUntilStartupIsRunning();
    //LoadOrderBackupWithOwnBindingAndDifferentBootCountId ist nun sicher gelaufen oder läuft noch
    
    LoadOrderBackupWithOwnBindingAndDifferentBootCountId loadProcess = loadingAtStartungHelperProcess;
    while( loadProcess != null ) {
      if (loadingAbortedException != null) {
        throw new LoadingAbortedWithErrorException(loadingAbortedException);
      }
      boolean loaded = loadProcess.waitFor(rootOrderId);
      if( loaded ) {
        return true; //Orderbackup wurde prozessiert
      } else {
        //Retry
        loadProcess = loadingAtStartungHelperProcess;
      }
    }
    //loadProcess ist null, daher ist LoadOrderBackup.. schon gelaufen.
    //weiter mit MigrateMIsAndCronsAndOrderBackups
    
    MigrateMIsAndCronsAndOrderBackups migrateProcess = migratingHelperProcess;
    while( migrateProcess != null ) {
      if (migrationAbortedException != null) {
        throw new MigrationAbortedWithErrorException(migrationAbortedException);
      }
      boolean migrated = migrateProcess.waitFor(rootOrderId);
      if( migrated ) {
        return true; //Orderbackup wurde prozessiert
      } else {
        //Retry
        migrateProcess = migratingHelperProcess;
      }
    }
    //migrateProcess ist null, daher ist MigrateMIsAndCronsAndOrderBackups bereits gelaufen 
    //oder wird nicht laufen.
    
    //Orderbackup wurde nicht oder nicht erkennbar prozessiert
    return false;
  }


  public void pauseLoadingAtStartup() {
    if (!loadingAtStartupRunning) {
      return;
    }
    logger.debug("called pauseLoadingAtStartup");
    LoadOrderBackupWithOwnBindingAndDifferentBootCountId process;
    loadingLock.lock();
    try {
      process = loadingAtStartungHelperProcess;
    } finally {
      loadingLock.unlock();
    }
    if (process != null) {
      process.pause();
    }
  }


  public void resumeLoadingAtStartup() {
    if (!loadingAtStartupRunning) {
      return;
    }
    logger.debug("called resumeLoadingAtStartup");
    loadingLock.lock();
    try {
      if (loadingAtStartungHelperProcess != null) {
        loadingAtStartungHelperProcess.resume();
      }
    } finally {
      loadingLock.unlock();
    }
  }


  public void stopMigrating() {
    if (!migrationRunning) {
      return;
    }
    logger.debug("called stopMigrating");
    try {
      //nicht ewig am lock hängen. wenn benötigt, wird diese methode von aussen erneut aufgerufen
      if (!migratingLock.tryLock(30, TimeUnit.MILLISECONDS)) {
        return;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    try {
      abortWaiting = true;
      // sicherheitshalber Wartezeitwarter benachichtigen
      synchronized (timeoutWaitObject) {
        timeoutWaitObject.notify();
      }
      if (migratingHelperProcess != null) {
        migratingHelperProcess.stopWorking();
      }
    } finally {
      migratingLock.unlock();
    }
  }


  public static OrderStartupAndMigrationManagement getInstance(int ownBinding) {
    if (instance == null) {
      synchronized (OrderStartupAndMigrationManagement.class) {
        if (instance == null) {
          instance = new OrderStartupAndMigrationManagement(ownBinding);
        }
      }
    }
    return instance;
  }


  public static OrderStartupAndMigrationManagement getInstance() {
    if (instance == null) {
      synchronized (OrderStartupAndMigrationManagement.class) {
        if (instance == null) {
          throw new RuntimeException(OrderStartupAndMigrationManagement.class + " is not correctly configured.");
        }
      }
    }
    return instance;
  }


  public boolean isMigrationRunning() throws MigrationAbortedWithErrorException {
    if (migrationAbortedException != null) {
      throw new MigrationAbortedWithErrorException(migrationAbortedException);
    }
    return migrationRunning;
  }


  public boolean isLoadingAtStartupRunning() throws LoadingAbortedWithErrorException {
    if (loadingAbortedException != null) {
      throw new LoadingAbortedWithErrorException(loadingAbortedException);
    }
    return loadingAtStartupRunning || !loadingAtStartupWasRunning;
  }


  public boolean isWaitingForMigration() {
    migratingLock.lock();
    try {
      if (migrationRunning && migratingThread == null) {
        return true;
      } else {
        return false;
      }

    } finally {
      migratingLock.unlock();
    }
  }

  public boolean needMigrationThreadOrLoadingThreadConnections() {
    boolean result = false;
    if(loadingAtStartupRunning) {
      loadingLock.lock();
      try {
        if(loadingAtStartupRunning && loadingAtStartungHelperProcess != null) {
          result = loadingAtStartungHelperProcess.hasConnection();
        }
      } finally {
        loadingLock.unlock();
      }
    }
    if(migrationRunning && !result) {
      migratingLock.lock();
      try {
        if(migrationRunning && migratingHelperProcess != null) {
          result = migratingHelperProcess.hasConnection();
        }
      } finally {
        migratingLock.unlock();
      }
    }
    return result;
  }

  public static class MigrationAbortedException extends Exception {

    private static final long serialVersionUID = -6234982658984070077L;

  }
  
  public static class MigrationAbortedWithErrorException extends Exception {
    
    private static final long serialVersionUID = -6234982658984070077L;
    
    public MigrationAbortedWithErrorException(Throwable e) {
      super(e);
    }
    
  }
  
  public static class LoadingAbortedWithErrorException extends Exception {
    
    private static final long serialVersionUID = -6234982658984070077L;
    
    public LoadingAbortedWithErrorException(Throwable e) {
      super(e);
    }
    
  }

  public void clearError() {
    if (migrationAbortedException != null) {
      logger.info("clearing migrationAbortedException");
      if (logger.isDebugEnabled()) {
        logger.debug("exception was:", migrationAbortedException);
      }
    }
    migrationAbortedException = null;
  }

}
