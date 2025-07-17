/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package com.gip.xyna.xact.trigger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


class ConnectionQueue {

  private static BlockingQueue<NetConfNotificationReceiverTriggerConnection> requests = new LinkedBlockingQueue<>();


  public static void push(NetConfNotificationReceiverTriggerConnection conn) {
    requests.add(conn);
  };


  public static boolean isEmpty() {
    return requests.isEmpty();
  };


  public static NetConfNotificationReceiverTriggerConnection get() {
    return requests.poll();
  };


  public static int size() {
    return requests.size();
  }
  
}
