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
package snmpTrapDemon.poolUsage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.failover.Failover;
import com.gip.xyna.utils.db.failover.FailoverDBConnectionData;
import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;
import com.gip.xyna.utils.snmp.agent.utils.SparseSnmpTable;
import com.gip.xyna.utils.snmp.manager.SnmpContextImplApache;

import snmpTrapDemon.poolUsage.PoolUsageSnmp.Type;

import dhcpAdapterDemon.db.SQLUtilsLoggerImpl;

/**
 * periodisches Ausführen von PoolUsage und PoolUsageChecker
 *
 */
public class PoolUsageCronJob {
  static Logger logger = Logger.getLogger(PoolUsageCronJob.class.getName());
  
  private Timer timer;
  private long period;
  private SQLUtils sqlUtils;
  private FailoverDBConnectionData dbConData;
  private Failover failover;
  private SQLUtilsLoggerImpl sqlUtilsLogger;
  private long lastRun;
  private AtomicInteger trapCounter = new AtomicInteger();
  private int numTraps;
  private SnmpAccessData sadTrap;
  private PoolUsageSnmp poolUsageSnmp;
  private SparseSnmpTable sparseSnmpTable;
  private PoolUsageSnmp poolUsageSumSnmp;
  private SparseSnmpTable sparseSnmpSumTable;
  private boolean started;
  
  public PoolUsageCronJob() {
    //Lesen des Zeitintervalls aus der DB,
    //TrapOids etc..
    period = DemonProperties.getIntProperty( "snmpTrap.check.interval", 600 );
    
    DBConnectionData cd = DemonProperties.getDBProperty( "snmpTrap" );
    dbConData = DemonProperties.getFailoverDBProperty( "snmpTrap", cd );
    failover = dbConData.createNewFailover();
   
    sqlUtilsLogger = new SQLUtilsLoggerImpl(logger);
    sadTrap = DemonProperties.getSnmpAccessData(SnmpAccessData.VERSION_2c, "trap" );
    poolUsageSnmp = new PoolUsageSnmp(Type.ALL);
    sparseSnmpTable = new SparseSnmpTable(poolUsageSnmp, poolUsageSnmp.getStandardOid());
    
    poolUsageSumSnmp = new PoolUsageSnmp(Type.SUM);
    sparseSnmpSumTable = new SparseSnmpTable(poolUsageSumSnmp, poolUsageSumSnmp.getStandardOid());
    
    
  }
  
  private SQLUtils getSqlUtils() {
    boolean isValid = false;
    if( sqlUtils != null ) {
      try {
        isValid = sqlUtils.getConnection().isValid(0);
      } catch (SQLException e) {
        sqlUtilsLogger.logException(e);
        isValid = false;
      }
    }
    if( isValid ) {
      sqlUtils = failover.checkAndRecreate( sqlUtils, sqlUtilsLogger );
    } else {
      sqlUtils = failover.recreateSQLUtils(sqlUtils, sqlUtilsLogger);
    }
    return sqlUtils;
  }
    
  private SparseSnmpTable getSparseSnmpTable() {
    return sparseSnmpTable;
  }
  private SparseSnmpTable getSparseSnmpSumTable() {
    return sparseSnmpSumTable;
  }
  private PoolUsageSnmp getPoolUsageSnmp() {
    return poolUsageSnmp;
  }
  private PoolUsageSnmp getPoolUsageSumSnmp() {
    return poolUsageSumSnmp;
  }
  
  
  /**
   * Dieser Timertask führt PoolUsage und PoolUsageChecker aus
   *
   */
  private static class PUTimerTask extends TimerTask {
    
    private PoolUsageCronJob poolUsageCronJob;
    private PoolUsageThreshold poolUsageThreshold;
    private SnmpAccessData sadTrap;
    public PUTimerTask(PoolUsageCronJob poolUsageCronJob, SnmpAccessData sadTrap ) {
      this.poolUsageCronJob = poolUsageCronJob;
      this.sadTrap = sadTrap;
    }
    
    @Override
    public void run() {
      try {
        logger.debug("run PoolUsageCronJob");
        SQLUtils sqlUtils = poolUsageCronJob.getSqlUtils();
        if( sqlUtils == null ) {
          logger.error( "No connection to database");
          return;
        }
        work(sqlUtils);
      } catch( Exception e ) {
        logger.error( "Error while working on pool usage", e);
      } catch( Throwable t) {
        logger.error( "Severe error while working on pool usage, trying to keep running", t);
      }
    }
    
    private void work(SQLUtils sqlUtils) {
      SnmpContextImplApache trapsender = null;
      
      try {
        trapsender = new SnmpContextImplApache(sadTrap);
      } catch (IOException e) {
        //SnmpContext kann nicht angelegt werden
        logger.error( "Error while initializing TrapSender",e );
      }
      
      poolUsageCronJob.setLastRun( System.currentTimeMillis() );
      
      //Lesen der konfigurierten Schwellwerte
      poolUsageThreshold = new PoolUsageThreshold();
      poolUsageThreshold.read( sqlUtils );

      //Berechnung der PoolUsage
      PoolUsage poolUsage = new PoolUsage( sqlUtils );
      poolUsage.searchPools();
      poolUsage.calculateUsage();
      
      PoolUsageTable poolUsageTable = poolUsage.readPoolUsageTable();
      poolUsageCronJob.getPoolUsageSnmp().setPoolUsageTable( poolUsageTable );
      poolUsageCronJob.getPoolUsageSnmp().setPoolUsageThreshold( poolUsageThreshold );
      poolUsageCronJob.getSparseSnmpTable().refreshTableModel();
      
      PoolUsageTable poolUsageSumTable = poolUsage.readPoolUsageSumTable();
      PoolUsageThreshold poolUsageSumThreshold = new PoolUsageThreshold();
      poolUsageSumThreshold.read( sqlUtils );

      poolUsageCronJob.getPoolUsageSumSnmp().setPoolUsageTable( poolUsageSumTable );
      poolUsageCronJob.getPoolUsageSumSnmp().setPoolUsageThreshold( poolUsageThreshold );
      poolUsageCronJob.getSparseSnmpSumTable().refreshTableModel(); 
      
      PoolUsageChecker poolUsageChecker = new PoolUsageChecker( poolUsageTable, trapsender, poolUsageThreshold, poolUsageCronJob.getTrapCounter() );
      int numTraps = poolUsageChecker.checkThreshold();
      poolUsageCronJob.setNumTraps(numTraps);
      if( trapsender != null ) {
        trapsender.close();
      }
    }
  }
  
  public void start() {
    logger.debug("start PoolUsageCronJob");
    timer = new Timer("PoolUsageCronJob");
    timer.schedule( new PUTimerTask(this,sadTrap), 0, period*1000L );
    started = true;
  }

  /**
   * @param numTraps
   */
  public void setNumTraps(int numTraps) {
    this.numTraps = numTraps;
  }

  /**
   * @param lastRun
   */
  public void setLastRun(long lastRun) {
    this.lastRun = lastRun;
  }

  /**
   * @return
   */
  public AtomicInteger getTrapCounter() {
    return trapCounter;
  }

  public void terminate() {
    logger.debug("terminate PoolUsageCronJob");
    timer.cancel();
    if( sqlUtils != null ) {
      sqlUtils.closeConnection();
      sqlUtils = null;
    }
  }

  /**
   * @param statusLogger
   */
  public void logStatus(Logger statusLogger) {
    if( started ) {
      StringBuilder sb = new StringBuilder();
      sb.append("PoolUsageCronJob is running every ").append(period).append(" seconds");
      sb.append(", last exceution was ").append(new Date(lastRun)).append(" with ").append(numTraps).append(" traps");
      sb.append(", number of total sent traps ").append(trapCounter.get());
      sb.append(", database is ").append( sqlUtils );
      Exception e = sqlUtilsLogger.getLastException();
      if( e != null ) {
        sb.append(", last exception was \"").append(e.getMessage()).append("\" at ").append(sqlUtilsLogger.getLastExceptionDate());
      }
      statusLogger.info(sb.toString());
    } else {
      statusLogger.info("PoolUsageCronJob is not started");
    }
    
  }

  /**
   * @return
   */
  public OidSingleHandler getPoolUsageSnmpTable() {
    return sparseSnmpTable;
  }
  
  /**
   * @return
   */
  public OidSingleHandler getPoolUsageSnmpSumTable() {
    return sparseSnmpSumTable;
  }

}
