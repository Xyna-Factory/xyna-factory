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
package dhcpAdapterDemon.db.leaselog;

import dhcpAdapterDemon.db.AbstractDBFillerStatistics;
import dhcpAdapterDemon.db.dbFiller.DBFiller;
import dhcpAdapterDemon.db.dbFiller.DBFiller.Status;

/**
 * Statistiken für den LeaseLog-DBFiller
 *
 */
public class LeaseLogDBFillerStatistics extends AbstractDBFillerStatistics {

  private DBFiller<LeaseLogPacket> dbFiller;
  private float bulkSize;
  
  /**
   * @param dbFiller
   * @param bulkSize 
   */
  public LeaseLogDBFillerStatistics(String name, DBFiller<LeaseLogPacket> dbFiller, int bulkSize) {
    super(name);
    this.dbFiller = dbFiller;
    this.bulkSize = bulkSize;
  }
  
  public Status getStatus() {
    return dbFiller.getStatus();
  }

  public int getWaiting() {
    return dbFiller.getWaiting();
  }

  public int getNumReconnects() {
    return dbFiller.getNumReconnects();
  }
  
  public int getNumFailovers() {
    return dbFiller.getNumFailovers();
  }

  public int getBufferSize() {
    return dbFiller.getCapacity();
  }

  public float getAverageBulkCommitSize() {
    return bulkSize;
  }

  public String getLastException() {
    return dbFiller.getLastException();
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DBFillerStatistics#getConnectString()
   */
  public String getConnectString() {
    return dbFiller.getConnectString();
  }

  public int getNumDeadlocks() {
    return dbFiller.getNumDeadlocks();
  }

}
