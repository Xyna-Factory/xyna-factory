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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xdelivery.CreateDeliveryItem;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Backup;



public class BackupImpl extends XynaCommandImplementation<Backup> {

  @Override
  public void execute(OutputStream statusOutputStream, Backup payload) throws XynaException {

    CommandControl.tryLock(CommandControl.Operation.PACKAGE_BUILD);
    try {
      CreateDeliveryItem bdi = new CreateDeliveryItem(new File(payload.getTargetFile()), statusOutputStream);
      bdi.setVerboseOutput(payload.getVerbose());
      bdi.setIncludeXynaComponents(payload.getIncludexynacomp());

      bdi.doBackup();
    } catch (IOException e) {
      throw new Ex_FileAccessException(payload.getTargetFile(), e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.PACKAGE_BUILD);
    }

  }

}
