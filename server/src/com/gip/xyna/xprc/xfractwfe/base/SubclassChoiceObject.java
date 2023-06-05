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
package com.gip.xyna.xprc.xfractwfe.base;



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_NoInputSpecifiedForChoice;



public class SubclassChoiceObject {

  private static final Logger logger = CentralFactoryLogging.getLogger(SubclassChoiceObject.class);


  public SubclassChoiceObject() {
  }


  public static void decide(Object input, Class<?>[] subclasses, ChoiceLane[] lanes) throws XynaException {

    if (input == null) {
      throw new XPRC_NoInputSpecifiedForChoice();
    }

    if (logger.isTraceEnabled()) {
      traceClassLoader("Input", input.getClass());
      for (int i = 0; i < subclasses.length; i++) {
        traceClassLoader("Choice class", subclasses[i]);
      }
    }
    for (int i = 0; i < subclasses.length; i++) {
      if (subclasses[i].isAssignableFrom(input.getClass())) {
        lanes[i].execute();
        return;
      }
    }

    throw new RuntimeException("input of choice was not handled in workflow. type=" + input.getClass().getName() + ".");

  }


  private static void traceClassLoader(String prefix, Class<?> c) {
    if (c != null) {
      logger.trace(new StringBuilder(prefix).append(": ").append(c.getName()).append(" => ").append(c.getClassLoader())
          .toString());
    } else {
      logger.trace(prefix + ": " + null);
    }
  }

}
