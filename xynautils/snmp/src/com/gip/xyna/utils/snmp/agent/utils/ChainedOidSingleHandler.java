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
package com.gip.xyna.utils.snmp.agent.utils;

import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.varbind.NullVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;

/**
 * ChainedOidSingleHandler dient dazu mehrere OidSingleHandler aneinanderzuhaengen,
 * damit ein Walk fortgesetzt werden kann.
 *
 */
public abstract class ChainedOidSingleHandler extends AbstractOidSingleHandler {

  protected OID oidBase;
  
  public ChainedOidSingleHandler() {
  }
  
  public ChainedOidSingleHandler(OID oidBase) {
    this.oidBase = oidBase;
  }
  
  public OID startOID() {
    return oidBase;
  }

  /**
   * WALK_END ist eine fertige Implementierung des ChainedOidSingleHandler-Interfaces,
   * die dazu dient, dass das Ende eines Walks eine zu hohe OID zurueckgibt.
   * Diese WALK_END sollte in allen ChainedOidSingleHandler der Default sein.
   */
  public static ChainedOidSingleHandler WALK_END = new ChainedOidSingleHandler() {
    OID walkEnd = new OID(".1.3");

    public OID startOID() {
      return walkEnd;
    }

    public boolean matches(SnmpCommand snmpCommand, OID oid) {
      return snmpCommand == SnmpCommand.GET_NEXT;
    }

    public VarBind getNext(OID oid, VarBind varBind, int i) {
      return new NullVarBind(walkEnd.getOid());
    }

    public VarBind get(OID oid, int i) { throw new UnsupportedOperationException(); }
    public void set(OID oid, VarBind varBind, int i) { throw new UnsupportedOperationException(); }
  };

}