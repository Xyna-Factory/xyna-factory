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
package com.gip.xyna.tlvdecoding.dhcp.decoderfactories;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.AbstractDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;




/**
 * Tests abstract DHCP TLV decoder factory.
 */
public final class AbstractDHCPTlvDecoderFactoryTest {

    @Test
    public void testSuccessfulNormalUse() {
        DHCPv6TlvDecoderFactory decoderFactory = new MockDecoderFactory("Foo");
        assertEquals("Foo", decoderFactory.getDataTypeName());
        DHCPv6TlvDecoder decoder = decoderFactory.create(0, "Bar", new ArrayList<DHCPv6TlvDecoder>(),null);
        assertEquals(0, decoder.getTypeEncoding());
        assertEquals("Bar", decoder.getTypeName());

        decoderFactory = new MockDecoderFactory("Baz");
        assertEquals("Baz", decoderFactory.getDataTypeName());
        decoder = decoderFactory.create(255, "Qux", new ArrayList<DHCPv6TlvDecoder>(),null);
        assertEquals(255, decoder.getTypeEncoding());
        assertEquals("Qux", decoder.getTypeName());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorFailsWithNull() {
        new MockDecoderFactory(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateFailsWithTypeEncodingTooLow() {
        new MockDecoderFactory("Foo").create(-1, "Bar", new ArrayList<DHCPv6TlvDecoder>(),null);
    }

//    @Test (expected = IllegalArgumentException.class)
//    public void testCreateFailsWithTypeEncodingTooHigh() {
//        new MockDecoderFactory("Foo").create(70000, "Bar", new ArrayList<DHCPv6TlvDecoder>(),null);
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateFailsWithTypeNameNull() {
        new MockDecoderFactory("Foo").create(0, null, new ArrayList<DHCPv6TlvDecoder>(),null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateFailsWithSubTlvDecodersNull() {
        new MockDecoderFactory("Foo").create(0, "Bar", null,null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateFailsWithSubTlvDecodersListNotEmpty() {
        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
        subTlvDecoders.add(new MockDecoder(234, "Qux"));
        new MockDecoderFactory("Foo").create(0, "Bar", subTlvDecoders,null);
    }

    private final class MockDecoderFactory extends AbstractDHCPv6TlvDecoderFactory {

        public MockDecoderFactory(final String dataTypeName) {
            super(dataTypeName);
        }

        @Override
        protected DHCPv6TlvDecoder createTlvDecoder(final long typeEncoding, final String typeName) {
            return new MockDecoder(typeEncoding, typeName);
        }
    }

    private final class MockDecoder extends AbstractDHCPv6TlvDecoder {

        public MockDecoder(long typeEncoding, String typeName) {
            super(typeEncoding, typeName);
        }

        @Override
        protected String decodeTlvValue(byte[] value) {
            return Integer.toString(value.length);
        }
    }
}
