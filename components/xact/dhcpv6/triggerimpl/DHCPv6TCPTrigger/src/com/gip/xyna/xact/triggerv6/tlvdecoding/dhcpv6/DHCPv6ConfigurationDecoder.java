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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.UnknownDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;




/**
 * DOCSIS configuration decoder.
 */
public final class DHCPv6ConfigurationDecoder implements Decoder {

    public static final String PADDING_DATA_TYPE_NAME = "Padding";
    public static final String END_OF_DATA_MARKER_TYPE_NAME = "EndOfDataMarker";

    final Map<Long, DHCPv6TlvDecoder> decoders;
    private final Map<String, List<Integer>> rootNodesDataTypeEncodingMap;

    private final Set<Long> paddingTypeEncodings;
    private final Set<Integer> multipleEncodingsAllowed;
    private final long paddingTypeEncoding;
    private final long endOfDataTypeEncoding;

    
    public DHCPv6ConfigurationDecoder(final List<DHCPv6Encoding> decodings) {
        if (decodings == null) {
            throw new IllegalArgumentException("Decodings may not be null.");
        }
        this.multipleEncodingsAllowed = new HashSet<Integer>();

        List<DHCPv6Encoding> decodingsList = validateAndMakeCopy(decodings);
        this.rootNodesDataTypeEncodingMap = createRootNodesDataTypeEncodingMap(decodingsList);
        this.decoders = createRootDecodersMap(decodingsList);

        this.paddingTypeEncoding = getTypeEncoding(DHCPv6TlvDecoderFactoryProvider.PADDING);
        this.endOfDataTypeEncoding = getTypeEncoding(DHCPv6TlvDecoderFactoryProvider.END_OF_DATA_MARKER);


        Set<Long> encodings = new HashSet<Long>();
        encodings.add(this.paddingTypeEncoding);
        encodings.add(this.endOfDataTypeEncoding);
        this.paddingTypeEncodings = Collections.unmodifiableSet(encodings);
        
    }

    private List<DHCPv6Encoding> validateAndMakeCopy(final List<DHCPv6Encoding> decodings) {
        List<DHCPv6Encoding> copy = new ArrayList<DHCPv6Encoding>(decodings.size());
        for (DHCPv6Encoding decoding : decodings) {
            if (decoding == null) {
                throw new IllegalArgumentException("Found a null decoding in: <" + decodings + ">.");
            }
            copy.add(decoding);
        }
        return copy;
    }

    private Map<String, List<Integer>> createRootNodesDataTypeEncodingMap(final List<DHCPv6Encoding> encodings) {
        Map<String, List<Integer>> result = new HashMap<String, List<Integer>>();
        for (DHCPv6Encoding encoding : encodings) {
            if (encoding.getParentId() == null) {
                String dataType = encoding.getValueDataTypeName();
                int typeEncoding = (int)encoding.getTypeEncoding();
                if (!result.containsKey(dataType)) {
                    result.put(dataType, new ArrayList<Integer>());
                } else if (result.get(dataType).contains(typeEncoding)) {
                    // Eigenen Code fuer Ausnahme eingefuegt, bei EContainer darf es doppelte Codierungen geben wegen Opt 17
                    if(!dataType.equals("EContainer")&&!dataType.equals("EOctetString"))
                    {
                      throw new IllegalArgumentException("Type encoding used more than once: <" + typeEncoding + ">.");
                    }
                }
                if(!result.get(dataType).contains(typeEncoding))result.get(dataType).add(typeEncoding);
            }
        }
        for (Map.Entry<String, List<Integer>> entry : result.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));            
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<Long, DHCPv6TlvDecoder> createRootDecodersMap(final List<DHCPv6Encoding> decodingsList) {
        List<DHCPv6Encoding> rootNodes = new ArrayList<DHCPv6Encoding>();
        for (DHCPv6Encoding decoding : decodingsList) {
            if (decoding.getParentId() == null) {
                rootNodes.add(decoding);
            }
        }
        for (int i = 0; i < rootNodes.size(); ++i) {
            DHCPv6Encoding decoding = rootNodes.get(i);
            decodingsList.remove(decoding);
            if(decoding.getValueDataTypeName().contains("EContainer")||decoding.getValueDataTypeName().contains("EOctetString"))
            {
              multipleEncodingsAllowed.add((int)decoding.getTypeEncoding());
            }
            
            
            for (int j = (i + 1); j < rootNodes.size(); ++j) {
                if (decoding.getTypeEncoding() == rootNodes.get(j).getTypeEncoding()) {
                  // Eigener Code fuer Ausnahme eingefuegt
                  if(!decoding.getValueDataTypeName().contains("EContainer")&&!decoding.getValueDataTypeName().contains("EOctetString"))
                  {
                    throw new IllegalArgumentException("Type encoding <" + decoding.getTypeEncoding()
                                                       + "> is used by more than: <" + decoding + ">.");
                  }
                }
            }
        }
        Map<Long, DHCPv6TlvDecoder> encodersMap = new HashMap<Long, DHCPv6TlvDecoder>();
        for (DHCPv6Encoding decoding : rootNodes) {
            if(!multipleEncodingsAllowed.contains((int)decoding.getTypeEncoding())) // EIGENER CODE if inklusive Else Block
            {
              encodersMap.put((long)decoding.getTypeEncoding(), createDecoder(decoding, decodingsList));
            }
            
            else
            {
              // Enterprise Nummer holen
              String enrtmp = decoding.getValueDataTypeArguments().get("enterprisenr");
              int enrint = Integer.parseInt(enrtmp);
              
              // eindeutige ID generieren
              long decoderid = (decoding.getTypeEncoding()<<32) | enrint;
              
              // Decoder statt an Codierung an diese neue ID in Liste schreiben
              encodersMap.put(decoderid, createDecoder(decoding, decodingsList));
              
            }
            
            
            
        }
        if (decodingsList.size() > 0) {
            throw new IllegalArgumentException("Failed to use all nodes when creating encoders map: <"
                    + decodingsList + ">.");
        }
        return Collections.unmodifiableMap(encodersMap);
    }

    private DHCPv6TlvDecoder createDecoder(final DHCPv6Encoding encoding, final List<DHCPv6Encoding> encodingsList) {
        List<DHCPv6Encoding> childNodes = new ArrayList<DHCPv6Encoding>();
        
        List<DHCPv6Encoding> markedForRemoval = new ArrayList<DHCPv6Encoding>(); //Versuch
        
        for (DHCPv6Encoding otherEncoding : encodingsList) {
            if (Integer.valueOf(encoding.getId()).equals(otherEncoding.getParentId())) {
                if(!otherEncoding.getValueDataTypeName().contains("EContainer")&&!otherEncoding.getValueDataTypeName().contains("EOctetString")) // Versuch
                {
                  childNodes.add(otherEncoding);
                }
                else
                {
                  markedForRemoval.add(otherEncoding);
                  
                  String enrtmp = otherEncoding.getValueDataTypeArguments().get("enterprisenr");
                  int enrint = Integer.parseInt(enrtmp);
                  
                  // eindeutige ID generieren
                  long decoderid = (otherEncoding.getTypeEncoding()<<32) | enrint;
                  
                  
                  DHCPv6Encoding modifiedEncoding = new DHCPv6Encoding(otherEncoding.getId(), otherEncoding.getParentId(),otherEncoding.getTypeName(),decoderid,otherEncoding.getEnterpriseNr(),otherEncoding.getValueDataTypeName(),otherEncoding.getValueDataTypeArguments());
                  
                  childNodes.add(modifiedEncoding);
                  
                  
                }
            }
        }
        
        for(DHCPv6Encoding e:markedForRemoval)
        {
          encodingsList.remove(e);
        }

        
        for (int i = 0; i < childNodes.size(); ++i) {
            DHCPv6Encoding childNode = childNodes.get(i);
            encodingsList.remove(childNode);
            for (int j = (i + 1); j < childNodes.size(); ++j) {
                if (childNode.getTypeEncoding() == childNodes.get(j).getTypeEncoding()) {
                    if(!childNode.getValueDataTypeName().contains("EContainer")&&!childNode.getValueDataTypeName().contains("EOctetString"))
                    throw new IllegalArgumentException("Type encoding <" + childNode.getTypeEncoding()
                            + "> is used by more than: <" + childNode + ">.");
                }
            }
        }
        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
        for (DHCPv6Encoding childNode : childNodes) {
            subTlvDecoders.add(createDecoder(childNode, encodingsList));
        }
        DHCPv6TlvDecoderFactory factory = DHCPv6TlvDecoderFactoryProvider.get(encoding.getValueDataTypeName());
        if (factory == null) {
            throw new IllegalArgumentException("Unknown value data type: <" + encoding.getValueDataTypeName() + ">.");
        }
        // Versuch (If Block)
//        if(encoding.getTypeEncoding()==17)
//        {
//          if(encoding.getTypeName().contains("4491"))
//          {
//            return factory.create(14491, encoding.getTypeName(), subTlvDecoders);
//          }
//          else if(encoding.getTypeName().contains("3561"))
//          {
//            return factory.create(13561, encoding.getTypeName(), subTlvDecoders);
//          }
//
//        }
        return factory.create(encoding.getTypeEncoding(), encoding.getTypeName(), subTlvDecoders, multipleEncodingsAllowed);
    }

    private int getTypeEncoding(final String dataTypeName) {
        List<Integer> encodings = this.rootNodesDataTypeEncodingMap.get(dataTypeName);
        if (encodings == null) {
            throw new IllegalArgumentException("No decoders defined that use data type: <" + dataTypeName + ">.");
        } else if (encodings.size() != 1) {
            throw new IllegalArgumentException("Expected exactly 1 element using type <" + dataTypeName
                    + ">, but was: <" + encodings + ">.");
        }
        return encodings.get(0);
    }

    public String decode(final byte[] configFileData) throws DecoderException {
        if (configFileData == null) {
            throw new IllegalArgumentException("Config file data may not be null.");
        }
        /*
        int endMarkerSize = getEndMarkerSize(configFileData);
        if (endMarkerSize < 1) {
            throw new DecoderException("Config file does not have a DOCSIS file ending.");
        }
        */
        StringBuilder sb = new StringBuilder();
        /*
        DHCPv6TlvReader reader = new DHCPv6TlvReader(new ByteArrayInputStream(configFileData, 0,
                configFileData.length- endMarkerSize), this.paddingTypeEncodings);
                */

        DHCPv6TlvReader reader = new DHCPv6TlvReader(new ByteArrayInputStream(configFileData, 0,
                                                                              configFileData.length), this.paddingTypeEncodings);

        
        Tlv tlv = null;
        int countReadTlvs = 0;
        try {
            while ((tlv = reader.read()) != null) {
                countReadTlvs++;

                DHCPv6TlvDecoder decoder = null;
                //decoder = this.decoders.get(tlv.getTypeEncoding());
                if(multipleEncodingsAllowed.contains((int)tlv.getTypeEncoding()))
                {
                  byte[] conversion =new byte[4];
                  System.arraycopy(tlv.getValueAsByteArray(), 0, conversion, 0, 4);
                  String convstring = ByteUtil.toHexValue(conversion).substring(2);
                  long enterprisenrtmp = Long.parseLong(convstring, 16);

                  long decoderid = tlv.getTypeEncoding()<<32 | enterprisenrtmp;

                  decoder = this.decoders.get(decoderid);
                  
                }
                else
                {
                  decoder = this.decoders.get(tlv.getTypeEncoding());
                }
                
                if (decoder == null) {
                    decoder = new UnknownDHCPv6TlvDecoder(tlv.getTypeEncoding());
                }
                sb.append(decoder.decode(tlv));
                sb.append("\n");
            }
        } catch (TlvReaderException e) {
            throw new DecoderException("Failed to read TLV nr " + (countReadTlvs + 1) + " from input data.", e);
        } catch (RuntimeException e) {
            throw new DecoderException("Failed to decode TLV nr " + countReadTlvs + ": <" + tlv + ">", e);
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    private int getEndMarkerSize(final byte[] configFileData) {
        if (configFileData.length < 4) {
            return 0;
        } else if (configFileData.length % 4 != 0) {
            return 0;
        }
        for (int i = 1; i < 5; ++i) {
            int value = ((int) configFileData[configFileData.length - i]) & 0xFF;
            if (value == this.endOfDataTypeEncoding) {
                return i;
            } else if (value != this.paddingTypeEncoding) {
                break;
            }
        }
        return 0;
    }

    public boolean isHasMatchingEndMarker(final byte[] configFileData) {
        return 0 < getEndMarkerSize(configFileData);
    }
}
