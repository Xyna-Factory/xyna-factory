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
package xint.inference.impl.storage;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import xint.inference.InferenceServer;
import xint.inference.impl.supportedservers.InferenceServerManagement;
import xint.inference.impl.supportedservers.InferenceServerManagementRegistry;



public class InferenceServerList {

  private long lastRefreshTime;
  private long lifetime = 300_000;

  private List<InferenceServer> servers;

  private static InferenceServerList instance;


  public static InferenceServerList getInstance() {
    if (instance == null) {
      synchronized (InferenceServerList.class) {
        if (instance == null) {
          instance = new InferenceServerList();
        }
      }
    }
    return instance;
  }


  private InferenceServerList() {
    servers = new ArrayList<>();
    lastRefreshTime = -1l;
  }


  public List<InferenceServer> getServers(boolean forceRefresh) {
    if (forceRefresh || lastRefreshTime + lifetime < System.currentTimeMillis()) {
      servers = readServersFromDisk();
    }
    return servers;
  }


  private List<InferenceServer> readServersFromDisk() {
    List<InferenceServer> result = new ArrayList<>();
    Collection<InferenceServerManagement> mgmts = InferenceServerManagementRegistry.getInstance().getAllServerMgmt();
    for (InferenceServerManagement mgmt : mgmts) {
      result.addAll(mgmt.listServers());
    }
    return result;
  }
}
