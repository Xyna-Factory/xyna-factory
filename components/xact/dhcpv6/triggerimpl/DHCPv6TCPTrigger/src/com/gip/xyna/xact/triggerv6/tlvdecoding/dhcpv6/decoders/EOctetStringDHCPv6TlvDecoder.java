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
 * OctetString DOCSIS TLV decoder.
 */
public final class EOctetStringDHCPv6TlvDecoder extends AbstractDHCPv6TlvDecoder {

    public EOctetStringDHCPv6TlvDecoder(final long typeEncoding, final String typeName) {
        super(typeEncoding, typeName);
    }

    @Override
    protected String decodeTlvValue(byte[] value) {
        if (value.length < 5) {
          return "";
        }

        byte[] result = new byte[value.length-4];
        System.arraycopy(value, 4, result, 0, value.length-4);
        return OctetStringUtil.toString(result);
    }
}
