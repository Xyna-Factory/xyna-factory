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



import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class MDMClassLoader extends MDMClassLoaderXMLBase {

  private static Logger logger = CentralFactoryLogging.getLogger(MDMClassLoader.class);

  private final String[] sharedLibs;
  private boolean checkedBaseClassLoader = false;
  private String baseClassName;
  
  /**
   * Creates a new MDM classloader that is already registered at all provided parent shared lib class loaders.
   * @param fqClassName The name of the loaded class
   * @param parents All parent shared lib class loaders
   * @param sharedLibs The shared libs that this class loader uses
   */
  protected MDMClassLoader(String fqClassName, SharedLibClassLoader[] parents, String[] sharedLibs,
                           String originalXmlPath, String originalXmlName, Long revision) {

    super(ClassLoaderType.MDM, fqClassName, new URL[] {getClassUrl(fqClassName, revision)}, parents, originalXmlPath,
          originalXmlName, revision);

    for (SharedLibClassLoader slc : parents) {
      slc.addDependencyToReloadIfThisClassLoaderIsRecreated(fqClassName, revision, ClassLoaderType.MDM, ClassLoadingDependencySource.ClassloaderCreation);
    }

    this.sharedLibs = sharedLibs;

    if (logger.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder("Created ");
      sb.append(this).append(" with parents = [");
      for (SharedLibClassLoader slc : parents) {
        sb.append(slc.toString()).append(" ");
      }
      sb.append("]");
      logger.debug(sb.toString());
    }

  }


  protected static URL getClassUrl(String fqClassName, Long revision) { //auch von wfclassloader genutzt
    URL url;
    try {
      url = new File(VersionManagement.getPathForRevision(PathType.XMOMCLASSES, revision) + Constants.fileSeparator).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } //TODO performance
    return url;
  }

  
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    return loadClass(name, resolve, true);
  }
  
  
  Class<?> loadClass(String name, boolean resolve, boolean delegateToOtherXMOM) throws ClassNotFoundException {
    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder().append(this).append(" trying to load ").append(name));
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
            ((ClassLoaderBase) c.getClassLoader())
                .addDependencyToReloadIfThisClassLoaderIsRecreated(getFqClassName(), getRevision(), ClassLoaderType.MDM,
                                                                   ClassLoadingDependencySource.Classloading);
          }
        } catch (ClassNotFoundException e) {
          // ignorieren
        }
        idx++;
      }
      if (c == null) {
        IsResponsibleEnum isResponsible = isResponsible2(name);
        if (delegateToOtherXMOM && isResponsible != IsResponsibleEnum.RESPONSIBLE) { //innere klassen heissen <classname>$<innerclassname>
          c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                          .loadMDMClass(name, false, null, null, getRevision());
          if (c != null) {
            ((ClassLoaderBase) c.getClassLoader())
                                .addDependencyToReloadIfThisClassLoaderIsRecreated(getFqClassName(),
                                                                                   getRevision(),
                                                                                   ClassLoaderType.MDM, ClassLoadingDependencySource.Classloading);
          } else {
            c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                           .loadExceptionClass(name, false, null, null, getRevision());
            if (c != null) {
              ((ClassLoaderBase) c.getClassLoader())
                                  .addDependencyToReloadIfThisClassLoaderIsRecreated(getFqClassName(),
                                                                                     getRevision(),
                                                                                     ClassLoaderType.MDM, ClassLoadingDependencySource.Classloading);
            }
          }
        }
        if (c == null) {
          if (isResponsible != IsResponsibleEnum.NOT_RESPONSIBLE) {
            checkClosed();
            try {
              //achtung, das findet auch klassen, f�r die der classloader eigtl nicht zust�ndig w�re, n�mlich die, die auch in xmomclasses 
              //liegen. deshalb muss man vorher andere mdmclassloader checken. 
              c = findClass(name);
            } catch (ClassNotFoundException e) {
            }
          }
          if (c == null) {
            if (!checkedBaseClassLoader) {
              if (!getFqClassName().equals(name)) {
                Class<?> thisClass = findLoadedClass(getFqClassName());
                if (thisClass != null) { // ansonsten ist der ClassLoader noch nicht fertig initialisiert und hat seine Klasse noch nie geladen
                  Class<?> superClass = thisClass.getSuperclass();
                  if (superClass != null && XynaObject.class != superClass
                      && XynaObject.class.isAssignableFrom(superClass)) {
                    baseClassName = superClass.getName();
                    if (logger.isDebugEnabled()) {
                      logger.debug("found super mdm class " + baseClassName + " for class " + getFqClassName());
                    }
                  } else if (logger.isDebugEnabled()) {
                    logger.debug("class " + getFqClassName() + " has no mdm super class");
                  }

                  checkedBaseClassLoader = true;
                }
              }
            }
            //bei vererbung in verbindung mit instanzmethoden-impls muss man evtl klassen aus einer "ober-impl-klasse" laden
            if (baseClassName != null) {
              try {
                //nicht bei rekursion erneut versuchen an xmom objekt classloader zu delegieren, weil es sonst eine endlosrekursion geben kann.
                c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                        .getMDMClassLoader(baseClassName, getRevision(), true).loadClass(name, resolve, false);
              } catch (XFMG_MDMObjectClassLoaderNotFoundException e) {
                throwClassNotFoundException(name, e);
              }
            }
            if (c == null) {
              throwClassNotFoundException(name, null);
            }
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
    
    addPreviouslyLoadedClass(name, c);
    setHasBeenUsed();
    return c;

  }
  
  private enum IsResponsibleEnum {
    RESPONSIBLE, NOT_RESPONSIBLE, MAYBE_RESPONSIBLE;

  }

  private IsResponsibleEnum isResponsible2(String name) {
    if (isResponsible(name)) {
      return IsResponsibleEnum.RESPONSIBLE;
    }
    if (isOtherXMOMObject(name)) {
      return IsResponsibleEnum.NOT_RESPONSIBLE;
    }
    //z.b. klassen aus korrelierten jar-files. k�nnte aber auch in jars der superklassen sein, und deshalb muss anderes xmomobjekt das laden
    return IsResponsibleEnum.MAYBE_RESPONSIBLE;
  }


  private boolean isOtherXMOMObject(String name) {
    int idx = name.indexOf("$");
    if (idx > -1) {
      name = name.substring(0, idx);
    }
    RuntimeContextDependencyManagement rcdm =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Long rev = rcdm.getRevisionDefiningXMOMObject(name, getRevision());
    if (rev != null) {
      return true;
    }
    if (name.contains("._")) {
      //xmlname != fqclassname?
      String fqXmlName = GenerationBase.lookupXMLNameByJavaClassName(name, getRevision(), true);
      if (fqXmlName.equals(name)) {
        return false;
      }
      return true;
    } else {
      return false;
    }
  }


  protected String[] getSharedLibs() {
    return sharedLibs;
  }


  @Override
  protected void undeployWhenReload(String className) throws XFMG_ClassLoaderRedeploymentException {
    try {
      XynaObject.undeploy((Class<? extends XynaObject>) loadClass(className));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("mdm class " + className + " not found", e);
    } catch (XynaException e) {
      throw new XFMG_ClassLoaderRedeploymentException("MDM", className, e);
    } finally {
      super.undeployWhenReload(className);
    }
  }


  @Override
  protected void deployWhenReload(String className) throws XFMG_ClassLoaderRedeploymentException {
    try {
      XynaObject.deploy((Class<? extends XynaObject>) loadClass(className));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("mdm class " + className + " not found", e);
    } catch (XynaException e) {
      throw new XFMG_ClassLoaderRedeploymentException("MDM", className, e);
    }
      
    super.deployWhenReload(className);    
  }


}
