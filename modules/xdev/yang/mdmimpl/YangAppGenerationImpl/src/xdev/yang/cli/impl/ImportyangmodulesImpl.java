/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xdev.yang.ModuleCollectionGenerationParameter;
import xdev.yang.cli.generated.Importyangmodules;
import xdev.yang.impl.ModuleCollectionApp;
import xdev.yang.impl.ModuleCollectionApp.YangModuleApplicationData;
import xfmg.xfctrl.filemgmt.ManagedFileId;



public class ImportyangmodulesImpl extends XynaCommandImplementation<Importyangmodules> {

  private static Logger logger = CentralFactoryLogging.getLogger(ImportyangmodulesImpl.class);


  public void execute(OutputStream statusOutputStream, Importyangmodules payload) throws XynaException {

    File tmpFile;
    File yangDir = new File(payload.getPath());
    try {
      tmpFile = File.createTempFile("/tmp/yang_modules_", ".zip");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpFile))) {
      FileUtils.zipDir(yangDir, zos, yangDir);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    FileManagement fileMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    String id = null;
    try (FileInputStream is = new FileInputStream(tmpFile)) {
      id = fileMgmt.store("yang", tmpFile.getAbsolutePath(), is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    String appName = payload.getApplicationName();
    ModuleCollectionGenerationParameter genParameter = new ModuleCollectionGenerationParameter();
    genParameter.setApplicationName(appName);
    genParameter.setApplicationVersion(payload.getApplicationVersion());
    genParameter.setDataTypeFQN(payload.getFqDatatypeName());
    genParameter.setFileID(new ManagedFileId(id));
    try (YangModuleApplicationData appData = ModuleCollectionApp.createModuleCollectionApp(genParameter)) {
      writeToCommandLine(statusOutputStream, appName + " ManagedFileId: " + appData.getId() + " ");
    } catch (IOException e) {
      writeToCommandLine(statusOutputStream, "Could not clean up temporary files for " + appName);
      if (logger.isWarnEnabled()) {
        logger.warn("Could not clean up temporary files for " + appName, e);
      }
    }

    writeToCommandLine(statusOutputStream, "Done.");
  }
}