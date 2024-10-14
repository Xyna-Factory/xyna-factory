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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.FactoryContent;
import xmcp.gitintegration.FactoryContentItem;
import xmcp.gitintegration.impl.FactoryContentCreator;
import xmcp.gitintegration.impl.processing.FactoryContentProcessingPortal;

public class FactoryContentXmlConverter {
  
  private static final String TAG_FACTORYCONFIG = "factoryConfig";

  public String convertToXml(FactoryContent content) {
    FactoryContentProcessingPortal portal = new FactoryContentProcessingPortal();
    XmlBuilder builder = new XmlBuilder();
    
   builder.append(XynaProperty.XML_HEADER_COMMENT.get());
    
    builder.startElement(TAG_FACTORYCONFIG);
    if (content.getFactoryContentItems() != null) {
      for (FactoryContentItem item : content.getFactoryContentItems()) {
        portal.writeItem(builder, item);
      }
    }
    builder.endElement(TAG_FACTORYCONFIG);
    
    return builder.toString();
  }
  
  public FactoryContent convertFromXml(String content) {
    FactoryContent result = new FactoryContent();
    Document doc = StringToDocument(content);

    convertFactoryContentItems(doc, result);

    return result;
  }
  

  private void convertFactoryContentItems(Document doc, FactoryContent content) {
    FactoryContentProcessingPortal portal = new FactoryContentProcessingPortal();
    List<FactoryContentItem> wsItems = null;
    wsItems = content.getFactoryContentItems() == null ? new ArrayList<>() : new ArrayList<>(content.getFactoryContentItems());
    NodeList list = doc.getDocumentElement().getChildNodes();
    int length = list.getLength();
    for (int i = 0; i < length; i++) {
      FactoryContentItem item = portal.parseFactoryContentItem(list.item(i));
      wsItems.add(item);
    }
    wsItems.removeIf(x -> x == null);
    content.setFactoryContentItems(wsItems);
  }
  

  //TODO: Move to shared resource
  private Document StringToDocument(String content) {
    try {
      return XMLUtils.parseString(content);
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }
  }
  
  public List<Pair<String, String>> split(FactoryContent content) {
    FactoryContentProcessingPortal portal = new FactoryContentProcessingPortal();
    List<Pair<String, String>> result = new ArrayList<Pair<String,String>>();
    List<? extends FactoryContentItem> items = content.getFactoryContentItems();
    Map<Class<? extends FactoryContentItem>, List<FactoryContentItem>> grouped = new HashMap<>();
    for(FactoryContentItem item : items) {
      Class<? extends FactoryContentItem> clazz = item.getClass();
      grouped.putIfAbsent(clazz, new ArrayList<FactoryContentItem>());
      grouped.get(clazz).add(item);
    }
    
    for(Entry<Class<? extends FactoryContentItem>, List<FactoryContentItem>> group : grouped.entrySet()) {
      String fileName = portal.getTagName(group.getKey()) + ".xml";
      FactoryContent singleTypeContent = createSingleTypeFactoryContent(content, group.getValue());
      String singleTypeContentString = convertToXml(singleTypeContent);
      result.add(new Pair<>(fileName, singleTypeContentString));
    }
    
    //add factory.xml for meta data
    FactoryContent c = new FactoryContent();
    String meta = convertToXml(c);
    result.add(new Pair<>(FactoryContentCreator.FACTORY_XML_FILENAME, meta));
    
    return result;
  }
  
  public void addToFactoryContent(String input, FactoryContent content) {
    Document doc = StringToDocument(input);
    convertFactoryContentItems(doc, content);
  }
  
  private FactoryContent createSingleTypeFactoryContent(FactoryContent full, List<FactoryContentItem> items) {
    FactoryContent result = new FactoryContent();
    result.setFactoryContentItems(items);
    return result;
  }
  
}
