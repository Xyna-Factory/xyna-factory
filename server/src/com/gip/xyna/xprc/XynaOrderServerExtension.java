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

package com.gip.xyna.xprc;



import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OrderInputCreationInstanceWithSeriesInfo;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessMarker;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess.XynaProcessState;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.cleanup.CleanupDispatcher;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler;
import com.gip.xyna.xprc.xpce.planning.MasterWorkflowPreScheduler;
import com.gip.xyna.xprc.xpce.planning.PlanningDispatcher;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.BackupAction;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupMode;
import com.gip.xyna.xprc.xsched.SchedulerBean;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.XynaScheduler;



public class XynaOrderServerExtension extends XynaOrder {


  private static final long serialVersionUID = -1870498391149324074L;

  private XynaOrderServerExtension parentOrder = null;
  private Integer parentStepNo = null;

  /*
   * Misc
   */
  private SchedulerBean schedulerBean;

  /**
   * Is used to identify unserializable field content during updates
   */
  public static final String EXECUTION_WF_FIELD_NAME = "executionWF";
  private transient XynaProcess executionWF;

  private Redirection redirection = null;
  private MiscellaneousDataBean dataBean;
  private transient GeneralXynaObject modifiedInputPayload = null;

  private transient List<BackupAction> additionalBackupActions;
  
  protected transient EnumMap<ProcessingStage, Collection<XynaException>> processingStageErrors;


  // FIXME typo aquire -> acquire all over the place
  //beim serverstart wird das flag resettet und auf false gestellt (von {@link XynaProcessing.startPersistedSuspendedOrders()})
  @Deprecated //nun in SchedulingData
  private volatile boolean hasAquiredCapacities = false;
  
  
  public enum TransientFlags {
    /**
     * XynaOrder ist dem Scheduler bekannt: Sicherung der �bergabe der XynaOrder von
     * MasterWorkflowPreScheduler.startOrder in den XynaScheduler.addOrderIntoAllOrdersEtc
     */
    WasKnownToScheduler,
    /**
     * nur im scheduler verwendet.
     */
    BackuppedInScheduler,
    /**
     * wird immer auf false gestellt, wenn sich etwas �ndert, was ein backup sinnvoll macht
     */
    BackuppedAfterChange,
    /**
     * wird nur einmal auf true gestellt nach einem backup - dann ist die entscheidung einfach, ob der auftrag aus dem
     * backup gel�scht werden muss
     */
    BackuppedAtLeastOnce,
    
    /**
     * die XynaOrder ist Teil einer Auftragsserie, konnte aber nicht ins OrderSeriesManagement
     * eingestellt werden, weil bereits eine SeriesInformation mit derselben
     * correlationId vorhanden ist
     */
    DuplicateSeriesCorrelationId;
  }
  
  private transient EnumSet<TransientFlags> transientFlags = EnumSet.noneOf(TransientFlags.class);
  
  private volatile transient long idOfLatestDeploymentKnownToOrder; //not meant to be stored
  
  private List<XynaOrderServerExtension> childOrders = new ArrayList<XynaOrderServerExtension>();
  private boolean informStateTransitionListeners = true; //statuschangelistener cached hiermit, ob die statuschangelistener �berpr�fungen weggelassen werden k�nnen

  private volatile boolean isSuspended = false;
  private volatile boolean isSuspendedOnShutdown = false;
  private volatile boolean isAttemptingSuspension = false;
  
  private volatile ProcessAbortedException abortionException = null;

  @Deprecated //nun in SchedulingData
  private volatile boolean needsToAquireCapacitiesOnNextScheduling = true;
  @Deprecated //nun in SchedulingData
  private volatile boolean needsToAquireVetosOnNextScheduling = true;
  
  private volatile transient boolean needsToBeBackupedOnSuspensionOfParent = false;
  
  private transient boolean acknowledgeSuccessfullyExecuted = false;

  
  private boolean letOrderAbort = false;
  private boolean letOrderCompensateAfterAbort = true;
  
  // default: unsafe!
  private OrderStartupMode orderStartupMode = OrderStartupMode.UNSAFE;
  private Long executionTimeoutTimestamp;
  
  private String parentLaneId; //falls Auftrag ein Subworkflow ist: in dieser Lane im Parent wurde er gestartet

  private BatchProcessMarker batchProcessMarker = null;
  
  private volatile boolean monitoringLevelAlreadyDiscovered = false;
  
  public enum ExecutionType {

    UNKOWN("Unknown"), XYNA_FRACTAL_WORKFLOW("Xyna Fractal Workflow"), JAVA_DESTINATION("Java Destination"), SERVICE_DESTINATION(
        "Service Destination");

    private final String typeAsString;

    private ExecutionType(String s) {
      this.typeAsString = s;
    }

    public String getTypeAsString() {
      return this.typeAsString;
    }


    public static ExecutionType getByTypeString(String typeString) {
      for (ExecutionType type : values()) {
        if (type.getTypeAsString().equals(typeString)) {
          return type;
        }
      }
      throw new IllegalArgumentException("Unknown execution destination type: '" + typeString + "'");
    }

  }


  private transient ExecutionType executionType = ExecutionType.UNKOWN;



  public XynaOrderServerExtension(DestinationKey dk, GeneralXynaObject... payload) {
    super(dk, payload);    
  }
  
  public XynaOrderServerExtension() {
    //f�r storable ben�tigt
  }

  public XynaOrderServerExtension(XynaOrder xo) {
    super(xo,true);
    //flache Kopie nun noch etwas modifizieren
    if( getSeriesInformation() != null ) {
      getSeriesInformation().changeParent(this);
    }
    handleOrderExecutionTimeout();
    setRevision(xo.revision);
  }
  
  
  public XynaOrderServerExtension(XynaOrderCreationParameter xocp) {
    this(xocp, idgen.getUniqueId());
  }

  public XynaOrderServerExtension(XynaOrderCreationParameter xocp, long orderId) {
    super(xocp, orderId);
    this.idOfLatestDeploymentKnownToOrder = xocp.getIdOfLatestDeploymentKnownToOrder();
    this.dataBean = xocp.getDataBean();
    handleOrderExecutionTimeout();
  }

  public String toStringWithChildIds() {
    if( parentOrder == null ) {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toString()).append(" root with family [" );
      String sep = "";
      for( XynaOrderServerExtension xo : getOrderAndChildrenRecursively() ) {
        sb.append(sep).append(xo.getId());
        sep = ", ";
      }
      sb.append("]");
      return sb.toString();
    } else {
      return super.toString();
    }
  }
  
  
  
  private void handleOrderExecutionTimeout() {
    if(getOrderExecutionTimeout() != null) {
      // ermitteln des absoluten Timestamp, an dem Auftrag mit Timeout beendet wird
      executionTimeoutTimestamp = System.currentTimeMillis();
      executionTimeoutTimestamp += getOrderExecutionTimeout().getRelativeTimeoutForNowIn(TimeUnit.MILLISECONDS);
      
      // umwandeln des relativen ExecutionTimeout in einen absoluten
      if (getOrderExecutionTimeout() instanceof ExecutionTimeoutConfiguration.ExecutionTimeoutConfigurationRelative) {
        long timeout = System.currentTimeMillis() + getOrderExecutionTimeout()
                        .getRelativeTimeoutForNowIn(TimeUnit.MILLISECONDS);
        ExecutionTimeoutConfiguration absoluteExecutionTimeout = ExecutionTimeoutConfiguration
                        .generateAbsoluteExecutionTimeout(timeout, TimeUnit.MILLISECONDS);
        setOrderExecutionTimeout(absoluteExecutionTimeout);
      }
      
      // ggf. Scheduling-Timeout anpassen, wenn Scheduling-Timeout gr��er oder nicht gesetzt
      Long schedulingTimeout = schedulingData.getTimeConstraintData().getSchedulingTimeout();
      if (schedulingTimeout == null || schedulingTimeout > executionTimeoutTimestamp) {
        setSchedulingTimeout(executionTimeoutTimestamp);
      }
    }
  }
  
  
  public void calculateExecutionTimeoutFromWorkflowTimeout() {
    if(getWorkflowExecutionTimeout() != null) {
      if (getOrderExecutionTimeout() != null) {
        long orderTimeout = getOrderExecutionTimeout().getRelativeTimeoutForNowIn(TimeUnit.MILLISECONDS);
        long workflowTimeout = getWorkflowExecutionTimeout().getRelativeTimeoutForNowIn(TimeUnit.MILLISECONDS);

        // if the orderTimeout is sooner than the workflowTimeout, then we do not need to do anything
        if (workflowTimeout > orderTimeout) {
          return;
        }
      }
      
      // ermitteln des absoluten Timestamp, an dem Auftrag mit Timeout beendet wird
      executionTimeoutTimestamp = System.currentTimeMillis();
      executionTimeoutTimestamp += getWorkflowExecutionTimeout().getRelativeTimeoutForNowIn(TimeUnit.MILLISECONDS);
      
      // umwandeln des relativen ExecutionTimeout in einen absoluten
      if (getWorkflowExecutionTimeout() instanceof ExecutionTimeoutConfiguration.ExecutionTimeoutConfigurationRelative) {
        long timeout = System.currentTimeMillis() + getWorkflowExecutionTimeout()
                        .getRelativeTimeoutForNowIn(TimeUnit.MILLISECONDS);
        ExecutionTimeoutConfiguration absoluteExecutionTimeout = ExecutionTimeoutConfiguration
                        .generateAbsoluteExecutionTimeout(timeout, TimeUnit.MILLISECONDS);
        setOrderExecutionTimeout(absoluteExecutionTimeout);
      }
      
      // ggf. Scheduling-Timeout anpassen, wenn Scheduling-Timeout gr��er oder nicht gesetzt
      // f�r rescheduling bei z.b. suspend/resume
      Long schedulingTimeout = schedulingData.getTimeConstraintData().getSchedulingTimeout();
      if (schedulingTimeout == null || schedulingTimeout > executionTimeoutTimestamp) {
        setSchedulingTimeout(executionTimeoutTimestamp);
      }
    }
  }
  

  public void setSchedulerBean(SchedulerBean sb) {
    schedulerBean = sb;
    schedulingData.setSchedulerBean(sb);
  }

  /**
   * @deprecated data from SchedulerBean is now stored in SchedulingData
   */
  @Deprecated
  public SchedulerBean getSchedulerBean() {
    return schedulerBean;
  }


  public void setMonitoringLevel(Integer monitoringLevel) {
    this.monitoringCode = monitoringLevel;
  }

  public void setEntranceTimestamp( long timestamp ) {
    this.entranceTimestamp = timestamp;
    this.schedulingData.setEntranceTimestamp(timestamp);
  }
  
  /**
   * @deprecated use setTimeConstraint( TimeConstraint.at( now+5000 )
   */
  @Deprecated
  public void setEarliestStartTimestamp( long timestamp ) {
    schedulingData.setTimeConstraint( SchedulingData.legacyTimeConstraintFor(entranceTimestamp, timestamp, schedulingTimeout ) );
  }
  
  /**
   * @deprecated use setTimeConstraint( TimeConstraint.delayed( 5000 )
   */
  @Deprecated
  public void setEarliestStartTimestampRelativeToNow( long delay ) {
    long earliestStartTimestamp = System.currentTimeMillis()+delay;
    schedulingData.setTimeConstraint( SchedulingData.legacyTimeConstraintFor(entranceTimestamp, earliestStartTimestamp, schedulingTimeout ) );
  }
  
  /**
   * @deprecated use setTimeConstraint( TimeConstraint.delayed( 5000, TimeUnit.SECONDS )
   */
  @Deprecated
  public void setEarliestStartTimestampRelativeToNow( long duration, TimeUnit timeUnit, boolean relative ) {
    long earliestStartTimestamp = (relative? System.currentTimeMillis() : 0 ) + timeUnit.toMillis(duration);
    schedulingData.setTimeConstraint( SchedulingData.legacyTimeConstraintFor(entranceTimestamp, earliestStartTimestamp, schedulingTimeout ) );
  }
 
/**
   * speichert die process instance der executionphase zum auftrag. ben�tigt zb f�r subworkflow aufrufe um sp�ter
   * kompensieren zu k�nnen
   * 
   * @param p
   */
  public void setExecutionProcessInstance(XynaProcess p) {
    executionWF = p;
  }


  public XynaProcess getExecutionProcessInstance() {
    return executionWF;
  }

  /**
   * R�ckgabe true: Auftrag wurde noch nie geschedult
   * TODO dies muss f�r JavaDestination evtl. verbessert werden
   * @return
   */
  public boolean wasNeverScheduled() {
    return executionWF == null;
  }

  
  
  public boolean hasParentOrder() {
    return parentOrder != null;
  }


  public void setParentOrder(XynaOrderServerExtension parentOrder) {
    this.parentOrder = parentOrder;
    if (parentOrder == null) {
      return;
    }
    parentOrder.setHasBeenBackuppedAfterChange(false);
    parentOrder.addChildOrder(this);
    this.setSessionId(parentOrder.getSessionId());
    rootRevision = parentOrder.rootRevision;
  }


  public XynaOrderServerExtension getParentOrder() {
    return parentOrder;
  }


  /**
   * FIXME: dieser parameter wird nicht benutzt?
   * falls ein subauftrag. intern verwendete id f�rs processmonitoring.
   * 
   * @param parentStepNo
   */
  public void setParentStepNo(Integer parentStepNo) {
    this.parentStepNo = parentStepNo;
  }


  public Integer getParentStepNo() {
    return parentStepNo;
  }


  public void setInformStateTransitionListeners(boolean b) {
    this.informStateTransitionListeners = b;
  }


  public boolean getInformStateTransitionListeners() {
    return informStateTransitionListeners;
  }


  /**
   * wird vor dem starten des auftrags gesetzt
   * 
   * @param rl
   */
  public void setResponseListener(ResponseListener rl) {
    responseListener = rl;
  }

  
  /**
   * only call this method when having the lock the scheduler needs for scheduling orders.
   * @param timedOut
   */
  public void setTimedOut(boolean timedOut) {
    this.timedOut = timedOut;
  }


  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
  

  public void clearErrors() {
    errors.clear();
  }
  
  
  public Redirection getRedirection() {
    return redirection;
  }

  
  public void setRedirection(Redirection redirection) {
    setHasBeenBackuppedAfterChange(false);
    setNeedsToBeBackupedOnSuspensionOfParent(true);
    this.redirection = redirection;
  }


  public static Long getThreadLocalRootRevision() {
    RootRevisionHolder rrh = rootRevisionTL.get();
    if (rrh.stack == 0) {
      rootRevisionTL.remove();
      return null;
    }
    return rrh.rootRevision;
  }

  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    //Daten sind in SchedulingData umgezogen, m�ssen aber aus Kompatibilit�tsgr�nden noch aus XynaOrder in readObject gelesen werden.
    //Dehalb m�ssen sie hier auch richtig geschrieben werden
    hasAquiredCapacities = schedulingData.isHasAcquiredCapacities();
    needsToAquireCapacitiesOnNextScheduling = schedulingData.isNeedsToAcquireCapacitiesOnNextScheduling();
    needsToAquireVetosOnNextScheduling = schedulingData.isNeedsToAcquireVetosOnNextScheduling();
    
    synchronized (childOrders) {
      s.defaultWriteObject();
    }
    s.writeObject(new SerializableClassloadedObject(executionWF, revision));
    s.writeObject(new SerializableClassloadedXynaObject(modifiedInputPayload));
  }


  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    if (childOrders == null) {
      childOrders = new ArrayList<>();
    }

    RootRevisionHolder rrh = rootRevisionTL.get();
    rrh.set(rootRevision);
    try {
      executionWF = (XynaProcess) ((SerializableClassloadedObject) s.readObject()).getObject(this, EXECUTION_WF_FIELD_NAME);
      executionType = ExecutionType.UNKOWN;
      modifiedInputPayload = ((SerializableClassloadedXynaObject) s.readObject()).getXynaObject();
      if (schedulerBean != null) {
        schedulingData.setSchedulerBean(schedulerBean);
      }
      transientFlags = EnumSet.noneOf(TransientFlags.class);
      restoreProcessingStageMapping();

      //siehe writeObject
      schedulingData.setHasAcquiredCapacities(hasAquiredCapacities);
      schedulingData.setNeedsToAcquireCapacitiesOnNextScheduling(needsToAquireCapacitiesOnNextScheduling);
      schedulingData.setNeedsToAcquireVetosOnNextScheduling(needsToAquireVetosOnNextScheduling);
    } finally {
      if (rrh.remove()) {
        rootRevisionTL.remove();
      }
    }
  }
  
  private Object readResolve() throws ObjectStreamException {
    if (parentOrder == null) {
      orderFamily = new ConcurrentHashMap<Long, XynaOrderServerExtension>(4, 0.75f, 2);
      List<XynaOrderServerExtension> l = new ArrayList<XynaOrderServerExtension>();
      addOrderAndChildrenRecursivelyInternal(l);
      for (XynaOrderServerExtension xo : l) {
        orderFamily.put(xo.getId(), xo);
      }
    }
    return this;
  }

  public void cleanup() {
    redirection = null;    
    //workflowinstance nullen, sobald die nicht mehr gebraucht wird
    //(aufpassen: wird f�r compensation ben�tigt und kann deshalb nicht sofort entfernt werden)
    if (!hasParentOrder()) {
      nullResponseListenersRecursively();
    }
  }
  
  private void nullResponseListenersRecursively() {
    for (XynaOrderServerExtension xo : getOrderAndChildrenRecursively()) {
      xo.responseListener = null;
    }
  }


  public void setExecutionType(ExecutionType executionType) {
    this.executionType = executionType;
  }


  public ExecutionType getExecutionType() {
    return executionType;
  }


  private void addOrderAndChildrenRecursivelyInternal(List<XynaOrderServerExtension> list) {
    list.add(this);
    for (XynaOrderServerExtension child : getDirectChildOrders() ) {
      child.addOrderAndChildrenRecursivelyInternal(list);
    }
  }


  public XynaOrderServerExtension getOrderInFamilyById(long id) {
    Map<Long, XynaOrderServerExtension> f = getRootOrder().orderFamily;
    if (f == null) {
      if (id == getId()) {
        return this;
      } else {
        return null;
      }
    } else {
      return f.get(id);
    }
  }


  private void initOrderFamily() {
    if (orderFamily == null) {
      synchronized (this) {
        if (orderFamily == null) {
          Map<Long, XynaOrderServerExtension> tmpOrderFamily = new ConcurrentHashMap<Long, XynaOrderServerExtension>(4, 0.75f, 2);
          tmpOrderFamily.put(id, this);
          // assignment to volatile variable only after it has been fully constructed (XBE-256)
          orderFamily = tmpOrderFamily;
        }
      }
    }
  }


  public List<XynaOrderServerExtension> getOrderAndChildrenRecursively() {
    if (parentOrder == null) {
      initOrderFamily();
      return new ArrayList<XynaOrderServerExtension>(orderFamily.values());
    }
    List<XynaOrderServerExtension> ret = new ArrayList<XynaOrderServerExtension>();
    addOrderAndChildrenRecursivelyInternal(ret);
    return ret;
  }

  private transient volatile Map<Long, XynaOrderServerExtension> orderFamily;

  private void addChildOrder(XynaOrderServerExtension childOrder) {
    synchronized (childOrders) {
      childOrders.add(childOrder);
    }
    if (parentOrder == null) {
      initOrderFamily();
      orderFamily.put(childOrder.getId(), childOrder);
    } else {
      Map<Long, XynaOrderServerExtension> m = getRootOrder().orderFamily;
      m.put(childOrder.getId(), childOrder);
    }
  }


  public List<XynaOrderServerExtension> getDirectChildOrders() {
    if (childOrders == null || childOrders.size() == 0) {
      return Collections.emptyList();
    }
    synchronized (childOrders) {
      return new ArrayList<XynaOrderServerExtension>(childOrders); // schutz gegen concurrentmodification
    }
  }


  public void setSuspended(boolean isSuspended) {
    this.isSuspended = isSuspended;
  }


  public boolean isSuspended() {
    return isSuspended;
  }
  
  
  public void setSuspendedOnShutdown(boolean isSuspendedOnShutdown) {
    this.isSuspendedOnShutdown = isSuspendedOnShutdown;
  }


  public boolean isSuspendedOnShutdown() {
    return isSuspendedOnShutdown;
  }


  public void setAttemptingSuspension(boolean attemptingSuspension) {
    this.isAttemptingSuspension = attemptingSuspension;
  }


  public boolean isAttemptingSuspension() {
    return isAttemptingSuspension;
  }
  
  
  public void setAbortionException(ProcessAbortedException abortionException) {
    this.abortionException = abortionException;
  }


  public ProcessAbortedException getAbortionException() {
    return abortionException;
  }


  public boolean isAborted() {
    return abortionException != null;
  }


  public OrderContextServerExtension getOrderContext() {
    return (OrderContextServerExtension) super.getOrderContext();
  }


  public void setOrderContext(OrderContext ctx) {
    if (!(ctx instanceof OrderContextServerExtension)) {
      ctx = new OrderContextServerExtension(this);
    }
    this.ctx = ctx;
  }

  public void setNewOrderContext() {
    ctx = new OrderContextServerExtension(this);
  }
  
  
  /**
   * Bestimmt die CREATION_ROLE aus dem OrderContext.
   */
  public Role getCreationRole() {
    Role role = null;
    //Die Rolle steht nur im OrderContext des Root-Auftrags und wird nicht vererbt.
    OrderContext rootOrderContext = getRootOrder().getOrderContext();
    if (rootOrderContext != null) {
      role = (Role) rootOrderContext.get(OrderContextServerExtension.CREATION_ROLE_KEY);
    }
    
    return role;
  }
  
  
  /**
   * wurde der auftrag nach der letzten �nderung die backup-wert ist, bereits gebackupped?
   */
  public boolean hasBeenBackuppedAfterChange() {
    return isTransientFlagSet(TransientFlags.BackuppedAfterChange);
  }


  public void setHasBeenBackuppedAfterChange(boolean hasBeenBackuppedAfterChange) {
    setTransientFlag(TransientFlags.BackuppedAfterChange, hasBeenBackuppedAfterChange);
  }

  /**
   * transientes feld, signalisiert, dass der auftrag im backup existiert
   */
  public boolean hasBeenBackuppedAtLeastOnce() {
    return isTransientFlagSet(TransientFlags.BackuppedAtLeastOnce);
  }
  
  public void setHasBeenBackuppedInScheduler(boolean hasBeenBackuppedInScheduler) {
    setTransientFlag(TransientFlags.BackuppedInScheduler, hasBeenBackuppedInScheduler);
  }

  public boolean hasBeenBackuppedInScheduler() {
    return isTransientFlagSet(TransientFlags.BackuppedInScheduler);
  }

  public void setHasBeenBackuppedAtLeastOnce() {
    setTransientFlag(TransientFlags.BackuppedAtLeastOnce);
  }
  
  public long getIdOfLatestDeploymentFromOrder() {
    return idOfLatestDeploymentKnownToOrder;
  }
  
  public void setIdOfLatestDeploymentKnownToOrder(long id) {
    idOfLatestDeploymentKnownToOrder = id;
  }
  
  
  public void setMiscellaneousDataBean(MiscellaneousDataBean miscellaneousDataBean) {
    this.dataBean = miscellaneousDataBean;
  }


  public MiscellaneousDataBean getMiscellaneousDataBean() {
    return dataBean;
  }
  
  
  public void updateInputPayload() {
    if (modifiedInputPayload != null) {
      setInputPayload(modifiedInputPayload);
    }
  }

  
  public boolean needsToBeBackupedOnSuspensionOfParent() {
    return needsToBeBackupedOnSuspensionOfParent;
  }

  
  public void setNeedsToBeBackupedOnSuspensionOfParent(boolean needsToBeBackupedOnSuspensionOfParent) {
    this.needsToBeBackupedOnSuspensionOfParent = needsToBeBackupedOnSuspensionOfParent;
  }
  
  
  public void addBackupAction(BackupAction ba) {
    if (additionalBackupActions == null) {
      additionalBackupActions = new ArrayList<BackupAction>();
    }
    additionalBackupActions.add(ba);
  }
  
  
  public List<BackupAction> getAdditionalBackupActions() {
    if (additionalBackupActions == null) {
      return Collections.emptyList();
    } else {
      return additionalBackupActions;
    }
  }
  

  public void clearAdditionalBackupActions() {
    if (additionalBackupActions != null) {
      additionalBackupActions.clear();
    }
  }
  
  private transient XynaOrderServerExtension rootOrder;
  
  public XynaOrderServerExtension getRootOrder() {
    if (rootOrder != null) {
      return rootOrder;
    }
    XynaOrderServerExtension currentOrder = this;
    
    while (currentOrder.hasParentOrder()) {
      currentOrder = currentOrder.getParentOrder();
    }
    
    rootOrder = currentOrder;
    return currentOrder;
  }

  public boolean isLetOrderAbort() {
    return letOrderAbort;
  }


  
  public void setLetOrderAbort(boolean letOrderAbort) {
    this.letOrderAbort = letOrderAbort;
  }


  
  public boolean isLetOrderCompensateAfterAbort() {
    return letOrderCompensateAfterAbort;
  }


  
  public void setLetOrderCompensateAfterAbort(boolean letOrderCompensateAfterAbort) {
    this.letOrderCompensateAfterAbort = letOrderCompensateAfterAbort;
  }

  public OrderStartupMode getOrderStartupMode() {
    return orderStartupMode;
  }

  public void setOrderStartupMode(OrderStartupMode orderStartupMode) {
    this.orderStartupMode = orderStartupMode;
  }

  
  public Long getExecutionTimeoutTimestamp() {
    return executionTimeoutTimestamp;
  }

  /*
   * achtung: wird vom generierten code aus aufgerufen! (vgl StepFunction)
   */
  public Long getRevision() {
    return revision;
  }

  
  public void setRevision(Long revision) {
    this.revision = revision;
    if (parentOrder == null) {
      for (XynaOrderServerExtension xo : getOrderAndChildrenRecursively()) {
        xo.rootRevision = revision;
      }
    }
  }

  public void setParentRevision(Long parentRevision) {
    this.parentRevision = parentRevision;
  }

  public Long getParentRevision() {
    return this.parentRevision;
  }

  
  public boolean isAcknowledgeSuccessfullyExecuted() {
    return acknowledgeSuccessfullyExecuted;
  }

  
  public void setAcknowledgeSuccessfullyExecuted(boolean acknowledgeSuccessfullyExecuted) {
    this.acknowledgeSuccessfullyExecuted = acknowledgeSuccessfullyExecuted;
  }

  public void abortResumingOrder(boolean ignoreCapacitiesAndVetos, Throwable cause) {
    /*
     * (teil-)auftragsfamilie ist beim resume und soll aborted werden
     * => keiner der auftr�ge macht irgendwelche workflowschritte bis auf das compensate
     * das flag ignoreCapacitiesAndVetos soll bringen, dass beim wieder einstellen der kind-auftr�ge irgendeiner auf
     * capacities warten muss (wird ja eh abgebrochen).
     * 
     * wenn das flag also gesetzt ist, muss f�r alle (bestehenden und damit zu resumenden) kindauftr�ge die capacities und vetos ausgeschaltet werden
     * 
     * in beiden f�llen werden alle workflowinstanzen auf aborting gesetzt
     */
    if( cause != null ) {
      XynaProcess xp = getExecutionProcessInstance();
      if( xp != null ) {
        xp.getRootProcessData().setAbortionCause(cause);
      }
    }
    
    for (XynaOrderServerExtension xo : getOrderAndChildrenRecursively()) {
      XynaProcess xp = xo.getExecutionProcessInstance();
      if (xp != null) {
        if (xp.getState() == XynaProcessState.SUSPENDED) {
          xp.setState(XynaProcessState.SUSPENDED_AFTER_ABORTING);
        } else if (xp.getState() == XynaProcessState.FINISHED || xp.getState() == XynaProcessState.SUSPENDED_AFTER_ABORTING) {
          //ok nichts zu tun.
        } else {
          logger.warn("Found xynaprocess in unexpected state " + xp.getState()
              + " trying to cancel a resuming order. (" + xo + ")");
        }
      }
      if (ignoreCapacitiesAndVetos) {
        //die alten capacities/vetos nicht nochmal verwenden
        xo.setSchedulerBean(new SchedulerBean());
      }
    }
    if (ignoreCapacitiesAndVetos) {
      //so tun, als w�re man gescheduled worden. die subauftr�ge setzen das selbst im scheduler
      schedulingData.setHasAcquiredCapacities(true);
    }
    setHasBeenBackuppedAfterChange(false);
  }

  public String getParentLaneId() {
    return parentLaneId;
  }

  public void setParentLaneId(String parentLaneId) {
    this.parentLaneId = parentLaneId;
  }

  
  public void replaceSchedulingData(SchedulingData schedulingData) {
    this.schedulingData = schedulingData;
  }

  public BatchProcessMarker getBatchProcessMarker() {
    return batchProcessMarker;
  }
  
  public void setBatchProcessMarker(BatchProcessMarker batchProcessMarker) {
    this.batchProcessMarker = batchProcessMarker;
  }


  public boolean setTransientFlag(TransientFlags flag) {
    return transientFlags.add(flag);
  }
  
  public boolean unsetTransientFlag(TransientFlags flag) {
    return transientFlags.remove(flag);
  }
  public boolean isTransientFlagSet(TransientFlags flag) {
    return transientFlags.contains(flag);
  }
  
 
  /**
   * Setzt das Flag auf vorhanden/nicht vorhanden
   * @param flag
   * @param set
   * @return true, wenn �nderung bewirkt wurde
   */
  private boolean setTransientFlag(TransientFlags flag, boolean set) {
    if( set ) {
      return transientFlags.add(flag);
    } else {
      return transientFlags.remove(flag);
    }
  }
  
  private OrderInputCreationInstanceWithSeriesInfo inputCreationInfo;

  public void setOrderInputCreationInstances(OrderInputCreationInstanceWithSeriesInfo inputCreationInfo) {
    this.inputCreationInfo = inputCreationInfo;    
  }
  
  public XynaOrderCreationParameter getOrCreateOrderInput(String idOfInputSourceInWF, String inputSourceName) throws XynaException {
    return inputCreationInfo.getOrCreate(idOfInputSourceInWF, inputSourceName, revision);
  }

  public boolean areOrderInputSourcesPrepared() {
    return inputCreationInfo != null;
  }

  public Long getOrderInputGenerationContextId() {
    if (inputCreationInfo != null) {
      return inputCreationInfo.getGenerationContextId();
    }
    if (parentOrder != null) {
      return parentOrder.getOrderInputGenerationContextId();
    }
    return null;
  }

  public OrderInputCreationInstanceWithSeriesInfo getAndRemoveOrderInputCreationInstances(String idOfInputSourceInWF) {
    return inputCreationInfo.remove(idOfInputSourceInWF);
  }
  
  public void clearOrderInputCreationInstances() {
    inputCreationInfo.clear();
  }

  
  public boolean monitoringLevelAlreadyDiscovered() {
    return monitoringLevelAlreadyDiscovered;
  }

  public void setMonitoringLevelAlreadyDiscovered(boolean monitoringLevelAlreadyDetermined) {
    this.monitoringLevelAlreadyDiscovered = monitoringLevelAlreadyDetermined;
  }
  
  @Override
  public void addException(XynaException e) {
    super.addException(e);
    determineStageAndInsert(e);
  }
  
  public void addException(XynaException e, ProcessingStage stage) {
    super.addException(e);
    if (processingStageErrors == null) {
      processingStageErrors = new EnumMap<ProcessingStage, Collection<XynaException>>(ProcessingStage.class);
    }
    Collection<XynaException> stageErrors = processingStageErrors.get(stage);
    if (stageErrors == null) {
      stageErrors = new ArrayList<XynaException>();
    }
    stageErrors.add(e);
    processingStageErrors.put(stage, stageErrors);
  }
  
  public static enum ProcessingStage {
    
    COMPENSATION {
      @Override
      public boolean isMyStage(StackTraceElement ste) {
        return (ste.getClassName().equals(MasterWorkflowPostScheduler.class.getName()) && ste.getMethodName().equals("compensateMasterWorkflow")) ||
               (ste.getClassName().equals(XynaProcess.class.getName()) && ste.getMethodName().equals("compensate"));
      }
    },
    PLANNING {
      @Override
      public boolean isMyStage(StackTraceElement ste) {
        return ste.getClassName().equals(PlanningDispatcher.class.getName());
      }
    },
    EXECUTION {
      @Override
      public boolean isMyStage(StackTraceElement ste) {
        return ste.getClassName().equals(ExecutionDispatcher.class.getName());
      }
    },
    CLEANUP {
      @Override
      public boolean isMyStage(StackTraceElement ste) {
        return ste.getClassName().equals(CleanupDispatcher.class.getName());
      }
    },
    ARCHIVING {
      @Override
      public boolean isMyStage(StackTraceElement ste) {
        return ste.getClassName().equals(OrderArchive.class.getName()) && ste.getMethodName().equals("archive");
      }
    },
    SCHEDULING {
      @Override
      public boolean isMyStage(StackTraceElement ste) {
        return ste.getClassName().equals(XynaScheduler.class.getName());
      }
    },
    INITIALIZATION {
      @Override
      public boolean isMyStage(StackTraceElement ste) {
        return ste.getClassName().equals(MasterWorkflowPreScheduler.class.getName());
      }
    },
    OTHER {
      @Override
      public boolean isMyStage(StackTraceElement ste) {
        return false;
      }
    };
    
    private ProcessingStage() {
    }
    
    public static ProcessingStage determineStage(Throwable t) {
      // the order of the stages is important as some stages can be detected as sub-stages of others
      ProcessingStage[] stageOrder = {COMPENSATION, PLANNING, EXECUTION, CLEANUP, ARCHIVING, SCHEDULING,INITIALIZATION };
      for (int i=0; i<stageOrder.length; i++) {
        Throwable current = t;
        while (current != null) {
          ProcessingStage stage = stageOrder[i];
          for (StackTraceElement ste : current.getStackTrace()) {
            if (stage.isMyStage(ste)) {
              return stage;
            }
          }
          current = current.getCause();
        }
      }
      return OTHER;
    }
    
    public abstract boolean isMyStage(StackTraceElement ste);
    
  }
  
  public boolean hadErrorsDuring(ProcessingStage stage) {
    return processingStageErrors != null &&
           processingStageErrors.containsKey(stage) &&
           processingStageErrors.get(stage) != null && 
           processingStageErrors.get(stage).size() > 0;
  }
  
  public Collection<XynaException> getErrorsFrom(ProcessingStage stage) {
    if (hadErrorsDuring(stage)) {
      return processingStageErrors.get(stage);
    } else {
      return Collections.emptySet();
    }
  }
  
  private void restoreProcessingStageMapping() {
    processingStageErrors = new EnumMap<ProcessingStage, Collection<XynaException>>(ProcessingStage.class);
    if (errors != null && errors.size() > 0) {
      for (XynaException error : errors) {
        determineStageAndInsert(error);
      }
    }
  }
  
  
  private void determineStageAndInsert(XynaException error) {
    ProcessingStage stage = ProcessingStage.determineStage(error);
    if (processingStageErrors == null) {
      processingStageErrors = new EnumMap<ProcessingStage, Collection<XynaException>>(ProcessingStage.class);
    }
    Collection<XynaException> stageErrors = processingStageErrors.get(stage);
    if (stageErrors == null) {
      stageErrors = new ArrayList<XynaException>();
    }
    stageErrors.add(error);
    processingStageErrors.put(stage, stageErrors);
  }
  
  private transient boolean freeCapacitiesLater = false;

  public void setFreeCapacityLater(boolean freeCapacitiesLater) {
    this.freeCapacitiesLater = freeCapacitiesLater;
  }

  public boolean getFreeCapacityLater() {
    return freeCapacitiesLater;
  }
  
  /*
   * Vergleiche Kommentar in XynaProcessCtrlExecution.startOrder
   * null/0 = muss nicht runtergez�hlt werden
   * 1 = muss runtergez�hlt werden
   * 2 = ist runtergez�hlt worden
   */
  private transient AtomicInteger deploymentCounterCountDownDone;

  public boolean isDeploymentCounterCountDownDone() {
    return deploymentCounterCountDownDone != null && deploymentCounterCountDownDone.get() == 2;
  }
  
  public void setDeploymentCounterCountDownDone() {
    deploymentCounterCountDownDone.compareAndSet(1, 2);
  }
  
  public void setDeploymentCounterMustBeCountDown() {
    if (deploymentCounterCountDownDone == null) {
      deploymentCounterCountDownDone = new AtomicInteger(0);
    }
    deploymentCounterCountDownDone.set(1);
  }

  public boolean mustDeploymentCounterBeCountDown() {
    return deploymentCounterCountDownDone != null && deploymentCounterCountDownDone.get() == 1;
  }
  
  private int[][] stepCoordinates;
  
  //wird nur (aus dem generierten code) gesetzt, falls auftrag beim beenden archiviert/aus memory entfernt werden darf, weil er nicht mehr f�r compensationzwecke ben�tigt wird
  //vgl StepFunction.createCodeForLazyCreateSubWf
  public void setStepCoordinates(int[][] stepCoordinates) {
    this.stepCoordinates = stepCoordinates;
  }
  
  //soll der auftrag aus dem parentwf rausgeworfen werden?
  private boolean removeFromParentWF = false;
  
  //wird vom generierten code aufgerufen. vgl StepFunction.appendExecuteInternallyForWFRef
  public boolean removeFromParentWF() {
    return removeFromParentWF;
  }
  
  public boolean removeOrderReferenceIfNotNeededForCompensation() {
    if (stepCoordinates == null) {
      return false;
    }
    //solange keine genauere evaluierung passiert, ob compensation notwendig ist, ist dieser aufruf nicht n�tig. 
    //ansonsten w�re dieser aufruf hilfreich, wenn man erst nach dem ausf�hren des subauftrags schaut, ob die gelaufenen schritte compensations hatten.
    //boolean removed = getParentOrder().getExecutionProcessInstance().removeOrderReferenceIfNotNeededForCompensation(stepCoordinates, getId());
    boolean removed = true;
    if (removed) {
      removeFromParentWF = true;
      XynaOrderServerExtension root = getRootOrder();
      if (root.orderFamily != null) {
        removeOrderAndChildren(root.orderFamily, this);
      }
      List<XynaOrderServerExtension> pchos = getParentOrder().childOrders;
      synchronized (pchos) {
        pchos.remove(this);
      }
    }
    return removed;
  }

  private static void removeOrderAndChildren(Map<Long, XynaOrderServerExtension> orderFamily, XynaOrderServerExtension xo) {
    orderFamily.remove(xo.getId());
    for (XynaOrderServerExtension child : xo.getDirectChildOrders()) {
      removeOrderAndChildren(orderFamily, child);
    }
  }
}
