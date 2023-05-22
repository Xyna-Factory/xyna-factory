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

package com.gip.xyna.xnwh.xclusteringservices;



import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.DedicatedConnection;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.StorableClassList;



/**
 * WarehouseRetryExecutor f�hrt Executables mit PersistenceLayer-Zugriffen aus.
 * <p>
 * Dabei werden Retries gemacht, wenn die PersistenceLayerConnections nicht erhalten werden
 * konnten oder bei der Ausf�hrung XNWH_RetryTransactionException auftreten. 
 * Der WarehouseRetryExecutor k�mmert sich auch um das �ffnen und  Schlie�en der Connections,
 * wenn keine �bergeben wird.
 * <p>
 * Zur vereinfachten Verwendung gibt es einen WarehouseRetryExecutorBuilder, mit dem die 
 * ben�tigten Angaben einfach gesammelt werden k�nnen (Siehe unteres Beispiel). Die Defaults 
 * sind so gew�hlt, dass viele m�gliche Angaben meist entfallen k�nnen.   
 * <p>
 * Vor der ersten Ausf�hrung sollten alle Storables, mit denen gearbeitet wird, dem WarehouseRetryExecutor
 * bekanntgegeben werden, damit er �ber {@link ODSConnection#ensurePersistenceLayerConnectivity(java.util.List)}
 * alle Verbindungen zu den PersistenceLayern herstellen kann. Wird dies nicht gemacht, k�nnen 
 * DeadLocks entstehen, wenn Connections erst sp�ter dazugeholt werden m�ssen, aber nicht mehr
 * verf�gbar sind.
 * <p>
 * Um das Commit k�mmert sich der WarehouseRetryExecutor automatisch, es muss nicht in den Executables
 * gemacht werden:
 * <ul>
 * <li> Wenn eine normale Connection �bergeben wird, wird kein Commit gemacht, dies ist Aufgabe des 
 * Connection-Erstellers bzw. der �u�eren Transaktion.</li>
 * <li> Wenn eine dedizierte Connection verwendet wird, werden automatisch Commits (und Rollbacks) gemacht</li>
 * <li> Wenn keine Connection �bergeben wird, wird die interne Connection automatisch committet.</li>
 * <li> Das Commit/Rollback-Verhalten ist zus�tzlich noch konfigurierbar.</li>
 * </ul>
 * <p>
 * Wenn sich die �bergebene Connection bereits in einer Transaktion befindet, werden keine Retries gemacht,
 * wenn die Ausf�hrung des Executables scheitert.
 * <p>
 * Beispiel:
 * <pre>
   WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {
      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
            .backup(xose, getBackupCause(), con);
        //hier kein Commit mehr, Name muss noch angepasst werden. TODO
      }
    };
    
    WarehouseRetryExecutor.buildCriticalExecutor().
      connection(getConnection()). //WarehouseRetryExecutor macht Commit, wenn getConnection() null liefert!
      storable(OrderInstanceBackup.class).
      execute(wre);
   
 </pre>
 */
public class WarehouseRetryExecutor<T, E1 extends XynaException, E2 extends XynaException> {

  private static final Logger logger = CentralFactoryLogging.getLogger(WarehouseRetryExecutor.class);
  private int numberOfRetriesOnConnectionBroken;
  private int numberOfRetriesOnNoConnectionAvailable;
  private int counterConnectionBroken;
  private int counterNoConnectionAvailable;
  
  private boolean internalConnection;
  private ODSConnection con;
  private T executionResult;
  private PersistenceLayerException lastException;
  private boolean succeeded;
  private boolean transactionAlreadyStarted;
  
  public enum Commit {
    Always,
    OnlyIfOpened,
    Never;
  }
  public enum Rollback {
    Always,
    Never,
    OnError;
  }

  
  /**
   * @param numberOfRetriesOnConnectionBroken
   * @param numberOfRetriesOnNoConnectionAvailable
   */
  public WarehouseRetryExecutor(int numberOfRetriesOnConnectionBroken, int numberOfRetriesOnNoConnectionAvailable,
                                ODSConnection connection, ODSConnectionType connectionType) {
    this.numberOfRetriesOnConnectionBroken = numberOfRetriesOnConnectionBroken;
    this.numberOfRetriesOnNoConnectionAvailable = numberOfRetriesOnNoConnectionAvailable;
    if( connection != null ) {
      if( connectionType != null && connection.getConnectionType() != connectionType ) {
        throw new RuntimeException("Mismatch in connectionType "+connectionType+"and given connection with type "+connection.getConnectionType());
      }
      this.internalConnection = false;
      this.con = connection;
    } else {
      //Anlegen einer neuen Con, wenn diese nicht �bergeben wurde
      this.internalConnection = true;
      this.con = ODSImpl.getInstance().openConnection(connectionType);
    }
  }
  
  /**
   * Pr�fung, ob Connection verwendbar ist durch ensurePersistenceLayerConnectivity
   * @param ensuringStorables 
   * @return
   * @throws PersistenceLayerException 
   */
  private boolean checkConnectionIsUsable(StorableClassList ensuringStorables) throws PersistenceLayerException {
    try {
      con.ensurePersistenceLayerConnectivity(ensuringStorables);
      return true;
    } catch (XNWH_RetryTransactionException e) {
      if( logger.isDebugEnabled() ) {
        logger.debug("Tried to ensurePersistenceLayerConnectivity: "+e.getMessage() );
      }
      lastException = e;
      if( e.getCause() instanceof NoConnectionAvailableException ) {
        NoConnectionAvailableException ncae = (NoConnectionAvailableException) e.getCause();
        if( ncae.getReason() == NoConnectionAvailableException.Reason.PoolExhausted ) {
          ++counterNoConnectionAvailable;
          return false; //con nicht verwendbar
        } else {
          ++counterConnectionBroken;
          return false; //con nicht verwendbar
        }
      } else {
        ++counterConnectionBroken;
        return false; //con nicht verwendbar
      }
    } catch ( PersistenceLayerException e ) {
      lastException = e;
      throw e; //con nicht verwendbar; keinen Retry versuchen, da keine XNWH_RetryTransactionException
    }
  }

  /**
   * Verwendung der Connection: Ausf�hren von executable
   * @param commitMode 
   * @return true, wenn executable ausgef�hrt wurde und keine XNWH_RetryTransactionException/PersistenceLayerException auftrat
   * @throws E1 von executable definiert
   * @throws E2 von executable definiert
   */
  private boolean useConnection( WarehouseRetryExecutable<T, E1, E2> executable, Commit commitMode) throws E1, E2, PersistenceLayerException, InterruptedException {
    try {
      executionResult = executable.executeAndCommit(con);
      commit(commitMode);
      succeeded = true;
      return true;
    } catch ( XNWH_RetryTransactionException e ) {
      if( transactionAlreadyStarted ) {
        //�u�ere Transaktion, daher diesen Fehler propagieren
        lastException = e;
        throw e; //keinen Retry versuchen, da �u�ere Transaktion wahrscheinlich auch gesch�digt
      } else {
        if (!maxRetriesReached(false)) {
          con.rollback(); //immer rollback auf connection bei retry
        }
        //keine �u�ere Transaktion, daher kann lokale Transaktion wiederholt werden
        lastException = e;
        ++counterConnectionBroken;
        return false;
      }
    } catch (PersistenceLayerException e) {
      lastException = e;
      throw e; //keinen Retry versuchen, da keine XNWH_RetryTransactionException
    } catch ( XynaException e ) {
      succeeded = true; //Fehler lag nicht am WarehouseRetryExecutor
      throwAsE1(e);
      throwAsE2(e);
      throw new RuntimeException(e); //kann eigentlich nicht auftreten
    }
  }
  
  private void commit(Commit commitMode) throws PersistenceLayerException {
    switch( commitMode ) {
      case Always:
        con.commit();
        break;
      case OnlyIfOpened:
        if( internalConnection ) {
          con.commit();
        }
        break;
      case Never:
        break;
      default:
        logger.warn("Unexpected commitMode "+commitMode);
    }
  }

  private void throwAsE1(XynaException e) throws E1 {
    try {
      @SuppressWarnings("unchecked")
      E1 cast = (E1)e;
      throw cast;
    } catch (ClassCastException cce) { }
  }
  private void throwAsE2(XynaException e) throws E2 {
    try {
      @SuppressWarnings("unchecked")
      E2 cast = (E2)e;
      throw cast;
    } catch (ClassCastException cce) { }
  }

  private T getExecutionResult() {
    return executionResult;
  }

  private PersistenceLayerException lastException() {
    return lastException;
  }

  private SleepCounter sleepCnt;

  private boolean maxRetriesReached(boolean sleep) throws InterruptedException {
    
    boolean maxRetriesReached = counterConnectionBroken > numberOfRetriesOnConnectionBroken
        || counterNoConnectionAvailable > numberOfRetriesOnNoConnectionAvailable;
    
    if (logger.isDebugEnabled()) {
      logger.debug("maxRetriesReached? connectionBroken="
          + (counterConnectionBroken > numberOfRetriesOnConnectionBroken) + ", noConnectionAvailable="
          + (counterNoConnectionAvailable > numberOfRetriesOnNoConnectionAvailable)+" -> "+maxRetriesReached);
    }
    if( maxRetriesReached ) {
      return true; //maxRetriesReached
    }
    if( XynaFactory.getInstance().isShuttingDown() ) {
      return true; //maxRetries nicht erreicht, aber wegen Factory-Shutdown ist weiteres Warten nicht sinnvoll
    }
    
    //maxRetries nicht erreicht
    if ( sleep) {
      //warten, bevor man erneut probiert
      if (sleepCnt == null) {
        sleepCnt = new SleepCounter(50, 5000, 1, TimeUnit.MILLISECONDS, false);
      }
      sleepCnt.sleep();
    }
    return false;
  }


  private void closeConnection(Rollback rollbackMode, boolean error) {
    if( con == null ) {
      //nichts zu tun, sollte aber auch nicht auftreten
    } else {
      try {
        if (internalConnection ) {
          //intern ge�ffnete Connection wird nun geschlossen
          con.closeConnection();
        } else {
          if( rollbackMode == Rollback.Always || (error && rollbackMode == Rollback.OnError) ) {
            //auf �bergebene Connection wird nun rollback gemacht
            con.rollback();
          }
        }
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close or rollback connection", e);
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("WarehouseRetryExecutor");
    if( succeeded ) {
      sb.append(" succeeded" );
    } else {
      sb.append(" failed" );
      if( lastException == null ) {
        sb.append(" or did not run");
      }
    }
    if(counterConnectionBroken != 0 || counterNoConnectionAvailable != 0 ) {
      sb.append(" after " ).
      append(counterConnectionBroken).append(" retries due to broken connections and " ).
      append(counterNoConnectionAvailable).append(" retries due to unavailable connections");
    }
    if( succeeded ) {
      sb.append(" with result ");
      if( executionResult instanceof Collection ) {
        sb.append( executionResult.getClass().getSimpleName() ).append(" with ").append(((Collection<?>)executionResult).size()).append(" entries");
      } else {
        if( executionResult == null ) {
          sb.append(executionResult);
        } else {
          sb.append( executionResult.getClass().getSimpleName() );
        }
      }
    } else {
      if( lastException != null ) {
        sb.append(" with exception \"").append( lastException.getMessage() ).append("\"");
        if( lastException.getCause() != null ) {
          sb.append(" caused by \"").append( lastException.getCause().getMessage() ).append("\"");
        }
      }
    }
    return sb.toString();
  }

  public T execute( WarehouseRetryExecutable<T, E1, E2> executable, 
                    StorableClassList ensuringStorables, 
                    Commit commitMode, Rollback rollbackMode ) throws PersistenceLayerException, E1, E2 {
    sleepCnt = null;
    boolean logDiagnostics = logger.isTraceEnabled();
    transactionAlreadyStarted = con.isInTransaction();
    long start = logDiagnostics ? System.currentTimeMillis() : 0;
    boolean executionSucceeded = false;
    try {
      do {
        boolean conIsUsable = checkConnectionIsUsable(ensuringStorables); //ist con verwendbar?
        if( conIsUsable ) { //Connection ist valide, daher Task ausf�hren
          executionSucceeded = useConnection(executable,commitMode); //hat Task fehlerfrei geklappt? Wirft Task-definierte Exceptions!
          if( executionSucceeded ) {
            return getExecutionResult(); //Ergebnis des erfolreichen Tasks zur�ckgeben
          }
        }
      } while( ! maxRetriesReached(true) ); //wiederholen, falls es Probleme gab
      throw lastException(); //definierte Retries �berschritten, daher letzte Exception weiterwerfen
    } catch (InterruptedException e) {
      if (lastException != null) {
        throw new XNWH_GeneralPersistenceLayerException("Interrupted waiting for retries after exception.", lastException);
      } else {
        throw new RuntimeException("Interrupted waiting for retries");
      }
    } finally {
      closeConnection(rollbackMode, !executionSucceeded); //intern ge�ffnete Connections schlie�en, externe Con rollbacken
      if( logDiagnostics ) {
        String wreString = this.toString();
        long stop = System.currentTimeMillis();
        logger.trace( "WarehouseRetryExecutor took "+(stop-start)+" ms: "+wreString);
      }
    }
  }
  
  
  public static class WarehouseRetryExecutorBuilder {

    private int numberOfRetriesOnConnectionBroken;
    private int numberOfRetriesOnNoConnectionAvailable;
    private ODSConnectionType connectionType = ODSConnectionType.DEFAULT;
    private ODSConnection connection = null;
    private StorableClassList ensuringStorables;
    private Rollback rollbackMode = Rollback.Never;
    private Commit commitMode = Commit.Always;
    private DedicatedConnection dedicatedConnection;
    
    public WarehouseRetryExecutorBuilder(int numberOfRetriesOnConnectionBroken,
                                         int numberOfRetriesOnNoConnectionAvailable) {
      this.numberOfRetriesOnConnectionBroken = numberOfRetriesOnConnectionBroken;
      this.numberOfRetriesOnNoConnectionAvailable = numberOfRetriesOnNoConnectionAvailable;
    }

    /**
     * Setzt den ODSConnectionType, Default ist ODSConnectionType.DEFAULT
     * Der WarehouseRetryExecutor k�mmert sich automatisch ums �ffnen und
     * Schlie�en der Connection.
     * @param connectionType
     * @return
     */
    public WarehouseRetryExecutorBuilder connection(ODSConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }
    /**
     * �bernimmt connectionType aus �bergebener connection, bei null ODSConnectionType.DEFAULT
     * Commit.OnlyIfOpened: Es wird ein Commit ausgef�hrt, wenn keine Connection �bergeben wird.
     * @param connection
     * @return
     */
    public WarehouseRetryExecutorBuilder connection(ODSConnection connection) {
      if( connection != null ) {
        this.connectionType = connection.getConnectionType();
        this.connection = connection;
      }
      this.commitMode = Commit.OnlyIfOpened;
      this.rollbackMode = Rollback.Never; //macht entweder �u�ere Transaktion oder die lokale Connection wird eh geschlossen
      return this;
    }
    

    /**
     * Holt sich dedicated Connection
     * Es wird am Ende immer ein Rollback ausgef�hrt, da die Connection ja nicht geschlossen wird
     * @param dedicatedConnection
     * @return
     */
    public WarehouseRetryExecutorBuilder connectionDedicated(DedicatedConnection dedicatedConnection) {
      this.commitMode = Commit.Always;
      this.rollbackMode = Rollback.OnError; //dedicated Connection soll Transaktion verlieren analog zum Close
      this.dedicatedConnection = dedicatedConnection;
      return this;
    }
   
    /**
     * Setzt ein Storable, f�r welches die Connection vor Ausf�hrung des Executables gepr�ft wird.
     * Diese Methode kann mehrfach gerufen werden, alle Storables werden gesammelt.
     * @param storable
     * @return
     */
    public WarehouseRetryExecutorBuilder storable(Class<? extends Storable<?>> storable) {
      if( ensuringStorables == null ) {
        ensuringStorables = new StorableClassList(storable);
      } else {
        ensuringStorables = StorableClassList.combine( ensuringStorables, storable);
      }
      return this;
    }
    /**
     * Setzt alle zu pr�fenden Storables
     * @param storables
     * @return
     */
    public WarehouseRetryExecutorBuilder storables(StorableClassList storables) {
      if( ensuringStorables == null ) {
        ensuringStorables = storables;
      } else {
        ensuringStorables = StorableClassList.combine( ensuringStorables, storables);
      }
      return this;
    }
    
    public <T, E1 extends XynaException, E2 extends XynaException> T execute(WarehouseRetryExecutable<T,E1,E2> executable) throws E1, E2, PersistenceLayerException {
      return executeInternal(executable);
    }
    
    public <T> T execute(WarehouseRetryExecutableNoException<T> executable) throws PersistenceLayerException {
      try {
        return executeInternal(new WarehouseRetryExecutableNoExceptionImpl<T>(executable));
      } catch (TempException e) {
        throw new RuntimeException(e);
      }
    }

    public <T, E1 extends XynaException> T execute(WarehouseRetryExecutableOneException<T,E1> executable) throws E1, PersistenceLayerException {
      try {
        return executeInternal(new WarehouseRetryExecutableOneExceptionImpl<T,E1>(executable));
      } catch (TempException e) {
        throw new RuntimeException(e);
      }
    }
    
    public <E1 extends XynaException> void execute(WarehouseRetryExecutableNoResultOneException<E1> executable) throws E1, PersistenceLayerException {
      try {
        executeInternal(new WarehouseRetryExecutableNoResultOneExceptionImpl<E1>(executable));
      } catch (TempException e) {
        throw new RuntimeException(e);
      }
    }
   
    public void execute(WarehouseRetryExecutableNoResult executable) throws PersistenceLayerException {
      try {
        executeInternal(new WarehouseRetryExecutableNoResultImpl(executable));
      } catch (TempException e) {
        throw new RuntimeException(e);
      }
    }
    
    private <T, E1 extends XynaException, E2 extends XynaException> T executeInternal(WarehouseRetryExecutable<T,E1,E2> executable) throws E1, E2, PersistenceLayerException {
      if( dedicatedConnection != null ) {
        int retries = numberOfRetriesOnConnectionBroken;
        connection = CentralComponentConnectionCache.getConnectionFor(dedicatedConnection, retries);
        connectionType = connection.getConnectionType();
      }
      
      WarehouseRetryExecutor<T,E1,E2> wre = new WarehouseRetryExecutor<T,E1,E2>(
          numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable, connection, connectionType );
      if( ensuringStorables == null ) {
        throw new IllegalArgumentException( "storables must be set to ensure connectivity");
      }
      return wre.execute(executable, ensuringStorables, commitMode, rollbackMode );
    }

    /**
     * Soll ein Commit auf der Connection gemacht werden?
     * Default ist Commit.Always
     * @param mode
     * @return
     */
    public WarehouseRetryExecutorBuilder commit(Commit mode) {
      this.commitMode = mode;
      return this;
    }
    
    /**
     * Soll ein Rollback auf der externen Connection gemacht werden, nachdem das Executable ausgef�hrt wurde?
     * Default ist Rollback.Never
     * @param rollback
     * @return
     */
    public WarehouseRetryExecutorBuilder rollback(Rollback rollback) {
      this.rollbackMode = rollback;
      return this;
    }
    
    public boolean hasConnection() {
      return connection != null;
    }
    
    public boolean hasConnectionType() {
      return connectionType != null;
    }

  }
  
  private static class TempException extends XynaException {
    private static final long serialVersionUID = 1L;
    public TempException() {
      super("temp");
    }
  }

  private static class WarehouseRetryExecutableNoExceptionImpl<T> implements WarehouseRetryExecutable<T, TempException, TempException> {

    private WarehouseRetryExecutableNoException<T> executable;

    public WarehouseRetryExecutableNoExceptionImpl(WarehouseRetryExecutableNoException<T> executable) {
      this.executable = executable;
    }

    public T executeAndCommit(ODSConnection con) throws PersistenceLayerException, TempException, TempException {
      return executable.executeAndCommit(con);
    }
    
  }

  private static class WarehouseRetryExecutableOneExceptionImpl<T, E1 extends XynaException> implements WarehouseRetryExecutable<T, E1, TempException> {

    private WarehouseRetryExecutableOneException<T,E1> executable;

    public WarehouseRetryExecutableOneExceptionImpl(WarehouseRetryExecutableOneException<T,E1> executable) {
      this.executable = executable;
    }

    public T executeAndCommit(ODSConnection con) throws PersistenceLayerException, E1, TempException {
      return executable.executeAndCommit(con);
    }
    
  }

  private static class WarehouseRetryExecutableNoResultOneExceptionImpl<E1 extends XynaException> implements WarehouseRetryExecutable<Object, E1, TempException> {

    private WarehouseRetryExecutableNoResultOneException<E1> executable;

    public WarehouseRetryExecutableNoResultOneExceptionImpl(WarehouseRetryExecutableNoResultOneException<E1> executable) {
      this.executable = executable;
    }

    public Object executeAndCommit(ODSConnection con) throws PersistenceLayerException, E1, TempException {
      executable.executeAndCommit(con);
      return null;
    }
    
  }
  
  private static class WarehouseRetryExecutableNoResultImpl implements WarehouseRetryExecutable<Object, TempException, TempException> {

    private WarehouseRetryExecutableNoResult executable;

    public WarehouseRetryExecutableNoResultImpl(WarehouseRetryExecutableNoResult executable) {
      this.executable = executable;
    }

    public Object executeAndCommit(ODSConnection con) throws PersistenceLayerException, TempException, TempException {
      executable.executeAndCommit(con);
      return null;
    }
    
  }

  

  
  /**
   * Erzeugen eines WarehouseRetryExecutorBuilder f�r kritische Ausf�hrungen: 
   * Wiedeholtes Connection-Erzeugen solange, bis Connection verf�gbar ist
   * @return
   */
  public static WarehouseRetryExecutorBuilder buildCriticalExecutor() {
    return new WarehouseRetryExecutorBuilder(Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                             Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL
                                             );
  }
  
  public static WarehouseRetryExecutorBuilder buildMinorExecutor() {
    return new WarehouseRetryExecutorBuilder(Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                             Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__MINOR
                                             );
  }
  
  public static WarehouseRetryExecutorBuilder buildUserInteractionExecutor() {
    return new WarehouseRetryExecutorBuilder(Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                             Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__USERINTERACTION
                                             );
  }
  
  public static WarehouseRetryExecutorBuilder buildExecutor(int numberOfRetriesOnConnectionBroken,
                                                            int numberOfRetriesOnNoConnectionAvailable) {
    return new WarehouseRetryExecutorBuilder(numberOfRetriesOnConnectionBroken,numberOfRetriesOnNoConnectionAvailable);
  }
 
  
  
  
  
  
  
  /**
   * 5 Aufrufe
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static <T, E1 extends XynaException, E2 extends XynaException> T executeWithRetries(WarehouseRetryExecutable<T, E1, E2> executable,
                                                                                             ODSConnectionType connectionType,
                                                                                             int numberOfRetriesOnConnectionBroken,
                                                                                             int numberOfRetriesOnNoConnectionAvailable,
                                                                                             StorableClassList ensuringStorables)
      throws PersistenceLayerException, E1, E2 {
    return buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).storables(ensuringStorables).
    execute(executable);
  }

  /**
   * 0 Aufrufe.
   *
   * @param <T> the generic type
   * @param <E1> the generic type
   * @param <E2> the generic type
   * @param executable the executable
   * @param connectionType the connection type
   * @param numberOfRetriesOnConnectionBroken the number of retries on connection broken
   * @param numberOfRetriesOnNoConnectionAvailable the number of retries on no connection available
   * @param connection the connection
   * @param ensuringStorables the ensuring storables
   * @return the t
   * @throws PersistenceLayerException the persistence layer exception
   * @throws E1 the e1
   * @throws E2 the e2
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static <T, E1 extends XynaException, E2 extends XynaException> T executeWithRetries(WarehouseRetryExecutable<T, E1, E2> executable,
                                                                                             ODSConnectionType connectionType,
                                                                                             int numberOfRetriesOnConnectionBroken,
                                                                                             int numberOfRetriesOnNoConnectionAvailable,
                                                                                             final ODSConnection connection,
                                                                                             StorableClassList ensuringStorables)
      throws PersistenceLayerException, E1, E2 {
    return buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).connection(connection).storables(ensuringStorables).
    execute(executable);
  }


  /**
   * 12 Aufrufe
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static void executeWithRetriesNoException(final WarehouseRetryExecutableNoResult executable,
                                                   ODSConnectionType connectionType,
                                                   int numberOfRetriesOnConnectionBroken,
                                                   int numberOfRetriesOnNoConnectionAvailable,
                                                   StorableClassList ensuringStorables)
      throws PersistenceLayerException {
    buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).storables(ensuringStorables).
    execute(executable);
  }


  /**
   * 0 Aufrufe
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static void executeWithRetriesNoException(final WarehouseRetryExecutableNoResult executable,
                                                   ODSConnectionType connectionType,
                                                   int numberOfRetriesOnConnectionBroken,
                                                   int numberOfRetriesOnNoConnectionAvailable,
                                                   ODSConnection connection,
                                                   StorableClassList ensuringStorables )
      throws PersistenceLayerException {
    buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).connection(connection).storables(ensuringStorables).
    execute(executable);
  }

  
  /**
   * 6 Aufrufe
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static <T extends XynaException> void executeWithRetriesOneException(final WarehouseRetryExecutableNoResultOneException<T> executable,
                                                                              ODSConnectionType connectionType,
                                                                              int numberOfRetriesOnConnectionBroken,
                                                                              int numberOfRetriesOnNoConnectionAvailable,
                                                                              StorableClassList ensuringStorables)
      throws PersistenceLayerException, T {
    buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).storables(ensuringStorables).
    execute(executable);
  }
  
  /**
   * 0 Aufrufe.
   *
   * @param <E1> the generic type
   * @param executable the executable
   * @param connectionType the connection type
   * @param numberOfRetriesOnConnectionBroken the number of retries on connection broken
   * @param numberOfRetriesOnNoConnectionAvailable the number of retries on no connection available
   * @param connection the connection
   * @param ensuringStorables the ensuring storables
   * @throws PersistenceLayerException the persistence layer exception
   * @throws E1 the e1
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static <E1 extends XynaException> void executeWithRetriesOneException(final WarehouseRetryExecutableNoResultOneException<E1> executable,
                                                                              ODSConnectionType connectionType,
                                                                              int numberOfRetriesOnConnectionBroken,
                                                                              int numberOfRetriesOnNoConnectionAvailable,
                                                                              ODSConnection connection,
                                                                              StorableClassList ensuringStorables)
      throws PersistenceLayerException, E1 {
    buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).connection(connection).storables(ensuringStorables).
    execute(executable);
  }
  
  /**
   * 6 Aufrufe
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static <T> T executeWithRetriesNoException(final WarehouseRetryExecutableNoException<T> executable,
                                                    ODSConnectionType connectionType,
                                                    int numberOfRetriesOnConnectionBroken,
                                                    int numberOfRetriesOnNoConnectionAvailable,
                                                    StorableClassList ensuringStorables)
      throws PersistenceLayerException {
    return buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).storables(ensuringStorables).
    execute(executable);
  }
    
  /**
   * 0 Aufruf
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static <T> T executeWithRetriesNoException(final WarehouseRetryExecutableNoException<T> executable,
                                                    ODSConnectionType connectionType,
                                                    int numberOfRetriesOnConnectionBroken,
                                                    int numberOfRetriesOnNoConnectionAvailable,
                                                    ODSConnection connection,
                                                    StorableClassList ensuringStorables)
      throws PersistenceLayerException {
    return buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).connection(connection).storables(ensuringStorables).
    execute(executable);
  }  
  
  /**
   * 4 Aufrufe
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static <T, E1 extends XynaException> T executeWithRetriesOneException(
                                                                               final WarehouseRetryExecutableOneException<T, E1> executable,
                                                                               ODSConnectionType connectionType,
                                                                               int numberOfRetriesOnConnectionBroken,
                                                                               int numberOfRetriesOnNoConnectionAvailable,
                                                                               StorableClassList ensuringStorables)
      throws PersistenceLayerException, E1 {
    return buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).storables(ensuringStorables).
    execute(executable);
  }
  
  /**
   * 0 Aufrufe
   * @deprecated Use WarehouseRetryExecutorBuilder
   */
  @Deprecated
  public static <T, E1 extends XynaException> T executeWithRetriesOneException(
                                                                                 final WarehouseRetryExecutableOneException<T, E1> executable,
                                                                                 ODSConnectionType connectionType,
                                                                                 int numberOfRetriesOnConnectionBroken,
                                                                                 int numberOfRetriesOnNoConnectionAvailable,
                                                                                 ODSConnection connection,
                                                                                 StorableClassList ensuringStorables)
        throws PersistenceLayerException, E1 {
    return buildExecutor(numberOfRetriesOnConnectionBroken, numberOfRetriesOnNoConnectionAvailable).
    connection(connectionType).connection(connection).storables(ensuringStorables).
    execute(executable);
  }

}
