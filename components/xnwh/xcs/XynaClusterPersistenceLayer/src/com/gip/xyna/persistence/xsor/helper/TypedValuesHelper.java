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
package com.gip.xyna.persistence.xsor.helper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.xsor.indices.search.ComparisionAlgorithm;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Storable;


public class TypedValuesHelper {
  
  public enum ValueType {
    STRING(ComparisionAlgorithm.Object, String.class) {
      @Override
      String generateTypedObjectFromString(String value) {
        return value;
      }
    },
    LONG(ComparisionAlgorithm.Object, long.class, Long.class) {
      @Override
      Long generateTypedObjectFromString(String value) {
        return new Long(value);
      }
    },
    INTEGER(ComparisionAlgorithm.Object, int.class, Integer.class) {
      @Override
      Integer generateTypedObjectFromString(String value) {
        return new Integer(value);
      }
    },
    BOOLEAN(ComparisionAlgorithm.Object, boolean.class, Boolean.class) {
      @Override
      Boolean generateTypedObjectFromString(String value) {
        return new Boolean(value);
      }
    },
    DOUBLE(ComparisionAlgorithm.Object, double.class, Double.class) {
      @Override
      Double generateTypedObjectFromString(String value) {
        return new Double(value);
      }
    },
    FLOAT(ComparisionAlgorithm.Object, float.class, Float.class) {
      @Override
      Float generateTypedObjectFromString(String value) {
        return new Float(value);
      }
    },
    BYTE(ComparisionAlgorithm.Object, byte.class, Byte.class) {
      @Override
      Byte generateTypedObjectFromString(String value) {
        return new Byte(value);
      }
    },
    BYTE_ARRAY(ComparisionAlgorithm.ByteArray, byte[].class) {
      @Override
      byte[] generateTypedObjectFromString(String value) {
        if (value.startsWith("'[") &&
            value.endsWith("]'")) {
          String[] splitted = value.substring(2, value.length()-2).split(",");
          byte[] bytes = new byte[splitted.length];
          for (int i=0; i < splitted.length; i++) {
            bytes[i] = Byte.parseByte(splitted[i].trim());
          }
          return bytes;
        } else {
          throw new IllegalArgumentException(value + " for byte[]");
        }
      }
    };
    
    
    private ComparisionAlgorithm algorithmType;
    private Class<?>[] fieldTypes; 
        
    ValueType(ComparisionAlgorithm algorithmType, Class<?>... fieldTypes) {
      this.algorithmType = algorithmType;
      this.fieldTypes = fieldTypes;
    }
    
    public ComparisionAlgorithm getComparisionAlgorithmType() {
     return algorithmType; 
    }
    
    public Class<?>[] getFieldTypes() {
      return fieldTypes; 
     }
    
    abstract Object generateTypedObjectFromString(String value);
    
    static ValueType getValueTypeByFieldClass(Class<?> fieldClazz) {
      for (ValueType valueType : values()) {
        for (Class<?> fieldType : valueType.getFieldTypes()) {
          if (fieldType == fieldClazz) {
            return valueType;
          }
        }
      }
      throw new IllegalArgumentException(fieldClazz.getSimpleName() + " is not a valid fieldType!");
    }
  }
  
  
  private static Map<String, Map<String, ValueType>> cachedTypes = new HashMap<String,  Map<String, ValueType>>();
  
  public static SearchValue generateSearchValueFromStringValue(String columnName, String value, Class<? extends Storable> storableClazz) {
    String tableName = Storable.getPersistable(storableClazz).tableName();
    if (cachedTypes.get(tableName) == null) {
      prepareTypedValues(tableName, storableClazz);
    }
    ValueType columnValueType = cachedTypes.get(tableName).get(columnName);
    if (columnValueType == null) {
      throw new RuntimeException("Unknown column '" + columnName + "' in field list.");
    } else {
      return new SearchValue(columnValueType.generateTypedObjectFromString(value));
    }
  }
  
  
  public static ValueType getValueTypeForColumn(String columnName, Class<? extends Storable> storableClazz) {
    String tableName = Storable.getPersistable(storableClazz).tableName();
    if (cachedTypes.get(tableName) == null) {
      prepareTypedValues(tableName, storableClazz);
    }
    ValueType columnValueType = cachedTypes.get(tableName).get(columnName);
    if (columnValueType == null) {
      throw new RuntimeException("Unknown column '" + columnName + "' in field list.");
    } else {
      return columnValueType;
    }
  }  
  
  
  public static Object generateTypedArrayFromStringArray(String columnName, String[] values, Class<? extends Storable> storableClazz) {
    String tableName = Storable.getPersistable(storableClazz).tableName();
    if (cachedTypes.get(tableName) == null) {
      prepareTypedValues(tableName, storableClazz);
    }
    ValueType columnValueType = cachedTypes.get(tableName).get(columnName);
    if (columnValueType == null) {
      throw new RuntimeException("Unknown column '" + columnName + "' in field list.");
    } else {
      Object[] array = new Object[values.length];
      for (int i=0; i<values.length; i++) {
        array[i] = columnValueType.generateTypedObjectFromString(values[i]);
      }
      return array;
    }
  }
  
  
  private static synchronized void prepareTypedValues(String tableName, final Class<? extends Storable> storableClazz) {
    if (cachedTypes.get(tableName) == null) {
      Map<String, ValueType> preparedTypedValues = new HashMap<String, TypedValuesHelper.ValueType>();
      Class<? extends Storable> currentClass = storableClazz;
      while (currentClass != Storable.class) {
        Field[] fields = currentClass.getDeclaredFields();
        for (Field field : fields) {
          Column annotation = field.getAnnotation(Column.class);
          if (annotation != null) {
            ValueType columnValueType = ValueType.getValueTypeByFieldClass(field.getType());
            preparedTypedValues.put(annotation.name(), columnValueType);
          }
        }
        currentClass = (Class<? extends Storable>) currentClass.getSuperclass();
      }
      cachedTypes.put(tableName, preparedTypedValues);
    }
  }

}
