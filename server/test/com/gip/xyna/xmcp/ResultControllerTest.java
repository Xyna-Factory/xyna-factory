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
package com.gip.xyna.xmcp;



import java.net.URLClassLoader;

import junit.framework.TestCase;

import com.gip.xyna.utils.exceptions.XynaException;



public class ResultControllerTest extends TestCase {

  private static class MyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

  }

  private static class MyException2 extends MyException {

    private static final long serialVersionUID = 1L;
  }

  private static class MyClassLoader extends URLClassLoader {

    public MyClassLoader(URLClassLoader parent) {
      super(parent.getURLs(), parent);
    }


    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class<?> c = null;
      if (name.startsWith(MyException.class.getName())) {
        c = findClass(name);
      }
      if (c != null && resolve) {
        resolveClass(c);
      }
      if (c != null) {
        return c;
      }
      return super.loadClass(name, resolve);
    }

  }


  public void testClassSupport() {
    ResultController rc = new ResultController();

    rc.addSupportedExceptionClass(MyException.class, WrappingType.ORIGINAL);
    assertTrue(rc.isSupported(MyException.class, WrappingType.ORIGINAL));
    assertFalse(rc.isSupported(MyException.class, WrappingType.XML));

    assertTrue(rc.isSupported(MyException.class, WrappingType.SIMPLE)); //das ist der default

    assertFalse(rc.isSupported(XynaException.class, WrappingType.ORIGINAL));
    assertFalse(rc.isSupported(XynaException.class, WrappingType.XML));
    assertTrue(rc.isSupported(MyException2.class, WrappingType.ORIGINAL));
    assertFalse(rc.isSupported(MyException2.class, WrappingType.XML));
  }


  public void testClassLoaderSupport1() throws ClassNotFoundException {
    ResultController rc = new ResultController();
    //appclassloader
    ClassLoader cl = new MyClassLoader((URLClassLoader) getClass().getClassLoader());
    ClassLoader cl2 = new MyClassLoader((URLClassLoader) getClass().getClassLoader()); //anderer classloader, aber gleiche classloaderklasse
    Class<? extends Throwable> myExClass = (Class<? extends Throwable>) cl.loadClass(MyException.class.getName());  
    Class<? extends Throwable> myEx2Class = (Class<? extends Throwable>) cl.loadClass(MyException2.class.getName());
    Class<? extends Throwable> myEx2Class_v2 = (Class<? extends Throwable>) cl2.loadClass(MyException2.class.getName());
    rc.addSupportedExceptionClassLoader(myExClass.getClassLoader().getClass(), WrappingType.ORIGINAL);
    rc.setDefaultWrappingTypeForExceptions(WrappingType.SIMPLE);

    //bootstrapclassloader
    assertTrue(rc.isSupported(RuntimeException.class, WrappingType.ORIGINAL));
    assertFalse(rc.isSupported(RuntimeException.class, WrappingType.XML));

    assertFalse(rc.isSupported(MyException.class, WrappingType.ORIGINAL));
    assertFalse(rc.isSupported(MyException.class, WrappingType.XML));
    assertTrue(rc.isSupported(MyException.class, WrappingType.SIMPLE));
    assertFalse(rc.isSupported(XynaException.class, WrappingType.ORIGINAL));
    assertFalse(rc.isSupported(XynaException.class, WrappingType.XML));
    assertFalse(rc.isSupported(MyException2.class, WrappingType.ORIGINAL));
    assertFalse(rc.isSupported(MyException2.class, WrappingType.XML));

    assertTrue(rc.isSupported(myExClass, WrappingType.ORIGINAL));
    assertTrue(rc.isSupported(myEx2Class, WrappingType.ORIGINAL));
    assertTrue(rc.isSupported(myEx2Class_v2, WrappingType.ORIGINAL));
    assertFalse(rc.isSupported(myEx2Class, WrappingType.XML));
  }


  public void testClassLoaderSupport2() throws ClassNotFoundException {
    ResultController rc = new ResultController();
    
    ClassLoader cl = new MyClassLoader((URLClassLoader) getClass().getClassLoader());
    Class<? extends Throwable> myExClass = (Class<? extends Throwable>) cl.loadClass(MyException.class.getName());  
    
    //appclassloader    
    rc.addSupportedExceptionClassLoader("sun.misc.Launcher$AppClassLoader", WrappingType.ORIGINAL);
    rc.setDefaultWrappingTypeForExceptions(WrappingType.XML);

    //bootstrapclassloader
    assertTrue(rc.isSupported(RuntimeException.class, WrappingType.ORIGINAL));
    assertTrue(rc.isSupported(RuntimeException.class, WrappingType.XML));

    assertTrue(rc.isSupported(MyException.class, WrappingType.ORIGINAL));
    assertTrue(rc.isSupported(MyException.class, WrappingType.XML));
    assertTrue(rc.isSupported(MyException.class, WrappingType.SIMPLE));
    assertTrue(rc.isSupported(XynaException.class, WrappingType.ORIGINAL));
    assertTrue(rc.isSupported(XynaException.class, WrappingType.XML));
    assertTrue(rc.isSupported(MyException2.class, WrappingType.ORIGINAL));
    assertTrue(rc.isSupported(MyException2.class, WrappingType.XML));
    
    assertFalse(rc.isSupported(myExClass, WrappingType.ORIGINAL));
    assertTrue(rc.isSupported(myExClass, WrappingType.XML));
  }
}
