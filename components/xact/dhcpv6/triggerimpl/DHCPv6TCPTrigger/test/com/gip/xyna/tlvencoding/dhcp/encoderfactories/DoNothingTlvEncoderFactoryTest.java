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
package com.gip.xyna.tlvencoding.dhcp.encoderfactories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.DoNothingTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.DoNothingTlvEncoder;



/**
 * Tests do nothing TLV encoder factory.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class DoNothingTlvEncoderFactoryTest {

    @Test
    public void testCreate() throws IOException {
        DoNothingTlvEncoderFactory factory = new DoNothingTlvEncoderFactory();
        assertTrue(factory instanceof DoNothingTlvEncoderFactory);
        TlvEncoder encoder = factory.create(45, new HashMap<String, String>(), new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof DoNothingTlvEncoder);
        assertEquals(45, encoder.getTypeEncoding());
        encoder = factory.create(67, new HashMap<String, String>(), new HashMap<String, TlvEncoder>());
        assertEquals(67, encoder.getTypeEncoding());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCrateWithArguments() {
        DoNothingTlvEncoderFactory factory = new DoNothingTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("foo", "bar");
        factory.create(45, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCrateWithSubNodeEncodings() {
        DoNothingTlvEncoderFactory factory = new DoNothingTlvEncoderFactory();
        Map<String, TlvEncoder> encoders = new HashMap<String, TlvEncoder>();
        encoders.put("foo", new DoNothingTlvEncoder(123));
        factory.create(45, new HashMap<String, String>(), encoders);
    }
}
