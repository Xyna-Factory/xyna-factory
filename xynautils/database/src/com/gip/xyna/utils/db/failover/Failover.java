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
package com.gip.xyna.utils.db.failover;

import java.util.HashMap;

import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.SQLUtilsLogger;

/**
 * Failover knows how to build connections for the SqlUtils. 
 * It decides which database to connect to depending on the answer 
 * from the failoverSource.
 */
public class Failover {
  
  private FailoverSource failoverSource;
  private FailoverDBConnectionData failoverConnectionData;
  private HashMap<SQLUtils,Boolean> suMap;
  private FailoverConnectionLifecycle failoverConnectionLifecycle;
  
  /**
   * Constructor
   * @param failoverSource
   * @param policy
   * @param failoverDBConnectionData
   * @param failoverConnectionLifecycle
   */
  public Failover(FailoverSource failoverSource, FailoverDBConnectionData failoverDBConnectionData, FailoverConnectionLifecycle failoverConnectionLifecycle) {
    this.failoverSource = failoverSource;
    this.failoverConnectionData = failoverDBConnectionData;
    this.failoverConnectionLifecycle = failoverConnectionLifecycle;
    suMap = new HashMap<SQLUtils,Boolean>();
  }
  
  /**
   * @return
   */
  public boolean isFailover() {
    return failoverSource.isFailover();
  }


  /**
   * Build new sqlUtils
   * @param sqlUtilsLogger
   * @return null if sqlUtils could not be created
   */
  public SQLUtils createSQLUtils(SQLUtilsLogger sqlUtilsLogger) {
    boolean failover = failoverSource.isFailover();
    SQLUtils sqlUtils = failoverConnectionData.getConnectionData(failover).createSQLUtils(sqlUtilsLogger);
    if( sqlUtils != null ) {
      suMap.put(sqlUtils,failover);
      if( failoverConnectionLifecycle != null ) {
        failoverConnectionLifecycle.initialize(sqlUtils);
      }
    }
    return sqlUtils;
  }

  /**
   * Closes the given sqlUtils an builds a new one. For closing the sqlUtils the configured policy is used.
   * @param sqlUtils
   * @param sqlUtilsLogger
   * @return null if sqlUtils could not be recreated
   */
  public SQLUtils recreateSQLUtils(SQLUtils sqlUtils, SQLUtilsLogger sqlUtilsLogger) {
    //SQLUtils close and recreate
    if( sqlUtils != null ) {
      if( failoverConnectionLifecycle != null ) {
        failoverConnectionLifecycle.finalize(sqlUtils);
      }
      if( ! failoverConnectionData.getNormalConnectionData().isAutoCommit() ) {
        sqlUtils.rollback(); //Rollback only if possible
      }
      sqlUtils.closeConnection();
      suMap.remove(sqlUtils);
    }
    //recreate
    return createSQLUtils(sqlUtilsLogger);
  }

  /**
   * Fast-Check, whether the sqlUtils is connected to the right URL
   * @param sqlUtils
   * @param sqlUtilsLogger
   * @return null if check fails and sqlUtils could not be recreated
   */
  public SQLUtils checkAndRecreate(SQLUtils sqlUtils, SQLUtilsLogger sqlUtilsLogger) {
    boolean fo = failoverSource.isFailover();
    if( sqlUtils != null ) {
      Boolean foCreation = suMap.get(sqlUtils);
      if( foCreation != null && foCreation.booleanValue() == fo ) {
        return sqlUtils;//SQLUtils is connected to the right URL
      }  
    }
    //Failover-state has changed (or was unknown)
    sqlUtilsLogger.logSQL("Failover-Switch");
    //now recreate
    return recreateSQLUtils(sqlUtils,sqlUtilsLogger);
  }
  
  /**
   * Slow-Check, whether the sqlUtils is valid and connected to the right URL
   * The validity is tested with a call "SELECT count(*) FROM DUAL", which is only possible 
   * on Oracle- and MySQL-databases
   * @param sqlUtils
   * @param sqlUtilsLogger
   * @return null if check fails and sqlUtils could not be recreated
   */
  public SQLUtils checkValidAndRecreate(SQLUtils sqlUtils, SQLUtilsLogger sqlUtilsLogger) {
    //zuerst failover-Pr�fung
    SQLUtils su = checkAndRecreate(sqlUtils,sqlUtilsLogger);
    boolean isValid = false;
    //unter 1.6 w�re es sch�n einfach:
    /*
          try {
            isValid = sqlUtils.getConnection().isValid(0);
          } catch (SQLException e) {
            sqlUtilsLogger.logException(e);
            isValid = false;
          }
     */
    //unter 1.5 ist leider Query n�tig
    //FIXME nur f�r Oracle und MySQL m�glich!
    Integer res = su.queryInt("SELECT count(*) FROM DUAL", null );
    isValid = (res != null);
    if( isValid ) {
      return su;//SQLUtils is OK
    } else {
      sqlUtilsLogger.logSQL("Connection is not valid");
      return recreateSQLUtils(su,sqlUtilsLogger);
    }
  }

  /**
   * Returns the currently used DBConnectionData
   * @return
   */
  public DBConnectionData getConnectionData() {
    return failoverConnectionData.getConnectionData( failoverSource.isFailover() );
  }

}
