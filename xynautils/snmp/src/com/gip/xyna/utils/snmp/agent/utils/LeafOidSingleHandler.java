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
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.agent.utils.Access.ReadWrite;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.IntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.NullVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;

/**
 * Behandlung eine Blattes im MIB-Baum.
 * 
 * Datentyp und Zugriffsart werden ueber die generischen 
 * Parameter {@code Value und AccessType} angeben.
 * Der LeafOidSingleHandler kann keine GetNext-Requests bearbeiten, 
 * da er die Nachfolger nicht kennen kann.
 *
 * @param <Value>  Datentyp
 * @param <Access> Zugriffsart ReadOnly oder ReadWrite
 */
public class LeafOidSingleHandler<Value,AccessType extends Access.Type<Value> > extends AbstractOidSingleHandler {
  
  private Access.Type<Value> access;
  private Class<Value> valueType;

  /**
   * Konstruktor
   * @param access
   * @param valueType
   */
  public LeafOidSingleHandler(Access.Type<Value> access, Class<Value> valueType ) {
    this.access = access;
    this.valueType = valueType;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#matches(com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler.SnmpCommand, com.gip.xyna.utils.snmp.OID)
   */
  public boolean matches(SnmpCommand snmpCommand, OID oid) {
    switch( snmpCommand ) {
    case GET: return true;
    case GET_NEXT: return true; //GetNext ist zwar erlaubt, wird aber hier nicht behandelt
    case SET: return access instanceof ReadWrite;
    case INFORM: return false;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#get(com.gip.xyna.utils.snmp.OID, int)
   */
  public VarBind get(OID oid, int i) {
    Object value = access.get();
    if( value instanceof String ) {
      return new StringVarBind( oid.getOid(), (String)value );
    } else if ( value instanceof Integer ) {
      return new IntegerVarBind( oid.getOid(), (Integer)value );
    } else if ( value == null ) {
      return new NullVarBind( oid.getOid() );
    } else {
      return new StringVarBind( oid.getOid(), value.toString() );
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#getNext(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public VarBind getNext(OID oid, VarBind varBind, int i) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler#set(com.gip.xyna.utils.snmp.OID, com.gip.xyna.utils.snmp.varbind.VarBind, int)
   */
  public void set(OID oid, VarBind varBind, int i) {
    Object value = varBind.getValue();
    if( valueType.isInstance( value ) ) {
      ((ReadWrite<Value>)access).set( valueType.cast( value ) );
    } else {
      throw new SnmpRequestHandlerException( RequestHandler.BAD_VALUE, i );
    }
  }

}