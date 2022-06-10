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
package com.gip.xyna.xnwh.persistence.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException.Reason;
import com.gip.xyna.utils.db.ConnectionPool.PooledConnection;
import com.gip.xyna.utils.db.pool.FastValidationStrategy;
import com.gip.xyna.utils.db.pool.ValidationStrategy;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;

public class OraclePLClusterStateChangeHandler implements ClusterStateChangeHandler {
  
  private static final Logger logger = CentralFactoryLogging
      .getLogger(OraclePLClusterStateChangeHandler.class);

  private List<ConnectionPool> pools = new ArrayList<>();
  private String plName;
  private long clusterInstanceId;
  
  public void setId(String plName) {
    this.plName = plName;
  }

  public void setClusterInstanceId(long clusterInstanceId) {
    this.clusterInstanceId = clusterInstanceId;
  }

  @Override
  public boolean isReadyForChange(ClusterState newState) {
    return true;
  }

  public void addPool(ConnectionPool pool) {
    pools.add(pool);
  }

  @Override
  public void onChange(ClusterState newState) {
    if( newState == ClusterState.DISCONNECTED_SLAVE ) {
      PoolBrokenHandler pbh = new PoolBrokenHandler(plName);
      for( ConnectionPool pool : pools ) {
        if( ! pbh.checkPool(pool) ) {
          pool.markAsBroken();
          pbh.addBrokenPool(pool);
        }
      }
      if( pbh.hasBrokenPools() ) {
        new Thread( pbh, pbh.getThreadName() ).start();
      }
    }
  }


  public long getClusterInstanceId() {
    return clusterInstanceId;
  }
  
  private static class PoolBrokenHandler implements Runnable {
    
    private List<ConnectionPool> pools = new ArrayList<>();
    private String name;
    private ValidationStrategy validationStrategy;
    private long getConTimeout;
    
    public PoolBrokenHandler(String plName) {
      this.name = "OraclePersistence-PoolCloser-"+plName;
      this.validationStrategy = FastValidationStrategy.validateAlwaysWithTimeout(100); //TODO timeout
      this.getConTimeout = 1000;  //TODO timeout
    }
    
    /**
     * @param pool
     * @return true, wenn Pool valide ist
     */
    public boolean checkPool(ConnectionPool pool) {
      PooledConnection peCon = null;
      try {
        peCon = pool.getConnection( getConTimeout, "test" );
        //FIXME besser mit neuem ConnectionPool: peCon = pool.getConnection(getConTimeout, "test", validationStrategy);
        return true;
      } catch( NoConnectionAvailableException e ) {
        if( e.getReason() == Reason.PoolExhausted ) {
          // Pool könnte valide sein, aber derzeit überlastet, dies muss genauer geprüft werden
          return checkExhaustedPool(pool);
        } else {
          logger.info("Failed to validate test connection", e);
          return false;
        }
      } finally {
        finallyClose(peCon);
      }
    }
    
    private boolean checkExhaustedPool(ConnectionPool pool) {
      //Überlasteter Pool kann derzeit keine Connection liefern.
      //Daher nochmal versuchen, eine Connection direkt anzulegen
      Connection con = null;
      try {
        con = pool.getConnectionBuildStrategy().createNewConnection();
        validationStrategy.validate(con);
        return true;
      } catch( Exception e ) {
        logger.info("Failed to create or validate test connection", e);
        return false;
      } finally {
        finallyClose(con);
      }
    }
    
    private void finallyClose(Connection con) {
      if( con != null ) {
        try {
          con.close();
        } catch (SQLException e) {
          logger.info("Failed to close test connection", e);
        }
      }
    }

    public String getThreadName() {
      return name;
    }
    
    public boolean hasBrokenPools() {
      return ! pools.isEmpty();
    }

    public void addBrokenPool(ConnectionPool pool) {
      pools.add(pool);
    }

    @Override
    public void run() {
      StringBuilder sb = new StringBuilder();
      sb.append( "Marked pools as broken: ");
      String sep = ": ";
      for( ConnectionPool pool : pools ) {
        pool.markAsBroken();
        sb.append(sep).append(pool.getId());
        sep = ", ";
      }
      sb.append("; starting new thread ").append(name).append(" to close pools");
      logger.info(sb.toString());
      
      for( ConnectionPool pool : pools ) {
        try {
          ConnectionPool.closedownPool(pool, true, 200);
          logger.info("ConnectionPool "+pool.getId()+" is closed");
        } catch (ConnectionCouldNotBeClosedException e) {
          logger.info("ConnectionPool "+pool.getId()+" cannot be closed");
        }
      }
    }
  }
  

}
