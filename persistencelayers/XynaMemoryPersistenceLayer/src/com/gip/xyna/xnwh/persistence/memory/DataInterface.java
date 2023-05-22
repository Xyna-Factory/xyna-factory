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

package com.gip.xyna.xnwh.persistence.memory;

import java.util.Collection;

import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;



public interface DataInterface<T extends Storable, X extends MemoryRowData<T>> {

  public int dataSize(PersistenceLayer pl);


  public void dataClear(PersistenceLayer pl);


  public Collection<X> values(PersistenceLayer pl);


  /**
   * abgesehen von gleichzeitig stattfindenden neuen inserts etc:
   * - zeile wird aus tabelle entfernt, d.h. darauf folgende zugriffe darauf finden es nicht mehr
   * - vorhandene referenzen auf objekt sind noch valide
   * - vorhandene referenzen auf das {@link MemoryRowLock} werden ung�ltig, d.h. ein lock-versuch auf das sustainedlock wirft eine {@link UnderlyingDataNotFoundException}.
   * @return previous value
   */
  public X remove(PersistenceLayer pl, Object pk);


  public X get(PersistenceLayer pl, Object primaryKey);


  public void put(PersistenceLayer pl, Object pk, X content);


  public boolean containsKey(PersistenceLayer pl, Object pk);


 // public void removeTemporaryObject(PersistenceLayer pl, X data);


  public X createRowData(PersistenceLayer pl, T content) throws PersistenceLayerException;


  public MemoryRowLock putUncommitted(PersistenceLayer pl, T content) throws PersistenceLayerException;


  public void moveUncommittedToCommitted(PersistenceLayer pl, T target);

}
