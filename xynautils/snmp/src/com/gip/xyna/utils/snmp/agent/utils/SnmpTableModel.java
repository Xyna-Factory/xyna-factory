/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.utils.snmp.agent.utils;

/**
 * Interface, ueber welches die Daten der {@link SnmpTable} zugaenglich gemacht werden. 
 *
 */
public interface SnmpTableModel {

  /**
   * Ist angegebene Position schreibbar? 
   * @param row
   * @param column
   * @return
   */
  public boolean canSet(int row, int column);

  /**
   * Ausgabe des Objects in angegebener Position
   * @param row
   * @param column
   * @return
   */
  public Object get(int row, int column);

  /**
   * Eintragen des Objekts an der angebenen Position
   * @param row
   * @param column
   * @param o
   * @return true, falls erfolgreich
   */
  public boolean set(int row, int column, Object o );
  
  /**
   * @return Anzahl der Spalten, Indexspalten werden mitgezaehlt
   */
  public int getColumnCount();

  /**
   * @return Anzahl der Zeilen
   */
  public int getRowCount();

}
