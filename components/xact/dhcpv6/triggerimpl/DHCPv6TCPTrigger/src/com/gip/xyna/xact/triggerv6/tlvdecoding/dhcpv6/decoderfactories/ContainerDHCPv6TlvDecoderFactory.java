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
package com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories;

import java.util.List;
import java.util.Set;

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.ContainerDHCPv6TlvDecoder;




/**
 * DOCSIS Container TLV decoder factory.
        return DATA_TYPE_NAME;
    }

    public DHCPv6TlvDecoder create(final long typeEncoding, final String typeName,
            final List<DHCPv6TlvDecoder> subTlvDecoders,Set<Integer> multipleEncodingsAllowed) {
        if (typeEncoding < 0){ //|| typeEncoding > 65536) {
            throw new IllegalArgumentException("Invalid type encoding: <" + typeEncoding + ">.");
        } else if (typeName == null) {
            throw new IllegalArgumentException("Type name may not be null.");
        } else if (subTlvDecoders == null) {
            throw new IllegalArgumentException("Sub TLV decoders may not be null.");
        }
        return new ContainerDHCPv6TlvDecoder(typeEncoding, typeName, subTlvDecoders);
    }
}
