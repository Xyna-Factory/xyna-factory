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

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;



/**
 * MAC address TLV encoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class MacAddressTlvEncoder extends AbstractTypeWithValueTlvEncoder {

  private static final Pattern VALUE_MATCHER_PATTERN = Pattern.compile("[0-9A-F]{2}(:[0-9A-F]{2}){5}");

    public MacAddressTlvEncoder(final int typeEncoding) {
        super(typeEncoding);
  }


  @Override
  protected void writeTypeWithValueNode(final TypeWithValueNode node, final OutputStream target) throws IOException {
    String value = node.getValue();
    if (!VALUE_MATCHER_PATTERN.matcher(value.toUpperCase()).matches()) {
      throw new IllegalArgumentException("Invalid MAC address: <" + value + ">.");
    }
    String[] parts = value.split(":", -1);
    target.write(ByteUtil.toByteArray(this.getTypeEncoding(), 2));
    target.write(ByteUtil.toByteArray(parts.length, 2));
    for (String hexValue : parts) {
      target.write(Integer.parseInt(hexValue, 16));
    }
  }
}
