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
package xact.ssh;



import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



public class HostKeyHashMap {

  private final static Logger logger = CentralFactoryLogging.getLogger(HostKeyHashMap.class);

  /*
   * Remarks - HostKeyHashMap:
   * Init (put) from createSession (SSHConnectionInstanceOperationImpl) in injectHostKey (HostKeyStorableRepository (SharedLib))
   * Used (get) in HostKeyStorableRepository (SharedLib) by verify (sshj intern)
   */

  // Assumption: Maximum of 1000 "SSHConnectionInstanceOperationImpl - createSession" calls per ACS at the same time. If necessary, please adjust.
  private final static int MAX_ENTRIES = 1000; //Default: 1000;

  private static Map<String, Collection<HostKeyStorable>> HostKeyHash = Collections.synchronizedMap(new LinkedHashMap<>() {

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Collection<HostKeyStorable>> eldest) {
      return size() > MAX_ENTRIES;
    }

  } );


  public static void putHostKeyCollection(String id, Collection<HostKeyStorable> hostKeyEntry) {
    try {
      HostKeyHash.put(id, hostKeyEntry);
    } catch(Exception e) {
      logger.trace("Error in putHostKeyCollection",e);
    }
  }


  public static Collection<HostKeyStorable> getHostKeyCollection(String id) {
    try {
      Collection<HostKeyStorable> response = HostKeyHash.get(id);
      if (response != null) {
        return response;
      }
    } catch(Exception e) {
      logger.trace("Error in getHostKeyCollection", e);
    }
    return Collections.emptyList();
  }


  public static long getNumberOfKeys(String id) {
    return HostKeyHashMap.getHostKeyCollection(id).size();
  }
  
}