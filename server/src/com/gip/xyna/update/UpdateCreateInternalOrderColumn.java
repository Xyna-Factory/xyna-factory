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

package com.gip.xyna.update;

import java.lang.reflect.Method;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;


public class UpdateCreateInternalOrderColumn extends Update {

  private final Version allowedForUpdate;
  private final Version afterUpdate;
  private final boolean mustRegenerate;


  public UpdateCreateInternalOrderColumn(Version allowedForUpdate, Version afterUpdate, boolean mustRegenerate) {
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

    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(OrderInstance.class);
    ODSConnection hisCon = ods.openConnection(ODSConnectionType.HISTORY);
    try {

      StringBuilder updateSqlCmd1 =
          new StringBuilder("update " + OrderInstance.TABLE_NAME + " set " + OrderInstance.COL_INTERNAL_ORDER
              + "=? where ");

      Parameter param = new Parameter(true);
      for (int i = 0; i < OrdertypeManagement.INTERNAL_ORDERTYPES.length; i++) {
        updateSqlCmd1.append(OrderInstance.COL_ORDERTYPE + "=? ");
        if (i < OrdertypeManagement.INTERNAL_ORDERTYPES.length - 1) {
          updateSqlCmd1.append("OR ");
        }
        param.add(OrdertypeManagement.INTERNAL_ORDERTYPES[i]);
      }

      String updateSqlCmd2 =
          "update " + OrderInstance.TABLE_NAME + " set " + OrderInstance.COL_INTERNAL_ORDER + "=? where "
              + OrderInstance.COL_INTERNAL_ORDER + " is null";
      try {
        PreparedCommand updateHistoryArchive = hisCon.prepareCommand(new Command(updateSqlCmd1.toString()));
        PreparedCommand updateHistoryArchive2 = hisCon.prepareCommand(new Command(updateSqlCmd2));
        hisCon.executeDML(updateHistoryArchive, param);
        hisCon.executeDML(updateHistoryArchive2, new Parameter(false));
        hisCon.commit();
      } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
        long memoryId = ods.getMemoryPersistenceLayerID();
        try {
          Method getLayer = ODSImpl.class.getDeclaredMethod("getPersistenceLayer", ODSConnectionType.class, String.class);
          getLayer.setAccessible(true);
          PersistenceLayerInstanceBean persLayer = (PersistenceLayerInstanceBean) getLayer.invoke(ods, ODSConnectionType.HISTORY, OrderInstance.TABLE_NAME);
          if (persLayer.getPersistenceLayerID() != memoryId) {  // we won't be able to update a mem-Ini, but as those aren't persistent...who cares?
            throw e;
          }
        } catch (Throwable t) {
          logger.debug(null, t);
          throw e;
        }
      }
    } finally {
      hisCon.closeConnection();
      ods.unregisterStorable(OrderInstance.class);
    }

  }

}
