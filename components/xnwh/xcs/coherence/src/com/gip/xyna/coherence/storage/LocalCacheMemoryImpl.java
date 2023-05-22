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

package com.gip.xyna.coherence.storage;



import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.coherence.ClusterMemberChangeInformation;
import com.gip.xyna.coherence.coherencemachine.CoherenceDirectoryData;
import com.gip.xyna.coherence.coherencemachine.CoherenceObject;
import com.gip.xyna.coherence.coherencemachine.CoherenceState;
import com.gip.xyna.coherence.exceptions.ClusterInconsistentException;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.utils.debugging.Debugger;



/**
 * This is a simple map based memory implementation of the local cache. This may later be replaced by a class that uses
 * the warehouse as data storage.
 */
public class LocalCacheMemoryImpl implements StorageInterface {

  private static final Debugger debugger = Debugger.getDebugger();
  /**
   * Cache content indexed by object ids
   */
  private ConcurrentMap<Long, CoherenceObject> data;

  private ConcurrentMap<Long, CoherenceObject> locksForDeletedObjects = new ConcurrentHashMap<Long, CoherenceObject>();


  public LocalCacheMemoryImpl() {
    // Note that synchronization on a per-object-level is not required since the global locking mechanism of the
    // coherence layer makes sure that object access is always restricted to a single thread.
    data = new ConcurrentHashMap<Long, CoherenceObject>();
  }


  /**
   * Check if an object with the given id is stored in the cache
   */
  public boolean contains(long nextId) {
    return data.containsKey(nextId);
  }


  /**
   * Add the given object to the local cache
   * @param the object to add
   */
  public void create(CoherenceObject newObject) {
    CoherenceObject previousValue = data.put(newObject.getId(), newObject);
    if (previousValue != null) {
      // this is not thread safe but it should never happen anyway. only other failures of the same kind
      // may cause another thread to put the same object id into the map.
      data.put(newObject.getId(), previousValue);
      throw new ClusterInconsistentException("Duplicate ID <" + newObject.getId() + "> while creating object <"
          + newObject.getPayload() + ">");
    }
  }


  /**
   * Delete an object from the local cache and move it to the lockForDeletedObjects list. Mark its lock object as deleted.
   * @param id of the object to remove
   * @return the removed object
   */
  public CoherenceObject delete(long objectId) throws ObjectNotInCacheException {
    Long objectIdObj = objectId; // avoid two conversions "long -> Long"
    CoherenceObject removedObject = data.remove(objectIdObj);
    if (removedObject == null) {
      throw new ObjectNotInCacheException(objectId);
    }
    removedObject.getLockObject().setDeleted();
    locksForDeletedObjects.put(objectIdObj, removedObject);
    return removedObject;
  }


  public CoherenceObject read(long objectId) throws ObjectNotInCacheException {
    CoherenceObject result = data.get(objectId);
    if (result == null) {
      throw new ObjectNotInCacheException(objectId);
    }
    return result;
  }


  public void update(CoherenceObject updatedObject) throws ObjectNotInCacheException {
    // TODO prio2: performance: kann man das hier einfach weglassen? Wenn alle Objekte, die hier reingegeben werden
    // immer die Objekte sind, die vorher per "read" geholt wurden, dann ist das ein �berfl�ssiger HashMap-Zugriff.
    CoherenceObject previousValue = data.put(updatedObject.getId(), updatedObject);
    if (previousValue == null) {
      data.remove(updatedObject.getId());
      throw new ObjectNotInCacheException(updatedObject.getId());
    }
  }


  public List<CoherenceObject> getSnapShot(int localClusterNodeId,
                                           ClusterMemberChangeInformation clusterMemberChangeInformation,
                                           boolean forNewClusterNode) {

    // by the time this method is called the cluster is paused anyway, no extra lock required
    List<CoherenceObject> result = new ArrayList<CoherenceObject>(data.size());
    for (CoherenceObject o : data.values()) {
      if (forNewClusterNode) {
        o.getDirectoryData().removeInvalidClusterMembers(clusterMemberChangeInformation);
        CoherenceObject copy = CoherenceObject.createInvalidCopyWithoutPayload(o);
        copy.getDirectoryData().addDirectoryData(localClusterNodeId, o.getState());
        result.add(copy);
      } else {
        CoherenceObject copy = CoherenceObject.createCopy(o);
        result.add(copy);
      }
    }
    return result;

  }


  /**
   * �berschreibt vorhandene objekte
   */
  public void importShapShot(List<CoherenceObject> metadata) {
    for (CoherenceObject o : metadata) {
      data.put(o.getId(), o);
    }
  }


  /**
   * liefert alle coherence objekte, die auf dem knoten clusterNodeId modified oder exclusive sind.
   */
  public List<CoherenceObject> getAllModifiedExclusive(int clusterNodeId, int localClusterNodeId,
                                                       ClusterMemberChangeInformation clusterMemberChangeInformation) {

    List<CoherenceObject> result = new ArrayList<CoherenceObject>();
    for (CoherenceObject o : data.values()) {
      if (clusterNodeId == localClusterNodeId) {
        CoherenceState state = o.getStateWithCheck(clusterMemberChangeInformation);
        if (state == CoherenceState.EXCLUSIVE || state == CoherenceState.MODIFIED) {
          result.add(o);
        } else {
          if (debugger.isEnabled()) {
            debugNotModifiedExclusiveObject(o);
          }
        }
      } else {
        if (o.getDirectoryData().isModifiedExclusiveOnSpecificNode(clusterNodeId)) {
          result.add(o);
        } else {
          if (debugger.isEnabled()) {
            debugNotModifiedExclusiveObject(o);
          }
        }
      }
    }
    return result;
  }


  private static class CoherenceObjectDebugData {

    private long objectId;
    private CoherenceDirectoryData dirData;


    public CoherenceObjectDebugData(CoherenceObject o) {
      objectId = o.getId();
      dirData = o.getDirectoryData().clone();
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(70);
      sb.append("object ").append(objectId).append(" dirData=").append(dirData);
      return sb.toString();
    }
  }


  private void debugNotModifiedExclusiveObject(CoherenceObject o) {
    debugger.debug(new CoherenceObjectDebugData(o));
  }


  public CoherenceObject popDeletedObjectIfNotLocallyReentrant(long objectId) throws ObjectNotInCacheException {
    Long objectIdObj = objectId; // avoid two conversions "long -> Long"
    CoherenceObject deleted = locksForDeletedObjects.remove(objectIdObj);
    if (deleted == null) {
      throw new ObjectNotInCacheException(objectId);
    }
    if (deleted.getLockObject().isLocallyReentrant()) {
      locksForDeletedObjects.put(objectIdObj, deleted);
    }
    return deleted;
  }
  
  
  /**
   * Get an object marked as deleted from the cache. Leave it in cache.
   */
  public CoherenceObject getDeletedObject(long objectId) throws ObjectNotInCacheException {
    CoherenceObject deleted = locksForDeletedObjects.get(objectId);
    if (deleted == null) {
      throw new ObjectNotInCacheException(objectId);
    }
    return deleted;
  }


  /**
   * Remove an deleted object from cache.
   */
  public void cleanupDeletedObject(final long objectId) {
    if (debugger.isEnabled()) {
      debugger.debug(new Object() {
        @Override
        public String toString() {
          return "cleaning deleted object <" + objectId + ">";
        }
      });
    }
    Long objectIdObj = objectId;
    CoherenceObject lockForDeletedObject = locksForDeletedObjects.remove(objectIdObj);
    if (lockForDeletedObject == null) {
      throw new ClusterInconsistentException("Could not release lock for deleted object <" + objectId + ">, not found");
    }
    lockForDeletedObject.getLockObject().setDeleted();
  }

}
