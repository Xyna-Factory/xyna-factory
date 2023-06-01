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
package com.gip.xyna.xprc.xsched.vetos.cache;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.ReplicateVetoRequest;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VCP_Remote;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VCP_RemoteImpl;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VCP_RemoteInterface;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoRequest;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoRequest.VetoRequestEntry;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse.VetoResponseEntry;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse.VetoResponseEntry.Compare;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse.VetoResponseEntry.Response;

public class VCP_Clustered extends VCP_Abstract {

  private static final Logger logger = CentralFactoryLogging.getLogger(VCP_Clustered.class);

  private VCP_Remote local;
  private VCP_RemoteInterface remoteImpl;
  private VetoRequest remoteVetoRequests;
  private int ownBinding;
  private long requestId;
  private volatile Status status;
  private CountDownLatch replicationLatch;
  private RMIClusterProvider clusterProvider;
  private boolean conflictedCompare;
  
  private enum Status {Init, Replicating, Running; }
  
  public VCP_Clustered(VetoCache vetoCache, VetoCachePersistence persistence, int ownBinding) {
    super(vetoCache, persistence);
    this.ownBinding = ownBinding;
    this.remoteVetoRequests = new VetoRequest(requestId++, ownBinding);
    this.local = new VCP_Remote(this, vetoCache, ownBinding);
    this.status = Status.Init;
    this.replicationLatch = new CountDownLatch(1);
  }
  
  public VCP_Remote getLocal() {
    return local;
  }
  
  public void setRemoteImpl(VCP_RemoteInterface remoteImpl) {
    this.remoteImpl = remoteImpl;
  }

  @Override
  protected boolean hasVetosToProcess() {
    return vetoCache.hasVetosToProcess() || ! remoteVetoRequests.isEmpty();
  }
  
  @Override
  protected void startBatch() {
    if( status == Status.Running ) {
      return; //Normalbetrieb, nichts zu tun
    }
    long now = System.currentTimeMillis();
    long start = now;
    while( status == Status.Init ) {
      int rep = replicateVetos();
      if( rep >= 0  ) {
        //eigenes Replizieren hat geklappt
        status = Status.Replicating;
        now = System.currentTimeMillis();
        logger.info("Replicated "+rep+" vetos to other node after "+(now-start)+" ms");
        start = now;
      }
    }
    
    //nun noch Replizieren des anderen Knotens abwarten
    while( status == Status.Replicating ) {
      try {
        if( ! replicationLatch.await(180, TimeUnit.SECONDS) ) {
          logger.warn("Failed to replicate vetos from other node. VetoManagement can not run normally. "+
             "To prevent OOM due to full VetoCache.vetosToProcess factory will disconnect now.");
          clusterProvider.disconnect();
          continue;
        }
        status = Status.Running;
        now = System.currentTimeMillis();
        logger.info("Replicated vetos from other node after "+(now-start)+" ms");
      } catch (InterruptedException e) {
        //dann halt nicht mehr warten
        Thread.currentThread().interrupt();
      }
    }
  }
  
  @Override
  protected void endBatch() {
    if( ! remoteVetoRequests.isEmpty() ) {
      VetoRequest currentRequest = remoteVetoRequests;
      this.remoteVetoRequests = new VetoRequest(requestId++, ownBinding);
      
      VetoResponse response = sendRemoteRequests(currentRequest);
      if( logger.isTraceEnabled()) {
        logger.trace("##  response = "+response); 
      }
      handleResponse(currentRequest, response );
      if( conflictedCompare ) {
        //Verhinderung eines permanenten Zustandswechsels Compare->Comparing->Compare->Comparing->...
        try {
          Thread.sleep((long)(Math.random()*3));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        conflictedCompare = false;
      }
    }
    //erst jetzt Vetos persistieren, da danach evtl. Vetos entfernt werden
    super.endBatch();
  }
  
  private VetoResponse sendRemoteRequests(VetoRequest currentRequest) {
    if( logger.isTraceEnabled()) {
      logger.trace("##  remoteVetoRequests = "+currentRequest); 
    }
    try {
      return remoteImpl.processRemoteVetoRequest(currentRequest);
    } catch (RemoteException e) {
      //unerwartet
      return VetoResponse.failed( currentRequest.getId(), e.getMessage() ); 
    }
  }
  
  private void handleResponse(VetoRequest currentRequest, VetoResponse response) {
    boolean failed = false;
    if( response.isFailed() ) {
      //FIXME was nun? 
      //Sollte eigentlich nicht auftreten außer bei disconnect
      logger.warn( "VetoResponse is failed " + response );
      failed = true;
    } else if( currentRequest.getId() != response.getId() ) {
      logger.warn( "VetoResponse is unexpected: got " + response.getId()+ " expected "+currentRequest.getId() );
      failed = true;
    }
    
    if( failed ) {
      for( VetoRequestEntry vre : currentRequest.getEntries() ) {
        VetoCacheEntry veto = vetoCache.get(vre.getName());
        if( veto != null ) {
          veto.getHistory().remoteActionEnd(ownBinding, "failed");
          vetoCache.process(veto.getName()); //ob das klappt?
        }
      }
    } else {
      List<Pair<VetoRequestEntry, VetoResponseEntry>> requestResponses = 
          combineRequestResponse(currentRequest,response);
      
      for( Pair<VetoRequestEntry, VetoResponseEntry> requestResponse : requestResponses ) {
        processRemoteResponse( currentRequest.getId(), requestResponse );
      }
    }
  }

  private int replicateVetos() {
    ReplicateVetoRequest replicateVetoRequest = new ReplicateVetoRequest(ownBinding);
    for( VetoCacheEntry veto : vetoCache.entries() ) {
      replicateVetoRequest.replicate(veto);
    }
    try {
      remoteImpl.replicate(replicateVetoRequest);
      return replicateVetoRequest.getEntries().size();
    } catch (RemoteException e) {
      //unerwartet
      logger.warn( "Veto Replication is failed", e);
      return -1;
    }
  }

  private List<Pair<VetoRequestEntry, VetoResponseEntry>> combineRequestResponse(VetoRequest request, VetoResponse response) {
    List<Pair<VetoRequestEntry,VetoResponseEntry>> combi = new ArrayList<Pair<VetoRequestEntry,VetoResponseEntry>>();
    Iterator<VetoRequestEntry> reqIter = request.getEntries().iterator();
    Iterator<VetoResponseEntry> respIter = response.getResponses().iterator();
    while( reqIter.hasNext() && respIter.hasNext() ) { //TODO Länge prüfen, Namen prüfen
      combi.add( Pair.of(reqIter.next(), respIter.next() ) );
      
    }
    return combi;
  }

  @Override
  protected boolean processCompare(VetoCacheEntry veto) {
    if( veto.compareAndSetState(State.Compare, State.Comparing ) ) {
      remoteVetoRequests.compare(veto);
      return false;
    } else {
      return processAgain(veto, "not compare"); //unerwartet, nochmal bearbeiten
    }
  }
  //dazwischen auf anderem Knoten VCP_Remote.processCompare
  private Result processCompare(VetoCacheEntry veto, Response response, VetoResponseEntry responseEntry) {
    State next = State.Compare;
    if( response == Response.COMPARE ) {
      Compare result = responseEntry.getCompareResult();
      switch( result ) {
      case ADMIN: //administratives Veto muss gleich in DB eingetragen werden
        next =  State.Scheduled;
        break;
      case LOCAL: //anderer Knoten erhält Veto
        next =  State.Remote;
        break;
      case REMOTE: //Knoten erhält Veto
        next =  State.Local;
        break;
      case UNUSED: // Veto wird nicht mehr verwendet
        next =  State.None;
        break;
      }
    }
    if( veto.compareAndSetState(State.Comparing, next) ) {
      switch( next ) {
      case None:
        vetoCache.remove(veto, State.None);
        return Result.Ok;
      case Scheduled:
        processScheduled(veto);
        return Result.Ok;
      case Compare:
        if( response == Response.IN_USE ) {
          conflictedCompare = true;
        }
        return Result.Reprocess;
      case Local:
      case Remote:
        return Result.Ok;
      default:
        return Result.Failed;
      }
    } else {
      return Result.Failed;
    }
  }

  public boolean isVetoPreferred(long ownUrgency, VetoCacheEntry veto) {
    //Über Long.bitCount(ownUrgency) % 2 wird eine möglichst "zufällige" Zahl 0 oder 1 auf beiden Knoten gleich
    //ermittelt und mit ownBinding verglichen, diese ergibt dann nur auf einem Knoten true.
    //ownUrgency % 2 ergibt häufig 0, wenn die Auftragsstartzeit nur sekundengenau angegeben wurde.
    return ownBinding % 2 == Long.bitCount(ownUrgency) % 2;
  }

  
  @Override
  protected boolean processRemote(VetoCacheEntry veto) {
    //nicht bearbeiten, muss über RMI geschehen
    return false; 
  }
  @Override
  protected boolean processComparing(VetoCacheEntry veto) {
    //nicht bearbeiten, muss im Anschluss an RMI geschehen
    return false; 
  }

  @Override
  protected boolean processScheduled(VetoCacheEntry veto) {
    persist(veto, true);
    if( veto.getBinding() == ownBinding ) {
      remoteVetoRequests.scheduled(veto);
    } else {
      //nichts mehr zu tun
    }
    return false;
  }
  private Result processScheduled(VetoCacheEntry veto, Response response, VetoResponseEntry responseEntry) {
    switch( response ) {
    case IN_USE:
      return Result.Reprocess;
    case SUCCESS:
      return Result.Ok;
    default:
      return Result.Failed;
    }
  }
  

  @Override
  protected boolean processFree(VetoCacheEntry veto) {
    persist(veto, false);
    if( veto.isAdministrative() ) {
      //in jedem Fall an anderen Knoten weitergeben, da removeadminveto evtl. auf anderem Knoten aufgerufen wird
      remoteVetoRequests.freeAdmin(veto);
    } else {
      if( veto.getBinding() == ownBinding ) {
        remoteVetoRequests.free(veto);
      } 
      //nichts mehr zu tun
    }
    return false;
  }
  
  @Override
  protected void processFreeAfterPersist(VetoCacheEntry veto) {
    int binding = veto.getBinding();
    if( veto.prepareCompare(State.Free, binding != ownBinding ) ) {
      veto.removeVetoInformation();
      if( binding == ownBinding ) {
        processCompare(veto);
      }
    } else {
      //unerwartet
      vetoCache.process(veto); //nochmal bearbeiten
    }
  }

  private Result processFree(VetoCacheEntry veto, Response response, VetoResponseEntry responseEntry) {
    switch( response ) {
    case IN_USE:
      return Result.Reprocess;
    case MISSING:
    case SUCCESS:
      //Veto wurde auf anderem Knoten zum Austragen aus DB vermerkt oder war dort nicht vorhanden
      return Result.Ok;
    default:
      return Result.Failed;
    }
  }

  
  @Override
  public VetoCacheEntry createNewVeto(String vetoName, long urgency) {
    return new VetoCacheEntry(vetoName, State.Compare, urgency);
  }

  
  private enum Result {
    Ok, Reprocess, Failed;
  }
  
  private void processRemoteResponse(long id, Pair<VetoRequestEntry, VetoResponseEntry> requestResponse) {
    
    //Bearbeitung der VetoResponses vom anderen Knoten als Antwort auf die eigenen Requests
    VetoCacheEntry veto = vetoCache.get(requestResponse.getSecond().getName());
    if( veto == null ) {
      return; //sollte eigentlich nicht auftreten, dass veto während der Remote-Kommunikation verschwindet
      //aber besser gegen NPE schützen. Ein Reprocess hilft hier auch nicht mehr, da das veto nicht mehr existiert
    }
    VetoRequestEntry requestEntry = requestResponse.getFirst();
    VetoResponseEntry responseEntry = requestResponse.getSecond();
    Result result = null;
    try {
      
      result = processRemoteResponse( veto, requestEntry, responseEntry );
      switch( result ) {
      case Failed:
        StringBuilder sb = new StringBuilder();
        if( responseEntry.getResponse() == Response.FAILED ) {
          sb.append("Failed in Veto communication ");
        } else {
          sb.append("Failed after Veto communication ");
        }
        sb.append(id).append(" ").append(requestEntry);
        sb.append(" -> ").append(responseEntry).append(" for ").append(veto);
        logger.warn(sb.toString());
        vetoCache.process(veto);
        break;
      case Ok:
        break;
      case Reprocess:
        vetoCache.process(veto);
        break;
      default:
        logger.warn("Unexpected Result "+result);
        break;
      }
      
    } finally {
      veto.getHistory().remoteActionEnd(ownBinding, String.valueOf(result));
    }
  }

  private Result processRemoteResponse(VetoCacheEntry veto, VetoRequestEntry requestEntry, VetoResponseEntry responseEntry) {
    Result result = null;
    Response response = responseEntry.getResponse();
    switch( requestEntry.getAction() ) {
    case Compare:
      result = processCompare( veto, response, responseEntry );
      break;
    case Free:
      result = processFree( veto, response, responseEntry );
      break;
    case Scheduled:
      result = processScheduled( veto, response, responseEntry );
      break;
    }
    if( result == null ) {
      logger.warn("Unexpected Result null");
      return Result.Reprocess;
    }
    return result;
  }

  public void close() {
    if( remoteImpl instanceof VCP_RemoteImpl ) {
      ((VCP_RemoteImpl)remoteImpl).closeRMI();
    }
  }

  @Override
  public String showBatch() {
    if( status == Status.Running ) {
      return super.showBatch()+"-"+remoteVetoRequests.getEntries().size();
    } else {
      return super.showBatch()+"-"+status;
    }
  }

  @Override
  protected void appendImplInformation(StringBuilder sb) {
    if( status == Status.Running ) {
      sb.append("clustered");
    } else {
      sb.append("clustered, status=").append(status);
    }
  }

  public void setReplicated(int binding, int size) {
    logger.info("Replicated "+size+" vetos for binding "+binding);
    replicationLatch.countDown();
  }

  public void setClusterProvider(RMIClusterProvider clusterProvider) {
    this.clusterProvider = clusterProvider;
  }

  public boolean compareAndSetState(VetoCacheEntry veto, State expect, State update) {
    return veto.compareAndSetState(expect, update);
  }

}
