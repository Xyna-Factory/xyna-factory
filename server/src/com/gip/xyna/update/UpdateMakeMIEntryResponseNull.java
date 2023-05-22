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

import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;


public class UpdateMakeMIEntryResponseNull extends Update {

  private final Version allowedForUpdate;
  private final Version afterUpdate;
  private final boolean mustRegenerate;


  public UpdateMakeMIEntryResponseNull(Version allowedForUpdate, Version afterUpdate, boolean mustRegenerate) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
    this.mustRegenerate = mustRegenerate;
  }


  @Override
  protected Version getAllowedVersionForUpdate() {
    return allowedForUpdate;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return afterUpdate;
  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return mustRegenerate;
  }


  @Override
  protected void update() throws XynaException {
    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      UpdateGeneratedClasses.mockFactory();
      ODS ods = ODSImpl.getInstance();
      ods.registerStorable(ManualInteractionEntry.class);
      ODSConnection defaultCon = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
  
        PreparedQuery<OrderCount> q =
            defaultCon.prepareQuery(new Query<OrderCount>("select count(*) from " + ManualInteractionEntry.TABLE_NAME,
                                                          OrderCount.getCountReader()));
        OrderCount existingEntries = defaultCon.queryOneRow(q, new Parameter());
  
        if (existingEntries.getCount() > 0) {
          // instead of deserializing them all (with all the problems, SerializableClassLoadedObject) just
          // assume that all pending MIs dont have been processed before.
          StringBuilder updateSqlCmd1 =
              new StringBuilder("update " + ManualInteractionEntry.TABLE_NAME + " set "
                  + ManualInteractionEntry.MI_COL_RESULT + "=null");
  
          PreparedCommand updateMIArchiveSQL = defaultCon.prepareCommand(new Command(updateSqlCmd1.toString()));
          defaultCon.executeDML(updateMIArchiveSQL, new Parameter());
          defaultCon.commit();
        }
  
      } finally {
        defaultCon.closeConnection();
        ods.unregisterStorable(ManualInteractionEntry.class);
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
    }

  }

}
