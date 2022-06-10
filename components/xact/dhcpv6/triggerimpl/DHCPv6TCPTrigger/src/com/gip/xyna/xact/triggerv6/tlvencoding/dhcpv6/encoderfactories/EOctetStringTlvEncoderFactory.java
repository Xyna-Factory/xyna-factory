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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories;

import java.util.Map;
import java.util.regex.Pattern;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.EOctetStringTlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.OctetStringTlvEncoder;



/**
 * Octet string TLV encoder factory.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class EOctetStringTlvEncoderFactory extends AbstractTypeWithValueTlvEncoderFactory {

    private static final Pattern enterpriseNumberPattern = Pattern.compile("[0-9]+");
    private static final Pattern subencodingPattern = Pattern.compile("[0-9a-fA-F]*");

  
    @Override
    protected TlvEncoder createTypeWithValueTlvEncoder(final int typeEncoding, final Map<String, String> arguments) {
        int minLength = 1;
        int maxLength = DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH;
        String subencoding="";

        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            if ("minLength".equals(entry.getKey())) {
                minLength = parseLengthValue(entry);
            } else if ("maxLength".equals(entry.getKey())) {
                maxLength = parseLengthValue(entry);
            } else if ("enterprisenr".equals(entry.getKey())) {
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
        
        if (!arguments.containsKey("enterprisenr")) {
          throw new IllegalArgumentException("Mandatory argument <enterprisenr> missing from provided arguments: <"
                  + arguments + ">.");
        }

        
        return new EOctetStringTlvEncoder(typeEncoding, subencoding, minLength, maxLength);
    }

    private static int parseLengthValue(final Map.Entry<String, String> entry) {
        String value = entry.getValue(); 
        if (!value.matches("([1-9][0-9]?)?[0-9]")) {
            throw new IllegalArgumentException("Key <" + entry.getKey() + "> contains an illegal value: <"
                    + entry.getValue() + ">.");
        }
        int parsedValue = Integer.parseInt(value);
        if (parsedValue > DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH || parsedValue < 1) {
            throw new IllegalArgumentException("Argument <" + entry.getKey() + "> is not in range [1, "
                    + DHCPv6ConfigurationEncoder.MAX_TLV_LENGTH + "]: <" + entry.getValue() + ">.");
        }
        return parsedValue;
    }
}
