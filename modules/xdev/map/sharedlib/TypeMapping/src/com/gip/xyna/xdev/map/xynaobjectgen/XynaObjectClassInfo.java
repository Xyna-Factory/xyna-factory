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
package com.gip.xyna.xdev.map.xynaobjectgen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.map.exceptions.XynaObjectCreationException;
import com.gip.xyna.xdev.map.xynaobjectgen.XynaObjectCreator.XOCExceptionType;
import com.gip.xyna.xdev.map.xynaobjectgen.XynaObjectCreatorCache.TypeMappingCacheForTarget;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


/**
 * Kapselt Daten zu einem XynaObject-Typ
 */
public class XynaObjectClassInfo {

  Logger logger = CentralFactoryLogging.getLogger(XynaObjectClassInfo.class);
  
  private static class ClassInfo {
    String xsdName;
    Class<? extends XynaObject> clazz;

    @SuppressWarnings("unchecked")
    public ClassInfo(String xsdName, Class<?> clazz) {
      if( ! XynaObject.class.isAssignableFrom(clazz) ) {
        throw new IllegalArgumentException(clazz.getName() + " is not a XynaObject");
      }
      if( xsdName == null ) {
        throw new IllegalArgumentException("xsdName must not be null");
      }
      this.xsdName = xsdName;
      this.clazz = (Class<? extends XynaObject>) clazz;
    }

    public String getClassName() {
      return clazz.getName();
    }

    public Class<? extends XynaObject> getXynaObjectClass() {
      return clazz;
    }

    public Field getField(String memberVarName) throws NoSuchFieldException {
      Field field = clazz.getDeclaredField(memberVarName);
      field.setAccessible(true);
      return field;
    }

    @Override
    public String toString() {
      return xsdName+"->"+getClassName();
    }

    public String getXsdName() {
      return xsdName;
    }

    public Method getMethod(String methodName, Class<?> listType) throws NoSuchMethodException {
      Method method = clazz.getMethod(methodName, listType);
      method.setAccessible(true);
      return method;
    }

    public String lookup(TypeMappingCacheForTarget typeMappingCache, String childKey) {
      return typeMappingCache.lookup( xsdName+":"+childKey );
    }

    public boolean hasField(String name) {
      try {
        clazz.getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        return false;
      }
      return true;
    }
    
  }

  private ClassInfo xynaObject;
  private ArrayList<ClassInfo> parents;
  
  /**
   * @param typeMappingCache
   * @param typeName
   * @throws XynaObjectCreationException 
   */
  public XynaObjectClassInfo(TypeMappingCacheForTarget typeMappingCache, ClassLoader classLoader, String xmlTypeName ) throws XynaObjectCreationException {
    XOCExceptionType exceptionType = XOCExceptionType.ClassLoading;
    try {
      String javaTypeName = GenerationBase.transformNameForJava(xmlTypeName);
      Class<?> clazz = typeMappingCache.lookupClass( classLoader, javaTypeName );
      exceptionType = XOCExceptionType.Lookup;
      System.err.println( "lockup "+clazz.getName()+ "-> "+ typeMappingCache.lookupReverse(clazz.getName() ));
      xynaObject = new ClassInfo( typeMappingCache.lookupReverse(clazz.getName() ), clazz);
      
      parents = new ArrayList<ClassInfo>();
      while( clazz != null ) {
        clazz = clazz.getSuperclass();
        if( clazz == null || clazz == XynaObject.class ) {
          break;
        }
        String xtn = typeMappingCache.lookupReverse(clazz.getName() ); 
        if( xtn != null ) {
          parents.add( new ClassInfo(xtn, clazz) );
        }
        logger.trace("Found parent "+clazz.getName() +" -> " + xtn );
        
      }        
      logger.trace("xynaObject " + xynaObject+", parents "+ parents );
      
    } catch( Exception e ) {
      throw new XynaObjectCreationException("?", exceptionType.name(), xmlTypeName, e ); 
    }
    if( xynaObject == null ) {
      throw new XynaObjectCreationException("?", exceptionType.name(), xmlTypeName ); 
    }
  }

  /**
   * @return
   */
  public String getClassName() {
    return xynaObject.getClassName();
  }

  public String getFieldName(String name) {
    if( xynaObject.hasField(name) ) {
      return xynaObject.getClassName()+":"+name;
    }
    for( ClassInfo parent : parents ) {
      if( parent.hasField(name) ) {
        return parent.getClassName()+":"+name;
      }
    }
    //Field existiert nicht
    return "?:"+name;
  }

  
  public String getFieldName(TypeMappingCacheForTarget typeMappingCache, String childKey) {
    String fieldName = xynaObject.lookup(typeMappingCache,childKey);
    if( fieldName == null ) {
      for( ClassInfo parent : parents ) {
        fieldName = parent.lookup(typeMappingCache,childKey);
        if( fieldName != null ) {
          break;
        }
      }
    }
    return fieldName;
  }

  /**
   * @param fieldName
   * @return
   * @throws NoSuchFieldException 
   */
  public Field getField(String fieldName) throws NoSuchFieldException {
    String[] parts = fieldName.split(":");
    String type = parts[0];
    String name = parts[1];
    logger.trace( "getField "+ fieldName);
    if( type.equals(xynaObject.getClassName() ) ) {
      return xynaObject.getField(name);
    } else {
      for( ClassInfo classInfo : parents ) {
        if( type.equals(classInfo.getClassName() ) ) {
          return classInfo.getField(name);
        }
      }
    }
    //Falls nicht gefunden: Exception werfen durch letzten Versuch:
    return xynaObject.getField(name);
  }

  /**
   * @param methodName
   * @param listType
   * @return
   */
  public Method getMethod(String methodName, Class<?> listType) throws NoSuchMethodException {
    //Methoden sollten sich vererben, daher nur in xynaObject lesen
    return xynaObject.getMethod(methodName, listType);
  }
  
  /**
   * @return
   */
  public String getXsdTypes() {
    if( parents.isEmpty() ) {
      return xynaObject.getXsdName();
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(xynaObject.getXsdName());
      for( ClassInfo classInfo : parents ) {
        sb.append(", ").append(classInfo.getXsdName());
      }
      return sb.toString();
    }
  }

  /**
   * @return
   */
  public Class<? extends XynaObject> getXynaObjectClass() {
    return xynaObject.getXynaObjectClass();
  }
  
}
