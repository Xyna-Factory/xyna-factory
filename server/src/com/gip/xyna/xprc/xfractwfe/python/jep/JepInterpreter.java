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
package com.gip.xyna.xprc.xfractwfe.python.jep;



import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xprc.xfractwfe.python.PythonInterpreter;

import jep.Jep;
import jep.JepConfig;
import jep.MainInterpreter;



public class JepInterpreter implements PythonInterpreter {

  private static final Pattern PYTHON_JEP_VERSION = Pattern.compile("(?m)^__VERSION__\\s*=\\s*(['\"])([^'\"\\r\\n]+)\\1\\s*(?:#.*)?$");

  private Jep jep;

  JepInterpreter(ClassLoader classloader) {
    jep = new JepConfig().setClassLoader(classloader).createSubInterpreter();
  }


  static Path validateJepLibrary(String modulePath) {
    if (modulePath == null || modulePath.isBlank()) {
      throw new IllegalStateException("Cannot verify JEP compatibility: jep.module.path is not set. "
          + "Configure the absolute path to the native JEP library from the selected Python installation.");
    }
    try {
      Path library = Path.of(modulePath).toRealPath();
      if (!Files.isRegularFile(library)) {
        throw new IllegalStateException("Native JEP library is not a regular file: " + library);
      }
      String javaVersion = getJavaJepVersion();
      Path versionFile = library.resolveSibling("version.py");
      // Reading metadata must not import JEP or load its native library.
      Matcher matcher = PYTHON_JEP_VERSION.matcher(Files.readString(versionFile));
      if (!matcher.find()) {
        throw new IllegalStateException("Cannot determine the Python JEP version from " + versionFile);
      }
      String pythonVersion = matcher.group(2);
      if (!javaVersion.equals(pythonVersion)) {
        throw new IllegalStateException("JEP version mismatch: Java JEP version " + javaVersion + ", Python/native JEP version "
            + pythonVersion + " at " + library + ". Install jep==" + javaVersion
            + " in the selected Python installation and restart the factory.");
      }
      return library;
    } catch (IOException | InvalidPathException e) {
      throw new IllegalStateException("Cannot verify JEP compatibility for " + modulePath
          + ". The native library and its adjacent version.py must be readable.", e);
    }
  }


  static String getJavaJepVersion() throws IOException {
    try (InputStream input = MainInterpreter.class.getResourceAsStream("/META-INF/maven/black.ninia/jep/pom.properties")) {
      if (input == null) {
        throw new IOException("Java JEP version metadata is missing from the JEP JAR.");
      }
      Properties properties = new Properties();
      properties.load(input);
      String version = properties.getProperty("version");
      if (version == null || version.isBlank()) {
        throw new IOException("Java JEP version metadata does not contain a version.");
      }
      return version.trim();
    }
  }




  @Override
  public void close() {
    jep.close();
  }


  @Override
  public void exec(String script) {
    jep.exec(script);

  }


  @Override
  public Object get(String variableName) {
    return jep.getValue(variableName);

  }


  @Override
  public void set(String variableName, Object value) {
    jep.set(variableName, value);

  }


  @Override
  public void runScript(String path) {
    jep.runScript(path);
  }
}