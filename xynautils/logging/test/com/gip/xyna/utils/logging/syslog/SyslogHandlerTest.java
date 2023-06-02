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
package com.gip.xyna.utils.logging.syslog;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Handler;
import java.util.logging.Logger;

import junit.framework.TestCase;

/**
 * 
 */
public class SyslogHandlerTest extends TestCase {

   private static final String HOST = "gipsun173.gip.local";

   // FIXME: check result
   // should print message in oracle.log
   public void testSyslogHandler() throws SocketException, UnknownHostException {
      Logger logger = Logger.getLogger("syslog");
      Handler h = new SyslogHandler(HOST, SysLogger.DEFAULT_PORT);
      h.setFormatter(new SyslogFormatter(Facility.LOCAL0));
      logger.addHandler(h);
      logger.info("SysHandlerTest");
   }

   // TODO: testSyslogHandler_multipleLineMessage
   // TODO: testSyslogHandler_normalFormatter

}
