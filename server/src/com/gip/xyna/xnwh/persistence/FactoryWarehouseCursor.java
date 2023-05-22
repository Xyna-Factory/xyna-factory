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

package com.gip.xyna.xnwh.persistence;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.Department;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;



public final class FactoryWarehouseCursor<T> {
  
  private static Logger logger = Logger.getLogger(FactoryWarehouseCursor.class);
  
  private AtomicBoolean started = new AtomicBoolean(false);
  private QueryExecutor<T> queryExecutor;
  private SynchronousQueue<T> queue;
  private int batchSize;
  private boolean cursor_finished = false;

  //wird von odsimpl verwaltet
  static ThreadPoolExecutor threadPool;


  /**
   * Anlegen des FactoryWarehouseCursor mit batchSize=1 f�r foreach-Aufruf
   * @param connection
   * @param sqlStatement
   * @param parameter
   * @param resultSetReader
   * @throws PersistenceLayerException
   */
  public FactoryWarehouseCursor(ODSConnection connection, String sqlStatement, Parameter parameter,
                                ResultSetReader<T> resultSetReader) throws PersistenceLayerException {
    this(connection, sqlStatement, parameter, resultSetReader, 1, new PreparedQueryCache());
  }


  /**
   * Anlegen des FactoryWarehouseCursor
   * @param connection
   * @param sqlStatement
   * @param parameter
   * @param resultSetReader
   * @param batchSize
   * @throws PersistenceLayerException
   */
  public FactoryWarehouseCursor(ODSConnection connection, String sqlStatement, Parameter parameter,
                                ResultSetReader<T> resultSetReader, int batchSize) throws PersistenceLayerException {
    this(connection, sqlStatement, parameter, resultSetReader, batchSize, new PreparedQueryCache());
  }


  /**
   * Anlegen des FactoryWarehouseCursor
   * @param connection
   * @param sqlStatement
   * @param parameter
   * @param resultSetReader
   * @param batchSize
   * @throws PersistenceLayerException
   */
  public FactoryWarehouseCursor(ODSConnection connection, String sqlStatement, Parameter parameter,
                                ResultSetReader<T> resultSetReader, int batchSize, PreparedQueryCache queryCache)
      throws PersistenceLayerException {
    String creator = Thread.currentThread().getName();
    if (queryCache == null) {
      queryCache = new PreparedQueryCache();
    }
    this.queryExecutor =
        new QueryExecutor<T>(creator, sqlStatement, connection, parameter, resultSetReader, queryCache);
    this.queue = queryExecutor.getQueue();
    this.batchSize = batchSize;
  }


  /**
   * Liefert den n�chsten Batch (leer, falls keine Daten eine leere Menge falls n
   * @return Batch (leer, falls keine Daten mehr gelesen werden k�nnen
   * @throws PersistenceLayerException falls w�hrend des Lesens eine Exception auftrat
   */
  public List<T> getRemainingCacheOrNextIfEmpty() throws PersistenceLayerException {
    startInnerThread();
    queryExecutor.rethrowLastThrowable();
    List<T> batch = getNextBatch(batchSize);
    
    if ( batch.isEmpty() ) {
      queryExecutor.rethrowLastThrowable();
    }
    
    return batch;
  }
 
  /**
   * Schlie�t den Cursor: es werden keine weiteren Daten gelesen
   */
  public void close() {
    queryExecutor.close();
  }


  /**
   * Einfacher Zugriff auf die Daten in einer foreach-Schleife. Achtung: W�hrend der 
   * Iteration kann keine PersistenceLayerException geworfen werden. Daher muss diese
   * nach der Iteration �ber {@link #checkForExceptions()} ermittelt werden
   * @param batchSize
   * @throws PersistenceLayerException bitte {@link #checkForExceptions()} pr�fen!
   */
  public Iterable<List<T>> batched( final int batchSize) throws PersistenceLayerException {
    if( batchSize <= 0 ) {
      throw new IllegalArgumentException("batchSize must not be negative or 0");
    }
    return new Iterable<List<T>>() {
      public Iterator<List<T>> iterator() {
        startInnerThread();
        return new BatchIterator(batchSize);
      }
    };
  }
 
  /**
   * Einfacher Zugriff auf die Daten in einer foreach-Schleife. Achtung: W�hrend der 
   * Iteration kann keine PersistenceLayerException geworfen werden. Daher muss diese
   * nach der Iteration �ber {@link #checkForExceptions()} ermittelt werden
   * @return Iterable&lt;List&lt;T&gt;&gt;
   * @throws PersistenceLayerException bitte {@link #checkForExceptions()} pr�fen!
   */
  public Iterable<T> separated() throws PersistenceLayerException {
    return new Iterable<T>() {
      public Iterator<T> iterator() {
        startInnerThread();
        return new SingleIterator();
      }
    };
  }
  
  /**
   * Wirft die w�hrend der Ausf�hrung der Query im Hilfsthread gefangene Exception.
   */
  public void checkForExceptions() throws PersistenceLayerException, RuntimeException {
    queryExecutor.rethrowLastThrowable();
  }
  
  
  
  /**
   * BatchIterator-Implementierung: Daten werden in foreach in Batches der Anzahl batchSize behandelt
   */
  private class BatchIterator implements Iterator<List<T>> {
    List<T> nextBatch;
    private int batchSize;
    
    public BatchIterator(int batchSize) {
      this.batchSize = batchSize;
    }

    public boolean hasNext() {
      if (nextBatch == null) {
        nextBatch = getNextBatch(batchSize);
      }
      return ! nextBatch.isEmpty();
    }

    public List<T> next() {
      if (hasNext()) {
        List<T> ret = nextBatch;
        nextBatch = null;
        return ret;
      } else {
        throw new NoSuchElementException();
      }
    }
  }
  
  /**
   * SingleIterator-Implementierung: Daten werden in foreach einzeln behandelt
   */
  private class SingleIterator implements Iterator<T> {
    T nextEntry;
    
    public boolean hasNext() {
      if (nextEntry == null) {
        nextEntry = getNext();
      }
      return nextEntry != null;
    }

    public T next() {
      if (hasNext()) {
        T ret = nextEntry;
        nextEntry = null;
        return ret;
      } else {
        throw new NoSuchElementException();
      }
    }
  }
  
  private static final Object END_OF_RESULTSET = new Storable() {

    @Override
    public ResultSetReader getReader() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object getPrimaryKey() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void setAllFieldsFromData(Storable data) {
      // TODO Auto-generated method stub
      
    }
    
  };

  
  /**
   * Lesen des n�chsten Datensatz aus der Queue
   * @return
   */
  private T getNext() {
    if (cursor_finished) {
      return null;
    }
    T nextElement = null;
    while ( nextElement == null ) {
      try {
        nextElement = queue.poll(1000, TimeUnit.MILLISECONDS);
        if (nextElement == END_OF_RESULTSET) {
          nextElement = null;
          cursor_finished = true;
          break;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        queryExecutor.close();
        break;
      }
    }
    return nextElement;
  }
  
  /**
   * Lesen des n�chsten Batches
   * @param batchSize
   * @return
   */
  private List<T> getNextBatch(int batchSize) {
    List<T> batch = new ArrayList<T>(batchSize);
    for (int i = 0; i < batchSize; i++) {
      T nextElement = getNext();
      if( nextElement == null ) {
        break;
      }
      batch.add(nextElement);
    }
    return batch;
  }


  private void startInnerThread() {
    while (started.compareAndSet(false, true)) {
      try {
        threadPool.execute(queryExecutor);
        return;
      } catch (RuntimeException e) {
        if (logger.isTraceEnabled()) {
          logger.trace("Could not start runnable in thread pool: " + threadPool, e);
        }
        started.set(false);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
      } catch (Error e) {
        started.set(false);
        Department.handleThrowable(e);
        throw e;
      }
    }
  }
  
 
  /**
   * Ausf�hrung der Query: Runnable, um in einem Extra-Thread zu laufen
   *
   * @param <T>
   */
  private static class QueryExecutor<T> implements Runnable, ResultSetReader<T> {
    
    private String threadName;
    private PreparedQuery<T> preparedQuery;
    private PreparedQueryCache queryCache;
    private ODSConnection connection;
    private Parameter parameter;
    private ResultSetReader<T> resultSetReader;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile Throwable lastThrowable;
    private SynchronousQueue<T> queue;
    private volatile Thread thread;


    public QueryExecutor(String creator, String sqlStatement, ODSConnection connection, Parameter parameter,
                         ResultSetReader<T> resultSetReader, PreparedQueryCache queryCache)
        throws PersistenceLayerException {
      this.threadName = "FactoryWarehouseCursorThread (created by: " + creator + ")";
      this.queryCache = queryCache;
      this.connection = connection;
      this.parameter = parameter;
      this.resultSetReader = resultSetReader;
      this.preparedQuery = this.queryCache.getQueryFromCache(sqlStatement, connection, this);  // actually "this" makes no sense as we are not reusing it because it is an old instance and passing the real current "this" on .query() instead
      this.queue = new SynchronousQueue<T>();
    }
    
    public SynchronousQueue<T> getQueue() {
      return queue;
    }

    /**
     * Weiterwerfen der beim Ausf�hren der Query aufgetretenen Exception. Falls keine Exception auftrat, ist dies eine NOP.
     */
    public void rethrowLastThrowable() throws PersistenceLayerException {
      if (lastThrowable != null) {
        throw new XNWH_GeneralPersistenceLayerException("Exception executing cursor in separate cursor thread", lastThrowable);
      }
    }


    /**
     * Stoppen des QueryExecutor: es werden keine weiteren Daten gelesen
     */
    public void close() {
      closed.set(true);
      Thread t = thread;
      if (t != null) {
        t.interrupt();
      }
    }

    
    /** 
     * Ausf�hren der Query in eigenem Thread
     */
    public void run() {
      closed.set(false);
      thread = Thread.currentThread();
      final String originalThreadName = thread.getName();
      thread.setName(threadName);

      try {
        connection.query(preparedQuery, parameter, Integer.MAX_VALUE, this);
      } catch (FinishException e) {
        //Thread soll beendet werden
      } catch (Throwable t) {
        Department.handleThrowable(t);
        lastThrowable = t;
        logger.debug("Exception in QueryExecutor", t);
      } finally {
        if (closed.compareAndSet(false, true)) { 
          try {
            ((SynchronousQueue) queue).put(END_OF_RESULTSET);
          } catch (InterruptedException e) {
          }
        }
        thread.setName(originalThreadName);
        thread = null;
      }
    }
    
    /** 
     * Lesen des Datensatz und Eintragen in die Queue
     */
    public T read(ResultSet rs) throws SQLException {
      if( closed.get() ) {
        throw new FinishException();
      }
      
      T result = resultSetReader.read(rs);
      try {
        queue.put(result);
      } catch (InterruptedException e) {
      }
      return null; //damit keine zweite Datenhaltung geschieht
    }
    
    private static class FinishException extends RuntimeException {
      private static final long serialVersionUID = 1L;
    }
  }

}
