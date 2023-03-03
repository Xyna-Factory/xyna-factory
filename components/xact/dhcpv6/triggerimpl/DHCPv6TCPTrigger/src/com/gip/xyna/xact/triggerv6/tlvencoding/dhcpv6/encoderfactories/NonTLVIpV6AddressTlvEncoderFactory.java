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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories;

import java.util.Map;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.NonTLVIpV6AddressTlvEncoder;



/**
 * IPv4 address TLV encoder factory.
 */
public final class NonTLVIpV6AddressTlvEncoderFactory extends AbstractTypeWithValueTlvEncoderFactory {

    @Override
    protected TlvEncoder createTypeWithValueTlvEncoder(final int typeEncoding, final Map<String, String> arguments) {
        if (arguments.size() > 0) {
            throw new IllegalArgumentException("Expected no arguments when creating <IpV4Address> encoder, but got: <"
                    + arguments + ">.");
        }
        return new NonTLVIpV6AddressTlvEncoder(typeEncoding);
    }
}
