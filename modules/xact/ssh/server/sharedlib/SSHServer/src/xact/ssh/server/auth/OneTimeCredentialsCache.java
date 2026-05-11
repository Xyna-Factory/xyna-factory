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

package xact.ssh.server.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class OneTimeCredentialsCache {
  
  private Queue<String> userQueue;
  private Map<String, UserData> credentialsCache;


  public OneTimeCredentialsCache(int bound) {
    userQueue = new LinkedBlockingQueue<String>(bound);
    credentialsCache = new HashMap<String, UserData>();
  }

  
  public synchronized boolean add(UserData ud) {
    if (credentialsCache.containsKey(ud.getUser())) {
      return false;
    }
    while (!userQueue.offer(ud.getUser())) {
      String userToRemove = userQueue.poll();
      credentialsCache.remove(userToRemove);
    }
    credentialsCache.put(ud.getUser(), ud);
    return true;
  }

  
  public UserData get(String username) {
    return credentialsCache.get(username);
  }

  
  public synchronized UserData remove(String username) {
    userQueue.remove(username);
    return credentialsCache.remove(username);
  }


}
