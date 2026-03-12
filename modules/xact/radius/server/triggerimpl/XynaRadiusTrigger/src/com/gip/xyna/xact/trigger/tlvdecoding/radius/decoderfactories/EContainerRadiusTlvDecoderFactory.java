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
import com.gip.xyna.xact.trigger.tlvdecoding.radius.decoders.EContainerRadiusTlvDecoder;



/**
 * DOCSIS Container TLV decoder factory.
 *
 */
public final class EContainerRadiusTlvDecoderFactory implements RadiusTlvDecoderFactory {

    public static final String DATA_TYPE_NAME = "EContainer";


    public String getDataTypeName() {
        return DATA_TYPE_NAME;
    }


    public RadiusTlvDecoder create(final int typeEncoding, final String typeName, final List<RadiusTlvDecoder> subTlvDecoders) {
        if (typeEncoding < 0 || typeEncoding > 255) {
            throw new IllegalArgumentException("Invalid type encoding: <" + typeEncoding + ">.");
        } else if (typeName == null) {
            throw new IllegalArgumentException("Type name may not be null.");
        } else if (subTlvDecoders == null) {
            throw new IllegalArgumentException("Sub TLV decoders may not be null.");
        }
        return new EContainerRadiusTlvDecoder(typeEncoding, typeName, subTlvDecoders);
    }
}
