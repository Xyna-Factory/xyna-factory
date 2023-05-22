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



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.memory.index.Condition;
import com.gip.xyna.xnwh.persistence.memory.index.ConditionType;
import com.gip.xyna.xnwh.persistence.memory.index.Index;
import com.gip.xyna.xnwh.persistence.memory.index.ResultHandler;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser.EscapeParams;



public abstract class PreparedQueryForMemory<E> implements IPreparedQueryForMemory<E> {

  private static final Logger logger = CentralFactoryLogging.getLogger(PreparedQueryForMemory.class);

  private final Query<E> query;
  private final PersistenceLayer persistenceLayer;
  private static final Comparator<MemoryRowData<?>> comparatorForLocking = new Comparator<MemoryRowData<?>>() {

    public int compare(MemoryRowData<?> o1, MemoryRowData<?> o2) {
      return o1.getUniqueID() > o2.getUniqueID() ? 1 : -1;
    }
  };


  public PreparedQueryForMemory(Query<E> query, PersistenceLayer persistenceLayer) {
    this.query = query;
    this.persistenceLayer = persistenceLayer;
  }


  public final PersistenceLayer getPersistenceLayer() {
    return this.persistenceLayer;
  }


  protected abstract MemoryBaseResultSet getNewResultSet();


  private static Pattern removedEscapedQuotationMarks = Pattern.compile("\"");


  protected int getIndexedColumnIndex() {
    return -1;
  }


  protected abstract boolean orderByDiffersFromIndex();


  protected boolean isIndexOrderReversed() {
    return false;
  }


  protected ConditionType getIndexedConditionType() {
    throw new RuntimeException("This method has to be overridden by the implementation.");
  }


  protected int getPositionOfIndexedParameter() {
    throw new RuntimeException("This method has to be overridden by the implementation.");
  }


  private static class MyCondition<B extends Comparable<B>> implements Condition<Comparable<?>> {

    private final Comparable<B> value;
    private final ConditionType conditionType;


    public MyCondition(Comparable<B> c, ConditionType conditionType) {
      this.value = c;
      this.conditionType = conditionType;
    }


    public Comparable<?> getLookupValue() {
      return value;
    }


    public ConditionType getType() {
      return conditionType;
    }

  }

  private interface CheckWCExecutor<T extends Storable> {

    /**
     * gibt true zur�ck, falls row gelockt bleiben soll
     */
    boolean exec(MemoryRowData<T> rd, MemoryRowLock lock);
  }

  private enum WCEnum {
    CONTINUE, OK;
  }

  private static class CheckWCResult {

    private boolean match;
    private WCEnum continuation;


    public CheckWCResult(WCEnum continuation, boolean match) {
      this.match = match;
      this.continuation = continuation;
    }


    private static CheckWCResult CONTINUE = new CheckWCResult(WCEnum.CONTINUE, false);
    private static CheckWCResult OKTRUE = new CheckWCResult(WCEnum.OK, true);
    private static CheckWCResult OKFALSE = new CheckWCResult(WCEnum.OK, false);


    public static CheckWCResult valueOf(boolean whereClauseMatches) {
      if (whereClauseMatches) {
        return OKTRUE;
      } else {
        return OKFALSE;
      }
    }

  }

  private static class AssumedDeadlockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

  }


  private <T extends Storable<?>> CheckWCResult checkWhereClauseLocked(MemoryRowData<T> rd, CheckWCExecutor<T> executor, Parameter params,
                                                                       boolean detectDeadlock) {
    boolean whereClauseMatches;
    MemoryRowLock lock;
    T data;
    boolean keepLocked = false;
    try {
      lock = rd.getLock(persistenceLayer);
    } catch (UnderlyingDataNotFoundException e) {
      return CheckWCResult.CONTINUE;
    }
    //1. erst check ohne sustainedlock f�r selects ohne "for update": 
    try {
      //achtung, deadlock kann passieren, weil commit hat zeilenlock und will dann index-update machen (-> index writelock)
      //dieser thread genau andersherum: hat index-readlock und will zeilen-lock.
      while (!lock.temporaryLock().readLock().tryLock(500, TimeUnit.MICROSECONDS)) {
        if (detectDeadlock) {
          throw new AssumedDeadlockException();
        }
      }
    } catch (InterruptedException e) {
      if (detectDeadlock) {
        throw new AssumedDeadlockException();
      } else {
        throw new RuntimeException("interrupted");
      }
    }

    try {

      try {
        data = rd.getData(persistenceLayer);
      } catch (UnderlyingDataNotFoundException e1) {
        // object has been removed in the meantime
        return CheckWCResult.CONTINUE;
      }

      //whereClauseCheck ohne sustainedlock.
      //bei select for Update:
      //  falls matches: ist ein preliminary check. wird wiederholt innerhalb von sustainedlock
      //  falls nicht matches: wird nicht nochmal gecheckt.
      //bei normalem select:
      //  falls matches wird direkt dem resultset hinzugef�gt. keine weiteren checks mit sustainedlock
      whereClauseMatches = checkWhereClause(data, params);
      if (whereClauseMatches) {
        keepLocked = executor.exec(rd, lock);
      }

    } finally {
      if (!keepLocked) {
        lock.temporaryLock().readLock().unlock();
      } else {
        // System.out.println(Thread.currentThread().getName() + " locked " + rd.getUniqueID());
      }
    }
    return CheckWCResult.valueOf(whereClauseMatches);
  }


  private static final CheckWCExecutor dontLockDontAddToResultSet = new CheckWCExecutor() {

    public boolean exec(MemoryRowData rd, MemoryRowLock lock) {
      return false;
    }

  };


  private static final CheckWCExecutor lockButDontAddToResultSet = new CheckWCExecutor() {

    public boolean exec(MemoryRowData rd, MemoryRowLock lock) {
      return true;
    }

  };


  private static class LockAndAddToResultSet implements CheckWCExecutor {

    private MemoryBaseResultSet rs;


    private LockAndAddToResultSet(MemoryBaseResultSet rs) {
      this.rs = rs;
    }


    public boolean exec(MemoryRowData rd, MemoryRowLock lock) {
      rs.add(rd, lock);
      return true;
    }

  };


  protected Comparable<?> getFixedTargetValueOfIndexedColumn() {
    // may be overwritten within the generated code if the query uses an index by a fixed value that is not
    // taken from a Parameter object
    return null;
  }


  /**
   * achtung, nachdem diese methode aufgerufen wird, m�ssen die angesammelten readlocks wieder freigegeben werden!!
   * (rs.unlockReadLocks();)
   */
  public <T extends Storable, X extends MemoryRowData<T>> MemoryBaseResultSet execute(TableObject<T, X> table, Parameter p,
                                                                                      final boolean forUpdate, final int maxRows)
      throws PersistenceLayerException {

    final Parameter escapedParams = escapeParameters(p);

    List<X> copyOfRowData;

    final AtomicBoolean interruptedIndexTraversal = new AtomicBoolean(false);
    int indexedColumnIndex = getIndexedColumnIndex();

    /*
     * forUpdate  orderByDiffersFromIndex  useIndex     fall
     *     x                x                 x           A
     *     -                x                 x           B
     *     x                -                 x           C
     *     -                -                 x           D
     *     x                -                 -           E
     *     -                -                 -           F
     */


    final MemoryBaseResultSet rs = getNewResultSet();
    final CheckWCExecutor<T> wcExecutor;
    if (forUpdate) {
      //fall A, C, E -> lockt nicht bei checkWC
      wcExecutor = dontLockDontAddToResultSet;
    } else {
      if (orderByDiffersFromIndex() && indexedColumnIndex > -1) {
        //fall B
        wcExecutor = lockButDontAddToResultSet;
      } else {
        //fall D, F  -> f�gt direkt zu resultset hinzu
        wcExecutor = new LockAndAddToResultSet(rs);
      }
    }

    final Comparator<MemoryRowData> comparator = getRowDataComparator();
    final int modifiedMaxRows = (!forUpdate && orderByDiffersFromIndex()) ? /* B */maxRows : Math.max(maxRows + 3, maxRows); //overflow ber�cksichtigen

    if (indexedColumnIndex > -1) {
      ColumnDeclaration indexedColumn = table.getColTypes()[indexedColumnIndex];
      Index tmp = (Index) table.getIndex(indexedColumn);
      Index<? extends Comparable<?>, X> index = (Index<Long, X>) tmp;
      final List<X> copyOfRowData2 = new ArrayList<X>();
      final boolean orderByDiffersFromIndex = orderByDiffersFromIndex();

      Comparable<?> targetValueOfUsedIndexedColumn = getFixedTargetValueOfIndexedColumn();
      if (targetValueOfUsedIndexedColumn == null) {
        if (getPositionOfIndexedParameter() < 0 || getPositionOfIndexedParameter() > escapedParams.size() - 1) {
          throw new RuntimeException("Inconsistent compilation of prepared query, trying to use object at index <"
              + getPositionOfIndexedParameter() + "> of parameters <" + escapedParams + ">.");
        }
        targetValueOfUsedIndexedColumn = (Comparable<?>) escapedParams.get(getPositionOfIndexedParameter());
      }


      final List<X> checkingSet = new ArrayList<X>();

      while (true) {
        try {
          index.readOnly(new ResultHandler<X>() {

            private int cnt = 0;


            public boolean handle(List<X> values) {
              //falls !forUpdate, && !orderByDiffersFromIndex muss man sich die zeilen nicht merken, weil sie bereits in checkWCLocked zum resultset geaddet werden.
              for (X x : values) {
                CheckWCResult result = checkWhereClauseLocked(x, wcExecutor, escapedParams, true);
                //gel�scht kann es hier nicht sein, weil der index gelockt ist.
                if (result.match) {
                  if (orderByDiffersFromIndex) {
                    //fall A oder B
                    checkingSet.add(x);
                    if (checkingSet.size() > modifiedMaxRows) {
                      //nach order-by richten, und nicht ben�tigte zeilen wegschmeissen TODO performance: weniger h�ufig sortieren
                      Collections.sort(checkingSet, comparator);
                      X lastElement = checkingSet.remove(checkingSet.size() - 1);
                      if (!forUpdate) {
                        //fall B
                        try {
                          lastElement.getLock(persistenceLayer).temporaryLock().readLock().unlock();
                        } catch (UnderlyingDataNotFoundException e) {
                          // should not happen, was locked
                          logger.error("Read locked data vanished!", e);
                        }
                      }
                    }
                  } else {
                    if (forUpdate) {
                      //fall C
                      copyOfRowData2.add(x);
                      if (copyOfRowData2.size() >= modifiedMaxRows) {
                        //man selektiert durch den abbruch nicht alle daten. wenn nach dem index-traversal die 
                        //indizierten spalten auf den objekten ge�ndert werden, hat man beim re-check der condition
                        //evtl weniger zeilen gefunden, als man finden k�nnte (unter ber�cksichtigung von maxrows)
                        //um diese f�lle abzuschw�chen, werden hier bereits mehr zeilen selektiert als man eigentlich ben�tigt.
                        interruptedIndexTraversal.set(true); //TODO f�r composite order bys (order by a,b), sollte man besser alle values mit selektieren und dieses abbrechen ans ende der methode schieben
                        return false;
                      }
                    } else {
                      //fall D
                      //zeile bereits zu resultset hinzugef�gt (NotForUpdateCheckWCExecutor)
                      if (++cnt >= maxRows) {
                        interruptedIndexTraversal.set(true); //unn�tig, schadet aber nichts
                        return false;
                      }
                    }
                  }
                }
              }
              return true;
            }
          }, new MyCondition(targetValueOfUsedIndexedColumn, getIndexedConditionType()), isIndexOrderReversed());
        } catch (AssumedDeadlockException e) {
          //nochmal probieren, resultset clearen
          logger.trace("got deadlocked, retrying ...");
          rs.clear();
          continue;
        }
        break;
      }
      if (orderByDiffersFromIndex) {
        //fall A oder B
        copyOfRowData = checkingSet;
      } else {
        //fall C oder D
        copyOfRowData = copyOfRowData2;
      }
    } else {
      //fall E oder F: full table scan
      //hier darf man sich nicht auf maxRows beschr�nken, weil noch unklar ist, ob die gefundenen zeilen tats�chlich der whereclause entsprechen.
      copyOfRowData = getCopyOfRowDataForFullTableScan(table, forUpdate);

      if (isOrdered()) {
        //ungelockte vorauswahl treffen, damit man hinterher die locks in der sicheren reihenfolge holen kann
        Collections.sort(copyOfRowData, comparator);
        List<X> sublist = new ArrayList<X>();
        Iterator<X> it = copyOfRowData.iterator();
        while (it.hasNext()) {
          X rd = it.next();
          CheckWCResult result = checkWhereClauseLocked(rd, dontLockDontAddToResultSet, escapedParams, false);
          if (result.continuation == WCEnum.CONTINUE) {
            continue;
          }
          if (result.match) {
            sublist.add(rd);
            if (sublist.size() >= modifiedMaxRows) {
              if (it.hasNext()) {
                interruptedIndexTraversal.set(true);
              }
              break;
            }
          }
        }
        copyOfRowData = sublist;
      }
    }

    rs.setInterruptedIndexTraversal(interruptedIndexTraversal.get());

    if (forUpdate || indexedColumnIndex == -1) {
      //1) sortieren nach lock-order
      //fall B und D sind bereits gelockt -> fall A, C, E, F m�ssen nach lockreihenfolge sortiert werden
      Collections.sort(copyOfRowData, comparatorForLocking);
    } else {
      //einfachste f�lle -> gleich fertig.

      //D -> kann direkt zur�ckgegeben werden, wurde bereits zu resultset hinzugef�gt
      if (orderByDiffersFromIndex() && indexedColumnIndex > -1) {
        //B -> noch in richtiger reihenfolge zu resultset hinzuf�gen
        Collections.sort(copyOfRowData, comparator);
        for (X x : copyOfRowData) {
          try {
            rs.add(x, x.getLock(persistenceLayer));
          } catch (UnderlyingDataNotFoundException e) {
            //seit �berpr�fung der wc im index ist das objekt eigtl gelockt!
            throw new RuntimeException("Data was unexpectedly deleted");
          }
        }
      }
      return rs;
    }

    try {
      Iterator<X> it = copyOfRowData.iterator(); //nun wegen obiger sortierung in einer vor deadlocks sicheren reihenfolge
      while (it.hasNext()) {
        MemoryRowData<T> rd = it.next();

        //2) whereclause �berpr�fen, falls notwendig
        if (!forUpdate) {
          //F -> f�gt direkt zu resultset hinzu, falls whereclause noch stimmt
          checkWhereClauseLocked(rd, wcExecutor, escapedParams, false);
        } else {
          //forUpdate-f�lle A, C, E
          //jetzt nochmal gefundene objekte bei "selects for update" sustained-locken
          boolean needToUnlock = true;
          MemoryRowLock lock;
          try {
            lock = rd.getLock(persistenceLayer);
            lock.sustainedLock().lock();
          } catch (UnderlyingDataNotFoundException e1) {
            // object has been removed in the meantime
            continue;
          }
          T data;
          try {
            data = rd.getData(persistenceLayer);
          } catch (UnderlyingDataNotFoundException e) {
            throw new RuntimeException("Object should have been locked but data got lost", e);
          }

          try {
            lock.temporaryLock().readLock().lock();
            try {
              boolean whereClauseMatches = checkWhereClause(data, escapedParams);
              if (whereClauseMatches) {
                rs.add(rd, lock);
                needToUnlock = false;
              }
            } finally {
              if (needToUnlock) {
                lock.temporaryLock().readLock().unlock();
              }
            }
          } finally {
            if (needToUnlock) {
              lock.sustainedLock().unlock();
            }
          }
        }
      }

      //3. sortieren nach order-by. ausserdem die aus sicherheit zuviel selektierten zeilen wieder aus resultset entfernen. das kann man erst nach dem sortieren tun
      rs.orderByAndTruncateResult(comparator, maxRows);

    } catch (RuntimeException e) {
      //locks freigeben, weil das draussen keiner macht.
      rs.unlockReadLocks();
      rs.unlockWriteLocks();
      throw e;
    } catch (Error e) {
      rs.unlockReadLocks();
      rs.unlockWriteLocks();
      throw e;
    }

    return rs;
  }


  private <T extends Storable<?>, X extends MemoryRowData<T>> List<X> getCopyOfRowDataForFullTableScan(TableObject<T, X> table,
                                                                                                       boolean forUpdate) {
    // get a copy of the data to avoid having the table lock for too long
    final Lock tableReadLock = table.getTableLock().readLock();
    tableReadLock.lock();
    try {
      return new ArrayList<X>(table.getAllRowDatas(persistenceLayer));
    } finally {
      tableReadLock.unlock();
    }
  }


  private Parameter escapeParameters(Parameter p) {
    if (p == null) {
      return new Parameter();
    }

    boolean[] isLike = query.getLikeParameters();

    Parameter escapedParams = new Parameter();
    for (int i = 0; i < p.size(); i++) {
      Object next = p.get(i);
      if (next instanceof String) {
        escapedParams.add(SelectionParser.escapeParams((String) next, isLike[i], new EscapeForMemory()));
      } else {
        escapedParams.add(next);
      }
    }
    return escapedParams;
  }


  public static class EscapeForMemory implements EscapeParams {

    public String escapeForLike(String toEscape) {
      if (toEscape == null || toEscape.length() == 0) {
        return toEscape;
      }

      return Pattern.quote(toEscape);
    }


    public String getWildcard() {
      return ".*";
    }

  }


  protected abstract boolean checkWhereClause(Storable s, Parameter p);


  public String getTable() {
    return query.getTable();
  }


  public ResultSetReader<? extends E> getReader() {
    return query.getReader();
  }


  public Comparator getComparator() {
    return null;
  }


  public Comparator<MemoryRowData> getRowDataComparator() {
    final Comparator innerComp = getComparator();
    if (innerComp == null) {
      return null;
    } else {
      return new Comparator<MemoryRowData>() {

        public int compare(MemoryRowData o1, MemoryRowData o2) {
          try {
            return innerComp.compare(o1.getData(getPersistenceLayer()), o2.getData(getPersistenceLayer()));
          } catch (UnderlyingDataNotFoundException e) {
            // ignore, data wont match the where clause later anyway
            return -1;
          }
        }
      };
    }
  }


  public abstract boolean isOrdered();


  public abstract boolean isForUpdate();


  public String toString() {
    return query.getSqlString();
  }

}
