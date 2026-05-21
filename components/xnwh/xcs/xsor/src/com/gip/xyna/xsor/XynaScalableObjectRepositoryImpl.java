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
package com.gip.xyna.xsor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.cluster.ClusterManagement;
import com.gip.xyna.cluster.ClusterState;
import com.gip.xyna.cluster.StateChangeHandler;
import com.gip.xyna.cluster.SyncType;
import com.gip.xyna.debug.Debugger;
import com.gip.xyna.debug.XSORDebuggingInterface;
import com.gip.xyna.xsor.common.InternalIdAndPayloadPair;
import com.gip.xyna.xsor.common.PendingActionRetrieable;
import com.gip.xyna.xsor.common.ResultCodeWrapper.CreateResultCode;
import com.gip.xyna.xsor.common.ResultCodeWrapper.DeleteResultCode;
import com.gip.xyna.xsor.common.ResultCodeWrapper.GrabResultCode;
import com.gip.xyna.xsor.common.ResultCodeWrapper.ReleaseResultCode;
import com.gip.xyna.xsor.common.ResultCodeWrapper.WriteResultCode;
import com.gip.xyna.xsor.common.exceptions.ActionNotAllowedInClusterStateException;
import com.gip.xyna.xsor.common.exceptions.CollisionWithRemoteRequestException;
import com.gip.xyna.xsor.common.exceptions.RemoteProcessExecutionTimeoutException;
import com.gip.xyna.xsor.common.exceptions.RetryDueToPendingCreateAction;
import com.gip.xyna.xsor.common.exceptions.UnexpectedProcessException;
import com.gip.xyna.xsor.common.exceptions.XSORThrowableCarrier;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.XSORPayloadPrimaryKeyIndex;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.management.BasicIndexFactory;
import com.gip.xyna.xsor.indices.management.IndexManagement;
import com.gip.xyna.xsor.indices.management.IndexManagementImpl;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.indices.tools.MultiIntValueWrapper;
import com.gip.xyna.xsor.interconnect.InterconnectSender;
import com.gip.xyna.xsor.interconnect.InterconnectServer;
import com.gip.xyna.xsor.persistence.PersistenceException;
import com.gip.xyna.xsor.persistence.PersistenceStrategy;
import com.gip.xyna.xsor.persistence.PersistenceStrategy.XSORPayloadPersistenceBean;
import com.gip.xyna.xsor.protocol.XSORMemory;
import com.gip.xyna.xsor.protocol.XSORMemoryDebugger;
import com.gip.xyna.xsor.protocol.XSORNodeProcess;
import com.gip.xyna.xsor.protocol.XSORPayload;
import com.gip.xyna.xsor.protocol.XSORPayloadInformation;
import com.gip.xyna.xsor.protocol.XSORProcess;

public class XynaScalableObjectRepositoryImpl
    implements XynaScalableObjectRepositoryInterface, XSORDebuggingInterface, XSORMemoryRepository {

  private static final Debugger debugger = Debugger.getInstance();
  private static final Logger logger = Logger.getLogger(XynaScalableObjectRepositoryImpl.class.getName());

  private IndexManagement indices = new IndexManagementImpl(new BasicIndexFactory()); // TODO make configurable
  private ConcurrentMap<String, Table> tables; // map von tablename -> xcmemory
  private PersistenceStrategy persistenceStrategy;

  private String nodeId; // id des knotens
  private boolean nodePreference; // bei konfliktauflösung als letzter ausweg verwendet. muss auf beiden knoten
                                  // unterschiedlich sein.

  private InterconnectSender interconnectSender;
  private InterconnectServer interconnectServer;
  private volatile int messagesSentLastSync;
  private volatile boolean syncFinished = true;

  public abstract class RestrictedSearchAlgorithm {

    public void search(SearchRequest searchRequest, SearchParameter searchParameter,
        TransactionContext transactionContext, int maxResults, boolean lockResults,
        boolean strictlyCoherent) {

    }

  }

  public void addIndices(String tablename,
      List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions) {
    indices.createXSORPayloadPrimaryKeyIndex(tablename);
    for (IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion> indexDefinition : indexDefinitions) {
      indices.createIndex(indexDefinition);
    }
  }

  public boolean persistPayload(final XSORPayload payload, final TransactionContext transactionContext,
      final boolean strictlyCoherent)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException {
    final XSORMemory correspondingMemory = tables.get(payload.getTableName()).getXSORMemory();
    final XSORPayloadPrimaryKeyIndex index = indices.getXSORPayloadPrimaryKeyIndex(payload.getTableName());
    return new PendingActionRetrieable<Boolean>() {
      @Override
      public Boolean executeInternally() throws RetryDueToPendingCreateAction, RemoteProcessExecutionTimeoutException,
          CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException {
        int id = index.getUniqueValueForKey(payload.getPrimaryKey());
        if (id < 0) {
          return create(payload, correspondingMemory, transactionContext, strictlyCoherent);
        } else {
          return update(payload, id, correspondingMemory, transactionContext, strictlyCoherent);
        }
      }
    }.execute();
  }

  private boolean create(XSORPayload payload, XSORMemory correspondingMemory, TransactionContext transactionContext,
      boolean strictlyCoherent) throws RemoteProcessExecutionTimeoutException,
      CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException {
    CreateResultCode createResult = XSORProcess.create(payload, correspondingMemory, strictlyCoherent);
    switch (createResult) {
      case NON_UNIQUE_IDENTIFIER:
        // TODO recursive call, not necessarily all that good
        return persistPayload(payload, transactionContext, strictlyCoherent);
      case RESULT_OK:
      case RESULT_OK_ONLY_LOCAL_CHANGES_MASTER:
        return false;
      case TIMEOUT_LOCAL_CHANGES:
        if (strictlyCoherent) {
          throw new RuntimeException("TIMEOUT_LOCAL_CHANGES but we are in strictMode");
        }
        return false;
      case CLUSTER_TIMEOUT_LOCAL_CHANGES:
        if (strictlyCoherent) {
          throw new RemoteProcessExecutionTimeoutException();
        }
        return false;
      case CONFLICTING_REQUEST_FROM_OTHER_NODE:
        throw new CollisionWithRemoteRequestException();
      case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
        throw new ActionNotAllowedInClusterStateException();
      default:
        throw new RuntimeException("No handling defined for " + createResult);
    }
  }

  private boolean update(XSORPayload payload, int internalId, XSORMemory correspondingMemory,
      TransactionContext transactionContext, boolean strictlyCoherent)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException, RetryDueToPendingCreateAction {
    if (transactionContext.isLockedByTransaction(internalId, payload.getTableName())) {
      return updateWithoutGrab(payload, internalId, correspondingMemory, transactionContext, strictlyCoherent);
    } else {
      return updateWithGrabAndTransactionLock(payload, internalId, correspondingMemory, transactionContext,
          strictlyCoherent);
    }
  }

  private boolean updateWithoutGrab(XSORPayload payload, int internalId, XSORMemory correspondingMemory,
      TransactionContext transactionContext, boolean strictlyCoherent)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException {
    WriteResultCode writeResult = XSORProcess.write(internalId, payload, correspondingMemory);
    switch (writeResult) {
      case RESULT_OK:
        return true;
      case OBJECT_NOT_FOUND:
        throw new RuntimeException("We did succesfully grab and now the object is gone!");
      case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
        throw new ActionNotAllowedInClusterStateException();
      case REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE:
        throw new RuntimeException(
            "A locked object was in an invalid state for a write, this might result from a resolved conflict.");
      default:
        throw new RuntimeException("No handling defined for " + writeResult);
    }
  }

  private boolean updateWithGrabAndTransactionLock(XSORPayload payload, int internalId, XSORMemory correspondingMemory,
      TransactionContext transactionContext, boolean strictlyCoherent)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException, RetryDueToPendingCreateAction {
    internalId = txLock(transactionContext, internalId, payload);
    try {
      GrabResultCode grabResult = XSORProcess.grab(internalId, payload, correspondingMemory, strictlyCoherent);
      switch (grabResult) {
        case RESULT_OK:
        case RESULT_OK_ONLY_LOCAL_CHANGES:
        case RESULT_OK_ONLY_LOCAL_CHANGES_MASTER:
        case TIMEOUT_LOCAL_CHANGES:
        case CLUSTER_TIMEOUT_LOCAL_CHANGES:
        case PENDING_ACTIONS_LOCAL_CHANGES:
          if (strictlyCoherent) {
            if (grabResult == GrabResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES) {
              throw new RemoteProcessExecutionTimeoutException();
            }
            if (grabResult == GrabResultCode.TIMEOUT_LOCAL_CHANGES) {
              throw new RuntimeException("TIMEOUT_LOCAL_CHANGES but we are strictlyCoherent");
            }
          }
          transactionContext.grabbed(internalId, payload.getTableName());
          try {
            updateWithoutGrab(payload, internalId, correspondingMemory, transactionContext, strictlyCoherent);
          } finally {
            // writecopy falls vorhanden auch transaction-locken, damit nicht beim
            // sichtbarwerden im index bereits darauf zugegriffen werden kann
            int newId = correspondingMemory.getInternalIdOfWriteCopy(internalId);
            if (newId != internalId) {
              transactionContext.lockForTransaction(newId, payload.getTableName());
            }
            ReleaseResultCode releaseResult;
            try {
              releaseResult = XSORProcess.release(internalId, correspondingMemory, strictlyCoherent);
            } finally {
              if (newId != internalId) {
                // FIXME erst beim commit freigeben?!
                transactionContext.unlockFromTransaction(newId, payload.getTableName());
              }
            }

            switch (releaseResult) {
              case RESULT_OK:
              case RESULT_OK_ONLY_LOCAL_CHANGES:
                break; // nothing to do
              case NOBODY_EXPECTS:
                throw new UnexpectedProcessException(
                    "Unexpected failure of " + XSORProcess.class.getSimpleName() + ".release");
              case REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS:
                // TODO can this happen?
                throw new RuntimeException("This should not happen according to our access pattern");
              case TIMEOUT_LOCAL_CHANGES:
                if (strictlyCoherent) {
                  throw new RuntimeException("TIMEOUT_LOCAL_CHANGES but we are strictlyCoherent");
                }
                break;
              case CLUSTER_TIMEOUT_LOCAL_CHANGES:
              case CLUSTERSTATECHANGED:
                if (strictlyCoherent) {
                  throw new RemoteProcessExecutionTimeoutException();
                } else {
                  break;
                }
              case OBJECT_NOT_FOUND:
                throw new RuntimeException("We did succesfully grab & write and now the object is gone!");
              case CONFLICTING_REQUEST_FROM_OTHER_NODE:
                throw new CollisionWithRemoteRequestException();
              case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
                throw new ActionNotAllowedInClusterStateException();
              case REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE:
                throw new RuntimeException(
                    "A locked object was in an invalid state for a release, this might result from a resolved conflict.");
              default:
                throw new RuntimeException("No handling defined for " + releaseResult);
            }
            transactionContext.released(internalId, payload.getTableName());
          }
          return true;
        case NOBODY_EXPECTS:
          throw new UnexpectedProcessException("Unexpected failure of " + XSORProcess.class.getSimpleName() + ".grab");
        case OBJECT_NOT_FOUND:
          // TODO recursive call, not necessarily all that good
          return persistPayload(payload, transactionContext, strictlyCoherent);
        case CONFLICTING_REQUEST_FROM_OTHER_NODE:
          throw new CollisionWithRemoteRequestException();
        case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
          throw new ActionNotAllowedInClusterStateException();
        case REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS:
          throw new RetryDueToPendingCreateAction();
        case REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE:
          // sounds treatable with a backoff, but that might be already be taken care off
          // by our local concurrency
          // protection (if we have any?)
          throw new RuntimeException("Could not lock object, invalid XSOR-State for locking");
        default:
          throw new RuntimeException("No handling defined for " + grabResult);
      }
    } finally {
      transactionContext.unlockFromTransaction(internalId, payload.getTableName());
    }
  }

  public void deletePayload(final XSORPayload payload, final TransactionContext transactionContext,
      final boolean strictlyCoherent)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException {
    final XSORPayloadPrimaryKeyIndex index = indices.getXSORPayloadPrimaryKeyIndex(payload.getTableName());

    new PendingActionRetrieable<Void>() {
      @Override
      public Void executeInternally() throws RetryDueToPendingCreateAction, RemoteProcessExecutionTimeoutException,
          CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException {
        int internalId = index.getUniqueValueForKey(payload.getPrimaryKey());
        if (internalId < 0) {
          return null; // object does not exist
        } else {
          XSORMemory correspondingMemory = tables.get(payload.getTableName()).getXSORMemory();
          if (transactionContext.isLockedByTransaction(internalId, payload.getTableName())) {
            deletePayloadWithoutGrab(payload, internalId, correspondingMemory, transactionContext, strictlyCoherent);
          } else {
            deletePayloadWithGrabAndTransactionLock(payload, internalId, correspondingMemory, transactionContext,
                strictlyCoherent);
          }
          return null;
        }
      }
    }.execute();
  }

  private void deletePayloadWithGrabAndTransactionLock(XSORPayload payload, int internalId,
      XSORMemory correspondingMemory,
      TransactionContext transactionContext, boolean strictlyCoherent)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException, RetryDueToPendingCreateAction {
    internalId = txLock(transactionContext, internalId, payload);
    try {
      GrabResultCode grabResult = XSORProcess.grab(internalId, payload, correspondingMemory, strictlyCoherent);
      switch (grabResult) {
        case RESULT_OK:
        case RESULT_OK_ONLY_LOCAL_CHANGES:
        case RESULT_OK_ONLY_LOCAL_CHANGES_MASTER:
        case PENDING_ACTIONS_LOCAL_CHANGES:
        case CLUSTER_TIMEOUT_LOCAL_CHANGES:
        case TIMEOUT_LOCAL_CHANGES:
          if (strictlyCoherent) {
            if (grabResult == GrabResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES) {
              throw new RemoteProcessExecutionTimeoutException();
            }
            if (grabResult == GrabResultCode.TIMEOUT_LOCAL_CHANGES) {
              throw new RuntimeException("TIMEOUT_LOCAL_CHANGES but we are strictlyCoherent");
            }
          }
          transactionContext.grabbed(internalId, payload.getTableName());
          boolean successfullDeletion = false;
          try {
            deletePayloadWithoutGrab(payload, internalId, correspondingMemory, transactionContext, strictlyCoherent);
            successfullDeletion = true;
          } finally {
            if (!successfullDeletion) {
              // writecopy falls vorhanden auch transaction-locken, damit nicht beim
              // sichtbarwerden im index bereits darauf zugegriffen werden kann
              int newId = correspondingMemory.getInternalIdOfWriteCopy(internalId);
              if (newId != internalId) {
                transactionContext.lockForTransaction(newId, payload.getTableName());
              }
              ReleaseResultCode releaseResult;
              try {
                releaseResult = XSORProcess.release(internalId, correspondingMemory, strictlyCoherent);
              } finally {
                if (newId != internalId) {
                  transactionContext.unlockFromTransaction(newId, payload.getTableName());
                }
              }

              switch (releaseResult) {
                case RESULT_OK:
                case RESULT_OK_ONLY_LOCAL_CHANGES:
                  break; // nothing to do
                case NOBODY_EXPECTS:
                  throw new UnexpectedProcessException(
                      "Unexpected failure of " + XSORProcess.class.getSimpleName() + ".release");
                case OBJECT_NOT_FOUND:
                  throw new RuntimeException("We did succesfully grab and now the object is gone!");
                case CLUSTERSTATECHANGED:
                case CLUSTER_TIMEOUT_LOCAL_CHANGES:
                  if (strictlyCoherent) {
                    throw new RemoteProcessExecutionTimeoutException();
                  }
                  break;
                case TIMEOUT_LOCAL_CHANGES:
                  if (strictlyCoherent) {
                    throw new RuntimeException("TIMEOUT_LOCAL_CHANGES but we are strictlyCoherent");
                  }
                  break;
                case CONFLICTING_REQUEST_FROM_OTHER_NODE:
                  throw new CollisionWithRemoteRequestException();
                case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
                  throw new ActionNotAllowedInClusterStateException();
                case REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE:
                  throw new RuntimeException(
                      "A locked object was in an invalid state for a release, this might result from a resolved conflict.");
                case REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS:
                  // TODO can this happen?
                  throw new RuntimeException("This should not happen according to our access pattern");
                default:
                  throw new RuntimeException("No handling defined for " + releaseResult);
              }
              transactionContext.released(internalId, payload.getTableName());
            }
          }
          break;
        case NOBODY_EXPECTS:
          throw new UnexpectedProcessException("Unexpected failure of " + XSORProcess.class.getSimpleName() + ".grab");
        case OBJECT_NOT_FOUND:
          throw new RuntimeException("We did succesfully grab and now the object is gone!");
        case CONFLICTING_REQUEST_FROM_OTHER_NODE:
          throw new CollisionWithRemoteRequestException();
        case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
          throw new ActionNotAllowedInClusterStateException();
        case REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE:
          throw new RuntimeException(
              "Grabbing an object it was in an invalid state for a delete, this might result from a resolved conflict or an error in application logic.");
        case REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS:
          throw new RetryDueToPendingCreateAction();
        default:
          throw new RuntimeException("No handling defined for " + grabResult);
      }
    } finally {
      transactionContext.unlockFromTransaction(internalId, payload.getTableName());
    }
  }

  /**
   * lockt das objekt, welches zum PK der übergebenen payload passt und die
   * internalId hat.
   * da sich die letztere aber während des wartens auf das lock ändern kann, wird
   * die internalId
   * nach dem locken überprüft und ggfs erneut ermittelt.
   * 
   * @param returnImmediatelyWhenIdChanged
   * 
   * @return gelockte internalId
   * @throws RetryDueToPendingCreateAction falls objekt gelöscht wurde oder
   *                                       zumindest dieser PK nicht mehr
   *                                       existiert
   */
  int txLock(TransactionContext transactionContext, int internalId, XSORPayload payload,
      boolean returnImmediatelyWhenIdChanged) throws RetryDueToPendingCreateAction {
    while (true) {
      transactionContext.lockForTransaction(internalId, payload.getTableName());

      // nun sicherstellen, dass die id noch die korrekte ist.
      XSORPayloadPrimaryKeyIndex index = indices.getXSORPayloadPrimaryKeyIndex(payload.getTableName());
      int currentInternalId = index.getUniqueValueForKey(payload.getPrimaryKey());
      if (currentInternalId == internalId) {
        // ok
        break;
      } else {
        transactionContext.unlockFromTransaction(internalId, payload.getTableName());
        if (returnImmediatelyWhenIdChanged) {
          return -1;
        }
        if (currentInternalId == -1) {
          throw new RetryDueToPendingCreateAction(); // führt weiter oben im stack zum retry + create. FIXME fehlername
                                                     // irreführend
        }
        internalId = currentInternalId;
      }
    }
    return internalId;
  }

  int txLock(TransactionContext transactionContext, int internalId, XSORPayload payload)
      throws RetryDueToPendingCreateAction {
    return txLock(transactionContext, internalId, payload, false);
  }

  public enum SearchTransactionLockingMechanism {
    HARD(null) {
      @Override
      protected boolean lockInternally(TransactionContext transactionContext, int internalId, String tableName) {
        transactionContext.lockForTransaction(internalId, tableName);
        return true;
      }
    },
    TRY(HARD) {
      @Override
      protected boolean lockInternally(TransactionContext transactionContext, int internalId, String tableName) {
        return transactionContext.tryLockForTransaction(internalId, tableName);
      }
    };

    private SearchTransactionLockingMechanism(SearchTransactionLockingMechanism fallbackMechanism) {
      this.fallbackMechanism = fallbackMechanism;
    }

    private SearchTransactionLockingMechanism fallbackMechanism;

    protected abstract boolean lockInternally(TransactionContext transactionContext, int internalId, String tableName);

    /**
     * @return internalId des gelockten objekts (evtl hat sie sich geändert) oder -1
     *         falls nicht gelockt
     */
    public int lock(TransactionContext transactionContext, int internalId, XSORPayload payload,
        XSORPayloadPrimaryKeyIndex index) {
      while (true) {
        if (lockInternally(transactionContext, internalId, payload.getTableName())) {
          // nun sicherstellen, dass die id noch die korrekte ist.
          int currentInternalId = index.getUniqueValueForKey(payload.getPrimaryKey());
          if (currentInternalId == internalId) {
            // ok
            break;
          } else {
            transactionContext.unlockFromTransaction(internalId, payload.getTableName());
            if (currentInternalId < 0) {
              return currentInternalId;
            }
            internalId = currentInternalId;
          }
        } else {
          // Nicht gelockt
          return -1;
        }
      }
      return internalId;
    }

    public void unlock(TransactionContext transactionContext, int internalId, String tableName) {
      transactionContext.unlockFromTransaction(internalId, tableName);
    }

    public SearchTransactionLockingMechanism getFallbackMechanism() {
      return fallbackMechanism;
    }

    public boolean hasFallbackMechanism() {
      return fallbackMechanism != null;
    }

    public static SearchTransactionLockingMechanism getAppropriateTransactionLockingForSearch(int maxResults) {
      if (maxResults < 0) {
        return SearchTransactionLockingMechanism.HARD;
      } else {
        return SearchTransactionLockingMechanism.TRY;
      }
    }

  }

  public void deletePayloadWithoutGrab(XSORPayload payload, int internalId, XSORMemory correspondingMemory,
      TransactionContext transactionContext, boolean strictlyCoherent)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException {
    DeleteResultCode deleteResult = XSORProcess.delete(payload.getPrimaryKey(), correspondingMemory, strictlyCoherent);
    switch (deleteResult) {
      case RESULT_OK:
      case RESULT_OK_ONLY_LOCAL_CHANGES:
        // nach einem delete gilt das objekt nicht mehr als grabbed (muss nicht mehr
        // released werden)
        transactionContext.released(internalId, payload.getTableName());
        return; // nothing to be done
      case NOBODY_EXPECTS:
        throw new UnexpectedProcessException("Unexpected failure of " + XSORProcess.class.getSimpleName() + ".delete");
      case OBJECT_NOT_FOUND:
        throw new RuntimeException("We did succesfully grab and now the object is gone!");
      case CLUSTER_TIMEOUT_LOCAL_CHANGES:
        if (strictlyCoherent) {
          throw new RemoteProcessExecutionTimeoutException();
        }
        // nach einem delete gilt das objekt nicht mehr als grabbed (muss nicht mehr
        // released werden)
        transactionContext.released(internalId, payload.getTableName());
        return;
      case REQUESTED_ACTION_NOT_ALLOWED_IN_NOT_STRICT_MODE:
        if (strictlyCoherent) {
          throw new RuntimeException("Result: " + deleteResult + " but we are in strictMode");
        } else {
          throw new RuntimeException("Deletion is only allowed in strictMode");
        }
      case CONFLICTING_REQUEST_FROM_OTHER_NODE:
        throw new CollisionWithRemoteRequestException();
      case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
        throw new ActionNotAllowedInClusterStateException();
      case REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE:
        throw new RuntimeException(
            "A locked object was in an invalid state for a delete, this might result from a resolved conflict.");
      default:
        throw new RuntimeException("No handling defined for " + deleteResult);
    }
  }

  public TransactionContext beginTransaction(boolean strictlyCoherent) {
    return TransactionContext.newTransactionContext(this);
  }

  public void endTransaction(TransactionContext transactionContext, boolean strictlyCoherent)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException {
    endTransaction(transactionContext, strictlyCoherent, false);
  }

  // this signature is currently not offered from the interface
  public void endTransaction(TransactionContext transactionContext, boolean strictlyCoherent, boolean rollback)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException {
    if (logger.isTraceEnabled()) {
      logger.trace((strictlyCoherent ? "strictlyCoherent" : "not strictlyCoherent")
          + (rollback ? " endTransaction with rolback" : " endTransaction without rollback") + " for "
          + String.valueOf(transactionContext.getTransactionId()));
    }
    try {
      Map<String, Set<Integer>> allInternalIdsPerTable = transactionContext.getInternalIdsForAllGrabbedObjects();
      List<Throwable> releaseExceptions = new ArrayList<Throwable>();
      if (logger.isTraceEnabled()) {
        logger.trace("number of tables: " + String.valueOf(allInternalIdsPerTable.size()));
      }
      for (Entry<String, Set<Integer>> entry : allInternalIdsPerTable.entrySet()) {
        String tableName = entry.getKey();
        XSORMemory correspondingMemory = tables.get(tableName).getXSORMemory();
        if (logger.isTraceEnabled()) {
          logger.trace("number of entries for table " + tableName + ": " + String.valueOf(entry.getValue().size()));
        }
        for (Integer internalId : entry.getValue()) {
          try {
            if (logger.isTraceEnabled()) {
              logger.trace("trying : " + String.valueOf(internalId));
            }
            if (rollback) {
              correspondingMemory.rollback(internalId);
            }
            // writecopy falls vorhanden auch transaction-locken, damit nicht beim
            // sichtbarwerden im index bereits darauf zugegriffen werden kann
            int newId = correspondingMemory.getInternalIdOfWriteCopy(internalId);
            if (newId != internalId) {
              transactionContext.lockForTransaction(newId, tableName);
            }
            ReleaseResultCode releaseResult;
            try {
              releaseResult = XSORProcess.release(internalId.intValue(), correspondingMemory, strictlyCoherent);
            } finally {
              if (newId != internalId) {
                transactionContext.unlockFromTransaction(newId, tableName);
              }
            }
            switch (releaseResult) {
              case RESULT_OK:
              case RESULT_OK_ONLY_LOCAL_CHANGES:
                break; // nothing to do
              case NOBODY_EXPECTS:
                throw new UnexpectedProcessException(
                    "Unexpected failure of " + XSORProcess.class.getSimpleName() + ".release");
              case OBJECT_NOT_FOUND:
                throw new RuntimeException("Shouldn't we have received this id from " + XSORMemory.class.getSimpleName()
                    + ", what made it disappear?");
              case CLUSTERSTATECHANGED:
                // TODO retry request if the state does still allow it?
                throw new RuntimeException("currently no impl present");
              case REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS:
                // TODO can this happen?
                throw new RuntimeException("This should not happen according to our access pattern");
              case CLUSTER_TIMEOUT_LOCAL_CHANGES:
                if (strictlyCoherent) {
                  throw new RemoteProcessExecutionTimeoutException();
                }
                break;
              case TIMEOUT_LOCAL_CHANGES:
                if (strictlyCoherent) {
                  throw new RuntimeException("TIMEOUT_LOCAL_CHANGES but we are strictlyCoherent");
                }
                break;
              case CONFLICTING_REQUEST_FROM_OTHER_NODE:
                throw new CollisionWithRemoteRequestException();
              case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
                throw new ActionNotAllowedInClusterStateException();
              case REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE:
                throw new RuntimeException(
                    "A locked object was in an invalid state for a release, this might result from a resolved conflict.");
              default:
                throw new RuntimeException("No handling defined for " + releaseResult);
            }
          } catch (Throwable t) {
            if (logger.isTraceEnabled())
              logger.trace(t);
            releaseExceptions.add(t);
          }
        }
      }
      transactionContext.releasedAll();
      if (releaseExceptions.size() > 0) {
        debugger.debug("There were " + releaseExceptions.size() + " problems during endTransaction");
        throw new XSORThrowableCarrier(releaseExceptions);
      }
    } finally {
      transactionContext.releaseTransactionLocks();
    }
  }

  public void initializeTable(String tableName, Class<? extends XSORPayload> clazz, int maxTableSize)
      throws PersistenceException {
    // xcmemory-map initialisieren
    logger.info("initializeTable(" + tableName + ") start");
    XSORMemory xsorMemory = createXSORMemory(clazz, maxTableSize);
    Table table = new Table(maxTableSize, xsorMemory);
    tables.put(tableName, table);
    interconnectSender.register(xsorMemory);
    interconnectServer.register(xsorMemory);

    Iterator<XSORPayloadPersistenceBean> objects = persistenceStrategy.loadObjects(tableName, clazz);
    logger.info("initializeTable(" + tableName + ") read from db");
    int cnt = 0;
    MultiIntValueWrapper.disableIndices();
    while (objects.hasNext()) {
      cnt++;
      XSORPayloadPersistenceBean bean = objects.next();
      XSORNodeProcess.create(bean, xsorMemory);
      // persistPayloadFromStorage(o);
    }
    MultiIntValueWrapper.enableIndices();
    logger.info("initializeTable(" + tableName + ") inserted " + cnt + " records");
  }

  public void removeTable(String tableName, Class<? extends XSORPayload> clazz) throws PersistenceException {
    Table table = tables.remove(tableName);
    if (table != null) {
      interconnectSender.unregister(table.getXSORMemory());
      interconnectServer.unregister(table.getXSORMemory());
    }
    indices.remove(tableName);
  }

  private XSORMemory createXSORMemory(Class<? extends XSORPayload> clazz, int maxTableSize) {
    XSORPayload exampleInstance;
    try {
      exampleInstance = clazz.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException(clazz.getName() + " could not be instantiated.", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(clazz.getName() + " could not be instantiated.", e);
    }

    XSORPayloadInformation ci = clazz.getAnnotation(XSORPayloadInformation.class);
    if (ci == null) {
      throw new RuntimeException(
          XSORPayload.class.getSimpleName() + " must provide annotation " + XSORPayloadInformation.class.getName());
    }
    int uniqueClusterWideTableId = ci.uniqueId();
    int recordSize = ci.recordSize();
    // FIXME maxtablesize ist irreführend, weil writecopys auch platz benötigen.
    return new XSORMemory(recordSize, maxTableSize, uniqueClusterWideTableId, nodeId, nodePreference, exampleInstance,
        indices);
  }

  public void init(String nodeId, boolean nodePreference, final PersistenceStrategy persistenceStrategy,
      final ClusterManagement clusterManagement) {
    this.nodeId = nodeId;
    this.nodePreference = nodePreference;
    this.persistenceStrategy = persistenceStrategy;
    XSORProcess.init(persistenceStrategy, clusterManagement);
    tables = new ConcurrentHashMap<String, Table>();
    clusterManagement.registerStateChangeHandler(new StateChangeHandler() {

      private volatile Thread threadWaitingForSyncToFinish;

      public boolean readyForStateChange(ClusterState oldState, ClusterState newState) {
        if (newState == ClusterState.SYNC_SLAVE) {
          syncFinished = false;
          messagesSentLastSync = 0;
        } else if (newState.isSync()) {
          syncFinished = false;
          messagesSentLastSync = Integer.MAX_VALUE; // wird beim zustandsübergang dann korrekt gesetzt.
        }

        if (newState == ClusterState.NEVER_CONNECTED) {
          addAllObjectsToOutgoingQueue(false);// 1
        } else if (oldState == ClusterState.STARTUP && newState == ClusterState.SYNC_PARTNER) {
          addAllObjectsToOutgoingQueue(false);
        } else if (oldState != ClusterState.NEVER_CONNECTED && newState == ClusterState.SYNC_PARTNER
            && clusterManagement.getOtherNodesSyncType() == SyncType.WAS_NEVER_CONNECTED_BEFORE) {
          clearOutgoingQueues();
          addAllObjectsToOutgoingQueue(false);
        }

        if (newState == ClusterState.SHUTDOWN) {
          prepareToShutdown();
        }
        return true;
      }

      public void onChange(ClusterState oldState, ClusterState newState) {
        if (!newState.isSync()) {
          interruptWaitingForSyncEnd();
        }
        switch (newState) {
          case SYNC_PARTNER:
            syncAsPartner();
            break;
          case SYNC_SLAVE:
            syncAsSlave();
            break;
          case SYNC_MASTER:
            syncAsMaster();
            break;
          case CONNECTED:
            changeToConnected();
            break;
          case NEVER_CONNECTED:
          case DISCONNECTED:
            changeToDisconnectedOrNeverConnected();
            break;
          case DISCONNECTED_MASTER:
            changeToDisconnectedMaster();
            break;
          case SHUTDOWN:
            changeToShutdown();
            break;
          case STARTUP:
            // ntbd
            break;
          case INIT:
          default:
            throw new RuntimeException("unexpected state " + newState);
        }
      }

      private void interruptWaitingForSyncEnd() {
        Thread tt = threadWaitingForSyncToFinish;
        if (tt != null) {
          debugger.debug("New state is not SYNC. Cancelling existing thread waiting for SYNC to finish.");
          tt.interrupt();
        }
      }

      // vor dem zustandsübergang
      private void prepareToShutdown() {
        // (TODO dafür muss derzeit die applikation sorgen)
        // "auftrags(requests an CC)"-eingang unterbinden

        // der andere knoten ist nach shutdown auf disc_master und redet nicht mehr mit
        // uns. diese letzte chance nutzen aufzuräumen

        // auf antworten wartende threads notifizieren

        // queueing mechanismus beenden
      }

      // nach dem zustandsübergang
      private void changeToShutdown() {
        // eingehende requests abarbeiten (TODO siehe todo bei prepareToShutdown)

        changeToDisconnectedOrNeverConnected();
      }

      private void changeToDisconnectedMaster() {
        changeToDisconnectedOrNeverConnected(); // genauso bis auf den state.
      }

      private void changeToDisconnectedOrNeverConnected() {
        debugger
            .debug("Going to disconnect: pausing interconnect, signalling clusterstatechange to all waiting threads.");
        for (Table table : tables.values()) {
          XSORMemory xsorMem = table.getXSORMemory();
          // sender in aufräum-modus bringen (merging von einträgen zum gleichen objekt)
          xsorMem.changeQueueModeToMerging();

          // auf remote-antwort wartende application-threads mit passendem ergebnis
          // (disconnected!) notifzieren
          xsorMem.getWaitManagement().notifyAllWaitingThreadsWithClusterStateChange();
        }

        // incoming replies receiver umstellen, so dass eingehende anfragen sofort
        // negativ notifiziert werden
        // ?????

        // abarbeitung der incoming queue aufhören
        interconnectServer.pauseWorking();

        // versenden der outgoing replies einstellen
        interconnectSender.pauseWorking();
      }

      private void changeToConnected() {
        // ggfs rückgängig machen, was man für sync an besonderen einstellungen brauchte
      }

      private void syncAsMaster() {
        boolean success = false;
        try {
          clearOutgoingQueues();

          addAllObjectsToOutgoingQueue(true);

          prepareInterconnectForSync();

          waitForSyncFinishedAsync();
          success = true;
        } finally {
          if (!success) {
            syncFinished = true;
            clusterManagement.notifySyncFinishedCondition();
            messagesSentLastSync = 0; // damit nicht die synccondition im clusterprovider ewig wartet.
          }
        }
      }

      private void syncAsSlave() {
        boolean success = false;
        try {
          removeAllLocalObjects();

          clearOutgoingQueues(); // TODO sind das die richtigen queues? in removeALlLocalObjects hat man ja
                                 // XSORMemory ausgetauscht

          MultiIntValueWrapper.disableIndices();

          prepareInterconnectForSync();

          waitForSyncFinishedAsync();
          MultiIntValueWrapper.enableIndices();
          success = true;

        } finally {
          if (!success) {
            syncFinished = true;
            clusterManagement.notifySyncFinishedCondition();
          }
        }
      }

      private void syncAsPartner() {
        boolean success = false;
        try {

          prepareInterconnectForSync();

          waitForSyncFinishedAsync();
          success = true;
        } finally {
          if (!success) {
            syncFinished = true;
            clusterManagement.notifySyncFinishedCondition();
          }
        }
      }

      private void waitForSyncFinishedAsync() {
        debugger.info("clusterstate Waiting for sync to finish.");
        threadWaitingForSyncToFinish = new Thread(new Runnable() {

          public void run() {
            // darauf warten, dass die queues abgearbeitet wurden.
            try {
              for (Table table : tables.values()) {
                XSORMemory xsorMem = table.getXSORMemory();
                try {
                  xsorMem.waitForQutgoingMessages();
                } catch (InterruptedException e) {
                  debugger.trace(null, e);
                  debugger.info("Thread waiting for SYNC to finish got interrupted.");
                  // dann muss man nicht notifySyncFinishedCondition aufrufen, weil der state
                  // nicht mehr sync ist...
                  return;
                }
              }
            } finally {
              threadWaitingForSyncToFinish = null; // nicht mehr unterbrechbar
            }

            debugger.info("clusterstate Sync finished");
            syncFinished = true;
            clusterManagement.notifySyncFinishedCondition();
          }
        }, "SyncFinishWaiting-Thread");
        threadWaitingForSyncToFinish.setDaemon(true);
        // in eigenem thread warten
        threadWaitingForSyncToFinish.start();
      }

      private void clearOutgoingQueues() {
        debugger.debug("Clearing outgoing queues.");
        for (Table table : tables.values()) {
          XSORMemory xsorMem = table.getXSORMemory();
          xsorMem.getOutgoingQueue().clear();
        }
        interconnectSender.clearQueue();
        interconnectServer.clearQueue();
      }

      private void prepareInterconnectForSync() {
        debugger.debug("Activating interconnect.");
        // sicherstellen, dass man sicher bestimmen kann, wann alle eben eingestellten
        // nachrichten der queue
        // verarbeitet worden sind, also dass XCMemory.waitForQutgoingMessages
        // funktioniert.

        // queueing mechanismus in sende modus bringen
        for (Table table : tables.values()) {
          XSORMemory xsorMem = table.getXSORMemory();
          xsorMem.changeQueueModeToSend();
        }

        // outgoing replies versenden
        // wenn man zu früh daten empfängt, ist xcmemory evtl noch nicht korrekt
        // registriert.
        interconnectServer.continueWorking();

        // nach interconnectServer.continueWorking(); damit ist einer der Server immer
        // empfangsbereit
        messagesSentLastSync = interconnectSender.continueWorking();

      }

      private void addAllObjectsToOutgoingQueue(boolean insertAtHeadOfQueue) {
        debugger.debug("Adding all local objects to outgoing queue for later sync.");
        // für alle lokal vorhandenen objekte einen eintrag in die outgoing queue
        // schreiben (falls noch nicht vorhanden)
        for (Table table : tables.values()) {
          XSORMemory xsorMem = table.getXSORMemory();
          xsorMem.addAllObjectsToOutgoingQueue(insertAtHeadOfQueue);
        }
      }

      private void removeAllLocalObjects() {
        debugger.debug("Clearing all local objects, because other node is master.");
        Set<String> tableNames = new HashSet<String>(tables.keySet());
        for (String tableName : tableNames) {

          // neues (leeres) xcmemory erstellen
          // aus speichergründen wiederverwendung des vorhandenen byte-arrays
          Table oldTable = tables.get(tableName);
          XSORMemory oldXSORMem = oldTable.getXSORMemory();
          XSORMemory newXSORMem = new XSORMemory(oldXSORMem, indices);
          Table newTable = new Table(oldTable.getSize(), newXSORMem);

          // im interconnect xcmemory austauschen
          interconnectSender.register(newXSORMem);
          interconnectServer.register(newXSORMem);

          // xc memory austauschen
          tables.put(tableName, newTable);

          // FIXME gleichzeitig ankommende requests sehen noch die alten indizes => besser
          // wäre, diese neu anzulegen, so dass sie erst
          // mit dem ersetzen von XCMemory sichtbar werden.
          indices.clear(tableName);

          // altes xcmemory leeren
          // TODO hilft es hier noch etwas zu tun, oder genügt es, die referenz zu
          // vergessen und gc tun zu lassen?

          // alles aus backingstore löschen
          try {
            persistenceStrategy.clearAllData(tableName, newXSORMem.getExample().getClass());
          } catch (PersistenceException e) {
            debugger.warn("could not clear persistent backing store for " + tableName + ".", e);
          }
        }
      }

    });
    clusterManagement.registerSyncFinishedCondition();
    debugger.info("xyna coherence initialized.");
  }

  public void setInterconnect(InterconnectSender interconnectSender, InterconnectServer interconnectServer) {
    this.interconnectSender = interconnectSender;
    this.interconnectServer = interconnectServer;
  }

  // ------------------------ DEBUGGING -------------------------------------

  private void writeTable(BufferedWriter w, String tableName) {
    try {
      w.write("------------ ");
      w.write(tableName);
      w.write(" ------------");
      w.newLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void listAllObjects(BufferedWriter w) {
    Set<String> tableNames = new HashSet<String>(tables.keySet());
    for (String tableName : tableNames) {
      writeTable(w, tableName);
      XSORMemory xsorMem = tables.get(tableName).getXSORMemory();
      XSORMemoryDebugger xsorMemDebugger = new XSORMemoryDebugger(xsorMem);
      xsorMemDebugger.listAllObjects(w);
    }
    try {
      w.flush();
      w.close();
    } catch (IOException e) {
      debugger.warn(null, e);
    }
  }

  public void listObject(BufferedWriter w, byte[] pk) {
    Set<String> tableNames = new HashSet<String>(tables.keySet());
    for (String tableName : tableNames) {
      writeTable(w, tableName);
      XSORMemory xsorMem = tables.get(tableName).getXSORMemory();
      XSORMemoryDebugger xsorMemDebugger = new XSORMemoryDebugger(xsorMem);
      xsorMemDebugger.listObject(w, pk);
    }
    try {
      w.flush();
      w.close();
    } catch (IOException e) {
      debugger.warn(null, e);
    }
  }

  public void listQueueState(BufferedWriter w) {
    Set<String> tableNames = new HashSet<String>(tables.keySet());
    for (String tableName : tableNames) {
      writeTable(w, tableName);
      XSORMemory xsorMem = tables.get(tableName).getXSORMemory();
      XSORMemoryDebugger xsorMemDebugger = new XSORMemoryDebugger(xsorMem);
      xsorMemDebugger.writeQueueState(w);
    }
    try {
      w.flush();
      w.close();
    } catch (IOException e) {
      debugger.warn(null, e);
    }
  }

  public void listFreeListState(BufferedWriter w) {
    Set<String> tableNames = new HashSet<String>(tables.keySet());
    for (String tableName : tableNames) {
      writeTable(w, tableName);
      XSORMemory xsorMem = tables.get(tableName).getXSORMemory();
      XSORMemoryDebugger xsorMemDebugger = new XSORMemoryDebugger(xsorMem);
      xsorMemDebugger.writeFreeListState(w);
    }
    try {
      w.flush();
      w.close();
    } catch (IOException e) {
      debugger.warn(null, e);
    }
  }

  public Table getByTableName(String tableName) {
    return tables.get(tableName);
  }

  public void checkPrimaryKeyIndexIntegrity(BufferedWriter w, String[] indexIdentifiers) {
    if (indexIdentifiers.length == 0) {
      Set<String> tablesSet = tables.keySet();
      List<String> tablesList = new ArrayList<String>(tablesSet);
      indexIdentifiers = tablesList.toArray(new String[tablesSet.size()]);
    }
    for (String tableName : indexIdentifiers) {
      try {
        w.write("Checking integrity of pkindex for");
        w.write(tableName);
        w.newLine();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      indices.getXSORPayloadPrimaryKeyIndex(tableName).checkIntegrity(w);
    }
    try {
      w.flush();
      w.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void unlock(int idx) {
    // xcMemoryMap.get(tableName).unlockAllObjectsForTransactionId()
  }

  // TODO unschön, die folgenden beiden methoden zu veröffentlichen
  public int getNumberOfMessagesSentLastSync() {
    return messagesSentLastSync;
  }

  public boolean isSyncFinished() {
    return syncFinished;
  }

  IndexManagement getIndexManagement() {
    return indices;
  }

  /**
   * sucht objekte ohne reihenfolge zu gewährleisten. z.b. können so zwei select
   * for updates gleichzeitig mit
   * gleicher bedingung durchgeführt werden, wenn die summe der maxrows
   * kleinergleich der menge passender objekte ist, weil
   * dann jeder thread einfach die bereits gelockten objekte überspringt.
   * 
   * usecase: dhcp lease vergabe -> ziehen eines freien leases.
   */
  public List<XSORPayload> search(SearchRequest searchRequest, SearchParameter searchParameter,
      TransactionContext transactionContext, int maxResults, boolean lockResults,
      boolean strictlyCoherent) throws RemoteProcessExecutionTimeoutException,
      CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException {
    String tableName = searchRequest.getTablename();
    XSORMemory correspondingMemory = tables.get(tableName).getXSORMemory();
    XSORPayloadPrimaryKeyIndex index = indices.getXSORPayloadPrimaryKeyIndex(searchRequest.getTablename());
    SearchingIndexSearchResultIterator sisri = new SearchingIndexSearchResultIterator(indices, searchRequest,
        searchParameter, maxResults, strictlyCoherent);
    try {
      iteration: while (sisri.hasNext()) {
        int internalId = sisri.next();
        XSORPayload payload = XSORProcess.read(internalId, correspondingMemory);
        if (!searchRequest.fits(payload, searchParameter)) {
          sisri.remove();
        } else {
          sisri.addFittingPayload(payload);
        }

        // eigtl soll jetzt das nächste objekt gelesen werden, nur wenn man mal alle
        // hat, will man die evtl noch locken, und deshalb
        // passiert hier viel innerhalb der iteration
        if ((maxResults > -1 && sisri.fittingResultSize() >= maxResults) || !sisri.hasNext()) {
          if (!lockResults) {
            return sisri.getFittingResults();
          } else {
            List<InternalIdAndPayloadPair> fittingPairs = sisri.getFittingPairs();
            SearchTransactionLockingMechanism transactionLocking = SearchTransactionLockingMechanism
                .getAppropriateTransactionLockingForSearch(maxResults);
            while (transactionLocking != null) { // [TRY ->] HARD -> null
              boolean gotAllTxLocks = true;
              List<InternalIdAndPayloadPair> transactionLockedPairs = new ArrayList<InternalIdAndPayloadPair>();
              for (int i = 0; i < fittingPairs.size(); i++) {
                InternalIdAndPayloadPair fittingPair = fittingPairs.get(i);
                int possibleChangedId = transactionLocking.lock(transactionContext, fittingPair.getInternalId(),
                    fittingPair.getPayload(), index);
                if (possibleChangedId >= 0) {
                  transactionLockedPairs.add(new InternalIdAndPayloadPair(possibleChangedId, fittingPair.getPayload()));
                } else {
                  // nicht gelockt
                  gotAllTxLocks = false;
                }
              }
              boolean enoughResults = transactionLockedPairs.size() >= maxResults;
              if (!enoughResults && (sisri.hasNext() || !gotAllTxLocks)) {
                for (InternalIdAndPayloadPair transactionLockedPair : transactionLockedPairs) {
                  transactionLocking.unlock(transactionContext, transactionLockedPair.getInternalId(), tableName);
                }
                if (sisri.hasNext()) {
                  continue iteration; // lets get those nexts!
                } else {
                  transactionLocking = transactionLocking.getFallbackMechanism();
                }
              } else {
                List<Integer> grabbedIds = new ArrayList<Integer>();
                List<XSORPayload> grabbedPayloads = new ArrayList<XSORPayload>();
                for (int i = 0; i < transactionLockedPairs.size(); i++) {
                  InternalIdAndPayloadPair transactionLockedPair = transactionLockedPairs.get(i);
                  XSORPayload grabbedPayload = grabDoubleCheckAndReturnRereadPayload(
                      transactionLockedPair.getInternalId(), transactionLockedPair.getPayload(),
                      correspondingMemory, strictlyCoherent, searchRequest,
                      searchParameter, maxResults, transactionContext);
                  if (grabbedPayload != null) { // if null it will already be unlocked from the transaction
                    grabbedPayloads.add(grabbedPayload);
                    if (grabbedPayloads.size() >= maxResults && !(maxResults < 0)) {
                      return grabbedPayloads;
                    }
                    grabbedIds.add(transactionLockedPair.getInternalId());
                  }
                }
                if (!sisri.hasNext()) { // if we had enough results we would have returned above
                  return grabbedPayloads;
                } else {
                  for (int i = 0; i < grabbedIds.size(); i++) { // TODO collect errors?
                    releaseAndUnlockFromTransaction(grabbedIds.get(i), grabbedPayloads.get(i), correspondingMemory,
                        strictlyCoherent, transactionContext);
                  }
                  continue iteration;
                }
              }
            }
          }
        }
      }
      return Collections.emptyList();
    } finally {
      sisri.close();
    }
  }

  protected XSORPayload grabDoubleCheckAndReturnRereadPayload(int internalId, XSORPayload payload,
      XSORMemory correspondingMemory,
      boolean strictlyCoherent, SearchRequest searchRequest,
      SearchParameter searchParameter, int maxResult,
      TransactionContext transactionContext)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException {
    if (transactionContext.isLockedByTransaction(internalId, searchRequest.getTablename())) {
      switch (correspondingMemory.getState(internalId)) {
        case 'E':
        case 'M':
          return checkAndReturnRereadPayload(internalId, correspondingMemory, searchRequest, searchParameter); // no
                                                                                                               // grab
                                                                                                               // and
                                                                                                               // possible
                                                                                                               // release
        default:
          GrabResultCode grabResult = XSORProcess.grab(internalId, payload, correspondingMemory, strictlyCoherent);
          switch (grabResult) {
            case RESULT_OK:
            case RESULT_OK_ONLY_LOCAL_CHANGES:
            case RESULT_OK_ONLY_LOCAL_CHANGES_MASTER:
            case TIMEOUT_LOCAL_CHANGES:
            case CLUSTER_TIMEOUT_LOCAL_CHANGES:
            case PENDING_ACTIONS_LOCAL_CHANGES: // TODO correct treatment?
              if (strictlyCoherent) {
                if (grabResult == GrabResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES) {
                  throw new RemoteProcessExecutionTimeoutException();
                }
                if (grabResult == GrabResultCode.TIMEOUT_LOCAL_CHANGES) {
                  throw new RuntimeException("TIMEOUT_LOCAL_CHANGES but we are strictlyCoherent");
                }
                if (grabResult == GrabResultCode.PENDING_ACTIONS_LOCAL_CHANGES) {
                  throw new RuntimeException("PENDING_ACTIONS_LOCAL_CHANGES but we are strictlyCoherent");
                }
              }
              transactionContext.grabbed(internalId, searchRequest.getTablename());
              boolean doubleCheckSuccessfull = false;
              try {
                XSORPayload rereadPayload = checkAndReturnRereadPayload(internalId, correspondingMemory, searchRequest,
                    searchParameter);
                doubleCheckSuccessfull = rereadPayload != null;
                return rereadPayload;
              } finally {
                if (!doubleCheckSuccessfull) {
                  releaseAndUnlockFromTransaction(internalId, payload, correspondingMemory, strictlyCoherent,
                      transactionContext);
                }
              }
            case NOBODY_EXPECTS:
              throw new UnexpectedProcessException(
                  "Unexpected failure of " + XSORProcess.class.getSimpleName() + ".grab");
            case OBJECT_NOT_FOUND:
              return null;
            case REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS:
              return null;
            case CONFLICTING_REQUEST_FROM_OTHER_NODE:
              // state is 'E' but the grab failed due to inconsitency.
              transactionContext.grabbed(internalId, searchRequest.getTablename());
              releaseAndUnlockFromTransaction(internalId, payload, correspondingMemory, strictlyCoherent,
                  transactionContext);
              throw new CollisionWithRemoteRequestException();
            case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
              throw new ActionNotAllowedInClusterStateException();
            case REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE:
              // sounds treatable with a backoff, but that might be already be taken care off
              // by our local concurrency
              // protection (if we have any?)
              throw new RuntimeException("Could not lock object, invalid XSOR-State for locking");
            default:
              throw new RuntimeException("No handling defined for " + grabResult);
          }
      }
    }
    throw new RuntimeException("TransactionLockedPair is not locked!");
  }

  private XSORPayload checkAndReturnRereadPayload(int internalId, XSORMemory correspondingMemory,
      SearchRequest searchRequest, SearchParameter searchParameter) {
    XSORPayload payload = XSORProcess.read(internalId, correspondingMemory);
    if (searchRequest.fits(payload, searchParameter)) {
      return payload;
    } else {
      return null;
    }
  }

  protected void releaseAndUnlockFromTransaction(int internalId, XSORPayload payload, XSORMemory correspondingMemory,
      boolean strictlyCoherent, TransactionContext transactionContext)
      throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException,
      ActionNotAllowedInClusterStateException {
    try {
      // writecopy falls vorhanden auch transaction-locken, damit nicht beim
      // sichtbarwerden im index bereits
      // darauf zugegriffen werden kann
      int newId = correspondingMemory.getInternalIdOfWriteCopy(internalId);
      if (newId != internalId) {
        transactionContext.lockForTransaction(newId, payload.getTableName());
      }
      ReleaseResultCode releaseResult;
      try {
        releaseResult = XSORProcess.release(internalId, correspondingMemory, strictlyCoherent);
      } finally {
        if (newId != internalId) {
          transactionContext.unlockFromTransaction(newId, payload.getTableName());
        }
      }
      switch (releaseResult) {
        case RESULT_OK:
        case RESULT_OK_ONLY_LOCAL_CHANGES:
        case TIMEOUT_LOCAL_CHANGES:
        case CLUSTER_TIMEOUT_LOCAL_CHANGES:
          break;
        case NOBODY_EXPECTS:
          throw new UnexpectedProcessException("Unexpected failure of " + XSORProcess.class.getSimpleName()
              + ".release");
        case OBJECT_NOT_FOUND:
          break; // might have vanished, no need to release it
        case CLUSTERSTATECHANGED:
          if (strictlyCoherent) { // TODO throw in !strictlyCoherent as well
            throw new RemoteProcessExecutionTimeoutException();
          }
          break;
        case CONFLICTING_REQUEST_FROM_OTHER_NODE:
          transactionContext.released(internalId, payload.getTableName());
          throw new CollisionWithRemoteRequestException();
        case REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE:
          throw new ActionNotAllowedInClusterStateException();
        case REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE:
          // sounds treatable with a backoff, but that might be already be taken care off
          // by our local
          // concurrency protection (if we have any?)
          throw new RuntimeException("Could not lock object, invalid XSOR-State for locking");
        case REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS:
          // TODO can this happen?
          throw new RuntimeException("This should not happen according to our access pattern");
        default:
          throw new RuntimeException("No handling defined for " + releaseResult);
      }
      transactionContext.released(internalId, payload.getTableName());
    } finally {
      transactionContext.unlockFromTransaction(internalId, payload.getTableName());
    }

  }

}
