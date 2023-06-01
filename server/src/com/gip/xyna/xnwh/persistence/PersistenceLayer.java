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

package com.gip.xyna.xnwh.persistence;

import java.io.Reader;


public interface PersistenceLayer {
  
  /**
   * Gibt eine Connection zurück, die spezifisch zu dem PersistenceLayer ist. Kann intern zb über eine HashMap&lt;args, ConPool&gt;
   * gepoolt werden, so dass gleichartige Verbindungen aus dem gleichen Pool kommen.
   */
  public PersistenceLayerConnection getConnection() throws PersistenceLayerException;
  
  /**
   * wie {@link #getConnection()}, aber verwendet die innere Connection von der übergebenen Connection (aus dem gleichen Pool!)
   * Achtung: Die innere connection wird dann nicht beim close geschlossen - sondern nur beim close der connection, die die innere connection geöffnet hat.
   */
  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException;
  
  public PersistenceLayerConnection getDedicatedConnection() throws PersistenceLayerException;

  /**
   * kann nach aussen hin abgefragt werden. Laufzeit Informationen zu der PersistenceLayerInstance
   */
  public String getInformation();
  
  /**
   * spezialinformationen abfragen, über interne datenhaltung etc 
   */
  public Reader getExtendedInformation(String[] args);
  
  /**
   * Welche Parameter frisst die init Methode? für jeden Parameter einen beschreibenden String
   */
  public String[] getParameterInformation();

  /**
   * gibt true zurück, falls beide PersistenceLayer auf den gleichen Tabellen arbeiten, und dadurch zb Locks in dem einen
   * PersistenceLayer mit Locks vom anderen kollidieren können.
   */
  public boolean describesSamePhysicalTables(PersistenceLayer plc);

  /**
   * gibt true zurück, falls beide PersistenceLayer den gleichen ConnectionPool verwenden
   */
  public boolean usesSameConnectionPool(PersistenceLayer plc);

  public void init(Long persistenceLayerInstanceID, String... connectionParameters) throws PersistenceLayerException;

  public void shutdown() throws PersistenceLayerException;

}
