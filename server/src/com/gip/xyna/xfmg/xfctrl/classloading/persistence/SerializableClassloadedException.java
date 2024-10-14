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
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.XynaClassLoader;



public class SerializableClassloadedException implements Serializable {

  private static final long serialVersionUID = -2654853515105468390L;

  private SerializableClassloadedObject serializableObject;


  public SerializableClassloadedException(final Throwable t) {

    Throwable tCopy = t;

    if (t == null) {
      return;
    }

    Set<ClassLoader> set = new HashSet<ClassLoader>();

    while (tCopy != null) {

      if (tCopy.getClass().getClassLoader() instanceof ClassLoaderBase
          && !(tCopy.getClass().getClassLoader() instanceof XynaClassLoader)) {
        set.add(tCopy.getClass().getClassLoader());
      }
      tCopy = tCopy.getCause();
    }

    serializableObject = new SerializableClassloadedObject(t, set.toArray(new ClassLoader[] {}));
  }


  public Throwable getThrowable() {
    if (serializableObject == null) {
      return null;
    }
    return (Throwable) serializableObject.getObject();
  }

}
