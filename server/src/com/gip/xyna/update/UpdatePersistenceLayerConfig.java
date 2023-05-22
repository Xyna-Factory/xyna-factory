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



import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.update.outdatedclasses_6_1_2_0.User;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.securestorage.SecuredStorable;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xsched.OrderSeriesManagementStorable;



public class UpdatePersistenceLayerConfig extends Update {

  @Override
  protected Version getAllowedVersionForUpdate() {
    return new Version("3.0.0.10");
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return new Version("3.0.0.11");
  }


  @Override
  protected void update() throws XynaException {

    ODS ods = ODSImpl.getInstance();

    // the persistence layer IDs are set to the values that they where set to at the time this update was written
    final long memoryPersistenceLayerId = ods.getMemoryPersistenceLayerID();
    final long javaPersistenceLayerId = ods.getJavaPersistenceLayerID();
    final long devNullPersistenceLayerId = ods.getDevNullPersistenceLayerID();

    long memInstanceId = ods.instantiatePersistenceLayerInstance(memoryPersistenceLayerId, XynaProcessing.DEFAULT_NAME,
                                                                 ODSConnectionType.DEFAULT, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, memInstanceId);

    //orderarchive config in orderarchive-klasse.

    long javaInstanceId = ods.instantiatePersistenceLayerInstance(javaPersistenceLayerId, XynaProcessing.DEFAULT_NAME,
                                                                  ODSConnectionType.DEFAULT, new String[0]);

    ods.registerStorable(OrderSeriesManagementStorable.class);
    ods.setPersistenceLayerForTable(javaInstanceId, new OrderSeriesManagementStorable().getTableName(), null);

    ods.registerStorable(User.class);
    ods.setPersistenceLayerForTable(javaInstanceId, new User().getTableName(), null);

    ods.registerStorable(Role.class);
    ods.setPersistenceLayerForTable(javaInstanceId, new Role().getTableName(), null);

    ods.registerStorable(Right.class);
    ods.setPersistenceLayerForTable(javaInstanceId, new Right().getTableName(), null);

    ods.registerStorable(SecuredStorable.class);
    ods.setPersistenceLayerForTable(javaInstanceId, new SecuredStorable().getTableName(), null);

    ods.registerStorable(OrderInstanceDetails.class);
    ods.registerStorable(OrderInstanceBackup.class);
    String orderInstanceTableName = new OrderInstance().getTableName();
    if (!ods.isTableRegistered(ODSConnectionType.DEFAULT, orderInstanceTableName)) {
      PersistenceLayerInstanceBean pli = ods.getDefaultPersistenceLayerInstance(ODSConnectionType.DEFAULT);
      ods.setPersistenceLayerForTable(pli.getPersistenceLayerInstanceID(), orderInstanceTableName, null);
    }

    String orderInstanceBackupName = new OrderInstanceBackup().getTableName();
    if (!ods.isTableRegistered(ODSConnectionType.DEFAULT, orderInstanceBackupName)) {
      long pliDefaultId = ods.instantiatePersistenceLayerInstance(memoryPersistenceLayerId,
                                                                  XynaProcessing.DEFAULT_NAME,
                                                                  ODSConnectionType.DEFAULT, new String[0]);
      ods.setPersistenceLayerForTable(pliDefaultId, new OrderInstanceBackup().getTableName(), null);
    }

    if (!ods.isTableRegistered(ODSConnectionType.HISTORY, orderInstanceTableName)) {
      long pliHistoryId = ods.instantiatePersistenceLayerInstance(devNullPersistenceLayerId,
                                                                  XynaProcessing.DEFAULT_NAME,
                                                                  ODSConnectionType.HISTORY, new String[0]);
      ods.setPersistenceLayerForTable(pliHistoryId, new OrderInstanceDetails().getTableName(), null);
    }

  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return false;
  }

}
