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


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;

/**
 * updated das orderarchive:
 * 1. alte WorkflowInstanceObjekte werden umgestellt auf OrderInstanceObjekte
 * 2. fertige aufträge wandern ins archiv
 */
public class UpdateConfigurationUseHistoryAndDefault extends Update {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateConfigurationUseHistoryAndDefault.class);

  private final Version allowedForUpdate;
  private final Version versionAfterUpdate;


  public UpdateConfigurationUseHistoryAndDefault(Version allowedForUpdate, Version versionAfterUpdate) {
    this.allowedForUpdate = allowedForUpdate;
    this.versionAfterUpdate = versionAfterUpdate;
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
  protected void update() throws XynaException {

    ODS ods = ODSImpl.getInstance();

    long targetDefaultId = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(),
                                                                   XynaFactoryManagement.DEFAULT_NAME,
                                                                   ODSConnectionType.DEFAULT, new String[0]);
    long targetHistoryId = ods.instantiatePersistenceLayerInstance(ods.getXmlPersistenceLayerID(),
                                                                   XynaFactoryManagement.DEFAULT_NAME,
                                                                   ODSConnectionType.HISTORY,
                                                                   new String[] {"Configuration"});

    ods.setPersistenceLayerForTable(targetHistoryId, XynaPropertyStorable.TABLE_NAME, null);

    ods.registerStorable(XynaPropertyStorable.class);
    ods.copy(XynaPropertyStorable.class, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
    ods.unregisterStorable(XynaPropertyStorable.class);

    ods.setPersistenceLayerForTable(targetDefaultId, XynaPropertyStorable.TABLE_NAME, null);
  }

  @Override
  public boolean mustUpdateGeneratedClasses() {
    return true;
  }

}
