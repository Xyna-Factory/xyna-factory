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

import java.net.URL;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;


public class TriggerClassLoader extends ClassLoaderBase {
  
  private static Logger logger = CentralFactoryLogging.getLogger(TriggerClassLoader.class);

  private String fqTriggerName;
  private String[] sharedLibs;
  

  protected TriggerClassLoader(String fqTriggerName, SharedLibClassLoader[] parents, String[] sharedLibs, Long revision) {

    super(ClassLoaderType.Trigger, fqTriggerName, new URL[]{}, parents, revision);

    for (SharedLibClassLoader slc : parents) {
      slc.addDependencyToReloadIfThisClassLoaderIsRecreated(fqTriggerName, revision, ClassLoaderType.Trigger, ClassLoadingDependencySource.ClassloaderCreation);
    }
    this.fqTriggerName = fqTriggerName;
    this.sharedLibs = sharedLibs;

    logger.debug("created " + this);

  }

  
  public String toString() {
    return super.toString() + "-" + fqTriggerName;
  }


  protected String[] getSharedLibs() {
    return sharedLibs;
  }
  
  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> c = findLoadedClass(name);
    if (c== null) {
      //erst bei parent schauen, dann bei sich selbst
      int idx = 0;
      ClassLoaderBase[] pars = getParents();
      while (c == null && idx < pars.length) {
        try {
          c = pars[idx].loadClass(name);
          if (c.getClassLoader() instanceof SharedLibClassLoader) {
            ((ClassLoaderBase) c.getClassLoader())
                .addDependencyToReloadIfThisClassLoaderIsRecreated(fqTriggerName, getRevision(), ClassLoaderType.Trigger,
                                                                   ClassLoadingDependencySource.Classloading);
          }
        } catch (ClassNotFoundException e) {
          //ignorieren
        }
        idx++;
      }
      if (c== null) {
        checkClosed();
        try {
          c = findClass(name); //wirft classnotfoundexception
        }
        catch (ClassNotFoundException e) {
        }
        catch (NoClassDefFoundError e) {
        }
      }
      if (c == null) {
        c = super.loadClass(name, resolve);
      }
    }
    if (resolve) {
      resolveClass(c);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder(name).append(" was loaded by ").append(c.getClassLoader()).toString());
    }

    return c;

  }

  @Override
  protected void deployWhenReload(String c) throws XFMG_ClassLoaderRedeploymentException {
    // trigger neu deployen
    XynaActivationTrigger xat =  XynaFactory.getInstance().getActivation().getActivationTrigger();
    xat.redeployTriggerInstancesOfTriggerWithSameParas(getClassLoaderID(), getRevision());

    super.deployWhenReload(c);
  }


  @Override
  protected void undeployWhenReload(String c) throws XFMG_ClassLoaderRedeploymentException {
    super.undeployWhenReload(c);
  }

}
