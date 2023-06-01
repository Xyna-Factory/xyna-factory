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
package com.gip.xyna.xprc.xsched;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.CounterMap;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.concurrent.HashParallelReentrantLock;
import com.gip.xyna.utils.scheduler.Scheduler;
import com.gip.xyna.utils.timing.TimedTasks;
import com.gip.xyna.utils.timing.TimedTasks.Executor;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.TransientFlags;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess.XynaProcessState;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.ordercancel.ICancelResultListener;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder.ExtendedStatus;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder.WaitingCause;


/**
 * AllOrdersList ist die Liste aller Aufträge im XynaScheduler. 
 * 
 * Der SchedulerAlgorithm fragt regelmäßig nach den neuen Aufträgen und pflegt diese in einer 
 * eigenen Liste. Die eigene Liste ist auf Grund der anderen Sortierung nötig und hilfreich, 
 * um nicht immer wieder langdauernde synchronisierte Zugriffe zu haben.<br> 
 * 
 * Dies macht es jedoch auch erforderlich, dass geschedulte Orders entfernt werden. Dazu wird
 * jeder geschedulte Auftrage über {@link #removeOrder(SchedulingOrder)} aus der Liste aller 
 * Aufträge entfernt.<br> 
 * 
 * Da die interne Speicherung in AllOrdersList eine ConcurrentHashMap ist, können viele Operationen 
 * ohne Lock auskommen. Allerdings hat die ConcurrentHashMap den Nachteil, dass sie nicht mehr 
 * schrumpft, wenn sie zuviele Aufträge speichern musste.<br>
 * 
 * Zusätzlich zur Liste aller Aufträge wird noch eine weitere, kleinere Liste von SchedulingOrders 
 * gepflegt, um einen OutOfMemory-Schutz zu haben. 
 * Dies ist die {@link com.gip.xyna.xprc.xsched.AllOrdersList.OOMProtectionList OOMProtectionList}
 * Über diese Liste werden XynaOrders in der SchedulingOrder gebackupt und restoret.<br> 
 * Dadurch, dass nur alte XynaOrders gebackupt und aus dem Speicher entfernt werden und wieder 
 * nachgeladene XynaOrders als neu gelten, ist gewährleistet, dass der Scheduler selten XynaOrders 
 * mehrfach nachladen muss. Mehrfaches Nachladen sollte nur passieren, wenn Aufträge langdauernd 
 * nicht geschedult werden und die OOMProtectionList sehr voll ist.<br> 
 * Das Kriterium, ob ein Auftrag in die OOMProtectionList gelangt, ist derzeit nur, ob er 
 * Parent-Aufträge hat, siehe {@link com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder#isRelevantForOOMProtection() SchedulingOrder.isRelevantForOOMProtection()}.<br> 
 *  
 * <br><br>
 * 
 * TODO Was wäre noch gut?<br> 
 * OOMProtectionList sollte keine LinkedList sein, sondern HashMap bzw LinkedHashMap, 
 * damit die remove-Operationen günstig durchgeführt werden können. 
 * Mit Umsetzung von Bug 13829 könnte OOMProtectionList eine einfache Erweiterung von LRUCache sein
 * 
 * 
 */
public class AllOrdersList {
  
  private static Logger logger = CentralFactoryLogging.getLogger(AllOrdersList.class);

  private ConcurrentHashMap<Long,SchedulingOrder> allOrders;    //alle Orders
  private OOMProtectionList oomOrders;           //für OOM-Schutz relevante SchedulingOrder
  
  private CanceledOrders canceledOrders;         //für gleichzeitiges Add- und CancelOrder: Cancel darf vor Add gerufen werden
  
  private Scheduler<SchedulingOrder,SchedulerInformationBean> scheduler;
  
  /**
   * Liste aller SchedulingOrders, die wegen OOM überwacht werden müssen.
   * 
   * {@link #add(SchedulingOrder) } überwacht, dass nicht mehr als maxOrders Aufträge in der Liste stehen.
   * Überzählige werden gebackupt und aus der Liste entfernt. Dabei werden die ältesten Aufräge (vorne) 
   * in der Liste entfernt, neue Aufträge werden hinten angehängt.
   * 
   * Geschedulte oder gecancelte Aufträge werden nicht direkt nach dem Schedulen oder beim Cancel aus der
   * Liste entfernt, daher wächst die OOMProtectionList immer auf maxOrders an, bevor sie wieder stark 
   * zusammenschrumpft, wenn im addOrder alle geschedulten oder gecancelten Aufträge entfernt werden.
   * 
   * Grund dafür ist die höhere Effizienz einer vollständigen Iteration über die OOMProtectionList im 
   * addOrder, welche nur alle maxOrders/10 bis maxOrders (also typischerweise 1000-10000) eingestellten 
   * Aufträge auftritt im Vergleich zu einer Iteration bei jedem Cancel oder nach jedem Schedule-Vorgang.
   *
   */
  private static class OOMProtectionList {
    
    private LinkedList<SchedulingOrder> oomOrders = new LinkedList<SchedulingOrder>();
    private int maxOrders;
    private int maxOrdersAfterBackup;
    private AllOrdersList allOrdersList;
    
    public OOMProtectionList(AllOrdersList allOrdersList, int maxOrders) {
      this.allOrdersList = allOrdersList;
      if( maxOrders <= 0 ) {
        throw new IllegalArgumentException("maxOrders must be greater than 0 ");
      }
      this.maxOrders = maxOrders;
      this.maxOrdersAfterBackup = maxOrders*9/10; //etwa 90% bei einem Listen-Schrumpfen behalten
    }

    /**
     * Aufnahme der SchedulingOrder in die OOMProtectionList, wenn sie für den OOM-Schutz relevant ist
     * @param so
     */
    public void add(SchedulingOrder so) {
      if( ! so.isRelevantForOOMProtection() ) {
        return;
      }
      synchronized ( oomOrders ) {
        oomOrders.add(so); //als junger Auftrag eintragen (hinten)
        if( oomOrders.size() > maxOrders ) {
          decreaseListSize();
        }
      }
    }

    /**
     * Liste ist zu lang geworden: Zuerst Aufräumen, danach Entfernen einiger Einträge 
     * aus der OOMProtectionList, damit OOM-Schutz gewährleistet ist. XynaOrders werden 
     * gebackupt und aus dem Speicher entfernt.
     */
    private void decreaseListSize() {
      Iterator<SchedulingOrder> iter = oomOrders.iterator();
      while( iter.hasNext() ) {
        SchedulingOrder so = iter.next();
        if( so.canBeRemovedFromOOMProtection() ) {
          iter.remove();
        }
      }
      
      int ordersToBackup = Math.max( 0, oomOrders.size()-maxOrdersAfterBackup );
      int cnt = 0;
      int i = 0;
      while (cnt < ordersToBackup && i++ < oomOrders.size()) {
        SchedulingOrder so = oomOrders.remove();//holt ältesten Eintrag (vorne)
        synchronized (so) {
          if( so.isLocked() ) {
            continue; //dann halt anderen Auftrag aus dem Memory werfen
          }
          allOrdersList.lock(so);
        }
        try {
          if( so.canBeRemovedFromOOMProtection() ) {
            //Auftrag läuft nun anscheinend doch schon. (Im Scheduler wird 
            //...OrderInstanceStatus auf Running gesetzt)
            cnt++;
          } else {
            if( so.backup() ) { //TODO batch-verarbeitung für die backups
              //XynaOrder aus Memory entfernen: SchedulingOrder wird noch in anderen Listen gehalten
              so.removeXynaOrder();
              cnt++;
            } else {
              //Backup fehlgeschlagen, daher Auftrag weiter aufbewahren
              oomOrders.add(so); //als junger Auftrag eintragen (hinten)
            }
          }
        } finally {
          allOrdersList.unlock(so);
        }
      }
      if (cnt > 0) {
        if (logger.isDebugEnabled()) {
          logger.debug("decreased OOMProtectionList-size to " + oomOrders.size() + ", backuped " + cnt + " orders");
        }
      }
    }


    /**
     * Restore der XynaOrder, Wiederaufnahme in OOMProtectionList
     * @param so
     * @return
     */
    public XynaOrderServerExtension getXynaOrder(SchedulingOrder so) {
      XynaOrderServerExtension xo = so.getXynaOrderOrNull();
      if( xo == null ) {
        xo = so.restore();
        add(so); //als junger Auftrag eintragen (hinten)
      }
      return xo;
    }

  }
  
  
  /**
   * Für Tests
   * @param maxOrders
   */
  public AllOrdersList(int maxOrders) {
    init();
    initOOMProtection(maxOrders);
  }
  
  /**
   * In der Factory
   */
  public AllOrdersList() {
    init();
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(AllOrdersList.class,"AllOrdersList.initOOMProtection").
      after(XynaProperty.class).
      execAsync(new Runnable() { public void run() {
        XynaProperty.MAX_ORDERS_IN_SCHEDULER_MEMORY.registerDependency(UserType.XynaFactory, "AllOrdersList");
        int maxOrders = XynaProperty.MAX_ORDERS_IN_SCHEDULER_MEMORY.readOnlyOnce();
        initOOMProtection(maxOrders);
        } });
  }
  
  
  private void init() {
    allOrders = new ConcurrentHashMap<Long,SchedulingOrder>();
    canceledOrders = new CanceledOrders();
  }
  
  private void initOOMProtection(int maxOrders) {
    oomOrders = new OOMProtectionList(this,maxOrders);
  }
  
  
  private static class CanceledOrders implements Executor<CanceledOrders.Cancel> {
    
    private static class Cancel {

      private long orderId;
      private ICancelResultListener listener;

      public Cancel(long orderId, ICancelResultListener listener) {
        this.orderId = orderId;
        this.listener = listener;
      }

      public Cancel(long orderId) {
        this.orderId = orderId;
      }
      
      public Throwable getCause() { //TODO verwenden...
        return null;
      }
      
      
      @Override
      public boolean equals(Object obj) {
        if (obj == null) {
          return false;
        }
        if( obj instanceof Cancel ) {
          return orderId == ((Cancel)obj).orderId;
        }
        return false;
      }

      private int hash;

      @Override
      public int hashCode() {
        int h = hash;
        if (h == 0) {
          h = Long.valueOf(orderId).hashCode();
          hash = h;
        }
        return h;
      }

    }
    
    private final TimedTasks<Cancel> cancelTasks;
    private final HashParallelReentrantLock<Long> orderIdLock = new HashParallelReentrantLock<Long>(32); //Lock, mit dem 
    //konkurrierende addOrder- und cancelOrder-Aufrufe auf die gleiche orderId verhindert werden sollen

    public CanceledOrders() {
      cancelTasks = new TimedTasks<Cancel>("CancelOrderThread", this );
    }

    public void add(Long orderId, ICancelResultListener listener) {
      cancelTasks.addTask( listener.getAbsoluteCancelTimeout(), new Cancel(orderId,listener) );
    }

    
    /**
     * falls kein listener existiert, wird false zurückgegeben.
     * 
     * ansonsten signalisiert der rückgabewert, ob der aufrufer den auftrag canceln muss.
     * beispiele:
     * 1. auftrag kann gecancelt werden, aber er wird vom scheduler gescheduled (resume) -> return false, listener.cancelsucceeded
     * 2. auftrag kann gecancelt werden, soll der aufrufer machen -> return true, listener.cancelsucceeded
     * 3. auftrag kann nicht gecancelt werden -> return false, listener.cancelfailed
     * 
     * zu diesem zeitpunkt darf der auftrag noch nicht in der allorderliste sein. er wird nicht daraus entfernt!
     */
    public boolean isCanceled(XynaOrderServerExtension xo) {
      Cancel cancel = cancelTasks.removeTask(new Cancel(xo.getId()));
      if (cancel != null) {
        boolean cancelSucceeded = true;
        boolean cancelToBeExecutedByCaller = true;
        if (!cancel.listener.cancelCompensationAndResumes()) {
          if (xo.getExecutionProcessInstance() != null) {
            cancelSucceeded = false;
          }
        }

        if (xo.getExecutionProcessInstance() != null
            && (xo.getExecutionProcessInstance().getState() == XynaProcessState.SUSPENDED || xo
                .getExecutionProcessInstance().getState() == XynaProcessState.SUSPENDED_AFTER_ABORTING)) {
          //resuming
          cancelToBeExecutedByCaller = false; //macht der scheduler!          

          if (cancelSucceeded) {
            //resume darf abgebrochen werden -> das macht der scheduler, indem die capacities und vetos ausgeschaltet werden
            xo.abortResumingOrder(cancel.listener.ignoreResourcesWhenResuming(), cancel.getCause() );
          }
        }

        if (cancelSucceeded) {
          cancel.listener.callCancelSucceededAndSetSuccessFlag();
          return cancelToBeExecutedByCaller;
        } else {
          cancel.listener.cancelFailed();
        }
      }
      return false;
    }

    public void execute(CanceledOrders.Cancel work) {
      try {
        work.listener.cancelFailed(); //weil noch nicht entfernt bisher
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.warn("Execution of CancelListener.cancelFailed() failed.", t);
      }
    }

    public void handleThrowable(Throwable executeFailed) {
      logger.warn( "listener.cancelFailed() failed ", executeFailed );
    }

    public void lock(Long lockId) {
      orderIdLock.lock(lockId);
    }

    public void unlock(Long lockId) {
      orderIdLock.unlock(lockId);
    }

    public void stop() {
      cancelTasks.stop();
    }
    
  }
  
  /**
   * Berechnen der SchedulingOrder, Eintragen der SchedulingOrder in allOrders
   * @param xo
   * @param waitingCauses
   * @return
   */
  public SchedulingOrder addOrder(XynaOrderServerExtension xo, EnumSet<WaitingCause> waitingCauses) {
    if( logger.isDebugEnabled() ) {
      logger.debug("addOrder("+xo.getId()+",waitingCauses="+waitingCauses+")");
    }
    SchedulingOrder so = new SchedulingOrder(xo,waitingCauses);
    allOrders.put(so.getOrderId(),so);
    oomOrders.add(so);
    return so;
  }
  
  /**
   * Berechnen der SchedulingOrder, Eintragen der SchedulingOrder in allOrders und newOrders
   * Nur von Tests gerufen!
   * @param xo
   */
  public void addOrder(XynaOrderServerExtension xo) {
    SchedulingOrder so = addOrder( xo, EnumSet.noneOf(WaitingCause.class) );
    so.setStateAccordingToWaitingCause();
    so.markAsScheduling();
    addOrderToScheduler(so);
  }
  
  public boolean isCanceled(XynaOrderServerExtension xo) {
    canceledOrders.lock(xo.getId());
    try {
      return canceledOrders.isCanceled(xo);
    } finally {
      canceledOrders.unlock(xo.getId());
    }
  }
  
  /**
   * gibt den gecancelten auftrag zurück, falls er aus der liste entfernt wurde
   */
  public XynaOrderServerExtension cancelOrder(Long orderId, ICancelResultListener listener, boolean cancelCompensationAndResumes, boolean ignoreResourcesWhenResuming) {
    canceledOrders.lock(orderId);
    try {
      
      Pair<XynaOrderServerExtension, RemoveState> removed = removeOrder(orderId.longValue(), cancelCompensationAndResumes, ignoreResourcesWhenResuming);
      
      RemoveState removeState = removed.getSecond();
      XynaOrderServerExtension xo = removed.getFirst();
      
      if( logger.isDebugEnabled() ) {
        if( removeState == RemoveState.BeforeScheduling ) {
          logger.debug("Removed xynaOrder " + orderId + " from allOrders: " + xo);
        } else {
          logger.debug("Did not remove xynaOrder " + orderId + " from allOrders: "+removeState+".");
        }
      }
      
      switch( removeState ) {
        case BeforeScheduling:
          return xo; //Auftrag konnte abgebrochen und entfernt werden
        case AlreadyScheduled:
        case ResumeOrCompensate:
          //auftrag konnte nicht abgebrochen werden, war aber in der liste 
          //in beiden fällen soll kein listener registriert werden.
          if (listener != null) {
            listener.cancelFailed();
          }
          return null;
        case Aborted:
          // auftrag konnte nicht entfernt werden, aber abgebrochen. das ist nur für den fall:
          // er ist resuming und braucht resourcen und flag verbietet ihn zu entnehmen.
          if (listener != null) {
            listener.callCancelSucceededAndSetSuccessFlag();
          }
          return null;
        case NotFound:
          if (listener != null) {
            listener.setCancelCompensationAndResumes(cancelCompensationAndResumes);
            listener.setIgnoreResourcesWhenResuming(ignoreResourcesWhenResuming);
            canceledOrders.add(orderId, listener);
          } else {
            //Auf Cancel warten ohne Listener ist nicht so gut
          }
          return null;
        default:
          logger.error("Unexpected RemoveState "+removeState);
          if (listener != null) {
            listener.cancelFailed();
          }
          return null;
      }
    } finally {
      canceledOrders.unlock(orderId);
    }
  }

  public enum RemoveState {
    
    BeforeScheduling(true,true),   //Auftrag wartete im Scheduler und konnte daher abgebrochen werden
    AlreadyScheduled(true,false),  //Auftrag wurde bereits ausgeführt
    ResumeOrCompensate(true,false),//Resume- oder Compensate-Auftrag wird nicht abgebrochen
    Aborted(true,true),            //Auftrag während eines Resumes; wurde nun abgebrochen, um nicht auf Caps oder Vetos zu warten
    NotFound(false,false);         //unbekannter Auftrag
    
    private boolean knownToScheduler;
    private boolean removedOrAborted;
    
    private RemoveState(boolean knownToScheduler, boolean removedOrAborted) {
      this.knownToScheduler = knownToScheduler;
      this.removedOrAborted = removedOrAborted;
    }
 
    public boolean isKnownToScheduler() {
      return knownToScheduler;
    }
    
    public boolean isRemovedOrAborted() {
      return removedOrAborted;
    }
  }
  
 
  
  public XynaOrderServerExtension removeOrder(Long orderId) {
    //auftrag auch entfernen wenn er ein compensate oder resume ist
    Pair<XynaOrderServerExtension, RemoveState> p = removeOrder(orderId.longValue(), true, true);
    if( logger.isDebugEnabled() ) {
      if( p.getSecond() == RemoveState.BeforeScheduling ) {
        logger.debug("Removed xynaOrder " + orderId + " from allOrders before scheduling: " + p.getFirst());
      } else {
        logger.debug("Did not remove xynaOrder " + orderId + " from allOrders: "+p.getSecond());
      }
    }
    return p.getFirst();
  }


  /**
   * entfernt den auftrag aus der allorderslist, falls vorhanden und falls er entfernt werden darf (bei compensates und resumes)
   * und falls er nicht bereits gescheduled wurde.
   * wenn ignoreResourcesWhenResuming = false, dann soll der resuming auftrag im scheduler bleiben auch wenn removeResumes=true
   * 
   * gibt die xynaorder zurück, falls abort erfolgreich, und true/false ob der auftrag entfernt wurde.
   */
  public Pair<XynaOrderServerExtension, RemoveState> removeOrder(long orderId, boolean removeCompensatingAndResumes, boolean ignoreResourcesWhenResuming) {
    SchedulingOrder so = allOrders.get(orderId);
    if( so == null ) {
      return Pair.of(null, RemoveState.NotFound);
    }
    XynaOrderServerExtension xo = getXynaOrder(so);
    lock(so); //Kann nicht gleichzeitig gescheduled werden
    try { 
      if( so.isAlreadyScheduled() ) {
        //bereits geschedult.
        return Pair.of(null, RemoveState.AlreadyScheduled);
      }

      //Order nur entfernen, wenn man sicher ist, dass man das will. weil man hinterher nicht gut zurück kann
      //Daher nun erst einige Prüfungen

      //nun kann man threadsicher in die xynaorder reinschauen:
      XynaProcess process = xo.getExecutionProcessInstance();
      if( process != null ) {
        //da Order bereits einen XynaProcess kennt, muss dies ein Compensate oder Resume sein
        
        if (!removeCompensatingAndResumes ) {
          //=> nicht entfernen!
          return Pair.of(null, RemoveState.ResumeOrCompensate);
        }
        
        boolean isResuming = process.getState() == XynaProcessState.SUSPENDED || process.getState() == XynaProcessState.SUSPENDED_AFTER_ABORTING;
        if (!ignoreResourcesWhenResuming && isResuming ) {
          SchedulingData schedulingData = xo.getSchedulingData();
          if ( schedulingData.needsCapacities() || ! schedulingData.getVetos().isEmpty() ) {
            //auftrag benötigt capacities oder vetos und diese sollen beim resume nicht ignoriert werden
            //abgebrochen soll der auftrag trotzdem werden!
            xo.abortResumingOrder(false, null); //TODO cause
            return Pair.of(xo, RemoveState.Aborted);
          }
          //else auftrag entfernen, das abort macht der caller //TODO wieso?
        }
      }

      //Als removed markieren, damit SchedulerAlgorithm die SchedulingOrder ebenfalls entfernt
      so.markAsRemoved();
      
      if( so.isWaitingFor(WaitingCause.StartTime) ) {
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getTimeConstraintManagement().removeOrder(so.getOrderId());
      }
      
    } finally {
      unlock(so);
    }
    allOrders.remove(orderId);
    so.removeXynaOrder();
    return Pair.of(xo, RemoveState.BeforeScheduling);
  }


  public int size() {
    return allOrders.size();
  }

  /**
   * Entfernen einer geschedulten SchedulingOrder
   * @param so
   */
  public void removeOrder(SchedulingOrder so) {
    so.removeXynaOrder();
    allOrders.remove(so.getOrderId());
  }

  public List<XynaOrderServerExtension> getAllNotBackupedOrders() {
    return CollectionUtils.transformAndSkipNull(allOrders.values(), SchedulingOrder.xynaOrdersNotBackuped );
  }
  
  public List<XynaOrderInfo> getSchedulingOrders() {
    return CollectionUtils.transformAndSkipNull(allOrders.values(), SchedulingOrder.ordersScheduling );
  }
  
  public List<XynaOrderInfo> getWaitingForSeries() {
    return CollectionUtils.transformAndSkipNull(allOrders.values(), SchedulingOrder.ordersWaitingForSeries );
  }
  
  public List<XynaOrderInfo> getWaitingForStartTime() {
    return CollectionUtils.transformAndSkipNull(allOrders.values(), SchedulingOrder.ordersWaitingForStartTime );
  }
  
  public List<XynaOrderInfo> getAllOrders() {
    return CollectionUtils.transformAndSkipNull(allOrders.values(), SchedulingOrder.allOrders );
  }
  
  public List<XynaOrderInfo> getOrdersInRuntimeContext(RuntimeContext runtimeContext) {
    return CollectionUtils.transformAndSkipNull(allOrders.values(), SchedulingOrder.allOrdersInRuntimeContext(runtimeContext) );
  }

  public List<XynaOrderInfo> getOrdersInRevision(long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RuntimeContext runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    return getOrdersInRuntimeContext(runtimeContext);
  }

  public List<XynaOrderInfo> getRootOrders() {
    return CollectionUtils.transformAndSkipNull(allOrders.values(), SchedulingOrder.rootOrders );
  }

  public List<SchedulingOrder> getFilteredOrders( Transformation<SchedulingOrder, SchedulingOrder> filter ) {
    return CollectionUtils.transformAndSkipNull(allOrders.values(), filter );
  }
  
  /**
   * AllOrders ist dafür veranwortlich, die XynaOrders fürs Scheduling zu liefern
   * @param so
   * @return XynaOrderServerExtension, evtl. null, falls restore aus Orderbackup nicht klappt!
   */
  public XynaOrderServerExtension getXynaOrder(SchedulingOrder so) {
    XynaOrderServerExtension xo = so.getXynaOrderOrNull();
    if( xo != null  ) {
      //XynaOrder ist einfach erreichbar
      return xo;
    }
    //Nachlesen aus dem Backup
    return oomOrders.getXynaOrder(so);
  }

  /**
   * 
   */
  public void stop() {
    logger.debug("Stopping cancel maintenance thread");
    canceledOrders.stop();
  }


  public SchedulingOrder getSchedulingOrder(Long orderId) {
    return allOrders.get(orderId);
  }


  public boolean startTimeReached(SchedulingOrder so, boolean recalculateUrgency) {
    synchronized (so) {
      boolean removed = so.removeWaitingCause( WaitingCause.StartTime );
      return tryScheduleOrder( so, "startTimeReached", removed, recalculateUrgency, recalculateUrgency);
    }
  }  
  
  public boolean seriesCompleted(SchedulingOrder so) {
    synchronized (so) {
      boolean removed = so.removeWaitingCause( WaitingCause.Series );
      return tryScheduleOrder( so, "seriesCompleted", removed, false, false);
    }
  }
  
  public boolean continueBatchProcess(SchedulingOrder so, boolean recalculateUrgency) {
    synchronized (so) {
      boolean removed = so.removeWaitingCause( WaitingCause.BatchProcess );
      return tryScheduleOrder( so, "continueBatchProcess", removed, recalculateUrgency, recalculateUrgency);
    }
  }
  
  public void timeoutOrder(SchedulingOrder so) {
    synchronized(so) {
      so.markAsTimedout();
      //Reschedule trägt Auftrag evtl wieder in Scheduler ein, wenn SchedulingOrder außerhalb aufbewahrt wird
      tryScheduleOrder( so, "timeoutOrder", false, false, true );
    }
  }
  
  public boolean reschedule(SchedulingOrder so, boolean recalculateUrgency) {
    synchronized (so) {
      return tryScheduleOrder( so, "reschedule", false, recalculateUrgency, true);
    }
  }
  
  public void reorder(SchedulingOrder so) {
    synchronized (so) {
      //SchedulingOrder muss neu in Scheduler reinkommen
      tryScheduleOrder( so, "reorder", true, true, false);
    }
  }
  
  public void scheduleOrder(SchedulingOrder so) {
    synchronized (so) {
      //allererster Eintrag in Scheduler: Status von New auf Scheduling umtragen
      so.markAsScheduling();
      tryScheduleOrder( so, "scheduleOrder", true, true, false);
    }
  }

  /**
   * Versucht, den Auftrag in den Scheduler einzustellen
   * Dies kann scheitern, wenn noch ein anderer Wartegrund vorliegt
   * @param so
   * @param msg
   * @param addToScheduler
   * @param recalculateUrgency
   * @param reschedule
   * @return true, wenn Auftrag wieder geschedult wird; 
   *         false, wenn Auftrag nicht mehr laufen kann oder noch warten muss
   */
  private boolean tryScheduleOrder(SchedulingOrder so, String msg, boolean addToScheduler, boolean recalculateUrgency, boolean reschedule) {
    //WaitingCause ist bereits korrekt, daher State anpassen
    so.setStateAccordingToWaitingCause();
    if( so.isMarkedAsNew() ) {
      return false;
    } else if( so.isWaitingOrLocked() ) {
      if( so.isLocked() ) {
        //SchedulingOrder ist gesperrt: daher keine weiteren Änderungen vornehmen sondern fürs Unlock vormerken.
        if( addToScheduler ) {
          so.addLockAction(WaitingCause.Unlock_ReaddToScheduler);
        }
        if( recalculateUrgency ) {
          so.addLockAction(WaitingCause.Unlock_RecalculateUrgency);
        }
        if( reschedule ) {
          so.addLockAction(WaitingCause.Unlock_Reschedule);
        }
        return false;
      } else {
        if( logger.isDebugEnabled() ) {
          logger.debug( msg+" "+so+": can not be scheduled due to remaining WaitingCause "+so.getWaitingCauses());
        }
        return false;
      }
    } else if( ! so.canBeScheduled() ) {
      if( so.isAlreadyScheduled() ) {
        if( so.isMarkedAsTimedout() ) {
          //Dies ist so zu erwarten, da zwei Threads das Timeout entdecken: a) TimeConstraintExecutor.SchedulingTimeout 
          //b) TryScheduleAbstract.TimeConstraint. In diesem Fall hier war der Scheduler mit b) schneller
        } else {
          if( logger.isDebugEnabled() ) {
            logger.debug("tryScheduleOrder A "+so+" is already scheduled");
          }
        }
      } else {
        //Auftrag kann nicht regulär geschedult werden, deswegen Scheduler zum Aufräumen geben
        rescheduleOrder(so);
      }
      return false;
    } else {
      if( addToScheduler ) {
        //Auftrag kommt (wieder) neu in den Scheduler
        addOrderToScheduler(so);
      } else if( reschedule ) {
        //Auftrag verbleibt im Scheduler, wird aber neu einsortiert
        //Dies macht Last im Scheduler...
        rescheduleOrder(so);
      } else {
        //Auftrag verbleibt im Scheduler
      }
      return true;
    }
  }
  
  private void addOrderToScheduler(SchedulingOrder so) {
    XynaOrderServerExtension xo = getXynaOrder(so); //hier schon holen, damit kein 
    //.. Backup während newOrdersLock gelesen werden muss
    xo.setTransientFlag(TransientFlags.WasKnownToScheduler);
   // scheduler.putOrder(so); //FIXME blockierend einstellen
    scheduler.offerOrder(so);
    
    notifyScheduler();
    if( logger.isDebugEnabled() ) {
      logger.debug( "adding "+so+" to scheduler" );
    }
  }
  
  /**
   * Auftrag soll im Scheduler neu einsortiert werden (kurz entfernt und als neu eingetragen).
   * Der Scheduler wird notified.
   * @param so
   */
  private void rescheduleOrder(SchedulingOrder so) {
    scheduler.offerReorderOrder(so); //FIXME
    notifyScheduler();
  }
  
  

  private void notifyScheduler() {
    XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler(); //TODO besser
  }

  /**
   * Update des OrderInstanceStatus sowohl in SchedulingOrder als auch im OrderArchive
   * TODO OrderArchive-Update fehlt noch! 
   * @param so
   * @param orderInstanceStatus null ist erlaubt -&gt; keine Änderung
   */
  public void updateOrderStatus(SchedulingOrder so, OrderInstanceStatus orderInstanceStatus) {
    if( orderInstanceStatus == null ) {
      return; //nichts zu tun
    }
    so.setOrderStatus(orderInstanceStatus);
    //
  }

  public void listExtendedInfo(StringBuilder sb) {
    CounterMap<String> counter = new CounterMap<String>();
    for( SchedulingOrder so : allOrders.values() ) {
      counter.increment( so.getExtendedStatus()+"-"+so.getOrderStatus());
    }
    sb.append( "Scheduling status of ").append(counter.getTotalCount()).append(" orders in scheduler:\n");
    for( ExtendedStatus es : ExtendedStatus.values() ) {
      for( OrderInstanceStatus ois : OrderInstanceStatus.values() ) {
        String key = es+"-"+ois;
        if( counter.containsKey(key) ) {
          sb.append("  ").append(es.name()).append(", ").append(ois.getName()).append(": ").append(counter.getCount(key)).append("\n");
        }
      }
    }
  }
  
  public XynaOrderServerExtension waitForDeployment(Long orderId) { 
    SchedulingOrder so = allOrders.get(orderId);
    if( so == null ) {
      return null; //Order nicht gefunden
    }
    synchronized (so) {
      so.waitIfLocked();
      if( so.isMarkedAsRemove() ) {
        return null;
      }
      so.addWaitingCause( WaitingCause.Deployment );
    }
    return so.getXynaOrderOrNull(); //kann null sein wegen OOM
  }
  
  public void deploymentFinished(Long orderId) {
    SchedulingOrder so = allOrders.get(orderId);
    if( so == null ) {
      return; //Order nicht gefunden, sollte nicht auftreten können
    }
    synchronized (so) {
      so.waitIfLocked();
      so.removeWaitingCause( WaitingCause.Deployment );
    }
  }

  public int changeCapacityRequirement(DestinationKey destinationKey, boolean add, String capName, int cardinality) {
    List<SchedulingOrder> affected = getFilteredOrders( new DestinationKeyFilter(destinationKey) );
    if( affected.size() == 0 ) {
      return 0; //nichts zu tun
    }
    int modified = 0;
    for( SchedulingOrder so : affected ) {
      synchronized (so) { //gegen gleichzeitiges Schedulen geschützt
        so.waitIfLocked();
        if( !so.canBeScheduled() ) {
          continue; //zu spät, Auftrag kann nicht mehr geschedult werden
        }
        SchedulingData sd = getXynaOrder(so).getSchedulingData();
        if( add ) {
          if( sd.addOrChangeCapacity(capName,cardinality) ) {
            ++modified;
          }
        } else {
          if( sd.removeCapacity(capName) ) {
            ++modified;
            //Evtl. ist nun der Wartegrund im Scheduler weg. Daher reschedulen
            rescheduleOrder(so);
          }
        }
      }
    }
    return modified;
  }


  
  public static class DestinationKeyFilter implements Transformation<SchedulingOrder, SchedulingOrder> {

    private DestinationKey destinationKey;

    public DestinationKeyFilter(DestinationKey destinationKey) {
      if( destinationKey == null ) {
        throw new IllegalArgumentException("destinationKey may not be null");
      }
      this.destinationKey = destinationKey;
    }

    public SchedulingOrder transform(SchedulingOrder from) {
      if( destinationKey.equals(from.getDestinationKey()) ) {
        return from;
      }
      return null;
    }
    
  }

  /**
   * Achtung: nicht reentrant!
   */
  public void lock(SchedulingOrder so) {
    synchronized (so) {
      so.waitIfLocked();
      so.addLockAction(WaitingCause.Locked);
    }
    if( logger.isDebugEnabled() ) {
      logger.debug("SchedulingOrder "+so+" is locked");
    }
  }

  public void unlock(SchedulingOrder so) {
    synchronized (so) {
      if( ! so.isLocked() ) {
        throw new IllegalStateException("SchedulingOrder is not locked");
      }
      so.removeLockAction(WaitingCause.Locked);
      boolean addToScheduler = so.removeLockAction(WaitingCause.Unlock_ReaddToScheduler);
      boolean recalculateUrgency = so.removeLockAction(WaitingCause.Unlock_RecalculateUrgency);
      boolean reschedule = so.removeLockAction(WaitingCause.Unlock_Reschedule);
      
      if( addToScheduler || recalculateUrgency || reschedule ) {
        tryScheduleOrder(so, "unlock SchedulingOrder", addToScheduler, recalculateUrgency, reschedule);
      }
      if( logger.isDebugEnabled() ) {
        logger.debug("SchedulingOrder "+so+" is unlocked");
      }
      so.notifyAll();
    }
  }

  public void setScheduler(Scheduler<SchedulingOrder,SchedulerInformationBean> scheduler) {
    this.scheduler = scheduler;
  }
  
}
