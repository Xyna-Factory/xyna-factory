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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.ACSTlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.AbstractTypeWithValueTlvEncoder;



/**
 * Tests octet string TLV encoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class ACSTlvEncoderTest {

    @Test
    public void testEncodeOfACSValue() throws IOException {
        ACSTlvEncoder encoder = new ACSTlvEncoder(12);
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        
        TypeWithValueNode node = new TypeWithValueNode("URL", "http://acs.me.com");
       // URL entspricht 68:74:74:70:3a:2f:2f:61:63:73:2e:6d:65:2e:63:6f:6d
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(12);
        expectedResult.write(0);
        expectedResult.write(0x11);
        expectedResult.write(0x68);
        expectedResult.write(0x74);
        expectedResult.write(0x74);
        expectedResult.write(0x70);
        expectedResult.write(0x3a);
        expectedResult.write(0x2f);
        expectedResult.write(0x2f);
        expectedResult.write(0x61);
        expectedResult.write(0x63);
        expectedResult.write(0x73);
        expectedResult.write(0x2e);
        expectedResult.write(0x6d);
        expectedResult.write(0x65);
        expectedResult.write(0x2e);
        expectedResult.write(0x63);
        expectedResult.write(0x6f);
        expectedResult.write(0x6d);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

    }

}
