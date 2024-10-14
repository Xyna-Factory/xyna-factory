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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



public class XynaClassLoader extends ClassLoaderBase {

  private static Logger logger = CentralFactoryLogging.getLogger(XynaClassLoader.class);

  
  private static XynaClassLoader instance;
  static {
    ClassLoader cl = XynaClassLoader.class.getClassLoader();
    //cl = getNextParentUrlClassLoader(cl);
    if (cl != null)
      instance = new XynaClassLoader(cl);
    else {
      
      // this should not happen!
      logger.fatal("Could not obtain " + URLClassLoader.class.getSimpleName() + ", shutting down");
      System.exit(-1);
    }
  }
  
  
  private static URL[] getClassPathURLs() {
    String cp = System.getProperty("java.class.path");
    ArrayList<URL> path = new ArrayList<>();
    if (cp != null) {
      String[] entries = cp.split(File.pathSeparator);
      URL[] result = new URL[entries.length];
      for (int i = 0; i < entries.length; i++) {
        try {
          result[i] = Paths.get(entries[i]).toAbsolutePath().toUri().toURL();
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }
      return result;
    }
    return path.toArray(new URL[0]);
  }

  private XynaClassLoader(ClassLoader parent) {
    super(ClassLoaderType.XYNA, "single xyna classloader", getClassPathURLs(), parent, null);
    logger.debug("created xynaclassloader");
  }  


  public static XynaClassLoader getInstance() {
    return instance;
  }
  
  
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

    // logger.debug("looking for " + name);

    Class<?> c = findLoadedClass(name);

    if (c == null) {
      // erst bei sich selbst schauen, dann parent (ausser diese klasse selbst, damit sie nicht doppelt vorhanden ist)
      /*
       * if (name.startsWith("com.gip.") && false) { //TODO try { c = findClass(name); } catch (ClassNotFoundException
       * e) { //ignore } }
       */
      // if (c == null) {
      // logger.debug("checking parent-classloader");
      c = getParent().loadClass(name);
      // }
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }


}