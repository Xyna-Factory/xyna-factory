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

package com.gip.xyna.xnwh.persistence.memory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_ConnectionClosedException;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TransactionProperty;
import com.gip.xyna.xnwh.persistence.memory.PreparedCommandForMemory.CommandType;
import com.gip.xyna.xnwh.persistence.memory.index.AtomicBulkUpdate;
import com.gip.xyna.xnwh.persistence.memory.index.Index;



public abstract class MemoryPersistenceLayerConnection implements PersistenceLayerConnection {

  private static final Logger logger = CentralFactoryLogging.getLogger(MemoryPersistenceLayerConnection.class);


  private List<MemoryRowLock> locksForUpdate;

  private boolean closed = false;


  private final TransactionCache transactionCache;
  private final XynaMemoryPersistenceLayer pl;

  public MemoryPersistenceLayerConnection(XynaMemoryPersistenceLayer pl) {
    this.pl = pl;
    transactionCache = new TransactionCache();
  }


  private final TransactionCache getTransactionCache() {
    return this.transactionCache;
  }


  public final <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows)
      throws PersistenceLayerException {
    return this.query(query, parameter, maxRows, query.getReader());
  }
  

  public final <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader)
      throws PersistenceLayerException {

    ensureOpen();
    String table = query.getTable();
    TableObject<Storable, MemoryRowData<Storable>> t = checkTable(table);
    if (maxRows < 0) {
      maxRows = Integer.MAX_VALUE;
    }
    if (query instanceof PreparedQueryForMemory) {

      PreparedQueryForMemory<E> memoryQuery = (PreparedQueryForMemory<E>) query;
      boolean forUpdate = memoryQuery.isForUpdate();
      QueryResult<E> result =
          t.query(getContainingPersistenceLayer(), memoryQuery, parameter, reader, maxRows, forUpdate);
      List<MemoryRowLock> locks = result.getLocks();
      if (locks != null && locks.size() > 0) {
        for (MemoryRowLock lock : locks) {
          addLock(lock);
        }
      }

      List<E> resultList = result.getResult();
      TransactionCacheTable tableCache = getTransactionCache().getUpdatedTableContent(table);
      if (tableCache != null) {
        Map<Object, Storable> resultMap = null;

        if (tableCache.allUpdatedObjects() != null) {
          if (resultMap == null) {
            resultMap = new HashMap<Object, Storable>();
            for (Storable storable : (List<Storable>) resultList) {
              resultMap.put(storable.getPrimaryKey(), storable);
            }
          }
          Map<Object, TransactionCacheEntry> updatedMap = tableCache.allUpdatedObjects();
          for (TransactionCacheEntry storable: updatedMap.values()) {
            boolean cachedVersionMatchesQuery = memoryQuery.checkWhereClause(storable.getNewContent(), parameter);
            if (cachedVersionMatchesQuery) {
              Storable entryInQueryResult = resultMap.get(storable.getNewContent().getPrimaryKey());
              if (entryInQueryResult != null) {
                entryInQueryResult.setAllFieldsFromData(storable.getNewContent());
              } else {
                resultMap.put(storable.getNewContent().getPrimaryKey(), storable.getNewContent());
              }
            } else {
              resultMap.remove(storable.getNewContent().getPrimaryKey());
            }
          }
        }

        if (tableCache.allDeletedObjects() != null) {
          if (resultMap == null) {
            resultMap = new HashMap<Object, Storable>();
            for (Storable storable : (List<Storable>) resultList) {
              // dont even add entries that have already been deleted within this transaction
              if (!tableCache.allDeletedObjects().containsKey(storable.getPrimaryKey())) {
                resultMap.put(storable.getPrimaryKey(), storable);
              }
            }
          } else {
            // remove entries that have been added due to query hit but have been deleted within this transaction
            for (Object o : tableCache.allDeletedObjects().keySet()) {
              resultMap.remove(o);
            }
          }
        }

        // check all newly inserted objects
        if (tableCache.allInsertedObjects() != null) {
          if (resultMap == null) {
            resultMap = new HashMap<Object, Storable>();
            for (Storable storable : (List<Storable>) resultList) {
              resultMap.put(storable.getPrimaryKey(), storable);
            }
          }
          Map<Object, TransactionCacheEntry> insertedMap = tableCache.allInsertedObjects();
          for (TransactionCacheEntry cacheEntry : insertedMap.values()) {
            if (memoryQuery.checkWhereClause(cacheEntry.getNewContent(), parameter)) {
              resultMap.put(cacheEntry.getNewContent().getPrimaryKey(), cacheEntry.getNewContent());
            }
          }
        }

        if (resultMap != null) {
          resultList = new ArrayList<E>((Collection<? extends E>) resultMap.values());
          if (memoryQuery.isOrdered()) {
            Collections.sort(resultList, memoryQuery.getComparator());
          }
        }

      }

      return resultList != null ? resultList : new ArrayList<E>();

    } else if (query instanceof PreparedCountQueryForMemory) {

      List<E> result = new ArrayList<E>();
      E el = t.queryOneRow(getContainingPersistenceLayer(), (PreparedCountQueryForMemory<E>) query, parameter, reader, false);
      result.add(el);
      return result;

    }

    throw new XNWH_IncompatiblePreparedObjectException(PreparedQuery.class.getSimpleName());

  }
  

  public final <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
    ensureOpen();
    TableObject<T, ? extends MemoryRowData<T>> t = checkTable(storable.getTableName());
    return t.contains(getContainingPersistenceLayer(), storable);
  }


  public final <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {

    ensureOpen();

    // transaction handling
    if (storableCollection == null || storableCollection.size() == 0) {
      return;
    }

    // create a copy since the passed collection may be unmodifiable
    storableCollection = new ArrayList<T>(storableCollection);

    TransactionCacheTable<T> tableCache = null;
    Iterator<T> iter = storableCollection.iterator();
    String tableName = null;
    while (iter.hasNext()) {
      T next = iter.next();
      if (tableName == null) {
        tableName = next.getTableName();
      }
      if (tableCache == null) {
        tableCache = (TransactionCacheTable<T>) getTransactionCache().getUpdatedTableContent(tableName);
        if (tableCache == null || tableCache.allInsertedObjects() == null
            || tableCache.allInsertedObjects().size() == 0) {
          break;
        }
      }
      TransactionCacheEntry<T> previouslyInsertedEntry = tableCache.allInsertedObjects().remove(next.getPrimaryKey());
      if (previouslyInsertedEntry != null) {
        iter.remove();
        if (tableCache.allInsertedObjects().size() == 0) {
          break;
        }
      }
    }

    deleteInternally(storableCollection, false, null);

  }


  public final <T extends Storable> void deleteOneRow(T toBeDeleted) throws PersistenceLayerException {
    // TODO performance
    Collection<T> toBeDeletedList = new ArrayList<T>();
    toBeDeletedList.add(toBeDeleted);
    delete(toBeDeletedList);
  }


  private <T extends Storable> void deleteInternally(Collection<T> storableCollection, boolean commit,
                                                     Map<ColumnDeclaration, AtomicBulkUpdate> indexUpdates)
      throws PersistenceLayerException {

    if (storableCollection != null && storableCollection.size() > 0) {
      T item = storableCollection.iterator().next();
      String tableName = item.getTableName();
      TableObject<T, ? extends MemoryRowData<T>> t = checkTable(tableName);
      Map<Object, MemoryRowData<T>> locksAndRowData =
          t.delete(getContainingPersistenceLayer(), storableCollection, commit, indexUpdates);
      if (locksAndRowData != null) {
        for (MemoryRowData<T> rd : locksAndRowData.values()) {
          try {
            addLock(rd.getLock(getContainingPersistenceLayer()));
          } catch (UnderlyingDataNotFoundException e) {
            throw new RuntimeException("object should have been locked but lock was deleted.", e);
          }
        }
      }
      if (!commit) {
        if (locksAndRowData != null) {
          for (T storable : storableCollection) {
            MemoryRowData<T> rd = locksAndRowData.get(storable.getPrimaryKey());
            if (rd == null) {
              // object did not exist
            } else {
              getTransactionCache().addDeletedTableContent(tableName, rd, storable);
            }
          }
        }
      }
    }

  }


  public final <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
    TableObject<T, ? extends MemoryRowData<T>> t = checkTable(klass);
    if (t.getSize(pl) == 0) {
      TransactionCacheTable<T> cacheForTable =
          (TransactionCacheTable<T>) getTransactionCache().getUpdatedTableContent(t.getName());
      if (cacheForTable == null || cacheForTable.isEmpty()) {
        // nothing is in the table and the table has not been changed in the transaction so far
        // -> no need to calculate the complicated delete statement right now - maybe never required
        return;
      }
    }
    WeakHashMap<Class<?>, PreparedCommand> deleteAllCommandCache = pl.getDeleteAllCommandCache();
    PreparedCommand pc;
    synchronized (deleteAllCommandCache) {
      pc = deleteAllCommandCache.get(klass);
      if (pc == null) {
        pc = prepareCommand(new Command("delete from " + t.getName()));
        deleteAllCommandCache.put(klass, pc);
      }
    }
    executeDML(pc, new Parameter());
  }


  private final void addLock(MemoryRowLock rl) {
    if (rl != null) {
      if (locksForUpdate == null) {
        //nicht threadsicher, aber connection ist nicht dazu gedacht, aus mehreren threads benutzt zu werden
        locksForUpdate = new ArrayList<MemoryRowLock>();
      }
      locksForUpdate.add(rl);
    }
  }


  public final void closeConnection() throws PersistenceLayerException {
    if (closed) {
      return;
    }
    // rollback also releases all locks
    try {
      rollback();
    } catch (Throwable t) {
      logger.error(null, t);
    }
    closed = true;
  }


  protected abstract void commitInternally(TransactionCache transactionInformation, List<MemoryRowLock> sustainedLocks)
      throws PersistenceLayerException;


  public final void commit() throws PersistenceLayerException {
    ensureOpen();
    commitInternally(getTransactionCache(), locksForUpdate);
    
    // simple use the predefined method to release all locks in a sequence
    releaseLocks();
    
    getTransactionCache().clear();
  }
  
  private static class BLA extends Storable { //FIXME workaround um ant-compiler zum compilieren zu überlisten.
    public Object getPrimaryKey() {
      return null;
    }
    public ResultSetReader getReader() {
      return null;
    }
    public void setAllFieldsFromData(Storable arg0) {
    }
  }
  
  
  private static abstract class CommitAction implements Comparable<CommitAction> {
    private final long uniqueId;
    
    public CommitAction(long uniqueId) {
      this.uniqueId = uniqueId;
    }
    public abstract void exec() throws PersistenceLayerException;
    @Override
    public int compareTo(CommitAction o) {
      if (o.uniqueId == uniqueId) {
        return 0;
      }
      if (uniqueId > o.uniqueId) {
        return 1;
      }
      return -1;
    }
    
    
  }


  protected final void defaultCommit(TransactionCache transactionInformation) throws PersistenceLayerException {
    /*
     * geänderte zeilen haben zeilenlocks (sustained lock).
     * schwierige aufgabe; konsistentes update von sowohl indizes, als auch table/row
     * ausserdem: temporary locks in der gleichen reihenfolge holen, wie es auch queries tun!
     * 
     * usecases:
     * - gleichzeitige änderung oder queries for update: geschützt durch zeilenlocks (sustained lock)
     * - gleichzeitige queries NOT for update: überprüfung des temporary locks muss verhindern, dass gleichzeitig die daten rausgegeben werden. 
     * 
     * problem: gleichzeitige query hat index-readlock, bevor es versucht, auf zeilen zuzugreifen und dafür zeilen-readlocks holt.
     *          -> ist in preparedqueryformemory durch eine "assumeddeadlockexception" gelöst
     */

    final XynaMemoryPersistenceLayer pl = getContainingPersistenceLayer();
    Collection<TransactionCacheTable<?>> transactionCacheData = transactionCache.allUpdatedTablesOrNull();
    if (transactionCacheData != null && transactionCacheData.size() > 0) {
      for (TransactionCacheTable<?> m : transactionCacheData) {
        final TableObject t = this.<BLA, MemoryRowData<BLA>> checkTable(m.getTableName());

        final List<Lock> temporaryLocks = new ArrayList<Lock>(); //TODO benötigt man beim delete die locks auch?
        try {

          // TODO bulk updates lazy erstellen
          final Map<ColumnDeclaration, AtomicBulkUpdate> bulkUpdates = new HashMap<ColumnDeclaration, AtomicBulkUpdate>();

          Map<ColumnDeclaration, Index> tmp = t.getAllIndices();
          for (Entry<ColumnDeclaration, Index> e : tmp.entrySet()) {
            AtomicBulkUpdate bulkUpdate = e.getValue().startBulkUpdate(null);
            bulkUpdates.put(e.getKey(), bulkUpdate);
          }
          
          List<CommitAction> allActions = new ArrayList<CommitAction>();

          if (m.allUpdatedObjects() != null) {
            Map<Object, ? extends TransactionCacheEntry<?>> updatedMap = m.allUpdatedObjects();
            for (final TransactionCacheEntry<?> updatedCacheEntry : updatedMap.values()) {
              if (!updatedCacheEntry.getRowData().isCommitted()) {
                Lock writeLock = t.getTableLock().writeLock();
                writeLock.lock();
                try {
                  t.getDataInterface().moveUncommittedToCommitted(pl, updatedCacheEntry.getNewContent());
                } finally {
                  writeLock.unlock();
                }
              }
              allActions.add(new CommitAction(updatedCacheEntry.getRowData().getUniqueID()) {

                @Override
                public void exec() throws PersistenceLayerException {
                  t.commitUpdateAfterLock(pl, updatedCacheEntry.getNewContent(), updatedCacheEntry.getRowData(),
                                          bulkUpdates, temporaryLocks);
                }
                
              });

            }
          }
          
          //richtige lockreihenfolge
          Collections.sort(allActions);
          for (CommitAction ca : allActions) {
            ca.exec();
          }
          
          if (m.allInsertedObjects() != null) {
            Map<Object, ? extends TransactionCacheEntry<?>> insertedMap = m.allInsertedObjects();
            for (TransactionCacheEntry<?> insertedCacheEntry : insertedMap.values()) {
              if (!insertedCacheEntry.getRowData().isCommitted()) {
                Lock writeLock = t.getTableLock().writeLock();
                writeLock.lock();
                try {
                  t.getDataInterface().moveUncommittedToCommitted(pl, insertedCacheEntry.getNewContent());
                } finally {
                  writeLock.unlock();
                }
              }
              t.commitInsertAfterLock(pl, insertedCacheEntry.getRowData(), insertedCacheEntry.getNewContent(),
                                      bulkUpdates);
            }
          }
          if (m.allDeletedObjects() != null && m.allDeletedObjects().size() > 0) {
            Collection<Storable<?>> allToBeDeleted = new ArrayList<Storable<?>>();
            Map<Object, ? extends TransactionCacheEntry<?>> deletedMap = m.allDeletedObjects();
            for (TransactionCacheEntry<?> entry : deletedMap.values()) {
              allToBeDeleted.add(entry.getNewContent());
            }
            deleteInternally(allToBeDeleted, true, bulkUpdates); 
          }

          for (Entry<ColumnDeclaration, AtomicBulkUpdate> entry : bulkUpdates.entrySet()) {
            AtomicBulkUpdate abu = entry.getValue();
            if (abu.size() > 0) {
              try {
                synchronized (tmp.get(entry.getKey())) {
                  abu.commit();
                }
              } catch (RuntimeException e) {
                throw new RuntimeException("Exception during commit on index <" + entry.getKey().getName() + "> in table " + t.getName(), e);
              }
            }
          }

        } finally {
          for (Lock l : temporaryLocks) {
            l.unlock();
          }
        }

      }

    }
  }


  protected final void releaseLocks() {
    if (locksForUpdate != null && locksForUpdate.size() > 0) {
      for (MemoryRowLock l : locksForUpdate) {
        l.sustainedLock().unlock();
      }
      locksForUpdate.clear();
    }
  }


  public final int executeDML(PreparedCommand cmdInterface, Parameter paras) throws PersistenceLayerException {

    PreparedCommandForMemory commandForMemory;
    try {
      commandForMemory = (PreparedCommandForMemory) cmdInterface;
    } catch (ClassCastException e) {
      throw new XNWH_IncompatiblePreparedObjectException(PreparedCommand.class.getSimpleName());
    }

    ensureOpen();
    String table = commandForMemory.getTable();
    TableObject<Storable, MemoryRowData<Storable>> t = checkTable(table);

    if (commandForMemory.getCommandType() == CommandType.delete) {
      //select statement bauen:
      PreparedQueryForMemory preparedQuery = commandForMemory.getPreparedSelectQueryForAffectedRows();
      List result = query(preparedQuery, paras, -1);
      deleteInternally(result, false, null);
      return result.size();
    }

    throw new XNWH_UnsupportedPersistenceLayerFeatureException("command type: " + commandForMemory.getCommandType());

  }


  public final <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {

    TableObject<T, ? extends MemoryRowData<T>> t = checkTable(klass);

    Collection<T> resultList = (Collection<T>) t.getAll(getContainingPersistenceLayer());
    if (resultList != null && resultList.size() > 0) {
      TransactionCacheTable cache = getTransactionCache().getUpdatedTableContent(t.getName());
      if (cache != null) {
        Map<Object, T> resultMap = new HashMap<Object, T>(resultList.size());
        for (T storable : resultList) {
          resultMap.put(storable.getPrimaryKey(), storable);
        }
        // the order in which updates due to transaction inserts/updates/deletes are taken into account
        // should not matter since an object can only be in one of these categories
        if (cache.allInsertedObjects() != null) {
          resultMap.putAll((Map<? extends Object, ? extends T>) cache.allInsertedObjects());
        }
        if (cache.allDeletedObjects() != null && cache.allDeletedObjects().size() > 0) {
          for (Object deletedPk : cache.allDeletedObjects().keySet()) {
            resultMap.remove(deletedPk);
          }
        }
        if (cache.allUpdatedObjects() != null && cache.allUpdatedObjects().size() > 0) {
          Map<Object, TransactionCacheEntry<T>> updatedMap = cache.allUpdatedObjects();
          for (TransactionCacheEntry<T> updatedCacheEntry : updatedMap.values()) {
            Storable overwrittenEntry = resultMap.get(updatedCacheEntry.getNewContent().getPrimaryKey());
            if (overwrittenEntry != null) {
              overwrittenEntry.setAllFieldsFromData(updatedCacheEntry.getNewContent());
            }
          }
        }
        resultList = resultMap.values();
      }
    }
    List<T> clonedResults = new ArrayList<T>();
    for( T r : resultList ) {
      clonedResults.add( Storable.clone( r ) );
    }
    return clonedResults;
  }


  public final <T extends Storable> void persistCollection(Collection<T> storableCollection)
      throws PersistenceLayerException {
    // TODO performance
    if (storableCollection != null && storableCollection.size() > 0) {
      for (T object : storableCollection) {
        persistObject(object);
      }
    }
  }


  public final <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {

    ensureOpen();

    String tableName = storable.getTableName();
    TableObject<T, ? extends MemoryRowData<T>> tableObject = checkTable(tableName);
    
    MemoryRowData<T> rowData;
    Lock readLock = tableObject.getTableLock().readLock();
    readLock.lock();
    try {
      rowData =
        tableObject.getDataInterface().get(getContainingPersistenceLayer(), storable.getPrimaryKey());
    } finally {
      readLock.unlock();
    }

    TransactionCacheTable cache = getTransactionCache().getUpdatedTableContent(tableName);
    if (cache != null) {
      if (cache.allDeletedObjects() != null && cache.allDeletedObjects().remove(storable.getPrimaryKey()) != null) {
        getTransactionCache().addUpdatedTableContent(tableName, rowData, storable);
        return false;
      }
      if (cache.allUpdatedObjects() != null) {
        Map<Object, TransactionCacheEntry<T>> map = cache.allUpdatedObjects();
        TransactionCacheEntry<T> previouslyUpdated = map.get(storable.getPrimaryKey());
        if (previouslyUpdated != null) {
          previouslyUpdated.getNewContent().setAllFieldsFromData(storable);
          return true;
        }
      }
      if (cache.allInsertedObjects() != null) {
        Map<Object, TransactionCacheEntry<T>> map = cache.allInsertedObjects();
        TransactionCacheEntry<T> previouslyInserted = map.get(storable.getPrimaryKey());
        if (previouslyInserted != null) {
          previouslyInserted.getNewContent().setAllFieldsFromData(storable);
          return true;
        }
      }
      if (cache.allLockedRows() != null) {
        Map<Object, TransactionCacheEntry<T>> map = cache.allLockedRows();
        TransactionCacheEntry<T> previouslyLocked = map.remove(storable.getPrimaryKey());
        if (previouslyLocked != null) {
          Map<Object, TransactionCacheEntry<T>> updatedMap = cache.allUpdatedObjects(true);
          updatedMap.put(storable.getPrimaryKey(), previouslyLocked);
          previouslyLocked.getNewContent().setAllFieldsFromData(storable);
          return true;
        }
      }
    }
    //else not in transactioncache

    PersistResult<T> result = persistObjectInternally(storable);
    if (result.wasAnUpdate) {
      getTransactionCache().addUpdatedTableContent(tableName, result.memoryRowData, storable);
    } else {
      getTransactionCache().addInsertedTableContent(tableName, result.memoryRowData, storable);
    }
    return result.wasAnUpdate;

  }


  static class PersistResult<T extends Storable> {

    public MemoryRowData<T> memoryRowData;
    public boolean wasAnUpdate;


    public PersistResult(MemoryRowData<T> memoryRowData, boolean wasAnUpdate) {
      this.memoryRowData = memoryRowData;
      this.wasAnUpdate = wasAnUpdate;
    }
  }


  protected final <T extends Storable> PersistResult<T> persistObjectInternally(T storable)
      throws PersistenceLayerException {
    TableObject<T, ? extends MemoryRowData<T>> t = checkTable(storable.getTableName());
    PersistResult<T> result = t.persistObject(getContainingPersistenceLayer(), storable);
    if (result == null) {
      throw new NullPointerException("persistObject may never return null.");
    }
    try {
      addLock(result.memoryRowData.getLock(getContainingPersistenceLayer()));
    } catch (UnderlyingDataNotFoundException e) {
      throw new RuntimeException("object should have been locked but lock was deleted.", e);
    }
    return result;
  }
  

  public final <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
    String table = query.getTable();
    TableObject<Storable, MemoryRowData<Storable>> t = checkTable(table);
    Class<IPreparedQueryForMemory<E>> c =
        getContainingPersistenceLayer().getPreparedQueryCreator().createClass(query, t);
    IPreparedQueryForMemory<E> q = instantiateClass(c, query);
    return q;
  }


  public final <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    ensureOpen();
    String tableName = storable.getTableName();

    // check whether the object is contained in the transaction cache
    TransactionCacheTable transactionCache = getTransactionCache().getUpdatedTableContent(tableName);
    if (transactionCache != null) {
      if (transactionCache.allDeletedObjects() != null
          && transactionCache.allDeletedObjects().containsKey(storable.getPrimaryKey())) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(storable.getPrimaryKey() + "", tableName);
      }
      TransactionCacheEntry<T> cachedResult = null;
      if (transactionCache.allInsertedObjects() != null) {
        cachedResult = (TransactionCacheEntry) transactionCache.allInsertedObjects().get(storable.getPrimaryKey());
      }
      if (cachedResult == null && transactionCache.allUpdatedObjects() != null) {
        cachedResult = (TransactionCacheEntry) transactionCache.allUpdatedObjects().get(storable.getPrimaryKey());
      }
      if (cachedResult == null && transactionCache.allLockedRows() != null) {
        cachedResult = (TransactionCacheEntry) transactionCache.allLockedRows().get(storable.getPrimaryKey());
      }
      if (cachedResult != null) {
        storable.setAllFieldsFromData(cachedResult.getNewContent());
        return;
      }
    }

    TableObject<T, ? extends MemoryRowData<T>> t = checkTable(tableName);
    t.queryRowAndFill(getContainingPersistenceLayer(), storable, false);

  }


  public final <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {

    IPreparedQueryForMemory<E> queryForMemory;
    try {
      queryForMemory = (IPreparedQueryForMemory<E>) query;
    } catch (ClassCastException e) {
      throw new XNWH_IncompatiblePreparedObjectException(query.getClass().getName());
    }

    ensureOpen();
    String table = query.getTable();

    // if this is not a count query, first check the transaction cache for query hits
    if (queryForMemory instanceof PreparedQueryForMemory) {

      TransactionCacheTable tableCache = getTransactionCache().getUpdatedTableContent(table);
      if (tableCache != null) {
        PreparedQueryForMemory<E> queryForMemoryNoCount = (PreparedQueryForMemory<E>) queryForMemory;
        if (tableCache.allUpdatedObjects() != null && tableCache.allUpdatedObjects().size() > 0) {
          Map<Object, TransactionCacheEntry> map = tableCache.allUpdatedObjects();
          for (TransactionCacheEntry possiblyMatchingCachedEntry : map.values()) {
            if (queryForMemoryNoCount.checkWhereClause(possiblyMatchingCachedEntry.getNewContent(), parameter)) {
              // FIXME dont publish the internal object or at least clone the object if no "high performance"
              // transaction property is set?
              return (E) possiblyMatchingCachedEntry.getNewContent();
            }
          }
        }
        if (tableCache.allInsertedObjects() != null && tableCache.allInsertedObjects().size() > 0) {
          Map<Object, TransactionCacheEntry> map = tableCache.allInsertedObjects();
          for (TransactionCacheEntry possiblyMatchingCachedStorable : map.values()) {
            if (queryForMemoryNoCount.checkWhereClause(possiblyMatchingCachedStorable.getNewContent(), parameter)) {
              // FIXME dont publish the internal object or at least clone the object if no "high performance"
              // transaction property is set?
              return (E) possiblyMatchingCachedStorable.getNewContent();
            }
          }
        }
      }

    }

    TableObject<Storable, MemoryRowData<Storable>> t = checkTable(table);
    // FIXME "forUpdate" korrekt ermitteln und berücksichtigen
    E result = t.queryOneRow(getContainingPersistenceLayer(), queryForMemory, parameter, false);

    if (queryForMemory instanceof PreparedCountQueryForMemory) {
      // FIXME count result unter Berücksichtigung der Transaktions-Daten updaten
    }
    return result;

  }


  public final void rollback() throws PersistenceLayerException {
    // first discard all changes within this transaction...
    if (!getTransactionCache().isEmpty()) {
      rollbackInternallyWithoutLocks(getTransactionCache());
      getTransactionCache().clear();
    }
    // ... and finally release all locks
    releaseLocks();
  }


  protected abstract void rollbackInternallyWithoutLocks(TransactionCache transactionCache)
      throws PersistenceLayerException;


  public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    ensureOpen();

    boolean foundInTransactionCache = false;
    String tableName = storable.getTableName();
    TransactionCacheTable tablecache = getTransactionCache().getUpdatedTableContent(tableName);
    if (tablecache != null) {
      if (tablecache.allLockedRows() != null && tablecache.allLockedRows().size() > 0) {
        Map<Object, TransactionCacheEntry<T>> map = tablecache.allLockedRows();
        TransactionCacheEntry<T> existingLockedRow = map.get(storable.getPrimaryKey());
        if (existingLockedRow != null) {
          storable.setAllFieldsFromData(existingLockedRow.getNewContent());
          foundInTransactionCache = true;
        }
      }
      if (!foundInTransactionCache) {
        if (tablecache.allUpdatedObjects() != null && tablecache.allUpdatedObjects().size() > 0) {
          Map<Object, TransactionCacheEntry<T>> map = tablecache.allUpdatedObjects();
          TransactionCacheEntry<T> existingUpdatedRow = map.get(storable.getPrimaryKey());
          if (existingUpdatedRow != null) {
            storable.setAllFieldsFromData(existingUpdatedRow.getNewContent());
            foundInTransactionCache = true;
          }
        }
        if (!foundInTransactionCache) {
          if (tablecache.allInsertedObjects() != null && tablecache.allInsertedObjects().size() > 0) {
            Map<Object, TransactionCacheEntry<T>> map = tablecache.allInsertedObjects();
            TransactionCacheEntry<T> existingInsertedRow = map.get(storable.getPrimaryKey());
            if (existingInsertedRow != null) {
              storable.setAllFieldsFromData(existingInsertedRow.getNewContent());
              foundInTransactionCache = true;
            }
          }
        }
      }
    }

    if (!foundInTransactionCache) {
      TableObject<T, ? extends MemoryRowData<T>> t = checkTable(tableName);
      MemoryRowLock updateWriteLock = t.queryRowAndFill(getContainingPersistenceLayer(), storable, true);
      addLock(updateWriteLock);
      
      Lock readLock = t.getTableLock().readLock();
      readLock.lock();
      try {
        getTransactionCache().addLockedRow(tableName,
                                         t.getDataInterface().get(getContainingPersistenceLayer(),
                                                                  storable.getPrimaryKey()), storable);
      } finally {
        readLock.unlock();
      }
    }

  }


  public final PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
    String table = cmd.getTable();
    TableObject<Storable, MemoryRowData<Storable>> tableObj = checkTable(table);
    PreparedCommandForMemory pcmd = new PreparedCommandForMemory(cmd, tableObj, this);
    if (pcmd.getCommandType() != CommandType.delete) {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("command type: " + pcmd.getCommandType());
    }
    return pcmd;
  }


  private void ensureOpen() throws PersistenceLayerException {
    if (closed) {
      throw new XNWH_ConnectionClosedException();
    }
  }


  private <E> IPreparedQueryForMemory<E> instantiateClass(Class<IPreparedQueryForMemory<E>> c, Query<E> query)
      throws PersistenceLayerException {
    try {
      Constructor<IPreparedQueryForMemory<E>> constructor = c.getConstructor(Query.class, PersistenceLayer.class);
      IPreparedQueryForMemory<E> result = constructor.newInstance(query, getContainingPersistenceLayer());
      return result;
    } catch (Exception e) {
      throw new XNWH_GeneralPersistenceLayerException("could not create instance of generated class", e);
    }
  }


  protected abstract XynaMemoryPersistenceLayer getContainingPersistenceLayer();


  private final <T extends Storable, X extends MemoryRowData<T>> TableObject<T, X> checkTable(Class<T> klazz)
      throws PersistenceLayerException {
    String tableName = Storable.getPersistable(klazz).tableName();
    return this.<T,X>checkTable(tableName);
  }


  protected abstract <T extends Storable, X extends MemoryRowData<T>> TableObject<T, X> checkTable(String tableName)
      throws PersistenceLayerException;


  public void setTransactionProperty(TransactionProperty arg0) {
  }
  
  
  public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> arg0) throws PersistenceLayerException {
    //TODO something?    
  }


  public boolean isOpen() {
    return true;
  }

}
