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
package com.gip.xyna.xprc.xfractwfe.base;



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
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

   
    //type not found. check if classloader is current
    Class<?> classWithSameName = null;
    for (int i = 0; i < subclasses.length; i++) {
      if (subclasses[i].getName().equals(input.getClass().getName())) {
        classWithSameName = subclasses[i];
      }
    }
    
    if (classWithSameName == null) {
      //not a classloader bug, just a modelling problem: the type is missing in the choice.
      throw new RuntimeException("input of choice was not handled in workflow. type=" + input.getClass().getName() + ".");
    }
    
    if (classWithSameName.getClassLoader() instanceof ClassLoaderBase) {
      try {
        ((ClassLoaderBase) classWithSameName.getClassLoader()).checkClosed();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Workflow references old classloader for " + input.getClass().getName(), e);
      }
    }
    if (input.getClass().getClassLoader() instanceof ClassLoaderBase) {
      try {
        ((ClassLoaderBase) input.getClass().getClassLoader()).checkClosed();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Input of choice is loaded by old classloader: " + input.getClass().getName(), e);
      }
    }
    throw new RuntimeException("input of choice could not be handled. classloaders do not match: " + input.getClass().getClassLoader() + ", " + classWithSameName.getClassLoader());
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
