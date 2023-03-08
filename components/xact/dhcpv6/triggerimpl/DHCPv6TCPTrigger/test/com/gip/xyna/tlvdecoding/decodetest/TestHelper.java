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
package com.gip.xyna.tlvdecoding.decodetest;

import java.util.ArrayList;
import java.util.List;

/**
 * Test helper utility.
 */
public final class TestHelper {

    private TestHelper() {
    }

    public static byte[] toByteArray(final String hexValue) {
        if (hexValue == null) {
            throw new IllegalArgumentException("Hex value may not be null.");
        } else if (!hexValue.matches("0x([0-9A-F]{2})+")) {
            throw new IllegalArgumentException("Expected hex value, but got: <" + hexValue + ">");
        }
        String hexString = hexValue.split("x")[1];
        int byteCount = hexString.length() / 2;
        byte[] bytes = new byte[byteCount];
        for (int i = 0; i < byteCount; ++i) {
            int index = i * 2;
            String element = hexString.substring(index, index + 2);
            bytes[i] = (byte) Integer.parseInt(element, 16);
        }
        return bytes;
    }

    public static List<Byte> toByteList(final String hexValue) {
        byte[] byteArray = toByteArray(hexValue);
        List<Byte> bytes = new ArrayList<Byte>(byteArray.length);
        for (byte b : byteArray) {
            bytes.add(b);
        }
        return bytes;
    }
}
