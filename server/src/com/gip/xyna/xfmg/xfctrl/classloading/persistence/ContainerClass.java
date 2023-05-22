/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class ContainerClass implements Serializable {

  private static final long serialVersionUID = 2979081048552636472L;

  private Serializable containedObject;

  public ContainerClass() {
  }


  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  /**
   * wird nur von {@link SerializableClassloadedObject} aufgerufen. deshalb muss die signatur nicht der vom normalen
   * readObject entsprechen, welches zu {@link Serializable} klassen geh�rt. 
   */
  /*
   * beim deserialisieren wird vom objectinputstream f�r das aufl�sen der klassen folgende funktion verwendet:
   *   private static native ClassLoader latestUserDefinedLoader();
   *   
   * diese schaut im stack weiter oben nach dem letzten element, welches einen classloader!=appclassloader verwendet.
   * der wird dann verwendet, um weitere klassen zu laden.
   */
  public void readObjectFromStream(ObjectInputStream in) throws IOException, ClassNotFoundException {
    containedObject = (Serializable) in.readObject();
  }


  public Serializable getObject() {
    return containedObject;
  }

}
