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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories;

import java.util.Map;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.Unsigned1to7ByteIntegerTlvEncoder;



/**
 * Unsigned integer tlv encoder factory.
 */
public class UnsignedIntegerTlvEncoderFactory extends AbstractTypeWithValueTlvEncoderFactory {

    @Override
    protected TlvEncoder createTypeWithValueTlvEncoder(int typeEncoding, Map<String, String> arguments) {
        if (!arguments.containsKey("nrBytes")) {
            throw new IllegalArgumentException("Mandatory argument <nrBytes> missing from provided arguments: <"
                    + arguments + ">.");
        }
        int nrBytes = -1;
        Long minValue = null;
        Long maxValue = null;
        Long multipleOf = null;
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            try {
                if ("nrBytes".equals(entry.getKey())) {
                    nrBytes = Integer.parseInt(entry.getValue());
                } else if ("minValue".equals(entry.getKey())) {
                    minValue = Long.parseLong(entry.getValue());
                } else if ("maxValue".equals(entry.getKey())) {
                    maxValue = Long.parseLong(entry.getValue());
                } else if ("multipleOf".equals(entry.getKey())) {
                    multipleOf = Long.parseLong(entry.getValue());
                } else {
                    throw new IllegalArgumentException("Unknown argument: <" + entry.getKey() + ">.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Argument <" + entry.getKey() + "> has an illegal value: <"
                        + entry.getValue() + ">.", e);
            }
        }
        if (nrBytes < 8) {
            return new Unsigned1to7ByteIntegerTlvEncoder(typeEncoding, nrBytes, minValue, maxValue, multipleOf);
        } else {
            throw new IllegalArgumentException("No <UnsignedInteger> implementation available for <nrBytes="
                    + nrBytes + ">.");
        }
    }
}
