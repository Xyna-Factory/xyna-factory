/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.tlvencoding.dhcp;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.ConfigFileReadException;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTree;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTreeReader;




/**
 * Tests DHCP configuration encoder.
 */
public final class DHCPConfigurationEncoderTest {

    @Test
    public void testSuccessForUseWithoutCmAndCmtsMic() throws IOException {
        DHCPv6ConfigurationEncoder encoder = new DHCPv6ConfigurationEncoder(
                Collections.unmodifiableList(createEncodingList()));
        ByteArrayOutputStream target = new ByteArrayOutputStream();

        encoder.encode(createTextConfigTreeDHCP().getNodes(), target);

        List<Integer> expectedResult = new ArrayList<Integer>();
        add(expectedResult, 0, 53, 0, 1, 13);
        add(expectedResult, 0, 3, 0, 4, 192, 168, 12, 25);
        add(expectedResult, 0, 54, 0, 4, 192, 168, 12, 25);
        add(expectedResult, 0, 51, 0, 4, 0, 0, 13, 5);
        add(expectedResult, 0, 58, 0, 4, 0, 0, 17, 92);
        add(expectedResult, 0, 59, 0, 4, 0, 0, 21, 179);
        add(expectedResult, 0, 91, 0, 4, 0, 0, 26, 10);
        add(expectedResult, 0, 82, 0, 18);
        add(expectedResult, 0, 1, 0, 4, 0, 0, 21, 179);
        add(expectedResult, 0, 2, 0, 6, 0, 224, 82, 154, 194, 4);
        assertEquals(expectedResult, TestHelper.toUnsignedIntList(target));
        
        
    }

    private static TextConfigTree createTextConfigTreeDHCP() {
        StringBuilder sb = new StringBuilder();
        sb.append("DHCPMessageType:13\n");
        sb.append("Router:192.168.12.25\n");
        sb.append("ServerIdentifier:192.168.12.25\n");
        sb.append("LeaseTime:3333\n");
        sb.append("RenewalTime:4444\n");
        sb.append("RebindingTime:5555\n");
        sb.append("ClientLastTransactionTime:6666\n");
        sb.append("AgentInformation\n");
        sb.append("  AgentCircuitID:5555\n");
        sb.append("  AgentRemoteID:00:E0:52:9A:C2:04\n");

        try {
            return new TextConfigTreeReader(new StringReader(sb.toString())).read();
        } catch (ConfigFileReadException e) {
            throw new IllegalStateException("Failed to create text config tree for test.", e);
        }
    }


    private static List<DHCPv6Encoding> createEncodingList() {
        List<DHCPv6Encoding> encodings = new ArrayList<DHCPv6Encoding>();
        Map <String,String> valuearguments = new HashMap<String, String>();

        // Anfang
        encodings.add(new DHCPv6Encoding(0,null,"Pad",0,null,"Padding", valuearguments));
        valuearguments.clear();
        
        // DHCP Message Option T=53 
        valuearguments.put("nrBytes", "1");   
        encodings.add(new DHCPv6Encoding(1,null,"DHCPMessageType",53,null,"UnsignedInteger", valuearguments));
        valuearguments.clear();
        
        // requested address Option T=50
        
        encodings.add(new DHCPv6Encoding(2,null,"RequestedAddress",50,null,"IpV4Address", valuearguments));
        valuearguments.clear();

        // Server Identifier Option T=54
        
        encodings.add(new DHCPv6Encoding(3,null,"ServerIdentifier",54,null,"IpV4Address", valuearguments));
        valuearguments.clear();
        
        // Router Option T=3
        
        encodings.add(new DHCPv6Encoding(4,null,"Router",3,null,"IpV4AddressList", valuearguments));
        valuearguments.clear();
        
        // LeaseTime Option T=51 
        valuearguments.put("nrBytes", "4");   
        encodings.add(new DHCPv6Encoding(5,null,"LeaseTime",51,null,"UnsignedInteger", valuearguments));
        valuearguments.clear();

        // Client Identifier Option T = 61
        
        encodings.add(new DHCPv6Encoding(6,null,"ClientIdentifier",61,null,"MacAddress", valuearguments));
        valuearguments.clear();

        // Renewal Time Option T=58 
        valuearguments.put("nrBytes", "4");   
        encodings.add(new DHCPv6Encoding(7,null,"RenewalTime",58,null,"UnsignedInteger", valuearguments));
        valuearguments.clear();
        
        // Rebinding Time Option T=59 
        valuearguments.put("nrBytes", "4");   
        encodings.add(new DHCPv6Encoding(8,null,"RebindingTime",59,null,"UnsignedInteger", valuearguments));
        valuearguments.clear();
        
        // Client Last Transaction Time Option T=91
        valuearguments.put("nrBytes", "4");   
        encodings.add(new DHCPv6Encoding(9,null,"ClientLastTransactionTime",91,null,"UnsignedInteger", valuearguments));
        valuearguments.clear();

        // Agent Information Option T=82
        encodings.add(new DHCPv6Encoding(10,null,"AgentInformation",82,null,"Container", valuearguments));
        valuearguments.clear();
        
        // Agent Circuit ID SubOption T=1
        valuearguments.put("nrBytes", "4");   
        encodings.add(new DHCPv6Encoding(11,10,"AgentCircuitID",1,null,"UnsignedInteger", valuearguments));
        valuearguments.clear();
        
        // Agent Remote ID Option T=2
        encodings.add(new DHCPv6Encoding(12,10,"AgentRemoteID",2,null,"MacAddress", valuearguments));
        valuearguments.clear();
        
        // Parameter Request List Option T=55
        encodings.add(new DHCPv6Encoding(13,null,"ParameterRequestList",55,null,"OctetString", valuearguments));
        valuearguments.clear();
        
        // Maximum DHCP Message Size Option T=57
        valuearguments.put("nrBytes", "2");   
        encodings.add(new DHCPv6Encoding(14,null,"MaximumDHCPMessageSize",57,null,"UnsignedInteger", valuearguments));
        valuearguments.clear();
        
        
        
        /*
        // Test Option T=66
        valuearguments.put("\"nrBytes\"", "\"6\"");   
        valuearguments.put("\"multipleOf\"", "\"2\"");    
        
        encodings.add(new DHCPEncoding(3,null,"TestOption",66,null,"UnsignedInteger", valuearguments));
        valuearguments.clear();
        */
        
        // End of Data Markierung
        
        encodings.add(new DHCPv6Encoding(255,null,"End-of-Data",255,null,"EndOfDataMarker", valuearguments));
        valuearguments.clear();

        return encodings;
    }

    /*
    private static Map<String, String> createUnsIntArgs(final Long minValue, final Long maxValue, final Long multipleOf) {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "4");
        if (minValue != null) {
            arguments.put("minValue", minValue.toString());
        }
        if (maxValue != null) {
            arguments.put("maxValue", maxValue.toString());
        }
        if (multipleOf != null) {
            arguments.put("multipleOf", multipleOf.toString());
        }
        return arguments;
    }
*/
    private void add(final List<Integer> list, final int... values) {
        for (int value : values) {
            list.add(value);
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorWithEncodingsNull() {
        new DHCPv6ConfigurationEncoder(null);
    }


    @Test (expected = IllegalArgumentException.class)
    public void testConstructorWithNullInEncodingsList() throws IOException {
        List<DHCPv6Encoding> encodings = createEncodingList();
        encodings.add(null);
        new DHCPv6ConfigurationEncoder(encodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorWithBadParentIdElementInEncodingsList() throws IOException {
        List<DHCPv6Encoding> encodings = createEncodingList();
        encodings.add(new DHCPv6Encoding(4242, 42, "Bad one", 42, null, "Container", new HashMap<String, String>()));
        new DHCPv6ConfigurationEncoder(encodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorWithUsedTypeEncodingRootElementAddedToEncodingsList() throws IOException {
        List<DHCPv6Encoding> encodings = createEncodingList();
        encodings.add(new DHCPv6Encoding(4242, null, "Bad one", 53, null, "Container", new HashMap<String, String>()));
        new DHCPv6ConfigurationEncoder(encodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorWithUsedTypeEncodingSubElementAddedToEncodingsList() throws IOException {
        List<DHCPv6Encoding> encodings = createEncodingList();
        encodings.add(new DHCPv6Encoding(4242, 10, "Bad one", 1, null,"Container", new HashMap<String, String>()));
        new DHCPv6ConfigurationEncoder(encodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithTextConfigTreeNull() throws IOException {
        DHCPv6ConfigurationEncoder encoder = new DHCPv6ConfigurationEncoder(createEncodingList());
        encoder.encode(null, new ByteArrayOutputStream());
    }

}
