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
package com.gip.xyna.xact.triggerv6.tlvdecoding.utilv6;

import java.io.UnsupportedEncodingException;

/**
 * Octet string util.
 */
public final class OctetStringUtil {

    private OctetStringUtil() {
    }

    public static String toString(final byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Byte array may not be null.");
        } else if (bytes.length < 1) {
            throw new IllegalArgumentException("Empty byte array provided.");
        }
        for (byte b : bytes) {
            int value = ((int) b) & 0xFF;
            if (value < 32 || value > 126) {
                return ByteUtil.toHexValue(bytes);
            }
        }
        String result;
        try {
            result = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to use UTF-8 encoding.");
        }
//        StringBuilder sb = new StringBuilder();
//        sb.append("\"");
//        sb.append(result.replaceAll("\\\\", "\\\\b").replaceAll("\"", "\\\\\"").replaceAll("\\\\b", "\\\\\\\\"));
//        sb.append("\"");
//        return sb.toString();
        return result;
    }
}
