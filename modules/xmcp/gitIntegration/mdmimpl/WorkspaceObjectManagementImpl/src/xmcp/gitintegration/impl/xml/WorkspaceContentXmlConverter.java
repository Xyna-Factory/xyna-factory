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
import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentItem;
import xmcp.gitintegration.impl.WorkspaceConfigSplit;
import xmcp.gitintegration.impl.WorkspaceContentCreator;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;



// Can be used without running factory
public class WorkspaceContentXmlConverter {

  private static final String TAG_WORKSPACECONFIG = "workspaceConfig";
  private static final String ATT_WORKSPACENAME = "workspaceName";
  private static final String ATT_SPLIT = "split";


  public String convertToXml(WorkspaceContent content) {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    XmlBuilder builder = new XmlBuilder();

    builder.append("<!--" + XynaProperty.XML_HEADER_COMMENT.get() + "-->\n");

    openTag(builder, content);
    if (content.getWorkspaceContentItems() != null) {
      for (WorkspaceContentItem item : content.getWorkspaceContentItems()) {
        portal.writeItem(builder, item);
      }
    }
    closeTag(builder);

    return builder.toString();
  }


  private void openTag(XmlBuilder builder, WorkspaceContent content) {
    builder.startElementWithAttributes(TAG_WORKSPACECONFIG);
    builder.addAttribute(ATT_WORKSPACENAME, content.getWorkspaceName());
    if(content.getSplit() != null && !content.getSplit().isEmpty()) {
      builder.addAttribute(ATT_SPLIT, content.getSplit());
    }
    builder.endAttributes();
  }


  private void closeTag(XmlBuilder builder) {
    builder.endElement(TAG_WORKSPACECONFIG);
  }


  public WorkspaceContent convertFromXml(String content) {
    WorkspaceContent result = new WorkspaceContent();
    Document doc = StringToDocument(content);

    convertWorkspaceName(doc, result);
    convertSplit(doc, result);
    convertWorkspaceContentItems(doc, result);


    return result;
  }


  private Document StringToDocument(String content) {
    try {
      return XMLUtils.parseString(content);
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }
  }


  public void addToWorkspaceContent(String input, WorkspaceContent content) {
    Document doc = StringToDocument(input);
    convertWorkspaceContentItems(doc, content);
  }


  private void convertWorkspaceName(Document doc, WorkspaceContent content) {
    try {
      String workspaceName = doc.getDocumentElement().getAttributes().getNamedItem(ATT_WORKSPACENAME).getTextContent();
      content.setWorkspaceName(workspaceName);
    } catch (NullPointerException e) {
      throw new RuntimeException("Could not read workspace name from xml.");
    }
  }
  
  private void convertSplit(Document doc, WorkspaceContent content) {
    try {
      String split = doc.getDocumentElement().getAttributes().getNamedItem(ATT_SPLIT).getTextContent();
      content.unversionedSetSplit(split);
    } catch (NullPointerException e) {
      // document  may not include split information
    }
  }


  private void convertWorkspaceContentItems(Document doc, WorkspaceContent content) {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    List<WorkspaceContentItem> wsItems = null;
    wsItems = content.getWorkspaceContentItems() == null ? new ArrayList<>() : new ArrayList<>(content.getWorkspaceContentItems());
    NodeList list = doc.getDocumentElement().getChildNodes();
    int length = list.getLength();
    for (int i = 0; i < length; i++) {
      WorkspaceContentItem item = portal.parseWorkspaceContentItem(list.item(i));
      wsItems.add(item);
    }
    wsItems.removeIf(x -> x == null);
    content.unversionedSetWorkspaceContentItems(wsItems);
  }

  
  public List<Pair<String, String>> splitContent(WorkspaceContent content) {
    Optional<WorkspaceConfigSplit> type = WorkspaceConfigSplit.fromId(content.getSplit());
    if(type.isEmpty()) {
      throw new RuntimeException("Invalid WorkspaceConfigSplit type: '" + content.getSplit() + "'");
    }
    switch (type.get()) {
      case NONE :
        return List.of(new Pair<>(WorkspaceContentCreator.WORKSPACE_XML_FILENAME, convertToXml(content)));
      case BYTYPE:
        return splitContentByType(content);
      case FINE:
        return splitContentFine(content);
    }
    throw new RuntimeException("Unexpected WorkspaceConfigSplit type: '" + content.getSplit() + "'"); 
  }
  
  public List<Pair<String, String>> splitContentFine(WorkspaceContent content) {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
    List<? extends WorkspaceContentItem> items = content.getWorkspaceContentItems();
    Map<String, List<WorkspaceContentItem>> grouped = new HashMap<>();
    for(WorkspaceContentItem item : items) {
      Class<? extends WorkspaceContentItem> clazz = item.getClass();
      String configType = portal.getTagName(clazz);
      String fileName = sanitize(portal.createItemKeyString(item));
      String path = String.format("%s/%s.xml", configType, fileName);
      grouped.putIfAbsent(path, new ArrayList<>());
      grouped.get(path).add(item);
    }
    
    for(Entry<String, List<WorkspaceContentItem>> group : grouped.entrySet()) {
      String fileName = group.getKey();
      WorkspaceContent groupContent = createGroupWorkspaceContent(content, group.getValue());
      String singleGroupString = convertToXml(groupContent);
      result.add(new Pair<>(fileName, singleGroupString));
    }

    //add workspace.xml for workspace meta data
    WorkspaceContent.Builder c = new WorkspaceContent.Builder();
    c.workspaceName(content.getWorkspaceName());
    c.split(WorkspaceConfigSplit.FINE.getId());
    String meta = convertToXml(c.instance());
    result.add(new Pair<>(WorkspaceContentCreator.WORKSPACE_XML_FILENAME, meta));
    
    return result;
  }

  public List<Pair<String, String>> splitContentByType(WorkspaceContent content) {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
    List<? extends WorkspaceContentItem> items = content.getWorkspaceContentItems();
    Map<Class<? extends WorkspaceContentItem>, List<WorkspaceContentItem>> grouped = new HashMap<>();
    for (WorkspaceContentItem item : items) {
      Class<? extends WorkspaceContentItem> clazz = item.getClass();
      grouped.putIfAbsent(clazz, new ArrayList<WorkspaceContentItem>());
      grouped.get(clazz).add(item);
    }

    for (Entry<Class<? extends WorkspaceContentItem>, List<WorkspaceContentItem>> group : grouped.entrySet()) {
      String fileName = portal.getTagName(group.getKey()) + ".xml";
      WorkspaceContent singleTypeContent = createGroupWorkspaceContent(content, group.getValue());
      String singleTypeContentString = convertToXml(singleTypeContent);
      result.add(new Pair<>(fileName, singleTypeContentString));
    }

    //add workspace.xml for workspace meta data
    WorkspaceContent.Builder c = new WorkspaceContent.Builder();
    c.workspaceName(content.getWorkspaceName());
    c.split(WorkspaceConfigSplit.BYTYPE.getId());
    String meta = convertToXml(c.instance());
    result.add(new Pair<>(WorkspaceContentCreator.WORKSPACE_XML_FILENAME, meta));

    return result;
  }

  private WorkspaceContent createGroupWorkspaceContent(WorkspaceContent full, List<WorkspaceContentItem> items) {
    WorkspaceContent.Builder result = new WorkspaceContent.Builder();
    result.workspaceName(full.getWorkspaceName());
    result.split(full.getSplit());
    result.workspaceContentItems(items);
    return result.instance();
  }

  private String sanitize(String s) {
    String result = s.replaceAll("[^a-zA-Z0-9_\\-\\.]", "-");
    s = s.replaceAll("^-+", "");
    if(s.isEmpty()) {
      return "unnamed";
    }
    if(s.length() > 50) {
      return s.substring(0, 50);
    }
    return result;
  }
  
}
