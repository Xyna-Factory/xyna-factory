/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xact.trigger.tlvencoding.radius.encoders;



import java.io.IOException;
import java.io.OutputStream;

import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusConfigurationEncoder;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TypeWithValueNode;
import com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil;



/**
 * Octet string TLV encoder.
 *
 */
public final class OctetStringTlvEncoder extends AbstractTypeWithValueTlvEncoder {

  private final int minLength;
  private final int maxLength;


  public OctetStringTlvEncoder(final int typeEncoding, final int minLength, final int maxLength) {
    super(typeEncoding);
    if (minLength < 1) {
      throw new IllegalArgumentException("Expected minimum length larger than 0, but got: <" + minLength + ">.");
    } else if (maxLength < minLength) {
      throw new IllegalArgumentException("Expected maximum length to be larger than minimum length <" + minLength + ">, but got: <"
          + maxLength + ">.");
    } else if (maxLength > RadiusConfigurationEncoder.MAX_TLV_LENGTH) {
      throw new IllegalArgumentException("Expected maximum length less or equal to " + RadiusConfigurationEncoder.MAX_TLV_LENGTH + ": <"
          + maxLength + ">.");
    }
    this.minLength = minLength;
    this.maxLength = maxLength;
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
    if (value.matches("0x([0-9A-F]{2})+")) {
      content = ByteUtil.toByteArray(node.getValue());
      //        } else if (QuotedStringFormatUtil.isQuoteFormat(value)) {
      //            content = QuotedStringFormatUtil.unquote(value).getBytes("UTF-8");
    } else {
      try {
        content = value.getBytes("UTF-8");
      } catch (Exception e) {
        throw new IllegalArgumentException("Expected octet string value, but got: <" + value + ">");
      }
    }
    if (content.length > maxLength) {
      throw new IllegalArgumentException("Octet string length longer than <" + maxLength + "> bytes: <" + content.length + ">.");
    } else if (content.length < minLength) {
      throw new IllegalArgumentException("Octet string length shorter than <" + minLength + "> bytes: <" + content.length + ">.");
    }
    target.write(this.getTypeEncoding());
    target.write(content.length + 2);
    target.write(content);
  }
}
