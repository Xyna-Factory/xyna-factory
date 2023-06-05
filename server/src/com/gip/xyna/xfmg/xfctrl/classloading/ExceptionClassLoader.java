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



import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;



public class ExceptionClassLoader extends MDMClassLoaderXMLBase {

  private static Logger logger = CentralFactoryLogging.getLogger(ExceptionClassLoader.class);


  /**
   * Creates a new exception classloader
   */
  protected ExceptionClassLoader(String fqClassName, String originalXmlPath, String originalXmlName, Long revision) {

    // FIXME no parents?
    super(ClassLoaderType.Exception, fqClassName, new URL[] {getClassUrl(fqClassName, revision)},
          new ClassLoaderBase[] {XynaClassLoader.getInstance()}, originalXmlPath, originalXmlName, revision);

    if (logger.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder("Created ").append(this).append(" with parents = [");
      //      for (SharedLibClassLoader slc : parents) {
      //        sb.append(slc.toString() + " ");
      //      }
      sb.append("]");
      logger.debug(sb.toString());
    }

  }


  private static URL getClassUrl(String fqClassName, Long revision) {
    URL url;
    try {
      url = new File(VersionManagement.getPathForRevision(PathType.XMOMCLASSES, revision) + Constants.fileSeparator).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } //TODO performance
    return url;
  }


  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder().append(this).append(" trying to load ").append(name).toString());
    }

    Class<?> c = findLoadedClass(name);
    if (c == null) {
      // erst bei parent schauen, dann bei sich selbst
      int idx = 0;
      ClassLoaderBase[] pars = getParents();
      while (c == null && idx < pars.length) {
        try {
          c = pars[idx].loadClass(name);
        }
        catch (ClassNotFoundException e) {
          // ignorieren
        }
        idx++;
      }
      if (c == null) {
        boolean isResponsible = isResponsible(name);
        if (!isResponsible) {
          c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                          .loadMDMClass(name, false, null, null, getRevision());
          if (c != null) {
            ((ClassLoaderBase) c.getClassLoader())
                                .addDependencyToReloadIfThisClassLoaderIsRecreated(getFqClassName(),
                                                                                   getRevision(),
                                                                                   ClassLoaderType.Exception, ClassLoadingDependencySource.Classloading);
          } else {
            c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                            .loadExceptionClass(name, false, null, null, getRevision());
            if (c != null) {
              ((ClassLoaderBase) c.getClassLoader())
                                  .addDependencyToReloadIfThisClassLoaderIsRecreated(getFqClassName(),
                                                                                     getRevision(),
                                                                                     ClassLoaderType.Exception, ClassLoadingDependencySource.Classloading);
            }
          }
        }
        if (c == null) {
          if (isResponsible) {
            checkClosed();
            try {
              c = findClass(name);
            } catch (ClassNotFoundException e) {
              throwClassNotFoundException(name, e);
            }
          }
          if (c == null) {
            throwClassNotFoundException(name, null);
          }
        }
      }
    }
    if (resolve) {
      resolveClass(c);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder("++++").append(name).append("++++ was loaded by ####").append(c.getClassLoader())
                      .append("####").toString());
    }

    setHasBeenUsed();
    return c;

  }

  
}
