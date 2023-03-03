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
package com.gip.xyna.xact.triggerv6.tlvencoding.utilv6;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 HMAC. The performed operation looks like this: MD5(key XOR opad, MD5(key XOR ipad, message)). The value of ipad
 * is 0x36 repeated 64 times. The value of opad is 0x5C repeated 64 times. Key is padded with 0x00 if it is less than
 * 64 bytes. If the key is larger than 64 bytes it is md5:ed and padded with 0x00 to 64 bytes.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class Md5HMAC {

    private static final int BLOCK_SIZE = 64;

    private Md5HMAC() {
    }

    public static byte[] hmac(final byte[] key, final byte[] message) {
        if (key == null) {
            throw new IllegalArgumentException("Key may not be null.");
        } else if (message == null) {
            throw new IllegalArgumentException("Message may not be null.");
        }
        byte[] paddedKey;
        if (key.length > BLOCK_SIZE) {
            paddedKey = padKey(getMD5MessageDigestInstance().digest(key));
        } else {
            paddedKey = padKey(key);
        }
        byte[] opadKey = xor(paddedKey, 0x5C);
        byte[] ipadKey = xor(paddedKey, 0x36);

        MessageDigest innerDigest = getMD5MessageDigestInstance();
        innerDigest.update(ipadKey);
        innerDigest.update(message);

        MessageDigest digest = getMD5MessageDigestInstance();

        digest.update(opadKey);
        digest.update(innerDigest.digest());
        return digest.digest();
    }

    private static byte[] padKey(final byte[] key) {
        byte[] paddedKey = new byte[BLOCK_SIZE];
        for (int i = 0; i < key.length; ++i) {
            paddedKey[i] = key[i];
        }
        int paddingCount = BLOCK_SIZE - key.length;
        for (int i = BLOCK_SIZE - paddingCount; i < BLOCK_SIZE; ++i) {
            paddedKey[i] = (byte) 0x00;
        }
        return paddedKey;
    }

    private static byte[] xor(byte[] data, int b) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; ++i) {
            result[i] = (byte) ((((int) data[i] & 0xFF) ^ b));
        }
        return result;
    }

    private static MessageDigest getMD5MessageDigestInstance() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to get MD5 message digest instance.");
        }
    }
}
