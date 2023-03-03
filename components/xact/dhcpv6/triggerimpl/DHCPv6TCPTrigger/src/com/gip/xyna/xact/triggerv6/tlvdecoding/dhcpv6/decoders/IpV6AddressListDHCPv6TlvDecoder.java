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
package com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IPv4 address list DOCSIS TLV decoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class IpV6AddressListDHCPv6TlvDecoder extends AbstractDHCPv6TlvDecoder {

    public IpV6AddressListDHCPv6TlvDecoder(long typeEncoding, String typeName) {
        super(typeEncoding, typeName);
    }

    @Override
    protected String decodeTlvValue(final byte[] value) {
        if (value.length == 0) {
            return "";
        } else if (value.length % 16 != 0) {
            throw new IllegalArgumentException("Invalid length: <" + value.length + ">.");
        }
        StringBuilder sb = new StringBuilder();
        byte[] tmpbytes = new byte[16];
        int counter=0;
        for (int i = 0; i < value.length; ++i) {
            tmpbytes[counter] = value[i];
            counter++;
            
             if (counter > 0) {
                if (counter % 16 == 0) {
                    
                    try {
                      sb.append(InetAddress.getByAddress(tmpbytes).getHostAddress());
                    }
                    catch (UnknownHostException e) {
                      throw new IllegalArgumentException("Invalid IPv6 Address in List!");
                    }
                    sb.append(",");
                    counter=0;
                } 
            }
        }
        return sb.toString().substring(0, sb.toString().length()-1);
    }
}
