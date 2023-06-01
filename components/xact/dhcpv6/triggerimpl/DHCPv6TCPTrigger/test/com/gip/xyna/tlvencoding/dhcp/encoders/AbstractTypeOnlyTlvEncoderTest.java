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
package com.gip.xyna.tlvencoding.dhcp.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.AbstractTlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.AbstractTypeOnlyTlvEncoder;




/**
 * Tests abstract type only TLV encoder.
 */
public final class AbstractTypeOnlyTlvEncoderTest {

    @Test
    public void testNormalUse() throws IOException {
        AbstractTypeOnlyTlvEncoderImpl encoder = new AbstractTypeOnlyTlvEncoderImpl(123);
        assertTrue(encoder instanceof AbstractTlvEncoder);
        assertEquals(123, encoder.getTypeEncoding());
        Node node = new TypeOnlyNode("Foo", new ArrayList<Node>());
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(123);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTypeWithValueNode() throws IOException {
        AbstractTypeOnlyTlvEncoderImpl encoder = new AbstractTypeOnlyTlvEncoderImpl(123);
        Node node = new TypeWithValueNode("Bar", "baz");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    private final class AbstractTypeOnlyTlvEncoderImpl extends AbstractTypeOnlyTlvEncoder {

        public AbstractTypeOnlyTlvEncoderImpl(final int typeEncoding) {
            super(typeEncoding);
        }

        @Override
        protected void writeTypeOnlyNode(final TypeOnlyNode node, final OutputStream target) throws IOException {
            assertNotNull(node);
            assertNotNull(target);
            target.write(this.getTypeEncoding());
            target.write(0);
        }
    }
}
