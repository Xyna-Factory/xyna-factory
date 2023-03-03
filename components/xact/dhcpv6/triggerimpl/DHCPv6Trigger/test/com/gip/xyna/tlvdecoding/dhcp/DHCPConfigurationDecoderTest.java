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
package com.gip.xyna.tlvdecoding.dhcp;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6ConfigurationDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Decoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DecoderException;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6Encoding;



/**
 * Tests DHCP configuration decoder.
 */
public final class DHCPConfigurationDecoderTest {

    @Test
    public void testDecodeSuccessfullyDecodesAllDifferentDataTypes() throws DecoderException {
        List<DHCPv6Encoding> decodings = new ArrayList<DHCPv6Encoding>();
        Map <String,String> valuearguments = new HashMap<String, String>();
        
        decodings.add(new DHCPv6Encoding(0, null, "Pad", 0, null,"Padding",valuearguments));
        decodings.add(new DHCPv6Encoding(100, null, "Octet string", 100, null,"OctetString",valuearguments));
        decodings.add(new DHCPv6Encoding(101, null, "Composite TLV", 101,null, "Container",valuearguments));
        decodings.add(new DHCPv6Encoding(102, null, "Unsigned integer", 102,null, "UnsignedInteger",valuearguments));
        decodings.add(new DHCPv6Encoding(103, null, "IPv4 address", 103,null, "IpV4Address",valuearguments));
        decodings.add(new DHCPv6Encoding(104, null, "MAC-address", 104,null, "MacAddress",valuearguments));
        decodings.add(new DHCPv6Encoding(105, null, "Deprecated TLV", 105,null, "Disallowed",valuearguments));
        decodings.add(new DHCPv6Encoding(255, null, "End-of-Data", 255,null, "EndOfDataMarker",valuearguments));
        Decoder decoder = new DHCPv6ConfigurationDecoder(decodings);

        StringBuilder inputData = new StringBuilder();
        inputData.append("0x");
        //inputData.append("0000");
        inputData.append("006400080123456789ABCDEF");
        inputData.append("0065000500C8000101");
        inputData.append("006600025ABF");
        inputData.append("00670004ABCD3201");
        inputData.append("00680006AB1256DF0934");
        inputData.append("00690008AAAAAAAAAAAAAAAA");

        StringBuilder expectedResult = new StringBuilder();
        //expectedResult.append("Pad\n");
        expectedResult.append("Octet string:0x0123456789ABCDEF\n");
        expectedResult.append("Composite TLV\n");
        expectedResult.append("  Tlv:200:OctetString:0x01\n");
        expectedResult.append("Unsigned integer:23231\n");
        expectedResult.append("IPv4 address:171.205.50.1\n");
        expectedResult.append("MAC-address:AB:12:56:DF:09:34\n");
        expectedResult.append("Deprecated TLV:0xAAAAAAAAAAAAAAAA\n");

        assertEquals(expectedResult.toString(), decoder.decode(TestHelper.toByteArray(inputData.toString())));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorFailsWithDecodingsNull() {
        new DHCPv6ConfigurationDecoder(null);
    }

    // Padding nicht mehr in Verwendung hier
//    @Test
//    public void testSuccessfulCreateAndUseWithMinimumDecodings() throws DecoderException {
//        List<DHCPv6Encoding> decodings = new ArrayList<DHCPv6Encoding>();
//        Map <String,String> valuearguments = new HashMap<String, String>();
//
//        decodings.add(new DHCPv6Encoding(0, null, "Pad", 0, null,"Padding", valuearguments));
//        decodings.add(new DHCPv6Encoding(255, null, "End-of-Data", 255, null,"EndOfDataMarker",valuearguments));
//        Decoder decoder = new DHCPv6ConfigurationDecoder(decodings);
//        assertEquals("Pad\n", decoder.decode(TestHelper.toByteArray("0x0000")));
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorFailsWithNullValueInDecodings() throws DecoderException {
        List<DHCPv6Encoding> decodings = new ArrayList<DHCPv6Encoding>();
        Map <String,String> valuearguments = new HashMap<String, String>();

        decodings.add(new DHCPv6Encoding(0, null, "Pad", 0,null, "Padding",valuearguments));
        decodings.add(new DHCPv6Encoding(255, null, "End-of-Data", 255, null,"EndOfDataMarker",valuearguments));
        decodings.add(null);
        new DHCPv6ConfigurationDecoder(decodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorFailsWithoutPadding() throws DecoderException {
        List<DHCPv6Encoding> decodings = new ArrayList<DHCPv6Encoding>();
        Map <String,String> valuearguments = new HashMap<String, String>();

        decodings.add(new DHCPv6Encoding(255, null, "End-of-Data", 255,null, "EndOfDataMarker",valuearguments));
        new DHCPv6ConfigurationDecoder(decodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorFailsWithoutEndOfDataMarker() throws DecoderException {
        List<DHCPv6Encoding> decodings = new ArrayList<DHCPv6Encoding>();
        Map <String,String> valuearguments = new HashMap<String, String>();

        decodings.add(new DHCPv6Encoding(0, null, "Pad", 0,null, "Padding",valuearguments));
        new DHCPv6ConfigurationDecoder(decodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDecodeFailsWithNull() throws DecoderException {
        List<DHCPv6Encoding> decodings = new ArrayList<DHCPv6Encoding>();
        Map <String,String> valuearguments = new HashMap<String, String>();

        decodings.add(new DHCPv6Encoding(0, null, "Pad", 0,null, "Padding",valuearguments));
        decodings.add(new DHCPv6Encoding(255, null, "End-of-Data", 255,null, "EndOfDataMarker",valuearguments));
        new DHCPv6ConfigurationDecoder(decodings).decode(null);
    }




}
