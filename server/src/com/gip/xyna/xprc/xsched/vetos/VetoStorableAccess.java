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
package com.gip.xyna.xprc.xsched.vetos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.DedicatedConnection;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResultOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_VetonameMustNotBeEmpty;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;

public class VetoStorableAccess implements VetoManagementInterface {

  
  private static final Logger logger = CentralFactoryLogging.getLogger(VetoStorableAccess.class);

  private ODS ods;
  private boolean useMemoryFallbackForPreparedStatements = false;
  
  private PreparedCommand preparedDeletionByOrderId;
  private PreparedCommand preparedDeletionByOrderIdAndBinding;

  private static final String deleteByOrderIdSqlString = "delete from " + VetoInformationStorable.TABLE_NAME
      + " where " + VetoInformationStorable.COL_USING_ORDER_ID + " = ? ";
  private static final String deleteByOrderIdAndBindingSqlString = "delete from " + VetoInformationStorable.TABLE_NAME
  + " where " + VetoInformationStorable.COL_USING_ORDER_ID + " = ? AND " + VetoInformationStorable.COL_BINDING + " = ?";

  private int ownBinding;
  
  public void init() throws PersistenceLayerException {
    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ods.registerStorable(VetoInformationStorable.class);

    ownBinding = new VetoInformationStorable().getLocalBinding(ODSConnectionType.DEFAULT);
  }
  
  public int getOwnBinding() {
    return ownBinding;
  }

  
  
  public Collection<VetoInformationStorable> listVetoInformationStorable() throws PersistenceLayerException {
    Collection<VetoInformationStorable> vetos = null;
    ODSConnection con = ods.openConnection();
    try {
      vetos = con.loadCollection(VetoInformationStorable.class);
    } finally {
      finallyClose(con);
    }
    return vetos;
  }
  
  public Collection<VetoInformation> listVetos() {
    try {
      return CollectionUtils.transform(listVetoInformationStorable(), 
          VetoInformationStorable.toVetoInformation );
    } catch (PersistenceLayerException e) {
      return Collections.emptyList();
    }
  }

  public boolean deleteVetosByOrderId(long orderId) throws PersistenceLayerException {
    if (useMemoryFallbackForPreparedStatements) {
      return deleteVetosByOrderIdInMemory(orderId);
    } else {
      return deleteVetosByOrderIdWithPreparedStatement(orderId);
    }
  }

  public boolean deleteVetosByOrderIdAndBinding(long orderId) throws PersistenceLayerException {
    if (useMemoryFallbackForPreparedStatements) {
      return deleteVetosByOrderIdAndBindingInMemory(orderId, ownBinding);
    } else {
      return deleteVetosByOrderIdAndBindingWithPreparedStatement(orderId, ownBinding);
    }
  }
  

  private boolean deleteVetosByOrderIdWithPreparedStatement(final long orderId) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      if (preparedDeletionByOrderId == null) {
        try {
          preparedDeletionByOrderId = con.prepareCommand(new Command(deleteByOrderIdSqlString), true);
        } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
          logger.warn("Vetos are not configured on a PersistenceLayer with support for PreparedCommands.");
          useMemoryFallbackForPreparedStatements = true;
          return deleteVetosByOrderIdInMemory(orderId);
        }
      }
      int affectedRows = con.executeDML(preparedDeletionByOrderId, new Parameter(orderId));
      con.commit();
      return affectedRows > 0;
    } finally {
      finallyClose(con);
    }
  }
  
  
  private boolean deleteVetosByOrderIdAndBindingWithPreparedStatement(final long orderId, final long binding) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    
    try {
      if (preparedDeletionByOrderIdAndBinding == null) {
        try {
          preparedDeletionByOrderIdAndBinding =
              con.prepareCommand(new Command(deleteByOrderIdAndBindingSqlString), true);
        } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
          logger.warn("Vetos are not configured on a PersistenceLayer with support for PreparedCommands.");
          useMemoryFallbackForPreparedStatements = true;
          return deleteVetosByOrderIdInMemory(orderId);
        }
      }
      
      int affectedRows = con.executeDML(preparedDeletionByOrderIdAndBinding, new Parameter(orderId, binding));
      con.commit();
      return affectedRows > 0;
    } finally {
      finallyClose(con);
    }
  }


  private boolean deleteVetosByOrderIdInMemory(final long orderId) throws PersistenceLayerException {
    List<VetoInformationStorable> vetosToFree = new ArrayList<VetoInformationStorable>();
    ODSConnection con = ods.openConnection();
    try {
      Collection<VetoInformationStorable> vetos = con.loadCollection(VetoInformationStorable.class);
      for (VetoInformationStorable vis : vetos) {
        if (vis.getUsingOrderId() == orderId) {
          vetosToFree.add(vis);
        }
      }
      if (vetosToFree.size() > 0) {
        con.delete(vetosToFree);
        con.commit();
        return true;
      } else {
        return false;
      }
    } finally {
      finallyClose(con);
    }
  }
  
  
  private boolean deleteVetosByOrderIdAndBindingInMemory(final long orderId, final long binding)
      throws PersistenceLayerException {
    List<VetoInformationStorable> vetosToFree = new ArrayList<VetoInformationStorable>();
    ODSConnection con = ods.openConnection();

    try {
      Collection<VetoInformationStorable> vetos = con.loadCollection(VetoInformationStorable.class);

      for (VetoInformationStorable vis : vetos) {
        if ((vis.getUsingOrderId() == orderId) && (vis.getBinding() == binding)) {
          vetosToFree.add(vis);
        }
      }

      if (vetosToFree.size() > 0) {
        con.delete(vetosToFree);
        con.commit();
        return true;
      } else {
        return false;
      }
    } finally {
      finallyClose(con);
    }
  }


  private void finallyClose(ODSConnection con) {
    if (con != null) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Could not close connection", e);
      }
    }
  }
  
  private final VetoSearch vetosearch = new VetoSearch();
  
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    return vetosearch.searchVetos(select, maxRows);
  }

  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> vetos, long urgency) {
    try {
      return WarehouseRetryExecutor.buildCriticalExecutor().
          connectionDedicated(DedicatedConnection.XynaScheduler).
          storable(VetoInformationStorable.class).
          execute( new AllocateVetos(orderInformation, vetos) );
    } catch ( PersistenceLayerException e ) {
      logger.warn("Error while trying to allocate Vetos", e);
      return VetoAllocationResult.FAILED;
    }
  }
  
  public void undoAllocation(OrderInformation orderInformation, List<String> vetos) {
    freeVetos(orderInformation);
  }

  public boolean freeVetos(OrderInformation orderInformation) {
    try {
      return deleteVetosByOrderId(orderInformation.getOrderId());
    } catch (PersistenceLayerException e) {
      logger.error("Error while trying to deallocate vetos.", e);
      return false;
    }
  }
  
  public boolean freeVetosForced(long orderId) {
    try {
      return deleteVetosByOrderId(orderId);
    } catch (PersistenceLayerException e) {
      logger.error("Error while trying to force deallocation of vetos.", e);
      return false;
    }
  }

  

  private class AllocateVetos implements WarehouseRetryExecutableNoException<VetoAllocationResult> {
    private List<String> vetos;
    private OrderInformation orderInformation;

    public AllocateVetos(OrderInformation orderInformation, List<String> vetos ) {
      this.vetos = vetos;
      this.orderInformation = orderInformation;
    }

    public VetoAllocationResult executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      VetoAllocationResult var = allocateVetos(con, vetos, orderInformation);
      con.commit();
      return var;
    }
    
  }


  private VetoAllocationResult allocateVetos(ODSConnection con, List<String> vetos, OrderInformation orderInformation) throws PersistenceLayerException {

    long usingOrderId = orderInformation.getOrderId();
    //Liste der Veto-DB-Objekte erzeugen
    SortedMap<String, VetoInformationStorable> viss = new TreeMap<String, VetoInformationStorable>();
    for (String veto : vetos) {
      if( veto == null || veto.length() == 0 ) {
        return new VetoAllocationResult(new XPRC_VetonameMustNotBeEmpty());
      }
      viss.put(veto, new VetoInformationStorable(veto, orderInformation, ownBinding));
    }

    //Pruefen, ob Vetos bereits in Verwendung sind
    VetoAllocationResult var = checkVetos(usingOrderId, con, viss.values());
    if (var != null) {
      //Vetos sind bereits belegt, dies melden
      return var;
    } else {
      //Vetos sind noch frei, daher versuchen, sie in die DB einzutragen
      boolean success = insertVetos(usingOrderId, con, viss.values());
      if (success) {
        //Vetos erfolgreich eingetragen
        return VetoAllocationResult.SUCCESS;
      } else {
        //vermutlich eine Unique-Contraint-Violation
        //Dieser Fall sollte so gut wie nie (race condition) auftreten, daher muss er nicht performant sein
        logger.debug("Failed to insert new Vetos, checking again...");

        //Bei welchem Veto ist nun der andere schneller gewesen?
        var = checkVetos(usingOrderId, con, viss.values());
        if (var != null) {
          return var; //Meldung der bereits belegten Vetos
        } else {
          //Doch kein belegtes Veto gefunden, unbekannter Grund, warum Vetos nicht eingetragen werden konnten
          return VetoAllocationResult.FAILED;
        }
      }
    }

  }
  
  public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos) {
    //nichts zu tun
  }

  private VetoAllocationResult checkVetos(long ownOrderId, ODSConnection con, Collection<VetoInformationStorable> viss)
      throws PersistenceLayerException {
    for (VetoInformationStorable vis : viss) {
      try {
        VetoInformationStorable existingVis = new VetoInformationStorable(vis.getVetoName(), vis.getBinding());
        con.queryOneRow(existingVis);
        // This can happen if the order was resumed from backup, it will always try to reallocate as it could have released
        // but would no be continued from a previous checkpoint
        if (existingVis.getUsingOrderId() != ownOrderId) {
          //Veto ist bereits in Verwendung
          return new VetoAllocationResult(VetoInformationStorable.toVetoInformation.transform(existingVis) );
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // fully expected, veto is not used by anyone else
      }
    }
    return null;
  }
  
  private boolean insertVetos(long ownOrderId, ODSConnection con, Collection<VetoInformationStorable> viss)
      throws PersistenceLayerException {

    //einfacher Algorithmus 
    //for( VetoInformationStorable vis : viss ) { con.persistObject(vis); }
    //con.commit();
    //funktioniert so nicht, da con.persistObject(vis) keine UniqueConstraintViolation werden kann,
    //sondern stattdessen immer ein Update durchfuehrt.

    //Daher nun ReturnWert von persistObject pruefen

    for (VetoInformationStorable vis : viss) {
      try {
        // query using a new object to make sure that the passed object is not changed
        VetoInformationStorable existingVis = new VetoInformationStorable(vis.getVetoName(), vis.getBinding());
        con.queryOneRow(existingVis);
        if (existingVis.getUsingOrderId() == ownOrderId) {
          // veto has already been allocated. this can happen e.g. in a case in which one cluster node crashes and the
          // other tries to resume an order that had allocated a veto on the crashed node before. this is a more reasonable
          // solution than deleting all allocated vetos once one node crashed because than the resumed order could not be
          // sure that no other order has been doing something requireing the veto.

          //Tritt bei jedem Wiederherstellen aus dem OrderBackup auf, nicht nur bei Crash.
          if (logger.isDebugEnabled()) {
            logger.debug("Order <" + ownOrderId + "> had already allocated the veto <" + vis.getVetoName()
                + ">. This is probably due to a resuming an order from orderBackup during Startup or OrderMigration.");
          }
          continue;
        } else {
          con.rollback();
          return false;
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // veto does not exist, try to create it
        boolean alreadyExists = con.persistObject(vis);
        if (alreadyExists) {
          //Veto existiert nun doch schon und kann daher nicht neu eingetragen werden
          con.rollback();
          return false;
        }
      }
    }
    //alle Vetos erfolgreich eingetragen
    con.commit();
    return true;

  }

  
  private static class AllocateAdministrativeVeto implements WarehouseRetryExecutableNoResultOneException<XPRC_AdministrativeVetoAllocationDenied> {

    private VetoInformationStorable veto;

    public AllocateAdministrativeVeto(VetoInformationStorable veto) {
      this.veto = veto;
    }

    public void executeAndCommit(ODSConnection con)
        throws PersistenceLayerException, XPRC_AdministrativeVetoAllocationDenied {
      if (con.persistObject(veto)) {
        con.rollback();
        try {
          con.queryOneRow(veto);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new XPRC_AdministrativeVetoAllocationDenied(veto.getVetoName(), 0L);
        }
        throw new XPRC_AdministrativeVetoAllocationDenied(veto.getVetoName(), veto.getUsingOrderId());
      }
      con.commit();
    }
    
  }

  public void allocateAdministrativeVeto(AdministrativeVeto av) throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    WarehouseRetryExecutor.buildUserInteractionExecutor().
                           storable(VetoInformationStorable.class).
                           execute( new AllocateAdministrativeVeto(av.toVetoInformationStorable(ownBinding)));
  }

  private static class FreeAdministrativeVeto implements WarehouseRetryExecutableOneException<VetoInformation,XPRC_AdministrativeVetoDeallocationDenied> {

    private VetoInformationStorable veto;

    public FreeAdministrativeVeto(VetoInformationStorable veto) {
      this.veto = veto;
    }

    public VetoInformation executeAndCommit(ODSConnection con)
        throws PersistenceLayerException, XPRC_AdministrativeVetoDeallocationDenied {
      try {
        con.queryOneRow(veto);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new XPRC_AdministrativeVetoDeallocationDenied(veto.getVetoName());
      }
      if (veto.getUsingOrderId().equals(AdministrativeVeto.ADMIN_VETO_ORDERID)) {
        con.deleteOneRow(veto);
      } else {
        throw new XPRC_AdministrativeVetoDeallocationDenied(veto.getVetoName());
      }
      con.commit();
      return VetoInformationStorable.toVetoInformation.transform(veto);
    }
    
  }


  public VetoInformation freeAdministrativeVeto(AdministrativeVeto av) throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    return WarehouseRetryExecutor.buildUserInteractionExecutor().
                           storable(VetoInformationStorable.class).
                           execute(new FreeAdministrativeVeto(av.toVetoInformationStorable(ownBinding)));
  }

  private static class DocumentAdministrativeVeto implements WarehouseRetryExecutableOneException<String,XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY> {

    private VetoInformationStorable veto;
    private String documentation;

    public DocumentAdministrativeVeto(VetoInformationStorable veto) {
      this.veto = veto;
      this.documentation = veto.getDocumentation();
      this.veto.setDocumentation(null);
    }

    public String executeAndCommit(ODSConnection con)
        throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      con.queryOneRow(veto);
      String oldDoc = veto.getDocumentation();
      veto.setDocumentation(documentation);
      if (con.persistObject(veto)) {
        con.commit();
      } else {
        con.rollback();
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(veto.getPrimaryKey().toString(), VetoInformationStorable.TABLE_NAME);
      }
      return oldDoc;
    }
    
  }

  public String setDocumentationOfAdministrativeVeto(AdministrativeVeto av) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    return WarehouseRetryExecutor.buildUserInteractionExecutor().
                           storable(VetoInformationStorable.class).
                           execute(new DocumentAdministrativeVeto(av.toVetoInformationStorable(ownBinding)));
  }

  @Override
  public VetoManagementAlgorithmType getAlgorithmType() {
    return VetoManagementAlgorithmType.Storable;
  }

  @Override
  public String showInformation() {
    return getAlgorithmType() + ": "+getAlgorithmType().getDocumentation().get(DocumentationLanguage.EN);
  }

}
