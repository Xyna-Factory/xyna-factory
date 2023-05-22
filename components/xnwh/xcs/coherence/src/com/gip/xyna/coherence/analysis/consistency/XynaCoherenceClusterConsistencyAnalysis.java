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

package com.gip.xyna.coherence.analysis.consistency;


public interface XynaCoherenceClusterConsistencyAnalysis {

  /**
   * Checks the global consistency for a single object. Note that this requires globally freezing all activity on the
   * cluster.
   */
  // TODO Is a global lock really required? The idea is that a lock is required and a "lock" would change the state,
  // especially a "lock" would never succeed if the object is locked at that time.
  public ConsistencyCheckResult checkGlobalConsistency(long objectID);


  /**
   * Checks the global consistency for all objects within the pool.
   */
  public ConsistencyCheckResult checkGlobalConsistencyForAllObjects();

}
