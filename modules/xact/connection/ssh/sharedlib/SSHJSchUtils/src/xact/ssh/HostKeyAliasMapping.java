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



import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



public class HostKeyAliasMapping {

  private final static Logger logger = CentralFactoryLogging.getLogger(HostKeyAliasMapping.class);

  /*
   * Remarks - HostKeyAliasMapping:
   * Init in SSHConnectionInstanceOperationImpl - createSession.
   * Used in HostKeyStorableRepository (SharedLib): verify/verifyIntern (sshj intern - reason for LinkedHashMap), findExistingAlgorithms (authenticate).
   * Called in SSHConnectionInstanceOperationImpl - createSession.
   */

  // Assumption: Maximum of 10000 "SSHConnectionInstanceOperationImpl - createSession" calls per ACS at the same time. If necessary, please adjust.
  private final static int MAX_ENTRIES = 10000; //Default: 10000;

  private static java.util.Map<String, AliasEntry> Alias = java.util.Collections.synchronizedMap(new LinkedHashMap<String, AliasEntry>() {

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, AliasEntry> eldest) {
      return size() > MAX_ENTRIES;
    }

  } );


  private static List<String> listAlias() {
    List<String> list = new LinkedList<String>();
    Alias.forEach((id, object) -> list.add(id));
    return list;
  }


  private static void clearAlias() {
    Alias.clear();
    logger.info("SSH App: HostKeyAliasMapping - Clear HashMap");
  }


  private static void putAlias(String id, AliasEntry aliasEntry) {
    try {
      Alias.put(id, aliasEntry);
    } catch(Exception e) {
      logger.trace("Error in putAlias",e);
    }
  }


  private static boolean containsAlias(String id) {
    try {
      boolean response = Alias.containsKey(id);
      return response;
    } catch(Exception e) {
      logger.trace("Error in containsAlias",e);
      return false;
    }
  }


  private static void removeAlias(String id) {
    try {
      Alias.remove(id);
    } catch(Exception e) {
      logger.trace("Error in removeAlias",e);
    }
  }


  private static AliasEntry getAlias(String id, String fallback_hostname, boolean fallback_persist) {
    try {
      AliasEntry response = Alias.get(id);
      return response;
    } catch(Exception e) {
      AliasEntry response = new AliasEntry(fallback_hostname, fallback_persist);
      logger.trace("Error in getAlias",e);
      return response;
    }
  }


  private static long sizeAlias() {
    return Alias.size();
  }


  public static void injectHostname(String hostname, String hostnamealias, boolean overwrite) {
    if ((hostnamealias != null) && (!hostnamealias.isEmpty())) {
      AliasEntry aliasEntry = new AliasEntry(hostnamealias, overwrite);
      putAlias(hostname, aliasEntry);
    } else {
      AliasEntry aliasEntry = new AliasEntry(hostname, overwrite);
      putAlias(hostname, aliasEntry);
    }
  }


  public static void releaseHostname(String hostname) {
    removeAlias(hostname);
  }


  public static String convertHostname(String hostname) {
    if (containsAlias(hostname)) {
      AliasEntry alias = getAlias(hostname,hostname,false);
      String reply = alias.getAlias();
      return reply;
    } else {
      return hostname;
    }
  }


  public static boolean persist(String hostname) {
    if (containsAlias(hostname)) {
      AliasEntry alias = getAlias(hostname,hostname,false);
      boolean reply = alias.getOverwrite();
      return reply;
    } else {
      return false;
    }
  }

}