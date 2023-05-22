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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;



/**
 * Unsigned 1 to 7 byte integer TLV encoder.
 */
public final class Unsigned1to7ByteIntegerTlvEncoder extends AbstractTypeWithValueTlvEncoder {

  private static final Pattern VALUE_MATCHER_PATTERN = Pattern.compile("([1-9][0-9]*)?[0-9]");


    private final int nrBytes;
    private final long minValue;
    private final long maxValue;
    private final long multipleOf;

    public Unsigned1to7ByteIntegerTlvEncoder(final int typeEncoding, final int nrBytes, final Long minValue,
            final Long maxValue, final Long multipleOf) {
        super(typeEncoding);
        if (nrBytes < 1) {
            throw new IllegalArgumentException("Illegal number of bytes (less than 1): <" + nrBytes + ">.");
        } else if (nrBytes > 7) {
            throw new IllegalArgumentException("Illegal number of bytes (larger than 7): <" + nrBytes + ">.");
        }
        long maxMaxValue = computeMaxMaxValue(nrBytes);

        this.nrBytes = nrBytes;
        this.minValue = toLongValue(minValue, 0L);
        this.maxValue = toLongValue(maxValue, maxMaxValue);
        this.multipleOf = toLongValue(multipleOf, 1L);

        if (this.maxValue > maxMaxValue) {
            throw new IllegalArgumentException("Illegal max value (larger than " + maxMaxValue + "): <"
                    + this.maxValue + ">.");
        } else if (this.minValue > this.maxValue) {
            throw new IllegalArgumentException("Illegal min value (larger than max value: <" + this.maxValue + ">): <"
                    + minValue + ">.");
        } else if (this.minValue < 0L) {
            throw new IllegalArgumentException("Illegal min value (less than 0): <" + this.minValue + ">.");
        } else if (this.multipleOf != 1L && this.multipleOf > this.maxValue) {
            throw new IllegalArgumentException("Illegal multiple of value (larger than max value: <" + this.maxValue
                    + ">): <" + this.multipleOf + ">.");
        } else if (this.multipleOf < 1L) {
            throw new IllegalArgumentException("Illegal multiple of value (less than 1): <" + this.multipleOf + ">.");
        }
    }

    private static long computeMaxMaxValue(int nrBytes) {
        long result = 0;
        for (int i = 0; i < nrBytes; ++i) {
            result <<= 8;
            result |= 0xFFL;
        }
        return result;
    }

    private static long toLongValue(final Long value, final long defaultValue) {
        if (value == null) {
            return defaultValue;
        } else {
            return value.longValue();
        }
    }

    public int getNrBytes() {
        return nrBytes;
    }

    public long getMinValue() {
        return minValue;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public long getMultipleOf() {
        return multipleOf;
    }


    @Override
    protected void writeTypeWithValueNode(final TypeWithValueNode node, final OutputStream target) throws IOException {
        String value = node.getValue();
        if (!VALUE_MATCHER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Ivalid unsigned integer value: <" + value + ">, for node: <" + node
                    + ">.");
        }
        long unsignedInt;
        try {
            unsignedInt = Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ivalid unsigned integer value (out of range): <" + value + ">.", e);
        }
        if (unsignedInt < minValue) {
            throw new IllegalArgumentException("Value is less than min value <" + minValue + ">: <" + value + ">.");
        } else if (unsignedInt > maxValue) {
            throw new IllegalArgumentException("Value is larger than max value <" + maxValue + ">: <" + value + ">.");
        } else if (unsignedInt % multipleOf != 0L) {
            throw new IllegalArgumentException("Value is not a multiple of <" + multipleOf + ">: <" + value + ">.");
        }
        target.write(ByteUtil.toByteArray(this.getTypeEncoding(), 2));
        target.write(ByteUtil.toByteArray(this.nrBytes,2));
        target.write(ByteUtil.toByteArray(unsignedInt), (8 - this.nrBytes), this.nrBytes);
    }
}
