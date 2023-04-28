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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMapping;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.XMOMStorable;



public class XMOMStorableProcessor implements WorkspaceContentProcessor<XMOMStorable> {

  private static final String TAG_XMOMSTORABLE = "xmomstorable";
  private static final String TAG_XMLNAME = "xmlname";
  private static final String TAG_PATH = "path";
  private static final String TAG_ODSNAME = "odsname";
  private static final String TAG_FQPATH = "fqpath";
  private static final String TAG_COLUMNNAME = "columnname";


  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends XMOMStorable> from, Collection<? extends XMOMStorable> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<WorkspaceContentDifference>();
    List<XMOMStorable> toWorkingList = new ArrayList<XMOMStorable>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, XMOMStorable> toMap = new HashMap<String, XMOMStorable>();
    for (XMOMStorable toEntry : toWorkingList) {
      toMap.put(createItemKeyString(toEntry), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (XMOMStorable fromEntry : from) {
        XMOMStorable toEntry = toMap.get(createItemKeyString(fromEntry));
        WorkspaceContentDifference wcd = new WorkspaceContentDifference();
        wcd.setContentType(TAG_XMOMSTORABLE);
        wcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          if (!Objects.equals(fromEntry.getPath(), toEntry.getPath()) || !Objects.equals(fromEntry.getODSName(), toEntry.getODSName())
              || !Objects.equals(fromEntry.getColumnName(), toEntry.getColumnName())) {
            wcd.setDifferenceType(new MODIFY());
            wcd.setNewItem(toEntry);
            toWorkingList.remove(toEntry); // remove entry from to-list
          } else {
            toWorkingList.remove(toEntry); // remove entry from to-list
            continue; // EQUAL -> ignore entry
          }
        } else {
          wcd.setDifferenceType(new DELETE());
        }
        wcdList.add(wcd);
      }
    }

    // iterate over toWorking-list (only CREATE-Entries remain)
    for (XMOMStorable toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_XMOMSTORABLE);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  @Override
  public XMOMStorable parseItem(Node node) {
    XMOMStorable filter = new XMOMStorable();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_XMLNAME)) {
        filter.setXMLName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_PATH)) {
        filter.setPath(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_FQPATH)) {
        filter.setFQPath(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_ODSNAME)) {
        filter.setODSName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_COLUMNNAME)) {
        filter.setColumnName(childNode.getTextContent());
      }
    }
    return filter;
  }


  @Override
  public void writeItem(XmlBuilder builder, XMOMStorable item) {
    builder.startElement(TAG_XMOMSTORABLE);
    builder.element(TAG_XMLNAME, item.getXMLName());
    if (item.getPath() != null) {
      builder.element(TAG_PATH, item.getPath());
    }
    builder.element(TAG_ODSNAME, item.getODSName());
    if (item.getFQPath() != null) {
      builder.element(TAG_FQPATH, item.getFQPath());
    }
    if (item.getColumnName() != null) {
      builder.element(TAG_COLUMNNAME, item.getColumnName());
    }
    builder.endElement(TAG_XMOMSTORABLE);
  }


  @Override
  public String getTagName() {
    return TAG_XMOMSTORABLE;
  }


  @Override
  public String createItemKeyString(XMOMStorable item) {
    StringBuilder sb = new StringBuilder();
    sb.append(item.getXMLName());
    if ((item.getFQPath() != null) && (item.getFQPath().length() > 0)) {
      sb.append(":").append(item.getFQPath());
    }
    return sb.toString();
  }


  @Override
  public String createDifferencesString(XMOMStorable from, XMOMStorable to) {
    StringBuffer ds = new StringBuffer();

    if (!Objects.equals(from.getPath(), to.getPath())) {
      ds.append("\n");
      ds.append("    " + TAG_PATH + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getPath() + "\"=>\"" + to.getPath() + "\"");
    }
    if (!Objects.equals(from.getODSName(), to.getODSName())) {
      ds.append("\n");
      ds.append("    " + TAG_ODSNAME + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getODSName() + "\"=>\"" + to.getODSName() + "\"");
    }
    if (!Objects.equals(from.getFQPath(), to.getFQPath())) {
      ds.append("\n");
      ds.append("    " + TAG_FQPATH + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getFQPath() + "\"=>\"" + to.getFQPath() + "\"");
    }
    if (!Objects.equals(from.getColumnName(), to.getColumnName())) {
      ds.append("\n");
      ds.append("    " + TAG_COLUMNNAME + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getColumnName() + "\"=>\"" + to.getColumnName() + "\"");
    }
    return ds.toString();
  }


  @Override
  public List<XMOMStorable> createItems(Long revision) {
    List<XMOMStorable> xmomStorableList = new ArrayList<XMOMStorable>();
    try {
      Collection<XMOMODSMapping> xoMappingList = XMOMODSMappingUtils.getAllMappingsForRevision(revision);
      for (XMOMODSMapping xoMapping : xoMappingList) {
        XMOMStorable xmomStorable = new XMOMStorable();
        xmomStorable.setXMLName(xoMapping.getFqxmlname());
        xmomStorable.setPath(xoMapping.getPath());
        xmomStorable.setODSName(xoMapping.getTablename());
        xmomStorable.setFQPath(xoMapping.getFqpath());
        xmomStorable.setColumnName(xoMapping.getColumnname());
        xmomStorableList.add(xmomStorable);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return xmomStorableList;
  }


  @Override
  public void create(XMOMStorable item, long revision) {
    // TODO Auto-generated method stub

  }


  @Override
  public void modify(XMOMStorable from, XMOMStorable to, long revision) {
    // TODO Auto-generated method stub

  }


  @Override
  public void delete(XMOMStorable item, long revision) {
    // TODO Auto-generated method stub

  }


}
