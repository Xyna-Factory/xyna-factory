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
package com.gip.xyna.xdev.map.xynaobjectgen;



import java.lang.reflect.Constructor;

import org.w3c.dom.Node;

import com.gip.xyna.xdev.map.exceptions.XynaObjectCreationException;
import com.gip.xyna.xdev.map.xynaobjectgen.XynaObjectCreator.XOCExceptionType;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;



/**
 *
 */
public class TypeConverterFactory {

  public static interface TypeConverter<T> {

    T convert(Node node) throws XynaObjectCreationException;
  }

  public static class StringConverter implements TypeConverter<String> {

    public String convert(Node node) throws XynaObjectCreationException {
      return node.getTextContent();
    }
  }

  public static class IntegerConverter implements TypeConverter<Integer> {

    public Integer convert(Node node) throws XynaObjectCreationException {
      String s = node.getTextContent();
      if (isEmpty(s)) {
        return null;
      }
      try {
        return Integer.valueOf(s);
      } catch (Exception e) {
        throw new XynaObjectCreationException("Integer", XOCExceptionType.TypeConverter.name(), node.getNodeName(), e);
      }
    }

  }

  private static boolean isEmpty(String string) {
    return string == null || string.length() == 0;
  }
  
  public static class LongConverter implements TypeConverter<Long> {

    public Long convert(Node node) throws XynaObjectCreationException {
      String s = node.getTextContent();
      if (isEmpty(s)) {
        return null;
      }
      try {
        return Long.valueOf(s);
      } catch (Exception e) {
        throw new XynaObjectCreationException("Long", XOCExceptionType.TypeConverter.name(), node.getNodeName(), e);
      }
    }
  }

  public static class BooleanConverter implements TypeConverter<Boolean> {

    public Boolean convert(Node node) throws XynaObjectCreationException {
      String s = node.getTextContent();
      if (isEmpty(s)) {
        return null;
      }
      try {
        //Sonderimplementierung, da Boolean.valueOf("trues") als false ansieht und keine Exception wirft 
        String value = s.toLowerCase();
        if ("true".equals(value)) {
          return Boolean.TRUE;
        }
        if ("false".equals(value)) {
          return Boolean.FALSE;
        }
      } catch (Exception e) {
        throw new XynaObjectCreationException("Boolean", XOCExceptionType.TypeConverter.name(), node.getNodeName(), e);
      }
      throw new XynaObjectCreationException("Boolean", XOCExceptionType.TypeConverter.name(), node.getNodeName() + "->"
          + node.getTextContent());
    }
  }

  public static class DoubleConverter implements TypeConverter<Double> {

    public Double convert(Node node) throws XynaObjectCreationException {
      String s = node.getTextContent();
      if (isEmpty(s)) {
        return null;
      }
      try {
        return Double.valueOf(s);
      } catch (Exception e) {
        throw new XynaObjectCreationException("Double", XOCExceptionType.TypeConverter.name(), node.getNodeName(), e);
      }
    }

  }

  public static class FloatConverter implements TypeConverter<Float> {

    public Float convert(Node node) throws XynaObjectCreationException {
      String s = node.getTextContent();
      if (isEmpty(s)) {
        return null;
      }
      try {
        return Float.valueOf(s);
      } catch (Exception e) {
        throw new XynaObjectCreationException("Float", XOCExceptionType.TypeConverter.name(), node.getNodeName(), e);
      }
    }

  }
  public static class ShortConverter implements TypeConverter<Short> {

    public Short convert(Node node) throws XynaObjectCreationException {
      String s = node.getTextContent();
      if (isEmpty(s)) {
        return null;
      }
      try {
        return Short.valueOf(s);
      } catch (Exception e) {
        throw new XynaObjectCreationException("Short", XOCExceptionType.TypeConverter.name(), node.getNodeName(), e);
      }
    }

  }
  public static class ByteConverter implements TypeConverter<Byte> {

    public Byte convert(Node node)throws XynaObjectCreationException {
      String s = node.getTextContent();
      if (isEmpty(s)) {
        return null;
      }
      try {
        return Byte.valueOf(s);
      } catch (Exception e) {
        throw new XynaObjectCreationException("Byte", XOCExceptionType.TypeConverter.name(), node.getNodeName(), e);
      }
    }

  }
  public static class JavaTypeConverter<T> implements TypeConverter<T> {

    private Constructor<T> constructor;


    public JavaTypeConverter(Class<T> clazz) throws XynaObjectCreationException {
      try {
        constructor = clazz.getConstructor(String.class);
      } catch (Exception e) {
        throw new XynaObjectCreationException("?", XOCExceptionType.TypeConverterCreation.name(),
                                              clazz.getCanonicalName(), e);
      }
    }


    public T convert(Node node) throws XynaObjectCreationException {
      String value = node.getTextContent();
      try {
        return constructor.newInstance(value);
      } catch (Exception e) {
        throw new XynaObjectCreationException("?", XOCExceptionType.TypeConverter.name(), value, e);
      }
    }

  }

  public static class XynaObjectTypeConverter implements TypeConverter<XynaObject> {

    XynaObjectCreator xynaObjectCreator;


    public XynaObjectTypeConverter(XynaObjectCreator xynaObjectCreator) {
      this.xynaObjectCreator = xynaObjectCreator;
    }


    public XynaObject convert(Node node) throws XynaObjectCreationException {
      return xynaObjectCreator.createXynaObject(node);
    }

  }


  /**
   * @param type
   * @return
   * @throws XynaObjectCreationException 
   */
  public static TypeConverter<?> getTypeConverterForType(Class<?> type) throws XynaObjectCreationException {
    if (type.isPrimitive()) {
      if (type == int.class) {
        return new IntegerConverter();
      } else if (type == long.class) {
        return new LongConverter();
      } else if (type == boolean.class) {
        return new BooleanConverter();
      } else if (type == double.class) {
        return new DoubleConverter();
      } else if (type == float.class) {
        return new FloatConverter();
      } else if (type == byte.class) {
        return new ByteConverter();
      } else if (type == short.class) {
        return new ShortConverter();
      } else {
        throw new XynaObjectCreationException("?", XOCExceptionType.TypeConverterCreation.name(),
                                              type.getCanonicalName());
      }
    } else {
      if (type == Integer.class) {
        return new IntegerConverter();
      } else if (type == Long.class) {
        return new LongConverter();
      } else if (type == Boolean.class) {
        return new BooleanConverter();
      } else if (type == Double.class) {
        return new DoubleConverter();
      } else if (type == Float.class) {
        return new FloatConverter();
      } else if (type == Byte.class) {
        return new ByteConverter();
      } else if (type == Short.class) {
        return new ShortConverter();
      } else {
        return constructTypeConverterForType(type);
      }
    }
  }


  private static <T> TypeConverter<T> constructTypeConverterForType(Class<T> type) throws XynaObjectCreationException {
    return new JavaTypeConverter<T>(type);
  }


}
