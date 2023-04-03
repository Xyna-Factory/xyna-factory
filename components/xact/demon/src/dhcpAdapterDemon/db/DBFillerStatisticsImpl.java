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
package dhcpAdapterDemon.db;

import dhcpAdapterDemon.DhcpData;
import dhcpAdapterDemon.db.dbFiller.BulkDBFiller;
import dhcpAdapterDemon.db.dbFiller.DBFiller;
import dhcpAdapterDemon.db.dbFiller.DBFiller.Status;

/**
 *
 */
public class DBFillerStatisticsImpl extends AbstractDBFillerStatistics {

  private DBFiller<DhcpData> dbFiller;
  
  /**
   * @param dbFiller
   */
  public DBFillerStatisticsImpl(String name, DBFiller<DhcpData> dbFiller) {
    super(name);
    this.dbFiller = dbFiller;
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
    if( dbFiller instanceof BulkDBFiller<?> ) {
      return ((BulkDBFiller<?>)dbFiller).getAverageBulkCommitSize();
    } else {
      return 1;
    }
  }

  public String getLastException() {
    return dbFiller.getLastException();
  }

  public String getConnectString() {
    return dbFiller.getConnectString();
  }

  public int getNumDeadlocks() {
    return dbFiller.getNumDeadlocks();
  }
  
}
