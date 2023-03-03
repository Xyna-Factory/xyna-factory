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
package com.gip.xyna.xact.triggerv6.tlvdecoding.utilv6;

/**
 * Byte util.
 */
public final class ByteUtil {

    private ByteUtil() {
    }

    public static String toHexValue(final byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Byte array may not be null.");
        } else if (bytes.length < 1) {
            throw new IllegalArgumentException("Empty byte array provided.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("0x");
        for (byte b : bytes) {
            sb.append(toHexValue(b));
        }
        return sb.toString();
    }

    public static String toHexValue(byte b) {
        String hex = Integer.toHexString(((int) b) & 0xFF);
        if (hex.length() < 2) {
            return "0" + hex.toUpperCase(); 
        }
        return hex.toUpperCase();
    }
}
