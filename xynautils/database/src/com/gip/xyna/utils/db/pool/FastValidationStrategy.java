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
package com.gip.xyna.utils.db.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.utils.db.DBConnectionData.DBConnectionDataBuilder;
import com.gip.xyna.utils.db.SQLUtils;

public class FastValidationStrategy implements ValidationStrategy {
  

  private final AtomicLong validationInterval;
  private int validationSocketTimeoutInMillis;
  private boolean rebuildConnectionAfterFailedValidation;
  
  public FastValidationStrategy(long validationInterval, long validationSocketTimeoutInMillis, boolean rebuildConnectionAfterFailedValidation) {
    this.validationInterval = new AtomicLong(validationInterval);
    this.validationSocketTimeoutInMillis = (int)validationSocketTimeoutInMillis;
    this.rebuildConnectionAfterFailedValidation = rebuildConnectionAfterFailedValidation;
  }
 
  public static FastValidationStrategy validateAlwaysWithTimeout(long validationSocketTimeoutInMillis) {
    return new FastValidationStrategy(0,validationSocketTimeoutInMillis, true);
  }
  
  public static FastValidationStrategy validateAlwaysWithTimeout_dontRebuildConnectionAfterFailedValidation(long validationSocketTimeoutInMillis) {
    return new FastValidationStrategy(0,validationSocketTimeoutInMillis, false);
  }
  
  public static FastValidationStrategy validateAfterIntervalWithTimeout(long validationInterval, long validationSocketTimeoutInMillis) {
    return new FastValidationStrategy(validationInterval,validationSocketTimeoutInMillis, true);
  }
  
  public boolean isValidationNecessary(long currentTime, long lastcheck) {
    return currentTime - lastcheck >= validationInterval.get();
  }

  public Exception validate(Connection con) {
    return validateConnection(con);
  }

  public void setValidationInterval(long validationInterval) {
    this.validationInterval.set(validationInterval);
  }
  
  public long getValidationInterval() {
   return this.validationInterval.get();
  }

  private Exception validateConnection(Connection con) {
    try {
      if( con.isClosed() ) {
        //hier klappt keine Validierung mehr
        return new Exception("Connection "+con+" is already closed");
      }
    } catch ( SQLException e ) {
      return e;
    }
    
    //1) zuerst NetworkTimeout anpassen, damit Validierung schneller ist
    int oldTimeout;
    try {
      oldTimeout = con.getNetworkTimeout();
      con.setNetworkTimeout(DBConnectionDataBuilder.DefaultNetworkTimeoutExecutor, validationSocketTimeoutInMillis);
    } catch ( Exception e ) {
      //hier gibt es leider eine NPE, wenn die Connection einen Fehler hat
      return e;
    }
    try {
      //2) dann eigentliche Validierung
      SQLUtils checkUtils = createSQLUtils(con);
      checkUtils.queryInt("select 1 from dual", null);
      return checkUtils.getLastException();
    } catch (RuntimeException e) {
      return e;
    } finally {
      //3) NetworkTimeout zur�cksetzen
      try {
        con.setNetworkTimeout(DBConnectionDataBuilder.DefaultNetworkTimeoutExecutor, oldTimeout);
      } catch ( Exception e ) {
        //hier gibt es leider eine NPE, wenn die Connection einen Fehler hat
      }
    }
  }

  private SQLUtils createSQLUtils(Connection con) {
    SQLUtils checkUtils = new SQLUtils(con);
    checkUtils.setQueryTimeout((int)(0.9*validationSocketTimeoutInMillis/1000)); //etwas kleiner als SocketTimeout
    return checkUtils;
  }

  public boolean rebuildConnectionAfterFailedValidation() {
    return rebuildConnectionAfterFailedValidation;
  }

}
