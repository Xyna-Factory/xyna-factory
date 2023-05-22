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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoderFactory;



/**
 * Abstract TLV encoder factory.
 */
public abstract class AbstractTlvEncoderFactory implements TlvEncoderFactory {

    public final TlvEncoder create(final int typeEncoding, final Map<String, String> arguments,
            final Map<String, TlvEncoder> subNodeEncodings) {
        if (typeEncoding < 0) {
            throw new IllegalArgumentException("Illegal type encoding (less than 0): <" + typeEncoding + ">.");
        } else if (typeEncoding > 65536) {
            //throw new IllegalArgumentException("Illegal type encoding (larger than 255): <" + typeEncoding + ">.");
         
        } else if (arguments == null) {
            throw new IllegalArgumentException("Arguments may not be null.");
        } else if (subNodeEncodings == null) {
            throw new IllegalArgumentException("Sub nodes encodings may not be null.");
        }
        return createTlvEncoder(typeEncoding, validateAndMakeUnmodifiable(arguments), subNodeEncodings);
    }

    private static Map<String, String> validateAndMakeUnmodifiable(final Map<String, String> map) {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null) {
                throw new IllegalArgumentException("Key may not be null.");
            } else if (value == null) {
                throw new IllegalArgumentException("Value may not be null for key <" + key + ">.");
            }
            result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }

    protected abstract TlvEncoder createTlvEncoder(int typeEncoding, Map<String, String> arguments,
            Map<String, TlvEncoder> subNodeEncodings);
}
