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

package com.gip.xyna.coherence.coherencemachine;



import java.util.Iterator;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.ClusterMemberChangeInformation;
import com.gip.xyna.coherence.exceptions.ClusterInconsistentException;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.ha.BulkActionElement;
import com.gip.xyna.coherence.storage.StorageInterface;
import com.gip.xyna.coherence.utils.debugging.Debugger;



public class ActionHandler {


  private StorageInterface localCache;
  private CacheController cacheController;


  public ActionHandler(StorageInterface localCache, CacheController cacheController) {
    this.localCache = localCache;
    this.cacheController = cacheController;
  }


  /**
   * In all cases the remote node has to be added to the directory with its resulting state SHARED. If the entry has
   * been EXCLUSIVE or MODIFIED on the local node, the state has to be changed to SHARED.
   * @return
   */
  public CoherencePayload handleReadAction(long objectId, ReadObjectAction action, boolean needsToReturnValue,
                                           ClusterMemberChangeInformation clusterMemberChangeInfo)
      throws ObjectNotInCacheException {

    CoherenceObject existingObject;
    existingObject = localCache.read(objectId);

    switch (existingObject.getStateWithCheck(clusterMemberChangeInfo)) {
      case SHARED :
        //requesting cluster node kommt als zus�tzlicher shared node dazu.
        existingObject.getDirectoryData().addSharingClusterNode(action.getRequestingClusterNodeID());
        break;
      case INVALID :
        // no state transition to be done
        existingObject.getDirectoryData().convertToShared(action.getRequestingClusterNodeID());
        break;
      case EXCLUSIVE :
      case MODIFIED :
        existingObject.setState(CoherenceState.SHARED);
        existingObject.getDirectoryData().convertToShared(action.getRequestingClusterNodeID());
        break;
      default :
        throw new RuntimeException("Unsupported action type: <" + action.getActionType() + ">");
    }

    localCache.update(existingObject);

    if (needsToReturnValue) {
      if (Debugger.getDebugger().isEnabled()) {
        Debugger.getDebugger().debug("NEED to return value for read request");
      }
      if (existingObject.getState() == CoherenceState.INVALID) {
        throw new ClusterInconsistentException("read has been requested on invalid object (id <"
            + existingObject.getId() + ">)");
      }
      CoherencePayload returnValue = existingObject.getPayload();
      if (returnValue == null) {
        throw new ClusterInconsistentException("Failed to return payload for object <" + existingObject.getId()
            + ">, payload is null. state was <" + existingObject.getState() + ">");
      }
      return returnValue;
    } else {
      if (Debugger.getDebugger().isEnabled()) {
        Debugger.getDebugger().debug("NO NEED to return value for read request");
      }
      return null;
    }

  }


  /**
   * If the object is updated on another node, the local copy is invalid and the only relevant remote state is that the
   * object is in state MODIFIED on one node
   */
  public void handleUpdateAction(long objectId, UpdateObjectAction action) throws ObjectNotInCacheException {
    handleUpdateActionInternally(objectId, action.getRequestingClusterNodeID());
  }


  private void handleUpdateActionInternally(long objectId, int requestingClusterNodeID)
      throws ObjectNotInCacheException {

    CoherenceObject existingObject;
    existingObject = localCache.read(objectId);

    existingObject.setState(CoherenceState.INVALID);
    // clear the payload to free up memory and/or reduce the cost of writing the object to a persistence layer
    existingObject.setPayload(null);
    existingObject.getDirectoryData().setModifiedOnRemoteNode(requestingClusterNodeID);
    localCache.update(existingObject);
    existingObject.onEvent(ObjectChangeEvent.DIRECTORYUPDATE_WHEN_UPDATE, cacheController);

  }


  /**
   * The only thing to be done is remove the coherence object from the local cache.
   */
  public void handleDeleteAction(long objectId, boolean cleanupDeleted) throws ObjectNotInCacheException {
    handleDeleteActionInternally(objectId, cleanupDeleted);
  }


  private void handleDeleteActionInternally(long objectId, boolean cleanupDeleted) throws ObjectNotInCacheException {
    CoherenceObject deletedObject = localCache.delete(objectId);
    if (cleanupDeleted) {
      localCache.cleanupDeletedObject(objectId);
    }
    deletedObject.onEvent(ObjectChangeEvent.DELETE, cacheController);
    deletedObject.getLockObject().setDeleted();
  }


  /**
   * The only thing to be done is create a new entry in the local cache that knows that the only entry containing valid
   * data is the one remote node that holds the valid copy in state EXCLUSIVE.
   */
  public void handleCreateAction(long objectId, CreateObjectAction action) {
    CoherenceObject newObject =
        CoherenceObject.createNewInvalidObjectForCreate(objectId, action.getRequestingClusterNodeID());
    localCache.create(newObject);
    newObject.onEvent(ObjectChangeEvent.OBJECT_CREATION, cacheController);
  }

  /**
   * falls payload gesetzt ist, wird sie lokal gesetzt, ansonsten ist die action nur zur benachrichtigung, an wen gepusht wurde.
   * 
   * pushen ist nicht additiv, sondern die in der action angegebenen knoten, die das objekt halten sind absolut. <p>
   * alle anderen werden auf invalid gesetzt. die push-action wird nur aufgerufen, falls die payload gesetzt ist.
   */
  public void handlePushAction(long objectId, int ownId, PushObjectAction action) throws ObjectNotInCacheException {

    CoherenceObject existingObject = localCache.read(objectId);

    if (action.getPayload() == null) {
      if (action.getNodesHoldingSharedObject().length > 1) {
        existingObject.getDirectoryData().setShared(action.getNodesHoldingSharedObject(), ownId);
        if (existingObject.getDirectoryData().getAllClusterIDsHoldingValidCopies().size() != action.getNodesHoldingSharedObject().length) {
          throw new ClusterInconsistentException("push action contained invalid cluster node id");
        }
      } else if (action.getNodesHoldingSharedObject().length == 1) {
        if (action.getNodesHoldingSharedObject()[0] == ownId) {
          throw new ClusterInconsistentException("push has been called incorrectly with payload but wrong nodeinformation");
        }
        existingObject.getDirectoryData().setEclusiveOnRemoteNode(action.getNodesHoldingSharedObject()[0]);
      } else {
        throw new ClusterInconsistentException("push has been called with empty nodes set.");
      }
      existingObject.setState(CoherenceState.INVALID);
      existingObject.setPayload(null);
    } else {
      if (action.getNodesHoldingSharedObject().length > 1) {
        existingObject.getDirectoryData().setShared(action.getNodesHoldingSharedObject(), ownId);
        existingObject.setState(CoherenceState.SHARED);
      } else if (action.getNodesHoldingSharedObject().length == 1) {
        if (action.getNodesHoldingSharedObject()[0] != ownId) {
          throw new ClusterInconsistentException("push has been called incorrectly with payload but wrong nodeinformation");
        }
        existingObject.getDirectoryData().setModifiedOnLocalNode();
        existingObject.setState(CoherenceState.EXCLUSIVE);
      } else {
        throw new ClusterInconsistentException("push has been called with empty nodes set.");
      }
      existingObject.setPayload(action.getPayload());
    }

    localCache.update(existingObject);
    existingObject.onEvent(ObjectChangeEvent.PUSH, cacheController);
  }


  public void handleBulkAction(BulkCommitAction actions) throws ObjectNotInCacheException {
    Iterator<BulkActionElement> elementIterator = actions.getActionContainer().orderedElementIterator();
    while (elementIterator.hasNext()) {
      BulkActionElement nextActionElement = elementIterator.next();
      switch (nextActionElement.getType()) {
        case UPDATE :
          handleUpdateActionInternally(nextActionElement.getObjectID(), actions.getRequestingClusterNodeID());
          break;
        case DELETE :
          // cleanup is taken care of during unlock
          handleDeleteActionInternally(nextActionElement.getObjectID(), false);
          break;
        default :
          throw new RuntimeException("Unrecognized bulk action type: " + nextActionElement.getType());
      }
    }
  }

}
