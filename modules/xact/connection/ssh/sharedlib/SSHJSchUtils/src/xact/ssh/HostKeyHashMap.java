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
package xact.ssh;



import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
//import java.util.List;
//import java.util.LinkedList;

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

  private static java.util.Map<String, Collection<HostKeyStorable>> HostKeyHash = java.util.Collections.synchronizedMap(new LinkedHashMap<String, Collection<HostKeyStorable>>() {

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Collection<HostKeyStorable>> eldest) {
      return size() > MAX_ENTRIES;
    }

  } );

  
  private static void clearHostKeyHash() {
    HostKeyHash.clear();
    logger.info("SSH App: HostKeyHash - Clear HashMap");
  }


  public static void putHostKeyCollection(String id, Collection<HostKeyStorable> hostKeyEntry) {
    try {
      HostKeyHash.put(id, hostKeyEntry);
    } catch(Exception e) {
      logger.trace("Error in putHostKeyCollection",e);
    }
  }


  private static boolean containsHostKeyCollection(String id) {
    return HostKeyHash.containsKey(id);
  }


  private static void removeHostKeyCollection(String id) {
    HostKeyHash.remove(id);
  }


  public static Collection<HostKeyStorable> getHostKeyCollection(String id) {
    try {
      Collection<HostKeyStorable> response = HostKeyHash.get(id);
      return response;
    } catch(Exception e) {
      //Collection<HostKeyStorable> response = Collections.emptyList();
      Collection<HostKeyStorable> response = new java.util.ArrayList<HostKeyStorable>();
      logger.trace("Error in getHostKeyCollection",e);
      return response;
    }
  }


  private static long sizeHostKeyHash() {
    return HostKeyHash.size();
  }

  public static long getNumberOfKeys(String id) {
    Collection<HostKeyStorable> tmpHostKeyStorable = HostKeyHashMap.getHostKeyCollection(id);
    if (!tmpHostKeyStorable.isEmpty()) {
      return tmpHostKeyStorable.size();
    } else {
      return 0;
    }
  }
  
}