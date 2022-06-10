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
package com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagementInterface.StepStatus;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable;



public class MigrateMIsAndCronsAndOrderBackups extends OrderBackupHelperProcessAbstract {


  public static final String COMPONENTDISPLAYNAME = "Migrating orderbackup";


  public MigrateMIsAndCronsAndOrderBackups(List<PrioritizedRootId> idsWithPriority, int ownBinding) {
    super(idsWithPriority, ownBinding);
  }


  @Override
  protected  WarehouseRetryExecutableNoResult getWarehouseRetryExecutable() {
    WarehouseRetryExecutableNoResult wren = new WarehouseRetryExecutableNoResult() {

      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        
        hasConnection = true;
        
        // vor der ersten Persistierung prüfen wir nochmal, ob nicht doch Migration gestoppt werden soll
        if (stop) {
          return;
        }
         
        migrateMIs(con);
        migrateCronLikeOrders(con);
        try {
          migrateAndLoadOrderBackup(con);
        } catch( PersistenceLayerException e ) {
          logger.warn( "migrateAndLoadOrderBackup failed", e);
          throw e;
        } catch( RuntimeException e ) {
          logger.warn( "migrateAndLoadOrderBackup failed", e);
          throw e;
        }
        con.commit();
      }

    };
    return wren;
  }
  
  
  protected StorableClassList getAdditionalStorables() {
    return new StorableClassList(ManualInteractionEntry.class, CronLikeOrder.class, SeriesInformationStorable.class);                           
  }

  private void migrateAndLoadOrderBackup(ODSConnection con) throws PersistenceLayerException {
    int orderBackupLoadCounter = 0;

    PreparedQuery<OrderCount> prepareQueryCountOrders =
        con.prepareQuery(new Query<OrderCount>("select count(*) from " + OrderInstanceBackup.TABLE_NAME
            + " where not (" + OrderInstanceBackup.COL_BINDING + " = ?)", OrderCount.getCountReader()), false);

    OrderCount orderCount = con.queryOneRow(prepareQueryCountOrders, new Parameter(ownBinding));

    // select * from orderbackup where not (binding = ?) order by rootorderid
    FactoryWarehouseCursor<OrderInstanceBackup> cursor =
        con.getCursor("select * from " + OrderInstanceBackup.TABLE_NAME + " where not ("
                          + OrderInstanceBackup.COL_BINDING + " = ?) order by " + OrderInstanceBackup.COL_ROOT_ID
                          + " asc",
                      new Parameter(ownBinding), OrderInstanceBackup.getReaderWarnIfNotDeserializable(), BLOCKSIZE);

    List<OrderInstanceBackup> incompleteFamily = new ArrayList<OrderInstanceBackup>();
    while ( ! stop ) { //stop kann Migration vorzeitig abbrechen, wenn anderer Knoten wieder startet
      
      List<OrderInstanceBackup> nextOBs = cursor.getRemainingCacheOrNextIfEmpty();
      if (nextOBs == null || nextOBs.size() == 0) {
        break;
      }
      
      //komplette Root-Familien extrahieren, unvollständige Familien aufbewahren
      List<OrderInstanceBackup> completeFamilies = extractCompleteOrderFamilies( incompleteFamily, nextOBs );
      
      //priorisierte Migrationen ergänzen
      completeFamilies.addAll( retrievePrioritized(con, getLastRootId(completeFamilies), 
                                                   "MigrateMIsAndCronsAndOrderBackups" ) );
      
      //Migration durchführen und Aufträge starten
      migrateAndLoadOrderBackup(con, completeFamilies);
      
      //Zwischencommit: falls viele Einträge im OrderBackup liegen darf Hauptspeicher nicht platzen
      con.commit();
      
      XynaExtendedStatusManagement xesm = XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement();
      xesm.updateStep(StepStatus.MIGRATION, COMPONENTDISPLAYNAME,
                      "Migrating orderbackup entries " + orderBackupLoadCounter + "/" + orderCount.getCount());
      
    }
    if( ! stop ) {
      //Migration des Rests durchführen und Aufträge starten
      migrateAndLoadOrderBackup(con, incompleteFamily);
      con.commit();
    }
  }
  

  private void migrateAndLoadOrderBackup(ODSConnection con, List<OrderInstanceBackup> completeFamilies) throws PersistenceLayerException {
    if (completeFamilies.isEmpty()) {
      return; //nichts zu tun
    }
    
    //Migration durchführen
    Map<MigrationResult, List<OrderInstanceBackup>> backupOrderMap = loadAndMigrateOrderBackup(completeFamilies, con, "migration loop");
    
    //Fehlgeschlagene Migrationen abbrechen
    abortBackups(backupOrderMap.get(MigrationResult.NOK), con);  
    
    //Erfolreiche Migrationen fertigstellen
    resumeOrdersFromBackup(con, backupOrderMap.get(MigrationResult.OK) );
  }

  
  private void migrateCronLikeOrders(ODSConnection con) throws PersistenceLayerException {

    String selectCLOCountSQL = "select count(*) from " + CronLikeOrder.TABLE_NAME
        + " where not (" + CronLikeOrder.COL_BINDING + " = ?)";
    
    OrderCount cloCount =
        con.queryOneRow(con.prepareQuery(new Query<OrderCount>(selectCLOCountSQL, OrderCount.getCountReader())),
                        new Parameter(ownBinding));
        
    int cloIndex = 0;

    FactoryWarehouseCursor<CronLikeOrder> cloCursor =
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
            .getCursorForCronLikeOrdersWithOtherBinding(ownBinding, con, BLOCKSIZE);
    
    List<CronLikeOrder> cronlikeorders = cloCursor.getRemainingCacheOrNextIfEmpty();
    
    while(!cronlikeorders.isEmpty() && !stop) {
      cloIndex += cronlikeorders.size();
      List<Long> rootorderIds = new ArrayList<Long>();
      List<CronLikeOrder> changedCronLikeOrders = new ArrayList<CronLikeOrder>();
      for(CronLikeOrder cron : cronlikeorders) {
        if (cron.getRootOrderId() == null || alreadyProcessedIds.contains(cron.getRootOrderId())) {
          // schon migriert, offensichtlich
          continue;
        }
        migrateOneCron(cron.getId(), con);
        rootorderIds.add(cron.getRootOrderId());
        changedCronLikeOrders.add(cron);
      }
        
      // Prüfen, ob einzelne rootids priorisiert bearbeitet werden sollen
      List<Long> prioIds = retrievePrioritized(-1, "MigrateMIsAndCronsAndOrderBackups");
      
      if (!prioIds.isEmpty()) {
        // laden der Crons

        List<? extends CronLikeOrder> cronsPriority = getCronLikeOrderItems(prioIds, con);
        for(CronLikeOrder cron : cronsPriority) {
          migrateOneCron(cron.getId(), con);
          changedCronLikeOrders.add(cron);
        }
      }
      
      rootorderIds.addAll(prioIds);
      Map<MigrationResult, List<OrderInstanceBackup>> backup = loadAndMigrateOrderBackupWithRootOrderIds(rootorderIds, con, "cronlikeorder migration");
      
      List<OrderInstanceBackup> backupsToAbort = backup.get(MigrationResult.NOK);
      abortBackups(backupsToAbort, con);

      cleanupListOfCrons(changedCronLikeOrders, backupsToAbort);
      
      if (!changedCronLikeOrders.isEmpty()) {
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
            .tryAddNewOrders(changedCronLikeOrders);
      }
      
      resumeOrdersFromBackup(con, backup.get(MigrationResult.OK));
      
      processPrioritized(con, prioIds, "MigrateMIsAndCronsAndOrderBackups");
      
      con.commit(); //Zwischencommit
      
      cronlikeorders = cloCursor.getRemainingCacheOrNextIfEmpty();
      
      XynaFactory
          .getInstance()
          .getFactoryManagement()
          .getXynaExtendedStatusManagement()
          .updateStep(StepStatus.MIGRATION, COMPONENTDISPLAYNAME,
                      "Migrating cron like orders " + cloIndex + "/" + cloCount.getCount());
    }
    
  }

  @SuppressWarnings("unchecked")
  private void migrateMIs(ODSConnection con) throws PersistenceLayerException {

    String selectMISQL = "select * from " + ManualInteractionEntry.TABLE_NAME
        + " where not (" + ManualInteractionEntry.COL_BINDING + " = ?) order by "
        + ManualInteractionEntry.MI_COL_XYNAORDER_ROOT_ID;
    
    String selectMICountSQL = "select count(*) from " + ManualInteractionEntry.TABLE_NAME
        + " where not (" + ManualInteractionEntry.COL_BINDING + " = ?)";
    
    OrderCount miCount =
        con.queryOneRow(con.prepareQuery(new Query<OrderCount>(selectMICountSQL, OrderCount.getCountReader())),
                        new Parameter(ownBinding));
    
    FactoryWarehouseCursor<? extends ManualInteractionEntry> miCursor =
        con.getCursor(selectMISQL, new Parameter(ownBinding), new ManualInteractionEntry().getReader(), BLOCKSIZE);

    
    int miIndex = 0;
    
    List<? extends ManualInteractionEntry> miEntries = miCursor.getRemainingCacheOrNextIfEmpty();
    while(!miEntries.isEmpty() && !stop) {
      miIndex += miEntries.size();

      List<ManualInteractionEntry> changedMIEntries = new ArrayList<ManualInteractionEntry>();
      List<? extends CronLikeOrder> cronEntries = null;
      Map<MigrationResult, List<OrderInstanceBackup>> backup = null;
      Set<Long> usedRootOrderIdsInMIs = new HashSet<Long>();
      // sammeln aller RootOrderIds, um Crons, die selbe RootOrderId wie MI verwenden, schneller zu finden
      for (ManualInteractionEntry mientry : miEntries) {
        usedRootOrderIdsInMIs.add(mientry.getRootOrderId());
      }
      for(ManualInteractionEntry miEntry : miEntries) {
        if (alreadyProcessedIds.contains(miEntry.getRootOrderId())) {
          // wurde schon verarbeitet
          continue;
        }
        miEntry.setBinding(ownBinding);
        changedMIEntries.add(miEntry);
      }
      alreadyProcessedIds.addAll(usedRootOrderIdsInMIs);
      
      // handle idsWithPriority
      List<Long> prioIds = retrievePrioritized(-1, "MigrateMIsAndCronsAndOrderBackups");
      List<Long> mientryIdsWithPriority = new ArrayList<Long>();
      usedRootOrderIdsInMIs.addAll(prioIds);
      mientryIdsWithPriority.addAll(prioIds);
      
      if(!mientryIdsWithPriority.isEmpty()) {
        List<? extends ManualInteractionEntry> misPriority = getManualInteractionItems(mientryIdsWithPriority, con);
        for(ManualInteractionEntry mi : misPriority) {
          mi.setBinding(ownBinding);
          changedMIEntries.add(mi);   
        }
      }
      
      if(!usedRootOrderIdsInMIs.isEmpty()) {
        // change binding of cron like orders
        cronEntries = getCronLikeOrderItems(usedRootOrderIdsInMIs, con);
        Iterator<? extends CronLikeOrder> iterCrons = cronEntries.iterator();
        while(iterCrons.hasNext()) {
          CronLikeOrder clo = iterCrons.next();
          if (!migrateOneCron(clo.getId(), con)) {
            // offensichtlich schon ausgeführt ... Cron ist jedenfalls nicht mehr da
            iterCrons.remove();
          }
        }
        
        // change orderbackup entries
        backup = loadAndMigrateOrderBackupWithRootOrderIds(usedRootOrderIdsInMIs, con, "manual interaction migration");
      }
      
      con.persistCollection(changedMIEntries);
      
      List<OrderInstanceBackup> backupsToAbort = backup.get(MigrationResult.NOK);
      abortBackups(backupsToAbort, con);
      
      cleanupListOfMIs(changedMIEntries, backupsToAbort);
      cleanupListOfCrons(cronEntries, backupsToAbort);

      if (!cronEntries.isEmpty()) {
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
            .tryAddNewOrders((List<CronLikeOrder>)cronEntries);
      }
      
      resumeOrdersFromBackup(con, backup.get(MigrationResult.OK));
      
      processPrioritized(con, prioIds, "MigrateMIsAndCronsAndOrderBackups");
      
      con.commit(); //Zwischencommit

      XynaFactory
          .getInstance()
          .getFactoryManagement()
          .getXynaExtendedStatusManagement()
          .updateStep(StepStatus.MIGRATION, COMPONENTDISPLAYNAME,
                      "Migrating manual interaction entries " + miIndex + "/" + miCount.getCount());
      
      miEntries = miCursor.getRemainingCacheOrNextIfEmpty();
    }
  }

  
  private void cleanupListOfCrons(List<? extends CronLikeOrder> cronEntries, List<OrderInstanceBackup> backupsToAbort) {
    if (backupsToAbort.size() == 0) {
      return;
    }
    Set<Long> abortedRootOrderIds = new HashSet<Long>();
    for (OrderInstanceBackup oib : backupsToAbort) {
      abortedRootOrderIds.add(oib.getRootId());
    }
    Iterator<? extends CronLikeOrder> it = cronEntries.iterator();
    while (it.hasNext()) {
      CronLikeOrder clo = it.next();
      if (abortedRootOrderIds.contains(clo.getRootOrderId())) {
        it.remove();
      }
    }
  }


  private void cleanupListOfMIs(List<ManualInteractionEntry> changedMIEntries, List<OrderInstanceBackup> backupsToAbort) {
    if (backupsToAbort.size() == 0) {
      return;
    }
    Set<Long> abortedRootOrderIds = new HashSet<Long>();
    for (OrderInstanceBackup oib : backupsToAbort) {
      abortedRootOrderIds.add(oib.getRootId());
    }
    Iterator<ManualInteractionEntry> it = changedMIEntries.iterator();
    while (it.hasNext()) {
      ManualInteractionEntry mie = it.next();
      if (abortedRootOrderIds.contains(mie.getRootOrderId())) {
        it.remove();
      }
    }
  }


  private List<? extends CronLikeOrder> getCronLikeOrderItems(Collection<Long> rootorderIds, ODSConnection con)
      throws PersistenceLayerException {
    
    List<Object> rootOrderIDList = new ArrayList<Object>();
    StringBuilder sql = new StringBuilder();
    sql.append("select * from " + CronLikeOrder.TABLE_NAME + " where (");

    Iterator<Long> iterRootOrderIds = rootorderIds.iterator();
    while(iterRootOrderIds.hasNext()) {
      sql.append(CronLikeOrder.COL_ASSIGNED_ROOT_ORDER_ID).append("=?");
      rootOrderIDList.add(iterRootOrderIds.next());
      
      if(iterRootOrderIds.hasNext()) {
        sql.append(" or ");
      }
    }
    sql.append(") order by ").append(CronLikeOrder.COL_ASSIGNED_ROOT_ORDER_ID);
    
    PreparedQuery<? extends CronLikeOrder> loadcloEntriesForRootIds =
        preparedQueryCache.getQueryFromCache(sql.toString(), con, new CronLikeOrder().getReader());
    
    return con.query(loadcloEntriesForRootIds, new Parameter(rootOrderIDList.toArray()), -1);
  }
  
  
  private List<? extends ManualInteractionEntry> getManualInteractionItems(Collection<Long> rootorderIds, ODSConnection con)
      throws PersistenceLayerException {
    
    List<Object> rootOrderIDList = new ArrayList<Object>();
    StringBuilder sql = new StringBuilder();
    sql.append("select * from " + ManualInteractionEntry.TABLE_NAME + " where (");

    Iterator<Long> iterRootOrderIds = rootorderIds.iterator();
    while(iterRootOrderIds.hasNext()) {
      sql.append(ManualInteractionEntry.MI_COL_XYNAORDER_ROOT_ID).append("=?");
      rootOrderIDList.add(iterRootOrderIds.next());
      
      if(iterRootOrderIds.hasNext()) {
        sql.append(" or ");
      }
    }
    sql.append(") order by ").append(ManualInteractionEntry.MI_COL_XYNAORDER_ROOT_ID);
    
    PreparedQuery<? extends ManualInteractionEntry> loadMIEntriesForRootIds =
        preparedQueryCache.getQueryFromCache(sql.toString(), con, new ManualInteractionEntry().getReader());
    
    return con.query(loadMIEntriesForRootIds, new Parameter(rootOrderIDList.toArray()), -1);
  }
  

  private boolean migrateOneCron(final Long cronLikeOrderId, ODSConnection con) throws PersistenceLayerException {

    CronLikeOrder clo = new CronLikeOrder(cronLikeOrderId);
    XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
        .markAsNotToScheduleAndRemoveFromQueue(cronLikeOrderId);
    con.executeAfterCommitFails(new Runnable() {

      public void run() {
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
            .unmarkAsNotToSchedule(cronLikeOrderId);
      }
    });

    try {
      con.queryOneRowForUpdate(clo);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // offensichtlich ist CLO schon ausgeführt wurden? Jedenfalls ist nichts zu tun.
      con.executeAfterCommit(new Runnable() {

        public void run() {
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
              .unmarkAsNotToSchedule(cronLikeOrderId);
        }
      });
      return false;
    }

    if (clo.getRootOrderId() != null) {
      clo.setBinding(ownBinding);
      con.persistObject(clo);
    }
    return true;
  }


  private Map<MigrationResult, List<OrderInstanceBackup>> loadAndMigrateOrderBackup(List<OrderInstanceBackup> backupOrders, ODSConnection con, String action)
      throws PersistenceLayerException {
    
    Map<MigrationResult, List<OrderInstanceBackup>> backupOrderMap = emptyMap();
    if (backupOrders.isEmpty()) {
      return backupOrderMap;
    }
    List<OrderInstanceBackup> migrated = backupOrderMap.get(MigrationResult.OK);
    List<OrderInstanceBackup> failed = backupOrderMap.get(MigrationResult.NOK);
        
    List<OrderInstanceDetails> orderArchive = new ArrayList<OrderInstanceDetails>();
    List<SeriesInformationStorable> seriesInfos = new ArrayList<SeriesInformationStorable>();
    
    for( OrderInstanceBackup foreignBackupInstance : backupOrders ) {
      // Prüfung, ob orderbackup nicht vllt. doch schon migriert wurde ... sollte eigentlich nicht passieren
      boolean bootCountDiffers = foreignBackupInstance.getBootCntId() != XynaFactory.getInstance().getBootCntId();
      //bootCountDiffers ist hier wahrscheinlich überflüssig
      if (bootCountDiffers && foreignBackupInstance.getBinding() != ownBinding) {
        //es muss migriert werden
        foreignBackupInstance.setBootCntId(XynaFactory.getInstance().getBootCntId());
        foreignBackupInstance.setBinding(ownBinding);
        if (foreignBackupInstance.getDetails() != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("Found order details for order <" + foreignBackupInstance.getId() + ">");
          }
          if (checkOrderBackupInstanceForRemoval(foreignBackupInstance, con, false)) {
            //removed
            failed.add(foreignBackupInstance);
            continue;
          }
          orderArchive.add(foreignBackupInstance.getDetails());
        }
        migrated.add(foreignBackupInstance);
      } else {
        //ignorieren
      }
      XynaOrder xo = foreignBackupInstance.getXynaorder();
      if( xo != null && xo.isInOrderSeries() ) {
        SeriesInformationStorable sis = new SeriesInformationStorable(xo.getId());
        try {
          con.queryOneRow( sis );
        }
        catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          //Dies sollte nur direkt nach der Umstellung auf die Version mit SeriesInformationStorable
          //auftreten können.
          logger.warn( "No SeriesInformationStorable found for xynaorder "+xo+", creating new." );
          sis = XynaFactory.getInstance().getProcessing().getXynaScheduler()
                         .getOrderSeriesManagement().createSeriesInformationStorable(xo);
        }
        sis.setBinding(ownBinding);
        seriesInfos.add(sis);
      }
      
    }

    if (logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      int size = backupOrders.size();
      sb.append("Found ").append(size).append(" foreign ").append(size==1?"entry":"entries");
      sb.append(" in table ").append(OrderInstanceBackup.TABLE_NAME);
      sb.append(" while performing ").append(action);
      sb.append(", switching ");
      size = migrated.size();
      sb.append(size).append(" order backup ").append(size==1?"entry":"entries");
      size = orderArchive.size();
      if( size > 0 ) {
        size = orderArchive.size();
        sb.append(", ").append(size).append(" order archive ").append(size==1?"entry":"entries");
      }
      size = seriesInfos.size();
      if( size > 0 ) {
        sb.append(", ").append(size).append(" series information ").append(size==1?"entry":"entries");
      }
      sb.append(" to own binding <").append(ownBinding).append(">");
      size = failed.size();
      if( size > 0 ) { 
        sb.append("and detecting ").append(size).append(" incomplete order backup").append(size==1?"entry":"entries");
      }
      sb.append(".");
      logger.info(sb.toString());
    }

    con.persistCollection(orderArchive);
    con.persistCollection(migrated);
    con.persistCollection(seriesInfos);
    return backupOrderMap;
  }

  private Map<MigrationResult, List<OrderInstanceBackup>> emptyMap() {
    Map<MigrationResult, List<OrderInstanceBackup>> map = new HashMap<MigrationResult, List<OrderInstanceBackup>>();
    for (MigrationResult mr : MigrationResult.values()) {
      map.put(mr, new ArrayList<OrderInstanceBackup>());
    }
    return map;
  }

  public static enum MigrationResult {
    OK, NOK;
  }
  
  /**
   * @return  
   */
  private Map<MigrationResult, List<OrderInstanceBackup>> loadAndMigrateOrderBackupWithRootOrderIds(Collection<Long> ids, ODSConnection con, String action)
      throws PersistenceLayerException {
    if (ids.isEmpty()) {
      return emptyMap();
    }
    List<OrderInstanceBackup> backupOrders = getBackupItems(ids, con);
    return loadAndMigrateOrderBackup(backupOrders, con, action);
  }
  

}
