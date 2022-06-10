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
package com.gip.xyna.xprc.xsched.orderseries.tasks;

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xsched.orderseries.OSMCache;
import com.gip.xyna.xprc.xsched.orderseries.OSMInterface;
import com.gip.xyna.xprc.xsched.orderseries.OSMInterface.OrderState;
import com.gip.xyna.xprc.xsched.orderseries.OSMLocalImpl;
import com.gip.xyna.xprc.xsched.orderseries.OSMRemoteProxyImpl;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable.OrderStatus;



public abstract class OSMTask {
  
  protected static Logger logger = CentralFactoryLogging.getLogger(OSMTask.class);
  
  
  protected OSMCache osmCache;
  protected OSMInterface osm;
  protected OSMLocalImpl localOsm;
  protected OSMRemoteProxyImpl remoteOsm;
  protected PredecessorTrees predecessorTrees;
  protected int ownBinding;
    
  public void execute( OSMCache osmCache, OSMInterface osm, OSMLocalImpl localOsm, OSMRemoteProxyImpl remoteOsm, PredecessorTrees predecessorTrees ) {
    this.osmCache = osmCache;
    this.osm = osm;
    this.localOsm = localOsm;
    this.remoteOsm = remoteOsm;
    this.predecessorTrees = predecessorTrees;
    this.ownBinding = osm.getBinding();
    executeInternal();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()+"("+getCorrelationId()+")";
  }
  
  protected abstract void executeInternal();
  
  
  public abstract String getCorrelationId();
  
  /*
  Versuch, eine PriorityQueue zu verwenden: Problem Finish darf nicht Preschedule überholen, da sonst der 
  Cache (osmCache und predecessorTrees) in Unordnung gerät (nicht schlimm, aber zuviel Anfragen in DB
  sowie Leichen im Cache) 

  protected OSMTask(int priority, long creationTime) {
    this.priority = priority;
    this.creationTime = creationTime;
  }
  
  implements Comparable<OSMTask>
 
  protected int priority;
  protected long creationTime;

  public int compareTo(OSMTask o) {
    int otherPrio = o.priority;
    if( priority==otherPrio ) {
      return creationTime < o.creationTime ? -1 : 1;
    } else {
      return priority<otherPrio ? -1 : 1;
    }
  }
  */


  public static OSMTask updateSuccessor(int binding,
                                        String successorCorrId, String predecessorCorrId, long predecessorOrderId,
                                        boolean cancel) {
    return new OSMTask_UpdateSuccessor(binding, successorCorrId, predecessorCorrId, predecessorOrderId, cancel);
  }

  public static OSMTask updatePredecessor(int binding,
                                          String predecessorCorrId, String successorCorrId, long successorOrderId) {
    return new OSMTask_UpdatePredecessor(binding,predecessorCorrId, successorCorrId, successorOrderId);
  }
  
  public static OSMTask finish(XynaOrderServerExtension xo) {
    OrderStatus orderStatus = OrderStatus.SUCCEEDED;
    if( xo.isCancelled() ) {
      orderStatus = OrderStatus.CANCELING;
    } else if( xo.hasError() ) {
      orderStatus = OrderStatus.FAILED;
    } else {
      orderStatus = OrderStatus.SUCCEEDED;
    }
    return new OSMTask_Finish(xo.getSeriesCorrelationId(), orderStatus );
  }
  
  public static OSMTask abort(String seriesCorrelationId) {
    return new OSMTask_Finish(seriesCorrelationId, OrderStatus.FAILED );
  }
  
  public static OSMTask finish(String correlationId, OrderStatus orderStatus ) {
    return new OSMTask_Finish(correlationId, orderStatus );
  }

  public static OSMTask preschedule(SeriesInformationStorable sis ) {
    return new OSMTask_Preschedule(sis.getCorrelationId());
  }
 
  public static OSMTask preschedule(SeriesInformationStorable sis, XynaOrderServerExtension xo ) {
    return new OSMTask_Preschedule(sis.getCorrelationId());
  }
 
  public static OSMTask resume(String correlationId) {
    return new OSMTask_Resume(correlationId);
  }
 
  public static OSMTask preschedule(String correlationId) {
    return new OSMTask_Preschedule(correlationId);
  }
 
  public static OSMTask readyToRun(String correlationId, Long orderId, OrderState orderState, List<String> cycle) {
    return new OSMTask_ReadyToRun(correlationId, orderId, orderState, cycle);
  }
 
  public static OSMTask cleanPredecessorTrees() {
    return new OSMTask_CleanPredecessorTrees();
  }
 
  
}
