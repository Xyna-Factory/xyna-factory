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
import static org.junit.Assert.assertNull;

import java.io.StringReader;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.ConfigFileReadException;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigToken;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTokenReader;

/**
 * Tests text config token reader.
 */
public final class TextConfigTokenReaderTest {

    @Test
    public void testRead() throws ConfigFileReadException {
        String config = "foo\n  bar:123\n";
        TextConfigTokenReader reader = new TextConfigTokenReader(new StringReader(config));
        assertEquals(new TextConfigToken(0, "foo", null), reader.read());
        assertEquals(new TextConfigToken(1, "bar", "123"), reader.read());
        assertNull(reader.read());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithNull() {
        new TextConfigTokenReader(null);
    }

    @Test (expected = ConfigFileReadException.class)
    public void testReadOfConfigWithWrongIndentation() throws ConfigFileReadException {
        String config = "foo\n bar:123\n";
        TextConfigTokenReader reader = new TextConfigTokenReader(new StringReader(config));
        assertEquals(new TextConfigToken(0, "foo", null), reader.read());
        reader.read();
    }

    @Test (expected = ConfigFileReadException.class)
    public void testReadOfConfigWithEmptyIdentifier() throws ConfigFileReadException {
        String config = "foo\n  :123\n";
        TextConfigTokenReader reader = new TextConfigTokenReader(new StringReader(config));
        assertEquals(new TextConfigToken(0, "foo", null), reader.read());
        reader.read();
    }

    @Test (expected = ConfigFileReadException.class)
    public void testReadOfConfigWithEmptyLine() throws ConfigFileReadException {
        String config = "foo\n\n  bar:123\n";
        TextConfigTokenReader reader = new TextConfigTokenReader(new StringReader(config));
        assertEquals(new TextConfigToken(0, "foo", null), reader.read());
        reader.read();
    }
}
