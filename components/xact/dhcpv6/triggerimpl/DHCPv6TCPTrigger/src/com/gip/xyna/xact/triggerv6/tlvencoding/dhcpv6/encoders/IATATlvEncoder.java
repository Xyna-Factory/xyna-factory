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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;




/**
 * Container TLV encoder.
 */
public final class IATATlvEncoder extends AbstractTypeOnlyTlvEncoder {

    final int typeEncoding;
    final Map<String, TlvEncoder> subNodeEncodings;

    public IATATlvEncoder(final int typeEncoding, final Map<String, TlvEncoder> subNodeEncodings) {
        super(typeEncoding);
        if (subNodeEncodings == null) {
            throw new IllegalArgumentException("Subnode encodings may not be null.");
        }
        this.typeEncoding = typeEncoding;
        this.subNodeEncodings = validateAndMakeUnmodifiable(subNodeEncodings);
    }

    private static Map<String, TlvEncoder> validateAndMakeUnmodifiable(final Map<String, TlvEncoder> encodings) {
        Map<String, TlvEncoder> response = new HashMap<String, TlvEncoder>();
        for (Map.Entry<String, TlvEncoder> entry : encodings.entrySet()) {
            String key = entry.getKey();
            TlvEncoder encoding = entry.getValue();
            if (key == null) {
                throw new IllegalArgumentException("Data type name may not be null.");
            } else if (key.length() == 0) {
                throw new IllegalArgumentException("Data type name may not be empty string.");
            } else if (encoding == null) {
                throw new IllegalArgumentException("Sub node encoder not set for data type: <" + key + ">.");
            }
            response.put(key, encoding);
        }
        return Collections.unmodifiableMap(response);
    }

    @Override
    protected void writeTypeOnlyNode(final TypeOnlyNode node, final OutputStream target) throws IOException {
        ByteArrayOutputStream subNodesData = new ByteArrayOutputStream();
        for (Node subNode : node.getSubNodes()) {
            TlvEncoder encoder = subNodeEncodings.get(subNode.getTypeName());
            if (encoder == null) {
                throw new IllegalArgumentException("Unknown sub type: <" + subNode.getTypeName() + ">.");
            }
            encoder.write(subNode, subNodesData);
        }
        if (subNodesData.size() > DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH) {
            throw new IllegalArgumentException("Subnodes contain more than "
                    + DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH + " bytes: <" + node + ">.");
        }
        // DHCPv6 2 Byte für TypeEncoding und Länge
        
        
        target.write(ByteUtil.toByteArray(typeEncoding,2));
        target.write(ByteUtil.toByteArray(subNodesData.size(),2));  
        subNodesData.writeTo(target);
    }
}
