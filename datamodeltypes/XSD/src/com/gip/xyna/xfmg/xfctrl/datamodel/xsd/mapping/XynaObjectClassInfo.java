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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;


/**
 * Kapselt Daten zu einem XynaObject-Typ
 */
public class XynaObjectClassInfo {

  Logger logger = CentralFactoryLogging.getLogger(XynaObjectClassInfo.class);
  
  private static class ClassInfo {
    Class<? extends XynaObject> clazz;
    ClassInfo parent; 
    
    @SuppressWarnings("unchecked")
    public ClassInfo(Class<?> clazz) {
      if( ! XynaObject.class.isAssignableFrom(clazz) ) {
        throw new IllegalArgumentException(clazz.getName() + " is not a XynaObject");
      }
      this.clazz = (Class<? extends XynaObject>) clazz;
      Class<?> par = clazz.getSuperclass();
      if( par != null && par != XynaObject.class ) {
        this.parent = new ClassInfo(par);
      } else {
        this.parent = null;
      }
    }

    public String getClassName() {
      return clazz.getName();
    }

    public Class<? extends XynaObject> getXynaObjectClass() {
      return clazz;
    }
    
    public Long getRevision() {
      ClassLoader cl = clazz.getClassLoader();
      if( cl instanceof ClassLoaderBase ) {
        return ((ClassLoaderBase)cl).getRevision();
      }
      return null;
    }

    public Field getField(String memberVarName) throws NoSuchFieldException {
      try {
        Field field = clazz.getDeclaredField(memberVarName);
        field.setAccessible(true);
        return field;
      } catch( NoSuchFieldException e ) {
        if( parent == null ) {
          NoSuchFieldException nsf = new NoSuchFieldException( memberVarName+" in " + getClassName() +" rev "+getRevision() );
          nsf.initCause(e);
          throw nsf;
        } else {
          return parent.getField(memberVarName); 
        }
      }
    }

    @Override
    public String toString() {
      return getClassName();
    }

    public Method getMethod(String methodName, Class<?> listType) throws NoSuchMethodException {
      try {
        Method method = clazz.getMethod(methodName, listType);
        method.setAccessible(true);
        return method;
      } catch( NoSuchMethodException e ) {
        if( parent == null ) {
          throw e;
        } else {
          return parent.getMethod(methodName,listType); 
        }
      }  
    }

     public String getParentXynaObjectClassName() {
      if( parent != null ) {
        return parent.getClassName();
      }
      return null;
    }
    
  }

  private ClassInfo xynaObject;

  public XynaObjectClassInfo(Class<? extends XynaObject> xynaObjectClass) {
    xynaObject = new ClassInfo( xynaObjectClass);
  }

  @Override
  public String toString() {
    return "XynaObjectClassInfo("+xynaObject+")";
  }
  
  /**
   * @return
   */
  public String getClassName() {
    return xynaObject.getClassName();
  }

  
  public Class<? extends XynaObject> getXynaObjectClass() {
    return xynaObject.getXynaObjectClass();
  }
  
  /**
   * @param fieldName
   * @return
   * @throws NoSuchFieldException 
   */
  public Field getField(String fieldName) throws NoSuchFieldException {
    return xynaObject.getField(fieldName);
  }

  /**
   * @param methodName
   * @param listType
   * @return
   */
  public Method getMethod(String methodName, Class<?> listType) throws NoSuchMethodException {
    logger.debug( "getMethod("+methodName+","+listType+")");
    //Methoden sollten sich vererben, daher nur in xynaObject lesen
    return xynaObject.getMethod(methodName, listType);
  }

  public String getParentXynaObjectClassName() {
    return xynaObject.getParentXynaObjectClassName();
  }

}
