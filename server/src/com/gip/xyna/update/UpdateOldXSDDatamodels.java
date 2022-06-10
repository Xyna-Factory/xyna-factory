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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelSpecificStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelXmomTypeStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;



public class UpdateOldXSDDatamodels extends UpdateJustVersion {

  public UpdateOldXSDDatamodels(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }


  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(DataModelStorable.class);
    ods.registerStorable(DataModelSpecificStorable.class);
    ods.registerStorable(DataModelXmomTypeStorable.class);
    try {
      ODSConnection c = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        Collection<DataModelStorable> dmss = c.loadCollection(DataModelStorable.class);
        Collection<DataModelSpecificStorable> toModify = c.loadCollection(DataModelSpecificStorable.class);
        Collection<DataModelXmomTypeStorable> xmomToModify = c.loadCollection(DataModelXmomTypeStorable.class);
        Map<String, Collection<DataModelSpecificStorable>> specificsByFqName = new HashMap<String, Collection<DataModelSpecificStorable>>();
        for (DataModelSpecificStorable oldSpecific : toModify) {
          Collection<DataModelSpecificStorable> specificsForName = specificsByFqName.get(oldSpecific.getFqName());
          if (specificsForName == null) {
            specificsForName = new ArrayList<DataModelSpecificStorable>();
            specificsByFqName.put(oldSpecific.getFqName(), specificsForName);
          }
          specificsForName.add(oldSpecific);
        }
        Map<String, Collection<DataModelXmomTypeStorable>> xmomTypesByFqName = new HashMap<String, Collection<DataModelXmomTypeStorable>>();
        for (DataModelXmomTypeStorable oldXmomType : xmomToModify) {
          Collection<DataModelXmomTypeStorable> xmomTypesForName = xmomTypesByFqName.get(oldXmomType.getFqName());
          if (xmomTypesForName == null) {
            xmomTypesForName = new ArrayList<DataModelXmomTypeStorable>();
            xmomTypesByFqName.put(oldXmomType.getFqName(), xmomTypesForName);
          }
          xmomTypesForName.add(oldXmomType);
        }
        List<DataModelSpecificStorable> specificsToStore = new ArrayList<DataModelSpecificStorable>();
        List<DataModelSpecificStorable> specificsToDelete = new ArrayList<DataModelSpecificStorable>();
        List<DataModelStorable> modelsToStore = new ArrayList<DataModelStorable>();
        List<DataModelStorable> modelsToDelete = new ArrayList<DataModelStorable>();
        List<DataModelXmomTypeStorable> xmomTypesToStore = new ArrayList<DataModelXmomTypeStorable>();
        List<DataModelXmomTypeStorable> xmomTypesToDelete = new ArrayList<DataModelXmomTypeStorable>();
        for (DataModelStorable dms : dmss) {
          if (dms.getDataModelType().equals("XSD")) {
            // update xsd fqName
            modelsToDelete.add(dms);
            DataModelStorable update = new DataModelStorable();
            update.setAllFieldsFromData(dms);
            update.setFqName(dms.getDataModelPrefix() + ".v" + dms.getVersion().replaceAll("\\.", "_") + "." + dms.getLabel());
            modelsToStore.add(update);
            
            // add new specfic
            DataModelSpecific dmSpecific = new DataModelSpecific("%0%." + "distributeToWorkspaces", Boolean.TRUE.toString(), "");
            specificsToStore.addAll(DataModelSpecificStorable.toStorables(update.getFqName(), Collections.singletonList(dmSpecific)));
            
            // update old specifics
            Collection<DataModelSpecificStorable> specificsOfDM = specificsByFqName.get(dms.getFqName());
            if (specificsOfDM != null) {
              for (DataModelSpecificStorable oldSpecific : specificsOfDM) {
                specificsToDelete.add(oldSpecific);
                DataModelSpecificStorable newSpecific = new DataModelSpecificStorable();
                newSpecific.setAllFieldsFromData(oldSpecific);
                newSpecific.setFqName(update.getFqName());
                newSpecific.setFqNameIndex(update.getFqName() + "_" + newSpecific.getKey());
                specificsToStore.add(newSpecific);
              }
            }
            // update old xmomTypes
            Collection<DataModelXmomTypeStorable> xmomtypesOfDM = xmomTypesByFqName.get(dms.getFqName());
            if (xmomtypesOfDM != null) {
              for (DataModelXmomTypeStorable oldXmomType : xmomtypesOfDM) {
                xmomTypesToDelete.add(oldXmomType);
                DataModelXmomTypeStorable newXmomType = new DataModelXmomTypeStorable();
                newXmomType.setAllFieldsFromData(oldXmomType);
                newXmomType.setFqName(update.getFqName());
                newXmomType.setFqNameIndex(update.getFqName() + "_" + oldXmomType.getIndex());
                xmomTypesToStore.add(newXmomType);
              }
            }
          }
        }
        c.delete(specificsToDelete);
        c.delete(modelsToDelete);
        c.delete(xmomTypesToDelete);
        c.persistCollection(specificsToStore);
        c.persistCollection(modelsToStore);
        c.persistCollection(xmomTypesToStore);
        
        c.commit();
      } finally {
        c.closeConnection();
      }
    } finally {
      ods.unregisterStorable(DataModelStorable.class);
      ods.unregisterStorable(DataModelSpecificStorable.class);
      ods.unregisterStorable(DataModelXmomTypeStorable.class);
    }
  }


}
