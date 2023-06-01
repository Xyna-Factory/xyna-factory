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

package com.gip.xyna.xprc.xsched.scheduling;



import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.gip.xyna.xprc.XynaOrderInfo;



public class SchedulerInformationBean {

  public static enum Mode {
    Basic, Orders, Consistent,
    Histogram;
  }
  
  public static enum HistogramColumn {
    Timestamp(false,false), Width(false,false),
    SchedulerRuns(true,true), OrdersScheduled(true,true), CapacitiesTransfered(true,true);
    private boolean isValue;
    private boolean isRate;  //Raten-Werte können im Histogramm normlisiert werden: auf ganze Sekunden beziehen
    HistogramColumn(boolean isValue, boolean isRate) {
      this.isValue = isValue;
      this.isRate = isRate;
    }
    public boolean isValue() {
      return isValue;
    }
    public boolean isRate() {
      return isRate;
    }
    public String normalize(Number value, Number width) {
      if( isRate ) {
        return String.valueOf(value.doubleValue()*1000./width.doubleValue() );
      } else {
        return String.valueOf(value);
      }
    }
  }


  //timestamps
  private long timestampStart = -1L;
  private long timestampPreparation;
  private long timestampScheduling;
  private long timestampFinished;
  //waiting
  private int waitingForCapacity = 0;
  private int waitingForVeto = 0;
  private int waitingForUnknown = 0;
  //counts
  private int lastIteratedOrders = 0;
  private int lastScheduledOrders = 0;
  private int countOfOrdersInScheduler = 0; // possibly redundant
  private int lastTransportedCaps=-1;
  
  //TODO sortieren
  private long schedulerRunNumber = 0; //Id des letzten SchedulerRuns, Anzahl der bislang durchgeführten SchedulerRuns 
  
  private long schedulerRunsLast5Minutes = 0;
  private long schedulerRunsLast60Minutes = 0;
  private boolean isSchedulerAlive = false;
  private List<XynaOrderInfo> ordersInScheduler = new ArrayList<XynaOrderInfo>();
  private String schedulerStatus = "Indetermined";
  private boolean currentlyScheduling;
  private Throwable threadDeathCause;
  private long threadDeathTimestamp;
  private boolean loopEndedRegularily;
  private int unsatisfiedForeignDemand;
  private List<EnumMap<HistogramColumn, Number>> histogram;
  
  public SchedulerInformationBean() {
  }
  
  public SchedulerInformationBean(SchedulerInformationBean sib) {
    //timestamps
    this.timestampStart = sib.timestampStart;
    this.timestampPreparation = sib.timestampPreparation;
    this.timestampScheduling = sib.timestampScheduling;
    this.timestampFinished = sib.timestampFinished;
    //waiting
    this.waitingForCapacity = sib.waitingForCapacity;
    this.waitingForVeto = sib.waitingForVeto;
    this.waitingForUnknown = sib.waitingForUnknown;
    //counts
    this.lastIteratedOrders = sib.lastIteratedOrders;
    this.lastScheduledOrders = sib.lastScheduledOrders;
    this.countOfOrdersInScheduler = sib.countOfOrdersInScheduler;
    this.lastTransportedCaps = sib.lastTransportedCaps;
    //
    this.schedulerRunNumber = sib.schedulerRunNumber;
  }

  public Throwable getThreadDeathCause() {
    return threadDeathCause;
  }
  
  public void setThreadDeathCause(Throwable threadDeathCause) {
    this.threadDeathCause = threadDeathCause;
  }

  public long getThreadDeathTimestamp() {
    return threadDeathTimestamp;
  }

  public void setThreadDeathTimestamp(long threadDeathTimestamp) {
    this.threadDeathTimestamp = threadDeathTimestamp;
  }


  public int getCountOfOrdersInScheduler() {
    return countOfOrdersInScheduler;
  }


  public void setCountOfOrdersInScheduler(int cnt) {
    countOfOrdersInScheduler = cnt;
  }


  public int getWaitingForCapacity() {
    return waitingForCapacity;
  }


  public void setWaitingForCapacity(int cnt) {
    waitingForCapacity = cnt;
  }


  public int getWaitingForVeto() {
    return waitingForVeto;
  }


  public void setWaitingForVeto(int cnt) {
    waitingForVeto = cnt;
  }


  public int getWaitingForUnknown() {
    return waitingForUnknown;
  }


  public void setWaitingForUnknown(int cnt) {
    waitingForUnknown = cnt;
  }


  public long getLastScheduled() {
    return timestampStart;
  }


  public long getLastSchedulingTook() {
    return timestampFinished - timestampStart;
  }


  public boolean isSchedulerAlive() {
    return isSchedulerAlive;
  }


  public void setSchedulerAlive(boolean alive) {
    isSchedulerAlive = alive;
  }


  public List<XynaOrderInfo> getOrdersInScheduler() {
    return ordersInScheduler;
  }


  public void setOrdersInScheduler(List<XynaOrderInfo> orderInfo) {
    ordersInScheduler.addAll(orderInfo);
    countOfOrdersInScheduler = ordersInScheduler.size();
  }

  public long getTotalSchedulerRuns() {
    return this.schedulerRunNumber;
  }
  
  
  public void setSchedulerRunsLast5Minutes(long schedulerRuns) {
    this.schedulerRunsLast5Minutes = schedulerRuns;
  }


  public long getSchedulerRunsLast5Minutes() {
    return this.schedulerRunsLast5Minutes;
  }
  
  
  public void setSchedulerRunsLast60Minutes(long schedulerRuns) {
    this.schedulerRunsLast60Minutes = schedulerRuns;
  }


  public long getSchedulerRunsLast60Minutes() {
    return this.schedulerRunsLast60Minutes;
  }

  public void setSchedulerStatus(String status) {
    this.schedulerStatus = status;
  }
  
  public String getSchedulerStatus() {
    return this.schedulerStatus;
  }


  public void setCurrentlyScheduling(boolean currentlyScheduling) {
    this.currentlyScheduling = currentlyScheduling;
  }


  public boolean isCurrentlyScheduling() {
    return this.currentlyScheduling;
  }

  
  public int getLastIteratedOrders() {
    return lastIteratedOrders;
  }

  
  public int getLastScheduledOrders() {
    return lastScheduledOrders;
  }

  
  public void setLastIteratedOrders(int lastIteratedOrders) {
    this.lastIteratedOrders = lastIteratedOrders;
  }

  
  public void setLastScheduledOrders(int lastScheduledOrders) {
    this.lastScheduledOrders = lastScheduledOrders;
  }

  
  public int getLastTransportedCaps() {
    return lastTransportedCaps;
  }

  
  public void setLastTransportedCaps(int lastTransportedCaps) {
    this.lastTransportedCaps = lastTransportedCaps;
  }
  
  public void timestampStart(long ts) {
    this.timestampStart = ts;
  }
  
  public void timestampPreparation(long ts) {
    this.timestampPreparation = ts;
  }

  public void timestampScheduling(long ts) {
    this.timestampScheduling = ts;
  }
  
  public void timestampFinished(long ts) {
    this.timestampFinished = ts;
  }

  public void setLoopEndedRegularily(boolean loopEndedRegularily) {
    this.loopEndedRegularily = loopEndedRegularily;
  }

  public boolean isLoopEndedRegularily() {
    return loopEndedRegularily;
  }

  public long getPreparationDuration() {
    return timestampPreparation - timestampStart;
  }

  public long getSchedulingDuration() {
    return timestampScheduling - timestampPreparation;
  }

  public long getFinishDuration() {
    return timestampFinished - timestampScheduling;
  }

  public int getUnsatisfiedForeignDemand() {
    return unsatisfiedForeignDemand;
  }
 
  public void setUnsatisfiedForeignDemand(int unsatisfiedForeignDemand) {
    this.unsatisfiedForeignDemand = unsatisfiedForeignDemand;
  }

  public void setHistogram(List<EnumMap<HistogramColumn, Number>> histogram) {
    this.histogram = histogram;
  }
  
  public List<EnumMap<HistogramColumn, Number>> getHistogram() {
    return histogram;
  }

  public long getTimestampStart() {
    return timestampStart;
  }

  public long getSchedulingRunNumber() {
    return schedulerRunNumber;
  }

  public void setSchedulerRunNumber(long schedulerRunNumber) {
    this.schedulerRunNumber = schedulerRunNumber;
  }

}
