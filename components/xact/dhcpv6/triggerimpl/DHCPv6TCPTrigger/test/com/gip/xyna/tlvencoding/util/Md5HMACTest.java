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
package com.gip.xyna.tlvencoding.util;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.Md5HMAC;



/**
 * MD5 HMAC test.
 */
public final class Md5HMACTest {

    @Test
    public void testNormalUse() throws UnsupportedEncodingException {
        byte[] key = ByteUtil.toByteArray("0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        byte[] message = ByteUtil.toByteArray("0xDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD"
                + "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
        byte[] digest = Md5HMAC.hmac(key, message);
        assertEquals(TestHelper.toUnsignedIntList(ByteUtil.toByteArray("0x56BE34521D144C88DBB8C733F0E8B3F6")),
                TestHelper.toUnsignedIntList(digest));

        key = ByteUtil.toByteArray("0x0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B");
        message = "Hi There".getBytes("UTF-8");
        digest = Md5HMAC.hmac(key, message);
        assertEquals(TestHelper.toUnsignedIntList(ByteUtil.toByteArray("0x9294727A3638BB1C13F48EF8158BFC9D")),
                TestHelper.toUnsignedIntList(digest));

        key = "Jefe".getBytes("UTF-8");
        message = "what do ya want for nothing?".getBytes("UTF-8");
        digest = Md5HMAC.hmac(key, message);
        assertEquals(TestHelper.toUnsignedIntList(ByteUtil.toByteArray("0x750C783E6AB0B503EAA86E310A5DB738")),
                TestHelper.toUnsignedIntList(digest));

        key = ByteUtil.toByteArray("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
                + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        message = "foo".getBytes("UTF-8");
        digest = Md5HMAC.hmac(key, message);
        assertEquals(TestHelper.toUnsignedIntList(ByteUtil.toByteArray("0x30BBAC1319B751D2F2A6C9D77CF362C8")),
                TestHelper.toUnsignedIntList(digest));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithKeyNull() throws UnsupportedEncodingException {
        byte[] message = "Hi There".getBytes("UTF-8");
        Md5HMAC.hmac(null, message);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithMessageNull() throws UnsupportedEncodingException {
        byte[] key = ByteUtil.toByteArray("0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        Md5HMAC.hmac(key, null);
    }
}
