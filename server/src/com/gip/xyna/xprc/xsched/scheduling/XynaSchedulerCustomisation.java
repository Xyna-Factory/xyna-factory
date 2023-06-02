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
package com.gip.xyna.xprc.xsched.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.TaggedOrderedCollection;
import com.gip.xyna.utils.scheduler.ScheduleResult;
import com.gip.xyna.utils.scheduler.ScheduleResult.TagScheduleResult;
import com.gip.xyna.utils.scheduler.Scheduler;
import com.gip.xyna.utils.scheduler.SchedulerCustomisation;
import com.gip.xyna.utils.scheduler.SchedulerInformationBuilder;
import com.gip.xyna.utils.scheduler.UrgencyOrderList;
import com.gip.xyna.utils.scheduler.UrgencyOrderList.Urgency;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean.HistogramColumn;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean.Mode;
import com.gip.xyna.xprc.xsched.scheduling.TrySchedule.TryScheduleResult;
import com.gip.xyna.xprc.xsched.scheduling.TrySchedule.TryScheduleResultType;
import com.gip.xyna.xprc.xsched.scheduling.UrgencyCalculators.UrgencyCalculator;


public class XynaSchedulerCustomisation implements SchedulerCustomisation<SchedulingOrder,SchedulerInformationBean> {

  
  private static Logger logger = CentralFactoryLogging.getLogger(XynaSchedulerCustomisation.class);
  
  private TryScheduleImpl trySchedule;
  private AllOrdersList allOrders;
  private UrgencyCalculator urgencyCalculator;
  private XynaSchedulerStatistics schedulerStatistics = new XynaSchedulerStatistics();
  private XynaSchedulerCustomisationCapacities xscCapacities;
  private XynaSchedulerCustomisationVetos xscVetos;
  private Scheduler<SchedulingOrder,SchedulerInformationBean> scheduler;
  private XynaSchedulerInformationBuilder informationBuilder;

  public XynaSchedulerCustomisation(TryScheduleImpl trySchedule, 
                                    CapacityReservation capacityReservation,
                                    AllOrdersList allOrders,
                                    UrgencyCalculator urgencyCalculator,
                                    CapacityCache capacityCache) {
    this.trySchedule = trySchedule;
    this.allOrders = allOrders;
    this.urgencyCalculator = urgencyCalculator;
    xscCapacities = new XynaSchedulerCustomisationCapacities(capacityCache, capacityReservation);
    xscVetos = new XynaSchedulerCustomisationVetos(); //FIXME
    this.informationBuilder = new XynaSchedulerInformationBuilder();
  }
  
  public void setScheduler(Scheduler<SchedulingOrder,SchedulerInformationBean> scheduler) {
    this.scheduler = scheduler;
  }

  public Pair<BlockingQueue<Urgency<SchedulingOrder>>, 
              BlockingQueue<Urgency<SchedulingOrder>>> createQueues() {

    BlockingQueue<Urgency<SchedulingOrder>> entrance;
    BlockingQueue<Urgency<SchedulingOrder>> reorder;

    entrance = new LinkedBlockingQueue<Urgency<SchedulingOrder>>();
    reorder = new LinkedBlockingQueue<Urgency<SchedulingOrder>>();

    return Pair.of(entrance, reorder);
  }


  public SchedulerInformationBuilder<SchedulerInformationBean> getInformationBuilder() {
    return informationBuilder;
  }


  
  
  public long calculateUrgency(SchedulingOrder order) {
    return urgencyCalculator.calculate(order.getSchedulingData());
  }
  
  
  

  /**
   * Versuch, einen einzelnen Auftrag zu schedulen
   * @param uo
   * @return TryScheduleResult
   */
  public ScheduleResult trySchedule(Urgency<SchedulingOrder> uo) {
    SchedulingOrder so = uo.getOrder();
    so.setCurrentUrgency(uo.getUrgency());
    TryScheduleResult tsr = tryScheduleOrder(so);
    return switchTryScheduleResult(tsr, uo);
  }
  
  
  private TryScheduleResult tryScheduleOrder(SchedulingOrder so) { 
    synchronized (so) { //SchedulingOrder kann nicht mehr fremd durch AllOrders.remove etc. verändert werden
      if( so.isLocked() ) {
        return TryScheduleResult.RETRY_NEXT; //Auftrag überspringen und Scheduling wiederholen
      }
      if( so.isWaitingOrLocked() ) { //locked sollte hier nicht auftreten können
        return TryScheduleResult.CONTINUE; //Auftrag überspringen
      }
      if( so.isMarkedAsRemove() ) {
        //XynaOrder wurde durch ein CancelOrder/Suspend/etc. im XynaScheduler entfernt
        return TryScheduleResult.DELETE;
      }
      if( so.isAlreadyScheduled() ) {
        //XynaOrder wurde bereits geschedult, aber nicht aus der Liste gelöscht. 
        //Dies kann auftreten, wenn die SchedulingOrder in AllOrdersList.rescheduleOrder(..) gelangt
        logger.warn("Already scheduled "+so);
        return TryScheduleResult.DELETE;
      }
      
      TryScheduleResult result = null;
      try {
        result = trySchedule.trySchedule(so);
        if( result.getType() == TryScheduleResultType.SCHEDULE ) {
          //Auftrag konnte geschedult werden
          so.setOrderStatus(OrderInstanceStatus.RUNNING);
        }
      } finally {
        if (logger.isTraceEnabled()) {
          logger.trace("TryScheduleResult " + so.getOrderId() + " " + result);
        }
      }
      return result;
    }
  }

  
  
  private ScheduleResult switchTryScheduleResult( TryScheduleResult result, Urgency<SchedulingOrder> uo ) {
    SchedulingOrder so = uo.getOrder();
    so.setTag(null); //bisherige Tags löschen
    switch (result.getType()) {
      case SCHEDULE :
        //Auftrag wurde geschedult
        return ScheduleResult.Scheduled;
      case CAPACITY :
        //Auftrag konnte wegen Capacity nicht geschedult werden
        String capName = result.car.getCapName();
        boolean maxDemandTried = xscCapacities.addDemand(result.car, uo.getUrgency());
        allOrders.updateOrderStatus(so, result.car.getOrderInstanceStatus());
        so.setTag(capName);
        if( maxDemandTried ) {
          //es werden keine weiteren SchedulingOrders versucht zu schedulen, 
          //die diese Capacity ebenfalls benötigen. (SchedulingOrders müssen bereits getaggt sein) 
          return new TagScheduleResult(capName, true);
        } else {
          //obwohl bekannt ist, dass die Capacity nicht mehr verfügbar ist, müssen trotzdem weitere
          //Aufträge geschedult werden, um dem anderen Knoten den Bedarf melden zu können
          return new TagScheduleResult(capName, false);
        }
      case VETO :
        //Auftrag konnte wegen Veto nicht geschedult werden
        allOrders.updateOrderStatus(so, result.var.getOrderInstanceStatus());
        so.setTag(result.var.getVetoName());
        return new TagScheduleResult(null, false); //UrgencyOrder markieren, dass sie keine Cap verlangt
      case TIME_CONSTRAINT :
        //Auftrag konnte wegen TimeConstraint nicht geschedult werden
        allOrders.updateOrderStatus(so, result.tr.getOrderInstanceStatus());
        so.setTag("timeConstraint");
        if( result.tr.removeFromScheduler() ) {
          return ScheduleResult.Remove;
        } else {
          return ScheduleResult.Continue;
        }
      case REMOVE:
        return ScheduleResult.Remove;
      case BREAKLOOP :
        trySchedule.notifyScheduler();
        return ScheduleResult.BreakLoop;
      case DELETE:
        //Auftrag ist entweder kaputt, gecancelt oder durch ein Remove aus dem Scheduler entfernt worden
        return ScheduleResult.Scheduled;
      case PAUSE:
        return new TagScheduleResult("paused", true);
      case CONTINUE :
        //Auftrag soll aus irgendeinem Grund übersprungen werden
        break;
      case RETRY_NEXT :
        notifyScheduler(); //sicherstellen, dass es einen nächsten Schedulerlauf gibt
        return ScheduleResult.Continue;
      case REORDER :
        return ScheduleResult.Reorder;
    }
    return ScheduleResult.Continue;
  }
  
  private void notifyScheduler() {
    XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
  }

  public void beforeScheduling(long urgency) {
    //Caps fuer andere Knoten reservieren, die eine hoehere urgency haben als die aktuelle UrgencyOrder
    xscCapacities.reserveCapsForOtherNodes(urgency);
  }

  public void postparation() {
    //Caps fuer andere Knoten reservieren, eigenen Bedarf melden und Capacities übertragen
    xscCapacities.capacityReservation(informationBuilder.getSchedulerInformationBean());

    xscCapacities.setUnsatisfiedForeignDemand(informationBuilder.getSchedulerInformationBean());
  }

  public void preparation() {
    //Sammeln der Capacity-Forderungen der anderen Knoten
    xscCapacities.gatherDemands();
  }

  public void endScheduling() {
    SchedulerInformationBean information = informationBuilder.getSchedulerInformationBean();
    xscVetos.endScheduling();
    
    
    if (logger.isDebugEnabled()) {
      
      int transported = information.getLastTransportedCaps();
      int waiting = information.getWaitingForCapacity() + information.getWaitingForVeto() + information.getWaitingForUnknown();
      logger.debug("############# Scheduling ("+information.getTotalSchedulerRuns()+") finished"
          +(information.isLoopEndedRegularily()?"":" unregularily")
          +", looped over "+information.getLastIteratedOrders()+" orders, scheduled "+ information.getLastScheduledOrders()
          +" orders, with "+waiting+" orders waiting "+
          "(cap=" + information.getWaitingForCapacity()+
          ", veto="+information.getWaitingForVeto()+
          ", other="+information.getWaitingForUnknown()+")"+
          ", "+(transported>=0?(transported+" transported capacities "):"")+
          "and "+information.getUnsatisfiedForeignDemand()+" unsatisfied foreign demands");
      logger.debug("############# preparation=" + information.getPreparationDuration() 
                   + ", schedule=" + information.getSchedulingDuration()
                   + ", capReservation=" + information.getFinishDuration() 
                   + ", total=" + information.getLastSchedulingTook());
    }
    
    long last = information.getLastScheduled();
    schedulerStatistics.addToSlidingWindows( HistogramColumn.SchedulerRuns, last, 1 );
    schedulerStatistics.addToSlidingWindows( HistogramColumn.OrdersScheduled, last, information.getLastScheduledOrders() );
    schedulerStatistics.addToSlidingWindows( HistogramColumn.CapacitiesTransfered, last, information.getLastTransportedCaps() );    
  }
  
  public void beginScheduling() {
    SchedulerInformationBean information = informationBuilder.getSchedulerInformationBean();
    
    xscCapacities.setCurrentSchedulingRun( information.getSchedulingRunNumber() );
    xscVetos.beginScheduling( information.getSchedulingRunNumber() );
    
    schedulerStatistics.start(information.getTimestampStart(), information.getSchedulingRunNumber() );
  }

  public static enum ExceptionReaction {
    LogOnlyAndRetry,
    ShutdownFactory,
    HaltScheduler,
    WaitForFreeMemory;
  }  

  public void handleThrowable(Throwable t) {
    ExceptionReaction er = null;
    if( t instanceof OutOfMemoryError ) {
      er = XynaProperty.SCHEDULER_OOM_ERROR_REACTION.get();
    } else {
      er = XynaProperty.SCHEDULER_GENERAL_EXCEPTION_REACTION.get();
    }
   
    switch( er ) {
      case ShutdownFactory:
        logger.error("Exception in SchedulerAlgorithm, ExceptionReaction="+er+", Exception is "+t.getMessage(), t );
        XynaSchedulerCustomisationUtils.shutdownFactory();
        break;
      case WaitForFreeMemory:
        logger.error("Exception in SchedulerAlgorithm, ExceptionReaction="+er+", Exception is "+t.getMessage(), t );
        XynaSchedulerCustomisationUtils.waitForFreeMemory();
        break;
      case HaltScheduler:
        XynaSchedulerCustomisationUtils.throwThrowable(t);
      case LogOnlyAndRetry:
        logger.info("Trying to continue scheduling");
        break;
      default:
        XynaSchedulerCustomisationUtils.throwThrowable(t);
    }
  }
  
  public long getOrderId(SchedulingOrder order) {
    return order.getOrderId();
  }

  public SchedulerInformationBean getInformationBean(Mode mode) {
    GetInformationBean gib = new GetInformationBean(mode);
    if( mode == SchedulerInformationBean.Mode.Consistent ) {
      scheduler.executeExclusively(gib);
    } else if( mode == SchedulerInformationBean.Mode.Histogram ) {
      return schedulerStatistics.getInformationBeanHistogram();
    } else {
      gib.run();
    }
    return gib.getSchedulerInformationBean();
  }
  
  private class GetInformationBean implements Runnable {

    private Mode mode;
    private SchedulerInformationBean sib = null;
    
    public GetInformationBean(Mode mode) {
      this.mode = mode;
    }


    public void run() {
      SchedulerInformationBean lastSib = scheduler.getSchedulerInformation();
      if( lastSib != null ) {
        sib = new SchedulerInformationBean(lastSib);
      } else {
        sib = new SchedulerInformationBean();
      }

      addOrderInformation(sib, mode);
      sib.setSchedulerStatus(trySchedule.state());
      
      schedulerStatistics.addSchedulerRuns(sib);
      sib.setCurrentlyScheduling(scheduler.isCurrentlyScheduling());
    }
    
    public SchedulerInformationBean getSchedulerInformationBean() {
      return sib;
    }

    private void addOrderInformation(SchedulerInformationBean sib, SchedulerInformationBean.Mode mode) {
      List<XynaOrderInfo> orders = null;
      if (mode == SchedulerInformationBean.Mode.Orders ) {
        orders = allOrders.getSchedulingOrders(); //lockt intern
        sib.setOrdersInScheduler( orders );
        sib.setCountOfOrdersInScheduler( orders.size() );
      } else if (mode == SchedulerInformationBean.Mode.Consistent ) {
        UrgencyOrderList<SchedulingOrder> urgencyOrders = scheduler.getUrgencyOrderList();
        
        TaggedOrderedCollection<Urgency<SchedulingOrder>>.Iterator iter = urgencyOrders.iterator();
        orders = new ArrayList<XynaOrderInfo>();
        while (iter.hasNext()) {
          SchedulingOrder so = iter.next().getOrder();
          if( so.isMarkedAsRemove() ) {
            iter.remove();
          } else {
            orders.add( new XynaOrderInfo(so) );
          }
        }
        sib.setOrdersInScheduler( orders );
        sib.setCountOfOrdersInScheduler( orders.size() );

      }
    }

  }

  public void setCapacityReservation(CapacityReservation capacityReservation) {
    xscCapacities.setCapacityReservation(capacityReservation);
  }

  public void listExtendedSchedulerInfo(StringBuilder sb) {
    scheduler.executeExclusively( new ListExtendedSchedulerInfo(sb) );
  }

  private class ListExtendedSchedulerInfo implements Runnable {

    private StringBuilder sb;

    public ListExtendedSchedulerInfo(StringBuilder sb) {
      this.sb = sb;
    }

    public void run() {
      SchedulerInformationBean lastSchedulerInformation = (SchedulerInformationBean)scheduler.getSchedulerInformation();
      
      UrgencyOrderList<SchedulingOrder> urgencyOrders = scheduler.getUrgencyOrderList();

      sb.append("Last scheduling took ").append(lastSchedulerInformation.getLastSchedulingTook())
      .append(" ms and iterated over ").append(lastSchedulerInformation.getLastIteratedOrders())
      .append(" orders.\n");
      xscCapacities.listExtendedSchedulerInfo(sb);
      xscVetos.listExtendedSchedulerInfo(sb);
      //Capacities
      int waitingCnt = urgencyOrders.getWaitingForTags();
      if( waitingCnt == 0 ) {
        sb.append("No orders waiting for capacities\n");
      } else {
        sb.append(waitingCnt).append(" order").append(waitingCnt==1?"":"s").append(" waiting for capacities:\n");
      }
      for( String capName : urgencyOrders.getTags() ) {
        if( capName == null ) {
          continue;//dies sind Vetos
        }
        sb.append(" * ").append(capName).append(": ");
        appendWaitingList(sb, urgencyOrders.getTagged(capName) );
      }
      
      //Vetos
      List<Urgency<SchedulingOrder>> waitingForVeto = urgencyOrders.getTagged(null);
      if( waitingForVeto.size() == 0 ) {
        sb.append("No orders waiting for vetos\n");
      } else {
        sb.append(waitingForVeto.size()).append(" order").append(waitingForVeto.size()==1?"":"s").append(" waiting for vetos:\n");
        VetoWaiter vw = new VetoWaiter();
        for( Urgency<SchedulingOrder> uo : waitingForVeto ) {
          vw.add( uo.getOrder().getTag(), uo);
        }
        for( Pair<String,Integer> vetoCnt : vw.vetosOrderedByUsage() ) {
          String veto = vetoCnt.getFirst();
          sb.append(" * ").append(veto).append(": ");
          appendWaitingList(sb, vw.getWaiting(veto) );
        }
      }
    }

    private void appendWaitingList(StringBuilder sb, List<Urgency<SchedulingOrder>> waiting) {
      sb.append(waiting.size()).append(" waiting order").append(waiting.size()==1?"":"s").append(":\n");
      int cnt = 0;
      int max = 5;
      for( Urgency<SchedulingOrder> uo : waiting ) {
        ++cnt;
        sb.append("   urgency=").append( uo.getUrgency() );
        if( cnt < max || waiting.size() == max ) {
          SchedulingOrder so = uo.getOrder();
          sb.append(", orderId=").append(uo.getOrderId()).append(", destination=");
          sb.append(so.getDestinationKey().serializeToString()).append("\n");
        } else {
          sb.append(" and lower: ").append(waiting.size()-max+1).append(" more orders\n");
          break; //das reicht nun
        }
      }
    }
  }
  
  private static class VetoWaiter {
    Map<String,List<Urgency<SchedulingOrder>>> waitingMap = new HashMap<String,List<Urgency<SchedulingOrder>>>();
    
    public void add(String veto, Urgency<SchedulingOrder> uo) {
      List<Urgency<SchedulingOrder>> l = waitingMap.get(veto);
      if( l == null ) {
        l = new ArrayList<Urgency<SchedulingOrder>>();
        waitingMap.put( veto, l );
      }
      l.add(uo);
    }

    public List<Urgency<SchedulingOrder>> getWaiting(String veto) {
      return waitingMap.get(veto);
    }

    public List<Pair<String,Integer>> vetosOrderedByUsage() {
      List<Pair<String,Integer>> list = new ArrayList<Pair<String,Integer>>();
      for( Map.Entry<String, List<Urgency<SchedulingOrder>>> entry : waitingMap.entrySet() ) {
        list.add( Pair.of( entry.getKey(), entry.getValue().size() ) );
      }
      Collections.sort(list, Collections.reverseOrder(Pair.<Integer>comparatorSecond()) );
      return list;
    }
    
  }

  public XynaSchedulerCustomisationVetos getSchedulerCustomisationVetos() {
    return xscVetos;
  }
  
}
