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
 * Einfache Implementierung eines OidSingleHandler, der auf weitere 
 * OidSingleHandler in dem Baum verzweigt.
 * 
 * Die ihm bekannten OidSingleHandler werden ueber eine Enumeration 
 * {@code Index} und die Funktion {@link add} mitgeteilt. Fuer die haeufig 
 * vorkommenden Enden (Blaetter im MIB-Baum) verinfacht eine 
 * Funktion {@link addLeaf} die Benutzung.
 *
 * @param <Index> Eine Enumeration
 */
public class EnumOidSingleHandler<Index extends Enum<Index>> extends ChainedOidSingleHandler {

  private OidSingleHandler[] oshs;
  private OID oidBase;

  /**
   * Konstruktor
   * @param indexType
   * @param oidBase
   */
  public EnumOidSingleHandler(Class<Index> indexType, OID oidBase ) {
    oshs = new OidSingleHandler[indexType.getEnumConstants().length+2];
    oshs[oshs.length-1] = ChainedOidSingleHandler.WALK_END;
    this.oidBase = oidBase;
    
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.snmp.OidSingleHandler#matches(com.gip.xyna.demon.snmp.OidSingleHandler.SnmpCommand, com.gip.xyna.utils.snmp.OID)
   */
  public boolean matches(SnmpCommand snmpCommand, OID oid) {
    if( ! oid.startsWith(oidBase) ) {
      return false;
    }
    if( oid.length()==oidBase.length() ) {
      //nur bei GET_NEXT zugelassen
      return snmpCommand == SnmpCommand.GET_NEXT;
    }
    int index = getIndex(oid);
    if( index >= oshs.length ) {
      return false;
    }
    return oshs[index].matches(snmpCommand,oid);
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.demon.snmp.OidSingleHandler#get(com.gip.xyna.utils.snmp.OID, int)
   */
  public VarBind get(OID oid, int i) {
    return oshs[getIndex(oid)].get(oid,i);
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.snmp.OidSingleHandler#getNext(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public VarBind getNext(OID oid, VarBind varBind, int i) {
    int index = getIndex(oid);
    OidSingleHandler osh = oshs[index];
    
    if( osh instanceof LeafOidSingleHandler ) {
      osh = oshs[index+1]; //Leaf ist fertig, daher mit naechstem Leaf weiter
    }
    
    if( osh instanceof LeafOidSingleHandler ) {
      OID next = oidBase.append( index+1 );
      return osh.get(next, i);
    } else {
      return osh.getNext( oid, varBind, i);
    }
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.demon.snmp.OidSingleHandler#set(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public void set(OID oid, VarBind varBind, int i) {
    oshs[getIndex(oid)].set(oid,varBind,i);
  }

  /**
   * fuegt ein neues Blatt in den OID-Baum ein
   * @param <Value>
   * @param index
   * @param valueType
   * @param access
   */
  public <Value> void addLeaf(Index index, Class<Value> valueType, Access.Type<Value> access) {
    add( index, new LeafOidSingleHandler<Value,Access.Type<Value>>(access,valueType) );
  }

  /**
   * fuegt einen Zweig in den OID-Baum ein
   * @param index
   * @param osh
   */
  public void add(Index index, OidSingleHandler osh ) {
    int idx = index.ordinal();
    if( idx == 0 ) {
      oshs[0] = osh; //Zusaetzlicher Eintrag, um Walks zu vereinfachen
    }
    oshs[idx+1] = osh;
  }
  
  /**
   * @param oid
   * @return
   */
  private int getIndex(OID oid) {
    String index = oid.getIndex( oidBase.length() );
    if( index == null ) {
      return 0;
    }
    return Integer.parseInt( index );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.ChainedOidSingleHandler#startOID()
   */
  public OID startOID() {
    return oidBase;
  }

}
