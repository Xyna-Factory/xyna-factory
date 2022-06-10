/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.generation.compile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;

class CompileErrorCollector implements DiagnosticListener<JavaFileObject> {

  private final List<XPRC_CompileError> errors = new ArrayList<XPRC_CompileError>();
  private final InMemoryCompilationSet imcs;

  public CompileErrorCollector(InMemoryCompilationSet inMemoryCompilationSet) {
    this.imcs = inMemoryCompilationSet;
  }


  public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
      String msg = diagnostic.getMessage(Constants.DEFAULT_LOCALE);
      if( msg.contains("\n") ) {
        msg = "\n"+msg;
      }
      XPRC_CompileError ce = new XPRC_CompileError(getFqName(diagnostic), getSourceFileAndLine(diagnostic), msg);
      ce.setStackTrace(new StackTraceElement[]{});
      errors.add(ce);
      imcs.failed.put(getFqName(diagnostic), ce);
    }
    if (Compilation.logger.isDebugEnabled()) {
      if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
        Compilation.logger.debug("compile error : " + diagnostic.getMessage(Constants.DEFAULT_LOCALE));
      } else if (Compilation.logger.isTraceEnabled()) {
        Compilation.logger.trace("compiler report of type " + diagnostic.getKind().name());
        Compilation.logger.trace("message: " + diagnostic.getMessage(Constants.DEFAULT_LOCALE));
      }
      if (Compilation.logger.isTraceEnabled()) {
        Compilation.logger.trace("code: " + diagnostic.getCode());
        Compilation.logger.trace("position: " + diagnostic.getPosition());
        Compilation.logger.trace("start position: " + diagnostic.getStartPosition());
        Compilation.logger.trace("end position: " + diagnostic.getEndPosition());
      }
    }
  }


  private String getSourceFileAndLine(Diagnostic<? extends JavaFileObject> diagnostic) {
    return String.valueOf(getFqName(diagnostic)) + ":" + diagnostic.getLineNumber();
  }


  private String getFqName(Diagnostic<? extends JavaFileObject> diagnostic) {
    JavaFileObject fileObj = getSourceOrNull(diagnostic);
    if (fileObj == null) {
      String originatingFileName = getOriginatingFileNameOrNull(diagnostic);
      return transformSourceFileToFqName(originatingFileName);
    } else if (fileObj instanceof JavaSourceFromString) {
      JavaFileObject source = getSourceOrNull(diagnostic);
      if (source != null) {
        return ((JavaSourceFromString) source).getFqClassName();
      }
    }
    return "";
  }
  

  private JavaFileObject getSourceOrNull(Diagnostic<? extends JavaFileObject> diagnostic) {
    try {
      return diagnostic.getSource();
    } catch (NullPointerException e) {
      return null; //passiert aus bisher unerfindlichen gründen manchmal beim eclipsecompiler, vgl bug 24184
    }
  }
  
  private String getOriginatingFileNameOrNull(Diagnostic<? extends JavaFileObject> diagnostic) {
    try {
      JavaFileObject jfo = diagnostic.getSource();
      if (jfo == null) {
        for (Field field : diagnostic.getClass().getDeclaredFields()) {
          if (field.getName().endsWith("originatingFileName")) {
            field.setAccessible(true);
            char[] value = (char[]) field.get(diagnostic);
            String asString = new String(value);
            return asString;
          }
        }
        return null;
      } else {
        return jfo.getName();
      }
    } catch (NullPointerException | IllegalArgumentException | IllegalAccessException e) {
      return null; //passiert aus bisher unerfindlichen gründen manchmal beim eclipsecompiler, vgl bug 24184
    }
  }

  
  private final static String JAVA_FILE_SUFFIX = ".java";
  
  private String transformSourceFileToFqName(String originatingFileName) {
    String fqName = originatingFileName;
    if (fqName.endsWith(JAVA_FILE_SUFFIX)) {
      fqName = fqName.substring(0, fqName.length() - JAVA_FILE_SUFFIX.length());
    }
    if (fqName.startsWith(Constants.FILE_SEPARATOR)) {
      fqName = fqName.substring(Constants.FILE_SEPARATOR.length());
    }
    fqName = fqName.replaceAll("[" + Constants.FILE_SEPARATOR + "]", ".");
    return fqName;
  }

  public List<XPRC_CompileError> getCollectedErrors() {
    return errors;
  }

}