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

package com.gip.xyna.xfmg.xfctrl.classloading;

import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;



public class ClassProvider {

  private Class<?> c;
  private Long revision = VersionManagement.REVISION_WORKINGSET;
  
  //es gibt nur classprovider bestimmter klassen
  private ClassProvider(Class<?> c) {
    this.c = c;
    if(c.getClassLoader() instanceof ClassLoaderBase) {
      ClassLoaderBase clb = (ClassLoaderBase)c.getClassLoader();
      revision = clb.getRevision();
    }
  }


  public static ClassProvider getClassProviderForXynaProcess(Class<? extends XynaProcess> c) {
    return new ClassProvider(c);
  }


  public static ClassProvider getClassProviderForXynaObject(Class<? extends XynaObject> c) {
    return new ClassProvider(c);
  }


  public static ClassProvider getClassProviderForTrigger(Class<? extends EventListener<?, ?>> c) {
    return new ClassProvider(c);
  }


  public static ClassProvider getClassProviderForFilter(Class<? extends ConnectionFilter<?>> c) {
    return new ClassProvider(c);
  }

  public Class<?> getContainedClass() {
    return c;
  }
  
  public Long getContainedClassRevision() {
    return revision;
  }
}
