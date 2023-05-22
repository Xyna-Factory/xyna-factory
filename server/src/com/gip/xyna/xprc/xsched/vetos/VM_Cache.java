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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Filter;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;

public class VM_Cache implements VetoManagementInterface {

  
  private static final Logger logger = CentralFactoryLogging.getLogger(VM_Cache.class);

  private ConcurrentHashMap<String, VetoInformation> vetoCache;
  private ConcurrentHashMap<Long,List<String>> allocatedVetos;
  private int ownBinding;
  
  public VM_Cache(int ownBinding) {
    this.ownBinding = ownBinding;
    this.vetoCache = new ConcurrentHashMap<String, VetoInformation>();
    this.allocatedVetos = new ConcurrentHashMap<Long,List<String>>();
  }
  
  //package private
  void init(Collection<VetoInformation> existingVetos) {
    for( VetoInformation vi : existingVetos ) {
      vetoCache.put( vi.getName(), vi);
      if( vi.getUsingOrder() != null ) {
        long orderId = vi.getUsingOrder().getOrderId();
        List<String> allocated = allocatedVetos.get(orderId);
        if( allocated == null ) {
          allocated = new ArrayList<String>();
          allocatedVetos.put(orderId, allocated);
        }
        allocated.add(vi.getName());
      }
    }
  }

  

  public VetoAllocationResult allocateVetos(OrderInformation usingOrder, List<String> vetos, long urgency) {
    boolean reallocate = false;
    //erste Pr�fung, die h�ufig fehlschl�gt
    for( String v : vetos ) {
      VetoInformation existing = vetoCache.get(v);
      if( existing != null ) {
        if( existing.getUsingOrderId() != null && !existing.getUsingOrderId().equals(usingOrder.getOrderId()) ) {
          return new VetoAllocationResult(existing);
        } else {
          reallocate = true;
        }
      }
    }
    
    //jetzt ist es unwahrscheinlich, dass administratives Veto zeitgleich gesetzt wird
    List<String> allocated = new ArrayList<String>(vetos.size());
    for( String v : vetos ) {
      VetoInformation vi = new VetoInformation(v,usingOrder, ownBinding);
      VetoInformation existing = vetoCache.putIfAbsent(v, vi );
      if( existing != null ) {
        if( existing.getUsingOrderId() != null && !existing.getUsingOrderId().equals(usingOrder.getOrderId()) ) {
          //soeben allokierte wieder freigeben
          freeVetosByOrderId( usingOrder.getOrderId() );
          return new VetoAllocationResult(existing);
        } else {
          //reallocate ignorieren
        }
      }
      allocated.add(v);
    }
    
    if( reallocate ) {
      logger.info("veto reallocation for "+usingOrder.getOrderId());
      List<String> prevAllocVetos = allocatedVetos.get(usingOrder.getOrderId());
      if( ! prevAllocVetos.equals(vetos) ) {
        //TODO was nun? einfach zusammenfassen? teilweise deallokieren?
        logger.warn("veto reallocation for "+usingOrder.getOrderId() +" changed vetos!");
      }
    } else {
      allocatedVetos.put(usingOrder.getOrderId(), allocated);
    }
    //alle allokiert
    return VetoAllocationResult.SUCCESS;
  }
  
  public void undoAllocation(OrderInformation orderInformation, List<String> vetos) {
    freeVetosByOrderId(orderInformation.getOrderId());
  }
  
  public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos) {
    //nichts zu tun
  }

  private boolean freeVetosByOrderId(long orderId) {
    List<String> allocated = allocatedVetos.remove(orderId);
    if( allocated == null ) {
      return false;
    }
    for( String v : allocated ) {
      VetoInformation vi = vetoCache.get(v);
      if( vi != null ) {
        if( vi.getUsingOrderId() == orderId ) {
          if( ! vetoCache.remove(v, vi) ) {
            //TODO wer sollte den Eintrag �ndern?
          }
        }
      } else {
        //TODO evtl. war Veto doppelt angefordert; Ansonsten: wer sollte das Veto bereits entfernt haben
      }
    }
    return ! allocated.isEmpty();
  }

  public void allocate(VetoAllocationResult var) {
    String veto = var.getVetoName();
    if( veto != null ) {
      VetoInformation vi = var.getExistingVeto();
      VetoInformation existing = vetoCache.putIfAbsent(veto, vi );
      if( existing != null ) {
        //TODO unerwartet
      }
    }
  }
  
  public boolean freeVetos(OrderInformation orderInformation) {
    return freeVetosByOrderId(orderInformation.getOrderId());
  }
  
  public boolean freeVetosForced(long orderId) {
    return freeVetosByOrderId(orderId);
  }

  public void allocateAdministrativeVeto(AdministrativeVeto administrativeVeto) throws XPRC_AdministrativeVetoAllocationDenied {
    VetoInformation vi = new VetoInformation(administrativeVeto, ownBinding);
    VetoInformation existing = vetoCache.putIfAbsent(vi.getName(), vi);
    if( existing != null ) {
      throw new XPRC_AdministrativeVetoAllocationDenied(existing.getName(), existing.getUsingOrderId());
    }
  }

  public String setDocumentationOfAdministrativeVeto(AdministrativeVeto administrativeVeto) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    VetoInformation existing = vetoCache.get(administrativeVeto.getName());
    if( existing != null ) {
      String oldDoc = existing.getDocumentation();
      existing.setDocumentation(administrativeVeto.getDocumentation());
      return oldDoc;
    } else {
      throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(administrativeVeto.getName(), VetoInformationStorable.TABLE_NAME);
    }
  }

  public VetoInformation freeAdministrativeVeto(AdministrativeVeto administrativeVeto) throws XPRC_AdministrativeVetoDeallocationDenied {
    VetoInformation veto = vetoCache.get(administrativeVeto.getName());
    if( veto == null || ! veto.isAdministrative() ) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    if( vetoCache.remove(administrativeVeto.getName(), veto) ) {
      return veto;
    }
    throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
  }

  public Collection<VetoInformation> listVetos() {
    return Collections.unmodifiableCollection( vetoCache.values() );
  }

  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    List<VetoInformation> filtered = CollectionUtils.filter(listVetos(), new VetoFilter(select) );
    int countAll = filtered.size();
    if( maxRows>=0 ) {
      filtered = filtered.subList(0, Math.min(countAll, maxRows) );
    }
    List<VetoInformationStorable> viss = CollectionUtils.transform(filtered, VetoInformationStorable.fromVetoInformation );
    return new VetoSearchResult(viss, countAll);
  }

  public static class VetoFilter implements Filter<VetoInformation> {

    private enum AcceptAlgorithm {
      
      All {
        public boolean accept(VetoInformation value) {
          return true;
        }
      }, 
      AdminVeto {
        public boolean accept(VetoInformation value) {
          return value.isAdministrative();
        }
      },
      
      ;

      public abstract boolean accept(VetoInformation value);
      
    }
    
    private AcceptAlgorithm acceptAlgorithm;
    
    public VetoFilter(VetoSelectImpl select) {
      acceptAlgorithm = AcceptAlgorithm.All;
      try {
        String selectString = select.getSelectString();
        int idx = selectString.indexOf("from vetos" );
        String where = selectString.substring(idx+ 10 );
        if (where.startsWith(" where usingOrdertype = ?") && select.getParameter().get(0).equals(AdministrativeVeto.ADMIN_VETO_ORDERTYPE)) {
          acceptAlgorithm = AcceptAlgorithm.AdminVeto; //TODO ignoriert weitere whereclauses
        } else if (where.equals("")) {
          acceptAlgorithm = AcceptAlgorithm.All;
        } else {
          //TODO Suche funktioniert nicht
          logger.info("#+#+# select Vetos " + selectString );
          logger.info("#+#+# select Vetos " + where );
          logger.info("#+#+# select Vetos " + select.getParameter() );
        }
      } catch( Exception e) {
        logger.info( "VetoFilter failed ", e );
      }
    }

    public boolean accept(VetoInformation value) {
      
      return acceptAlgorithm.accept(value);
    }
    
  }
  
  
  public int size() {
    return vetoCache.size();
  }
  
  @Override
  public VetoManagementAlgorithmType getAlgorithmType() {
    return VetoManagementAlgorithmType.Cache;
  }
  
  @Override
  public String showInformation() {
    return getAlgorithmType() + ": "+getAlgorithmType().getDocumentation().get(DocumentationLanguage.EN)+" Cache size "+vetoCache.size();
  }

}
