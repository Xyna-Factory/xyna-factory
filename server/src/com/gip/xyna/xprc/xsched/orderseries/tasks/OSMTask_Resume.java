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

package com.gip.xyna.xprc.xsched.orderseries.tasks;

import java.util.ArrayList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xprcods.orderarchive.MasterWorkflowStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xsched.orderseries.OSMInterface.OrderState;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees.SisData;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees.TreeNode;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable.OrderStatus;




/**
 * OSMTask_Resume:
 * <br>
 * Im OrderBackupHelperProcessAbstract gerufen, um für gebackupte Sereinaufträge erneut zu 
 * untersuchen, ob diese starten können. In der Zwischenzeit (während des Backups) könnten 
 * unter Umständen Predecessoren gelaufen sein, ohne dass diese ihren Successor starten konnten.
 * Im Grunde muss hier genau das gleiche gemacht werden wie bei 
 * {@link com.gip.xyna.xprc.xsched.orderseries.tasks.OSMTask_Preschedule OSMTask_Preschedule},
 * allerdings wird hier zusätzlich vorher für jeden bekannten Predecessor überprüft, ob dieser nicht
 * mittlerweile fertig ist. Dies hilft bei dem Fehler, dass der Server gecrasht ist und nicht alle
 * Daten über fertige Aufträge in SeriesInformationStorable eintragen konnte.
 * Leider ist häufig das OrderArchive so konfiguriert, dass diese Information nicht mehr nachträglich 
 * beschafft werden kann. In diesem Fall muss der Serienauftrag manuell gestartet werden.
 *
 */
public class OSMTask_Resume extends OSMTask {

  private String correlationId;

  OSMTask_Resume(String correlationId) {
    this.correlationId = correlationId;
  }
  
  @Override
  public String getCorrelationId() {
    return correlationId;
  }

  @Override
  protected void executeInternal() {
    
    ArrayList<String> predecessorCorrIds = null;
    SeriesInformationStorable sis = null;
    osmCache.lock( correlationId );
    try {
      predecessorTrees.buildTree( correlationId );    
      TreeNode tree = predecessorTrees.getTree( correlationId );
      
      sis = osmCache.get(correlationId);
      if( sis == null ) {
        throw new IllegalStateException("SeriesInformationStorable not found");
      }
      //Tree-Daten korrigieren
      checkTreeNodeBinding( tree, sis );
        
      predecessorCorrIds = new ArrayList<String>(sis.getPredecessorCorrIds());
    } finally {
      osmCache.unlock( correlationId );
    }
    
    for( String predecessorCorrId : predecessorCorrIds ) {
      osmCache.lock( predecessorCorrId );
      try {
        TreeNode preTree = predecessorTrees.getTree( predecessorCorrId );
        SeriesInformationStorable sisPre = osmCache.get(predecessorCorrId);
        if( sisPre != null ) {
          if( preTree == null ) {
            predecessorTrees.buildTree( predecessorCorrId );    
            preTree = predecessorTrees.getTree( predecessorCorrId );
          }
          checkTreeNodeBinding( preTree, sisPre );
          if( ! sisPre.getOrderStatus().isFinished() ) {
            //Predecessor ist noch nicht fertig. Dies ist wahrscheinlich korrekt, allerdings sollte
            //dies jetzt hier geprüft werden. Es besteht nämlich die Möglichkeit, dass der Update 
            //des SeriesInformationStorable vor dem Resume nicht mehr geklappt hat, da der eigene oder 
            //der fremde Knoten gecrasht war.
            checkFinished(sisPre);
          }
        } else {
          //keinen Predecessor gefunden, wahrscheinlich existiert er noch nicht
        }
      } finally {
        osmCache.unlock( predecessorCorrId );
      }
    }
    
    boolean orderStarted = false;
    OrderStatus orderStatus = sis.getOrderStatus();
    switch( orderStatus ) {
      case WAITING:
        //restliche Bearbeitung wie in OSMTask_Preschedule, daher hier aufrufen
        OSMTask_Preschedule preschedule = new OSMTask_Preschedule(correlationId);
        preschedule.execute(osmCache, osm, localOsm, remoteOsm, predecessorTrees);
        orderStarted = preschedule.isOrderStarted();
        break;
      case RUNNING:
        //Auftrag lief bereits schonmal, daher nun wieder starten
        osm.readyToRun( sis.getCorrelationId(), sis.getId(), OrderState.CanBeStarted, null );
        orderStarted = true;
        break;
      default:
        throw new IllegalStateException("SeriesInformationStorable should not have orderStatus "+orderStatus );
    }
    if( orderStarted ) {
      logger.debug( "SeriesOrder "+sis.getId()+" could be started in status "+orderStatus );
    }
  }

  /**
   * @param sisPre
   */
  private void checkFinished(SeriesInformationStorable sisPre) {
    String status = searchOrderStatusInArchive( sisPre.getId() );
    MasterWorkflowStatus mws = MasterWorkflowStatus.fromName(status);
    if( mws == null ) {
      //OrderInstance wurde nicht gefunden oder hatte unerwarteten Status
      if( "not found".equals(status) ) {
        //Order ist entweder noch nicht gelaufen oder aber nicht mehr im Archiv auffindbar.
      } else {
        logger.warn("OrderInstance status "+status+" unexpected for id="+sisPre.getId());
      }
      //Falls der Auftrag doch bereits gelaufen sein sollte, hat dies die Konsequenz, dass 
      //die Nachfolge-Aufträge nie automatisch starten und daher manuell gestartet werden müssen.
    } else {
      switch( mws ) {
        case CANCELED:
          if( sisPre.getOrderStatus() != OrderStatus.CANCELING ) {
            sisPre.setOrderStatus(OrderStatus.CANCELED);
          } else {
            //hier könnte selten ein Fehler entstehen, wenn OrderStatus CANCELING ist, der
            //Auftrag bereits gecancelt ist, alle Vorgänger bereits fertig sind und diese 
            //Information kurz vor dem Crash nicht mehr in sisPre geschrieben werden konnte.
          }
          break;
        case FINISHED:
          sisPre.setOrderStatus(OrderStatus.SUCCEEDED);
          break;
        case SCHEDULING_TIME_OUT:
        case XYNA_ERROR:
          sisPre.setOrderStatus(OrderStatus.FAILED);
          break;
        default:
          logger.warn("Unexpected MasterWorkflowStatus "+mws);
          sisPre.setOrderStatus(OrderStatus.FAILED);
      }
      osmCache.update(sisPre);
    }    
  }
  
  //package private, damit von OSMTask_Reschedule ebenfalls verwendbar
  static String searchOrderStatusInArchive( long id ) {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      String status = null;
      try {
        OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
        OrderInstance oi = oa.getAuditAccess().restore(con, id, false);
        con.queryOneRow(oi);
        status = oi.getStatusAsString();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to read OrderInstance for order series resume", e);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        //Order ist entweder noch nicht gelaufen oder aber nicht mehr im Archiv auffindbar.
        //Evtl. in OrderArchive_DEFAULT
        status = "not found";
      }
      return status;
    } finally {
      try {
        con.closeConnection();
      }
      catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection for order series resume", e);
      }
    }
  }

  /**
   * @param tree
   * @param sis
   */
  private void checkTreeNodeBinding(TreeNode tree, SeriesInformationStorable sis) {
    if( tree.hasData() ) {
      if( tree.getBinding() != sis.getBinding() ) {
        tree.setValue( new SisData(sis) );
      }
    } else {
      tree.setValue( new SisData(sis) );
    }
  }

}
