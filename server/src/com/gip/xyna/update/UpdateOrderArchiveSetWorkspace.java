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



import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;



public class UpdateOrderArchiveSetWorkspace extends UpdateJustVersion {

  public UpdateOrderArchiveSetWorkspace(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }


  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    XynaFactoryBase oldInstance = XynaFactory.getInstance();

    UpdateGeneratedClasses.mockFactory();
    try {
      ods.registerStorable(OrderInstance.class);
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        try {
          PreparedCommand cmd =
              con.prepareCommand(new Command("update " + OrderInstance.TABLE_NAME + " set " + OrderInstance.COL_WORKSPACENAME + " = '"
                  + RevisionManagement.DEFAULT_WORKSPACE.getName() + "' where " + OrderInstance.COL_WORKSPACENAME + " IS NULL"));
          con.executeDML(cmd, new Parameter());
          con.commit();
        } catch (PersistenceLayerException e) {
          //orderarchive ist typischerweise auf datenbank konfiguriert. es gibt keine bekannten objekte, wo das anders ist
          //ausnahme: memory-persistencelayer -> dann gibt es hier halt eine warnung
          logger.warn("Could not update orderarchive. This is probably no critical error. "
              + "Existing orders have a null value in column 'workspace' which results in them not "
              + "being shown in process monitor when 'show all versions' is not selected.", e);
        }
      } finally {
        con.closeConnection();
        ods.unregisterStorable(OrderInstance.class);
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }

}
