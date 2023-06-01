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
package com.gip.xyna.xnwh.persistence;

import java.util.Collection;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


/**
 *
 */
public interface Connection {
  /**
   * übernimmt vorgenommene änderungen dieser transaktion und gibt locks wieder frei
   * @throws PersistenceLayerException
   */
  public void commit() throws PersistenceLayerException;
  /**
   * verwirft vorgenommene änderungen dieser transaktion und gibt locks wieder frei
   * @throws PersistenceLayerException
   */
  public void rollback() throws PersistenceLayerException;
  /**
   * gibt alle angesammelten resourcen frei. falls connection bereits geschlossen ist,
   * passiert gar nichts.
   * @throws PersistenceLayerException
   */
  public void closeConnection() throws PersistenceLayerException;
  
  /**
   * insert+update von objekten. die objekte darin sollten  über annotations persistable und column
   * konfiguriert sein. beim udpate werden immer alle spalten aktualisiert. möchte man nur einzelne
   * spalten aktualisieren muss man das mit {@link #executeDML(PreparedCommand, Parameter)} erledigen
   * @return true if the objected existed before and false otherwise
   * @see com.gip.xyna.xnwh.persistence.Column
   * @see com.gip.xyna.xnwh.persistence.Persistable
   */
  public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException;
  
  public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException;
  
  public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException;
  
  /**
   * 
   * @param cmd
   * @param paras werden, anders als bei {@link #query(PreparedQuery, Parameter, int)}, direkt übernommen,
   *        d.h. es werden keine PersistenceLayer-spezifischen Anpassungen vorgenommen. 
   * @return
   * @throws PersistenceLayerException
   */
  public int executeDML(PreparedCommand cmd, Parameter paras) throws PersistenceLayerException;

  /**
   * 
   * @param query
   * @param parameter 
   *        Anfragen verwenden " um Bereiche und \ um einzelne Zeichen zu Escapen. Bei Like-Anfragen müssen alle
   *        %, die nicht als Wildcards interpretiert werden sollen, escaped werden.
   *        Alle unescapten % müssen durch PersistenceLayer-spezifische Wildcards ersetzt werden.
   *        Außerdem müssen alle PersistenceLayer-spezifischen Steuerzeichen escaped werden.
   *        zB. soll "name like '%baum"%"wald%'" oder "name like '%baum\%wald%'" die Zeichenfolge baum%wald treffen können.
   * @param maxRows -1 für unbegrenzt. ansonsten gibt es maximal soviele elemente zurück
   * @return
   * @throws PersistenceLayerException
   */
  public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows) throws PersistenceLayerException;
  public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader ) throws PersistenceLayerException;

  //TODO:*querymethode, die nur die referenzen zurückgibt, falls möglich. d.h. ohne resultsetreader benutzung
  //     *update-methode, die java-updates auf objekten definiert, die per query ausgesucht werden. das update wird dann "unten"
  //        ausgeführt. vorteil: zb in clusterpersistencelayer braucht man nicht die gesamten objekte ändern, sondern kann die
  //        transformationen speichern. in anderen persistencelayers kann man sie u.a. in sql-updates umwandeln oder
  //        direkt ausführen
  
  /**
   * füllt das übergebene objekt über seine defaultquery und nimmt als parameter den primarykey der in dem übergebenen
   * objekt gesetzt sein muss
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY falls kein objekt mit dem primarykey gefunden wurde
   * @throws PersistenceLayerException bei sonstigen fehlern in der persistencelayer
   */
  public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


  public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


  /**
   * @return one object matching the given query or null if no object matches
   */
  public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException;

  /**
   * @return true, falls ein objekt mit dem gleichen PK wie das übergebene bereits existiert
   */
  public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException;

  /**
   * führt ein upsert durch, d.h. vorhandene Zeilen (erkannt an id) werden geupdated, nicht vorhandene zeilen werden eingefügt
   * @param <T>
   * @param storableCollection
   * @throws PersistenceLayerException
   */
  public <T extends Storable> void persistCollection(Collection<T> storableCollection) throws PersistenceLayerException;


  public <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException;


  public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException;


  public <T extends Storable> void deleteOneRow(T toBeDeleted) throws PersistenceLayerException;


  public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException;


  // public int query(PreparedQueryWithoutResult sqlString, Parameter paras, ResultSetReaderFunction readerFunction) throws PersistenceLayerException;


  // public int executeDDL(String sqlString, Parameter paras) throws PersistenceLayerException;

  /**
   * Hiermit können Transaction Properties an den PersistenceLayer durchgereicht werden. Diese werden je nach
   * Implementierung des PersistenceLayers behandelt oder ignoriert.
   */
  public void setTransactionProperty(TransactionProperty property);
  
  
  public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> storableClazz) throws PersistenceLayerException;
  
  
  public boolean isOpen();
  
}
