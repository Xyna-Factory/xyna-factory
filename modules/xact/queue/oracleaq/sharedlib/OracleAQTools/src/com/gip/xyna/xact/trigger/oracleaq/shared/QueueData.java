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

package com.gip.xyna.xact.trigger.oracleaq.shared;

import java.io.Serializable;

import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.OracleAQConnectData;


public class QueueData implements Serializable {

  public static int SOCKET_TIMEOUT_SECONDS = 15;
  public static int CONNECTION_TIMEOUT_SECONDS = 15;
  
  private static final long serialVersionUID = 1L;
  
  protected String queueName;
  protected String dbSchema;
  protected String dbPassword;
  protected String jdbcUrl;
  protected transient DBConnectionData dbd;
  protected String consumerName;
  
  public QueueData(String queueName, String dbSchema, String dbPassword, String jdbcUrl, String consumerName) {
    this.queueName = queueName;
    this.dbSchema = dbSchema;
    this.dbPassword = dbPassword;
    this.jdbcUrl = jdbcUrl;
    this.consumerName = consumerName;
  }
  
  public QueueData(String queueName, OracleAQConnectData connData, String consumerName) {
    this( queueName,
          connData.getUserName(),
          connData.getPassword(),
          connData.getJdbcUrl(),
          consumerName );
  }

  public QueueData(String queueName, String dbSchema, String dbPassword, String jdbcUrl) {
    this(queueName, dbSchema, dbPassword, jdbcUrl, null);
  }
  
  public QueueData(String queueName, OracleAQConnectData connData) {
    this(queueName, connData, null);
  }

 
  
  public String getQueueName() {
    return queueName;
  }

  public String getDbSchema() {
    return dbSchema;
  }

  public String getDbPassword() {
    return dbPassword;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }
  
  public String getConsumerName() {
    return consumerName;
  }

  public DBConnectionData getDBConnectionData( ClassLoader classLoaderToLoadDriver ) {
    if( dbd == null ) {
      dbd = DBConnectionData.newDBConnectionData()
          .user(dbSchema)
          .password(dbPassword)
          .url(jdbcUrl)
          .connectTimeoutInSeconds(CONNECTION_TIMEOUT_SECONDS)
          .socketTimeoutInSeconds(SOCKET_TIMEOUT_SECONDS)
          .classLoaderToLoadDriver(classLoaderToLoadDriver)
          .build();
    }
    return dbd;
  }
  

}
