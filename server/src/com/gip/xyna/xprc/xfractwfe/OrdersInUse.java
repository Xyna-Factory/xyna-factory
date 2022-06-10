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
package com.gip.xyna.xprc.xfractwfe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcess;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;


public class OrdersInUse implements Serializable{

  private static final long serialVersionUID = 1L;
  
  private static Logger logger = CentralFactoryLogging.getLogger(OrdersInUse.class);

  public enum FillingMode {
    OnlyIds, //nur die orderIds werden gesetzt
    EasyInfos, //alle Informationen die leicht bestimmt werden können, werden gesetzt
    Complete //es werden alle Informationen bestimmt (z.B. orderType aus XynaOrder im OrderBackup)
  }
  
  private FillingMode fillingMode; //gibt an wie viele Information in den Maps enthalten sind
  private final Map<Long, OrderInfo> rootOrders = new HashMap<Long, OrderInfo>(); //laufende Root-Aufträge (ohne Batch Prozess Master)
  private final Map<Long, OrderInfo> crons = new HashMap<Long, OrderInfo>();
  private final Map<Long, OrderInfo> batchProcesses = new HashMap<Long, OrderInfo>();
  private final Map<Long, OrderInfo> frequencyControlledTasks = new HashMap<Long, OrderInfo>();
  
  
  public static class OrderInfo implements Serializable{
    
    private static final long serialVersionUID = 1L;
    
    Long orderId;
    String orderType;
    String status;
    int binding;
    
    public OrderInfo(Long orderId, String orderType, String status, int binding) {
      this.orderId = orderId;
      this.orderType = orderType;
      this.status = status;
      this.binding = binding;
    }
    
    public void addOrderType(String orderType) {
      this.orderType += ", " + orderType;
    }
    
    /**
     * Groupiert die OrderInfos nach dem OrderType
     * @param orderInfos
     * @return
     */
    public static Map<String, ArrayList<OrderInfo>> groupByOrderType(Collection<OrderInfo> orderInfos) {
      Transformation<OrderInfo, String> transformation = new Transformation<OrderInfo, String>() {
        public String transform(OrderInfo from) {
          if( from == null ) {
            return null;
          }
          return from.orderType;
        }
      };
      
      return CollectionUtils.group(orderInfos, transformation);
    }
    
    /**
     * Groupiert die OrderInfos nach dem Status
     * @param orderInfos
     * @return
     */
    public static Map<String, ArrayList<OrderInfo>> groupByStatus(List<OrderInfo> orderInfos) {
      Transformation<OrderInfo, String> transformation = new Transformation<OrderInfo, String>() {
        public String transform(OrderInfo from) {
          if( from == null ) {
            return null;
          }
          return from.status;
        }
      };
      
      return CollectionUtils.group(orderInfos, transformation);
    }
    
    /**
     * Groupiert die OrderInfos nach dem Binding
     * @param orderInfos
     * @return
     */
    public static Map<Integer, ArrayList<OrderInfo>> groupByBinding(List<OrderInfo> orderInfos) {
      Transformation<OrderInfo, Integer> transformation = new Transformation<OrderInfo, Integer>() {
        public Integer transform(OrderInfo from) {
          if( from == null ) {
            return null;
          }
          return from.binding;
        }
      };
      
      return CollectionUtils.group(orderInfos, transformation);
    }

    /**
     * Liefert alle orderIds der orderInfos
     * @param orderInfos
     * @return
     */
    public static List<Long> getOrderIds(List<OrderInfo> orderInfos) {
      Transformation<OrderInfo, Long> transformation = new Transformation<OrderInfo, Long>() {
        public Long transform(OrderInfo from) {
          if( from == null ) {
            return null;
          }
          return from.orderId;
        }
      };
      
      return CollectionUtils.transform(orderInfos, transformation);
    }
  }
  

  public OrdersInUse(FillingMode fillingMode) {
    this.fillingMode = fillingMode;
  }

  /**
   * Liefert die Ids der laufenden RootOrders und Batch Prozesse
   * @return
   */
  public Set<Long> getRootOrdersAndBatchProcesses() {
    Set<Long> orders =  new HashSet<Long>();
    orders.addAll(rootOrders.keySet());
    orders.addAll(batchProcesses.keySet());
    return orders;
  }

  
  public Collection<OrderInfo> getRunningRootOrders() {
    return rootOrders.values();
  }

  public Collection<OrderInfo> getBatchProcesses() {
    return batchProcesses.values();
  }

  public Set<Long> getCronIds() {
    return crons.keySet();
  }
  
  public Collection<OrderInfo> getCrons() {
    return crons.values();
  }
  
  public Set<Long> getFrequencyControlledTaskIds() {
    return frequencyControlledTasks.keySet();
  }

  public Collection<OrderInfo> getFrequencyControlledTasks() {
    return frequencyControlledTasks.values();
  }

  public void addOrders(OrdersInUse ordersInUse) {
    rootOrders.putAll(ordersInUse.rootOrders);
    crons.putAll(ordersInUse.crons);
    batchProcesses.putAll(ordersInUse.batchProcesses);
    frequencyControlledTasks.putAll(ordersInUse.frequencyControlledTasks);
  }
  

  /**
   * Trägt eine neue XynaOrder ein, falls es ein Root-Auftrag ist und kein Batch Prozess Master
   * @param oi
   * @param binding
   */
  public void addRootOrder(XynaOrderInfo oi, int binding) {
    if (!batchProcesses.containsKey(oi.getOrderId())) { //es ist kein Batch Prozess Master
      if (oi.getOrderId().equals(oi.getRootOrderId())) { //es ist ein Root-Auftrag
        OrderInfo order = null;
        if (fillingMode != FillingMode.OnlyIds) { //es sollen nicht nur die Ids bestimmt werden
          order = new OrderInfo(oi.getOrderId(), oi.getDestinationKey().getOrderType(), oi.getStatus().toString(), binding);
        }
        
        rootOrders.put(oi.getOrderId(), order);
      }
    }
  }

  /**
   * Trägt eine neue XynaOrder ein, falls noch nicht vorhanden und
   * es ein Root-Auftrag ist und kein Batch Prozess Master.
   * @param oib
   */
  public void addRootOrder(OrderInstanceBackup oib) {
    if (!rootOrders.containsKey(oib.getRootId())
                    && !batchProcesses.containsKey(oib.getRootId())) {
      if (oib.getId() == oib.getRootId()) {
        OrderInfo order = null;
        if (fillingMode != FillingMode.OnlyIds) {
          //Status aus dem BackupCause bestimmen
          OrderInstanceStatus status = getStatusFromBackupCause(oib.getBackupCauseAsEnum());
          //OrderType versuchen zu bestimmen
          String orderType = getOrderType(oib);

          order = new OrderInfo(oib.getRootId(), orderType, status.toString(), oib.getLocalBinding(ODSConnectionType.DEFAULT));
        }
        
        rootOrders.put( oib.getRootId(), order);
      }
    }
  }

  /**
   * Trägt einen Batch Prozess ein
   * @param batchProcess
   * @param binding
   */
  public void addBatchProcess(BatchProcess batchProcess, int binding) {
    Long orderId = batchProcess.getBatchProcessId();
    OrderInfo order = null;
    if (fillingMode != FillingMode.OnlyIds) {
      BatchProcessStatus status = batchProcess.getBatchProcessStatus();
      order = new OrderInfo(orderId, batchProcess.getMasterOrder().getDestinationKey().getOrderType(), status.toString(), binding);
    }
    
    batchProcesses.put(orderId, order);
  }
  
  /**
   * Trägt eine Cron Like Order ein
   * @param clo
   */
  public void addCron(CronLikeOrder clo) {
    OrderInfo order = null;
    if (fillingMode != FillingMode.OnlyIds) {
      String status = clo.isEnabled() ? "Active" : "Disabled";
      order = new OrderInfo(clo.getId(), clo.getOrdertype(), status, clo.getLocalBinding(ODSConnectionType.DEFAULT));
    }
    
    crons.put (clo.getId(), order);
  }
  
  /**
   * Trägt einen FrequencyControlledTask ein
   * @param id
   * @param orderType
   * @param binding
   */
  public void addFrequencyControlledTask(Long id, String orderType, int binding) {
    OrderInfo order = null;
    if (fillingMode != FillingMode.OnlyIds) {
      if (frequencyControlledTasks.containsKey(id)) {
        // weiteren OrderType hinzufügen
        order = frequencyControlledTasks.get(id);
        order.addOrderType(orderType);
      } else {
        // neuen FrequencyControlledTask eintragen
        order = new OrderInfo(id, orderType, null, binding);
      }
    }
    
    frequencyControlledTasks.put(id, order);
  }
  
  
  /**
   * Bestimmt den OrderInstanceStatus anhand des BackupCauses
   * @param backupCause
   * @return
   */
  private OrderInstanceStatus getStatusFromBackupCause(BackupCause backupCause) {
    switch (backupCause) {
      case SUSPENSION:
      case AFTER_SUSPENSION:
      case AFTER_SCHEDULING:
      case FINISHED_SUBWF:
        return OrderInstanceStatus.RUNNING_EXECUTION;
      case ACKNOWLEDGED:
        return OrderInstanceStatus.SCHEDULING;
      case WAITING_FOR_CAPACITY:
        return OrderInstanceStatus.SCHEDULING_CAPACITY;
      default:
        return OrderInstanceStatus.RUNNING;
    }
  }

  /**
   * Bestimmt den OrderType aus dem OrderArchive. Falls hier nicht vorhanden und fillingMode == Complete
   * wird er aus dem OrderBackup geholt.
   * @param oib
   * @return
   */
  private String getOrderType(OrderInstanceBackup oib) {
    String orderType = "orders in backup";
    //OrderType aus OrderArchive auslesen
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ODSConnection conDefault = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      OrderInstanceDetails oid = new OrderInstanceDetails(oib.getRootId());
      conDefault.queryOneRow(oid);
      orderType = oid.getOrderType();
    } catch (XynaException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("could not find order " + oib.getRootId() + " in OrderArchive", e);
      }
      if (fillingMode == FillingMode.Complete) {
        //OrderType aus der XynaOrder im OrderBackup bestimmen
        try {
          OrderInstanceBackup rootOib = new OrderInstanceBackup(oib.getRootId(), oib.getBinding());
          conDefault.queryOneRow(rootOib);
          if (rootOib.getXynaorder() != null) {
            orderType = rootOib.getXynaorder().getDestinationKey().getOrderType();
          }
        } catch (XynaException e2) {
          if (logger.isDebugEnabled()) {
            logger.debug("could not find order " + oib.getRootId() + " in OrderBackup", e2);
          }
        }
      }
    } finally {
      try {
        if (conDefault != null) {
          conDefault.closeConnection();
        }
      } catch (PersistenceLayerException e) {
        logger.warn("could not close connection");
      }
    }
    
    return orderType;
  }
}
