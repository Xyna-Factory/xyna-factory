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

import xint.inference.InferenceManagementActionFailedException;
import xint.inference.InferenceServerConfiguration;
import xint.inference.impl.ProcessInfo;
import xint.inference.impl.ProcessInteraction;
import xint.inference.impl.storage.InferenceServerConfigurationStorage;
import xint.inference.impl.storage.InferenceServerManagementRequestHistoryStorage;
import xint.inference.impl.supportedservers.InferenceServerManagement;
import xint.inference.impl.supportedservers.InferenceServerManagementRegistry;



public class DeleteInferenceServerConfiguration {

  public void deleteInferenceServerConfiguration(long requestId, InferenceServerConfiguration entry)
      throws InferenceManagementActionFailedException {
    InferenceServerConfigurationStorage serverConfigStorage = new InferenceServerConfigurationStorage();
    InferenceServerManagementRequestHistoryStorage requestHistoryStorage = new InferenceServerManagementRequestHistoryStorage();

    InferenceServerManagement mgmt = InferenceServerManagementRegistry.getInstance().getServerMgmt(entry.getServerType());
    if (mgmt == null) {
      String desc = String.format("Unknown server type %s. Check for running process skipped!", entry.getServerType());
      requestHistoryStorage.persistEntry(requestId, desc);
    } else {
      List<ProcessInfo> processes = ProcessInteraction.listProcesses();
      Long pid = mgmt.getPid(entry, processes);
      if (pid != null) {
        boolean stopped = mgmt.stop(requestId, entry);
        if (!stopped) {
          String desc = String.format("Failed to stop Inference Server Config: %s for deletion", entry.getId());
          requestHistoryStorage.persistEntry(requestId, desc);
          throw new InferenceManagementActionFailedException("Delete Inference Server", requestId);
        }
      }
    }


    try {
      serverConfigStorage.deleteEntry(entry);
      String desc = String.format("Deleted Inference Server Config: %s", entry.getId());
      requestHistoryStorage.persistEntry(requestId, desc);
    } catch (Exception e) {
      String desc = String.format("Failed to delete Inference Server Config: %s", entry.getId());
      requestHistoryStorage.persistEntry(requestId, desc);
      throw new InferenceManagementActionFailedException("Delete Inference Server", requestId, e);
    }
  }
}
