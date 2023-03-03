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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.QuotedStringFormatUtil;



/**
 * Octet string TLV encoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class EOctetStringTlvEncoder extends AbstractTypeWithValueTlvEncoder{

  private static final Pattern VALUE_MATCHER_PATTERN = Pattern.compile("0x([0-9A-F]{2})+");

    private final int minLength;
    private final int maxLength;
    final String subencoding;

    public EOctetStringTlvEncoder(final int typeEncoding, final String subencoding, final int minLength, final int maxLength) {
        super(typeEncoding);
        if (minLength < 1) {
            throw new IllegalArgumentException("Expected minimum length larger than 0, but got: <" + minLength + ">.");
        } else if (maxLength < minLength) {
            throw new IllegalArgumentException("Expected maximum length to be larger than minimum length <" + minLength
                    + ">, but got: <" + maxLength + ">.");
        } else if (maxLength > DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH) {
            throw new IllegalArgumentException("Expected maximum length less or equal to "
                    + DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH + ": <" + maxLength + ">.");
        }
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.subencoding = subencoding;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    protected void writeTypeWithValueNode(final TypeWithValueNode node, final OutputStream target) throws IOException {
        String value = node.getValue();
        byte[] content;
        if (VALUE_MATCHER_PATTERN.matcher(value).matches()) {
            content = ByteUtil.toByteArray(node.getValue());
//        } else if (QuotedStringFormatUtil.isQuoteFormat(value)) {
//            content = QuotedStringFormatUtil.unquote(value).getBytes("UTF-8");
        } else {
          if(value.length()!=0)
          {
            try
            {
              content = value.getBytes("UTF-8");
            }
            catch(Exception e)
            {
              throw new IllegalArgumentException("Expected octet string value, but got: <" + value + ">");
            }
          }
          else
          {
            content = new byte[0];
          }
        }
        if (content.length > maxLength) {
            throw new IllegalArgumentException("Octet string length longer than <" + maxLength + "> bytes: <"
                    + content.length + ">.");
        } 
        // allow zero byte octetstrings
//        else if (content.length < minLength) { 
//            throw new IllegalArgumentException("Octet string length shorter than <" + minLength + "> bytes: <"
//                    + content.length + ">.");
//        }
        target.write(ByteUtil.toByteArray(this.getTypeEncoding(),2));
        target.write(ByteUtil.toByteArray(content.length+ByteUtil.toByteArray(Integer.parseInt(subencoding,16)).length,2));
        target.write(ByteUtil.toByteArray(Integer.parseInt(subencoding,16),4));
        target.write(content);
    }
}
