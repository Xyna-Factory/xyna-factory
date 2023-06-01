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

import java.util.regex.Pattern;

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;



/**
 * Abstract DOCSIS TLV decoder.
 */
public abstract class AbstractDHCPv6TlvDecoder implements DHCPv6TlvDecoder {

    private final long typeEncoding;
    private final String typeName;
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\n");
    //private static final Pattern STARTS_WITH_LINE_BREAK_PATTERN = Pattern.compile("^\\n(.*\\n*)*$");

    private final String SEPARATOR = ":";

    public AbstractDHCPv6TlvDecoder(final long typeEncoding, final String typeName) {
        if (typeEncoding < 0){ //|| typeEncoding > 65536) {
            throw new IllegalArgumentException("Invalid type encoding: <" + typeEncoding + ">.");
        } else if (typeName == null) {
            throw new IllegalArgumentException("Type name may not be null.");
        }
        this.typeEncoding = typeEncoding;
        this.typeName = typeName;
    }

    public final long getTypeEncoding() {
        return this.typeEncoding;
    }

    public final String getTypeName() {
        return this.typeName;
    }

    public final String decode(final Tlv tlv) {
        if (tlv == null) {
            throw new IllegalArgumentException("Tlv may not be null.");
        } else if (tlv.getTypeEncoding() != this.typeEncoding) {
//            if(tlv.getTypeEncoding()!=17)throw new IllegalArgumentException("Tlv does not match type encoding <" + this.typeEncoding + ">: <" + tlv
//                    + ">.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.typeName);
        String value = decodeTlvValue(tlv.getValueAsByteArray());
        boolean scnd = value.startsWith("\n");
        //boolean scnd = STARTS_WITH_LINE_BREAK_PATTERN.matcher(value).matches();
        if (!(value.length() == 0 || scnd)) { //
            sb.append(SEPARATOR);
        }
        sb.append(LINE_BREAK_PATTERN.matcher(value).replaceAll("\n  ")); //value.replaceAll("\\n", "\n  ")); // note that this handles indentation.
        return sb.toString();
    }

    protected abstract String decodeTlvValue(byte[] value);
}
