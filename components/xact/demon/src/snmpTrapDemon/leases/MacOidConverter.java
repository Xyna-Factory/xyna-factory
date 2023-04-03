/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package snmpTrapDemon.leases;

import com.gip.xyna.utils.snmp.OID;

public class MacOidConverter {

  public static String toLongOid(MacAddress mac) {
    if( mac == null ) {
      throw new IllegalArgumentException("mac is null");
    }
    StringBuilder sb = new StringBuilder();
    for( int p : mac.toIntArray() ) {
      sb.append('.').append( p );
    }
    return sb.toString();
  }
  
  public static String toOid(MacAddress mac) {
    if( mac == null ) {
      throw new IllegalArgumentException("mac is null");
    }
    int[] parts = mac.toIntArray();
    StringBuilder sb = new StringBuilder();
    sb.append('.').append( (parts[0]<<16) + (parts[1]<<8) + parts[2] );
    sb.append('.').append( (parts[3]<<16) + (parts[4]<<8) + parts[5] );
    return sb.toString();
  }

  public static MacAddress toMac(String oid) {
    return toMac( new OID(oid) );
  }
  
  public static MacAddress toMac(OID oid) {
    int parts[] = new int[6];
    if( oid.length() == 2 ) {
      long mac = Long.parseLong( oid.getIndex(0) ) * 0x1000000L + Long.parseLong( oid.getIndex(1) );
      return MacAddress.parse( mac );
    } else if( oid.length() == 6 ) {
      for( int i=0; i<6; ++i ) {
        parts[i] = Integer.parseInt( oid.getIndex(i) );
      }
      return MacAddress.parse(parts);
    } else {
      throw new IllegalArgumentException("Oid could not be parsed");
    }
  }

  
  
}
