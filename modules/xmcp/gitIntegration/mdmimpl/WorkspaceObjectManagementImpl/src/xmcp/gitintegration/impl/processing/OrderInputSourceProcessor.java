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
import java.util.Map;
import java.util.Objects;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.InputSourceSpecific;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.OrderInputSource;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.impl.ItemDifference;



public class OrderInputSourceProcessor implements WorkspaceContentProcessor<OrderInputSource> {

  private static final String TAG_ORDERINPUTSOURCE = "orderinputsource";
  private static final String TAG_NAME = "name";
  private static final String TAG_TYPE = "type";
  private static final String TAG_ORDERTYPE = "ordertype";
  private static final String TAG_DOCUMENTATION = "documentation";

  private static final int DIFFERENCE_STRING_MAX_LENGTH_VALUE = 50;

  private static OrderInputSourceManagement orderInputSourceManagement;
  private static RevisionManagement revisionManagement;

  private OrderInputSourceManagement getOrderInputSourceManagement() {
    if(orderInputSourceManagement == null) {
      orderInputSourceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
    }
    return orderInputSourceManagement;
  }
  
  private RevisionManagement getRevisionManagement() {
    if(revisionManagement == null) {
      revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    }
    return revisionManagement;
  }

  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends OrderInputSource> from, Collection<? extends OrderInputSource> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<WorkspaceContentDifference>();
    List<OrderInputSource> toWorkingList = new ArrayList<OrderInputSource>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, OrderInputSource> toMap = new HashMap<String, OrderInputSource>();
    for (OrderInputSource toEntry : toWorkingList) {
      toMap.put(toEntry.getName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (OrderInputSource fromEntry : from) {
        OrderInputSource toEntry = toMap.get(fromEntry.getName());
        WorkspaceContentDifference wcd = new WorkspaceContentDifference();
        wcd.setContentType(TAG_ORDERINPUTSOURCE);
        wcd.setExistingItem(fromEntry);
        InputSourceSpecificSupport issSupport = new InputSourceSpecificSupport();
        if (toEntry != null) {
          if (!fromEntry.getType().equals(toEntry.getType()) || !fromEntry.getOrderType().equals(toEntry.getOrderType())
              || !Objects.equals(fromEntry.getDocumentation(), toEntry.getDocumentation())
              || (issSupport.compare(fromEntry.getInputSourceSpecifics(), toEntry.getInputSourceSpecifics()).size() > 0)) {
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
    for (OrderInputSource toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_ORDERINPUTSOURCE);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  @Override
  public OrderInputSource parseItem(Node node) {
    OrderInputSource ois = new OrderInputSource();
    NodeList childNodes = node.getChildNodes();
    InputSourceSpecificSupport issSupport = new InputSourceSpecificSupport();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_NAME)) {
        ois.setName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_TYPE)) {
        ois.setType(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_ORDERTYPE)) {
        ois.setOrderType(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DOCUMENTATION)) {
        ois.setDocumentation(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(issSupport.getTagName())) {
        ois.setInputSourceSpecifics(issSupport.parseTags(childNode));
      }
    }
    return ois;
  }


  @Override
  public void writeItem(XmlBuilder builder, OrderInputSource item) {
    builder.startElement(TAG_ORDERINPUTSOURCE);
    builder.element(TAG_NAME, item.getName());
    builder.element(TAG_TYPE, item.getType());
    builder.element(TAG_ORDERTYPE, item.getOrderType());
    if (item.getDocumentation() != null) {
      builder.element(TAG_DOCUMENTATION, XmlBuilder.encode(item.getDocumentation()));
    }
    InputSourceSpecificSupport issSupport = new InputSourceSpecificSupport();
    issSupport.appendInputSourceSpecifics(item.getInputSourceSpecifics(), builder);
    builder.endElement(TAG_ORDERINPUTSOURCE);
  }


  @Override
  public String getTagName() {
    return TAG_ORDERINPUTSOURCE;
  }


  @Override
  public String createItemKeyString(OrderInputSource item) {
    return item.getName();
  }


  @Override
  public String createDifferencesString(OrderInputSource from, OrderInputSource to) {
    StringBuffer ds = new StringBuffer();

    if (!Objects.equals(from.getType(), to.getType())) {
      ds.append("\n");
      ds.append("    " + TAG_TYPE + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + Objects.toString(from.getType(), "") + "\"=>\"" + Objects.toString(to.getType(), "")
          + "\"");
    }

    if (!Objects.equals(from.getOrderType(), to.getOrderType())) {
      ds.append("\n");
      ds.append("    " + TAG_ORDERTYPE + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + Objects.toString(from.getOrderType(), "") + "\"=>\""
          + Objects.toString(to.getOrderType(), "") + "\"");
    }

    if (!Objects.equals(from.getDocumentation(), to.getDocumentation())) {
      ds.append("\n");
      ds.append("    " + TAG_DOCUMENTATION + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + Objects.toString(from.getDocumentation(), "") + "\"=>\""
          + Objects.toString(to.getDocumentation(), "") + "\"");
    }


    // Block TAG_INPUTSOURCESPECIFICS
    InputSourceSpecificSupport issSupport = new InputSourceSpecificSupport();
    List<ItemDifference<InputSourceSpecific>> issDiffList = getInputSourceSpecificrtDifferenceList(from, to);
    if (issDiffList.size() > 0) {
      ds.append("\n");
      ds.append("    " + issSupport.getTagName());
      for (ItemDifference<InputSourceSpecific> issDiff : issDiffList) {
        StringBuffer refEntry = new StringBuffer();
        refEntry.append("\n");
        refEntry.append("      " + issDiff.getType().getSimpleName() + " ");
        if (issDiff.getType().getSimpleName().equals((CREATE.class.getSimpleName()))) {
          refEntry.append(issDiff.getTo().getKey() + ":" + abbreviate(issDiff.getTo().getValue()));
        } else if (issDiff.getType().getSimpleName().equals((MODIFY.class.getSimpleName()))) {
          refEntry.append(issDiff.getFrom().getKey() + ":" + abbreviate(issDiff.getFrom().getValue()) + "=>" + issDiff.getTo().getKey()
              + ":" + abbreviate(issDiff.getTo().getValue()));
        } else if (issDiff.getType().getSimpleName().equals((DELETE.class.getSimpleName()))) {
          refEntry.append(issDiff.getFrom().getKey() + ":" + abbreviate(issDiff.getFrom().getValue()));
        }
        ds.append(refEntry.toString());
      }
    }

    return ds.toString();
  }


  private List<ItemDifference<InputSourceSpecific>> getInputSourceSpecificrtDifferenceList(OrderInputSource from, OrderInputSource to) {
    InputSourceSpecificSupport issSupport = new InputSourceSpecificSupport();
    return issSupport.compare(from.getInputSourceSpecifics(), to.getInputSourceSpecifics());
  }


  @Override
  public List<OrderInputSource> createItems(Long revision) {
    List<OrderInputSource> resultList = new ArrayList<OrderInputSource>();
    try {
      List<OrderInputSourceStorable> oisStorableList = getOrderInputSourceManagement().getOrderInputSourcesForRevision(revision);
      for (OrderInputSourceStorable oisStorable : oisStorableList) {
        OrderInputSource ois = new OrderInputSource();
        ois.setName(oisStorable.getName());
        ois.setType(oisStorable.getType());
        ois.setOrderType(oisStorable.getOrderType());
        if (oisStorable.getDocumentation() != null) {
          ois.setDocumentation(oisStorable.getDocumentation());
        }
        Map<String, String> map = oisStorable.getParameters();
        List<InputSourceSpecific> issList = new ArrayList<InputSourceSpecific>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
          InputSourceSpecific iss = new InputSourceSpecific();
          iss.setKey(entry.getKey());
          iss.setValue(entry.getValue());
          issList.add(iss);
        }
        ois.setInputSourceSpecifics(issList);
        resultList.add(ois);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return resultList;
  }


  private String abbreviate(String value) {
    String result = value;
    if (value != null && value.length() > DIFFERENCE_STRING_MAX_LENGTH_VALUE) {
      result = value.substring(0, DIFFERENCE_STRING_MAX_LENGTH_VALUE - 1) + "...";
    }
    return result;
  }


  @Override
  public void create(OrderInputSource item, long revision) {
    try {
      Workspace workspace = getRevisionManagement().getWorkspace(revision);
      Map<String, String> parameters = new HashMap<String, String>();
      for (InputSourceSpecific iss : item.getInputSourceSpecifics()) {
        parameters.put(iss.getKey(), iss.getValue());
      }
      OrderInputSourceStorable oisSource = new OrderInputSourceStorable(item.getName(), item.getType(), item.getOrderType(), null, null,
                                                                        workspace.getName(), item.getDocumentation(), parameters);
      getOrderInputSourceManagement().createOrderInputSource(oisSource);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void modify(OrderInputSource from, OrderInputSource to, long revision) {
    try {
      Workspace workspace = getRevisionManagement().getWorkspace(revision);
      Map<String, String> parameters = new HashMap<String, String>();
      for (InputSourceSpecific iss : to.getInputSourceSpecifics()) {
        parameters.put(iss.getKey(), iss.getValue());
      }
      OrderInputSourceStorable fromOisStorable = getOrderInputSourceManagement().getInputSourceByName(revision, to.getName());
      OrderInputSourceStorable toOisSource = new OrderInputSourceStorable(to.getName(), to.getType(), to.getOrderType(), null, null,
                                                                          workspace.getName(), to.getDocumentation(), parameters);
      toOisSource.setId(fromOisStorable.getId());
      getOrderInputSourceManagement().modifyOrderInputSource(toOisSource);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void delete(OrderInputSource item, long revision) {
    try {
      OrderInputSourceStorable oisStorable = getOrderInputSourceManagement().getInputSourceByName(revision, item.getName());
      getOrderInputSourceManagement().deleteOrderInputSource(oisStorable.getId());
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }

}
