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

package com.gip.xyna.coherence.remote;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;


public class RemoteLockUnlockRequest extends RemoteRunnable {

  private static final long serialVersionUID = 5017136188588023292L;
  private long objectId;


  public void setObjectId(long objectId) {
    this.objectId = objectId;
  }


  @Override
  public void run(CacheController targetController) {
    try {
      int i = 0;
      while (i < 100) {
        i++;
        targetController.lock(objectId);
        targetController.unlock(objectId);
      }
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }
  }

}
