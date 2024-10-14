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
package com.gip.xyna.xprc.xfractwfe.generation.compile;



import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;



public class Compilation {

  static final Logger logger = CentralFactoryLogging.getLogger(Compilation.class);
  static final String OPTION_NAME_PROCEED_ON_ERROR = "-proceedOnError";

  
  static JavaCompiler getCompiler(boolean proceedOnError) {
    Collection<JavaCompiler> compilers = getCompilers();
    for (JavaCompiler compiler : compilers) {
      if (proceedOnError && 
          compiler.isSupportedOption(OPTION_NAME_PROCEED_ON_ERROR) >= 0) {
        return compiler;
      }
    }
    // TODO use default or throw?
    return ToolProvider.getSystemJavaCompiler();
  }

  private static Collection<JavaCompiler> getCompilers() {
    Collection<JavaCompiler> compilers = new ArrayList<JavaCompiler>();
    JavaCompiler systemCompiler = ToolProvider.getSystemJavaCompiler();
    if (systemCompiler != null) {
      compilers.add(systemCompiler); 
    }
    ServiceLoader<JavaCompiler> loader = ServiceLoader.load(JavaCompiler.class);
    for (JavaCompiler javaCompiler : loader) {
      compilers.add(javaCompiler);
    }
    return compilers;
  }
  
  public static boolean proceedOnErrorPossible() {
    if (XynaProperty.TRY_PROCEED_ON_COMPILE_ERROR.get()) {
      Collection<JavaCompiler> compilers = getCompilers();
      for (JavaCompiler compiler : compilers) {
        if (compiler.isSupportedOption(OPTION_NAME_PROCEED_ON_ERROR) >= 0) {
          return true;
        }
      }
    }
    return false;
  }

  
}
