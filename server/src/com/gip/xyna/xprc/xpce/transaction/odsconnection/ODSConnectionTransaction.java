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
package com.gip.xyna.xprc.xpce.transaction.odsconnection;


import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xprc.xpce.transaction.TypedTransaction;


public class ODSConnectionTransaction implements TypedTransaction {

  private final ODSConnection con;
  
  
  public ODSConnectionTransaction(ODSConnection con) {
    this.con = con;
  }


  public void commit() throws Exception {
    con.commit();
  }


  public void rollback() throws Exception {
    con.rollback();
  }


  public void end() throws Exception {
    con.closeConnection();
  }
  
  
  // TODO split in public and hidden method
  // call hidden method internally and return OperationPreventingConnection on public
  public ODSConnection getConnection() {
    return con;
  }
  
}
