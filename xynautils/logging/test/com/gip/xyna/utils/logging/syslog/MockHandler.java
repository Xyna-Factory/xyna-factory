/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.utils.logging.syslog;

import java.net.SocketException;
import java.util.Vector;

/**
 * 
 */
public class MockHandler extends SyslogHandler {

   private Vector<String> logEntries;

   /**
    * @param host
    * @param port
    * @throws SocketException
    */
   public MockHandler(String host, int port) throws SocketException {
      super(host, port);
      logEntries = new Vector<String>();
   }

   protected void sendMessage(String message) {
      logEntries.add(message);
   }

   public String[] getLogEntries() {
      return logEntries.toArray(new String[0]);
   }

}
