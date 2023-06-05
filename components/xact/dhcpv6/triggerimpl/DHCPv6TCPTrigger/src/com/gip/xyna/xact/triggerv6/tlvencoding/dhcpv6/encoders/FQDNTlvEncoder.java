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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;




public final class FQDNTlvEncoder extends AbstractTypeWithValueTlvEncoder{

  private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
  private static final Pattern HEX_PATTERN = Pattern.compile("0x");
  private static final Pattern VALUE_MATCHER_PATTERN = Pattern.compile("0x([0-9A-F]{2})+");

  
    public FQDNTlvEncoder(final int typeEncoding) {
        super(typeEncoding);
    }

    
    private static String encodeHEX_FQDN(String name) {
      StringBuilder result = new StringBuilder();
      result.append("0x");
      String[] nameParts = DOT_PATTERN.split(name.toLowerCase());
      for (String part : nameParts) {
        byte[] nameAsBytearray = Charset.forName("US-ASCII").encode(part).array();
        String hexString = ByteUtil.toHexValue(nameAsBytearray);
        String[] hexPart = HEX_PATTERN.split(hexString);// Abschneiden der
        // vornangestellten 0x

        int partlength = part.length();
        StringBuilder sb = new StringBuilder();
        if (partlength < 16) {
          sb.append("0").append(Integer.toHexString(part.length()).toUpperCase());
        }
        else {
          sb.append(Integer.toHexString(part.length()).toUpperCase());
        }

        result.append(sb.toString()).append(hexPart[1]);
      }
      result.append("00");

      return result.toString();
    }

    

    @Override
    protected void writeTypeWithValueNode(final TypeWithValueNode node, final OutputStream target) throws IOException {
        String value = node.getValue();

        if(!VALUE_MATCHER_PATTERN.matcher(value).matches())
        {
          throw new IllegalArgumentException("FQDN Encoder: Given input value is no OctetString!");
        }
        
        String klartext = "";
        byte[] hex = ByteUtil.toByteArray(value);
        try {
          klartext = new String(hex, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
          throw new IllegalArgumentException("Problems in FQDN Encoder: ",e);
        }
       
        String fqdncodiert = encodeHEX_FQDN(klartext);
        
        byte[] content = ByteUtil.toByteArray(fqdncodiert);
        
        target.write(ByteUtil.toByteArray(this.getTypeEncoding(),2));
        target.write(ByteUtil.toByteArray(content.length,2));
        target.write(content);
    }
}
