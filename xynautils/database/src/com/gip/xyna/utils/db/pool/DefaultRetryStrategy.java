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
package com.gip.xyna.utils.db.pool;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableReasonDetector;

public class DefaultRetryStrategy implements RetryStrategy {
  
  private int maxRetries;
  
  
  private static Logger logger = Logger.getLogger(DefaultRetryStrategy.class);
  
  public DefaultRetryStrategy(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  /**
   * Anzahl der Retries, wieder eine Exception zu erhalten. O bedeutet: nur ein Versuch, kein Retry
   * @param maxRetries
   */
  public void setMaxRetries(int maxRetries) {
    if( maxRetries < 0 ) {
      throw new IllegalArgumentException("maxRetries can not be negative");
    }
    this.maxRetries = maxRetries;
  }
  
  public int getMaxRetries() {
    return this.maxRetries;
  }
  
  public Connection createConnectionWithRetries(ConnectionBuildStrategy connectionBuildStrategy, 
      ValidationStrategy validationStrategy, 
      NoConnectionAvailableReasonDetector noConnectionAvailableReasonDetector
      ) throws NoConnectionAvailableException {
    Connection con;
    Exception exception = null;
    for( int retry = 0; retry <= maxRetries; ++ retry ) {
      try {
        con = connectionBuildStrategy.createNewConnection();
      } catch (RuntimeException e) {
        exception = e;
        if( retry == 0 );
        //todo beim ersten Durchlauf warn
        logger.trace("could not create new connection", exception);
        continue; //nächster Retry
      }
      exception = validationStrategy.validate(con);
      if( exception != null ) {
        logger.trace("could not validate new connection", exception);
        try {
          con.close();
        } catch (SQLException e) {
          logger.trace("connection could not be closed successfully", e);
        }
        continue; //nächster Retry
      } else {
        //valide Connection erhalten
        return con;
      }
    }
    throw new NoConnectionAvailableException( exception, noConnectionAvailableReasonDetector );
  }

  

}
