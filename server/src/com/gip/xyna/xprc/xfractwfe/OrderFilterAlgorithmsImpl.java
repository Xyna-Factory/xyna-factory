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



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.MtoNMapping;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.DispatcherType;
import com.gip.xyna.xprc.xfractwfe.base.AFractalWorkflowProcessor;
import com.gip.xyna.xprc.xfractwfe.base.AFractalWorkflowProcessorProcessingCheckAlgorithm;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionProcessingRejectionState;
import com.gip.xyna.xprc.xsched.PreScheduler;
import com.gip.xyna.xprc.xsched.PreSchedulerAddOrderAlgorithm;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderStartUnderlyingOrderAlgorithm;



/**
 * singleton.
 * aufträge filtern anhand von angegebenen {@link OrderFilter}.
 * beim registrieren des ersten {@link OrderFilter} werden die normalen algorithmen gegen diesen ausgetauscht.
 * beim deregistrieren des letzten {@link OrderFilter} werden die algorithmen wieder zurückgetauscht.
 *
 * beim deregistrieren eines filters werden alle aufträge, die nicht mehr von filtern rausgefiltert werden, fortgesetzt.
 * d.h. aufträge bleiben so lange angehalten, bis es keinen filter mehr gibt, der sie rausfiltert.
 */
public class OrderFilterAlgorithmsImpl
    implements
      AFractalWorkflowProcessorProcessingCheckAlgorithm,
      CronLikeOrderStartUnderlyingOrderAlgorithm,
      PreSchedulerAddOrderAlgorithm {

  /**
   * es können mehrere filter registriert werden. aufträge können von mehreren filtern angenommen werden
   */
  public static interface OrderFilter {

    /**
     * @return true, falls auftrag rausgefiltert werden soll
     */
    public boolean filterForAddOrderToScheduler(XynaOrderServerExtension xo);


    /**
     * @return true, falls auftrag rausgefiltert werden soll
     */
    public boolean filterForCheckOrderReadyForProcessing(XynaOrderServerExtension xo, DispatcherType type);


    /**
     * @return true, falls auftrag rausgefiltert werden soll
     */
    public boolean startUnderlyingOrder(CronLikeOrder cronLikeOrder, CronLikeOrderCreationParameter clocp, ResponseListener rl);


    public void continueOrderReadyForProcessing(XynaOrderServerExtension xo);
  }


  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(OrderFilterAlgorithmsImpl.class);
  private static final OrderFilterAlgorithmsImpl INSTANCE = new OrderFilterAlgorithmsImpl();


  public static OrderFilterAlgorithmsImpl getInstance() {
    return INSTANCE;
  }


  private final ConcurrentMap<OrderFilter, Boolean> filter = new ConcurrentHashMap<OrderFilter, Boolean>();

  private final Object pillow = new Object();

  private volatile AFractalWorkflowProcessorProcessingCheckAlgorithm previousFWPPCA;
  private volatile CronLikeOrderStartUnderlyingOrderAlgorithm previousCLOSUA;
  private volatile PreSchedulerAddOrderAlgorithm previousPSAOA;

  private final MtoNMapping<XynaOrderServerExtension, OrderFilter> ordersHeldAtProcessors =
      new MtoNMapping<XynaOrderServerExtension, OrderFilter>();
  private final MtoNMapping<XynaOrderServerExtension, OrderFilter> ordersHeldAtPreScheduler =
      new MtoNMapping<XynaOrderServerExtension, OrderFilter>();
  private final MtoNMapping<CronParameter, OrderFilter> cronLikeOrders = new MtoNMapping<CronParameter, OrderFilter>();


  private static class CronParameter {

    private final CronLikeOrder clo;
    private final ResponseListener rl;
    private final CronLikeOrderCreationParameter clocp;


    private CronParameter(CronLikeOrder clo, ResponseListener rl, CronLikeOrderCreationParameter clocp) {
      this.clo = clo;
      this.rl = rl;
      this.clocp = clocp;
    }


    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clo == null) ? 0 : clo.hashCode());
      result = prime * result + ((clocp == null) ? 0 : clocp.hashCode());
      result = prime * result + ((rl == null) ? 0 : rl.hashCode());
      return result;
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      CronParameter other = (CronParameter) obj;
      if (clo == null) {
        if (other.clo != null)
          return false;
      } else if (!clo.equals(other.clo))
        return false;
      if (clocp == null) {
        if (other.clocp != null)
          return false;
      } else if (!clocp.equals(other.clocp))
        return false;
      if (rl == null) {
        if (other.rl != null)
          return false;
      } else if (!rl.equals(other.rl))
        return false;
      return true;
    }


  }


  private OrderFilterAlgorithmsImpl() {
  }


  private void setPreviousAlgorithms(AFractalWorkflowProcessorProcessingCheckAlgorithm previousFWPPCA,
                                     CronLikeOrderStartUnderlyingOrderAlgorithm previousCLOSUA, PreSchedulerAddOrderAlgorithm previousPSAOA) {
    this.previousFWPPCA = previousFWPPCA;
    this.previousCLOSUA = previousCLOSUA;
    this.previousPSAOA = previousPSAOA;
  }


  public void checkOrderReadyForProcessing(XynaOrderServerExtension xo, DispatcherType type) throws XynaException {
    boolean resume;
    Set<OrderFilter> filterThatHeldOrderAtSomeTime = new HashSet<OrderFilter>(); //OrderFilter müssen nicht unbedingt equals/hashcode überschreiben, geht auch ohne
    do {
      resume = true;
      //Die Liste der Filter kann sich während des Wartens ändern...
      List<OrderFilter> activeFilters = new ArrayList<OrderFilter>();
      for (OrderFilter of : filter.keySet()) {
        if (of.filterForCheckOrderReadyForProcessing(xo, type)) {
          filterThatHeldOrderAtSomeTime.add(of);
          activeFilters.add(of);
          ordersHeldAtProcessors.add(xo, of);
          resume = false;
          //kein break, sondern die anderen filter auch benachrichtigen
        }
      }
      if (resume) {
        //nothing needs to be done, if we let them pass they start processing internally
        updateDeploymentId(xo);
      } else {

        DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
        try {
          synchronized (pillow) {
            pillow.wait();
          }
        } catch (InterruptedException e) {
        } finally {
          xo.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
          DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
        }

        for (OrderFilter of : activeFilters) {
          ordersHeldAtProcessors.removeMapping(xo, of);
        }
      }
    } while (!resume);

    for (OrderFilter of : filterThatHeldOrderAtSomeTime) {
      of.continueOrderReadyForProcessing(xo);
    }
    previousFWPPCA.checkOrderReadyForProcessing(xo, type);
  }


  private void updateDeploymentId(XynaOrderServerExtension xo) {
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
    xo.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
  }


  public void addOrderToScheduler(final XynaOrderServerExtension xo) throws XPRC_OrderEntryCouldNotBeAcknowledgedException,
      XNWH_RetryTransactionException {

    boolean resume = true;
    Set<OrderFilter> filterCopy = filter.keySet(); //könnte sich während des wartens ändern...
    for (OrderFilter of : filterCopy) {
      if (of.filterForAddOrderToScheduler(xo)) {
        ordersHeldAtPreScheduler.add(xo, of);
        resume = false;
        //kein break, sondern die anderen filter auch benachrichtigen
      }
    }

    if (resume) {
      updateDeploymentId(xo);
      previousPSAOA.addOrderToScheduler(xo);
    }

  }


  public void startUnderlyingOrder(CronLikeOrder cronLikeOrder, CronLikeOrderCreationParameter clocp, ResponseListener rl) {
    boolean resume = true;
    Set<OrderFilter> filterCopy = filter.keySet(); //könnte sich während des wartens ändern...
    for (OrderFilter of : filterCopy) {
      if (of.startUnderlyingOrder(cronLikeOrder, clocp, rl)) {
        cronLikeOrders.add(new CronParameter(cronLikeOrder, rl, clocp), of);
        resume = false;
        //kein break, sondern die anderen filter auch benachrichtigen
      }
    }
    if (resume) {
      previousCLOSUA.startUnderlyingOrder(cronLikeOrder, clocp, rl);
    }
  }


  public int countFilteredBy(OrderFilter of) {
    int cnt = 0;
    Set<XynaOrderServerExtension> keys = ordersHeldAtProcessors.getKeys(of);
    if (keys != null) {
      cnt += keys.size();
    }
    Set<XynaOrderServerExtension> keys2 = ordersHeldAtPreScheduler.getKeys(of);
    if (keys2 != null) {
      cnt += keys2.size();
    }
    Set<CronParameter> keys3 = cronLikeOrders.getKeys(of);
    if (keys3 != null) {
      cnt += keys3.size();
    }
    return cnt;
  }


  public void addFilter(OrderFilter of) {
    synchronized (this) {
      if (filter.size() == 0) {
        setPreviousAlgorithms(AFractalWorkflowProcessor.getAlgorithm(), CronLikeOrder.getAlgorithm(), PreScheduler.getAlgorithm());
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement()
            .setProcessingRejectionState(ManualInteractionProcessingRejectionState.DEPLOYMENT);
        PreScheduler.setAlgorithm(this);
        AFractalWorkflowProcessor.setAlgorithm(this);
        CronLikeOrder.setAlgorithm(this);
      }
      filter.put(of, true);
    }

    //m-to-n mappings aktualisieren für hier angehaltene aufträge

    //processoren: nochmal checken, weil neuer filter da ist
    synchronized (pillow) {
      pillow.notifyAll();
    }

    //crons:
    synchronized (cronLikeOrders) {
      for (CronParameter cp : cronLikeOrders.getAllKeys()) {
        if (of.startUnderlyingOrder(cp.clo, cp.clocp, cp.rl)) {
          cronLikeOrders.add(cp, of);
        }
      }
    }

    //suspended orders:
    synchronized (ordersHeldAtPreScheduler) {
      for (XynaOrderServerExtension xo : ordersHeldAtPreScheduler.getAllKeys()) {
        if (of.filterForAddOrderToScheduler(xo)) {
          ordersHeldAtPreScheduler.add(xo, of);
        }
      }
    }
  }


  public void removeOrderFilter(OrderFilter of) {
    synchronized (this) {
      filter.remove(of);

      if (filter.size() == 0) {
        //FIXME was ist, wenn in der zwischenheit der stack der deploymentalgorithmen gar nicht mehr this an der spitze hat?
        //      dann ersetzen wir hier den falschen algorithmus
        PreScheduler.setAlgorithm(previousPSAOA);
        AFractalWorkflowProcessor.setAlgorithm(previousFWPPCA);
        CronLikeOrder.setAlgorithm(previousCLOSUA);
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement()
            .setProcessingRejectionState(ManualInteractionProcessingRejectionState.NONE);
        /* TODO wann?
        PreScheduler.setAlgorithm(new DefaultPreSchedulerAddOrderAlgorithm());
        AFractalWorkflowProcessor.setAlgorithm(new DefaultAFractalWorkflowProcessorProcessingCheckAlgorithm());
        CronLikeOrder.setAlgorithm(DefaultCronLikeOrderStartUnderlyingOrderAlgorithm.singleInstance);
        */
      }
    }

    resumeOrders(of);
  }


  private void resumeOrders(OrderFilter of) {
    //an den processoren wartende aufträge fortsetzen
    //funktioniert automatisch, weil der filter nicht mehr registriert ist
    synchronized (pillow) {
      pillow.notifyAll();
    }

    //crons fortsetzen
    createUnstartedCronLikeOrders(of);

    //suspendierte aufträge resumen
    resumeSuspendedOrders(of);
  }


  private void resumeSuspendedOrders(OrderFilter of) {
    List<XynaOrderServerExtension> resumeTargets = new ArrayList<XynaOrderServerExtension>();
    
    //davor schützen, dass neue filter sich gleichzeitig eintragen
    synchronized (ordersHeldAtPreScheduler) {
      Set<XynaOrderServerExtension> orders = ordersHeldAtPreScheduler.getKeys(of);
      if (orders != null) {
        for (XynaOrderServerExtension xo : orders) {
          ordersHeldAtPreScheduler.removeMapping(xo, of);
          Set<OrderFilter> otherFilters = ordersHeldAtPreScheduler.getValues(xo);
          if (otherFilters == null || otherFilters.size() == 0) {
            //dies war der letzte filter, der diese order referenziert hat
            resumeTargets.add(xo);
          }
        }
      }
    }
    if (resumeTargets.size() > 0) {
      for (XynaOrderServerExtension xo : resumeTargets) {
        try {
          logger.debug("Resuming order held at preScheduler: " + xo.getId());
          previousPSAOA.addOrderToScheduler(xo);
        } catch (XNWH_RetryTransactionException e) {
          logger.warn("Error while trying to continue order previously held at preScheduler", e);
        } catch (XPRC_OrderEntryCouldNotBeAcknowledgedException e) {
          logger.warn("Error while trying to continue order previously held at preScheduler", e);
        } catch (RuntimeException e) {
          logger.warn("Error while trying to continue order previously held at preScheduler", e);
        }
      }
    }
  }


  private void createUnstartedCronLikeOrders(OrderFilter of) {
    List<CronParameter> cronsToStart = new ArrayList<CronParameter>();

    synchronized (cronLikeOrders) {
      Set<CronParameter> crons = cronLikeOrders.getKeys(of);
      if (crons != null) {
        for (CronParameter cp : crons) {
          cronLikeOrders.removeMapping(cp, of);
          Set<OrderFilter> otherFilters = cronLikeOrders.getValues(cp);
          if (otherFilters == null || otherFilters.size() == 0) {
            //dies war der letzte filter, der diesen cron referenziert hat
            cronsToStart.add(cp);
          }
        }
      }
    }

    int size = cronsToStart.size();
    if (logger.isDebugEnabled()) {
      if (size > 0) {
        logger.debug("Creating " + size + " cron like order" + (size != 1 ? "s" : "") + " that were collected during the deployment");
      } else {
        logger.debug("No cron like orders to be created.");
      }
    }
    if (size == 0) {
      return;
    }

    for (CronParameter cp : cronsToStart) {
      previousCLOSUA.startUnderlyingOrder(cp.clo, cp.clocp, cp.rl);
    }
  }


  public Collection<? extends XynaOrderServerExtension> getOrdersHeldAtProcessors(OrderFilter of) {
    Collection<XynaOrderServerExtension> keys = ordersHeldAtProcessors.getKeys(of);
    if (keys == null) {
      keys = new ArrayList<XynaOrderServerExtension>();
    }
    Collection<XynaOrderServerExtension> preSchedulerKeys = ordersHeldAtPreScheduler.getKeys(of);
    if (preSchedulerKeys != null) {
      keys.addAll(preSchedulerKeys);
    }
    return keys;
  }


  public Collection<? extends CronLikeOrder> getUnstartedAffectedCrons(OrderFilter of) {
    Set<CronParameter> keys = cronLikeOrders.getKeys(of);
    if (keys == null) {
      return Collections.emptyList();
    }
    Set<CronLikeOrder> s = new HashSet<CronLikeOrder>();
    for (CronParameter cp : keys) {
      s.add(cp.clo);
    }
    return s;
  }

}
