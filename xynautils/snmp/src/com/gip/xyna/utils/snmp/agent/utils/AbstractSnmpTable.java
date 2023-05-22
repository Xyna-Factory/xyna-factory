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
import com.gip.xyna.utils.snmp.varbind.VarBind;


/**
 * Basis fuer die beiden Tabellenvarianten SnmpTable und SparseSnmpTable.
 */
public abstract class AbstractSnmpTable extends ChainedOidSingleHandler {
  protected SnmpTableModel tableModel;
  protected OID oidBase;
  private int oidLength;
  private ChainedOidSingleHandler nextOHS;
  
  /**
   * Konstruktor
   * @param snmpTableModel
   * @param oidBase
   */
  public AbstractSnmpTable(SnmpTableModel snmpTableModel, OID oidBase) {
    this(snmpTableModel,oidBase,WALK_END);
  }
  
  /**
   * Konstruktor
   * @param snmpTableModel
   * @param oidBase
   * @param nextOHS
   */
  public AbstractSnmpTable(SnmpTableModel snmpTableModel, OID oidBase, ChainedOidSingleHandler nextOHS) {
    this.tableModel = snmpTableModel;
    this.oidBase = oidBase;
    oidLength = oidBase.length() + 1/*Datenspalte*/ + 1 /*Index*/; //TODO tableModel.numIndexes() Tabellen mit mehreren Indexen
    this.nextOHS = nextOHS;
  }
  
  /**
   * Diese Methode muss uafgerufen werden, falls sich das TableModel so aendert,
   * dass die Anzahl der Zeilen bzw Spalten aendert.
   */
  public abstract void refreshTableModel();

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
    case SET: return false; //TODO schreibbare Tabellen zulassen
    case INFORM: return false;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#get(com.gip.xyna.utils.snmp.OID, int)
   */
  public VarBind get(OID oid, int i) {
    int column = Integer.parseInt( oid.getIndex( oidBase.length() ) );
    int row = Integer.parseInt( oid.getIndex( oidBase.length() +1 ) );
    return VarBind.newVarBind(oid.getOid(), tableModel.get( row, column ) );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#getNext(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public VarBind getNext(OID oid, VarBind varBind, int i) {
    if( tableModel.getRowCount() == 0 ) {
      //es liegen keine Daten vor
      return nextOHS.getNext( nextOHS.startOID(), varBind, i);
    }
    OID next = getNextOID(oid);
    
    if( next != null ) {
      return get( next, i);
    } else {
      return nextOHS.getNext( nextOHS.startOID(), varBind, i);
    }
  }

  /**
   * Rueckgabe der naechsten OID fuer die getNext-Requests
   * @param oid
   * @return
   */
  protected abstract OID getNextOID(OID oid);

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
  