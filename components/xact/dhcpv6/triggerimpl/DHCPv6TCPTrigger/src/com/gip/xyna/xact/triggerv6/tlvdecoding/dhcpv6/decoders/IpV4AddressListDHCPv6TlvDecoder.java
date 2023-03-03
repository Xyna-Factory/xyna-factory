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

/**
 * IPv4 address list DOCSIS TLV decoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class IpV4AddressListDHCPv6TlvDecoder extends AbstractDHCPv6TlvDecoder {

    public IpV4AddressListDHCPv6TlvDecoder(long typeEncoding, String typeName) {
        super(typeEncoding, typeName);
    }

    @Override
    protected String decodeTlvValue(final byte[] value) {
        if (value.length == 0) {
            return "";
        } else if (value.length % 4 != 0) {
            throw new IllegalArgumentException("Invalid length: <" + value.length + ">.");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length; ++i) {
            if (i > 0) {
                if (i % 4 == 0) {
                    sb.append(",");
                } else {
                    sb.append(".");
                }
            }
            int part = ((int) value[i]) & 0xFF; 
            sb.append(part);
        }
        return sb.toString();
    }
}
