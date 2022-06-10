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
package com.gip.xyna.coherence.coherencemachine.interconnect;



import com.gip.xyna.coherence.coherencemachine.CoherenceAction;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockAwaitResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockRequestResponse;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.NodeInformation;



/**
 * Schnittstelle zu anderen Knoten der Kohärenz. Es gibt in einem Kohärenz Knoten normalerweise zwei Implementierungen.
 * Die eine ist die des Callers, die andere die das Callees.
 */
public interface InterconnectProtocol {

  /**
   * es war kein weiterer thread am locken
   */
  public static final int SUCCESSFUL_LOCK_NO_COMPARISON_REQ = -1;

  /**
   * This is returned if the local thread already owns the requested lock and thus does not need to get any further
   * remote locks.
   */
  public static final int RESPONSE_REENTRANT_LOCAL_LOCK = -2; //weil nur für lokale requests gedacht, steht es hier nur der vollständigkeit halber, damit nicht einer auf die idee kommt, dass -2 nicht vergeben ist

  /**
   * es gab andere threads, und man hat gewonnen
   */
  public static final int SUCCESSFUL_LOCK_BY_COMPARISON = -3;

  /**
   * es konnte nicht entschieden werden wie vorgegangen werden muss => retry notwendig
   */
  public static final int RETRY_LOCK = -5;


  /**
   * Versuch einen Lock auf das angegebene Objekt zu erlangen.
   * gibt SUCCESSFUL_LOCK_NO_COMPARISON_REQ zurück, falls Lock erfolgreich erlangt wurde, ohne dass mit der angefragten
   * verglichen werden musste bzw. ohne dass der Knoten lockend/gelockt ist. Ansonsten wird die Priorität des
   * vorhandenen Locks auf der anderen Seite zurückgegeben. Gibt SUCCESSFUL_LOCK_BY_COMPARISON zurück, falls es andere
   * Lock-Anfragen an diesen Knoten gibt.
   */
  public LockRequestResponse requestLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException;


  /**
   * führt die angegebenen Actions remote aus.
   */
  public CoherencePayload executeActions(CoherenceAction actions);


  /**
   * gibt das aktuelle lock auf das objekt frei
   */
  public void releaseLock(long objectId, long priorityToRelease) throws ObjectNotInCacheException;


  /**
   * nimmt den vorher geschickten lockrequest wieder zurück. ähnlich wie {@link #releaseLock(long)}, aber kann auch
   * ausgeführt werden, wenn das lock nicht erfolgreich geholt wurde.
   */
  public void recallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                                boolean countedInLockCircle, int lockCircleSize) throws ObjectNotInCacheException;


  /**
   * wartet, bis das remote lock erlangt und freigegeben wurde, welches die übergebene Priorität hatte.
   */
  public LockAwaitResponse awaitLock(long objectId, long priorityToWaitUpon, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException;


  /**
   * gibt metadaten+id etc zurück
   */
  public InitialConnectionData connectToClusterRemotely(NodeInformation nodeInformation);


  /**
   * wartet, bis alle threads des angegebenen typs beendet sind.
   */
  public void waitForActiveThreads(ThreadType type);

}
