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
package com.gip.xyna.tlvencoding.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.IpV4AddressValidator;

/**
 * Tests IPv4 address validator.
 */
public final class IpV4AddressValidatorTest {

    @Test
    public void testValidateNormally() {
        assertTrue(IpV4AddressValidator.isValid("0.0.0.0"));
        assertTrue(IpV4AddressValidator.isValid("123.5.0.255"));
        assertTrue(IpV4AddressValidator.isValid("12.35.0.128"));
        assertTrue(IpV4AddressValidator.isValid("255.255.255.255"));
        assertFalse(IpV4AddressValidator.isValid(""));
        assertFalse(IpV4AddressValidator.isValid("..."));
        assertFalse(IpV4AddressValidator.isValid("123.5.0.256"));
        assertFalse(IpV4AddressValidator.isValid("123.05.0.255"));
        assertFalse(IpV4AddressValidator.isValid("012.35.0.128"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testValidateWithNull() {
        IpV4AddressValidator.isValid(null);
    }
}

