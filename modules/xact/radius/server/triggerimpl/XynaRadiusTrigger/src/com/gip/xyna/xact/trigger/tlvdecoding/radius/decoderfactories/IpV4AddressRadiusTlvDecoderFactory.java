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



import com.gip.xyna.xact.trigger.tlvdecoding.radius.RadiusTlvDecoder;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.decoders.IpV4AddressRadiusTlvDecoder;



/**
 * IPv4 address DOCSIS TLV decoder factory.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class IpV4AddressRadiusTlvDecoderFactory extends AbstractRadiusTlvDecoderFactory {

    public static final String DATA_TYPE_NAME = "IpV4Address";


    public IpV4AddressRadiusTlvDecoderFactory() {
        super(DATA_TYPE_NAME);
    }


    @Override
    protected RadiusTlvDecoder createTlvDecoder(final int typeEncoding, final String typeName) {
        return new IpV4AddressRadiusTlvDecoder(typeEncoding, typeName);
    }
}
