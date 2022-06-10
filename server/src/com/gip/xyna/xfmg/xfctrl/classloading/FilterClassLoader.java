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
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class FilterClassLoader extends ClassLoaderBase {
  
  private static Logger logger = CentralFactoryLogging.getLogger(FilterClassLoader.class);
  
  private String fqFilterName;
  private String fqTriggerClassName;
  private String[] sharedLibs;
  
  private Long parentRevision;


  /**
   * Creates a new FilterClassLoader instance. The constructor already registers this as a dependency for all parent
   * trigger and shared lib ClassLoaders
   * 
   */
  protected FilterClassLoader(String fqFilterName, TriggerClassLoader parent, SharedLibClassLoader[] parentLibs,
                              String fqTriggerClassName, Long revision) {
    super(ClassLoaderType.Filter, fqFilterName, new URL[] {}, joinParents(parent, parentLibs), revision);
    if (revision.equals(parent.getRevision())
        || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .isDependency(revision, parent.getRevision())) {
      parentRevision = null;
      parent.addDependencyToReloadIfThisClassLoaderIsRecreated(fqFilterName, revision, ClassLoaderType.Filter, ClassLoadingDependencySource.ClassloaderCreation);
      for (SharedLibClassLoader scl : parentLibs) {
        scl.addDependencyToReloadIfThisClassLoaderIsRecreated(fqFilterName, revision, ClassLoaderType.Filter, ClassLoadingDependencySource.ClassloaderCreation);
      }
    } else {
      parentRevision = parent.getRevision();
      ClassLoaderIdRevisionRef clId = new ClassLoaderIdRevisionRef(fqFilterName, revision, parentRevision);
      parent.addDependencyToReloadIfThisClassLoaderIsRecreated(clId, ClassLoaderType.OutdatedFilter, ClassLoadingDependencySource.ClassloaderCreation);
      for (SharedLibClassLoader scl : parentLibs) {
        scl.addDependencyToReloadIfThisClassLoaderIsRecreated(clId, ClassLoaderType.OutdatedFilter, ClassLoadingDependencySource.ClassloaderCreation);
      }
      type = ClassLoaderType.OutdatedFilter;
    }

    sharedLibs = new String[parentLibs.length];
    for (int i = 0; i<sharedLibs.length; i++) {
      sharedLibs[i] = parentLibs[i].getName();
    }

    this.fqFilterName = fqFilterName;
    this.fqTriggerClassName = fqTriggerClassName;

    if (logger.isDebugEnabled()) {
      logger.debug("created " + this);
    }
  }


  private static ClassLoaderBase[] joinParents(ClassLoaderBase a, ClassLoaderBase[] b) {
    ClassLoaderBase[] ret = new ClassLoaderBase[1 + b.length];
    ret[0] = a;
    System.arraycopy(b, 0, ret, 1, b.length);
    return ret;
  }

  
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (logger.isTraceEnabled()) {
      logger.trace(this + " trying to load " + name);
    }
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      // erst bei parent schauen, dann bei sich selbst
      int idx = 0;
      ClassLoaderBase[] pars = getParents();
      while (c == null && idx < pars.length) {
        try {
          c = pars[idx].loadClass(name);
          if (c.getClassLoader() instanceof SharedLibClassLoader) {
            ClassLoaderIdRevisionRef clId = new ClassLoaderIdRevisionRef(fqFilterName, getRevision(), parentRevision);
            ((ClassLoaderBase) c.getClassLoader())
                .addDependencyToReloadIfThisClassLoaderIsRecreated(clId, getType(), ClassLoadingDependencySource.Classloading);
          }
        } catch (ClassNotFoundException e) {
          // ignorieren
        }
        idx++;
      }
      if (c == null) {
        checkClosed();
        try {
          c = findClass(name);
        } catch (ClassNotFoundException e) {
          // ignorieren
        }
        if (c == null) {
          c =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                  .loadMDMClass(name, false, null, null, getRevision());
          ClassLoaderIdRevisionRef clId = new ClassLoaderIdRevisionRef(fqFilterName, getRevision(), parentRevision);
          if (c != null) {
            ((ClassLoaderBase) c.getClassLoader())
                                .addDependencyToReloadIfThisClassLoaderIsRecreated(clId, getType(), ClassLoadingDependencySource.Classloading);
          } else {
            c =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                 .loadExceptionClass(name, false, null, null, getRevision());
            if (c != null) {
              ((ClassLoaderBase) c.getClassLoader())
                                  .addDependencyToReloadIfThisClassLoaderIsRecreated(clId, getType(), ClassLoadingDependencySource.Classloading);
            }
          }
          if (c == null) {
            throw new ClassNotFoundException(name + " not found");
          }
        }
      }
    }
    if (resolve) {
      resolveClass(c);
    }

    if (logger.isDebugEnabled())
      logger.debug("++++" + name + "++++ was loaded by ####" + c.getClassLoader() + "####");

    return c;

  }


  public String toString() {
    return super.toString() + "-" + fqFilterName;
  }


  protected String getFQTriggerClass() {
    return fqTriggerClassName;
  }


  public String[] getSharedLibs() {
    return sharedLibs;
  }


  @Override
  protected void deployWhenReload(String c) throws XFMG_ClassLoaderRedeploymentException {   
    //ClassCache in der/den dazugehörigen TriggerInstanz leeren
    //TODO we could set a Map<filterName,Boolean> that the EventListeners read before accessing their classMapping
    //that way they would only clear their cache if the need to and propably less often
    
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    Class<? extends ConnectionFilter> clazz;
    try {
      clazz = (Class<? extends ConnectionFilter>) loadClass(c, true);
    } catch (ClassNotFoundException e) {
      throw new XFMG_ClassLoaderRedeploymentException("Filter",c,e);
    }
    Filter[] filters;
    try {
      filters = xat.getFilters(getRevision());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    for (Filter filter : filters) {
      if (filter.getFQFilterClassName().equals(fqFilterName)) {
        //ondeployment für alle filterinstanzen mit der zugehörigen triggerinstanz aufrufen.
        ConnectionFilterInstance[] cfis;
        if (parentRevision == null) {
          //für "normale" Filter in der eigenen revision suchen
          cfis = xat.getFilterInstances(filter.getName(), getRevision());
        } else {
          //für OutdatedFilter in der parentRevision nach OutdatedFiltern suchen
          cfis = xat.getOutdatedFilterInstances(filter.getName(), getRevision(), getParentRevision());
        }
        
        for (ConnectionFilterInstance cfi : cfis) {
          EventListenerInstance eli = xat.getEventListenerInstanceByName(cfi.getTriggerInstanceName(), cfi.getRevision(), true);
          if (eli != null) {
            ConnectionFilter cf;
            try {
              cf = clazz.getConstructor().newInstance();
            } catch (Exception e) {
              throw new XFMG_ClassLoaderRedeploymentException("Filter",c,e);
            }
            cf.onDeployment(eli.getEL());
            eli.getEL().resetFilterCache();
          }
        }  
      }
    }
    //filter muss nicht neu deployed werden, weil bei empfang von nachrichten vom trigger immer eine neue instanz des filters erstellt wird.
    super.deployWhenReload(c);
  }
  
  
  @Override
  protected void undeployWhenReload(String c) throws XFMG_ClassLoaderRedeploymentException {
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    Filter[] filters;
    try {
      filters = xat.getFilters(getRevision());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    for (Filter filter : filters) {
      if (filter.getFQFilterClassName().equals(fqFilterName)) {
        //undeployment für alle filterinstanzen mit der zugehörigen triggerinstanz aufrufen.
        ConnectionFilterInstance[] cfis;
        if (parentRevision == null) {
          //für "normale" Filter in der eigenen revision suchen
          cfis = xat.getFilterInstances(filter.getName(), getRevision());
        } else {
          //für OutdatedFilter in der parentRevision nach OutdatedFiltern suchen
          cfis = xat.getOutdatedFilterInstances(filter.getName(), getRevision(), getParentRevision());
        }
        
        for (ConnectionFilterInstance cfi : cfis) {
          EventListenerInstance eli = xat.getEventListenerInstanceByName(cfi.getTriggerInstanceName(), cfi.getRevision(), true);
          if (eli != null) {
            XynaActivationTrigger.callUndeploymentOfFilterInstance(cfi.getCF(), eli.getEL());
          }
        }  
      }
    }
    //filter muss nicht neu deployed werden, weil bei empfang von nachrichten vom trigger immer eine neue instanz des filters erstellt wird.
    super.undeployWhenReload(c);
  }

  @Override
  public Long getParentRevision() {
    return parentRevision;
  }

  
  
}
