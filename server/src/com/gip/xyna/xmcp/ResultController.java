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
package com.gip.xyna.xmcp;



import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;



/**
 * Steuerung welche XynaObjekte oder Exceptions in welcher Form zurückgegeben werden, damit man sich gegen
 * Deserialisierungsprobleme durch unbekannte Klassen schützen kann.<br>
 * 
 * Beispiel: Eine Exception enthält einen Cause, dessen Exceptionklasse aus einem PersistenceLayer kommt. Dann
 * kennt der RMI-Client diese Klasse typischerweise nicht und beim Aufruf gibt es eine ClassNotFoundException.<br>
 * 
 * Mit dieser Klasse kann man angeben, welche Exceptions einer Cause-Hierarchie als Original Javaobjekt 
 * zurückgegeben werden sollen, und welche in anderer Form (z.b. nur bestehend aus Klassenname, Message
 * und Stacktrace).<br>
 * Das gleiche kann auch für XynaObjekte gesteuert werden (Wahl zwischen XML und Original Javaobjekt). 
 * TODO Achtung: Diese können als Membervariablen auch Exception enthalten. Diese sollen von dieser Klasse
 * genauso wie normale Exceptions behandelt werden.
 */
public final class ResultController implements Serializable {

  private static final long serialVersionUID = 1L;

  private WrappingType defaultWrappingForXMOMTypes;
  private WrappingType defaultWrappingForExceptions;

  private final Map<String, WrappingType> whiteListExceptionClasses;
  private final Map<String, WrappingType> whiteListExceptionClassLoaders;


  /**
   * defaultWrappingForExceptions = SIMPLE<br>
   * defaultWrappingForXMOMTypes = XML
   */
  public ResultController() {
    whiteListExceptionClasses = new HashMap<String, WrappingType>();
    whiteListExceptionClassLoaders = new HashMap<String, WrappingType>();
    defaultWrappingForExceptions = WrappingType.SIMPLE;
    defaultWrappingForXMOMTypes = WrappingType.XML;
  }


  /**
   * default ist XML. hiermit kann man angeben, dass auch andere daten zurückgegeben werden sollen
   */
  public void setDefaultWrappingTypeForXMOMTypes(WrappingType type) {
    if (type == null) {
      throw new IllegalArgumentException();
    }
    if (type == WrappingType.SIMPLE) {
      throw new IllegalArgumentException(type + " not allowed.");
    }
    
    this.defaultWrappingForXMOMTypes = type;
  }

  /**
   * default ist SIMPLE, diese daten bekommt man immer. hiermit kann man angeben, dass man immer auch andere daten bekommen
   * möchte.
   */
  public void setDefaultWrappingTypeForExceptions(WrappingType type) {
    if (type == null) {
      throw new IllegalArgumentException();
    }
    this.defaultWrappingForExceptions = type;
  }


  /**
   * alle exceptions die von der angegebenen klasse sind (oder davon abgeleitet) werden wie angegeben
   * gewrapped.
   */
  public void addSupportedExceptionClass(Class<? extends Throwable> clazz, WrappingType type) {
    if (clazz == null || type == null) {
      throw new IllegalArgumentException();
    }
    whiteListExceptionClasses.put(clazz.getName(), type);
  }


  /**
   * alle exceptions deren classloader von der übergebenen klasse ist (oder davon abgeleitet) werden wie angegeben
   * gewrapped.
   */
  public void addSupportedExceptionClassLoader(Class<? extends ClassLoader> cl, WrappingType type) {
    if (cl == null || type == null) {
      throw new IllegalArgumentException();
    }
    whiteListExceptionClassLoaders.put(cl.getName(), type);
  }


  /**
   * alle exceptions die von der angegebenen klasse sind (oder davon abgeleitet) werden wie angegeben
   * gewrapped.<br>
   * hilfsmethode für private classloader klassen.
   */
  public void addSupportedExceptionClassLoader(String classLoaderFQName, WrappingType type) {
    if (classLoaderFQName == null || type == null) {
      throw new IllegalArgumentException();
    }
    whiteListExceptionClassLoaders.put(classLoaderFQName, type);
  }


  Map<String, WrappingType> getSupportedExceptionClasses() {
    return whiteListExceptionClasses;
  }


  Map<String, WrappingType> getSupportedExceptionClassLoaders() {
    return whiteListExceptionClassLoaders;
  }


  WrappingType getDefaultWrappingTypeForXMOMTypes() {
    return defaultWrappingForXMOMTypes;
  }


  WrappingType getDefaultWrappingTypeForExceptions() {
    return defaultWrappingForExceptions;
  }


  /**
   * soll ein throwable dieser klasse in dieser art verschickt werden?<br>
   * <ul>
   * <li>SIMPLE ist immer unterstützt</li>
   * <li>defaultForException ist immer unterstützt</li>
   * <li>ORIGINAL ist für alle mit dem bootstrap classloader geladenen klassen unterstützt</li>
   * <li>ansonsten ist die klasse genau dann unterstützt, wenn sie über supportedExceptionClasses oder -ClassLoader angegeben wurde</li>
   * </ul> 
   */
  public boolean isSupported(Class<? extends Throwable> throwableClass, WrappingType type) {
    if (type == defaultWrappingForExceptions || type == WrappingType.SIMPLE) {
      return true;
    }
    if (throwableClass.getClassLoader() == null) {
      //bootstrap classloader -> diese klassen kann der client immer verstehen
      //TODO dieses verhalten konfigurierbar machen
      return type == WrappingType.ORIGINAL;
    }
    return checkClass(throwableClass, type) || checkClassLoader(throwableClass.getClassLoader().getClass(), type);
  }


  private boolean checkClassLoader(Class<? extends ClassLoader> classloader, WrappingType type) {
    if (whiteListExceptionClassLoaders.get(classloader.getName()) == type) {
      return true;
    }
    Class<?> superClass = classloader.getSuperclass();
    if (ClassLoader.class.isAssignableFrom(superClass)) {
      return checkClassLoader((Class<? extends ClassLoader>) superClass, type);
    }
    return false;
  }


  private boolean checkClass(Class<? extends Throwable> throwableClass, WrappingType type) {
    if (whiteListExceptionClasses.get(throwableClass.getName()) == type) {
      return true;
    }
    Class<?> superClass = throwableClass.getSuperclass();
    if (Throwable.class.isAssignableFrom(superClass)) {
      return checkClass((Class<? extends Throwable>) superClass, type);
    }
    return false;
  }


}
