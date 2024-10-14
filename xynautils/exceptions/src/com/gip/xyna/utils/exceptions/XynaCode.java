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
package com.gip.xyna.utils.exceptions;

/**
 * interne fehlercodes
 * 
 */
public enum XynaCode {

   /**
    * 
    */
   UNKNOWN("XYNA-99999", "Dieser Fehler hat keine Fehlernachricht erzeugt"),
   /**
    * Fehlercode ist schon vorhanden.
    */
   DUPLICATE_CODE(
         "XYNA-00001",
         "Es wurde bereits eine Fehlernachricht zum Fehlercode %%0 in der Sprache %%1 gecacht."),
   /**
    * Zu viele Parameter für die Fehlernachricht.
    */
   TOO_MANY_PARAS("XYNA-00002",
         "Es wurden zuviele Parameter übergeben (max 10)"),
   /**
    * Keine Nachricht für den Fehlercode gespeichert.
    */
   NO_MSG_FOR_CODE("XYNA-00003",
         "Es wurde keine Fehlernachricht zum Fehlercode %%0 gefunden.");

   private String code;
   private String message;

   private XynaCode(String code, String message) {
      this.code = code;
      this.message = message;
   }

   public String getCode() {
      return code;
   }

   public String getMessage() {
      return message;
   }

}
