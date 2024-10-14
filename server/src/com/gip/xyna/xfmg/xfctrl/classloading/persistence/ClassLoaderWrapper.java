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
package com.gip.xyna.xfmg.xfctrl.classloading.persistence;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



/**
 * sort dafür, dass {@link ContainerClass} von diesem classloader geladen wird, und das laden nicht delegiert wird. nur
 * das laden von abhängigen klassen wird an den parent delegiert.
 */
public class ClassLoaderWrapper extends ClassLoader {

  private static final Logger logger = CentralFactoryLogging.getLogger(ClassLoaderWrapper.class);

  private static boolean EXECUTION_DURING_TESTS = true;


  private URLClassLoader[] allParents;


  public ClassLoaderWrapper(URLClassLoader[] parents) {
    //super(SerializableClassloadedObject.getURLs(), parents[0]);
    allParents = parents;

    // only exists during Testcases. at least dont check for directory existence every time in production environment.
 /*   if (EXECUTION_DURING_TESTS) {
      if (new File("classes.test").exists()) { //FIXME: das gehört nicht in den produktivcode
        try {
          addURL(new File("deploy/xynaserver.jar").toURI().toURL());
        } catch (MalformedURLException e) {
          //throw new XynaException("Test failed");
        }
      } else {
        EXECUTION_DURING_TESTS = false;
      }
    }*/

  }

  private volatile static byte[] containerClassBytes;


  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    if (!name.equals(ContainerClass.class.getName())) {
      throw new ClassNotFoundException(name);
    }
    if (containerClassBytes == null) {
      synchronized (ClassLoaderWrapper.class) {
        if (containerClassBytes == null) {
          InputStream is = ClassLoaderWrapper.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
          int len = 0;
          byte[] bytes = new byte[2048];
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          try {
            while (-1 != (len = is.read(bytes))) {
              baos.write(bytes, 0, len);
            }
          } catch (IOException e) {
            throw new ClassNotFoundException("Could not load class " + name, e);
          } finally {
            try {
              is.close();
            } catch (IOException e) {
              logger.warn("Could not close stream", e);
            }
          }
          containerClassBytes = baos.toByteArray();
        }
      }
    }
    return defineClass(name, containerClassBytes, 0, containerClassBytes.length);
  }


  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      if (name.equals(ContainerClass.class.getName())) {
        //dafür ist der classloader wrapper gedacht: die containerklasse nicht mit einem fremden classloader laden,
        //sondern mit diesem, damit man kontrolle über die parent-classloader hat
        c = findClass(name);
      } else {
        //alle anderen klassen können nun delegiert werden, automatisch zuerst an den gewünschten parent-classloader
        //und von dort falls unbekannt zum applicationclassloader hin...

        for (URLClassLoader parent : allParents) {
          if (parent != null) {
            try {
              c = parent.loadClass(name);
            } catch (ClassNotFoundException f) {
              //ignore
            }
            if (c != null) {
              break;
            }
          }
        }

        if (c == null) {
          if (logger.isDebugEnabled()) {
            logger
                .debug("ClassLoaderWrapper in SerializableClassloadedObject failed to load class " + name + ". urls:");
            logger.debug("parentClassloaders:");
            for (URLClassLoader parent : allParents) {
              logger.debug(" - " + parent);
            }
          }
          throw new ClassNotFoundException(name);
        }
      }
    }
    return c;
  }
  
  @Override
  public String toString() {
    return "ClassLoaderWrapper("+Arrays.asList(allParents)+")";
  }

}
