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
package com.gip.xyna.update;



import java.util.Iterator;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



public class UpdateOrderInstanceBackupToNewBackupCauses extends UpdateJustVersion {

  public UpdateOrderInstanceBackupToNewBackupCauses(Version oldVersion, Version newVersion, boolean needsRegenerate) {
    super(oldVersion, newVersion, needsRegenerate);
  }


  @Override
  protected void update() throws XynaException {
    
    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      UpdateGeneratedClasses.mockFactory();
 
      ODS ods = ODSImpl.getInstance();
      ods.registerStorable(OrderInstanceBackup.class);
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
  
      try {
  
        FactoryWarehouseCursor<OrderInstanceBackup> cursor = con
                        .getCursor("select * from " + OrderInstanceBackup.TABLE_NAME, new Parameter(),
                                   OrderInstanceBackup.getReaderWarnIfNotDeserializable(), 100);
  
  
        List<OrderInstanceBackup> orderList = cursor.getRemainingCacheOrNextIfEmpty();
        while (orderList != null && orderList.size() > 0) {
          Iterator<OrderInstanceBackup> iter = orderList.iterator();
          boolean changesCommit = false;
          while (iter.hasNext()) {
            OrderInstanceBackup oib = iter.next();
            if (oib.getBackupcause().equals("OTHER")) {
              changesCommit = true;
              if (oib.getXynaorder() == null && oib.getDetails() != null) {
                OrderInstanceDetails oid = oib.getDetails();
                if (OrderInstanceStatus.FINISHED == oid.getStatusAsEnum() ) {
                  oib.setBackupCause(BackupCause.FINISHED_SUBWF);
                } else {
                  // kann es sein, das Pr�fung oben nicht vollst�ndig ist? Selbst wenn, sollte dies kein Problem sein -
                  // der Eintrag sollte
                  // nur falsch getaggt sein.
                  oib.setBackupCause(BackupCause.ACKNOWLEDGED);
                }
              } else {
                oib.setBackupCause(BackupCause.ACKNOWLEDGED);
              }
              
              con.persistObject(oib);
            }
          }
  
          if (changesCommit) {
            con.commit();
          }
          orderList = cursor.getRemainingCacheOrNextIfEmpty();
        }
  
      } catch (XynaException e) {
        logger.error("Update OrderInstanceBackup failed: " + e.getMessage(), e);
        throw new RuntimeException(e);
      } finally {
        con.closeConnection();
        ods.unregisterStorable(OrderInstanceBackup.class);
      }
    }  finally {
      XynaFactory.setInstance(oldInstance);
    }
  }


}
