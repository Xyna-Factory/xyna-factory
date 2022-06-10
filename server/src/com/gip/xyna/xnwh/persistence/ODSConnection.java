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

import java.util.List;



public interface ODSConnection extends Connection {

  /**
   * falls listenToPersistenceLayerChanges=true, bleibt zurückgegebenes command wirksam, wenn die zugrundeliegende
   * persistencelayer geändert wird. achtung, diese preparedcommands werden nicht garbagecollected, bis zum
   * servershutdown
   */
  public PreparedCommand prepareCommand(Command cmd, boolean listenToPersistenceLayerChanges)
      throws PersistenceLayerException;


  /**
   * falls listenToPersistenceLayerChanges=true, bleibt zurückgegebene query wirksam, wenn die zugrundeliegende
   * persistencelayer geändert wird achtung, diese preparedqueries werden nicht garbagecollected, bis zum servershutdown
   */
  public <E> PreparedQuery<E> prepareQuery(Query<E> query, boolean listenToPersistenceLayerChanges)
      throws PersistenceLayerException;


  public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(String sqlQuery, Parameter parameters, ResultSetReader<T> rsr, int cacheSize)
      throws PersistenceLayerException;

  public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(String sqlQuery, Parameter parameters, ResultSetReader<T> rsr,
                                                                     int cacheSize, PreparedQueryCache cache) throws PersistenceLayerException;


  public ODSConnectionType getConnectionType();


  public void executeAfterCommit(Runnable runnable);
  
  public void executeAfterCommitFails(Runnable runnable);
  
  /**
   * wird auch ausgeführt, wenn rollback nicht funktioniert.
   */
  public void executeAfterRollback(Runnable runnable);

  
  public void executeAfterClose(Runnable runnable);
  
  public void executeAfterCommit(Runnable runnable, int priority);
  
  public void executeAfterCommitFails(Runnable runnable, int priority);
  
  /**
   * wird auch ausgeführt, wenn rollback nicht funktioniert.
   */
  public void executeAfterRollback(Runnable runnable, int priority);

  
  public void executeAfterClose(Runnable runnable, int priority);

  /**
   * Nimmt ODSConnection bereits an einer Transaktion teil?
   * @return 
   */
  public boolean isInTransaction();
  
  /**
   * Stellt sicher, dass alle benötigten Connections vorliegen, um auf die übergebenen Storables zugreifen zu können.
   * Bereits in der Transaktion benutzte Connections werden nicht validiert.
   * @param storableClazz
   * @throws PersistenceLayerException
   */
  public void ensurePersistenceLayerConnectivity(List<Class<? extends Storable<?>>> storableClazz) throws PersistenceLayerException;

  /**
   * verbindet die andere connection mit dieser (und andersherum), so dass intern benötigte connections aus dem gleichen connectionpool nicht doppelt aufgemacht werden.
   * genauer: falls nach diesem aufruf neue connections aufgemacht werden (in einer der beiden connections), wird falls möglich eine bestehende connection wiederverwendet.
   * 
   * beispiel:
   * OrderArchive persist-&gt;HISTORY + delete from OrderBackup.DEFAULT sollen über den gleichen Connectionpool gehen.
   */
  public void shareConnectionPools(ODSConnection con);

}
