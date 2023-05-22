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

package com.gip.xyna.xfmg.xfctrl.classloading;



import java.net.URL;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;



public class WFClassLoader extends MDMClassLoaderXMLBase {

  private static Logger logger = CentralFactoryLogging.getLogger(WFClassLoader.class);

  private String fqClassName;


  protected WFClassLoader(String fqClassName, String originalXmlPath, String originalXmlName, Long revision) {
    super(ClassLoaderType.WF, fqClassName, new URL[] {MDMClassLoader.getClassUrl(fqClassName, revision)},
          new ClassLoaderBase[] {XynaClassLoader.getInstance()}, originalXmlPath, originalXmlName, revision);
    this.fqClassName = fqClassName;

    if (logger.isDebugEnabled())
      logger.debug("created " + this);

  }


  /**
   * Loads a workflow class, thereby first trying to load a java class. If the class is not found this way, the parent
   * class loaders are asked and the own classes are checked. If still the class is not found
   * @param name The name of the workflow class to load
   * @param resolve Whether to call {@link java.lang.ClassLoader#resolveClass(Class)} at the end
   */
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> c = findLoadedClass(name);

    if (c == null) {
      // erst bei parent schauen, dann bei sich selbst
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
      if (c == null) {
        boolean isResponsible = isResponsible(name);
        if (!isResponsible) { //innere klassen heissen <classname>$<innerclassname>
          c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                          .loadMDMClass(name, false, null, null, getRevision());
          if (c != null) {
            ((ClassLoaderBase) c.getClassLoader())
                                .addDependencyToReloadIfThisClassLoaderIsRecreated(fqClassName, getRevision(), ClassLoaderType.WF, ClassLoadingDependencySource.Classloading);
          } else {
            c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                            .loadExceptionClass(name, false, null, null, getRevision());
            if (c != null) {
              ((ClassLoaderBase) c.getClassLoader())
                                  .addDependencyToReloadIfThisClassLoaderIsRecreated(fqClassName, getRevision(), ClassLoaderType.WF, ClassLoadingDependencySource.Classloading);
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
      logger.debug("++++" + name + "++++ was loaded by ####" + c.getClassLoader() + "####");
    }

    addPreviouslyLoadedClass(name, c);
    setHasBeenUsed();
    return c;

  }


  public String toString() {
    return super.toString() + "-" + getRevision() + "-" + fqClassName;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected void deployWhenReload(String className) throws XFMG_ClassLoaderRedeploymentException {
    try {
      XynaProcess.deploy((Class<? extends XynaProcess>) loadClass(className));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("wf class " + className + " not found", e);
    } catch (XynaException e) {
      throw new XFMG_ClassLoaderRedeploymentException("WF", className, e);
    }
      
    super.deployWhenReload(className);    
  }


  @SuppressWarnings("unchecked")
  @Override
  protected void undeployWhenReload(String className) throws XFMG_ClassLoaderRedeploymentException {
    ((XynaFractalWorkflowEngine) XynaFactory.getInstance().getProcessing().getWorkflowEngine()).getProcessManager()
        .clearInstancePool(new FractalWorkflowDestination(className), this.getRevision());
    
    try {
      XynaProcess.undeploy((Class<? extends XynaProcess>) loadClass(className));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("wf class " + className + " not found", e);
    } catch (XynaException e) {
      throw new XFMG_ClassLoaderRedeploymentException("WF", className, e);
    }
    super.undeployWhenReload(className);
  }

}
