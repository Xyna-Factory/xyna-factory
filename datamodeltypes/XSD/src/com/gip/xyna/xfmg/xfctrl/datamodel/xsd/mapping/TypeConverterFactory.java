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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;


import java.lang.reflect.Constructor;

import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.SetterGetterException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.SetterGetterException.SetterGetterFailure;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException.TypeMapperCreationFailure;

/**
 *
 */
public class TypeConverterFactory {

  public static interface TypeConverter<T> {

    String getType();

    T fromString(String value) throws SetterGetterException;
    
    String toString(T value, CreateXmlOptions cxo);
  }
  
  public static abstract class AbstractTypeConverter<T> implements TypeConverter<T> {

    protected String name;
    public AbstractTypeConverter(String name) {
      this.name = name;
    }
    
    public abstract T fromString(String string);

    public String getType() {
      return name;
    }
    
    public String toString( T value, CreateXmlOptions cxo) {
      if( value == null ) {
        return null;
      }
      return String.valueOf(value);
    }

    protected boolean isEmpty(String string) {
      return string == null || string.length() == 0;
    }
    
  }

  public static class StringConverter extends AbstractTypeConverter<String> {

    public StringConverter() {
      super("String");
    }

    @Override
    public String fromString(String string) {
      if( isEmpty(string) ) {
        return null;
      }
      return string;
    }
    
  }

  public static class IntegerConverter extends AbstractTypeConverter<Integer> {
    
    public IntegerConverter() {
      super("Integer");
    }
    
    @Override
    public Integer fromString(String string) {
      if( isEmpty(string) ) {
        return null;
      }
      return Integer.valueOf(string);
    }

  }

  public static class LongConverter extends AbstractTypeConverter<Long> {
    
    public LongConverter() {
      super("Long");
    }
    
    @Override
    public Long fromString(String string) {
      if( isEmpty(string) ) {
        return null;
      }
      return Long.valueOf(string);
    }
  }
 
  public static class BooleanConverter extends AbstractTypeConverter<Boolean> {
    
    public BooleanConverter() {
      super("Boolean");
    }
            
    @Override
    public Boolean fromString(String string) {
      if( isEmpty(string) ) {
        return null;
      }
      //Sonderimplementierung, da Boolean.valueOf("trues") als false ansieht und keine Exception wirft 
      String value = string.toLowerCase();
      if ("true".equals(value)) {
        return Boolean.TRUE;
      }
      if ("false".equals(value)) {
        return Boolean.FALSE;
      }
      if ("1".equals(value)) {
        return Boolean.TRUE;
      }
      if ("0".equals(value)) {
        return Boolean.FALSE;
      }
      throw new IllegalArgumentException("Expected [true|false|1|0] instead of \""+string+"\"");
    }
    
    @Override
    public String toString( Boolean value, CreateXmlOptions cxo) {
      if( value == null ) {
        return null;
      }
      if( cxo.booleanAsInteger() ) {
        return value.booleanValue() ? "1" : "0";
      } else {
        return String.valueOf(value);
      }
    }
    
  }

  public static class DoubleConverter extends AbstractTypeConverter<Double> {
    
    public DoubleConverter() {
      super("Double");
    }
    
    @Override
    public Double fromString(String string) {
      if( isEmpty(string) ) {
        return null;
      }
      return Double.valueOf(string);
    }
  }

  public static class FloatConverter extends AbstractTypeConverter<Float> {
    
    public FloatConverter() {
      super("Float");
    }
    
    @Override
    public Float fromString(String string) {
      if( isEmpty(string) ) {
        return null;
      }
      return Float.valueOf(string);
    }
  }

  public static class ShortConverter extends AbstractTypeConverter<Short> {
    
    public ShortConverter() {
      super("Short");
    }
    
    @Override
    public Short fromString(String string) {
      if( isEmpty(string) ) {
        return null;
      }
      return Short.valueOf(string);
    }
  }

  public static class ByteConverter extends AbstractTypeConverter<Byte> {
    
    public ByteConverter() {
      super("Byte");
    }
    
    @Override
    public Byte fromString(String string) {
      if( isEmpty(string) ) {
        return null;
      }
      return Byte.valueOf(string);
    }
  }

  public static class JavaTypeConverter<T> implements TypeConverter<T> {
    
    private Constructor<T> constructor;

    public JavaTypeConverter(Class<T> clazz) throws TypeMapperCreationException {
      try {
        constructor = clazz.getConstructor(String.class);
      } catch (Exception e) {
        throw new TypeMapperCreationException(TypeMapperCreationFailure.StringConstructorMissing, clazz.getCanonicalName(), e);
      }
    }
    
    public String getType() {
      return "JavaType";
    }

    public T fromString(String value) throws SetterGetterException {
      if( value == null ) {
        return null;
      }
      try {
        return constructor.newInstance(value);
      } catch (Exception e) {
        throw new SetterGetterException( SetterGetterFailure.Conversion, value, e);
      }
    }

    public String toString(T value, CreateXmlOptions cxo ) {
      if( value == null ) {
        return null;
      }
      return String.valueOf(value);
    }
    
  }
  
  
  /**
   * @param type
   * @return
   * @throws TypeMapperCreationException 
   */
  @SuppressWarnings("unchecked")
  public static <T> TypeConverter<T> getTypeConverterForType(Class<T> type) throws TypeMapperCreationException {
    TypeConverter<?> tc;
    if (type.isPrimitive()) {
      if (type == int.class) {
        tc = new IntegerConverter();
      } else if (type == long.class) {
        tc = new LongConverter();
      } else if (type == boolean.class) {
        tc = new BooleanConverter();
      } else if (type == double.class) {
        tc = new DoubleConverter();
      } else if (type == float.class) {
        tc = new FloatConverter();
      } else if (type == byte.class) {
        tc = new ByteConverter();
      } else if (type == short.class) {
        tc = new ShortConverter();
      } else {
        throw new TypeMapperCreationException( TypeMapperCreationFailure.UnexpectedType, type.getCanonicalName());
      }
    } else {
      if (type == String.class) {
        tc = new StringConverter();
      } else if (type == Integer.class) {
        tc = new IntegerConverter();
      } else if (type == Long.class) {
        tc = new LongConverter();
      } else if (type == Boolean.class) {
        tc = new BooleanConverter();
      } else if (type == Double.class) {
        tc = new DoubleConverter();
      } else if (type == Float.class) {
        tc = new FloatConverter();
      } else if (type == Byte.class) {
        tc = new ByteConverter();
      } else if (type == Short.class) {
        tc = new ShortConverter();
      } else {
        return constructTypeConverterForType(type);
      }
    }
    return (TypeConverter<T>) tc;
  }


  private static <T> TypeConverter<T> constructTypeConverterForType(Class<T> type) throws TypeMapperCreationException {
    return new JavaTypeConverter<T>(type);
  }


}
