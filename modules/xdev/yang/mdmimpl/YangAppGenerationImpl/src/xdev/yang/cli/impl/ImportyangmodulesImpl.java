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
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Importapplication;
import com.gip.xyna.xmcp.xfcli.impl.ImportapplicationImpl;

import xdev.yang.ModuleCollectionGenerationParameter;
import xdev.yang.cli.generated.Importyangmodules;
import xdev.yang.impl.ModuleCollectionApp;
import xdev.yang.impl.ModuleCollectionApp.YangModuleApplicationData;
import xfmg.xfctrl.filemgmt.ManagedFileId;



public class ImportyangmodulesImpl extends XynaCommandImplementation<Importyangmodules> {

  private static Logger logger = CentralFactoryLogging.getLogger(ImportyangmodulesImpl.class);


  public void execute(OutputStream statusOutputStream, Importyangmodules payload) throws XynaException {

    Boolean inputIsZip = payload.getPath().endsWith(".zip");
    File inputFile = inputIsZip ? new File(payload.getPath()) : createTempZipFile(payload.getPath());

    FileManagement fileMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    String inputFileId = null;
    try (FileInputStream is = new FileInputStream(inputFile)) {
      inputFileId = fileMgmt.store("yang", inputFile.getAbsolutePath(), is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    String appName = payload.getApplicationName();
    ModuleCollectionGenerationParameter genParameter = new ModuleCollectionGenerationParameter();
    genParameter.setApplicationName(appName);
    genParameter.setApplicationVersion(payload.getVersionName());
    genParameter.setDataTypeFQN(payload.getFqDatatypeName());
    genParameter.setFileID(new ManagedFileId(inputFileId));

    String appFileId = null;
    try (YangModuleApplicationData appData = ModuleCollectionApp.createModuleCollectionApp(genParameter)) {
      writeToCommandLine(statusOutputStream, appName + " ManagedFileId: " + appData.getId() + " ");
      appFileId = appData.getId();
    } catch (IOException e) {
      writeToCommandLine(statusOutputStream, "Could not clean up temporary files for " + appName + "\n");
      if (logger.isWarnEnabled()) {
        logger.warn("Could not clean up temporary files for " + appName + "\n", e);
      }
    }

    fileMgmt.remove(inputFileId);
    if (!inputIsZip) {
      inputFile.delete();
    }

    ImportapplicationImpl importApp = new ImportapplicationImpl();
    Importapplication importPayload = new Importapplication();
    importPayload.setFilename(fileMgmt.retrieve(appFileId).getOriginalFilename());
    importApp.execute(statusOutputStream, importPayload);
    writeToCommandLine(statusOutputStream, "Done.");
  }


  /*
   * create zip file of input dir
   */
  private File createTempZipFile(String yangModulesPath) {
    File yangModulesDir = new File(yangModulesPath);
    File tmpFile = null;
    try {
      tmpFile = File.createTempFile("/tmp/yang_modules_", ".zip");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpFile))) {
      FileUtils.zipDir(yangModulesDir, zos, yangModulesDir);
    } catch (Ex_FileAccessException | IOException e) {
      throw new RuntimeException(e);
    }

    return tmpFile;
  }
}