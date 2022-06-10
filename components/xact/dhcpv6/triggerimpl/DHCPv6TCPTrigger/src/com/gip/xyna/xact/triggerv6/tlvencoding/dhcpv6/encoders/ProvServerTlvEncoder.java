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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;



/**
 * Octet string TLV encoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class ProvServerTlvEncoder extends AbstractTypeWithValueTlvEncoder{

  private static final Pattern VALUE_MATCHER_PATTERN = Pattern
      .compile("(([0-9]*[a-zA-Z-]{1,}[0-9]*)+\\.)+([0-9]*[a-zA-Z-]{1,}[0-9]*)+");


  public ProvServerTlvEncoder(final int typeEncoding) {
    super(typeEncoding);
  }


  @Override
  protected void writeTypeWithValueNode(final TypeWithValueNode node, final OutputStream target) throws IOException {
    String value = node.getValue();

    if (!VALUE_MATCHER_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("No valid domainname given for ProvServer.");
    }

    List<Byte> res = new ArrayList<Byte>();

    String[] parts = SPLIT_AT_DOT_PATTERN.split(value.trim().toLowerCase());
    for (String split : parts) {
      int len = split.length();
      res.add((byte) len);

      byte[] tmp = split.getBytes();
      for (byte b : tmp) {
        res.add(b);
      }

    }

    byte[] content = new byte[res.size()];

    for (int z = 0; z < res.size(); z++) {
      content[z] = res.get(z);
    }

    target.write(this.getTypeEncoding());
    target.write(content.length + 2);
    target.write((byte) 0);
    target.write(content);
    target.write((byte) 0);

  }
}
