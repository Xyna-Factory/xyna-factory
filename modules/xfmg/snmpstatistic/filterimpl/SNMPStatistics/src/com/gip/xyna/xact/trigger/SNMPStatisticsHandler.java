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
package com.gip.xyna.xact.trigger;


import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.varbind.VarBind;




public class SNMPStatisticsHandler extends AbstractSNMPStatisticsHandler {

  public SNMPStatisticsHandler(AbstractSNMPStatisticsHandler nextHandler) {
    super(nextHandler);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void updateMap(int i) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public VarBind get(OID oid, int i) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected OID getBase() {
    // TODO Auto-generated method stub
    return null;
  }
}
