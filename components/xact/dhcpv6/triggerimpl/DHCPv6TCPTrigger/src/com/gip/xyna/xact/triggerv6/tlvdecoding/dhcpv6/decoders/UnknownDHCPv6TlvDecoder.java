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
package com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders;

import com.gip.xyna.xact.triggerv6.tlvdecoding.utilv6.OctetStringUtil;




/**
 * DOCSIS TLV decoder for unknown TLVs.
 */
public final class UnknownDHCPv6TlvDecoder extends AbstractDHCPv6TlvDecoder {

    public UnknownDHCPv6TlvDecoder(long typeEncoding) {
        super(typeEncoding, "Tlv");
    }

    @Override
    protected String decodeTlvValue(byte[] value) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getTypeEncoding());
        sb.append(":");
        // TODO: this should be defined as a constant somewhere.
        sb.append("OctetString");
        if (value.length > 0) {
            sb.append(":");
            sb.append(OctetStringUtil.toString(value));
        }
        return sb.toString();
    }
}
