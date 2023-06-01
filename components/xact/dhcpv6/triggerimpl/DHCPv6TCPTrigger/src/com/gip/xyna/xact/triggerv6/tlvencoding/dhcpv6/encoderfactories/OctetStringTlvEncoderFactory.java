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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories;

import java.util.Map;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.OctetStringTlvEncoder;



/**
 * Octet string TLV encoder factory.
 */
public final class OctetStringTlvEncoderFactory extends AbstractTypeWithValueTlvEncoderFactory {

    @Override
    protected TlvEncoder createTypeWithValueTlvEncoder(final int typeEncoding, final Map<String, String> arguments) {
        int minLength = 1;
        int maxLength = DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH;
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            if ("minLength".equals(entry.getKey())) {
                minLength = parseLengthValue(entry);
            } else if ("maxLength".equals(entry.getKey())) {
                maxLength = parseLengthValue(entry);
            } else {
                throw new IllegalArgumentException("Unknown argument: <" + entry.getKey() + ">.");
            }
        }
        return new OctetStringTlvEncoder(typeEncoding, minLength, maxLength);
    }

    private static int parseLengthValue(final Map.Entry<String, String> entry) {
        String value = entry.getValue(); 
        if (!value.matches("([1-9][0-9]?)?[0-9]")) {
            throw new IllegalArgumentException("Key <" + entry.getKey() + "> contains an illegal value: <"
                    + entry.getValue() + ">.");
        }
        int parsedValue = Integer.parseInt(value);
        if (parsedValue > DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH || parsedValue < 1) {
            throw new IllegalArgumentException("Argument <" + entry.getKey() + "> is not in range [1, "
                    + DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH + "]: <" + entry.getValue() + ">.");
        }
        return parsedValue;
    }
}
