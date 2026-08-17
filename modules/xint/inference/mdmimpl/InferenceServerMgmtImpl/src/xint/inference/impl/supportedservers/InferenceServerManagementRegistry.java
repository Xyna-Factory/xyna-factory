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
package xint.inference.impl.supportedservers;



import java.util.Collection;
import java.util.Map;



public class InferenceServerManagementRegistry {

  private final Map<String, InferenceServerManagement> serverMgmtMap;
  private static InferenceServerManagementRegistry instance;


  public static InferenceServerManagementRegistry getInstance() {
    if (instance == null) {
      synchronized (InferenceServerManagement.class) {
        if (instance != null) {
          return instance;
        }
        instance = new InferenceServerManagementRegistry();
      }
    }
    return instance;
  }


  private InferenceServerManagementRegistry() {
    LlamaCppServerManagement llamacppMgmt = new LlamaCppServerManagement();
    serverMgmtMap = Map.of(llamacppMgmt.serverType(), llamacppMgmt);
  }


  public InferenceServerManagement getServerMgmt(String serverType) {
    return serverMgmtMap.get(serverType);
  }


  public Collection<InferenceServerManagement> getAllServerMgmt() {
    return serverMgmtMap.values();
  }


  public Collection<String> getSupportedServerTypes() {
    return serverMgmtMap.keySet();
  }
}
