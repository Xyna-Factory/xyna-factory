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
package com.gip.xyna.xprc.xsched.vetos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.vetos.VM_Cache.VetoFilter;
import com.gip.xyna.xprc.xsched.vetos.cache.VCP_Abstract;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheEntry;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCachePersistence;

public class VM_SeparateThread implements VetoManagementInterface {

  
  private static final Logger logger = CentralFactoryLogging.getLogger(VM_SeparateThread.class);
  private VetoCache vetoCache;
  
  private ConcurrentHashMap<Long,List<String>> allocatedVetos;

  
  public VM_SeparateThread(VetoCache vetoCache) {
    this.vetoCache = vetoCache;
    this.allocatedVetos = new ConcurrentHashMap<Long,List<String>>();
  }

  public VetoCache getVetoCache() {
    return vetoCache;
  }
  
  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> vetoNames, long urgency) {
    VetoAllocationResult var = vetoCache.checkAllocation();
    if( var != null ) {
      return var;
    }
    
    if( logger.isTraceEnabled() ) {
      logger.trace("VetoCache before alloc " + vetoCache.showVetoCache() );
    }
    
    //1) Scheitert Allocation an bereits vergebenen Vetos?
    List<VetoCacheEntry> vces = new ArrayList<VetoCacheEntry>(vetoNames.size());
    for( String vetoName : vetoNames ) {
      VetoCacheEntry veto = vetoCache.get(vetoName);
      vces.add(veto);
      if( veto != null ) {
        //TODO bei allen Vetos als wartend eintragen? oder nur beim ersten? 
        //Eintragen als wartend ist n�tig, damit nicht niedrig-priorisierter Auftrag Veto erh�lt
        
        //Zur geforderten Fairnis ist es wahrscheinlich ausreichend, dies beim ersten Veto zu pr�fen
        var = vetoCache.checkAllocation(veto, orderInformation, urgency);
        if( var != null ) {
          return var;
        }
      }
    }
    
    //2) Vetos neu anlegen und gleich pr�fen
    for( int i=0; i<vetoNames.size(); ++i ) {
      if( vces.get(i) == null ) {
        VetoCacheEntry veto = vetoCache.getOrCreate(vetoNames.get(i), urgency);
        vces.set(i, veto );
        VetoAllocationResult var2 = vetoCache.checkAllocation(veto, orderInformation, urgency);
        if( var2 != null ) {
          var = var2;
        }
      }
    }
    
    if( var != null ) {
      //Beim Anlegen der Vetos in Schritt 2) wurde festgestellt, dass nicht geschedult werden kann
      //a) Im Cluster wurden Vetos im Zustand "New" angelegt, diese m�ssen vom VetoCacheProcessor abgekl�rt werden
      vetoCache.notifyProcessor();
      //b) in der Zeit von 1) bis 2) wurde konkurrierend ein Veto angelegt, entweder AdminVeto oder im Cluster
      return var;
    }
    
    //3) eigentliche Allozierung, da nun alle Vetos verwendbar sind
    List<String> allocated = new ArrayList<String>();
    for( VetoCacheEntry veto : vces ) {
      vetoCache.allocate(veto, orderInformation, urgency);
      allocated.add( veto.getName() );
    }
    List<String> list = allocatedVetos.get(orderInformation.getOrderId());
    if( list == null ) {
      allocatedVetos.put(orderInformation.getOrderId() , allocated );
    } else {
     //TODO was nun? einfach zusammenfassen? teilweise deallokieren?
      logger.warn("veto reallocation for "+orderInformation.getOrderId() +" changed vetos!");
    }
    
    if( logger.isTraceEnabled() ) {
      logger.trace(" Allocated Vetos + "+ allocated + " for " + orderInformation );
      logger.trace("VetoCache after alloc " + vetoCache.showVetoCache() );
    }
    return VetoAllocationResult.SUCCESS;
  }

   public void undoAllocation(OrderInformation orderInformation, List<String> vetos) {
     logger.trace(" UndoAllocation ");
     List<String> allocated = allocatedVetos.remove(orderInformation.getOrderId());
     if( allocated != null ) {
       for( String v : allocated ) {
         vetoCache.get(v).undoAllocation(orderInformation);
       }
     }
     if( logger.isTraceEnabled() ) {
       logger.trace("VetoCache after undoAlloc " + vetoCache.showVetoCache() );
     }
   }

   public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos) {
     for( String v : vetos ) {
       vetoCache.finalizeAllocation(v);
     }
     vetoCache.notifyProcessor();
     if( logger.isTraceEnabled() ) {
       logger.trace("VetoCache after finalizeAllocation " + vetoCache.showVetoCache() );
     }
   }
  

  public boolean freeVetos(OrderInformation orderInformation) {
    if( logger.isDebugEnabled() ) {
      logger.debug("freeVetos "+ orderInformation );
    }
    long orderId = orderInformation.getOrderId();
    List<String> vetos = allocatedVetos.remove(orderId);
    if( vetos != null ) {
      return freeVetos(vetos, orderId);
    }
    return false;
  }
  
  public boolean freeVetosForced(long orderId) {
    if( logger.isDebugEnabled() ) {
      logger.debug("freeVetosForced "+ orderId );
    }
    List<String> vetos = allocatedVetos.remove(orderId);
    //if( vetos != null ) { 
    //  return freeVetos(vetos, orderId);
    //}
    //freeVetosForced wird nur selten (bei killprocess) aufgerufen, 
    //deswegen teure Suche immer durchf�hren.
    List<VetoInformation> vis = vetoCache.listVetosUsedByOrderId(orderId);
    vetos = CollectionUtils.transform(vis, VetoInformation.extractName );
    return freeVetos(vetos, orderId);
  }
  
  private boolean freeVetos(List<String> vetos, long orderId) {
    boolean freed = false;
    for( String v : vetos ) {
      VetoCacheEntry veto = vetoCache.get(v);
      if( veto == null ) {
        logger.warn("Failed to get Veto "+v+" from cache");
      } else {
        boolean f = vetoCache.free(veto, orderId);
        freed = freed || f;
      }
    }
    if( freed ) {
      vetoCache.notifyProcessor();
    }
    return freed;
  }

  public void allocateAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    if( ! vetoCache.createAdministrativeVeto(administrativeVeto) ) {
      VetoCacheEntry veto = vetoCache.get(administrativeVeto.getName());
      VetoInformation vi = veto.getVetoInformation();
      Long holdingOrderId =  vi == null ? null : vi.getUsingOrderId();
      throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), holdingOrderId);
    }
    VetoCacheEntry veto = vetoCache.get(administrativeVeto.getName());
    vetoCache.waitWhileVetoHasState("allocateAdministrativeVeto", veto, State.Compare);
    if( ! veto.isAdministrative() ) {
      //Veto existiert nun, ist aber nicht das erwartet administrative Veto
      Long usingOrderId = -2L; //unknown
      VetoInformation vi = veto.getVetoInformation();
      if( vi != null ) {
        usingOrderId = vi.getUsingOrderId();
      }
      throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), usingOrderId );
    }
  }

  public String setDocumentationOfAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    VetoCacheEntry veto = vetoCache.get(administrativeVeto.getName());
    if( veto == null || ! veto.isAdministrative() ) {
      throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(administrativeVeto.getName(), VetoInformationStorable.TABLE_NAME);
    }
    String oldDoc = vetoCache.changeAdminVeto(veto, administrativeVeto);
    vetoCache.waitWhileVetoHasState("setDocumentationOfAdministrativeVeto", veto, State.Compare);
    return oldDoc;
  }

  public VetoInformation freeAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    VetoCacheEntry veto = vetoCache.get(administrativeVeto.getName());
    if( veto == null || ! veto.isAdministrative() ) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    VetoInformation oldVetoInfo = veto.getVetoInformation();
    vetoCache.freeAdminVeto(veto);
    vetoCache.waitWhileVetoHasState("freeAdministrativeVeto", veto, State.Free);
    return oldVetoInfo;
  }

  public Collection<VetoInformation> listVetos() {
    return vetoCache.listVetos();
  }
  

  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    VCP_Abstract vcp = (VCP_Abstract) vetoCache.getVetoCacheProcessor();
    VetoCachePersistence persistence = vcp.getPersistence();
    if (persistence != null) {
      return persistence.searchVetos(select, maxRows);
    } else {
      List<VetoInformation> filtered = CollectionUtils.filter(listVetos(), new VetoFilter(select));
      int countAll = filtered.size();
      if (maxRows >= 0) {
        filtered = filtered.subList(0, Math.min(countAll, maxRows));
      }
      List<VetoInformationStorable> viss = CollectionUtils.transform(filtered, VetoInformationStorable.fromVetoInformation);
      return new VetoSearchResult(viss, countAll);
    }
  }
  
  @Override
  public VetoManagementAlgorithmType getAlgorithmType() {
    return VetoManagementAlgorithmType.SeparateThread;
  }
  
  @Override
  public String showInformation() {
    return getAlgorithmType() + ": "+getAlgorithmType().getDocumentation().get(DocumentationLanguage.EN)+
        ":\n" + allocatedVetos.size() +" allocated vetos, " +
        vetoCache.showInformation();
  }

}
