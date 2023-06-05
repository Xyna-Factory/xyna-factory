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

import java.io.IOException;
import java.io.OutputStream;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;



/**
 * Octet string TLV encoder.
 */
public final class ACSTlvEncoder extends AbstractTypeWithValueTlvEncoder{

    public ACSTlvEncoder(final int typeEncoding) {
        super(typeEncoding);
    }


    @Override
    protected void writeTypeWithValueNode(final TypeWithValueNode node, final OutputStream target) throws IOException {
        String value = node.getValue();
        byte[] content;
        content = value.getBytes("UTF-8");
        target.write(ByteUtil.toByteArray(this.getTypeEncoding(),2));
        target.write(ByteUtil.toByteArray(content.length,2));
        target.write(content);
    }
}
