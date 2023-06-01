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
package com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagementInterface.StepStatus;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;


public class LoadOrderBackupWithOwnBindingAndDifferentBootCountId extends OrderBackupHelperProcessAbstract {
 
  private volatile boolean attemptingPause;
  private volatile boolean isPaused;
  
  public static final String COMPONENTDISPLAYNAME = "Loading orderbackup";
  
  public LoadOrderBackupWithOwnBindingAndDifferentBootCountId(List<PrioritizedRootId> idsWithPriority, int ownBinding) {
    super(idsWithPriority, ownBinding);
  }

  protected  WarehouseRetryExecutableNoResult getWarehouseRetryExecutable() {
    WarehouseRetryExecutableNoResult wren = new WarehouseRetryExecutableNoResult() {

      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        
        hasConnection = true;
        
        int orderBackupLoadCounter = 0;
        attemptingPause = false;
        isPaused = false;
        long lastId = 0;
        
        PreparedQuery<OrderCount> prepareQueryCountOrders = con.prepareQuery(new Query<OrderCount>("select count(*) from "
                     + OrderInstanceBackup.TABLE_NAME + " where not (" + OrderInstanceBackup.COL_BOOTCNTID + "=?) and "
                     + OrderInstanceBackup.COL_ID + " > ? and " + OrderInstanceBackup.COL_BINDING + " =?", OrderCount.getCountReader()), false);
        
        refreshCursorAfterPause: while(true) {
          
          // Gesamtanzahl der zu migrierenden Aufträge ermitteln ermitteln
          OrderCount orderCount = con.queryOneRow(prepareQueryCountOrders,  new Parameter(XynaFactory.getInstance().getBootCntId(), lastId, ownBinding));
          orderBackupLoadCounter = 0;
          
          // Sortierung nach rootid, damit Auftragsfamilien zusammenhängend sind und zusammen verarbeitet werden
          // select * from orderbackup where not (bootcntid = ?) and binding = ? order by rootid asc
          FactoryWarehouseCursor<OrderInstanceBackup> cursor = con.getCursor("select * from " + OrderInstanceBackup.TABLE_NAME + 
                     " where not (" + OrderInstanceBackup.COL_BOOTCNTID + "=?) and " + OrderInstanceBackup.COL_ID + " > ? and "
                     + OrderInstanceBackup.COL_BINDING + " =? order by " + OrderInstanceBackup.COL_ROOT_ID + " asc",
                     new Parameter(XynaFactory.getInstance().getBootCntId(), lastId, ownBinding), OrderInstanceBackup.getReaderWarnIfNotDeserializable(), BLOCKSIZE);
          
          List<OrderInstanceBackup> incompleteFamily = new ArrayList<OrderInstanceBackup>();
          while ( ! stop ) { //stop kann Laden vorzeitig abbrechen
            
            List<OrderInstanceBackup> nextOBs = cursor.getRemainingCacheOrNextIfEmpty();
            if (nextOBs == null || nextOBs.size() == 0) {
              break;
            }
            
            //komplette Root-Familien extrahieren, unvollständige Familien aufbewahren
            List<OrderInstanceBackup> completeFamilies = extractCompleteOrderFamilies( incompleteFamily, nextOBs );
            
            //priorisierte OrderBackups ergänzen
            completeFamilies.addAll( retrievePrioritized(con, getLastRootId(completeFamilies), 
                                                         "LoadOrderBackupWithOwnBindingAndDifferentBootCountId" ) );
            
            //Aufträge starten
            loadOrderBackup(con, completeFamilies);
            
            //Zwischencommit: falls viele Einträge im OrderBackup liegen darf Hauptspeicher nicht platzen
            con.commit();
            
            XynaExtendedStatusManagement xesm = XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement();
            xesm.updateStep(StepStatus.POSTSTARTUP, COMPONENTDISPLAYNAME,
                            orderBackupLoadCounter + "/" + orderCount.getCount());
            
            if(attemptingPause) {
              isPaused = true;
              logger.debug("LoadOrderBackupWithOwnBindingAndDifferentBootCountId: Pause loading.");
              
              cursor.close();
              while(attemptingPause) {
                try {
                  Thread.sleep(50);
                } catch (InterruptedException e) {
                  return;
                }
              }
              isPaused = false;
              logger.debug("LoadOrderBackupWithOwnBindingAndDifferentBootCountId: Refresh cursor after pause.");
              continue refreshCursorAfterPause;
            }
            
          }
          if( ! stop ) {
            //restliche Aufträge starten
            loadOrderBackup(con, incompleteFamily);
            con.commit();
          }
          
          break;
        }
        
        con.commit();
      }

    };
    return wren;
  }
  
  @Override
  protected void runInternally() {
    try {
      super.runInternally();
    } finally {
      attemptingPause = false;
    }
  }
  
  private void loadOrderBackup(ODSConnection con, List<OrderInstanceBackup> completeFamilies) throws PersistenceLayerException {
    if (completeFamilies.isEmpty()) {
      return; //nichts zu tun
    }
    
    //Migration durchführen
    Pair<List<OrderInstanceBackup>,List<OrderInstanceBackup>> pair = loadOrderBackup(completeFamilies, con, "migration loop");
    
    //Fehlerhafte OrderBackups abbrechen
    abortBackups(pair.getSecond(), con);  
    
    //Erfolreiche Migrationen fertigstellen
    resumeOrdersFromBackup(con, pair.getFirst() );
  }
  
  
  

  private Pair<List<OrderInstanceBackup>,List<OrderInstanceBackup>> loadOrderBackup(List<OrderInstanceBackup> backupOrders,
                                                                                    ODSConnection con, String string) throws PersistenceLayerException {
   
    List<OrderInstanceBackup> failed = new ArrayList<OrderInstanceBackup>();
    List<OrderInstanceBackup> loaded = new ArrayList<OrderInstanceBackup>();
    List<OrderInstanceDetails> orderArchive = new ArrayList<OrderInstanceDetails>();
    for( OrderInstanceBackup oib : backupOrders ) {
      // Prüfung, ob orderbackup nicht vllt. doch schon geladen wurde ... sollte eigentlich nicht passieren
      boolean alreadyLoaded = alreadyProcessedIds.contains( oib.getRootId() ); 
      boolean bootCountDiffers = oib.getBootCntId() != XynaFactory.getInstance().getBootCntId();
      if (bootCountDiffers && ! alreadyLoaded) {
        //es muss migriert werden
        oib.setBootCntId(XynaFactory.getInstance().getBootCntId());
        if (oib.getDetails() != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("Found order details for order <" + oib.getId() + ">");
          }
          if (checkOrderBackupInstanceForRemoval(oib, con, false)) {
            //removed
            failed.add(oib);
            continue;
          }
          orderArchive.add(oib.getDetails());
        }
        loaded.add(oib);
      } else {
        //ignorieren
      }
    }
    
    con.persistCollection(orderArchive);
    con.persistCollection(loaded); //TODO persist notwendig wenn nicht im SAFE-mode  
    // nicht unbedingt nötig, da jedes weitere ...
    //...Backup den einzig geänderten Wert BootCntId schreibt
    
    return Pair.of(loaded, failed);
  }

  public void pause() {
    if(attemptingPause) {
      return;
    }
    attemptingPause = true;
    while(!isPaused && attemptingPause) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {        
      }
    }
  }
  
  public void resume() {
    attemptingPause = false;
  }
  
}
