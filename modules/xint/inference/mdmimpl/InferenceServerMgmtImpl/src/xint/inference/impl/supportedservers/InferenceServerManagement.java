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



import java.nio.file.Path;
import java.util.List;

import xint.inference.InferenceServer;
import xint.inference.InferenceServerConfiguration;
import xint.inference.impl.ProcessInfo;



public interface InferenceServerManagement {

  String serverType();


  Long getPid(InferenceServerConfiguration serverConfig, List<ProcessInfo> processes);


  List<InferenceServer> listServers();


  boolean start(long requestId, InferenceServerConfiguration serverConfig);


  boolean stop(long requestId, InferenceServerConfiguration serverConfig);


  boolean deleteServer(InferenceServer server);


  boolean handleDownload(Path path);
}
