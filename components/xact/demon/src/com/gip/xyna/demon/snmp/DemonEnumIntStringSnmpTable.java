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
package com.gip.xyna.demon.snmp;

import com.gip.xyna.utils.snmp.NextOID;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.NextOID.NoNextOIDException;
import com.gip.xyna.utils.snmp.agent.utils.EnumIntStringSnmpTable;

/**
 * DemonEnumIntStringSnmpTable erweitert EnumIntStringSnmpTable,
 * damit diese passend wird für die Tabellen der Demon-MIB. (OIDs enthalten zusäzliche Teile)
 */
public class DemonEnumIntStringSnmpTable<Index extends Enum<Index>> extends EnumIntStringSnmpTable<Index> {

  private int demonIndex;

  /**
   * @param indexType
   * @param oidBase
   * @param demonIndex  
   */
  public DemonEnumIntStringSnmpTable(Class<Index> indexType, OID oidBase, String demonIndex ) {
    super(indexType, oidBase);
    this.getSetLength = oidBaseLength+4;//type, "1", index, demonIndex
    this.demonIndex = Integer.parseInt(demonIndex);
    this.indexPos = oidBaseLength + 2;
    this.nextOID = new NextOID(2,1,leavesSize);
  }
  
  /**
   * @param oid
   * @return
   * @throws NoNextOIDException 
   */
  @Override
  protected OID getNextOID(OID oid) throws NoNextOIDException {
    OID next = nextOID.getNext( oid.subOid(oidBaseLength) ).append(demonIndex);
    return next;
  }


}
