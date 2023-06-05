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

package com.gip.xyna.coherence.management;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.coherence.coherencemachine.CoherenceObject;


/**
 * 1. statistiken über alle objekte
 *    oder
 * 2. statistiken + liste aller objekte
 */
public class ObjectPoolInformation {

  public Map<Long, ObjectInfo> info;
  
  public static class ObjectInfo {
    public String state;
    public String payloadClassName;
  }
  
  
  public ObjectPoolInformation(List<CoherenceObject> snapshot) {
    info = new HashMap<Long, ObjectInfo>();
    for (CoherenceObject o : snapshot) {
      ObjectInfo objectInfo = new ObjectInfo();
      objectInfo.state = o.getState().toString();
      if (o.getPayload() != null) {
        objectInfo.payloadClassName = o.getPayload().getClass().getName();
      } else {
        objectInfo.payloadClassName = "unknown";
      }
      info.put(o.getId(), objectInfo);
    }
  }
  
  public Map<Long, ObjectInfo> getInfoForAllObjects() {
    return info;
  }
  
}
