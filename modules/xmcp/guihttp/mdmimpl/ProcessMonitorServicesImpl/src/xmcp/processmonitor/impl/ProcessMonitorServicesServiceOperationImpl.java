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
package xmcp.processmonitor.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderWaitingForResourceInfo;
import com.gip.xyna.xprc.XynaProcessingPortal;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.XynaScheduler.ResourceInfo;
import com.gip.xyna.xprc.xsched.XynaScheduler.ResourceType;
import com.gip.xyna.xprc.xsched.capacities.CapacityUsageSlotInformation;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;

import xfmg.xopctrl.UserAuthenticationRight;
import xmcp.factorymanager.shared.InsufficientRights;
import xmcp.graphs.datatypes.GraphData;
import xmcp.graphs.datatypes.GraphInfo;
import xmcp.processmonitor.ProcessMonitorServicesServiceOperation;
import xmcp.processmonitor.datatypes.CancelFrequencyControlledTaskException;
import xmcp.processmonitor.datatypes.FrequencyControlledTaskDetails;
import xmcp.processmonitor.datatypes.GraphDatasource;
import xmcp.processmonitor.datatypes.LoadFrequencyControlledTasksException;
import xmcp.processmonitor.datatypes.LoadGraphDataException;
import xmcp.processmonitor.datatypes.LoadManualInteractionException;
import xmcp.processmonitor.datatypes.ManualInteractionEntry;
import xmcp.processmonitor.datatypes.ManualInteractionId;
import xmcp.processmonitor.datatypes.ManualInteractionProcessResponse;
import xmcp.processmonitor.datatypes.ManualInteractionResponse;
import xmcp.processmonitor.datatypes.NoFrequencyControlledTaskDetails;
import xmcp.processmonitor.datatypes.OrderOverviewEntry;
import xmcp.processmonitor.datatypes.SearchFlag;
import xmcp.processmonitor.datatypes.TaskId;
import xmcp.processmonitor.datatypes.filter.IncludeUnused;
import xmcp.processmonitor.datatypes.filter.ShowOnlyRootOrders;
import xmcp.processmonitor.resources.Capacity;
import xmcp.processmonitor.resources.Filter;
import xmcp.processmonitor.resources.Order;
import xmcp.processmonitor.resources.Resource;
import xmcp.processmonitor.resources.Veto;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.tables.datatypes.transformation.ColumnPath;
import xmcp.xact.modeller.KillOrdersResponse;
import xmcp.zeta.TableHelper;
import xprc.xpce.CustomFields;
import xprc.xpce.OrderId;


public class ProcessMonitorServicesServiceOperationImpl implements ExtendedDeploymentTask, ProcessMonitorServicesServiceOperation {

  private static final String CUSTOM_FIELD_PROPERTY_PREFIX  = "xyna.processmonitor.customColumn";
  private static final String CUSTOM_FIELD_PROPERTY_ENABLED = "enabled";
  private static final String CUSTOM_FIELD_PROPERTY_LABEL   = "label";

  private static final String NAME_KEY = "name";
  private static final String WAITING_ORDERS_COUNT = "waitingOrdersCount";
  private static final String ASCENDING_ORDER = "asc";
  private static final String DESCENDING_ORDER = "dsc";

  private static final String ZETA_TABLE_LIMIT_KEY = "zeta.table.limit";

  private enum SortCriterion { ORDER_ID, NAME, WAITING_ORDERS_COUNT };

  public static final XynaPropertyBoolean CUSTOM_COLUMN0_ENABLED = new XynaPropertyBoolean(CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_ENABLED, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die 1. Custom Column in der Auftragsübersicht mit " + CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_LABEL + " als Label angezeigt wird.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the 1st Custom Column is shown in Order Overview, using the label defined by " + CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_LABEL + ".");
  public static final XynaPropertyString CUSTOM_COLUMN0_LABEL = new XynaPropertyString(CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_LABEL, "")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Das Label, das für die 1. Custom Column in der Order Overview angezeigt werden soll, wenn " + CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_ENABLED + " auf true gesetzt ist.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The label to show for 1st Custom Column in Order Overview in case " + CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_ENABLED + " is set to true.");

  public static final XynaPropertyBoolean CUSTOM_COLUMN1_ENABLED = new XynaPropertyBoolean(CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_ENABLED, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die 2. Custom Column in der Auftragsübersicht mit " + CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_LABEL + " als Label angezeigt wird.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the 2nd Custom Column is shown in Order Overview, using the label defined by " + CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_LABEL + ".");
  public static final XynaPropertyString CUSTOM_COLUMN1_LABEL = new XynaPropertyString(CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_LABEL, "")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Das Label, das für die 2. Custom Column in der Order Overview angezeigt werden soll, wenn " + CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_ENABLED + " auf true gesetzt ist.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The label to show for 2nd Custom Column in Order Overview in case " + CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_ENABLED + " is set to true.");

  public static final XynaPropertyBoolean CUSTOM_COLUMN2_ENABLED = new XynaPropertyBoolean(CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_ENABLED, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die 3. Custom Column in der Auftragsübersicht mit " + CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_LABEL + " als Label angezeigt wird.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the 3rd Custom Column is shown in Order Overview, using the label defined by " + CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_LABEL + ".");
  public static final XynaPropertyString CUSTOM_COLUMN2_LABEL = new XynaPropertyString(CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_LABEL, "")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Das Label, das für die 3. Custom Column in der Order Overview angezeigt werden soll, wenn " + CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_ENABLED + " auf true gesetzt ist.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The label to show for 3rd Custom Column in Order Overview in case " + CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_ENABLED + " is set to true.");

  public static final XynaPropertyBoolean CUSTOM_COLUMN3_ENABLED = new XynaPropertyBoolean(CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_ENABLED, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die 4. Custom Column in der Auftragsübersicht mit " + CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_LABEL + " als Label angezeigt wird.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the 4th Custom Column is shown in Order Overview, using the label defined by " + CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_LABEL + ".");
  public static final XynaPropertyString CUSTOM_COLUMN3_LABEL = new XynaPropertyString(CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_LABEL, "")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Das Label, das für die 4. Custom Column in der Order Overview angezeigt werden soll, wenn " + CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_ENABLED + " auf true gesetzt ist.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The label to show for 4th Custom Column in Order Overview in case " + CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_ENABLED + " is set to true.");

  public static final XynaPropertyInt ORDEROVERVIEW_LIMIT = new XynaPropertyInt("xyna.processmonitor.orderoverview.limit", 100).
      setDefaultDocumentation(DocumentationLanguage.DE, "Die maximale Anzahl an Einträgen, die für die Order Overview zurück gegeben wird.").
      setDefaultDocumentation(DocumentationLanguage.EN, "The maximum number of table entries to be returned for the Order Overview.");

  private static XynaMultiChannelPortal multiChannelPortal = ((XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal());
  private static XynaProcessingPortal processingPortal = XynaFactory.getPortalInstance().getProcessingPortal();
  private static Configuration configuration = com.gip.xyna.XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
  private static XynaScheduler xynaScheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
  private static final Logger logger = CentralFactoryLogging.getLogger(ProcessMonitorServicesServiceOperationImpl.class);


  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }
  
  @Override
  public List<? extends ManualInteractionProcessResponse> processMI(List<? extends ManualInteractionId> manualInteractionIds, ManualInteractionResponse manualInteractionResponse) {
    return ManualInteractions.processMI(manualInteractionIds, manualInteractionResponse);
  }
  
  @Override
  public void cancelFrequencyControlledTask(TaskId taskId) throws CancelFrequencyControlledTaskException {
    FrequencyControlledTasks.cancelFrequencyControlledTask(taskId);
  }
  
  @Override
  public List<? extends ManualInteractionEntry> getMIEntries(TableInfo tableInfo) throws LoadManualInteractionException {
    return ManualInteractions.getMIEntries(tableInfo);
  }
  
  @Override
  public List<? extends GraphData> getFrequencyControlledTaskGraphData(GraphInfo graphInfo, GraphDatasource graphDatasource, TaskId taskId) throws LoadGraphDataException {
    return FrequencyControlledTaskGraphData.getFrequencyControlledTaskGraphData(graphInfo, graphDatasource, taskId);   
  }
  
  @Override
  public List<? extends FrequencyControlledTaskDetails> getFrequencyControlledTasks(TableInfo tableInfo)
      throws LoadFrequencyControlledTasksException {
    return FrequencyControlledTasks.getFrequencyControlledTasks(tableInfo);
  }
  
  @Override
  public FrequencyControlledTaskDetails getFrequencyControlledTaskDetails(TaskId taskId) throws NoFrequencyControlledTaskDetails {
    return FrequencyControlledTasks.getFrequencyControlledTaskDetails(taskId);
  }

  public enum OrderInstanceGuiStatus {
    FINISHED("Finished", 0),
    RUNNING("Running", 1),
    FAILED("Failed", 2),
    UNKNOWN("Unknown", 3);
    
    private String name;
    private int severity;
    
    private OrderInstanceGuiStatus(String name, int severity) {
      this.name = name;
      this.severity = severity;
    }
    
    String getName() {
      return name;
    }
    
    int getSeverity() {
      return severity;
    }
    
    public static OrderInstanceGuiStatus createOrderInstanceGuiStatus(String name) {
      if (name.equals(OrderInstanceGuiStatus.FINISHED.getName())) {
        return OrderInstanceGuiStatus.FINISHED;
      } if (name.equals(OrderInstanceGuiStatus.RUNNING.getName())) {
        return OrderInstanceGuiStatus.RUNNING;
      } if (name.equals(OrderInstanceGuiStatus.FAILED.getName())) {
        return OrderInstanceGuiStatus.FAILED;
      } else {
        return OrderInstanceGuiStatus.UNKNOWN;
      }
    }
  }
  
  private boolean isCustomFieldEnabled(int fieldIndex) {
    XynaPropertyWithDefaultValue enabledProperty = configuration.getPropertyWithDefaultValue(CUSTOM_FIELD_PROPERTY_PREFIX + fieldIndex + "." + CUSTOM_FIELD_PROPERTY_ENABLED);
    if (enabledProperty == null || enabledProperty.getValueOrDefValue() == null) {
      return false;
    } else {
      return Boolean.TRUE.toString().toLowerCase().equals(enabledProperty.getValueOrDefValue().toLowerCase());
    }
  }
  
  private String getCustomFieldLabel(int fieldIndex) {
    if (isCustomFieldEnabled(fieldIndex)) {
      XynaPropertyWithDefaultValue labelProperty = configuration.getPropertyWithDefaultValue(CUSTOM_FIELD_PROPERTY_PREFIX + fieldIndex + "." + CUSTOM_FIELD_PROPERTY_LABEL);
      return (labelProperty != null) ? labelProperty.getValueOrDefValue() : null;
    } else {
      return null;
    }
  }
 
  @Override
  public List<? extends OrderOverviewEntry> getOrderOverviewEntries(XynaOrderServerExtension correlatedXynaOrder, TableInfo searchCriteria, List<? extends SearchFlag> searchFlags) {
    return OrderOverview.getOrderOverviewEntries(multiChannelPortal, correlatedXynaOrder, searchCriteria, searchFlags);
  }
  
  @Override
  public CustomFields getCustomFieldLabels() {
    return new CustomFields(getCustomFieldLabel(0), getCustomFieldLabel(1), getCustomFieldLabel(2), getCustomFieldLabel(3));
  }

  @Override
  public TableInfo removeRootIdIfShowOnlyRootOrders(TableInfo tableInfo, List<? extends SearchFlag> searchFlags) {
    boolean removeRootId = false;
    for (SearchFlag searchFlag : searchFlags) {
      if (searchFlag instanceof ShowOnlyRootOrders) {
        removeRootId = true;
        break;
      }
    }

    if (removeRootId) {
      // remove column with root id since only root orders are to be shown
      List<TableColumn> filteredColumns = tableInfo.getColumns().stream().filter(x -> !x.getPath().equals("rootId")).collect(Collectors.toList());
      tableInfo.setColumns(filteredColumns);
    } else if (!tableInfo.getColumns().stream().anyMatch(x -> x.getPath().equals("rootId"))) {
      // re-add column with root id as second column

      List<TableColumn> newColumns = new ArrayList<>();
      for (TableColumn column : tableInfo.getColumns()) {
        newColumns.add(column);
      }

      TableColumn rootIdColumn = new TableColumn();
      rootIdColumn.setName("Root Id");
      rootIdColumn.setPath("rootId");
      newColumns.add(1, rootIdColumn);

      tableInfo.setColumns(newColumns);
    }

    return tableInfo;
  }

  @Override
  public List<? extends Capacity> getCapacities(Filter filter, List<? extends SearchFlag> searchFlags) {
    boolean includeUnused = false;
    for (SearchFlag searchFlag : searchFlags) {
      if (searchFlag instanceof IncludeUnused) {
        includeUnused = true;
      }
    }

    Map<String, Capacity> nameToCapacity = new HashMap<>();
    ExtendedCapacityUsageInformation capacityUsageInfos = processingPortal.listExtendedCapacityInformation();
    Map<ResourceInfo, Set<XynaOrderWaitingForResourceInfo>> waitingOrders = xynaScheduler.getOrdersWaitingForResources();

    for (Entry<Integer, HashSet<CapacityUsageSlotInformation>> capacityUsageInfo : capacityUsageInfos.getSlotInformation().entrySet()) {
      for (CapacityUsageSlotInformation slotInfo : capacityUsageInfo.getValue()) {
        if (!includeUnused && !slotInfo.isOccupied()) {
          continue;
        }

        Capacity capacity = nameToCapacity.get(slotInfo.getCapacityName());
        if (capacity == null) {
          CapacityInformation capacityInfo = processingPortal.getCapacityInformation(slotInfo.getCapacityName());
          Set<XynaOrderWaitingForResourceInfo> waitingInfos = waitingOrders.get(new ResourceInfo(slotInfo.getCapacityName(), ResourceType.CAPACITY));
          int waitingCount = waitingInfos == null ? 0 : waitingInfos.size();
          capacity = new Capacity(slotInfo.getCapacityName(), new ArrayList<Long>(), waitingCount, 0, capacityInfo.getCardinality(), capacityInfo.getState() == State.ACTIVE);
          nameToCapacity.put(slotInfo.getCapacityName(), capacity);
        }

        if (slotInfo.isOccupied()) {
          capacity.setUsage(capacity.getUsage() + 1);
          addToOrders(capacity.getRunningOrders(), slotInfo.getUsingOrderId());
        }
      }
    }

    List<Capacity> capacities = new ArrayList<>();
    for (Entry<String, Capacity> capacityEntry : nameToCapacity.entrySet()) {
      Capacity capacity = capacityEntry.getValue();
      Collections.sort(capacity.getRunningOrders());
      capacities.add(capacity);
    }

    // filter, limit and sort result
    TableHelper<Capacity, Filter> tableHelper = TableHelper.<Capacity, Filter>init(filter).addSelectFunction(NAME_KEY, Capacity::getName);
    setupResourceTableHelper(tableHelper, SortCriterion.NAME);
    List<Capacity> result = capacities.stream().filter(tableHelper.filter()).collect(Collectors.toList());
    tableHelper.sort(result);

    return tableHelper.limit(result);
  }

  private boolean addToOrders(List<Long> orderIds, Long newOrderId) {
    if (newOrderId == null) {
      return false;
    }

    for (long orderId : orderIds) {
      if (orderId == newOrderId.longValue()) {
        return false;
      }
    }

    orderIds.add(newOrderId);

    return true;
  }

  private void setupResourceTableHelper(TableHelper<?, Filter> tableHelper, SortCriterion sortCriterion) {
    int tableLimit = Integer.parseInt(configuration.getPropertyWithDefaultValue(ZETA_TABLE_LIMIT_KEY).getValueOrDefValue());
    tableHelper.limitConfig(fi -> tableLimit)
        .filterConfig(fi -> fi != null && fi.getFilter() != null && !fi.getFilter().isEmpty() ?
                            Collections.unmodifiableList(Arrays.asList(new TableHelper.Filter(NAME_KEY, fi.getFilter()))) :
                            Collections.emptyList());

    switch (sortCriterion) {
      case WAITING_ORDERS_COUNT:
        tableHelper.sortConfig(fi -> TableHelper.createSortIfValid(WAITING_ORDERS_COUNT, DESCENDING_ORDER));
        break;
      default :
        tableHelper.sortConfig(fi -> TableHelper.createSortIfValid(NAME_KEY, ASCENDING_ORDER));
        break;
    }
  }

  @Override
  public List<? extends Veto> getVetoes(Filter filter, xmcp.processmonitor.resources.SortCriterion sortCriterion) {
    Map<ResourceInfo, Set<XynaOrderWaitingForResourceInfo>> waitingOrders = xynaScheduler.getOrdersWaitingForResources();
    TableHelper<Veto, Filter> tableHelper = TableHelper.<Veto, Filter>init(filter)
        .addSelectFunction(NAME_KEY, Veto::getName)
        .addSelectFunction(WAITING_ORDERS_COUNT, Veto::getWaitingOrdersCount);

    SortCriterion sortCriterionEnum;
    try {
      sortCriterionEnum = SortCriterion.valueOf(sortCriterion.getField());
    } catch (Exception e) {
      sortCriterionEnum = SortCriterion.NAME;
    }
    setupResourceTableHelper(tableHelper, sortCriterionEnum);

    try {
      Collection<VetoInformationStorable> vetoInformations = multiChannelPortal.listVetoInformation();

      // filter, limit and sort result

      List<Veto> result = vetoInformations.stream()
          .map(storable -> new Veto(storable.getVetoName(), Collections.unmodifiableList(Arrays.asList(storable.getUsingOrderId())), 
                                    waitingOrders.get(new ResourceInfo(storable.getVetoName(), ResourceType.VETO)) == null ? 0 :
                                    waitingOrders.get(new ResourceInfo(storable.getVetoName(), ResourceType.VETO)).size()))
          .filter(tableHelper.filter())
          .collect(Collectors.toList());
      tableHelper.sort(result);

      result = tableHelper.limit(result);

      if (sortCriterionEnum == SortCriterion.ORDER_ID) {
        Collections.sort(result, new Comparator<Veto>() {
          @Override
          public int compare(Veto o1, Veto o2) {
            if (o1.getRunningOrders() == null || o1.getRunningOrders().isEmpty() || o2.getRunningOrders() == null || o2.getRunningOrders().isEmpty()) {
              return 0;
            }

            if (o1.getRunningOrders().get(0) < o2.getRunningOrders().get(0)) {
              return 1;
            } else if (o1.getRunningOrders().get(0) > o2.getRunningOrders().get(0)) {
              return -1;
            } else {
              return 0;
            }
          }
        });
      }

      return result;
    } catch (XynaException e) {
      logger.error("Could not determine used Vetoes", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<? extends Order> getOrders(TableInfo searchRequest, Resource resource) {
    return ResourceOrders.getOrders(searchRequest, resource);
  }

  @Override
  public KillOrdersResponse killOrders(XynaOrderServerExtension correlatedXynaOrder, List<? extends OrderId> orderIds) throws InsufficientRights {
    String rightName = Rights.KILL_STUCK_PROCESS.name();
    if (!hasRight(correlatedXynaOrder, rightName)) {
      throw new InsufficientRights(Arrays.asList(new UserAuthenticationRight(rightName)));
    }

    KillOrdersResponse response = new KillOrdersResponse();
    response.setResultMessages(new ArrayList<>());
    if (orderIds == null) {
      return response;
    }

    for (OrderId orderId : orderIds) {
      if (orderId == null || orderId.getOrderId() == null) {
        continue;
      }

      try {
        KillStuckProcessBean input = new KillStuckProcessBean(orderId.getOrderId(), false, AbortionCause.MANUALLY_ISSUED, false);
        response.addToResultMessages(processingPortal.killStuckProcess(input).getResultMessage());
      } catch (Exception e) {
        logger.warn("Could not kill process " + orderId.getOrderId(), e);
        response.addToResultMessages(e.getMessage());
      }
    }

    return response;
  }

  private boolean hasRight(XynaOrderServerExtension correlatedXynaOrder, String right) {
    try {
      Role role = correlatedXynaOrder.getCreationRole();
      return XynaFactory.getInstance().getFactoryManagementPortal().hasRight(right, role);
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public List<? extends xmcp.processmonitor.resources.SortCriterion> getVetoSortCriteria() {
    List<xmcp.processmonitor.resources.SortCriterion> result = new ArrayList<>();
    for (SortCriterion sortCriterion : SortCriterion.values()) {
      result.add(new xmcp.processmonitor.resources.SortCriterion(sortCriterion.name()));
    }

    return result;
  }

  @Override
  public List<? extends TableColumn> mapColumnPath(List<? extends TableColumn> tableColumns, ColumnPath from, ColumnPath to) {
    for (var column : tableColumns) {
      if (column.getPath().equals(from.getValue())) {
        column.setPath(to.getValue());
      }
    }

    return tableColumns;
  }

}
