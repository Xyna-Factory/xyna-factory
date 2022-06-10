/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna.xfmg.xfctrl.classloading;



import com.gip.xyna.utils.exceptions.XynaException;



public class ClassLoaderDispatcherFactory {

  private volatile ClassLoaderDispatcher impl;
  private static ClassLoaderDispatcherFactory instance = new ClassLoaderDispatcherFactory();


  public static ClassLoaderDispatcherFactory getInstance() {
    return instance;
  }


  private ClassLoaderDispatcherFactory() {
  }


  public ClassLoaderDispatcher getImpl() {
    if (impl == null) {
      synchronized (this) {
        try {
          impl = new ClassLoaderDispatcher();
        } catch (XynaException e) {
          throw new RuntimeException("Failed to create " + ClassLoaderDispatcher.class.getSimpleName(), e);
        }
      }
    }
    return impl;
  }


  public void setImpl(ClassLoaderDispatcher newImpl) {
    impl = newImpl;
  }

}
