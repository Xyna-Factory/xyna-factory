/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.python;



import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;

import jep.python.PyObject;


public class JepInterpreterFactory extends PythonInterpreterFactory {

  private static final Logger logger = CentralFactoryLogging.getLogger(JepInterpreterFactory.class);
  private Map<Long, Set<String>> packagesPerRevision = new HashMap<>();
  private static final Map<String, Class<?>> typeConversionMap = createTypeConversionMap();
  
  private static Map<String, Class<?>> createTypeConversionMap() {
    Map<String, Class<?>> result = new HashMap<>();
    result.put("boolean", boolean.class);
    result.put("Boolean", Boolean.class);
    result.put("byte", byte.class);
    result.put("Byte", Byte.class);
    result.put("double", double.class);
    result.put("Double", Double.class);
    result.put("int", int.class);
    result.put("Integer", Integer.class);
    result.put("log", long.class);
    result.put("Long", Long.class);
    result.put("Sting", String.class);
    return result;
  }
  
  
  @Override
  public PythonInterpreter createInterperter(ClassLoaderBase classLoader) {
    Long revision = classLoader.getRevision();
    if (!packagesPerRevision.containsKey(revision)) {
      packagesPerRevision.put(revision, searchPackages(revision));
    }
    return new JepInterpreter(classLoader, packagesPerRevision.get(revision));
  }


  @Override
  public void init() {
    try {
      String jepModulePath = System.getProperty("jep.module.path");
      if (jepModulePath != null && !jepModulePath.isBlank()) {
        jep.MainInterpreter.setJepLibraryPath(jepModulePath);
        if (logger.isDebugEnabled()) {
          logger.debug("set jep library path to : " + jepModulePath);
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("jep library path not set!");
        }
      }
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Error during jep init. ", e);
      }
    }
  }


  @Override
  public void invalidateRevisions(Collection<Long> revisions) {
    for (Long rev: revisions) {
      packagesPerRevision.remove(rev);
    }
  }
  
  @Override
  public Map<String, Object> convertToPython(GeneralXynaObject obj) {
    return null;
  }

  @Override
  public GeneralXynaObject convertToJava(Context context, Object obj) {
    return null;
  }


  @Override
  public Object invokeService(Context context, String fqn, String serviceName, List<String> types, Object... args) {
    return invokeMethod(context, fqn, null, serviceName, types, args);
  }


  private Object invokeMethod(Context context, String fqn, Object instance, String serviceName, List<String> types, Object... args) {
    Object result = null;
    Method method = findMethod(context, fqn, serviceName, types, args);
    try {
      Object[] inputs = convertArguments(context, args);
      result = method.invoke(instance, inputs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (result != null) {
      result = convertJavaValue(result);
    }
    return result;
  }


  private Object[] convertArguments(Context context, Object... args) {
    List<Object> result = new ArrayList<Object>();
    for (Object input : args) {
      result.add(convertPythonValue(context, input));
    }
    return result.toArray();
  }


  @Override
  public Object invokeInstanceService(Context context, Object obj, String serviceName, List<String> types, Object... args) {
    GeneralXynaObject xo = convertToJava(context, obj);
    return invokeMethod(context, xo.getClass().getCanonicalName(), xo, serviceName, types, args);
  }


  private Object convertJavaValue(Object value) {
    if (value instanceof Container) {
      List<Object> asList = new ArrayList<Object>();
      Container container = (Container) value;
      for (int i = 0; i < container.size(); i++) {
        asList.add(container.get(i));
      }
      return convertJavaValue(asList);
    }
    if (value instanceof List) {
      List<Object> result = new ArrayList<Object>();
      List<?> asList = (List<?>) value;
      for (Object o : asList) {
        result.add(convertJavaValue(o));
      }
      return result;
    }
    if (value instanceof GeneralXynaObject) {
      return convertToPython((GeneralXynaObject) value);
    }
    //primitive
    return value;
  }


  private Object convertPythonValue(Context context, Object value) {
    if (value instanceof PyObject) {
      return convertToJava(context, value);
    }
    if (value instanceof List<?>) {
      List<Object> result = new ArrayList<Object>();
      for (Object entry : (List<?>) value) {
        result.add(convertPythonValue(context, entry));
      }
      return result;
    }
    //primitive
    return value;
  }


  private Method findMethod(Context context, String canonicalName, String serviceName, List<String> types, Object[] args) {
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    ClassLoaderBase cl = cld.getClassLoaderByType(ClassLoaderType.MDM, canonicalName, context.revision);
    Class<?> c = null;
    Method result = null;
    try {
      c = cl.loadClass(canonicalName);
      Class<?>[] parameterClasses = new Class<?>[args.length];
      for(int i=0; i< args.length; i++) {
        parameterClasses[i] = loadType(cl, types.get(i));
      }
      result = c.getMethod(serviceName, parameterClasses);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return result;
  }
  

  private Class<?> loadType(ClassLoaderBase cl, String name) {
    try {
      return typeConversionMap.getOrDefault(name, cl.loadClass(name));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }


  private Set<String> searchPackages(Long revision) {
    XMOMDatabase xmomDB = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
    Set<String> result = new HashSet<String>();
    try {
      XMOMDatabaseSelect selectStatement = new XMOMDatabaseSelect();
      selectStatement.addAllDesiredResultTypes(Arrays.asList(XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION, XMOMDatabaseType.SERVICEGROUP));
      XMOMDatabaseSearchResult xmoms = xmomDB.searchXMOMDatabase(Arrays.asList(selectStatement), Integer.MAX_VALUE, revision);
      for (XMOMDatabaseSearchResultEntry searchEntry: xmoms.getResult()) {
        String fqn = searchEntry.getFqName();
        String[] split = fqn.split("\\.");
        result.add(split[0]);
      }
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Error during searching in xmom Database. ", e);
      }
    }
    return result;
  }
}
