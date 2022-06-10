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



import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderInformationStorable;



public class UpdateConfigureAbandonedOrdersPersistenceLayerToXmlShell extends Update {

  private final Version allowedForUpdate;
  private final Version afterUpdate;
  private final boolean mustRegenerateJava;


  public UpdateConfigureAbandonedOrdersPersistenceLayerToXmlShell(Version allowedForUpdate, Version afterUpdate,
                                                                  boolean regenerateDeployedClasses) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
    this.mustRegenerateJava = regenerateDeployedClasses;
  }


  @Override
  protected void update() throws XynaException {

    ODS ods = ODSImpl.getInstance();

    long existingPlId =
        ods.getPersistenceLayerInstanceId(ODSConnectionType.HISTORY, AbandonedOrderInformationStorable.class);

    long targetXmlShellPlID = -1;
    for (PersistenceLayerInstanceBean bean : ods.getPersistenceLayerInstances()) {
      if (bean.getConnectionTypeEnum() == ODSConnectionType.HISTORY
          && bean.getPersistenceLayerInstance().getClass().getName()
              .equals(ODSImpl.XYNA_XMLSHELL_PERSISTENCE_LAYER_FQ_CLASSNAME)) {
        if (targetXmlShellPlID == -1) {
          targetXmlShellPlID = bean.getPersistenceLayerInstanceID();
        }
        // trotzdem noch weitersuchen, um zu prüfen, ob die config evtl schon OK ist
        if (targetXmlShellPlID == existingPlId) {
          logger.debug("Abandoned orders have already been configured properly");
          return;
        }
      }
    }

    if (targetXmlShellPlID < 0) {
      throw new RuntimeException("Missing " + ODSImpl.XYNA_XMLSHELL_PERSISTENCE_LAYER_FQ_CLASSNAME);
    }

    ods.setPersistenceLayerForTable(targetXmlShellPlID, AbandonedOrderInformationStorable.TABLE_NAME, null);

  }


  @Override
  protected Version getAllowedVersionForUpdate() {
    return this.allowedForUpdate;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return this.afterUpdate;
  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return this.mustRegenerateJava;
  }

}
