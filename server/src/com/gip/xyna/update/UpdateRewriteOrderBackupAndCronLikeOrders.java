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
package com.gip.xyna.update;



import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.update.specialstorablesignoringserialversionuid.CronLikeOrderIgnoringSerialVersionUID;
import com.gip.xyna.update.specialstorablesignoringserialversionuid.OrderInstanceBackupIgnoringSerialVersionUID;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;


/*
 * falls serialiversionuid im backup geupdated werden muss 
 */
public class UpdateRewriteOrderBackupAndCronLikeOrders {


  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateRewriteOrderBackupAndCronLikeOrders.class);

  private static final String sqlGetAllOrderBackups = "select * from "
    + OrderInstanceBackup.TABLE_NAME + " ORDER BY " + OrderInstanceBackup.COL_ID + " ASC";
  private static final String sqlGetAllCronLikeOrders = "select * from "
      + CronLikeOrder.TABLE_NAME + " ORDER BY "
      + CronLikeOrder.COL_ID + " ASC";


  private final boolean workflows;
  private final boolean datatypes;
  private final boolean exceptions;
  
  public interface Transformation {
    public void transform(OrderInstanceBackupIgnoringSerialVersionUID ob);
  }
  
  private static List<Transformation> transformations;
  
  public static void addTransformation(Transformation t) {
    if (transformations == null) {
      transformations = new ArrayList<Transformation>();
    }
    transformations.add(t);
  }


  public UpdateRewriteOrderBackupAndCronLikeOrders(boolean workflows, boolean datatypes, boolean exceptions) {
    this.workflows = workflows;
    this.datatypes = datatypes;
    this.exceptions = exceptions;
  }

  

  protected void update() throws XynaException {

    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      // factory ist noch nicht initialisiert, zur Deserialisierung des xprc.xfractwfe.base.XynaProcess aber nötig
      //FIXME siehe Bug 12743 xprc.xfractwfe.base.XynaProcess
      UpdateGeneratedClasses.mockFactory();

      ODS ods = ODSImpl.getInstance();
      ods.registerStorable(OrderInstanceBackupIgnoringSerialVersionUID.class);
      ods.registerStorable(CronLikeOrderIgnoringSerialVersionUID.class);
      ODSConnection conDef = ods.openConnection(ODSConnectionType.DEFAULT);
      ODSConnection conHis = ods.openConnection(ODSConnectionType.HISTORY);
      SerializableClassloadedObject.setIgnoreExceptionsWhileDeserializing(true);
      try {
        if (workflows || datatypes || exceptions) {
          updateOrderBackup(conDef);
        }
        if (datatypes || exceptions) {
          updateCronLikeOrder(conHis);
        }
      } finally {
        conDef.closeConnection();
        conHis.closeConnection();
        ods.unregisterStorable(CronLikeOrderIgnoringSerialVersionUID.class);
        ods.unregisterStorable(OrderInstanceBackupIgnoringSerialVersionUID.class);
        SerializableClassloadedObject.setIgnoreExceptionsWhileDeserializing(false);
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }


  private void updateCronLikeOrder(ODSConnection conHis) throws PersistenceLayerException {
    try {
      FactoryWarehouseCursor<CronLikeOrderIgnoringSerialVersionUID> cursorCrons =
          conHis.getCursor(sqlGetAllCronLikeOrders, new Parameter(), CronLikeOrderIgnoringSerialVersionUID.reader, 100);
      for (List<CronLikeOrderIgnoringSerialVersionUID> nextOBs : cursorCrons.batched(100)) {
        // FIXME catch the exception and continue with the next batch? reduce batch size to decrease impact of broken entries?
        cursorCrons.checkForExceptions();
        conHis.persistCollection(nextOBs);
        conHis.commit();
      }
    } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
      //xmlpersistence oder ähnliches. crons kann man nicht verwenden, wenn sie nicht auf einen durchsuchbaren persistencelayer konfiguriert sind
      //deshalb ist hier nichts zu tun
      logger.trace("Found cronlikeorders configured to non searchable persistencelayer.", e);
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to read CronLikeOrders");
      throw e;
    }
  }


  private void updateOrderBackup(ODSConnection conDef) throws PersistenceLayerException {
    try {
      FactoryWarehouseCursor<OrderInstanceBackupIgnoringSerialVersionUID> cursorOIB =
          conDef.getCursor(sqlGetAllOrderBackups, new Parameter(),
                           OrderInstanceBackupIgnoringSerialVersionUID.reader, 100);
      for (List<OrderInstanceBackupIgnoringSerialVersionUID> nextOBs : cursorOIB.batched(100)) {
        // FIXME catch the exception and continue with the next batch? reduce batch size to decrease impact of broken entries?
        cursorOIB.checkForExceptions();
        if (transformations != null) {
          for (Transformation t : transformations) {
            for (OrderInstanceBackupIgnoringSerialVersionUID ob : nextOBs) {
              t.transform(ob);
            }
          }
        }
        conDef.persistCollection(nextOBs);
        conDef.commit();
      }
    } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
      //xmlpersistence oder ähnliches. backups kann man nicht verwenden, wenn nicht auf einen durchsuchbaren persistencelayer konfiguriert
      //deshalb ist hier nichts zu tun
      logger.trace("Found orderbackup configured to non searchable persistencelayer.", e);
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to read OrderInstanceBackup ");
      throw e;
    }
  }


}
