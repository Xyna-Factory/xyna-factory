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

import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentItem;
import xmcp.gitintegration.impl.WorkspaceContentCreator;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;



// Can be used without running factory
public class WorkspaceContentXmlConverter {

  private static final String TAG_WORKSPACECONFIG = "workspaceConfig";
  private static final String ATT_WORKSPACENAME = "workspaceName";


  public String convertToXml(WorkspaceContent content) {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    XmlBuilder builder = new XmlBuilder();

    builder.append(XynaProperty.XML_HEADER_COMMENT.get());

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
    builder.endAttributes();
  }


  private void closeTag(XmlBuilder builder) {
    builder.endElement(TAG_WORKSPACECONFIG);
  }


  public WorkspaceContent convertFromXml(String content) {
    WorkspaceContent result = new WorkspaceContent();
    Document doc = StringToDocument(content);

    convertWorkspaceName(doc, result);
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
    content.setWorkspaceContentItems(wsItems);
  }


  public List<Pair<String, String>> split(WorkspaceContent content) {
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
      WorkspaceContent singleTypeContent = createSingleTypeWorkspaceContent(content, group.getValue());
      String singleTypeContentString = convertToXml(singleTypeContent);
      result.add(new Pair<>(fileName, singleTypeContentString));
    }

    //add workspace.xml for workspace meta data
    WorkspaceContent c = new WorkspaceContent();
    c.setWorkspaceName(content.getWorkspaceName());
    String meta = convertToXml(c);
    result.add(new Pair<>(WorkspaceContentCreator.WORKSPACE_XML_FILENAME, meta));

    return result;
  }


  private WorkspaceContent createSingleTypeWorkspaceContent(WorkspaceContent full, List<WorkspaceContentItem> items) {
    WorkspaceContent result = new WorkspaceContent();
    result.setWorkspaceName(full.getWorkspaceName());
    result.setWorkspaceContentItems(items);
    return result;
  }

}
