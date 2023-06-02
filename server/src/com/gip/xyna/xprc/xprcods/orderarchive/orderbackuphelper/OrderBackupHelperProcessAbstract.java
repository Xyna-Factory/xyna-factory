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
package com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.exceptions.XPRC_OrderBackupIncomplete_XynaOrderMissing;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;


public abstract class OrderBackupHelperProcessAbstract {

  protected final int BLOCKSIZE = 100;
  
  protected Logger logger;
  
  protected volatile boolean stop;
  protected volatile List<PrioritizedRootId> idsWithPriority;
  protected volatile boolean hasConnection;
  
  protected int ownBinding;
  
  protected PreparedQueryCache preparedQueryCache;
  protected Set<Long> alreadyProcessedIds;
  protected volatile boolean finished;
  protected Object runMonitor = new Object();
  
  
  public static enum ResumeAction {
    AddToScheduler,
    Resume,
    Redirect,
    NoRoot,
    Keep;
  }
  
  
  public OrderBackupHelperProcessAbstract(List<PrioritizedRootId> idsWithPriority, int ownBinding) {
    this.idsWithPriority = idsWithPriority;
    logger = CentralFactoryLogging.getLogger(getClass());
    this.ownBinding = ownBinding;
    preparedQueryCache = new PreparedQueryCache();
    hasConnection = false;
    alreadyProcessedIds = new HashSet<Long>();
    finished = false;
  }

  protected void runInternally() {
    try {
      WarehouseRetryExecutorBuilder wreb = WarehouseRetryExecutor.buildCriticalExecutor().
          storable(OrderInstanceBackup.class).storable(OrderInstanceDetails.class);
      StorableClassList additionalStorables = getAdditionalStorables();
      if( additionalStorables != null ) {
        wreb.storables(additionalStorables);
      }
      wreb.execute(getWarehouseRetryExecutable());
      
      //fertig geworden, nun noch übrige Warter benachrichtigen
      synchronized (idsWithPriority) {
        for( PrioritizedRootId pri : idsWithPriority ) {
          pri.countDown();
        }
        idsWithPriority.clear();
        finished = true;
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to transfer from <" + OrderInstanceBackup.TABLE_NAME + "> to <"
          + OrderInstanceDetails.TABLE_NAME + ">", e);
    }
  }
  
  protected abstract WarehouseRetryExecutableNoResult getWarehouseRetryExecutable();

  protected StorableClassList getAdditionalStorables() {
    return null;
  }

  public boolean hasConnection() {
    return hasConnection;
  }
  
  public void stopWorking() {
    stop = true;
  }
  
  /** 
   * @return true, wenn Auftrag zum Scheduler hinzugefügt werden soll - sonst false
   */
  private ResumeAction specialHandlingForUnsafeOrders(OrderInstanceBackup orderBackup) {
      OrderStartupMode orderStartupMode = orderBackup.getXynaorder().getOrderStartupMode();
      if(!orderStartupMode.isSafeMode()) {
        // unsafe mode ... also akzeptieren wir diese Causes auch
        return ResumeAction.Resume;
      } else {
        // safe mode
        if(orderStartupMode == OrderStartupMode.SAFE_ABORT_ORDER_WITH_COMPENSATION || orderStartupMode == OrderStartupMode.SAFE_ABORT_ORDER_WITHOUT_COMPENSATION) {
          boolean compensationAllowed = true;
          if(orderStartupMode == OrderStartupMode.SAFE_ABORT_ORDER_WITHOUT_COMPENSATION) {
            compensationAllowed = false;
          }
          if(logger.isDebugEnabled()) {
            logger.debug("Mark order with id <" + orderBackup.getId() + "> to abort with compensation = "
                            + compensationAllowed + " because of orderStartupMode = <" + orderStartupMode + ">");
          }
          List<XynaOrderServerExtension> orders = orderBackup.getXynaorder().getOrderAndChildrenRecursively();
          for(XynaOrderServerExtension order : orders) {
            order.setLetOrderAbort(true);
            order.setLetOrderCompensateAfterAbort(compensationAllowed);
          }
          return ResumeAction.Resume;
        }
      }
    return ResumeAction.Keep;
  }


  protected List<OrderInstanceBackup> extractCompleteOrderFamilies(List<OrderInstanceBackup> incompleteFamily, //wird modifiziert!
                                                                 List<OrderInstanceBackup> nextOBs ) {
    long incompleteRootId = getLastRootId(nextOBs); 
    
    List<OrderInstanceBackup> completeFamily = new ArrayList<OrderInstanceBackup>();
    if( ! incompleteFamily.isEmpty() ) {
      if( incompleteFamily.get(0).getRootId() < incompleteRootId ) {
        completeFamily.addAll(incompleteFamily);
        incompleteFamily.clear();
      }
    }
    
    for( OrderInstanceBackup oib : nextOBs ) {
      if( oib.getRootId() < incompleteRootId ) {
        completeFamily.add(oib);
      } else {
        incompleteFamily.add(oib);
      }
    }
    return completeFamily;
  }


  protected long getLastRootId(List<OrderInstanceBackup> oibs) {
    if( oibs == null || oibs.isEmpty() ) {
      return -1;
    }
    return oibs.get(oibs.size()-1).getRootId();
  }

  protected List<Long> retrievePrioritized( long lastProcessedRootId, String user) {
    List<Long> ids = new ArrayList<Long>();
    synchronized (idsWithPriority) {
      for( PrioritizedRootId id : idsWithPriority) {
        if (lastProcessedRootId >= id.getId() || alreadyProcessedIds.contains(id.getId())) {
          if (logger.isDebugEnabled()) {
            logger.debug( user+": Priority order with id " + id.getId()+ " was already processed.");
          }
        } else {
          ids.add(id.getId());
        }
      }
    }
    return ids;
  }
  
  protected void processPrioritized(ODSConnection con, List<Long> ids, String user) {
    if( ! ids.isEmpty() ) {
      con.executeAfterCommit( new PriorityProcessedRootIds(user, ids, idsWithPriority, alreadyProcessedIds, logger) );
    }
  }
  
  protected List<OrderInstanceBackup> retrievePrioritized(ODSConnection con, long lastProcessedRootId, String user) throws PersistenceLayerException {
    List<Long> ids = retrievePrioritized(lastProcessedRootId, user);
    if( ! ids.isEmpty() ) {
      List<OrderInstanceBackup> backupOrders = getBackupItems(ids, con);
      processPrioritized(con, ids, user);
      return backupOrders;
    } else {
      return Collections.emptyList();
    }
  }


  protected static class PriorityProcessedRootIds implements Runnable {

    private String user;
    private Set<Long> alreadyProcessedIds;
    private Set<Long> processed;
    private Logger logger;
    private List<PrioritizedRootId> idsWithPriority;
    
    public PriorityProcessedRootIds(String user, List<Long> processed, List<PrioritizedRootId> idsWithPriority, Set<Long> alreadyProcessedIds, Logger logger) {
      this.user = user;
      this.idsWithPriority = idsWithPriority;
      this.processed = new HashSet<Long>(processed);
      this.alreadyProcessedIds = alreadyProcessedIds;
      this.logger = logger;
    }

    public void run() {
      synchronized( idsWithPriority ) {
        Iterator<PrioritizedRootId> iter = idsWithPriority.iterator();
        while (iter.hasNext()) {
          PrioritizedRootId pri = iter.next();
          if( processed.contains(pri.getId() ) ) {
            if (logger.isDebugEnabled()) {
              logger.debug(user +": Notify waiting proccess for priority order with id "+ pri.getId());
            }
            pri.countDown();
            iter.remove();
          }
        }
      }
      alreadyProcessedIds.addAll(processed);
    }
    
  }


  protected void resumeOrdersFromBackup(ODSConnection con, List<OrderInstanceBackup> orderBackups) 
      throws PersistenceLayerException {
        
    List<Pair<XynaOrderServerExtension,ResumeAction>> orders = new ArrayList<Pair<XynaOrderServerExtension,ResumeAction>>();
    boolean debugOrderInfo = logger.isInfoEnabled();
    List<Triple<Long,BackupCause,ResumeAction>> orderInfo = new ArrayList<Triple<Long,BackupCause,ResumeAction>>();
    for (OrderInstanceBackup orderBackup : orderBackups ) {
      ResumeAction resumeAction = getResumeAction(orderBackup);
      switch( resumeAction ) {
        case AddToScheduler:
        case Redirect:
        case Resume:
          orders.add(Pair.of(orderBackup.getXynaorder(),resumeAction));
          break;
        default:
        //Auftrag verbleibt im Backup
      }
      if( debugOrderInfo ) {
        orderInfo.add( Triple.of( orderBackup.getId(), orderBackup.getBackupCauseAsEnum(), resumeAction));
      }
    }
    if( debugOrderInfo ) {
      logger.info( "Found orders to resume in orderBackup: "+orderInfo);
    }
    resumeOrders(con, orders);
  }


  protected ResumeAction getResumeAction(OrderInstanceBackup orderBackup) {
    if(orderBackup.getRootId() == orderBackup.getId() && orderBackup.getXynaorder() == null) {
      XynaExtendedStatusManagement.addFurtherInformationAtStartup("OrderInstanceBackup","Order with id " + orderBackup.getId()
          + " is not started because deserialization of order backup entry failed.");
    }
    
    // Wir sichern die XynaOrders in der Liste mit den zu verarbeitenden Aufträgen, wenn die Familie vollständig ist.
    // Ansonsten werfen wir sie in die Liste mit unvollständigen Familien. Die Annahme hier ist, dass
    // Familienmitglieder adjazent im OrderBackup sind (sichergestellt durch ein SQL Query mit orderBy RootId).
    if (orderBackup.getXynaorder() == null) {
      //dies ist keine Root-Order, daher überspringen
      return ResumeAction.NoRoot;
    } else {
      BackupCause obBackupCauseAsEnum = orderBackup.getBackupCauseAsEnum();
      if( obBackupCauseAsEnum.isSafeForResumeAtStartup() ) {
        return ResumeAction.AddToScheduler;
      } else if( obBackupCauseAsEnum == BackupCause.AFTER_SCHEDULING ) {
        return specialHandlingForUnsafeOrders(orderBackup);
      } else if (obBackupCauseAsEnum == BackupCause.SUSPENSION ) {
        if (orderBackup.getXynaorder().getRedirection() != null) {
          // evtl vom vorletzten shutdown noch im backup. redirection-auftrag ist dann jetzt noch im system,
          // der eigentliche auftrag darf nicht zum scheduler geaddet werden.
          return ResumeAction.Keep;
        } else {
          //resume-sources sind evtl nur in memory und werden durch das resume wiederhergestellt (z.b. crons, mis)
          return ResumeAction.Resume;
        }
      } else {
        return ResumeAction.Keep;
      }
    }
  }


  protected void resumeOrders(ODSConnection con, List<Pair<XynaOrderServerExtension,ResumeAction>> orders) throws PersistenceLayerException {

    for( Pair<XynaOrderServerExtension,ResumeAction> pair : orders ) {
      final XynaOrderServerExtension order = pair.getFirst();
      ResumeAction resumeAction = pair.getSecond();
      
      for (XynaOrderServerExtension o : order.getOrderAndChildrenRecursively()) {
        SchedulingData sd = o.getSchedulingData();
        //Capacity-Vergabe ist nicht persistent: Daher muss andere Knoten erneut Capacities belegen.
        sd.setHasAcquiredCapacities(false);
        sd.setNeedsToAcquireCapacitiesOnNextScheduling(true);
        //Veto-Vergabe ist zwar persistent: VetoManagmente benötigt aber Informationen über das Binding, 
        //die es über dan erneute Allokieren erhält
        sd.setNeedsToAcquireVetosOnNextScheduling(true);

        // execution process instance can be null e.g. if the order has not been scheduled
        if (o.getExecutionProcessInstance() != null) {
          o.getExecutionProcessInstance().prepareForResume();
        }
      }
      
      switch( resumeAction ) {
        case Redirect:
          order.getRedirection().redirectOrder(order, con);
          break;
        case AddToScheduler:
          //direktes Einstellen in den Scheduler ist unsicher, da nicht bekannt ist, obe Auftrag bereits gelaufen ist.
          //Falls er bereits gelaufen war, können Resumes auftreten. Daher muss der Auftrag über das
          //SuspendResumeManagement in den Scheduler eingestellt werden, damit sich das SRM um konkurrierende Resumes kümmern kann.
        case Resume:
          // Execution Timeout erreicht und bereits einmal gescheduled (TODO destinationtype != WF behandeln)
          if (order.getExecutionProcessInstance() != null && order.getOrderExecutionTimeout() != null) {
            con.executeAfterCommit(new Runnable() {

              public void run() {
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getOrderExecutionTimeoutManagement()
                    .registerOrderTimeout(order);
              }
            }, Thread.MIN_PRIORITY);
          }
          
          //Auch Aufträge mit SchedulingTimeout gelangen nun in den Scheduler. 
          //Das TimeConstraintManagement wird diese Aufträge beenden. 
          if (order.isInOrderSeries()) {
            XynaFactory.getInstance().getProcessing().getXynaScheduler().getOrderSeriesManagement().resume(order, con);
          }
          SuspendResumeManagement srm =
              XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
          Pair<ResumeResult, String> result = srm.resumeOrder(order, con);
          switch (result.getFirst()) {
            case Resumed :
              break;
            case Unresumeable :
              //TODO was nun? 
              //Factory-Shutdown: nichts zu tun
              //Deployment: 
              //MI-Redirection:
              logger.warn("Could not resume " + order + ": " + result);
              break;
            case Failed :
              logger.warn("Could not resume " + order + ": " + result);
              break;
            default :
              logger.error("Could not resume " + order + ": " + result);
          }
          break;
        default:
          logger.warn("Unexpected resumeAction "+resumeAction+" for xynaorder "+order );
         
      }
    } 
    con.commit();
  }


  protected List<OrderInstanceBackup> getBackupItems(Collection<Long> rootorderIds, ODSConnection con)
      throws PersistenceLayerException {
    List<Object> rootOrderIDList = new ArrayList<Object>();
    StringBuilder orderbackupSQL = new StringBuilder();
    orderbackupSQL.append("select * from " + OrderInstanceBackup.TABLE_NAME + " where (");

    Iterator<Long> iterRootOrderIds = rootorderIds.iterator();
    while(iterRootOrderIds.hasNext()) {
      orderbackupSQL.append(OrderInstanceBackup.COL_ROOT_ID).append("=?");
      rootOrderIDList.add(iterRootOrderIds.next());
      
      if(iterRootOrderIds.hasNext()) {
        orderbackupSQL.append(" or ");
      }
    }
    
    orderbackupSQL.append(") order by ").append(OrderInstanceBackup.COL_ROOT_ID);
    
    PreparedQuery<OrderInstanceBackup> loadOrderBackupForRootIds =
        preparedQueryCache.getQueryFromCache(orderbackupSQL.toString(), con,
                                             OrderInstanceBackup.getReaderWarnIfNotDeserializable());
    
    return con.query(loadOrderBackupForRootIds, new Parameter(rootOrderIDList.toArray()), -1);
  }
  
  /**
   * gibt true zurück, falls auftrag nicht wiederherstellbar ist 
   * @param removeImmediately auftragsfamilie sofort aufräumen
   */
  protected boolean checkOrderBackupInstanceForRemoval(OrderInstanceBackup orderInstanceBackup, ODSConnection con,
                                                       boolean removeImmediately) throws PersistenceLayerException {
    OrderInstanceDetails oid = orderInstanceBackup.getDetails();

    boolean bothNull = oid == null && orderInstanceBackup.getXynaorder() == null;
    boolean rootOrderWithoutXynaOrder =
        oid != null && oid.getParentId() == -1 && orderInstanceBackup.getXynaorder() == null;

    if (bothNull || rootOrderWithoutXynaOrder) {
      logger.warn("Backup information for order " + orderInstanceBackup.getId()
                  + " has no parent and no order information. Removing it from orderbackup.");
      if (removeImmediately) {
        // Fall kann auftreten, wenn der Auftrag auf dem Node gestartet wird, während der Node
        // herunter fährt und die Daten noch nicht vollständig ins orderbackup gesichert wurden.
        removeOrderBackupInstance(orderInstanceBackup, con);
      }
      return true;
    }
    return false;
  }

  protected void abortBackups(List<OrderInstanceBackup> list, ODSConnection con) throws PersistenceLayerException {
    if (list.size() == 0) {
      return;
    }
    for (OrderInstanceBackup oib : list) {
      removeOrderBackupInstance(oib, con);
    }
  }

  protected void removeOrderBackupInstance(OrderInstanceBackup orderInstanceBackup, ODSConnection con)
      throws PersistenceLayerException {
    if( orderInstanceBackup.getDetails() != null ) {
      archive(orderInstanceBackup.getDetails(), orderInstanceBackup.getRevision());
    }
    KillStuckProcessBean killStuckProcessBean = new KillStuckProcessBean(orderInstanceBackup.getId(), false, AbortionCause.UNKNOWN);
    if (logger.isDebugEnabled()) {
      logger.debug("Try to clean up order " + orderInstanceBackup.getId());
    }
    XynaFactory.getInstance().getProcessing().getXynaScheduler().getOrderAbortionManagement()
      .cleanupOrderRelicsForFamily(orderInstanceBackup.getRootId(), killStuckProcessBean);
    if (logger.isInfoEnabled()) {
      logger.info("Result of order abortion for clean up: " + killStuckProcessBean.getResultMessage());
    }
    String info = "Aborted order "+orderInstanceBackup.getId() +":\n"
        + killStuckProcessBean.getResultMessage();
    XynaExtendedStatusManagement.addFurtherInformationAtStartup( "OrderStartupManagement", info );
  }


  /**
   * @param foreignBackupInstance
   */
  private void archive(OrderInstanceDetails oid, Long revision) {
    logger.info("Broken OrderInstanceBackup for order " + oid.getId() + " will be archived.");
    
    OrderArchive orderArchive = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
    try {
      oid.setStatus(OrderInstanceStatus.XYNA_ERROR);
      oid.addException( new XPRC_OrderBackupIncomplete_XynaOrderMissing() );
      long now = System.currentTimeMillis();
      oid.setLastUpdate(now);
      oid.setStopTime(now);
      
      try {
        oid.convertAuditDataToXML(revision, true);
      } catch (XPRC_CREATE_MONITOR_STEP_XML_ERROR e) {
        logger.warn("Failed to convertAuditDataToXML for broken OrderInstanceBackup", e);
      }
      
      orderArchive.archive(oid);
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to archive broken OrderInstanceBackup", e);
    }
  }
  
  public static class PrioritizedRootId {
    private Long rootId;
    private CountDownLatch cdl;
    
    public PrioritizedRootId(Long rootId) {
      this.rootId = rootId;
      cdl = new CountDownLatch(1);
    }
    
    public void countDown() {
      cdl.countDown();
    }

    public Long getId() {
      return rootId;
    }

    public void await() throws InterruptedException {
      cdl.await();
    }
  }
    
  public boolean isAlreadyProcessed(long rootOrderId) {
    return alreadyProcessedIds.contains(rootOrderId);
  }
  
  /**
   * 
   * @param rootOrderId
   * @return true, wenn RootOrderId prozessiert wurde
   * @throws InterruptedException 
   */
  public boolean waitFor(long rootOrderId) throws InterruptedException {
    if( isAlreadyProcessed(rootOrderId) ) {
      return true; //Es muss nicht länger gewartet werden
    }
    
    if( finished ) {
      if( isAlreadyProcessed(rootOrderId) ) {
        return true; //Es muss nicht länger gewartet werden
      } else {
        //unerwartet: rootOrderId ist nicht prozessiert worden
        //FIXME was nun
        return false;
      }
    }
    PrioritizedRootId pri = new PrioritizedRootId(rootOrderId);
    synchronized (idsWithPriority) {
      if( ! finished ) {
        idsWithPriority.add(pri);
      } else {
        return false;
      }
    }
    pri.await();
    return true;
  }



}
