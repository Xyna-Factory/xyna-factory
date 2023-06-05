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

import com.gip.queue.utils.exception.QueueException;


public class CachedQueueConnection {

  private final QueueConnectionBuilder _connBuilder;
  private ThreadManagedQueueConnection _currentConn;


  public CachedQueueConnection(QueueConnectionBuilder connBuilder) {
    _connBuilder = connBuilder;
  }


  public synchronized void send(MsgToSend msg, int timeoutSec) throws QueueException {
    openIfNecessary();
    _currentConn.send(msg, timeoutSec);
  }

  public synchronized void close() {
    _currentConn.close();
  }

  private void openIfNecessary() throws QueueException {
    if (_currentConn == null) {
      open();
    }
    else if (!_currentConn.IsOpen()) {
      open();
    }
  }

  private void open() throws QueueException {
    _currentConn = new ThreadManagedQueueConnection(_connBuilder);
  }

}
