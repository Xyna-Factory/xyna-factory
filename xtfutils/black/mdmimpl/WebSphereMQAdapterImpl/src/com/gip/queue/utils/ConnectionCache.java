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

package com.gip.queue.utils;

import java.util.HashMap;
import java.util.Map;


public class ConnectionCache {

  private Map<String, CachedQueueConnection> _map = new HashMap<String, CachedQueueConnection>();


  public synchronized CachedQueueConnection get(String key) {
    return _map.get(key);
  }

  public synchronized CachedQueueConnection add(String key, QueueConnectionBuilder builder) {
    CachedQueueConnection conn = new CachedQueueConnection(builder);
    _map.put(key, conn);
    return conn;
  }

  public synchronized void closeAll() {
    for (CachedQueueConnection conn : _map.values()) {
      conn.close();
    }
    _map.clear();
  }

}
