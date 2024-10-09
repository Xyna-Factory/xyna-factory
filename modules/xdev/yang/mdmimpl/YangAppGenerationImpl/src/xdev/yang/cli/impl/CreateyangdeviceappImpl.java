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
package xdev.yang.cli.impl;



import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Importapplication;
import com.gip.xyna.xmcp.xfcli.impl.ImportapplicationImpl;

import xdev.yang.YangAppGenerationInputParameter;
import xdev.yang.cli.generated.Createyangdeviceapp;
import xdev.yang.impl.YangApplicationGeneration;
import xdev.yang.impl.YangApplicationGeneration.YangApplicationGenerationData;
import xfmg.xfctrl.filemgmt.ManagedFileId;



public class CreateyangdeviceappImpl extends XynaCommandImplementation<Createyangdeviceapp> {

  private static Logger logger = CentralFactoryLogging.getLogger(CreateyangdeviceappImpl.class);


  public void execute(OutputStream statusOutputStream, Createyangdeviceapp payload) throws XynaException {
    FileManagement fileMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    File inputFile = new File(payload.getPath());
    String inputFileId = null;
    try (FileInputStream is = new FileInputStream(inputFile)) {
      inputFileId = fileMgmt.store("yang", inputFile.getAbsolutePath(), is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    String appName = payload.getApplicationName();
    YangAppGenerationInputParameter genParameter =
        new YangAppGenerationInputParameter(appName, payload.getVersionName(), payload.getFqDatatypeName(), new ManagedFileId(inputFileId));

    String appFileId = null;
    try (YangApplicationGenerationData appData = YangApplicationGeneration.createDeviceApp(genParameter)) {
      writeToCommandLine(statusOutputStream, appName + " ManagedFileId: " + appData.getId() + " ");
      appFileId = appData.getId();
    } catch (IOException e) {
      writeToCommandLine(statusOutputStream, "Could not clean up temporary files for " + appName + "\n");
      if (logger.isWarnEnabled()) {
        logger.warn("Could not clean up temporary files for " + appName + "\n", e);
      }
    }

    fileMgmt.remove(inputFileId);

    ImportapplicationImpl importApp = new ImportapplicationImpl();
    Importapplication importPayload = new Importapplication();
    importPayload.setFilename(fileMgmt.retrieve(appFileId).getOriginalFilename());
    importApp.execute(statusOutputStream, importPayload);
    writeToCommandLine(statusOutputStream, "Done.");
  }

}
