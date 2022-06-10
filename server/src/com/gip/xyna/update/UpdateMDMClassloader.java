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
package com.gip.xyna.update;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.classloading.XynaClassLoader;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class UpdateMDMClassloader extends ClassLoaderBase {

  private static ConcurrentMap<Long, ConcurrentMap<String, ClassLoaderBase>> map = new ConcurrentHashMap<Long, ConcurrentMap<String,ClassLoaderBase>>();
  
  
  public static ClassLoaderBase generateUpdateMDMClassLoader(ClassLoaderType type, String id, Long revision, Long parentRevision) {

    ConcurrentMap<String, ClassLoaderBase> targetMap = map.get(revision);
    if (targetMap == null) {
      targetMap = map.putIfAbsent(revision, new ConcurrentHashMap<String, ClassLoaderBase>());
      if (targetMap == null) {
        targetMap = map.get(revision);
      }
    }
    
    ClassLoaderBase newClassLoader = targetMap.get(id);
    if (newClassLoader == null) {
      try {
        String xmomPath = VersionManagement.getPathForRevision(PathType.XMOMCLASSES, revision == null ? -1 : revision);
        newClassLoader =
            new UpdateMDMClassloader(type, id, new URL[] {new File(xmomPath)
                .toURI().toURL()}, XynaClassLoader.getInstance(), revision);
        newClassLoader = targetMap.putIfAbsent(id, newClassLoader);
        if (newClassLoader == null) {
          newClassLoader = targetMap.get(id);
        }
      } catch (MalformedURLException e) {
        logger.info(null, e);
        return null;
      }
    }
    
    File serviceFolder = new File(GenerationBase.getFileLocationOfServiceLibsForDeployment(id, revision == null ? -1 : revision));
    if (serviceFolder.exists()) {
      File[] jarFiles = serviceFolder.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".jar");
        }
      });
      for (File file : jarFiles) {
        try {
          newClassLoader.addJarFile(file.getAbsolutePath());
        } catch (Ex_FileAccessException e) {
          logger.warn(e);
        }
      }
    }

    return newClassLoader;

  }


  public static ClassLoaderBase getExistingUpdateMDMClassLoader(ClassLoaderType type, String id, Long revision, Long parentRevision) {
    ConcurrentMap<String, ClassLoaderBase> targetMap = map.get(revision);
    if (targetMap == null) {
      return null;
    }
    return targetMap.get(id);
  }


  protected UpdateMDMClassloader(ClassLoaderType type, String id, URL[] urls, ClassLoader parent, Long revision) {
    super(type, id, urls, parent, revision);
  }

  
  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (UpdateMDMClassloader.class) {
      Class<?> c = findLoadedClass(name);
      if (c != null) {
        return c;
      }
      if (name.equals(getClassLoaderID()) || 
          name.startsWith(getClassLoaderID() + "$") ||
          getClassLoaderID().equals("UpdateMDMClassloader")) {
          try {
            c = findClass(name);
          } catch (ClassNotFoundException e) {
            if (getClassLoaderID().equals("UpdateMDMClassloader")) {
              c = getParent().loadClass(name);
            } else {
              throw e;
            }
          }
          if (resolve) {
            resolveClass(c);
          }
      } else {
        ConcurrentMap<String, ClassLoaderBase> targetMap = map.get(getRevision());
        if (targetMap.containsKey(name)) {
          try {
            c = targetMap.get(name).loadClass(name, resolve);
          } catch (ClassNotFoundException e) {
            ;
          }
        } else {
          for (String clID : targetMap.keySet()) {
            if (clID.startsWith(name + "$")) {
              try {
                c = targetMap.get(clID).loadClass(name, resolve);
              } catch (ClassNotFoundException e) {
              }
              break;
            }
          }
          if (c == null) {
            try {
              c = getParent().loadClass(name);
            } catch (ClassNotFoundException e) {
            }
          }
          if (c == null) {
            ClassLoaderBase clb = getUpdateMDMClassLoaderIfClassIsLoadable(name, getRevision(), null, null);
            if (clb != null) {
              c = clb.loadClass(name);
            } else {
              c = findClass(name);
              if (resolve) {
                resolveClass(c);
              }
            }
          }
        }
      }
      return c;
    }
  }


  public static ClassLoaderBase getStaticMDMClassLoader(long revision) {
    return generateUpdateMDMClassLoader(ClassLoaderType.XYNA, "UpdateMDMClassloader", revision, null);
  }

  public static ClassLoaderBase getUpdateMDMClassLoaderIfClassIsLoadable(String classLoaderName, long revision, ClassLoaderType clt, Long parentRevision) {

    if (GenerationBase.isReservedServerObjectByFqClassName(classLoaderName)) {
      return null;
    }

    try {
      if (classLoaderName.contains("$")) {
        //innere Klassen werden mit dem ClassLoader der ‰uﬂeren Klasse geladen
        classLoaderName = classLoaderName.split("\\$")[0];
      }
      ClassLoaderBase cl = UpdateMDMClassloader.getStaticMDMClassLoader(revision);
      if (cl == null) {
        return null;
      }
      Class loadedClass = cl.loadClass(classLoaderName);
      if (loadedClass == Container.class || loadedClass == XynaObjectList.class || loadedClass == XynaObject.class
                      || loadedClass == XynaProcess.class || loadedClass == XynaException.class || loadedClass == GeneralXynaObjectList.class) {
        // FIXME what else?
        return null;
      }
      if (XynaObject.class.isAssignableFrom(loadedClass) && (clt == ClassLoaderType.MDM || clt == null)) {
        if (clt == null) {
          clt = ClassLoaderType.MDM;
        }
        return UpdateMDMClassloader.generateUpdateMDMClassLoader(clt, classLoaderName,
                                                                 revision, parentRevision);
      } else if (XynaProcess.class.isAssignableFrom(loadedClass) && (clt == ClassLoaderType.WF || clt == null)) {
        if (clt == null) {
          clt = ClassLoaderType.WF;
        }
        return UpdateMDMClassloader.generateUpdateMDMClassLoader(clt, classLoaderName,
                                                                 revision, parentRevision);
      } else if (XynaException.class.isAssignableFrom(loadedClass) && (clt == ClassLoaderType.Exception || clt == null)) {
        if (clt == null) {
          clt = ClassLoaderType.Exception;
        }
        return UpdateMDMClassloader.generateUpdateMDMClassLoader(clt, classLoaderName,
                                                                 revision, parentRevision);
      } else {
        return null;
      }
    } catch (ClassNotFoundException e) {
      return null;
    }

  }

}
