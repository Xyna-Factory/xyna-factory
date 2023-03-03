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
package com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6DUIDReader;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvReader;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.TlvReaderException;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;




/**
 * Container DOCSIS TLV decoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class IATADHCPv6TlvDecoder extends AbstractDHCPv6TlvDecoder {

    private final Map<Long, DHCPv6TlvDecoder> subTlvDecoders;
    private final Set<Long> paddingTypeEncodings;
    private final Set<Integer> multipleEncodingsAllowed;

    public IATADHCPv6TlvDecoder(final long typeEncoding, final String typeName,
            final List<DHCPv6TlvDecoder> subTlvDecoders,Set<Integer> multipleEncodingsAllowed) {
        super(typeEncoding, typeName);
        if (subTlvDecoders == null) {
            throw new IllegalArgumentException("Sub TLV decoders may not be null.");
        }
        Map<Long, DHCPv6TlvDecoder> decoders = new HashMap<Long, DHCPv6TlvDecoder>();
        for (DHCPv6TlvDecoder decoder : subTlvDecoders) {
            if (decoder == null) {
                throw new IllegalArgumentException("Found null value in sub TLV decoders list.");
            }
            if (decoders.put(decoder.getTypeEncoding(), decoder) != null) {
                if(decoder.getTypeEncoding()!=17)throw new IllegalArgumentException("Decoder for TLV <" + decoder.getTypeEncoding()
                        + "> defined more than once.");
            }
        }
        this.subTlvDecoders = Collections.unmodifiableMap(decoders);
        this.multipleEncodingsAllowed = multipleEncodingsAllowed;

        Set<Long> typeEncodings = new HashSet<Long>();
        for (Map.Entry<Long, DHCPv6TlvDecoder> entry : this.subTlvDecoders.entrySet()) {
            if (entry.getValue() instanceof PaddingDHCPv6TlvDecoder) {
                typeEncodings.add(entry.getKey());
            }
        }
        paddingTypeEncodings = Collections.unmodifiableSet(typeEncodings);
    }

    @Override
    protected String decodeTlvValue(final byte[] value) {
        DHCPv6DUIDReader reader = new DHCPv6DUIDReader(new ByteArrayInputStream(value), this.paddingTypeEncodings);
        DHCPv6TlvReader tlvreader = new DHCPv6TlvReader(new ByteArrayInputStream(value,4,value.length-4), this.paddingTypeEncodings);
        
        
        StringBuilder sb = new StringBuilder();
        Tlv tlv;
        
        try {
          
            tlv = reader.read(100001,4);
            sb.append("\n");
            DHCPv6TlvDecoder decoder = subTlvDecoders.get(tlv.getTypeEncoding());
            if (decoder == null) 
            {
              sb.append(new UnknownDHCPv6TlvDecoder(tlv.getTypeEncoding()).decode(tlv));
            } 
            else {
              sb.append(decoder.decode(tlv));
            }

            while((tlv = tlvreader.read()) != null)
            {
              sb.append("\n");
              if(!multipleEncodingsAllowed.contains((int)tlv.getTypeEncoding()))
              {
                decoder = subTlvDecoders.get(tlv.getTypeEncoding());
              }
              else
              {
                // Enterprise Nr holen
                byte[] conversion =new byte[4];
                System.arraycopy(tlv.getValueAsByteArray(), 0, conversion, 0, 4);
                String convstring = ByteUtil.toHexValue(conversion).substring(2);
                long enterprisenrtmp = Long.parseLong(convstring, 16);

                long decoderid = tlv.getTypeEncoding()<<32 | enterprisenrtmp;
                decoder = subTlvDecoders.get(decoderid);
                  
              }

              if (decoder == null) 
              {
                sb.append(new UnknownDHCPv6TlvDecoder(tlv.getTypeEncoding()).decode(tlv));
              } 
              else {
                sb.append(decoder.decode(tlv));
              }
            }

            
            
            
          
        } catch (TlvReaderException e) {
            throw new IllegalArgumentException("Failed to read TLV data.", e);
        } finally {
            reader.close();
        }
        return sb.toString();
    }
}
