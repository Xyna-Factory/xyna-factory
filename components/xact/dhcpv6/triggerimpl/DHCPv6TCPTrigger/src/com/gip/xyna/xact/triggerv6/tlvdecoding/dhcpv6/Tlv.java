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
package com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.xact.triggerv6.tlvdecoding.utilv6.ByteUtil;




/**
 * TLV - type length value object.
 */
public final class Tlv {

    private final long typeEncoding;
    private final List<Byte> value;

    public Tlv(final int typeEncoding, final List<Byte> value) {
        if (typeEncoding < 0){ //|| typeEncoding > 65536) {
            throw new IllegalArgumentException("Invalid type encoding: <" + typeEncoding + ">."); 
        } else if (value == null) {
            throw new IllegalArgumentException("Value may not be null.");
        }
        this.typeEncoding = typeEncoding;
        this.value = validateAndMakeUnmodifiable(value);
    }

    private List<Byte> validateAndMakeUnmodifiable(final List<Byte> value) {
        List<Byte> bytes = new ArrayList<Byte>(value.size());
        for (Byte b : value) {
            if (b == null) {
                throw new IllegalArgumentException("TLV value contains a null element.");
            }
            bytes.add(b);
        }
        return Collections.unmodifiableList(bytes);
    }

    public long getTypeEncoding() {
        return this.typeEncoding;
    }

    public List<Byte> getValue() {
        return this.value;
    }

    public byte[] getValueAsByteArray() {
        byte[] bytes = new byte[this.value.size()];
        for (int i = 0; i < this.value.size(); ++i) {
            bytes[i] = this.value.get(i);
        }
        return bytes;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TLV:{typeEncoding:<");
        sb.append(this.typeEncoding);
        sb.append(">,length:<");
        sb.append(this.value.size());
        sb.append(">,value:<");
        if (this.value.size() > 0) {
            sb.append("0x");
            for (byte b : this.value) {
                sb.append(ByteUtil.toHexValue(b));
            }
        }
        sb.append(">}");
        return sb.toString();
    }
}
