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

package com.gip.xyna.coherence;



import com.gip.xyna.coherence.CacheControllerImpl.GlobalLockPayload;



interface InternalStaticObjectIDs {


  /**
   * shared objekt, dass die liste aller knoten im cluster hält.
   */
  public static final long ID_IDGEN = 0;

  /**
   * Shared object holding cluster member information
   */
  public static final long ID_CLUSTER_MEMBERS = 1;

  /**
   * shared object mit payload {@link GlobalLockPayload}, über dessen update-trigger an andere knoten die information
   * verbreitet wird, dass das globale lock gesetzt werden muss
   * @see {@link #globalLock}
   */
  static final long ID_GLOBAL_LOCK = 2;

  /**
   * shared object mit payload {@link GlobalLockPayload}, über dessen update-trigger an andere knoten die information
   * verbreitet wird, dass das globale unlock-lock gesetzt werden muss
   * @see {@link #globalUnlockLock}
   */
  static final long ID_GLOBAL_UNLOCK_LOCK = 3;

  /**
   * shared object, welches das verteilte (globale) lock realisiert, an dem alle lokalen threads warten, die kein unlock
   * sind. wird bei {@link #pauseCluster()} gelockt.
   * @see {@link ThreadLock#checkLock()}
   * @see {@link #globalLock}
   */
  static final long ID_GLOBAL_LOCK_T = 4;

  /**
   * shared object, welches das verteilte (globale) lock realisiert, an dem alle lokalen threads warten, die ein unlock
   * sind. wird bei {@link #pauseCluster()} gelockt.
   * @see {@link ThreadLock#checkLock()}
   * @see {@link #globalUnlockLock}
   */
  static final long ID_GLOBAL_UNLOCK_LOCK_T = 5;

  /**
   * shared object mit payload {@link GlobalLockPayload}, über dessen update-trigger an andere knoten die information
   * verbreitet wird, dass das globale special-lock gesetzt werden muss
   * @see {@link #specialLock}
   */
  static final long ID_SPECIAL = 6;


  /**
   * shared object, welches gelockt wird, wenn ein objekt mit einer von außen vorgegebenen ID erstellt werden soll.
   */
  static final long ID_GLOBAL_STATIC_OBJECT_CREATION_LOCK = 7;

  /**
   * Maximale fuer interne Zwecke vorbelegte id
   */
  public static final long ID_MAXIMUM_INTERNALLY_USED = ID_GLOBAL_STATIC_OBJECT_CREATION_LOCK;

}
