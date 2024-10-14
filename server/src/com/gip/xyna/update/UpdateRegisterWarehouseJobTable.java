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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.XynaFactoryWarehouse;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.xwarehousejobs.WarehouseJob;


public class UpdateRegisterWarehouseJobTable extends Update {

  private final Version allowedForUpdate;
  private final Version versionAfterUpdate;

  public UpdateRegisterWarehouseJobTable(Version allowedForUpdate, Version versionAfterupdate) {
    this.allowedForUpdate = allowedForUpdate;
    this.versionAfterUpdate = versionAfterupdate;
  }

  @Override
  protected Version getAllowedVersionForUpdate() {
    return allowedForUpdate;
  }

  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return versionAfterUpdate;
  }

  @Override
  public boolean mustUpdateGeneratedClasses() {
    return true;
  }


  @Override
  protected void update() throws XynaException {

    ODS ods = ODSImpl.getInstance();
    long instanceId = ods.instantiatePersistenceLayerInstance(ods.getXmlPersistenceLayerID(),
                                                              XynaFactoryWarehouse.DEFAULT_NAME,
                                                              ODSConnectionType.DEFAULT,
                                                              new String[] {XynaFactoryWarehouse.DEFAULT_NAME});
    ods.setPersistenceLayerForTable(instanceId, WarehouseJob.TABLE_NAME, null);

  }

}
