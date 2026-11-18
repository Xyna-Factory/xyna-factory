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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.utils.db.utils.RepeatedExceptionCheck;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.DedicatedConnection;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.VetoSearch;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;

public class VetoCachePersistenceImpl implements VetoCachePersistence {

  private static final Logger logger = CentralFactoryLogging.getLogger(VetoCachePersistenceImpl.class);

  private VetoCache vetoCache;
  private int ownBinding;
  private final VetoSearch vetosearch = new VetoSearch();
  private PersistenceFailure persistenceFailure;
  
  public VetoCachePersistenceImpl(VetoCache vetoCache) {
    this.vetoCache = vetoCache;
  }
  
  @Override
  public void init() throws XynaException {
    CentralComponentConnectionCache.getInstance()
    .openCachedConnection(ODSConnectionType.DEFAULT,
        DedicatedConnection.VetoManagement,
        new StorableClassList( VetoInformationStorable.class, OrderInstanceBackup.class ) );

    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ods.registerStorable(VetoInformationStorable.class);

    ownBinding = new VetoInformationStorable().getLocalBinding(ODSConnectionType.DEFAULT);
  }
  
  @Override
  public void initVetoCache(int binding) {
    try {
      WarehouseRetryExecutor.buildCriticalExecutor().
        connectionDedicated(DedicatedConnection.VetoManagement).
        storable(VetoInformationStorable.class).storable(OrderInstanceBackup.class).
        execute( new InitializeVetos(binding) );
    } catch( PersistenceLayerException e ) {
      logger.warn("Could not read vetos", e);
    }
  }
  
  @Override
  public int getOwnBinding() {
    return ownBinding;
  }

  @Override
  public List<VetoCacheEntry> persist(List<VetoCacheEntry> toPersist, List<VetoCacheEntry> toDelete) {
    List<VetoInformationStorable> toDeleteStorables = new ArrayList<>();
    List<VetoInformationStorable> toPersistStorables = new ArrayList<>();
    List<VetoCacheEntry> notPersisted = new ArrayList<>();
    
    //Siehe Kommentar in VCP_Abstract.PersistVetos.add(...)
    
    for( VetoCacheEntry veto : toPersist ) {
      if( veto.getState() == State.Scheduled ) {
        VetoInformation vi = veto.getVetoInformation();
        if( vi != null ) {
          toPersistStorables.add( VetoInformationStorable.fromVetoInformation.transform(vi));
        } else {
          //unerwartet, da bei free erst State umgesetzt wird, dann VetoInformation entfernt
          // State Scheduled sollte immer VetoInformation haben
          logger.warn("Veto in state Scheduled but no VetoInformation for "+ veto);
          notPersisted.add(veto);
        }
      } else {
        notPersisted.add(veto);
      }
    }
    for( VetoCacheEntry veto : toDelete ) {
      toDeleteStorables.add( new VetoInformationStorable(veto.getName(), 0) );
    }
    
    try {
      
      if( persistenceFailure != null ) {
        //Versuch, die bisherigen Daten nachzutragen
        WarehouseRetryExecutor.buildCriticalExecutor().
          connectionDedicated(DedicatedConnection.VetoManagement).
          storable(VetoInformationStorable.class).
          execute( new PersistVetoRequests(persistenceFailure.toPersist(), persistenceFailure.toDelete() ) );
        persistenceFailure = null; //nachtragen hat geklappt
      }
      
      //aktuelle Daten eintragen
      WarehouseRetryExecutor.buildCriticalExecutor().
          connectionDedicated(DedicatedConnection.VetoManagement).
          storable(VetoInformationStorable.class).
          execute( new PersistVetoRequests(toPersistStorables, toDeleteStorables) );
      
    } catch ( Exception e ) {
      //Fehler sind hier unerwartet: Die SQL-Datenmodifikationen sollten keine Fehler liefern.
      //Es bleiben Fehler wie Verbindung zur DB kaputt oder Tabelle kaputt
      if( persistenceFailure == null ) {
        PersistenceFailure pf = new PersistenceFailure();
        pf.fail(e, toPersistStorables, toDeleteStorables);
        persistenceFailure = pf;
      } else {
        persistenceFailure.fail(e, toPersistStorables, toDeleteStorables);
      }
    }
    return notPersisted;
  }
  
  
  
  @Override
  public void cleanupVetoCache() {
    try {
      WarehouseRetryExecutor.buildCriticalExecutor().
        connectionDedicated(DedicatedConnection.VetoManagement).
        storable(VetoInformationStorable.class).storable(OrderInstanceBackup.class).
        execute( new CleanupVetos(ownBinding) );
    } catch( PersistenceLayerException e ) {
      logger.warn("Could not read vetos", e);
    }
  }

  private abstract class ForEachValidVeto implements WarehouseRetryExecutableNoResult {
    
    protected int binding;
    private Set<Long> rootOrderIdsInBackup;
    private ODSConnection orderBackupConnection;
    
    public ForEachValidVeto(int binding) {
      this.binding = binding;
    }
    
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      orderBackupConnection = con;
      OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
      rootOrderIdsInBackup = new HashSet<>(oa.listRootOrderIdsFromOrderBackup(con));
      
      Collection<VetoInformationStorable> vetos = con.loadCollection(VetoInformationStorable.class);
      for( VetoInformationStorable veto : vetos ) {
        VetoInformation original = VetoInformationStorable.toVetoInformation.transform(veto);
        VetoInformation vi = pruneInvalidSharedHolders(original);
        boolean delete = execute(vi);
        if( delete ) {
          logger.warn("Deleted invalid veto "+veto.getVetoName() );
          con.deleteOneRow(veto);
        } else if (vi != original) {
          con.persistObject(VetoInformationStorable.fromVetoInformation.transform(vi));
        }
      }
      con.commit();
      executeAfterwards();
    }

    protected abstract void executeAfterwards() throws PersistenceLayerException;

    protected abstract boolean execute(VetoInformation vi) throws PersistenceLayerException;

    protected boolean checkValid(VetoInformation vi) throws PersistenceLayerException {
      if (vi.isAdministrative()) {
        return true;
      }
      if (vi.getUsingOrderId() != null && rootOrderIdsInBackup.contains(vi.getUsingRootOrderId())) {
        return true;
      }
      if (vi.getPendingExclusiveOrderId() != null && isOrderValid(vi.getPendingExclusiveOrderId())) {
        return true;
      }
      if (vi.getSharedOrderIds() != null) {
        for (Long sharedOrderId : vi.getSharedOrderIds()) {
          if (isOrderValid(sharedOrderId)) {
            return true;
          }
        }
      }
      return false;
    }

    private VetoInformation pruneInvalidSharedHolders(VetoInformation vi) throws PersistenceLayerException {
      List<Long> sharedOrderIds = vi.getSharedOrderIds();
      List<Long> validSharedOrderIds = null;
      if (sharedOrderIds != null) {
        validSharedOrderIds = new ArrayList<>();
        for (Long orderId : sharedOrderIds) {
          if (isOrderValid(orderId)) {
            validSharedOrderIds.add(orderId);
          }
        }
      }
      Long pendingOrderId = vi.getPendingExclusiveOrderId();
      if (pendingOrderId != null && !isOrderValid(pendingOrderId)) {
        pendingOrderId = null;
      }
      if (Objects.equals(sharedOrderIds, validSharedOrderIds) &&
          Objects.equals(vi.getPendingExclusiveOrderId(), pendingOrderId)) {
        return vi;
      }
      return new VetoInformation(vi.getName(), vi.getUsingOrder(), validSharedOrderIds, pendingOrderId,
          vi.getDocumentation(), System.currentTimeMillis(), vi.getBinding(), vi.getAuthorityBinding(),
          vi.getAuthorityEpoch(), vi.getAuthorityVersion() + 1);
    }

    private boolean isOrderValid(Long orderId) throws PersistenceLayerException {
      if (orderId == null || rootOrderIdsInBackup.contains(orderId)) {
        return orderId != null;
      }
      OrderArchive orderArchive = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
      try {
        long rootOrderId = orderArchive.readRootOrderIdFromOrderBackup(orderBackupConnection, orderId);
        return rootOrderIdsInBackup.contains(rootOrderId);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return true;
      }
    }

    protected void freeInvalidHolders(VetoCacheEntry veto, VetoInformation information)
        throws PersistenceLayerException {
      if (information.getUsingOrderId() != null && !rootOrderIdsInBackup.contains(information.getUsingRootOrderId())) {
        vetoCache.free(veto, information.getUsingOrderId());
      }
      if (information.getSharedOrderIds() != null) {
        for (Long orderId : new ArrayList<>(information.getSharedOrderIds())) {
          if (!isOrderValid(orderId)) {
            vetoCache.free(veto, orderId);
          }
        }
      }
      Long pendingOrderId = information.getPendingExclusiveOrderId();
      if (pendingOrderId != null && !isOrderValid(pendingOrderId)) {
        vetoCache.free(veto, pendingOrderId);
      }
    }
  }
  
  private class InitializeVetos extends ForEachValidVeto {

    public InitializeVetos(int binding) {
      super(binding);
    }

    @Override
    protected boolean execute(VetoInformation vi) throws PersistenceLayerException {
      if( binding == -1 || vi.getBinding() == binding ) {
        if( checkValid(vi) ) {
          if( ! vetoCache.createUsedVeto(vi) ) {
            logger.warn("Veto "+vi.getName() +" already exists");
          }
          return false;
        } else {
          return true;
        }
      }
      return false;
    }

    @Override
    protected void executeAfterwards() {
      //nichts zu tun
    }

  }
  
  private class CleanupVetos extends ForEachValidVeto {

    public CleanupVetos(int binding) {
      super(binding);
     
    }
    
    @Override
    protected boolean execute(VetoInformation vi) throws PersistenceLayerException {
      VetoCacheEntry vce = vetoCache.get(vi.getName());
      if( vce == null ) {
        return true;
      }
      if( vi.isAdministrative() ) {
        return false; //adminstrative Vetos behalten
      }
      if( vi.getBinding() == binding ) {
        return false; //eigene Vetos behalten
      }
      freeInvalidHolders(vce, vi);
      return false;
    }
    
    @Override
    protected void executeAfterwards() throws PersistenceLayerException {
      //alle fremden Vetos löschen, die nicht mehr valide sind
      for( VetoInformation vi : vetoCache.listVetos() ) {
        VetoCacheEntry vce = vetoCache.get(vi.getName());
        if (vce != null && vi.getBinding() != binding && !vi.isAdministrative()) {
          freeInvalidHolders(vce, vi);
        }
      }
    }

  }
  
  
  private static class PersistVetoRequests implements WarehouseRetryExecutableNoResult {
    private Collection<VetoInformationStorable> toPersist;
    private Collection<VetoInformationStorable> toDelete;
    
    public PersistVetoRequests(Collection<VetoInformationStorable> toPersist, Collection<VetoInformationStorable> toDelete) {
      this.toPersist = toPersist;
      this.toDelete = toDelete;
    }

    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      //Siehe Kommentar in VCP_Abstract.PersistVetos.add(...): Zuerst delete, dann persist
      con.delete(toDelete);
      con.persistCollection(toPersist);
      con.commit();
      if( logger.isTraceEnabled() ) {
        StringBuilder sb = new StringBuilder();
        sb.append("persisted ");
        String sep = "persisted ";
        for( VetoInformationStorable vis : toPersist ) {
          sb.append(sep).append(vis.getVetoName());
          sep = ", ";
        }
        sep = "; deleted ";
        for( VetoInformationStorable vis : toDelete ) {
          sb.append(sep).append(vis.getVetoName());
          sep = ", ";
        }
        logger.trace( sb.toString() );
      } else if (logger.isDebugEnabled()) {
        logger.debug( "deleted "+toDelete.size()+ " and persisted " + toPersist.size() + " vetos");
      }
    }
    
  }

  @Override
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    return vetosearch.searchVetos(select, maxRows);
  }

  @Override
  public void appendInformation(StringBuilder sb) {
    PersistenceFailure pf = persistenceFailure;
    if( pf == null ) {
      sb.append("persistent");
    } else {
      sb.append("persistent (");
      pf.appendInformation(sb);
      sb.append(")");
    }
  }

  private static class PersistenceFailure {
    private RepeatedExceptionCheck rec = new RepeatedExceptionCheck();
    private Map<String,VetoInformationStorable> toPersist  = new HashMap<>();
    private Map<String,VetoInformationStorable> toDelete = new HashMap<>();
    
    
    private void fail(Exception e, List<VetoInformationStorable> toPersistStorables,
        List<VetoInformationStorable> toDeleteStorables) {
      int repeated = rec.checkRepeationCount(e);
      if( repeated == 0 ) {
        logger.warn("Error while trying to process vetos. Trying to proceed anyway", e);
      } else {
        logger.warn("Error while trying to process vetos again ("+repeated+"). Trying to proceed anyway");
      }
      //Zuerst delete, dann persist: wie auch bei der DB selbst
      for( VetoInformationStorable delete : toDeleteStorables ) {
        String id = delete.getVetoName();
        VetoInformationStorable existing = toPersist.remove(id); 
        if( existing != null ) {
          //Muss nicht mehr in DB ergänzt werden
        } else {
          toDelete.put(id,delete);
          //Muss weiterhin aus DB entfernt werden
        }
      }
      for( VetoInformationStorable persist : toPersistStorables ) {
        toPersist.put(persist.getVetoName(),persist); 
      }
    }
    
    

    public Collection<VetoInformationStorable> toDelete() {
      return toDelete.values();
    }

    public Collection<VetoInformationStorable> toPersist() {
      return toPersist.values();
    }

    public void appendInformation(StringBuilder sb) {
      sb.append("failed: \"").append(rec.getLastThrowable().getMessage()).append("\"; ");
      sb.append(toPersist.size()).append(" to persist, ");
      sb.append(toDelete.size()).append(" to delete");
      
    }

  }
  
}
