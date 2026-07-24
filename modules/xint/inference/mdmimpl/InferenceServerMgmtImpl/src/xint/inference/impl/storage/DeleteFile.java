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



import java.io.File;

import xint.inference.InferenceManagementActionFailedException;
import xint.inference.InferenceServer;
import xint.inference.Model;
import xint.inference.impl.InferenceServerMgmtServiceOperationImpl;
import xint.inference.impl.supportedservers.InferenceServerManagement;
import xint.inference.impl.supportedservers.InferenceServerManagementRegistry;



public class DeleteFile {


  public void delete(long requestId, InferenceServer server) throws InferenceManagementActionFailedException {
    InferenceServerManagement mgmt = InferenceServerManagementRegistry.getInstance().getServerMgmt(server.getType());
    InferenceServerManagementRequestHistoryStorage historyStorage = new InferenceServerManagementRequestHistoryStorage();
    if (mgmt == null) {
      String desc = String.format("Failed to delete inference server %s. Unknown type %s", server.getId(), server.getType());
      historyStorage.persistEntry(requestId, desc);
      throw new InferenceManagementActionFailedException("Delete Inference Server", requestId);
    }
    if (mgmt.deleteServer(server)) {
      String desc = String.format("Successfully deleted inference server %s", server.getId());
      historyStorage.persistEntry(requestId, desc);
    } else {
      String desc = String.format("Failed to delete inference server %s", server.getId());
      historyStorage.persistEntry(requestId, desc);
      throw new InferenceManagementActionFailedException("Delete Inference Server", requestId);
    }
  }


  public void delete(long requestId, Model model) throws InferenceManagementActionFailedException {
    InferenceServerManagementRequestHistoryStorage historyStorage = new InferenceServerManagementRequestHistoryStorage();
    String modelPath = InferenceServerMgmtServiceOperationImpl.MODEL_PATH.get();
    File toDelete = new File(modelPath, model.getName());
    boolean success = toDelete.delete();
    if (success) {
      String desc = String.format("Successfully deleted model %s", model.getName());
      historyStorage.persistEntry(requestId, desc);
    } else {
      String desc = String.format("Failed to delete model %s", model.getName());
      historyStorage.persistEntry(requestId, desc);
      throw new InferenceManagementActionFailedException("Delete Model", requestId);
    }
  }

}
