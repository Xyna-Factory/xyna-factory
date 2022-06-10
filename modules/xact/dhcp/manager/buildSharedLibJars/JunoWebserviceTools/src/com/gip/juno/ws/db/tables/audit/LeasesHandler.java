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

package com.gip.juno.ws.db.tables.audit;

import com.gip.juno.ws.handler.LeasesTools;
import com.gip.juno.ws.handler.TableHandlerBasic;
import com.gip.juno.ws.tools.DBTableInfo;

public class LeasesHandler extends TableHandlerBasic  {
  //private DBTableInfo _leasesCpeInfo;
  
  public LeasesHandler() {
    super("Leases", false);
  }
  public DBTableInfo initDBTableInfo() {
    //_leasesCpeInfo = LeasesTools.getDBTableInfoLeasesCpe();
    return LeasesTools.getDBTableInfoLeases();
  }
  /*
  public DBTableInfo getDBTableInfoLeasesCpe() {
    return _leasesCpeInfo; 
  }
  */
}
