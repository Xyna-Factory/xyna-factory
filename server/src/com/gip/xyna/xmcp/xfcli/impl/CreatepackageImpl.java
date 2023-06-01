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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Createpackage;



public class CreatepackageImpl extends XynaCommandImplementation<Createpackage> {

  @Override
  public void execute(OutputStream logOutputStream, Createpackage payload) throws XynaException {
    CommandControl.tryLock(CommandControl.Operation.PACKAGE_BUILD);
    try {
      FileInputStream packageDefinitionInputStream;
      try {
        packageDefinitionInputStream = new FileInputStream(new File(payload.getDeliveryItem()));
      } catch (FileNotFoundException e) {
        throw new Ex_FileAccessException(payload.getDeliveryItem(), e);
      }
      try {
        try {
          File targetFile = new File(payload.getTargetFile());
          if (!targetFile.exists()) {
            if (targetFile.getParentFile() != null) {
              targetFile.getParentFile().mkdirs();
            }
            targetFile.createNewFile();
          }
          FileOutputStream targetFileOutputStream = new FileOutputStream(targetFile);
          try {
            XynaFactory
                .getPortalInstance()
                .getXynaMultiChannelPortalPortal()
                .createDeliveryItem(packageDefinitionInputStream, targetFileOutputStream, logOutputStream, payload.getVerbose(),
                                    payload.getIncludexynacomp());
            targetFileOutputStream.flush();
          } finally {
            targetFileOutputStream.close();
          }
        } finally {
          packageDefinitionInputStream.close();
        }
      } catch (IOException e) {
        throw new Ex_FileWriteException(payload.getTargetFile(), e);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.PACKAGE_BUILD);
    }
  }

}
