/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package xmcp.gitintegration.impl.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.Reference;
import xmcp.gitintegration.impl.ItemDifference;
import xmcp.gitintegration.impl.OutputCreator;

public class ReferenceXmlConverter {

  public static final String TAG_REFERENCES = "references";
  public static final String TAG_REFERENCE = "reference";
  public static final String TAG_PATH = "path";
  public static final String TAG_TYPE = "type";

  public String getTagName() {
    return TAG_REFERENCES;
  }
  
  public void appendReferences(List<? extends Reference> tags, XmlBuilder builder) {
    builder.startElement(TAG_REFERENCES);
    for (Reference tag : tags) {
      appendReference(tag, builder);
    }
    builder.endElement(TAG_REFERENCES);
  }


  public void appendReference(Reference tag, XmlBuilder builder) {
    builder.startElement(TAG_REFERENCE);
    builder.element(TAG_TYPE, tag.getType().toString());
    builder.element(TAG_PATH, tag.getPath());
    builder.endElement(TAG_REFERENCE);
  }
  
  /**
   * Input should point to a node with getNodeName() equals TAG_REFERENCES
   */
  public List<Reference> parseTags(Node n) {
    List<Reference> result = new ArrayList<Reference>();
    if (n.getNodeName().equals(TAG_REFERENCES)) {
      NodeList list = n.getChildNodes();
      for (int i = 0; i < list.getLength(); i++) {
        Node child = list.item(i);
        if (child.getNodeName().equals(TAG_REFERENCE)) {
          Reference tag = parse(child);
          result.add(tag);
        }
      }
    }
    return result;
  }
  
  /**
   * Input should point to a node with getNodeName() equals TAG_REFERENCE
   */
  public Reference parse(Node n) {
    Reference result = new Reference();
    NodeList list = n.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node child = list.item(i);
      String nodeName = child.getNodeName();
      if (nodeName.equals(TAG_PATH)) {
        result.setPath(child.getTextContent());
      } else if (nodeName.equals(TAG_TYPE)) {
        result.setType(child.getTextContent());
      }
    }

    return result;
  }
  
  public void createDifferencesString(StringBuilder ds, List<ItemDifference<Reference>> differences) {
    OutputCreator.appendDiffs(ds, differences, TAG_REFERENCES, this::appendDifference);
  }
  
  private void appendDifference(StringBuilder ds, ItemDifference<Reference> difference) {
    appendFormatted(ds, difference.getFrom());
    if (difference.getType() == MODIFY.class) {
      ds.append(" => ");
    }
    appendFormatted(ds, difference.getTo());
    ds.append("\n");
  }

  /**
   * reference may be null
   */
  private void appendFormatted(StringBuilder sb, Reference reference) {
    if (reference == null) {
      return;
    }
    sb.append(reference.getType()).append(": \"").append(reference.getPath()).append("\"");
  }
}
