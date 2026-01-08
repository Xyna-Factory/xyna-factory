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
package com.gip.xyna.xact.trigger.tlvdecoding.radius.decoderfactories;



import java.util.List;

import com.gip.xyna.xact.trigger.tlvdecoding.radius.RadiusTlvDecoder;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.RadiusTlvDecoderFactory;



/**
 * Abstract DOCSIS TLV decoder for decoders that do not have sub TLV decoders.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public abstract class AbstractRadiusTlvDecoderFactory implements RadiusTlvDecoderFactory {

    private final String dataTypeName;


    public AbstractRadiusTlvDecoderFactory(final String dataTypeName) {
        if (dataTypeName == null) {
            throw new IllegalArgumentException("Data type name may not be null.");
        }
        this.dataTypeName = dataTypeName;
    }


    public final String getDataTypeName() {
        return this.dataTypeName;
    }


    public final RadiusTlvDecoder create(final int typeEncoding, final String typeName, final List<RadiusTlvDecoder> subTlvDecoders) {
        if (typeEncoding < 0 || typeEncoding > 255) {
            throw new IllegalArgumentException("Invalid type encoding: <" + typeEncoding + ">.");
        } else if (typeName == null) {
            throw new IllegalArgumentException("Type name may not be null.");
        } else if (subTlvDecoders == null) {
            throw new IllegalArgumentException("Sub TLV decoders may not be null.");
        } else if (subTlvDecoders.size() > 0) {
            throw new IllegalArgumentException("Expected no sub TLV decoders for type <" + typeName + "> with encoding: <" + typeEncoding
                    + ">.");
        }
        return createTlvDecoder(typeEncoding, typeName);
    }


    protected abstract RadiusTlvDecoder createTlvDecoder(final int typeEncoding, String typeName);
}
