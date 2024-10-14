/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.IpV4AddressValidator;



/**
 * IPv4 address tlv encoder.
 */
public final class IpV4AddressTlvEncoder extends AbstractTypeWithValueTlvEncoder {

    static final int IPV4_ADDRESS_LENGTH = 4;


    public IpV4AddressTlvEncoder(final int typeEncoding) {
        super(typeEncoding);
    }

    @Override
    protected void writeTypeWithValueNode(final TypeWithValueNode node, final OutputStream target) throws IOException {
        target.write(ByteUtil.toByteArray(this.getTypeEncoding(),2));
        target.write(ByteUtil.toByteArray(IPV4_ADDRESS_LENGTH,2));
        target.write(ipV4AddressToBytes(node.getValue()));
    }

    static byte[] ipV4AddressToBytes(String ipAddress) {
        if (!IpV4AddressValidator.isValid(ipAddress)) {
            throw new IllegalArgumentException("Ivalid IPv4 address: <" + ipAddress + ">.");
        }
        String[] parts = SPLIT_AT_DOT_PATTERN.split(ipAddress);
        byte[] result = new byte[IPV4_ADDRESS_LENGTH];
        for (int i = 0; i < result.length; ++i) {
            result[i] = (byte) Integer.parseInt(parts[i]);
        }
        return result;
    }

}
