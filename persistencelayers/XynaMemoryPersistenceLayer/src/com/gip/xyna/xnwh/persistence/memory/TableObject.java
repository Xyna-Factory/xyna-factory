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



import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidObjectForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_MissingAnnotationsException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_UniqueConstraintViolationException;
import com.gip.xyna.xnwh.persistence.AnnotationHelper;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.memory.MemoryPersistenceLayerConnection.PersistResult;
import com.gip.xyna.xnwh.persistence.memory.index.AtomicBulkUpdate;
import com.gip.xyna.xnwh.persistence.memory.index.Index;



public abstract class TableObject<T extends Storable, X extends MemoryRowData<T>> implements TableInfo {

  private static final Logger logger = CentralFactoryLogging.getLogger(TableObject.class);
  private static final long serialVersionUID = 1L;

  private ColumnDeclaration[] colTypes;
  private int indexOfPrimaryKey = -1;
  private String name;
  private Class<T> backingClass;


  public TableObject(Class<T> storableClass) throws PersistenceLayerException {

    backingClass = storableClass;
    Persistable persi = AnnotationHelper.getPersistable(storableClass);
    if (persi == null) {
      throw new XNWH_MissingAnnotationsException(storableClass.getName());
    }
    name = persi.tableName();

    ArrayList<ColumnDeclaration> cols = new ArrayList<ColumnDeclaration>();
    Class<?> klass = storableClass;
    while (Storable.class.isAssignableFrom(klass)) {
      Field[] fields = klass.getDeclaredFields();
      for (Field f : fields) {
        Column col = f.getAnnotation(Column.class);
        if (col != null) {
          boolean isPk = col.name().equals(persi.primaryKey());
          if (isPk) {
            indexOfPrimaryKey = cols.size();
          }
          cols.add(new ColumnDeclaration(col, backingClass, isPk, f.getType()));
        }
      }
      klass = klass.getSuperclass();
    }

    colTypes = cols.toArray(new ColumnDeclaration[cols.size()]);

  }


  private Map<ColumnDeclaration, Index<? extends Comparable<?>, X>> indices =
      new HashMap<ColumnDeclaration, Index<? extends Comparable<?>, X>>();
  private ReentrantReadWriteLock indicesLock = new ReentrantReadWriteLock();


  public Index<? extends Comparable<?>, X> getIndex(ColumnDeclaration c) {
    return indices.get(c);
  }


  public Map<ColumnDeclaration, Index<? extends Comparable<?>, X>> getAllIndices() {
    final Lock rLock = indicesLock.readLock();
    rLock.lock();
    try {
      return new HashMap<ColumnDeclaration, Index<? extends Comparable<?>, X>>(indices);
    } finally {
      rLock.unlock();
    }
  }


  public void addIndex(ColumnDeclaration c, Index<? extends Comparable<?>, X> index) {
    final Lock wLock = indicesLock.writeLock();
    wLock.lock();
    try {
      indices.put(c, index);
    } finally {
      wLock.unlock();
    }
  }


  public ColumnDeclaration[] getColTypes() {
    return colTypes;
  }


  public <U extends T> boolean contains(PersistenceLayer pl, U storable) {
    Object pk = storable.getPrimaryKey();
    Lock readLock = getTableLock().readLock();
    readLock.lock();
    try {
      return getDataInterface().containsKey(pl, pk);
    } finally {
      readLock.unlock();
    }
  }


  public PersistResult<T> persistObject(PersistenceLayer pl, T storable) throws PersistenceLayerException {

    checkConstraintsAndLockRowSustained(pl, storable);

    MemoryRowData<T> rd = getSingleRow(pl, storable.getPrimaryKey());
    if (rd != null) {
      //update muss nur das lock holen. ist schon passiert
      return new PersistResult<T>(rd, true);
    } else {
      try {
        return insertAfterLock(pl, storable);
      } catch (XNWH_UniqueConstraintViolationException e) {
        throw new RuntimeException(e); //kann nicht passieren, weil vorher gelockt
      }
    }

  }


  private void checkConstraintsAndLockRowSustained(PersistenceLayer pl, T storable) throws PersistenceLayerException {
    //1. check constraints
    //   standard: vergleich mit werten von committeten zeilen: fehler, falls constraint verletzt.
    //   nicht standard: falls constraint mit werten in nicht abgeschlossener transaktion verletzt ist, warten bis diese transaktion beendet ist. dann standard.
    //TODO

    //2. hole sustained lock

    MemoryRowLock rl;
    final Lock tableWriteLock = getTableLock().writeLock();
    while (true) {
      tableWriteLock.lock();
      try {
        //gibt keine uncommitteten rowdatas zur�ck
        X rd = getDataInterface().get(pl, storable.getPrimaryKey());
        if (rd == null) {
          //falls andere uncommittete transaktion das rowlock bereits erstellt hat, wird dies hier erkannt und zur�ckgegeben.
          //ansosnten wird es neu erstellt.
          rl = getDataInterface().putUncommitted(pl, storable);
        } else {
          try {
            rl = rd.getLock(pl);
          } catch (UnderlyingDataNotFoundException e) {
            throw new RuntimeException("lock was unexpectedly missing while having tablelock", e);
          }
        }
      } finally {
        tableWriteLock.unlock();
      }
      try {
        rl.sustainedLock().lock();
        break;
      } catch (UnderlyingDataNotFoundException e) {
        continue; //nochmal versuchen, das rowlock zu holen oder neu anzulegen, falls jemand schneller war.
      }
    }
  }


  private PersistResult<T> insertAfterLock(PersistenceLayer pl, T storable) throws PersistenceLayerException {

    Object pk = storable.getPrimaryKey();
    // check again that the object has not been created in the meantime
    MemoryRowData<T> rd = getSingleRow(pl, pk);
    if (rd == null) {
      Lock writeLock = getTableLock().writeLock();
      writeLock.lock();
      try {
        X newRowData = getDataInterface().createRowData(pl, storable);
        return new PersistResult<T>(newRowData, false);
      } finally {
        writeLock.unlock();
      }
    } else {
      throw new XNWH_UniqueConstraintViolationException(pk.toString(), storable.getTableName());
    }
  }


  public void commitInsertAfterLock(PersistenceLayer pl, X newRowData, T storable,
                                    Map<ColumnDeclaration, AtomicBulkUpdate> indexUpdates) {
    //muss checken, ob es ein uncommitted lock gibt und dieses in die neuen rowdatas �bernehmen.
    
    //FIXME kann das objekt bereits existieren (konfliktierendes anderes insert)?! dann ben�tigt man hier ein indexupdate
    Lock writeLock = getTableLock().writeLock();
    writeLock.lock();
    try {
      getDataInterface().put(pl, storable.getPrimaryKey(), newRowData);
    } finally {
      writeLock.unlock();
    }
    for (Entry<ColumnDeclaration, AtomicBulkUpdate> col : indexUpdates.entrySet()) {
      AtomicBulkUpdate nextUpdate = col.getValue();
      nextUpdate.add((Comparable) storable.getValueByColString(col.getKey().getName()), newRowData);
    }
  }


  public void commitUpdateAfterLock(PersistenceLayer pl, T storable, X rd,
                                    Map<ColumnDeclaration, AtomicBulkUpdate> indexUpdates, List<Lock> temporaryLocks)
      throws XNWH_InvalidObjectForTableException {
    MemoryRowLock rlTemp;
    try {
      rlTemp = rd.getLock(pl);
    } catch (UnderlyingDataNotFoundException e) {
      throw new RuntimeException("row was deleted from other source during update", e);
    }

    
    Lock l = rlTemp.temporaryLock().writeLock();
    temporaryLocks.add(l); 
    l.lock(); //nach index-commit freigeben
   // System.out.println(Thread.currentThread().getName() + " locked " + rd.getUniqueID());
    boolean deleted = rd.isDeleted();
    T previousValue = rd.setData(pl, storable);

    if (deleted) {
      //ein delete ist dazwischengekommen
      Lock writeLock = getTableLock().writeLock();
      writeLock.lock();
      try {
        getDataInterface().put(pl, storable.getPrimaryKey(), rd);
      } finally {
        writeLock.unlock();
      }
      for (Entry<ColumnDeclaration, AtomicBulkUpdate> e : indexUpdates.entrySet()) {
        AtomicBulkUpdate nextUpdate = e.getValue();
        nextUpdate.add((Comparable) storable.getValueByColString(e.getKey().getName()), rd);
      }
    } else {
      //TODO kann previousvalue null sein?
      for (Entry<ColumnDeclaration, AtomicBulkUpdate> e : indexUpdates.entrySet()) {
        AtomicBulkUpdate nextUpdate = e.getValue();
        Comparable oldValue = (Comparable) previousValue.getValueByColString(e.getKey().getName());
        Comparable newValue = (Comparable) storable.getValueByColString(e.getKey().getName());
        if (oldValue != null && oldValue.compareTo(newValue) != 0 || oldValue == null && newValue != null) {
          nextUpdate.update(oldValue, newValue, rd);
        }
      }
    }

  }

  public <E> QueryResult<E> query(PersistenceLayer pl, PreparedQueryForMemory<E> query, Parameter p, int maxRows,
                                  boolean forUpdate) throws PersistenceLayerException {
    return this.query(pl, query, p, query.getReader(), maxRows, forUpdate);
  }

  public <E> QueryResult<E> query(PersistenceLayer pl, PreparedQueryForMemory<E> query, Parameter p, ResultSetReader<? extends E> rsr, int maxRows,
                                  boolean forUpdate) throws PersistenceLayerException {

    //kann eigentlich nicht null zur�ckgeben, da selbst generierte klasse daf�r verantwortlich ist
    IMemoryBaseResultSet rs = getResultSet(query, p, forUpdate, maxRows);
    
    List<E> result = new ArrayList<E>();
    boolean executedSuccessfully = false;
    try {
      int cnt = 0;
      while (rs.next() && cnt < maxRows) {
        cnt++;
        E e = rsr.read(rs);
        if (e != null) { //non-null elemente kommen insbesondere durch den factory-warehouse cursor. aber null-elemente zur�ckzugeben macht nie sinn!
          result.add(e);
        }
      }
      executedSuccessfully = true;
    } catch (SQLException e) {
      throw new XNWH_GeneralPersistenceLayerException("Error executing resultsetreader", e);
    } finally {
      rs.unlockReadLocks(); //jetzt erst freigeben, um sicherzustellen, dass die evaluierung der whereclause konsistent 
      //zusammen mit der r�ckgabe der objekte ist
      if (!executedSuccessfully) {
        rs.unlockWriteLocks();
      }
    }

    return new QueryResult<E>(result, rs.getWriteLocks());

  }


  public MemoryRowLock queryRowAndFill(PersistenceLayer pl, Storable<? super T> storable, boolean lockForUpdate)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    if (storable.getPrimaryKey() == null) {
      throw new XNWH_GeneralPersistenceLayerException("Primarykey must not be null.");
    }

    MemoryRowData<T> rd = getSingleRow(pl, storable.getPrimaryKey());

    if (rd == null) {
      throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(storable.getPrimaryKey().toString(), getName());
    }

    if (lockForUpdate) {
      
      MemoryRowLock rowWriteLock;
      try {
        rowWriteLock = rd.getLock(pl);
        rowWriteLock.sustainedLock().lock();
      } catch (UnderlyingDataNotFoundException e1) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(storable.getPrimaryKey().toString(), getName());
      }
      rowWriteLock.temporaryLock().readLock().lock();
      try {
        rd.fillFromData(pl, storable);
      } catch (RuntimeException t) {
        rowWriteLock.sustainedLock().unlock();
        throw t;
      } catch (Error e) {
        Department.handleThrowable(e);
        rowWriteLock.sustainedLock().unlock();
        throw e;
      } catch (UnderlyingDataNotFoundException e) {
        throw new RuntimeException("Object not found though row has already been locked", e);
      } finally {
        rowWriteLock.temporaryLock().readLock().unlock();
      }
      return rowWriteLock;

    } else {

      try {
        final MemoryRowLock rowLock = rd.getLock(pl);
        rowLock.temporaryLock().readLock().lock();
        try {
          rd.fillFromData(pl, storable);
        } finally {
          rowLock.temporaryLock().readLock().unlock();
        }
      } catch (UnderlyingDataNotFoundException e) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(storable.getPrimaryKey().toString(), getName());
      }
      return null;

    }

  }


  private MemoryRowData<T> getSingleRow(PersistenceLayer pl, Object primaryKey) {
    final Lock tableReadLock = getTableLock().readLock();
    tableReadLock.lock();
    try {
      return getDataInterface().get(pl, primaryKey);
    } finally {
      tableReadLock.unlock();
    }
  }
  
  private <E> IMemoryBaseResultSet getResultSet(IPreparedQueryForMemory<E> query, Parameter p, boolean forUpdate,
                                                int maxRows) throws PersistenceLayerException {
    IMemoryBaseResultSet rs = query.execute(this, p, forUpdate, maxRows);
    while (rs.isInterruptedIndexTraversal() && rs.size() < maxRows) {
      //offenbar kann man noch mehr zeilen finden
      if (logger.isTraceEnabled()) {
        logger.trace("query retry because of maxrows " + maxRows);
      }
      rs.unlockReadLocks();
      rs.unlockWriteLocks();
      rs = query.execute(this, p, forUpdate, maxRows);
    }
    return rs;
  }


  public <E> E queryOneRow(PersistenceLayer pl, IPreparedQueryForMemory<E> query, Parameter p, boolean forUpdate) throws PersistenceLayerException {
    return this.queryOneRow(pl, query, p, query.getReader(), forUpdate);
  }
  
  public <E> E queryOneRow(PersistenceLayer pl, IPreparedQueryForMemory<E> query, Parameter p, ResultSetReader<? extends E> rsr, boolean forUpdate)
      throws PersistenceLayerException {
    IMemoryBaseResultSet rs = getResultSet(query, p, forUpdate, 1);

    E result = null;
    boolean executedSuccessfully = false;
    try {
      if (rs.next()) {
        result = rsr.read(rs);
      } //else not found
      executedSuccessfully = true;
    } catch (SQLException e) {
      throw new XNWH_GeneralPersistenceLayerException("error executing resultsetreader", e);
    } finally {
      rs.unlockReadLocks();
      if (!executedSuccessfully) {
        rs.unlockWriteLocks();
      }
    }

    return result;

  }


  /**
   * muss von aussen synchronisiert werden �ber tablelock
   */
  public Collection<X> getAllRowDatas(PersistenceLayer pl) {
    return getDataInterface().values(pl);
  }


  /**
   * muss von aussen synchronisiert werden �ber tablelock
   */
  public Iterator<X> iterator(PersistenceLayer pl) {
    Collection<X> col = getDataInterface().values(pl);
    return col.iterator();
  }


  public Class<T> getBackingClass() {
    return backingClass;
  }


  public String getName() {
    return name;
  }


  public void delete(PreparedCommandForMemory cmd, Parameter paras) {
    //TODO implement
    throw new RuntimeException("unsupported");
  }


  public <T extends Storable> Map<Object, MemoryRowData<T>> delete(PersistenceLayer pl, Collection<T> coll,
                                                                   boolean commit,
                                                                   Map<ColumnDeclaration, AtomicBulkUpdate> indexUpdates) {

    Map<Object, MemoryRowData<T>> rowdatas = null;

    Iterator<T> toBeRemovedIterator = coll.iterator();

    try {
      while (toBeRemovedIterator.hasNext()) {
        Storable<?> s = toBeRemovedIterator.next();
        Object pk = s.getPrimaryKey();
        if (commit) {
          //kein temporary lock notwendig, weil keine daten kaputt/inkonsistent gehen k�nnen.
          X removedRowData;
          final Lock writeLock = getTableLock().writeLock();
          writeLock.lock();
          try {
            removedRowData = getDataInterface().remove(pl, pk);
          } finally {
            writeLock.unlock();
          }
          if (removedRowData != null) {
            removedRowData.deleted();
            /*
             * eigtl darf das nicht null sein, weil man sich durch locks dagegen sch�tzt, dass ein anderer thread das objekt bereits entfernt hat.
             * ausnahme: w�hrend einer laufenden transaktion kann ein removetable aufgerufen werden. (usecase: updates, die storables migrieren, vgl bug 18497)
             * f�r die ist es aber ok, die entfernten objekte einfach zu ignorieren
             */
            for (Entry<ColumnDeclaration, AtomicBulkUpdate> e : indexUpdates.entrySet()) {
              AtomicBulkUpdate nextUpdate = e.getValue();
              Comparable oldValue;
              try {
                oldValue = (Comparable) removedRowData.getData(pl).getValueByColString(e.getKey().getName());
              } catch (UnderlyingDataNotFoundException e1) {
                throw new RuntimeException("Object should have been locked", e1);
              }
              nextUpdate.remove(oldValue, removedRowData);
            }
          }
        } else {
          MemoryRowData<T> rd = (MemoryRowData<T>) getDataInterface().get(pl, pk);
          if (rd != null) {
            if (rowdatas == null) {
              rowdatas = new HashMap<Object, MemoryRowData<T>>();
            }
            // throw an objectnotfound exception if null?
            try {
              MemoryRowLock rl = rd.getLock(pl);
              rl.sustainedLock().lock();
            } catch (UnderlyingDataNotFoundException e) {
              continue;
            }
            rowdatas.put(s.getPrimaryKey(), rd);
          }
        }
      }

    } catch (RuntimeException e) {
      if (rowdatas != null) {
        for (MemoryRowData<T> rd : rowdatas.values()) {
          try {
            rd.getLock(pl).sustainedLock().unlock();
          } catch (UnderlyingDataNotFoundException e1) {
            logger.warn("could not unlock " + rd.getUniqueID(), e1);
            continue;
          }
        }
      }
      throw e;
    } catch (Error e) {
      if (rowdatas != null) {
        for (MemoryRowData<T> rd : rowdatas.values()) {
          try {
            rd.getLock(pl).sustainedLock().unlock();
          } catch (UnderlyingDataNotFoundException e1) {
            logger.warn("could not unlock " + rd.getUniqueID(), e1);
            continue;
          }
        }
      }
      throw e;
    }

    return rowdatas;

  }


  public Collection<T> getAll(PersistenceLayer pl) {
    Collection<T> coll = new ArrayList<T>();
    final Lock readLock = getTableLock().readLock();
    readLock.lock();
    try {
      try {
        for (MemoryRowData<T> rd : getDataInterface().values(pl)) {
          Lock l = rd.getLock(pl).temporaryLock().readLock();
          l.lock();
          try {
            coll.add(rd.getData(pl));
          } finally {
            l.unlock();
          }
        }
      } catch (UnderlyingDataNotFoundException e) {
        throw new RuntimeException("Underlying object got lost while table was locked", e);
      }
    } finally {
      readLock.unlock();
    }
    return coll;
  }


  public void clear(PersistenceLayer pl) {
    final Lock writeLock = getTableLock().writeLock();
    writeLock.lock();
    try {
      getDataInterface().dataClear(pl);
    } finally {
      writeLock.unlock();
    }
  }


  public int getSize(PersistenceLayer pl) {
    Lock readLock = getTableLock().readLock();
    readLock.lock();
    try {
      return getDataInterface().dataSize(pl); // FIXME size separat speichern?
    } finally {
      readLock.unlock();
    }
  }


  public abstract DataInterface<T, X> getDataInterface();


  public abstract ReadWriteLock getTableLock();


  private volatile String nameOfPrimaryKey = null;
  private Object nameOfPrimaryKeyLock = new Object();


  public String getNameOfPrimaryKey() {
    if (nameOfPrimaryKey == null) {
      synchronized (nameOfPrimaryKeyLock) {
        if (nameOfPrimaryKey == null) {
          for (ColumnDeclaration cd : getColTypes()) {
            if (cd.isPrimaryKey()) {
              nameOfPrimaryKey = cd.getName();
            }
          }
        }
      }
    }
    return nameOfPrimaryKey;
  }

}
