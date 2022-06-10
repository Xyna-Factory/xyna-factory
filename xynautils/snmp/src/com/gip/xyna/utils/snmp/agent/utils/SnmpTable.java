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

import com.gip.xyna.utils.snmp.NextOID;
import com.gip.xyna.utils.snmp.OID;


/**
 * SnmpTable ist ein Tool, um recht einfach eine statische Tabelle ueber SNMP abfragbar zu machen.
 * Die einzelnen Tabellenzeilen muessen ohne Luecke im Index aufeinanderfolgen.
 * 
 * Statisch heisst, dass derzeit nur GET und GET_NEXT unterstuetzt sind.
 *
 * Die angzeigten Daten werden ueber eine Implementierung des {@link SnmpTableModel} geliefert. 
 */
public class SnmpTable extends AbstractSnmpTable {
  
  private NextOID nextOID;

  /**
   * Konstruktor
   * @param snmpTableModel
   * @param oidBase
   */
  public SnmpTable(SnmpTableModel snmpTableModel, OID oidBase) {
    super(snmpTableModel, oidBase);
    nextOID = new NextOID( tableModel.getColumnCount(), tableModel.getRowCount() );
  }

  /**
   * Konstruktor
   * @param snmpTableModel
   * @param oidBase
   * @param nextOHS
   */
  public SnmpTable(SnmpTableModel snmpTableModel, OID oidBase, ChainedOidSingleHandler nextOHS) {
    super(snmpTableModel, oidBase, nextOHS);
    nextOID = new NextOID( tableModel.getColumnCount(), tableModel.getRowCount() );
  }

  @Override
  public void refreshTableModel() {
    int r = tableModel.getRowCount();
    int c = tableModel.getColumnCount();
    if( nextOID.size(0) != c || nextOID.size(1) != r ) {
      nextOID = new NextOID(c,r);
    }
  }

  @Override
  protected OID getNextOID(OID oid) {
    OID subOid = oid.subOid(oidBase.length());
    try {
      OID next = nextOID.getNext( subOid );
      return oidBase.append(next);
    } catch ( NextOID.NoNextOIDException e ) {
      return null;
    }
  }
  
}
  