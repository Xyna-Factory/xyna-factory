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
package com.gip.xyna.tlvencoding.dhcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigToken;

/**
 * Tests text config token.
 */
public final class TextConfigTokenTest {

    @Test
    public void testNormalUse() {
        TextConfigToken token = new TextConfigToken(0, "foo", "bar");
        assertEquals(0, token.getLevel());
        assertEquals("foo", token.getKey());
        assertEquals("bar", token.getValue());

        token = new TextConfigToken(1, "baz", null);
        assertEquals(1, token.getLevel());
        assertEquals("baz", token.getKey());
        assertNull(token.getValue());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithLevelTooLow() {
        new TextConfigToken(-1, "foo", "bar");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithKeyNull() {
        new TextConfigToken(0, null, "bar");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithKeyAsEmptyString() {
        new TextConfigToken(0, "", "bar");
    }

    @Test
    public void testEquals() {
        assertEquals(new TextConfigToken(0, "foo", "bar"), new TextConfigToken(0, "foo", "bar"));
        assertEquals(new TextConfigToken(1, "baz", null), new TextConfigToken(1, "baz", null));
        assertFalse(new TextConfigToken(0, "foo", "bar").equals(new TextConfigToken(1, "foo", "bar")));
        assertFalse(new TextConfigToken(0, "foo", "bar").equals(new TextConfigToken(0, "baz", "bar")));
        assertFalse(new TextConfigToken(0, "foo", "bar").equals(new TextConfigToken(0, "foo", "baz")));
        assertFalse(new TextConfigToken(0, "foo", "bar").equals(new TextConfigToken(0, "foo", null)));
    }

    @Test
    public void testHashCode() {
        assertEquals(new TextConfigToken(0, "foo", "bar").hashCode(),
                new TextConfigToken(0, "foo", "bar").hashCode());
        assertEquals(new TextConfigToken(1, "baz", null).hashCode(),
                new TextConfigToken(1, "baz", null).hashCode());
        assertFalse(new TextConfigToken(0, "foo", "bar").hashCode()
                == new TextConfigToken(1, "foo", "bar").hashCode());
        assertFalse(new TextConfigToken(0, "foo", "bar").hashCode()
                == new TextConfigToken(0, "baz", "bar").hashCode());
        assertFalse(new TextConfigToken(0, "foo", "bar").hashCode()
                == new TextConfigToken(0, "foo", "baz").hashCode());
        assertFalse(new TextConfigToken(0, "foo", "bar").hashCode()
                == new TextConfigToken(0, "foo", null).hashCode());
    }
}
