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
package com.gip.xyna.xprc.xsched.orderseries;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoException;
import com.gip.xyna.xprc.xsched.orderseries.tasks.OSMTask;


/**
 * OSMRemoteProxyImpl:
 * Implementierung des Interfaces OSMRemoteInterface, die die notwendigen Änderungen an den
 * SeriesInformationStorable remote durchführt.<br>
 * <br>
 * Algorithmus für update{Successor/Predecessor}<br>
 * 1) Ist <code>clustered==true</code><br>
 * 1.1) Ja: Remote-Aufruf über RMIClusterProviderTools<br>
 * 1.2) Nein: Aufruf update{Successor/Predecessor}Later<br>
 * <br>
 * Algorithmus für update{Successor/Predecessor}Later<br>
 * 1) Lesen des SeriesInformationStorable <code>sis</code> zu {successor/predecessor}CorrId<br>
 * 2) Eintragen eines neuen OSMTask_Update{Successor/Predecessor} mit Binding aus <code>sis</code> in die Taskqueue<br>
 * 3) Rückgabe Result.Later<br>
 */
public class OSMRemoteProxyImpl implements OSMRemoteInterface {
  
  private RMIClusterProvider clusterInstance;
  private volatile long clusteredOSMInterfaceId;
  private Queue<OSMTask> queue;
  private OSMCache osmCache;
  private static final long RMI_IS_CLOSED = -1;
  private volatile boolean rmiConnected;
  private OSMRemoteEndpointImpl osmRemoteEndpointImpl;
  private ClusterContext rmiClusterContext;
  private Timer retryLaterTimer;
  
  /**
   * @param rmiClusterContext 
   * @param osmRemoteEndpointImpl
   */
  public OSMRemoteProxyImpl(ClusterContext rmiClusterContext, OSMRemoteEndpointImpl osmRemoteEndpointImpl, Queue<OSMTask> queue, OSMCache osmCache ) {
    this.rmiClusterContext = rmiClusterContext;
    this.osmRemoteEndpointImpl = osmRemoteEndpointImpl;
    this.queue = queue;
    this.osmCache = osmCache;
    this.retryLaterTimer = new Timer("OSMRemoteProxyImpl-retryLaterTimer");
  }

  /**
   * Auf <code>true</code> zu setzen, wenn der Remote-Knoten erreicht werden kann
   * @param rmiConnected
   */
  public synchronized void setRmiConnected(boolean rmiConnected) {
    this.rmiConnected = rmiConnected;
    if( rmiConnected ) {
      if( clusterInstance == null ) {
        clusterInstance = ((RMIClusterProvider) rmiClusterContext.getClusterInstance());
        clusteredOSMInterfaceId = clusterInstance.addRMIInterface("RemoteOrderSeriesManagement", osmRemoteEndpointImpl);
      }
    }
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xsched.orderseries.OSMRemoteInterface#updateSuccessor(int, java.lang.String, java.lang.String, long, boolean)
   */
  public Result updateSuccessor(int binding, String successorCorrId, String predecessorCorrId, long predecessorOrderId,
                                boolean cancel) {
    if( rmiConnected ) {
      try {
        //TODO Besser: nur an Knoten mit Binding "binding" schicken! 
        UpdateSuccessorRunnable usr = new UpdateSuccessorRunnable(binding,successorCorrId, predecessorCorrId, predecessorOrderId, cancel );
        RMIClusterProvider clusterInstance;
        long clusteredOSMInterfaceId;
        synchronized (this) {
          clusterInstance = this.clusterInstance;
          clusteredOSMInterfaceId = this.clusteredOSMInterfaceId;
        }
        List<Result> res = RMIClusterProviderTools.executeAndCumulateNoException(clusterInstance, clusteredOSMInterfaceId, usr, null, Result.Later);
        Result result = singleResult( res );
        if( result == Result.Later ) {
          return updateSuccessorLater(binding,successorCorrId,predecessorCorrId,predecessorOrderId,cancel);
        } else {
          return result;
        }
      } catch (InvalidIDException e) {
        handleInvalidIDException(e);
        return updateSuccessorLater(binding,successorCorrId,predecessorCorrId,predecessorOrderId,cancel);
      }
    } else {
      return updateSuccessorLater(binding,successorCorrId,predecessorCorrId,predecessorOrderId,cancel);
    }
  }
  

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xsched.orderseries.OSMRemoteInterface#updatePredecessor(int, java.lang.String, java.lang.String, long)
   */
  public Result updatePredecessor(int binding, String predecessorCorrId, String successorCorrId, long successorOrderId) {
    if( rmiConnected ) {
      try {
        //TODO Besser: nur an Knoten mit Binding "binding" schicken!  
        UpdatePredecessorRunnable upr = new UpdatePredecessorRunnable(binding, predecessorCorrId, successorCorrId, successorOrderId );
        RMIClusterProvider clusterInstance;
        long clusteredOSMInterfaceId;
        synchronized (this) {
          clusterInstance = this.clusterInstance;
          clusteredOSMInterfaceId = this.clusteredOSMInterfaceId;
        }
        List<Result> res = RMIClusterProviderTools.executeAndCumulateNoException(clusterInstance, clusteredOSMInterfaceId, upr, null, Result.Later );
        Result result = singleResult( res );
        if( result == Result.Later ) {
          return updatePredecessorLater(binding,predecessorCorrId, successorCorrId, successorOrderId);
        } else {
          return result;
        }
      } catch (InvalidIDException e) {
        handleInvalidIDException(e);
        return updatePredecessorLater(binding,predecessorCorrId, successorCorrId, successorOrderId);
      }
    } else {
      return updatePredecessorLater(binding,predecessorCorrId, successorCorrId, successorOrderId);
    }
  }

  private Result updateSuccessorLater(int binding, String successorCorrId, String predecessorCorrId,
                                      long predecessorOrderId, boolean cancel) {
    
    //Binding überprüfen, da sonst Endlosschleife auftritt, falls Knoten mit dem binding <code>binding</code> nicht wiederkommt
    SeriesInformationStorable sis = osmCache.get(successorCorrId);
    return executeLater( OSMTask.updateSuccessor(sis.getBinding(),successorCorrId,predecessorCorrId,predecessorOrderId,cancel) );
  }
  
  private Result updatePredecessorLater(int binding, String predecessorCorrId, String successorCorrId,
                                        long successorOrderId) {
    //Binding überprüfen, da sonst Endlosschleife auftritt, falls Knoten mit dem binding <code>binding</code> nicht wiederkommt
    SeriesInformationStorable sis = osmCache.get(successorCorrId);
    return executeLater( OSMTask.updatePredecessor(sis.getBinding(),predecessorCorrId, successorCorrId, successorOrderId) );
  }
 
  /**
   * @param updateSuccessor
   * @return
   */
  private Result executeLater(OSMTask osmTask) {
    //Mit Verzögerung einstellen zur Lastbegrenzung
    //2 Anwendungsfälle:
    //a) kurzfristiger Ausfall der RMI-Verbindung
    //b) Ordermigration: Predecessor ist bereits migriert, und möchte nun updateSuccessor ausführen,
    //dieser ist aber noch nicht migriert
    retryLaterTimer.schedule( new Later(queue, osmTask), 500); //TODO delay konfigurierbar
    return Result.Later; 
  }

  private static class Later extends TimerTask {

    private Queue<OSMTask> queue;
    private OSMTask osmTask;

    public Later(Queue<OSMTask> queue, OSMTask osmTask) {
      this.queue = queue;
      this.osmTask = osmTask;
    }

    @Override
    public void run() {
      queue.add( osmTask );
    }
    
  }
  
  
  private Result singleResult(List<Result> res) {
    //alle falschen Knoten antworten mit Result.NotFound, nur der mit richtigem Binding 
    //antwortet evtl. anders
    boolean later = false;
    for( Result r : res ) {
      switch( r ) {
        case Later:
          later = true; //mindestens ein Knoten antwortet nicht
          break;
        case NotFound:
          break;   //Knoten kennt das Serienmitglied nicht 
        case Cancel:
        case Running:
        case Success:
          //Knoten kennt das Serienmitglied
          return r;
      }
    }
    if( later ) {
      Logger.getLogger(getClass()).info("Foreign node failed to answer, retrying later");
      return Result.Later; //einer der Knoten antwortete nicht, andere Knoten antworten nur Result.NotFound
    } else {
      return Result.NotFound; //Result.NotFound war einzige Rückmeldung
    }
  }

  private void handleInvalidIDException(InvalidIDException e) {
    if (clusteredOSMInterfaceId == RMI_IS_CLOSED) {
      //closed rmi        
    } else {
      //nicht erwartet
      throw new RuntimeException(e);
    }
  }

  
  
  private static class UpdateSuccessorRunnable implements RMIRunnableNoException<Result, OSMRemoteInterface> {

    private int binding;
    private String successorCorrId;
    private String predecessorCorrId;
    private long predecessorOrderId;
    private boolean cancel;
    
    public UpdateSuccessorRunnable(int binding, String successorCorrId, String predecessorCorrId, long predecessorOrderId,
                                   boolean cancel) {
      this.binding = binding;
      this.successorCorrId = successorCorrId;
      this.predecessorCorrId = predecessorCorrId;
      this.predecessorOrderId = predecessorOrderId;
      this.cancel = cancel;
    }

    public Result execute(OSMRemoteInterface clusteredInterface) throws RemoteException {
      return clusteredInterface.updateSuccessor(binding, successorCorrId, predecessorCorrId, predecessorOrderId, cancel);
    }
    
  }
  

  private static class UpdatePredecessorRunnable implements RMIRunnableNoException<Result, OSMRemoteInterface> {

    private int binding;
    private String predecessorCorrId;
    private String successorCorrId;
    private long successorOrderId;
    
    public UpdatePredecessorRunnable(int binding, String predecessorCorrId, String successorCorrId, long successorOrderId) {
      this.binding = binding;
      this.predecessorCorrId = predecessorCorrId;
      this.successorCorrId = successorCorrId;
      this.successorOrderId = successorOrderId;
    }

    public Result execute(OSMRemoteInterface clusteredInterface) throws RemoteException {
      return clusteredInterface.updatePredecessor(binding, predecessorCorrId, successorCorrId, successorOrderId);
    }
    
  }

  

  
}
