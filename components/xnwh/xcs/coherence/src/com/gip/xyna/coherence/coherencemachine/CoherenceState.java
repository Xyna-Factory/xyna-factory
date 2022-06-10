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
package com.gip.xyna.coherence.coherencemachine;



/**
 * Status according to the MESI protocol. Due to the fact that no common repository is used there is no real different
 * between modified and exclusive.
 */
public enum CoherenceState {

  /**
   * Object modified on local node. All other copies, including the one in the potential common repository, are invalid.
   */
  MODIFIED,
  /**
   * Local node holds only copy. Copy in potential common repository is identical. All other nodes have invalid as state
   * of the object.
   */
  EXCLUSIVE,
  /**
   * At least two nodes holds copies of the object. All copies are identical to the one in the potential common
   * repository. All other nodes have invalid as state of the object.
   */
  SHARED,
  /**
   * Object content is not known on the local node.
   */
  INVALID

}
