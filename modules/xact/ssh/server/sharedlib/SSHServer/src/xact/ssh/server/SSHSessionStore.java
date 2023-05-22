/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package xact.ssh.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SSHSessionStore {

  private static Map<String, SSHConnection> connections = new ConcurrentHashMap<String, SSHConnection>();
  
  public interface SSHConnection {

    public void send(String msg) throws IOException;
   
    public void sendLine(String msg) throws IOException;
    
    public void sendPrompt() throws IOException;
    
    public String readLine() throws IOException;
    
    public String readLine(long timeout) throws IOException;
    
    public String readMultiLineNonBlocking() throws IOException;

    public String getHostKey();

    public String readAll(long absoluteTime) throws IOException;
    
  }
 
  public static void store(String sessionId, SSHConnection sshCon) {
    connections.put(sessionId, sshCon);
  }

  public static void remove(String sessionId) {
    connections.remove(sessionId);
  }

  public static SSHConnection getSSHConnection(String sessionId) {
    return connections.get(sessionId);
  }

}
