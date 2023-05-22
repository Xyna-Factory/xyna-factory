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
package com.gip.xyna.xnwh.persistence.memory;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;



public class MemoryResultSetCreator {

  private static final Logger logger = CentralFactoryLogging.getLogger(MemoryResultSetCreator.class);

  //signatur soll werden: public B getC(), feld in objekt in tabelle kann einen der folgenden typen haben 
  private static String[][] typeInfoForResultSetImpl = { 
      //String returnType, String getName, String ... compatibleTypes
      {"long", "Long", "long", "Long"},    
      {"int", "Int", "int", "Integer"}, 
      {"boolean", "Boolean", "boolean", "Boolean"},
      {"double", "Double", "double", "Double"},
      {"Object", "Object", "Object"},
      {"String", "String", "String"}};

  

  private static String getTypeOfMatrix(int col, String javaTypeCol) {
    for (int i = 0; i < typeInfoForResultSetImpl.length; i++) {
      for (int j = 2; j < typeInfoForResultSetImpl[i].length; j++) {
        if (javaTypeCol.equals(typeInfoForResultSetImpl[i][j])) {
          return typeInfoForResultSetImpl[i][col];
        }
      }
    }
    if (logger.isTraceEnabled()) {
      logger.trace("Unsupported type for resultset in " + XynaMemoryPersistenceLayer.class.getSimpleName()
          + " implementation: <" + javaTypeCol + ">. Treating as java.lang.Object.");
    }
    return "Object";
  }


  private static String getResultTypeJavaType(String javaTypeCol) {
    return getTypeOfMatrix(0, javaTypeCol);
  }


  private static String toNameForResultSetGetter(String javaTypeCol) {
    return getTypeOfMatrix(1, javaTypeCol);
  }
  
  
  private static final String _METHODNAME_ENSURE_IDX_ORIG = "ensureIdx";
  private static final String METHODNAME_ENSURE_IDX;
  
  private static final String _METHODNAME_GET_CURRENT_DATA_ORIG = "getCurrentData";
  private static final String METHODNAME_GET_CURRENT_DATA;
  
  private static final String _METHODNAME_NVLSTRING_ORIG = "nvlString";
  private static final String METHODNAME_NVLSTRING;
  
  static {
    //methoden namen auf diese art gespeichert k�nnen von obfuscation tools mit "refactored" werden.
    try {
      METHODNAME_ENSURE_IDX = BaseResultSet.class.getDeclaredMethod(_METHODNAME_ENSURE_IDX_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_ENSURE_IDX_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_CURRENT_DATA = BaseResultSet.class.getDeclaredMethod(_METHODNAME_GET_CURRENT_DATA_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_CURRENT_DATA_ORIG + " not found", e);
    }
    try {
      METHODNAME_NVLSTRING = BaseResultSet.class.getDeclaredMethod(_METHODNAME_NVLSTRING_ORIG, String.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_NVLSTRING_ORIG + " not found", e);
    }
    
    
  }
  
  /**
   * erstellt code f�r className extends superClassName mit konstruktor, der persistencelayer erwartet.
   * dabei werden resultset methoden implementiert, die zu den spalten in tableinfo passen.
   * 
   * die oberklasse muss von {@link BaseResultSet} abgeleitet sein.
   */
  public static void create(CodeBuffer cb, TableInfo t, String className, String superClassName) {
    //TODO JavaClass verwenden, um dynamischer imports definieren zu k�nnen, etc
    // extend resultset
    cb.addLine("public class ", className, " extends ", superClassName, " {").addLB();

    cb.addLine("public ", className, "(", PersistenceLayer.class.getSimpleName(), " pl, boolean isForUpdate) {");
    cb.addLine("super(pl, isForUpdate)");
    cb.addLine("}").addLB();

    // FIXME: eigtl m�ssten nur die spalten verf�gbar gemacht werden, die im sqlstatement angegeben sind
    // in der implementierung werden derzeit immer alle verf�gbar gemacht.
    HashMap<String, ArrayList<Integer>> colTypesToIndices = new HashMap<String, ArrayList<Integer>>();
    for (int i = 0; i < t.getColTypes().length; i++) {
      ColumnDeclaration colType = t.getColTypes()[i];
      Set<String> compatibleJavaTypes = new HashSet<String>();
      if (colType.getAnnotation().type() == ColumnType.BLOBBED_JAVAOBJECT) {
        compatibleJavaTypes.add("Object");
      }
      compatibleJavaTypes.add(getResultTypeJavaType(colType.getJavaType()));
      compatibleJavaTypes.add("String"); //getString sollte immer m�glich sein
      for (String javaType : compatibleJavaTypes) {
        ArrayList<Integer> indices = colTypesToIndices.get(javaType);
        if (indices == null) {
          indices = new ArrayList<Integer>();
          colTypesToIndices.put(javaType, indices);
        }
        indices.add(i);
      }
    }
    for (String javaType : colTypesToIndices.keySet()) {
      String nvl = javaType.equals("String") ? METHODNAME_NVLSTRING : "nvl";
      cb.addLine("public ", javaType, " get", toNameForResultSetGetter(javaType),
                 "(String columnName) throws SQLException {");
      cb.addLine(METHODNAME_ENSURE_IDX, "()");
      ArrayList<Integer> indices = colTypesToIndices.get(javaType);
      for (int i = 0; i < indices.size(); i++) {
        String getter = t.getColTypes()[indices.get(i)].getGetter();
        if (getter == null) {
          continue;
        }
        cb.add("if (\"", t.getColTypes()[indices.get(i)].getName(), "\".equals(columnName)) {").addLB();
        cb.add("return ",nvl,"(((", t.getBackingClass().getCanonicalName(), ") ", METHODNAME_GET_CURRENT_DATA, "()).", getter, "())").addLB();
        cb.add("} else ");
      }

      cb.add("{").addLB();
      cb.addLine("throw new SQLException(\"column \" + columnName + \" not found\")");
      cb.addLine("}");
      cb.addLine("}").addLB();
    }

  }
}
