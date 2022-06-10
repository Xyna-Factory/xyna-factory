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
package gip.base.common;

import java.util.logging.Filter;
import java.util.logging.LogRecord;


/**
 * LogSessionFilter
 * NICE noch nicht im Einsatz und auch noch nicht fertig implementiert.
 * Ueberlegung: die Logs pro Session zu filtern
 */
public class LogSessionFilter implements Filter {

  private String _sessionPattern;

  
  /**
   * @param con Connection
   */
  public LogSessionFilter(OBContextInterface con) {
    _sessionPattern=con.getSessionIdentifier();
  }

  
  /**
   * @see java.util.logging.Filter#isLoggable(java.util.logging.LogRecord)
   */
  public boolean isLoggable(LogRecord record) {
    if (record.getMessage().indexOf(_sessionPattern) > 0) {
      return true;
    } 
    else {
      return false;
    }
  }

  
}


