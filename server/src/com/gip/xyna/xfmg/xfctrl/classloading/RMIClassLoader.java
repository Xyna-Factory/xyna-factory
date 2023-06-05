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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.RegisteringClassLoader;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ReplaceableClassLoader;
import com.gip.xyna.xmcp.RMIChannelImplSessionExtension;


public class RMIClassLoader extends ClassLoaderBase implements RegisteringClassLoader, ReplaceableClassLoader {

  private static Logger logger = CentralFactoryLogging.getLogger(RMIClassLoader.class);

  private Set<String> rmiImplClassNames = new HashSet<String>();

  public RMIClassLoader(Long revision) {
    super(ClassLoaderType.RMI, "single rmi classloader", XynaClassLoader.getInstance().getURLs(),
          new ClassLoaderBase[] {XynaClassLoader.getInstance()}, revision);
    rmiImplClassNames.add(RMIChannelImplSessionExtension.class.getName());
  }

  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    checkClosed();
    Class<?> c = findLoadedClass(name);

    if (c == null) {
      // erst bei parent schauen, dann bei sich selbst ausser es ist rmichannelimpl      
      boolean isRMIImplClass;
      synchronized (rmiImplClassNames) {
        isRMIImplClass = rmiImplClassNames.contains(name);
        if (!isRMIImplClass) {
          for (String rmiImplClassName : rmiImplClassNames) {
            if (name.startsWith(rmiImplClassName + "$")) {
              isRMIImplClass = true;
              break;
            }
          }
        }
      }
      if (!isRMIImplClass) {
        int idx = 0;
        ClassLoaderBase[] pars = getParents();
        while (c == null && idx < pars.length) {
          try {
            c = pars[idx].loadClass(name);
          } catch (ClassNotFoundException e) {
            // ignorieren
          }
          idx++;
        }
      }
      if (c == null) {
        ClassNotFoundException oldEx = null;
        try {
          c = findClass(name);
        } catch (ClassNotFoundException e) {
          oldEx = e;
          // ignorieren
        }
        if (c == null) {
          try {
            c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                    .loadMDMClass(name, false, null, null, getRevision());
          } catch (ClassNotFoundException e) {
            // ignorieren
          }

          if (c == null) {
            c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                    .loadExceptionClass(name, false, null, null, getRevision());
          }

          if (c == null) {
            throw new ClassNotFoundException(name + " not found", oldEx);
          }
        }
      }
    }
    if (resolve) {
      resolveClass(c);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(name + "++++ was loaded by ####" + c.getClassLoader() + "####");
    }

    return c;
  }


  public void register(String fqClassname) {
    synchronized (rmiImplClassNames) {
      rmiImplClassNames.add(fqClassname);
    }
  }

  @Override
  public ClassLoaderBase replace() {
    RMIClassLoader newCl = new RMIClassLoader(getRevision());
    synchronized (rmiImplClassNames) {
      for (String rmiImplClass : rmiImplClassNames) {
        newCl.register(rmiImplClass);
      }
    }
    return newCl;
  }

}
