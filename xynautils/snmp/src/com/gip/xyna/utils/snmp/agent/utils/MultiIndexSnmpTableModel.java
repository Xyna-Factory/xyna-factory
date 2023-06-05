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
package com.gip.xyna.utils.snmp.agent.utils;

import com.gip.xyna.utils.snmp.OID;

/**
 * Tabellenmodell fuer mehrdimensionale Tabellen
 */
public interface MultiIndexSnmpTableModel {
  
  
  /**
   * Ausgabe des Objects in angegebener Position.
   * Die Laenge der OID betraegt numIndexes()+1.
   * @param row
   * @param column
   * @return
   */
  public Object get( OID oid);

  
  /**
   * Berechnung der naechsten OID fuer getNext-Requests. 
   * Die Laenge der OID betraegt numIndexes()+1.
   * Falls kein getNext mehr moeglich ist, weil das Tabellenende
   * erreicht wurde, soll null zurueckgegeben werden.
   * @param oid
   * @return
   */
  public OID getNextOID( OID oid );


  /**
   * Liefert die Anzahl der Indexe (Tabellendimension -1)
   * @return
   */
  public int numIndexes();
  
}
