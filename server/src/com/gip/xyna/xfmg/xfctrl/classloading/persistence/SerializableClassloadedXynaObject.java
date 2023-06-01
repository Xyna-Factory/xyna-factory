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
package com.gip.xyna.xfmg.xfctrl.classloading.persistence;



import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionContainer;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.XynaClassLoader;



/**
 * zum serialisieren von xynaobjects inkl classloader informationen die zum deserialisieren notwendig sind
 */
public class SerializableClassloadedXynaObject implements Serializable {

  private static final Logger logger = CentralFactoryLogging.getLogger(SerializableClassloadedXynaObject.class);

  private static final long serialVersionUID = -9143318016511612764L;
  private SerializableClassloadedObject serializableObject;


  public SerializableClassloadedXynaObject(final GeneralXynaObject xo) {
    if (xo == null) {
      serializableObject = null;
    } else {
      serializableObject = new SerializableClassloadedObject(xo, collectRelevantClassLoaders(xo)
                      .toArray(new ClassLoader[0]));
    }
  }


  private Collection<ClassLoader> collectRelevantClassLoaders(GeneralXynaObject xo) {
    Set<ClassLoader> set = new HashSet<ClassLoader>();
    getRelevantClassLoadersRecursively(xo, set);
    return set;
  }


  /**
   * Für Container und Listen genügt ein mdm classloader. Falls die liste auch abgeleitete klassen enthält, muss der
   * classloader die klassen durch delegation finden können.
   */
  private void getRelevantClassLoadersRecursively(GeneralXynaObject xo, Set<ClassLoader> classloaders) {

    if (xo == null) {
      return;
    }

    if (xo instanceof GeneralXynaObjectList) {
      GeneralXynaObjectList<?> xol = (GeneralXynaObjectList<?>) xo;
      if (xol.getContainedClass() != null && xol.getContainedClass().getClassLoader() instanceof ClassLoaderBase) {
        classloaders.add(xol.getContainedClass().getClassLoader());
      }
      for (int i = 0; i < xol.size(); i++) {
        // the list might start with nulls so take the first value that is non null
        if (xol.get(i) != null) {
          getRelevantClassLoadersRecursively(xol.get(i), classloaders);
        }
      }
    } else if (xo instanceof Container) {
      Container xoc = (Container) xo;
      for (int i = 0; i < xoc.size(); i++) {
        if (xoc.get(i) != null) {
          getRelevantClassLoadersRecursively(xoc.get(i), classloaders);
        }
      }
    } else if (xo instanceof Exception) {
      //mehrere classloader
      addClassloadersForException((Exception) xo, classloaders);
    } else if (xo instanceof XynaExceptionContainer) {
      addClassloadersForException(((XynaExceptionContainer) xo).getException(), classloaders);
    } else if (xo.getClass().getClassLoader() instanceof ClassLoaderBase) {
      classloaders.add(xo.getClass().getClassLoader());
    } else {
      // SchedulerBean or other internally loaded classes that are not reloadable. 
      // Achtung, es muss in solchen Objekten z.b. über writeObject sichergestellt werden, dass membervariablen korrekt behandelt werden
    }
  }


  private void addClassloadersForException(Throwable ex, Set<ClassLoader> classloaders) {
    while (ex != null) {

      if (ex.getClass().getClassLoader() instanceof ClassLoaderBase
                      && !(ex.getClass().getClassLoader() instanceof XynaClassLoader)) {
        classloaders.add(ex.getClass().getClassLoader());
      }
      ex = ex.getCause();
    }
  }


  public GeneralXynaObject getXynaObject() {
    if (serializableObject == null) {
      return null;
    }
    return (GeneralXynaObject) serializableObject.getObject();
  }

}
