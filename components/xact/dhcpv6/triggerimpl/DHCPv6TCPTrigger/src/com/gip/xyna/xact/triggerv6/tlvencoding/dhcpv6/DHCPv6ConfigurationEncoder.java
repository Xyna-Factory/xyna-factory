package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6;



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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * DHCP configuration encoder.
 */
public final class DHCPv6ConfigurationEncoder {

  public static final int MAX_TLV_LENGTH = 256*256;

  public static final String CABLE_MODEM_MIC_DATA_TYPE_NAME = "CableModemMic";
  public static final String CMTS_MIC_DATA_TYPE_NAME = "CmtsMic";
  public static final String PADDING_DATA_TYPE_NAME = "Padding";
  public static final String END_OF_DATA_MARKER_TYPE_NAME = "EndOfDataMarker";

  private final Map<String, TlvEncoder> encoders;
  private final Map<String, List<String>> rootNodesDataTypeMap;

  private final int paddingTypeEncoding;
  private final int endOfDataTypeEncoding;


  public DHCPv6ConfigurationEncoder(final List<DHCPv6Encoding> encodings) {
    if (encodings == null) {
      throw new IllegalArgumentException("List of encodings may not be null.");
    }
    List<DHCPv6Encoding> encodingsList = validateAndMakeCopy(encodings);
    this.rootNodesDataTypeMap = createRootNodesDataTypeMap(Collections.unmodifiableList(encodingsList));
    this.encoders = createRootEncodersMap(encodingsList);


    this.paddingTypeEncoding = this.getTypeEncoding(getTypeName(PADDING_DATA_TYPE_NAME));
    this.endOfDataTypeEncoding = this.getTypeEncoding(getTypeName(END_OF_DATA_MARKER_TYPE_NAME));

  }


  private List<DHCPv6Encoding> validateAndMakeCopy(final List<DHCPv6Encoding> encodings) {
    List<DHCPv6Encoding> copy = new ArrayList<DHCPv6Encoding>(encodings.size());
    for (DHCPv6Encoding encoding : encodings) {
      if (encoding == null) {
        throw new IllegalArgumentException("Found a null encoding in: <" + encodings + ">.");
      }
      copy.add(encoding);
    }
    return copy;
  }


  private Map<String, List<String>> createRootNodesDataTypeMap(final List<DHCPv6Encoding> encodings) {
    Map<String, List<String>> result = new HashMap<String, List<String>>();
    for (DHCPv6Encoding encoding : encodings) {
      if (encoding.getParentId() == null) {
        String dataType = encoding.getValueDataTypeName();
        String typeName = encoding.getTypeName();
        if (!result.containsKey(dataType)) {
          result.put(dataType, new ArrayList<String>());
        }
        else if (result.get(dataType).contains(typeName)) {
          // Eigenen Code fuer Ausnahme eingefuegt, bei EContainer oder EOctetString darf es doppelte Codierungen geben fuer VendorSpecificInformation
          if (!dataType.equals("EContainer") && !dataType.equals("EOctetString")) {
            throw new IllegalArgumentException("Type name used more than once: <" + typeName + ">.");
          }
        }
        result.get(dataType).add(typeName);


      }
    }
    for (Map.Entry<String, List<String>> entry : result.entrySet()) {
      entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }


    return Collections.unmodifiableMap(result);
  }


  private Map<String, TlvEncoder> createRootEncodersMap(final List<DHCPv6Encoding> encodingsList) {
    List<DHCPv6Encoding> rootNodes = new ArrayList<DHCPv6Encoding>();
    for (DHCPv6Encoding encoding : encodingsList) {
      if (encoding.getParentId() == null) { // Oberelemente ohne Eltern sammeln
        rootNodes.add(encoding);
      }
    }
    for (int i = 0; i < rootNodes.size(); ++i) {
      DHCPv6Encoding encoding = rootNodes.get(i);
      encodingsList.remove(encoding); // Oberelemente ohne Eltern aus uebergebener Liste streichen
      for (int j = (i + 1); j < rootNodes.size(); ++j) {
        if (encoding.getTypeEncoding() == rootNodes.get(j).getTypeEncoding()) {
          if (!encoding.getValueDataTypeName().contains("EContainer") && !encoding.getValueDataTypeName().contains("EOctetString"))
          throw new IllegalArgumentException(
                                             "Type encoding <" + encoding.getTypeEncoding() + "> is used by more than: <" + encoding + ">.");
        }
      }
    }
    Map<String, TlvEncoder> encodersMap = new HashMap<String, TlvEncoder>();
    for (DHCPv6Encoding encoding : rootNodes) {
      encodersMap.put(encoding.getTypeName(), createEncoder(encoding, encodingsList));
    }
    if (encodingsList.size() > 0) {
      throw new IllegalArgumentException("Failed to use all nodes when creating encoders map: <" + encodingsList + ">.");
    }
    return encodersMap;
  }


  private TlvEncoder createEncoder(final DHCPv6Encoding encoding, final List<DHCPv6Encoding> encodingsList) {
    List<DHCPv6Encoding> childNodes = new ArrayList<DHCPv6Encoding>();
    for (DHCPv6Encoding otherEncoding : encodingsList) {
      if (Integer.valueOf(encoding.getId()).equals(otherEncoding.getParentId())) {
        childNodes.add(otherEncoding); // Kinder zu uebergebenem encoding merken
      }
    }
    for (int i = 0; i < childNodes.size(); ++i) {
      DHCPv6Encoding childNode = childNodes.get(i);
      encodingsList.remove(childNode); // Kinder aus Liste aller Encodings entfernen
      for (int j = (i + 1); j < childNodes.size(); ++j) {
        if (childNode.getTypeEncoding() == childNodes.get(j).getTypeEncoding()) {
          if(!childNode.getValueDataTypeName().contains("EContainer") && !childNode.getValueDataTypeName().contains("EOctetString") )throw new IllegalArgumentException(
                                             "Type encoding <" + childNode.getTypeEncoding() + "> is used by more than: <" + childNode + ">.");
        }
      } // Typencodierung (Type) kommt mehrfach bei Kindern vor => Exception
    }
    Map<String, TlvEncoder> subNodeEncodings = new HashMap<String, TlvEncoder>();
    for (DHCPv6Encoding childNode : childNodes) {
      subNodeEncodings.put(childNode.getTypeName(), createEncoder(childNode, encodingsList));
    }
    TlvEncoderFactory factory = TlvEncoderFactoryProvider.get(encoding.getValueDataTypeName());
    if (factory == null) {
      throw new IllegalArgumentException("Unknown value data type: <" + encoding.getValueDataTypeName() + ">.");
    }
    return factory.create((int)encoding.getTypeEncoding(), encoding.getValueDataTypeArguments(), subNodeEncodings);
  }


  private int getTypeEncoding(final String typeName) {
    TlvEncoder encoder = this.encoders.get(typeName);
    if (encoder == null) {
      throw new IllegalArgumentException("No encoding exists for <" + typeName + ">.");
    }
    return encoder.getTypeEncoding();
  }


  private String getTypeName(final String dataType) {
    List<String> names = this.rootNodesDataTypeMap.get(dataType);
    if (names.size() != 1) {
      throw new IllegalArgumentException(
                                         "Expected exactly 1 element using type <" + dataType + ">, but was: <" + names + ">.");
    }
    return names.get(0);
  }


  /*
   * public void encode(final TextConfigTree configuration, final OutputStream target) throws IOException { if
   * (configuration == null) { throw new IllegalArgumentException("Configuration may not be null."); } else if (target
   * == null) { throw new IllegalArgumentException("Output stream may not be null."); } ByteArrayOutputStream
   * mainDataOutputStream = new ByteArrayOutputStream(); for (Node node : configuration.getNodes()) { encodeNode(node,
   * mainDataOutputStream); } byte[] mainData = mainDataOutputStream.toByteArray(); int amountPadding = 4; //
   * modifiziert, Vorsicht target.write(mainData); target.write(endOfDataTypeEncoding); for (int i = 0; i <
   * amountPadding; ++i) { target.write(paddingTypeEncoding); } }
   */
  public void encode(final List<Node> nodes, final OutputStream target) throws IOException {

    if (nodes == null) {
      throw new IllegalArgumentException("Nodes may not be null.");
    }
    if (target == null) {
      throw new IllegalArgumentException("Output stream may not be null.");
    }
    ByteArrayOutputStream mainDataOutputStream = new ByteArrayOutputStream();

    for (Node node : nodes) {
      encodeNode(node, mainDataOutputStream);
    }
    byte[] mainData = mainDataOutputStream.toByteArray();
    // int amountPadding = (4 - ((1 + mainData.length) % 4)) % 4;
    target.write(mainData);
    // target.write(endOfDataTypeEncoding);
    /*
     * for (int i = 0; i < amountPadding; ++i) { target.write(paddingTypeEncoding); }
     */

  }


  private void encodeNode(final Node node, final OutputStream target) throws IOException {
    TlvEncoder encoder = encoders.get(node.getTypeName());
    if (encoder == null) {
      throw new IllegalArgumentException("Unknown type name: <" + node.getTypeName() + ">.");
    }
    encoder.write(node, target);
  }


}
