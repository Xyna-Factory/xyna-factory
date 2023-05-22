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
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;

public class SharedLibClassLoader extends ClassLoaderBase {
  
  public static final String EMPTYSHAREDLIB = "--empty shared lib";
  private static Logger logger = CentralFactoryLogging.getLogger(SharedLibClassLoader.class);
  private String sharedLibName;

  public SharedLibClassLoader(String sharedLibName, Long revision) throws XFMG_SHARED_LIB_NOT_FOUND {
    super(ClassLoaderType.SharedLib, sharedLibName, getUrls(sharedLibName, revision), new ClassLoaderBase[] {XynaClassLoader
                    .getInstance()}, revision);
    this.sharedLibName = sharedLibName;
    if (logger.isDebugEnabled()) {
      logger.debug("created " + this);
    }
  }


  private static URL[] getUrls(String sharedLibName, Long revision) throws XFMG_SHARED_LIB_NOT_FOUND {

    if (sharedLibName.equals(EMPTYSHAREDLIB)) {
      return new URL[0];
    }
    File[] jars = getJarsOfSharedLib(sharedLibName, revision);
    URL[] urls = new URL[jars.length];
    for (int i = 0; i < jars.length; i++) {
      try {
        urls[i] = jars[i].toURI().toURL();
      }
      catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    return urls;

  }


  public static File[] getJarsOfSharedLib(String sharedLibName, Long revision) throws XFMG_SHARED_LIB_NOT_FOUND {

    if (".".equals(sharedLibName)) {
      throw new XFMG_SHARED_LIB_NOT_FOUND(sharedLibName);
    }
    
    File sharedLibDir = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision) + sharedLibName);
    if (!(sharedLibDir.isDirectory() && sharedLibDir.exists())) {
      throw new XFMG_SHARED_LIB_NOT_FOUND(sharedLibName + " in " + sharedLibDir);
    }
    return sharedLibDir.listFiles(new FilenameFilter() {

      public boolean accept(File dir, String name) {
        return name.endsWith(".jar");
      }

    });
  }

  
  public String toString() {
    return super.toString() + "-" + sharedLibName;
  }


  public String getName() {
    return sharedLibName;
  }

}
