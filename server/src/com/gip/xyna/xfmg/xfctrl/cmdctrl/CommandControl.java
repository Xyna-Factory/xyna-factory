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
package com.gip.xyna.xfmg.xfctrl.cmdctrl;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotLockOperation;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotUnlockOperation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


/**
 * Factory Operationen sollen unter Umständen nicht gleichzeitig laufen. Mit dieser Klasse kann man ausdrücken, dass eine Menge 
 * von Operations vorrübergehend gesperrt sein soll. Das ganze pro Revision.
 * 
 * Zum Sperren von Operations a und b die Methode wlock(eigeneOperation, {a, b}, revision) aufrufen; zum Entsperren wunlock.
 * 
 * Damit die Sperre funktioniert, muss jede betroffene Operation immer tryLock -&gt; unlock aufrufen.
 * 
 * Technisch verbirgt sich dahinter ein read+writeLock-Mechanismus.
 *
 */
public class CommandControl {

  private static final Logger logger = CentralFactoryLogging.getLogger(CommandControl.class);
  
  private static class SingleLock<T> extends ObjectWithRemovalSupport {

    private final ReentrantReadWriteLock l = new ReentrantReadWriteLock();
    private final ReentrantLock syncWriteLock = new ReentrantLock(); //Lock, um sicherzustellen, dass immer nur ein Thread versucht das writeLock zu bekommen
    private T source;

    /**
     * Holt ein writeLock, falls nicht ein anderer Thread die Operation bereits gewrite-locked hat.
     * 
     * @param lockedBy
     * @return null, falls Lock bekommen, sonst Operation, die das writeLock hält
     */
    public boolean writeLock(T lockedBy) {
      if (isReadLockedByCurrentThread()) {
        //eigener Thread hat bereits ein readLock, dies würde zu einem DeadLock führen
        throw new RuntimeException("cannot upgrade readlock to writelock");
      }
      
      //sicherstellen, dass kein anderes writelock in der queue ist
      syncWriteLock.lock();
      try{
        if (l.readLock().tryLock()) {
          l.readLock().unlock();
          l.writeLock().lock(); //auf fremde readlocks warten
          source = lockedBy;
          return true;
        } else {
          return false;
        }
      } finally {
        syncWriteLock.unlock();
      }
    }
    
    /**
     * Holt ein writeLock, falls die Operation nicht durch einen anderen Thread gelocked (read oder write) ist.
     * Dabei wird bis waitUntil auf die Freigabe des Locks gewartet.
     * 
     * @param lockedBy
     * @param waitUntil absoluter Zeitpunkt in Millisekunden, bis zu dem gewartet wird
     * @return true, falls Lock geholt werden konnte
     * @throws InterruptedException
     */
    public boolean tryWriteLock(T lockedBy, long waitUntil) throws InterruptedException {
      if (isReadLockedByCurrentThread()) {
        //eigener Thread hat bereits ein readLock, dies würde zu einem DeadLock führen
        throw new RuntimeException("cannot upgrade readlock to writelock");
      }
      
      //sicherstellen, dass kein anderes writelock in der queue ist
      if (syncWriteLock.tryLock(waitUntil - System.currentTimeMillis(), TimeUnit.MILLISECONDS)) {
        try{
          //auf fremde read- und writeLocks warten
          if (l.writeLock().tryLock(waitUntil - System.currentTimeMillis(), TimeUnit.MILLISECONDS)) {
            source = lockedBy; //Lock bekommen
            return true;
          } else {
            return false; //Lock nicht erhalten
          }
        } finally {
          syncWriteLock.unlock();
        }
      } else {
        return false; //ein anderer Thread wartet gerade auf das writeLock
      }
    }

    public T getLockedBy() {
      return source;
    }

    public void writeUnlock() {
      l.writeLock().unlock();
      source = null;
    }


    @Override
    protected boolean shouldBeDeleted() {
      boolean result = true;
      if (!l.writeLock().tryLock()) {
        //ein anderer thread (oder wir selbst) hat ein read- oder writelock.
        result = false;
      } else {
        //vielleicht haben wir selbst das lock mehrfach?
        l.writeLock().unlock();
        if (l.getWriteHoldCount() > 0) {
          result = false;
        }
      }
      return result;
    }


    public boolean tryReadLock(long timeout, TimeUnit unit) throws InterruptedException {
      if (timeout <= 0) {
        return l.readLock().tryLock();
      } else {
        return l.readLock().tryLock(timeout, unit);
      }
    }


    public void readUnlock() {
      l.readLock().unlock();
    }
    
    /**
     * Überprüft, ob der eigene Thread bereits ein readLock hält.
     */
    private boolean isReadLockedByCurrentThread() {
      return l.getReadHoldCount() > 0;
    }
  }


  private static final Comparator<Object> comparator = new Comparator<Object>() {

    public int compare(Object o1, Object o2) {
      return o1.toString().compareTo(o2.toString());
    }

  };


  private static class LockCollection<T, S> extends ObjectWithRemovalSupport {

    private final ConcurrentMapWithObjectRemovalSupport<T, SingleLock<S>> locks = new ConcurrentMapWithObjectRemovalSupport<T, SingleLock<S>>() {

      private static final long serialVersionUID = -5705646649999114494L;


      @Override
      public SingleLock<S> createValue(T key) {
        return new SingleLock<S>();
      }
    };



    /**
     * sortiert die objekte des arrays anhand toString() und lockt sie dann in dieser reihenfolge
     * @return null, falls alle objekte gelockt werden konnte. ansonsten das objekt, welches nicht gelockt werden konnte
     */
    public Pair<T, S> writeLock(S source, T[] os) {
      Arrays.sort(os, comparator);
      List<T> locked = new ArrayList<T>();
      Pair<T, S> failure = null;
      boolean exception = true;
      try {
        forloop : for (T o : os) {
          SingleLock<S> lock = locks.lazyCreateGet(o);
          if (!lock.writeLock(source)) {
            S lockedBy = lock.getLockedBy();
            while (lockedBy == null) {
              logger.debug("didn't get lock, but owner was null!");
              //evtl. wurde Lock inzwischen freigegeben -> nochmal versuchen es zu bekommen
              if (lock.writeLock(source)) {
                locked.add(o);
                continue forloop;
              } else {
                lockedBy = lock.getLockedBy();
              }
            }
            failure = Pair.of(o, lockedBy);
            break;
          } else {
            locked.add(o); //write-Lock bekommen
          }
        }
        exception = false;
      } finally {
        if (failure != null || exception) {
          for (T o : locked) {
            //ein Lock nicht bekommen, da von anderem Thread gehalten oder Exception
            //-> alle bereits geholten Locks wieder freigeben
            locks.get(o).writeUnlock();
            locks.cleanup(o);
          }
        }
      }
      return failure;
    }

    
    /**
     * WriteLocks für die angegebenen Operations in allen Revisions. Falls eine Operation durch einen anderen Thread
     * gelocked (read oder write) ist, wird maximal bis 'waitUntil' abgewartet, ob das Lock freigegeben wird.
     * 
     * @param source
     * @param os
     * @param waitUntil, absoluter Zeitpunkt in Millisekunden bis zu dem maximal auf die Freigabe des Locks gewartet wird
     * @return
     * @throws InterruptedException
     */
    public Pair<T, S> tryWriteLock(S source, T[] os, long waitUntil) throws InterruptedException {
      Arrays.sort(os, comparator);
      List<T> locked = new ArrayList<T>();
      Pair<T, S> failure = null;
      boolean success = false;
      try {
        for (T o : os) {
          SingleLock<S> lock = locks.lazyCreateGet(o);
          if (lock.tryWriteLock(source, waitUntil)) {
            locked.add(o);
          } else {
            //Lock nicht bekommen
            S lockedBy = lock.getLockedBy();
            failure = Pair.of(o, lockedBy);
            break;
          }
        }
        success = true;
      } finally {
        if (failure != null || !success) {
          for (T o : locked) {
            //Lock nicht bekommen, weil von anderem Thread gehalten oder Exception
            //-> alle bereits geholten Locks wieder freigeben
            locks.get(o).writeUnlock();
            locks.cleanup(o);
          }
        }
      }
      return failure;
    }


    public void writeUnlock(T[] os) {
      for (T o : os) {
        locks.get(o).writeUnlock();
        locks.cleanup(o);
      }
    }


    @Override
    protected boolean shouldBeDeleted() {
      return locks.size() == 0;
    }

    /**
     * @return null falls erfolg, sonst den lockowner
     */
    public S tryReadLock(T o, long timeout, TimeUnit unit) throws InterruptedException {
      SingleLock<S> ol = locks.lazyCreateGet(o);
      S lockowner = null;
      try {
          if (!ol.tryReadLock(timeout, unit)) {
            lockowner = ol.getLockedBy();
            while (lockowner == null) {
              logger.debug("didn't get lock, but owner was null!");
              if (ol.tryReadLock(timeout, unit)) {
                break;
              } else {
                lockowner = ol.getLockedBy();
              }
            }
          }
      } finally {
        if (lockowner != null) {
          locks.cleanup(o);
        }
      }
      return lockowner;
    }


    public void readUnlock(T o) {
      locks.get(o).readUnlock();
    }

  }


  private final static ConcurrentMapWithObjectRemovalSupport<Long, LockCollection<Operation, Operation>> locks =
      new ConcurrentMapWithObjectRemovalSupport<Long, LockCollection<Operation, Operation>>() {

        private static final long serialVersionUID = 8295258544927801615L;


        @Override
        public LockCollection<Operation, Operation> createValue(Long key) {
          return new LockCollection<Operation, Operation>();
        }
      };

  private static RevisionManagement rm;
  static {
    if (XynaFactory.isFactoryServer()) {
      rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    }
  }

  /**
   * readlock in default workingset für diese operation.
   * andere komponenten sollen sich davor schützen, dass nicht gleichzeitig ein prozess am laufen ist, der ein writelock auf der operation benötigt.
   * z.b. clearworkingset oder removeapplication.
   * wirft exceptions, falls nicht gelockt.
   */
  public static void tryLock(Operation op) {
    tryLock(op, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

/*
 * TODO folgende befehle sind noch zu integrieren, weil dort z.b. anhand von ids gearbeitet wird und man die revision noch nicht weiss
 * removecron, modify cron
 * cancel, pause, continue, migrate, refresh batch
 * 
 * nicht notwendig?!:
 * list-befehle
 * process mi
 */
  public enum Operation {
    APPLICATION_BUILD("Build Application"), 
    APPLICATION_COPY_TO_WORKINGSET("Copy Application to Workingset"),
    APPLICATION_CLEAR_WORKINGSET("Clear Workingset"),
    APPLICATION_ADDOBJECT("Add Object to Application"),
    APPLICATION_COPY_CRONS("Copy Cron Like Orders"),
    APPLICATION_COPY_ORDERTYPES("Copy Ordertypes"),
    APPLICATION_MIGRATE_BATCH("Migrate Batchprocess"),
    APPLICATION_DEFINE("Define Application"),
    APPLICATION_EXPORT("Export Application"), 
    APPLICATION_REMOVE_DEFINITION("Remove Application Definition"),
    APPLICATION_REMOVE("Remove Application"), 
    APPLICATION_REMOVE_OBJECT("Remove Object from Application"), 
    APPLICATION_START("Start Application"), 
    APPLICATION_STOP("Stop Application"),
    
    BATCH_START("Start Batchprocess"), 
    BATCH_MODIFY("Modify Batchprocess"), 
    BATCH_CANCEL("Cancel Batchprocess"),
    //migrate ist bei applications dabei
    
    BUILD_SERVICETEMPLATE("Build Servicetemplate"), 
    BUILD_TRIGGERTEMPLATE("Build Triggertmeplate"), 
    BUILD_FILTERTEMPLATE("Build Filtertemplate"), 
    BUILD_MDMJAR("Build MDM Jar"), 
    BUILD_SERVICEDEFINITION_JAR("Build Servicedefinition Jar"),
    
    CRON_CREATE("Create Cron"),
    CRON_MODIFY("Modify Cron"),
    CRON_DELETE("Delete Cron"),
    CRON_ENABLE("Enable Crons"),
    CRON_DISABLE("Disable Crons"),
    //copycrons ist bei applications dabei
    
    DESTINATION_REMOVE("Remove Destination"),
    DESTINATION_SET("Set Destination"), 

    FILTER_ADD("Add Filter"),
    FILTER_DEPLOY("Deploy Filter"),
    FILTER_INSTANCE_DISABLE("Disable Filter Instance"),
    FILTER_INSTANCE_ENABLE("Enable Filter Instance"),
    FILTER_REMOVE("Remove Filter"),
    FILTER_UNDEPLOY("Undeploy Filter"),

    FREQUENCYCONTROLLED_TASK_START("Start frequency controlled task"),
    
    ORDERTYPE_CAPACITY_MAPPING_CREATE("Create Ordertype Capacity Mapping"), 
    ORDERTYPE_CAPACITY_MAPPING_DELETE("Delete Ordertype Capacity Mapping"), 
    ORDERTYPE_CREATE("Create Ordertype"), 
    ORDERTYPE_MODIFY("Modify Ordertype"), 
    ORDERTYPE_DELETE("Delete Ordertype"),

    PACKAGE_INSTALL("Install Package"), 
    PACKAGE_BUILD("Build Package"),
    PACKAGE_UNINSTALL("Uninstall Package"),
    
    SHAREDLIB_RELOAD("Reload Sharedlib"), 
    
    TRIGGER_ADD("Add Trigger"), 
    TRIGGER_DEPLOY("Deploy Trigger"), 
    TRIGGER_MODIFY("Modify Trigger"),
    TRIGGER_INSTANCE_DISABLE("Disable Trigger Instance"), 
    TRIGGER_INSTANCE_ENABLE("Enable Trigger Instance"),
    TRIGGER_REMOVE("Remove Trigger"),
    TRIGGER_UNDEPLOY("Undeploy Trigger"), 
    
    WORKSPACE_CREATE("Create Workspace"),
    WORKSPACE_REMOVE("Remove Workspace"),
    
    XMOM_DATATYPE_DEPLOY("Deploy Datatype"), 
    XMOM_EXCEPTION_DEPLOY("Deploy Exception"), 
    XMOM_WORKFLOW_DEPLOY("Deploy Workflow"), 
    XMOM_SAVE("Save XMOM Object"), 
    XMOM_DATATYPE_UNDEPLOY("Undeploy Datatype"), 
    XMOM_EXCEPTION_UNDEPLOY("Undeploy Exception"),
    XMOM_WORKFLOW_UNDEPLOY("Undeploy Workflow"),
    XMOM_UNDEPLOY("Undeploy XMOM Object"),
    XMOM_DATATYPE_DELETE("Delete Datatype"),
    XMOM_EXCEPTION_DELETE("Delete Exception"), 
    XMOM_WORKFLOW_DELETE("Delete Workflow"),
    XMOM_REFACTORING("XMOM Refactoring"),    
    XMOM_DELETE("Delete XMOM Object"),
    XMOM_ODS_NAME_SET("Set XMOM ODS Name"),

    SERVER_STOP("Stop Xyna Factory");
    ;
    
    private final String label;
    
    private Operation(String label) {
      this.label = label;
    }

    public static Operation[] all() {
      return values();
    }

    /**
     * Liefert alle Operations außer der übergebenen zurück.
     * @param operation
     * @return
     */
    public static Operation[] allExcept(Operation operation) {
      EnumSet<Operation> operations = EnumSet.complementOf(EnumSet.of(operation));
      
      return operations.toArray(new Operation[]{});
    }
    
    
    public String toString() {
      return label;
    }

  }


  /**
   * readlock für diese operation in der revision.
   * andere komponenten sollen sich davor schützen, dass nicht gleichzeitig ein prozess am laufen ist, der ein writelock auf der operation benötigt.
   * z.b. clearworkingset oder removeapplication.
   * wirft exceptions, falls nicht gelockt.
   */
  public static void tryLock(Operation op, long revision) {
    tryLock(op, revision, 0, TimeUnit.SECONDS);
  }

  
  /**
   * readlock für diese operation in dem runtimecontext
   * andere komponenten sollen sich davor schützen, dass nicht gleichzeitig ein prozess am laufen ist, der ein writelock auf der operation benötigt.
   * z.b. clearworkingset oder removeapplication.
   * wirft exceptions, falls nicht gelockt.
   * @throws XFMG_CouldNotLockOperation 
   */
  public static void tryLock(Operation operation, RuntimeContext runtimeContext) throws XFMG_CouldNotLockOperation {
    try {
      long revision = getRevision(runtimeContext);
      tryLock(operation, revision);
    } catch (XynaException e) {
      throw new XFMG_CouldNotLockOperation(operation.toString(), runtimeContext.toString(), e); //TODO unschöne fehlermeldung
    }
  }
  
  private static long getRevision(RuntimeContext runtimeContext) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return rm.getRevision(runtimeContext);
  }


  /**
   * readlock für diese operation in der application.
   * andere komponenten sollen sich davor schützen, dass nicht gleichzeitig ein prozess am laufen ist, der ein writelock auf der operation benötigt.
   * z.b. clearworkingset oder removeapplication.
   * wirft exceptions, falls nicht gelockt werden konnte.
   */
  public static void tryLock(Operation op, long revision, long timeout, TimeUnit unit) {
    LockCollection<Operation, Operation> l = locks.lazyCreateGet(revision);
    Operation lockowner = null;
    boolean success = false;
    try {
      try {
        lockowner = l.tryReadLock(op, timeout, unit);
        success = true;
      } catch (InterruptedException e) {
        throw new RuntimeException("interrupted", e);
      }
    } finally {
      if (lockowner != null || !success) {
        //interrupted oder lock nicht bekommen
        locks.cleanup(revision);
      }
    }
    if (lockowner != null) {
      throw new RuntimeException("Operation " + op + " locked by " + lockowner);
    }
  }


  public static void unlock(Operation op) {
    unlock(op, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public static void unlock(Operation op, long revision) {
    LockCollection<Operation, Operation> l = locks.get(revision);
    l.readUnlock(op);
    locks.cleanup(revision);
  }


  public static void unlock(Operation op, RuntimeContext runtimeContext) throws XFMG_CouldNotUnlockOperation {
    try {
      long revision = getRevision(runtimeContext);
      unlock(op, revision);
    } catch (XynaException e) {
      throw new XFMG_CouldNotUnlockOperation(op.toString(), runtimeContext.toString(), e);
    }
  }


  /**
   * writelock für die angegebenen operations in der angegebenen revision.
   * 
   * verwendung von z.b. clearworkingset oder removeapplication.
   * 
   * @param source lockowner
   * @param toWriteLock welche operations gelockt werden sollen
   * @param revision in welcher revision
   * @return null falls writelock geholt, ansonsten ein paar bestehend aus 
   * 1. die nicht lockbare operation, falls ein anderer thread diese operation gewrite-locked hat
   * 2. die source operation die die nicht lockbare operation gelockt hat
   */
  public static Pair<Operation, Operation> wlock(Operation source, Operation[] toWriteLock, long revision) {
    LockCollection<Operation, Operation> l = locks.lazyCreateGet(revision);

    Pair<Operation, Operation> result = l.writeLock(source, clone(toWriteLock));
    if (result != null) {
      locks.cleanup(revision);
    }
    return result;
  }

  /**
   * WriteLocks für die angegebenen Operations in allen Revisions. Falls eine Operation durch einen anderen Thread
   * gelocked (read oder write) ist, wird das timeout abgewartet, ob das Lock freigegeben wird.
   * 
   * Verwendung von z.B. stop factory
   * 
   * @param source
   * @param toWriteLock
   * @param timout
   * @param unit
   * @return null, falls writeLocks für alle Operations in allen Revisions geholt werden konnten. Ansonsten ein Paar bestehend aus 
   * 1. der nicht lockbaren Operation, falls ein anderer Thread diese Operation geread- oder gewrite-locked hat
   * 2. der Source Operation die die nicht lockbare Operation gelockt hat, falls es ein writeLock ist (null falls es ein readLock ist)
   * @throws InterruptedException
   */
  public static Pair<Operation, Operation> tryWriteLock(Operation source, Operation[] toWriteLock, long timout, TimeUnit unit) throws InterruptedException {
    List<Long> revisions = rm.getAllRevisions();
    Collections.sort(revisions); //sortieren, damit Locks immer in derselben Reihenfolge geholt werden
    List<Long> locked = new ArrayList<Long>();
    Pair<Operation, Operation> result = null;
    boolean success = false;
    try {
      long waitUntil = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timout, unit); //Endzeitpunkt bis zu dem maximal auf Locks gewartet wird
      for (long revision : revisions) {
        LockCollection<Operation, Operation> l = locks.lazyCreateGet(revision);
        boolean suc = false;
        try {
          result = l.tryWriteLock(source, clone(toWriteLock), waitUntil);
          if (result != null) {
              break;  //Locks nicht bekommen -> abbrechen
            } else {
              locked.add(revision); //Locks bekommen -> Locks für nächste Revision holen
            }
          suc = true;
        } finally {
          if (result != null || !suc) {
            //Lock nicht bekommen, weil von anderem Thread gehalten oder Exception -> cleanup
            locks.cleanup(revision); 
          }
        }
      }
      success = true;
    } finally {
      if (result != null || !success) {
        //Lock nicht bekommen, weil von anderem Thread gehalten oder Exception
        //-> alle bereits geholten Locks wieder freigeben
        for (long revision : locked) {
          wunlock(toWriteLock, revision);
        }
      }
    }
    return result;
  }
  

  //damit man das array nach aussen hin unverändert lässt, es aber intern sortieren kann
  private static Operation[] clone(Operation[] ops) {
    Operation[] cloned = new Operation[ops.length];
    System.arraycopy(ops, 0, cloned, 0, ops.length);
    return ops;
  }


  public static void wunlock(Operation[] ops, long revision) {
    LockCollection<Operation, Operation> l = locks.get(revision);
    l.writeUnlock(ops);
    locks.cleanup(revision);
  }

  public static void wunlock(Operation[] ops) {
    List<Long> revisions = rm.getAllRevisions();
    for (long revision : revisions) {
      wunlock(ops, revision);
    }
  }


  public static void tryLock(Operation[] ops, long revision) {
    int locked = 0;
    boolean success = false;
    try {
      for (locked = 0; locked < ops.length; locked++) {
        tryLock(ops[locked], revision);
      }
      success = true;
    } finally {
      if (!success) {
        for (int i = 0; i < locked; i++) {
          unlock(ops[i], revision);
        }
      }
    }
  }


  public static void unlock(Operation[] ops, long revision) {
    for (Operation op : ops) {
      unlock(op, revision);
    }
  }

}
