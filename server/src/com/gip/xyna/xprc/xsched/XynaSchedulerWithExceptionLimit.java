///*
// * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// * Copyright 2023 Xyna GmbH, Germany
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *  http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// */
//package com.gip.xyna.xprc.xsched;
//
//
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.SortedMap;
//import java.util.TreeMap;
//import java.util.concurrent.PriorityBlockingQueue;
//import java.util.concurrent.locks.ReentrantLock;
//
//import org.apache.log4j.Logger;
//import org.apache.log4j.NDC;
//
//import com.gip.xyna.CentralFactoryLogging;
//import com.gip.xyna.Department;
//import com.gip.xyna.Section;
//import com.gip.xyna.XynaFactory;
//import com.gip.xyna.XynaRuntimeException;
//import com.gip.xyna.utils.exceptions.XynaException;
//import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
//import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
//import com.gip.xyna.xprc.Codes;
//import com.gip.xyna.xprc.ResponseListenerWithSuspensionSupport;
//import com.gip.xyna.xprc.SeriesInformation;
//import com.gip.xyna.xprc.XynaExecutor;
//import com.gip.xyna.xprc.XynaOrder;
//import com.gip.xyna.xprc.XynaOrderServerExtension;
//import com.gip.xyna.xprc.xfractwfe.ProcessSuspendedException;
//import com.gip.xyna.xprc.xpce.cleanup.XynaCleanup;
//import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
//import com.gip.xyna.xprc.xprcods.currprocdb.OrderInstanceStatus;
//import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
//import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeSchedulerFactory;
//import com.gip.xyna.xprc.xsched.ordercancel.ICancelResultListener;
//
//
//
//public class XynaSchedulerWithExceptionLimit extends Section implements IPropertyChangeListener, SchedulerAlgorithm {
//
//  public static final String DEFAULT_NAME = "Xyna Scheduler";
//  private static Logger logger = CentralFactoryLogging.getLogger(XynaScheduler.class);
//
//  private boolean createLoggingContext = false;
//
//  private OrderTimeoutMaintenanceRunnable schedulingMaintenanceRunnable;
//  private CancelTimeoutMaintenanceRunnable schedulingCancelRunnable;
//  private Thread orderTimeoutMaintenanceThread;
//  private Thread cancelTimeoutMaintenanceThread;
//
//  private Comparator<ICancelResultListener> cancelTimeoutComparator;
//
//  private final HashMap<Long, XynaOrderServerExtension> suspendedOrders;
//  private final ReentrantLock suspendedOrdersLock;
//
//  private OrderSeriesManagement orderSeriesManagement;
//  private CronLikeScheduler cronLikeScheduler;
//  private CapacityManagement capacityManagement;
//  private PreScheduler preScheduler;  
//  private SchedulerRunnable innerSchedulerRunnable;
//
//  private ReentrantLock ordersLock;
//  private ReentrantLock ordersBeingCheckedForSchedLock;
//  private ReentrantLock schedulerCntLock;
//  private volatile int schedulerCnt;
//
//  private LinkedList<XynaOrderServerExtension> orders;
//  private LinkedList<XynaOrderServerExtension> ordersBeingCheckedForSched;
//  private Comparator<XynaOrderServerExtension> schedulerComparator;
//
//  private int MAX_THROWABLES_BEFORE_STOP = 10;
//
//  public XynaSchedulerWithExceptionLimit() throws XynaException {
//    super();
//    suspendedOrders = new HashMap<Long, XynaOrderServerExtension>();
//    suspendedOrdersLock = new ReentrantLock();
//  }
//
//
//  @Override
//  public String getDefaultName() {
//    return DEFAULT_NAME;
//  }
//
//
//  @Override
//  public void init() throws XynaException {
//
//    capacityManagement = CapacityManagementFactory.createCapacityManagement();
//    deployFunctionGroup(capacityManagement);
//
//    preScheduler = PreSchedulerFactory.createPreScheduler();
//    deployFunctionGroup(preScheduler);
//
//    cronLikeScheduler = CronLikeSchedulerFactory.createCronLikedScheduler();
//    deployFunctionGroup(cronLikeScheduler);
//
//    orderSeriesManagement = OrderSeriesManagementFactory.createOrderSeriesManagement();
//    deployFunctionGroup(orderSeriesManagement);
//
//    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
//                    .addPropertyChangeListener(this);    
//
//    ordersLock = new ReentrantLock();
//    ordersBeingCheckedForSchedLock = new ReentrantLock();
//    orders = new LinkedList<XynaOrderServerExtension>();
//    ordersBeingCheckedForSched = new LinkedList<XynaOrderServerExtension>();
//
//    schedulingMaintenanceRunnable = new OrderTimeoutMaintenanceRunnable(this);
//
//    orderTimeoutMaintenanceThread = new Thread(schedulingMaintenanceRunnable);
//    orderTimeoutMaintenanceThread.setName("Scheduler Order Maintenance Thread");
//    orderTimeoutMaintenanceThread.setPriority(Thread.MAX_PRIORITY);
//
//    orderTimeoutMaintenanceThread.start();
//
//
//    // TODO cancel zeugs in cancelrunnable stecken, wie bei timeoutbehandlung
//    // cancel listeners and timeout
//    cancelTimeoutComparator = new Comparator<ICancelResultListener>() {
//
//      public int compare(ICancelResultListener o1, ICancelResultListener o2) {
//        if (o1.getAbsoluteCancelTimeout() == null)
//          return -1;
//        if (o2.getAbsoluteCancelTimeout() == null)
//          return 1;
//        if (o1.getAbsoluteCancelTimeout().compareTo(o2.getAbsoluteCancelTimeout()) > 0)
//          return 1;
//        else if (o1.getAbsoluteCancelTimeout().compareTo(o2.getAbsoluteCancelTimeout()) < 0)
//          return -1;
//        else
//          return 0;
//      }
//
//    };
//    cancelListenerQueue = new PriorityBlockingQueue<ICancelResultListener>(1, cancelTimeoutComparator);
//    cancelListenerLock = new ReentrantLock();
//
//    schedulingCancelRunnable = new CancelTimeoutMaintenanceRunnable(cancelListenerQueue, this);
//    cancelTimeoutMaintenanceThread = new Thread(schedulingCancelRunnable);
//    cancelTimeoutMaintenanceThread.setName("Scheduler Cancel Maintenance Thread");
//
//    cancelTimeoutMaintenanceThread.start();
//
//    schedulerCntLock = new ReentrantLock();
//    schedulerComparator = new Comparator<XynaOrderServerExtension>() {
//
//      private boolean mustBeResumed(XynaOrderServerExtension xo) {
//        return xo.getProcessInstance() != null && (xo.getProcessInstance().isSuspended() || xo.getProcessInstance()
//                        .attemptingSuspension());
//      }
//
//
//      // negativ, falls o1 > o2
//      // da die sortierte liste mit hohen priorit�ten anfangen soll, und
//      // niedrige priorit�ten kleiner sind als grosse,
//      // gilt also:
//      public int compare(XynaOrderServerExtension o1, XynaOrderServerExtension o2) {
//        if (mustBeResumed(o1)) {
//          if (mustBeResumed(o2)) {
//            return 0;
//          } else {
//            return -1;
//          }
//        }
//        if (mustBeResumed(o2))
//          return 1;
//        if (o1.isCancelled()) {
//          if (o2.isCancelled()) {
//            return 0;
//          } else {
//            return -1;
//          }
//        }
//        if (o2.isCancelled())
//          return 1;
//        if (o1.isTimedOut()) {
//          if (o2.isTimedOut()) {
//            return 0;
//          } else {
//            return -1;
//          }
//        }
//        if (o2.isTimedOut())
//          return 1;
//        return o2.getPriority() - o1.getPriority();
//      }
//    };
//
//    innerSchedulerRunnable = new SchedulerRunnable();
//    innerSchedulerRunnable.startNewSchedulerThread(this);
//  }
//
//
//  /**
//   * beim start des servers wird der scheduler mit einem comparator initialisiert, der beim serverstart resumte auftr�ge
//   * bevorzugt. wegen schlechterer performance wird er dann mittels dieser methode ausgetauscht gegen einen der das
//   * nicht mehr ber�cksichtigt.
//   */
//  public void setDefaultComparator() {
//    schedulerComparator = new Comparator<XynaOrderServerExtension>() {
//
//      // negativ, falls o1 > o2
//      // da die sortierte liste mit hohen priorit�ten anfangen soll, und
//      // niedrige priorit�ten kleiner sind als grosse,
//      // gilt also:
//      public int compare(XynaOrderServerExtension o1, XynaOrderServerExtension o2) {
//        if (o1.isCancelled()) {
//          if (o2.isCancelled()) {
//            return 0;
//          } else {
//            return -1;
//          }
//        }
//        if (o2.isCancelled())
//          return 1;
//        if (o1.isTimedOut()) {
//          if (o2.isTimedOut()) {
//            return 0;
//          } else {
//            return -1;
//          }
//        }
//        if (o2.isTimedOut())
//          return 1;
//        return o2.getPriority() - o1.getPriority();
//      }
//
//    };
//  }
//
//
//  /**
//   * Override the super method to remove the property change listener and shutdown the runnables
//   */
//  @Override
//  public void shutdown() throws XynaException {
//
//    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
//                    .removePropertyChangeListener(this);
//
//    shutdownRunnables();
//
//    super.shutdown();
//
//  }
//
//
//  /**
//   * Package private shutdown class to stop the runnables without touching any factory stuff
//   */
//  void shutdownRunnables() {
//
//    schedulingMaintenanceRunnable.stop(orderTimeoutMaintenanceThread);
//    
//    logger.debug("Stopping cancel maintenance thread");
//    synchronized (cancelListenerQueue) {
//      schedulingCancelRunnable.stop();
//      cancelTimeoutMaintenanceThread.interrupt();
//    }
//    
//    logger.debug("Stopping internal scheduling thread");
//    innerSchedulerRunnable.stopSchedulingThread();
//  }
//
//
//  public CapacityManagement getCapacityManagement() {
//    CapacityManagement cap = (CapacityManagement) getFunctionGroup(CapacityManagement.DEFAULT_NAME);
//    if (cap == null)
//      logger.debug("tried to access undeployed function group " + CapacityManagement.DEFAULT_NAME);
//
//    return cap;
//  }
//
//
//  public OrderSeriesManagement getOrderSeriesManagement() {
//    return orderSeriesManagement;
//  }
//
//
//  public PreScheduler getPreScheduler() {
//    PreScheduler presched = (PreScheduler) getFunctionGroup(PreScheduler.DEFAULT_NAME);
//    if (presched == null)
//      logger.debug("tried to access undeployed function group " + PreScheduler.DEFAULT_NAME);
//
//    return presched;
//  }
//
//
//  public CronLikeScheduler getCronLikeScheduler() {
//    CronLikeScheduler cronsched = (CronLikeScheduler) getFunctionGroup(CronLikeScheduler.DEFAULT_NAME);
//    if (cronsched == null)
//      logger.debug("tried to access undeployed function group " + CronLikeScheduler.DEFAULT_NAME);
//
//    return cronsched;
//  }
//
//
//  public void addOrder(XynaOrderServerExtension xo) {
//
//    if (logger.isDebugEnabled()) {
//      logger.debug("Adding to scheduler: " + xo.getId());
//      if (logger.isTraceEnabled()) {
//        xo.debugDetails();
//      }
//    }
//    
//    cancelListenerLock.lock();
//    try {
//      if (cancelListeners.containsKey(xo.getId())) {
//
//        ICancelResultListener listener = cancelListeners.remove(xo.getId());
//
//        if (listener != null && listener.isObsolete()) {
//          // the order arrives after the cancel order has timed out, so do not cancel but just remove the cancel order
//          // since the only reason it exists is that it has not been cleaned up yet
//          cancelListeners.remove(listener);
//        } else {
//          //TODO lock fr�her freigeben: im elsezweig bruacht man es nicht mehr
//
//          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
//                          .notifyListeners(xo, OrderInstanceStatus.CANCELED);
//
//          try {
//            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
//                            .updateInstanceCancel(xo);
//          } catch (XynaException e) {
//            //TODO evtl konfigurieren, ob auftr�ge bei fehlern in der persistierung abgebrochen werden?
//            logger.warn("could not update instance of " + xo, e);
//          }
//
//          // this can be null even if the key existed in the case that no listener was registered
//          if (listener != null) {
//            listener.cancelSucceeded();
//          }
//
//          logger.info("Detected canceled order while adding to scheduler, doing nothing and notifying cancel listener");
//          return;
//
//        }
//
//      }
//    } finally {
//      cancelListenerLock.unlock();
//    }
//
//    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
//                    .notifyListeners(xo, OrderInstanceStatus.SCHEDULING);
//
//    try {
//      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
//                      .updateStatusScheduling(xo);
//    } catch (XynaException e) {
//      //TODO evtl konfigurieren, ob auftr�ge bei fehlern in der persistierung abgebrochen werden?
//      logger.warn("could not update instance of " + xo, e);
//    }
//
//    if (xo.getSchedulingTimeout() != null) {
//      schedulingMaintenanceRunnable.add(xo);
//    }
//
//    ordersLock.lock();
//    try {
//      // validate schedulerbean TODO
//      orders.add(xo); // => auftrag wartet
//    }
//    finally {
//      ordersLock.unlock();
//    }
//
//    notifyScheduler(); // versuchen zu starten
//
//  }
//
//
//  /**
//   * Starts a scheduling cycle if it not running already
//   */
//  public void notifyScheduler() {
//    innerSchedulerRunnable.triggerScheduler();
//  }
//
//
//  /**
//   * schedulerthread mit management f�r pausieren, beenden, wecken
//   */
//  private class SchedulerRunnable implements Runnable {
//    
//    private volatile boolean threadMayRun = false;
//    private boolean threadIsAsleep = false;
//    private boolean goToSleep = true;
//    private volatile boolean isPaused = false;
//    private Thread internalSchedulerThread;
//    // regelt zugriff auf threadIsAsleep und goToSleep
//    private Object sleepLock = new Object();
//    private SchedulerAlgorithm schedulerAlgorithm;
//
//
//    /**
//     * stoppt scheduling thread gracefully, d.h. er l�uft seine jetzige schleife zuende und beendet sich danach.
//     */
//    public void stopSchedulingThread() {
//      threadMayRun = false;
//      triggerScheduler();
//    }
//
//
//    /**
//     * thread schl�ft solange bis er durch unPauseScheduling aufgeweckt wird
//     */
//    public void pauseScheduling() {
//      // thread kann nicht interrupted werden
//      isPaused = true;
//    }
//
//
//    public void unPauseScheduling() {
//      isPaused = false;
//      triggerScheduler();
//    }
//
//
//    /**
//     * dem scheduler mitteilen, dass es arbeit gibt.
//     */
//    public void triggerScheduler() {
//      boolean unlocked = false;
//      schedulerCntLock.lock();
//      try {
//        if (schedulerCnt == 0) {
//          schedulerCnt++;
//          schedulerCntLock.unlock();
//          unlocked = true;
//
//          // interrupted thread, so dass er erneut l�uft
//          synchronized (sleepLock) {
//            if (threadIsAsleep && !isPaused) {
//              sleepLock.notify(); // schlafenden thread interrupten
//            } else { // TODO isPaused ??
//              // entweder vor trySchedule() => alles ok, das passiert dann gleich
//              // oder am ende davon/kurz danach (bevor das sleeplock geholt wird)
//              goToSleep = false;
//            }
//          }
//        } else {
//          // ansonsten l�uft er noch... => signalisierung, dass er nochmal laufen soll
//          schedulerCnt++;
//        }
//      } finally {
//        if (!unlocked)
//          schedulerCntLock.unlock();
//      }
//    }
//
//
//    /**
//     * kann nur einmal gestartet werden. falls der thread noch existiert, macht dieser aufruf nichts.
//     */
//    public synchronized void startNewSchedulerThread(SchedulerAlgorithm schedulerAlgorithm) {
//      if (internalSchedulerThread == null) {
//        this.schedulerAlgorithm = schedulerAlgorithm;
//        threadMayRun = true;
//        internalSchedulerThread = new Thread(this, "SchedulerThread");
//        internalSchedulerThread.setPriority(Thread.MAX_PRIORITY);
//        internalSchedulerThread.start();
//      }
//    }
//
//
//    public void changeSchedulerAlgorithm(SchedulerAlgorithm schedulerAlgorithm) {
//      this.schedulerAlgorithm = schedulerAlgorithm;
//    }
//
//
//    public void run() {
//      try {
//        logger.debug("Scheduler started.");
//        HashSet<Long> lastThrowableTimes = new HashSet<Long>();
//        while (threadMayRun) {
//          try {
//            schedulerAlgorithm.trySchedule();
//          } catch (Throwable t) {
//            Department.handleThrowable(t);
//            Department.checkMassiveExceptionOccurrence(lastThrowableTimes, t, MAX_THROWABLES_BEFORE_STOP);
//            logger.error("A severe error occurred while trying to perform scheduling", t);
//          }
//          synchronized (sleepLock) {
//            if (!goToSleep) {
//              goToSleep = true;
//              continue;
//            }
//            try {
//              logger.debug("Scheduler goes to sleep.");
//              threadIsAsleep = true;
//              sleepLock.wait();
//            } catch (InterruptedException e) {
//              // got notified
//            }
//            threadIsAsleep = false;
//            logger.debug("Scheduler has been woken up.");
//          }
//        }
//        logger.debug("Scheduler was shut down.");
//        internalSchedulerThread = null;
//      } catch (Throwable t) {
//        Department.handleThrowable(t);
//        logger.fatal("scheduler was shut down because a severe error occured", t);
//      }
//    }
//  };
//
//
//  /**
//   * Tries to schedule as many orders as possible. To do that, first all orders from the temporary incoming queue are
//   * moved to the priority ordered queue. After that the list is ordered again. all entries are tried to be scheduled.
//   */
//  public synchronized void trySchedule() {
//    XynaOrderServerExtension xo = null;
//    schedulerCntLock.lock();
//    try {
//      while (schedulerCnt > 0) {
//
//        schedulerCnt = 1;
//        schedulerCntLock.unlock();
//
//        HashSet<Long> lastThrowableTimes = null;
//        ordersBeingCheckedForSchedLock.lock();
//        try {
//
//          // checken, ob auftr�ge laufen d�rfen
//          ordersLock.lock();
//          try {
//            // copy all orders to a private list to be able to rapidly iterate over it without locking for every item
//            // and slow access by index
//            ordersBeingCheckedForSched.addAll(orders);
//            orders.clear();
//            Collections.sort(ordersBeingCheckedForSched, schedulerComparator);
//          } finally {
//            ordersLock.unlock();
//          }
//
//          Iterator<XynaOrderServerExtension> iter = ordersBeingCheckedForSched.iterator();
//
//          while (iter.hasNext()) {
//            xo = iter.next();
//
//            if (xo.isTimedOut() || xo.isCancelled()) {
//              // canceled should not happen since these entries have already been deleted, but just to be sure
//              // but timedout can happen!
//              iter.remove();
//            } else if (checkSchedulingConditions(xo)) {
//
//              iter.remove();
//              // set timeout to null because it might be readded after suspension and should not be marked as timedout
//              // then
//
//              if (xo.getSchedulingTimeout() != null) {
//                if (!schedulingMaintenanceRunnable.notifyOnScheduled(xo)) {
//                  // TODO achtung, hier muss alles was in checkSchedulingConditions passiert ist, r�ckg�ngig gemacht
//                  // werden
//                  // => besser benamen!
//                  freeCapacities(xo);
//                  continue;
//                }
//                xo.setSchedulingTimeout(null);
//              }
//
//              schedule(xo, lastThrowableTimes);
//            }
//          }
//        } catch (RuntimeException t) {
//          Department.handleThrowable(t);
//          if (lastThrowableTimes == null)
//            lastThrowableTimes = new HashSet<Long>();
//          Department
//                          .checkMassiveExceptionOccurrence(
//                                                           lastThrowableTimes,
//                                                           t,
//                                                           ordersBeingCheckedForSched.size() < 5 ? 5 : MAX_THROWABLES_BEFORE_STOP);
//          logger.error("A severe error occurred while trying to perform scheduling", t);
//        } finally {
//          ordersBeingCheckedForSchedLock.unlock();
//        }
//
//        /*
//         * ordersLock.lock(); try { checkForDouble("nachscheduling"); } finally { ordersLock.unlock(); }
//         */
//
//        schedulerCntLock.lock();
//        schedulerCnt--;
//
//      }
//    } finally {
//      schedulerCntLock.unlock();
//    }
//
//  }
//
//
//  private boolean checkSchedulingConditions(XynaOrderServerExtension xo) {
//    if (xo.getSeriesInformation() != null) {
//      SeriesInformation si = xo.getSeriesInformation();
//      if (!si.mayBeScheduled()) {
//        return false;
//      }
//    }
//    boolean capsOk = getCapacityManagement().allocateCapacities(xo);
//    if (!capsOk) {
//      return false;
//    }
//    return true;
//  }
//
//
//  /**
//   * beantwortet auftrag mit fehler wegen timeout
//   */
//  protected void handleTimeout(XynaOrderServerExtension xo) {
//    logger.debug("Removed timed out order");
//    xo.addException(new XynaException(Codes.CODE_PROCESS_SCHEDULING_TIMEOUT(xo.getId())));
//    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().addResponse(xo);
//  }
//
//
//  protected void handleCancelTimeout(ICancelResultListener listener) {
//    listener.makeObsolete();
//    listener.cancelFailed();
//    logger.debug("Removed timed out cancel request");
//  }
//
//
//  public void processCancellation(XynaOrderServerExtension xo) {
//    xo.setCancelled(true);
//    xo.addException(new XynaException("order has been canceled"));
//    try {
//      handleSeries(xo, true);
//    } catch (XynaException e) {
//      xo.addException(e);
//    }
//    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().addResponse(xo);
//  }
//
//
//  /**
//   * starten des auftrags
//   */
//  private void schedule(final XynaOrderServerExtension xo, final HashSet<Long> throwableTimes) {
//
//    if (logger.isDebugEnabled()) {
//      logger.debug("going to start " + xo.getId());
//      if (logger.isTraceEnabled()) {
//        xo.debugDetails();
//      }
//    }
//
//
//    Runnable r = new Runnable() {
//
//      public void run() {
//        boolean cleanupFinished = false;
//        boolean seriesHandlingFinished = false;
//        final boolean b = createLoggingContext;
//        if (b) {
//          NDC.push("" + xo.getId());
//        }
//        else {
//          if (logger.isDebugEnabled())
//            logger.debug("Executing order " + xo.getId() + " with thread '" + Thread.currentThread().getName() + "'");
//        }
//
//        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
//                        .notifyListeners(xo, OrderInstanceStatus.RUNNING_EXECUTION);
//
//        try {
//          if (!xo.isHasAquiredCapacities()) {
//            throw new RuntimeException("order is expected to have aquired its capacities after scheduling.");
//          }
//
//          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
//                          .updateStatusRunningExecution(xo);
//
//          try {
//            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().dispatch(xo);
//          }
//          catch (XynaException e) {
//
//            String[] codeArray = Codes.CODE_UNEXPECTED_ERROR_PROCESS("", "");
//            if (e.getCode().equals(codeArray[0])) {
//              throw e.getCause();
//            }
//            else {
//              xo.addException(e);
//            }
//
//          }
//          catch (ProcessSuspendedException e) {
//
//            logger.debug("Order " + xo.getId() + " was suspended");
//
//            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
//                            .updateInstanceSuspended(xo);
//
//            boolean parentProcessInstanceAttemptingSuspension = xo.hasParentOrder() && xo.getParentOrder()
//                            .getProcessInstance() != null && xo.getParentOrder().getProcessInstance()
//                            .attemptingSuspension();
//
//            if (xo.hasParentOrder() && (e.needsToSuspendParentOrderEvenIfNotSuspended() || parentProcessInstanceAttemptingSuspension)) {
//
//              if (xo.getResponseListener() instanceof ResponseListenerWithSuspensionSupport) {
//                ((ResponseListenerWithSuspensionSupport) xo.getResponseListener()).onSuspended(e
//                                .needsToSuspendParentOrderEvenIfNotSuspended());
//              }
//              else {
//                throw new XynaException(
//                                        "TODO: Fatal error: could not pause" +
//                    " parent order because it did not specify a correct response listener"); // TODO exception  code
//              }
//
//            }
//
//            if (!xo.hasParentOrder() || !e.needsToSuspendParentOrderEvenIfNotSuspended()) {
//              // falls mainworkflow, oder falls ein auftrag manuell suspendiert wurde
//              // offen: was passiert, wenn gleichzeitig suspendauftrag (zb runterfahren) + MI?
//              addAsSuspendedAndFreeCapacities(xo);
//            }
//            else {
//              freeCapacities(xo);
//            }
//
//
//            // nothing special for series
//
//            return;
//
//          }
//
//          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().runningInstanceCleanup(xo);
//
//          // this is just for consistency
//          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
//                          .notifyListeners(xo, OrderInstanceStatus.FINISHED_EXECUTION);
//
//          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
//                          .notifyListeners(xo, OrderInstanceStatus.RUNNING_CLEANUP);
//
//          XynaCleanup.cleanup(xo);
//          cleanupFinished = true;
//          try {
//            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().dispatch(xo);
//          } catch (XynaException e) {
//            xo.addException(e);
//          }
//
//          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().finishInstanceCleanup(xo);
//
//          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
//                          .notifyListeners(xo, OrderInstanceStatus.FINISHED_CLEANUP);
//
//          handleSeries(xo, false);
//          seriesHandlingFinished = true;
//
//          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().addResponse(xo);
//
//        } catch (Throwable t) {
//          Department.handleThrowable(t);
//          if (t instanceof RuntimeException) {
//            if (throwableTimes == null)
//              Department.checkMassiveExceptionOccurrence(new HashSet<Long>(), t, 10);
//            else
//              Department.checkMassiveExceptionOccurrence(throwableTimes, t, 10);
//          }
//          addThrowableAsXynaException(xo, t);
//
//          if (logger.isDebugEnabled()) {
//            String message;
//            if (t instanceof XynaRuntimeException) {
//              message = t.toString();
//            } else
//              message = t.getMessage();
//            logger.debug("Runtime exception ('" + message + "') caught during execution");
//          }
//
//          if (!cleanupFinished) {
//            // cleanup noch zu tun
//            XynaCleanup.cleanup(xo);
//
//            try {
//              XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
//                              .updateInstanceRuntimeError(xo, xo.getDestinationKey().getOrderType(),
//                              // letzter fehler
//                                                          xo.getErrors()[xo.getErrors().length - 1]);
//            } catch (XynaException e) {
//              // TODO evtl konfigurieren, ob auftr�ge bei fehlern in der persistierung abgebrochen werden?
//              logger.warn("could not update instance of " + xo, e);
//            }
//
//            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
//                            .notifyListeners(xo, OrderInstanceStatus.RUNTIME_ERROR);
//
//          }
//          if (!seriesHandlingFinished) {
//            try {
//              handleSeries(xo, true);
//            } catch (XynaException e) {
//              xo.addException(e);
//            } catch (Throwable tt) {
//              Department.handleThrowable(tt);
//              addThrowableAsXynaException(xo, tt);
//            }
//          }
//
//          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().addResponse(xo);
//        }
//        finally {
//          if (b)
//            NDC.pop();
//        }
//      }
//
//    };
//
//    if (logger.isDebugEnabled())
//      logger.debug(xo.getId() + " has been started. (priority=" + xo.getPriority() + ") runnable= " + r);
//
//    XynaExecutor.getInstance().executeRunnable(r, xo.getPriority());
//
//  }
//
//
//  private static void addThrowableAsXynaException(XynaOrder xo, Throwable t) {
//    XynaException newE = new XynaException(Codes.CODE_UNEXPECTED_ERROR_PROCESS((xo.getDestinationKey() != null ? xo
//                    .getDestinationKey().getOrderType() : "unknown"), t.getClass().getSimpleName() + " " + t
//                    .getMessage())).initCause(t);
//
//    xo.addException(newE);
//  }
//
//
//  /**
//   * vorg�ngerauftr�ge von nachfolgeauftrag entfernen.
//   */
//  private void handleSeries(XynaOrder xo, boolean errorOccurred) throws XynaException {
//    if (xo.getSeriesInformation() != null) {
//      if (!xo.getSeriesInformation().cleanup(errorOccurred)) {
//        notifyScheduler();
//      }
//    }
//  }
//
//
//  /*
//   * ##### CANCEL #####
//   */
//
//  private ReentrantLock cancelListenerLock = new ReentrantLock();
//  private HashMap<Long, ICancelResultListener> cancelListeners = new HashMap<Long, ICancelResultListener>();
//  private PriorityBlockingQueue<ICancelResultListener> cancelListenerQueue;
//
//
//  /**
//   * Tries to cancel an order immediately and schedules it for later canceling if an order with the given ID arrives
//   * some time later.
//   * 
//   * @param l
//   * @param listener
//   * @return true, if the order could be canceled immediately and false otherwise
//   */
//  public boolean cancelOrder(Long l) {
//    return cancelOrder(l, null);
//  }
//
//
//  /**
//   * Tries to cancel an order immediately and schedules it for later canceling if an order with the given ID arrives
//   * some time later. Additionally, a listener is registered that performs a user-defined action once it is clear
//   * whether canceling succeeded, failed or timed out.
//   * 
//   * @param l
//   * @param listener
//   * @return true, if the order could be canceled immediately and false otherwise
//   */
//  public boolean cancelOrder(Long l, ICancelResultListener listener) {
//
//    ordersBeingCheckedForSchedLock.lock();
//    ordersLock.lock();
//
//    try {
//
//      XynaOrderServerExtension removedOrder = null;
//
//      Iterator<XynaOrderServerExtension> iter = orders.iterator();
//      while (iter.hasNext()) {
//
//      XynaOrderServerExtension xo = iter.next();
//        if (l.equals(xo.getId())) {
//          xo.setCancelled(true);
//          if (listener != null)
//            listener.cancelSucceeded();
//          removedOrder = xo;
//        }
//
//      }
//
//      iter = ordersBeingCheckedForSched.iterator();
//      while (iter.hasNext()) {
//
//        XynaOrderServerExtension xo = iter.next();
//        if (l.equals(xo.getId())) {
//          iter.remove();
//          xo.setCancelled(true);
//          if (listener != null)
//            listener.cancelSucceeded();
//          removedOrder = xo;
//        }
//
//      }
//
//      if (removedOrder != null) {
//        processCancellation(removedOrder);
//        return true;
//      }
//
//      if (listener == null || listener.getAbsoluteCancelTimeout() <= System.currentTimeMillis()) {
//        if (listener != null) // listener already timed out
//          listener.cancelFailed();
//        return false;
//      }
//
//      cancelListenerLock.lock();
//      try {
//
//        cancelListeners.put(l, listener);
//        synchronized (cancelListenerQueue) {
//          cancelListenerQueue.add(listener);
//          cancelListenerQueue.notify();
//        }
//
//        // remove old entries that could not be removed
//        if (cancelCount == 15) {
//          Iterator<ICancelResultListener> iter2 = cancelListeners.values().iterator();
//          while (iter2.hasNext()) {
//            if (iter2.next().isObsolete())
//              iter2.remove();
//          }
//        }
//        else {
//          cancelCount++;
//        }
//
//      }
//      finally {
//        cancelListenerLock.unlock();
//      }
//
//      return false;
//
//    }
//    finally {
//      ordersLock.unlock();
//      ordersBeingCheckedForSchedLock.unlock();
//    }
//
//  }
//  private int cancelCount = 0; 
//
//  /*
//   * ##### PROPERTY CHANGE LISTENING #####
//   */
//
//
//  public ArrayList<String> getWatchedProperties() {
//    ArrayList<String> watches = new ArrayList<String>();
//    watches.add(XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT);
//    return watches;
//  }
//
//
//  public void propertyChanged() {
//
//    logger.debug(getClass().getSimpleName() + " is adapting to changes of property " + XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT + "");
//
//    String s = XynaFactory.getInstance().getFactoryManagement()
//                    .getProperty(XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT);
//    try {
//      createLoggingContext = Boolean.valueOf(s);
//      return;
//    }
//    catch (NumberFormatException e) {
//      createLoggingContext = false;
//    }
//
//  }
//
//
//  /*
//   * ##### MANAGEMENT INTERFACE #####
//   */
//
//  public SortedMap<Integer, XynaOrderServerExtension> getOrdersToBeScheduled(final long offset, final int count) {
//
//    TreeMap<Integer, XynaOrderServerExtension> result = new TreeMap<Integer, XynaOrderServerExtension>();
//
//    int myCount = 0;
//
//    ordersBeingCheckedForSchedLock.lock();
//    ordersLock.lock();
//    try {
//      Iterator<XynaOrderServerExtension> iter = orders.iterator();
//
//      while (iter.hasNext() && myCount < count) {
//        XynaOrderServerExtension xo = iter.next();
//        result.put(xo.getPriority(), xo);
//        myCount++;
//      }
//
//      iter = ordersBeingCheckedForSched.iterator();
//      while (iter.hasNext() && myCount < count) {
//        XynaOrderServerExtension xo = iter.next();
//        result.put(xo.getPriority(), xo);
//        myCount++;
//      }
//
//    } finally {
//      ordersLock.unlock();
//      ordersBeingCheckedForSchedLock.unlock();
//    }
//
//    return result;
//
//  }
//
//
//  public Map<Long, XynaOrderServerExtension> getSuspendedOrders() {
//    return Collections.unmodifiableMap(suspendedOrders);
//  }
//
//
//  public void addAsSuspendedAndFreeCapacities(XynaOrderServerExtension xo) {
//    if (!xo.hasParentOrder()) {
//      suspendedOrdersLock.lock();
//      try {
//        suspendedOrders.put(xo.getId(), xo);
//      } finally {
//        suspendedOrdersLock.unlock();
//      }
//    }
//    freeCapacities(xo);
//  }
//
//
//  public boolean resumeOrder(Long orderId) {
//    XynaOrderServerExtension xo = null;
//    suspendedOrdersLock.lock();
//    try {
//      if (suspendedOrders.containsKey(orderId)) {
//        xo = suspendedOrders.remove(orderId);
//      }
//      else {
//        return false;
//      }
//    }
//    finally {
//      suspendedOrdersLock.unlock();
//    }
//    try {
//      logger.debug("Updating monitoring settings for resumed order <" + xo.getId() + ">");
//      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher().dispatch(xo);
//    }
//    catch (XynaException e) {
//      logger.warn("Could not set monitoring level for resumed order", e);
//    }
//    addOrder(xo);
//    return true;
//  }
//
//
//  public boolean resumeMultipleOrders(List<Long> orderIds) {
//
//    boolean success = true;
//
//    for (Long id : orderIds) {
//      success = success && resumeOrder(id);
//    }
//
//    return success;
//
//  }
//
//
//  /**
//   * 1. scheduler anhalten (pause-auftr�ge m�ssen noch laufen d�rfen!)
//   *   (implementierung durch austausch des scheduler algorithmus, der nur noch suspendauftr�ge durchl�sst)
//   * 2. auftr�ge die bald einen timeout haben mit fehler beantworten
//   */
//  public void pauseScheduling() {
//    innerSchedulerRunnable.changeSchedulerAlgorithm(new SchedulerAlgorithm() {
//
//      public synchronized void trySchedule() {
//
//        XynaOrderServerExtension xo = null;
//
//        schedulerCntLock.lock();
//        try {
//          while (schedulerCnt > 0) {
//
//            schedulerCnt = 1;
//            schedulerCntLock.unlock();
//
//            HashSet<Long> lastThrowableTimes = null;
//            ordersBeingCheckedForSchedLock.lock();
//            try {
//
//              // checken, ob auftr�ge laufen d�rfen
//              ordersLock.lock();
//              try {
//                // copy all orders to a private list to be able to rapidly iterate over it without locking for every
//                // item and
//                // slow access by index
//                ordersBeingCheckedForSched.addAll(orders);
//                orders.clear();
//                Collections.sort(ordersBeingCheckedForSched, schedulerComparator);
//              } finally {
//                ordersLock.unlock();
//              }
//
//              Iterator<XynaOrderServerExtension> iter = ordersBeingCheckedForSched.iterator();
//
//              while (iter.hasNext()) {
//                xo = iter.next();
//
//                if (xo.isTimedOut() || xo.isCancelled()) {
//                  // canceled should not happen since these entries have already been deleted, but just to be sure
//                  iter.remove();
//                } else if (!(xo.getDestinationKey().equals(XynaDispatcher.DESTINATION_KEY_SUSPEND_ALL) || xo
//                                .getDestinationKey().equals(XynaDispatcher.DESTINATION_KEY_SUSPEND))) {
//                  continue;
//                } else if (checkSchedulingConditions(xo)) {
//
//                  iter.remove();
//                  // set timeout to null because it might be readded after suspension and should not be marked as
//                  // timedout then
//
//                  if (xo.getSchedulingTimeout() != null) {
//                    if (!schedulingMaintenanceRunnable.notifyOnScheduled(xo)) {
//                      // TODO achtung, hier muss alles was in checkSchedulingConditions passiert ist, r�ckg�ngig gemacht
//                      // werden
//                      // => besser benamen!
//                      freeCapacities(xo);
//                      continue;
//                    }
//                    xo.setSchedulingTimeout(null);
//                  }
//
//                  schedule(xo, lastThrowableTimes);
//                }
//              }
//            } catch (RuntimeException t) {
//              Department.handleThrowable(t);
//              if (lastThrowableTimes == null)
//                lastThrowableTimes = new HashSet<Long>();
//              Department
//                              .checkMassiveExceptionOccurrence(
//                                                               lastThrowableTimes,
//                                                               t,
//                                                               ordersBeingCheckedForSched.size() < 5 ? 5 : MAX_THROWABLES_BEFORE_STOP);
//              logger.error("A severe error occurred while trying to perform scheduling", t);
//            } finally {
//              ordersBeingCheckedForSchedLock.unlock();
//            }
//
//            /*
//             * ordersLock.lock(); try { checkForDouble("nachscheduling"); } finally { ordersLock.unlock(); }
//             */
//
//            schedulerCntLock.lock();
//            schedulerCnt--;
//
//          }
//        } finally {
//          schedulerCntLock.unlock();
//        }
//
//      }
//      
//    });
//
//    long offset = 0;
//    try {
//      offset = Long.valueOf(XynaFactory.getInstance().getFactoryManagement()
//                      .getProperty(XynaProperty.XYNA_SCHEDULER_STOP_TIMEOUT_OFFSET));
//    } catch (NumberFormatException e) {
//      logger.warn("XynaProperty " + XynaProperty.XYNA_SCHEDULER_STOP_TIMEOUT_OFFSET + " not set. Using default = 0.");
//    }
//
//    schedulingMaintenanceRunnable.setOffset(offset);
//
//  }
//
//
//  public int getSuspendedOrdersCount() {
//    int count = 0;
//    suspendedOrdersLock.lock();
//    try {
//      for (XynaOrderServerExtension xo : suspendedOrders.values()) {
//        count += countOrdersRecursively(xo);
//      }
//    } finally {
//      suspendedOrdersLock.unlock();
//    }
//    return count;
//  }
//
//
//  private static int countOrdersRecursively(XynaOrderServerExtension xo) {
//    int count = 0;
//    count++;
//    if (xo.getProcessInstance() != null) {
//      for (XynaOrderServerExtension child : xo.getProcessInstance().getAllChildOrdersRecursively()) {
//        count += countOrdersRecursively(child);
//      }
//    }
//    return count;
//  }
//
//
//  public int getWaitingOrdersCount() {
//    //TODO auftr�ge die mit startzeit in zukunft eingestellt wurden beachten. cron like order?
//    ordersBeingCheckedForSchedLock.lock();
//    ordersLock.lock();
//    try {
//      return orders.size() + ordersBeingCheckedForSched.size();
//    } finally {
//      ordersLock.unlock();
//      ordersBeingCheckedForSchedLock.unlock();
//    }
//  }
//
//
//  public Collection<XynaOrderServerExtension> getWaitingOrders() {
//    ordersBeingCheckedForSchedLock.lock();
//    ordersLock.lock();
//    try {
//      Collection<XynaOrderServerExtension> coll = new ArrayList<XynaOrderServerExtension>();
//      coll.addAll(orders);
//      coll.addAll(ordersBeingCheckedForSched);
//      return coll;
//    } finally {
//      ordersLock.unlock();
//      ordersBeingCheckedForSchedLock.unlock();
//    }
//  }
//
//  /**
//   * befreit caps falls nicht bereits geschehen, und notified scheduler
//   * @param xo
//   */
//  public void freeCapacities(XynaOrderServerExtension xo) {
//    if (getCapacityManagement().freeCapacities(xo)) {
//      notifyScheduler();
//    }
//  }
//
//  /**
//   * falls im scheduler, wird der auftrag entfernt.
//   * ansonsten wird er in der execution versucht zu suspendieren.
//   * @param order
//   * @throws XynaException
//   */
//  public void suspendOrder(XynaOrderServerExtension order) throws XynaException {
//    //lock holen, falls waiting, dann suspend
//    //TODO was tun, wenn der auftrag noch nicht hier ist, sondern noch im planning, und der scheduler evtl nicht angehalten ist    
//    //     evtl �hnlich behandeln wie cancel
//    if (order.getProcessInstance() != null) {
//      order.getProcessInstance().suspend();
//    } else {
//      //FIXME: dieser code evtl ist nur richtig, wenn scheduler angehalten ist! (s.o. TODO)
//      boolean removedOrderFromScheduler = false;
//      ordersBeingCheckedForSchedLock.lock();
//      try {
//        removedOrderFromScheduler = ordersBeingCheckedForSched.remove(order);
//      } finally {
//        ordersBeingCheckedForSchedLock.unlock();
//      }
//      if (removedOrderFromScheduler) {
//        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
//        .updateInstanceSuspended(order);
//        addAsSuspendedAndFreeCapacities(order);
//      } else {
//        //TODO auftrag im planning? auftrag bereits geschedult, hatte aber noch keine processinstance?
//        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
//        .updateInstanceSuspended(order);
//        logger.warn("order " + order + " could not be suspended properly. set state to suspended anyway.");
//      }
//    }
//
//  }
//
//
//}
