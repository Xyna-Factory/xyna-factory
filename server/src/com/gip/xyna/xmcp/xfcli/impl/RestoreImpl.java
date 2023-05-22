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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Restore;



public class RestoreImpl extends XynaCommandImplementation<Restore> {

  public void execute(OutputStream statusOutputStream, Restore payload) throws XynaException {
    CommandControl.tryLock(CommandControl.Operation.PACKAGE_INSTALL);
    try {
      FileInputStream deliveryItemInputStream = new FileInputStream(new File(payload.getSourceZipFile()));
      try {
        boolean noUpdateMdm = false;
        factory.getXynaMultiChannelPortalPortal().installDeliveryItem(deliveryItemInputStream, statusOutputStream,
                                                                      payload.getForceoverwrite(), noUpdateMdm,
                                                                      payload.getVerbose());
      } finally {
        deliveryItemInputStream.close();
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(payload.getSourceZipFile(), e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.PACKAGE_INSTALL);
    }
  }

}
