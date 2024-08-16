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
        if (memberObj instanceof XynaObject) {
          resultMap.put(i, convertToPython((XynaObject) memberObj));
        } else if (memberObj instanceof XynaExceptionBase) {
          resultMap.put(i, convertToPython((XynaExceptionBase) memberObj));
        } else if (memberObj instanceof List) {
          List<?> memberObjList = (List<?>) memberObj;
          if (memberObjList.size() > 0) {
            if (memberObjList.get(0) instanceof GeneralXynaObject) {
              List<Object> resultList = new ArrayList<Object>();
              for (Object o : memberObjList) {
                resultList.add(convertToPython((GeneralXynaObject) o));
              }
              resultMap.put(i, resultList);
            } else {
              resultMap.put(i, memberObjList);
            }
          }
        } else {
          resultMap.put(i, memberObj);
        }
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
          if (memberAttr instanceof PyObject) {
            resultObj.set(fieldName, convertToJava(context, memberAttr));
          } else if (memberAttr instanceof List) {
            List<?> memberAttrList = (List<?>) memberAttr;
            if (memberAttrList.size() > 0) {
              if (memberAttrList.get(0) instanceof PyObject) {
                List<Object> resultList = new ArrayList<Object>();
                for (Object o : memberAttrList) {
                  resultList.add(convertToJava(context, o));
                }
                resultObj.set(fieldName, resultList);
              } else {
                resultObj.set(fieldName, memberAttrList);
              }
            }
          } else {
            resultObj.set(fieldName, memberAttr);
          }
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
  public Object invokeService(Context context, String fqn, String serviceName, Object... args) {
    return null;
  }

  @Override
  public Object invokeInstanceService(Context context, Object obj, String serviceName, Object... args) {
    return null;
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
