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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.AbstractTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.AbstractTypeOnlyTlvEncoderFactory;




/**
 * Tests abstract type only TLV encoder factory.
 */
public final class AbstractTypeOnlyTlvEncoderFactoryTest {

    @Test
    public void testNormalUse() {
        AbstractTypeOnlyTlvEncoderFactoryImpl factory = new AbstractTypeOnlyTlvEncoderFactoryImpl();
        assertTrue(factory instanceof AbstractTlvEncoderFactory);
        TlvEncoder encoder = EasyMock.createMock(TlvEncoder.class);
        EasyMock.replay(encoder);
        factory.setEncoder(encoder);
        TlvEncoder createdEncoder = factory.create(123, new HashMap<String, String>(),
                new HashMap<String, TlvEncoder>());
        assertTrue(encoder == createdEncoder);
        EasyMock.verify(encoder);
    }

    private final class AbstractTypeOnlyTlvEncoderFactoryImpl extends AbstractTypeOnlyTlvEncoderFactory {

        private TlvEncoder encoder = null;

        @Override
        protected TlvEncoder createTypeOnlyTlvEncoder(final int typeEncoding, final Map<String, String> arguments,
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
