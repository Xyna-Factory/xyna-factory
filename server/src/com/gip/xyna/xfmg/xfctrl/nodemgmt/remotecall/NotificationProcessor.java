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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.RemoteCallNotification;

public class NotificationProcessor implements Runnable {

  private static final Logger logger = CentralFactoryLogging.getLogger(NotificationProcessor.class);
  private final FactoryNodeCaller factoryNodeCaller;
  private long keepAliveTimeout;
  private BlockingQueue<RemoteCallNotification> notifications;
  private volatile boolean running;
  private Thread processingThread;
  private String nodeName;
  
  public enum RemoteCallNotificationStatus {
    New(false),       //neuer Auftrag
    Execution(false), //Auftrag ist gerade in Bearbeitung
    Succeeded(true),  //Auftrag erfolgreich bearbeitet
    Failed(true),     //Auftrag nicht erfolgreich bearbeitet
    Parked(true),     //Auftrag geparkt für Resume
    Removed(true),    //Auftrag soll nicht mehr bearbeitet werden
  ;
    private boolean finalState;
    private RemoteCallNotificationStatus(boolean finalState) {
      this.finalState = finalState;
    }
    public boolean isFinalState() {
      return finalState;
    }
  };
  
  NotificationProcessor(
      FactoryNodeCaller factoryNodeCaller, 
      long keepAliveTimeout) {
    this.factoryNodeCaller = factoryNodeCaller;
    this.notifications = new LinkedBlockingQueue<RemoteCallNotification>();
    this.keepAliveTimeout = keepAliveTimeout;
    this.nodeName = factoryNodeCaller.getNodeName();
  }
  
  public void start() {
    this.running = true;
    this.processingThread = new Thread( this, "FNC-N-"+factoryNodeCaller.getNodeName());
    processingThread.start();
  }
  

  public void run() {
    while( running ) {
      try {
        RemoteCallNotification n = notifications.poll(keepAliveTimeout, TimeUnit.MILLISECONDS);
        if( n != null ) {
          processRemoteCallNotification(n, nodeName, factoryNodeCaller);
        } else {
          this.factoryNodeCaller.idle();
        }
      } catch (InterruptedException e) {
        //ignorieren: Wer diesen Thread beenden möchte, sollte stopRunning() rufen
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("Caught unexpected error in NotificationProcessor", t);
      }
    }
  }
  
  public void stopRunning() {
    running = false;
    processingThread.interrupt();
    processingThread = null;
  }
  
  /*
   * wird aufgerufen für
   * - StartOrder von NotificationProcessorthread
   * - AwaitConnectivity/AwaitApplicationAvailable => von NotificationProcessorthread
   * - AwaitOrder von Auftragsthread
   * - GetResult von Auftragsthread
   */
  public static void processRemoteCallNotification(RemoteCallNotification notification, String nodeName, FactoryNodeCaller fnc) {
    if( notification.getStatus().isFinalState() ) {
      return; //nichts zu tun (Fehler bei Succeeded oder Failed)
    }
    if( notification.startExecution(nodeName) ) { //zustandsübergang
      RemoteCallNotificationStatus status = notification.execute(fnc); //notification-typ-spezifisches execute (z.b. bei startorder der rmi-call)
      if( status == RemoteCallNotificationStatus.Execution ) {
        //bleibt in Bearbeitung, daher kein Notify 
      } else {
        if( notification.changeStatusFromExecutingAndNotify(status) ) { //benachrichtigt einen auf notification.await wartenden thread (gibt es nur für startorder/awaitorder) 
           //ok
          // im Falle einer Failed ausführung bei vorher pausiertem processor müssen wir jetzt pollen
          fnc.retrieveOrders(); //Zustandsübergang des RemoteProcessors auf Awake
        } else {
          //unerwartet
          notification.setStatusAndNotify(status); //trotzdem
        }
      }
    } else {
      //unerwartet
      if( notification.getStatus().isFinalState() ) {
        return; //nichts mehr zu tun (Sollte nach Remove gewechselt sein)
      } else {
        //bereits in Execution? 
        FactoryNodeCaller.logger.warn("Unexpected status "+ notification.getStatus() );
      }
    }
  }

  public void add(RemoteCallNotification notification) {
    notifications.add(notification);
  }

  public boolean isFinished() {
    return notifications.isEmpty();
  }

  public Collection<RemoteCallNotification> getNotifications() {
    return Collections.unmodifiableCollection(notifications);
  }

}