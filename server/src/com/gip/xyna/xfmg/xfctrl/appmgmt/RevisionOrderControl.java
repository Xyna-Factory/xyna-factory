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
package com.gip.xyna.xfmg.xfctrl.appmgmt;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_FilterImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_FilterInstanceNeedsEnabledFilterException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleFilterImplException;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xact.exceptions.XACT_LibOfFilterImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_OldFilterVersionInstantiationException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterInstanceStorable.FilterInstanceState;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ClearWorkingSet.RevisionContentBlackWhiteList;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.CustomOrderEntryInformation.DefaultBehavior;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_IllegalStateForTaskArchiving;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCronLikeOrderParametersException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidFrequencyControlledTaskId;
import com.gip.xyna.xprc.exceptions.XPRC_TimeoutWhileWaitingForUnaccessibleOrderException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcess;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.XynaFrequencyControl;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse.FillingMode;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;



/**
 * handling von aufträgen und auftragseingangsschnittstellen in einer revision
 */
public class RevisionOrderControl {
  
  //default order entries
  private static final String OrderEntryRMI = "RMI";
  private static final String OrderEntryCLI = "CLI";

  private static Long DefaultOrderEntryRevision = -2L;


  private static final Logger logger = CentralFactoryLogging.getLogger(RevisionOrderControl.class);

  protected final long revision;

  protected final XynaActivationTrigger xt = XynaFactory.getInstance().getActivation().getActivationTrigger();


  public RevisionOrderControl(long revision) {
    this.revision = revision;
  }


  /**
   * gibt true zurück sobald kein auftrag mehr in der revision am laufen ist und keine tco/clo's mehr existieren.
   * falls dies nicht der fall ist, wird maximal bis zum angegebenen timeout gewartet.
   * @return true falls keine aufträge und tco/clo's mehr am laufen sind, ansonsten false.
   */
  public boolean waitForRunningOrders(long relativeTimeout, TimeUnit unit) throws PersistenceLayerException,
      XPRC_TimeoutWhileWaitingForUnaccessibleOrderException {
    long absoluteTimeout = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(relativeTimeout, unit);
    while (DeploymentManagement.getInstance().isInUse(revision)) {
      long relative = absoluteTimeout - System.currentTimeMillis();
      if (relative <= 0) {
        return false;
      }
      try {
        Thread.sleep(Math.min(relative, 500));
      } catch (InterruptedException e) {
      }
    }
    return true;
  }


  //contains all order entries (includes RMI, CLI and custom order entry types)
  private static final ConcurrentMap<Long, OrderEntryInstances> closedOrderEntries = new ConcurrentHashMap<Long, OrderEntryInstances>();
  
  //key is the revision that registered the order entry type
  //second key is the name of the CustomOrderEntry
  private static final ConcurrentHashMap<Long, ConcurrentHashMap<String, CustomOrderEntryInformation>> customOrderEntryTypes =
      new ConcurrentHashMap<Long, ConcurrentHashMap<String, CustomOrderEntryInformation>>();


  /**
   * dürfen über die rmi schnittstelle aufträge in dieser application gestartet werden?
   * @param revision
   */
  public static void checkRmiClosed(long revision) {
    checkRmiCliClosed(revision, OrderEntranceType.RMI);
  }

  /**
   * dürfen über die cli schnittstelle aufträge in dieser application gestartet werden?
   * @param revision
   */
  public static void checkCliClosed(long revision) {
    checkRmiCliClosed(revision, OrderEntranceType.CLI);
  }
  
  /**
   * dürfen über die rmi schnittstelle aufträge in dieser application gestartet werden?
   * @param applicationName
   * @param versionName
   */
  public static void checkRmiClosed(String applicationName, String versionName) {
    checkRmiCliClosed(applicationName, versionName, OrderEntranceType.RMI);
  }

  /**
   * dürfen über die cli schnittstelle aufträge in dieser application gestartet werden?
   * @param applicationName
   * @param versionName
   */
  public static void checkCliClosed(String applicationName, String versionName) {
    checkRmiCliClosed(applicationName, versionName, OrderEntranceType.CLI);
  }
  
  /**
   * dürfen über die rmi bzw. cli schnittstelle aufträge in dieser application gestartet werden?
   * @param revision
   * @param type RMI oder CLI
   */
  private static void checkRmiCliClosed(long revision, OrderEntranceType type) {
    Boolean val = getRmiCliClosed(revision, type);
    
    if (val != null && val) {
      RuntimeContext runtimeContext;
      try {
        runtimeContext =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      throw new RuntimeException("orders may not be started in " + runtimeContext + ".");
    }
  }


  /**
   * dürfen über die rmi bzw. cli schnittstelle aufträge in dieser application gestartet werden?
   * @param applicationName
   * @param versionName
   * @param type RMI oder CLI
   */
  private static void checkRmiCliClosed(String applicationName, String versionName,  OrderEntranceType type) {
    long revision;
    try {
      revision =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRevision(applicationName, versionName, null);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    Boolean val = getRmiCliClosed(revision, type);
    if (val != null && val) {
      throw new RuntimeException("orders may not be started in application '" + applicationName + "' / '" + versionName + "'.");
    }
  }


  public static Boolean getRmiCliClosed(long revision, OrderEntranceType type) {
    
    OrderEntryInstances oei = closedOrderEntries.get(revision);
    if (oei == null) {
      oei = new OrderEntryInstances();
      closedOrderEntries.put(revision, oei);
    }
    
    switch (type) {
      case RMI:
        return oei.isEntryClosed(DefaultOrderEntryRevision, OrderEntryRMI);
      case CLI:
        return oei.isEntryClosed(DefaultOrderEntryRevision, OrderEntryCLI);
      default:
        throw new IllegalStateException("Unexpected orderEntranceType "+ type);
    }
  }


  public static boolean checkCustomOrderEntryClosed(Long orderRevision, Long definingRevision, String orderEntryType) {
    if (closedOrderEntries.get(orderRevision) == null) {
      closedOrderEntries.put(orderRevision, new OrderEntryInstances());
    }
    
    return closedOrderEntries.get(orderRevision).isEntryClosed(definingRevision, orderEntryType);
  }


  /**
   *  closes all custom order entries for the given revision
   **/
  public static void closeCustomOrderEntries(Long revision) {
    OrderEntryInstances closedEntriesForRevision = closedOrderEntries.get(revision);

    if (closedEntriesForRevision == null) {
      closedEntriesForRevision = new OrderEntryInstances();
      closedOrderEntries.put(revision, closedEntriesForRevision);
    }

    Collection<CustomOrderEntryInformation> coe = getAllCustomOrderEntryTypes();
    for (CustomOrderEntryInformation c : coe) {
      closedEntriesForRevision.closeEntry(c.getDefiningRevision(), c.getName());
    }
  }


  /**
   * opens all custom order entries for the given revision
   */
  public static void openCustomOrderEntries(Long revision) {
    OrderEntryInstances closedEntriesForRevision = closedOrderEntries.get(revision);

    if (closedEntriesForRevision == null) {
      closedEntriesForRevision = new OrderEntryInstances();
      closedOrderEntries.put(revision, closedEntriesForRevision);
    }

    Collection<CustomOrderEntryInformation> coe = getAllCustomOrderEntryTypes();
    for (CustomOrderEntryInformation c : coe) {
      closedEntriesForRevision.openEntry(c.getDefiningRevision(), c.getName());
    }   
  }


  /**
   * entfernt alle in der revision laufenden aufträge und time-controlled orders/crons.
   */
  public void killRunningOrders() throws XPRC_TimeoutWhileWaitingForUnaccessibleOrderException, PersistenceLayerException {
    OrdersInUse ordersInUse = DeploymentManagement.getInstance().getInUse(revision, FillingMode.OnlyIds);
    CronLikeScheduler cls = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();

    final CountDownLatch latch = new CountDownLatch(ordersInUse.getRootOrdersAndBatchProcesses().size());
    for (final Long orderId : ordersInUse.getRootOrdersAndBatchProcesses()) {
      //TODO threadpool verwenden, weil planning synchron lange dauert?
      //TODO funktioniert das so mit batchprozessen?
      KillStuckProcessBean bean = new KillStuckProcessBean(orderId, true, AbortionCause.DEPLOYMENT, true);
      try {
        ((XynaProcessing) XynaFactory.getInstance().getProcessing()).killStuckProcess(bean, false, new ResponseListener() {

          private static final long serialVersionUID = -4874174579526675019L;


          @Override
          public void onResponse(GeneralXynaObject response, OrderContext ctx) throws XNWH_RetryTransactionException {
            latch.countDown();
          }


          @Override
          public void onError(XynaException[] e, OrderContext ctx) throws XNWH_RetryTransactionException {
            logger.warn("could not abort order " + orderId, e[0]);
            for (int i = 1; i < e.length; i++) {
              logger.warn("other exceptions: ", e[i]);
            }
            latch.countDown();
          }

        });
        if (logger.isDebugEnabled()) {
          logger.debug("aborted order " + orderId + ". abortion result = " + bean.getResultMessage());
        }
      } catch (Throwable t) {
        Department.handleThrowable(t);
        //abort auftrag konnte nicht gestartet werden -> latch.countdown durchführen, damit nicht ewig gewartet wird.
        logger.warn("could not abort order " + orderId, t);
        latch.countDown();
      }
    }

    try {
      if (!latch.await(5 * 60, TimeUnit.SECONDS)) {
        logger.warn("Timeout waiting for abortion of running orders");
      }
    } catch (InterruptedException e) {
      logger.warn("interrupted: don't wait for running killprocess orders");
    }

    if (ordersInUse.getRootOrdersAndBatchProcesses().size() > 0) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        //kurz warten, evtl verschwinden durch das kill bereits crons
      }
    }

    for (Long cronId : ordersInUse.getCronIds()) {
      try {
        if (cls.removeCronLikeOrder(new CronLikeOrder(cronId))) {
          disabledCrons.remove(cronId);
          if (logger.isDebugEnabled()) {
            logger.debug("removed cronlike order " + cronId + ".");
          }
        } else {
          if (logger.isDebugEnabled()) {
            //kein fehler. der cron ist evtl von allein losgelaufen oder wurde vom kill eines zugehörigen auftrags entfernt
            logger.debug("could not remove cronlike order " + cronId + ".");
          }
        }
      } catch (XPRC_CronLikeOrderStorageException e) {
        logger.warn("could not abort cron " + cronId, e);
      } catch (XPRC_CronRemovalException e) {
        logger.warn("could not abort cron " + cronId, e);
      }
    }

    XynaFrequencyControl fc = XynaFactory.getInstance().getProcessing().getFrequencyControl();
    for (Long fctid : ordersInUse.getFrequencyControlledTaskIds()) {
      try {
        FrequencyControlledTaskInformation fqi = fc.getFrequencyControlledTaskInformation(fctid, null);
        if (fqi == null) {
          continue;
        }
        switch (fqi.getStatusAsEnum()) {
          case Canceled :
          case Error :
          case Finished :
            continue;
          default :
            //abbrechen
        }
        fc.cancelFrequencyControlledTask(fctid);
        if (logger.isDebugEnabled()) {
          logger.debug("removed frequencycontrolled task " + fctid + ".");
        }
      } catch (XPRC_InvalidFrequencyControlledTaskId e) {
        logger.warn("could not abort frequencycontrolled task " + fctid, e);
      } catch (XPRC_IllegalStateForTaskArchiving e) {
        logger.warn("could not abort frequencycontrolled task " + fctid, e);
      }
    }
  }


  protected final List<EventListenerInstance<?, ?>> stoppedTriggerInstances = new ArrayList<EventListenerInstance<?, ?>>();
  protected final List<EventListenerInstance<?, ?>> disabledTriggerInstances = new ArrayList<EventListenerInstance<?, ?>>();
  protected final List<ConnectionFilterInstance<?>> disabledFilterInstances = new ArrayList<ConnectionFilterInstance<?>>();
  protected final Set<Long> disabledCrons = new HashSet<Long>();
  protected final Set<Long> pausedFqTasks = new HashSet<Long>();


  public static class OrderEntryInterfacesCouldNotBeClosedException extends Exception {

    public OrderEntryInterfacesCouldNotBeClosedException(Throwable e) {
      super(e);
    }


    private static final long serialVersionUID = -102459577351622076L;

  }


  /**
   * stoppt in der revision alle trigger, time-controlled orders/crons und verhindert, dass weitere
   * aufträge in dieser revision über rmi/cli eingestellt werden.
   * filter werden auch disabled, damit keine aufträge über trigger in anderen revisions (abwärtskompatibel) eingestellt werden.
   * 
   * FIXME man muss auch verhindern, dass neue tco/clo's erstellt werden.
   * 
   * tco/clo's die eine interne order starten werden nicht angehalten (vergleiche {@link XynaDispatcher#INTERNAL_ORDER_TYPES}).
   * @param blackList objekte aus der blacklist werden nicht gestoppt. darf null sein
   * @param temporarily Schnittstellen sollen nur temporär gestoppt werden
   */
  public void closeOrderEntryInterfaces(RevisionContentBlackWhiteList blackList, boolean temporarily) throws OrderEntryInterfacesCouldNotBeClosedException {
    stoppedTriggerInstances.clear();
    disabledFilterInstances.clear();
    disabledCrons.clear();
    pausedFqTasks.clear();

    boolean success = false;
    try {
      if (temporarily) {
        //Trigger nur temporär anhalten, Filter disablen
        stopTriggerInstances(blackList);
        disableFilterInstances(blackList);
      } else {
        //Trigger disablen (Filter werden dadurch automatisch auch disabled)
        disableTriggerInstances(blackList, false);
      }

      try {
        //Cron Like Orders disablen
        disableCrons(null);
        
        //Time Controlled Orders disablen
        BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
        bpm.closeBatchProcessEntrance(revision);
      }
      catch (XPRC_CronLikeOrderStorageException e) {
        throw new OrderEntryInterfacesCouldNotBeClosedException(e);
      }
      catch (PersistenceLayerException e) {
        throw new OrderEntryInterfacesCouldNotBeClosedException(e);
      }
      
      //Frequency Controlled Tasks pausieren
      pauseFCTasks();
      
      //RMI/CLI-Schnittstellen schließen
      closeRMICLI();
      
      success = true;
    } finally {
      if (!success) {
        openPreviouslyClosedOrderEntryInterfaces();
      }
    }
  }


  private void stopTriggerInstances(RevisionContentBlackWhiteList blackList) throws OrderEntryInterfacesCouldNotBeClosedException {
    try {
      Collection<TriggerInstanceInformation> tiis = xt.getTriggerInstanceInformation(revision);
      for (TriggerInstanceInformation tii : tiis) {
        if (blackList == null || !blackList.getTriggersNames().contains(tii.getTriggerName())) {
          EventListenerInstance<?, ?> triggerInstance = xt.getEventListenerInstanceByName(tii.getTriggerInstanceName(), tii.getRevision(), false);
          if (triggerInstance != null && (blackList == null || !blackList.getTriggerInstanceNames().contains(triggerInstance.getInstanceName()))) {
            triggerInstance.getEL().stop();
            stoppedTriggerInstances.add(triggerInstance);
          }
        }
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XACT_TriggerCouldNotBeStoppedException e) {
      throw new OrderEntryInterfacesCouldNotBeClosedException(e);
    }
  }
  
  /**
   * disabled TriggerInstances und zugehörige Filterinstances
   * @param blackList
   * @param temporarily
   * @throws OrderEntryInterfacesCouldNotBeClosedException
   */
  public void disableTriggerInstances(RevisionContentBlackWhiteList blackList, boolean temporarily) throws OrderEntryInterfacesCouldNotBeClosedException {
    disabledTriggerInstances.clear();
    disabledFilterInstances.clear();
    
    try {
      Collection<TriggerInstanceInformation> tiis = xt.getTriggerInstanceInformation(revision);
      for (TriggerInstanceInformation tii : tiis) {
        if (blackList == null || !blackList.getTriggersNames().contains(tii.getTriggerName())) {
          if (temporarily) {
            EventListenerInstance<?, ?> triggerInstance =
                xt.getEventListenerInstanceByName(tii.getTriggerInstanceName(), tii.getRevision(), false);
            if (triggerInstance != null) {
              //nur enabled Triggerinstanzen disablen und EventListenerinstanz für späteres enablen merken
              if (blackList == null || !blackList.getTriggerInstanceNames().contains(triggerInstance.getInstanceName())) {
                //Trigger- und Filterinstanzen merken, damit sie später wieder enabled werden können
                disabledFilterInstances.addAll(Arrays.asList(triggerInstance.getEL().getAllFilters()));
                disabledTriggerInstances.add(triggerInstance);
                //Trigger- und Filterinstanzen disablen
                xt.disableTriggerInstance(triggerInstance.getInstanceName(), revision, true);
              }
            }
          } else { //auch fehlerhafte Triggerinstanzen disablen (z.B. für stopApplication)
            if (blackList == null || !blackList.getTriggerInstanceNames().contains(tii.getTriggerInstanceName())) {
              if (tii.getState() != TriggerInstanceState.DISABLED) {
                xt.disableTriggerInstance(tii.getTriggerInstanceName(), revision, true);
              }
            }
          }
        }
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    } catch (PersistenceLayerException e) {
      throw new OrderEntryInterfacesCouldNotBeClosedException(e);
    } catch (XACT_TriggerInstanceNotFound e) {
      throw new RuntimeException(e);
    } catch (XACT_TriggerNotFound e) {
      throw new RuntimeException(e);
    }
  }
  
  
  /**
   * Enabled alle disabledTriggerInstances. Dabei werden zunächst die 
   * Filterinstanzen enabled, dann die Triggerinstanzen und
   * zuletzt der Thread gestartet.
   */
  public void enablePreviouslyDisabledTriggerInstances() {
    XynaActivationTrigger xat =  XynaFactory.getInstance().getActivation().getActivationTrigger();
    for (EventListenerInstance<?, ?> eli: disabledTriggerInstances) {
      try {
        for (ConnectionFilterInstance<?> cf : disabledFilterInstances) {
          //nur vorher disabled wieder enablen
          try {
            xat.enableFilterInstance(cf.getInstanceName(), cf.getRevision());
          } catch (Throwable t) {
            Department.handleThrowable(t);
            logger.warn("Failed to enable filter instance " + cf.getInstanceName(), t);
          }
        }
        xat.enableTriggerInstance(eli.getInstanceName(), eli.getRevision(), true, -1, false);
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.warn("Failed to enable trigger instance " + eli.getInstanceName(), t);
      }
    }
  }
  

  private void disableFilterInstances(RevisionContentBlackWhiteList blackList) throws OrderEntryInterfacesCouldNotBeClosedException {
    try {
      Collection<FilterInstanceInformation> fiis = xt.getFilterInstanceInformations(revision);
      for (FilterInstanceInformation fii : fiis) {
        if (blackList == null || !blackList.getFilterNames().contains(fii.getFilterName())) {
          ConnectionFilterInstance<?> filterInstance =
              xt.getFilterInstance(fii.getFilterInstanceName(), fii.getTriggerInstanceName(), fii.getRevision());
          if (filterInstance != null && (blackList == null || !blackList.getFilterInstanceNames().contains(filterInstance.getInstanceName()))) {
            xt.disableFilterInstance(filterInstance, true);
            disabledFilterInstances.add(filterInstance);
          }
        }
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    } catch (PersistenceLayerException e) {
      throw new OrderEntryInterfacesCouldNotBeClosedException(e);
    } catch (XACT_TriggerInstanceNotFound e) {
      throw new RuntimeException(e);
    }
  }
  
  
  /**
   * Überprüft, ob alle Auftragseingangsschnittstellen geschlossen sind.
   * @return
   * @throws PersistenceLayerException
   */
  public boolean orderEntryInterfacesClosed() throws PersistenceLayerException {
    //sind alle Trigger disabled?
    Collection<TriggerInstanceInformation> tiis = xt.getTriggerInstanceInformation(revision);
    for (TriggerInstanceInformation tii : tiis) {
      if (tii.getState() == TriggerInstanceState.ENABLED) {
        return false;
      }
    }
    
    //Filter können nicht laufen, wenn alle Trigger disabled sind
    //-> müssen nicht extra überprüft werden
    
    //sind alle CLOs disabled?
    CronLikeScheduler cls = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();
    ODSConnection con = ODSImpl.getInstance().openConnection();
    
    try {
      FactoryWarehouseCursor<CronLikeOrder> cursor = cls.getCursorForCronLikeOrders(con, 50, revision, null, false);
      List<CronLikeOrder> clos = cursor.getRemainingCacheOrNextIfEmpty();
      while (clos.size() > 0) {
        for (CronLikeOrder clo : clos) {
          if (clo.isEnabled()) {
            return false; //CLO ist nicht disabled
          }
        }
        clos = cursor.getRemainingCacheOrNextIfEmpty();
      }
    } finally {
      finallyClose(con);
    }
    
    //sind alle TCOs pausiert?
    BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
    if (!bpm.batchProcessEntranceClosed(revision)) {
      return false;
    }
    
    //sind alle orderEntry-Schnittstellen geschlossen?
    if(!allOrderEntriesClosed(revision)) {
      return false;
    }
    
    return true; //alle Auftragseingangsschnittstellen sind geschlossen
  }
  
  
  public void openRMICLI() {
    OrderEntryInstances oei = closedOrderEntries.get(revision);
    if (oei == null) {
      oei = new OrderEntryInstances();
      closedOrderEntries.put(revision, oei);
    }

    oei.openEntry(DefaultOrderEntryRevision, OrderEntryCLI);
    oei.openEntry(DefaultOrderEntryRevision, OrderEntryRMI);
  }


  public void closeRMICLI() {
    OrderEntryInstances oei = closedOrderEntries.get(revision);
    if (oei == null) {
      oei = new OrderEntryInstances();
      closedOrderEntries.put(revision, oei);
    }

    if(logger.isDebugEnabled()) {
      logger.debug("closing revision: " + revision);
    }
    
    oei.closeEntry(DefaultOrderEntryRevision, OrderEntryCLI);
    oei.closeEntry(DefaultOrderEntryRevision, OrderEntryRMI);
  }

  /**
   * Öffnet die RMI und/oder CLI Schnittstellen.
   * @param types Schnittstellen, die geöffnet werden sollen.
   *  Wird null übergeben, so werden beide geöffnet.
   */
  public void openRMICLI(EnumSet<OrderEntranceType> types) {
    OrderEntryInstances oei = closedOrderEntries.get(revision);
    if (oei == null) {
      oei = new OrderEntryInstances();
      closedOrderEntries.put(revision, oei);
    }
    if (types == null || types.contains(OrderEntranceType.RMI)) {
      oei.openEntry(DefaultOrderEntryRevision, OrderEntryRMI);
    }
    if (types == null || types.contains(OrderEntranceType.CLI)) {
      oei.openEntry(DefaultOrderEntryRevision, OrderEntryCLI);
    }
  }
  
  
  /**
   * Schließt die RMI und/oder CLI Schnittstellen.
   * @param types Schnittstellen, die geöffnet werden sollen.
   *  Wird null übergeben, so werden beide geschloßen.
   */
  public void closeRMICLI(EnumSet<OrderEntranceType> types) {
    OrderEntryInstances oei = closedOrderEntries.get(revision);
    if (oei == null) {
      oei = new OrderEntryInstances();
      closedOrderEntries.put(revision, oei);
    }
    if (types == null || types.contains(OrderEntranceType.RMI)) {
      

      if(logger.isDebugEnabled()) {
        logger.debug("closing revision: " + revision);
      }
      
      oei.closeEntry(DefaultOrderEntryRevision, OrderEntryRMI);
    }
    if (types == null || types.contains(OrderEntranceType.CLI)) {
      oei.closeEntry(DefaultOrderEntryRevision, OrderEntryCLI);
    }
  }


  /**
   * startet in der revision alle trigger, tco/clo's, die vorher über {@link #closeOrderEntryInterfaces(RevisionContentBlackWhiteList, boolean)} 
   * ausgeschaltet wurden und die noch existieren.
   * öffnet rmi/cli schnittstellen für diese revision.
   * 
   * darf mehrfach aufgerufen werden.
   */
  public void openPreviouslyClosedOrderEntryInterfaces() {
    openRMICLI();
    
    RuntimeContext runtimeContext;
    try {
      runtimeContext =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }

    //erst filter aktivieren, dann trigger starten
    Iterator<ConnectionFilterInstance<?>> it = disabledFilterInstances.iterator();
    while (it.hasNext()) {
      ConnectionFilterInstance<?> cfi = it.next();
      try {
        xt.enableFilterInstance(cfi.getInstanceName(), revision);
        it.remove();
      } catch (XACT_FilterImplClassNotFoundException |
          XACT_IncompatibleFilterImplException |
          XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY |
          XACT_FilterNotFound |
          XACT_TriggerInstanceNotFound |
          XFMG_SHARED_LIB_NOT_FOUND |
          XACT_LibOfFilterImplNotFoundException |
          XACT_OldFilterVersionInstantiationException |
          XACT_FilterInstanceNeedsEnabledFilterException |
          XACT_InvalidFilterConfigurationParameterValueException 
          e) {
        //unerwarteter Fehler beim Deployen: Filter war vorher ja fehlerfrei deployt
        throw new RuntimeException(e);
      } catch (PersistenceLayerException e) {
        //unerwartetes DB-Problem, kann hier aber nicht behandelt werden. Ignorieren, um andere Filter enablen zu können.
        //TODO Fehler sammeln und anschließend weiterwerfen?
        logger.warn("could not enable filter instance " + cfi.getInstanceName() + " in " + runtimeContext + ".", e);
      }
    }

    Iterator<EventListenerInstance<?, ?>> itt = stoppedTriggerInstances.iterator();
    while (itt.hasNext()) {
      EventListenerInstance triggerInstance = itt.next();
      try {
        triggerInstance.getEL().start(triggerInstance.getStartParameter());
        itt.remove();
      } catch (XACT_TriggerCouldNotBeStartedException e) {
        logger.warn("could not restart trigger instance " + triggerInstance.getInstanceName() + " in " + runtimeContext + ".", e);
      }
    }
    
    enablePreviouslyDisabledTriggerInstances();
    
    
    //alle disabledCrons wieder enablen
    if (disabledCrons.size() > 0) {
      try {
        enableCrons(null, true);
      } catch (PersistenceLayerException e) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Long id : disabledCrons) {
          if (first) {
            first = false;
          } else {
            sb.append(", ");
          }
          sb.append(id);
        }
        logger.warn("could not reenable cron like orders: " + sb.toString(), e);
      }
    }
    
    //Time Controlled Orders wieder aktivieren
    BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
    try {
      bpm.openBatchProcessEntrance(revision);
    }
    catch (PersistenceLayerException e) {
      logger.warn("could not reenable time controlled orders", e);
    }
    
    //Frequency-Controlled Tasks fortsetzen
    for (Long fctId : pausedFqTasks) {
      XynaFrequencyControl xfc = XynaFactory.getInstance().getProcessing().getFrequencyControl();
      if (xfc.getActiveFrequencyControlledTask(fctId) != null) {
        xfc.resumeFrequencyControlledTasks(fctId);
      }
    }
  }

  /**
   * Disabled alle Crons für die angegebenen OrderTypes. Um alle Crons einer Revision zu disablen
   * muss 'null' für die orderTypes übergeben werden.
   * @param orderTypes
   * @throws XPRC_CronLikeOrderStorageException
   * @throws PersistenceLayerException
   */
  public void disableCrons(String[] orderTypes) throws XPRC_CronLikeOrderStorageException, PersistenceLayerException {
    CronLikeScheduler cls = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();
    ODSConnection con = ODSImpl.getInstance().openConnection();
    
    try {
      FactoryWarehouseCursor<CronLikeOrder> cursor = cls.getCursorForCronLikeOrders(con, 50, revision, orderTypes, false);
      List<CronLikeOrder> clos = cursor.getRemainingCacheOrNextIfEmpty();
      while (clos.size() > 0) {
        Set<Long> modifiedCrons = new HashSet<Long>();

        for (CronLikeOrder clo : clos) {
          if (!clo.isEnabled()) {
            continue; //CLO ist bereits disabled
          }
          
          if (revision == RevisionManagement.REVISION_DEFAULT_WORKSPACE) {
            if (OrdertypeManagement.internalOrdertypes.contains(clo.getCreationParameters().getOrderType())) {
              continue; //interne clo
            }
          }

          try {
            //disable cron
            cls.modifyCronLikeOrder(clo.getId(), null, null, null, null, null, null, null, null, false, null, null, null, null, null, clo,
                                    con);
            modifiedCrons.add(clo.getId());
          } catch (XPRC_InvalidCronLikeOrderParametersException e) {
            throw new RuntimeException(e);
          }
        }

        con.commit();
        disabledCrons.addAll(modifiedCrons);
        modifiedCrons.clear();
        clos = cursor.getRemainingCacheOrNextIfEmpty();
      }
    } finally {
      finallyClose(con);
    }
  }
  
  
  /**
   * Enabled alle Crons für die angegebenen OrderTypes. Um alle Crons einer Revision zu enablen
   * muss 'null' für die orderTypes übergeben werden.
   * @param orderTypes
   * @param onlyDisabled true, wenn nur die in 'disabledCrons' enthaltenden Crons wieder enabled werden sollen
   * @throws PersistenceLayerException
   */
  public void enableCrons(String[] orderTypes, boolean onlyDisabled) throws PersistenceLayerException {
    CronLikeScheduler cls = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();

    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      FactoryWarehouseCursor<CronLikeOrder> cursor = cls.getCursorForCronLikeOrders(con, 50, revision, orderTypes, false);
      List<CronLikeOrder> clos = cursor.getRemainingCacheOrNextIfEmpty();
      while (clos.size() > 0) {
        for (CronLikeOrder clo : clos) {
          if (clo.isEnabled()) {
            continue; //CLO ist schon enabled -> nichts zu machen
          }
          
          if (onlyDisabled && !disabledCrons.remove(clo.getId())) {
            continue; //CLO soll nicht enabled werden, weil sie nicht in den disabledCrons enthalten ist
          }
          
          if (revision == RevisionManagement.REVISION_DEFAULT_WORKSPACE) {
            if (OrdertypeManagement.internalOrdertypes.contains(clo.getCreationParameters().getOrderType())) {
              continue; //interne clo
            }
          }

          try {
            //enable cron
            cls.modifyCronLikeOrder(clo.getId(), null, null, null, null, null, null, null, null, true, null, null, null, null, null, clo,
                                    con);
          } catch (XPRC_InvalidCronLikeOrderParametersException e) {
            throw new RuntimeException(e);
          } catch (XPRC_CronLikeOrderStorageException e) {
            logger.warn("could not reenable cron like order " + clo.getId(), e);
          }
        }

        con.commit();
        clos = cursor.getRemainingCacheOrNextIfEmpty();
      }
    } finally {
      finallyClose(con);
    }
  }
  
  
  /**
   * Pausiert die Frequency-Controlled Tasks der Revision
   */
  public void pauseFCTasks(){
    for (Long fctId : getFrequencyControlledTaskIds()) {
      XynaFrequencyControl xfc = XynaFactory.getInstance().getProcessing().getFrequencyControl();
      xfc.pauseFrequencyControlledTasks(fctId);
      pausedFqTasks.add(fctId);
    }
  }

  /**
   * Setzt die Frequency-Controlled Tasks der Revision wieder fort
   */
  public void resumeFCTasks() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    for (Long fctId : getFrequencyControlledTaskIds()) {
      XynaFrequencyControl xfc = XynaFactory.getInstance().getProcessing().getFrequencyControl();
      xfc.resumeFrequencyControlledTasks(fctId);
    }
  }
  
  /**
   * Zählt die Cron Like Orders der Revision
   * @return Pair mit Anzahl an aktiven und inaktiven Cron Like Orders
   * @throws PersistenceLayerException
   */
  public Pair<Integer, Integer> countCrons() throws PersistenceLayerException {
    int enabled = 0;
    int disabled = 0;
    CronLikeScheduler cls = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();
    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      FactoryWarehouseCursor<CronLikeOrder> cursor = cls.getCursorForCronLikeOrders(con, 50, revision, null, false);
      List<CronLikeOrder> clos = cursor.getRemainingCacheOrNextIfEmpty();
      while (clos.size() > 0) {
        for (CronLikeOrder clo : clos) {
          if (revision == RevisionManagement.REVISION_DEFAULT_WORKSPACE) {
            if (OrdertypeManagement.internalOrdertypes.contains(clo.getCreationParameters().getOrderType())) {
              continue; //interne clo
            }
          }
          
          if (clo.isEnabled()) {
            enabled++;
          } else {
            disabled++;
          }
        }
        clos = cursor.getRemainingCacheOrNextIfEmpty();
      }
    } finally {
      finallyClose(con);
    }
    
    return Pair.of(enabled, disabled);
  }

  /**
   * Zählt die Batch Prozesse der Revision
   * @return Pair mit Anzahl an aktiven und inaktiven Batch Prozessen
   * @throws PersistenceLayerException
   */
  public Pair<Integer, Integer> countBatchProcesses() throws PersistenceLayerException {
    BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
    List<BatchProcess> batchProcesses = bpm.getBatchProcesses(revision);
    int active = 0;
    int paused = 0;
    for (BatchProcess bp: batchProcesses) {
      if (bp.isPaused()) {
        paused++;
      } else {
        active++;
      }
    }
    
    return Pair.of(active, paused);
  }

  /**
   * Zählt die Triggerinstanzen der Revision
   * @return Triple mit Anzahl an aktiven, inaktiven und fehlerhaften Triggerinstanzen
   * @throws PersistenceLayerException
   */
  public Triple<Integer, Integer, Integer> countTriggerInstances() throws PersistenceLayerException {
    Collection<TriggerInstanceStorable> triggerInstances;
    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      triggerInstances = con.loadCollection(TriggerInstanceStorable.class);
    } finally {
      finallyClose(con);
    }
    
    int enabled = 0;
    int disabled = 0;
    int erroneous = 0;
    for(TriggerInstanceStorable triggerInstance : triggerInstances) {
      if(triggerInstance.getRevision() == revision) {
        switch (triggerInstance.getStateAsEnum()) {
          case ENABLED:
            enabled++;
            break;
          case DISABLED:
            disabled++;
            break;
          case ERROR:
            erroneous++;
            break;
        }
      }
    }
    
    return Triple.of(enabled, disabled, erroneous);
  }
  
  /**
   * Zählt die Filterinstanzen der Revision
   * @return Triple mit Anzahl an aktiven, inaktiven und fehlerhaften Filterinstanzen
   * @throws PersistenceLayerException
   */
  public Triple<Integer, Integer, Integer> countFilterInstances() throws PersistenceLayerException {
    Collection<FilterInstanceStorable> filterInstances;
    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      filterInstances = con.loadCollection(FilterInstanceStorable.class);
    } finally {
      finallyClose(con);
    }
    
    int enabled = 0;
    int disabled = 0;
    int erroneous = 0;
    for(FilterInstanceStorable filterInstance : filterInstances) {
      if(filterInstance.getRevision() == revision) {
        switch (filterInstance.getStateAsEnum()) {
          case ENABLED:
            enabled++;
            break;
          case DISABLED:
            disabled++;
            break;
          case ERROR:
            erroneous++;
            break;
        }
      }
    }
    
    return Triple.of(enabled, disabled, erroneous);
  }
  
  
  public List<OrderEntrance> getErronousOrderEntrances() throws PersistenceLayerException {
    List<OrderEntrance> erroneous = new ArrayList<OrderEntrance>();
    Collection<TriggerInstanceStorable> triggerInstances;
    Collection<FilterInstanceStorable> filterInstances;
    
    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      triggerInstances = con.loadCollection(TriggerInstanceStorable.class);
      filterInstances = con.loadCollection(FilterInstanceStorable.class);
    } finally {
      finallyClose(con);
    }
    
    //Trigger
    for (TriggerInstanceStorable triggerInstance : triggerInstances) {
      if (triggerInstance.getRevision() == revision 
            && triggerInstance.getStateAsEnum().equals(TriggerInstanceState.ERROR)) {
        erroneous.add(new OrderEntrance(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.triggerInstance, triggerInstance.getTriggerInstanceName()));
      }
    }
    
    //Filter
    for(FilterInstanceStorable filterInstance : filterInstances) {
      if(filterInstance.getRevision() == revision
           && filterInstance.getStateAsEnum().equals(FilterInstanceState.ERROR)) {
        erroneous.add(new OrderEntrance(OrderEntranceType.filterInstance, filterInstance.getFilterInstanceName()));
      }
    }
    
    return erroneous;
  }
  
  /**
   * Liefert die Ids Frequency-Controlled Tasks der Revision
   */
  public Set<Long> getFrequencyControlledTaskIds() {
    DeploymentManagement dm = DeploymentManagement.getInstance();
    OrdersInUse fcts = dm.getFrequencyControlledTasks(revision,
                                                      FillingMode.OnlyIds, //nur die Ids bestimmen
                                                      0); //binding wird hier nicht gebraucht -> Wert ist egal
    
    return fcts.getFrequencyControlledTaskIds();
  }
  
  private static void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }
  
  /**
   * returns true if RMI, CLI and all custom order entry types are closed
   */
  private static boolean allOrderEntriesClosed(Long revision) {
    Collection<CustomOrderEntryInformation> allOrderEntries =
        customOrderEntryTypes.values().stream().flatMap(x -> x.values().stream().map(y -> y)).collect(Collectors.toList());
    OrderEntryInstances oei = closedOrderEntries.get(revision);

    if (!oei.isEntryClosed(DefaultOrderEntryRevision, OrderEntryRMI)) {
      return false;
    }

    if (!oei.isEntryClosed(DefaultOrderEntryRevision, OrderEntryCLI)) {
      return false;
    }

    for (CustomOrderEntryInformation orderEntry : allOrderEntries) {
      if (!oei.isEntryClosed(orderEntry.getDefiningRevision(), orderEntry.getName())) {
        return false;
      }
    }

    return true;
  }

  
  private static void applyDefaultBehavior(CustomOrderEntryInformation orderEntryInfo) {
    if (orderEntryInfo.getDefaultBehavior() == DefaultBehavior.alwaysOpen || orderEntryInfo.getDefaultBehavior() == null) {
      return; //nothing to be done
    }

    //iterate over all applications and set state.
    ApplicationManagementImpl applicationManagement =
        (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    List<ApplicationInformation> apps = applicationManagement.listApplications(true, false);
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    boolean enable;
    Long applicationRevision;
    for (ApplicationInformation app : apps) {
      enable = shouldCustomOrderEntryBeEnabled(orderEntryInfo.getDefaultBehavior(), app.getState().isRunning());
      try {
        applicationRevision = rm.getRevision(app.getName(), app.getVersion(), null);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        continue;
      }
      setCustomOrderEntryState(applicationRevision, orderEntryInfo.getDefiningRevision(), orderEntryInfo.getName(), enable);
    }
  }

  
  public static void applyCustomOrderEntries(String appName, String appVersion, boolean running) {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long applicationRevision;
    boolean enable = true;

    try {
      applicationRevision = rm.getRevision(appName, appVersion, null);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return;
    }
    //iterate over customOrderEntries.
    Collection<CustomOrderEntryInformation> coes = getAllCustomOrderEntryTypes();
    for (CustomOrderEntryInformation coe : coes) {
      enable = shouldCustomOrderEntryBeEnabled(coe.getDefaultBehavior(), running);

      setCustomOrderEntryState(applicationRevision, coe.getDefiningRevision(), coe.getName(), enable);
    }
  }


  private static boolean shouldCustomOrderEntryBeEnabled(DefaultBehavior db, boolean appRunning) {
    if (db == DefaultBehavior.alwaysOpen || db == null) {
      return true;
    } else if (db == DefaultBehavior.alwaysClosed) {
      return false;
    } else if (db == DefaultBehavior.appState) {
      return appRunning;
    } else {
      throw new RuntimeException("Unknown DefaultBehavior: '" + db + "'");
    }
  }


  private static void setCustomOrderEntryState(Long revisionToChange, Long definingRevision, String orderEntryName, boolean enable) {
    if (enable) {
      enableCustomOrderEntry(revisionToChange, definingRevision, orderEntryName);
    } else {
      disableCustomOrderEntry(revisionToChange, definingRevision, orderEntryName);
    }
  }


  public static void registerCustomOrderEntryType(Long revision, CustomOrderEntryInformation orderEntryInfo) {
    customOrderEntryTypes.putIfAbsent(revision, new ConcurrentHashMap<String, RevisionOrderControl.CustomOrderEntryInformation>());
    ConcurrentHashMap<String, CustomOrderEntryInformation> map = customOrderEntryTypes.get(revision);
    if(!map.contains(orderEntryInfo.getName())) {
      applyDefaultBehavior(orderEntryInfo);
    }
    map.put(orderEntryInfo.getName(), orderEntryInfo);
  }
  
  public static void unregisterCustomOrderEntryType(Long revision, String orderEntryInfoName) {
    customOrderEntryTypes.putIfAbsent(revision, new ConcurrentHashMap<String, RevisionOrderControl.CustomOrderEntryInformation>());
    ConcurrentHashMap<String, CustomOrderEntryInformation> map = customOrderEntryTypes.get(revision);
    map.remove(orderEntryInfoName);
  }
  
  public static void unregisterAllCustomOrderEntryTypes(Long revision) {
    customOrderEntryTypes.remove(revision);
  }
  
  
  public static Collection<CustomOrderEntryInformation> getAllCustomOrderEntryTypes(){
    return customOrderEntryTypes.values().stream().flatMap(x -> x.values().stream()).collect(Collectors.toList());
  }
  
  
  public static void enableCustomOrderEntry(Long revisionToOpen, Long definingRevision, String orderEntryName) {
    OrderEntryInstances oei = closedOrderEntries.get(revisionToOpen);
    if (oei == null) {
      oei = new OrderEntryInstances();
      closedOrderEntries.put(revisionToOpen, oei);
    }

    oei.openEntry(definingRevision, orderEntryName);
  }


  public static void disableCustomOrderEntry(Long revisionToClose, Long definingRevision, String orderEntryName) {
    OrderEntryInstances oei = closedOrderEntries.get(revisionToClose);
    if (oei == null) {
      oei = new OrderEntryInstances();
      closedOrderEntries.put(revisionToClose, oei);
    }

    oei.closeEntry(definingRevision, orderEntryName);
  }


  public static class CustomOrderEntryInformation {

    private String name;
    private String description;
    private Long definingRevision;
    private DefaultBehavior defaultBehavior;


    public void setName(String name) {
      this.name = name;
    }


    public String getName() {
      return name;
    }


    public void setDescription(String description) {
      this.description = description;
    }


    public String getDescription() {
      return description;
    }


    public void setDefiningRevision(Long definingRevision) {
      this.definingRevision = definingRevision;
    }


    public Long getDefiningRevision() {
      return definingRevision;
    }


    public void setDefaultBehavior(DefaultBehavior behavior) {
      defaultBehavior = behavior;
    }


    public DefaultBehavior getDefaultBehavior() {
      return defaultBehavior;
    }


    public static enum DefaultBehavior {
      alwaysOpen, alwaysClosed, appState
    }
  }

  
  /* orderEntry instance information for a single revision */
  private static class OrderEntryInstances {
      private final ConcurrentMap<Long, ConcurrentMap<String, Boolean>> closedEntries = new ConcurrentHashMap<Long, ConcurrentMap<String, Boolean>>();
      
      public void closeEntry(Long definingRevision, String entryName) {
        closedEntries.putIfAbsent(definingRevision, new ConcurrentHashMap<String, Boolean>());
        ConcurrentMap<String, Boolean> map = closedEntries.get(definingRevision);
        map.put(entryName, true);
      }
      
      public void openEntry(Long definingRevision, String entryName) {
         ConcurrentMap<String, Boolean> map = closedEntries.get(definingRevision);
          if(map == null) {
            //nothing to be done - all entries are open already
            return;
          }
          map.remove(entryName);
      }
      
      public boolean isEntryClosed(Long definingRevision, String entryName) {
          return closedEntries.containsKey(definingRevision) && 
              closedEntries.get(definingRevision).containsKey(entryName) &&
              closedEntries.get(definingRevision).get(entryName) == true;
      }
  }
}
