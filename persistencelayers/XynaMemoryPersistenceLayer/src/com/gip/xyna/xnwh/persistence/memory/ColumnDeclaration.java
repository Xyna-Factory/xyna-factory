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
package com.gip.xyna.xnwh.persistence.memory;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;


public class ColumnDeclaration {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(ColumnDeclaration.class);

  private String name;
  private boolean isPk;
  private Class<?> type;
  private String getter;
  private IndexType indexType;
  private Column annotation;

  public ColumnDeclaration(Column col, Class<?> backingClass, boolean isPk, Class<?> fieldType) {
    this.name = col.name();
    this.isPk = isPk;
    this.type = fieldType;
    this.indexType = col.index();
    this.annotation = col;
    Method[] methods = backingClass.getMethods();
    String getterName1 = "is" + name.toLowerCase();
    String getterName2 = "get" + name.toLowerCase();
    
    //Suche nach Getter: 1. kein ÜbergabeParameter; 2. passender Name; 3. passender ReturnType
    for (Method m : methods) {
      if (m.getParameterTypes().length == 0) {
        String methodName = m.getName().toLowerCase();
        if (getterName1.equals(methodName) || getterName2.equals(methodName) ) {
          if( checkReturnType(m.getReturnType(), fieldType ) ) {
            getter = m.getName();
            break;
          } else {
            if (logger.isInfoEnabled()) {
              logger.info("Found getter method " + methodName + " in " + backingClass.getCanonicalName()
                  + " with incompatible return type '" + m.getReturnType().getName() + "', expected '" + fieldType.getName() + "'.");
            }
          }
        }
      }
    }
    if( getter == null ) {
      if (logger.isInfoEnabled()) {
        logger.info( "No getter found for " + this.toDebugString() + " in storable " + backingClass.getName());
      }
    }
  }
  
  private boolean checkReturnType(Class<?> returnType, Class<?> fieldType) {
    //returnType und fieldType sind identisch
    if (returnType == fieldType ) {
      return true;
    }
    //returnType und fieldType haben gleichen Namen
    String returnTypeName = returnType.getSimpleName().toLowerCase();
    String fieldTypeName = fieldType.getSimpleName().toLowerCase();
    if( returnTypeName.equals(fieldTypeName) || (returnTypeName.startsWith("int") && (fieldTypeName.startsWith("int")))) {
      return true;
    }
    //returnType ist von fieldType zuweisbar: Beispiel: Zurückgegeben wird ein Interface returnType, das von fieldType implementiert wird
    if( returnType.isAssignableFrom(fieldType) ) {
      return true;
    }
    //Typen passen nicht
    return false;
  }

  public Column getAnnotation() {
    return annotation;
  }


  public IndexType getIndexType() {
    return indexType;
  }


  public boolean isPrimaryKey() {
    return isPk;
  }


  public String getJavaType() {
    return type.getSimpleName();
  }
  
  public String getFQJavaType() {
    return type.getName();
  }


  public Class<?> getType() {
    return type;
  }


  /**
   * @return javagetter für diese spalte auf dem basierenden java objekt
   */
  public String getGetter() {
    return getter;
  }


  public String getName() {
    return name;
  }


  public String toString() {
    return name;
  }
  
  public String toDebugString() {
    return "ColumnDeclaration("+name+",isPk="+isPk+", type="+type
      +", getter="+getter+", indexType="+indexType+", annotation="+annotation+")";
  }
  
}
