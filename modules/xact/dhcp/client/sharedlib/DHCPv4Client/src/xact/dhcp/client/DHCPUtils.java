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
package xact.dhcp.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.ByteUtils;
import com.gip.xyna.xact.dhcp.RawBOOTPPacket;

public class DHCPUtils {

  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPUtils.class);

  
  private static void putByte(EnumMap<DHCPFields, Object> map, DHCPFields key, byte b) {
    map.put(key, Integer.valueOf(b) );
  }
  private static void putHexString(EnumMap<DHCPFields, Object> map, DHCPFields key, byte[] bs) {
    map.put(key, ByteUtils.toHexString(bs, false, "") );
  }
  private static void putShort(EnumMap<DHCPFields, Object> map, DHCPFields key, byte[] bs) {
    map.put(key, Integer.valueOf(ByteUtils.readShort(bs,  0) ) );
  }
  private static void putInetAddress(EnumMap<DHCPFields, Object> map, DHCPFields key, byte[] addr) {
    try {
      InetAddress iAddr = InetAddress.getByAddress(addr);
      map.put(key, iAddr.getHostAddress() );
    }
    catch (UnknownHostException e) {
      logger.warn("UnknownHostException for "+key, e);
      map.put(key, "" );
    }
  }
  
  private static void putHardwareAddress(EnumMap<DHCPFields, Object> map, DHCPFields key, byte[] addr) {
    logger.info("CHAddr  "+ addr.length +" "+ ByteUtils.toHexString(addr, false, ":") ); //FIXME
    map.put(key, ByteUtils.toHexString(addr, false, ":") );
  }

  private static void putString(EnumMap<DHCPFields, Object> map, DHCPFields key, byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for( int i=0; i< bytes.length; ++i ) {
      if(  bytes[i] == 0 ) {
        break;
      }
      sb.append( (char) bytes[i] ); //FIXME encoding?
    }
    map.put(key, sb.toString() );
  }
  
  public enum DHCPFields {
    op,
    htype,
    hlen,
    hops,
    xid,
    secs,
    flags,
    ciaddr,
    yiaddr,
    siaddr,
    giaddr,
    chaddr,
    sname,
    file
    ;
    
  }
  
  
  public static EnumMap<DHCPFields,Object> analyzePacket(RawBOOTPPacket packet) {
    EnumMap<DHCPFields,Object> map = new EnumMap<DHCPFields,Object>(DHCPFields.class);
    
    putByte( map, DHCPFields.op, packet.getOpRaw() );
    putByte( map, DHCPFields.htype, packet.getHTypeRaw() );
    putByte( map, DHCPFields.hlen,  packet.getHLenRaw() );
    putByte( map, DHCPFields.hops, packet.getHopsRaw() );
    
    putHexString( map, DHCPFields.xid, packet.getXIDRaw() );
    putShort( map, DHCPFields.secs, packet.getSecsRaw() );
    putShort( map, DHCPFields.flags, packet.getFlagsRaw() );
    
    putInetAddress( map, DHCPFields.ciaddr, packet.getCIAddrRaw() );
    putInetAddress( map, DHCPFields.yiaddr, packet.getYIAddrRaw() );
    putInetAddress( map, DHCPFields.siaddr, packet.getSIAddrRaw() );
    putInetAddress( map, DHCPFields.giaddr, packet.getGIAddrRaw() );
    
    putHardwareAddress( map, DHCPFields.chaddr, packet.getCHAddrRaw() );
    
    putString(map, DHCPFields.sname,  packet.getSNameRaw() );
    putString(map, DHCPFields.file,  packet.getFileRaw() );
    
    return map;
  }
  
  public static byte[] extractOptions( byte[] data ) {
    // Kopf der Nachricht mit Magic Cookie abhacken und nur Optionen behalten
    List<Byte> optiondata = new ArrayList<Byte>();
    // search for endmarker
    int endmarker = 0;
    for (int i = data.length - 1; i >= 0; i--) {
      if (data[i] == -1) {
        endmarker = i;
        break;
      }
    }
    for (int z = 240; z <= endmarker; z++) {
      optiondata.add(data[z]);
    }

    // Padding beachten
    while (optiondata.size() % 4 != 0) {
      optiondata.add((byte) 0);
    }

    // Liste in Array schreiben (geht sicher besser?)
    byte[] optarg = new byte[optiondata.size()];
    for (int z = 0; z < optiondata.size(); z++) {
      optarg[z] = optiondata.get(z);
    }
   
    return optarg;
  }

}
