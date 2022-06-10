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

import java.util.logging.Level;

import junit.framework.TestCase;

/**
 * 
 */
public class SeverityTest extends TestCase {

   public void testGetSeverity_int() {
      assertEquals(Severity.EMERGENCY, Severity.getSeverity(0));
      assertEquals(Severity.DEBUG, Severity.getSeverity(7));
      assertEquals(null, Severity.getSeverity(-1));
      assertEquals(null, Severity.getSeverity(8));
   }

   public void testGetSeverity_level_log() {
      assertEquals(Severity.ERROR, Severity.getSeverity(Level.SEVERE));
      assertEquals(Severity.WARNING, Severity.getSeverity(Level.WARNING));
      assertEquals(Severity.NOTICE, Severity.getSeverity(Level.INFO));
      assertEquals(Severity.INFORMATIONAL, Severity.getSeverity(Level.CONFIG));
      assertEquals(Severity.DEBUG, Severity.getSeverity(Level.FINE));
      assertEquals(Severity.DEBUG, Severity.getSeverity(Level.FINER));
      assertEquals(Severity.DEBUG, Severity.getSeverity(Level.FINEST));
   }

   public void testGetSeverity_level_syslog() {
      assertEquals(Severity.EMERGENCY, Severity.getSeverity(Severity.EMERGENCY
            .getLevel()));
      assertEquals(Severity.ALERT, Severity.getSeverity(Severity.ALERT
            .getLevel()));
      assertEquals(Severity.CRITICAL, Severity.getSeverity(Severity.CRITICAL
            .getLevel()));
      assertEquals(Severity.ERROR, Severity.getSeverity(Severity.ERROR
            .getLevel()));
      assertEquals(Severity.WARNING, Severity.getSeverity(Severity.WARNING
            .getLevel()));
      assertEquals(Severity.NOTICE, Severity.getSeverity(Severity.NOTICE
            .getLevel()));
      assertEquals(Severity.INFORMATIONAL, Severity
            .getSeverity(Severity.INFORMATIONAL.getLevel()));
      assertEquals(Severity.DEBUG, Severity.getSeverity(Severity.DEBUG
            .getLevel()));
   }

}
