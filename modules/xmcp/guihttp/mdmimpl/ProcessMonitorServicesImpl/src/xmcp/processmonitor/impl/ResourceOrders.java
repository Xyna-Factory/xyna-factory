/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderWaitingForResourceInfo;
import com.gip.xyna.xprc.XynaProcessingPortal;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.XynaScheduler.ResourceInfo;
import com.gip.xyna.xprc.xsched.XynaScheduler.ResourceType;
import com.gip.xyna.xprc.xsched.capacities.CapacityUsageSlotInformation;

import xmcp.processmonitor.resources.Capacity;
import xmcp.processmonitor.resources.Order;
import xmcp.processmonitor.resources.Resource;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;



public class ResourceOrders {

  private static final String COLUMN_PATH_STATUS = "status";
  private static final String COLUMN_PATH_ID = "id";
  private static final String COLUMN_PATH_TYPE = "type";
  private static final String COLUMN_PATH_USAGE = "usage";

  private static XynaProcessingPortal processingPortal = XynaFactory.getPortalInstance().getProcessingPortal();
  private static XynaScheduler xynaScheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
  private static XynaMultiChannelPortal multiChannelPortal = ((XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal());

  enum OrderStatus { STATUS_RUNNING, STATUS_WAITING }

  private static final Logger logger = CentralFactoryLogging.getLogger(ResourceOrders.class);


  private ResourceOrders() {}


  public static List<Order> getOrders(TableInfo searchRequest, Resource resource) {
    List<Order> orders;
    ResourceType type;

    if (resource instanceof Capacity) {
      Map<Long, Order> orderIdToOrder = new HashMap<>();
      ExtendedCapacityUsageInformation capacityUsageInfos = processingPortal.listExtendedCapacityInformation();
      type = ResourceType.CAPACITY;

      for (Entry<Integer, HashSet<CapacityUsageSlotInformation>> capacityUsageInfo : capacityUsageInfos.getSlotInformation().entrySet()) {
        for (CapacityUsageSlotInformation slotInfo : capacityUsageInfo.getValue()) {
          if (!slotInfo.isOccupied() || slotInfo.getUsingOrderId() == null || !Objects.equals(resource.getName(), slotInfo.getCapacityName())) {
            continue;
          }

          Order order = orderIdToOrder.get(slotInfo.getUsingOrderId());
          if (order == null) {
            order = new Order(OrderStatus.STATUS_RUNNING.name(), Long.toString(slotInfo.getUsingOrderId()), slotInfo.getUsingOrderType(), 0);
            orderIdToOrder.put(slotInfo.getUsingOrderId(), order);
          }

          order.setUsage(order.getUsage() + 1);
        }
      }

      orders = new ArrayList<>(orderIdToOrder.values());
    } else {
      type = ResourceType.VETO;
      orders = new ArrayList<>();

      try {
        Collection<VetoInformationStorable> vetoInfos = multiChannelPortal.listVetoInformation();
        for (VetoInformationStorable vetoInfo : vetoInfos) {
          if (!Objects.equals(vetoInfo.getVetoName(), resource.getName())) {
            continue;
          }

          orders = new ArrayList<>(Arrays.asList(new Order(OrderStatus.STATUS_RUNNING.name(), Long.toString(vetoInfo.getUsingOrderId()), vetoInfo.getUsingOrdertype(), null)));
        }
      } catch (PersistenceLayerException e) {
        logger.error("Could not determine used Vetoes", e);
        throw new RuntimeException(e);
      }
    } 

    // add waiting orders
    Map<ResourceInfo, Set<XynaOrderWaitingForResourceInfo>> waitingOrders = xynaScheduler.getOrdersWaitingForResources();
    Set<XynaOrderWaitingForResourceInfo> waitingInfos = waitingOrders.get(new ResourceInfo(resource.getName(), type));
    if (waitingInfos != null) {
      for (XynaOrderWaitingForResourceInfo waitingInfo : waitingInfos) {
        orders.add(new Order(OrderStatus.STATUS_WAITING.name(), Long.toString(waitingInfo.getOrderId()), waitingInfo.getDestinationKey().getOrderType(), null));
      }
    }

    return sortAndFilter(orders, searchRequest);
  }

  private static List<Order> sortAndFilter(List<Order> orders, TableInfo searchRequest) {
    TableHelper<Order, TableInfo> tableHelper = TableHelper.<Order, TableInfo>init(searchRequest)
       .limitConfig(TableInfo::getLimit)
       .sortConfig(ti -> {
         for (TableColumn tc : ti.getColumns()) {
           TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
           if(sort != null)
             return sort;
         }
         return null;
       })
       .filterConfig(ti -> 
         ti.getColumns().stream()
         .filter(tableColumn -> 
           !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
         )
         .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
         .collect(Collectors.toList())
       )
       .addSelectFunction(COLUMN_PATH_STATUS, Order::getStatus)
       .addSelectFunction(COLUMN_PATH_ID, Order::getId)
       .addSelectFunction(COLUMN_PATH_TYPE, Order::getType)
       .addSelectFunction(COLUMN_PATH_USAGE, Order::getUsage);

    orders = orders.stream().filter(tableHelper.filter()).collect(Collectors.toList());
    tableHelper.sort(orders);

    return tableHelper.limit(orders);
  }

}
