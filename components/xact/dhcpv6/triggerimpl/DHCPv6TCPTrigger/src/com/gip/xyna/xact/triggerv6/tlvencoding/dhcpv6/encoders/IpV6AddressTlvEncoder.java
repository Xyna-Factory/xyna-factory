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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;



/**
 * IPv4 address tlv encoder.
 */
public final class IpV6AddressTlvEncoder extends AbstractTypeWithValueTlvEncoder {

    static final int IPV6_ADDRESS_LENGTH = 16;

    public IpV6AddressTlvEncoder(final int typeEncoding) {
        super(typeEncoding);
    }

    @Override
    protected void writeTypeWithValueNode(final TypeWithValueNode node, final OutputStream target) throws IOException {
        target.write(ByteUtil.toByteArray(this.getTypeEncoding(),2));
        target.write(ByteUtil.toByteArray(IPV6_ADDRESS_LENGTH,2));
        target.write(ipV6AddressToBytes(node.getValue()));
    }

    static byte[] ipV6AddressToBytes(String ipAddress) {

      InetAddress ipv6 = null;
      try {
        ipv6 = InetAddress.getByName(ipAddress);
      }
      catch (Exception e) {
        // TODO Auto-generated catch block
        throw new IllegalArgumentException("Invalid IPv6 address: <" + ipAddress + ">.");
      }
      if(!(ipv6 instanceof Inet6Address)) throw new IllegalArgumentException("Invalid IPv6 address: <" + ipAddress + ">.");
        
      
      byte[] result = ipv6.getAddress();    
      
      return result;
    }
}
