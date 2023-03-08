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

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.NonTLVOctetStringDHCPv6TlvDecoder;


;

/**
 * Octet string DOCSIS TLV decoder factory.
 */
public final class NonTLVOctetStringDHCPv6TlvDecoderFactory extends AbstractDHCPv6TlvDecoderFactory {

    public static final String DATA_TYPE_NAME = "NonTLVOctetString";

    public NonTLVOctetStringDHCPv6TlvDecoderFactory() {
        super(DATA_TYPE_NAME);
    }

    public NonTLVOctetStringDHCPv6TlvDecoderFactory(final String dataTypeName) {
        super(dataTypeName);
    }

    @Override
    protected DHCPv6TlvDecoder createTlvDecoder(final long typeEncoding, final String typeName) {
        return new NonTLVOctetStringDHCPv6TlvDecoder(typeEncoding, typeName);
    }
}
