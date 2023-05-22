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
package com.gip.xyna.coherence.coherencemachine.interconnect;



import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.gip.xyna.coherence.coherencemachine.CoherenceObject;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject;



public class InitialConnectionData implements Serializable {

  private static final long serialVersionUID = 1L;
  private int id;
  private List<CoherenceObject> metadata;
  private Map<Long, LockObject> lockdata;

  public int getId() {
    return id;
  }


  public void setId(int id) {
    this.id = id;
  }


  public List<CoherenceObject> getMetadata() {
    return metadata;
  }


  public void setMetaData(List<CoherenceObject> metadata) {
    this.metadata = metadata;
  }


  public void setLockData(Map<Long, LockObject> lockdata) {
    this.lockdata = lockdata;
  }
  
  public Map<Long, LockObject> getLockdata() {
    return lockdata;
  }


}
