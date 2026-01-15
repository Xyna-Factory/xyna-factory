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
package com.gip.xyna.xact.trigger.tlvdecoding.radius;



import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xact.trigger.tlvdecoding.radius.decoders.UnknownRadiusTlvDecoder;
import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusEncoding;



/**
 * DOCSIS configuration decoder.
 *
 */
public final class RadiusConfigurationDecoder implements Decoder {


    final Map<Integer, RadiusTlvDecoder> decoders;
    private int vendorspecificcounter = 0;
    private Map<String, Integer> vendorspecificmap = new HashMap<String, Integer>();


    public RadiusConfigurationDecoder(final List<RadiusEncoding> decodings) {
        if (decodings == null) {
            throw new IllegalArgumentException("Decodings may not be null.");
        }
        List<RadiusEncoding> decodingsList = validateAndMakeCopy(decodings);
        createRootNodesDataTypeEncodingMap(decodingsList);
        this.decoders = createRootDecodersMap(decodingsList);

    }


    private List<RadiusEncoding> validateAndMakeCopy(final List<RadiusEncoding> decodings) {
        List<RadiusEncoding> copy = new ArrayList<RadiusEncoding>(decodings.size());
        for (RadiusEncoding decoding : decodings) {
            if (decoding == null) {
                throw new IllegalArgumentException("Found a null decoding in: <" + decodings + ">.");
            }
            copy.add(decoding);
        }
        return copy;
    }


    private Map<String, List<Integer>> createRootNodesDataTypeEncodingMap(final List<RadiusEncoding> encodings) {
        Map<String, List<Integer>> result = new HashMap<String, List<Integer>>();
        for (RadiusEncoding encoding : encodings) {
            if (encoding.getParentId() == null) {
                String dataType = encoding.getValueDataTypeName();
                int typeEncoding = encoding.getTypeEncoding();
                if (!result.containsKey(dataType)) {
                    result.put(dataType, new ArrayList<Integer>());
                } else if (result.get(dataType).contains(typeEncoding)) {
                    // Eigenen Code fuer Ausnahme eingefuegt, bei VContainer darf es doppelte Codierungen geben
                    if (!dataType.equals("EContainer")) {
                        throw new IllegalArgumentException("Type encoding used more than once: <" + typeEncoding + ">.");
                    }
                }
                if (!result.get(dataType).contains(typeEncoding))
                    result.get(dataType).add(typeEncoding);
            }
        }
        for (Map.Entry<String, List<Integer>> entry : result.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }


    private Map<Integer, RadiusTlvDecoder> createRootDecodersMap(final List<RadiusEncoding> decodingsList) {
        List<RadiusEncoding> rootNodes = new ArrayList<RadiusEncoding>();
        for (RadiusEncoding decoding : decodingsList) {
            if (decoding.getParentId() == null) {
                rootNodes.add(decoding);
            }
        }
        for (int i = 0; i < rootNodes.size(); ++i) {
            RadiusEncoding decoding = rootNodes.get(i);
            decodingsList.remove(decoding);
            for (int j = (i + 1); j < rootNodes.size(); ++j) {
                if (decoding.getTypeEncoding() == rootNodes.get(j).getTypeEncoding()) {
                    // Eigener Code fuer Ausnahme eingefuegt
                    if (decoding.getTypeEncoding() != 26)
                        throw new IllegalArgumentException("Type encoding <" + decoding.getTypeEncoding() + "> is used by more than: <"
                                + decoding + ">.");
                }
            }
        }
        Map<Integer, RadiusTlvDecoder> encodersMap = new HashMap<Integer, RadiusTlvDecoder>();
        for (RadiusEncoding decoding : rootNodes) {
            if (decoding.getTypeEncoding() != 26) // EIGENER CODE if inklusive Else Block
            {
                encodersMap.put(decoding.getTypeEncoding(), createDecoder(decoding, decodingsList));
            }

            else {
                vendorspecificcounter++;
                encodersMap.put(1000 + vendorspecificcounter, createDecoder(decoding, decodingsList));
                vendorspecificmap.put(decoding.getTypeName().substring("Vendor-Specific".length()), 1000 + vendorspecificcounter);
            }

        }
        if (decodingsList.size() > 0) {
            throw new IllegalArgumentException("Failed to use all nodes when creating encoders map: <" + decodingsList + ">.");
        }
        return Collections.unmodifiableMap(encodersMap);
    }


    private RadiusTlvDecoder createDecoder(final RadiusEncoding encoding, final List<RadiusEncoding> encodingsList) {
        List<RadiusEncoding> childNodes = new ArrayList<RadiusEncoding>();
        for (RadiusEncoding otherEncoding : encodingsList) {
            if (Integer.valueOf(encoding.getId()).equals(otherEncoding.getParentId())) {
                childNodes.add(otherEncoding);
            }
        }
        for (int i = 0; i < childNodes.size(); ++i) {
            RadiusEncoding childNode = childNodes.get(i);
            encodingsList.remove(childNode);
            for (int j = (i + 1); j < childNodes.size(); ++j) {
                if (childNode.getTypeEncoding() == childNodes.get(j).getTypeEncoding()) {
                    throw new IllegalArgumentException("Type encoding <" + childNode.getTypeEncoding() + "> is used by more than: <"
                            + childNode + ">.");
                }
            }
        }
        List<RadiusTlvDecoder> subTlvDecoders = new ArrayList<RadiusTlvDecoder>();
        for (RadiusEncoding childNode : childNodes) {
            subTlvDecoders.add(createDecoder(childNode, encodingsList));
        }
        RadiusTlvDecoderFactory factory = RadiusTlvDecoderFactoryProvider.get(encoding.getValueDataTypeName());
        if (factory == null) {
            throw new IllegalArgumentException("Unknown value data type: <" + encoding.getValueDataTypeName() + ">.");
        }
        return factory.create(encoding.getTypeEncoding(), encoding.getTypeName(), subTlvDecoders);
    }


    public String decode(final byte[] configFileData) throws DecoderException {
        if (configFileData == null) {
            throw new IllegalArgumentException("Config file data may not be null.");
        }

        StringBuilder sb = new StringBuilder();
        RadiusTlvReader reader = new RadiusTlvReader(new ByteArrayInputStream(configFileData, 0, configFileData.length));

        Tlv tlv = null;
        int countReadTlvs = 0;
        try {
            while ((tlv = reader.read()) != null) {
                countReadTlvs++;

                RadiusTlvDecoder decoder = null;
                decoder = this.decoders.get(tlv.getTypeEncoding());
                if (decoder == null) {
                    decoder = new UnknownRadiusTlvDecoder(tlv.getTypeEncoding());
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


    public String decode2(final byte[] configFileData) throws DecoderException {
        if (configFileData == null) {
            throw new IllegalArgumentException("Config file data may not be null.");
        }

        StringBuilder sb = new StringBuilder();
        RadiusTlvReader reader = new RadiusTlvReader(new ByteArrayInputStream(configFileData, 0, configFileData.length));

        Tlv tlv = null;
        int countReadTlvs = 0;
        try {
            while ((tlv = reader.read()) != null) {
                countReadTlvs++;

                RadiusTlvDecoder decoder = null;
                if (tlv.getTypeEncoding() == 26) {
                    List<Byte> value = tlv.getValue();
                    int vendor = value.get(0) * 256 * 256 * 256 + value.get(1) * 256 * 256 + value.get(2) * 256 + value.get(3);
                    if (vendorspecificmap.containsKey(String.valueOf(vendor))) {
                        int posToGet = vendorspecificmap.get(String.valueOf(vendor));
                        decoder = this.decoders.get(posToGet);
                    } else {
                        decoder = null;
                    }
                } else {
                    decoder = this.decoders.get(tlv.getTypeEncoding());
                }

                if (decoder == null) {
                    decoder = new UnknownRadiusTlvDecoder(tlv.getTypeEncoding());
                }
                try {
                    sb.append(decoder.decode(tlv));
                    sb.append("\n");
                } catch (RuntimeException e) {
                    throw e;
                }
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
}
