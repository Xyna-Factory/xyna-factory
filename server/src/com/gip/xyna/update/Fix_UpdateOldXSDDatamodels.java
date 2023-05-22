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



import java.util.Collection;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelSpecificStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelStorable;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;



public class Fix_UpdateOldXSDDatamodels extends UpdateJustVersion {

  public Fix_UpdateOldXSDDatamodels(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }


  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(DataModelStorable.class);
    ods.registerStorable(DataModelSpecificStorable.class);
    try {
      ODSConnection c = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        Collection<DataModelStorable> dmss = c.loadCollection(DataModelStorable.class);

        for (DataModelStorable dms : dmss) {
          if (dms.getDataModelType().equals("XSD")) {
            DataModelSpecificStorable dmSpecific = new DataModelSpecificStorable();
            dmSpecific.setFqNameIndex(dms.getDataModelPrefix() + "." +  dms.getLabel() + "_"  + "%0%." + "distributeToWorkspaces");
            
            try {
              c.queryOneRow(dmSpecific);
              c.deleteOneRow(dmSpecific);
              dmSpecific.setFqName(dms.getFqName());
              dmSpecific.setFqNameIndex(dms.getFqName() + "_" + "%0%." + "distributeToWorkspaces");
              c.persistObject(dmSpecific);
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              // everything fine
            }
          }
        }
        
        c.commit();
      } finally {
        c.closeConnection();
      }
    } finally {
      ods.unregisterStorable(DataModelStorable.class);
      ods.unregisterStorable(DataModelSpecificStorable.class);
    }
  }


}
