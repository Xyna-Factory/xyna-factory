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

package com.gip.xyna;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.XynaException;


public abstract class XynaFactoryComponent {

  protected transient Logger logger;
  
  public XynaFactoryComponent() {
    logger = CentralFactoryLogging.getLogger(getClass());
  }

  // FIXME Change to 'Map<String, List<XynaFactoryPath>>'? With String being the fqClassName of a XynaFactoryComponent
  private static volatile HashMap<Class<? extends XynaFactoryComponent>, List<XynaFactoryPath>> dependencies;

  public abstract String getDefaultName();

  protected abstract void init() throws XynaException;
  
  protected abstract void shutdown() throws XynaException;

  private volatile boolean initialized = false;

  public static List<XynaFactoryPath> getDependencies(XynaFactoryComponent component) {

    if (dependencies == null)
      return new ArrayList<XynaFactoryPath>();

    List<XynaFactoryPath> result = new ArrayList<XynaFactoryPath>();
    Class c = component.getClass();
    while (c != null) {
      addIfNotNull(result, dependencies.get(c));
      c = c.getSuperclass();
    }
    return result;
  }
  
  private static void addIfNotNull(List<XynaFactoryPath> list, List<XynaFactoryPath> arg) {
    if (arg == null) {
      return;
    }
    list.addAll(arg);
  }


  public final void initInternally() throws XynaException {
    if (initialized) {
      throw new XynaException(this + " is already initialized");
    }
    init();
    initialized = true;
  }


  public final void shutDownInternally() throws XynaException {
    if (!initialized) {
      return;
    }
    shutdown();
    dependencies.remove(this.getClass());
    initialized = false;
  }


  public static void addDependencies(Class<? extends XynaFactoryComponent> clazz, ArrayList<XynaFactoryPath> pathes) {
    if (dependencies == null) {
      synchronized (XynaFactoryComponent.class) {
        if (dependencies == null) {
          dependencies = new HashMap<Class<? extends XynaFactoryComponent>, List<XynaFactoryPath>>();
        }
      }
    }
    dependencies.put(clazz, pathes);
    //FIXME: was ist, wenn mehrere components von der gleichen klasse abgeleitet sind,
    //und hier die abgeleitete klasse übergeben wird. beispiel xynadispatcher
  }


  public static HashMap<Class<? extends XynaFactoryComponent>, List<XynaFactoryPath>> getDependencies() {
    return dependencies;
  }

}
