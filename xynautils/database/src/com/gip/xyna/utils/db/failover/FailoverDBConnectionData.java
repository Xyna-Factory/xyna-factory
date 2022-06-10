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
package com.gip.xyna.utils.db.failover;

import com.gip.xyna.utils.db.DBConnectionData;

/**
 * All needed Data for a failover database connection
 */
public class FailoverDBConnectionData  {
  
  private DBConnectionData dbConnectionData;
  private DBConnectionData failoverConnectionData;
  private String failoverSource;
  private String failoverSourceParam;
 
  FailoverDBConnectionData() {/*package-private constructor, internal use only*/}
  
  /**
   * @param fcd
   */
  FailoverDBConnectionData(FailoverDBConnectionData fcd) {
    this.dbConnectionData = fcd.dbConnectionData;
    this.failoverConnectionData = fcd.failoverConnectionData;
    this.failoverSource = fcd.failoverSource;
    this.failoverSourceParam = fcd.failoverSourceParam;
  }

  /**
   * @return
   */
  public static FailoverDBConnectionDataBuilder newFailoverDBConnectionData() {
    return new FailoverDBConnectionDataBuilder();
  }
  
  public static class FailoverDBConnectionDataBuilder {
      
    private FailoverDBConnectionData fcd;
    private String failoverUrl;

    FailoverDBConnectionDataBuilder() {
      fcd = new FailoverDBConnectionData();
    }
    FailoverDBConnectionDataBuilder(FailoverDBConnectionData fcd) {
      fcd = new FailoverDBConnectionData(fcd);
    }
     
    public FailoverDBConnectionDataBuilder dbConnectionData(DBConnectionData dbConnectionData) {
      fcd.dbConnectionData = dbConnectionData;
      return this;
    }
    
    public FailoverDBConnectionDataBuilder failoverUrl(String failoverUrl) {
      this.failoverUrl = failoverUrl;
      return this;
    }

    public FailoverDBConnectionDataBuilder failoverSource(String failoverSource) {
      fcd.failoverSource = failoverSource;
      return this;
    }

    public FailoverDBConnectionDataBuilder failoverSourceParam(String failoverSourceParam) {
      fcd.failoverSourceParam = failoverSourceParam;
      return this;
    }

    public FailoverDBConnectionData build() {
      if( this.failoverUrl == null ) {
        throw new IllegalStateException( "Failover-URL is not set" );
      }
      if( fcd.failoverSource == null ) {
        throw new IllegalStateException( "FailoverSource is not set" );
      }
      fcd.failoverConnectionData = DBConnectionData.copyDBConnectionData(fcd.dbConnectionData).url(failoverUrl).build();
      return fcd;
    }

  }

  /**
   * @return the failoverSource
   */
  public String getFailoverSource() {
    return failoverSource;
  }
  
  /**
   * @return the failoverSourceParam
   */
  public String getFailoverSourceParam() {
    return failoverSourceParam;
  }
  
  /**
   * @return the normal DBConnectionData
   */
  public DBConnectionData getNormalConnectionData() {
    return dbConnectionData;
  }
  
  /**
   * @return the failover DBConnectionData
   */
  public DBConnectionData getFailoverConnectionData() {
    return failoverConnectionData;
  }
  
  /**
   * @param failover
   * @return the DBConnectionData
   */
  public DBConnectionData getConnectionData(boolean failover) {
    if( failover ) {
      return failoverConnectionData;
    } else {
      return dbConnectionData;
    }
  }

  
  
  
  /**
   * Create a new Failover-instance, which should be used to create as SqlUtils-instance
    * @return
   */
  public Failover createNewFailover() {
    return new Failover( FailoverSources.getFailoverSource(failoverSource,failoverSourceParam),this,null);
  }
  
  /**
   * Create a new Failover-instance, which should be used to create as SqlUtils-instance
   * @param sqlUtilsInitializer Additional initialization of the sqlUtils
   * @return
   */
  public Failover createNewFailover(FailoverConnectionLifecycle failoverConnectionLifecycle) {
    return new Failover( FailoverSources.getFailoverSource(failoverSource,failoverSourceParam),this,failoverConnectionLifecycle);
  }

}
