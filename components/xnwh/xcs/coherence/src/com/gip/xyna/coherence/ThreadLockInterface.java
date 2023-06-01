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

import com.gip.xyna.coherence.management.ClusterMember;


public interface ThreadLockInterface {

  static final long THREADCNT_SLEEP_INTERVAL = 30;
  void unlockLocally();

  void lockLocally();

  void waitForThreads();

  void checkLock();

  void decrementThreadCount();

  boolean isLocked();

  void lockAll();

  void unlockAll(ClusterMember[] members);
}
