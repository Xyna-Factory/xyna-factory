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



import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.utils.RepeatedExceptionCheck;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_TooManyDedicatedConnections;



public class CentralComponentConnectionCache {

  private static Logger logger = CentralFactoryLogging.getLogger(CentralComponentConnectionCache.class);
  private static AtomicReference<CentralComponentConnectionCache> instanceReference =
      new AtomicReference<CentralComponentConnectionCache>(null);

  private final ConcurrentMap<String, CachedConnection> connectionCache;
  private ODSImpl ods;


  private CentralComponentConnectionCache() {
    connectionCache = new ConcurrentHashMap<String, CachedConnection>();
    ods = ODSImpl.getInstance();
  }


  public static CentralComponentConnectionCache getInstance() {
    CentralComponentConnectionCache instance = instanceReference.get();
    if (instance == null) {
      instanceReference.compareAndSet(null, new CentralComponentConnectionCache());
      return instanceReference.get();
    } else {
      return instance;
    }
  }

  public enum DedicatedConnection {
    IDGenerator,
    OSMCache,
    CronLikeScheduler,
    XynaScheduler,
    SchedulerOOMProtectionBackups, 
    VetoManagement;
  }
  
  public static ODSConnection getConnectionFor(DedicatedConnection user) throws CentralComponentConnectionCacheException {
    return getInstance().getCachedConnection(user.name(), Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES);
  }
  public static ODSConnection getConnectionFor(DedicatedConnection user, int retries) throws CentralComponentConnectionCacheException {
    return getInstance().getCachedConnection(user.name(), retries);
  }
  
  public ODSConnection openCachedConnection(ODSConnectionType type, DedicatedConnection user,
                                            StorableClassList storables) throws XNWH_TooManyDedicatedConnections, CentralComponentConnectionCacheException {
    CachedConnectionParameters ccp = new CachedConnectionParameters(type,user.name(),storables);
    return openCachedConnectionInternally(ccp, Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES, false );
  }
  
  public ODSConnection openCachedConnection(ODSConnectionType type, String identifier,
      StorableClassList storables) throws XNWH_TooManyDedicatedConnections, CentralComponentConnectionCacheException {
    CachedConnectionParameters ccp = new CachedConnectionParameters(type,identifier,storables);
    return openCachedConnectionInternally(ccp, Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES, false );
  }

  public ODSConnection openCachedConnection(ODSConnectionType type, String identifier, StorableClassList storables, int retries)
          throws XNWH_TooManyDedicatedConnections, CentralComponentConnectionCacheException {
    CachedConnectionParameters ccp = new CachedConnectionParameters(type,identifier,storables);
    return openCachedConnectionInternally(ccp, retries, false);
  }
  
  public ODSConnection getCachedConnection(String identifier, int retries) throws CentralComponentConnectionCacheException {
    CachedConnection con = getCachedConnectionInternally(identifier, retries);
    if( Constants.RECORD_THREAD_INFO_CONNECTION_CACHE ) {
      con.setThread();
    }
    return con;
  }
  
  private CachedConnection openCachedConnectionInternally(CachedConnectionParameters ccp, int retries, boolean replace) throws XNWH_TooManyDedicatedConnections, CentralComponentConnectionCacheException {
    CachedConnection con = connectionCache.get(ccp.getConnectionName());
    if( replace ) {
      if( con == null ) {
        throw new CentralComponentConnectionCacheException(ccp, "Connection is not opened for");
      }
      closeConnectionOnlyLoggingExceptions(con);
    } else {
      if (con != null) {
        throw new CentralComponentConnectionCacheException(ccp, "Connection is already opened for");
      }
    }
    CachedConnection newCon = new CachedConnection(openAndValidateConnectionWithRetries(ccp,retries), ccp);
    if( cacheConnection( replace, con, newCon) ) {
      return newCon;
    } else {
      logger.warn("Tried to cache a connection with " + ccp.getConnectionName()+ " where there already is a cached connection");
      closeConnectionOnlyLoggingExceptions(newCon);
      //da muss ein anderer Thread dazwischengekommen sein. Dies ist so nicht erwartet, da jeder ccp.getConnectionName() 
      //nur von einem Thread verwendet werden sollte
      throw new CentralComponentConnectionCacheException(ccp, "Connection is already opened for");
    }
  }
  
  private boolean cacheConnection(boolean replace, CachedConnection con, CachedConnection newCon) {
    if( replace ) {
      return connectionCache.replace(newCon.getConnectionName(), con, newCon);
    } else {
      return connectionCache.putIfAbsent(newCon.getConnectionName(), newCon) == null;
    }
  }


  private ODSConnection openAndValidateConnectionWithRetries(CachedConnectionParameters ccp, int retries) throws XNWH_TooManyDedicatedConnections, CentralComponentConnectionCacheException {
    try {
      return openAndValidateConnection(ccp);
    } catch ( XNWH_TooManyDedicatedConnections e ) {
      throw e;
    } catch (Exception e) { //XNWH_RetryTransactionException, PersistenceLayerException
      logger.warn("Failed to create or validate dedicated connection \""+ccp.getConnectionName()+"\"", e);
      RepeatedExceptionCheck rec = new RepeatedExceptionCheck();
      rec.checkRepeated(e);
      SleepCounter sleepCnt = new SleepCounter(50, 5000, 1, TimeUnit.MILLISECONDS, false);
      for( int r=0; r<retries; ++r ) {
        ODSConnection con = retryOpenAndValidateConnection(r,ccp,rec,sleepCnt);
        if( con != null ) {
          return con;
        }
      }
      throw new CentralComponentConnectionCacheException(ccp, "Maximum retries reached: cannot create and validate", rec);
    }
  }
  
  private ODSConnection retryOpenAndValidateConnection(int retry, CachedConnectionParameters ccp,
      RepeatedExceptionCheck rec, SleepCounter sleepCnt) throws XNWH_TooManyDedicatedConnections, CentralComponentConnectionCacheException {
    
    if( XynaFactory.getInstance().isShuttingDown() ) {
      //weitere Retries sind nicht mehr sinnvoll
      throw new CentralComponentConnectionCacheException(ccp, "Maximum retries reached (Factory Shutdown): cannot create and validate", rec);
    }
    try {
      sleepCnt.sleep();
    } catch (InterruptedException e) {
      //dann halt kürzer warten
      Thread.currentThread().interrupt();
    }
    
    try {
      return openAndValidateConnection(ccp);
    } catch ( XNWH_TooManyDedicatedConnections e ) {
      throw e;
    } catch (Exception e) { //XNWH_RetryTransactionException, PersistenceLayerException
      int repeated = rec.checkRepeationCount(e);
      if( repeated == 0 ) {
        logger.warn("Failed to create or validate dedicated connection \""+ccp.getConnectionName()+"\" in retry "+retry, e);
      } else {
        logger.warn("Failed to create or validate dedicated connection \""+ccp.getConnectionName()+"\" again("+repeated+") in retry "+retry);
      }
      return null;
    }
  }
  
  private ODSConnection openAndValidateConnection(CachedConnectionParameters ccp) throws PersistenceLayerException, XNWH_TooManyDedicatedConnections {
    ODSConnection con = null;
    boolean success = false;
    try {
      con = ods.openDedicatedConnection(ccp.getConnectionType(), ccp.getEnsuringStorable());
      //validieren
      con.ensurePersistenceLayerConnectivity(ccp.getEnsuringStorable());
      success = true;
      return con;
    } finally { 
      if( !success && con != null ) {
        closeConnectionOnlyLoggingExceptions(con);
      }
    } 
  }
  

  private CachedConnection getCachedConnectionInternally(String identifier, int retries) throws CentralComponentConnectionCacheException {
    CachedConnection con = connectionCache.get(identifier);
    if (con == null) {
      //openCachedConnection muss zuvor gerufen werden!
      throw new CentralComponentConnectionCacheException(identifier, "CachedConnection must be opened first for");
    }
    
    try {
      con.rollback(); //evtl. noch offene Transaktion beenden
      con.ensurePersistenceLayerConnectivity(con.getEnsuringStorable() );
      return con;
    } catch (PersistenceLayerException e) {
      logger.warn("Could not ensure a dedicated connection \""+con.getConnectionName()+"\"", e);
    }

    //CachedConnection kann nicht geheilt werden, daher nun neu bauen
    try {
      con = openCachedConnectionInternally(con, retries, true);
      logger.info("Recreated dedicated connection \""+con.getConnectionName()+"\"");
      return con;
    } catch (XNWH_TooManyDedicatedConnections e) {
      //hätte bei der ersten Initialisierung in openCachedConnection schon geworfen werden müssen.
      //hier deshalb unerwartet, daher auch keine Retries
      throw new CentralComponentConnectionCacheException(con, "Too many", e);
    }
  }
  
  
  static void closeConnectionOnlyLoggingExceptions(ODSConnection con) {
    try {
      if (con != null && 
          con.isOpen()) {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to close connection.",e);
    }
  }
  
  
  public List<CachedConnectionInformation> getConnectionCacheInformation() {
    List<CachedConnectionInformation> info = new ArrayList<CachedConnectionInformation>();
    for (Entry<String, CachedConnection> entry : connectionCache.entrySet()) {
      info.add(new CachedConnectionInformation(entry.getKey(), entry.getValue()));
    }
    return info;
  }
  
  public static class CentralComponentConnectionCacheException extends XNWH_GeneralPersistenceLayerException {
    private static final long serialVersionUID = 1L;
    
    public CentralComponentConnectionCacheException(CachedConnectionParameters ccp, String msg) {
      super(completeMsg(ccp.getConnectionName(),msg));
    }
    
    public CentralComponentConnectionCacheException(CachedConnectionParameters ccp, String msg, RepeatedExceptionCheck rec) {
      super(completeMsg(ccp.getConnectionName(),msg));
      if( rec != null ) {
        initCause(rec.getLastThrowable());
      }
    }
    
    public CentralComponentConnectionCacheException(CachedConnectionParameters ccp, String msg, Throwable cause) {
      super(completeMsg(ccp.getConnectionName(),msg), cause);
    }
    
    public CentralComponentConnectionCacheException(String connectionName, String msg) {
      super(completeMsg(connectionName,msg));
    }

    private static String completeMsg(String connectionName, String msg) {
      return msg+ " dedicated connection \""+connectionName+"\"";
    }

  }

  
  private static class CachedConnectionParameters {
    protected final StorableClassList ensuringStorable;
    private final ODSConnectionType connectionType;
    private Thread currentThread;
    protected final String connectionName;
    
    public CachedConnectionParameters(CachedConnectionParameters ccp) {
      this.ensuringStorable = ccp.getEnsuringStorable();
      this.connectionType = ccp.getConnectionType();
      this.connectionName = ccp.getConnectionName();
    }
    
    public CachedConnectionParameters(ODSConnectionType connectionType, String connectionName, StorableClassList ensuringStorable) {
      this.ensuringStorable = ensuringStorable;
      this.connectionType = connectionType;
      this.connectionName = connectionName;
    }


    StorableClassList getEnsuringStorable() {
      return ensuringStorable;
    }
    
    
    ODSConnectionType getConnectionType() {
      return connectionType;
    }
    
    
    void setThread() {
      setThread(Thread.currentThread());
    }
    
    
    void setThread(Thread currentThread) {
      this.currentThread = currentThread; 
    }


    ThreadInformation getThreadInfo() {
      if (currentThread != null) {
        return new ThreadInformation(currentThread);
      } else {
        return null;
      }
    }

     public String getConnectionName() {
      return connectionName;
    }

  }


  // implements ODSConnection to possibly intercept calls or exceptions
  // although this has the rather ugly effect of getting all the sunlight from connection-logging
  private static class CachedConnection extends CachedConnectionParameters implements ODSConnection {

    private ODSConnection innerConnection;
    private List<String> ensuringTables;
    private static HashMap<String,Set<String>> unexpectedAccess = new HashMap<String,Set<String>>();
    
    CachedConnection(ODSConnection innerConnection, CachedConnectionParameters ccp) {
      super(ccp);
      this.innerConnection = innerConnection;
      this.ensuringTables = extractTableNames(ensuringStorable);
    }
    
    private List<String> extractTableNames(StorableClassList ensuringStorable) {
      List<String> tableNames = new ArrayList<String>(ensuringStorable.size());
      for( Class<? extends Storable<?>> storableClass : ensuringStorable ) {
        tableNames.add( Storable.getPersistable(storableClass).tableName() );
      }
      return Collections.unmodifiableList(tableNames);
    }

    private void checkAllowedStorable(Storable<?> storable) {
      if( storable == null ) {
        //ignorieren, soll anderswo Fehler erzeugen
      } else {
        if( ! ensuringStorable.contains( storable.getClass() ) ) {
          warnNotAllowedStorable( storable.getClass().getSimpleName() );
        }
      }
    }
    
    private <T extends Storable<?>> void checkAllowedStorable(Class<T> storableClass) {
      if( ! ensuringStorable.contains( storableClass ) ) {
        warnNotAllowedStorable( storableClass.getSimpleName() );
      }
    }
    
    private void checkAllowedStorable(String tableName) {
      if( ! ensuringTables.contains( tableName ) ) {
        warnNotAllowedStorable( tableName );
      }
    }
    
    private void warnNotAllowedStorable(String name) {
      Set<String> nameSet = unexpectedAccess.get(connectionName);
      if( nameSet == null ) {
        nameSet = new HashSet<String>();
        unexpectedAccess.put( connectionName, nameSet );
      }
      if( nameSet.add(name) ) {
        logger.warn( "unexpected access to storable \"" + name+"\" from dedicated connection \""+connectionName+"\"",
                   new Exception("called from") );
      }
    }


    public void commit() throws PersistenceLayerException {
      innerConnection.commit();
    }


    public void rollback() throws PersistenceLayerException {
      innerConnection.rollback();
    }


    public void closeConnection() throws PersistenceLayerException {
      innerConnection.closeConnection();
    }


    public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {
      checkAllowedStorable(storable);
      return innerConnection.persistObject(storable);
    }


    public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
      checkAllowedStorable(cmd.getTable());
      return innerConnection.prepareCommand(cmd);
    }

    public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
      checkAllowedStorable(query.getTable());
      return innerConnection.prepareQuery(query);
    }


    public int executeDML(PreparedCommand cmd, Parameter paras) throws PersistenceLayerException {
      checkAllowedStorable(cmd.getTable());
      return innerConnection.executeDML(cmd, paras);
    }


    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows) throws PersistenceLayerException {
      checkAllowedStorable(query.getTable());
      return innerConnection.query(query, parameter, maxRows);
    }
    
    
    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader) throws PersistenceLayerException {
      checkAllowedStorable(query.getTable());
      return innerConnection.query(query, parameter, maxRows, reader);
    }


    public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      checkAllowedStorable(storable);
      innerConnection.queryOneRow(storable);
    }


    public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      checkAllowedStorable(storable);
      innerConnection.queryOneRowForUpdate(storable);
    }


    public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
      checkAllowedStorable(query.getTable());
      return innerConnection.queryOneRow(query, parameter);
    }


    public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
      checkAllowedStorable(storable);
      return innerConnection.containsObject(storable);
    }


    public <T extends Storable> void persistCollection(Collection<T> storableCollection)
        throws PersistenceLayerException {
      for( T storable : storableCollection ) {
        checkAllowedStorable(storable);
      }
      innerConnection.persistCollection(storableCollection);
    }


    public <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {
      checkAllowedStorable(klass);
      return innerConnection.loadCollection(klass);
    }


    public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
      for( T storable : storableCollection ) {
        checkAllowedStorable(storable);
      }
      innerConnection.delete(storableCollection);
    }


    public <T extends Storable> void deleteOneRow(T toBeDeleted) throws PersistenceLayerException {
      checkAllowedStorable(toBeDeleted);
      innerConnection.deleteOneRow(toBeDeleted);
    }


    public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
      checkAllowedStorable(klass);
      innerConnection.deleteAll(klass);
    }


    public void setTransactionProperty(TransactionProperty property) {
      innerConnection.setTransactionProperty(property);
    }


    public PreparedCommand prepareCommand(Command cmd, boolean listenToPersistenceLayerChanges)
        throws PersistenceLayerException {
      checkAllowedStorable(cmd.getTable());
      return innerConnection.prepareCommand(cmd, listenToPersistenceLayerChanges);
    }


    public <E> PreparedQuery<E> prepareQuery(Query<E> query, boolean listenToPersistenceLayerChanges)
        throws PersistenceLayerException {
      checkAllowedStorable(query.getTable());
      return innerConnection.prepareQuery(query, listenToPersistenceLayerChanges);
    }

    @Deprecated
    public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(String sqlQuery, Parameter parameters,
                                                                       ResultSetReader<T> rsr, int cacheSize)
        throws PersistenceLayerException {
      String tableName = Query.parseSqlStringFindTable(sqlQuery);
      return getCursor(sqlQuery, tableName, parameters, rsr, cacheSize);
    }

    public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(String sqlQuery, String tableName, Parameter parameters,
                                                                       ResultSetReader<T> rsr, int cacheSize)
        throws PersistenceLayerException {
      checkAllowedStorable(tableName);
      return innerConnection.getCursor(sqlQuery, tableName, parameters, rsr, cacheSize);
    }

    public boolean isOpen() {
      return innerConnection.isOpen();
    }


    public ODSConnectionType getConnectionType() {
      return innerConnection.getConnectionType();
    }


    public void executeAfterCommit(Runnable runnable) {
      innerConnection.executeAfterCommit(runnable);
    }
    
    public void executeAfterCommitFails(Runnable runnable) {
      innerConnection.executeAfterCommitFails(runnable);
    }


    public void executeAfterRollback(Runnable runnable) {
      innerConnection.executeAfterRollback(runnable);
    }


    public void executeAfterClose(Runnable runnable) {
      innerConnection.executeAfterClose(runnable);
    }


    public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> storableClazz)
        throws PersistenceLayerException {
      checkAllowedStorable(storableClazz);
      innerConnection.ensurePersistenceLayerConnectivity(storableClazz);
    }

    public boolean isInTransaction() {
      return innerConnection.isInTransaction();
    }
    
    public void ensurePersistenceLayerConnectivity(List<Class<? extends Storable<?>>> storableClazz)
        throws PersistenceLayerException {
      for( Class<? extends Storable<?>> storable : storableClazz ) {
        checkAllowedStorable(storable);
      }
      innerConnection.ensurePersistenceLayerConnectivity(storableClazz);
    }


    public void executeAfterCommit(Runnable runnable, int priority) {
     innerConnection.executeAfterCommit(runnable, priority);
    }


    public void executeAfterCommitFails(Runnable runnable, int priority) {
      innerConnection.executeAfterCommitFails(runnable, priority);      
    }


    public void executeAfterRollback(Runnable runnable, int priority) {
      innerConnection.executeAfterRollback(runnable, priority); 
    }


    public void executeAfterClose(Runnable runnable, int priority) {
      innerConnection.executeAfterClose(runnable, priority); 
    }

    @Deprecated
    public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(String sqlQuery, Parameter parameters,
                                                                       ResultSetReader<T> rsr, int cacheSize,
                                                                       PreparedQueryCache cache)
                    throws PersistenceLayerException {
      String tableName = Query.parseSqlStringFindTable(sqlQuery);
      return getCursor(sqlQuery, tableName, parameters, rsr, cacheSize, cache);
    }

    public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(String sqlQuery, String tableName, Parameter parameters,
                                                                       ResultSetReader<T> rsr, int cacheSize,
                                                                       PreparedQueryCache cache)
                    throws PersistenceLayerException {
      checkAllowedStorable(tableName);
      return innerConnection.getCursor(sqlQuery, tableName, parameters, rsr, cacheSize, cache);
    }

    public void shareConnectionPools(ODSConnection conHistory) {
      throw new RuntimeException("Connection pool sharing not supported");
    }

  }


  // FIXME Duplicated code from ConnectionPool: cause private constructor
  public static class ThreadInformation {

    private String name;
    private StackTraceElement[] stacktrace;
    private State state;
    private int priority;
    private long id;


    public ThreadInformation(Thread t) {
      stacktrace = t.getStackTrace();
      name = t.getName();
      state = t.getState();
      priority = t.getPriority();
      id = t.getId();
    }


    public StackTraceElement[] getStackTrace() {
      return stacktrace;
    }


    public String getName() {
      return name;
    }


    public State getState() {
      return state;
    }


    public int getPriority() {
      return priority;
    }


    public long getId() {
      return id;
    }

  }


  public static class CachedConnectionInformation {

    private String identifier;
    private ODSConnectionType type;
    private List<String> tables;
    private ThreadInformation threadinfo;


    private CachedConnectionInformation(String identifier, CachedConnection con) {
      this.identifier = identifier;
      this.type = con.getConnectionType();
      if (Constants.RECORD_THREAD_INFO_CONNECTION_CACHE) {
        this.threadinfo = con.getThreadInfo();
      }
      if (con.getEnsuringStorable() != null && con.getEnsuringStorable().size() > 0) {
        tables = new ArrayList<String>();
        for (Class<? extends Storable<?>> clazz : con.getEnsuringStorable()) {
          Persistable persi = AnnotationHelper.getPersistable(clazz);
          if (persi == null) {
            tables.add(clazz.getSimpleName());
          } else {
            tables.add(persi.tableName());
          }

        }
      }
    }


    public String getIdentifier() {
      return identifier;
    }


    public ODSConnectionType getType() {
      return type;
    }


    public List<String> getTables() {
      return tables;
    }


    public ThreadInformation getThreadinfo() {
      return threadinfo;
    }


  }

}
