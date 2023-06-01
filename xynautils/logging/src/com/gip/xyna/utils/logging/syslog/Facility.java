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
 * Enumeration for syslog facilities.
 * <p>
 * A facility is a numerical indicator of sender component/application that sent
 * the log message.
 * 
 * 
 */
@Deprecated
public enum Facility {

   /**
    * Kernel messages
    */
   KERNEL(0),
   /**
    * User-level messages
    */
   USER(1),
   /**
    * Mail system
    */
   MAIL(2),
   /**
    * System daemons
    */
   DAEMON(3),
   /**
    * Security/Authorization messages
    */
   AUTH(4),
   /**
    * Messages generated internally by syslog
    */
   SYSLOG(5),
   /**
    * Line printer subsystem
    */
   LPR(6),
   /**
    * network news subsystem
    */
   NEWS(7),
   /**
    * UUCP subsystem
    */
   UUCP(8),
   /**
    * Clock daemon
    */
   CLOCK(9),
   /**
    * Security/Authorization messages
    */
   SECURITY(10),
   /**
    * FTP daemon
    */
   FTP(11),
   /**
    * NTP subsystem
    */
   NTP(12),
   /**
    * Log audit
    */
   AUDIT(13),
   /**
    * Log alert
    */
   ALERT(14),
   /**
    * CRON daemon
    */
   CRON(15),
   /**
    * Local use 0
    */
   LOCAL0(16),
   /**
    * Local use 1
    */
   LOCAL1(17),
   /**
    * Local use 2
    */
   LOCAL2(18),
   /**
    * Local use 3
    */
   LOCAL3(19),
   /**
    * Local use 4
    */
   LOCAL4(20),
   /**
    * Local use 5
    */
   LOCAL5(21),
   /**
    * Local use 6
    */
   LOCAL6(22),
   /**
    * Local use 7
    */
   LOCAL7(23);

   private int value;

   private Facility(int value) {
      this.value = value;
   }

   /**
    * Get the numerical value of the facility.
    * 
    * @return numerical value
    */
   public int getValue() {
      return value;
   }

   /**
    * Get facility by its numerical value.
    * 
    * @param code
    *              the numerical value of the facility
    * @return the facility matching the input value
    */
   public static Facility getFacility(int code) {
      switch (code) {
      case 0:
         return KERNEL;
      case 1:
         return USER;
      case 2:
         return MAIL;
      case 3:
         return DAEMON;
      case 4:
         return AUTH;
      case 5:
         return SYSLOG;
      case 6:
         return LPR;
      case 7:
         return NEWS;
      case 8:
         return UUCP;
      case 9:
         return CLOCK;
      case 10:
         return SECURITY;
      case 11:
         return FTP;
      case 12:
         return NTP;
      case 13:
         return AUDIT;
      case 14:
         return ALERT;
      case 15:
         return CRON;
      case 16:
         return LOCAL0;
      case 17:
         return LOCAL1;
      case 18:
         return LOCAL2;
      case 19:
         return LOCAL3;
      case 20:
         return LOCAL4;
      case 21:
         return LOCAL5;
      case 22:
         return LOCAL6;
      case 23:
         return LOCAL7;
      }
      return null;
   }

}
