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

import java.util.Collection;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;


public class UpdateRootOrderIdInManualInteractionEntries extends UpdateJustVersion {

  public UpdateRootOrderIdInManualInteractionEntries(Version oldVersion, Version newVersion, boolean needsRegenerate) {
    super(oldVersion, newVersion, needsRegenerate);
  }


  @Override
  protected void update() throws XynaException {
    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      UpdateGeneratedClasses.mockFactory();
      ODS ods = ODSImpl.getInstance();
      
      ods.registerStorable(ManualInteractionEntry.class);
      ods.registerStorable(OrderInstanceBackup.class);
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT); //for each connectionType?
      
      try {
        Collection<ManualInteractionEntry> mientries = con.loadCollection(ManualInteractionEntry.class);
        
        for(ManualInteractionEntry mientry : mientries) {
          Long orderId = mientry.getXynaOrderId();
          OrderInstanceBackup oib = new OrderInstanceBackup(orderId, 0);
          try {
            con.queryOneRow(oib);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            logger.warn("ManualInteractionEntry " + mientry.getID() + " has no entry in orderbackup!");
            continue;
          }
          mientry.setRootOrderId(oib.getRootId());
        }
        con.persistCollection(mientries);
        con.commit();
      } finally {
        con.closeConnection();
        ods.unregisterStorable(ManualInteractionEntry.class);
        ods.unregisterStorable(OrderInstanceBackup.class);
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }

}
