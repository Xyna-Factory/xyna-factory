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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

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
    result.put("List", List.class);
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
    for (Long rev : revisions) {
      packagesPerRevision.remove(rev);
    }
  }

  @Override
  public Map<String, Object> convertToPython(GeneralXynaObject obj) {
    Set<String> varNames = new HashSet<String>();
    Map<String, Object> resultMap = new HashMap<String, Object>();

    GeneralXynaObject convertObj = null;

    if (obj instanceof XynaObject) {
      convertObj = (XynaObject) obj;
      resultMap.put("_xynatype", "DATATYPE");
      resultMap.put("_fqn", obj.getClass().getCanonicalName());
      varNames = ((XynaObject) obj).getVariableNames();
    } else if (obj instanceof XynaExceptionBase) {
      convertObj = (XynaExceptionBase) obj;
      resultMap.put("_xynatype", "EXCEPTION");
      resultMap.put("_fqn", convertObj.getClass().getCanonicalName());
      for (Field f : convertObj.getClass().getDeclaredFields()) {
        if (f.getModifiers() == 2) { // private members
          varNames.add(f.getName());
        }
      }
    } else {
      throw new UnsupportedOperationException();
    }

    for (String i : varNames) {
      try {
        Object memberObj = convertObj.get(i);
        resultMap.put(i, convertJavaValue(memberObj));
      } catch (InvalidObjectPathException e) {
        throw new RuntimeException("Could not load variable names from " + convertObj, e);
      }
    }

    return resultMap;
  }

  @Override
  public GeneralXynaObject convertToJava(Context context, Object obj) {
    PyObject pyObj = (PyObject) obj;
    String fqn = (String) pyObj.getAttr("_fqn");
    String xynatype = (String) pyObj.getAttr("_xynatype");

    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
        .getClassLoaderDispatcher();
    ClassLoaderBase cl;
    if (xynatype.equals("DATATYPE")) {
      cl = cld.findClassLoaderByType(fqn, context.revision, ClassLoaderType.MDM, true);
    } else if (xynatype.equals("EXEPTION")) {
      cl = cld.findClassLoaderByType(fqn, context.revision, ClassLoaderType.Exception, true);
    } else {
      throw new UnsupportedOperationException();
    }

    GeneralXynaObject resultObj = null;
    try {
      @SuppressWarnings("unchecked")
      Class<? extends GeneralXynaObject> clazz = (Class<? extends GeneralXynaObject>) cl.loadClass(fqn);
      resultObj = clazz.getDeclaredConstructor().newInstance();

      for (Field f : clazz.getDeclaredFields()) {
        if (f.getModifiers() == 2) { // private members
          String fieldName = f.getName();
          Object memberAttr = pyObj.getAttr(fieldName);
          resultObj.set(fieldName, convertPythonValue(context, f.getGenericType().getTypeName(), memberAttr));
        }
      }
    } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException
        | InstantiationException e) {
      throw new RuntimeException("Could not create instance of class " + fqn, e);
    } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
      throw new RuntimeException("Could not set member variables in " + resultObj, e);
    }

    return resultObj;
  }


  @Override
  public Object invokeService(Context context, String fqn, String serviceName, List<Object> args) {
    return invokeMethod(context, fqn, null, serviceName, args);
  }


  private Object invokeMethod(Context context, String fqn, Object instance, String serviceName, List<Object> args) {
    Object result = null;
    Method method = findMethod(context, fqn, serviceName);
    try {
      Object[] inputs = convertArguments(context, method, args);
      result = method.invoke(instance, inputs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (result != null) {
      result = convertJavaValue(result);
    }
    return result;
  }


  private Object[] convertArguments(Context context, Method method, List<Object> args) {
    List<Object> result = new ArrayList<Object>();
    for (int i = 0; i < args.size(); i++) {
      Object input = args.get(i);
      String type = method.getParameters()[i].getParameterizedType().getTypeName();
      if (type.startsWith("java.util.List")) {
        type = type.substring("java.util.".length());
        type = type.replace("<? extends ", "<");
      }
      result.add(convertPythonValue(context, type, input));
    }
    return result.toArray();
  }


  @Override
  public Object invokeInstanceService(Context context, Object obj, String serviceName, List<Object> args) {
    GeneralXynaObject xo = convertToJava(context, obj);
    return invokeMethod(context, xo.getClass().getCanonicalName(), xo, serviceName, args);
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


  private Object convertPythonValue(Context context, String type, Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof PyObject) {
      return convertToJava(context, value);
    }
    if (value instanceof List<?>) {
      List<Object> result = new ArrayList<Object>();
      for (Object entry : (List<?>) value) {
        result.add(convertPythonValue(context, removeListFromType(type), entry));
      }
      return result;
    }
    //primitive
    Class<?> c = typeConversionMap.get(type);
    if (c != null && !(c.isAssignableFrom(value.getClass()))) {
      if (value.getClass() == Double.class && c == Float.class) {
        value = (float) ((double) value);
      }
      if (value.getClass() == Long.class && c == int.class) {
        value = (int) ((long) value);
      }
    }
    return value;
  }


  private String removeListFromType(String type) {
    return type.substring(5, type.length() - 1);
  }


  private Method findMethod(Context context, String canonicalName, String serviceName) {
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    ClassLoaderBase cl = cld.getClassLoaderByType(ClassLoaderType.MDM, canonicalName, context.revision);
    try {
      Class<?> c = cl.loadClass(canonicalName);
      return Arrays.asList(c.getDeclaredMethods()).stream().filter(x -> x.getName().equals(serviceName)).findAny().get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private Set<String> searchPackages(Long revision) {
    XMOMDatabase xmomDB = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
    Set<String> result = new HashSet<String>();
    try {
      XMOMDatabaseSelect selectStatement = new XMOMDatabaseSelect();
      selectStatement
          .addAllDesiredResultTypes(Arrays.asList(XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION, XMOMDatabaseType.SERVICEGROUP));
      XMOMDatabaseSearchResult xmoms = xmomDB.searchXMOMDatabase(Arrays.asList(selectStatement), Integer.MAX_VALUE, revision);
      for (XMOMDatabaseSearchResultEntry searchEntry : xmoms.getResult()) {
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
