/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xact.trigger.tlvdecoding.radius.decoders;



import com.gip.xyna.xact.trigger.tlvdecoding.radius.RadiusTlvDecoder;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.Tlv;



/**
 * Abstract DOCSIS TLV decoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public abstract class AbstractRadiusTlvDecoder implements RadiusTlvDecoder {

    private final int typeEncoding;
    private final String typeName;

    private final String SEPARATOR = ":";


    public AbstractRadiusTlvDecoder(final int typeEncoding, final String typeName) {
        if (typeEncoding < 0 || typeEncoding > 255) {
            throw new IllegalArgumentException("Invalid type encoding: <" + typeEncoding + ">.");
        } else if (typeName == null) {
            throw new IllegalArgumentException("Type name may not be null.");
        }
        this.typeEncoding = typeEncoding;
        this.typeName = typeName;
    }


    public final int getTypeEncoding() {
        return this.typeEncoding;
    }


    public final String getTypeName() {
        return this.typeName;
    }


    public final String decode(final Tlv tlv) {
        if (tlv == null) {
            throw new IllegalArgumentException("Tlv may not be null.");
        } else if (tlv.getTypeEncoding() != this.typeEncoding) {
            throw new IllegalArgumentException("Tlv does not match type encoding <" + this.typeEncoding + ">: <" + tlv + ">.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.typeName);
        String value = decodeTlvValue(tlv.getValueAsByteArray());
        if (!("".equals(value) || value.startsWith("\n"))) {
            sb.append(SEPARATOR);
        }
        sb.append(value.replaceAll("\\n", "\n  ")); // note that this handles indentation.
        return sb.toString();
    }


    protected abstract String decodeTlvValue(byte[] value);
}
