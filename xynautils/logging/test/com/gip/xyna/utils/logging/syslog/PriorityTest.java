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

import junit.framework.TestCase;

/**
 * 
 */
public class PriorityTest extends TestCase {

   public void testGetFacility() {
      int priority = Priority.calculatePriority(Facility.ALERT, Severity.ERROR);
      Facility facility = Priority.calculateFacility(priority);
      assertEquals("calculateFacility", Facility.ALERT, facility);
      priority = Priority.calculatePriority(Facility.DAEMON, Severity.NOTICE);
      facility = Priority.calculateFacility(priority);
      assertEquals("calculateFacility", Facility.DAEMON, facility);
   }

   public void testGetFacility_wrongCode() {
      Facility facility = Priority.calculateFacility(-1);
      assertNull("Code too small", facility);
      facility = Priority.calculateFacility(Integer.MAX_VALUE);
      assertNull("Code to big", facility);
   }

   public void testGetSeverity() {
      int priority = Priority.calculatePriority(Facility.AUDIT, Severity.ERROR);
      Severity severity = Priority.calculateSeverity(priority);
      assertEquals("calculateSeverity", Severity.ERROR, severity);
      priority = Priority.calculatePriority(Facility.CLOCK, Severity.NOTICE);
      severity = Priority.calculateSeverity(priority);
      assertEquals("calculateSeverity", Severity.NOTICE, severity);
   }

   public void testGetSeverity_wrongCode() {
      Severity severity = Priority.calculateSeverity(-1);
      assertNull("Code too small", severity);
      severity = Priority.calculateSeverity(Integer.MAX_VALUE);
      assertNull("Code to big", severity);
   }

   public void testCalculatePriority() {
      assertEquals(134, Priority.calculatePriority(Facility.LOCAL0,
            Severity.INFORMATIONAL));
      assertEquals(135, Priority.calculatePriority(Facility.LOCAL0,
            Severity.DEBUG));
      // TODO: expand test
      assertEquals(14, Priority.calculatePriority(Facility.USER,
            Severity.INFORMATIONAL));
      // assertEquals("", , Priority.calculatePriority(Facility., Severity.));
      // assertEquals("", , Priority.calculatePriority(Facility., Severity.));
      // assertEquals("", , Priority.calculatePriority(Facility., Severity.));
   }

}
