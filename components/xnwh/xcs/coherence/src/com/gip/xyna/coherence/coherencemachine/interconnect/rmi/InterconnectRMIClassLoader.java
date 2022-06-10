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

package com.gip.xyna.coherence.coherencemachine.interconnect.rmi;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.utils.logging.LoggerFactory;



public class InterconnectRMIClassLoader extends URLClassLoader {

  private static Logger logger = LoggerFactory.getLogger(InterconnectRMIClassLoader.class);


  /**
   * liste von zusätzlichen parents, die nicht mit dem konstruktor, sondern zur laufzeit dynamisch belegt werden.
   * beispielsweise von persistencelayerclassloadern, die eine abhängigkeit auf storables erkennen, die in einem service
   * (mdmclassloader) definiert sind. diese abhängigkeit kann irgendwann aufhören, weil z.b. der service undeployed
   * wird. dann sollten auch die referenzen auf die classloader verschwinden wegen oom-gefahr.<br>
   * die weakreferences gewährleisten das.
   */
  private WeakReference<ClassLoader>[] weaklyReferencedParents;
  private final Object weaklyReferencedParentsLock = new Object();


  public InterconnectRMIClassLoader() {
    super(copyURLsOfCurrentClassload(InterconnectRMIClassLoader.class.getClassLoader()),
          InterconnectRMIClassLoader.class.getClassLoader());
  }


  private static URL[] copyURLsOfCurrentClassload(ClassLoader loader) {
    return ((URLClassLoader) loader).getURLs();
  }


  public void addWeakReferencedParentClassLoader(ClassLoader cl) {
    synchronized (weaklyReferencedParentsLock) {
      if (weaklyReferencedParents == null) {
        weaklyReferencedParents = new WeakReference[] {new WeakReference<ClassLoader>(cl)};
        return;
      }

      //check doubles and remove gc-ed
      List<WeakReference<ClassLoader>> newWrs = new ArrayList<WeakReference<ClassLoader>>();
      for (WeakReference<ClassLoader> wr : weaklyReferencedParents) {
        if (wr.get() != null) {
          newWrs.add(wr);
        }
      }
      newWrs.add(new WeakReference<ClassLoader>(cl));

      //create new array      
      weaklyReferencedParents = newWrs.toArray(new WeakReference[newWrs.size()]);
    }
  }


  private static InterconnectRMIClassLoader[] checkConstructorArgumentLength(InterconnectRMIClassLoader[] arg) {
    if (arg.length == 0)
      throw new IllegalArgumentException("No parent classloader specified while creating "
          + InterconnectRMIClassLoader.class.getSimpleName());
    return arg;
  }


  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    try {
      return super.findClass(name);
    } catch (LinkageError e) {
      if (e.getMessage().contains("duplicate class definition")) {
        //passiert durch fehlende synchronisierung von loadclass. die ist wegen deadlock gefahr nicht eingebaut.
        Class<?> c = findLoadedClass(name);
        if (c == null) {
          throw e;
        }
        if (logger.isDebugEnabled()) {
          logger.debug("catched linkage error handled it successfully: " + e.getMessage());
          if (logger.isTraceEnabled()) {
            logger.debug(null, e);
          }
        }
        return c;
      }
      throw e;
    }
  }


  /**
   * Tries to load a class by first asking java, then asking its parents and then looks for it itself. Always tries to
   * resolve the class by calling {@link #resolveClass(Class)}
   */
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return loadClass(name, true);
  }


  private static final Pattern classNamePattern =
      Pattern
          .compile("com\\.gip\\.xyna\\.coherence\\.coherencemachine\\.interconnect\\.rmi\\.rmiadapter\\.GenericRMIAdapter(\\$.*)?");


  /**
   * Tries to load a class by first asking java, then asking its parents and then looks for it itself. Can be configured
   * to resolve the class by calling {@link #resolveClass(Class)}
   */
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

    Class<?> c = findLoadedClass(name);
    if (c == null) {

      if (!classNamePattern.matcher(name).matches()
          && !name.equals(InterconnectCalleeRMI.INTERCONNECT_CALLEE_CLASSNAME)) {
        try {
          c = super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
          // ignore, try to load with weakly referenced parents
        }
        if (c != null) {
          if (logger.isTraceEnabled()) {
            logger.trace(new StringBuilder(name).append(" was loaded by ").append(c.getClassLoader()).toString());
          }
          return c;
        }
      }

      if (c == null) {
        if (weaklyReferencedParents != null) {
          synchronized (weaklyReferencedParentsLock) {
            for (WeakReference<ClassLoader> wr : weaklyReferencedParents) {
              ClassLoader p = wr.get();
              if (p == null) {
                continue;
              }
              try {
                c = p.loadClass(name);
              } catch (ClassNotFoundException e) {
                //ignorieren
              }
              if (c != null) {
                break;
              }
            }
          }
        }
      }
      if (c == null) {
        c = findClass(name); //wirft classnotfoundexception
      }
    }
    if (resolve) {
      resolveClass(c);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(new StringBuilder(name).append(" was loaded by ").append(c.getClassLoader()).toString());
    }

    return c;

  }
  
  protected void addURLs(URL[] urls) {
    for (URL u: urls) {
      if (!Arrays.asList(getURLs()).contains(u)) {
        addURL(u);
      }
    }
  }

}
