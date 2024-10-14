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
package com.gip.xyna.xprc.xsched.orderseries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.SeriesInformation;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.xpce.AbstractConnectionAwareAck;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;


/**
 *
 */
public class OrderSeriesSeparator {

  private static Logger logger = CentralFactoryLogging.getLogger(OrderSeriesSeparator.class);
    
  private static CollectionUtils.Transformation<XynaOrder, String> getOrCreateCorrId = 
                  new CollectionUtils.Transformation<XynaOrder, String>() {
    public String transform(XynaOrder from) {
      String correlationId = from.getSeriesCorrelationId();
      if( correlationId == null ) {
        correlationId = from.getSeriesInformation().getCorrelationId();
        if( correlationId == null ) {
          correlationId = SeriesInformation.createCorrelationId(from);
          logger.info( "old correlationId is null, generating new correlationId "+correlationId );
          from.getSeriesInformation().setCorrelationId(correlationId);
        }
      }
      return correlationId;
    }
  };
  
  /**
   * @param hasToBeAcknowledged
   * @param backupCon 
   */
  public OrderSeriesSeparator(boolean hasToBeAcknowledged, ODSConnection backupCon) {
    this.hasToBeAcknowledged = hasToBeAcknowledged;
    this.backupCon = backupCon;
  }

  public OrderSeriesSeparator() {
    this(false,null);
  }

  /**
   * Liegt die SeriesInformation im alten Format vor und müssen deshalb die darin enthaltenen 
   * XynaOrders separiert in den Scheduler eingestellt werden?
   * @param xo
   * @return
   */
  @SuppressWarnings("deprecation")
  public static boolean hasToSeparateSeries(XynaOrder xo) {
    if( ! xo.isInOrderSeries() ) {
      return false;
    }
    SeriesInformation si = xo.getSeriesInformation();
    if( isEmpty( si.getPredecessors() ) && isEmpty( si.getSuccessors() ) ) {
      return false;
    } 
    return true;
  }
 
  private LinkedHashMap<Long,XynaOrder> allOrders = new LinkedHashMap<Long,XynaOrder>();
  private long mainOrderId;
  private List<XynaOrderServerExtension> series;
  private BackupAck backup; 
  private boolean hasToBeAcknowledged;
  private ODSConnection backupCon;
  private AbstractConnectionAwareAck originalAck;
  

  /**
   * Separieren eines Auftrags:
   * <ul>
   * <li> Rekursive Sammlung aller Aufträge, die in den SeriesInformation gespeichert
   *      sind (XynaOrder-&gt;SeriesInformation-&gt;XynaOrder-&gt;SeriesInformation-&gt;...)
   * <li> Migration der enthalten SeriesInformation auf das neue Format mit CorrIds
   * <li> Für alle XynaOrder außer der übergebenen: Erzeugung der 
   *      XynaOrderServerExtension, Füllen des OrderContext und Speichern in Liste, 
   *      die über {@link getSeries()} erhältlich ist
   * </ul>
   * @param xo
   */
  public void separate(XynaOrderServerExtension xo) {
    gatherAllOrders( allOrders, xo );
    mainOrderId = xo.getId();
    OrderContext baseOrderContext = xo.getOrderContext();
    
    //nun erhält jeder Auftrag eine neue SeriesInformation
    for( XynaOrder o : allOrders.values() ) {
      o.setSeriesInformation(migrateSeriesInformation( o ));
    }
    
    series = new ArrayList<XynaOrderServerExtension>();
    for( XynaOrder seriesXo : allOrders.values() ) {
      if( seriesXo.getId() != mainOrderId ) {
        XynaOrderServerExtension seriesXose = new XynaOrderServerExtension(seriesXo);
        //application version von main order erben
        seriesXose.setParentRevision(xo.getParentRevision());
        seriesXose.getDestinationKey().setRuntimeContext(xo.getDestinationKey().getRuntimeContext());
        
        seriesXose.setOrderContext( 
          OrderContextServerExtension.createOrderContextFromExisting( baseOrderContext, seriesXose) );
        series.add( seriesXose );
      }
    }
    if( logger.isDebugEnabled() ) {
      logger.debug("Separated series into "+(series.size()+1)+" orders" );    
    }
    
    if( hasToBeAcknowledged ) {
      //BackupAck erzeugen
      backup = new BackupAck( backupCon );
      
      //Sichern des ursprüngliches Acks, da es sonst aus dem gemeinsamen OrderContext verschwindet
      originalAck = xo.getOrderContext().getAcknowledgableObject();
    }
  }
  
  /**
   * Liefert Liste aller Serien-Aufträge ohne den Basis-Auftrag
   * @return
   */
  public List<XynaOrderServerExtension> getSeries() {
    return series;
  }
  
  /**
   * Migration der enthalten SeriesInformation auf das neue Format mit CorrIds
   * @param xo
   * @return neue SeriesInformation
   */
  @SuppressWarnings("deprecation")
  public SeriesInformation migrateSeriesInformation(XynaOrder xo) {
    SeriesInformation oldSi = xo.getSeriesInformation();
    
    
    SeriesInformation newSi = new SeriesInformation( getOrCreateCorrId.transform(xo) );
    newSi.addToSeries(
      CollectionUtils.transform( Arrays.asList(oldSi.getPredecessors()), getOrCreateCorrId ),
      CollectionUtils.transform( Arrays.asList(oldSi.getSuccessors()), getOrCreateCorrId ) );
    newSi.setAutoCancel(oldSi.isAutoCancel());
    return newSi;
  }
  
  private static boolean isEmpty(Object[] array) {
    return array == null || array.length == 0;
  }

  /**
   * Extraktion aller SeriesInformation->XynaOrder aus der übergeben XynaOrder
   * @param allOrders
   * @param xo
   */
  @SuppressWarnings("deprecation")
  private void gatherAllOrders(Map<Long, XynaOrder> allOrders, XynaOrder xo) {
    if( ! allOrders.containsKey( xo.getId() ) ) {
      allOrders.put( Long.valueOf( xo.getId() ), xo );    
      for( XynaOrder suc : xo.getSeriesInformation().getSuccessors() ) {
        gatherAllOrders(allOrders, suc);
      }
      for( XynaOrder pre : xo.getSeriesInformation().getPredecessors() ) {
        gatherAllOrders(allOrders, pre);
      }
    }
  }
   
  /**
   * Lokale Implementation des {@link AbstractConnectionAwareAck}, 
   */
  public static class BackupAck extends AbstractConnectionAwareAck {
    private static final long serialVersionUID = 1L;

    public BackupAck(ODSConnection con) {
      super(con);
    }

    public void acknowledgeSchedulerEntry(XynaOrderServerExtension xose)
                    throws XPRC_OrderEntryCouldNotBeAcknowledgedException, XNWH_RetryTransactionException {
      try {
        backup(xose);
        xose.setHasBeenBackuppedInScheduler(true);
      } catch (XNWH_RetryTransactionException rte) {
        logger.warn("Failed to create backup for XynaOrder <" + xose.getId() + ">", rte);
        throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(rte);
        //FIXME hier nur rte weiterwerfen? In com.gip.xyna.xprc.xpce.AbstractBackupAck ebenfalls anpassen!
      } catch (PersistenceLayerException ple) {
        logger.warn("Failed to create backup for XynaOrder <" + xose.getId() + ">", ple);
        throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(ple);
      }
    }
    
    private void backup(final XynaOrderServerExtension xose) throws PersistenceLayerException {
      WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {
        public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
              .backup(xose, BackupCause.ACKNOWLEDGED, con);
        }
      };
      WarehouseRetryExecutor.buildCriticalExecutor().
        connection(getConnection()).
        storables(backupStorables()).
        execute(wre);
      }
  }

  public void prepareAcknowledge(XynaOrderServerExtension xose) {
    //BackupAck jedes mal neu setzen, da es aus dem gemeinsamen OrderContext nach jedem acknowledge gelöscht wird
    xose.getOrderContext().set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY,backup);
  }

  public void restoreAcknowledge(XynaOrderServerExtension xo) {
    if( hasToBeAcknowledged ) {
      //ursprüngliches Ack wiederherstellen
      xo.getOrderContext().set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY,originalAck);
    }
  }


  
}
