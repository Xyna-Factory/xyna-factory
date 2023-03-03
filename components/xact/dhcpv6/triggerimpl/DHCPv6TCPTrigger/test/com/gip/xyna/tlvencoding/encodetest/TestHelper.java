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
package com.gip.xyna.tlvencoding.encodetest;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTree;




/**
 * Helper utility for tests.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class TestHelper {

    private TestHelper() {
    }

    public static TextConfigTree createTextConfigTree(final Node node) {
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(node);
        return new TextConfigTree(nodes);
    }

    public static List<Integer> toUnsignedIntList(final ByteArrayOutputStream outputStream) {
        return toUnsignedIntList(outputStream.toByteArray());
    }

    public static List<Integer> toUnsignedIntList(final byte[] bytes) {
        List<Integer> values = new ArrayList<Integer>();
        for (byte b : bytes) {
            values.add(((int) b) & 0xFF);
        }
        return values;
    }

    public static String toHex(final ByteArrayOutputStream outputStream) {
        return toHex(outputStream.toByteArray());
    }

    public static String toHex(final byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            byte b = bytes[i];
            if (i > 0) {
                sb.append(" ");
            }
            String hex = Integer.toHexString(((int) b) & 0xFF);
            if (hex.length() < 2) {
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static <E> void add(final List<E> targetList, final E... values) {
        for (E value : values) {
            targetList.add(value);
        }
    }

    public static List<Integer> createIntegerList(final int... values) {
        List<Integer> list = new ArrayList<Integer>();
        for (int value : values) {
            list.add(value);
        }
        return list;
    }

    public static Set<String> createSet(final String ... values) {
        Set<String> result = new HashSet<String>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }
}
