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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.PythonOperation;
import com.gip.xyna.xprc.xfractwfe.python.PythonGeneration.MethodInformation;



public class PythonProjectGeneration {

  protected transient Logger logger;


  public InputStream getPythonServiceImplTemplate(String baseDir, String fqClassNameDOM, Long revision,
                                                  boolean deleteServiceImplAfterStreamClose)
      throws XynaException {
    File tempdir = new File(baseDir);
    String tempdirPath = tempdir.getAbsolutePath();

    try {
      XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement().exportPythonMdm(revision, tempdirPath);
    } catch (Exception e) {
      logger.error("Could not create file mdm.py.", e);
    }
    writeInitPy(tempdirPath);
    writeImplPy(tempdirPath, fqClassNameDOM, revision);

    try {
      File f = new File(fqClassNameDOM + "_" + PythonGeneration.getDateSuffix() + ".zip");
      while (f.exists()) {
        f = new File(fqClassNameDOM + "_" + PythonGeneration.getDateSuffix() + ".zip");
      }

      return XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getEclipseProjectTemplateFileProvider()
          .getHandledInputStreamFromFile(f, tempdir, deleteServiceImplAfterStreamClose);

    } finally {
      if (tempdir.exists()) {
        if (!FileUtils.deleteDirectory(tempdir)) {
          logger.warn("Could not delete directory " + tempdir + ".");
        }
      }
    }
  }


  private void writeInitPy(String tempdirPath) {
    String outFileName = tempdirPath + File.separator + "__init__.py";
    writeProjectFile(outFileName, "");
  }


  private void writeImplPy(String tempdirPath, String fqClassNameDOM, Long revision) {
    String outFileName = tempdirPath + File.separator + PythonGeneration.convertToPythonFqn(fqClassNameDOM) + "Impl.py";
    String fileContent = generateImplPy(fqClassNameDOM, revision);

    writeProjectFile(outFileName, fileContent);
  }


  private void writeProjectFile(String outFileName, String fileContent) {
    try {
      Files.write(Paths.get(outFileName), fileContent.getBytes(), StandardOpenOption.CREATE);
    } catch (IOException e) {
      logger.error("Could not create file " + outFileName, e);
    }
  }


  private String generateImplPy(String fqClassNameDOM, Long revision) {
    StringBuilder sb = new StringBuilder();

    String pythonClassName = PythonGeneration.convertToPythonFqn(fqClassNameDOM);
    sb.append("import mdm\n\n");
    sb.append("class " + pythonClassName + "Impl:\n");
    sb.append("  def __init__(self, this: mdm." + pythonClassName + "):\n");
    sb.append("    self.this = this\n\n");

    List<MethodInformation> methods = loadXynaMethodInfo(fqClassNameDOM, revision);

    for (MethodInformation info : methods) {
      generateServices(sb, info);
    }

    return sb.toString();
  }


  private void generateServices(StringBuilder sb, MethodInformation info) {
    sb.append("  def " + info.name + "(self ");
    if (info.argumentsWithTypes != null && !info.argumentsWithTypes.isEmpty()) {
      sb.append(", ");
      for (Pair<String, String> argument : info.argumentsWithTypes) {
        sb.append(argument.getFirst());
        sb.append(": " + argument.getSecond());
        sb.append(", ");
      }
      sb.setLength(sb.length() - 2); //remove last ", "
    }
    sb.append(")");
    if (info.returnType != null) {
      sb.append(" -> " + info.returnType);
    }
    sb.append(":\n");
    sb.append("    # TODO implementation\n");
    sb.append("    pass\n\n");
  }


  private List<MethodInformation> loadXynaMethodInfo(String fqn, Long revision) {
    DomOrExceptionGenerationBase doe;
    try {
      doe = DOM.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
      doe.parse(false);
    } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException
        | XPRC_MDMDeploymentException e) {
      throw new RuntimeException(e);
    }

    // TODO Abfrage erweitern && o.implementedInLib()
    List<Operation> operations =
        ((DOM) doe).getOperations().stream().filter(o -> o instanceof PythonOperation).collect(Collectors.toList());
    List<MethodInformation> result = PythonGeneration.loadOperations(operations, true);

    return result;
  }

}
