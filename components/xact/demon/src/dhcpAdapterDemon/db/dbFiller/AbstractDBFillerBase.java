/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon.db.dbFiller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.StatementCache;
import com.gip.xyna.utils.db.failover.Failover;
import com.gip.xyna.utils.db.failover.FailoverConnectionLifecycle;
import com.gip.xyna.utils.db.failover.FailoverDBConnectionData;
import com.gip.xyna.utils.snmp.SnmpAccessData;

import dhcpAdapterDemon.db.SQLUtilsNdcLogger;
import dhcpAdapterDemon.types.State;
import queue.SynchronizedRingBuffer;

public abstract class AbstractDBFillerBase<Data> implements DBFiller<Data> {
  //final static NdcLogger defaultLogger = new NdcLogger(AbstractDBFillerBase.class.getName());
  final static Logger defaultLogger = Logger.getLogger(AbstractDBFillerBase.class);
  
  protected SynchronizedRingBuffer<Data> ringBuffer;
  private Object mutex = new Object();
  protected DataProcessor<Data> dataProcessor;
  private ArrayList<String> statementCache = new ArrayList<String>();
  private volatile boolean running;
  protected SQLUtils sqlUtils;
  protected Logger logger = defaultLogger;
  private SQLUtilsNdcLogger sqlUtilsLogger;
  private FailoverDBConnectionData connectionData;
  private String name;
  private int rebuildCounter;
  private int failoverCounter;
  private int deadlockCounter;
  private Failover failover;
  private long waitReconnect;
  private Throwable lastException;
  private long lastExceptionDate;
  private TrapSender trapSender;
  
  protected long lastDataWrite = System.currentTimeMillis();
  private long dbcloseTimerInMs = 0; 
  protected boolean leaselog=false;

  /**
   * @param dbFillerData
   * @param name
   */
  public AbstractDBFillerBase(DBFillerData dbFillerData, String name) {
    connectionData = dbFillerData.getFailoverConnectionData();
    this.waitReconnect = dbFillerData.getWaitReconnect()*1000L;
    this.ringBuffer = new SynchronizedRingBuffer<Data>(dbFillerData.getCapacity());
    this.name = name;
    sqlUtilsLogger = new SQLUtilsNdcLogger(logger);
    this.failover= connectionData.createNewFailover( new FailoverConnectionLifecycleImpl<Data>(this)  );
    
    try
    {
      dbcloseTimerInMs = Long.valueOf(DemonProperties.getProperty("db.leaselog.dbclosetimerinms","0"));
    }
    catch(Exception e)
    {
      logger.warn("Timer for OracleDB not defined!");
    }
    //FIXME mehrere Traps, verschiedene OIDs
    trapSender = new TrapSender( name+"TrapSender", DemonProperties.getSnmpAccessData(SnmpAccessData.VERSION_2c, "trap.leaselog" ));
  }

  
  
  public AbstractDBFillerBase(DBFillerData dbFillerData, String name, boolean isLeaselog) {
    connectionData = dbFillerData.getFailoverConnectionData();
    this.waitReconnect = dbFillerData.getWaitReconnect()*1000L;
    this.ringBuffer = new SynchronizedRingBuffer<Data>(dbFillerData.getCapacity());
    this.name = name;
    sqlUtilsLogger = new SQLUtilsNdcLogger(logger);
    this.failover= connectionData.createNewFailover( new FailoverConnectionLifecycleImpl<Data>(this)  );
    this.leaselog=isLeaselog;
    
    try
    {
      dbcloseTimerInMs = Long.valueOf(DemonProperties.getProperty("db.leaselog.dbclosetimerinms","0"));
    }
    catch(Exception e)
    {
      logger.warn("Timer for OracleDB not defined!");
    }
    //FIXME mehrere Traps, verschiedene OIDs
    trapSender = new TrapSender( name+"TrapSender", DemonProperties.getSnmpAccessData(SnmpAccessData.VERSION_2c, "trap.leaselog" ));
  }

  
  public static class RebuildConnectionException extends RuntimeException {
    public RebuildConnectionException(String message) {
      super(message);
    }

    private static final long serialVersionUID = 1L;
  }   
  
  private static class FailoverConnectionLifecycleImpl<Data> implements FailoverConnectionLifecycle {

    private AbstractDBFillerBase<Data> abstractDBFillerBase;

    public FailoverConnectionLifecycleImpl(
        AbstractDBFillerBase<Data> abstractDBFillerBase) {
      this.abstractDBFillerBase = abstractDBFillerBase;
    }

    /* (non-Javadoc)
     * @see com.gip.xyna.utils.db.failover.FailoverConnectionLifecycle#initialize(com.gip.xyna.utils.db.SQLUtils)
     */
    public void initialize(SQLUtils sqlUtils) {
      if( sqlUtils != null ) {
        sqlUtils.setStatementCache( new StatementCache() );
        for( String statement : abstractDBFillerBase.getStatementCache() ) {
          sqlUtils.cacheStatement( statement );
        }
        abstractDBFillerBase.getDataProcessor().initialize( sqlUtils );
      }
    }

    /* (non-Javadoc)
     * @see com.gip.xyna.utils.db.failover.FailoverConnectionLifecycle#finalize(com.gip.xyna.utils.db.SQLUtils)
     */
    public void finalize(SQLUtils sqlUtils) {
      abstractDBFillerBase.incrementFailoverCounter();
      abstractDBFillerBase.rollback();
    }
    
    
  }
  
  public void add(Data data) {
    boolean accepted = ringBuffer.offer(data);
    while( !accepted ) {
      Data rejected = ringBuffer.poll();
      dataProcessor.state( rejected, State.REJECTED );
      accepted = ringBuffer.offer(data);
    }
    dataProcessor.state( data, State.REQUESTED );
    synchronized( mutex ) {
      mutex.notify();
    }
  }
  
  private void incrementFailoverCounter() {
    ++failoverCounter;
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DBFiller#getWaiting()
   */
  public int getWaiting() {
    return ringBuffer.size();
  }
  
  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DBFiller#getCapacity()
   */
  public int getCapacity() {
    return ringBuffer.capacity();
  }
 
  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DBFiller#cacheStatement(java.lang.String)
   */
  public void cacheStatement(String statement) {
    statementCache.add(statement);
  }
  
  /**
   * @return
   */
  private ArrayList<String> getStatementCache() {
    return statementCache;
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DBFiller#setDataProcessor(dhcpAdapterDemon.db.dbFiller.DataProcessor)
   */
  public void setDataProcessor(DataProcessor<Data> dataProcessor) {
    this.dataProcessor = dataProcessor;
  }
  /**
   * @return
   */
  private DataProcessor<Data> getDataProcessor() {
    return dataProcessor;
  }


  public void run() {
    
    if( sqlUtils == null ) {
      sqlUtils = failover.createSQLUtils(sqlUtilsLogger);
      if( sqlUtils == null ) {
        logger.error( getName()+" not initialized");
        trapSender.sendTrap("Connection initialization failed");
      }
    }
    
    running = true;
    while( running ) {
      
      try {
        //if(leaselog)logger.info("(Leaselog) Checking connection and connecting if necessary ..."); 
        sqlUtils = failover.checkAndRecreate(sqlUtils, sqlUtilsLogger);
        //falls keine DB-Connection existiert, muss diese neu aufgebaut werden
        if( sqlUtils == null ) {
          //Neubau der Connection nur selten durchführen, da dies viel Last 
          //verursacht und nicht damit zu rechnen ist, dass der Fehler 
          //kurzfristig behebbar ist.
          trapSender.sendTrap("Connection CheckAndRecreate failed");
          try {
            Thread.sleep( waitReconnect );
          } catch (InterruptedException e) {
            logger.error( "WaitReconnect interupted", e);
          }
          continue; //Rebuild hat nicht geklappt, daher nochmal probieren
        }
        try {
          //eigentliche Bearbeitung
          processData();
          trapSender.stop();
        } catch( RebuildConnectionException e ) {
          //ein schwerer Fehler ist aufgetreten, der nur durch Neubau 
          //der Connection behoben werden kann.
          rebuildConnection(e.getMessage());
        }

      } catch( RuntimeException e ) {
        logger.error( "Severe Failure, trying to keep "+getName()+" running", e);
      }
    }
    try {
      if( sqlUtils != null ) {
        commit();
        sqlUtils.closeConnection();
      }
    } catch( RuntimeException e ) {
      logger.error( "Final commit failed", e);
    }
    finally {
      NDC.remove();
    }
  }
      
  protected abstract void processData();
  protected abstract void commit();
  protected abstract void rollback();
  
  /**
   * @return
   */
  protected Data tryGetData() {
    //1. Versuch etwas auszulesen
    Data entry = readEntryFromRingBuffer();
    if( entry != null ) {
      return entry;
    }
    //warten bis Eintrag existiert
    entry = waitForNewData();
    return entry;
  }
  
  /**
   * liest den nächsten Datensatz aus dem RingBuffer
   * @return
   */
  protected Data readEntryFromRingBuffer() {
    return ringBuffer.poll();
  }

  protected Data waitForNewData() {
    //warten anhand des Mutex
    synchronized( mutex ) {
      //warten, bis mutex notifiziert wird
      while( ringBuffer.size() == 0 && running ) { //über running ist Terminierung möglich
        try {
          if(!leaselog)
          {
            mutex.wait();
          }
          else
          {
            mutex.wait(10000);
            if(dbcloseTimerInMs>0) // nur falls gesetzt
            {
              if(System.currentTimeMillis()-lastDataWrite>dbcloseTimerInMs)
              {
                if(!sqlUtils.getConnection().isClosed())
                {
                  logger.info("Closing Connection to OracleDB (Leaselog), no packet written for "+(dbcloseTimerInMs / 1000) +" seconds.");
                  sqlUtils.closeConnection();
                }
              }
            }
          }
        } catch (InterruptedException e) {
          //nicht schlimm, einfach nächste Schleife durchlaufen
        }
        catch (SQLException e) {
          logger.warn("SQL Exception during waitForNewData: ",e);
          
        }
      }
    }
    //Wartezeit ist abgelaufen, nun sollten Daten vorhanden sein
    return readEntryFromRingBuffer();
  }

  protected void rebuildConnection(String cause ) {
    logger.info( "Rebuild connection to "+failover.getConnectionData()+" due to "+cause );
    trapSender.sendTrap("Rebuild connection due to "+cause);
    ++rebuildCounter;
    sqlUtils = failover.recreateSQLUtils(sqlUtils, sqlUtilsLogger);
    if( sqlUtils == null ) {
      String message = "unknown cause";
      if( sqlUtilsLogger != null && sqlUtilsLogger.getLastException() != null && sqlUtilsLogger.getLastException().getMessage() != null ) {
        message = sqlUtilsLogger.getLastException().getMessage();
      }
      logger.error( "Rebuild connection to "+connectionData + " failed: " + message ); 
    }
  }    

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DBFiller#terminate()
   */
  public ArrayList<Data> terminate() {
    running = false;
    synchronized( mutex ) {
      mutex.notify();
    }
    ArrayList<Data> data = new ArrayList<Data>(ringBuffer.size());
    while( ! ringBuffer.isEmpty() ) {
      data.add( ringBuffer.poll() );
    }
    return data;
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DBFiller#setLogger(org.apache.log4j.Logger)
   */
  public void setLogger(Logger logger) {
    this.logger = logger;
  }
  
  /**
   * @return
   */
  public String getName() {
    return name;
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DBFiller#getStatus()
   */
  public Status getStatus() {
    if( running ) {
      return Status.RUNNING;
    } else {
      if( sqlUtils == null ) {
        return Status.NOT_CONNECTED;
      } else {
        return Status.CONNECTED;
      }
    }
  }
  
  
  /**
   * Logt den Status
   */
  private String getLogStatus() {
    StringBuilder sb = new StringBuilder();
     sb.append( getName() ).append(" ").append(getMode());
    if( sqlUtils != null ) {
      sb.append( " is").append( running?"":" not").append(" running on DB(").append(sqlUtils).append(")");
    } else {
      sb.append( " is not connected to DB(").append( failover.getConnectionData() ).append(")");
    }
   
    if( sqlUtilsLogger != null && sqlUtilsLogger.getLastException() != null ) {
      sb.append(", last exception (").append(sqlUtilsLogger.getLastExceptionDate()).append(") was ").append(sqlUtilsLogger.getLastException().getClass().getSimpleName()).append("(\"").append(sqlUtilsLogger.getLastException().getMessage() ).append("\")");
    }
    if( rebuildCounter > 0 ) {
      sb.append(", ").append(rebuildCounter).append(" tries to rebuild connection" );
    }
    return sb.toString();
  }
  
  /**
   * Logt den Status
   */
  public void logStatus(Logger statusLogger) {
    statusLogger.info( getLogStatus() );
  }
 
  @Override
  public String toString() {
    return getLogStatus();
  }

  
  /**
   * @return
   */
  protected abstract String getMode();

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DBFiller#getNumReconnects()
   */
  public int getNumReconnects() {
    return rebuildCounter;
  }
  
  public int getNumFailovers() {
    return failoverCounter;
  }
  
  public int getNumDeadlocks() {
    return deadlockCounter;
  }
  
  protected void incDeadlockCounter(){
    logger.warn("Deadlock occurred");
    deadlockCounter++;
  }
  
  
  public void getRingBufferCopy(Collection<Data> rbData) {
    ringBuffer.copyTo(rbData);
  }
  
  protected void setLastException( Throwable t ) {
    if( t != null ) {
      lastExceptionDate = System.currentTimeMillis();
      lastException = t;
    }
  }
  
  public String getLastException() {
    if( lastException == null ) {
      return "none";
    }
    return new Date(lastExceptionDate)+": " +lastException.getMessage();
  }
  
  public String getConnectString() {
    if( sqlUtils != null ) {
      return sqlUtils.toString();
    } else {
      return "none";
    }
  }
  
  

}
