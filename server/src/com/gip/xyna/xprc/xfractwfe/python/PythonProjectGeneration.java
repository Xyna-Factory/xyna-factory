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
package com.gip.xyna.xprc.xfractwfe.python;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;



public class PythonProjectGeneration {

  protected transient Logger logger;


  public InputStream getPythonServiceImplTemplate(String baseDir, String fqClassNameDOM, Long revision,
                                                  boolean deleteServiceImplAfterStreamClose)
      throws XynaException {
    File tempdir = new File(baseDir);
    String tempdirPath = tempdir.getAbsolutePath();

    writePythonMdm(tempdirPath, revision);
    writeInitPy(tempdirPath);
    writeImplPy(tempdirPath, fqClassNameDOM);

    try {
      File f = new File(fqClassNameDOM + "_" + getDateSuffix() + ".zip");
      while (f.exists()) {
        f = new File(fqClassNameDOM + "_" + getDateSuffix() + ".zip");
      }

      return XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getEclipseProjectTemplateFileProvider()
          .getHandledInputStreamFromFile(f, tempdir, deleteServiceImplAfterStreamClose);

    } finally {
      if (tempdir.exists()) {
        if (!FileUtils.deleteDirectory(tempdir)) {
          logger.warn("could not delete directory " + tempdir + ".");
        }
      }
    }
  }


  private void writePythonMdm(String tempdirPath, Long revision) {
    String mdmpy = XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement().createPythonMdm(revision, false, false);
    String outFileName = tempdirPath + File.separator + "mdm.py";
    writeProjectFile(outFileName, mdmpy);

  }


  private void writeInitPy(String tempdirPath) {
    String outFileName = tempdirPath + File.separator + "__init__.py";
    writeProjectFile(outFileName, "");
  }


  private void writeImplPy(String tempdirPath, String fqClassNameDOM) {
    String outFileName = tempdirPath + File.separator + fqClassNameDOM + "Impl.py";
    writeProjectFile(outFileName, "");
  }


  private void writeProjectFile(String outFileName, String fileContent) {
    try {
      Files.write(Paths.get(outFileName), fileContent.getBytes(), StandardOpenOption.CREATE);
    } catch (IOException e) {
      logger.error("Could not create file " + outFileName, e);
    }
  }


  private String getDateSuffix() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    return sdf.format(new Date());
  }
}
