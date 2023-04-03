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

import dhcpAdapterDemon.types.State;

public class SingleDBFiller<Data> extends AbstractDBFiller<Data> {

  boolean autoCommit;
  
  public SingleDBFiller(DBFillerData dbFillerData, String name) {
    super(dbFillerData,name);
    autoCommit = dbFillerData.getFailoverConnectionData().getNormalConnectionData().isAutoCommit();
  }

  @Override
  protected void commit(Data data, State state) {
    commit();
    dataProcessor.state(data, state );
  }

  @Override
  protected void rollback(Data data, State state) {
    rollback();
    dataProcessor.state(data, state );
  }

  @Override
  protected void rollback() {
    if( ! autoCommit ) {
      if( ! sqlUtils.rollback() ) {
        throw new RebuildConnectionException("rollback failed");
      }
    }
  }
  
  @Override
  protected void commit() {
    if( ! autoCommit ) {
      if( ! sqlUtils.commit() ) {
        throw new RebuildConnectionException("commit failed");
      }
    }
  }

  @Override
  protected void state(Data data, State state) {
    dataProcessor.state(data, state );
  }
  
  @Override
  protected String getMode() {
    return "autoCommit";
  }
 
}
