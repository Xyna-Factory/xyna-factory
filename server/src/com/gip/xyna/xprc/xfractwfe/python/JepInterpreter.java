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

import java.util.Set;

import jep.Jep;
import jep.JepConfig;
import jep.NamingConventionClassEnquirer;

public class JepInterpreter implements PythonInterpreter {

  public JepInterpreter(ClassLoader classloader, Set<String> javaPackages) {
    NamingConventionClassEnquirer enquirer = new NamingConventionClassEnquirer(true);
    for (String javaPackage: javaPackages) {
      enquirer.addTopLevelPackageName(javaPackage);
    }
    jep = new JepConfig().setClassLoader(classloader).setClassEnquirer(enquirer).createSubInterpreter();
  }
  
  Jep jep;
  
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