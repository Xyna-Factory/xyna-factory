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
package com.gip.xyna.utils.snmp.agent.utils;

import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.varbind.VarBind;


/**
 * Tabelle, die mit mehreren Tabellenindexen (fuer mehrdimensionale Tabellen) umgehen kann.
 */
public class MultiIndexSnmpTable extends ChainedOidSingleHandler {
  protected MultiIndexSnmpTableModel tableModel;
  protected OID oidBase;
  private int oidBaseLength;
  private int oidLength;
  private ChainedOidSingleHandler nextOHS;
  
  /**
   * Konstruktor
   * @param multiIndexSnmpTableModel
   * @param oidBase
   */
  public MultiIndexSnmpTable(MultiIndexSnmpTableModel multiIndexSnmpTableModel, OID oidBase) {
    this(multiIndexSnmpTableModel,oidBase,WALK_END);
  }
  
  /**
   * Konstruktor
   * @param multiIndexSnmpTableModel
   * @param oidBase
   * @param nextOHS
   */
  public MultiIndexSnmpTable(MultiIndexSnmpTableModel multiIndexSnmpTableModel, OID oidBase, ChainedOidSingleHandler nextOHS) {
    this.tableModel = multiIndexSnmpTableModel;
    this.oidBase = oidBase;
    oidBaseLength = oidBase.length();
    oidLength = oidBase.length() + 1 + tableModel.numIndexes();
    this.nextOHS = nextOHS;
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#matches(com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler.SnmpCommand, com.gip.xyna.utils.snmp.OID)
   */
  public boolean matches(SnmpCommand snmpCommand, OID oid) {
    if( ! oid.startsWith(oidBase) ) {
      return false;
    }
    if( oid.length() > oidLength ) {
      return false; //oid zu lang
    }
    if( oid.length() < oidLength ) {
      //nur bei GET_NEXT zugelassen
      return snmpCommand == SnmpCommand.GET_NEXT;
    }
    switch( snmpCommand ) {
    case GET: return true;
    case GET_NEXT: return true;
    case SET: return false;
    case INFORM: return false;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#get(com.gip.xyna.utils.snmp.OID, int)
   */
  public VarBind get(OID oid, int i) {
    return VarBind.newVarBind(oid.getOid(), tableModel.get( oid.subOid(oidBaseLength) ) );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#getNext(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public VarBind getNext(OID oid, VarBind varBind, int i) {
    OID next = tableModel.getNextOID(oid.subOid(oidBaseLength) );
    
    if( next != null ) {
      return VarBind.newVarBind(oidBase.append(next).getOid(), tableModel.get( next ) );
    } else {
      return nextOHS.getNext( nextOHS.startOID(), varBind, i);
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#set(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public void set(OID oid, VarBind varBind, int i) {
    throw new UnsupportedOperationException();
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.ChainedOidSingleHandler#startOID()
   */
  public OID startOID() {
    return oidBase;
  }
  
}
  