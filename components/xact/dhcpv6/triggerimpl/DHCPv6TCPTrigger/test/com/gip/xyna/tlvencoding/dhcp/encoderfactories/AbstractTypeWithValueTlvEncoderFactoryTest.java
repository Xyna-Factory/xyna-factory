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
package com.gip.xyna.tlvencoding.dhcp.encoderfactories;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.AbstractTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.AbstractTypeWithValueTlvEncoderFactory;




/**
 * Tests abstract type with value TLV encoder factory.
 */
public final class AbstractTypeWithValueTlvEncoderFactoryTest {

    @Test
    public void testNormalUse() {
        AbstractTypeWithValueTlvEncoderFactoryImpl factory = new AbstractTypeWithValueTlvEncoderFactoryImpl();
        assertTrue(factory instanceof AbstractTlvEncoderFactory);
        TlvEncoder encoder = EasyMock.createMock(TlvEncoder.class);
        EasyMock.replay(encoder);
        factory.setEncoder(encoder);
        TlvEncoder createdEncoder = factory.create(123, new HashMap<String, String>(),
                new HashMap<String, TlvEncoder>());
        assertTrue(encoder == createdEncoder);
        EasyMock.verify(encoder);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithSubNodeEncodingsNotEmpty() {
        AbstractTypeWithValueTlvEncoderFactoryImpl factory = new AbstractTypeWithValueTlvEncoderFactoryImpl();
        Map<String, TlvEncoder> encodings = new HashMap<String, TlvEncoder>();
        TlvEncoder encoder = EasyMock.createMock(TlvEncoder.class);
        EasyMock.replay(encoder);
        encodings.put("foo", encoder);
        factory.create(123, new HashMap<String, String>(), encodings);
    }

    private final class AbstractTypeWithValueTlvEncoderFactoryImpl extends AbstractTypeWithValueTlvEncoderFactory {

        private TlvEncoder encoder = null;

        @Override
        protected TlvEncoder createTypeWithValueTlvEncoder(int typeEncoding,
                Map<String, String> arguments) {
            assertNotNull(typeEncoding);
            assertNotNull(arguments);
            return encoder;
        }

        public void setEncoder(final TlvEncoder encoder) {
            this.encoder = encoder;
        }
    }
}
