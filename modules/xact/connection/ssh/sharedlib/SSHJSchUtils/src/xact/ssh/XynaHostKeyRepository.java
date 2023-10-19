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

import net.schmizz.sshj.transport.verification.HostKeyVerifier;

public interface XynaHostKeyRepository extends HostKeyVerifier {

  public void exportKnownHost(String hostname, String type, String filenameKnownHosts);
  
  public void importKnownHosts(String filenameKnownHosts);
  
  public void add(HostKeyStorable hostkey);
  
  public boolean remove(String host, String type);
  
  public void init();
  
  public void shutdown();
  
}
