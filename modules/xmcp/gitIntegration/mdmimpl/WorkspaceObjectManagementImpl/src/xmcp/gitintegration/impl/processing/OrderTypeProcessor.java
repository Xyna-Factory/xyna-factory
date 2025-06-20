/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter.DestinationValueParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.cleanup.CleanupDispatcher;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.planning.PlanningDispatcher;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.Capacity;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.DispatcherDestination;
import xmcp.gitintegration.InheritanceRule;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.OrderType;
import xmcp.gitintegration.PrioritySetting;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferenceType;



public class OrderTypeProcessor implements WorkspaceContentProcessor<OrderType> {

  private static final String TAG_ORDERTYPE = "ordertype";
  private static final String TAG_NAME = "name";
  private static final String TAG_DOCUMENTATION = "documentation";
  private static final String TAG_DISPATCHERDESTINATIONS = "dispatcherdestinations";
  private static final String TAG_DISPATCHERDESTINATION = "dispatcherdestination";
  private static final String TAG_DISPATCHERNAME = "dispatchername";
  private static final String TAG_DESTINATIONTYPE = "destinationtype";
  private static final String TAG_DESTINATIONVALUE = "destinationvalue";
  private static final String TAG_INHERITANCERULES = "inheritancerules";
  private static final String TAG_INHERITANCERULE = "inheritancerule";
  private static final String TAG_PARAMETERTYPE = "parameterType";
  private static final String TAG_CHILDFILTER = "childFilter";
  private static final String TAG_VALUE = "value";
  private static final String TAG_PRECEDENCE = "precedence";
  private static final String TAG_CAPACITIES = "capacities";
  private static final String TAG_CAPACITY = "capacity";
  private static final String TAG_CAPACITYNAME = "capacityname";
  private static final String TAG_CARDNINALITY = "cardinality";

  private static final String TAG_PRIRORITYSETTING = "prioritysetting";
  private static final String TAG_PRIRORITY = "priority";

  private static final String TAG_MONITORINGLEVEL = "monitoringlevel";

  public static final String DISPATCHERNAME_PLANNING = "PlanningDispatcher";
  public static final String DISPATCHERNAME_EXECUTION = "ExecutionDispatcher";
  public static final String DISPATCHERNAME_CLEANUP = "CleanupDispatcher";

  private static OrdertypeManagement orderTypeManagement;
  private static RevisionManagement revisionManagement;
  private static XynaProcessingBase processing;


  private static XynaProcessingBase getProcessing() {
    if(processing == null) {
      processing = XynaFactory.getInstance().getProcessing();
    }
    return processing;
  }

  private static OrdertypeManagement getOrderTypeManagement() {
    if(orderTypeManagement == null) {
      orderTypeManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();
    }
    return orderTypeManagement;
  }

  private static RevisionManagement getRevisionManagement() {
    if (revisionManagement == null) {
      revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    }
    return revisionManagement;
  }

  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends OrderType> from, Collection<? extends OrderType> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<WorkspaceContentDifference>();
    List<OrderType> toWorkingList = new ArrayList<OrderType>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, OrderType> toMap = new HashMap<String, OrderType>();
    for (OrderType toEntry : toWorkingList) {
      toMap.put(toEntry.getName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (OrderType fromEntry : from) {
        OrderType toEntry = toMap.get(fromEntry.getName());

        WorkspaceContentDifference wcd = new WorkspaceContentDifference();
        wcd.setContentType(TAG_ORDERTYPE);
        wcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          int fromPrio = 0;
          if (fromEntry.getPrioritySetting() != null) {
            fromPrio = fromEntry.getPrioritySetting().getPriority();
          }
          int toPrio = 0;
          if (toEntry.getPrioritySetting() != null) {
            toPrio = toEntry.getPrioritySetting().getPriority();
          }
          int fromMonitotingLevel = 0;
          if (fromEntry.getMonitoringLevel() != null) {
            fromMonitotingLevel = fromEntry.getMonitoringLevel();
          }
          int toMonitotingLevel = 0;
          if (toEntry.getMonitoringLevel() != null) {
            toMonitotingLevel = toEntry.getMonitoringLevel();
          }

          if (!Objects.equals(fromEntry.getDocumentation(), toEntry.getDocumentation())
              || (getDispatcherDestinationDiffTypeMap(fromEntry, toEntry).size() > 0)
              || (getInheritanceRuleDiffTypeMap(fromEntry, toEntry).size() > 0) || (getCapacityDiffTypeMap(fromEntry, toEntry).size() > 0)
              || (fromPrio != toPrio) || (fromMonitotingLevel != toMonitotingLevel)) {
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
    for (OrderType toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_ORDERTYPE);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  @Override
  public OrderType parseItem(Node node) {
    OrderType ot = new OrderType();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_NAME)) {
        ot.setName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DOCUMENTATION)) {
        ot.setDocumentation(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DISPATCHERDESTINATIONS)) {
        ot.setDispatcherDestinations(parseDispatcherDestinations(childNode));
      } else if (childNode.getNodeName().equals(TAG_INHERITANCERULES)) {
        ot.setInheritanceRules(parseInheritanceRules(childNode));
      } else if (childNode.getNodeName().equals(TAG_CAPACITIES)) {
        ot.setCapacities(parseCapacities(childNode));
      } else if (childNode.getNodeName().equals(TAG_PRIRORITYSETTING)) {
        ot.setPrioritySetting(parsePrioritySetting(childNode));
      } else if (childNode.getNodeName().equals(TAG_MONITORINGLEVEL)) {
        if (childNode.getTextContent() != null && !childNode.getTextContent().isEmpty()) {
          ot.setMonitoringLevel(Integer.parseInt(childNode.getTextContent()));
        }
      }
    }
    return ot;
  }


  private static List<DispatcherDestination> parseDispatcherDestinations(Node node) {
    List<DispatcherDestination> ddList = new ArrayList<DispatcherDestination>();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_DISPATCHERDESTINATION)) {
        ddList.add(parseDispatcherDestination(childNode));
      }
    }
    return ddList;
  }


  private static DispatcherDestination parseDispatcherDestination(Node node) {
    DispatcherDestination dd = new DispatcherDestination();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_DISPATCHERNAME)) {
        dd.setDispatcherName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DESTINATIONTYPE)) {
        dd.setDestinationType(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DESTINATIONVALUE)) {
        dd.setDestinationValue(childNode.getTextContent());
      }
    }
    return dd;
  }



  private static List<InheritanceRule> parseInheritanceRules(Node node) {
    List<InheritanceRule> ihList = new ArrayList<InheritanceRule>();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_INHERITANCERULE)) {
        ihList.add(parseInheritanceRule(childNode));
      }
    }
    return ihList;
  }


  private static InheritanceRule parseInheritanceRule(Node node) {
    InheritanceRule ih = new InheritanceRule();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_PARAMETERTYPE)) {
        ih.setParameterType(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_CHILDFILTER)) {
        ih.setChildFilter(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_VALUE)) {
        String value = childNode.getTextContent();
        ih.setValue(value.equals("null") ? "" : value);
      } else if (childNode.getNodeName().equals(TAG_PRECEDENCE)) {
        ih.setPrecedence(childNode.getTextContent());
      }
    }
    return ih;
  }


  private static List<Capacity> parseCapacities(Node node) {
    List<Capacity> capList = new ArrayList<Capacity>();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_CAPACITY)) {
        capList.add(parseCapacity(childNode));
      }
    }
    return capList;
  }


  private static Capacity parseCapacity(Node node) {
    Capacity cap = new Capacity();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_CAPACITYNAME)) {
        cap.setCapacityName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_CARDNINALITY)) {
        cap.setCardinality(Integer.parseInt(childNode.getTextContent()));
      }
    }
    return cap;
  }


  private static PrioritySetting parsePrioritySetting(Node node) {
    PrioritySetting ps = new PrioritySetting();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_PRIRORITY)) {
        if ((childNode.getTextContent() != null) && !childNode.getTextContent().isEmpty()) {
          ps.setPriority(Integer.parseInt(childNode.getTextContent()));
        }
      }
    }
    return ps;
  }


  @Override
  public void writeItem(XmlBuilder builder, OrderType item) {
    builder.startElement(TAG_ORDERTYPE);
    builder.element(TAG_NAME, item.getName());
    if (item.getDocumentation() != null) {
      builder.element(TAG_DOCUMENTATION, XmlBuilder.encode(item.getDocumentation()));
    }
    if ((item.getDispatcherDestinations() != null) && (item.getDispatcherDestinations().size() > 0)) {
      builder.startElement(TAG_DISPATCHERDESTINATIONS);
      for (DispatcherDestination de : item.getDispatcherDestinations()) {
        builder.startElement(TAG_DISPATCHERDESTINATION);
        builder.element(TAG_DISPATCHERNAME, de.getDispatcherName());
        builder.element(TAG_DESTINATIONTYPE, de.getDestinationType());
        builder.element(TAG_DESTINATIONVALUE, de.getDestinationValue());
        builder.endElement(TAG_DISPATCHERDESTINATION);
      }
      builder.endElement(TAG_DISPATCHERDESTINATIONS);
    }
    if ((item.getInheritanceRules() != null) && (item.getInheritanceRules().size() > 0)) {
      builder.startElement(TAG_INHERITANCERULES);
      for (InheritanceRule ih : item.getInheritanceRules()) {
        builder.startElement(TAG_INHERITANCERULE);
        builder.element(TAG_PARAMETERTYPE, ih.getParameterType());
        builder.element(TAG_CHILDFILTER, ih.getChildFilter());
        builder.element(TAG_VALUE, ih.getValue());
        builder.element(TAG_PRECEDENCE, ih.getPrecedence());
        builder.endElement(TAG_INHERITANCERULE);
      }
      builder.endElement(TAG_INHERITANCERULES);
    }
    if ((item.getCapacities() != null) && (item.getCapacities().size() > 0)) {
      builder.startElement(TAG_CAPACITIES);
      for (Capacity cap : item.getCapacities()) {
        builder.startElement(TAG_CAPACITY);
        builder.element(TAG_CAPACITYNAME, cap.getCapacityName());
        builder.element(TAG_CARDNINALITY, Integer.toString(cap.getCardinality()));
        builder.endElement(TAG_CAPACITY);
      }
      builder.endElement(TAG_CAPACITIES);
    }
    if (item.getPrioritySetting() != null) {
      builder.startElement(TAG_PRIRORITYSETTING);
      builder.element(TAG_PRIRORITY, Integer.toString(item.getPrioritySetting().getPriority()));
      builder.endElement(TAG_PRIRORITYSETTING);
    }
    if (item.getMonitoringLevel() != null) {
      builder.element(TAG_MONITORINGLEVEL, item.getMonitoringLevel().toString());
    }
    builder.endElement(TAG_ORDERTYPE);
  }


  @Override
  public String getTagName() {
    return TAG_ORDERTYPE;
  }


  @Override
  public String createItemKeyString(OrderType item) {
    return item.getName();
  }


  @Override
  public String createDifferencesString(OrderType from, OrderType to) {
    StringBuffer ds = new StringBuffer();

    if (!Objects.equals(from.getDocumentation(), to.getDocumentation())) {
      ds.append("\n");
      ds.append("    " + TAG_DOCUMENTATION + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getDocumentation() + "\"=>\"" + to.getDocumentation() + "\"");
    }

    // Block TAG_DISPATCHERDESTINATIONS
    Map<WorkspaceContentDifferenceType, List<DispatcherDestination>> ddMap = getDispatcherDestinationDiffTypeMap(from, to);
    if (ddMap.size() > 0) {
      ds.append("\n");
      ds.append("    " + TAG_DISPATCHERDESTINATIONS);
      for (Map.Entry<WorkspaceContentDifferenceType, List<DispatcherDestination>> entry : ddMap.entrySet()) {
        WorkspaceContentDifferenceType wcdt = entry.getKey();
        if (wcdt.getClass().getSimpleName().equals(MODIFY.class.getSimpleName())) {
          // MODIFY
          for (int i = 0; i < entry.getValue().size(); i += 2) {
            DispatcherDestination ddFrom = entry.getValue().get(i);
            DispatcherDestination ddTo = entry.getValue().get(i + 1);
            StringBuffer ddEntry = new StringBuffer();
            ddEntry.append("\n");
            ddEntry.append("      " + wcdt.getClass().getSimpleName() + " ");
            ddEntry.append(ddFrom.getDispatcherName() + ":" + ddFrom.getDestinationValue());
            ddEntry.append("=>");
            ddEntry.append(ddTo.getDispatcherName() + ":" + ddTo.getDestinationValue());
            ds.append(ddEntry.toString());
          }
        } else {
          // CREATE / DELETE
          for (DispatcherDestination dd : entry.getValue()) {
            StringBuffer ddEntry = new StringBuffer();
            ddEntry.append("\n");
            ddEntry.append("      " + wcdt.getClass().getSimpleName() + " ");
            ddEntry.append(dd.getDispatcherName() + ":" + dd.getDestinationValue());
            ds.append(ddEntry.toString());
          }
        }
      }
    }

    // Block TAG_INHERITANCERULES
    Map<WorkspaceContentDifferenceType, List<InheritanceRule>> ihMap = getInheritanceRuleDiffTypeMap(from, to);
    if (ihMap.size() > 0) {
      ds.append("\n");
      ds.append("    " + TAG_INHERITANCERULES);
      for (Map.Entry<WorkspaceContentDifferenceType, List<InheritanceRule>> entry : ihMap.entrySet()) {
        WorkspaceContentDifferenceType wcdt = entry.getKey();
        if (wcdt.getClass().getSimpleName().equals(MODIFY.class.getSimpleName())) {
          // MODIFY
          for (int i = 0; i < entry.getValue().size(); i += 2) {
            InheritanceRule ihFrom = entry.getValue().get(i);
            InheritanceRule ihTo = entry.getValue().get(i + 1);
            StringBuffer ihEntry = new StringBuffer();
            ihEntry.append("\n");
            ihEntry.append("      " + wcdt.getClass().getSimpleName() + " ");
            ihEntry.append(ihFrom.getParameterType() + ":" + ihFrom.getChildFilter() + ":" + ihFrom.getValue());
            ihEntry.append("=>");
            ihEntry.append(ihTo.getParameterType() + ":" + ihTo.getChildFilter() + ":" + ihTo.getValue());
            ds.append(ihEntry.toString());
          }
        } else {
          // CREATE / DELETE
          for (InheritanceRule ih : entry.getValue()) {
            StringBuffer ihEntry = new StringBuffer();
            ihEntry.append("\n");
            ihEntry.append("      " + wcdt.getClass().getSimpleName() + " ");
            ihEntry.append(ih.getParameterType() + ":" + ih.getChildFilter() + ":" + ih.getValue());
            ds.append(ihEntry.toString());
          }
        }
      }
    }

    // Block TAG_CAPACITIES
    Map<WorkspaceContentDifferenceType, List<Capacity>> capMap = getCapacityDiffTypeMap(from, to);
    if (capMap.size() > 0) {
      ds.append("\n");
      ds.append("    " + TAG_CAPACITIES);
      for (Map.Entry<WorkspaceContentDifferenceType, List<Capacity>> entry : capMap.entrySet()) {
        WorkspaceContentDifferenceType wcdt = entry.getKey();
        if (wcdt.getClass().getSimpleName().equals(MODIFY.class.getSimpleName())) {
          // MODIFY
          for (int i = 0; i < entry.getValue().size(); i += 2) {
            Capacity capFrom = entry.getValue().get(i);
            Capacity capTo = entry.getValue().get(i + 1);
            StringBuffer capEntry = new StringBuffer();
            capEntry.append("\n");
            capEntry.append("      " + wcdt.getClass().getSimpleName() + " ");
            capEntry.append(capFrom.getCapacityName() + ":" + capFrom.getCardinality());
            capEntry.append("=>");
            capEntry.append(capTo.getCapacityName() + ":" + capTo.getCardinality());
            ds.append(capEntry.toString());
          }
        } else {
          // CREATE / DELETE
          for (Capacity cap : entry.getValue()) {
            StringBuffer capEntry = new StringBuffer();
            capEntry.append("\n");
            capEntry.append("      " + wcdt.getClass().getSimpleName() + " ");
            capEntry.append(cap.getCapacityName() + ":" + cap.getCardinality());
            ds.append(capEntry.toString());
          }
        }
      }
    }

    // Block TAG_PRIRORITYSETTING
    int fromPrio = 0;
    if (from.getPrioritySetting() != null) {
      fromPrio = from.getPrioritySetting().getPriority();
    }
    int toPrio = 0;
    if (to.getPrioritySetting() != null) {
      toPrio = to.getPrioritySetting().getPriority();
    }

    if (fromPrio != toPrio) {
      ds.append("\n");
      ds.append("    " + TAG_PRIRORITYSETTING + " ");
      ds.append(CREATE.class.getSimpleName() + " \"" + fromPrio + "\"=>\"" + toPrio + "\"");
    }

    // Block TAG_MONITORINGLEVEL
    int fromMonitotingLevel = 0;
    if (from.getMonitoringLevel() != null) {
      fromMonitotingLevel = from.getMonitoringLevel();
    }
    int toMonitotingLevel = 0;
    if (to.getMonitoringLevel() != null) {
      toMonitotingLevel = to.getMonitoringLevel();
    }

    if (fromMonitotingLevel != toMonitotingLevel) {
      ds.append("\n");
      ds.append("    " + TAG_MONITORINGLEVEL + " ");
      ds.append(CREATE.class.getSimpleName() + " \"" + fromMonitotingLevel + "\"=>\"" + toMonitotingLevel + "\"");
    }

    return ds.toString();
  }


  private static Map<WorkspaceContentDifferenceType, List<DispatcherDestination>> getDispatcherDestinationDiffTypeMap(OrderType from,
                                                                                                                      OrderType to) {
    Map<WorkspaceContentDifferenceType, List<DispatcherDestination>> resultMap =
        new HashMap<WorkspaceContentDifferenceType, List<DispatcherDestination>>();

    // Create helper maps for finding the CREATE/DELETE/MODIFY Entries
    Map<String, DispatcherDestination> fromMap = new HashMap<String, DispatcherDestination>();
    if (from.getDispatcherDestinations() != null) {
      for (DispatcherDestination dd : from.getDispatcherDestinations()) {
        fromMap.put(dd.getDispatcherName(), dd);
      }
    }
    Map<String, DispatcherDestination> toMap = new HashMap<String, DispatcherDestination>();
    if (to.getDispatcherDestinations() != null) {
      for (DispatcherDestination dd : to.getDispatcherDestinations()) {
        toMap.put(dd.getDispatcherName(), dd);
      }
    }

    // Create CREATE List
    if (to.getDispatcherDestinations() != null) {
      List<DispatcherDestination> createList = new ArrayList<DispatcherDestination>();
      for (DispatcherDestination dd : to.getDispatcherDestinations()) {
        if (fromMap.get(dd.getDispatcherName()) == null && isCustomDestination(dd, to.getName())) {
          createList.add(dd);
        }
      }
      if (createList.size() > 0) {
        resultMap.put(new CREATE(), createList);
      }
    }

    // Create DELETE List
    if (from.getDispatcherDestinations() != null) {
      List<DispatcherDestination> deleteList = new ArrayList<DispatcherDestination>();
      for (DispatcherDestination dd : from.getDispatcherDestinations()) {
        if (toMap.get(dd.getDispatcherName()) == null) {
          deleteList.add(dd);
        }
      }
      if (deleteList.size() > 0) {
        resultMap.put(new DELETE(), deleteList);
      }
    }

    // Create MODIFY List
    if (from.getDispatcherDestinations() != null) {
      List<DispatcherDestination> modifyList = new ArrayList<DispatcherDestination>();
      for (DispatcherDestination dd : from.getDispatcherDestinations()) {
        if (toMap.get(dd.getDispatcherName()) != null) {
          DispatcherDestination fromDd = dd;
          DispatcherDestination toDd = toMap.get(dd.getDispatcherName());
          if (!fromDd.getDestinationType().equals(toDd.getDestinationType())
              || !fromDd.getDestinationValue().equals(toDd.getDestinationValue())) {
            // modifyList has 2 Elements (from,to)
            modifyList.add(fromDd);
            modifyList.add(toDd);
          }
        }
      }
      if (modifyList.size() > 0) {
        resultMap.put(new MODIFY(), modifyList);
      }
    }
    return resultMap;
  }

  private static String createKey(InheritanceRule ir) {
    return ir.getParameterType() + ":" + ir.getChildFilter();
  }
  
  
  private static boolean isCustomDestination(DispatcherDestination dd, String orderTypeName) {
    String dispatcherName = dd.getDispatcherName();
    if (DISPATCHERNAME_PLANNING.equals(dispatcherName)) {
      return !XynaDispatcher.DESTINATION_DEFAULT_PLANNING.getFQName().equals(dd.getDestinationValue());
    } else if (DISPATCHERNAME_EXECUTION.equals(dispatcherName)) {
      return !orderTypeName.equals(dd.getDestinationValue());
    } else if (DISPATCHERNAME_CLEANUP.equals(dispatcherName)) {
      return !XynaDispatcher.DESTINATION_EMPTY_WORKFLOW.getFQName().equals(dd.getDestinationValue());
    }
    return true;
  }

  private static Map<WorkspaceContentDifferenceType, List<InheritanceRule>> getInheritanceRuleDiffTypeMap(OrderType from, OrderType to) {
    Map<WorkspaceContentDifferenceType, List<InheritanceRule>> resultMap =
        new HashMap<WorkspaceContentDifferenceType, List<InheritanceRule>>();

    // Create helper maps for finding the CREATE/DELETE/MODIFY Entries
    Map<String, InheritanceRule> fromMap = new HashMap<String, InheritanceRule>();
    if (from.getInheritanceRules() != null) {
      for (InheritanceRule ir : from.getInheritanceRules()) {
        fromMap.put(createKey(ir), ir);
      }
    }
    Map<String, InheritanceRule> toMap = new HashMap<String, InheritanceRule>();
    if (to.getInheritanceRules() != null) {
      for (InheritanceRule ir : to.getInheritanceRules()) {
        toMap.put(createKey(ir), ir);
      }
    }

    // Create CREATE List
    if (to.getInheritanceRules() != null) {
      List<InheritanceRule> createList = new ArrayList<InheritanceRule>();
      for (InheritanceRule ir : to.getInheritanceRules()) {
        if (fromMap.get(createKey(ir)) == null) {
          createList.add(ir);
        }
      }
      if (!createList.isEmpty()) {
        resultMap.put(new CREATE(), createList);
      }
    }

    // Create DELETE List
    if (from.getInheritanceRules() != null) {
      List<InheritanceRule> deleteList = new ArrayList<InheritanceRule>();
      for (InheritanceRule ir : from.getInheritanceRules()) {
        if (toMap.get(ir.getParameterType() + ":" + ir.getChildFilter()) == null) {
          deleteList.add(ir);
        }
      }
      if (deleteList.size() > 0) {
        resultMap.put(new DELETE(), deleteList);
      }
    }

    // Create MODIFY List
    if (from.getInheritanceRules() != null) {
      List<InheritanceRule> modifyList = new ArrayList<InheritanceRule>();
      for (InheritanceRule ir : from.getInheritanceRules()) {
        InheritanceRule toIr = toMap.get(ir.getParameterType() + ":" + ir.getChildFilter());
        if (toIr != null) {
          InheritanceRule fromIr = ir;
          if (!compareValue(fromIr.getValue(), toIr.getValue()) || !Objects.equals(fromIr.getPrecedence(), toIr.getPrecedence())) {
            // modifyList has 2 Elements (from,to)
            modifyList.add(fromIr);
            modifyList.add(toIr);
          }
        }
      }
      if (modifyList.size() > 0) {
        resultMap.put(new MODIFY(), modifyList);
      }
    }
    return resultMap;
  }

  private static boolean compareValue(String val1, String val2) {
    if(Objects.equals(val1, val2)) {
      return true;
    }
    
    return (val1 == null && val2.isEmpty()) || (val2 == null && val1.isEmpty());
  }

  private static Map<WorkspaceContentDifferenceType, List<Capacity>> getCapacityDiffTypeMap(OrderType from, OrderType to) {
    Map<WorkspaceContentDifferenceType, List<Capacity>> resultMap = new HashMap<WorkspaceContentDifferenceType, List<Capacity>>();

    // Create helper maps for finding the CREATE/DELETE/MODIFY Entries
    Map<String, Capacity> fromMap = new HashMap<String, Capacity>();
    if (from.getCapacities() != null) {
      for (Capacity cap : from.getCapacities()) {
        fromMap.put(cap.getCapacityName(), cap);
      }
    }
    Map<String, Capacity> toMap = new HashMap<String, Capacity>();
    if (to.getCapacities() != null) {
      for (Capacity cap : to.getCapacities()) {
        toMap.put(cap.getCapacityName(), cap);
      }
    }

    // Create CREATE List
    if (to.getCapacities() != null) {
      List<Capacity> createList = new ArrayList<Capacity>();
      for (Capacity cap : to.getCapacities()) {
        if (fromMap.get(cap.getCapacityName()) == null) {
          createList.add(cap);
        }
      }
      if (createList.size() > 0) {
        resultMap.put(new CREATE(), createList);
      }
    }

    // Create DELETE List
    if (from.getCapacities() != null) {
      List<Capacity> deleteList = new ArrayList<Capacity>();
      for (Capacity cap : from.getCapacities()) {
        if (toMap.get(cap.getCapacityName()) == null) {
          deleteList.add(cap);
        }
      }
      if (deleteList.size() > 0) {
        resultMap.put(new DELETE(), deleteList);
      }
    }

    // Create MODIFY List
    if (from.getCapacities() != null) {
      List<Capacity> modifyList = new ArrayList<Capacity>();
      for (Capacity cap : from.getCapacities()) {
        if (toMap.get(cap.getCapacityName()) != null) {
          Capacity fromCap = cap;
          Capacity toCap = toMap.get(cap.getCapacityName());
          if (fromCap.getCardinality() != toCap.getCardinality()) {
            // modifyList has 2 Elements (from,to)
            modifyList.add(fromCap);
            modifyList.add(toCap);
          }
        }
      }
      if (modifyList.size() > 0) {
        resultMap.put(new MODIFY(), modifyList);
      }
    }
    return resultMap;
  }


  @Override
  public List<OrderType> createItems(Long revision) {
    List<OrderType> otList = new ArrayList<OrderType>();
    try {
      List<OrdertypeParameter> otpList = getOrderTypeManagement().listOrdertypes(revision);
      otpList.removeIf(x -> !x.containsCustomConfig());
      for (OrdertypeParameter otp : otpList) {
        OrderType ot = new OrderType();
        ot.setName(otp.getOrdertypeName());
        ot.setDocumentation(otp.getDocumentation());
        ot.setMonitoringLevel(otp.getMonitoringLevel());

        // DispatcherDestination
        List<DispatcherDestination> ddList = new ArrayList<DispatcherDestination>();
        ot.setDispatcherDestinations(ddList);
        DestinationValueParameter dvp = otp.getPlanningDestinationValue();
        if (dvp != null && otp.isCustomPlanningDestinationValue()) {
          DispatcherDestination dd = new DispatcherDestination();
          dd.setDispatcherName(DISPATCHERNAME_PLANNING);
          dd.setDestinationType(dvp.getDestinationType());
          dd.setDestinationValue(dvp.getFullQualifiedName());
          ddList.add(dd);
        }
        dvp = otp.getExecutionDestinationValue();
        if (dvp != null && otp.isCustomExecutionDestinationValue()) {
          DispatcherDestination dd = new DispatcherDestination();
          dd.setDispatcherName(DISPATCHERNAME_EXECUTION);
          dd.setDestinationType(dvp.getDestinationType());
          dd.setDestinationValue(dvp.getFullQualifiedName());
          ddList.add(dd);
        }
        dvp = otp.getCleanupDestinationValue();
        if (dvp != null && otp.isCustomCleanupDestinationValue()) {
          DispatcherDestination dd = new DispatcherDestination();
          dd.setDispatcherName(DISPATCHERNAME_CLEANUP);
          dd.setDestinationType(dvp.getDestinationType());
          dd.setDestinationValue(dvp.getFullQualifiedName());
          ddList.add(dd);
        }

        // InheritanceRules
        List<InheritanceRule> irList = new ArrayList<InheritanceRule>();
        ot.setInheritanceRules(irList);
        Map<ParameterType, List<com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule>> irMap =
            otp.getParameterInheritanceRules();
        if (irMap != null) {
          for (Map.Entry<ParameterType, List<com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule>> entry : irMap.entrySet()) {
            ParameterType ptEntry = entry.getKey();
            for (com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule irEntry : entry.getValue()) {
              InheritanceRule ir = new InheritanceRule();
              ir.setParameterType(ptEntry.toString());
              ir.setChildFilter(irEntry.getChildFilter());
              ir.setPrecedence(Integer.toString(irEntry.getPrecedence()));
              ir.setValue(irEntry.getUnevaluatedValue());
              irList.add(ir);
            }
          }
          Collections.sort(irList, this::compareInharitanceRules);
        }

        // Capacities
        List<Capacity> capList = new ArrayList<Capacity>();
        ot.setCapacities(capList);
        Set<com.gip.xyna.xprc.xpce.planning.Capacity> capSet = otp.getRequiredCapacities();
        if (capSet != null) {
          for (com.gip.xyna.xprc.xpce.planning.Capacity capEntry : capSet) {
            Capacity cap = new Capacity();
            cap.setCapacityName(capEntry.getCapName());
            cap.setCardinality(capEntry.getCardinality());
            capList.add(cap);
          }
          Collections.sort(capList, (x, y) -> x.getCapacityName().compareTo(y.getCapacityName()));
        }

        // Priority
        if (otp.getPriority() != null) {
          PrioritySetting ps = new PrioritySetting();
          ps.setPriority(otp.getPriority());
          ot.setPrioritySetting(ps);
        }
        otList.add(ot);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Collections.sort(otList, (x, y) -> x.getName().compareTo(y.getName()));
    return otList;
  }


  private int compareInharitanceRules(InheritanceRule rule1, InheritanceRule rule2) {
    int result = rule1.getParameterType().compareTo(rule2.getParameterType());
    if (result != 0) {
      return result;
    }
    result = rule1.getPrecedence().compareTo(rule2.getPrecedence());
    if (result != 0) {
      return result;
    }
    result = rule1.getChildFilter().compareTo(rule2.getChildFilter());
    return result;
  }


  @Override
  public void create(OrderType item, long revision) {
    try {
      OrdertypeParameter orderTypeParameter = createOrdertypeParameter(item, revision);
      getOrderTypeManagement().createOrUpdateOrdertypes(new ArrayList<>(List.of(orderTypeParameter)), true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void modify(OrderType from, OrderType to, long revision) {
    try {
      OrdertypeParameter orderTypeParameter = createOrdertypeParameter(to, revision);
      getOrderTypeManagement().modifyOrdertype(orderTypeParameter);
      updateDestinations(to, revision);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  
  private void updateDestinations(OrderType orderType, Long revision) {
    try {
      Workspace ws = getRevisionManagement().getWorkspace(revision);
      DestinationKey dk = new DestinationKey(orderType.getName(), ws);
      XynaProcessCtrlExecution xpce = getProcessing().getXynaProcessCtrlExecution();

      DispatcherDestination planningDispatcherDest = getDispatcherDestination(orderType.getDispatcherDestinations(), DISPATCHERNAME_PLANNING);
      PlanningDispatcher planningDispatcher = xpce.getXynaPlanning().getPlanningDispatcher();
      String defaultFqName = XynaDispatcher.DESTINATION_DEFAULT_PLANNING.getFQName();
      if (planningDispatcherDest != null && !defaultFqName.equals(planningDispatcherDest.getDestinationValue())) {
        DestinationValue pdv = new FractalWorkflowDestination(planningDispatcherDest.getDestinationValue());
        planningDispatcher.removeDestination(dk);
        planningDispatcher.setCustomDestination(dk, pdv);
      } else {
        planningDispatcher.removeCustomDestination(dk, XynaDispatcher.DESTINATION_DEFAULT_PLANNING);
        planningDispatcher.setDestination(dk, XynaDispatcher.DESTINATION_DEFAULT_PLANNING, false);
      }

      DispatcherDestination executionDispatcherDest = getDispatcherDestination(orderType.getDispatcherDestinations(), DISPATCHERNAME_EXECUTION);
      ExecutionDispatcher executionDispatcher = xpce.getXynaExecution().getExecutionEngineDispatcher();
      defaultFqName = orderType.getName();
      DestinationValue edv = new FractalWorkflowDestination(orderType.getName());
      if (executionDispatcherDest != null && !defaultFqName.equals(executionDispatcherDest.getDestinationValue())) {
        executionDispatcher.removeDestination(dk);
        executionDispatcher.setCustomDestination(dk, edv);
      } else {
        executionDispatcher.removeCustomDestination(dk, edv);
        executionDispatcher.setDestination(dk, edv, false);
      }

      DispatcherDestination cleanupDispatcherDest = getDispatcherDestination(orderType.getDispatcherDestinations(), DISPATCHERNAME_CLEANUP);
      CleanupDispatcher cleanupDispatcher = xpce.getXynaCleanup().getCleanupEngineDispatcher();
      defaultFqName = XynaDispatcher.DESTINATION_EMPTY_WORKFLOW.getFQName();
      if (cleanupDispatcherDest != null && !defaultFqName.equals(cleanupDispatcherDest.getDestinationValue())) {
        DestinationValue cdv = new FractalWorkflowDestination(cleanupDispatcherDest.getDestinationValue());
        cleanupDispatcher.removeDestination(dk);
        cleanupDispatcher.setCustomDestination(dk, cdv);
      } else {
        cleanupDispatcher.removeCustomDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW);
        cleanupDispatcher.setDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW, false);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private DispatcherDestination getDispatcherDestination(List<? extends DispatcherDestination> candidates, String destinationName) {
    if(candidates == null) { return null; }
    for(DispatcherDestination candidate : candidates) {
      if(candidate.getDispatcherName().equals(destinationName)) {
        return candidate;
      }
    }
    return null;
  }

  @Override
  public void delete(OrderType item, long revision) {
    try {
      OrdertypeParameter orderTypeParameter = createOrdertypeParameter(item, revision);
      if (orderTypeParameter.getOrdertypeName().equals(orderTypeParameter.getExecutionDestinationValue().getFullQualifiedName())) {
        // set default order type for workflow
        OrdertypeParameter newPara = new OrdertypeParameter();
        newPara.setOrdertypeName(orderTypeParameter.getOrdertypeName());
        newPara.setRuntimeContext(orderTypeParameter.getRuntimeContext());
        String destinationType = ExecutionType.XYNA_FRACTAL_WORKFLOW.getTypeAsString();
        newPara.setCleanupDestinationValue(new DestinationValueParameter("Empty", destinationType));
        newPara.setExecutionDestinationValue(new DestinationValueParameter(orderTypeParameter.getOrdertypeName(), destinationType));
        newPara.setPlanningDestinationValue(new DestinationValueParameter("DefaultPlanning", destinationType));
        newPara.setParameterInheritanceRules(new HashMap<>(Map.of(ParameterType.MonitoringLevel, new ArrayList<>(),
                                                                  ParameterType.SuspensionBackupMode, new ArrayList<>(),
                                                                  ParameterType.BackupWhenRemoteCall, new ArrayList<>())));
        newPara.setRequiredCapacities(new HashSet<>());
        getOrderTypeManagement().modifyOrdertype(newPara);

        DestinationKey dk = new DestinationKey(newPara.getOrdertypeName(), newPara.getRuntimeContext());
        DestinationValue dv = new FractalWorkflowDestination(orderTypeParameter.getOrdertypeName());
        XynaProcessCtrlExecution xpce = getProcessing().getXynaProcessCtrlExecution();
        xpce.getXynaPlanning().getPlanningDispatcher().removeCustomDestination(dk, XynaDispatcher.DESTINATION_DEFAULT_PLANNING);
        xpce.getXynaPlanning().getPlanningDispatcher().setDestination(dk, XynaDispatcher.DESTINATION_DEFAULT_PLANNING, false);
        xpce.getXynaExecution().getExecutionEngineDispatcher().removeCustomDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW);
        xpce.getXynaExecution().getExecutionEngineDispatcher().setDestination(dk, dv, false);
        xpce.getXynaCleanup().getCleanupEngineDispatcher().removeCustomDestination(dk, dv);
        xpce.getXynaCleanup().getCleanupEngineDispatcher().setDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW, false);
      } else {
        // delete additional custom order type
        getOrderTypeManagement().deleteOrdertype(orderTypeParameter);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private OrdertypeParameter createOrdertypeParameter(OrderType item, long revision) throws Exception {
    Workspace ws = getRevisionManagement().getWorkspace(revision);
    OrdertypeParameter orderTypeParameter = new OrdertypeParameter();
    orderTypeParameter.setRuntimeContext(ws);
    orderTypeParameter.setOrdertypeName(item.getName());
    orderTypeParameter.setDocumentation(item.getDocumentation());

    // DispatcherDestinations
    if ((item.getDispatcherDestinations() != null) && (item.getDispatcherDestinations().size() > 0)) {
      for (DispatcherDestination dd : item.getDispatcherDestinations()) {
        DestinationValueParameter destinationValueParameter = new DestinationValueParameter();
        destinationValueParameter.setDestinationType(dd.getDestinationType());
        destinationValueParameter.setFullQualifiedName(dd.getDestinationValue());
        if (dd.getDispatcherName().equals(DISPATCHERNAME_PLANNING)) {
          orderTypeParameter.setCustomPlanningDestinationValue(destinationValueParameter);
        } else if (dd.getDispatcherName().equals(DISPATCHERNAME_EXECUTION)) {
          orderTypeParameter.setCustomExecutionDestinationValue(destinationValueParameter);
        } else if (dd.getDispatcherName().equals(DISPATCHERNAME_CLEANUP)) {
          orderTypeParameter.setCustomCleanupDestinationValue(destinationValueParameter);
        } else {
          throw new Exception("Unknown Dispatcher Name " + dd.getDispatcherName());
        }
      }
    }

    // Capacities
    Set<com.gip.xyna.xprc.xpce.planning.Capacity> capacitySet = new HashSet<com.gip.xyna.xprc.xpce.planning.Capacity>();
    if ((item.getCapacities() != null) && (item.getCapacities().size() > 0)) {
      for (Capacity cap : item.getCapacities()) {
        com.gip.xyna.xprc.xpce.planning.Capacity capacity = new com.gip.xyna.xprc.xpce.planning.Capacity();
        capacity.setCapName(cap.getCapacityName());
        capacity.setCardinality(cap.getCardinality());
        capacitySet.add(capacity);
      }
    }
    orderTypeParameter.setRequiredCapacities(capacitySet);

    // InheritanceRules
    Map<ParameterType, List<com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule>> ruleMap;
    ruleMap = new HashMap<>(Map.of(ParameterType.MonitoringLevel, new ArrayList<>(), 
                                   ParameterType.SuspensionBackupMode, new ArrayList<>(),
                                   ParameterType.BackupWhenRemoteCall, new ArrayList<>()));
    orderTypeParameter.setParameterInheritanceRules(ruleMap);
    if ((item.getInheritanceRules() != null) && (item.getInheritanceRules().size() > 0)) {
      for (InheritanceRule ir : item.getInheritanceRules()) {
        ParameterType pt = ParameterType.valueOf(ir.getParameterType());
        ruleMap.putIfAbsent(pt, new ArrayList<>());

        int precedence = Integer.parseInt(ir.getPrecedence());
        ruleMap.get(pt).add(pt.createInheritanceRuleBuilder(ir.getValue()).precedence(precedence).childFilter(ir.getChildFilter()).build());

      }
    }
    orderTypeParameter.setParameterInheritanceRules(ruleMap);

    // PrioritySetting
    if (item.getPrioritySetting() != null) {
      orderTypeParameter.setPriority(item.getPrioritySetting().getPriority());
    }

    // MonitoringLevel
    if (item.getMonitoringLevel() != null) {
      orderTypeParameter.setMonitoringLevel(item.getMonitoringLevel());
    }

    return orderTypeParameter;
  }

}
