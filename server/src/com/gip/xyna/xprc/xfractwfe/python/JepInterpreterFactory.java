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



import java.util.Collection;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



public class JepInterpreterFactory extends PythonInterpreterFactory {

  private static final Logger logger = CentralFactoryLogging.getLogger(JepInterpreterFactory.class);


  @Override
  public PythonInterpreter createInterperter(Long revision) {
    return new JepInterpreter();
  }


  @Override
  public void init() {
    try {
      String jepModulePath = System.getenv("jep.module.path");
      if (jepModulePath != null && !jepModulePath.isBlank()) {
        jep.MainInterpreter.setJepLibraryPath(jepModulePath);
        if (logger.isDebugEnabled()) {
          logger.debug("set jep library path to : " + jepModulePath);
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("jep library path not set!");
        }
      }
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Error during jep init. ", e);
      }
    }
  }


  @Override
  public void invalidateRevisions(Collection<Long> revisions) {
  }

}
