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
package com.gip.xyna.utils.snmp.varbind;

import org.snmp4j.PDU;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

public class VarBindTypeConverterImplApache implements VarBindTypeConverter<VariableBinding> {

  public VariableBinding convert(NullVarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new Null() );
  }

  public VariableBinding convert(IntegerVarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new Integer32(vb.intValue()) );
  }

  public VariableBinding convert(UnsIntegerVarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new UnsignedInteger32( vb.longValue() ) );
  }

  public VariableBinding convert(StringVarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new OctetString((String) vb.getValue()) );
  }

  public VariableBinding convert(OIDVarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new OID((String) vb.getValue()) );
  }
  
  public VariableBinding convert(ByteArrayVarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new OctetString( vb.bytesValue() ) );
  }

  public VariableBinding convert(IpAddressVarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new IpAddress((String)vb.getValue()));
  }

  public VariableBinding convert(Counter64VarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new Counter64(vb.longValue()));
  }
  
  public VariableBinding convert(TimeTicksVarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new TimeTicks(vb.longValue()));
  }
  
  public VariableBinding convert(Gauge32VarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new Gauge32(vb.longValue()));
  }
  
  public VariableBinding convert(Counter32VarBind vb) {
    return new VariableBinding( new OID(vb.getObjectIdentifier()), new Counter32(vb.longValue()));
  }

  public VarBindList toVarBindList(PDU pdu) {
    VarBindList response = new VarBindList();
    for( int i=0; i<pdu.size(); ++i ) {
      VariableBinding vb = pdu.get(i);
      String oid = "." + vb.getOid();
      Variable var = vb.getVariable();
      if( var instanceof Null ) {
        response.add( new NullVarBind( oid, var.getSyntax() ) );
        continue;
      }
      if( var instanceof Integer32 ) {
        response.add( new IntegerVarBind( oid, var.toInt() ) );
        continue;
      }
      if( var instanceof OctetString ) {
        response.add( new StringVarBind( oid, var.toString() ) );
        continue;
      }
      if( var instanceof UnsignedInteger32 ) {
        if (var instanceof TimeTicks) {
          response.add(new TimeTicksVarBind(oid, var.toLong()));
          continue;
        }
        if (var instanceof Gauge32) {
          response.add(new Gauge32VarBind(oid, var.toLong()));
          continue;
        }
        if (var instanceof Counter32) {
          response.add(new Counter32VarBind(oid, var.toLong()));
          continue;
        }
        response.add( new UnsIntegerVarBind( oid, var.toLong() ) );
        continue;
      }
      if( var instanceof OID ) {
        response.add( new OIDVarBind( oid, var.toString() ) );
        continue;
      }
      if (var instanceof IpAddress) {
        response.add(new StringVarBind(oid, var.toString()));
        continue;
      }
      if (var instanceof Counter64) {
        response.add(new Counter64VarBind(oid, var.toLong()));
        continue;
      }
      
      
      throw new IllegalStateException("Unknown Variable "+ var.getClass().getName() ); 
    }
    return response;
  }

}
