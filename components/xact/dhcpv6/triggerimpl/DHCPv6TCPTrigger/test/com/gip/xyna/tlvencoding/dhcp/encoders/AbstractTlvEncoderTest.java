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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.AbstractTlvEncoder;




/**
 * Abstract TLV encoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class AbstractTlvEncoderTest {

    @Test
    public void testNormalUse() throws IOException {
        AbstractTlvEncoderImpl encoder = new AbstractTlvEncoderImpl(42);
        assertTrue(encoder instanceof TlvEncoder);
        assertEquals(42, encoder.getTypeEncoding());
        Node node = new TypeWithValueNode("Foo", "123");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(42);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new AbstractTlvEncoderImpl(0);
        assertEquals(0, encoder.getTypeEncoding());
        node = new TypeWithValueNode("Bar", "456");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new AbstractTlvEncoderImpl(255);
        assertEquals(255, encoder.getTypeEncoding());
        node = new TypeOnlyNode("Baz", new ArrayList<Node>());
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(255);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTypeEncodingTooLow() {
        new AbstractTlvEncoderImpl(-1);
    }

//    @Test (expected = IllegalArgumentException.class)
//    public void testCreateWithTypeEncodingTooHigh() {
//        new AbstractTlvEncoderImpl(70000);
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithNodeNull() throws IOException {
        AbstractTlvEncoderImpl encoder = new AbstractTlvEncoderImpl(42);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(null, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithOutputStreamNull() throws IOException {
        AbstractTlvEncoderImpl encoder = new AbstractTlvEncoderImpl(42);
        TypeWithValueNode node = new TypeWithValueNode("Foo", "123");
        encoder.write(node, null);
    }

    private final class AbstractTlvEncoderImpl extends AbstractTlvEncoder {

        public AbstractTlvEncoderImpl(int typeEncoding) {
            super(typeEncoding);
        }

        @Override
        protected void writeNode(Node node, OutputStream target) throws IOException {
            assertNotNull(node);
            assertNotNull(target);
            target.write(this.getTypeEncoding());
            target.write(0);
        }
    }
}
