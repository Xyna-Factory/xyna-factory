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
package com.gip.xyna.tlvdecoding.dhcp.decoderfactories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.LeaseQueryDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.LeaseQueryDHCPv6TlvDecoder;




/**
 * Tests DHCP LeaseQuery TLV decoder factory.
 */
public final class LeaseQueryDHCPTlvDecoderFactoryTest {

    @Test
    public void testSuccessfulNormalUse() {
        DHCPv6TlvDecoderFactory decoderFactory = new LeaseQueryDHCPv6TlvDecoderFactory();
        assertEquals("LeaseQuery", decoderFactory.getDataTypeName());
        assertEquals(decoderFactory.getDataTypeName(), LeaseQueryDHCPv6TlvDecoderFactory.DATA_TYPE_NAME);
        DHCPv6TlvDecoder decoder = decoderFactory.create(0, "Foo", new ArrayList<DHCPv6TlvDecoder>(),null);
        assertTrue(decoder instanceof LeaseQueryDHCPv6TlvDecoder);
        assertEquals(0, decoder.getTypeEncoding());
        assertEquals("Foo", decoder.getTypeName());

        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
        subTlvDecoders.add(new LeaseQueryDHCPv6TlvDecoder(56, "Qux", new ArrayList<DHCPv6TlvDecoder>(),null));
        decoder = decoderFactory.create(255, "Bar", subTlvDecoders,null);
        assertEquals(255, decoder.getTypeEncoding());
        assertEquals("Bar", decoder.getTypeName());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsCreateWithTypeEncodingTooLow() {
        new LeaseQueryDHCPv6TlvDecoderFactory().create(-1, "Foo", new ArrayList<DHCPv6TlvDecoder>(),null);
    }

//    @Test (expected = IllegalArgumentException.class)
//    public void testFailsCreateWithTypeEncodingTooHigh() {
//        new LeaseQueryDHCPv6TlvDecoderFactory().create(70000, "Foo", new ArrayList<DHCPv6TlvDecoder>(),null);
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsCreateWithTypeNameNull() {
        new LeaseQueryDHCPv6TlvDecoderFactory().create(123, null, new ArrayList<DHCPv6TlvDecoder>(),null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsCreateWithSubTlvDecodersNull() {
        new LeaseQueryDHCPv6TlvDecoderFactory().create(123, "Foo", null,null);
    }
}
