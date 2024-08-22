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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.PythonOperation;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;



public class PythonProjectGeneration {

  protected transient Logger logger;


  public InputStream getPythonServiceImplTemplate(String baseDir, String fqClassNameDOM, Long revision,
                                                  boolean deleteServiceImplAfterStreamClose)
      throws XynaException {
    File tempdir = new File(baseDir);
    String tempdirPath = tempdir.getAbsolutePath();

    writePythonMdm(tempdirPath, revision);
    writeInitPy(tempdirPath);
    writeImplPy(tempdirPath, fqClassNameDOM, revision);

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
    String mdmpy = XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement().createPythonMdm(revision, true, false);
    String outFileName = tempdirPath + File.separator + "mdm.py";
    writeProjectFile(outFileName, mdmpy);

  }


  private void writeInitPy(String tempdirPath) {
    String outFileName = tempdirPath + File.separator + "__init__.py";
    writeProjectFile(outFileName, "");
  }


  private void writeImplPy(String tempdirPath, String fqClassNameDOM, Long revision) {
    String outFileName = tempdirPath + File.separator + convertToPythonFqn(fqClassNameDOM) + "Impl.py";
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

    String pythonClassName = convertToPythonFqn(fqClassNameDOM);
    sb.append("import mdm\n\n");
    sb.append("class " + pythonClassName + "Impl:\n");
    sb.append("  def __init__(self, this: " + pythonClassName + "):\n");
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
    sb.append("    # TODO Auto-generated method stub\n");
    sb.append("    pass\n\n");
  }


  private List<MethodInformation> loadXynaMethodInfo(String fqn, Long revision) {
    List<Operation> operations = null;
    try {
      DomOrExceptionGenerationBase doe = DOM.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
      doe.parse(false);
      operations = ((DOM) doe).getOperations();
    } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException
        | XPRC_MDMDeploymentException e) {
      throw new RuntimeException(e);
    }

    List<MethodInformation> result = new ArrayList<MethodInformation>();
    for (Operation op : operations) {
      // TODO Abfrage erweitern && op.implementedInLib() 
      if (op instanceof PythonOperation) {
        MethodInformation info = new MethodInformation();
        info.isStatic = op.isStatic();
        info.name = op.getNameWithoutVersion();
        info.returnType = createReturnTypeFromOutputVars(op.getOutputVars());
        info.argumentsWithTypes = createArgumentsWithTypes(op.getInputVars());
        result.add(info);
      }
    }

    return result;
  }


  private String createReturnTypeFromOutputVars(List<AVariable> vars) {
    if (vars.isEmpty()) {
      return null;
    }
    if (vars.size() == 1) {
      AVariable avar = vars.get(0);
      return getPythonTypeOfVariable(avar);
    }
    return String.format("tuple[%s]", String.join(", ", vars.stream().map(this::getPythonTypeOfVariable).collect(Collectors.toList())));
  }


  private String getPythonTypeOfVariable(AVariable avar) {
    String type;
    if (avar.isJavaBaseType()) {
      type = primitive_types_mapping.getOrDefault(avar.getJavaTypeEnum(), "any");
    } else {
      type = "'" + convertToPythonFqn(avar.getOriginalPath() + "." + avar.getOriginalName()) + "'";
    }
    if (avar.isList()) {
      type = "list[" + type + "]";
    }
    return type;
  }


  private List<Pair<String, String>> createArgumentsWithTypes(List<AVariable> inputVars) {
    List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
    if (inputVars == null || inputVars.isEmpty()) {
      return null;
    }
    for (AVariable avar : inputVars) {
      result.add(new Pair<String, String>(avar.getVarName(), getPythonTypeOfVariable(avar)));
    }
    return result;
  }


  private String getDateSuffix() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    return sdf.format(new Date());
  }


  private String convertToPythonFqn(String fqn) {
    return fqn.replace('.', '_');
  }


  private static class MethodInformation {

    private String name;
    private String returnType;
    private boolean isStatic;
    private List<Pair<String, String>> argumentsWithTypes;
  }


  private static final Map<PrimitiveType, String> primitive_types_mapping = setupPrimitiveTypes();


  private static Map<PrimitiveType, String> setupPrimitiveTypes() {
    Map<PrimitiveType, String> result = new HashMap<>();
    result.put(PrimitiveType.BOOLEAN, "bool");
    result.put(PrimitiveType.BOOLEAN_OBJ, "bool");
    result.put(PrimitiveType.BYTE, "bytes");
    result.put(PrimitiveType.BYTE_OBJ, "bytes");
    result.put(PrimitiveType.DOUBLE, "decimal.Decimal");
    result.put(PrimitiveType.DOUBLE_OBJ, "decimal.Decimal");
    result.put(PrimitiveType.EXCEPTION, "XynaException");
    result.put(PrimitiveType.INT, "int");
    result.put(PrimitiveType.INTEGER, "int");
    result.put(PrimitiveType.LONG, "int");
    result.put(PrimitiveType.LONG_OBJ, "int");
    result.put(PrimitiveType.STRING, "str");
    result.put(PrimitiveType.VOID, "None");
    return result;
  }

}
