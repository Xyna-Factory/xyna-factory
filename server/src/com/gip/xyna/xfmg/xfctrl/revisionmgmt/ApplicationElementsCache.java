/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.revisionmgmt;



import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem.Collision.RuntimeContextCollisionType;



public class ApplicationElementsCache {

  private long hits;
  private long misses;

  private Map<Key, Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>>> singleRevCache;
  private Map<Key, Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>>> hierarchvRevCache;


  public ApplicationElementsCache() {
    singleRevCache = new HashMap<>();
    hierarchvRevCache = new HashMap<>();
  }


  public Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> getSingleRev(RuntimeContextCollisionType rcct,
                                                                              RuntimeDependencyContext rtc) {
    return singleRevCache.get(new Key(rcct, rtc));
  }
  

  public Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> getHierarchyRev(RuntimeContextCollisionType rcct,
                                                                                          RuntimeDependencyContext rtc) {
    return hierarchvRevCache.get(new Key(rcct, rtc));
  }


  public long getHits() {
    return hits;
  }


  public long getMisses() {
    return misses;
  }


  public void putSingleRev(RuntimeContextCollisionType rcct, RuntimeDependencyContext req,
                  Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> elements) {
    singleRevCache.put(new Key(rcct, req), elements);
  }
  
  public void putHierarchyRev(RuntimeContextCollisionType rcct, RuntimeDependencyContext req,
                           Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> elements) {
             hierarchvRevCache.put(new Key(rcct, req), elements);
           }
  
  
  @Override
  public String toString() {
    return String.format("[hits: %d, misses %d]", hits, misses);
  }


  private static class Key {

    private final RuntimeContextCollisionType rcct;
    private final RuntimeDependencyContext rtc;
    private final int hash;


    Key(RuntimeContextCollisionType rcct, RuntimeDependencyContext rtc) {
      this.rcct = rcct;
      this.rtc = rtc;
      this.hash = 31 * rcct.hashCode() + rtc.hashCode();
    }


    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof Key))
        return false;
      Key k = (Key) o;
      return rcct.equals(k.rcct) && rtc.equals(k.rtc);
    }


    @Override
    public int hashCode() {
      return hash;
    }
  }
}
