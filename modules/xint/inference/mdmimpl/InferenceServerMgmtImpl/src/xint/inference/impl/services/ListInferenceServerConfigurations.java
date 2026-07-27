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
package xint.inference.impl.services;



import java.util.List;
import java.util.Objects;
import xint.inference.InferenceServer;
import xint.inference.InferenceServerConfiguration;
import xint.inference.Model;
import xint.inference.impl.ProcessInfo;
import xint.inference.impl.ProcessInteraction;
import xint.inference.impl.storage.InferenceServerConfigurationStorage;
import xint.inference.impl.storage.InferenceServerList;
import xint.inference.impl.supportedservers.InferenceServerManagement;
import xint.inference.impl.supportedservers.InferenceServerManagementRegistry;



public class ListInferenceServerConfigurations {

  private final InferenceServerConfigurationStorage serverConfigStorage;


  public ListInferenceServerConfigurations(InferenceServerConfigurationStorage serverConfigStorage) {
    this.serverConfigStorage = serverConfigStorage;
  }


  public List<? extends InferenceServerConfiguration> listInferenceServerConfigurations() {
    List<InferenceServerConfiguration> result = serverConfigStorage.loadAllEntries();
    List<ProcessInfo> processes = ProcessInteraction.listProcesses();

    for (InferenceServerConfiguration item : result) {
      updateConfigStatus(item, processes);
    }
    return result;
  }


  private void updateConfigStatus(InferenceServerConfiguration item, List<ProcessInfo> processes) {
    String serverType = item.getServerType();
    String serverVersion = item.getServerVersion();
    String model = item.getModel();
    String state;
    Long pid = null;
    InferenceServerManagement mgmt = InferenceServerManagementRegistry.getInstance().getServerMgmt(serverType);
    if (mgmt == null) {
      state = "invalid";
    } else {
      pid = mgmt.getPid(item, processes);
      state = determineState(pid, serverType, serverVersion, model);
    }
    item.unversionedSetPid(pid);
    item.unversionedSetState(state);
  }


  private String determineState(Long pid, String serverType, String serverVersion, String model) {
    if (pid != null) {
      return "online";
    }
    List<InferenceServer> servers = InferenceServerList.getInstance().getServers(false);
    for (InferenceServer server : servers) {
      if (Objects.equals(server.getType(), serverType) && Objects.equals(server.getServerVersion(), serverVersion)) {
        List<Model> models = new ListModels().listModels();
        boolean hasModelSet = model != null && !model.isBlank();
        if (!hasModelSet || models.stream().filter(x -> Objects.equals(model, x.getName())).findFirst().isPresent()) {
          return "offline";
        }
      }
    }
    return "invalid";
  }
}
