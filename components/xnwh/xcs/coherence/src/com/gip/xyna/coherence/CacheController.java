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



import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.InterconnectRMIClassLoader;
import com.gip.xyna.coherence.exceptions.InvalidStaticObjectIDException;
import com.gip.xyna.coherence.exceptions.ObjectIDNotUniqueInCacheException;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.ha.BulkActionContainer;
import com.gip.xyna.coherence.management.XynaCoherenceClusterManagement;



/**
 * Schnittstelle zur Koher�nz Schicht
 */
public interface CacheController extends XynaCoherenceClusterManagement {


  /**
   * Minimale fuer statische Verwendung in Applikationen reservierte ID
   */
  public static final long ID_MIN_RESERVED_FOR_STATIC_ALLOCATION = 1000;

  /**
   * Maximale fuer statische Verwendung in Applikationen reservierte ID
   */
  public static final long ID_MAX_RESERVED_FOR_STATIC_ALLOCATION = 9999;


  /**
   * Creates an object within the id range between {@link ID_MIN_RESERVED_FOR_STATIC_ALLOCATION} and
   * {@link ID_MAX_RESERVED_FOR_STATIC_ALLOCATION}
   * @throws ObjectIDNotUniqueInCacheException
   * @throws InvalidStaticObjectIDException
   */
  public long create(long staticObjectId, CoherencePayload payload) throws ObjectIDNotUniqueInCacheException,
      InvalidStaticObjectIDException;


  /**
   * Create a new object for sharing in the cluster.
   * 
   * @param payload the content of the object
   * @return id of the new cluster object
   */
  public long create(CoherencePayload payload);


  /**
   * Read the object specified by <code>objectId</code>.
   * 
   * @param objectId the unique id of the object to be read
   * @return the content of the requested object
   * @throws ObjectNotInCacheException if no object with the given id could be found in the cache
   */
  public CoherencePayload read(long objectId) throws ObjectNotInCacheException;


  public void update(long objectId, CoherencePayload payload) throws ObjectNotInCacheException;


  public void delete(long objectId) throws ObjectNotInCacheException;


  /**
   * Locks the object specified by <code>objectId</code>.
   * 
   * @param objectId the unique id of the object to be locked.
   * @throws ObjectNotInCacheException if the specified object is not contained in the cache.
   */
  public void lock(long objectId) throws ObjectNotInCacheException;


  /**
   * This may also be called on deleted objects to release locks that other remote or local requests may be waiting on.
   */
  public void unlock(long objectId) throws ObjectNotInCacheException;


  public void atomicBulkAction(BulkActionContainer actions) throws ObjectNotInCacheException;


  public InterconnectRMIClassLoader getRMIClassLoader();

  /**
   * Locks the object specified by <code>objectId</code>. Waits for the specific time to get the lock.
   * 
   * @return true, if lock was successful. nanos = differential, not absolute
   */
  public boolean tryLock(long objectId, long nanos) throws ObjectNotInCacheException, InterruptedException;

}
