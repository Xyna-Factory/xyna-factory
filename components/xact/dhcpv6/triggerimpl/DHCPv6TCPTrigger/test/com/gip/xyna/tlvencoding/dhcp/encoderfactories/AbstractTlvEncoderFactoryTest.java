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



import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.AbstractTlvEncoderFactory;




/**
 * Tests abstract tlv encoder factory.
 */
public final class AbstractTlvEncoderFactoryTest {

    @Test
    public void testNormalUse() {
        AbstractTlvEncoderFactoryImpl factory = new AbstractTlvEncoderFactoryImpl();
        assertTrue(factory instanceof TlvEncoderFactory);
        TlvEncoder encoder = EasyMock.createMock(TlvEncoder.class);
        EasyMock.replay(encoder);
        factory.setEncoder(encoder);
        TlvEncoder createdEncoder = factory.create(123, new HashMap<String, String>(),
                new HashMap<String, TlvEncoder>());
        assertTrue(encoder == createdEncoder);

        createdEncoder = factory.create(0, new HashMap<String, String>(),
                new HashMap<String, TlvEncoder>());
        assertTrue(encoder == createdEncoder);

        createdEncoder = factory.create(255, new HashMap<String, String>(),
                new HashMap<String, TlvEncoder>());
        assertTrue(encoder == createdEncoder);
        EasyMock.verify(encoder);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTypeEncodingValueTooLow() {
        new AbstractTlvEncoderFactoryImpl().create(-1, new HashMap<String, String>(),
                new HashMap<String, TlvEncoder>());
    }

//    @Test (expected = IllegalArgumentException.class)
//    public void testCreateWithTypeEncodingValueTooHigh() {
//        new AbstractTlvEncoderFactoryImpl().create(70000, new HashMap<String, String>(),
//                new HashMap<String, TlvEncoder>());
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithArgumentsNull() {
        new AbstractTlvEncoderFactoryImpl().create(123, null, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithSubNodeEncodingsNull() {
        new AbstractTlvEncoderFactoryImpl().create(123, new HashMap<String, String>(), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithArgumentsContainingNullKey() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put(null, "bar");
        new AbstractTlvEncoderFactoryImpl().create(123, arguments,
                new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithArgumentsContainingNullValueForKey() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("foo", null);
        new AbstractTlvEncoderFactoryImpl().create(123, arguments,
                new HashMap<String, TlvEncoder>());
    }

    private final class AbstractTlvEncoderFactoryImpl extends AbstractTlvEncoderFactory {

        private TlvEncoder encoder = null;

        @Override
        protected TlvEncoder createTlvEncoder(final int typeEncoding, final Map<String, String> arguments,
                final Map<String, TlvEncoder> subNodeEncodings) {
            assertNotNull(arguments);
            assertNotNull(subNodeEncodings);
            return encoder;
        }

        public void setEncoder(final TlvEncoder encoder) {
            this.encoder = encoder;
        }
    }
}
