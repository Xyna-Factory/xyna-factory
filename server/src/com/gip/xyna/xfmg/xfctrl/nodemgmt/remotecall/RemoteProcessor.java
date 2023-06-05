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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.concurrent.AtomicEnum;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectOrderLostException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteData;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteData.RemoteDataApplicationChangeNotification;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteData.RemoteDataOrderResponse;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.RemoteCallNotification;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.SetResultNotification;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.RMIChannelImpl;

public class RemoteProcessor implements Runnable {

  static final Logger logger = CentralFactoryLogging.getLogger(RemoteProcessor.class);
  
  private String id;
  private FactoryNodeCaller factoryNodeCaller;
  private volatile boolean running;
  private Thread processingThread;
  private AtomicEnum<Mode> mode;
  private RemoteOrderExecution remoteOrderExecution;
  private SleepCounter checkConnectivityInterval;

  private enum Mode {
    RetrieveData, CheckConnectivity, Paused;
  }


  private static final XynaPropertyDuration propertyMaxCheckConnectivity =
      new XynaPropertyDuration("xfmg.xfctrl.nodemgmt.remotecall.checkconnectivity.interval.max", new Duration(30000))
          .setDefaultDocumentation(DocumentationLanguage.EN, "Maximum interval between remote call connectivity checks.");
  private static final XynaPropertyDuration propertyIncrementCheckConnectivity =
      new XynaPropertyDuration("xfmg.xfctrl.nodemgmt.remotecall.checkconnectivity.interval.increment", new Duration(5000))
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Minimum interval between remote call connectivity checks and increment after each unsuccessful check.");


  public RemoteProcessor(FactoryNodeCaller factoryNodeCaller, RemoteOrderExecution remoteOrderExecution,
                         Map<Long, OrderExecutionResponse> responses) {
    this.factoryNodeCaller = factoryNodeCaller;
    this.remoteOrderExecution = remoteOrderExecution;
    this.mode = new AtomicEnum<Mode>(Mode.class, Mode.CheckConnectivity);
    this.checkConnectivityInterval = new SleepCounter(propertyIncrementCheckConnectivity.getMillis(),
                                                      propertyMaxCheckConnectivity.getMillis(), 0, TimeUnit.MILLISECONDS, false);
    this.id = "FNC-R-" + factoryNodeCaller.getNodeName();
  }


  public void start() {
    this.running = true;
    this.processingThread = new Thread( this, id);
    processingThread.start();
  }

  public void run() {
    while( running ) {
      try {
        switch (switchMode()) {
          case DONTSLEEP:
            break;
          case AWAIT_DATA_TIMEOUT :
            Thread.sleep(XynaProperty.RMI_IL_SOCKET_TIMEOUT.getMillis()*9/10);
            break;
          case CHECK_CONNECTIVITY :
            checkConnectivityInterval.sleep();
            break;
        }
      } catch (InterruptedException e) {
        //dann eben kürzer warten
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("RemoteProcessor caught unexpected error", t);
      }
    }
  }
  
  private enum SleepType {
    DONTSLEEP, CHECK_CONNECTIVITY, AWAIT_DATA_TIMEOUT;
  }
  
  private SleepType switchMode() {
    switch( mode.get() ) {
    case CheckConnectivity:
      return executeCheckConnectivity();
    case RetrieveData:
      executeRetrieveData();
      return SleepType.DONTSLEEP;
    case Paused:
      return SleepType.AWAIT_DATA_TIMEOUT;
   default: 
      mode.set(Mode.CheckConnectivity);
      return SleepType.CHECK_CONNECTIVITY;
    }
  }
 
  
  private void executeRetrieveData() {
    List<RemoteData> remoteData;
    try {
      remoteData = remoteOrderExecution.awaitData(InterFactoryLinkProfileIdentifier.OrderExecution.getTimeout().getDurationInMillis()*9/10);
    } catch (XFMG_NodeConnectException e) {
      logger.warn(" executeRetrieveData -> ", e);
      factoryNodeCaller.checkConnectivity();
      return;
    }
    
    List<OrderExecutionResponse> oers = new ArrayList<OrderExecutionResponse>();
    for (RemoteData rd : remoteData) {
      if (rd instanceof RemoteDataApplicationChangeNotification) {
        RemoteDataApplicationChangeNotification rdacn = (RemoteDataApplicationChangeNotification) rd;
        for (String appName : rdacn.getApplications()) {
          factoryNodeCaller.getResumer().resumeWaitingForApplicationAvailability(factoryNodeCaller.getNodeName(), appName);        
        }
      } else if (rd instanceof RemoteDataOrderResponse) {
        oers.add(((RemoteDataOrderResponse) rd).getResponse());
      } else {
        throw new RuntimeException("unsupported: " + rd);
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug(" Retrieved "+ oers.size() + " remote OrderExecutionResponses");
    }
    if( oers.size() == 0 ) {
      List<Long> orderIds = new ArrayList<Long>(factoryNodeCaller.getAwaitOrderIds());
      if( ! orderIds.isEmpty() ) {
        List<Long> unknownOrderIds = remoteOrderExecution.checkRunningOrders(orderIds);
        if (logger.isDebugEnabled() && unknownOrderIds.size() > 0) {
          logger.debug( "unknownOrderIds " + unknownOrderIds );
        }
        for( Long remoteOrderId : unknownOrderIds ) {
          XFMG_NodeConnectOrderLostException cause = 
              new XFMG_NodeConnectOrderLostException(factoryNodeCaller.getNodeName(), remoteOrderId);
          ErroneousOrderExecutionResponse eoer = new ErroneousOrderExecutionResponse(cause, RMIChannelImpl.defaultController);
          eoer.setOrderId(remoteOrderId);
          addResponse(eoer);
        }
      }
      
    } else {
      for( OrderExecutionResponse oer : oers ) {
        addResponse(oer);
      }
    }
    
    if( factoryNodeCaller.getAwaitOrderIds().isEmpty() && factoryNodeCaller.getResumer().getWaitingForApplicationAvailability(factoryNodeCaller.getNodeName()) == 0 ) {
      mode.compareAndSet(Mode.RetrieveData, Mode.Paused); //alles abgeholt, daher pausieren
    }
  }

  private void addResponse(OrderExecutionResponse oer) {
    RemoteCallNotification notification = new SetResultNotification(oer);
    factoryNodeCaller.enqueue(notification);
  }


  public void stopRunning() {
    running = false;
    processingThread.interrupt();
    processingThread = null;
  }
  
  private SleepType executeCheckConnectivity() {
    factoryNodeCaller.checkConnectivity(); //setzt Status auf Connecting, zusätzliches awake verpufft, da mode nicht geändert wird
    boolean connected = remoteOrderExecution.checkConnectivity();
    if( ! connected ) {
      return SleepType.CHECK_CONNECTIVITY;
    } else {
      checkConnectivityInterval.reset();
      mode.compareAndSet(Mode.CheckConnectivity, Mode.RetrieveData);
      factoryNodeCaller.reconnect(); 
      return SleepType.DONTSLEEP;
    }
  }

  /**
   * von RemoteProcessor (Thread FNC-R-*), NotificationProcessor (Thread FNC-N-*) gerufen, evtl. extern
   */
  public void checkConnectivity() {
    awake(Mode.CheckConnectivity);
  }
  
  /**
   * von NotificationProcessor (Thread FNC-N-*) gerufen, evtl. extern
   */
  public void retrieveOrders() {
    awake(Mode.RetrieveData);
  }

  private void awake(Mode next) {
    if( mode.is(next) ) {
      return; //nichts zu tun
    }
    if( mode.compareAndSet(Mode.Paused, next) ) {
      processingThread.interrupt();
    } else {
      mode.compareAndSet(Mode.RetrieveData, next);
    }
    //Umsetzen von CheckConnectivity weg ist nicht erlaubt, dies geschieht automatisch in executeCheckConnectivity()
  }

}
