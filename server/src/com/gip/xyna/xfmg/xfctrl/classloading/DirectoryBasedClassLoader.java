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
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public abstract class DirectoryBasedClassLoader extends ClassLoaderBase {

  private static Logger logger = CentralFactoryLogging.getLogger(DirectoryBasedClassLoader.class);
  private String name;

  public DirectoryBasedClassLoader(ClassLoaderType classLoaderType, String name, String basedir) throws XFMG_JarFolderNotFoundException {
    super(classLoaderType, name, getUrls(classLoaderType, name, basedir),
          new ClassLoaderBase[] {XynaClassLoader.getInstance()}, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    this.name = name;
    if (logger.isDebugEnabled()) {
      logger.debug("Created " + this);
    }
  }


  private static URL[] getUrls(ClassLoaderType type, String name, String basedir) throws XFMG_JarFolderNotFoundException {
    String simpleName = GenerationBase.getSimpleNameFromFQName(name);
    if (simpleName == null) {
      return new URL[0];
    }
    File[] jars = getJarsInCorrespondingDirectory(type, simpleName, basedir);
    URL[] urls = new URL[jars.length];
    for (int i = 0; i < jars.length; i++) {
      try {
        urls[i] = jars[i].toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    return urls;

  }


  public static File[] getJarsInCorrespondingDirectory(ClassLoaderType type, String name, String basedir) throws XFMG_JarFolderNotFoundException {

    File correspondingDir = new File(basedir, name);
    if (!correspondingDir.exists() || !correspondingDir.isDirectory()) {
      throw new XFMG_JarFolderNotFoundException(type.name(), name, correspondingDir.getAbsolutePath());
    }
    return correspondingDir.listFiles(new FilenameFilter() {

      public boolean accept(File dir, String name) {
        return name.endsWith(".jar");
      }

    });
  }


  public String toString() {
    return super.toString() + "-" + name;
  }


  public String getName() {
    return name;
  }

}
