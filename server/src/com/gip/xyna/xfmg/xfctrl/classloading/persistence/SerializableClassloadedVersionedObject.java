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
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;


/**
 * Befähigt die Serialisierung eines VersionedObjects als SerializableClassLoadedObject.
 * Typische Verwendung: Serverinternes Objekt Referenz auf modelliertes VersionedObject von modelliertem Objekt.
 * Beispiel: Datentyp im Server hat Membervariable die zur Laufzeit von abgeleiteten Typ sein kann, der modelliert ist.
 * 
 * @param <T> sollte entweder ein XynaObject sein, oder eine Liste von XynaObjects
 */
public class SerializableClassloadedVersionedObject<T> implements Serializable {

  private static final long serialVersionUID = 1L;
  private final SerializableClassloadedObject object;


  public SerializableClassloadedVersionedObject(VersionedObject<T> oldVersions) {
    if (oldVersions == null) {
      this.object = null;
    } else {
      this.object = new SerializableClassloadedObject(oldVersions, collectClassLoaders(oldVersions));
    }
  }


  private ClassLoader[] collectClassLoaders(VersionedObject<T> oldVersions) {
    List<Version<T>> versions = oldVersions.getAllVersions();
    List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
    for (Version<T> v : versions) {
      T t = v.object;
      if (t != null) {
        if (t instanceof List) {
          for (Object element : (List<?>)t) {
            if (element != null) {
              if (element.getClass().getClassLoader() instanceof ClassLoaderBase) {
                classLoaders.add(t.getClass().getClassLoader());
              }
            }
          }
        } else {
          if (t.getClass().getClassLoader() instanceof ClassLoaderBase) {
            classLoaders.add(t.getClass().getClassLoader());
          }
        }
      }
    }
    return classLoaders.toArray(new ClassLoader[classLoaders.size()]);
  }


  @SuppressWarnings("unchecked")
  public VersionedObject<T> getVersions() {
    if (object == null) {
      return null;
    }
    return (VersionedObject<T>) object.getObject();
  }


}
