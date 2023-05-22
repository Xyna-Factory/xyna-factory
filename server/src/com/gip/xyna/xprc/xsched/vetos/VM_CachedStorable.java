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

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;

/**
 * Unterscheidung, ob Methoden synchron zum Scheduler gerufen werden oder nicht.
 * synchron darf nur vom Scheduler gerufen werden:
 *   - allocateVetos
 *   - undoAllocation
 * asynchrone Methoden setzen Cache fr�h und leeren Cache sp�t, damit Veto immer im Cache gefunden wird.
 *
 */
public class VM_CachedStorable implements VetoManagementInterface {
  
  
  private static final Logger logger = CentralFactoryLogging.getLogger(VM_CachedStorable.class);

  private VetoStorableAccess vetoStorableAccess;
  private VM_Cache vetoCache;

  
  public VM_CachedStorable(VetoStorableAccess vetoStorableAccess, VM_Cache vetoCache) {
    this.vetoStorableAccess = vetoStorableAccess;
    this.vetoCache = vetoCache;
  }

  public void init() {
    vetoCache.init(vetoStorableAccess.listVetos());
  }


  //synchron zum Scheduler
  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> vetos, long urgency) {
    VetoAllocationResult var = vetoCache.allocateVetos(orderInformation, vetos, urgency);
    if( ! var.isAllocated() ) {
      return var; //konnte nicht allokiert werden
    }
    
    VetoAllocationResult var2 = vetoStorableAccess.allocateVetos(orderInformation, vetos, urgency);
    if( ! var2.isAllocated() ) {
      if( var2.getVetoName() != null ) {
        vetoCache.allocate(var2); //nachtr�glich!
      }
      vetoCache.undoAllocation(orderInformation, vetos);
    }
    return var2;
  }
  
  //synchron zum Scheduler
  public void undoAllocation(OrderInformation orderInformation, List<String> vetos) {
    if (vetos.isEmpty() ) {
      return;
    }
    vetoCache.undoAllocation(orderInformation, vetos);
    vetoStorableAccess.freeVetos(orderInformation);
  }
  
  public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos) {
    vetoCache.finalizeAllocation(orderInformation, vetos);
    vetoStorableAccess.finalizeAllocation(orderInformation, vetos);
  }

  
  
  public boolean freeVetos(OrderInformation orderInformation) {
    long orderId = orderInformation.getOrderId();
    try {
      vetoStorableAccess.deleteVetosByOrderId(orderId);
    } catch (PersistenceLayerException e) {
      logger.warn("Error while trying to deallocate vetos.", e);
    } 
    return vetoCache.freeVetos(orderInformation);
  }
  
  public boolean freeVetosForced(long orderId) {
    try {
      vetoStorableAccess.deleteVetosByOrderId(orderId);
    } catch (PersistenceLayerException e) {
      logger.warn("Error while trying to force deallocation of vetos.", e);
    } 
    return vetoCache.freeVetosForced(orderId);
  }
 
  public void allocateAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    vetoCache.allocateAdministrativeVeto(administrativeVeto);
    boolean success = false;
    try {
      vetoStorableAccess.allocateAdministrativeVeto(administrativeVeto);
      success = true;
    } finally {
      if( ! success ) {
        try {
          vetoCache.freeAdministrativeVeto(administrativeVeto);
        } catch( XPRC_AdministrativeVetoDeallocationDenied e ) {
          //sollte nicht auftreten, da ja gerade zuvor eingetragen
          //vetoStorableAccess.allocateAdministrativeVeto sollte Exception werfen
        } 
      }
    }
  }

  public String setDocumentationOfAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    String oldDok = vetoCache.setDocumentationOfAdministrativeVeto(administrativeVeto);
    boolean success = false;
    try {
      vetoStorableAccess.setDocumentationOfAdministrativeVeto(administrativeVeto);
      success = true;
    } finally {
      if( ! success ) {
        try {
          AdministrativeVeto av = new AdministrativeVeto(administrativeVeto.getName(), oldDok);
          vetoCache.setDocumentationOfAdministrativeVeto(av);
        } catch( XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e ) {
          //sollte nicht auftreten, da ja gerade zuvor eingetragen
          //vetoStorableAccess.setDocumentationOfAdministrativeVeto sollte Exception werfen
        }
      }
    }
    return oldDok;
  }

  public VetoInformation freeAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    VetoInformation oldAV;
    try {
      vetoStorableAccess.freeAdministrativeVeto(administrativeVeto);
    } finally {
      oldAV = vetoCache.freeAdministrativeVeto(administrativeVeto);
      //darf ebenfalls XPRC_AdministrativeVetoDeallocationDenied werfen
    }
    return oldAV;
  }


  
  
  public Collection<VetoInformation> listVetos() {
    return vetoCache.listVetos();
  }

  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    return vetoStorableAccess.searchVetos(select, maxRows); //TODO vetoCache ber�cksichtigen?
  }
  
  @Override
  public VetoManagementAlgorithmType getAlgorithmType() {
    return VetoManagementAlgorithmType.CachedStorable;
  }
  
  @Override
  public String showInformation() {
    return getAlgorithmType() + ": "+getAlgorithmType().getDocumentation().get(DocumentationLanguage.EN)+" Cache size "+vetoCache.size();
  }

}
