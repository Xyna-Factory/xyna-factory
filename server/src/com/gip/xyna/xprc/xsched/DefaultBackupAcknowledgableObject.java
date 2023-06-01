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
package com.gip.xyna.xprc.xsched;



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.xpce.AbstractConnectionAwareAck;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;



public class DefaultBackupAcknowledgableObject extends AbstractConnectionAwareAck {

  private static final long serialVersionUID = -5081423723401874672L;
  private static final Logger logger = CentralFactoryLogging.getLogger(DefaultBackupAcknowledgableObject.class);


  public DefaultBackupAcknowledgableObject() {
    super(null);
  }


  public void acknowledgeSchedulerEntry(XynaOrderServerExtension xose)
      throws XPRC_OrderEntryCouldNotBeAcknowledgedException {

    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
          .backup(xose, BackupCause.ACKNOWLEDGED);
      xose.setHasBeenBackuppedInScheduler(true);
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to create backup for XynaOrder <" + xose.getId() + ">", e);
    }
  }

}
