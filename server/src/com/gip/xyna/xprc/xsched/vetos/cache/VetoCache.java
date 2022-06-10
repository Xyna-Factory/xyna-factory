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
package com.gip.xyna.xprc.xsched.vetos.cache;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xprc.xsched.LazyAlgorithmExecutor;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.vetos.AdministrativeVeto;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;


public class VetoCache {
  
  static final Logger logger = CentralFactoryLogging.getLogger(VetoCache.class);

  private ConcurrentHashMap<String, VetoCacheEntry> vetoCache;
  private ConcurrentLinkedQueue<String> vetosToProcess;
  private LazyAlgorithmExecutor<VetoCacheProcessor> vetoCacheProcessorExecutor;
  private long currentSchedulingRun;
  private VetoCacheProcessor vetoCacheProcessor;
  private int ownBinding;
  
  private static final Transformation<VetoCacheEntry,VetoInformation> extractVetoInformation = new Transformation<VetoCacheEntry,VetoInformation>() {
    public VetoInformation transform(VetoCacheEntry from) {
      switch( from.getState() ) {
      case Scheduled:
      case Used:
        return from.getVetoInformation();
      default:
        return null;
      }
    }
  };
  
  private static class ExtractVetoInformationUsedByOrderId implements Transformation<VetoCacheEntry,VetoInformation> {
    private long orderId;

    public ExtractVetoInformationUsedByOrderId(long orderId) {
      this.orderId = orderId;
    }
    
    public VetoInformation transform(VetoCacheEntry from) {
      VetoInformation vi = from.getVetoInformation();
      if (vi == null) {
        //kann von anderem thread gerade freigegeben worden sein
        return null;
      }
      if( vi.getUsingOrderId() == orderId ) {
        return vi;
      }
      return null;
    }
  }
  
  public enum State {
    None,       //existiert nicht im Cache
    Compare,    //nächster State muss untersucht werden 
    Comparing,  //State wird im Cluster zwischen beiden Knoten verhandelt
    Local,      //lokal verwendbar
    Remote,     //wird von anderem Knoten verwendet, lokal nicht verwendbar
    Usable,     //ist im aktuellen Schedulerlauf verwendbar
    Scheduling, //wird gerade zum Schedulen verwendet
    Scheduled,  //ist zum Schedulen verwendet worden, Auftrag läuft
    Used,       //ist nun in DB eingetragen
    Free,       //Auftrag benötigt Veto nicht mehr
    
    ;
   
  }

  public VetoCache() {
    vetoCache = new ConcurrentHashMap<String, VetoCacheEntry>();
    vetosToProcess = new ConcurrentLinkedQueue<String>();
  }
  
  public void init(int ownBinding) {
    vetoCacheProcessorExecutor = new LazyAlgorithmExecutor<VetoCacheProcessor>("VetoCacheProcessor-"+ownBinding+"-");
    this.ownBinding = ownBinding;
  }
  
  public void setVetoCacheProcessor(VetoCacheProcessor vetoCacheProcessor, boolean start) {
    this.vetoCacheProcessor = vetoCacheProcessor;
    if( start ) {
      if( vetoCacheProcessorExecutor.getCurrentAlgorithm() == null ) {
        logger.info( "vetoCacheProcessor started" );
        vetoCacheProcessorExecutor.startNewThread(vetoCacheProcessor);
      } else {
        vetoCacheProcessorExecutor.changeAlgorithm(vetoCacheProcessor);
      }
    }
  }
  
  public void stopVetoCacheProcessor() { //Zum Testen!
    vetoCacheProcessorExecutor.stopThread();
  }
  
  public boolean isStarted() {
    return vetoCacheProcessorExecutor.isRunning();
  }
  
  public String toString() {
    return vetoCache.toString();
  }
  
  public void notifyProcessor() {
    vetoCacheProcessorExecutor.requestExecution();
  }
  
  public void awaitProcessor(String caller) {
    try {
      vetoCacheProcessorExecutor.awaitExecution();
    } catch (InterruptedException e) {
      throw new RuntimeException(caller + " was interrupted. Check again for expected result", e);
    }
  }
  
  public VetoCacheEntry getOrCreate(String vetoName, long urgency) {
    VetoCacheEntry veto = vetoCache.get(vetoName);
    if( veto == null ) {
      VetoCacheEntry vceNew = vetoCacheProcessor.createNewVeto(vetoName, urgency);
      veto = vetoCache.putIfAbsent(vetoName, vceNew);
      if( veto == null ) {
        veto = vceNew;
        if( veto.getState() == State.Compare ) {
          process(veto);
        }
      }
      return veto;
    } else {
      return veto;
    }
  }
  
  public VetoCacheEntry get(String vetoName) {
    return vetoCache.get(vetoName);
  }
  
  public Collection<VetoInformation> listVetos() {
    //alle einsammeln? kurzlebige nicht, damit JUnit-Test erfolgreich bleibt
    
    return CollectionUtils.transformAndSkipNull( vetoCache.values(), extractVetoInformation );
  }
  
  public List<VetoInformation> listVetosUsedByOrderId(long orderId) {
    return CollectionUtils.transformAndSkipNull( vetoCache.values(), new ExtractVetoInformationUsedByOrderId(orderId) );
  }
  
  public VetoAllocationResult checkAllocation(VetoCacheEntry veto, OrderInformation orderInformation, long urgency) {
    //vom Scheduler-Thread aufgerufen
    if( veto.checkAllocation(orderInformation,urgency) ) {
      return null;
    } else {
      veto.updateWaiting(urgency, currentSchedulingRun);
      VetoInformation vi = veto.getVetoInformation();
      if( vi != null ) {
        return new VetoAllocationResult(vi);
      } else {
        return new VetoAllocationResult( new VetoInformation(veto.getName()) );
      }
    }
  }

  public VetoAllocationResult checkAllocation() {
    if( ! vetoCacheProcessor.canAllocate() ) {
      return VetoAllocationResult.UNSUPPORTED;
    }
    return null; 
  }
  
  public void allocate(VetoCacheEntry veto, OrderInformation orderInformation, long urgency) {
    VetoInformation vi = new VetoInformation(veto.getName(), orderInformation, ownBinding);
    veto.allocate(vi, urgency);
  }
  
  //package private
  boolean createUsedVeto(VetoInformation vetoInformation) {
    VetoCacheEntry vceNew = new VetoCacheEntry(vetoInformation.getName(), State.Used);
    vceNew.setVetoInformation( vetoInformation );
    VetoCacheEntry vce = vetoCache.putIfAbsent(vceNew.getName(), vceNew);
    return vce == null;
  }
  boolean createRemoteVeto(String vetoName) {
    VetoCacheEntry vceNew = new VetoCacheEntry(vetoName, State.Remote);
    VetoCacheEntry vce = vetoCache.putIfAbsent(vceNew.getName(), vceNew);
    return vce == null;
  }
  
  public VetoCacheEntry addVetoIfAbsent(VetoCacheEntry veto) {
    return vetoCache.putIfAbsent(veto.getName(), veto);
  }
  
  public boolean createAdministrativeVeto(AdministrativeVeto administrativeVeto) {
    VetoCacheEntry vceNew = new VetoCacheEntry(administrativeVeto.getName(), State.Compare);
    vceNew.setVetoInformation( new VetoInformation(administrativeVeto, ownBinding) );
    VetoCacheEntry vce = vetoCache.putIfAbsent(administrativeVeto.getName(), vceNew);
    if( vce == null ) {
      vetosToProcess.add(administrativeVeto.getName());
    }
    return vce == null;
  }
  
  public String changeAdminVeto(VetoCacheEntry veto, AdministrativeVeto administrativeVeto) {
    VetoInformation oldVetoInfo = veto.getVetoInformation();
    VetoInformation newVetoInfo = new VetoInformation(administrativeVeto, ownBinding);
    veto.setVetoInformation(newVetoInfo);
    if( veto.isAdministrative() ) {
      //Umsetzen des Status, damit Änderung vom VetoCacheProcessor bearbeitet wird
      veto.compareAndSetState(State.Used, State.Scheduled);
      vetosToProcess.add(veto.getName());
    }
    return oldVetoInfo.getDocumentation();
  }
  
  public boolean freeAdminVeto(VetoCacheEntry veto) {
    if( veto.isAdministrative() ) {
      if( veto.free() ) {
        vetosToProcess.add(veto.getName());
        return true;
      }
    }
    return false;
  }
  
  public void waitWhileVetoHasState(String caller, VetoCacheEntry veto, State state) {
    do {
      awaitProcessor(caller);
    } while( veto.getState() == state );
  }
 
 public boolean free(VetoCacheEntry veto, long orderId) {
   if( veto == null ) {
     return false;
   }
   if( veto.isUsedBy(orderId) ) {
     if( veto.free() ) {
       vetosToProcess.add(veto.getName());
       return true;
     }
   }
   return false;
  }

  public boolean hasVetosToProcess() {
    return ! vetosToProcess.isEmpty();
  }
  
  public String getVetoToProcess() {
    return vetosToProcess.poll();
  }

  public boolean remove(VetoCacheEntry veto, State currentState) {
    if( veto.compareAndSetState(currentState, State.None ) ) {
      vetoCache.remove(veto.getName() );
      return true;
    }
    return false;
  }

  //package private
  VetoCacheEntry putIfAbsent(VetoCacheEntry veto) {
    return vetoCache.putIfAbsent(veto.getName(), veto);
  }

  public void process(VetoCacheEntry veto) {
    vetosToProcess.add(veto.getName());
  }
  
  public void process(String vetoName) {
    vetosToProcess.add(vetoName);
  }

  public int size() {
    return vetoCache.size();
  }

  public void finalizeAllocation(String vetoName) {
    VetoCacheEntry veto = get(vetoName);
    if( veto != null ) {
      veto.compareAndSetState(State.Scheduling, State.Scheduled);
      vetosToProcess.add(vetoName);
    } else {
      //Wahrscheinlich ist Auftrag zu schnell gewesen und hat Veto 
      //vor dem finalizeAllocation bereits wieder freigegeben
    }
  }

  public void beginScheduling(long currentSchedulingRun) {
    //wird im Scheduler-Thread aufgerufen
    this.currentSchedulingRun = currentSchedulingRun;
    //alle Vetos im State Local nach Usable umsetzen
    //TODO Liste kleiner halten durch Auslagerung der selten veränderten Vetos im State Used?
    //also zwei Maps allVetos und activeVetos?
    for( VetoCacheEntry veto : vetoCache.values() ) {
      veto.setLocalToUsable();
    }
  }

  public void endScheduling() {
    //wird im Scheduler-Thread aufgerufen
    //alle übriggebliebenen Usable auf Unused setzen

    //wahrscheinlich ist dies schneller als erst nach State zu filtern...
    //TODO Liste kleiner halten durch Auslagerung der selten veränderten Vetos im State Used?
    //also zwei Maps allVetos und activeVetos?
    for( VetoCacheEntry veto : vetoCache.values() ) {
      boolean changed = veto.setUsableToCompare();
      if( changed ) {
        process(veto);
      }
    }
    notifyProcessor();
  }

  public boolean compareAndSetState(VetoCacheEntry veto, State expect, State update, boolean addToProcess ) {
    if( veto.compareAndSetState(expect, update) ) {
      if( addToProcess ) {
        process(veto);
      }
      return true;
    } else {
      return false;
    }
  }

  public int getOwnBinding() {
    return ownBinding;
  }

  public String showVetoCache() {
    return vetoCache.toString();
  }
  
  public String showVetoCache(List<String> vetoNames) {
    StringBuilder sb = new StringBuilder();
    String sep = "[";
    for( String vetoName : vetoNames ) {
      sb.append(sep).append( vetoCache.get(vetoName));
      sep = ", ";
    }
    sb.append("]");
    return sb.toString();
  }

  public String showVetoQueue() {
    return vetosToProcess.toString();
  }
  
  public VetoCacheProcessor getVetoCacheProcessor() {
    return vetoCacheProcessor;
  }

  public void reprocessAll() {
    vetosToProcess.addAll( vetoCache.keySet() );
  }

  public List<List<String>> exportVetoCacheData() {
    List<List<String>> export = new ArrayList<>();
    for( VetoCacheEntry veto : vetoCache.values() ) {
      export.add(exportVetoData(veto) );
    }
    return export;
  }
  
  public List<List<String>> exportVetoCacheData(long orderId) {
    List<List<String>> export = new ArrayList<>();
    for( VetoCacheEntry veto : vetoCache.values() ) {
      VetoInformation vi = veto.getVetoInformation();
      OrderInformation oi = vi != null ? vi.getUsingOrder() : null;
      if( oi != null && oi.getOrderId() == orderId) {
        export.add(exportVetoData(veto) );
      }
    }
    return export;
  }
  
  private List<String> exportVetoData(VetoCacheEntry veto) {
    List<String> vetoData = new ArrayList<>();
    vetoData.add( veto.getName() );
    vetoData.add( veto.getState().toString() );
    VetoInformation vi = veto.getVetoInformation();
    OrderInformation oi = vi != null ? vi.getUsingOrder() : null;
    if( oi != null ) {
      vetoData.add( String.valueOf(oi.getOrderId()) );
      vetoData.add( String.valueOf( vi.getBinding() ) );
      vetoData.add( oi.getOrderType() );
    } else {
      vetoData.add( "");
      vetoData.add( "");
      vetoData.add( "");
    }
    long urgency = veto.getUrgency();
    vetoData.add( urgency == Long.MIN_VALUE ? "" : String.valueOf(urgency) );
    vetoData.add( veto.hasWaiting() ? "Wait" : "" );
    vetoData.add( veto.getVetoHistory() );
    return vetoData;
  }

 
  
  
  public String showInformation() {
    StringBuilder sb = new StringBuilder();
    sb.append(vetoCache.size()).append(" entries in cache, ");
    sb.append(vetosToProcess.size()).append(" vetos to process, ");
    sb.append(vetoCacheProcessor.showInformation());
    if( ! vetoCacheProcessorExecutor.isRunning() ) {
      long timestamp = vetoCacheProcessorExecutor.getThreadDeathTimestamp();
      Throwable cause = vetoCacheProcessorExecutor.getThreadDeathCause();
      if( timestamp != 0 || cause != null ) {
        sb.append(", vetoCacheProcessorExecutor died");
      } else {
        sb.append(", vetoCacheProcessorExecutor is not started");
      }
      if( timestamp != 0 ) {
        sb.append(" at ").append( Constants.defaultUTCSimpleDateFormat().format(new Date(timestamp) ) );
      }
      if (cause != null) {
        sb.append(" due to ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage())
            .append("\n");
        StringWriter sw = new StringWriter();
        cause.printStackTrace( new PrintWriter(sw) );
        sb.append(sw.toString());
      }
    }
    return sb.toString();
    
  }

  public Collection<VetoCacheEntry> entries() {
    return vetoCache.values();
  }

  public void removePassiveVeto(VetoCacheEntry veto) {
    if( veto.compareAndSetState(State.Remote, State.None) ) {
      remove(veto, State.None);
    }
  }
}
