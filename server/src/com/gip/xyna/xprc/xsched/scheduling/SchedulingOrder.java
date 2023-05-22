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
package com.gip.xyna.xprc.xsched.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.DedicatedConnection;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderWaitingForResourceInfo;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessMarker;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.SchedulingData;


/**
 * Wrapper f�r eine XynaOrderServerExtension.
 * H�lt SchedulingData getrennt von der XynaOrder, wenn diese wegen OOM-Schutz gel�scht wird
 */
public class SchedulingOrder {
  
  
  
  public static enum WaitingCause {
    //Reihenfolge ist wichtig: letzter WaitingCause bestimmt angezeigten OrderInstanceStatus
    None(OrderInstanceStatus.SCHEDULING),
    Locked(OrderInstanceStatus.WAITING_FOR_LOCK), //Auftrag ist gesperrt und darf nicht ver�ndert oder geschedult werden
    Unlock_RecalculateUrgency(OrderInstanceStatus.WAITING_FOR_LOCK), //Beim Unlock muss Urgency neu berechnet werden
    Unlock_ReaddToScheduler(OrderInstanceStatus.WAITING_FOR_LOCK),   //Beim Unlock muss Auftrag wieder in Scheduler eingestellt werden
    Unlock_Reschedule(OrderInstanceStatus.WAITING_FOR_LOCK),         //Beim Unlock muss Auftrag im Scheduler reschedult werden
    StartTime(OrderInstanceStatus.WAITING_FOR_TIMECONSTRAINT),
    Series( OrderInstanceStatus.WAITING_FOR_PREDECESSOR),
    BatchProcess( OrderInstanceStatus.WAITING_FOR_BATCH_PROCESS),
    Deployment( OrderInstanceStatus.WAITING_FOR_DEPLOYMENT);
    
    private OrderInstanceStatus status;
    
    private WaitingCause(OrderInstanceStatus status) {
      this.status = status;
    }
 
    public OrderInstanceStatus getStatus() {
      return status;
    }

  }
  
  public static enum ExtendedStatus {
    New(       0,false), //Auftrag ist neu
    Scheduling(1,true),  //Auftrag ist im Scheduler (Normalzustand)
    Backupped( 1,true),  //Auftrag ist wegen OOM-Schutz entfernt worden
    TimedOut(  2,false), //Auftrag hat Timeout und muss daher abgebrochen werden
    Canceled(  2,false), //Auftrag wurde gecancelt und muss daher abgebrochen werden
    Terminated(3,false), //Auftrag hat einen Fehler und muss daher abgebrochen werden
    Remove(    4,false); //Auftrag muss aus SchedulerAlgorithm entfernt werden
    
    private int order;
    private boolean canBeScheduled;
    private ExtendedStatus(int order,boolean canBeScheduled) {
      this.order = order;
      this.canBeScheduled = canBeScheduled;
    }
    public boolean canChangeTo(ExtendedStatus newState) {
      return order <= newState.order; //neuer Status muss h�here Order haben
    }
    public boolean canBeScheduled() {
      return canBeScheduled;
    }
   
  }
  
  private static Logger logger = CentralFactoryLogging.getLogger(SchedulingOrder.class);

  public static Transformation<SchedulingOrder, XynaOrderServerExtension> xynaOrdersNotBackuped = new XynaOrdersNotBackuped();
  public static Transformation<SchedulingOrder, XynaOrderInfo> ordersScheduling = new OrdersWaiting( WaitingCause.None );
  public static Transformation<SchedulingOrder, XynaOrderInfo> ordersWaitingForSeries = new OrdersWaiting( WaitingCause.Series );
  public static Transformation<SchedulingOrder, XynaOrderInfo> ordersWaitingForStartTime = new OrdersWaiting( WaitingCause.StartTime );
  public static Transformation<SchedulingOrder, XynaOrderInfo> ordersWaitingForDeployment = new OrdersWaiting( WaitingCause.Deployment );
  public static Transformation<SchedulingOrder, XynaOrderInfo> allOrders = new AllOrders();
  public static Transformation<SchedulingOrder, XynaOrderInfo> allOrdersInRuntimeContext(RuntimeContext runtimeContext) {
    return new AllOrdersInRuntimeContext(runtimeContext);
  }
  public static Transformation<SchedulingOrder, XynaOrderInfo> rootOrders = new RootOrders();
  
  private XynaOrderServerExtension xynaOrder;
  private volatile OrderInstanceStatus orderStatus;
  private volatile ExtendedStatus extendedStatus;
  private final EnumSet<WaitingCause> waitingCauses;
  private int hash;
  private Long rootOrderId;
  private String tag;
  private OrderInformation orderInformation;
  private long currentUrgency; //Transport der urgency zu TryScheduleAbstract. Urgency<SchedulingOrder> ist Master!
  private SchedulingData schedulingData;
  private BatchProcessMarker batchProcessMarker;
  private boolean hasParentOrder;
  private List<XynaException> exceptions;
  
  public SchedulingOrder(XynaOrderServerExtension xynaOrder) {
    this(xynaOrder,EnumSet.noneOf(WaitingCause.class));
  }

  public SchedulingOrder(XynaOrderServerExtension xynaOrder, EnumSet<WaitingCause> waitingCauses) {
    this.xynaOrder = xynaOrder;
    this.orderInformation = new OrderInformation(xynaOrder);
    this.orderStatus = OrderInstanceStatus.INITIALIZATION;
    this.waitingCauses = waitingCauses;
    this.extendedStatus = ExtendedStatus.New;
    this.rootOrderId = xynaOrder.getRootOrder().getId();
    this.schedulingData = xynaOrder.getSchedulingData();
    this.batchProcessMarker = xynaOrder.getBatchProcessMarker();
    this.hasParentOrder = xynaOrder.hasParentOrder();
  }

  @Override
  public boolean equals(Object obj) {
    if( obj instanceof SchedulingOrder ) {
      return orderInformation.getOrderId().equals(((SchedulingOrder)obj).orderInformation.getOrderId());
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      h = orderInformation.getOrderId().hashCode();
      hash = h;
    }
    return h;
  }
  
  /**
   * muss in synchronized-Block gerufen werden
   */
  public void waitIfLocked() {
    while( isLocked() ) {
      try {
        wait();
      } catch (InterruptedException e) {
        //ignorieren, sollte keinen Fall geben, in denen ein wartender Thread so geweckt wird.
      }
    }
  }
  
  public void setTag(String tag) {
    this.tag = tag;
  }
  
  public String getTag() {
    return tag;
  }
  
  public OrderInformation getOrderInformation() {
    return orderInformation;
  }
  
  public void setCurrentUrgency(long urgency) {
    this.currentUrgency = urgency;
  }
  
  public long getCurrentUrgency() {
    return currentUrgency;
  }
  
  public List<String> getVetos() {
    return getSchedulingData().getVetos();
  }
  
  public void terminate(XynaException xynaException) {
    addException(xynaException);
    markAsTerminated();
  }
  
  public void timeout(XynaException xynaException) {
    addException(xynaException);
    markAsTimedout();
  }

  private void addException(XynaException xynaException) {
    if( xynaException == null ) {
      return;
    }
    if( exceptions == null ) {
      exceptions = Collections.singletonList(xynaException);
    } else {
      List<XynaException> es = new ArrayList<XynaException>(exceptions);
      es.add(xynaException);
      exceptions = es;
    }
  }

  public List<XynaException> getSchedulingExceptions() {
    return exceptions;
  }
  
  public boolean hasBeenBackupped() {
    return isState(ExtendedStatus.Backupped);
  }

  public ExtendedStatus getExtendedStatus() {
    return extendedStatus;
  }

  private boolean isState(ExtendedStatus state) {
    return extendedStatus == state;
  }
  private boolean setExtendedStatus(ExtendedStatus newState) {
    if( extendedStatus.canChangeTo(newState) ) {
      extendedStatus = newState;
      return true;
    }
    return false;
  }

  /**
   * Achtung: XynaOrder kann null sein!
   * Daher besser bei AllOrdersList nach getXynaOrder() fragen {@link com.gip.xyna.xprc.xsched.AllOrdersList#getXynaOrder(SchedulingOrder) AllOrdersList.getXynaOrder}
   * @return xynaOrder
   */
  public XynaOrderServerExtension getXynaOrderOrNull() {
    return xynaOrder;
  }
  
  public String toString() {
    return "SchedulingOrder("+orderInformation.getOrderId()+",orderStatus="+orderStatus+",extendedStatus="+extendedStatus+")";
  }
  
  public Long getOrderId() {
    return orderInformation.getOrderId();
  }

  public boolean markAsRemoved() {
    if( isAlreadyScheduled() ) {
      return false;
    }
    return setExtendedStatus(ExtendedStatus.Remove);
  }

  public boolean markAsTimedout() {
    if( isAlreadyScheduled() ) {
      return false;
    }
    return setExtendedStatus(ExtendedStatus.TimedOut);
  }
  
  public void markAsScheduling() {
    setExtendedStatus(ExtendedStatus.Scheduling);
  }
  
  public void markAsTerminated() {
    setExtendedStatus(ExtendedStatus.Terminated);
  }

  public void markAsCanceled() {
    setExtendedStatus(ExtendedStatus.Canceled);
  }

  /**
   * true, wenn Auftrag normal geschedult werden kann; 
   * false, wenn ein Fehlerzustand vorliegt und die Order deswegen abgebrochen werden muss
   * @return
   */
  public boolean canBeScheduled() {
    return extendedStatus.canBeScheduled();
  }
  
  public boolean isMarkedAsRemove() {
    return isState(ExtendedStatus.Remove);
  }
  public boolean isMarkedAsTimedout() {
    return isState(ExtendedStatus.TimedOut);
  }
  public boolean isMarkedAsNew() {
    return isState(ExtendedStatus.New);
  }
  public boolean isMarkedAsCanceled() {
    return isState(ExtendedStatus.Canceled);
  }


  public Set<WaitingCause> getWaitingCauses() {
    return Collections.unmodifiableSet(waitingCauses);
  }
  
  public boolean isLocked() {
    return waitingCauses.contains(WaitingCause.Locked);
  }
  
  public void addLockAction(WaitingCause lockAction) {
    this.waitingCauses.add(lockAction);
  }
  
  public boolean removeLockAction(WaitingCause lockAction) {
    return this.waitingCauses.remove(lockAction);
  }

  public boolean isWaitingOrLocked() {
    if( waitingCauses.isEmpty() ) {
      return false; //leer -> nicht wartend
    } else {
      if( waitingCauses.size() == 1 ) {
        if( waitingCauses.contains(WaitingCause.None) ) {
          return false; //nur "none" -> nicht wartend, sollte aber auch nicht vorkommen
        }
      }
    }
    return true; //WaitingCause ist gesetzt
  }
  
  public boolean isWaitingFor(WaitingCause waitingCause) {
    return waitingCauses.contains(waitingCause);
  }
  
  /**
   * WaitingCause waitingCause ist erledigt, daher verbleibende WaitingCauses und den State anpassen 
   * @param waitingCause
   */
  public boolean removeWaitingCause(WaitingCause waitingCause) {
    if( waitingCause == null || waitingCause == WaitingCause.None ) {
      return false; //kann nicht entfernt werden
    }
    boolean removed = this.waitingCauses.remove(waitingCause);
    setStateAccordingToWaitingCause();
    return removed;
  }
  
  /**
   * Neuer WaitingCause waitingCause kommt hinzu, daher WaitingCauses und den State anpassen 
   * @param waitingCause
   */
  public void addWaitingCause(WaitingCause waitingCause) {
    this.waitingCauses.add(waitingCause);
    setStateAccordingToWaitingCause();
  }

  
  /**
   * setzt den OrderStatus passend zum WaitingCause
   */
  public void setStateAccordingToWaitingCause() {
    if( orderStatus.isInScheduler() ) {
      OrderInstanceStatus status = WaitingCause.None.getStatus();
      for( WaitingCause wc : waitingCauses ) {
        status = wc.getStatus();
      }
      orderStatus = status;
    }
  }

  public SchedulingData getSchedulingData() {
    return schedulingData; //SchedulingOrder istimm Master gegen�ber XynaOrder
  }

  public boolean canBeRemovedFromOOMProtection() {
    if( isState(ExtendedStatus.Remove) ) {
      return true; //Soll aus Scheduler entfernt werden
    }
    return ! orderStatus.isInScheduler();
  }
  
  public boolean isAlreadyScheduled() {
    return ! orderStatus.isInScheduler();
  }

  public void setOrderStatus(OrderInstanceStatus status) {
    this.orderStatus = status;
  }

  public OrderInstanceStatus getOrderStatus() {
    return orderStatus;
  }
  
  public Long getRootOrderId() {
    return rootOrderId;
  }
  
  public DestinationKey getDestinationKey() {
    return orderInformation.getDestinationKey();
  }
  
  /**
   * Setzt SchedulingStatus auf Scheduling, auch wenn er vorher fehlerhaft war.
   * Wird z.B. im Falle von BatchProcessMaster ben�tigt, wenn die Slaves abgebrochen werden, der Master aber weiterlaufen soll
   */
  public void clearFailure() {
    extendedStatus = ExtendedStatus.Scheduling;
  }


  /**
   * Backup der XynaOrder: Schreiben ins OrderInstanceBackup, OrderInstanceDetails
   * @return false, falls Schreiben fehlschl�gt
   */
  public boolean backup() {
    if (xynaOrder == null) {
      return true; //es gibt nichts zu schreiben
    }
    if( ! isState(ExtendedStatus.Scheduling) ) {
      return false; //nicht backuppen, da im kurzlebigen Zustand New unsicher und in anderen Zust�nden nicht n�tig
    }
    if ( ! orderStatus.isInScheduler() ) {
      return true; //es gibt nichts zu schreiben
    }
    if (logger.isTraceEnabled()) {
      logger.trace("backup order " + getOrderId());
    }
    try {
      WarehouseRetryExecutor.buildCriticalExecutor().
      connectionDedicated(DedicatedConnection.SchedulerOOMProtectionBackups).
      storable(OrderInstanceBackup.class).storable(OrderInstanceDetails.class).
      execute(new XynaOrderWriter(this));
      return true;
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to backup order", e);
      return false;
    }
  }


  /**
   * Entfernen der XynaOrder (nur nach Aufruf von backup() oder nach Ausf�hrung des Auftrags m�glich oder beim Canceln)
   */
  public void removeXynaOrder() {
    if (logger.isTraceEnabled()) {
      logger.trace("remove order " + getOrderId());
    }
    schedulingData = xynaOrder.getSchedulingData(); //siehe getSchedulingData()
    switch( extendedStatus ) {
      case Backupped: //Auftrag gebackupt, kann daher f�r OOM-Schutz entfernt werden
      case Remove://Auftrag soll aus Scheduler ausgetragen werden, kann daher auch entfernt werden
        xynaOrder = null;
        return;
      case New:
      case Scheduling:
        if( isAlreadyScheduled() ) {
          //Auftrag wird nun ausgef�hrt, da gerade geschedult
          xynaOrder = null;
          return;
        } else {
          //unerwarteter Fall
        }
        break;
      case TimedOut:
        //Auftrag mit SchedulingTimeout beendet, kann daher auch entfernt werden
        xynaOrder = null;
        return;
      default:
        //Fehler
    }
    logger.warn("removeXynaOrder "+getOrderId()+" called for orderStatus="+orderStatus+" extendedStatus="+extendedStatus );
  }


  /**
   * Auslesen der XynaOrder aus dem OrderArchive, falls die xynaOrder fehlt
   * @return XynaOrderServerExtension
   */
  public XynaOrderServerExtension restore() {
    if (logger.isDebugEnabled()) {
      logger.debug("restore XynaOrder " + getOrderId());
    }
    synchronized (this) {
      waitIfLocked();
      try {
        return WarehouseRetryExecutor.buildCriticalExecutor().
        storable(OrderInstanceBackup.class).
        execute(new XynaOrderReader(this));
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to restore order from schedulingOrder", e);
        return null;
      }
    }
  }


  private static class XynaOrderReader implements WarehouseRetryExecutableNoException<XynaOrderServerExtension> {

    SchedulingOrder so;

    public XynaOrderReader(SchedulingOrder so) {
      this.so = so;
    }

    public XynaOrderServerExtension executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      try {
        // restores a backuped rootOrder, childOrders aren't backuped from the transformation
        OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
        OrderInstanceBackup oib = oa.getBackedUpRootOrder(so.getOrderId(), con);
        if( oib == null ) {
          logger.warn("could not read xynaorder " + so.getOrderId() + " from backup.");
          return null;
        }
        so.xynaOrder = oib.getXynaorder();
        oa.restoreTransientOrderParts(so.xynaOrder, so.getOrderId());
        so.setExtendedStatus(ExtendedStatus.Scheduling);
        so.xynaOrder.replaceSchedulingData( so.schedulingData ); //SchedulingData wurde evtl in der Backup-Zeit ge�ndert
        con.commit();
        return so.xynaOrder;
      } catch (PersistenceLayerException e) {
        logger.warn("could not read xynaorder " + so.getOrderId() + " from backup.", e);
      }
      return null;
    }
  }
  
  private static class XynaOrderWriter implements WarehouseRetryExecutableNoResult {

    SchedulingOrder so;

    public XynaOrderWriter(SchedulingOrder so) {
      this.so = so;
    }

    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      if( so.xynaOrder == null ) {
        logger.warn( "No xynaOrder to backup for "+so);
        return;
      }
      
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().
      backup(so.xynaOrder, BackupCause.WAITING_FOR_CAPACITY, con);
      con.commit();
      so.setExtendedStatus(ExtendedStatus.Backupped);
    }
  }
  
  public boolean isRelevantForOOMProtection() {
    if( xynaOrder == null ) {
      //XynaOrder fehlt: dies ist ein schwerer Fehler, der hier aber nicht zu einer NPE f�hren soll
      //keine OOMProtection n�tig; Scheduler wird sich um dieses Problem k�mmern und Auftrag beenden
      return false; 
    }
    if( xynaOrder.getParentOrder() != null ) {
      //ParentOrder ist vorhanden, dann ist dies ein Subauftrag,
      //der nicht aus dem Speicher entfernt werden kann, da eine
      //Referenz auf die XynaOrder vom Parent-Auftrag gehalten wird.
      return false;
    }
    return true;
  }
  
  public static class XynaOrdersNotBackuped implements Transformation<SchedulingOrder, XynaOrderServerExtension> {
    public XynaOrderServerExtension transform(SchedulingOrder from) {
      switch( from.extendedStatus ) {
        case Backupped: 
          return null; //nicht nochmal backuppen
        case Remove:
          return null; //nicht ausgeben, da keine g�ltige XynaOrder mehr
        case TimedOut: //sollte zwar nur noch kurz existieren, aber noch ein Backup ist sicherer 
        case New:
        case Scheduling:
          XynaOrderServerExtension xo = from.getXynaOrderOrNull();
          if (xo != null && !xo.hasBeenBackuppedAfterChange()) {
            return xo;
          } else {
            return null;
          }
        default:
           throw new IllegalStateException("Unexpected extendedStatus "+from.extendedStatus );
      }
    }
  }
  
  public static class OrdersWaiting implements Transformation<SchedulingOrder, XynaOrderInfo> {
    
    private WaitingCause waitingCause;

    public OrdersWaiting(WaitingCause waitingCause) {
      this.waitingCause = waitingCause;
    }

    public XynaOrderInfo transform(SchedulingOrder from) {
      if( from.isState(ExtendedStatus.Remove) ) { 
        return null; //keine g�ltige SchedulingOrder 
      }
      if( from.waitingCauses.contains(waitingCause) ||
          (from.waitingCauses.isEmpty() && waitingCause == WaitingCause.None) ) { // None enstspricht leerer Liste
        if (from.getOrderStatus() == OrderInstanceStatus.SCHEDULING_CAPACITY ||
            from.getOrderStatus() == OrderInstanceStatus.SCHEDULING_VETO) {
          return new XynaOrderWaitingForResourceInfo(from);
        } else {
          return new XynaOrderInfo(from);
        }
      } else {
        return null;
      }
    }
  }

  public static class AllOrders implements Transformation<SchedulingOrder, XynaOrderInfo> {
    
    public XynaOrderInfo transform(SchedulingOrder from) {
      if( from.isState(ExtendedStatus.Remove) ) { 
        return null; //keine g�ltige SchedulingOrder 
      }
      return new XynaOrderInfo(from);
    }
  }
  
  public static class RootOrders implements Transformation<SchedulingOrder, XynaOrderInfo> {
    
    public XynaOrderInfo transform(SchedulingOrder from) {
      if( from.isState(ExtendedStatus.Remove) ) { 
        return null; //keine g�ltige SchedulingOrder 
      }
      if( from.getOrderId().equals(from.rootOrderId) ) {
        return new XynaOrderInfo(from);
      } else {
        return null;
      }
    }
  }
  
  public static class AllOrdersInRuntimeContext implements Transformation<SchedulingOrder, XynaOrderInfo> {
    private RuntimeContext runtimeContext;
    
    public AllOrdersInRuntimeContext(RuntimeContext runtimeContext) {
      this.runtimeContext = runtimeContext;
    }

    public XynaOrderInfo transform(SchedulingOrder from) {
      if( from.isState(ExtendedStatus.Remove) ) { 
        return null; //keine g�ltige SchedulingOrder 
      }
      if( from.getDestinationKey().getRuntimeContext().equals(runtimeContext) ) {
        return new XynaOrderInfo(from);
      } else {
        return null;
      }
    }
  }

  public BatchProcessMarker getBatchProcessMarker() {
    return batchProcessMarker;
  }

  public void replaceSchedulingData(SchedulingData schedulingData) {
    this.schedulingData = schedulingData;
    //evtl auch in XynaOrder �ndern, wenn diese verf�gbar ist
    XynaOrderServerExtension xo = xynaOrder;
    if( xo != null ) {
      xo.replaceSchedulingData(schedulingData);
    }
  }

  public boolean hasParentOrder() {
    return hasParentOrder;
  }

}
