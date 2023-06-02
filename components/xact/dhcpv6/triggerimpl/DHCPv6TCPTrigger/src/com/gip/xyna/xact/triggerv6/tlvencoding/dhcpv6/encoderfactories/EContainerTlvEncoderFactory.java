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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories;

import java.util.Map;
import java.util.regex.Pattern;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.EContainerTlvEncoder;




/**
 * Container TLV encoder factory.
 */
public final class EContainerTlvEncoderFactory extends AbstractTypeOnlyTlvEncoderFactory {


  private static final Pattern enterpriseNumberPattern = Pattern.compile("[0-9]+");
  private static final Pattern subencodingPattern = Pattern.compile("[0-9a-fA-F]*");


  @Override
    protected TlvEncoder createTypeOnlyTlvEncoder(final int typeEncoding, final Map<String, String> arguments,
            final Map<String, TlvEncoder> subNodeEncodings) {
      /*  
      if (arguments.size() > 0) {
            throw new IllegalArgumentException("Expected no arguments when creating <Container> encoder, but got: <"
                    + arguments + ">.");
        }
        */
      if (!arguments.containsKey("enterprisenr")) {
        throw new IllegalArgumentException("Mandatory argument <enterprisenr> missing from provided arguments: <"
                + arguments + ">.");
      }
      
      String subencoding="";
      
      for (Map.Entry<String, String> entry : arguments.entrySet()) {
            if ("enterprisenr".equals(entry.getKey())) {
                //Umwandlung von EnterpriseNr in Hex
                if(enterpriseNumberPattern.matcher(entry.getValue()).matches())
                {
                  int tmp = Integer.parseInt(entry.getValue());
                  subencoding =Integer.toHexString(tmp);
                }
                else
                {
                  throw new IllegalArgumentException("No Value given for enterprisenr");
                }
            } else {
                throw new IllegalArgumentException("Unknown argument: <" + entry.getKey() + ">.");
            }
      }
      
      if (subencodingPattern.matcher(subencoding).matches()) {
          return new EContainerTlvEncoder(typeEncoding, subencoding, subNodeEncodings);
      } else {
        throw new IllegalArgumentException("No <EContainer> implementation available for <enterprisenr="
                +subencoding + ">.");
      }
    }
}
