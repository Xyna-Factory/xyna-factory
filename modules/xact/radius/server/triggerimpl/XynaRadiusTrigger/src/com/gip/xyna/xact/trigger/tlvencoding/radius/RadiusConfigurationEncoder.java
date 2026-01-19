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
package com.gip.xyna.xact.trigger.tlvencoding.radius;



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
public final class RadiusConfigurationEncoder {

  public static final int MAX_TLV_LENGTH = 255;


  private final Map<String, TlvEncoder> encoders;


  public RadiusConfigurationEncoder(final List<RadiusEncoding> encodings) {
    if (encodings == null) {
      throw new IllegalArgumentException("List of encodings may not be null.");
    }
    List<RadiusEncoding> encodingsList = validateAndMakeCopy(encodings);
    createRootNodesDataTypeMap(Collections.unmodifiableList(encodingsList));
    this.encoders = createRootEncodersMap(encodingsList);

  }


  private List<RadiusEncoding> validateAndMakeCopy(final List<RadiusEncoding> encodings) {
    List<RadiusEncoding> copy = new ArrayList<RadiusEncoding>(encodings.size());
    for (RadiusEncoding encoding : encodings) {
      if (encoding == null) {
        throw new IllegalArgumentException("Found a null encoding in: <" + encodings + ">.");
      }
      copy.add(encoding);
    }
    return copy;
  }


  private Map<String, List<String>> createRootNodesDataTypeMap(final List<RadiusEncoding> encodings) {
    Map<String, List<String>> result = new HashMap<String, List<String>>();
    for (RadiusEncoding encoding : encodings) {
      if (encoding.getParentId() == null) {
        String dataType = encoding.getValueDataTypeName();
        String typeName = encoding.getTypeName();
        if (!result.containsKey(dataType)) {
          result.put(dataType, new ArrayList<String>());
        } else if (result.get(dataType).contains(typeName)) {
          throw new IllegalArgumentException("Type name used more than once: <" + typeName + ">.");
        }
        result.get(dataType).add(typeName);
      }
    }
    for (Map.Entry<String, List<String>> entry : result.entrySet()) {
      entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }

    return Collections.unmodifiableMap(result);
  }


  private Map<String, TlvEncoder> createRootEncodersMap(final List<RadiusEncoding> encodingsList) {
    List<RadiusEncoding> rootNodes = new ArrayList<RadiusEncoding>();
    for (RadiusEncoding encoding : encodingsList) {
      if (encoding.getParentId() == null) { // Oberelemente ohne Eltern sammeln
        rootNodes.add(encoding);
      }
    }
    for (int i = 0; i < rootNodes.size(); ++i) {
      RadiusEncoding encoding = rootNodes.get(i);
      encodingsList.remove(encoding); // Oberelemente ohne Eltern aus uebergebener Liste streichen
      for (int j = (i + 1); j < rootNodes.size(); ++j) {
        if (encoding.getTypeEncoding() == rootNodes.get(j).getTypeEncoding()) {
          // Eigenen Code eingefuegt als IF Abfrage
          if (encoding.getTypeEncoding() != 26)
            throw new IllegalArgumentException("Type encoding <" + encoding.getTypeEncoding() + "> is used by more than: <" + encoding
                + ">.");
        }
      }
    }
    Map<String, TlvEncoder> encodersMap = new HashMap<String, TlvEncoder>();
    for (RadiusEncoding encoding : rootNodes) {
      encodersMap.put(encoding.getTypeName(), createEncoder(encoding, encodingsList));
    }
    if (encodingsList.size() > 0) {
      throw new IllegalArgumentException("Failed to use all nodes when creating encoders map: <" + encodingsList + ">.");
    }

    return encodersMap;
  }


  private TlvEncoder createEncoder(final RadiusEncoding encoding, final List<RadiusEncoding> encodingsList) {
    List<RadiusEncoding> childNodes = new ArrayList<RadiusEncoding>();
    for (RadiusEncoding otherEncoding : encodingsList) {
      if (Integer.valueOf(encoding.getId()).equals(otherEncoding.getParentId())) {
        childNodes.add(otherEncoding); // Kinder zu uebergebenem encoding merken
      }
    }
    for (int i = 0; i < childNodes.size(); ++i) {
      RadiusEncoding childNode = childNodes.get(i);
      encodingsList.remove(childNode); // Kinder aus Liste aller Encodings entfernen
      for (int j = (i + 1); j < childNodes.size(); ++j) {
        if (childNode.getTypeEncoding() == childNodes.get(j).getTypeEncoding()) {
          throw new IllegalArgumentException("Type encoding <" + childNode.getTypeEncoding() + "> is used by more than: <" + childNode
              + ">.");
        }
      } // Typencodierung (Type) kommt mehrfach bei Kindern vor => Exception
    }
    Map<String, TlvEncoder> subNodeEncodings = new HashMap<String, TlvEncoder>();
    for (RadiusEncoding childNode : childNodes) {
      subNodeEncodings.put(childNode.getTypeName(), createEncoder(childNode, encodingsList));
    }
    TlvEncoderFactory factory = TlvEncoderFactoryProvider.get(encoding.getValueDataTypeName());
    if (factory == null) {
      throw new IllegalArgumentException("Unknown value data type: <" + encoding.getValueDataTypeName() + ">.");
    }

    return factory.create(encoding.getTypeEncoding(), encoding.getValueDataTypeArguments(), subNodeEncodings);
  }


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
    target.write(mainData);
  }


  private void encodeNode(final Node node, final OutputStream target) throws IOException {
    TlvEncoder encoder = encoders.get(node.getTypeName());
    if (encoder == null) {
      throw new IllegalArgumentException("Unknown type name: <" + node.getTypeName() + ">.");
    }
    encoder.write(node, target);
  }
}
