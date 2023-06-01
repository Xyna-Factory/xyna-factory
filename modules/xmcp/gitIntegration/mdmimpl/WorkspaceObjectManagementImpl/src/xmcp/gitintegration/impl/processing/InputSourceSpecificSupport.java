/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package xmcp.gitintegration.impl.processing;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.InputSourceSpecific;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.WorkspaceContentDifferenceType;
import xmcp.gitintegration.impl.ItemDifference;


public class InputSourceSpecificSupport {

  private static final String TAG_INPUTSOURCESPECIFICS = "inputsourcespecifics";
  private static final String TAG_INPUTSOURCESPECIFIC = "inputsourcespecific";
  private static final String TAG_KEY = "key";
  private static final String TAG_VALUE = "value";


  public String getTagName() {
    return TAG_INPUTSOURCESPECIFICS;
  }


  public void appendInputSourceSpecifics(List<? extends InputSourceSpecific> tags, XmlBuilder builder) {
    if (tags != null) {
      builder.startElement(TAG_INPUTSOURCESPECIFICS);
      for (InputSourceSpecific tag : tags) {
        appendInputSourceSupport(tag, builder);
      }
      builder.endElement(TAG_INPUTSOURCESPECIFICS);
    }
  }


  public void appendInputSourceSupport(InputSourceSpecific tag, XmlBuilder builder) {
    builder.startElement(TAG_INPUTSOURCESPECIFIC);
    builder.element(TAG_KEY, tag.getKey());
    builder.element(TAG_VALUE, XmlBuilder.encode(tag.getValue()));
    builder.endElement(TAG_INPUTSOURCESPECIFIC);
  }


  /**
   * Input should point to a node with getNodeName() equals TAG_REFERENCES
   */
  public List<InputSourceSpecific> parseTags(Node n) {
    List<InputSourceSpecific> result = new ArrayList<InputSourceSpecific>();
    NodeList list = n.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node child = list.item(i);
      if (child.getNodeName().equals(TAG_INPUTSOURCESPECIFIC)) {
        InputSourceSpecific tag = parse(child);
        result.add(tag);
      }
    }
    return result;
  }


  /**
   * Input should point to a node with getNodeName() equals TAG_INPUTSOURCESPECIFIC
   */
  public InputSourceSpecific parse(Node n) {
    InputSourceSpecific result = new InputSourceSpecific();
    NodeList list = n.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node child = list.item(i);
      String nodeName = child.getNodeName();
      if (nodeName.equals(TAG_KEY)) {
        result.setKey(child.getTextContent());
      } else if (nodeName.equals(TAG_VALUE)) {
        result.setValue(child.getTextContent());
      }
    }
    return result;
  }


  private static String keyMap(InputSourceSpecific t) {
    return t.getKey();
  }


  //to may be null
  private boolean compareInputSourceSpecificTags(InputSourceSpecific from, InputSourceSpecific to) {
    return to != null && Objects.equals(from.getKey(), to.getKey()) && Objects.equals(from.getValue(), to.getValue());
  }


  public List<ItemDifference<InputSourceSpecific>> compare(Collection<? extends InputSourceSpecific> from,
                                                           Collection<? extends InputSourceSpecific> to) {
    List<ItemDifference<InputSourceSpecific>> result = new ArrayList<ItemDifference<InputSourceSpecific>>();
    from = from == null ? new ArrayList<InputSourceSpecific>() : from;
    List<InputSourceSpecific> toList = to == null ? new ArrayList<InputSourceSpecific>() : new ArrayList<InputSourceSpecific>(to);
    Map<String, InputSourceSpecific> toMap =
        toList.stream().collect(Collectors.toMap(InputSourceSpecificSupport::keyMap, Function.identity()));
    String key;

    // iterate over from-list
    // create MODIFY and DELETE entries
    for (InputSourceSpecific fromEntry : from) {
      key = keyMap(fromEntry);
      InputSourceSpecific toEntry = toMap.get(key);
      toList.remove(toEntry);
      if (!compareInputSourceSpecificTags(fromEntry, toEntry)) {
        Class<? extends WorkspaceContentDifferenceType> type = toEntry == null ? DELETE.class : MODIFY.class;
        result.add(new ItemDifference<InputSourceSpecific>(type, fromEntry, toEntry));
      }
    }

    // iterate over toWorking-list (only CREATE-Entries remain)
    for (InputSourceSpecific tag : toList) {
      result.add(new ItemDifference<InputSourceSpecific>(CREATE.class, null, tag));
    }

    return result;
  }

}
