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
package com.gip.xyna.coherence;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.gip.xyna.coherence.analysis.consistency.ConsistencyCheckResult;
import com.gip.xyna.coherence.analysis.consistency.XynaCoherenceClusterConsistencyAnalysis;
import com.gip.xyna.coherence.analysis.performance.ThreadPoolInformation;
import com.gip.xyna.coherence.analysis.performance.XynaCoherenceClusterPerformanceAnalysis;
import com.gip.xyna.coherence.coherencemachine.ActionHandler;
import com.gip.xyna.coherence.coherencemachine.BulkCommitAction;
import com.gip.xyna.coherence.coherencemachine.CoherenceAction;
import com.gip.xyna.coherence.coherencemachine.CoherenceActionType;
import com.gip.xyna.coherence.coherencemachine.CoherenceObject;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.CoherenceState;
import com.gip.xyna.coherence.coherencemachine.CreateObjectAction;
import com.gip.xyna.coherence.coherencemachine.DeleteObjectAction;
import com.gip.xyna.coherence.coherencemachine.LockedObjectAction;
import com.gip.xyna.coherence.coherencemachine.ObjectChangeEvent;
import com.gip.xyna.coherence.coherencemachine.PushObjectAction;
import com.gip.xyna.coherence.coherencemachine.ReadMissException;
import com.gip.xyna.coherence.coherencemachine.ReadObjectAction;
import com.gip.xyna.coherence.coherencemachine.RemoteActionRequest;
import com.gip.xyna.coherence.coherencemachine.SynchronousRequest;
import com.gip.xyna.coherence.coherencemachine.UpdateObjectAction;
import com.gip.xyna.coherence.coherencemachine.WaitForActiveThreadsRequest;
import com.gip.xyna.coherence.coherencemachine.interconnect.InitialConnectionData;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCallee;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCalleeProvider;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectProtocol;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProvider;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.ThreadType;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.InterconnectRMIClassLoader;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockAwaitResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockRequestResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockRequest;
import com.gip.xyna.coherence.coherencemachine.locking.ObjectDeletedWhileWaitingForLockException;
import com.gip.xyna.coherence.coherencemachine.locking.RecallLockRequest;
import com.gip.xyna.coherence.coherencemachine.locking.UnlockRequest;
import com.gip.xyna.coherence.exceptions.ClusterInconsistentException;
import com.gip.xyna.coherence.exceptions.InvalidStaticObjectIDException;
import com.gip.xyna.coherence.exceptions.ObjectIDNotUniqueInCacheException;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.ha.BulkActionContainer;
import com.gip.xyna.coherence.ha.BulkActionElement;
import com.gip.xyna.coherence.ha.BulkUpdateAction;
import com.gip.xyna.coherence.management.ClusterMember;
import com.gip.xyna.coherence.management.ClusterMembers;
import com.gip.xyna.coherence.management.ClusterNodeState;
import com.gip.xyna.coherence.management.ClusterState;
import com.gip.xyna.coherence.management.NodeInformation;
import com.gip.xyna.coherence.management.ObjectPoolInformation;
import com.gip.xyna.coherence.storage.LocalCacheMemoryImpl;
import com.gip.xyna.coherence.storage.StorageInterface;
import com.gip.xyna.coherence.utils.debugging.Debugger;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;
import com.gip.xyna.coherence.utils.threadpool.ThreadPoolExecutorWithThreadlocalsCleanup;



public final class CacheControllerImpl
    implements
      CacheController,
      InterconnectProtocol,
      XynaCoherenceClusterPerformanceAnalysis,
      XynaCoherenceClusterConsistencyAnalysis,
      InternalStaticObjectIDs {

  private static final boolean enableNDC = false;

  private static final Logger logger = LoggerFactory.getLogger(CacheControllerImpl.class);
  private static final Debugger debugger = Debugger.getDebugger();



  /**
   * niedrigste cluster-id
   */
  private static final int CLUSTER_ID_START = 0;
  

  // TODO prio3: konfigurierbarer Offset nach Verbrauchen eines ID-Kontingents. F�r HA-Szenarien muss beachtet werden,
  // dass die Realisierung eindeutiger IDs mit Hilfe von kleinen Kontingenten nicht funktioniert, weil die einzelnen
  // Knoten dann schnell keine IDs mehr zum Vergeben haben.
  private static final long INCREMENT_IDGEN = 1000;

  /**
   * achtung, siehe auch verwendung in {@link #delete(long)} methode
   */
  private static final int MAX_PRIORITY = (int)(ClusterMembers.STATIC_MOD_NUMBER*10);

  
  private static final Comparator<LockRequest> priorityRequestComperator = new Comparator<LockRequest>() {
    
    public int compare(LockRequest o1, LockRequest o2) {
      long tmp = o2.getResult().priority - o1.getResult().priority;
      return tmp < 0 ? -1 : 1;
    }
  };
  
  
  // ID generation
  private AtomicLong maxId = new AtomicLong(-1); //beim ersten zugriff auf maxId/nextId wird auf das cluster zugegriffen
  private AtomicLong nextId = new AtomicLong(0);
  private Object idGenLock = new Object();

  private StorageInterface localCache;
  private ActionHandler actionHandler;
  private Map<Integer, InterconnectProtocol> connections = new HashMap<Integer, InterconnectProtocol>();
  private Random priorityGenerationRandom = new Random();
  private int ownClusterNodeID; //unique auch �ber zeit. auch wenn knoten entfernt und ein neuer hinzugef�gt wird, hat der neue niemals die id eines ehemals entfernten.

  //clusterzustand
  private boolean clusteractive = false; //nur falls false, k�nnen callees geaddet werden. nur falls true, k�nnen standard-funktionen aufgerufen werden (create, read, etc)
  private volatile boolean valid = true; //wird beim shutdown auf false gestellt.

  private volatile Thread disconnectingThread;

  private List<InterconnectCallee> callees;
  private List<InterconnectCalleeProvider> calleeProvider = new ArrayList<InterconnectCalleeProvider>();

  // TODO prio5: konfigurierbarer ThreadPoolExecutor (setThreadPoolExecutor oder sowas). Dabei muss beachtet werden,
  // das ein eventueller custom-threadpoolexecutor auch die threadlocals aufr�umen kann.
  private ThreadPoolExecutor threadpool2 = 
      new ThreadPoolExecutorWithThreadlocalsCleanup(1, 200, 20, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
  //  private ThreadPool threadpool = new ThreadPool(0);
  
  private ClusterMemberChangeInformation clusterMemberChangeInformation;
  
  /**
   * steuert locking von threads die kein unlock sind, zb w�hrend das cluster pausiert wird.
   */
  private ThreadLockInterface globalLock;

  /**
   * steuert locking von threads, die {@link #unlock(long)} aufrufen, zb w�hrend das cluster pausiert wird.
   */
  private ThreadLockInterface globalUnlockLock;

  /**
   * steuert locking von threads, falls mehrere {@link #globalLock}-threads in einem lock-zirkel sind, und sie auf das
   * remote-lock warten, damit konsistent auf die cluster members zugegriffen wird.
   * @see {@link #lock(long)}
   */
  private ThreadLockInterface specialLock;
 
  
  CacheControllerImpl() {
  }


  public synchronized void addCallee(InterconnectCalleeProvider provider) {
    if (clusteractive) {
      throw new IllegalStateException("Cannot add additional callees after cluster is activated.");
    }
    if (!valid) {
      throw new IllegalStateException("Cannot add additional callees after cluster node has been shut down.");
    }
    calleeProvider.add(provider);
  }


  private synchronized void prepareClusterActivation() {

    if (clusteractive) {
      throw new IllegalStateException("Cluster is already active");
    }

    callees = new ArrayList<InterconnectCallee>();
    for (InterconnectCalleeProvider provider : calleeProvider) {
      InterconnectCallee callee = provider.newCallee(this);
      callee.initNotConnected();
      callees.add(callee);
    }

    localCache = new LocalCacheMemoryImpl();
    actionHandler = new ActionHandler(localCache, this);
    globalLock = new ThreadLock(ID_GLOBAL_LOCK, ID_GLOBAL_LOCK_T, this);
    globalUnlockLock = new ThreadLock(ID_GLOBAL_UNLOCK_LOCK, ID_GLOBAL_UNLOCK_LOCK_T, this);
    specialLock = new SpecialLock(ID_SPECIAL, this);

  }


  public synchronized void shutdown() {
    if (clusteractive) {
      if (logger.isInfoEnabled()) {
        logger.info("disconnecting node from cluster");
      }
      disconnectFromCluster();
    }
    valid = false;

    threadpool2.shutdown();
    // threadpool.shutdown();

  }


  private void checkControllerState() {
    if (!clusteractive) {
      if (Thread.currentThread() != disconnectingThread) {
        throw new IllegalStateException("Cluster node is not initialized.");
      }
    }
    if (!valid) {
      throw new IllegalStateException("Cluster node has been shut down.");
    }
  }


  /**
   * Create a new object for sharing in the cluster.
   * @param payload the content of the object
   * @return id of the new cluster object
   */
  public long create(CoherencePayload payload) {

    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }

    if (debugger.isEnabled()) {
      debugger.debug("Creating");
    }
    checkControllerState();
    globalLock.checkLock();
    try {
      return createInternally(payload, getNextUniqueId()).getId();
    } finally {
      globalLock.decrementThreadCount();
      if (enableNDC) {
        NDC.pop();
      }
    }

  }


  /**
   * Creates an object with a specific ID. This ID has to be larger than
   * {@link CacheController.ID_MIN_RESERVED_FOR_STATIC_ALLOCATION} and larger than
   * {@link CacheController.ID_MAX_RESERVED_FOR_STATIC_ALLOCATION}.
   */
  public long create(final long staticObjectId, CoherencePayload payload) throws ObjectIDNotUniqueInCacheException,
      InvalidStaticObjectIDException {

    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }

    if (staticObjectId > CacheController.ID_MAX_RESERVED_FOR_STATIC_ALLOCATION
        || staticObjectId < CacheController.ID_MIN_RESERVED_FOR_STATIC_ALLOCATION) {
      throw new InvalidStaticObjectIDException(staticObjectId);
    }

    if (localCache.contains(staticObjectId)) {
      throw new ObjectIDNotUniqueInCacheException(staticObjectId);
    }

    if (debugger.isEnabled()) {
      debugger.debug("Creating");
    }
    checkControllerState();
    globalLock.checkLock();
    try {

      try {
        lock(ID_GLOBAL_STATIC_OBJECT_CREATION_LOCK);
      } catch (ObjectNotInCacheException e) {
        throw new ClusterInconsistentException("Missing static lock object with id <"
            + ID_GLOBAL_STATIC_OBJECT_CREATION_LOCK + ">", e);
      }
      try {
        if (localCache.contains(staticObjectId)) {
          throw new ObjectIDNotUniqueInCacheException(staticObjectId);
        }
        return createInternally(payload, staticObjectId).getId();
      } finally {
        unlock(ID_GLOBAL_STATIC_OBJECT_CREATION_LOCK);
      }

    } finally {
      globalLock.decrementThreadCount();
      if (enableNDC) {
        NDC.pop();
      }
    }
  }


  /**
   * Create a new cluster object and inform other members of the cluster about the new object.
   * @param payload content of the new object
   * @param id id of the new object
   * @return new cluster object
   */
  private CoherenceObject createInternally(CoherencePayload payload, long id) {
    //keine locks notwendig, weil die id des objekts erst nach draussen gegeben wird, wenn die objekterstellung beendet ist.
    ClusterMember[] members = getCurrentMembers();
    CoherenceObject newObject = createLocally(payload, id);
    executeActionsRemotelyForMembers(new CreateObjectAction(id, ownClusterNodeID), members, false, newObject);
    newObject.onEvent(ObjectChangeEvent.OBJECT_CREATION, this);
    return newObject;
  }


  private CoherencePayload executeActionsRemotely(CoherenceObject targetObject, final CoherenceAction action,
                                                  ClusterMember[] members) {


//    CoherenceObject targetObject;
//    try {
//      // FIXME prio2: this call may not be necessary, all calling methods should provide the object (at least
//      //       if they already hold a reference to the object)
//      targetObject = localCache.read(action.getTargetObjectID());
//    } catch (ObjectNotInCacheException e) {
//      throw new ClusterInconsistentException("Object with id <" + action.getTargetObjectID() + "> got lost", e);
//    }

    if (members.length == 1 && members[0].getId() == ownClusterNodeID) {
      targetObject.getLockObject().closeLockCircleIfNecessaryAndGetCircleSize();
      return null;
    }
    if (debugger.isEnabled()) {
      debugger.debug(new Object() {
        @Override
        public String toString() {
          return "Executing action " + action;
        }
      });
    }
    return executeActionsRemotelyForMembers(action, members, true, targetObject);
  }


  private CoherencePayload executeActionsRemotelyForMembers(CoherenceAction action, ClusterMember[] members,
                                                            boolean needToPropagateLockCircleSizeIfNecessary,
                                                            CoherenceObject targetObject) {

    if (needToPropagateLockCircleSizeIfNecessary) {
      if (targetObject != null) {
        int lockCircleSize = targetObject.getLockObject().closeLockCircleIfNecessaryAndGetCircleSize();
        if (lockCircleSize > -1) {
          action.setLockCircleSize(lockCircleSize);
        }
      }
    }

    members = removeOwnClusterMemberEntry(members);

    if (members.length > 1) {

      List<RemoteActionRequest> remoteActionRequests = new ArrayList<RemoteActionRequest>(members.length);

      CountDownLatch latch = new CountDownLatch(members.length);
      for (ClusterMember member : members) {
        InterconnectProtocol nodeConnection = getNodeConnection(member);
        RemoteActionRequest rar = new RemoteActionRequest(nodeConnection, action, latch);
        remoteActionRequests.add(rar);
        executeInThreadPool(rar);
      }
      try {
        latch.await();
      } catch (InterruptedException e) {
        // TODO prio5: this might happen at factory shutdown when used within a service
        throw new RuntimeException("Got interrupted unexpectedly while waiting for latch", e);
      }

      if (action.getActionType() == CoherenceActionType.READ) {

        for (RemoteActionRequest request : remoteActionRequests) {
          if (request.isFailed()) {
            throw new RuntimeException("remote action failed unexpectedly for object " + action.getTargetObjectID()
                + ". threadname=" + request.getFailedThreadName(), request.getException());
          }
          if (request.getReturnValue() != null) {
            return request.getReturnValue();
          }
        }

        throw new ReadMissException();

//        StringBuilder sb = new StringBuilder();
//        sb.append("Did not receive a response on a read request. Sent requests to nodes ");
//        for (ClusterMember mem : members) {
//          sb.append(mem.getId()).append(", ");
//        }
//        sb.append("requested payload for object <").append(action.getTargetObjectID())
//            .append("> from node <" + ((ReadObjectAction) action).getClusterToProvideReturnData() + ">");
//        throw new ClusterInconsistentException(sb.toString());
      } else {
        for (RemoteActionRequest request : remoteActionRequests) {
          if (request.isFailed()) {
            throw new RuntimeException("remote action failed unexpectedly for object " + action.getTargetObjectID()
                + ". threadname=" + request.getFailedThreadName(), request.getException());
          }
        }
        return null;
      }

    } else if (members.length == 1) {

      // no thread pool overhead required if there is just a single remote node
      InterconnectProtocol nodeConnection = getNodeConnection(members[0]);
      CoherencePayload result = nodeConnection.executeActions(action);
      if (action.getActionType() == CoherenceActionType.READ && result == null) {
        throw new ClusterInconsistentException("Did not receive a response on a read request for object <"
            + action.getTargetObjectID() + ">");
      }
      return result;

    } else { // size == 0
      if (action.getActionType() == CoherenceActionType.READ) {
        throw new ClusterInconsistentException("Did not receive a response on a read request, no cluster members specified");
      }
      return null;
    }

  }


  private void executeInThreadPool(final SynchronousRequest rar) {

    boolean executed = false;
    do {
      try {
        threadpool2.execute(rar);
        executed = true;
      } catch (RejectedExecutionException e) {
      }
      // FIXME prio1: anderer Fehlerfall: "OutOfMemoryError: could not create new native thread"; wenn das z.B. bei einem
      // unlock passiert, wird das lock niemals freigegeben
    } while (!executed);

  }


  public void delete(final long objectId) throws ObjectNotInCacheException {

    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }

    if (debugger.isEnabled()) {
      debugger.debug(new Object() {
        @Override
        public String toString() {
          return "delete object <" + objectId + ">";
        }
      });
    }
    checkControllerState();
    globalLock.checkLock();

    try {

      ClusterMember[] members = getCurrentMembers();

      long nextPrio = getNextPriority();

      CoherenceObject oldObject = localCache.read(objectId);

      long lockResult;
      try {
        lockResult = oldObject.getLockObject().requestLock(nextPrio, true, false, -1).priority;
      } catch (ObjectDeletedWhileWaitingForLockException e1) {
        throw new ObjectNotInCacheException(objectId);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      try {
        localCache.read(objectId);
      } catch (ObjectNotInCacheException e) {
        oldObject.getLockObject().releaseLock(true, false, nextPrio);
        throw e;
      }

      final boolean reentrant = lockResult == RESPONSE_REENTRANT_LOCAL_LOCK;

      boolean needToCountdownLockCircleCounter = false;
      boolean gotRemoteLocks = false;

      boolean onlyGotPreliminaryLock = oldObject.getLockObject().isLockPreliminary();

      boolean cleanupDeletedLocally = false;
      try {
        //f�lle, wo man noch kein remotelock hat. komplement�r ist "reentrant && !onlygotpreliminary", genau da hat man bereits remotelocks.
        if (!reentrant || onlyGotPreliminaryLock) {
          
          
          //wenn man das remotelock nicht holt, solange man nur preliminarylock hat, funktioniert das cleanupdeletedobject
          //im cache nicht threadsicher.
          // => remote locks holen.
          //beispiel: thread 1 unlockt von knoten 2 aus. erst remoteunlock auf 0, dann auf 1. bevor das unlock auf 1 ankommt
          //          kommt thread 2 mit einem delete auf knoten 0. das delete holt dort nur ein preliminary lock
          //          (zb, erst lock, dann delete, state MODIFIED). danach schickt es die delete-action auch an knoten 1.
          //          dann k�nnen das unlock von thread 1 und die delete-action von thread 2 gleichzeitig bei knoten 1
          //          ankommen und zu problemen f�hren.
          //TODO: diese probleme anders l�sen, und dann hier performance gewinnen, weil das preliminary lock eigentlich
          //    ausreichen m�sste.
          
          /*
           * hier w�rde man gerne das lock upgraden.
           * das funktioniert aber nicht, wegen: thread 1 hat preliminary lock und ruft dann delete auf. 
           * dabei kann aber ein anderer thread in den lockzirkel kommen und dabei die h�here priorit�t haben. 
           * folgender workaround behebt dieses problem, indem thread 1 dann immer die h�chste priorit�t bekommt.
           *
           * ohne upgrade hat man ein �hnliches problem noch bei 2 konkurrierenden deletes. der thread mit dem
           * preliminary lock wartet dann unter umst�nden bei getremotelocks auf das remotelock mit einer h�heren
           * prio als man selbst => hohe prio hilft auch hier.
           */  
          if (onlyGotPreliminaryLock) {
             nextPrio += MAX_PRIORITY;
           }

          
          try {
            getRemoteLocks(members, oldObject, nextPrio, false, false, -1);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          gotRemoteLocks = true;
        }
        oldObject = localCache.delete(objectId);
        cleanupDeletedLocally = !reentrant;
        oldObject.getLockObject().setDeleted();
        
        //falls man remotelocks hat (=reentrant, dann hat ein parent-lock block die remotelocks), muss die action kein cleanup machen
        //TODO warum?
        executeActionsRemotely(oldObject, new DeleteObjectAction(objectId, ownClusterNodeID, !(gotRemoteLocks || reentrant)),
                               members);
        needToCountdownLockCircleCounter = true;
        oldObject.onEvent(ObjectChangeEvent.DELETE, this);
        if (debugger.isEnabled()) {
          debugger.debug("delete successful. cleaning up next.");
        }
      } finally {
        try {
          if (cleanupDeletedLocally) {
            localCache.cleanupDeletedObject(objectId);
          }
        } finally {
          if (gotRemoteLocks) {
            try {
              releaseRemoteLocks(members, objectId, oldObject.getLockObject().getLocalRequestPriority());
            } catch (ObjectNotInCacheException e) {
              // should have been handled remotely
              throw new ClusterInconsistentException("Release lock after delete should have been handled remotely", e);
            }
          }
        }
        oldObject.getLockObject().releaseLock(true, needToCountdownLockCircleCounter && gotRemoteLocks, nextPrio);
      }
    } finally {
      try {
        globalLock.decrementThreadCount();
      } finally {
        if (enableNDC) {
          NDC.pop();
        }
      }
    }

  }
  
  /**
   * {@inheritDoc}
   */
  public void lock(final long objectId) throws ObjectNotInCacheException {
    try {
      lock(objectId, false, 0);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param nanoTimeout absolute nanos {@link System#nanoTime()}
   * @return false, falls lock nicht innerhalb des timeouts bekommen
   */
  private boolean lock(final long objectId, final boolean tryLock, final long nanoTimeout) throws ObjectNotInCacheException, InterruptedException {

    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }

    try {

      if (debugger.isEnabled()) {
        final long timeoutNS = nanoTimeout - System.nanoTime();
        debugger.debug(new Object() {
          @Override
          public String toString() {
            StringBuilder sb = new StringBuilder("Lock object <").append(objectId).append(">");
            if (tryLock) {
              sb.append(" with timeout " + timeoutNS + "ns");
            }
            return sb.toString();
          }
        });
      }

      checkControllerState();
      if (objectId > ID_MAXIMUM_INTERNALLY_USED) {
        //FIXME prio2: deadlock gefahr: falls man hier auf global lock wartet und der thread will das lock auf das objekt
        //                        zum zweiten mal (reentry-fall), und es gibt einen anderen thread, der auf das lock
        //                        wartet, dann wird waitForActiveThreads nie fertig.
        globalLock.checkLock();
      }

      CoherenceObject existingObject = null;
      try {

        // check whether the object exists
        existingObject = localCache.read(objectId);

        long lockResult;
        try {
          LockRequestResponse resp = existingObject.getLockObject().requestPreliminaryLock(tryLock, nanoTimeout);
          if (resp.isLockTimeout()) {
            return false;
          }
          lockResult = resp.priority;
        } catch (ObjectDeletedWhileWaitingForLockException e1) {
          throw new ObjectNotInCacheException(objectId);
        }

        long nextPrio = getNextPriority();
        ClusterMember[] members = null; 
        GetRemoteLockResponse getRemoteLockResponse = GetRemoteLockResponse.REMOTELOCK_RESULT_SUCCESS; 
        // only get remote locks if the local thread has not previously obtained them, i.e. if this is not a reentrant lock
        if (lockResult != RESPONSE_REENTRANT_LOCAL_LOCK) {
          if (objectId == ID_GLOBAL_LOCK_T || objectId == ID_GLOBAL_UNLOCK_LOCK_T) {
            try {
              existingObject.getLockObject().upgradePreliminaryLock(nextPrio);
            } catch (ObjectDeletedWhileWaitingForLockException e) {
              throw new ClusterInconsistentException("Nondeletable internal object was deleted (id=" + objectId + ")",
                                                     e);
            }
            //gleichzeitig mit einem connectToCluster k�nnen noch andere (normale) threads ein lock auf ID_GLOBAL_LOCK_T (oder unlock) wollen
            //und damit einen lockzirkel bilden (das passiert nur, wenn zwei connectToClusters direkt nacheinander aufgerufen werden).
            //
            //das bedeutet, dass alle threads des lockzirkels das lokale lock erhalten haben.
            //insbesondere kann es dann sein, dass der gewinner-thread der "connectToCluster" thread ist.
            //dieser will dann ein snapshot vom logregister machen.
            //da aber andere mitglieder des lockzirkels hier an dieser stelle im code sein k�nnen (d.h. getCurrentMembers aufrufen),
            //werden die remotelocks unsynchronisiert mit dem snapshot und dem setzen der aktuellen clustermembers geholt.
            //
            //das kann dann dazu f�hren, dass der neue knoten inkonsistente lock-informationen f�r ID_GLOBAL_LOCK_T (oder unlock) bekommt.
            //
            //deshalb hier die spezialbehandlung f�r diesen fall: threads z�hlen, die hier sind, und ggfs warten, bis connectToCluster
            //soweit ist.
            specialLock.checkLock();
            checkControllerState();
            members = getCurrentMembers();
            
            try {
              getRemoteLockResponse = getRemoteLocks(members, existingObject, nextPrio, true, tryLock, nanoTimeout);
              
            } catch (InterruptedException e) {
              throw new ClusterInconsistentException("getremotelocks for internal object was unexpectedly interrupted", e);
            }
          } else {
            // Es reicht hier aus, das lock remote nur dann zu holen, wenn das Objekt nicht lokal MODIFIED oder EXCLUSIVE
            // ist. Das ist eine Vereinbarung, die darauf basiert, dass immer nur auf einem Knoten das Objekt in diesem Zustand
            // sein kann.
            boolean modifiedOrExclusive = existingObject.isModified() || existingObject.isExclusive();
            if (!modifiedOrExclusive) {
              try {
                existingObject.getLockObject().upgradePreliminaryLock(nextPrio);
              } catch (ObjectDeletedWhileWaitingForLockException e) {
                throw new ObjectNotInCacheException(objectId);
              }
              members = getCurrentMembers();
              try {
                getRemoteLockResponse = getRemoteLocks(members, existingObject, nextPrio, false, tryLock, nanoTimeout);
              } catch (ObjectNotInCacheException e) {
                existingObject.getLockObject().setDeleted();
                throw e;
              }
              if (getRemoteLockResponse.remoteLockState == GetRemoteLockState.REMOTELOCK_RESULT_SUCCESS) {
                // remember whether the initial state was MOD or EXC. this is important for the unlock procedure since
                // the state at the time of "unlock" might have changed
                existingObject.setNeedToReleaseRemoteLocksOnUnlock(true);
              }
            }

            // check back that the object still exists
            try {
              existingObject = localCache.read(objectId);
            } catch (ObjectNotInCacheException e) {
              boolean gotRemoteLock = getRemoteLockResponse.remoteLockState == GetRemoteLockState.REMOTELOCK_RESULT_SUCCESS;
              handleObjectNotInCacheDuringLock(objectId, gotRemoteLock, e);
            }
          }
        }
        
        if (getRemoteLockResponse.remoteLockState != GetRemoteLockState.REMOTELOCK_RESULT_SUCCESS) {
          if (debugger.isEnabled()) {
            debugger.debug("lock request timed out. recalling lock requests...");
          }
          /* lokales lock aufr�umen: das wurde vorher erfolgreich geholt.
           * zuerst lokal, dann remote, damit man die lockzirkelinformationen die lokal ermittelbar sind,
           * threadsicher remote verwenden kann. 
           * andersrum k�nnte es passieren, dass der lockzirkel status sich �ndert, 
           * w�hrend man die remote locks recalled, weil das lokale lock noch gesetzt ist. (zb: neuer lock
           * request denkt, er ist in einem lockzirkel, obwohl das gar nicht der fall ist)
           * 
           * wenn man das lokale lock zuerst freigibt, muss man aber daf�r sorgen, dass keine 
           * neuen lokalen requests mit der gleichen prio kommen. => preliminary lock holen und nach
           * dem recall der remote locks wieder freigeben.
           */
          boolean countedInLockCircle =
              existingObject.getLockObject().recallLocalLockRequest(nextPrio, getRemoteLockResponse.lockCircleSize);
          try {
            //remote locks aufr�umen.
            recallRemoteLocks(members, objectId, nextPrio, getRemoteLockResponse.priorityInOldLockCircle,
                              countedInLockCircle, getRemoteLockResponse.lockCircleSize,
                              getRemoteLockResponse.lockRequests);
          } finally {

            //auch aufr�umen, wenn remote locks recall fehler wirft (zb weil objekt gel�scht wurde)
            existingObject.getLockObject().recallLocalPreliminaryLock();
          }
          return false;
        }

        try {
          if (existingObject == null) {
            existingObject = localCache.read(objectId);
          }
          existingObject.onEvent(ObjectChangeEvent.LOCK, this);
        } catch (ObjectNotInCacheException e) {
          handleObjectNotInCacheDuringLock(objectId, e);
        }

      } finally {
        if (objectId > ID_MAXIMUM_INTERNALLY_USED) {
          globalLock.decrementThreadCount();
        }
      }

    } finally {
      if (enableNDC) {
        NDC.pop();
      }
    }

    return true;
  }


  private void handleObjectNotInCacheDuringLock(long objectId, ObjectNotInCacheException e)
      throws ObjectNotInCacheException {
    handleObjectNotInCacheDuringLock(objectId, true, e);
  }

  private void handleObjectNotInCacheDuringLock(long objectId, boolean gotRemoteLock, ObjectNotInCacheException e)
      throws ObjectNotInCacheException {
    if (debugger.isEnabled()) {
      debugger.debug("object with id " + objectId + " was deleted while waiting for remote locks. trying to unlock.");
    }
    try {
      unlock(objectId, gotRemoteLock);
    } catch (Exception t) {
      if (debugger.isEnabled()) {
        debugger.debug("could not unlock object " + objectId + " because exception occurred: " + t.getClass().getName()
            + " " + t.getMessage());
        if (logger.isTraceEnabled()) {
          logger.trace(null, t);
        }
      }
    }
    throw e;
  }


  /**
   * achtung, hier wird eine referenz ungelockt herausgegeben. d.h. andere threads die danach ein read machen, bekommen
   * die gleiche referenz.
   * f�r ein modify muss unbedingt vor dem schreiben in die referenz ein richtiges lock geholt werden.
   * 
   * f�r ein internes push (f�r redundanz) ist das nicht schlimm, weil dort nicht auf die gleiche referenz zugegriffen wird,
   * sondern die referenz �berschrieben wird. das gleiche gilt, falls das objekt invalidiert und direkt danach durch ein read 
   * aktualisiert wird. ein thread, der noch die alte referenz hat, merkt davon nichts.
   * 
   * achtung bei internen aufrufen. hier wird global lock �berpr�ft.
   */
  public CoherencePayload read(final long objectId) throws ObjectNotInCacheException {
    if (debugger.isEnabled()) {
      debugger.debug(new Object() {
        @Override
        public String toString() {
          return "Read object <" + objectId + ">";
        }
      });
    }
    checkControllerState();
    globalLock.checkLock();
    try {
      return readInternally(objectId);
    } finally {
      globalLock.decrementThreadCount();
    }
  }


  public void unlock(final long objectId) {
    unlock(objectId, true);
  }
  
  
  public void unlock(final long objectId, boolean gotRemoteLock) {
  
    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }

    if (debugger.isEnabled()) {
      debugger.debug(new Object() {
        @Override
        public String toString() {
          return "Unlock object <" + objectId + ">";
        }
      });
    }

    try {
      checkControllerState();

      if (objectId > ID_MAXIMUM_INTERNALLY_USED) {
        //unlocks m�ssen durchgelassen werden, damit bestehende aktive threads die auf locks warten weiterlaufen k�nnen.
        //wenn diese dann alle durchgelaufen sind (threadCnt == 0), dann d�rfen keine unlocks mehr vorbei.
        //das k�nnten zb unlocks zu locks sein, auf die keiner wartet.
        globalUnlockLock.checkLock();
      }
      try {
        //aber falls erst in "if lockCircleSize > -1" kann inzwischen ein neues member hinzugekommen sein
        //und bereits durch den lock-snapshot die lockCircleSize gesetzt haben.
        ClusterMember[] members = null;
        CoherenceObject lockedObject;
        try {
          lockedObject = localCache.read(objectId);
        } catch (ObjectNotInCacheException e1) {
          try {
            if (gotRemoteLock) {
            lockedObject = localCache.popDeletedObjectIfNotLocallyReentrant(objectId);
            } else { //don't pop if we didn't get RemoteLocks
              lockedObject = localCache.getDeletedObject(objectId);
            }
          } catch (ObjectNotInCacheException e) {
            throw new IllegalMonitorStateException();
          }
        }
        
        if (gotRemoteLock) {
        int lockCircleSize = lockedObject.getLockObject().closeLockCircleIfNecessaryAndGetCircleSize();
        if (lockCircleSize > -1) {
          if (debugger.isEnabled()) {
            debugger.debug(new Object() {
              @Override
              public String toString() {
                return "sending locked action for object <" + objectId + ">";
              }
            });
          }
          // send a remote action to make sure that on all remote nodes the lock circles are closed
          CoherenceAction action = new LockedObjectAction(objectId, ownClusterNodeID);
          action.setLockCircleSize(lockCircleSize);
          members = getCurrentMembers();
          executeActionsRemotely(lockedObject, action, members);
        } else {
          if (debugger.isEnabled()) {
            debugger.debug(new Object() {
              @Override
              public String toString() {
                return "No need to send locked action for object <" + objectId + ">";
              }
            });
          }
        }

        boolean countdownCircleOfTrustCounter = false;

        boolean lockIsPreliminary = lockedObject.getLockObject().isLockPreliminary();
        boolean lockIsLocallyReentrant = lockedObject.getLockObject().isLocallyReentrant();
        if (!lockIsPreliminary && !lockIsLocallyReentrant) {
          if (objectId <= ID_MAXIMUM_INTERNALLY_USED || lockedObject.needToReleaseRemoteLocksOnUnlock()) {
            if (members == null) {
              members = getCurrentMembers();
            }
            try {
              releaseRemoteLocks(members, objectId, lockedObject.getLockObject().getLocalRequestPriority());
            } catch (ObjectNotInCacheException e) {
              throw new ClusterInconsistentException("Object could not be unlocked on remote host "
                  + "though it had been locked before", e);
            }
            lockedObject.setNeedToReleaseRemoteLocksOnUnlock(false);
          }
          countdownCircleOfTrustCounter = true;
        }
        lockedObject.getLockObject().releaseLock(true, countdownCircleOfTrustCounter);
        } else {
          lockedObject.getLockObject().releaseLock(true, false);
        }
        
        // finally get the CoherenceObject to notify eventual change notification listeners
        // FIXME prio3: event auf alter payload ausf�hren? spezifikationsbedarf!
        CoherenceObject o = null;
        try {
          o = localCache.read(objectId);
        } catch (ObjectNotInCacheException e) {
          // this is ok since the object may have been deleted in the meantime
        }
        if (o != null) {
          o.onEvent(ObjectChangeEvent.UNLOCK, this);
        }
        
      } finally {
        if (objectId > ID_MAXIMUM_INTERNALLY_USED) {
          globalUnlockLock.decrementThreadCount();
        }
      }

    } finally {
      if (enableNDC) {
        NDC.pop();
      }
    }

  }


  public void update(final long objectId, CoherencePayload payload) throws ObjectNotInCacheException {

    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }
    
    if (debugger.isEnabled()) {
      debugger.debug(new Object() {
        @Override
        public String toString() {
          return "update object <" + objectId + ">";
        }
      });
    }

    try {

      checkControllerState();
      globalLock.checkLock();

      try {
        long nextPrio = getNextPriority();
        
        ClusterMember[] members = getCurrentMembers();

        CoherenceObject currentObj = localCache.read(objectId);

        // lock and find out whether remote locks have been obtained by a previous lock request
//        currentObj.getLockObject().flagAsRequested();
//        if (currentObj.isDeleted()) {
//          throw new ObjectNotInCacheException(objectId);
//        }
        long lockResult;
        try {
          lockResult = currentObj.getLockObject().requestPreliminaryLock(false, -1).priority;
        } catch (ObjectDeletedWhileWaitingForLockException e1) {
          throw new ObjectNotInCacheException(objectId);
        }

        final boolean needToObtainRemoteLocks = lockResult != RESPONSE_REENTRANT_LOCAL_LOCK;
        boolean gotRemoteLocks = false;
        boolean needToCountdownLockCircleCounter = false;
        try {

          // if the object is already modified, there is nothing to be done but write to it again
          if (currentObj.isModified() || currentObj.isExclusive()) {

            needToCountdownLockCircleCounter = false;

            currentObj.setPayload(payload);
            // this update call is not necessary when using memory but it can throw an
            // ObjectNotInCacheException if the object has been deleted between the time that it was
            // taken from the local cache and the time it was locked.
            localCache.update(currentObj);

            //FIXME prio3: event auf alter payload ausf�hren? spezifikationsbedarf!
            currentObj.onEvent(ObjectChangeEvent.UPDATE, this);

          } else {

            // only if an interaction with the cluster is required remote locks have to be acquired
            if (needToObtainRemoteLocks) {
              try {
                currentObj.getLockObject().upgradePreliminaryLock(nextPrio);
              } catch (ObjectDeletedWhileWaitingForLockException e) {
                throw new ObjectNotInCacheException(objectId);
              }
              try {
                getRemoteLocks(members, currentObj, nextPrio, false, false, -1);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              gotRemoteLocks = true;
            }

            // read the object again to make sure that the object still exists after the lock has been obtained
            currentObj = localCache.read(objectId);

            try {
              // the order of the following lines does not matter since every access is locked.
              executeActionsRemotely(currentObj, new UpdateObjectAction(currentObj.getId(), ownClusterNodeID), members);
              needToCountdownLockCircleCounter = true;

              currentObj.setPayload(payload);
              currentObj.setState(CoherenceState.MODIFIED);
              currentObj.getDirectoryData().setModifiedOnLocalNode();
              try {
                localCache.update(currentObj);
              } catch (ObjectNotInCacheException e) {
                throw new RuntimeException("Object <" + currentObj.getId()
                    + "> was removed from local cache also access was locked.");
              }

              currentObj.onEvent(ObjectChangeEvent.UPDATE, this);
            } finally {
              if (needToObtainRemoteLocks) {
                releaseRemoteLocks(members, objectId, currentObj.getLockObject().getLocalRequestPriority());
              }
            }

          }
        } finally {
          currentObj.getLockObject().releaseLock(true, needToCountdownLockCircleCounter && gotRemoteLocks);
        }
      } finally {
        globalLock.decrementThreadCount();
      }

    } finally {
      if (enableNDC) {
        NDC.pop();
      }
    }

  }


  public LockAwaitResponse awaitLock(long objectId, long priorityToWaitUpon, boolean tryLock, long nanoTimeout) throws ObjectNotInCacheException, InterruptedException {
    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }
    try {
      return localCache.read(objectId).getLockObject().awaitLock(priorityToWaitUpon, tryLock, nanoTimeout);
    } catch (ObjectDeletedWhileWaitingForLockException e) {
      throw new ObjectNotInCacheException(objectId);
    } finally {
      if (enableNDC) {
        NDC.pop();
      }
    }
  }


  public CoherencePayload executeActions(final CoherenceAction action) {

    final long objectId = action.getTargetObjectID();

    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }

    try {
      
      if (Debugger.getDebugger().isEnabled()) {
        Debugger.getDebugger().debug(new Object() {
          @Override
          public String toString() {
            return "Received action <" + action.getActionType() + "> for object <" + objectId + ">";
          }
        });
      }

      // das ist nur n�tig, wenn die �bergebene lock circle size auch gesetzt ist. in diesem Fall
      // schlie�t der aufrufende Knoten den Zirkel
      if (action.getLockCircleSize() > -1) {
        LockObject targetLockObject;
        try {
          targetLockObject = localCache.read(objectId).getLockObject();
        } catch (ObjectNotInCacheException e) {
          if (action.getActionType() == CoherenceActionType.LOCKED) {
            return null;
          }
          throw new ClusterInconsistentException("Object missing", e);
        }
        //FIXME wieso 2 methoden - hier reicht doch auch eine?
        targetLockObject.setPreliminaryLockCircleSize(action.getLockCircleSize(), false);
        targetLockObject.closeLockCircle();
      }

      if (Debugger.getDebugger().isEnabled()) {
        Debugger.getDebugger().debug(new Object() {
          @Override
          public String toString() {
            return "Executing action <" + action.getActionType() + "> for object <" + objectId + ">";
          }
        });
      }

      try {
        switch (action.getActionType()) {
          case CREATE :
            CreateObjectAction coa = (CreateObjectAction) action;
            actionHandler.handleCreateAction(objectId, coa);
            return null;
          case DELETE :
            DeleteObjectAction doa = (DeleteObjectAction) action;
            actionHandler.handleDeleteAction(objectId, doa.cleanupDeleted());
            return null;
          case READ :
            ReadObjectAction roa = (ReadObjectAction) action;
            return actionHandler.handleReadAction(objectId, roa, roa.needsToReturnValue(ownClusterNodeID),
                                                  clusterMemberChangeInformation);
          case UPDATE :
            UpdateObjectAction uoa = (UpdateObjectAction) action;
            actionHandler.handleUpdateAction(objectId, uoa);
            return null;
          case PUSH :
            PushObjectAction poa = (PushObjectAction) action;
            actionHandler.handlePushAction(objectId, ownClusterNodeID, poa);
            return null;
          case LOCKED :
            // nothing to be done
            if (action.getLockCircleSize() < 0) {
              throw new RuntimeException("No lock circle size set for " + CoherenceActionType.LOCKED + " action");
            }
            return null;
          case BULK :
            actionHandler.handleBulkAction((BulkCommitAction) action);
            return null;
          default :
            throw new RuntimeException("Unsupported action type: <" + action.getActionType() + ">");
        }
      } catch (ObjectNotInCacheException e) {
        // this cannot happen since the local actions will always fail first if the object does not exist
        throw new ClusterInconsistentException("Object <" + objectId
            + "> unexpectedly did not exist for received remote action request", e);
      }

    } finally {
      if (enableNDC) {
        NDC.pop();
      }
    }

  }


  public void releaseLock(long objectId, long priorityToRelease) {
    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }
    try {
      try {
        localCache.read(objectId).getLockObject().releaseLock(false, true, priorityToRelease);
      } catch (ObjectNotInCacheException e) {
        localCache.cleanupDeletedObject(objectId);
      }
    } finally {
      if (enableNDC) {
        NDC.pop();
      }
    }
  }


  public void recallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                                boolean countedInLockCircle, int lockCircleSize) throws ObjectNotInCacheException {
    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }
    try {
      localCache.read(objectId).getLockObject().recallRemoteLockRequest(priority, priorityOfLockInOldLockCircle,
                                                                        countedInLockCircle, lockCircleSize);
    } finally {
      if (enableNDC) {
        NDC.pop();
      }
    }
  }


  /**
   * called remotely, not locally
   */
  public LockRequestResponse requestLock(long objectId, long priority, boolean tryLock, long nanoTimeout) throws ObjectNotInCacheException, InterruptedException {
    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }
    try {
      LockObject relevantLockObject = localCache.read(objectId).getLockObject();
      boolean localRequest = false;
      LockRequestResponse response;
      try {
        response = relevantLockObject.requestLock(priority, localRequest, tryLock, nanoTimeout);
      } catch (ObjectDeletedWhileWaitingForLockException e) {
        throw new ObjectNotInCacheException(objectId);
      }
      return response;
    } finally {
      if (enableNDC) {
        NDC.pop();
      }
    }
  }


  private static class IDGenPayload extends CoherencePayload {

    private static final long serialVersionUID = 1L;

    private long nextId;


    private IDGenPayload(long startValue) {
      nextId = startValue;
    }

  }


  /**
   * Create a unique id for an cluster object.
   * @return
   */
  private long getNextUniqueId() {
    long currentMaxId = maxId.get();
    long possiblyNextId = nextId.getAndIncrement();
    if (possiblyNextId > currentMaxId) {
      synchronized (idGenLock) {
        currentMaxId = maxId.get();
        possiblyNextId = nextId.getAndIncrement();
        if (possiblyNextId > currentMaxId) {
          try {
            lock(ID_IDGEN);
          } catch (ObjectNotInCacheException e) {
            throw new RuntimeException(e);
          }
          try {
            IDGenPayload idGenShared = (IDGenPayload) readInternally(ID_IDGEN);
            possiblyNextId = idGenShared.nextId;
            nextId.set(idGenShared.nextId + 1);
            idGenShared.nextId += INCREMENT_IDGEN;
            maxId.set(idGenShared.nextId - 1);
            update(ID_IDGEN, idGenShared);
          } catch (ObjectNotInCacheException e) {
            throw new RuntimeException(e);
          } finally {
            unlock(ID_IDGEN);
          }
        }
      }
    }
    return possiblyNextId;
  }

  /**
   * @return a (pseudo-) random long that is never equal on two different cluster nodes
   */
  private long getNextPriority() {
    // must be positive which is ensured using a bit shift
    //long z = random.nextLong() >>> 1;
    long z = priorityGenerationRandom.nextInt(MAX_PRIORITY);

    // the following makes sure that the priorities are never equal on two different cluster nodes
    return z - (z % (ClusterMembers.STATIC_MOD_NUMBER)) + clusterMemberChangeInformation.getClusterPosition();

  }


  private CoherencePayload readInternally(final long objectId) throws ObjectNotInCacheException {

    // TODO prio4: performance: it might be reasonable to look for the entry in the local cache here without getting a lock.
    // if the entry is in SHARED state is that was is wanted? => man w�rde sich evtl auch das preliminary lock sparen
    //    CoherenceObject result = localCache.read(objectId);
    //    if (!result.isInvalid()) {
    //      return result.getPayload();
    //    }

    if (enableNDC) {
      NDC.push(ownClusterNodeID + "");
    }

    try {
      ClusterMember[] members = null;
      if (objectId != ID_CLUSTER_MEMBERS) {
        //w�rde f�r ID von clustermembers zu stackoverflow f�hren! ist eh nie invalid!
        members = getCurrentMembers();
      }

      //kein normales requestlock, weil gleichzeitige remoteanfragen das lock nicht in den zirkel aufnehmen
      //d�rfen, sofern lokales objekt im status INVALID ist.
      //weitere lokale threads m�ssen aber hier warten.
      CoherenceObject existingObject = localCache.read(objectId);
//      existingObject.getLockObject().flagAsRequested();
//      if (existingObject.isDeleted()) {
//        throw new ObjectNotInCacheException(objectId);
//      }
      long lockResult;
      try {
        lockResult = existingObject.getLockObject().requestPreliminaryLock(false, -1).priority;
      } catch (ObjectDeletedWhileWaitingForLockException e1) {
        throw new ObjectNotInCacheException(objectId);
      }
      final boolean needToObtainRemoteLocksBecauseLockIsNotReentrant = lockResult != RESPONSE_REENTRANT_LOCAL_LOCK;
      boolean gotRemoteLocks = false;
      boolean executedAction = false;
      boolean gotNoException = false;

      try {

        // read the object from the local cache
        existingObject = localCache.read(objectId);
        final CoherenceObject finalCopy = existingObject;
        if (debugger.isEnabled() && objectId > ID_MAXIMUM_INTERNALLY_USED) {
          debugger.debug(new Object() {
            private CoherenceState resultState = finalCopy.getStateWithCheck(clusterMemberChangeInformation);
            @Override
            public String toString() {
              return objectId + " state = " + resultState;
            }
          });
        }

        // find out whether remote actions are necessary and if so, execute them
        if (existingObject.isInvalid()) {
          if (needToObtainRemoteLocksBecauseLockIsNotReentrant) {
            long nextPrio = getNextPriority();
            try {
              existingObject.getLockObject().upgradePreliminaryLock(nextPrio);
            } catch (ObjectDeletedWhileWaitingForLockException e) {
              throw new ObjectNotInCacheException(objectId);
            }
            try {
              getRemoteLocks(members, existingObject, nextPrio, false, false, -1);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            gotRemoteLocks = true;
            // wenn das remote lock erteilt ist, kann das objekt remote bereits gel�scht worden sein. 
            // getRemoteLocks pr�ft das aber, also muss hier nicht noch einmal gecheckt werden.
          }
          if (members == null) {
            members = getCurrentMembers();
          }
          CoherencePayload updatedPayload = null;

          int clusterToProvideData = -1;
          do {
            clusterToProvideData =
                existingObject.getDirectoryData()
                    .getIdOfRandomClusterNodeHoldingValidCopy(clusterMemberChangeInformation);
            try {
              updatedPayload =
                  executeActionsRemotely(existingObject, new ReadObjectAction(objectId, ownClusterNodeID,
                                                                              clusterToProvideData), members);
            } catch (ReadMissException e) {
              // Das kann passieren, wenn gerade ein Knoten hinzugef�gt wird und ein interes Objekt auf den neuen Knoten
              // gepusht wird. Das Objekt ist dann intern schon als SHARED auf dem neuen Knoten markiert, dieser
              // beantwortet eingehende Requests aber zu diesem Zeitpunkt noch pauschal mit <null>.
            }
          } while (updatedPayload == null);

          executedAction = true;
          existingObject.setPayload(updatedPayload);
          existingObject.getDirectoryData().convertToShared(clusterToProvideData);
          existingObject.setState(CoherenceState.SHARED);
          localCache.update(existingObject);
        }

        gotNoException = true;

        CoherencePayload result = existingObject.getPayload();
        if (result == null) {
          throw new ClusterInconsistentException("Payload was null for <" + objectId + ">");
        }
        return result;

      } finally {
        // release remote locks only if necessary but release local lock in any case
        try {
          if (gotRemoteLocks) {
            if (members == null) {
              members = getCurrentMembers();
            }
            releaseRemoteLocks(members, objectId, existingObject.getLockObject().getLocalRequestPriority());
          }
          existingObject.getLockObject().releaseLock(true, executedAction && gotRemoteLocks);
        } catch (RuntimeException e) {
          // FIXME prio1: fehlerbehandlung �berall so!!!
          if (gotNoException) {
            throw e;
          } else {
            //folgefehler
            if (logger.isDebugEnabled()) {
              logger.debug("got exception in finally clause after read object.", e);
            }
          }
        }
      }

    } finally {
      if (enableNDC) {
        NDC.pop();
      }
    }
    //achtung: nach dem finally kann das objekt bereits wieder von einem anderen thread/knoten auf invalid gesetzt werden.

  }


  static class GlobalLockPayload extends CoherencePayload {

    private static final long serialVersionUID = 1L;

    ThreadType type;
    boolean locked;
    
    GlobalLockPayload(ThreadType type, boolean locked) {
      this.type = type;
      this.locked = locked;
    }


    @Override
    protected void onChange(ObjectChangeEvent event, CacheController controller) {
      final CacheControllerImpl ccImpl = (CacheControllerImpl) controller;
      if (event == ObjectChangeEvent.PUSH) {
        if (debugger.isEnabled()) {
          debugger.debug(new Object() {
            private String lockStr = ccImpl.getThreadLockByType(type).toString();
            private boolean _locked = locked;
            @Override
            public String toString() {
              return "trigger called. globalLock => " + _locked + " for lock: " + lockStr;
            }
          });
        }
        if (locked) {
          ccImpl.getThreadLockByType(type).lockLocally();
        } else {
          ccImpl.getThreadLockByType(type).unlockLocally();
        }
      }
    }

  }


  /**
   * Create a new object and add it to local cache.
   * @param payload
   * @param objectId
   * @return
   */
  private CoherenceObject createLocally(CoherencePayload payload, long objectId) {
    CoherenceObject newObject = new CoherenceObject(payload, objectId);
    localCache.create(newObject);
    return newObject;
  }


  /**
   * lokaler request, sich zu einem cluster zu verbinden. falls kein provider �bergeben wird, wird ein neues cluster
   * begonnen.
   */
  private synchronized void connectToClusterLocallyInternally(NodeConnectionProvider nodeConnectionProvider) {

    if (clusteractive) {
      throw new RuntimeException("Cluster node has already been activated.");
    }
    if (!valid) {
      throw new RuntimeException("Cluster node has already been shut down.");
    }
    
    if (debugger.isEnabled()) {
      debugger.debug("creating connection to cluster");
    }
    
    prepareClusterActivation();
    
    if (nodeConnectionProvider == null) {
      //neues cluster anlegen, d.h. notwendige daten initialisieren
      ownClusterNodeID = CLUSTER_ID_START;
      debugger.setContext(ownClusterNodeID);
      createLocally(new ClusterMembers(new ClusterMember(createNodeInformation(), CLUSTER_ID_START)), ID_CLUSTER_MEMBERS);
      createLocally(new IDGenPayload(ID_MAX_RESERVED_FOR_STATIC_ALLOCATION + 1), ID_IDGEN);
      
      //objekte mit onChange, um lokale locks auf andere knoten zu verbreiten.
      createLocally(new GlobalLockPayload(ThreadType.ELSE, false), ID_GLOBAL_LOCK);
      createLocally(new GlobalLockPayload(ThreadType.UNLOCK, false), ID_GLOBAL_UNLOCK_LOCK);
      createLocally(new GlobalLockPayload(ThreadType.SPECIAL, false), ID_SPECIAL);
      
      //nur als globales lockobjekt f�r die threads gedacht
      createLocally(new CoherencePayload(), ID_GLOBAL_LOCK_T);
      createLocally(new CoherencePayload(), ID_GLOBAL_UNLOCK_LOCK_T);

      // statisches lock zum Anlegen von Objekten mit von au�en vorgegebener ID
      createLocally(new CoherencePayload(), ID_GLOBAL_STATIC_OBJECT_CREATION_LOCK);

    } else {
      //verbinden, metadaten zur�ckbekommen, id zur�ckbekommen
      InterconnectProtocol connection = nodeConnectionProvider.createConnection();
      InitialConnectionData data = connection.connectToClusterRemotely(createNodeInformation());
      localCache.importShapShot(data.getMetadata());
      ownClusterNodeID = data.getId();
      debugger.setContext(ownClusterNodeID);
    }

    clusterMemberChangeInformation = new ClusterMemberChangeInformation(ownClusterNodeID);

    if (debugger.isEnabled()) {
      debugger.debug("new cluster node initialized. preparing callees for normal work.");
    }

    //in beiden f�llen zu tun:
    //TODO prio5: Reihenfolge der gequeueten Anfragen global �ber alle callees hinweg beibehalten (beim nachholen). Nur
    //            relevant, wenn man mehrere callees hat, z.B. HTTP und HTTPS bei Knoten an unterschiedlichen Standorten.
    //            Dabei eventuell die Abarbeitung der unterschiedlichen callees parallelisieren?
    for (InterconnectCallee callee : callees) {
      callee.initConnected();
    }

    clusteractive = true;

  }


  private NodeInformation createNodeInformation() {
    return new NodeInformation(callees);
  }


  /**
   * interconnectprotocol: von aussen meldet sich hier ein neuer knoten an
   */
  public InitialConnectionData connectToClusterRemotely(NodeInformation nodeInformation) {

    if (logger.isInfoEnabled()) {
      logger.info("Got connection request from remote cluster node");
    }

    // clustermembers aktualisieren und an andere knoten pushen
    InitialConnectionData initialConnectionData = new InitialConnectionData();

    // globales lock setzen, welches verhindert, dass weitere �nderungen an metadaten/lockdaten auf basis der alten
    // clustermitglieder geschieht. Wenn das sichergestellt ist, kann gefahrlos der snapshot der metadaten erstellt
    // werden und dann das lock freigegeben werden. Alle zuk�nftigen �nderungen an metadaten geschehen dann auf basis
    // er neuen clustermitglieder und es gehen somit keine metadaten�nderungen beim neu hinzugekommenen knoten verloren.
    ClusterMember[] oldMembers = null;
    pauseCluster();
    try {
      ClusterMembers members;
      try {
        members = (ClusterMembers) localCache.read(ID_CLUSTER_MEMBERS).getPayload();
      } catch (ObjectNotInCacheException e) {
        throw new RuntimeException("cluster members could not be found. locks can not be freed!", e);
      }
      oldMembers = new ClusterMember[members.getMembers().length];
      System.arraycopy(members.getMembers(), 0, oldMembers, 0, members.getMembers().length);
      
      if (debugger.isEnabled()) {
        debugger.debug("creating snapshot of meta- and lockdata");
      }
      //snapshot von metadaten erstellen um sie an den neuen knoten zur�ckzugeben
      initialConnectionData.setMetaData(localCache.getSnapShot(ownClusterNodeID, clusterMemberChangeInformation, true));

      //push members + warte auf aktive threads, die die metadaten beeinflussen
      
      int newId = clusterMemberChangeInformation.getNewClusterMemberId();
      if (logger.isInfoEnabled()) {
        logger.info("new cluster node will get id " + newId);
      }
      List<ClusterMember> membersList = new ArrayList<ClusterMember>(Arrays.asList(members.getMembers()));
      ClusterMember newClusterMember = new ClusterMember(nodeInformation, newId);
      initialConnectionData.setId(newClusterMember.getId());

      membersList.add(newClusterMember);
      members.setMembers(membersList.toArray(new ClusterMember[membersList.size()]));

      try {
        //push und nicht update, weil sonst bei den knoten, wo ID_CLUSTERMEMBERS invalidiert wurde ein stackoverflow passiert, weil
        //die getCurrentMembers methode readInternally und damit getRemoteLocks aufruft.
        pushClusterMembers(members);
      } catch (ObjectNotInCacheException e) {
        throw new RuntimeException(e);
      }

    } finally {
      if (oldMembers != null) { //darf nicht null sein, aber eine runtimeexception gibts dann eh schon - braucht man hier nicht nochmal werfen.
        resumeClusterInternally(oldMembers);
      }
    }

    return initialConnectionData;
  }


  private ThreadLockInterface getThreadLockByType(ThreadType type) {
    ThreadLockInterface lock = null;
    switch (type) {
      case ELSE :
        lock = globalLock;
        break;
      case UNLOCK :
        lock = globalUnlockLock;
        break;
      case SPECIAL :
        lock = specialLock;
        break;
      default :
        // should only be relevant during development 
        throw new RuntimeException("Unexpected " + ThreadType.class.getSimpleName() + ": <" + type + ">");
    }
    return lock;
  }


  public void waitForActiveThreads(final ThreadType type) {
    ThreadLockInterface lock = getThreadLockByType(type);
    lock.waitForThreads();
    if (debugger.isEnabled()) {
      debugger.debug(new Object() {
        @Override
        public String toString() {
          return "waited for active " + type + "-threads on node <" + ownClusterNodeID + ">.";
        }
      });
      debugger.debug(lock.toString());
    }
  }


  private ClusterMember[] removeOwnClusterMemberEntry(ClusterMember[] members) {
    // TODO prio2: performance: dont do this so many times, just keep the list of members in some kind of cache
    for (int i = members.length - 1; i > -1; i--) {
      ClusterMember member = members[i];
      if (member.getId() == ownClusterNodeID) {
        ClusterMember[] tmp = new ClusterMember[members.length - 1];
        System.arraycopy(members, 0, tmp, 0, i);
        System.arraycopy(members, i + 1, tmp, i, tmp.length - i);
        return tmp;
      }
    }
    return members;
  }


  /**
   * Waits for active threads on all cluster nodes
   */
  private void waitForActiveThreads(ClusterMember[] clusterMembersToWaitOn, ThreadType type) {

    clusterMembersToWaitOn = removeOwnClusterMemberEntry(clusterMembersToWaitOn);
    if (clusterMembersToWaitOn.length == 0) {
      waitForActiveThreads(type);
      return;
    } else {

      CountDownLatch latch = new CountDownLatch(clusterMembersToWaitOn.length);
      List<WaitForActiveThreadsRequest> requests =
          new ArrayList<WaitForActiveThreadsRequest>(clusterMembersToWaitOn.length);
      for (int i = 0; i < clusterMembersToWaitOn.length; i++) {
        ClusterMember member = clusterMembersToWaitOn[i];
        WaitForActiveThreadsRequest request = new WaitForActiveThreadsRequest(getNodeConnection(member), latch, type);
        requests.add(request);
        executeInThreadPool(request);
      }
      waitForActiveThreads(type);
      try {
        latch.await();
      } catch (InterruptedException e) {
        // TODO prio5: this might happen at factory shutdown when used within a service
        throw new RuntimeException(e);
      }
      for (WaitForActiveThreadsRequest request : requests) {
        if (request.isFailed()) {
          throw new RuntimeException("remote request to wait for active threads failed (thread = "
              + request.getFailedThreadName() + ")", request.getException());
        }
      }

    }

  }


  private void pushClusterMembers(ClusterMembers members) throws ObjectNotInCacheException {
    setAndPushAlreadyLockedObject(ID_CLUSTER_MEMBERS, members, true);
  }


  void setAndPushAlreadyLockedObject(long objectId, CoherencePayload payload,
                                     boolean ownsGlobalLockAndDidNotGetObjectLock) {
    setAndPushAlreadyLockedObject(objectId, payload, getCurrentMembers(), ownsGlobalLockAndDidNotGetObjectLock, true);
  }


  /**
   * setzt payload lokal und schickt payload an alle angegebenen cluster member. f�r diese operation muss man vorher das
   * lock holen! objekt wird lokal und bei den remote knoten auf shared gesetzt. bei allen nicht angegebenen remote
   * knoten wird nur das directorydata aktualisiert.
   */
  void setAndPushAlreadyLockedObject(long objectId, CoherencePayload payload, ClusterMember[] members,
                                     boolean ownsGlobalLockAndDidNotGetObjectLock,
                                     boolean pushEmptyToComplementaryClusterMembers) {

    int[] ids = createNodeIdArray(members);

    // TODO prio5: performance, diese �berpr�fung kann irgendwann raus oder durch ein static final flag konfigurierbar gemacht werden.
    boolean found = false;
    for (int i = 0; i < ids.length; i++) {
      if (ids[i] == ownClusterNodeID) {
        found = true;
        break;
      }
    }
    //throw new RuntimeException("method may not be called without own clusternode as member");

    CoherenceObject coherenceObject = null;
    try {
      coherenceObject = localCache.read(objectId);
      coherenceObject.setPayload(payload);
      // TODO prio2: testen, dass nur auf shared gesetzt wird, wenn man an mindestens einen weiteren member pusht.
//      if (members.length > 1 || found) {
        coherenceObject.setState(CoherenceState.SHARED);
        coherenceObject.getDirectoryData().setShared(ids, ownClusterNodeID);
//      }
      localCache.update(coherenceObject);
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }

    pushAlreadyLockedObject(coherenceObject, payload, members, ownsGlobalLockAndDidNotGetObjectLock,
                            pushEmptyToComplementaryClusterMembers);
    coherenceObject.onEvent(ObjectChangeEvent.PUSH, this);

  }


  private void pushAlreadyLockedObject(CoherenceObject object, CoherencePayload payload, ClusterMember[] members,
                                       boolean ownsGlobalLockAndDidNotGetObjectLock,
                                       boolean pushEmptyToComplementaryClusterMembers) {
    int[] ids = createNodeIdArray(members);
    executeActionsRemotelyForMembers(new PushObjectAction(object.getId(), ownClusterNodeID, payload, ids), members,
                                     !ownsGlobalLockAndDidNotGetObjectLock, object);

    if (pushEmptyToComplementaryClusterMembers) {
      ClusterMember[] otherMembers = getComplementaryMembers(members);
      if (otherMembers.length > 0) {
        executeActionsRemotelyForMembers(new PushObjectAction(object.getId(), ownClusterNodeID, null, ids),
                                         otherMembers, !ownsGlobalLockAndDidNotGetObjectLock, object);
      }
    }
  }


  private int[] createNodeIdArray(ClusterMember[] members) {
    int[] nodeIds = new int[members.length];
    for (int i = 0; i < members.length; i++) {
      nodeIds[i] = members[i].getId();
    }
    return nodeIds;
  }


  private ClusterMember[] getComplementaryMembers(ClusterMember[] members) {
    List<ClusterMember> complementaryMembers = new ArrayList<ClusterMember>();
    for (ClusterMember cm : getCurrentMembers()) {
      boolean found = false;
      for (ClusterMember m : members) {
        if (m == cm) {
          found = true;
          break;
        }
      }
      if (!found) {
        complementaryMembers.add(cm);
      }
    }
    return complementaryMembers.toArray(new ClusterMember[complementaryMembers.size()]);
  }
  
  /**
   * updates cluster members. directorydata of all objects will lazily adapt.
   */
  private void removeNodeFromCluster(int clusterNodeId, boolean distributeModifiedExclusive) {
    ClusterMember[] membersWithoutRemovedNode = null;
    pauseCluster();
    try {
      if (clusterNodeId == ownClusterNodeID) {
        disconnectingThread = Thread.currentThread();
        clusteractive = false;
      }
      membersWithoutRemovedNode = removeOwnClusterMemberEntry(getCurrentMembers());

      //TODO prio2: daf�r sorgen, dass keine locks vom zu entfernenden knoten offenbleiben, die nicht geschlossen werden.

      if (distributeModifiedExclusive && membersWithoutRemovedNode.length > 0) {
        Random random = new Random();
        //payload von modified/exclusive objekten verschicken.
        List<CoherenceObject> modifiedExclusive =
            localCache.getAllModifiedExclusive(clusterNodeId, ownClusterNodeID, clusterMemberChangeInformation);
        if (clusterNodeId == ownClusterNodeID) {
          for (CoherenceObject o : modifiedExclusive) {
            //push zu zuf�lligem knoten ausser dem zu entfernenden
            ClusterMember m = membersWithoutRemovedNode[random.nextInt(membersWithoutRemovedNode.length)];
            pushAlreadyLockedObject(o, o.getPayload(), new ClusterMember[] {m}, true, true);
          }
        } else {
          //TODO read payload from remote cluster node?
          throw new RuntimeException("unsupported");
        }
      }

      ClusterMembers membersPayload = new ClusterMembers(null);
      membersPayload.setMembers(membersWithoutRemovedNode);
      CoherenceObject clusterMembersObject;
      try {
        clusterMembersObject = localCache.read(ID_CLUSTER_MEMBERS);
      } catch (ObjectNotInCacheException e) {
        throw new ClusterInconsistentException("Missing cluster members object", e);
      }
      pushAlreadyLockedObject(clusterMembersObject, membersPayload, membersWithoutRemovedNode, true, false);
      connections.remove(clusterNodeId);
    } finally {
      resumeClusterInternally(membersWithoutRemovedNode);
    }
  }


  public synchronized void disconnectFromCluster() {

    if (debugger.isEnabled()) {
      final int clusterId = ownClusterNodeID;
      debugger.debug(new Object() {
        @Override
        public String toString() {
          return "disconnecting node " + clusterId + " from cluster";
        }
      });
    }

    removeNodeFromCluster(ownClusterNodeID, true);
    // TODO prio5: Parallelisieren?
    for (InterconnectCallee callee : callees) {
      callee.shutdown();
    }
    disconnectingThread = null;

  }


  private InterconnectProtocol getNodeConnection(ClusterMember member) {
    InterconnectProtocol con = connections.get(member.getId());
    if (con == null) {
      synchronized (connections) {
        con = connections.get(member.getId());
        if (con == null) {
          con = createNodeConnection(member);
          connections.put(member.getId(), con);
        }
      }
    }
    return con;
  }


  private InterconnectProtocol createNodeConnection(ClusterMember member) {
    //TODO prio3: wer entscheidet, welche connection hier aufgemacht wird, wenn es mehrere callees gibt?
    return NodeConnectionProviderFactory.getInstance().getProvider(member, this).createConnection();
  }

  
  private static final long REMOTELOCK_RESULT_SUCCESS = -2;
  private static final long REMOTELOCK_RESULT_TIMEOUT_NO_OLD_CIRCLE = -1;


  /**
   * gibt {@link #REMOTELOCK_RESULT_SUCCESS} zur�ck, falls das lock erfolgreich war.<br>
   * gibt {@link #REMOTELOCK_RESULT_TIMEOUT_NO_OLD_CIRCLE} zur�ck, falls timeout passiert ist, aber kein alter
   * lockzirkel gefunden wurde.<br>
   * gibt ansonsten die priority des verbleibenden locks eines alten lockzirkels zur�ck.
   */
  private GetRemoteLockResponse getRemoteLocks(ClusterMember[] members, CoherenceObject objectToBeLocked, long priority,
                                               boolean countThreads, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {

    long nextPriorityAfterMine = Long.MAX_VALUE;
    long oldLockCirclePriority = REMOTELOCK_RESULT_TIMEOUT_NO_OLD_CIRCLE;
    boolean waitingForOldLockCircle = false;

    int n1 = 0, n2 = 0, m = 0, l = 0, phantomNodes = 0;

    boolean gotRemoteLock = true;
    InterconnectProtocol nodeConnectionToWaitForLock = null;
    LockRequest[] lockRequests = null;
    try {

      if (members.length == 1 && members[0].getId() == ownClusterNodeID) {
        //achtung: request kann bereits zu einem zirkel geh�ren, wenn zuvor das vorletzte clustermember entfernt worden ist.
        //deshalb onlyIfNecessary = true;
        boolean onlyIfNecessary = true;
        objectToBeLocked.getLockObject().setPreliminaryLockCircleSize(1, onlyIfNecessary);
        if (Debugger.getDebugger().isEnabled()) {
          Debugger.getDebugger().debug("No need to obtain remote locks, no cluster relevant member found");
        }
        return new GetRemoteLockResponse(0l,0,GetRemoteLockState.REMOTELOCK_RESULT_SUCCESS);
      } else if (Debugger.getDebugger().isEnabled()) {
        final long objectId = objectToBeLocked.getId();
        Debugger.getDebugger().debug(new Object() {
          @Override
          public String toString() {
            return "Getting remote locks for object <" + objectId + ">";
          }
        });
      }

      members = removeOwnClusterMemberEntry(members);
      
      lockRequests = new LockRequest[members.length];
      
      CountDownLatch latch = new CountDownLatch(members.length);

      for (int i = 0; i < members.length; i++) {
        ClusterMember member = members[i];
        InterconnectProtocol nodeConnection = getNodeConnection(member);
        LockRequest rl =
            new LockRequest(nodeConnection, objectToBeLocked.getId(), priority, latch, tryLock, nanoTimeout);
        lockRequests[i] = rl;
        if (members.length > 1) {
          executeInThreadPool(rl);
        } else {
          rl.exec();
          latch.countDown();
        }
      }

      try {
        latch.await();
      } catch (InterruptedException e) {
        // TODO prio5: this might happen at factory shutdown when used within a service
        throw new RuntimeException("Got interrupted unexpectedly", e);
      }


      for (int lockRequestIdx = 0; lockRequestIdx < lockRequests.length; lockRequestIdx++) {
        final LockRequest request = lockRequests[lockRequestIdx];
        if (request == null) {
          continue;
        }
        if (request.isFailed()) {
          throw new RuntimeException("Remote lockRequest for object " + objectToBeLocked.getId()
              + " failed unexpectedly. thread=" + request.getFailedThreadName(), request.getException());
        }
        if (request.objectWasRemoved()) {
          throw new ObjectNotInCacheException(objectToBeLocked.getId());
        }
        if (request.wasInterrupted()) {
          throw new InterruptedException("Waiting thread was interrupted remotely (objectId = " + objectToBeLocked.getId() + ")");
        }

        if (request.getResult().isLockTimeout()) {
          gotRemoteLock = false;
        }
        
        if (request.getResult().isNodeTimedOutAsPartOfCircle()) {
          phantomNodes++;
          continue;
        }
        
        if (request.getResult().priority > -1) {
          if (request.getResult().remoteCircleSize > 0) {
            if (request.getResult().remoteCircleSize == 1) {
              //remote lockender thread ist der letzte des zirkels => lokaler thread kann nicht zum zirkel geh�ren
              if (debugger.isEnabled()) {
                debugger.debug(new Object() {
                  @Override
                  public String toString() {
                    return "Got response from a previous lock circle (n2++) prio = " + request.getResult().priority
                        + ", circleSize = " + request.getResult().remoteCircleSize;
                  }
                });
              }
              n2++;
              oldLockCirclePriority = request.getResult().priority;
              if (nodeConnectionToWaitForLock == null) {
                if (debugger.isEnabled()) {
                  debugger.debug("Need to wait on previous lock circle");
                }
                nodeConnectionToWaitForLock = request.getNodeConnection();
                waitingForOldLockCircle = true;
              }
              continue;
            } else {
              if (debugger.isEnabled()) {
                debugger.debug(new Object() {
                  @Override
                  public String toString() {
                    return "Got response from a closed circle while locally either the circle"
                        + " is closed or the remote priority is known (n1++) prio = " + request.getResult().priority
                        + ", circleSize = " + request.getResult().remoteCircleSize;
                  }
                });
              }
              n1++;
            }
          } else {
            m++;
          }
        } else {
          if (request.getResult().priority == InterconnectProtocol.SUCCESSFUL_LOCK_BY_COMPARISON) {
            l++;
          }
          continue;
        }

        //wenn priorit�t kleiner ist als die eigene, muss man nicht darauf warten.
        if (request.getResult().priority < nextPriorityAfterMine && request.getResult().priority > priority) {
          nextPriorityAfterMine = request.getResult().priority;
          nodeConnectionToWaitForLock = request.getNodeConnection();
          waitingForOldLockCircle = false;
        }

      }

      if (n2 > 1) {
        throw new ClusterInconsistentException("More than one node responded that there is another open lock circle");
      }

    } finally {
      if (countThreads) {
        specialLock.decrementThreadCount();
      }
    }

    if (debugger.isEnabled()) {
      final int fn1 = n1;
      final int fn2 = n2;
      final int fm = m;
      final int fl = l;
      final int fphantomNodes = phantomNodes;
      final boolean fGotRemoteLock = gotRemoteLock;
      final long fNextPriorityAfterMine = nextPriorityAfterMine;
      debugger.debug(new Object() {
        @Override
        public String toString() {
          return "contacted all remote nodes. got: n1=" + fn1 + ", n2=" + fn2 + ", m=" + fm + ", l=" + fl + ", phantomNodes=" + fphantomNodes
              + " gotRemoteLock=" + fGotRemoteLock + ", nextPrioAfterMine=" + fNextPriorityAfterMine;
        }
      });
    }
    
    final int preliminaryCircleSize = n1 + m + l + 1;
    final int preliminaryCircleSizeWithPhantomNodes;
    if (!waitingForOldLockCircle) {
      preliminaryCircleSizeWithPhantomNodes = preliminaryCircleSize + phantomNodes;
    } else {
      preliminaryCircleSizeWithPhantomNodes = preliminaryCircleSize;
    }
      
    if (gotRemoteLock) {
      if (nodeConnectionToWaitForLock != null) {
        // this request will block until the lock has been obtained

        //es kann passieren, dass man der letzte verbleibende seines lockzirkels ist, aber
        //man nicht der urspr�nglich h�chste war (der urspr�nglich h�chste hat dann aber auf jeden fall
        //den zirkel geschlossen).
        //in diesem fall muss man nicht warten und auch nicht die zirkelgr��e setzen.
        if (waitingForOldLockCircle && !objectToBeLocked.getLockObject().isLockCircleClosed()) {
          try {
            gotRemoteLock = nodeConnectionToWaitForLock.awaitLock(objectToBeLocked.getId(), oldLockCirclePriority, tryLock, nanoTimeout) != LockAwaitResponse.TIMEOUT;
          } catch (ObjectNotInCacheException e) {
            objectToBeLocked.getLockObject().setDeleted();
            throw e;
          }
          if (debugger.isEnabled()) {
            final int sum = preliminaryCircleSize;
            final boolean gotRemoteLockFinal = gotRemoteLock;
            debugger.debug(new Object() {
              @Override
              public String toString() {
                return "Waited for old circle, new circle size is " + sum + ". got remoteLock = " + gotRemoteLockFinal;
              }
            });
          }
          if (gotRemoteLock) {
            //man ist gewinner des "n�chsten" lockzirkels und hat nur auf den alten gewartet.
            objectToBeLocked.getLockObject().setPreliminaryLockCircleSize(preliminaryCircleSize, false);
          }
        } else {
          LockAwaitResponse resp = null;
          try {
            resp = nodeConnectionToWaitForLock.awaitLock(objectToBeLocked.getId(), nextPriorityAfterMine, tryLock, nanoTimeout);
          } catch (ObjectNotInCacheException e) {
            objectToBeLocked.getLockObject().setDeleted();
            throw e;
          }
          while (resp == LockAwaitResponse.RECALLED) {
            // lock @nextPriorityAfterMine was recalled
            LockRequest nextRequest = getNextLockRequest(nextPriorityAfterMine, lockRequests);
            if (nextRequest != null) {
              nextPriorityAfterMine = nextRequest.getResult().priority;
              nodeConnectionToWaitForLock = nextRequest.getNodeConnection();
              resp = nodeConnectionToWaitForLock.awaitLock(objectToBeLocked.getId(), nextPriorityAfterMine, tryLock, nanoTimeout);
              if (nextRequest.getResult().remoteCircleSize == 1) {
                debugger.debug("closing circle: we waited for oldCircle after a previous nexCircleMember timed out");
                objectToBeLocked.getLockObject().setPreliminaryLockCircleSize(preliminaryCircleSizeWithPhantomNodes, false);
              }
            } else {
              // if we could not find a next request we must have already waited at the response with the highest priotity
              // that action timed out before executing so the circle is still unclosed
              // closing is optional because we could have gotten our lockRequest response after predessesors already
              // finished their work and are not dicernable from not participating members
              debugger.debug("closing circle: next higher prio timed out before closing");
              resp = null;
              objectToBeLocked.getLockObject().setPreliminaryLockCircleSize(preliminaryCircleSizeWithPhantomNodes, true);
            }
          }
          if (resp == LockAwaitResponse.TIMEOUT) {
            gotRemoteLock = false;
          } else if (resp == LockAwaitResponse.SUCCESS_CIRCLE_NOT_CLOSED) {
            //der lockrequest mit der gewinner prio hatte einen timeout. dann muss die lockcirclesize trotzdem gesetzt werden
            objectToBeLocked.getLockObject().setPreliminaryLockCircleSize(preliminaryCircleSizeWithPhantomNodes, false);
          }
        }
      } else {
        objectToBeLocked.getLockObject().setPreliminaryLockCircleSize(preliminaryCircleSizeWithPhantomNodes, true);
      }

      if (objectToBeLocked.getLockObject().isDeleted()) {
        throw new ObjectNotInCacheException(objectToBeLocked.getId());
      }
    } 

    if (!gotRemoteLock) {
      //nur auf prio gesetzt, falls n2-fall aufgetreten ist. ansonsten auf REMOTELOCK_RESULT_TIMEOUT_NO_OLD_CIRCLE gesetzt
      if (oldLockCirclePriority == REMOTELOCK_RESULT_TIMEOUT_NO_OLD_CIRCLE) {
        return new GetRemoteLockResponse(oldLockCirclePriority, preliminaryCircleSize, GetRemoteLockState.REMOTELOCK_RESULT_TIMEOUT_NO_OLD_CIRCLE, lockRequests);
      } else {
        return new GetRemoteLockResponse(oldLockCirclePriority,  preliminaryCircleSize, GetRemoteLockState.OTHER, lockRequests);
      }
      
    }
    return new GetRemoteLockResponse(0l,0,GetRemoteLockState.REMOTELOCK_RESULT_SUCCESS);
  }
  
  
  
  private static LockRequest getNextLockRequest(final long priority, final LockRequest[] allRequests) {
    LockRequest result = null;
    for (final LockRequest lockRequest : allRequests) {
      if (debugger.isEnabled()) {
        debugger.debug(new Object() {
          final long finalOldPrio = priority;
          final long finalPrio = lockRequest.getResult().priority;
          final int finalLockCircleSize = lockRequest.getResult().remoteCircleSize;
          @Override
          public String toString() {
            return "Searching for nextLockRequest oldPrio="+finalOldPrio+", prio="+finalPrio+", remoteCircle="+finalLockCircleSize;
          }
        });
      }
      if (lockRequest.getResult().priority > priority && lockRequest.getResult().remoteCircleSize == 0) {
        if (result == null) {
          result = lockRequest;
        } else  if (result.getResult().priority > lockRequest.getResult().priority) {
          result = lockRequest;
        }
      }
    }
    // we did not find a higher priority for an unclosed circle, we might be due to waiting for the old circle
    if (result == null) {
      for (LockRequest lockRequest : allRequests) {
        if (lockRequest.getResult().remoteCircleSize > 0) {
          result = lockRequest;
          break;
        }
      }
    }
    return result;
  }

  /**
   * Get all members of the current cluster.
   * @return list of cluster members
   */
  private ClusterMember[] getCurrentMembers() {
    try {
      return ((ClusterMembers) localCache.read(ID_CLUSTER_MEMBERS).getPayload()).getMembers();
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }
  }

  private void recallRemoteLocks(ClusterMember[] members, long objectId, long priority, long priorityOfLockInOldLockCircle, boolean countedInLockCircle, int circleSize, LockRequest []lockRequests) throws ObjectNotInCacheException {

    if (debugger.isEnabled()) {
      debugger.debug("recalling remote locks");
    }

    if (members.length == 1 && members[0] != null && members[0].getId() == ownClusterNodeID) {
      return;
    }

    // Sortieren der LockRequest anhand der Priorit�t (absteigend), um danach in dieser Reihenfolge die recalls zu versenden.
    Arrays.sort(lockRequests, priorityRequestComperator);
    
    for(LockRequest lockrequest : lockRequests) {

      if (debugger.isEnabled()) {
        final long bla = lockrequest.getResult().priority;
        debugger.debug(new Object() {
          @Override
          public String toString() {
            return "Sending recallRemotLocks with priority " + bla;
          }
        });
      }  

      InterconnectProtocol nodeConnection = lockrequest.getNodeConnection();
      CountDownLatch latch = new CountDownLatch(1);
      RecallLockRequest recallReq = new RecallLockRequest(nodeConnection, objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle, circleSize, latch);
      executeInThreadPool(recallReq);
      try {
        latch.await();
      } catch (InterruptedException e) {
        // TODO prio5: this might happen at factory shutdown when used within a service
        throw new RuntimeException("Got interrupted unexpectedly while waiting for latch", e);
      }
      if(recallReq.isFailed()) {
        throw new ClusterInconsistentException("Recall lock request for object <" + objectId + "> failed. failed thread="
            + recallReq.getFailedThreadName() + ", this thread=" + Thread.currentThread(), recallReq.getException());
      }
      if (recallReq.objectWasRemoved()) {
        throw new ObjectNotInCacheException(objectId);
      }
    }
 
  }

  
  private void releaseRemoteLocks(ClusterMember[] members, long objectId, long priorityToRelease) throws ObjectNotInCacheException {

    if (debugger.isEnabled()) {
      debugger.debug("releasing remote locks");
    }

    if (members.length == 1 && members[0].getId() == ownClusterNodeID) {
      return;
    }

    List<UnlockRequest> unlockRequests = new ArrayList<UnlockRequest>(members.length);

    CountDownLatch latch = new CountDownLatch(members.length);
    for (ClusterMember member : members) {
      if (member.getId() == ownClusterNodeID) {
        latch.countDown();
        continue;
      }
      InterconnectProtocol nodeConnection = getNodeConnection(member);
      UnlockRequest unlockReq = new UnlockRequest(nodeConnection, objectId, priorityToRelease, latch);
      unlockRequests.add(unlockReq);
      executeInThreadPool(unlockReq);
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      // TODO prio5: this might happen at factory shutdown when used within a service
      throw new RuntimeException("Got interrupted unexpectedly while waiting for latch", e);
    }

    for (UnlockRequest ur : unlockRequests) {
      if (ur.isFailed()) {
        throw new ClusterInconsistentException("Unlock request for object <" + objectId + "> failed. failed thread="
            + ur.getFailedThreadName() + ", this thread=" + Thread.currentThread(), ur.getException());
      }
      if (ur.objectWasRemoved()) {
        throw new ObjectNotInCacheException(objectId);
      }
    }

  }


  public List<InterconnectCallee> getCallees() {
    return Collections.unmodifiableList(callees);
  }


  public void setupNewCluster() {
    connectToClusterLocallyInternally(null);
  }


  public void pauseCluster() {
    globalLock.lockAll();
    checkControllerState();
    //erst hier die members ermitteln, damit sie nicht in der zwischenzeit von anderem thread ge�ndert worden sein k�nnen.
    ClusterMember[] members = getCurrentMembers();
    waitForActiveThreads(members, ThreadType.ELSE);
    globalUnlockLock.lockAll();
    waitForActiveThreads(members, ThreadType.UNLOCK);
    specialLock.lockAll();
    waitForActiveThreads(members, ThreadType.SPECIAL);
  }


  private void resumeClusterInternally(ClusterMember[] membersToResume) {
    //locks in umgekehrter reihenfolge wie in pauseCluster() freigeben
    specialLock.unlockAll(membersToResume);
    globalUnlockLock.unlockAll(membersToResume);
    globalLock.unlockAll(membersToResume);
  }


  public void resumeCluster() {
    ClusterMember[] members = getCurrentMembers();
    resumeClusterInternally(members);
  }


  public ClusterNodeState getLocalClusterNodeState() {
    // TODO prio3: Implement getLocalClusterNodeState
    throw new RuntimeException("Unsupported");
  }


  public ClusterState getCompleteClusterState() {
    // TODO prio3: implement getCompleteClusterState
    throw new RuntimeException("Unsupported");
  }


  public ObjectPoolInformation getObjectPoolInformation(boolean includePerObjectInformation) {
    if (includePerObjectInformation) {
      return new ObjectPoolInformation(localCache.getSnapShot(ownClusterNodeID, clusterMemberChangeInformation, false));
    }
    throw new RuntimeException("Unsupported");
  }


  public ThreadPoolInformation getThreadPoolInformation() {
    return ThreadPoolInformation.getThreadPoolInformation("", this.threadpool2, 0);
  }


  public ConsistencyCheckResult checkGlobalConsistency(long objectID) {

    throw new RuntimeException("Unsupported");

//    pauseCluster();
//    try {
//      // TODO prio3 implement global consistency check for one object id
//    } finally {
//      resumeCluster();
//    }
//    return null;

  }


  public ConsistencyCheckResult checkGlobalConsistencyForAllObjects() {
    // TODO prio3 implement global consistency check for all objects
    throw new RuntimeException("Unsupported");
  }


  public void connectToClusterLocally(NodeConnectionProvider nodeConnectionProvider) {
    if (nodeConnectionProvider == null) {
      throw new IllegalArgumentException("Cannot connect to a remote cluster without "
          + NodeConnectionProvider.class.getSimpleName());
    }
    connectToClusterLocallyInternally(nodeConnectionProvider);
  }


  public void onEventNewMembers(ClusterMember[] members) {
    clusterMemberChangeInformation.update(members);
  }


  public void atomicBulkAction(BulkActionContainer actions) throws ObjectNotInCacheException {

    checkControllerState();
    globalLock.checkLock();

    try {

      // first lock all objects in correct order
      Iterator<Long> lockIterator = actions.orderedLockIterator();
      try {
        while (lockIterator.hasNext()) {
          lock(lockIterator.next());
        }

        // execute the bulk commit action remotely
        // TODO do this concurrently while already doing the same locally
        boolean propagateLockCircleSize = false; // no need to take any lock circle into account
        executeActionsRemotelyForMembers(new BulkCommitAction(ownClusterNodeID, actions), getCurrentMembers(),
                                         propagateLockCircleSize, null);

        Iterator<BulkActionElement> elementIterator = actions.orderedElementIterator();
        while (elementIterator.hasNext()) {
          BulkActionElement nextActionElement = elementIterator.next();
          switch (nextActionElement.getType()) {
            case UPDATE :
              CoherenceObject existingObject = localCache.read(nextActionElement.getObjectID());
              BulkUpdateAction updateElement = (BulkUpdateAction) nextActionElement;
              existingObject.setPayload(updateElement.getUpdatedPayload());
              existingObject.getDirectoryData().setModifiedOnLocalNode();
              existingObject.setState(CoherenceState.MODIFIED);
              break;
            case DELETE :
              CoherenceObject deletedObject = localCache.delete(nextActionElement.getObjectID());
//              if (!deletedObject.getLockObject().isHeldLocally()) {
//                // cleanup if the object is not required for a following "unlock"
//                localCache.cleanupDeletedObject(nextActionElement.getObjectID());
//              }
              deletedObject.getLockObject().setDeleted();
              break;
            default :
              throw new RuntimeException("Unrecognized bulk action type: " + nextActionElement.getType());
              // FIXME trigger feuern
          }
        }

        Iterator<Long> unlockIterator = actions.orderedUnlockIterator();
        while (unlockIterator.hasNext()) {
          unlock(unlockIterator.next());
        }

      } finally {
        lockIterator = actions.orderedLockIterator();
        while (lockIterator.hasNext()) {
          unlock(lockIterator.next());
        }
      }

    } finally {
      globalLock.decrementThreadCount();
    }

  }


  private InterconnectRMIClassLoader interconnectRMIClassLoader = new InterconnectRMIClassLoader();


  public InterconnectRMIClassLoader getRMIClassLoader() {
    return interconnectRMIClassLoader;
  }

  /**
   * {@inheritDoc}
   */
  public boolean tryLock(long objectId, long nanos) throws ObjectNotInCacheException, InterruptedException {
    return lock(objectId, true, System.nanoTime() + nanos);
  }
  
  private enum GetRemoteLockState {
    REMOTELOCK_RESULT_SUCCESS, REMOTELOCK_RESULT_TIMEOUT_NO_OLD_CIRCLE, OTHER 
  }
  
  private static class GetRemoteLockResponse {
    long priorityInOldLockCircle;
    int lockCircleSize;
    GetRemoteLockState remoteLockState;
    LockRequest []lockRequests = null;
    
    static GetRemoteLockResponse REMOTELOCK_RESULT_SUCCESS = new GetRemoteLockResponse(0l,0, GetRemoteLockState.REMOTELOCK_RESULT_SUCCESS);
     
    GetRemoteLockResponse(long priorityInOldLockCircle, int lockCircleSize, GetRemoteLockState remoteLockState, LockRequest []lockRequests) {
      this.priorityInOldLockCircle = priorityInOldLockCircle;
      this.lockCircleSize = lockCircleSize;
      this.remoteLockState = remoteLockState;
      this.lockRequests = lockRequests;
    }
    
    GetRemoteLockResponse(long priorityInOldLockCircle, int lockCircleSize, GetRemoteLockState remoteLockState) {
      this.priorityInOldLockCircle = priorityInOldLockCircle;
      this.lockCircleSize = lockCircleSize;
      this.remoteLockState = remoteLockState;
    }
    
  }

  public void connectToCluster() {
    // TODO Auto-generated method stub
    
  }

}
