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
package com.gip.xyna.tlvdecoding.dhcp.decoders;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;



/**
 * Tests abstract DHCP TLV decoder.
 */
public final class AbstractDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulNormalUse() {
        DHCPv6TlvDecoder decoder = new AbstractDHCPTlvDecoderExtender(123, "Foo");
        assertEquals("Foo:0", decoder.decode(new Tlv(123, new ArrayList<Byte>())));
        assertEquals("Foo:3", decoder.decode(new Tlv(123, TestHelper.toByteList("0xAAAAAA"))));
        assertEquals("Foo", decoder.decode(new Tlv(123, TestHelper.toByteList("0xAAAAAAAAAAAA"))));
        assertEquals("Foo\n  Baz:5", decoder.decode(new Tlv(123, TestHelper.toByteList("0xAAAAAAAAAA"))));
        decoder = new AbstractDHCPTlvDecoderExtender(45, "Bar");
        assertEquals("Bar:0", decoder.decode(new Tlv(45, new ArrayList<Byte>())));
        assertEquals("Bar:2", decoder.decode(new Tlv(45, TestHelper.toByteList("0xAAAA"))));
        assertEquals("Bar", decoder.decode(new Tlv(45, TestHelper.toByteList("0xAAAAAAAAAAAA"))));
        assertEquals("Bar\n  Baz:4", decoder.decode(new Tlv(45, TestHelper.toByteList("0xAAAAAAAA"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructFailsWithTypeEncodingTooLow() {
        new AbstractDHCPTlvDecoderExtender(-1, "Foo");
    }

    // Test nicht mehr sinnvoll, da obere Grenze ausgehebelt wurde fuer Fix von doppelten Optionen
//    @Test (expected = IllegalArgumentException.class)
//    public void testConstructFailsWithTypeEncodingTooHigh() {
//        new AbstractDHCPTlvDecoderExtender(70000, "Foo");
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructFailsWithTypeNameNull() {
        new AbstractDHCPTlvDecoderExtender(123, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDecodeFailsWithNull() {
        new AbstractDHCPTlvDecoderExtender(123, "Foo").decode(null);
    }

    // Test nicht mehr sinnvoll, da unterschiedliche Encodings sein muessen, um verschiedene Optionen vom gleichen Encoding zu
    //unterscheiden
//    @Test (expected = IllegalArgumentException.class) 
//    public void testDecodeFailsWithTlvWithWrongTypeEncoding() {
//        new AbstractDHCPTlvDecoderExtender(123, "Foo").decode(new Tlv(45, new ArrayList<Byte>()));
//    }

    private class AbstractDHCPTlvDecoderExtender extends AbstractDHCPv6TlvDecoder {

        public AbstractDHCPTlvDecoderExtender(final int typeEncoding, final String typeName) {
            super(typeEncoding, typeName);
        }

        @Override
        protected String decodeTlvValue(final byte[] value) {
            if (value.length < 4) {
                return Integer.toString(value.length);
            } else if (value.length < 6) {
                return "\nBaz:" + value.length;
            }
            return "";
        }
    }
}
