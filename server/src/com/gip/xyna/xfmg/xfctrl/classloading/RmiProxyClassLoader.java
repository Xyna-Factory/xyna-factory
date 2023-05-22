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

import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ReplaceableClassLoader;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;


public class RmiProxyClassLoader extends ClassLoaderBase implements ReplaceableClassLoader {
    
  public RmiProxyClassLoader( String name, File proxyJar) {
    this( name, getUrls(proxyJar) );
  }

  protected RmiProxyClassLoader( String name, URL[] urls) {
    super(ClassLoaderType.RMI, name, urls,
        new ClassLoaderBase[] { 
            new RMIClassLoader(RevisionManagement.REVISION_DEFAULT_WORKSPACE)}, 
        RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  private static URL[] getUrls( File proxyJar) {
    try {
      return new URL[] { proxyJar.toURI().toURL() };
    } catch( MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ClassLoaderBase replace() {
    return new RmiProxyClassLoader( getClassLoaderID(), getURLs() );
  }
  
}
