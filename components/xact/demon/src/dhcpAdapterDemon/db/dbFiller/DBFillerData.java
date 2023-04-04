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

import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.failover.FailoverDBConnectionData;

/**
 * 
 *
 */
public class DBFillerData {
  
  /**
   *
   *
   */
  public enum Type {
    SINGLE_COMMIT, 
    BULK_COMMIT,
    PACKET;
  }

  private FailoverDBConnectionData failoverConnectionData;
  private int capacity;
  private int bulksize;
  private String filename;
  public Type type;
  public int waitReconnect;
  
  private DBFillerData() { /*internal*/}
  
  
  public static class DBFillerDataBuilder {

    private DBFillerData dfd = new DBFillerData();
    /**
     * @param type
     */
    public DBFillerDataBuilder(Type type) {
      dfd.type = type;
    }
    /**
     * @param failoverDBconnectionData
     * @return
     */
    public DBFillerDataBuilder connectionData( DBConnectionData dbConnectionData) {
      dfd.failoverConnectionData = FailoverDBConnectionData.newFailoverDBConnectionData().
       dbConnectionData(dbConnectionData).
       failoverUrl(dbConnectionData.getUrl()).
       failoverSource("none").
       build();
      return this;
    }
    /**
     * @param failoverDBconnectionData
     * @return
     */
    public DBFillerDataBuilder connectionData( FailoverDBConnectionData failoverDBConnectionData) {
      dfd.failoverConnectionData = failoverDBConnectionData;
      return this;
    }
    /**
     * @param capacity
     * @return
     */
    public DBFillerDataBuilder capacity(int capacity) {
      dfd.capacity = capacity;
      return this;
    }
    /**
     * @param bulksize
     * @return
     */
    public DBFillerDataBuilder bulksize(int bulksize) {
      dfd.bulksize = bulksize;
      return this;
    }
    /**
     * @param filename
     * @return
     */
    public DBFillerDataBuilder filename(String filename) {
      dfd.filename = filename;
      return this;
    }
    /**
     * @param waitReconnect
     * @return
     */
    public DBFillerDataBuilder waitReconnect(int waitReconnect) {
      dfd.waitReconnect = waitReconnect;
      return this;
    }
    /**
     * @return
     */
    public DBFillerData build() {
      //TODO Prüfungen
      return dfd;
    }
     
    
    
  }
  
  /**
   * @param type
   * @return
   */
  public static DBFillerDataBuilder newDBFiller(Type type) {
    return new DBFillerDataBuilder(type);
  }

  /**
   * @return
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @return
   */
  public Type getType() {
    return type;
  }

  /**
   * @return
   */
  public int getCapacity() {
    return capacity;
  }

  /**
   * @return
   */
  public FailoverDBConnectionData getFailoverConnectionData() {
    return failoverConnectionData;
  }

  /**
   * @return
   */
  public int getBulksize() {
    return bulksize;
  }

  /**
   * @return
   */
  public long getWaitReconnect() {
    return waitReconnect;
  }

}
