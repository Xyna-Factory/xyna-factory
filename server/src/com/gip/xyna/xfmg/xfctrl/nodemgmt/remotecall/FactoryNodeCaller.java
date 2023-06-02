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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.NotificationProcessor.RemoteCallNotificationStatus;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.Resumer.ResumeData;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.AwaitOrderNotification;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.RemoteCallNotification;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xmcp.OrderExecutionResponse;

public class FactoryNodeCaller {

  static final Logger logger = CentralFactoryLogging.getLogger(FactoryNodeCaller.class);
  
  private FactoryNode node;
  private String nodeName; 
  private Map<Long,OrderExecutionResponse> responses;
  private Map<Long,AwaitOrderNotification> awaitResponses;
  private NotificationProcessor notificationProcessor;
  private RemoteProcessor remoteProcessor;
  private FactoryNodeCallerStatus status;
  private Resumer resumer;
  private RemoteOrderExecution remoteOrderExecution;
    
  public enum FactoryNodeCallerStatus {
    Unused, Connecting, Idle, Connected,
  }
  
  private static final XynaPropertyDuration fncKeepAlive = new XynaPropertyDuration("xfmg.xfctrl.nodemgmt.remotecall.keepAlive", "1 h")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Keep alive after last remote call")
      .setDefaultDocumentation(DocumentationLanguage.DE, "KeepAlive nach dem letzten Remote-Call")
      ;

  
  
  public FactoryNodeCaller(FactoryNode factoryNode, Resumer resumer, String identifier) {
    this.node = factoryNode;
    this.resumer = resumer;
    this.nodeName = this.node.getNodeInformation().getName();
    this.responses = new ConcurrentHashMap<Long,OrderExecutionResponse>();
    this.awaitResponses = new ConcurrentHashMap<Long,AwaitOrderNotification>();
    this.remoteOrderExecution = new RemoteOrderExecution(node, identifier );
    this.notificationProcessor = new NotificationProcessor(this, fncKeepAlive.getMillis() );
    this.remoteProcessor = new RemoteProcessor(this, remoteOrderExecution, responses);
    this.status = FactoryNodeCallerStatus.Unused;
    this.remoteProcessor.start();
    this.notificationProcessor.start();
  }

  /**
   * Trennung der Threads: hierüber werden RemoteCallNotification dem NotificationProcessor-Thread übergeben.
   * Dieser ruft ein notify auf dem RemoteCallNotification-Objekt auf, so dass der Aufrufer nach einem await() 
   * weiterarbeiten kann.
   * 
   * @param notification
   */
  public void enqueue(RemoteCallNotification notification) {
    notificationProcessor.add(notification);
  }
  
  public FactoryNodeCallerStatus getStatus() {
    return status;
  }

  public String getNodeName() {
    return nodeName;
  }
  
  public int getWaitingForResult() {
    return awaitResponses.size();
  }

  public int getWaitingForConnectivity() {
    return resumer.getWaiting(nodeName);
  }
  
  /**
   * NotificationThread ist idle, daher evtl. FactoryNodeCaller entfernen
   */
  protected void idle() {
    if( ! responses.isEmpty() ) {
      return;
    }
    if( ! awaitResponses.isEmpty() ) {
      return;
    }
    
    //erst aus NodeManagment entfernen, damit FactorNodeCaller nicht mehr gefunden wird
    NodeManagement nm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
    nm.removeFactoryNodeCaller(nodeName);
    shutdown();
  }
  
  public void shutdown() {
    shutdown(true);
  }
  
  public void shutdown(boolean hasSuccessor) {
    NodeManagement nm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
    
    //Threads beenden
    notificationProcessor.stopRunning();
    remoteProcessor.stopRunning();
    
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      //dann halt kürzer warten
    }
    
    //Sind noch Notifications eingegangen?
    if( ! notificationProcessor.isFinished() ) {
      //nun sind doch nochmal unerwartet Aufträge eingegangen ...
      //diese nun umtragen in aktuellen FactoryNodeCaller
      if(hasSuccessor) {
        FactoryNodeCaller successor = nm.getFactoryNodeCaller(nodeName);
        for( RemoteCallNotification n : notificationProcessor.getNotifications()) {
          successor.enqueue(n);
        }
      } else {
        for( RemoteCallNotification n : notificationProcessor.getNotifications()) {
          n.setStatusAndNotify(RemoteCallNotificationStatus.Removed);
        }
      }
    }
    
    //Sind noch Responses eingegangen?
    if( ! responses.isEmpty() && hasSuccessor) {
      //nun sind doch nochmal unerwartet Aufträge eingegangen ...
      //diese nun umtragen in aktuellen FactoryNodeCaller
      FactoryNodeCaller successor = nm.getFactoryNodeCaller(nodeName);
      successor.responses.putAll(responses);
    }
    
    if( ! awaitResponses.isEmpty() ) {
      //nun sind doch nochmal unerwartet Aufträge eingegangen ...
      //diese nun umtragen in aktuellen FactoryNodeCaller
      if(hasSuccessor) {
        FactoryNodeCaller successor = nm.getFactoryNodeCaller(nodeName);
        successor.awaitResponses.putAll(awaitResponses);
      } else {
        for(AwaitOrderNotification awaitResponse : awaitResponses.values()) {
          awaitResponse.abort(nodeName);
        }
      }
    }
    
  }
  

  public void checkConnectivity() {
    status = FactoryNodeCallerStatus.Connecting;
    remoteProcessor.checkConnectivity();
  }

  public void reconnect() {
    resumer.resumeAll(nodeName);
    status = FactoryNodeCallerStatus.Connected;
  }
  
  public void retrieveOrders() {
    remoteProcessor.retrieveOrders();
  }
  
  public void disconnect() {
    //alle AwaitOrderNotification mit Fehler beenden, damit wartende Threads nicht auf Reconnect warten müssen
    for( Map.Entry<Long,AwaitOrderNotification> entry : awaitResponses.entrySet() ) {
      entry.getValue().disconnect(nodeName);
      awaitResponses.remove(entry.getKey());
    }
  }

  /**
   * 
   * @return false, falls es bereits eine antwort gibt
   */
  public boolean addAwaitOrder(AwaitOrderNotification awaitOrder) {
    awaitResponses.put(awaitOrder.getRemoteOrderId(), awaitOrder );
    if (responses.containsKey(awaitOrder.getRemoteOrderId())) {
      return false;
    }
    if( awaitOrder.getResumeTarget() != null ) {
      resumer.addAwaitOrder(awaitOrder.getTimeout(), awaitOrder.getRemoteOrderId(), nodeName, awaitOrder.getResumeTarget());
    }
    remoteProcessor.retrieveOrders();
    return true;
  }

  public OrderExecutionResponse removeResponse(Long remoteOrderId) {
    awaitResponses.remove(remoteOrderId);
    return responses.remove(remoteOrderId);
  }

  public boolean addResponse(OrderExecutionResponse oer) {
    Long remoteOrderId = oer.getOrderId();
    responses.put(remoteOrderId, oer);
    AwaitOrderNotification awaitOrder = awaitResponses.remove(remoteOrderId);
    if (awaitOrder == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not deliver remote response " + remoteOrderId);
      }
      return false;
    }
    awaitOrder.setResponseRetrieved(resumer);
    return true;
  }

  public Set<Long> getAwaitOrderIds() {
    return Collections.unmodifiableSet(awaitResponses.keySet());
  }

  public Resumer getResumer() {
    return resumer;
  }

  public RemoteOrderExecution getRemoteOrderExecution() {
    return remoteOrderExecution;
  }

  public boolean hasResult(Long remoteOrderId) {
    return responses.containsKey(remoteOrderId);
  }

  public void cancel(AwaitOrderNotification awaitOrder) {
    awaitResponses.remove(awaitOrder.getRemoteOrderId());
    if( awaitOrder.getResumeTarget() != null ) {
      resumer.remove(ResumeData.normal(nodeName, awaitOrder.getRemoteOrderId(), awaitOrder.getResumeTarget()));
    }
  }

}
