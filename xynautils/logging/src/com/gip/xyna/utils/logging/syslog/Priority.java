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

/**
 * Class representing the sylog priority.
 * <p>
 * The syslog priority is calculated by: Facility * 8 + Severity.
 * 
 * 
 */
@Deprecated
public class Priority {

   private static final int MAX_VALUE = 191;

   /**
    * Calculate the syslog priority depend on the given facility and severity.
    * 
    * @param facility
    *              a syslog facility
    * @param severity
    *              a syslog severity
    * @return the syslog priority calculated from the facility and the severity
    */
   public static int calculatePriority(Facility facility, Severity severity) {
      return facility.getValue() * 8 + severity.getValue();
   }

   /**
    * Get the facility to a given priority.
    * 
    * @param priority
    *              a syslog priority
    * @return the facility used to calculate the given priority
    */
   public static Facility calculateFacility(int priority) {
      if ((priority < 0) || (priority > MAX_VALUE)) {
         return null;
      }
      return Facility.getFacility(priority / 8);
   }

   /**
    * Get the severity to a given priority.
    * 
    * @param priority
    *              a sylog priority
    * @return the severity used to calculate the given priority
    */
   public static Severity calculateSeverity(int priority) {
      if ((priority < 0) || (priority > MAX_VALUE)) {
         return null;
      }
      return Severity.getSeverity(priority % 8);
   }

}
