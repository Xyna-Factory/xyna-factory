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
package com.gip.xyna.xprc.xsched.timeconstraint;

import java.util.List;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.TransientFlags;
import com.gip.xyna.xprc.exceptions.XPRC_DuplicateTimeWindowNameException;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_ABORTED_EXCEPTION;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_SCHEDULING_TIMEOUT;
import com.gip.xyna.xprc.exceptions.XPRC_Scheduler_TimeWindowMissingException;
import com.gip.xyna.xprc.exceptions.XPRC_SchedulingTimeout_BatchProcess;
import com.gip.xyna.xprc.exceptions.XPRC_SchedulingTimeout_Capacity;
import com.gip.xyna.xprc.exceptions.XPRC_SchedulingTimeout_Deployment;
import com.gip.xyna.xprc.exceptions.XPRC_SchedulingTimeout_Predecessor;
import com.gip.xyna.xprc.exceptions.XPRC_SchedulingTimeout_TimeConstraint;
import com.gip.xyna.xprc.exceptions.XPRC_SchedulingTimeout_Veto;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowNotFoundInDatabaseException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowRemoteManagementException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowStillUsedException;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder.WaitingCause;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintExecutor.SchedulingTimeout;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintExecutor.StartTime;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintExecutor.TimeConstraintTaskWithOrder;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintExecutor.TimeConstraintTaskWithOrder.Type;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintExecutor.TimeWindowChanger;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagementSteps.AddTimeWindow;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagementSteps.ChangeTimeWindow;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagementSteps.RecreateTimeWindows;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagementSteps.RemoveTimeWindow;
import com.gip.xyna.xprc.xsched.timeconstraint.cluster.TCMLocalImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.cluster.TCMRemoteEndpointImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.cluster.TCMRemoteProxyImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindow;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowStorable;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowStorableQueries;


public class TimeConstraintManagement extends FunctionGroup implements TimeConstraintManagementInterface, Clustered {

  public static final String DEFAULT_NAME = "Time Constraint Management";

  private TimeConstraintExecutor timeConstraintExecutor; //Zeitsteuerung
  //private ConcurrentHashMap<String,TimeConstraintWindow> timeWindows;
  private AllTimeConstraintWindows allTimeConstraintWindows;
  private RMIClusterStateChangeHandler rmiClusterStateChangeHandler = new RMIClusterStateChangeHandler();
  private ClusterContext rmiClusterContext;
  private TCMLocalImpl tcmLocal;
  private TCMRemoteEndpointImpl tcmRemoteEndpoint;
  private TCMRemoteProxyImpl tcmRemoteProxy;
  private boolean hasToWaitForStableTimeWindows;
  private TimeConstraintWindowStorableQueries tcwsQueries;
  private AllOrdersList allOrders;
  private volatile boolean initialized = false;
  
  public static enum TimeConstraintProblemReaction {
    Wait,         //Auftrag wartet im Scheduler, bis TimeWindow-Problem gelöst ist
    Fail,         //Auftrag mit Exception abbrechen
    Schedule;     //trotzdem schedulen

    public static String documentation(DocumentationLanguage lang) {
      switch( lang ) {
        case DE:
          return "'Wait': Auftrag wartet im Scheduler, bis TimeWindow-Problem gelöst ist; "
          +"'Fail': Auftrag wird mit XynaException abgebrochen; "
          +"'Schedule': Auftrag wird sofort geschedult";
        case EN:
        default:
          return "'Wait': Order remains in the scheduler until time window problem is resolved; "
          +"'Fail': Order fails with a XynaException; "
          +"'Schedule': Order is scheduled immediately";
      }
    }
  }
     

  public TimeConstraintManagement() throws XynaException {
    super();
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    
    XynaProperty.SCHEDULER_UNDEFINED_TIME_WINDOW_REACTION.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCHEDULER_CLOSED_TIMEWINDOW_REMOVE_TIME_OFFSET.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCHEDULER_WAIT_FOR_STABLE_TIME_WINDOWS.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(TimeConstraintManagement.class,"TimeConstraintManagement.initCluster")
         .before(XynaClusteringServicesManagement.class)
         .execAsync(this::initCluster);
    fExec.addTask(TimeConstraintManagement.class,"TimeConstraintManagement.initAll")
         .after(AllOrdersList.class, PersistenceLayerInstances.class, XynaClusteringServicesManagement.class)
         .execAsync(this::initAll);
  }
  
  
  private void initCluster() {
    rmiClusterStateChangeHandler = new RMIClusterStateChangeHandler();
    try {
      rmiClusterContext = new ClusterContext( rmiClusterStateChangeHandler, this );
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException("Failed to register " + TimeConstraintManagement.class.getSimpleName() + " as clusterable component.", e);
    }
  }
  
  
  private void initAll() {
    allOrders = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList();
    timeConstraintExecutor = new TimeConstraintExecutor(this, allOrders);
    allTimeConstraintWindows = new AllTimeConstraintWindows(timeConstraintExecutor,allOrders);
    
    initStorables();
    
    initClusterStatusKnown();
    initialized = true;
  }
  
  /**
   * 
   */
  private void initStorables() {
    try {
      ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
      ods.registerStorable(TimeConstraintWindowStorable.class);
      
      tcwsQueries = new TimeConstraintWindowStorableQueries();
      ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        tcwsQueries.init(defCon);
      } finally {
        defCon.closeConnection();
      }
      
    } catch (PersistenceLayerException e) {
      //FIXME bessere Behandlung?
      throw new RuntimeException(e);
    }
  }

  private void initClusterStatusKnown() {
    //ClusterStatus ist nun bekannt, daher können die restlichen Initialisierungen durchgeführt werden.
    
    //Können Zeitfenster komplett initialisiert werden?
    XynaProperty.SCHEDULER_WAIT_FOR_STABLE_TIME_WINDOWS.registerDependency(UserType.XynaFactory, getDefaultName());
    hasToWaitForStableTimeWindows = false;
    if( ! rmiClusterContext.getClusterState().isStable() ) {
      //Zeitfenster müssen nach Übergang in einen stabilen ClusterState erneut initialisiert werden
      if( XynaProperty.SCHEDULER_WAIT_FOR_STABLE_TIME_WINDOWS.get() ) {
        //Zeitfenster werden gesperrt angelegt, damit Aufträge im Scheduler nicht wegen fehlender 
        hasToWaitForStableTimeWindows = true;
      } else {
        //Zeitfenster werden angelegt, könnten bei Verwendung aber veraltete Definition haben
      }
    }
    
    tcmLocal = new TCMLocalImpl(allTimeConstraintWindows, tcwsQueries);
    
    if( hasToWaitForStableTimeWindows ) {
      logger.info("TimeConstraintManagement has not initialized TimeWindows, order have to wait in scheduler");
    } else {
      int count = recreateTimeWindows();
      logger.info("TimeConstraintManagement has initialized "+count+" TimeWindows");
    }
 
    tcmRemoteEndpoint = new TCMRemoteEndpointImpl(tcmLocal);
    tcmRemoteProxy = new TCMRemoteProxyImpl( rmiClusterContext, tcmRemoteEndpoint );
  }
  


  @Override
  protected void shutdown() throws XynaException {
    if (!initialized) {
      return;
    }
    logger.debug("Stopping time constraint management thread");
    timeConstraintExecutor.stop();
  }

  
  /**
   * Muss der Auftrag auf StartTime warten? (geschlossenes Zeitfenster, EarliestStartTimestamp gesetzt)
   * @param schedulingData
   * @return true, wenn Auftrag warten muss
   */
  public boolean hasToWaitForStartTime(SchedulingData schedulingData) {
    TimeConstraintData tcd = schedulingData.getTimeConstraintData();
    
    if( ! tcd.isConfigured() ) {
      return false; //keinerlei zeitliche Einschränkung
    }
    long now = System.currentTimeMillis();
    
    if( tcd.hasToWaitForStartTime(now) ) {
      if( logger.isDebugEnabled()) {
        logger.debug("Entrance timestamp " + (now - tcd.getEntranceTimestamp()) 
                   + " ms ago, earliest start in "+(tcd.getStartTimestamp()-now) + " ms");
      }
      return true;
    }
    
    String windowName = tcd.getTimeWindowName();
    if( windowName != null ) {
      //TimeWindow fehlt: gleich in Scheduler eintragen, der soll sich dann um das Problem kümmern
      //TimeWindow ist offen: gleich in Scheduler eintragen
      //TimeWindow  ist geschlossen: Auftrag muss warten
      return allTimeConstraintWindows.isClosed(windowName);
    }   
    return false;
  }
  
  /**
   * Eintragen der Order in die TimeConstraint-Überwachung
   */
  public void addWaitingOrder(SchedulingOrder so, boolean wasNeverScheduled) {
    TimeConstraintData tcd = so.getSchedulingData().getTimeConstraintData();
    
    if( wasNeverScheduled) {
      //auf jeden Fall wegen SchedulingTimeout eintragen
      addSchedulingTimeout(so,tcd);
      
      //TODO ist es hier nicht sinnvoll, aufträge aktiv abzubrechen (wie beim cancel), falls bereits ein timeout erreicht wurde?
      // vorteil: kein unnötiges scheduling
      
      if( so.isWaitingFor(WaitingCause.StartTime) ) {
        //Normales Scheduling hier, um SchedulingOrder in TimeConstraintExecutor einzutragen bzw. zum Melden, dass Auftrag lauffähig ist. 
        TimeConstraintResult tcr = checkTimeConstraint(so);
        if (tcr.isExecutable()) {
          //Auftrag ist entweder sofort lauffähig oder hat Fehler, der zum Abbruch führen muss
          logger.warn(so + " is executable.");
          so.removeWaitingCause(WaitingCause.StartTime);
        } else if ( tcr.removeFromScheduler() ) {
          XynaOrderServerExtension xo = so.getXynaOrderOrNull();
          if( xo != null ) {
            xo.setTransientFlag(TransientFlags.WasKnownToScheduler);
          } //else: Unerwartet, da SchedulingOrder eben erst angelegt
        } else {
          //Auftrag ist entweder sofort lauffähig oder hat Fehler, der zum Abbruch führen muss
          so.removeWaitingCause(WaitingCause.StartTime);
        }
      }
    }
  }

  /**
   * Aufruf beim Scheduling: Kann die SchedulingOrder aktuell gestartet werden?
   * @param so
   * @return
   */
  public TimeConstraintResult checkTimeConstraint(SchedulingOrder so) {
    //1. Timeout ist bereits bekannt
    if( so.isMarkedAsTimedout() ) {
      return timeout(so);
    }
    
    //2. Sind TimeConstraints gesetzt?
    TimeConstraintData tcd = so.getSchedulingData().getTimeConstraintData();
    if( ! tcd.isConfigured() ) {
      return TimeConstraintResult.SUCCESS;
    }
    long now = System.currentTimeMillis();
    
    //3. Gibt es doch ein Timeout und nur der SchedulingOrder-State konnte noch nicht umgesetzt werden?
    if( tcd.hasTimeout(now) ) {
      return timeout(so);
    }
    
    //4. Muss doch noch länger auf Startzeit gewartet werden?
    if( tcd.hasToWaitForStartTime(now) ) {
      //Auftrag muss noch länger warten
      return waitAgain( so, null, tcd.getStartTimestamp(), now, false );
    }

    //5. Zeitfenster
    String windowName = tcd.getTimeWindowName();
    if( windowName == null ) {
      //Keine Zeitfenster-Beschränkung, d.h startbar
      return TimeConstraintResult.SUCCESS;
    } else {
      //Zeitfenster holen, gesperrt gegen gleichzeitige Änderungen
      TimeConstraintWindow window = allTimeConstraintWindows.getLockedTimeWindow(windowName);
      try {
        if( window == null ) {
          //Zeitfenster existiert nicht
          return missingTimeWindow(windowName);
        } else {
          return checkTimeConstraintForTimeWindow(so, tcd, window, now);
        } 
      } finally {
        if( window != null ) {
          window.unlock();
        }
      }
    }
  }
  
  /**
   * Aufruf beim Scheduling: Kann die SchedulingOrder aktuell gestartet werden?
   * @param so
   * @param tcd
   * @param window
   * @param now
   * @return
   */
  private TimeConstraintResult checkTimeConstraintForTimeWindow(SchedulingOrder so, TimeConstraintData tcd,
                                                                TimeConstraintWindow window, long now) {
    //1. Ist Zeitfenster geschlossen? 
    if( ! window.isOpen() ) {
      return waitAgain( so, window, 0L, now, false );
    }
    
    //2. Zeitfenster ist offen
    //nun um die TimeConstraints kümmern, die innerhalb des Zeitfensteres gelten:
    tcd.recalculateInWindow(window);
     
    //3. Im Zeitfenster könnte es Timeout geben
    if( tcd.hasTimeout(now) ) {
      //evtl. gibt es doch ein Timeout und nur der SchedulingOrder-State konnte noch nicht umgesetzt werden
      return timeout(so);
    } else {
      addSchedulingTimeout(so,tcd);
    }

    //4. Im Zeitfenster könnte es eine neue Wartezeit geben
    if( tcd.hasToWaitForStartTime(now) ) {
      //Auftrag muss noch länger warten
      return waitAgain( so, null, tcd.getStartTimestamp(), now, false );
    }

    //5. Auftrag ist startfähig
    tcd.setWindowIsOpenSince( window.getOpenSince() );
    return TimeConstraintResult.SUCCESS;
  }

  /**
   * Abbrechen des Auftrags mit XynaException
   * @param so
   * @return
   */
  private TimeConstraintResult timeout(SchedulingOrder so) {
    so.markAsTimedout();
    return TimeConstraintResult.TIMED_OUT;
  }
  
  public static XynaException buildSchedulingTimeoutException(Long orderId, SchedulingOrder so, XynaOrderServerExtension xo) {
    switch( so.getOrderStatus() ) {
      case WAITING_FOR_CAPACITY:
        return new XPRC_SchedulingTimeout_Capacity(orderId);
      case WAITING_FOR_VETO:
        return new XPRC_SchedulingTimeout_Veto(orderId);
      case WAITING_FOR_PREDECESSOR:
        return new XPRC_SchedulingTimeout_Predecessor(orderId);
      case WAITING_FOR_TIMECONSTRAINT:
        return new XPRC_SchedulingTimeout_TimeConstraint(orderId);
      case WAITING_FOR_BATCH_PROCESS:
        return new XPRC_SchedulingTimeout_BatchProcess(orderId);
      case WAITING_FOR_DEPLOYMENT:
        return new XPRC_SchedulingTimeout_Deployment(orderId);
      default:
        Long executionTS = xo.getExecutionTimeoutTimestamp();
        if (executionTS != null && executionTS < System.currentTimeMillis()) {
          return new XPRC_PROCESS_ABORTED_EXCEPTION(orderId, AbortionCause.TIME_TO_LIVE_EXPIRATION.toString());
        }
        return new XPRC_PROCESS_SCHEDULING_TIMEOUT(orderId);
    }
  }
  
  /**
   * Eintragen der SchedulingOrder in die SchedulingTimeout-Überwachung
   * @param so
   * @param tcd
   */
  private void addSchedulingTimeout(SchedulingOrder so, TimeConstraintData tcd) {
    if( tcd.getSchedulingTimeout() != null ) {
      if( ! tcd.isSchedulingTimeoutMonitored() ) {
        
        if( System.currentTimeMillis() >= tcd.getSchedulingTimeout() ) {
          so.markAsTimedout();
          if( logger.isDebugEnabled() ) {
            logger.debug( "Order "+so.getOrderId()+" has scheduling timeout at "+tcd.getSchedulingTimeout()+" in the past");
          }
        } else {
          timeConstraintExecutor.add( new SchedulingTimeout(so, tcd) );
          if( logger.isDebugEnabled() ) {
            logger.debug( "Order "+so.getOrderId()+" has scheduling timeout at "+tcd.getSchedulingTimeout());
          }
        }
      }
    }
  }
 
  /**
   * Auftrag muss weiter im TimeConstraintManagement warten, wird aus Scheduler entfernt
   * @param so
   * @param window
   * @param startTime
   * @param now
   * @return
   */
  private TimeConstraintResult waitAgain(SchedulingOrder so, TimeConstraintWindow window, long startTime, long now, boolean schedulingOrderIsAlreadyLocked) {
    boolean hasWindow = window != null;
    long hasToWait = ( hasWindow ? window.getNextOpen() : startTime ) -now;
     
    
    if( hasToWait < XynaProperty.SCHEDULER_CLOSED_TIMEWINDOW_REMOVE_TIME_OFFSET.getMillis() ) {
      //Auftrag ist in wenigen Millisekunden lauffähig, daher ist es nicht nötig, ihn aus 
      //dem Scheduler zu entfernen
      return TimeConstraintResult.CONTINUE;
    }  
    
    if( logger.isDebugEnabled() ) {
      logger.debug( "Order "+so.getOrderId()+" has to wait "+hasToWait+" ms for next scheduling");
    }
    
    if( schedulingOrderIsAlreadyLocked ) {
      so.addWaitingCause(WaitingCause.StartTime);
    } else {
      synchronized (so) {
        so.waitIfLocked();
        so.addWaitingCause(WaitingCause.StartTime);
      }
    }

    //Auftrag darf derzeit nicht laufen, daher aus dem Scheduler entfernen und hier im 
    //TimeConstraintManagement aufbewahren
    if( hasWindow ) {
      window.addWaitingOrder(so);
    } else {
      timeConstraintExecutor.add( new StartTime(so, startTime) );
    }

    return hasWindow ? TimeConstraintResult.WAIT_NOT_OPEN : TimeConstraintResult.WAIT_STARTTIME;
  }
  
  /**
   * Reaktion auf nicht-existentes Zeitfenster
   * @param windowName
   * @return
   */
  private TimeConstraintResult missingTimeWindow(String windowName) {
    if( hasToWaitForStableTimeWindows ) {
      return TimeConstraintResult.WAIT_MISSING;
    }
    TimeConstraintProblemReaction tcpr = XynaProperty.SCHEDULER_UNDEFINED_TIME_WINDOW_REACTION.get();
    switch( tcpr ) {
      case Wait:
        return TimeConstraintResult.WAIT_MISSING;
      case Schedule:
        return TimeConstraintResult.SUCCESS;
      case Fail:
        return new TimeConstraintResult(new XPRC_Scheduler_TimeWindowMissingException(windowName) );
    }
    throw new IllegalStateException("Unexpected TimeConstraintProblemReaction " + tcpr);
  }
    
  /**
   * Reschedule der SchedulingOrder, wenn TimeContraint gewechselt wird: 
   * SchedulingOrder befindet sich unter Umständen nicht im Scheduler, weil das alte Zeitfenster 
   * geschlossen ist oder der Startzeitpunkt noch nicht erreicht ist. 
   * Daher muss beim Wechsel des TimeConstraints dafür gesorgt werden, dass diese Methode 
   * gerufen wird, um den Auftrag umzutragen.
   * @param so
   * @param timeConstraint
   * @return true, wenn Auftrag wieder in den Scheduler darf
   */
  public boolean rescheduleOrder(SchedulingOrder so, TimeConstraint timeConstraint) {
    if( logger.isDebugEnabled() ) {
      logger.debug(" Rescheduling "+so+" with " +timeConstraint);
    }
    TimeConstraintData tcd = so.getSchedulingData().getTimeConstraintData();
    long now = System.currentTimeMillis();
    
    if( tcd.getSchedulingTimeout() != null ) {
      timeConstraintExecutor.removeSchedulingTimeout(so.getOrderId());
    }
    if( tcd.hasToWaitForStartTime(now) ) {
      timeConstraintExecutor.removeWaitingForStartTime(so.getOrderId());
    }
    if( tcd.getTimeWindowName() != null ) {
      TimeConstraintWindow oldWindow = allTimeConstraintWindows.getLockedTimeWindow(tcd.getTimeWindowName());
      if( oldWindow != null ) {
        try {
          SchedulingOrder waiting = oldWindow.removeWaitingOrder(so.getOrderId());
          if( waiting == null ) {
            logger.warn("Expected to find Order in TimeWindow "+oldWindow);
          }
        } finally {
          oldWindow.unlock();
        }
      } else {
        logger.warn("Expected to find TimeWindow "+tcd.getTimeWindowName());
      }
    }

    tcd.setEntranceTimestamp(now); //so tun, als ob Auftrag neu ins System gekommen ist, damit SchedulingTimeout funktioniert
    tcd.setDefinition(timeConstraint);
    //auf jeden Fall wegen SchedulingTimeout eintragen
    addSchedulingTimeout(so,tcd);
    
    if( so.isMarkedAsTimedout() ) {
      return true;
    }
    //muss wegen StartZeit gewartet werden? 
    if( tcd.hasToWaitForStartTime(now) ) {
      //Auftrag muss noch länger warten
      TimeConstraintResult result = waitAgain( so, null, tcd.getStartTimestamp(), now, true); //TODO sicherstellen, dass true stimmt
      if( result == TimeConstraintResult.CONTINUE ) {
        return true;
      }
      return result.isExecutable();
    } else {
      //TODO TimeWindow wird hier nicht kontrolliert, das kann dann auch der Scheduler machen...
      return true;
    }
  }

  /**
   * Frühzeitiger Timeout aller Aufträge, die ihren regulären Timeout innerhalb der nächsten timeout ms hätten
   * @param timeout
   */
  public void earlyTimeout(long timeout) {
    timeConstraintExecutor.earlyTimeout(timeout);
  }


  public TimeConstraintManagementInformation getTimeConstraintManagementInformation() {
    TimeConstraintManagementInformation tcmi = new TimeConstraintManagementInformation();
    tcmi.numTimedTasks = timeConstraintExecutor.size();
    tcmi.numStartTime = TimeConstraintTaskWithOrder.getCounter(Type.StartTime);
    tcmi.numSchedulingTimeout = TimeConstraintTaskWithOrder.getCounter(Type.SchedulingTimeout);
    allTimeConstraintWindows.fillTimeConstraintManagementInformation(tcmi);
    return tcmi;
  }
  
  public TimeConstraintWindowDefinition getDefinition(String timeWindowName) {
    return allTimeConstraintWindows.getDefinition(timeWindowName);
  }



  
  
  public void addTimeWindow(TimeConstraintWindowDefinition definition) throws XPRC_DuplicateTimeWindowNameException, PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException, XPRC_TimeWindowRemoteManagementException {
    String name = definition.getName();
    //gegen konkurrierende Änderungen schützen
    allTimeConstraintWindows.lock(name);
    try {
      if( allTimeConstraintWindows.hasTimeWindow(name) ) {
        throw new XPRC_DuplicateTimeWindowNameException(name);
      } else {
        AddTimeWindow addTimeWindow = new AddTimeWindow(tcmLocal, tcmRemoteProxy, tcwsQueries );
        TimeConstraintWindow timeWindow = new TimeConstraintWindow(definition);
        try {
          //zuerst in DB eintragen, ...
          addTimeWindow.addToDB(timeWindow);
          //...damit Remote das Zeitfenster gelesen werden kann
          addTimeWindow.remoteActivate(timeWindow);
          //zuletzt lokal aktivieren, da hier kein Fehler zu erwarten ist
          addTimeWindow.localActivate(timeWindow);
          //addTimeWindow ist erfolgreich, keine Compensation nötig
          addTimeWindow.success();
        } finally {
          addTimeWindow.compensate();
          if( addTimeWindow.hasCompensationExceptions() ) {
            logCompensationExceptions( "addTimeWindow "+name, addTimeWindow.getCompensationExceptions() );
          }
        }
      }
    } finally {
      allTimeConstraintWindows.unlock(name);
    }
  }
  
  private void logCompensationExceptions(String operation, List<Pair<String, Exception>> compensationExceptions) {
    logger.warn(compensationExceptions.size() + " exceptions while compensating " +operation+":");
    int i=0;
    for( Pair<String, Exception> e : compensationExceptions ) {
      ++i;
      logger.warn( i+". "+e.getFirst()+ ": "+e.getSecond().getMessage(), e.getSecond() );
    }
  }

  public void removeTimeWindow(String name, boolean force) throws PersistenceLayerException, XPRC_TimeWindowStillUsedException {
    //gegen konkurrierende Änderungen schützen
    allTimeConstraintWindows.lock(name);
    try {
      RemoveTimeWindow removeTimeWindow = new RemoveTimeWindow(tcmLocal, tcmRemoteProxy, tcwsQueries);
      try {
        //zuerst lokal deaktivieren, ...
        removeTimeWindow.localDeactivate(name,force);
        //...dann remote deaktivieren
        removeTimeWindow.remoteDeActivate(name,force);
        //zuletzt lokal aus DB löschen
        removeTimeWindow.remove(name);
        //removeTimeWindow ist erfolgreich, keine Compensation nötig
        removeTimeWindow.success();
      } finally {
        removeTimeWindow.compensate();
        if( removeTimeWindow.hasCompensationExceptions() ) {
          logCompensationExceptions( "removeTimeWindow "+name, removeTimeWindow.getCompensationExceptions() );
        }
      }
    } finally {
      allTimeConstraintWindows.unlock(name);
    }
  }

  public void changeTimeWindow(TimeConstraintWindowDefinition definition) throws PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException, XPRC_TimeWindowRemoteManagementException {
    String name = definition.getName();
    //gegen konkurrierende Änderungen schützen
    allTimeConstraintWindows.lock(name);
    try {
      ChangeTimeWindow changeTimeWindow = new ChangeTimeWindow(tcmLocal, tcmRemoteProxy, tcwsQueries);
      TimeConstraintWindow timeWindow = new TimeConstraintWindow(definition);
      try {
        //zuerst das alte TimeWindow in der DB ersetzen, ...
        changeTimeWindow.replaceInDB(timeWindow);
           
        //...damit Remote das Zeitfenster gelesen werden kann
        changeTimeWindow.remoteActivateWithoutCompensation(timeWindow);
        //zuletzt lokal aktivieren, da hier kein Fehler zu erwarten ist
        changeTimeWindow.localActivateWithoutCompensation(timeWindow);
        
        //changeTimeWindow ist erfolgreich, keine Compensation nötig
        changeTimeWindow.success();
      } finally {
        changeTimeWindow.compensate();
        
        changeTimeWindow.compensateRemoteActivate(name);
        //zuletzt lokal aktivieren, da hier kein Fehler zu erwarten ist
        changeTimeWindow.compensateLocalActivate(name);
        if( changeTimeWindow.hasCompensationExceptions() ) {
          logCompensationExceptions( "changeTimeWindow "+name, changeTimeWindow.getCompensationExceptions() );
        }
      }
    } finally {
      allTimeConstraintWindows.unlock(name);
    }
  }

  private int recreateTimeWindows() {
    RecreateTimeWindows recreateTimeWindows = new RecreateTimeWindows(tcmLocal);
    try {
      return recreateTimeWindows.recreateTimeWindows();
    } catch (PersistenceLayerException e) {
      logger.warn("Could not read TimeWindows from Database", e);
      logger.warn("TimeWindows are all locked and orders have to wait");
      hasToWaitForStableTimeWindows = true;
      return 0;
    }
  }
  

  public void readd(TimeWindowChanger timeWindowChanger) {
    timeConstraintExecutor.add( timeWindowChanger );
  }
  
  public void notifyScheduler() {
    XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
  }  
  
  public boolean isClustered() {
    return rmiClusterContext.isClustered();
  }

  public long getClusterInstanceId() {
    return rmiClusterContext.getClusterInstanceId();
  }

  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    rmiClusterContext.enableClustering(clusterInstanceId);
  }

  public void disableClustering() {
    rmiClusterContext.disableClustering();
  }

  public String getName() {
    return getDefaultName();
  }
  
  private class RMIClusterStateChangeHandler implements ClusterStateChangeHandler {
    private ClusterState clusterState;
    public boolean isReadyForChange(ClusterState newState) {
      return true; //immer bereit
    }
    public void onChange(ClusterState newState) {
      if( logger.isInfoEnabled() ) {
        logger.info("TimeConstraintManagement.RMIClusterStateChangeHandler.onChange("+newState+")");
      }
      boolean oldClusterStateIsStable = clusterState != null ? clusterState.isStable() : false;
      boolean newClusterStateIsStable = newState.isStable();
      
      if( ! oldClusterStateIsStable && newClusterStateIsStable ) {
        int count = recreateTimeWindows();
        hasToWaitForStableTimeWindows = false;
        if( logger.isInfoEnabled() ) {
          logger.info("TimeConstraintManagement has initialized "+count+" TimeWindows after rmiClusterContext-stateChange "
                      + clusterState + "->" + newState);   
        }
      }
      if( tcmRemoteProxy != null ) {
        tcmRemoteProxy.setRmiConnected( newState == ClusterState.CONNECTED );
      }
      clusterState = newState;

    }

  }

  public void removeOrder(Long orderId) {
    timeConstraintExecutor.removeWaitingForStartTime(orderId);
    timeConstraintExecutor.removeSchedulingTimeout(orderId);
    //TODO aus TimeWindow entfernen
  }
 
}
