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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;





/**
 * IPv4 address list tlv encoder.
 */
public final class IpV4AddressListTlvEncoder extends AbstractTypeWithValueTlvEncoder {


    private static final int MAXIMUM_NUMBER_OF_CONTAINED_IPS = DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH
            / IpV4AddressTlvEncoder.IPV4_ADDRESS_LENGTH; // = 63

    public IpV4AddressListTlvEncoder(int typeEncoding) {
        super(typeEncoding);
    }

    @Override
    protected void writeTypeWithValueNode(TypeWithValueNode node, OutputStream target) throws IOException {
        String value = node.getValue();
        if ("".equals(value)) {
            target.write(this.getTypeEncoding());
            target.write(0);
            return;
        }
        String[] ips = SPLIT_AT_COMMA_PATTERN.split(value, -1);        
        if (ips.length > MAXIMUM_NUMBER_OF_CONTAINED_IPS) {
            throw new IllegalArgumentException("Expected <" + MAXIMUM_NUMBER_OF_CONTAINED_IPS
                    + ">, or less, ip-addresses, but was more: <" + node.getValue() + ">.");
        }
        List<byte[]> bytes = new ArrayList<byte[]>();
        for (String ip : ips) {
            try {
                bytes.add(IpV4AddressTlvEncoder.ipV4AddressToBytes(ip));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to parse ip address in: <" + value + ">.", e);
            }
        }
        target.write(ByteUtil.toByteArray(this.getTypeEncoding(),2));
        target.write(ByteUtil.toByteArray((ips.length * IpV4AddressTlvEncoder.IPV4_ADDRESS_LENGTH),2));
        for (byte[] ipBytes : bytes) {
            target.write(ipBytes);
        }
    }
}
