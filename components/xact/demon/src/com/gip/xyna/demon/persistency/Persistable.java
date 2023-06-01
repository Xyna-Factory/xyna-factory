/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.demon.persistency;

/**
 * Über dieses Interface können der DemonPersistency-Instanz alle mögliche Objekte übergeben werden,
 * die etwas zu persistieren haben. 
 *
 */
public interface Persistable {

  /**
   * @return eindeutiger Name, unter dem Persistable in DemonPersistency eingetragen wird
   */
  public String getUniqueName();

  /**
   * DemonPersistency intialisiert hierüber den persistierten Wert.
   * Der String kann null sein, wenn der Wert noch nie persistiert wurde  
   * @param value
   */
  public void setPersistentValue(String value);

  /**
   * Liefert den aktuelle Wert als String
   * Rückgabe darf nicht null sein, ansonsten wird der Wert nicht aktualisiert
   * @return
   */
  public String getPersistentValue();

}
