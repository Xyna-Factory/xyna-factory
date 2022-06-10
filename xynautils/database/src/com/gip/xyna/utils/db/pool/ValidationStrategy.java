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
package com.gip.xyna.utils.db.pool;

import java.sql.Connection;

public interface ValidationStrategy {
  
  /**
   * Ist Validierung erforderlich?
   * @param currentTime
   * @param lastcheck
   * @return
   */
  public boolean isValidationNecessary(long currentTime, long lastcheck);

  /**
   * Validieren der Connection
   * @param con
   * @return null, falls Validierung erfolgreich, ansonsten Validierungsfehler
   */
  public Exception validate(Connection con);
  
  /**
   * in millisekunden: nach welcher zeit der nicht-benutzung im pool wird eine connection erneut überprüft, ob sie noch gültig ist.
   * falls <=0 : überprüfung jedes mal
   * 
   */
  public void setValidationInterval(long validationInterval);

  public long getValidationInterval();

  /**
   * Soll nach der fehlgeschlagenen Validierung eine neue Connection verwendet werden?
   * Oder soll der Aufrufer über eine NoConnectionAvailableException informiert werden, 
   * dass die Validierung fehlgeschlagen ist? 
   * Üblicherweise (true) soll eine neue Connection verwendet werden.
   * @return
   */
  public boolean rebuildConnectionAfterFailedValidation();

}
