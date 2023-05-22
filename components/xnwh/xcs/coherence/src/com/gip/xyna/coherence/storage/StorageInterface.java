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

import java.util.List;

import com.gip.xyna.coherence.ClusterMemberChangeInformation;
import com.gip.xyna.coherence.coherencemachine.CoherenceObject;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;


/**
 * Represents a local cache.
 */
public interface StorageInterface {

  void create(CoherenceObject newObject);


  CoherenceObject delete(long objectId) throws ObjectNotInCacheException;


  CoherenceObject read(long objectId) throws ObjectNotInCacheException;
//  CoherenceObject readAndFlag(long objectId) throws ObjectNotInCacheException;


  void update(CoherenceObject updatedObject) throws ObjectNotInCacheException;


  List<CoherenceObject> getSnapShot(int localClusterNodeId,
                                    ClusterMemberChangeInformation clusterMemberChangeInformation,
                                    boolean forNewClusterNode);


  void importShapShot(List<CoherenceObject> metadata);


  List<CoherenceObject> getAllModifiedExclusive(int clusterNodeId, int localClusterNodeId,
                                                ClusterMemberChangeInformation clusterMemberChangeInformation);


  boolean contains(long staticObjectId);


  void cleanupDeletedObject(long objectId);


  public CoherenceObject popDeletedObjectIfNotLocallyReentrant(long objectId) throws ObjectNotInCacheException;
  
  
  public CoherenceObject getDeletedObject(long objectId) throws ObjectNotInCacheException;

}
