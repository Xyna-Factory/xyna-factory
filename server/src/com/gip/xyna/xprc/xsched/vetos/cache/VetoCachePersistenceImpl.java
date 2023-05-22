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
package com.gip.xyna.xprc.xsched.vetos.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
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
    
    public ForEachValidVeto(int binding) {
      this.binding = binding;
    }
    
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
      rootOrderIdsInBackup = new HashSet<>(oa.listRootOrderIdsFromOrderBackup(con));
      
      Collection<VetoInformationStorable> vetos = con.loadCollection(VetoInformationStorable.class);
      for( VetoInformationStorable veto : vetos ) {
        VetoInformation vi = VetoInformationStorable.toVetoInformation.transform(veto);
        boolean delete = execute(vi);
        if( delete ) {
          logger.warn("Deleted invalid veto "+veto.getVetoName() );
          con.deleteOneRow(veto);
        }
      }
      con.commit();
      executeAfterwards();
    }

    protected abstract void executeAfterwards();

    protected abstract boolean execute(VetoInformation vi);

    protected boolean checkValid(VetoInformation vi) {
      return rootOrderIdsInBackup.contains( vi.getUsingRootOrderId() ); 
    }
  }
  
  private class InitializeVetos extends ForEachValidVeto {

    public InitializeVetos(int binding) {
      super(binding);
    }

    @Override
    protected boolean execute(VetoInformation vi) {
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
    protected boolean execute(VetoInformation vi) {
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
      if( checkValid(vi) ) {
        return false; //valide Vetos behalten
      } else {
        vetoCache.free(vce, vi.getUsingOrderId() );
        return false; //eigentlich true, aber L�schen wird von free erledigt
      }
    }
    
    @Override
    protected void executeAfterwards() {
      //alle fremden Vetos l�schen, die nicht mehr valide sind
      for( VetoInformation vi : vetoCache.listVetos() ) {
        if( ! checkValid(vi) ) {
          VetoCacheEntry vce = vetoCache.get(vi.getName());
          if( vce != null ) {
            vetoCache.free(vce, vi.getUsingOrderId() );
          }
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
      } else {
        logger.info( "deleted "+toDelete.size()+ " and persisted " + toPersist.size() + " vetos");
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
          //Muss nicht mehr in DB erg�nzt werden
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
