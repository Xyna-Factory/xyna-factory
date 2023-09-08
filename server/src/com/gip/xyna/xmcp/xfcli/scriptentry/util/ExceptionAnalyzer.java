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
package com.gip.xyna.xmcp.xfcli.scriptentry.util;



import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_WrappedCompileError;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;



public class ExceptionAnalyzer {


  public void analyzeException(MDMParallelDeploymentException e) {
    if (e.getNumberOfFailedObjects() < 1) {
      return; //nothing to be done
    }

    //since all objects are deployed together. they share the same ExceptionCause,
    //if exception occurs during compile.
    Throwable t = e.getFailedObjects().get(0).getExceptionCause();

    if (t instanceof XPRC_WrappedCompileError) {
      printCompileErrors((XPRC_WrappedCompileError) t);
      return;
    }

    //exception occurred before compile. Examine all exceptions individually.
    for (GenerationBase failedObject : e.getFailedObjects()) {
      Throwable th = failedObject.getExceptionCause();
      analyzeDeployException(th, failedObject);
    }

  }


  private void analyzeDeployException(Throwable t, GenerationBase gb) {

    //defined no or multiple unique identifiers
    if (t instanceof RuntimeException && t.getMessage().endsWith("unique identifier.")) {
      System.out.println("ERROR: " + t.getMessage() + " - check \"xprc.xfractwfe.generation.storable.xmom.interfaces\"");
      return;
    }

    //e.g. multiple members with the same name
    if (t instanceof XPRC_MDMDeploymentException) {
      XPRC_MDMDeploymentException ex = (XPRC_MDMDeploymentException) t;
      String clazz = ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : ex.getClass().getSimpleName();
      XynaException cause = ex.getCause() != null && ex.getCause() instanceof XynaException ? (XynaException) ex.getCause() : ex;
      System.out.println("ERROR: " + clazz + " at object " + ex.getFqXmlName() + " args: " + Arrays.asList(cause.getArgs()));
      return;
    }

    System.out.println("Unknown ERROR in " + gb.getFqClassName() + ": " + t);
    t.printStackTrace();

  }


  private void printCompileErrors(XPRC_WrappedCompileError ex) {
    List<XPRC_CompileError> exceptions = ex.getInnerExceptions();

    //group exceptions
    String fqn;
    String msg;
    String[] parts;
    String missingSymbol;
    String missingPackage;
    Set<ExceptionContainer> exceptionContainers = new HashSet<ExceptionContainer>();
    for (XPRC_CompileError error : exceptions) {
      fqn = error.getFqXmlName();
      msg = error.getArgs()[2];
      parts = msg.split("\n");
      if (parts.length > 1 && parts[1].contains("cannot find symbol")) {
        missingSymbol = parts[2].split("\\s+")[3];
        if (parts.length > 3 && parts[3].split("\\s+")[2].equals("package")) {
          missingPackage = parts[3].split("\\s+")[3];
          System.out.println("Missing: " + missingPackage + "." + missingSymbol + " required for " + fqn);
          ExceptionContainer container = new ExceptionContainer();
          container.fqn = fqn;
          container.type = "missing dependency";
          container.info = missingPackage + "." + missingSymbol;
          exceptionContainers.add(container);
        }
      } else {
        System.out.println("Unknown exception in " + fqn + ": " + msg);
      }
    }
    System.out.println("--- Exceptions: " + exceptionContainers.size());
    for (ExceptionContainer container : exceptionContainers) {
      System.out.println(container);
    }
  }


  private static class ExceptionContainer {

    String fqn;
    String type;
    String info;


    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ExceptionContainer)) {
        return false;
      }
      ExceptionContainer cast = (ExceptionContainer) obj;
      return Objects.equals(fqn, cast.fqn) && Objects.equals(type, cast.type) && Objects.equals(info, cast.info);
    }


    @Override
    public String toString() {
      return String.format("%s in %s: %s", type, fqn, info);
    }
  }
}
