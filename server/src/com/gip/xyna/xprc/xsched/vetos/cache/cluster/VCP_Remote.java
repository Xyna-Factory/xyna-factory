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
package com.gip.xyna.xprc.xsched.vetos.cache.cluster;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.cache.VCP_Clustered;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheEntry;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.ReplicateVetoRequest.ReplicateVetoRequestEntry;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.ReplicateVetoRequest.Replication;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoRequest.VetoRequestEntry;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoRequest.VetoRequestEntry.Action;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse.VetoResponseEntry;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse.VetoResponseEntry.Compare;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse.VetoResponseEntry.Response;

public class VCP_Remote implements VCP_RemoteInterface {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(VCP_Remote.class);
  private VetoCache vetoCache;
  private boolean notifyScheduler;
  private long ownBinding;
  private VCP_Clustered vcp_Clustered;
  
  public VCP_Remote(VCP_Clustered vcp_Clustered, VetoCache vetoCache, int ownBinding) {
    this.vcp_Clustered = vcp_Clustered;
    this.vetoCache = vetoCache;
    this.ownBinding = ownBinding;
  }

  public VetoResponse processRemoteVetoRequest(VetoRequest vetoRequest) {
    //Bearbeitung der VetoRequest vom anderen Knoten
    if( logger.isTraceEnabled() ) {
      logger.trace( "##"+ownBinding+" vetoRequest " +vetoRequest );
    }
    VetoResponse vr = new VetoResponse(vetoRequest.getId());
    for( VetoRequestEntry request : vetoRequest.getEntries() ) {
      vr.add( processRemoteVetoRequest(request, vetoRequest.getId(), vetoRequest.getBinding() ) );
    }
    vetoCache.notifyProcessor();
   
    if( notifyScheduler ) {
      vetoCache.getVetoCacheProcessor().notifyScheduler();
      notifyScheduler = false;
    }
    
    if( logger.isTraceEnabled() ) {
      logger.trace( "##"+ownBinding+" vetoResponse " +vr );
      logger.trace( "##"+ownBinding+" vetoCache "+vr.getId()+" " +vetoCache.showVetoCache(vr.getVetoNames()) );
    }
    return vr;
  }
  
  private VetoResponseEntry processRemoteVetoRequest(VetoRequestEntry request, long requestId, int binding) {
    String vetoName = request.getName();
    VetoCacheEntry veto = vetoCache.get(vetoName);
    if( veto == null && request.getAction() == Action.Compare ) {
      VetoResponseEntry response = processCompare(request);
      if( response != null) {
        return response; //Veto ist neu angelegt oder wird nicht gebraucht
      }
      veto = vetoCache.get(vetoName); //Veto ist eben durch anderen Thread angelegt worden, daher nun verwenden
    }
    if( veto == null ) {
      //Unerwartet: Veto existiert nicht, obwohl anderer Knoten dies erwartet (oder bei Compare oben nicht richtig angelegt...)
      return VetoResponseEntry.failMissing(vetoName);
    }
    
    veto.getHistory().remoteActionStart(request.getAction(),requestId, binding);
    VetoResponseEntry response = null;
    try {
      response = processRemoteVetoRequest(veto, request);
    } catch( RuntimeException e) {
      response = VetoResponseEntry.fail(vetoName, "exception: "+ e.getClass() + " " +e.getMessage()+" on " +request.getAction() );
      logger.warn("Failed to processRemoteVetoRequest "+request.getAction()+" "+veto, e);
    } finally {
      if( response == null ) {
        response = VetoResponseEntry.fail(vetoName, "response is null on action " +request.getAction() );
      }
      if( response.getResponse() == Response.FAILED) {
        logger.warn("Failed to process "+request.getAction()+" " +requestId+ " for veto "+vetoName+": "+veto);
      }
      veto.getHistory().remoteActionEnd(binding, String.valueOf(response));
    }
    return response;
  }
  
  private VetoResponseEntry processRemoteVetoRequest(VetoCacheEntry veto, VetoRequestEntry request) {
    switch( request.getAction() ) {
    case Compare:
      return processCompare(veto, request);
    case Scheduled:
      return processScheduled(veto, request);
    case Free:
      return processFree(veto, request);
    default:
      return VetoResponseEntry.fail(veto.getName(), "unexpected action "+request.getAction() );
    }
  }

  /**
   * Wie ist auf einen CompareRequest mit Urgency ru zu reagieren?
   * A) kein Veto -> Neubau,              Status "Remote", Antwort "Local"
   * B) Veto vorhanden -> processCompare(veto,request);
   * @param request
   * @return
   */
  private VetoResponseEntry processCompare(VetoRequestEntry request) {
    //lokal existiert kein Veto. Daher anlegen, falls der andere Knoten das Veto braucht.
    String vetoName = request.getName();
    VetoInformation vi = request.getVetoInformation();
    VetoCacheEntry vetoNew = null;
    Compare result = null;
    if( vi != null && vi.isAdministrative() ) {
      //administratives Veto
      vetoNew = new VetoCacheEntry(vetoName, State.Remote,false, vi);
      result = Compare.ADMIN;
    } else {
      //normales Veto
      if( request.getUrgency() == Long.MIN_VALUE) {
        return VetoResponseEntry.compare(vetoName, Compare.UNUSED);
      }
      vetoNew = new VetoCacheEntry(vetoName, State.Remote);
      result = Compare.REMOTE;
    }
    VetoCacheEntry veto = vetoCache.addVetoIfAbsent(vetoNew);
    if( veto == null ) {
      //Veto neu angelegt
      return VetoResponseEntry.compare(vetoName, result);
    } else {
      //nun existiert doch schon eines...
      return null;
    }
  }
  /**
   * Wie ist auf einen CompareRequest mit Urgency ru zu reagieren?
   * A) kein Veto -> processCompare(request)
   * B) Veto vorhanden: Eigene Urgency ist ou; Veto im Zustand
   *    1) New/Parked
   *       a) ou <  ru:                  Status "Remote" Antwort "REMOTE"
   *       b) ou <  ru:                  Status "Local" Antwort "LOCAL"
   *       c) ou == ru:                  je nach Binding und uo unterschiedlich: a), b) oder d) 
   *       d) ou == ru == Long.MinValue: Status "None", Antwort "UNUSED"
   *  2) Compare:                        Antwort "CONFLICT(uo,ru)"
   * 
   * @param veto
   * @param request
   * @return
   */
  private VetoResponseEntry processCompare(VetoCacheEntry veto, VetoRequestEntry request) {
    State currentState = veto.getState();
    if( currentState == State.Compare || currentState == State.Remote ) {
      Pair<Compare,State> result = compareUrgency(veto, request );
      logger.debug( "compareUrgency -> "+ result +" " +veto);
      if( vcp_Clustered.compareAndSetState(veto, currentState, result.getSecond() ) ) {
        switch( result.getFirst() ) {
        case ADMIN:
          veto.addAdministrativeData(request.getVetoInformation() );
          break;
        case LOCAL:
          notifyScheduler = true;
          break;
        case REMOTE:
          break;
        case UNUSED:
          vetoCache.remove(veto, State.None);
          break;
        default:
          break;
        }
        return VetoResponseEntry.compare(veto.getName(), result.getFirst() );
      } else {
        return VetoResponseEntry.inUse(veto.getName(), veto.getState() );
      }
    } else if( currentState == State.Comparing ) {
      //Konflikt: anderer Knoten führt gerade Vergleich durch
      return VetoResponseEntry.inUse(veto.getName(), veto.getState());
    } else if( currentState == State.Local ) {
      notifyScheduler = true;
      return VetoResponseEntry.inUse(veto.getName(), veto.getState());
    } else {
      return VetoResponseEntry.inUse(veto.getName(), veto.getState());
    }
  }

  private Pair<Compare,State> compareUrgency(VetoCacheEntry veto, VetoRequestEntry request) {
    VetoInformation vi = request.getVetoInformation();
    if( vi != null && vi.isAdministrative() ) {
      //administratives Veto
      return Pair.of(Compare.ADMIN, State.Remote);
    }
    vi = veto.getVetoInformation();
    if( vi != null && vi.isAdministrative() ) {
      //administratives Veto
      return Pair.of(Compare.LOCAL, State.Scheduled);
    }
    long requestedUrgency = request.getUrgency();
    long ownUrgency =veto.getUrgency();
    if( ownUrgency < requestedUrgency ) {
      return Pair.of(Compare.REMOTE, State.Remote);
    } else if( ownUrgency > requestedUrgency ) {
      return Pair.of(Compare.LOCAL, State.Local);
    } else { //ownUrgency == requestedUrgency
      if( ownUrgency == Long.MIN_VALUE ) {
        return Pair.of(Compare.UNUSED, State.None);
      }
      //bei Gleichheit dürfen nicht beide Knoten zum gleichen Ergebnis kommen, da sonst entweder beide das 
      //Veto verwenden oder keiner das Veto erhält.
      boolean useLocal = vcp_Clustered.isVetoPreferred(ownUrgency, veto);
      logger.info("Same urgency "+ownUrgency+" as on other node -> " + useLocal);
      return useLocal ? Pair.of(Compare.LOCAL, State.Local) : Pair.of(Compare.REMOTE, State.Remote);
    }
  }


  private VetoResponseEntry processFree(VetoCacheEntry veto, VetoRequestEntry request) {
    if( veto.remoteFree() ) {
      vetoCache.process(veto);
      return VetoResponseEntry.success(request.getName());
    } else if( request.getUrgency() == Long.MAX_VALUE ) {
      //eigenes free für AdminVetos ist zurückgekommen: siehe VCP_Clustered.processFree(VetoCacheEntry)
      return VetoResponseEntry.success(request.getName());
    } else if( veto.getState() == State.Remote ) {
      //Veto-Verwendung war auf anderem Knoten zu schnell, deswegen muss hier nichts freigeben werden
      return VetoResponseEntry.success(request.getName());
    } else {
      return VetoResponseEntry.failUnexpectedState(veto);
    }
  }
  
  private VetoResponseEntry processScheduled(VetoCacheEntry veto, VetoRequestEntry request) {
    switch( veto.getState() ) {
    case Remote:
      return setScheduled(State.Remote, veto, request, false);
    case Used:
      if( veto.isAdministrative() ) {
        //Ausnahme bei administrativen Vetos: Umsetzen der Documentation
        return setScheduled(State.Used, veto, request, false);
      } else {
        VetoInformation existing = veto.getVetoInformation();
        if( existing.getUsingOrderId().equals(request.getVetoInformation().getUsingOrderId() ) ) {
          //siehe VetoCacheEntry.allocate
          if( existing.getBinding() != request.getVetoInformation().getBinding() ) {
            return setScheduled(State.Used, veto, request, false);
          }
        }
        return VetoResponseEntry.failUnexpectedState(veto);
      }
    case Scheduled:
      if( veto.isReplicated() ) {
        //Direkt nach Übergang nach Clustered werden Scheduled-Vetos teilweise doppelt gemeldet, dies soll kein Fehler sein
        return VetoResponseEntry.success(request.getName());
      } else {
        return VetoResponseEntry.failUnexpectedState(veto);
      }
    case Comparing:
      //Dies ist zu erwarten, wenn dieser Knoten den anderen Knoten über ein Compare in Zustand Local gebracht hat,
      //dieser dann schnell nach Usable->Scheduling->Scheduled gegangen ist und hier lokal die Antwort (Comparing->Remote)
      //noch nicht umgesetzt wurde. Evtl ist dies gerade geschehen, deshalb nochmal probieren
      return setScheduled(State.Remote, veto, request, true);
    default:
      return VetoResponseEntry.failUnexpectedState(veto);
    }
  }
  
  private VetoResponseEntry setScheduled(State expected, VetoCacheEntry veto, VetoRequestEntry request, boolean inUse) {
    if( veto.setScheduled( expected, request.getVetoInformation() ) ) {
      vetoCache.process(veto);
      return VetoResponseEntry.success(request.getName());
    } else {
      if( inUse ) {
        return VetoResponseEntry.inUse(veto.getName());
      } else {
        return VetoResponseEntry.failUnexpectedState(veto);
      }
    }
  }

  @Override
  public void replicate(ReplicateVetoRequest replicateVetoRequest) {
   for( ReplicateVetoRequestEntry request : replicateVetoRequest.getEntries() ) {
      replicate(request);
    }
    vcp_Clustered.setReplicated(replicateVetoRequest.getBinding(), replicateVetoRequest.getEntries().size() );
   
    vetoCache.notifyProcessor();
   
    if( notifyScheduler ) {
      vetoCache.getVetoCacheProcessor().notifyScheduler();
      notifyScheduler = false;
    }
  }
  
  private void replicate(ReplicateVetoRequestEntry request) {
    String vetoName = request.getVetoName();
    VetoCacheEntry vetoNew = null;
    switch( request.getReplication() ) {
    case Info:
      //TODO mehr? 
      return;
    case Remote:
      vetoNew = new VetoCacheEntry(vetoName, State.Remote, true, null);
      break;
    case Scheduled:
      vetoNew = new VetoCacheEntry(vetoName, State.Scheduled, true, request.getVetoInformation());
      vetoNew.setScheduled(State.Scheduled, request.getVetoInformation());
      break;
    default:
      logger.warn("Failed replication for veto "+vetoName+": mode "+request.getReplication());
      return;
    }
    VetoCacheEntry veto = vetoCache.addVetoIfAbsent(vetoNew);
    if( veto == null ) {
      if( request.getReplication() == Replication.Scheduled ) {
        vetoCache.process(vetoNew);
      }
    } else {
      if( veto.replace(State.Compare, vetoNew) ) {
        //Veto konnte ersetzt werden. Sollte eigentlich klappen, wenn State Compare ist, da nur VetoCacheProcessor und RMI
        //den State Compare ändern darf. VCP sollte aber noch am CountDownLatch hängen.
      } else {
        logger.warn("Failed replication for veto "+vetoName+", "+request.getReplication()+": veto already exists: "+veto);
      }
    }
  }

}
